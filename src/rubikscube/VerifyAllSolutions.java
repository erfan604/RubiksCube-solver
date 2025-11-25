package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Simple checker: verifies each solutionXX.txt against testcases/scrambleXX.txt.
 * Prints "<no solution>" when the solution file is empty, otherwise prints true/false.
 */
public class VerifyAllSolutions {
    public static void main(String[] args) {
        for (int i = 1; i <= 40; i++) {
            String solFile = String.format("solution%02d.txt", i);
            String scrFile = String.format("testcases/scramble%02d.txt", i);
            try {
                String sol = Files.exists(Paths.get(solFile))
                        ? Files.readString(Paths.get(solFile)).trim()
                        : "";
                if (sol.isEmpty()) {
                    System.out.println(solFile + ": <no solution>");
                    continue;
                }
                boolean ok = checkSolution(sol, scrFile);
                System.out.println(solFile + ": " + ok);
            } catch (Exception e) {
                System.out.println(solFile + ": error " + e.getMessage());
            }
        }
    }

    // apply program/user-format sequence to the scramble and return whether it solves
    private static boolean checkSolution(String userSeq, String scrambleFile) throws Exception {
        // reuse parser from Solver (user-format accepted by programToUserPublic)
        int[] mv = new int[512];
        int[] pw = new int[512];
        int len = parse(userSeq, mv, pw);
        var lines = Files.readAllLines(Paths.get(scrambleFile));
        CubieCube cc = NetToCubie.fromFacelets(Solver.parseNetForVerify(lines));
        for (int i = 0; i < len; i++) {
            cc.applyMove(mv[i], pw[i]);
        }
        return cc.isSolved();
    }

    // simple parser for sequences like "U R2 F' ..."
    private static int parse(String seq, int[] mv, int[] pw) {
        if (seq == null || seq.trim().isEmpty()) return 0;
        String[] toks = seq.trim().split("\\s+");
        int idx = 0;
        for (String t : toks) {
            if (t.isEmpty()) continue;
            int power = 1;
            if (t.endsWith("2")) { power = 2; t = t.substring(0, t.length()-1); }
            else if (t.endsWith("'")) { power = 3; t = t.substring(0, t.length()-1); }
            if (t.isEmpty()) continue;
            char c = Character.toUpperCase(t.charAt(0));
            int move = switch (c) {
                case 'U' -> Moves.U;
                case 'R' -> Moves.R;
                case 'F' -> Moves.F;
                case 'D' -> Moves.D;
                case 'L' -> Moves.L;
                case 'B' -> Moves.B;
                default -> -1;
            };
            if (move < 0) continue;
            mv[idx] = move;
            pw[idx] = power;
            idx++;
        }
        return idx;
    }
}
