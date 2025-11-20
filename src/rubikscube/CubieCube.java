package rubikscube;

import java.util.Arrays;

public class CubieCube {

    // --- CORNER INDICES ---
    public static final int URF = 0;
    public static final int UFL = 1;
    public static final int ULB = 2;
    public static final int UBR = 3;
    public static final int DFR = 4;
    public static final int DLF = 5;
    public static final int DBL = 6;
    public static final int DRB = 7;

    // --- EDGE INDICES ---
    public static final int UR = 0;
    public static final int UF = 1;
    public static final int UL = 2;
    public static final int UB = 3;
    public static final int DR = 4;
    public static final int DF = 5;
    public static final int DL = 6;
    public static final int DB = 7;
    public static final int FR = 8;
    public static final int FL = 9;
    public static final int BL = 10;
    public static final int BR = 11;

    // --------------------------------------------
    // Cubie state
    // --------------------------------------------

    // Corner permutation (0..7)
    public byte[] cp = new byte[8];
    // Corner orientation (0..2)
    public byte[] co = new byte[8];

    // Edge permutation (0..11)
    public byte[] ep = new byte[12];
    // Edge orientation (0..1)
    public byte[] eo = new byte[12];

    // --------------------------------------------
    // Constructor: identity cube
    // --------------------------------------------
    public CubieCube() {
        for (byte i = 0; i < 8; i++) {
            cp[i] = i;
            co[i] = 0;
        }
        for (byte i = 0; i < 12; i++) {
            ep[i] = i;
            eo[i] = 0;
        }
    }

    public CubieCube(CubieCube c) {
        this.cp = Util.copyArray(c.cp);
        this.co = Util.copyArray(c.co);
        this.ep = Util.copyArray(c.ep);
        this.eo = Util.copyArray(c.eo);
    }

    // --------------------------------------------
    // Swap helpers
    // --------------------------------------------
    private static void cycleCorner(byte[] arr, int a, int b, int c, int d) {
        byte tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = arr[c];
        arr[c] = arr[d];
        arr[d] = tmp;
    }

    private static void cycleEdge(byte[] arr, int a, int b, int c, int d) {
        byte tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = arr[c];
        arr[c] = arr[d];
        arr[d] = tmp;
    }

    private static void addCornerOri(byte[] co, int a, int b, int c, int d) {
        co[a] = (byte) ((co[a] + 2) % 3);
        co[b] = (byte) ((co[b] + 1) % 3);
        co[c] = (byte) ((co[c] + 2) % 3);
        co[d] = (byte) ((co[d] + 1) % 3);
    }

    // --------------------------------------------
    // SINGLE MOVE APPLICATION (clockwise quarter-turn)
    // This applies the basic move; Move powers handled outside.
    // --------------------------------------------
    public void move(int m) {
        switch (m) {

            // ----------------- U -----------------
            case Moves.U:
                cycleCorner(cp, URF, UFL, ULB, UBR);
                cycleCorner(co, URF, UFL, ULB, UBR);
                cycleEdge(ep, UR, UF, UL, UB);
                cycleEdge(eo, UR, UF, UL, UB);
                break;

            // ----------------- R -----------------
            case Moves.R:
                cycleCorner(cp, URF, UBR, DRB, DFR);
                cycleCorner(co, URF, UBR, DRB, DFR);
                // corner orientation changes for R
                addCornerOri(co, URF, UBR, DRB, DFR);
                cycleEdge(ep, UR, BR, DR, FR);
                cycleEdge(eo, UR, BR, DR, FR);
                break;

            // ----------------- F -----------------
            case Moves.F:
                cycleCorner(cp, UFL, URF, DFR, DLF);
                cycleCorner(co, UFL, URF, DFR, DLF);
                // corner orientation changes for F
                addCornerOri(co, UFL, URF, DFR, DLF);
                cycleEdge(ep, UF, FR, DF, FL);
                cycleEdge(eo, UF, FR, DF, FL);
                // flip the four edges around F
                eo[UF] ^= 1;
                eo[FR] ^= 1;
                eo[DF] ^= 1;
                eo[FL] ^= 1;
                break;

            // ----------------- D -----------------
            case Moves.D:
                cycleCorner(cp, DLF, DFR, DRB, DBL);
                cycleCorner(co, DLF, DFR, DRB, DBL);
                cycleEdge(ep, DF, DR, DB, DL);
                cycleEdge(eo, DF, DR, DB, DL);
                break;

            // ----------------- L -----------------
            case Moves.L:
                cycleCorner(cp, ULB, UFL, DLF, DBL);
                cycleCorner(co, ULB, UFL, DLF, DBL);
                // corner orientation changes for L
                addCornerOri(co, ULB, UFL, DLF, DBL);
                cycleEdge(ep, UL, FL, DL, BL);
                cycleEdge(eo, UL, FL, DL, BL);
                break;

            // ----------------- B -----------------
            case Moves.B:
                // permutation
                cycleCorner(cp, UBR, ULB, DBL, DRB);
                cycleCorner(co, UBR, ULB, DBL, DRB);

                // correct corner orientation
                addCornerOri(co, UBR, ULB, DBL, DRB);

                cycleEdge(ep, UB, BL, DB, BR);
                cycleEdge(eo, UB, BL, DB, BR);

                // correct edge flips (all four around B)
                eo[UB] ^= 1;
                eo[BL] ^= 1;
                eo[DB] ^= 1;
                eo[BR] ^= 1;
                break;



        }
    }

