package rubikscube;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Pruning tables (PDBs) for coordinates.
 * Phase-1 tables: CO, EO, Slice, CP, UD edges.
 * Phase-2 helpers: CPxUD parity, CPxSlice, CPxUDxSlice (parity), U-edge perm, D-edge perm.
 * A full CPxUDxSlice parity table is built in memory (not cached) for stronger phase-2 heuristics.
 */
public class PruningTables {
    public static final int N_CO = 2187;
    public static final int N_EO = 2048;
    public static final int N_SLICE = 495;
    public static final int N_CP = 40320;
    public static final int N_UD_EP = 40320;
    public static final int N_CP_UD = N_CP * 2;          // cp x UD parity
    public static final int N_CP_SLICE = N_CP * N_SLICE; // cp x slice
    public static final int N_CP_UD_SLICE = N_CP * N_SLICE * 2; // cp x slice x UD parity
    private static final int SLICE_SOLVED = CubieCube.SLICE_SOLVED_COORD;

    public static final byte[] coPrun = new byte[N_CO];
    public static final byte[] eoPrun = new byte[N_EO];
    public static final byte[] slicePrun = new byte[N_SLICE];
    public static final byte[] cpPrun = new byte[N_CP];
    public static final byte[] udEpPrun = new byte[N_UD_EP];

    // phase-2 helpers
    public static final byte[] cp2Prun = new byte[N_CP];
    public static final byte[] udEp2Prun = new byte[N_UD_EP];
    public static final byte[] cpUdPrun = new byte[N_CP_UD];
    public static final byte[] cpSlicePrun2 = new byte[N_CP_SLICE];
    public static final byte[] cpUdSlicePrun = new byte[N_CP_UD_SLICE];
    public static final byte[] cpUdSliceFull = new byte[N_CP_UD_SLICE]; // in-memory only
    public static final byte[] uEdgePrun = new byte[24];
    public static final byte[] dEdgePrun = new byte[24];
    public static final byte[] udParity = new byte[N_UD_EP];

    public static volatile boolean initialized = false;
    private static volatile boolean started = false;

    private static final String CACHE_FILE = "pbt.cache";
    private static final byte[] MAGIC = new byte[] { 'P', 'B', 'T', 11 };

    // ---------- build control ----------
    public static synchronized void initAsyncStart() {
        if (started) return;
        started = true;
        MoveTables.init();
        if (loadFromDisk()) {
            for (int i = 0; i < N_UD_EP; i++) udParity[i] = (byte)permParityFromCoord(i);
            initialized = true;
            return;
        }
        resetArrays();
        prioritizedBuildLoop();
        initialized = true;
    }

    public static synchronized void buildAllBlocking() {
        if (initialized) return;
        if (started) {
            long t0 = System.currentTimeMillis();
            while (!initialized && System.currentTimeMillis() - t0 < 120_000) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            if (initialized) return;
        }
        started = true;
        MoveTables.init();
        resetArrays();
        prioritizedBuildLoop();
        initialized = true;
    }

    private static void resetArrays() {
        Arrays.fill(coPrun, (byte)-1);
        Arrays.fill(eoPrun, (byte)-1);
        Arrays.fill(slicePrun, (byte)-1);
        Arrays.fill(cpPrun, (byte)-1);
        Arrays.fill(udEpPrun, (byte)-1);
        Arrays.fill(cp2Prun, (byte)-1);
        Arrays.fill(udEp2Prun, (byte)-1);
        Arrays.fill(cpUdPrun, (byte)-1);
        Arrays.fill(cpSlicePrun2, (byte)-1);
        Arrays.fill(cpUdSlicePrun, (byte)-1);
        Arrays.fill(cpUdSliceFull, (byte)-1);
        Arrays.fill(uEdgePrun, (byte)-1);
        Arrays.fill(dEdgePrun, (byte)-1);
        Arrays.fill(udParity, (byte)0);

        coPrun[0] = eoPrun[0] = cpPrun[0] = udEpPrun[0] = 0;
        slicePrun[SLICE_SOLVED] = 0;
        cp2Prun[0] = udEp2Prun[0] = cpUdPrun[0] = 0;
        cpSlicePrun2[0 * N_SLICE + SLICE_SOLVED] = 0;
        cpUdSlicePrun[((0 * N_SLICE) + SLICE_SOLVED) * 2 + 0] = 0;
        cpUdSliceFull[((0 * N_SLICE) + SLICE_SOLVED) * 2 + 0] = 0;
        uEdgePrun[0] = dEdgePrun[0] = 0;
    }

