package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class FaceMappingFinder {
    private FaceMappingFinder() {}

    static char[] userFaces = {'U','R','F','D','L','B'};

    public static void main(String[] args) throws Exception {
        // sequences to test
        String seq1 = "U R U U U R R R"; // user expected for scramble01
        String seq2 = "F B B B"; // user expected for scramble02 (FBBB)

        List<String> lines1 = Files.readAllLines(Paths.get("testcases/scramble01.txt"));
        char[] f1 = parseNet(lines1);
        CubieCube c1 = NetToCubie.fromFacelets(f1);

        List<String> lines2 = Files.readAllLines(Paths.get("testcases/scramble02.txt"));
        char[] f2 = parseNet(lines2);
        CubieCube c2 = NetToCubie.fromFacelets(f2);

        int[] perm = new int[6];
        boolean[] used = new boolean[6];
        if (searchPerm(0, perm, used, c1, c2, seq1, seq2)) return;
        System.out.println("No mapping found that makes both sequences solve.");
    }

    private static char[] parseNet(List<String> n) {
        char[] f = new char[54];

        // U
        for (int r = 0; r < 3; r++) {
            String line = n.get(r).strip();
            f[r*3+0]=line.charAt(0);
            f[r*3+1]=line.charAt(1);
            f[r*3+2]=line.charAt(2);
        }

        // middle rows
        for (int r = 0; r < 3; r++) {
            String raw = n.get(3+r).replaceAll("\\s+", "");

            f[9 + r*3 + 0] = raw.charAt(0);
            f[9 + r*3 + 1] = raw.charAt(1);
            f[9 + r*3 + 2] = raw.charAt(2);

            f[18 + r*3 + 0] = raw.charAt(3);
            f[18 + r*3 + 1] = raw.charAt(4);
            f[18 + r*3 + 2] = raw.charAt(5);

            f[27 + r*3 + 0] = raw.charAt(6);
            f[27 + r*3 + 1] = raw.charAt(7);
            f[27 + r*3 + 2] = raw.charAt(8);

            f[36 + r*3 + 0] = raw.charAt(9);
            f[36 + r*3 + 1] = raw.charAt(10);
            f[36 + r*3 + 2] = raw.charAt(11);
        }

        // D
        for (int r = 0; r < 3; r++) {
            String line = n.get(6+r).strip();
            f[45 + r*3+0]=line.charAt(0);
            f[45 + r*3+1]=line.charAt(1);
            f[45 + r*3+2]=line.charAt(2);
        }

        return f;
    }

    private static boolean searchPerm(int idx, int[] perm, boolean[] used, CubieCube c1, CubieCube c2, String seq1, String seq2) {
        if (idx == 6) {
            // try all inversion masks
            for (int mask = 0; mask < (1<<6); mask++) {
                if (testMapping(perm, mask, c1, seq1) && testMapping(perm, mask, c2, seq2)) {
                    printMapping(perm, mask);
                    return true;
                }
            }
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (used[i]) continue;
            used[i] = true; perm[idx] = i;
            if (searchPerm(idx+1, perm, used, c1, c2, seq1, seq2)) return true;
            used[i] = false;
        }
        return false;
    }

    private static boolean testMapping(int[] perm, int mask, CubieCube base, String seq) {
        CubieCube c = new CubieCube(base);
        String[] tokens = seq.split("\\s+");
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            char uf = t.charAt(0);
            int ui = idxOfUserFace(uf);
            if (ui == -1) return false;
            int mapped = perm[ui]; // 0..5 program face index
            int move = programFaceToMove(mapped);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else if (t.charAt(1) == '\'') power = 3;
            // apply inversion
            if (((mask >> ui) & 1) == 1) {
                if (power == 1) power = 3; else if (power == 3) power = 1; // swap
            }
            c.applyMove(move, power);
        }
        return c.isSolved();
    }

    private static int idxOfUserFace(char f) {
        for (int i = 0; i < userFaces.length; i++) if (userFaces[i] == f) return i;
        return -1;
    }

    private static int programFaceToMove(int idx) {
        switch (idx) {
            case 0: return Moves.U;
            case 1: return Moves.R;
            case 2: return Moves.F;
            case 3: return Moves.D;
            case 4: return Moves.L;
            case 5: return Moves.B;
        }
        return -1;
    }

    private static void printMapping(int[] perm, int mask) {
        System.out.println("Found mapping:");
        for (int i = 0; i < perm.length; i++) {
            System.out.println(" user " + userFaces[i] + " -> program " + faceName(perm[i]) + ( ((mask>>i)&1)==1 ? " (inverted)" : ""));
        }
    }

    private static String faceName(int idx) {
        switch (idx) { case 0: return "U"; case 1: return "R"; case 2: return "F"; case 3: return "D"; case 4: return "L"; case 5: return "B"; }
        return "?";
    }
}
