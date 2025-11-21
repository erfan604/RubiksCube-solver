package rubikscube;

public class MoveTablesTest {
    public static void main(String[] args) throws Exception {
        MoveTables.init();
        boolean ok = true;
        System.out.println("Testing MoveTables vs direct CubieCube.applyMove...");
        // test CO
        for (int coord = 0; coord < Math.min(PruningTables.N_CO, 5000); coord += 37) {
            CubieCube c = CubieCube.fromCornerOriCoord(coord);
            for (int move = 0; move < 6; move++) for (int p = 1; p <= 3; p++) {
                CubieCube cc = new CubieCube(c);
                cc.applyMove(move, p);
                int expected = cc.getCornerOriCoord();
                int got = MoveTables.applyCO(move, p, coord);
                if (expected != got) {
                    System.out.println("CO mismatch coord="+coord+" move="+move+" p="+p+" expected="+expected+" got="+got);
                    ok = false; break;
                }
            }
            if (!ok) break;
        }
        // test EO
        for (int coord = 0; coord < Math.min(PruningTables.N_EO, 2048); coord += 23) {
            CubieCube c = CubieCube.fromEdgeOriCoord(coord);
            for (int move = 0; move < 6; move++) for (int p = 1; p <= 3; p++) {
                CubieCube cc = new CubieCube(c);
                cc.applyMove(move, p);
                int expected = cc.getEdgeOriCoord();
                int got = MoveTables.applyEO(move, p, coord);
                if (expected != got) {
                    System.out.println("EO mismatch coord="+coord+" move="+move+" p="+p+" expected="+expected+" got="+got);
                    ok = false; break;
                }
            }
            if (!ok) break;
        }
        // test SLICE
        for (int coord = 0; coord < Math.min(PruningTables.N_SLICE, 495); coord += 7) {
            CubieCube c = CubieCube.fromUDSliceCoord(coord);
            for (int move = 0; move < 6; move++) for (int p = 1; p <= 3; p++) {
                CubieCube cc = new CubieCube(c);
                cc.applyMove(move, p);
                int expected = cc.getUDSliceCoord();
                int got = MoveTables.applySlice(move, p, coord);
                if (expected != got) {
                    System.out.println("SLICE mismatch coord="+coord+" move="+move+" p="+p+" expected="+expected+" got="+got);
                    ok = false; break;
                }
            }
            if (!ok) break;
        }
        // test CP
        for (int coord = 0; coord < Math.min(PruningTables.N_CP, 40320); coord += 1377) {
            CubieCube c = CubieCube.fromCornerPermCoord(coord);
            for (int move = 0; move < 6; move++) for (int p = 1; p <= 3; p++) {
                CubieCube cc = new CubieCube(c);
                cc.applyMove(move, p);
                int expected = cc.getCornerPermCoord();
                int got = MoveTables.applyCP(move, p, coord);
                if (expected != got) {
                    System.out.println("CP mismatch coord="+coord+" move="+move+" p="+p+" expected="+expected+" got="+got);
                    ok = false; break;
                }
            }
            if (!ok) break;
        }
        System.out.println("MoveTables test result: " + (ok ? "PASS" : "FAIL"));
        if (!ok) System.exit(3);
    }
}
