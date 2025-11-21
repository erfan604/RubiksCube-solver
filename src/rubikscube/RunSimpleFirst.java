package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class RunSimpleFirst {
    public static void main(String[] args) throws Exception {
        int timeoutSec = 20;
        // ensure pruning tables
        boolean loaded = PruningTables.loadFromDisk();
        if (!loaded) {
            System.out.println("Pruning tables not found, building (may take a while)...");
            PruningTables.buildAllBlocking();
            PruningTables.saveToDisk();
        }

        for (int i = 1; i <= 3; i++) {
            String id = String.format("%02d", i);
            Path scramble = Paths.get("testcases/scramble" + id + ".txt");
            if (!Files.exists(scramble)) { System.out.println(id + " missing"); continue; }
            List<String> net = Files.readAllLines(scramble);
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            System.out.println("\n--- Scramble " + id + " ---");

            // Try SimpleIDA first
            SimpleIDA simple = new SimpleIDA();
            String sol = null; // program-format
            long start = System.nanoTime();
            ExecutorService ex = Executors.newSingleThreadExecutor();
            Future<String> fut = ex.submit(() -> simple.solve(new CubieCube(cc)));
            try {
                sol = fut.get(timeoutSec, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                fut.cancel(true);
                System.out.println("SimpleIDA timed out after " + timeoutSec + "s");
            } catch (Exception e) {
                fut.cancel(true);
                System.out.println("SimpleIDA failed: " + e.getMessage());
            } finally {
                ex.shutdownNow();
            }
            long end = System.nanoTime();
            long ms = (end - start) / 1_000_000;

            boolean ok = false;
            if (sol != null && !sol.isBlank()) {
                CubieCube copy = new CubieCube(cc);
                applySequence(copy, sol);
                ok = copy.isSolved();
                System.out.println("SimpleIDA produced solution (len ~" + sol.split("\\s+").length + ") solved? " + ok + " in " + ms + "ms");
                if (ok) {
                    String userOut = Solver.programToUserPublic(sol);
                    Files.writeString(Paths.get("out_run" + id + ".txt"), userOut + "\nTOTAL " + ms + "ms\n");
                }
            }

            if (!ok) {
                System.out.println("Falling back to TwoPhase for " + id);
                TwoPhaseIDA two = new TwoPhaseIDA();
                start = System.nanoTime();
                ex = Executors.newSingleThreadExecutor();
                fut = ex.submit(() -> two.solve(new CubieCube(cc)));
                try {
                    sol = fut.get(timeoutSec, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    fut.cancel(true);
                    System.out.println("TwoPhase timed out after " + timeoutSec + "s");
                    sol = null;
                } catch (Exception e) {
                    fut.cancel(true);
                    System.out.println("TwoPhase failed: " + e.getMessage());
                    sol = null;
                } finally { ex.shutdownNow(); }
                end = System.nanoTime(); ms = (end - start) / 1_000_000;
                if (sol != null && !sol.isBlank()) {
                    CubieCube copy = new CubieCube(cc);
                    applySequence(copy, sol);
                    ok = copy.isSolved();
                    System.out.println("TwoPhase produced solution solved? " + ok + " in " + ms + "ms");
                    if (ok) {
                        String userOut = Solver.programToUserPublic(sol);
                        Files.writeString(Paths.get("out_run" + id + ".txt"), userOut + "\nTOTAL " + ms + "ms\n");
                    }
                }
            }

            if (!ok) System.out.println(id + " -> no verified solution produced within time limits");
        }
    }

    private static void applySequence(CubieCube c, String seq) {
        if (seq == null || seq.isBlank()) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            c.applyMove(faceToMove(face), power);
        }
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
}
