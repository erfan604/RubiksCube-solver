package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Solver {

    public static void main(String[] args) {

        if (args.length < 2) return;

        try {
            List<String> lines = Files.readAllLines(Paths.get(args[0]));
            char[] facelets = parseNet(lines);

            // NEW: build cube directly from net
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            SimpleIDA solver = new SimpleIDA();

            // Run solver with a timeout of 9 seconds
            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<String> future = exec.submit(() -> solver.solve(cc));

            String sol;
            try {
                sol = future.get(9, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                sol = "TIMEOUT";
            } catch (InterruptedException | ExecutionException e) {
                future.cancel(true);
                sol = "";
                e.printStackTrace();
            } finally {
                exec.shutdownNow();
            }

            Files.writeString(Paths.get(args[1]), sol);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
            String raw = n.get(3+r).replaceAll("\\s+","");

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
