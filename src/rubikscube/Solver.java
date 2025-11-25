package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Solver {

    private static final int DEFAULT_SOLVER_TIMEOUT_MS = 10_000;
    private static final int BRUTE_MS = 1_000;
    private static final int TWOPHASE_MS = 9_000;

    /**
     * If args are provided: args[0]=scramble file, args[1]=output file, optional args[2]=timeoutSeconds
     * Batch mode (no args): solves testcases/scramble01..40 into solutionXX.txt with 10s per scramble.
     */
    public static void main(String[] args) {
        MoveTables.init();
        // build tables fresh in-memory; no cache saved to avoid large files
        PruningTables.buildAllBlocking();

        if (args.length < 2) {
            solveBatch();
            return;
        }

        String inFile = args[0];
        String outFile = args[1];
        int timeoutMs = DEFAULT_SOLVER_TIMEOUT_MS;
        if (args.length >= 3) {
            try { timeoutMs = Integer.parseInt(args[2]) * 1000; } catch (Exception ignored) { }
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(inFile));
            char[] facelets = parseNet(lines);
            CubieCube cc = NetToCubie.fromFacelets(facelets);
            String sol = solveOne(cc, timeoutMs);
            String user = (sol == null || sol.isEmpty()) ? "" : programToUser(sol);
            Files.write(Paths.get(outFile), Arrays.asList(user));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Batch harness: testcases/scramble01..40 -> solutionXX.txt, 10s each (1s brute, 9s TwoPhase)
    private static void solveBatch() {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (int i = 1; i <= 40; i++) {
                String scrambleFile = String.format("testcases/scramble%02d.txt", i);
                String outFile = String.format("solution%02d.txt", i);
                String userSolution = "";
                try {
                    List<String> lines = Files.readAllLines(Paths.get(scrambleFile));
                    CubieCube cc = NetToCubie.fromFacelets(parseNetForVerify(lines));

                    // quick brute try
                    FullBruteSolver brute = new FullBruteSolver();
                    long bruteDeadline = System.currentTimeMillis() + BRUTE_MS;
                    String bruteProg = brute.solve(new CubieCube(cc), 9, bruteDeadline);
                    if (bruteProg != null && !bruteProg.isEmpty()) {
                        userSolution = programToUser(bruteProg);
                    } else {
                        // TwoPhase with timeout
                        Future<String> fut = exec.submit(() -> new TwoPhaseIDA().solve(new CubieCube(cc)));
                        try {
                            String prog = fut.get(TWOPHASE_MS, TimeUnit.MILLISECONDS);
                            if (prog != null && !prog.isEmpty()) userSolution = programToUser(prog);
                        } catch (TimeoutException te) {
                            fut.cancel(true);
                        } catch (Exception e) {
                            fut.cancel(true);
                        }
                    }
                } catch (Exception e) {
                    userSolution = "";
                }
                try {
                    Files.write(Paths.get(outFile), Arrays.asList(userSolution));
                } catch (Exception ignored) { }
                System.out.println(String.format("scramble%02d: %s", i, userSolution.isEmpty() ? "<no solution>" : userSolution));
            }
        } finally {
            exec.shutdownNow();
        }
    }

    // Solve a single cube with a timeout (ms). Uses brute for 1s then TwoPhase for remaining.
    private static String solveOne(CubieCube cc, int timeoutMs) {
        // brute portion
        FullBruteSolver brute = new FullBruteSolver();
        long bruteDeadline = System.currentTimeMillis() + Math.min(BRUTE_MS, timeoutMs);
        String bruteProg = brute.solve(new CubieCube(cc), 9, bruteDeadline);
        if (bruteProg != null && !bruteProg.isEmpty()) return bruteProg;

        int remaining = Math.max(0, timeoutMs - BRUTE_MS);
        TwoPhaseIDA twoPhase = new TwoPhaseIDA();
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<String> fut = exec.submit(() -> twoPhase.solve(new CubieCube(cc)));
        try {
            String sol = fut.get(remaining, TimeUnit.MILLISECONDS);
            exec.shutdownNow();
            return sol == null ? "" : sol;
        } catch (Exception e) {
            fut.cancel(true);
            exec.shutdownNow();
            return "";
        }
    }

    private static String buildUserFromMoves(int[] moves, int[] powers, int start, int len) {
        if (moves == null || powers == null || len <= 0) return "";
        StringBuilder prog = new StringBuilder();
        int end = Math.min(moves.length, start + len);
        for (int i = start; i < end; i++) {
            int mv = moves[i];
            int p = powers[i];
            if (p == 0) break;
            if (prog.length() > 0) prog.append(' ');
            prog.append(Moves.moveToString(mv, p));
        }
        // convert program-format to user-format using existing helper to ensure consistent notation
        return programToUser(prog.toString());
    }

    private static int faceToMove(char f) {
        switch (f) { case 'U': return Moves.U; case 'R': return Moves.R; case 'F': return Moves.F; case 'D': return Moves.D; case 'L': return Moves.L; case 'B': return Moves.B; }
        return -1;
    }

    // convert program-format seq to user-format (no special inversion)
    private static String programToUser(String seq) {
        if (seq == null) return "";
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

    // Public helpers used by other test/driver classes
    public static char[] parseNetForVerify(List<String> n) { return parseNet(n); }
    public static String programToUserPublic(String seq) { return programToUser(seq); }

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
}
