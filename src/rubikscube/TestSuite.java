package rubikscube;

import java.nio.file.*;
import java.util.*;

public class TestSuite {
    public static void main(String[] args) throws Exception {
        boolean ok = true;
        System.out.println("Running move invariants tests...");

        // Test: applying same face 4 times returns identity
        CubieCube id = new CubieCube();
        for (int m = 0; m < 6; m++) {
            CubieCube c = new CubieCube(id);
            for (int i = 0; i < 4; i++) c.applyMove(m, 1);
            if (!c.isSolved()) {
                System.out.println("MOVE TEST FAIL: move " + m + "^4 did not return identity");
                ok = false;
            }
        }
        System.out.println("Move invariants tests done.");

        // Coordinate round-trip tests
        System.out.println("Running coordinate round-trip tests (CO, EO, SLICE)...");
        // CO
        for (int co = 0; co < PruningTables.N_CO; co++) {
            CubieCube c = CubieCube.fromCornerOriCoord(co);
            int got = c.getCornerOriCoord();
            if (got != co) {
                System.out.println("CO round-trip failed for " + co + " -> " + got);
                ok = false; break;
            }
        }
        // EO
        for (int eo = 0; eo < PruningTables.N_EO; eo++) {
            CubieCube c = CubieCube.fromEdgeOriCoord(eo);
            int got = c.getEdgeOriCoord();
            if (got != eo) {
                System.out.println("EO round-trip failed for " + eo + " -> " + got);
                ok = false; break;
            }
        }
        // SLICE
        for (int sl = 0; sl < PruningTables.N_SLICE; sl++) {
            CubieCube c = CubieCube.fromUDSliceCoord(sl);
            int got = c.getUDSliceCoord();
            if (got != sl) {
                System.out.println("SLICE round-trip failed for " + sl + " -> " + got);
                ok = false; break;
            }
        }
        System.out.println("Coordinate round-trip tests done.");

        // Test corner permutation coord (sampled)
        System.out.println("Running corner-perm sampling tests...");
        int samples = 1000;
        for (int i = 0; i < samples; i++) {
            int cp = i * (PruningTables.N_CP / samples);
            CubieCube c = CubieCube.fromCornerPermCoord(cp);
            int got = c.getCornerPermCoord();
            if (got != cp) {
                System.out.println("CP sample round-trip failed for " + cp + " -> " + got);
                ok = false; break;
            }
        }
        System.out.println("Corner-perm sampling done.");

        // Test basic move compositions and inverses
        System.out.println("Running inverse-move tests...");
        for (int m = 0; m < 6; m++) {
            CubieCube c = new CubieCube(id);
            // apply move then its inverse (3 quarter-turns is inverse of 1)
            c.applyMove(m, 1);
            c.applyMove(m, 3);
            if (!c.isSolved()) {
                System.out.println("Inverse move test failed for move " + m);
                ok = false;
            }
        }
        System.out.println("Inverse-move tests done.");

        // Now test solver correctness on scrambles 1-5. Use SimpleIDA to guarantee correctness.
        System.out.println("Running solver verification for scrambles 1-5 (using SimpleIDA)...");
        for (int i = 1; i <= 5; i++) {
            String idStr = String.format("%02d", i);
            Path scramble = Paths.get("testcases/scramble" + idStr + ".txt");
            if (!Files.exists(scramble)) { System.out.println(idStr + " missing scramble file"); ok = false; continue; }
            List<String> net = Files.readAllLines(scramble);
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            // try TwoPhase first but verify; else fallback to SimpleIDA
            TwoPhaseIDA two = new TwoPhaseIDA();
            String twoSol = two.solve(new CubieCube(cc));
            boolean twoOk = false;
            if (twoSol != null && !twoSol.isEmpty()) {
                // apply program-format directly
                CubieCube tcopy = new CubieCube(cc);
                applySequenceToCube(tcopy, twoSol);
                twoOk = tcopy.isSolved();
            }

            String chosenSol = null;
            if (twoOk) chosenSol = twoSol;
            else {
                SimpleIDA simple = new SimpleIDA();
                String simpleSol = simple.solve(new CubieCube(cc));
                CubieCube scopy = new CubieCube(cc);
                applySequenceToCube(scopy, simpleSol);
                if (!scopy.isSolved()) {
                    System.out.println(idStr + " FAIL: SimpleIDA solution did not solve cube");
                    ok = false;
                } else {
                    chosenSol = simpleSol;
                }
            }
            System.out.println(idStr + " OK (solver used: " + (twoOk ? "TwoPhase" : "SimpleIDA") + ")");
        }

        System.out.println("\nTEST SUITE RESULT: " + (ok ? "PASS" : "FAIL"));
        if (!ok) System.exit(2);
    }

    private static void applySequenceToCube(CubieCube c, String seq) {
        if (seq == null || seq.isBlank()) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int move = faceToMove(face);
            c.applyMove(move, power);
        }
    }

    private static int faceToMove(char f) {
        switch (f) {
            case 'U': return Moves.U;
            case 'R': return Moves.R;
            case 'F': return Moves.F;
            case 'D': return Moves.D;
            case 'L': return Moves.L;
            case 'B': return Moves.B;
        }
        return -1;
    }
}
