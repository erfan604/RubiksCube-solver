package rubikscube;

import java.util.*;
import java.nio.file.*;
import java.io.*;

public class PruningTables {

    public static final int N_CO = 2187;   // 3^7
    public static final int N_EO = 2048;   // 2^11
    public static final int N_SLICE = 495; // C(12,4)
    public static final int N_CP = 40320;  // 8!
    public static final int N_UD_EP = 40320; // 8! for UD edges

    public static final byte[] coPrun = new byte[N_CO];
    public static final byte[] eoPrun = new byte[N_EO];
    public static final byte[] slicePrun = new byte[N_SLICE];
    public static final byte[] cpPrun = new byte[N_CP];
    public static final byte[] udEpPrun = new byte[N_UD_EP];

    // true once all tables are fully built
    public static volatile boolean initialized = false;

    // true once async builder has been started
    private static volatile boolean started = false;

    private static final String CACHE_FILE = "pbt.cache";
    // bump magic/version because move definitions changed
    private static final byte[] MAGIC = new byte[] { 'P', 'B', 'T', 4 };

    // start the asynchronous incremental builder (non-blocking)
    public static synchronized void initAsyncStart() {
        if (started) return;
        started = true;

        // mark arrays as uninitialized
        Arrays.fill(coPrun, (byte)-1);
        Arrays.fill(eoPrun, (byte)-1);
        Arrays.fill(slicePrun, (byte)-1);
        Arrays.fill(cpPrun, (byte)-1);

        // set identity positions
        coPrun[0] = 0;
        eoPrun[0] = 0;
        slicePrun[0] = 0;
        cpPrun[0] = 0;
        udEpPrun[0] = 0;

        // build synchronously to ensure tables match current move defs
        prioritizedBuildLoop();
        initialized = true;
    }

    // public blocking full build
    public static synchronized void buildAllBlocking() {
        if (initialized) return;
        if (started) {
            // someone started async; wait a little for it to finish
            long t0 = System.currentTimeMillis();
            while (!initialized && System.currentTimeMillis() - t0 < 120_000) {
                try { Thread.sleep(200); } catch (InterruptedException e) { break; }
            }
            if (initialized) return;
        }
        started = true;

        Arrays.fill(coPrun, (byte)-1);
        Arrays.fill(eoPrun, (byte)-1);
        Arrays.fill(slicePrun, (byte)-1);
        Arrays.fill(cpPrun, (byte)-1);

        coPrun[0] = 0;
        eoPrun[0] = 0;
        slicePrun[0] = 0;
        cpPrun[0] = 0;
        udEpPrun[0] = 0;

        prioritizedBuildLoop();
        initialized = true;
    }

    // save current arrays to disk
    public static synchronized void saveToDisk() {
        try (OutputStream os = Files.newOutputStream(Paths.get(CACHE_FILE)); DataOutputStream dos = new DataOutputStream(os)) {
            dos.write(MAGIC);
            dos.writeInt(N_CO);
            dos.writeInt(N_EO);
            dos.writeInt(N_SLICE);
            dos.writeInt(N_CP);
            dos.writeInt(N_UD_EP);
            dos.write(coPrun);
            dos.write(eoPrun);
            dos.write(slicePrun);
            dos.write(cpPrun);
            dos.write(udEpPrun);
            dos.flush();
        } catch (IOException e) {
            System.err.println("Failed to save PBT cache: " + e.getMessage());
        }
    }

    // try load from disk; returns true on success
    public static synchronized boolean loadFromDisk() {
        Path p = Paths.get(CACHE_FILE);
        if (!Files.exists(p)) return false;
        try (InputStream is = Files.newInputStream(p); DataInputStream dis = new DataInputStream(is)) {
            byte[] magic = new byte[MAGIC.length];
            dis.readFully(magic);
            if (!Arrays.equals(magic, MAGIC)) return false;
            int nco = dis.readInt();
            int neo = dis.readInt();
            int nsl = dis.readInt();
            int ncp = dis.readInt();
            int nud = dis.readInt();
            if (nco != N_CO || neo != N_EO || nsl != N_SLICE || ncp != N_CP || nud != N_UD_EP) return false;
            dis.readFully(coPrun);
            dis.readFully(eoPrun);
            dis.readFully(slicePrun);
            dis.readFully(cpPrun);
            dis.readFully(udEpPrun);
            initialized = true;
            started = true;
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load PBT cache: " + e.getMessage());
            return false;
        }
    }

