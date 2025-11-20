package rubikscube;

import java.util.*;

public class Tables {

    public static int[] coPrun = new int[2187];
    public static int[] eoPrun = new int[2048];
    public static int[] cpPrun = new int[40320];
    public static int[] slicePrun = new int[495];

    private static boolean built = false;

    public static void init() {
        if (built) return;
        built = true;

        Arrays.fill(coPrun, -1);
        Arrays.fill(eoPrun, -1);
        Arrays.fill(cpPrun, -1);
        Arrays.fill(slicePrun, -1);

        // BFS init for each pruning table
        bfsCornerOri();
        bfsEdgeOri();
        bfsCornerPerm();
        bfsSlice();
    }

    private static void bfsCornerOri() {
        Queue<Integer> q = new ArrayDeque<>();
        coPrun[0] = 0;
        q.add(0);

        while (!q.isEmpty()) {
            int x = q.remove();
            int d = coPrun[x];
            for (int m = 0; m < 6; m++) {
                for (int p = 1; p <= 3; p++) {
                    CubieCube c = CubieCube.fromCornerOriCoord(x);
                    c.applyMove(m, p);
                    int y = c.getCornerOriCoord();
                    if (coPrun[y] == -1) {
                        coPrun[y] = d + 1;
                        q.add(y);
                    }
                }
            }
        }
    }

    private static void bfsEdgeOri() {
        Queue<Integer> q = new ArrayDeque<>();
        eoPrun[0] = 0;
        q.add(0);

        while (!q.isEmpty()) {
            int x = q.remove();
            int d = eoPrun[x];
            for (int m = 0; m < 6; m++) {
                for (int p = 1; p <= 3; p++) {
                    CubieCube c = CubieCube.fromEdgeOriCoord(x);
                    c.applyMove(m, p);
                    int y = c.getEdgeOriCoord();
                    if (eoPrun[y] == -1) {
                        eoPrun[y] = d + 1;
                        q.add(y);
                    }
                }
            }
        }
    }

    private static void bfsCornerPerm() {
        Queue<Integer> q = new ArrayDeque<>();
        cpPrun[0] = 0;
        q.add(0);

        while (!q.isEmpty()) {
            int x = q.remove();
            int d = cpPrun[x];
            for (int m = 0; m < 6; m++) {
                for (int p = 1; p <= 3; p++) {
                    CubieCube c = CubieCube.fromCornerPermCoord(x);
                    c.applyMove(m, p);
                    int y = c.getCornerPermCoord();
                    if (cpPrun[y] == -1) {
                        cpPrun[y] = d + 1;
                        q.add(y);
                    }
                }
            }
        }
    }

    private static void bfsSlice() {
        Queue<Integer> q = new ArrayDeque<>();
        slicePrun[0] = 0;
        q.add(0);

        while (!q.isEmpty()) {
            int x = q.remove();
            int d = slicePrun[x];
            for (int m = 0; m < 6; m++) {
                for (int p = 1; p <= 3; p++) {
                    CubieCube c = CubieCube.fromUDSliceCoord(x);
                    c.applyMove(m, p);
                    int y = c.getUDSliceCoord();
                    if (slicePrun[y] == -1) {
                        slicePrun[y] = d + 1;
                        q.add(y);
                    }
                }
            }
        }
    }
}
