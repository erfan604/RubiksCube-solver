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
    // CORNER facelet positions (Kociemba index layout)
    // -------------------------------------------------
    private static final int[][] CORNER_FACELET = {
            {0,  9, 38},  // URF
            {2, 36, 11},  // UFL
            {6, 18,  8},  // ULB
            {4, 20, 27},  // UBR
            {29, 26, 15}, // DFR
            {33, 13, 24}, // DLF
            {35, 47, 17}, // DBL
            {31, 44, 42}  // DRB
    };

    // -------------------------------------------------
    // EDGE facelet positions
    // -------------------------------------------------
    private static final int[][] EDGE_FACELET = {
            {1,  10}, // UR
            {3,  37}, // UF
            {7,  19}, // UL
            {5,  21}, // UB
            {28, 12}, // DR
            {30, 25}, // DF
            {32, 14}, // DL
            {34, 23}, // DB
            {16, 39}, // FR
            {22, 41}, // FL
            {46, 45}, // BL
            {52, 43}  // BR
    };

    // -------------------------------------------------
    // CORNER COLOR DEFINITION (using assignment colors!)
    // Order = URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB
    // -------------------------------------------------
    private char[][] cornerColor = {
            {Uc, Rc, Fc},  // URF
            {Uc, Fc, Lc},  // UFL
            {Uc, Lc, Bc},  // ULB
            {Uc, Bc, Rc},  // UBR
            {Dc, Fc, Rc},  // DFR
            {Dc, Lc, Fc},  // DLF
            {Dc, Bc, Lc},  // DBL
            {Dc, Rc, Bc}   // DRB
    };

    // -------------------------------------------------
    // EDGE COLOR DEFINITION (assignment colors)
    // Order = UR,UF,UL,UB,DR,DF,DL,DB,FR,FL,BL,BR
    // -------------------------------------------------
    private char[][] edgeColor = {
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
        for (byte i = 0; i < 8; i++) {
            char[] CC = cornerColor[i];
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

        // Orientation rules (same as Kociemba style)
        if (edge >= 8 && edge <= 11) {
            return (byte) ((a == Fc || a == Lc) ? 1 : 0);
        }

        return (byte) ((a == Uc || a == Dc) ? 0 : 1);
    }

    private byte findEdge(char a, char b) {
        for (byte i = 0; i < 12; i++) {
            char x = edgeColor[i][0];
            char y = edgeColor[i][1];
            if ((a==x && b==y) || (a==y && b==x)) return i;
        }
        return -1;
    }
}