    // --------------------------------------------
    // Apply a move with 1,2,3 quarter-turns (3 = counterclockwise)
    // --------------------------------------------
    public void applyMove(int move, int power) {
        for (int i = 0; i < power; i++) move(move);
    }

    // --------------------------------------------
    // Check solved state
    // --------------------------------------------
    public boolean isSolved() {
        for (int i = 0; i < 8; i++)
            if (cp[i] != i || co[i] != 0) return false;
        for (int i = 0; i < 12; i++)
            if (ep[i] != i || eo[i] != 0) return false;
        return true;
    }

    // -------- Corner Orientation (0..3^7-1 = 2186) --------
    public int getCornerOriCoord() {
        int res = 0;
        for (int i = 0; i < 7; i++)
            res = res * 3 + co[i];
        return res;
    }

    public void setCornerOriCoord(int coord) {
        int sum = 0;
        for (int i = 6; i >= 0; i--) {
            co[i] = (byte) (coord % 3);
            sum += co[i];
            coord /= 3;
        }
        co[7] = (byte) ((3 - sum % 3) % 3);
    }

    // -------- Edge Orientation (0..2^11 - 1 = 2047) --------
    public int getEdgeOriCoord() {
        int res = 0;
        for (int i = 0; i < 11; i++)
            res = (res << 1) | eo[i];
        return res;
    }

    public void setEdgeOriCoord(int coord) {
        int sum = 0;
        for (int i = 10; i >= 0; i--) {
            eo[i] = (byte) (coord & 1);
            sum += eo[i];
            coord >>= 1;
        }
        eo[11] = (byte) ((sum % 2 == 0) ? 0 : 1);
    }

    // ------------------------------------------------------------
// Correct UD-Slice coordinate: rank of positions of FR,FL,BL,BR among 12 edges
// Produces range 0..494 (495 total combinations)
// ------------------------------------------------------------
    public int getUDSliceCoord() {
        int[] slice = new int[4];
        int idx = 0;

        // collect the positions (0..11) where ep[] is one of the slice edges
        for (int i = 0; i < 12; i++) {
            int e = ep[i];
            if (e == 8 || e == 9 || e == 10 || e == 11) {
                slice[idx++] = i;
            }
        }

        // rank the 4-element combination (lexicographic rank)
        int coord = 0;
        coord += nCr(slice[0], 1);
        coord += nCr(slice[1], 2);
        coord += nCr(slice[2], 3);
        coord += nCr(slice[3], 4);

        return coord;
    }

    private int nCr(int n, int r) {
        if (r > n) return 0;
        int res = 1;
        for (int i = 1; i <= r; i++) {
            res = res * (n - i + 1) / i;
        }
        return res;
    }


    // ------------------------------------------------------------
// Inverse of the above: reconstruct slice edges positions
// ------------------------------------------------------------
    public void setUDSliceCoord(int coord) {
        boolean[] used = new boolean[12];
        int[] slicePos = new int[4];

        // extract 4 combination positions
        for (int r = 4; r >= 1; r--) {
            int n = 11;
            while (nCr(n, r) > coord) n--;
            slicePos[4 - r] = n;
            coord -= nCr(n, r);
        }

        // set/swap edges
        int s = 8; // FR=8, FL=9, BL=10, BR=11
        Arrays.fill(ep, (byte) -1);

        for (int pos : slicePos) {
            ep[pos] = (byte) s++;
            used[pos] = true;
        }

        // fill remaining edges
        int e = 0;
        for (int i = 0; i < 12; i++) {
            if (ep[i] == -1) {
                while (e == 8 || e == 9 || e == 10 || e == 11) e++;
                ep[i] = (byte) e++;
            }
        }
    }
    private boolean posContains(int[] arr, int v) {
        for (int x : arr) if (x == v) return true;
        return false;
    }

