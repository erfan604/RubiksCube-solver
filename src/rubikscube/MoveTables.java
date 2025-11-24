package rubikscube;

/**
 * Precomputed move tables for coordinate transforms.
 */
public class MoveTables {

    public static int[][][] coMove;    // [move][power][coord]
    public static int[][][] eoMove;
    public static int[][][] sliceMove;
    public static int[][][] cpMove;
    public static int[][][] udEpMove;
    public static int[][][] uEdgeMove; // 4! = 24
    public static int[][][] dEdgeMove; // 4! = 24

    private static volatile boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;

        coMove = new int[6][4][PruningTables.N_CO];
        eoMove = new int[6][4][PruningTables.N_EO];
        sliceMove = new int[6][4][PruningTables.N_SLICE];
        cpMove = new int[6][4][PruningTables.N_CP];
        udEpMove = new int[6][4][PruningTables.N_UD_EP];
        uEdgeMove = new int[6][4][24];
        dEdgeMove = new int[6][4][24];

        for (int move = 0; move < 6; move++) {
            for (int p = 1; p <= 3; p++) {
                for (int c = 0; c < PruningTables.N_CO; c++) {
                    CubieCube cc = CubieCube.fromCornerOriCoord(c);
                    cc.applyMove(move, p);
                    coMove[move][p][c] = cc.getCornerOriCoord();
                }
                for (int e = 0; e < PruningTables.N_EO; e++) {
                    CubieCube cc = CubieCube.fromEdgeOriCoord(e);
                    cc.applyMove(move, p);
                    eoMove[move][p][e] = cc.getEdgeOriCoord();
                }
                for (int s = 0; s < PruningTables.N_SLICE; s++) {
                    CubieCube cc = CubieCube.fromUDSliceCoord(s);
                    cc.applyMove(move, p);
                    sliceMove[move][p][s] = cc.getUDSliceCoord();
                }
                for (int cp = 0; cp < PruningTables.N_CP; cp++) {
                    CubieCube cc = CubieCube.fromCornerPermCoord(cp);
                    cc.applyMove(move, p);
                    cpMove[move][p][cp] = cc.getCornerPermCoord();
                }
                for (int ud = 0; ud < PruningTables.N_UD_EP; ud++) {
                    CubieCube cc = CubieCube.fromUDEdgePermCoord(ud);
                    cc.applyMove(move, p);
                    udEpMove[move][p][ud] = cc.getUDEdgePermCoord();
                }
                for (int ue = 0; ue < 24; ue++) {
                    CubieCube cc = CubieCube.fromUEdgePermCoord(ue);
                    cc.applyMove(move, p);
                    uEdgeMove[move][p][ue] = cc.getUEdgePermCoord();
                }
                for (int de = 0; de < 24; de++) {
                    CubieCube cc = CubieCube.fromDEdgePermCoord(de);
                    cc.applyMove(move, p);
                    dEdgeMove[move][p][de] = cc.getDEdgePermCoord();
                }
            }
        }

        initialized = true;
    }

    public static boolean isInitialized() { return initialized; }

    public static int applyCO(int move, int power, int coord) {
        return coMove[move][power][coord];
    }
    public static int applyEO(int move, int power, int coord) {
        return eoMove[move][power][coord];
    }
    public static int applySlice(int move, int power, int coord) {
        return sliceMove[move][power][coord];
    }
    public static int applyCP(int move, int power, int coord) {
        return cpMove[move][power][coord];
    }
    public static int applyUDEP(int move, int power, int coord) {
        return udEpMove[move][power][coord];
    }
    public static int applyUEdge(int move, int power, int coord) {
        return uEdgeMove[move][power][coord];
    }
    public static int applyDEdge(int move, int power, int coord) {
        return dEdgeMove[move][power][coord];
    }
}
