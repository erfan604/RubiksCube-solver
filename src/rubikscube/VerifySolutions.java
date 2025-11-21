package rubikscube;

import java.nio.file.*;
import java.util.*;

public final class VerifySolutions {
    private VerifySolutions() {}

    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 10; i++) {
            String id = String.format("%02d", i);
            Path scramble = Paths.get("testcases/scramble" + id + ".txt");
            Path out = Paths.get("out_run" + id + ".txt");
            if (!Files.exists(scramble)) {
                System.out.println(id + " MISSING SCRAMBLE");
                continue;
            }
            if (!Files.exists(out)) {
                System.out.println(id + " MISSING OUTPUT");
                continue;
            }

            List<String> lines = Files.readAllLines(out);
            String solLine = "";
            for (String L : lines) {
                if (L != null && !L.isBlank()) { solLine = L.trim(); break; }
            }
            if (solLine.isEmpty() || solLine.equals("TIMEOUT")) {
                System.out.println(id + " FAIL (no solution or TIMEOUT)");
                continue;
            }

            // parse scramble into facelets and cubie
            List<String> net = Files.readAllLines(scramble);
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            // apply program-format solution directly
            String[] toks = solLine.split("\\s+");
            for (String t : toks) {
                if (t.isEmpty()) continue;
                char face = t.charAt(0);
                int power = 1;
                if (t.length() == 1) power = 1;
                else if (t.charAt(1) == '2') power = 2;
                else if (t.charAt(1) == '\'') power = 3;
                int move = faceToMove(face);
                if (move < 0) { System.out.println(id + " FAIL (unknown face " + face + ")"); break; }
                cc.applyMove(move, power);
            }

            System.out.println(id + (cc.isSolved() ? " OK" : " FAIL"));
        }
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
