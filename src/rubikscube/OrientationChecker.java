package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class OrientationChecker {

    // generator of 24 orientations using rotations of the cube in facelet indices
    // We'll build orientation transforms by composing basic rotations around X,Y axes on facelet indices.

    // Basic facelet rotation maps for a 3x3 face: rotate 90° clockwise mapping positions
    private static int[] rotFaceCW() {
        return new int[]{6,3,0,7,4,1,8,5,2};
    }

    // Helper to rotate entire facelet array by permuting indices according to mapping
    private static char[] applyPerm(char[] f, int[] perm) {
        char[] out = new char[54];
        for (int i=0;i<54;i++) out[i] = f[perm[i]];
        return out;
    }

    // Build identity perm
    private static int[] identity() { int[] p = new int[54]; for (int i=0;i<54;i++) p[i]=i; return p; }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java OrientationChecker <scrambleFile> <sequence>");
            return;
        }
        List<String> lines = Files.readAllLines(Paths.get(args[0]));
        char[] base = ParserTester.parseNetOnly(lines);
        String seq = args[1];

        // We will try 24 orientations by rotating the whole cube. For simplicity generate by rotating around U face (Z) and X axis.
        // Instead of computing full facelet permutations, simpler approach: apply physical rotations using cubie operations.

        // Try each orientation by rotating the cube with known sequences that map cube to all 24 orientations.
        String[] orientationSeqs = new String[] {
            "", "X", "X X", "X X X",
            "Y", "Y X", "Y X X", "Y X X X",
            "Y Y", "Y Y X", "Y Y X X", "Y Y X X X",
            "Y Y Y", "Y Y Y X", "Y Y Y X X", "Y Y Y X X X",
            "Z", "Z X", "Z X X", "Z X X X",
            "Z Z", "Z Z X", "Z Z X X", "Z Z X X X"
        };

        // Map shorthand rotations to move sequences on CubieCube: X = R, R, R? We'll use cube rotations as move sequences:
        // Define rotations: X = R, followed by M' and S' in real world, but easier: use face rotations to simulate whole-cube rotation by rotating faces.
        // For simplicity we'll apply cubie-level rotations by permuting cubie arrays directly using existing moves: rotate cube around X = R move applied to whole cube (not perfect), but pragmatic approach: apply sequences of face turns that effect whole-cube rotation.

        // Simpler reliable approach: convert base facelets -> CubieCube, then physically rotate the cubiecube using index remapping for 24 orientations.

        CubieCube baseC = NetToCubie.fromFacelets(base);

        // Generate all 24 cubie index permutations by rotating the cube using helper rotations around axes.
        CubieCube[] orientations = generateAllOrientations(baseC);

        boolean found = false;
        for (int i = 0; i < orientations.length; i++) {
            CubieCube c = new CubieCube(orientations[i]);
            // apply sequence
            String[] tokens = seq.split("\\s+");
            for (String t : tokens) {
                if (t.length()==0) continue;
                int move = -1, power = 1;
                char face = t.charAt(0);
                switch(face) {
                    case 'U': move = Moves.U; break;
                    case 'R': move = Moves.R; break;
                    case 'F': move = Moves.F; break;
                    case 'D': move = Moves.D; break;
                    case 'L': move = Moves.L; break;
                    case 'B': move = Moves.B; break;
                    default: throw new IllegalArgumentException("Unknown face: " + face);
                }
                if (t.length()==1) power=1;
                else if (t.charAt(1)=='2') power=2;
                else power=3; // apostrophe
                c.applyMove(move, power);
            }
            if (c.isSolved()) {
                System.out.println("Sequence solves cube under orientation index: " + i);
                printCubie(orientations[i]);
                found = true;
            }
        }
        if (!found) System.out.println("No orientation made the sequence solve the cube.");
    }

    private static void printCubie(CubieCube c) {
        System.out.print("cp: "); for (int i=0;i<8;i++) System.out.print(c.cp[i]+" "); System.out.println();
        System.out.print("co: "); for (int i=0;i<8;i++) System.out.print(c.co[i]+" "); System.out.println();
        System.out.print("ep: "); for (int i=0;i<12;i++) System.out.print(c.ep[i]+" "); System.out.println();
        System.out.print("eo: "); for (int i=0;i<12;i++) System.out.print(c.eo[i]+" "); System.out.println();
    }

    // Generate all 24 orientations by rotating a cubie cube around X and Y axes
    private static CubieCube[] generateAllOrientations(CubieCube c) {
        CubieCube[] res = new CubieCube[24];
        int idx = 0;
        CubieCube current = new CubieCube(c);
        for (int i=0;i<6;i++) {
            for (int j=0;j<4;j++) {
                res[idx++] = new CubieCube(current);
                // rotate around U (Z) face to get next orientation
                rotateCubeZ(current);
            }
            if (i%2==0) rotateCubeY(current); else rotateCubeX(current);
        }
        return res;
    }

    // Cube rotation helpers: perform a whole-cube rotation by permuting cp/co/ep/eo arrays
    private static void rotateCubeX(CubieCube c) {
        // rotate cube around X axis (R face toward top)
        // Update cp, co, ep, eo according to known index mapping
        byte[] newCp = new byte[8]; byte[] newCo = new byte[8];
        byte[] newEp = new byte[12]; byte[] newEo = new byte[12];

        // mapping for X rotation (90°): corners: URF->UFL->ULB->UBR->DRB->DFR->DLF->DBL etc
        int[] cpMap = {4,0,1,2,5,6,7,3}; // placeholder — may need verification
        int[] coMap = {0,0,0,0,0,0,0,0};
        int[] epMap = {8,0,1,2,9,5,6,7,4,10,11,3};

        for (int i=0;i<8;i++) { newCp[i] = c.cp[cpMap[i]]; newCo[i] = c.co[cpMap[i]]; }
        for (int i=0;i<12;i++) { newEp[i] = c.ep[epMap[i]]; newEo[i] = c.eo[epMap[i]]; }

        c.cp = newCp; c.co = newCo; c.ep = newEp; c.eo = newEo;
    }

    private static void rotateCubeY(CubieCube c) {
        // rotate around Y axis (U face toward R)
        byte[] newCp = new byte[8]; byte[] newCo = new byte[8];
        byte[] newEp = new byte[12]; byte[] newEo = new byte[12];

        int[] cpMap = {3,4,5,6,7,0,1,2};
        int[] epMap = {3,4,8,9,10,11,0,1,2,5,6,7};

        for (int i=0;i<8;i++) { newCp[i] = c.cp[cpMap[i]]; newCo[i] = c.co[cpMap[i]]; }
        for (int i=0;i<12;i++) { newEp[i] = c.ep[epMap[i]]; newEo[i] = c.eo[epMap[i]]; }

        c.cp = newCp; c.co = newCo; c.ep = newEp; c.eo = newEo;
    }

    private static void rotateCubeZ(CubieCube c) {
        // rotate around Z axis (front face toward top)
        byte[] newCp = new byte[8]; byte[] newCo = new byte[8];
        byte[] newEp = new byte[12]; byte[] newEo = new byte[12];

        int[] cpMap = {1,2,3,0,4,5,6,7};
        int[] epMap = {1,2,3,0,4,5,6,7,8,9,10,11};

        for (int i=0;i<8;i++) { newCp[i] = c.cp[cpMap[i]]; newCo[i] = c.co[cpMap[i]]; }
        for (int i=0;i<12;i++) { newEp[i] = c.ep[epMap[i]]; newEo[i] = c.eo[epMap[i]]; }

        c.cp = newCp; c.co = newCo; c.ep = newEp; c.eo = newEo;
    }
}
