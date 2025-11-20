package rubikscube;

import java.nio.file.*;
import java.util.*;

public class ParserTester {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ParserTester <scrambleFile>");
            return;
        }

        List<String> lines = Files.readAllLines(Paths.get(args[0]));
        System.out.println("======= RAW LINES =======");
        for (String line : lines) {
            System.out.println("'" + line + "'  (len=" + line.length() + ")");
        }

        System.out.println("\n======= CLEANED MIDDLE ROWS (Remove whitespace) =======");
        for (int r = 3; r < 6; r++) {
            String cleaned = lines.get(r).replaceAll("[^A-Z]", "");
            System.out.println("Row " + r + ": '" + cleaned + "' (len=" + cleaned.length() + ")");
        }

        System.out.println("\n======= FACE EXTRACTION TEST =======");
        char[] f = parseNet(lines);

        // Print each face in 3×3 grids
        printFace("U", f, 0);
        printFace("L", f, 9);
        printFace("F", f, 18);
        printFace("R", f, 27);
        printFace("B", f, 36);
        printFace("D", f, 45);

        System.out.println("\n======= FULL 54 FACELET ARRAY =======");
        for (int i = 0; i < 54; i++) {
            System.out.print(f[i]);
            if ((i + 1) % 3 == 0) System.out.print(" ");
            if ((i + 1) % 9 == 0) System.out.println();
        }
        System.out.println("\n=====================================");
    }

    private static void printFace(String name, char[] f, int start) {
        System.out.println("\n" + name + " face:");
        for (int i = 0; i < 9; i++) {
            System.out.print(f[start + i]);
            if ((i + 1) % 3 == 0) System.out.println();
        }
    }

    // ===== SAME SAFE PARSER YOU AND I WILL USE =====
    private static char[] parseNet(List<String> n) {
        char[] f = new char[54];

        // U face
        for (int r = 0; r < 3; r++) {
            String row = n.get(r).replaceAll("[^A-Z]", "");
            f[r*3+0] = row.charAt(0);
            f[r*3+1] = row.charAt(1);
            f[r*3+2] = row.charAt(2);
        }

        // Middle rows: L F R B
        for (int r = 0; r < 3; r++) {
            String row = n.get(3+r).replaceAll("[^A-Z]", "");  // exactly 12 letters

            f[9 + r*3 + 0] = row.charAt(0);
            f[9 + r*3 + 1] = row.charAt(1);
            f[9 + r*3 + 2] = row.charAt(2);

            f[18 + r*3 + 0] = row.charAt(3);
            f[18 + r*3 + 1] = row.charAt(4);
            f[18 + r*3 + 2] = row.charAt(5);

            f[27 + r*3 + 0] = row.charAt(6);
            f[27 + r*3 + 1] = row.charAt(7);
            f[27 + r*3 + 2] = row.charAt(8);

            f[36 + r*3 + 0] = row.charAt(9);
            f[36 + r*3 + 1] = row.charAt(10);
            f[36 + r*3 + 2] = row.charAt(11);
        }

        // D face
        for (int r = 0; r < 3; r++) {
            String row = n.get(6+r).replaceAll("[^A-Z]", "");
            f[45 + r*3 + 0] = row.charAt(0);
            f[45 + r*3 + 1] = row.charAt(1);
            f[45 + r*3 + 2] = row.charAt(2);
        }

        return f;
    }

    // === HELPER: only returns facelet array without printing ===
    public static char[] parseNetOnly(List<String> lines) {
        // Your original ParserTester.parseNet code but without printing:
        char[] f = new char[54];

        // U
        for (int r = 0; r < 3; r++) {
            String line = lines.get(r).trim();
            f[r*3+0] = line.charAt(0);
            f[r*3+1] = line.charAt(1);
            f[r*3+2] = line.charAt(2);
        }

        // middle rows
        for (int r = 0; r < 3; r++) {
            String raw = lines.get(3+r).replaceAll("\\s+","");
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
            String line = lines.get(6+r).trim();
            f[45 + r*3 + 0] = line.charAt(0);
            f[45 + r*3 + 1] = line.charAt(1);
            f[45 + r*3 + 2] = line.charAt(2);
        }

        return f;
    }

}
