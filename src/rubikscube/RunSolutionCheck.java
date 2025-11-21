package rubikscube;

import java.nio.file.*;
import java.util.*;

public class RunSolutionCheck {
    public static void main(String[] args) throws Exception {
        int timeoutSec = 20;
        for (int i = 1; i <= 3; i++) {
            String id = String.format("%02d", i);
            System.out.println("\n--- Scramble " + id + " ---");
            List<String> net = Files.readAllLines(Paths.get("testcases/scramble" + id + ".txt"));
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            SimpleIDA simple = new SimpleIDA();
            String seq = simple.solve(new CubieCube(cc));

            System.out.println("Program-format returned: '" + seq + "'");
            String revInv = reverseInvert(seq);
            System.out.println("Reversed+inverted candidate: '" + revInv + "'");

            boolean forwardSolves = appliesAndSolves(cc, seq);
            boolean revSolves = appliesAndSolves(cc, revInv);

            System.out.println("Forward solves? " + forwardSolves);
            System.out.println("Reversed+inverted solves? " + revSolves);

            if (!forwardSolves && revSolves) {
                System.out.println("NOTE: solver returned reversed/inverted form. Consider outputting reversed/inverted.");
            } else if (forwardSolves && !revSolves) {
                System.out.println("Solver returned forward form (correct).");
            } else if (!forwardSolves && !revSolves) {
                System.out.println("Neither forward nor reversed/inverted solved the cube. Investigation needed.");
            } else {
                System.out.println("Both sequences solve (unexpected).\n");
            }
        }
    }

    private static boolean appliesAndSolves(CubieCube start, String seq) {
        if (seq == null || seq.isBlank()) return false;
        CubieCube c = new CubieCube(start);
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int move = faceToMove(face);
            if (move < 0) return false;
            c.applyMove(move, power);
        }
        return c.isSolved();
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

    private static String reverseInvert(String seq) {
        if (seq == null) return null;
        String[] toks = seq.trim().split("\\s+");
        if (toks.length == 0) return seq;
        StringBuilder sb = new StringBuilder();
        for (int i = toks.length - 1; i >= 0; i--) {
            String t = toks[i];
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int inv = (power == 2) ? 2 : ((power == 1) ? 3 : 1);
            sb.append(Moves.moveToString(faceCharToMove(face), inv));
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    private static int faceCharToMove(char f) {
        switch (f) {
            case 'U': return Moves.U;
            case 'R': return Moves.R;
            case 'F': return Moves.F;
            case 'D': return Moves.D;
            case 'L': return Moves.L;
            case 'B': return Moves.B;
        }
        return Moves.U;
    }
}
