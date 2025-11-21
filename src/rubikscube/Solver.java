package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Solver {

    private static final int SOLVER_TIMEOUT_SECONDS = 60;

    public static void main(String[] args) {

        if (args.length < 2) return;

        try {
            List<String> lines = Files.readAllLines(Paths.get(args[0]));
            char[] facelets = parseNet(lines);

            CubieCube cc = NetToCubie.fromFacelets(facelets);

            // Ensure pruning tables available (build if needed)
            boolean loaded = PruningTables.loadFromDisk();
            if (!loaded) {
                PruningTables.buildAllBlocking();
                PruningTables.saveToDisk();
            }

            TwoPhaseIDA twoPhase = new TwoPhaseIDA();
            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<String> fut = exec.submit(() -> twoPhase.solve(cc));

            String sol = null;
            long start = System.nanoTime();
            try {
                sol = fut.get(SOLVER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                fut.cancel(true);
                sol = "";
            } catch (InterruptedException | ExecutionException e) {
                fut.cancel(true);
                sol = "";
            } finally {
                exec.shutdownNow();
            }
            long end = System.nanoTime();

            long totalMs = (end - start) / 1_000_000;

            String phase1Seq = "";
            String phase2Seq = "";
            long phase1Time = -1;
            long phase2Time = -1;

            if (sol != null && !sol.isEmpty()) {
                // TwoPhase returns program-format; extract phase1/phase2 from solution arrays
                int p1len = twoPhase.getPhase1Length();
                int p2len = twoPhase.getPhase2Length();
                int[] moves = twoPhase.getSolutionMovesArray();
                int[] powers = twoPhase.getSolutionPowersArray();
                phase1Seq = buildUserFromMoves(moves, powers, 0, p1len);
                phase2Seq = buildUserFromMoves(moves, powers, p1len, p2len);
                phase1Time = twoPhase.getPhase1TimeMs();
                phase2Time = twoPhase.getPhase2TimeMs();
            }

            String fullUser = (sol == null) ? "" : programToUser(sol);

            List<String> out = new ArrayList<>();
            out.add("PHASE1: " + (phase1Seq == null ? "" : phase1Seq));
            out.add("PHASE1_TIME_MS: " + (phase1Time >= 0 ? phase1Time : ""));
            out.add("PHASE2: " + (phase2Seq == null ? "" : phase2Seq));
            out.add("PHASE2_TIME_MS: " + (phase2Time >= 0 ? phase2Time : ""));
            out.add("FULL: " + (fullUser == null ? "" : fullUser));
            out.add("FULL_TIME_MS: " + totalMs);

            Files.write(Paths.get(args[1]), out);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildUserFromMoves(int[] moves, int[] powers, int start, int len) {
        if (moves == null || powers == null || len <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int end = Math.min(moves.length, start + len);
        for (int i = start; i < end; i++) {
            int mv = moves[i];
            int p = powers[i];
            if (p == 0) break;
            char face = Moves.MOVE_NAMES[mv].charAt(0);
            int outPower = p;
            if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                if (p == 1) outPower = 3; else if (p == 3) outPower = 1;
            }
            sb.append(face);
            if (outPower == 2) sb.append('2');
            else if (outPower == 3) sb.append('\'');
            if (i < end - 1) sb.append(' ');
        }
        return sb.toString();
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
