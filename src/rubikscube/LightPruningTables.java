package rubikscube;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Lightweight pruning tables built entirely at runtime.
 * Focuses on:
 *  - Phase 1: CO×slice and EO×slice distances.
 *  - Phase 2: CP, UD-edge, U-edge-only, D-edge-only distances with restricted moves.
 *
 * No disk cache; tables are small enough to rebuild per run (~sub-second on modern CPUs).
 */
public class LightPruningTables {

    public static final int N_CO = 2187;
    public static final int N_EO = 2048;
    public static final int N_SLICE = 495;
    public static final int N_CP = 40320;
    public static final int N_UD_EP = 40320;

    public static final byte[] coSlicePrun = new byte[N_CO * N_SLICE];
    public static final byte[] eoSlicePrun = new byte[N_EO * N_SLICE];
    public static final byte[] cpPrunP2 = new byte[N_CP];
    public static final byte[] udPrunP2 = new byte[N_UD_EP];
    public static final byte[] uEdgePrun = new byte[24];
    public static final byte[] dEdgePrun = new byte[24];

    private static volatile boolean initialized = false;

    public static synchronized void buildAllBlocking() {
        if (initialized) return;
        MoveTables.init();
        buildCoSlice();
        buildEoSlice();
        buildCpP2();
        buildUdP2();
        buildUEdge();
        buildDEdge();
        initialized = true;
    }

    public static boolean isInitialized() { return initialized; }

    private static int coSliceIdx(int co, int sl) { return co * N_SLICE + sl; }
    private static int eoSliceIdx(int eo, int sl) { return eo * N_SLICE + sl; }

    // ---------- Phase-1 tables ----------
    private static void buildCoSlice() {
        Arrays.fill(coSlicePrun, (byte)-1);
        int startIdx = coSliceIdx(0, CubieCube.SLICE_SOLVED_COORD);
        coSlicePrun[startIdx] = 0;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{0, CubieCube.SLICE_SOLVED_COORD});
        int depth = 0;
        while (!q.isEmpty()) {
            ArrayDeque<int[]> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int[] cur = q.remove();
                int co = cur[0], sl = cur[1];
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        int nco = MoveTables.applyCO(m, p, co);
                        int nsl = MoveTables.applySlice(m, p, sl);
                        int idx = coSliceIdx(nco, nsl);
                        if (coSlicePrun[idx] == -1) {
                            coSlicePrun[idx] = (byte)(depth + 1);
                            next.add(new int[]{nco, nsl});
                        }
                    }
                }
            }
            q = next;
            depth++;
        }
    }

    private static void buildEoSlice() {
        Arrays.fill(eoSlicePrun, (byte)-1);
        int startIdx = eoSliceIdx(0, CubieCube.SLICE_SOLVED_COORD);
        eoSlicePrun[startIdx] = 0;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{0, CubieCube.SLICE_SOLVED_COORD});
        int depth = 0;
        while (!q.isEmpty()) {
            ArrayDeque<int[]> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int[] cur = q.remove();
                int eo = cur[0], sl = cur[1];
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        int neo = MoveTables.applyEO(m, p, eo);
                        int nsl = MoveTables.applySlice(m, p, sl);
                        int idx = eoSliceIdx(neo, nsl);
                        if (eoSlicePrun[idx] == -1) {
                            eoSlicePrun[idx] = (byte)(depth + 1);
                            next.add(new int[]{neo, nsl});
                        }
                    }
                }
            }
            q = next;
            depth++;
        }
    }

    // ---------- Phase-2 tables (restricted move set: U/D any, others half-turn only) ----------
    private static void buildCpP2() {
        Arrays.fill(cpPrunP2, (byte)-1);
        cpPrunP2[0] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(0);
        int depth = 0;
        while (!q.isEmpty()) {
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int cp = q.remove();
                for (int m = 0; m < 6; m++) {
                    boolean isUD = (m == Moves.U || m == Moves.D);
                    for (int p = 1; p <= 3; p++) {
                        if (!isUD && p != 2) continue;
                        int ncp = MoveTables.applyCP(m, p, cp);
                        if (cpPrunP2[ncp] == -1) {
                            cpPrunP2[ncp] = (byte)(depth + 1);
                            next.add(ncp);
                        }
                    }
                }
            }
            q = next;
            depth++;
        }
    }

    private static void buildUdP2() {
        Arrays.fill(udPrunP2, (byte)-1);
        udPrunP2[0] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(0);
        int depth = 0;
        while (!q.isEmpty()) {
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int ud = q.remove();
                for (int m = 0; m < 6; m++) {
                    boolean isUD = (m == Moves.U || m == Moves.D);
                    for (int p = 1; p <= 3; p++) {
                        if (!isUD && p != 2) continue;
                        int nud = MoveTables.applyUDEP(m, p, ud);
                        if (udPrunP2[nud] == -1) {
                            udPrunP2[nud] = (byte)(depth + 1);
                            next.add(nud);
                        }
                    }
                }
            }
            q = next;
            depth++;
        }
    }

    private static void buildUEdge() {
        Arrays.fill(uEdgePrun, (byte)-1);
        uEdgePrun[0] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(0);
        int depth = 0;
        while (!q.isEmpty()) {
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int ue = q.remove();
                for (int m = 0; m < 6; m++) {
                    boolean isUD = (m == Moves.U || m == Moves.D);
                    for (int p = 1; p <= 3; p++) {
                        if (!isUD && p != 2) continue;
                        int nue = MoveTables.applyUEdge(m, p, ue);
                        if (uEdgePrun[nue] == -1) {
                            uEdgePrun[nue] = (byte)(depth + 1);
                            next.add(nue);
                        }
                    }
                }
            }
            q = next;
            depth++;
        }
    }

    private static void buildDEdge() {
        Arrays.fill(dEdgePrun, (byte)-1);
        dEdgePrun[0] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(0);
        int depth = 0;
        while (!q.isEmpty()) {
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int de = q.remove();
                for (int m = 0; m < 6; m++) {
                    boolean isUD = (m == Moves.U || m == Moves.D);
                    for (int p = 1; p <= 3; p++) {
                        if (!isUD && p != 2) continue;
                        int nde = MoveTables.applyDEdge(m, p, de);
                        if (dEdgePrun[nde] == -1) {
                            dEdgePrun[nde] = (byte)(depth + 1);
                            next.add(nde);
                        }
                    }
                }
            }
            q = next;
            depth++;
        }
    }
}
