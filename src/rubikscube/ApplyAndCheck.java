package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ApplyAndCheck {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java ApplyAndCheck <scrambleFile> <sequence>");
            return;
        }
        List<String> lines = Files.readAllLines(Paths.get(args[0]));
        char[] f = ParserTester.parseNetOnly(lines);
        CubieCube c = NetToCubie.fromFacelets(f);

        // Discovered mapping: user U->program U (inv), R->R (inv), F->F (inv), D->D, L->L, B->B (inv)
        char[] userFaces = {'U','R','F','D','L','B'};
        int[] perm = {0,1,2,3,4,5}; // user index -> program face index
        boolean[] inverted = {true, true, true, false, false, true};

        String seq = args[1];
        System.out.println("Start isSolved=" + c.isSolved());
        System.out.println("Applying: " + seq);
        String[] tokens = seq.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.length() == 0) continue;
            char face = t.charAt(0);
            int ui = -1;
            for (int j = 0; j < userFaces.length; j++) if (userFaces[j] == face) { ui = j; break; }
            if (ui == -1) throw new IllegalArgumentException("Unknown face: " + face);

            int programFace = perm[ui];
            int move = -1;
            switch (programFace) {
                case 0: move = Moves.U; break;
                case 1: move = Moves.R; break;
                case 2: move = Moves.F; break;
                case 3: move = Moves.D; break;
                case 4: move = Moves.L; break;
                case 5: move = Moves.B; break;
            }

            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3; // apostrophe

            // apply inversion mapping if needed (swap 1<->3)
            if (inverted[ui]) {
                if (power == 1) power = 3;
                else if (power == 3) power = 1;
            }

            c.applyMove(move, power);
            System.out.println(" After " + t + " -> program(" + Moves.moveToString(move, power) + "): isSolved=" + c.isSolved());
        }
        System.out.println("Final isSolved=" + c.isSolved());
    }
}