    // build CO then EO then SLICE then CP sequentially but in an incremental manner
    private static void prioritizedBuildLoop() {
        buildCO();
        buildEO();
        buildSlice();
        buildCP();
        buildUDEP();
        initialized = true;
    }

    private static void buildCO() {
        ArrayDeque<Integer> front = new ArrayDeque<>();
        front.add(0);
        int depth = 0;
        while (!front.isEmpty()) {
            depth++;
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!front.isEmpty()) {
                int x = front.remove();
                CubieCube c = CubieCube.fromCornerOriCoord(x);
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        CubieCube d = new CubieCube(c);
                        d.applyMove(m, p);
                        int y = d.getCornerOriCoord();
                        if (coPrun[y] == -1) {
                            coPrun[y] = (byte) depth;
                            next.add(y);
                        }
                    }
                }
            }
            front = next;
            // yield briefly to allow solver threads to make progress
            try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
    }

    private static void buildEO() {
        ArrayDeque<Integer> front = new ArrayDeque<>();
        front.add(0);
        int depth = 0;
        while (!front.isEmpty()) {
            depth++;
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!front.isEmpty()) {
                int x = front.remove();
                CubieCube c = CubieCube.fromEdgeOriCoord(x);
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        CubieCube d = new CubieCube(c);
                        d.applyMove(m, p);
                        int y = d.getEdgeOriCoord();
                        if (eoPrun[y] == -1) {
                            eoPrun[y] = (byte) depth;
                            next.add(y);
                        }
                    }
                }
            }
            front = next;
            try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
    }

    private static void buildSlice() {
        ArrayDeque<Integer> front = new ArrayDeque<>();
        front.add(0);
        int depth = 0;
        while (!front.isEmpty()) {
            depth++;
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!front.isEmpty()) {
                int x = front.remove();
                CubieCube c = CubieCube.fromUDSliceCoord(x);
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        CubieCube d = new CubieCube(c);
                        d.applyMove(m, p);
                        int y = d.getUDSliceCoord();
                        if (slicePrun[y] == -1) {
                            slicePrun[y] = (byte) depth;
                            next.add(y);
                        }
                    }
                }
            }
            front = next;
            try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
    }

    private static void buildCP() {
        ArrayDeque<Integer> front = new ArrayDeque<>();
        front.add(0);
        int depth = 0;
        while (!front.isEmpty()) {
            depth++;
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!front.isEmpty()) {
                int x = front.remove();
                CubieCube c = CubieCube.fromCornerPermCoord(x);
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        CubieCube d = new CubieCube(c);
                        d.applyMove(m, p);
                        int y = d.getCornerPermCoord();
                        if (cpPrun[y] == -1) {
                            cpPrun[y] = (byte) depth;
                            next.add(y);
                        }
                    }
                }
            }
            front = next;
            try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
    }

    private static void buildUDEP() {
        ArrayDeque<Integer> front = new ArrayDeque<>();
        front.add(0);
        int depth = 0;
        while (!front.isEmpty()) {
            depth++;
            ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!front.isEmpty()) {
                int x = front.remove();
                CubieCube c = CubieCube.fromUDEdgePermCoord(x);
                for (int m = 0; m < 6; m++) {
                    for (int p = 1; p <= 3; p++) {
                        CubieCube d = new CubieCube(c);
                        d.applyMove(m, p);
                        int y = d.getUDEdgePermCoord();
                        if (udEpPrun[y] == -1) {
                            udEpPrun[y] = (byte) depth;
                            next.add(y);
                        }
                    }
                }
            }
            front = next;
            try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
    }

    // Helper queries so solver can see partial readiness
    public static boolean isCOReady() { return coPrun[N_CO-1] != -1; }
    public static boolean isEOReady() { return eoPrun[N_EO-1] != -1; }
    public static boolean isSliceReady() { return slicePrun[N_SLICE-1] != -1; }
    public static boolean isCPReady() { return cpPrun[N_CP-1] != -1; }
}
