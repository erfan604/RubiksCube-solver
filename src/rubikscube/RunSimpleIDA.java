package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class RunSimpleIDA {
    public static void main(String[] args) throws Exception {
        String[] files = {
                "testcases/scramble01.txt",
                "testcases/scramble02.txt",
                "testcases/scramble03.txt",
                "testcases/scramble04.txt",
                "testcases/scramble05.txt"
        };

        SimpleIDA ida = new SimpleIDA();
        for (String f : files) {
            List<String> lines = Files.readAllLines(Paths.get(f));
            char[] facelets = Solver.parseNetForVerify(lines);
            CubieCube start = NetToCubie.fromFacelets(facelets);
            long t0 = System.currentTimeMillis();
            String sol = ida.solve(start);
            long t1 = System.currentTimeMillis();
            System.out.println(f + ":");
            System.out.println("  solution: " + sol);
            System.out.println("  time_ms: " + (t1 - t0));
            System.out.println();
        }
    }
}
