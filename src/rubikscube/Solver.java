package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Solver {

    private static final int SOLVER_TIMEOUT_SECONDS = 9;

    public static void main(String[] args) {

        if (args.length < 2) return;

        try {
            List<String> lines = Files.readAllLines(Paths.get(args[0]));
            char[] facelets = parseNet(lines);

            CubieCube cc = NetToCubie.fromFacelets(facelets);

            // Attempt to load pruning tables from disk; build silently if missing.
            boolean loaded = PruningTables.loadFromDisk();
            if (!loaded) {
                PruningTables.buildAllBlocking();
                PruningTables.saveToDisk();
            }

            String sol = "";

            // First try the TwoPhase solver with a timeout; fall back to SimpleIDA if it times out or fails.
            TwoPhaseIDA twoPhase = new TwoPhaseIDA();
            ExecutorService exec1 = Executors.newSingleThreadExecutor();
            Future<String> future1 = exec1.submit(() -> twoPhase.solve(cc));

            boolean twoTimedOut = false;
            long start = System.nanoTime();
            try {
                sol = future1.get(SOLVER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                future1.cancel(true);
                twoTimedOut = true;
            } catch (InterruptedException | ExecutionException e) {
                future1.cancel(true);
                twoTimedOut = true;
            } finally {
                exec1.shutdownNow();
            }
            long end = System.nanoTime();

            if (!twoTimedOut && sol != null && !sol.isEmpty()) {
                // verify TwoPhase program-format solution actually solves the parsed cube
                if (appliesAndSolves(cc, sol, false)) {
                    long totalMs = (end - start) / 1_000_000;
                    long p1 = twoPhase.getPhase1TimeMs();
                    long p2 = twoPhase.getPhase2TimeMs();
                    // sol is program-format; write user-format output
                    String userSol = programToUserPublic(sol);
                    String out = String.format("%s\nPHASE1 %dms\nPHASE2 %dms\nTOTAL %dms\n", userSol, p1, p2, totalMs);
                    Files.writeString(Paths.get(args[1]), out);
                    return;
                }
                // mark for fallback
                sol = null;
            }

            if (twoTimedOut || sol == null || sol.isEmpty()) {
                SimpleIDA simple = new SimpleIDA();
                ExecutorService exec2 = Executors.newSingleThreadExecutor();
                Future<String> future2 = exec2.submit(() -> simple.solve(cc));
                start = System.nanoTime();
                try {
                    sol = future2.get(SOLVER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    future2.cancel(true);
                    sol = "TIMEOUT";
                } catch (InterruptedException | ExecutionException e) {
                    future2.cancel(true);
                    sol = "";
                } finally {
                    exec2.shutdownNow();
                }
                end = System.nanoTime();

                long totalMs = (end - start) / 1_000_000;
                String out;
                if ("TIMEOUT".equals(sol)) {
                    out = "TIMEOUT\nTOTAL " + totalMs + "ms\n";
                } else {
                    // sol is program-format; write directly
                    out = String.format("%s\nTOTAL %dms\n", sol, totalMs);
                }
                Files.writeString(Paths.get(args[1]), out);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String programToUserPublic(String seq) { return programToUser(seq); }

    public static char[] parseNetForVerify(List<String> n) {
        return parseNet(n);
    }

    private static char[] parseNet(List<String> n) {
        char[] f = new char[54];

        // U
        for (int r = 0; r < 3; r++) {
            String line = n.get(r).strip();
            f[r*3+0]=line.charAt(0);
            f[r*3+1]=line.charAt(1);
            f[r*3+2]=line.charAt(2);
        }

        // middle rows
        for (int r = 0; r < 3; r++) {
            String raw = n.get(3+r).replaceAll("\\s+", "");

            f[9 + r*3 + 0] = raw.charAt(0);
            f[9 + r*3 + 1] = raw.charAt(1);
            f[9 + r*3 + 2] = raw.charAt(2);

            f[18 + r*3 + 0] = raw.charAt(3);
            f[18 + r*3 + 1] = raw.charAt(4);
            f[18 + r*3 + 2] = raw.charAt(5);

            f[27 + r*3 + 0] = raw.charAt(6);
            f[27 + r*3 + 1] = raw.charAt(7);
            f[27 + r*3 + 2] = raw.charAt(8);

            f[36 + r*3 + 0] = raw.charAt(9);
            f[36 + r*3 + 1] = raw.charAt(10);
            f[36 + r*3 + 2] = raw.charAt(11);
        }

        // D
        for (int r = 0; r < 3; r++) {
            String line = n.get(6+r).strip();
            f[45 + r*3+0]=line.charAt(0);
            f[45 + r*3+1]=line.charAt(1);
            f[45 + r*3+2]=line.charAt(2);
        }

        return f;
    }

    // Normalize solution so that it actually solves the provided cube. Returns user-convention solution string or null.
    private static String normalizeSolution(CubieCube start, String sol) {
        String[] toks = sol.trim().split("\\s+");
        // variants: as-is, reversed
        String asIs = String.join(" ", toks).trim();
        String rev = "";
        for (int i = toks.length - 1; i >= 0; i--) {
            if (toks[i].isEmpty()) continue;
            if (!rev.isEmpty()) rev += " ";
            rev += toks[i];
        }

        // try: as-is treated as user (invert back when applying)
        if (appliesAndSolves(start, asIs, true)) return asIs;
        // as-is treated as program (convert to user for output)
        if (appliesAndSolves(start, asIs, false)) return programToUser(asIs);
        // reversed treated as user
        if (appliesAndSolves(start, rev, true)) return rev;
        // reversed treated as program
        if (appliesAndSolves(start, rev, false)) return programToUser(rev);
        return null;
    }

    private static boolean appliesAndSolves(CubieCube start, String seq, boolean tokensAreUser) {
        CubieCube c = new CubieCube(start);
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            // if tokens are user, invert URFB back to program convention
            if (tokensAreUser) {
                if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                    if (power == 1) power = 3; else if (power == 3) power = 1;
                }
            }
            int move = faceToMove(face);
            if (move < 0) return false;
            c.applyMove(move, power);
        }
        return c.isSolved();
    }

    private static int faceToMove(char f) {
        switch (f) {
            case 'U': return Moves.U;
            case 'R': return Moves.R;
            case 'F': return Moves.F;
            case 'D': return Moves.D;
            case 'L': return Moves.L;
            case 'B': return Moves.B;
        }
        return -1;
    }

    // convert program-format seq to user-format (no special inversion)
    private static String programToUser(String seq) {
        StringBuilder out = new StringBuilder();
        String[] toks = seq.trim().split("\\s+");
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i];
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            out.append(face);
            if (power == 2) out.append('2');
            else if (power == 3) out.append('\'');
            if (i < toks.length - 1) out.append(' ');
        }
        return out.toString();
    }
}
