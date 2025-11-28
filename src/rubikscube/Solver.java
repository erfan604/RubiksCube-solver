package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Solver {


    public static void main(String[] args) {
        MoveTables.init();
        LightPruningTables.buildAllBlocking();

        if (args.length < 2) {
            solveBatch();
            return;
        }

        String inFile = args[0];
        String outFile = args[1];

        try {
            List<String> lines = Files.readAllLines(Paths.get(inFile));
            char[] facelets = parseNet(lines);
            CubieCube cc = NetToCubie.fromFacelets(facelets);
            long t0 = System.nanoTime();
            String sol = solveOne(cc);
            double elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0;
            String user = (sol == null || sol.isEmpty()) ? "" : programToCompact(sol);
            Files.write(Paths.get(outFile), Arrays.asList(user));
            String label = Paths.get(inFile).getFileName().toString();
            System.out.printf("%s %s in %.3f seconds%n", label, user.isEmpty() ? "unsolved" : "solved", elapsedSec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Batch harness: testcases/scramble01..40 -> solutionXX.txt, no timeout limit
    private static void solveBatch() {
        for (int i = 1; i <= 40; i++) {
            long t0 = System.nanoTime();
            String scrambleFile = String.format("testcases/scramble%02d.txt", i);
            String outFile = String.format("solution%02d.txt", i);
            String userSolution = "";
            try {
                List<String> lines = Files.readAllLines(Paths.get(scrambleFile));
                CubieCube cc = NetToCubie.fromFacelets(parseNetForVerify(lines));
                String prog = new TwoPhaseIDA().solve(new CubieCube(cc));
                if (prog != null && !prog.isEmpty()) userSolution = programToCompact(prog);
            } catch (Exception e) {
                userSolution = "";
            }
            double elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0;
            try {
                Files.write(Paths.get(outFile), Arrays.asList(userSolution));
            } catch (Exception ignored) { }
            System.out.printf("scramble%02d %s in %.3f seconds%n", i, userSolution.isEmpty() ? "unsolved" : "solved", elapsedSec);
        }
    }

    // Solve a single cube with no timeout; try a stricter prune first for hard scrambles, then fall back
    private static String solveOne(CubieCube cc) {
        // Rough hardness estimate from phase-1 heuristic
        int h1 = estimatePhase1Heuristic(cc);

        if (h1 >= 7) {
            // Pass 1: stricter phase-2 pruning with opposite blocking to speed deeper cases
            TwoPhaseIDA.BLOCK_OPPOSITE_IN_PHASE2 = true;
            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<String> strict = exec.submit(() -> new TwoPhaseIDA().solve(new CubieCube(cc)));
            String sol = "";
            try {
                sol = strict.get(8, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                strict.cancel(true);
            } catch (Exception e) {
                strict.cancel(true);
            } finally {
                exec.shutdownNow();
            }
            if (sol != null && !sol.isEmpty()) {
                TwoPhaseIDA.BLOCK_OPPOSITE_IN_PHASE2 = false;
                return sol;
            }
        }
        // Fallback: relaxed pruning, no visited set to avoid false pruning
        TwoPhaseIDA.BLOCK_OPPOSITE_IN_PHASE2 = false;
        String fallback = new TwoPhaseIDA().solve(new CubieCube(cc));
        return fallback == null ? "" : fallback;
    }

    // Phase-1 heuristic estimate (matches TwoPhaseIDA)
    private static int estimatePhase1Heuristic(CubieCube c) {
        int co = c.getCornerOriCoord();
        int eo = c.getEdgeOriCoord();
        int sl = c.getUDSliceCoord();
        int hCo = LightPruningTables.coSlicePrun[co * LightPruningTables.N_SLICE + sl];
        int hEo = LightPruningTables.eoSlicePrun[eo * LightPruningTables.N_SLICE + sl];
        if (hCo < 0) hCo = 0;
        if (hEo < 0) hEo = 0;
        return Math.max(hCo, hEo);
    }


    // convert program-format sequence to compact letters-only (no spaces, no suffixes)
    private static String programToCompact(String seq) {
        return CompactMoveEncoder.programToCompact(seq);
    }

    // Public helpers used by other test/driver classes
    public static char[] parseNetForVerify(List<String> n) { return parseNet(n); }

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
