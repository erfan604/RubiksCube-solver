package rubikscube;

import java.nio.file.*;
import java.util.*;

public class DebugApply {
    public static void main(String[] args) throws Exception {
        Path scramble = Paths.get("testcases/scramble01.txt");
        List<String> net = Files.readAllLines(scramble);
        char[] facelets = Solver.parseNetForVerify(net);
        CubieCube cc = NetToCubie.fromFacelets(facelets);

        TwoPhaseIDA two = new TwoPhaseIDA();
        String sol = two.solve(new CubieCube(cc));
        System.out.println("TwoPhase sol: '" + sol + "'");

        CubieCube test = new CubieCube(cc);
        String[] toks = sol.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int move = faceToMove(face);
            test.applyMove(move, power);
        }
        System.out.println("TwoPhase apply solved=" + test.isSolved());
        System.out.print("cp: "); for (int i=0;i<8;i++) System.out.print(test.cp[i]+" "); System.out.println();
        System.out.print("co: "); for (int i=0;i<8;i++) System.out.print(test.co[i]+" "); System.out.println();
        System.out.print("ep: "); for (int i=0;i<12;i++) System.out.print(test.ep[i]+" "); System.out.println();
        System.out.print("eo: "); for (int i=0;i<12;i++) System.out.print(test.eo[i]+" "); System.out.println();

        // Also try SimpleIDA
        SimpleIDA simple = new SimpleIDA();
        String s2 = simple.solve(new CubieCube(cc));
        System.out.println("Simple sol: '" + s2 + "'");
        CubieCube test2 = new CubieCube(cc);
        String[] toks2 = s2.trim().split("\\s+");
        for (String t : toks2) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int move = faceToMove(face);
            test2.applyMove(move, power);
        }
        System.out.println("Simple apply solved=" + test2.isSolved());
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
