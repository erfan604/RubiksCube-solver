package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FindShortSolution {
    public static void main(String[] args) throws Exception {
        String file = "testcases/scramble02.txt";
        if (args.length > 0) file = args[0];

        List<String> lines = Files.readAllLines(Paths.get(file));
        char[] f = ParserTester.parseNetOnly(lines);
        CubieCube start = NetToCubie.fromFacelets(f);

        String[] faces = {"U","R","F","D","L","B"};

        // try length 1..4
        for (int len = 1; len <= 4; len++) {
            if (search(start, len, new int[len], new int[len], 0)) return;
        }
        System.out.println("No solution of length <=4 found.");
    }

    private static boolean search(CubieCube start, int len, int[] moves, int[] powers, int idx) {
        if (idx == len) {
            CubieCube c = new CubieCube(start);
            for (int i = 0; i < len; i++) c.applyMove(moves[i], powers[i]);
            if (c.isSolved()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; i++) sb.append(Moves.moveToString(moves[i], powers[i])).append(i<len-1?" ":"");
                System.out.println("Found solution length " + len + ": " + sb.toString());
                return true;
            }
            return false;
        }
        for (int m = 0; m < 6; m++) {
            for (int p = 1; p <= 3; p++) {
                moves[idx] = m;
                powers[idx] = p;
                if (search(start, len, moves, powers, idx+1)) return true;
            }
        }
        return false;
    }
}
