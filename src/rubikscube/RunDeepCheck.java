package rubikscube;

import java.nio.file.*;
import java.util.*;

public class RunDeepCheck {
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 3; i++) {
            String id = String.format("%02d", i);
            System.out.println("\n=== Deep check scramble " + id + " ===");
            List<String> net = Files.readAllLines(Paths.get("testcases/scramble" + id + ".txt"));
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);
            printCube("PARSED/INPUT", cc);

            SimpleIDA simple = new SimpleIDA();
            String prog = simple.solve(new CubieCube(cc));
            System.out.println("Program-format returned: '" + prog + "'");

            // Apply forward (program-format)
            CubieCube fwd = new CubieCube(cc);
            applyProgram(fwd, prog);
            printCube("AFTER applying FORWARD (program-format)", fwd);
            System.out.println("solved? " + fwd.isSolved());

            // build reversed+inverted candidate (program-format)
            String revInv = reverseInvert(prog);
            System.out.println("Reversed+inverted candidate (program-format): '" + revInv + "'");

            // Apply revInv as program-format
            CubieCube r1 = new CubieCube(cc);
            applyProgram(r1, revInv);
            printCube("AFTER applying REV+INV as program-format", r1);
            System.out.println("solved? " + r1.isSolved());

            // Interpret revInv as user-format (i.e., invert U,R,F,B powers before applying)
            String asUser = interpretAsUser(revInv);
            System.out.println("Reversed+inverted interpreted as USER-format: '" + asUser + "'");
            CubieCube r2 = new CubieCube(cc);
            applyProgram(r2, asUser); // applyProgram expects program-format; asUser here is actually program-format after conversion
            printCube("AFTER applying REV+INV interpreted as USER->program", r2);
            System.out.println("solved? " + r2.isSolved());

            // Also try applying revInv by parsing tokens and inverting URFB during application
            CubieCube r3 = new CubieCube(cc);
            applyWithUserToggle(r3, revInv, true);
            printCube("AFTER applying REV+INV with tokensAreUser=true", r3);
            System.out.println("solved? " + r3.isSolved());
        }
    }

    private static void applyProgram(CubieCube c, String seq) {
        if (seq == null || seq.isBlank()) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int move = faceToMove(face);
            c.applyMove(move, power);
        }
    }

    private static void applyWithUserToggle(CubieCube c, String seq, boolean tokensAreUser) {
        if (seq == null || seq.isBlank()) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            if (tokensAreUser) {
                if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                    if (power == 1) power = 3; else if (power == 3) power = 1;
                }
            }
            int move = faceToMove(face);
            c.applyMove(move, power);
        }
    }

    private static String reverseInvert(String seq) {
        if (seq == null) return null;
        String[] toks = seq.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = toks.length - 1; i >= 0; i--) {
            String t = toks[i]; if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1; if (t.length() == 1) power = 1; else if (t.charAt(1) == '2') power = 2; else power = 3;
            int inv = (power == 2) ? 2 : ((power == 1) ? 3 : 1);
            sb.append(Moves.moveToString(faceToMove(face), inv));
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    private static String interpretAsUser(String progSeq) {
        // Convert program-format sequence into a program-format sequence that corresponds to user-format tokens
        if (progSeq == null) return null;
        String[] toks = progSeq.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i]; if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1; if (t.length() == 1) power = 1; else if (t.charAt(1) == '2') power = 2; else power = 3;
            // if original tokens were user, they would invert URFB; invert back to program here
            if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                if (power == 1) power = 3; else if (power == 3) power = 1;
            }
            sb.append(Moves.moveToString(faceToMove(face), power));
            if (i < toks.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private static int faceToMove(char f) {
        switch (f) { case 'U': return Moves.U; case 'R': return Moves.R; case 'F': return Moves.F; case 'D': return Moves.D; case 'L': return Moves.L; case 'B': return Moves.B; }
        return -1;
    }

    private static void printCube(String label, CubieCube c) {
        System.out.println("--- " + label + " ---");
        System.out.print("cp: "); for (int i=0;i<8;i++) System.out.print(c.cp[i] + " "); System.out.println();
        System.out.print("co: "); for (int i=0;i<8;i++) System.out.print(c.co[i] + " "); System.out.println();
        System.out.print("ep: "); for (int i=0;i<12;i++) System.out.print(c.ep[i] + " "); System.out.println();
        System.out.print("eo: "); for (int i=0;i<12;i++) System.out.print(c.eo[i] + " "); System.out.println();
    }
}
