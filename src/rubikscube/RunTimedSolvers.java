package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

/**
 * Simple harness: for each scramble, run brute-force for 1s, then TwoPhase for the remaining 9s.
 */
public class RunTimedSolvers {

    private static final int BRUTE_TIME_MS = 1_000;
    private static final int TWOPHASE_TIME_MS = 9_000;

    public static void main(String[] args) {
        String[] files = {
                "testcases/scramble01.txt",
                "testcases/scramble02.txt",
                "testcases/scramble03.txt",
                "testcases/scramble04.txt",
                "testcases/scramble05.txt"
        };

        // Ensure tables are ready before timing per-scramble solve attempts.
        MoveTables.init();
        boolean loaded = PruningTables.loadFromDisk();
        if (!loaded) {
            System.err.println("PruningTables: cache miss, rebuilding...");
            PruningTables.buildAllBlocking();
            PruningTables.saveToDisk();
        } else {
            System.err.println("PruningTables: loaded from cache.");
        }
        System.err.println("PruningTables ready. CO ready=" + PruningTables.isCOReady() + " EO ready=" + PruningTables.isEOReady() + " SLICE ready=" + PruningTables.isSliceReady() + " CP ready=" + PruningTables.isCPReady());

        for (String f : files) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(f));
                char[] facelets = Solver.parseNetForVerify(lines);
                CubieCube cc = NetToCubie.fromFacelets(facelets);

                System.out.println(f + ":");

                // sanity check move tables on all moves/powers
                moveTableSanityCheck(cc);

                // quick brute-force attempt (short solutions only)
                FullBruteSolver brute = new FullBruteSolver();
                long bruteDeadline = System.currentTimeMillis() + BRUTE_TIME_MS;
                String bruteSol = brute.solve(new CubieCube(cc), 9, bruteDeadline);
                if (bruteSol != null) {
                    System.out.println("  Brute_solution: " + bruteSol);
                    System.out.println("  Brute_assembled_verified: " + verifyAndDump(bruteSol, cc));
                    System.out.println();
                    continue; // solved, skip TwoPhase
                }

