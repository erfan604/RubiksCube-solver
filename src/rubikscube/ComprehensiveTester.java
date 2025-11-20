package rubikscube;

public class ComprehensiveTester {
    public static void main(String[] args) {
        boolean ok = true;

        CubieCube solved = new CubieCube();
        if (!solved.isSolved()) {
            System.out.println("ERROR: newly constructed cube not solved");
            ok = false;
        }

        // test each face move
        for (int m = 0; m < 6; m++) {
            System.out.println("--- Testing move: " + Moves.MOVE_NAMES[m] + " ---");

            // apply move once
            CubieCube c = new CubieCube(solved);
            c.applyMove(m, 1);

            // move^4 should be identity
            for (int i = 0; i < 3; i++) c.applyMove(m, 1);
            if (!c.isSolved()) {
                System.out.println("FAIL: " + Moves.MOVE_NAMES[m] + "^4 != identity");
                ok = false;
            } else {
                System.out.println("OK: " + Moves.MOVE_NAMES[m] + "^4 == identity");
            }

            // orientation invariants: sum co mod3 == 0, sum eo mod2 == 0
            c = new CubieCube(solved);
            c.applyMove(m, 1);
            int sumCo = 0; for (int i = 0; i < 8; i++) sumCo += c.co[i];
            int sumEo = 0; for (int i = 0; i < 12; i++) sumEo += c.eo[i];
            if (sumCo % 3 != 0) {
                System.out.println("FAIL: corner orientation sum mod3 != 0 after " + Moves.MOVE_NAMES[m]);
                ok = false;
            } else System.out.println("OK: corner orientation invariant holds");
            if (sumEo % 2 != 0) {
                System.out.println("FAIL: edge orientation sum mod2 != 0 after " + Moves.MOVE_NAMES[m]);
                ok = false;
            } else System.out.println("OK: edge orientation invariant holds");

            // permutation parity: cp parity should equal ep parity
            c = new CubieCube(solved);
            c.applyMove(m, 1);
            int cpParity = parity(c.cp);
            int epParity = parity(c.ep);
            if (cpParity != epParity) {
                System.out.println("FAIL: permutation parity mismatch after " + Moves.MOVE_NAMES[m] + " (cp=" + cpParity + ", ep=" + epParity + ")");
                ok = false;
            } else System.out.println("OK: permutation parity matches (" + cpParity + ")");

            // test multiply + inverse: move * inverse == identity
            CubieCube moveC = new CubieCube(solved);
            moveC.applyMove(m,1);
            CubieCube inv = new CubieCube();
            moveC.inverse(inv);
            CubieCube prod = new CubieCube();
            moveC.multiply(inv, prod);
            if (!prod.isSolved()) {
                System.out.println("FAIL: move * inverse != identity for " + Moves.MOVE_NAMES[m]);
                ok = false;
            } else System.out.println("OK: move * inverse == identity");

            System.out.println();
        }

        if (ok) System.out.println("All basic move invariants passed.");
        else System.out.println("Some checks failed — inspect output above.");
    }

    private static int parity(byte[] arr) {
        int inv = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i+1; j < arr.length; j++) {
                if (arr[i] > arr[j]) inv++;
            }
        }
        return inv % 2;
    }
}
