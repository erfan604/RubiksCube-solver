package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MoveTester {

    private static void printFacelets(char[] f) {
        System.out.println("=== FACELETS (0–53) ===");
        for (int i = 0; i < 54; i++) {
            System.out.print(f[i]);
            if ((i + 1) % 3 == 0) System.out.print(" ");
            if ((i + 1) % 9 == 0) System.out.println();
        }
        System.out.println();
    }

    private static void printCubieState(String label, CubieCube c) {
        System.out.println(label);
        System.out.print("cp: ");
        for (int i = 0; i < 8; i++) System.out.print(c.cp[i] + " ");
        System.out.println();
        System.out.print("co: ");
        for (int i = 0; i < 8; i++) System.out.print(c.co[i] + " ");
        System.out.println();
        System.out.print("ep: ");
        for (int i = 0; i < 12; i++) System.out.print(c.ep[i] + " ");
        System.out.println();
        System.out.print("eo: ");
        for (int i = 0; i < 12; i++) System.out.print(c.eo[i] + " ");
        System.out.println();
        System.out.println();
    }

    private static int faceToMove(char m) {
        switch (m) {
            case 'U': return Moves.U;
            case 'R': return Moves.R;
            case 'F': return Moves.F;
            case 'D': return Moves.D;
            case 'L': return Moves.L;
            case 'B': return Moves.B;
        }
        throw new IllegalArgumentException("Unknown move face: " + m);
    }

    private static void applySequence(CubieCube cube, String seq) {
        System.out.println("=== Applying sequence: " + seq + " ===");
        for (int i = 0; i < seq.length(); i++) {
            char ch = seq.charAt(i);
            if (ch == ' ' || ch == '\t') continue;

            int move = faceToMove(ch);
            cube.applyMove(move, 1);

            printCubieState("-- after " + ch + " --", cube);
        }
    }

    public static void main(String[] args) throws Exception {
        String file = "testcases/scramble02.txt";
        if (args.length > 0) file = args[0];

        List<String> lines = Files.readAllLines(Paths.get(file));
        char[] facelets = ParserTester.parseNetOnly(lines);

        System.out.println("=== SCRAMBLE 02 RAW NET → FACELETS ===");
        printFacelets(facelets);

        System.out.println("=== BUILD CUBIECUBE FROM NET ===");
        CubieCube base = NetToCubie.fromFacelets(facelets);
        printCubieState("Initial cubie state (scramble02):", base);

        // FBB
        CubieCube c1 = new CubieCube(base);
        applySequence(c1, "FBB");

        // FBBB
        CubieCube c2 = new CubieCube(base);
        applySequence(c2, "FBBB");
    }
}