                // run TwoPhase with remaining time budget
                String sol = runTwoPhaseUnderTimeout(cc, TWOPHASE_TIME_MS);
                System.out.println("  TwoPhase_solution: " + (sol == null ? "" : sol));
                if (sol != null && !sol.isEmpty()) {
                    System.out.println("  TwoPhase_assembled_verified: " + verifyAndDump(sol, cc));
                }
                System.out.println();
            } catch (Exception e) {
                System.out.println(f + ": error - " + e.getMessage());
            }
        }
    }

    // run TwoPhase with a timeout and return solution or null on timeout/error
    private static String runTwoPhaseUnderTimeout(CubieCube cc, int timeoutMs) {
        Callable<String> task = () -> {
            TwoPhaseIDA tp = new TwoPhaseIDA();
            return tp.solve(new CubieCube(cc));
        };
        ExecutorService singleExec = Executors.newSingleThreadExecutor();
        long t0 = System.currentTimeMillis();
        Future<String> fut = singleExec.submit(task);
        String sol = null;
        try {
            sol = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
            long t1 = System.currentTimeMillis();
            System.out.println("  TwoPhase time_ms: " + (t1 - t0));
        } catch (Exception e) {
            fut.cancel(true);
            long t1 = System.currentTimeMillis();
            System.out.println("  TwoPhase timed out/err time_ms: " + (t1 - t0));
        } finally {
            singleExec.shutdownNow();
            try { singleExec.awaitTermination(2000, TimeUnit.MILLISECONDS); } catch (InterruptedException ie) { /* ignore */ }
        }
        return sol;
    }

    // sanity-check MoveTables against assembled applyMove for all moves
    private static void moveTableSanityCheck(CubieCube cc) {
        System.err.println("MoveTables sanity check (all moves/powers)");
        int baseCo = cc.getCornerOriCoord();
        int baseEo = cc.getEdgeOriCoord();
        int baseSl = cc.getUDSliceCoord();
        int baseCp = cc.getCornerPermCoord();
        int baseUd = cc.getUDEdgePermCoord();
        for (int m = 0; m < 6; m++) {
            for (int p = 1; p <= 3; p++) {
                CubieCube cCo = CubieCube.fromCornerOriCoord(baseCo); cCo.applyMove(m,p);
                int coA = cCo.getCornerOriCoord(); int coT = MoveTables.applyCO(m, p, baseCo);
                CubieCube cEo = CubieCube.fromEdgeOriCoord(baseEo); cEo.applyMove(m,p);
                int eoA = cEo.getEdgeOriCoord(); int eoT = MoveTables.applyEO(m, p, baseEo);
                CubieCube cSl = CubieCube.fromUDSliceCoord(baseSl); cSl.applyMove(m,p);
                int slA = cSl.getUDSliceCoord(); int slT = MoveTables.applySlice(m, p, baseSl);
                CubieCube cCp = CubieCube.fromCornerPermCoord(baseCp); cCp.applyMove(m,p);
                int cpA = cCp.getCornerPermCoord(); int cpT = MoveTables.applyCP(m, p, baseCp);
                CubieCube cUd = CubieCube.fromUDEdgePermCoord(baseUd); cUd.applyMove(m,p);
                int udA = cUd.getUDEdgePermCoord(); int udT = MoveTables.applyUDEP(m, p, baseUd);

                if (coA != coT || eoA != eoT || slA != slT || cpA != cpT || udA != udT) {
                    System.err.println("  MISMATCH move=" + Moves.moveToString(m,p) + ": co(assembled=" + coA + ",table=" + coT + ") eo(assembled=" + eoA + ",table=" + eoT + ") sl(assembled=" + slA + ",table=" + slT + ") cp(assembled=" + cpA + ",table=" + cpT + ") ud(assembled=" + udA + ",table=" + udT + ")");
                }
            }
        }
    }

    private static boolean verifyAndDump(String seq, CubieCube start) {
        try {
            int[] mv = new int[256];
            int[] pw = new int[256];
            int len = parseProgramSequencePublic(seq, mv, pw);
            CubieCube before = new CubieCube(start);
            CubieCube after = new CubieCube(start);
            for (int i = 0; i < len; i++) after.applyMove(mv[i], pw[i]);
            if (after.isSolved()) return true;
            System.out.println("*** ASSEMBLE_MISMATCH ***");
            System.out.println("sequence: " + seq);
            System.out.println("before cp: " + Arrays.toString(before.cp));
            System.out.println("before co: " + Arrays.toString(before.co));
            System.out.println("before ep: " + Arrays.toString(before.ep));
            System.out.println("before eo: " + Arrays.toString(before.eo));
            System.out.println("after cp: " + Arrays.toString(after.cp));
            System.out.println("after co: " + Arrays.toString(after.co));
            System.out.println("after ep: " + Arrays.toString(after.ep));
            System.out.println("after eo: " + Arrays.toString(after.eo));
            System.out.println("cornerPermCoord: " + after.getCornerPermCoord());
            System.out.println("udSliceCoord: " + after.getUDSliceCoord());
            System.out.println("edgeOriCoord: " + after.getEdgeOriCoord());
            return false;
        } catch (Exception e) {
            System.out.println("verifyAndDump error: " + e.getMessage());
            return false;
        }
    }

    // simple parser for program-format sequences like "U R2 F' D" -> mv/pw arrays
    private static int parseProgramSequencePublic(String seq, int[] mv, int[] pw) {
        if (seq == null || seq.trim().isEmpty()) return 0;
        String[] toks = seq.trim().split("\\s+");
        int idx = 0;
        for (String tk : toks) {
            if (tk.isEmpty()) continue;
            String t = tk.trim();
            int power = 1;
            if (t.endsWith("2")) { power = 2; t = t.substring(0, t.length()-1); }
            else if (t.endsWith("'")) { power = 3; t = t.substring(0, t.length()-1); }
            if (t.isEmpty()) continue;
            char c = Character.toUpperCase(t.charAt(0));
            int move = -1;
            switch (c) {
                case 'U': move = Moves.U; break;
                case 'R': move = Moves.R; break;
                case 'F': move = Moves.F; break;
                case 'D': move = Moves.D; break;
                case 'L': move = Moves.L; break;
                case 'B': move = Moves.B; break;
                default: continue;
            }
            mv[idx] = move; pw[idx] = power; idx++;
            if (idx >= mv.length) break;
        }
        return idx;
    }
}
