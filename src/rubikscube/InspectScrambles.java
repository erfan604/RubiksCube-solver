package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class InspectScrambles {
    public static void main(String[] args) throws Exception {
        int[] ids = {4,5};
        for (int id : ids) {
            String idStr = String.format("%02d", id);
            System.out.println("\n=== Inspecting scramble " + idStr + " ===");
            List<String> net = Files.readAllLines(Paths.get("testcases/scramble" + idStr + ".txt"));
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            TwoPhaseIDA two = new TwoPhaseIDA();
            String twoSol = runWithTimeout(() -> two.solve(new CubieCube(cc)), 9, TimeUnit.SECONDS, "TwoPhase-" + idStr);
            System.out.println("TwoPhase solution: " + twoSol);
            if (twoSol != null && !twoSol.isBlank()) {
                CubieCube copy = new CubieCube(cc);
                applySequence(copy, twoSol);
                System.out.println("TwoPhase solves: " + copy.isSolved());
            }

            SimpleIDA simple = new SimpleIDA();
            String simpleSol = runWithTimeout(() -> simple.solve(new CubieCube(cc)), 9, TimeUnit.SECONDS, "Simple-" + idStr);
            System.out.println("SimpleIDA solution: " + simpleSol);
            if (simpleSol != null && !simpleSol.isBlank()) {
                CubieCube copy2 = new CubieCube(cc);
                applySequence(copy2, simpleSol);
                System.out.println("SimpleIDA solves: " + copy2.isSolved());
            }
        }
    }

    private static String runWithTimeout(Callable<String> task, long timeout, TimeUnit unit, String name) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<String> f = ex.submit(task);
        try {
            return f.get(timeout, unit);
        } catch (TimeoutException te) {
            f.cancel(true);
            System.out.println(name + " timed out after " + timeout + " " + unit);
            return null;
        } catch (InterruptedException ie) {
            f.cancel(true); Thread.currentThread().interrupt(); return null;
        } catch (ExecutionException ee) {
            System.out.println(name + " threw: " + ee.getCause()); return null;
        } finally {
            ex.shutdownNow();
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
