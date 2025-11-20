package rubikscube;

public class PruningTables {

    public static final int N_CO = 2187;   // 3^7
    public static final int N_EO = 2048;   // 2^11
    public static final int N_SLICE = 495; // C(12,4)

    public static byte[] coPrun = new byte[N_CO];
    public static byte[] eoPrun = new byte[N_EO];
    public static byte[] slicePrun = new byte[N_SLICE];

    public static boolean initialized = false;

    // -------------------------------------------------------
    // Initialization entry point
    // -------------------------------------------------------
    public static void init() {
        if (initialized) return;
        initCOPrun();
        initEOPrun();
        initSlicePrun();
        initialized = true;
    }

    // -------------------------------------------------------
    // Corner orientation pruning table
    // -------------------------------------------------------
    private static void initCOPrun() {
        for (int i = 0; i < N_CO; i++) coPrun[i] = -1;

        CubieCube c = new CubieCube();
        coPrun[c.getCornerOriCoord()] = 0;

        int done = 1;
        int depth = 0;

        while (done < N_CO) {
            depth++;
            for (int i = 0; i < N_CO; i++) {
                if (coPrun[i] == depth - 1) {
                    c.setCornerOriCoord(i);
                    for (int m = 0; m < 6; m++) {
                        for (int p = 1; p <= 3; p++) {
                            CubieCube d = new CubieCube(c);
                            d.applyMove(m, p);
                            int idx = d.getCornerOriCoord();
                            if (coPrun[idx] == -1) {
                                coPrun[idx] = (byte) depth;
                                done++;
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------
    // Edge orientation pruning table
    // -------------------------------------------------------
    private static void initEOPrun() {
        for (int i = 0; i < N_EO; i++) eoPrun[i] = -1;

        CubieCube c = new CubieCube();
        eoPrun[c.getEdgeOriCoord()] = 0;

        int done = 1;
        int depth = 0;

        while (done < N_EO) {
            depth++;
            for (int i = 0; i < N_EO; i++) {
                if (eoPrun[i] == depth - 1) {
                    c.setEdgeOriCoord(i);
                    for (int m = 0; m < 6; m++) {
                        for (int p = 1; p <= 3; p++) {
                            CubieCube d = new CubieCube(c);
                            d.applyMove(m, p);
                            int idx = d.getEdgeOriCoord();
                            if (eoPrun[idx] == -1) {
                                eoPrun[idx] = (byte) depth;
                                done++;
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------
    // Slice coordinate pruning table
    // -------------------------------------------------------
    private static void initSlicePrun() {
        for (int i = 0; i < N_SLICE; i++) slicePrun[i] = -1;

        CubieCube c = new CubieCube();
        slicePrun[c.getUDSliceCoord()] = 0;

        int done = 1;
        int depth = 0;

        while (done < N_SLICE) {
            depth++;
            for (int i = 0; i < N_SLICE; i++) {
                if (slicePrun[i] == depth - 1) {
                    c.setUDSliceCoord(i);
                    for (int m = 0; m < 6; m++) {
                        for (int p = 1; p <= 3; p++) {
                            CubieCube d = new CubieCube(c);
                            d.applyMove(m, p);
                            int idx = d.getUDSliceCoord();
                            if (slicePrun[idx] == -1) {
                                slicePrun[idx] = (byte) depth;
                                done++;
                            }
                        }
                    }
                }
            }
        }
    }
}