    // ---------- cache I/O ----------
    public static synchronized void saveToDisk() {
        try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(Paths.get(CACHE_FILE)))) {
            dos.write(MAGIC);
            dos.writeInt(N_CO); dos.writeInt(N_EO); dos.writeInt(N_SLICE); dos.writeInt(N_CP); dos.writeInt(N_UD_EP);
            dos.writeInt(N_CP); dos.writeInt(N_UD_EP); dos.writeInt(N_CP_UD); dos.writeInt(N_CP_SLICE); dos.writeInt(N_CP_UD_SLICE);
            dos.writeInt(24); dos.writeInt(24);
            dos.write(coPrun); dos.write(eoPrun); dos.write(slicePrun); dos.write(cpPrun); dos.write(udEpPrun);
            dos.write(cp2Prun); dos.write(udEp2Prun); dos.write(cpUdPrun); dos.write(cpSlicePrun2); dos.write(cpUdSlicePrun);
            dos.write(uEdgePrun); dos.write(dEdgePrun);
        } catch (IOException e) {
            System.err.println("Failed to save PBT cache: " + e.getMessage());
        }
    }

    public static synchronized boolean loadFromDisk() {
        Path p = Paths.get(CACHE_FILE);
        if (!Files.exists(p)) return false;
        try (DataInputStream dis = new DataInputStream(Files.newInputStream(p))) {
            byte[] magic = new byte[MAGIC.length];
            dis.readFully(magic);
            if (!Arrays.equals(magic, MAGIC)) return false;
            int nco = dis.readInt(), neo = dis.readInt(), nsl = dis.readInt(), ncp = dis.readInt(), nud = dis.readInt();
            int ncp2 = dis.readInt(), nud2 = dis.readInt(), ncpu = dis.readInt(), ncpsl = dis.readInt(), ncpudsl = dis.readInt();
            int nue = dis.readInt(), nde = dis.readInt();
            if (nco!=N_CO||neo!=N_EO||nsl!=N_SLICE||ncp!=N_CP||nud!=N_UD_EP||ncp2!=N_CP||nud2!=N_UD_EP
                    ||ncpu!=N_CP_UD||ncpsl!=N_CP_SLICE||ncpudsl!=N_CP_UD_SLICE||nue!=24||nde!=24) return false;
            dis.readFully(coPrun); dis.readFully(eoPrun); dis.readFully(slicePrun); dis.readFully(cpPrun); dis.readFully(udEpPrun);
            dis.readFully(cp2Prun); dis.readFully(udEp2Prun); dis.readFully(cpUdPrun); dis.readFully(cpSlicePrun2); dis.readFully(cpUdSlicePrun);
            dis.readFully(uEdgePrun); dis.readFully(dEdgePrun);
            for (int i = 0; i < N_UD_EP; i++) udParity[i] = (byte)permParityFromCoord(i);
            initialized = started = true;
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load PBT cache: " + e.getMessage());
            return false;
        }
    }

    // ---------- builders ----------
    private static void prioritizedBuildLoop() {
        buildCO(); buildEO(); buildSlice(); buildCP(); buildUDEP();
        System.arraycopy(cpPrun, 0, cp2Prun, 0, cpPrun.length);
        System.arraycopy(udEpPrun, 0, udEp2Prun, 0, udEpPrun.length);
        buildCPUdParity();
        buildCPSlice2();
        buildCPUdSliceFull();
        buildUEdge();
        buildDEdge();
    }

    private static void buildCO() {
        ArrayDeque<Integer> q = new ArrayDeque<>(); q.add(0); int depth = 0;
        while (!q.isEmpty()) {
            depth++; ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int x = q.remove();
                CubieCube c = CubieCube.fromCornerOriCoord(x);
                for (int m=0;m<6;m++) {
                    for (int p=1;p<=3;p++) {
                        int power = p;
                        CubieCube d = new CubieCube(c); d.applyMove(m,power);
                        int y = d.getCornerOriCoord();
                        if (coPrun[y]==-1){ coPrun[y]=(byte)depth; next.add(y);}                    
                    }
                }
            }
            q = next;
        }
    }
    private static void buildEO() {
        ArrayDeque<Integer> q = new ArrayDeque<>(); q.add(0); int depth = 0;
        while (!q.isEmpty()) {
            depth++; ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int x = q.remove();
                CubieCube c = CubieCube.fromEdgeOriCoord(x);
                for (int m=0;m<6;m++) {
                    for (int p=1;p<=3;p++) {
                        int power = p;
                        CubieCube d = new CubieCube(c); d.applyMove(m,power);
                        int y = d.getEdgeOriCoord();
                        if (eoPrun[y]==-1){ eoPrun[y]=(byte)depth; next.add(y);}                    
                    }
                }
            }
            q = next;
        }
    }
    private static void buildSlice() {
        ArrayDeque<Integer> q = new ArrayDeque<>(); q.add(SLICE_SOLVED); int depth = 0;
        while (!q.isEmpty()) {
            depth++; ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int x = q.remove();
                CubieCube c = CubieCube.fromUDSliceCoord(x);
                for (int m=0;m<6;m++) {
                    for (int p=1;p<=3;p++) {
                        int power = p;
                        CubieCube d = new CubieCube(c); d.applyMove(m,power);
                        int y = d.getUDSliceCoord();
                        if (slicePrun[y]==-1){ slicePrun[y]=(byte)depth; next.add(y);}                    
                    }
                }
            }
            q = next;
        }
    }
    private static void buildCP() {
        ArrayDeque<Integer> q = new ArrayDeque<>(); q.add(0); int depth = 0;
        while (!q.isEmpty()) {
            depth++; ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int x = q.remove();
                CubieCube c = CubieCube.fromCornerPermCoord(x);
                for (int m=0;m<6;m++) {
                    for (int p=1;p<=3;p++) {
                        int power = p;
                        CubieCube d = new CubieCube(c); d.applyMove(m,power);
                        int y = d.getCornerPermCoord();
                        if (cpPrun[y]==-1){ cpPrun[y]=(byte)depth; next.add(y);}                    
                    }
                }
            }
            q = next;
        }
    }
    private static void buildUDEP() {
        ArrayDeque<Integer> q = new ArrayDeque<>(); q.add(0); int depth = 0;
        while (!q.isEmpty()) {
            depth++; ArrayDeque<Integer> next = new ArrayDeque<>();
            while (!q.isEmpty()) {
                int x = q.remove();
                CubieCube c = CubieCube.fromUDEdgePermCoord(x);
                for (int m=0;m<6;m++) {
                    for (int p=1;p<=3;p++) {
                        int power = p;
                        CubieCube d = new CubieCube(c); d.applyMove(m,power);
                        int y = d.getUDEdgePermCoord();
                        if (udEpPrun[y]==-1){ udEpPrun[y]=(byte)depth; next.add(y);}                    
                    }
                }
            }
            q = next;
        }
        for (int i=0;i<N_UD_EP;i++) udParity[i]=(byte)permParityFromCoord(i);
    }
    private static void buildCPUdParity() {
        Arrays.fill(cpUdPrun,(byte)-1);
        ArrayDeque<int[]> q = new ArrayDeque<>(); q.add(new int[]{0,0}); cpUdPrun[0]=0;
        int depth=0;
        while(!q.isEmpty()){
            depth++; ArrayDeque<int[]> next=new ArrayDeque<>();
            while(!q.isEmpty()){
                int[] cur=q.remove(); int cp=cur[0], ud=cur[1];
                for(int m=0;m<6;m++){ for(int p=1;p<=3;p++){
                    int power=p;
                    int ncp=MoveTables.applyCP(m,power,cp);
                    int nud=MoveTables.applyUDEP(m,power,ud);
                    int key=ncp*2+(udParity[nud]&1);
                    if(cpUdPrun[key]==-1){cpUdPrun[key]=(byte)depth; next.add(new int[]{ncp,nud});}
                }}
            }
            q=next;
        }
    }
    private static void buildCPSlice2() {
        Arrays.fill(cpSlicePrun2,(byte)-1);
        ArrayDeque<int[]> q=new ArrayDeque<>(); q.add(new int[]{0,SLICE_SOLVED}); cpSlicePrun2[0 * N_SLICE + SLICE_SOLVED]=0;
        int depth=0;
        while(!q.isEmpty()){
            depth++; ArrayDeque<int[]> next=new ArrayDeque<>();
            while(!q.isEmpty()){
                int[] cur=q.remove(); int cp=cur[0], sl=cur[1];
                for(int m=0;m<6;m++){ for(int p=1;p<=3;p++){
                    int power=p;
                    int ncp=MoveTables.applyCP(m,power,cp);
                    int nsl=MoveTables.applySlice(m,power,sl);
                    int key=ncp*N_SLICE+nsl;
                    if(cpSlicePrun2[key]==-1){cpSlicePrun2[key]=(byte)depth; next.add(new int[]{ncp,nsl});}
                }}
            }
            q=next;
        }
    }
    private static void buildCPUdSliceFull() {
        Arrays.fill(cpUdSliceFull,(byte)-1);
        ArrayDeque<int[]> q=new ArrayDeque<>(); q.add(new int[]{0,SLICE_SOLVED,0}); cpUdSliceFull[((0 * N_SLICE) + SLICE_SOLVED) * 2 + 0]=0;
        int depth=0;
        while(!q.isEmpty()){
            depth++; ArrayDeque<int[]> next=new ArrayDeque<>();
            while(!q.isEmpty()){
                int[] cur=q.remove(); int cp=cur[0], sl=cur[1], ud=cur[2];
                for(int m=0;m<6;m++){ for(int p=1;p<=3;p++){
                    int power=p;
                    int ncp=MoveTables.applyCP(m,power,cp);
                    int nud=MoveTables.applyUDEP(m,power,ud);
                    int nsl=MoveTables.applySlice(m,power,sl);
                    int key=((ncp*N_SLICE)+nsl)*2+(udParity[nud]&1);
                    if(cpUdSliceFull[key]==-1){cpUdSliceFull[key]=(byte)depth; next.add(new int[]{ncp,nsl,nud});}
                }}
            }
            q=next;
        }
    }
    private static void buildUEdge() {
        Arrays.fill(uEdgePrun,(byte)-1);
        ArrayDeque<Integer> q=new ArrayDeque<>(); q.add(0); uEdgePrun[0]=0; int depth=0;
        while(!q.isEmpty()){
            depth++; ArrayDeque<Integer> next=new ArrayDeque<>();
            while(!q.isEmpty()){
                int x=q.remove(); CubieCube c=CubieCube.fromUEdgePermCoord(x);
                for(int m=0;m<6;m++) for(int p=1;p<=3;p++){
                    CubieCube d=new CubieCube(c); d.applyMove(m,p);
                    int y=d.getUEdgePermCoord();
                    if(uEdgePrun[y]==-1){uEdgePrun[y]=(byte)depth; next.add(y);}
                }
            }
            q=next;
        }
    }
    private static void buildDEdge() {
        Arrays.fill(dEdgePrun,(byte)-1);
        ArrayDeque<Integer> q=new ArrayDeque<>(); q.add(0); dEdgePrun[0]=0; int depth=0;
        while(!q.isEmpty()){
            depth++; ArrayDeque<Integer> next=new ArrayDeque<>();
            while(!q.isEmpty()){
                int x=q.remove(); CubieCube c=CubieCube.fromDEdgePermCoord(x);
                for(int m=0;m<6;m++) for(int p=1;p<=3;p++){
                    CubieCube d=new CubieCube(c); d.applyMove(m,p);
                    int y=d.getDEdgePermCoord();
                    if(dEdgePrun[y]==-1){dEdgePrun[y]=(byte)depth; next.add(y);}
                }
            }
            q=next;
        }
    }

    // ---------- readiness accessors (used by harness/debug) ----------
    public static boolean isCOReady() { return coPrun != null && coPrun.length == N_CO && coPrun[0] != -1; }
    public static boolean isEOReady() { return eoPrun != null && eoPrun.length == N_EO && eoPrun[0] != -1; }
    public static boolean isSliceReady() { return slicePrun != null && slicePrun.length == N_SLICE && slicePrun[0] != -1; }
    public static boolean isCPReady() { return cpPrun != null && cpPrun.length == N_CP && cpPrun[0] != -1; }

    private static int permParityFromCoord(int coord) {
        int parity=0;
        int[] fact={1,1,2,6,24,120,720,5040,40320};
        int rem=coord; boolean[] used=new boolean[8];
        for(int i=0;i<8;i++){
            int div=fact[7-i]; int index=rem/div; rem%=div;
            int cnt=0,j=0;
            while(true){
                if(!used[j]){
                    if(cnt==index) break;
                    cnt++;
                }
                j++;
            }
            used[j]=true;
            parity^=index&1;
        }
        return parity&1;
    }
}
