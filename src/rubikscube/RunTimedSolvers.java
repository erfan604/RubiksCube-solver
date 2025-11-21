package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs multiple scrambles (01-05) through solvers with a 10s per-scramble timeout.
 * Solver order: TwoPhaseIDA then SimpleIDA. First non-empty solution is reported.
 */
public class RunTimedSolvers {

    private static final int TIMEOUT_SECONDS = 10;

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
        if (!PruningTables.loadFromDisk()) {
            PruningTables.buildAllBlocking();
            PruningTables.saveToDisk();
        }

        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (String f : files) {
                try {
                    List<String> lines = Files.readAllLines(Paths.get(f));
                    char[] facelets = Solver.parseNetForVerify(lines);
                    CubieCube cc = NetToCubie.fromFacelets(facelets);

                    Callable<String> task = () -> {
                        TwoPhaseIDA tp = new TwoPhaseIDA();
                        String sol = tp.solve(cc);
                        if (sol != null && !sol.isEmpty()) return sol;
                        SimpleIDA simple = new SimpleIDA();
                        return simple.solve(cc);
                    };

                    long t0 = System.currentTimeMillis();
                    Future<String> fut = exec.submit(task);
                    String sol;
                    try {
                        sol = fut.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        fut.cancel(true);
                        sol = "";
                    }
                    long t1 = System.currentTimeMillis();

                    System.out.println(f + ":");
                    System.out.println("  time_ms: " + (t1 - t0));
                    System.out.println("  solution: " + sol);
                    System.out.println();
                } catch (Exception e) {
                    System.out.println(f + ": error - " + e.getMessage());
                }
            }
        } finally {
            exec.shutdownNow();
        }
    }
}
