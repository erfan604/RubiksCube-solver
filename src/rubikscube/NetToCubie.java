package rubikscube;

import java.util.Arrays;

// Converting stickers from assignment 1 to cubies.
public class NetToCubie {
    private static final boolean DEBUG = false;

    private static final int U_CENTER = 4;
    private static final int R_CENTER = 31;
    private static final int F_CENTER = 22;
    private static final int D_CENTER = 49;
    private static final int L_CENTER = 13;
    private static final int B_CENTER = 40;

    public static CubieCube fromFacelets(char[] f) {
        if (f == null || f.length != 54) {
            throw new IllegalArgumentException("Expected 54 facelets");
        }

        FaceCube fc = new FaceCube(f);
        CubieCube cc = new CubieCube();

        for (int i = 0; i < 8; i++) {
            cc.cp[i] = fc.getCornerPerm(i);
            cc.co[i] = fc.getCornerOri(i);
        }
        for (int i = 0; i < 12; i++) {
            cc.ep[i] = fc.getEdgePerm(i);
            cc.eo[i] = fc.getEdgeOri(i);
        }

        validate(cc);
        if (DEBUG) {
            debugPrintCenters(f);
            debugPrintState(cc);
        }
        return cc;
    }

    private static void validate(CubieCube cc) {
        boolean[] cornerSeen = new boolean[8];
        boolean[] edgeSeen = new boolean[12];

        for (byte c : cc.cp) {
            if (c < 0 || c >= 8 || cornerSeen[c]) {
                debugPrintState(cc);
                throw new IllegalArgumentException("Invalid corner permutation");
            }
            cornerSeen[c] = true;
        }
        for (byte e : cc.ep) {
            if (e < 0 || e >= 12 || edgeSeen[e]) {
                debugPrintState(cc);
                throw new IllegalArgumentException("Invalid edge permutation");
            }
            edgeSeen[e] = true;
        }

        int coSum = 0;
        for (byte co : cc.co) coSum = (coSum + co) % 3;
        if (coSum != 0) {
            debugPrintState(cc);
            throw new IllegalArgumentException("Corner twist parity violated");
        }

        int eoSum = 0;
        for (byte eo : cc.eo) eoSum ^= eo;
        if (eoSum != 0) {
            debugPrintState(cc);
            throw new IllegalArgumentException("Edge flip parity violated");
        }

        if ((permParity(cc.cp) ^ permParity(cc.ep)) != 0) {
            debugPrintState(cc);
            throw new IllegalArgumentException("Corner/edge permutation parity mismatch");
        }
    }

    private static int permParity(byte[] perm) {
        int parity = 0;
        boolean[] seen = new boolean[perm.length];
        for (int i = 0; i < perm.length; i++) {
            if (seen[i]) continue;
            int j = i;
            int cycle = 0;
            while (!seen[j]) {
                seen[j] = true;
                j = perm[j];
                cycle++;
            }
            if (cycle > 0) parity ^= (cycle - 1) & 1;
        }
        return parity;
    }

    private static void debugPrintCenters(char[] f) {
        char Uc = f[U_CENTER];
        char Rc = f[R_CENTER];
        char Fc = f[F_CENTER];
        char Dc = f[D_CENTER];
        char Lc = f[L_CENTER];
        char Bc = f[B_CENTER];
        System.out.println("========== CENTER COLORS ==========");
        System.out.println("U=" + Uc + "  L=" + Lc + "  F=" + Fc +
                "  R=" + Rc + "  B=" + Bc + "  D=" + Dc);
        System.out.println();
    }

    private static void debugPrintState(CubieCube cc) {
        System.out.println("\n========== FINAL CUBIECUBE ==========");
        System.out.println("cp: " + Arrays.toString(cc.cp));
        System.out.println("co: " + Arrays.toString(cc.co));
        System.out.println("ep: " + Arrays.toString(cc.ep));
        System.out.println("eo: " + Arrays.toString(cc.eo));
        System.out.println("====================================\n");
    }
}
