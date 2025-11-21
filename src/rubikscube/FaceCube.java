package rubikscube;

public class FaceCube {

    // Assignment color scheme:
    // U = O, R = B, F = W, D = R, L = G, B = Y
    private char Uc, Rc, Fc, Dc, Lc, Bc;

    public char[] f = new char[54];

    // -------------------------------------------------
    // Constructor: save the 54 facelets and detect centers
    // -------------------------------------------------
    public FaceCube(char[] facelets) {
        System.arraycopy(facelets, 0, f, 0, 54);

        Uc = f[4];    // U center
        Rc = f[31];   // R center
        Fc = f[22];   // F center
        Dc = f[49];   // D center
        Lc = f[13];   // L center
        Bc = f[40];   // B center
    }

    // -------------------------------------------------
    // CORNER facelet positions (layout matching Solver.parseNet)
    // -------------------------------------------------
    private static final int[][] CORNER_FACELET = {
            { 8, 27, 20 }, // URF
            { 6, 18, 11 }, // UFL
            { 0,  9, 38 }, // ULB
            { 2, 36, 29 }, // UBR
            { 47, 26, 33 },// DFR
            { 45, 17, 24 },// DLF
            { 51, 44, 15 },// DBL
            { 53, 35, 42 } // DRB
    };

    // -------------------------------------------------
    // EDGE facelet positions (layout matching Solver.parseNet)
    // -------------------------------------------------
    private static final int[][] EDGE_FACELET = {
            {5, 28},   // 0 UR
            {7, 19},   // 1 UF
            {3, 10},   // 2 UL
            {1, 37},   // 3 UB

            {50, 34},  // 4 DR
            {46, 25},  // 5 DF
            {48, 16},  // 6 DL
            {52, 43},  // 7 DB

            {23, 30},  // 8 FR
            {21, 14},  // 9 FL
            {41, 12},  //10 BL
            {39, 32}   //11 BR
    };

    // dynamic color accessors to ensure centers are already captured
    private char[][] cornerColors() {
        return new char[][] {
                {Uc, Rc, Fc},  // URF
                {Uc, Fc, Lc},  // UFL
                {Uc, Lc, Bc},  // ULB
                {Uc, Bc, Rc},  // UBR
                {Dc, Fc, Rc},  // DFR
                {Dc, Lc, Fc},  // DLF
                {Dc, Bc, Lc},  // DBL
                {Dc, Rc, Bc}   // DRB
        };
    }

    private char[][] edgeColors() {
        return new char[][] {
                {Uc, Rc},   // UR
                {Uc, Fc},   // UF
                {Uc, Lc},   // UL
                {Uc, Bc},   // UB
                {Dc, Rc},   // DR
                {Dc, Fc},   // DF
                {Dc, Lc},   // DL
                {Dc, Bc},   // DB
                {Fc, Rc},   // FR
                {Fc, Lc},   // FL
                {Bc, Lc},   // BL
                {Bc, Rc}    // BR
        };
    }

    // -------------------------------------------------
    // CORNER PERMUTATION + ORIENTATION
    // -------------------------------------------------
    public byte getCornerPerm(int c) {
        char a = f[CORNER_FACELET[c][0]];
        char b = f[CORNER_FACELET[c][1]];
        char d = f[CORNER_FACELET[c][2]];
        return findCorner(a, b, d);
    }

    public byte getCornerOri(int c) {
        char a = f[CORNER_FACELET[c][0]];
        char b = f[CORNER_FACELET[c][1]];
        char d = f[CORNER_FACELET[c][2]];

        byte corner = findCorner(a, b, d);

        if (corner == -1) return 0;

        char[] cols = {a, b, d};

        for (byte ori = 0; ori < 3; ori++)
            if (cols[ori] == Uc || cols[ori] == Dc)
                return ori;

        return 0;
    }

    private byte findCorner(char a, char b, char c) {
        char[][] ccArr = cornerColors();
        for (byte i = 0; i < 8; i++) {
            char[] CC = ccArr[i];
            if (match3(a, b, c, CC[0], CC[1], CC[2])) return i;
        }
        return -1;
    }

    private boolean match3(char a, char b, char c, char x, char y, char z) {
        return (a==x && b==y && c==z)
                || (a==y && b==z && c==x)
                || (a==z && b==x && c==y);
    }

    // -------------------------------------------------
    // EDGE PERMUTATION + ORIENTATION
    // -------------------------------------------------
    public byte getEdgePerm(int e) {
        char a = f[EDGE_FACELET[e][0]];
        char b = f[EDGE_FACELET[e][1]];
        return findEdge(a, b);
    }

    public byte getEdgeOri(int e) {
        char a = f[EDGE_FACELET[e][0]];
        char b = f[EDGE_FACELET[e][1]];
        byte edge = findEdge(a, b);

        if (edge == -1) return 0;

        // Orientation rules (Kociemba style):
        //  - U/D layer edges are "good" if their U/D sticker is on U/D.
        //  - Slice edges are "good" if their F/B sticker is on F/B.
        if (edge >= 8 && edge <= 11) {
            return (byte) ((a == Fc || a == Bc) ? 0 : 1);
        }
        return (byte) ((a == Uc || a == Dc) ? 0 : 1);
    }

    private byte findEdge(char a, char b) {
        char[][] edgeColor = edgeColors();
        for (byte i = 0; i < 12; i++) {
            char x = edgeColor[i][0];
            char y = edgeColor[i][1];
            if ((a==x && b==y) || (a==y && b==x)) return i;
        }
        return -1;
    }
}
