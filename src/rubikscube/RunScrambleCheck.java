package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility to check a known move string against a known scramble file and print the resulting state.
 * Hardcoded for scramble01.txt and the sequence "URUUURRR".
 */
public class RunScrambleCheck {

    public static void main(String[] args) {
        String file = "testcases/scramble01.txt";
        String seq = "URUUURRR";

        try {
            List<String> lines = Files.readAllLines(Paths.get(file));
            char[] facelets = Solver.parseNetForVerify(lines);
            CubieCube start = NetToCubie.fromFacelets(facelets);

            int[] moves = new int[64];
            int[] pows = new int[64];
            int len = new ApplySequenceTest().parseSequencePublic(seq, moves, pows);

            System.out.println("Input file: " + file);
            System.out.println("Move sequence: " + seq);
            System.out.println("Parsed length: " + len);

            CubieCube cube = new CubieCube(start);
            for (int i = 0; i < len; i++) cube.applyMove(moves[i], pows[i]);

            System.out.println("After applying sequence:");
            printCubeState(cube);
            System.out.println("Solved? " + cube.isSolved());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printCubeState(CubieCube c) {
        System.out.print("cp: "); for (int i = 0; i < 8; i++) System.out.print(c.cp[i] + " "); System.out.println();
        System.out.print("co: "); for (int i = 0; i < 8; i++) System.out.print(c.co[i] + " "); System.out.println();
        System.out.print("ep: "); for (int i = 0; i < 12; i++) System.out.print(c.ep[i] + " "); System.out.println();
        System.out.print("eo: "); for (int i = 0; i < 12; i++) System.out.print(c.eo[i] + " "); System.out.println();
    }
}
