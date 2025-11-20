package rubikscube;

public class NetToCubie {

    // ---------------------------------------------------------
    // HARD-CODED CENTER COLORS (Correct for assignment cube)
    // ---------------------------------------------------------
    static final char Uc = 'O';  // Up
    static final char Dc = 'R';  // Down
    static final char Lc = 'G';  // Left
    static final char Fc = 'W';  // Front
    static final char Rc = 'B';  // Right
    static final char Bc = 'Y';  // Back

// True CORNER COLORS based on indices of solved cube
    private static final char[][] CORNER_COLORS = {
            { 'O','B','W' }, // 0 URF
            { 'O','W','G' }, // 1 UFL
            { 'O','G','Y' }, // 2 ULB
            { 'O','Y','B' }, // 3 UBR
            { 'R','W','B' }, // 4 DFR
            { 'R','G','W' }, // 5 DLF
            { 'R','Y','G' }, // 6 DBL
            { 'R','B','Y' }  // 7 DRB
    };

    // ---------------------------------------------------------
    // EDGE COLOR SCHEME
    // ---------------------------------------------------------
    private static final char[][] EDGE_COLORS = {
            { 'O', 'B' }, // UR
            { 'O', 'W' }, // UF
            { 'O', 'G' }, // UL
            { 'O', 'Y' }, // UB

            { 'R', 'B' }, // DR
            { 'R', 'W' }, // DF
            { 'R', 'G' },  // DL
            { 'R', 'Y' }, // DB

            { 'W', 'B' }, // FR
            { 'W', 'G' }, // FL
            { 'Y', 'G' }, // BL
            { 'Y', 'B' }, // BR


    };

    // ---------------------------------------------------------
    // FACELET MAPPING (must match your parser output)
    // ---------------------------------------------------------
    private static final int[][] CORNER_FACELETS = {
            { 8, 27, 20 }, // URF
            { 6, 18, 11 }, // UFL
            { 0,  9, 38 }, // ULB
            { 2, 36, 29 }, // UBR
            { 47, 26, 33 },// DFR
            { 45, 17, 24 },// DLF
            { 51, 44, 15 },// DBL
            { 53, 35, 42 } // DRB
    };


    private static final int[][] EDGE_FACELETS = {
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

    // ---------------------------------------------------------
    // PUBLIC ENTRY
    // ---------------------------------------------------------
    public static CubieCube fromFacelets(char[] f) {
        CubieCube cc = new CubieCube();

        System.out.println("========== CENTER COLORS ==========");
        System.out.println("U=" + Uc + "  L=" + Lc + "  F=" + Fc +
                "  R=" + Rc + "  B=" + Bc + "  D=" + Dc);
        System.out.println();


        assignCorners(f, cc);
        assignEdges(f, cc);


        System.out.println("\n========== FINAL CUBIECUBE ==========");

        System.out.print("cp: ");
        for (int i = 0; i < 8; i++) System.out.print(cc.cp[i] + " ");
        System.out.println();

        System.out.print("co: ");
        for (int i = 0; i < 8; i++) System.out.print(cc.co[i] + " ");
        System.out.println();

        System.out.print("ep: ");
        for (int i = 0; i < 12; i++) System.out.print(cc.ep[i] + " ");
        System.out.println();

        System.out.print("eo: ");
        for (int i = 0; i < 12; i++) System.out.print(cc.eo[i] + " ");
        System.out.println();
        System.out.println("====================================\n");

        return cc;
    }

    // ---------------------------------------------------------
    // CORNER MATCHING
    // ---------------------------------------------------------
    private static void assignCorners(char[] f, CubieCube cc) {
        for (int i = 0; i < 8; i++) {

            char a = f[ CORNER_FACELETS[i][0] ];
            char b = f[ CORNER_FACELETS[i][1] ];
            char c = f[ CORNER_FACELETS[i][2] ];

            // Build triple
            char[] triple = {a, b, c};

            // Find which cubie
            int cubie = findCornerCubie(triple);

            // DEBUG PRINT
            System.out.println("Corner " + c + " colors=" +
                    "[" + triple[0] + ", " + triple[1] + ", " + triple[2] + "]" +
                    "  → cubie=" + cubie);
            // Find orientation
            int ori = cornerOrientation(triple, CORNER_COLORS[cubie]);

            // DEBUG PRINT orientation
            System.out.println("    orientation=" + ori);

            cc.cp[i] = (byte) cubie;
            cc.co[i] = (byte) ori;
        }
    }

    private static int findCornerCubie(char[] triple) {
        for (int i = 0; i < 8; i++) {
            char[] cc = CORNER_COLORS[i];
            boolean m =
                    contains(triple, cc[0]) &&
                            contains(triple, cc[1]) &&
                            contains(triple, cc[2]);
            if (m) return i;
        }
        return 0; // never happens for valid cube
    }

    private static int cornerOrientation(char[] triple, char[] colors) {
        // U or D center determines orientation index 0
        char ref = colors[0]; // U or D color of this cubie set

        if (triple[0] == ref) return 0;
        if (triple[1] == ref) return 1;
        return 2;
    }

    // ---------------------------------------------------------
    // EDGE MATCHING
    // ---------------------------------------------------------
    private static void assignEdges(char[] f, CubieCube cc) {
        for (int i = 0; i < 12; i++) {

            char a = f[ EDGE_FACELETS[i][0] ];
            char b = f[ EDGE_FACELETS[i][1] ];

            int cubie = findEdgeCubie(a, b);

            // DEBUG PRINT
            System.out.println("Edge "  + " colors=[" + a + ", " + b + "] → cubie=" + cubie);

            int ori   = edgeOrientation(a, b, cubie);

            // DEBUG PRINT orientation
            System.out.println("    orientation=" + ori);

            cc.ep[i] = (byte) cubie;
            cc.eo[i] = (byte) ori;
        }
    }

    private static int findEdgeCubie(char a, char b) {
        for (int i = 0; i < 12; i++) {
            char x = EDGE_COLORS[i][0];
            char y = EDGE_COLORS[i][1];
            if ((a == x && b == y) || (a == y && b == x)) return i;
        }
        return 0; // never for valid cube
    }

    private static int edgeOrientation(char a, char b, int cubie) {
        char c0 = EDGE_COLORS[cubie][0];

        // If sticker a matches first color → ori = 0
        return (a == c0 ? 0 : 1);
    }

    // ---------------------------------------------------------
    // UTILS
    // ---------------------------------------------------------
    private static boolean contains(char[] arr, char target) {
        for (char c : arr) if (c == target) return true;
        return false;
    }
}
