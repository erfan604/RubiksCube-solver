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
    public static final int SLICE_SOLVED_COORD = new CubieCube().getUDSliceCoord();

    // state
    public byte[] cp = new byte[8];
    public byte[] co = new byte[8];
    public byte[] ep = new byte[12];
    public byte[] eo = new byte[12];

    public CubieCube() {
        for (byte i = 0; i < 8; i++) { cp[i] = i; co[i] = 0; }
        for (byte i = 0; i < 12; i++) { ep[i] = i; eo[i] = 0; }
    }

    public CubieCube(CubieCube c) {
        this.cp = Util.copyArray(c.cp);
        this.co = Util.copyArray(c.co);
        this.ep = Util.copyArray(c.ep);
        this.eo = Util.copyArray(c.eo);
    }

    // helpers
    private static void cycleCorner(byte[] arr, int a, int b, int c, int d) {
        byte tmp = arr[a]; arr[a] = arr[b]; arr[b] = arr[c]; arr[c] = arr[d]; arr[d] = tmp;
    }
    private static void cycleEdge(byte[] arr, int a, int b, int c, int d) {
        byte tmp = arr[a]; arr[a] = arr[b]; arr[b] = arr[c]; arr[c] = arr[d]; arr[d] = tmp;
    }
    private static void addCornerOri(byte[] co, int a, int b, int c, int d) {
        co[a] = (byte)((co[a] + 2) % 3);
        co[b] = (byte)((co[b] + 1) % 3);
        co[c] = (byte)((co[c] + 2) % 3);
        co[d] = (byte)((co[d] + 1) % 3);
    }

    // single clockwise quarter-turn (Singmaster)
    public void move(int m) {
        switch (m) {
            case Moves.U:
                cycleCorner(cp, URF, UBR, ULB, UFL);
                cycleCorner(co, URF, UBR, ULB, UFL);
                cycleEdge(ep, UR, UB, UL, UF);
                cycleEdge(eo, UR, UB, UL, UF);
                // orientations unchanged
                break;
            case Moves.R:
                cycleCorner(cp, URF, DFR, DRB, UBR);
                cycleCorner(co, URF, DFR, DRB, UBR);
                addCornerOri(co, URF, DFR, DRB, UBR);
                cycleEdge(ep, UR, FR, DR, BR);
                cycleEdge(eo, UR, FR, DR, BR);
                break;
            case Moves.F:
                cycleCorner(cp, UFL, DLF, DFR, URF);
                cycleCorner(co, UFL, DLF, DFR, URF);
                addCornerOri(co, UFL, DLF, DFR, URF);
                cycleEdge(ep, UF, FL, DF, FR);
                cycleEdge(eo, UF, FL, DF, FR);
                eo[UF] ^= 1; eo[FL] ^= 1; eo[DF] ^= 1; eo[FR] ^= 1;
                break;
            case Moves.D:
                cycleCorner(cp, DFR, DLF, DBL, DRB);
                cycleCorner(co, DFR, DLF, DBL, DRB);
                cycleEdge(ep, DF, DL, DB, DR);
                cycleEdge(eo, DF, DL, DB, DR);
                break;
            case Moves.L:
                cycleCorner(cp, ULB, DBL, DLF, UFL);
                cycleCorner(co, ULB, DBL, DLF, UFL);
                addCornerOri(co, ULB, DBL, DLF, UFL);
                cycleEdge(ep, UL, BL, DL, FL);
                cycleEdge(eo, UL, BL, DL, FL);
                break;
            case Moves.B:
                cycleCorner(cp, UBR, DRB, DBL, ULB);
                cycleCorner(co, UBR, DRB, DBL, ULB);
                addCornerOri(co, UBR, DRB, DBL, ULB);
                cycleEdge(ep, UB, BR, DB, BL);
                cycleEdge(eo, UB, BR, DB, BL);
                eo[UB] ^= 1; eo[BR] ^= 1; eo[DB] ^= 1; eo[BL] ^= 1;
                break;
        }
    }

    public void applyMove(int move, int power) {
        for (int i = 0; i < power; i++) move(move);
    }

    public boolean isSolved() {
        for (int i = 0; i < 8; i++) if (cp[i] != i || co[i] != 0) return false;
        for (int i = 0; i < 12; i++) if (ep[i] != i || eo[i] != 0) return false;
        return true;
    }

    // Corner orientation coord (3^7)
    public int getCornerOriCoord() {
        int res = 0;
        for (int i = 0; i < 7; i++) res = res * 3 + co[i];
        return res;
    }
    public void setCornerOriCoord(int coord) {
        int sum = 0;
        for (int i = 6; i >= 0; i--) {
            co[i] = (byte)(coord % 3);
            sum += co[i];
            coord /= 3;
        }
        co[7] = (byte)((3 - sum % 3) % 3);
    }

    // Edge orientation coord (2^11)
    public int getEdgeOriCoord() {
        int res = 0;
        for (int i = 0; i < 11; i++) res = (res << 1) | eo[i];
        return res;
    }
    public void setEdgeOriCoord(int coord) {
        int sum = 0;
        for (int i = 10; i >= 0; i--) {
            eo[i] = (byte)(coord & 1);
            sum += eo[i];
            coord >>= 1;
        }
        eo[11] = (byte)((sum % 2 == 0) ? 0 : 1);
    }

    // UD-slice coordinate (C(12,4)=495)
    private int nCr(int n, int r) {
        if (r > n) return 0;
        int res = 1;
        for (int i = 1; i <= r; i++) res = res * (n - i + 1) / i;
        return res;
    }

    public int getUDSliceCoord() {
        // combinatorial index of which 4 positions hold slice edges (FR,FL,BL,BR)
        int coord = 0;
        int r = 4;
        for (int i = 11; i >= 0 && r > 0; i--) {
            int e = ep[i];
            if (e == FR || e == FL || e == BL || e == BR) {
                coord += nCr(i, r);
                r--;
            }
        }
        return coord;
    }

    public void setUDSliceCoord(int coord) {
        Arrays.fill(ep, (byte)-1);
        int r = 4;
        int placed = 0;
        for (int i = 11; i >= 0 && r > 0; i--) {
            int comb = nCr(i, r);
            if (coord >= comb) {
                ep[i] = (byte)(FR + placed);
                placed++;
                coord -= comb;
                r--;
            }
        }
        int e = 0;
        for (int i = 0; i < 12; i++) {
            if (ep[i] == -1) {
                while (e == FR || e == FL || e == BL || e == BR) e++;
                ep[i] = (byte)e++;
            }
        }
    }

    // Corner permutation coord (Lehmer)
    public int getCornerPermCoord() {
        int coord = 0; int[] used = new int[8];
        for (int i = 0; i < 8; i++) {
            int v = cp[i]; int smaller = 0;
            for (int j = 0; j < v; j++) if (used[j] == 0) smaller++;
            coord = coord * (8 - i) + smaller;
            used[v] = 1;
        }
        return coord;
    }

    public void setCornerPermCoord(int coord) {
        int[] perm = new int[8]; boolean[] used = new boolean[8];
        int[] fact = new int[9]; fact[0]=1; for (int i=1;i<=8;i++) fact[i]=fact[i-1]*i;
        int rem = coord;
        for (int i = 0; i < 8; i++) {
            int div = fact[7 - i];
            int index = rem / div; rem = rem % div;
            int j = 0, cnt = 0;
            while (true) {
                if (!used[j]) {
                    if (cnt == index) break;
                    cnt++;
                }
                j++;
            }
            perm[i] = j; used[j] = true;
        }
        for (int i = 0; i < 8; i++) cp[i] = (byte)perm[i];
    }

    // U-layer edges (UR,UF,UL,UB) and D-layer edges (DR,DF,DL,DB)
    private static final int[] U_EDGE_IDX = {UR, UF, UL, UB};
    private static final int[] D_EDGE_IDX = {DR, DF, DL, DB};

    private int permCoord4(int[] perm) {
        int coord = 0;
        boolean[] used = new boolean[4];
        for (int i = 0; i < 4; i++) {
            int v = perm[i];
            int smaller = 0;
            for (int j = 0; j < v; j++) if (!used[j]) smaller++;
            coord = coord * (4 - i) + smaller;
            used[v] = true;
        }
        return coord;
    }

    private void setPerm4(int coord, int[] out) {
        boolean[] used = new boolean[4];
        int[] fact = {1,1,2,6,24};
        int rem = coord;
        for (int i = 0; i < 4; i++) {
            int div = fact[3 - i];
            int index = rem / div; rem %= div;
            int j = 0, cnt = 0;
            while (true) {
                if (!used[j]) {
                    if (cnt == index) break;
                    cnt++;
                }
                j++;
            }
            out[i] = j;
            used[j] = true;
        }
    }

    public int getUEdgePermCoord() {
        int[] perm = new int[4];
        // assume phase-2: U-layer edges occupy positions 0..3
        for (int i = 0; i < 4; i++) {
            int e = ep[i]; // piece at UR/UF/UL/UB
            if (e < 0 || e > 3) return 0; // invalid for phase-2
            perm[i] = e;
        }
        return permCoord4(perm);
    }

    public void setUEdgePermCoord(int coord) {
        byte[] solved = {UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR};
        System.arraycopy(solved, 0, ep, 0, ep.length);
        int[] perm = new int[4];
        setPerm4(coord, perm);
        for (int i = 0; i < 4; i++) ep[i] = (byte)perm[i];
    }

    public static CubieCube fromUEdgePermCoord(int coord) { CubieCube c = new CubieCube(); c.setUEdgePermCoord(coord); return c; }

    public int getDEdgePermCoord() {
        int[] perm = new int[4];
        // phase-2: D-layer edges occupy positions 4..7
        for (int i = 0; i < 4; i++) {
            int e = ep[4 + i];
            if (e < 4 || e > 7) return 0;
            perm[i] = e - 4;
        }
        return permCoord4(perm);
    }

    public void setDEdgePermCoord(int coord) {
        byte[] solved = {UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR};
        System.arraycopy(solved, 0, ep, 0, ep.length);
        int[] perm = new int[4];
        setPerm4(coord, perm);
        for (int i = 0; i < 4; i++) ep[i + 4] = (byte)(perm[i] + 4);
    }

    public static CubieCube fromDEdgePermCoord(int coord) { CubieCube c = new CubieCube(); c.setDEdgePermCoord(coord); return c; }

    // factories
    public static CubieCube fromCornerOriCoord(int coord) { CubieCube c = new CubieCube(); c.setCornerOriCoord(coord); return c; }
    public static CubieCube fromEdgeOriCoord(int coord) { CubieCube c = new CubieCube(); c.setEdgeOriCoord(coord); return c; }
    public static CubieCube fromUDSliceCoord(int coord) {
        CubieCube c = new CubieCube();
        c.setUDSliceCoord(coord);
        return c;
    }

    public static CubieCube fromCornerPermCoord(int coord) { CubieCube c = new CubieCube(); c.setCornerPermCoord(coord); return c; }

    // UD-edge permutation coordinate (8! for edges UR,UF,UL,UB,DR,DF,DL,DB assuming slice solved)
    public int getUDEdgePermCoord() {
        int[] perm = new int[8];
        int idx = 0;
        for (int i = 0; i < 12; i++) {
            int e = ep[i];
            if (e < 8) perm[idx++] = e;
        }
        if (idx != 8) return 0;

        int coord = 0; int[] used = new int[8];
        for (int i = 0; i < 8; i++) {
            int v = perm[i];
            int smaller = 0;
            for (int j = 0; j < v; j++) if (used[j] == 0) smaller++;
            coord = coord * (8 - i) + smaller;
            used[v] = 1;
        }
        return coord;
    }

    public void setUDEdgePermCoord(int coord) {
        int[] perm = new int[8]; boolean[] used = new boolean[8];
        int[] fact = new int[9]; fact[0]=1; for (int i=1;i<=8;i++) fact[i]=fact[i-1]*i;
        int rem = coord;
        for (int i = 0; i < 8; i++) {
            int div = fact[7 - i];
            int index = rem / div; rem = rem % div;
            int j = 0, cnt = 0;
            while (true) {
                if (!used[j]) {
                    if (cnt == index) break;
                    cnt++;
                }
                j++;
            }
            perm[i] = j; used[j] = true;
        }
        for (int i = 0; i < 8; i++) ep[i] = (byte)perm[i];
        ep[8]=FR; ep[9]=FL; ep[10]=BL; ep[11]=BR;
    }

    public static CubieCube fromUDEdgePermCoord(int coord) { CubieCube c = new CubieCube(); c.setUDEdgePermCoord(coord); return c; }

    // multiply and inverse
    public void multiply(CubieCube b, CubieCube out) {
        for (int i = 0; i < 8; i++) { out.cp[i] = cp[b.cp[i]]; out.co[i] = (byte)((co[b.cp[i]] + b.co[i]) % 3); }
        for (int i = 0; i < 12; i++) { out.ep[i] = ep[b.ep[i]]; out.eo[i] = (byte)((eo[b.ep[i]] + b.eo[i]) % 2); }
    }

    public void inverse(CubieCube out) {
        for (int i = 0; i < 8; i++) out.cp[cp[i]] = (byte)i;
        for (int i = 0; i < 12; i++) out.ep[ep[i]] = (byte)i;
        for (int i = 0; i < 8; i++) out.co[i] = (byte)((3 - co[out.cp[i]]) % 3);
        for (int i = 0; i < 12; i++) out.eo[i] = eo[out.ep[i]];
    }
}
