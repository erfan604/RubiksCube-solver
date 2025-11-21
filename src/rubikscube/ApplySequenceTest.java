package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ApplySequenceTest {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java rubikscube.ApplySequenceTest <netfile> <sequence>");
            return;
        }
        try {
            List<String> lines = Files.readAllLines(Paths.get(args[0]));
            char[] facelets = Solver.parseNetForVerify(lines);
            CubieCube start = NetToCubie.fromFacelets(facelets);

            String seq = args[1];
            System.err.println("Input sequence: '" + seq + "'");
            int[] moves = new int[64];
            int[] pows = new int[64];
            int len = parseSequence(seq, moves, pows);

            // build program-format and user-format
            StringBuilder prog = new StringBuilder();
            for (int i=0;i<len;i++) {
                if (prog.length()>0) prog.append(' ');
                prog.append(Moves.moveToString(moves[i], pows[i]));
            }
            String user = Solver.programToUserPublic(prog.toString());
            System.err.println("Program-format: " + prog.toString());
            System.err.println("User-format: " + user);

            // apply the moves to a copy and check solved
            CubieCube c = new CubieCube(start);
            for (int i=0;i<len;i++) c.applyMove(moves[i], pows[i]);
            System.err.println("After applying sequence:");
            printCubeState(c);
            System.err.println("Solved? " + c.isSolved());

            // compare against applying the sequence to solved cube to see if the sequence is a scramble for the net
            CubieCube solved = new CubieCube();
            for (int i=0;i<len;i++) solved.applyMove(moves[i], pows[i]);
            boolean matches = cubeEquals(solved, start);
            System.err.println("Sequence applied to a solved cube produces the parsed scramble? " + matches);

            // compute reversed+inverted and test as well
            int[] rmoves = new int[len]; int[] rpows = new int[len];
            for (int i = 0; i < len; i++) {
                int mv = moves[len - 1 - i]; int pw = pows[len - 1 - i];
                int invPw = (pw == 2) ? 2 : (pw == 1 ? 3 : 1);
                rmoves[i] = mv; rpows[i] = invPw;
            }
            StringBuilder rprog = new StringBuilder();
            for (int i=0;i<len;i++) { if (rprog.length()>0) rprog.append(' '); rprog.append(Moves.moveToString(rmoves[i], rpows[i])); }
            String ruser = Solver.programToUserPublic(rprog.toString());
            System.err.println("Reversed+inverted program-format: " + rprog.toString());
            System.err.println("Reversed+inverted user-format: " + ruser);
            CubieCube rc = new CubieCube(start);
            for (int i = 0; i < len; i++) rc.applyMove(rmoves[i], rpows[i]);
            System.err.println("After applying reversed+inverted: ");
            printCubeState(rc);
            System.err.println("Solved? " + rc.isSolved());

            CubieCube solvedBack = new CubieCube();
            for (int i = 0; i < len; i++) solvedBack.applyMove(rmoves[i], rpows[i]);
            boolean matches2 = cubeEquals(solvedBack, start);
            System.err.println("Reversed+inverted applied to solved cube produces parsed scramble? " + matches2);

            // try Y^k whole-cube rotations to see if sequence would solve under a different face orientation
            for (int k = 1; k <= 3; k++) {
                CubieCube rotated = new CubieCube(c);
                for (int t = 0; t < k; t++) {
                    CubieCube tmp = new CubieCube();
                    rotateY(rotated, tmp);
                    rotated = tmp;
                }
                System.err.println("Solved after applying whole-cube Y^" + k + " rotation? " + rotated.isSolved());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int parseSequence(String s, int[] movesOut, int[] powOut) {
        int n = 0;
        int i = 0;
        s = s.trim();
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (Character.isWhitespace(ch)) { i++; continue; }
            char face = ch;
            if (face != 'U' && face != 'R' && face != 'F' && face != 'D' && face != 'L' && face != 'B') {
                // if input provided in lowercase, accept
                face = Character.toUpperCase(face);
            }
            int mv = faceToMove(face);
            if (mv < 0) {
                // invalid char, skip
                i++; continue;
            }
            int p = 1;
            if (i + 1 < s.length()) {
                char c2 = s.charAt(i+1);
                if (c2 == '2') { p = 2; i += 2; }
                else if (c2 == '\'') { p = 3; i += 2; }
                else if (c2 == '3') { p = 3; i += 2; }
                else { p = 1; i++; }
            } else {
                p = 1; i++;
            }
            movesOut[n] = mv; powOut[n] = p; n++;
            if (n >= movesOut.length) break;
        }
        return n;
    }
    // public wrapper for debugging
    public int parseSequencePublic(String s, int[] movesOut, int[] powOut) { return parseSequence(s, movesOut, powOut); }

    private static int faceToMove(char f) {
        switch (f) { case 'U': return Moves.U; case 'R': return Moves.R; case 'F': return Moves.F; case 'D': return Moves.D; case 'L': return Moves.L; case 'B': return Moves.B; }
        return -1;
    }

    private static boolean cubeEquals(CubieCube a, CubieCube b) {
        for (int i = 0; i < 8; i++) if (a.cp[i] != b.cp[i] || a.co[i] != b.co[i]) return false;
        for (int i = 0; i < 12; i++) if (a.ep[i] != b.ep[i] || a.eo[i] != b.eo[i]) return false;
        return true;
    }

    // --- Cube rotations (Y-axis) to test alternative face orientation conventions ---
    private static void rotateY(CubieCube in, CubieCube out) {
        // corners: URF->UBR, UBR->UBL, UBL->UFL, UFL->URF; DFR->DRB->DBL->DLF->DFR
        out.cp[CubieCube.URF] = in.cp[CubieCube.UFL];
        out.cp[CubieCube.UBR] = in.cp[CubieCube.URF];
        out.cp[CubieCube.ULB] = in.cp[CubieCube.UBR];
        out.cp[CubieCube.UFL] = in.cp[CubieCube.ULB];
        out.cp[CubieCube.DFR] = in.cp[CubieCube.DLF];
        out.cp[CubieCube.DRB] = in.cp[CubieCube.DFR];
        out.cp[CubieCube.DBL] = in.cp[CubieCube.DRB];
        out.cp[CubieCube.DLF] = in.cp[CubieCube.DBL];
        // corner orientation unchanged under whole-cube rotation
        out.co = java.util.Arrays.copyOf(in.co, in.co.length);

        // edges: UR->UB->UL->UF->UR ; DR->DB->DL->DF->DR ; FR->BR->BL->FL->FR
        out.ep[CubieCube.UR] = in.ep[CubieCube.UF];
        out.ep[CubieCube.UB] = in.ep[CubieCube.UR];
        out.ep[CubieCube.UL] = in.ep[CubieCube.UB];
        out.ep[CubieCube.UF] = in.ep[CubieCube.UL];

        out.ep[CubieCube.DR] = in.ep[CubieCube.DF];
        out.ep[CubieCube.DB] = in.ep[CubieCube.DR];
        out.ep[CubieCube.DL] = in.ep[CubieCube.DB];
        out.ep[CubieCube.DF] = in.ep[CubieCube.DL];

        out.ep[CubieCube.FR] = in.ep[CubieCube.FL];
        out.ep[CubieCube.BR] = in.ep[CubieCube.FR];
        out.ep[CubieCube.BL] = in.ep[CubieCube.BR];
        out.ep[CubieCube.FL] = in.ep[CubieCube.BL];
        // edge orientation stays same under whole-cube rotation
        out.eo = java.util.Arrays.copyOf(in.eo, in.eo.length);
    }
    public static void rotateYPub(CubieCube in, CubieCube out) { rotateY(in, out); }


    private static void printCubeState(CubieCube c) {
        System.err.print("cp: "); for (int i=0;i<8;i++) System.err.print(c.cp[i] + " "); System.err.println();
        System.err.print("co: "); for (int i=0;i<8;i++) System.err.print(c.co[i] + " "); System.err.println();
        System.err.print("ep: "); for (int i=0;i<12;i++) System.err.print(c.ep[i] + " "); System.err.println();
        System.err.print("eo: "); for (int i=0;i<12;i++) System.err.print(c.eo[i] + " "); System.err.println();
    }
}