    private boolean used(byte[] arr, int v) {
        for (byte x : arr) if (x == v) return true;
        return false;
    }

    // ---------------------------------------------------------
    // MULTIPLY CUBIECUBES (used for move-tables)
    // ---------------------------------------------------------
    public void multiply(CubieCube b, CubieCube out) {
        for (int i = 0; i < 8; i++) {
            out.cp[i] = cp[b.cp[i]];
            out.co[i] = (byte) ((co[b.cp[i]] + b.co[i]) % 3);
        }
        for (int i = 0; i < 12; i++) {
            out.ep[i] = ep[b.ep[i]];
            out.eo[i] = (byte) ((eo[b.ep[i]] + b.eo[i]) % 2);
        }
    }

    // ---------------------------------------------------------
    // INVERSE CUBIECUBE
    // ---------------------------------------------------------
    public void inverse(CubieCube out) {
        for (int i = 0; i < 8; i++) out.cp[cp[i]] = (byte) i;
        for (int i = 0; i < 12; i++) out.ep[ep[i]] = (byte) i;

        for (int i = 0; i < 8; i++)
            out.co[i] = (byte) ((3 - co[out.cp[i]]) % 3);

        for (int i = 0; i < 12; i++)
            out.eo[i] = eo[out.ep[i]];
    }

    // ---------------------------------------------------------
    // CUBIECUBE FROM FACELETS (used via FaceCube conversion)
    // ---------------------------------------------------------
    public static CubieCube fromFaceCube(FaceCube fc) {
        CubieCube cc = new CubieCube();
        for (int i = 0; i < 8; i++) {
            cc.cp[i] = fc.getCornerPerm(i);
            cc.co[i] = fc.getCornerOri(i);
        }
        for (int i = 0; i < 12; i++) {
            cc.ep[i] = fc.getEdgePerm(i);
            cc.eo[i] = fc.getEdgeOri(i);
        }
        return cc;
    }

    // ----- Corner Permutation Coord (0..40319) -----
    public int getCornerPermCoord() {
        int coord = 0;
        int[] used = new int[8];

        for (int i = 0; i < 8; i++) {
            int v = cp[i];
            int smaller = 0;
            for (int j = 0; j < v; j++)
                if (used[j] == 0) smaller++;
            coord = coord * (8 - i) + smaller;
            used[v] = 1;
        }
        return coord;
    }

    public void setCornerPermCoord(int coord) {
        int[] perm = new int[8];
        boolean[] used = new boolean[8];

        for (int i = 0; i < 8; i++) {
            int f = coord % (8 - i);
            coord /= (8 - i);

            int j = 0, c = 0;
            while (true) {
                if (!used[j]) {
                    if (c == f) break;
                    c++;
                }
                j++;
            }
            perm[i] = j;
            used[j] = true;
        }
        for (int i = 0; i < 8; i++) cp[i] = (byte) perm[i];
    }

    public static CubieCube fromCornerOriCoord(int coord) {
        CubieCube c = new CubieCube();
        c.setCornerOriCoord(coord);
        return c;
    }

    public static CubieCube fromEdgeOriCoord(int coord) {
        CubieCube c = new CubieCube();
        c.setEdgeOriCoord(coord);
        return c;
    }

    public static CubieCube fromUDSliceCoord(int coord) {
        CubieCube c = new CubieCube();

        int[] pos = new int[4];  // locations of FR FL BL BR
        int[] edges = {8, 9, 10, 11};

        for (int i = 3; i >= 0; i--) {
            int fact = 1;
            for (int j = 1; j <= i; j++) fact *= (12 - j);

            int v = coord / fact;
            coord = coord % fact;

            int count = -1;
            for (int j = 0; j < 12; j++) {
                if (c.ep[j] == j) {
                    count++;
                    if (count == v) {
                        pos[3 - i] = j;
                        c.ep[j] = (byte) edges[3 - i];
                        break;
                    }
                }
            }
        }

        int e = 0;
        for (int i = 0; i < 12; i++) {
            if (c.ep[i] == i) {
                while (e < 12 && (edges[0] == e || edges[1] == e || edges[2] == e || edges[3] == e))
                    e++;
                if (e < 12)
                    c.ep[i] = (byte) e++;
            }
        }
        return c;
    }

    public static CubieCube fromCornerPermCoord(int coord) {
        CubieCube c = new CubieCube();
        c.setCornerPermCoord(coord);
        return c;
    }

}