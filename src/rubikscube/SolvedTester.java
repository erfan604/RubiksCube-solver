package rubikscube;

import java.nio.file.*;
import java.util.List;

public class SolvedTester {

    public static void main(String[] args) {
        try {
            System.out.println("=== TESTING SOLVED CUBE NET ===");

            // Load the solved net file
            List<String> lines = Files.readAllLines(Paths.get("testcases/solved.txt"));

            // Use your existing parser
            char[] facelets = ParserTester.parseNetOnly(lines);

            System.out.println("\n=== FACELETS (0–53) ===");
            for (int i = 0; i < 54; i++) {
                System.out.print(facelets[i]);
                if ((i+1) % 3 == 0) System.out.print(" ");
                if ((i+1) % 9 == 0) System.out.println();
            }

            System.out.println("\n=== CONVERT TO CUBIECUBE ===");
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            System.out.println("\n=== EXPECTED SOLVED STATE ===");
            System.out.println("cp: 0 1 2 3 4 5 6 7");
            System.out.println("co: 0 0 0 0 0 0 0 0");
            System.out.println("ep: 0 1 2 3 4 5 6 7 8 9 10 11");
            System.out.println("eo: 0 0 0 0 0 0 0 0 0 0 0 0");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
