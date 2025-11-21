package rubikscube;

public class SimpleIDA {

    private static final int MAX_DEPTH = 22;

    private int[] solMoves = new int[MAX_DEPTH];
    private int[] solPowers = new int[MAX_DEPTH];
    private int solLength;

    public String solve(CubieCube start) {
        // Start incremental pruning-table builder (non-blocking)
        PruningTables.initAsyncStart();

        if (start.isSolved()) return "";

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (search(start, 0, depth, -1)) {
                return buildSolutionVerified(start);
            }
        }
        return "";
    }

    private boolean search(CubieCube node, int depth, int limit, int lastMove) {
        if (Thread.currentThread().isInterrupted()) return false;
        int h = heuristic(node);
        if (depth + h > limit) return false;

        if (node.isSolved()) {
            solLength = depth;
            return true;
        }

        for (int move = 0; move < 6; move++) {
            if (Thread.currentThread().isInterrupted()) return false;
            if (move == lastMove) continue;

            for (int p = 1; p <= 3; p++) {
                if (Thread.currentThread().isInterrupted()) return false;
                CubieCube next = new CubieCube(node);
                next.applyMove(move, p);

                solMoves[depth] = move;
                solPowers[depth] = p;

                if (search(next, depth + 1, limit, move)) {
                    return true;
                }
            }
        }

        return false;
    }

    private int heuristic(CubieCube c) {
        int co = c.getCornerOriCoord();
        int eo = c.getEdgeOriCoord();
        int cp = c.getCornerPermCoord();
        int sl = c.getUDSliceCoord();

        int h1 = PruningTables.coPrun[co];
        int h2 = PruningTables.eoPrun[eo];
        int h3 = PruningTables.cpPrun[cp];
        int h4 = PruningTables.slicePrun[sl];

        int h = Math.max(Math.max(h1, h2), Math.max(h3, h4));
        return (h < 0) ? 0 : h;
    }

    private String buildSolution() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solLength; i++) {
            sb.append(Moves.moveToString(solMoves[i], solPowers[i]));
            if (i < solLength - 1) sb.append(" ");
        }
        return sb.toString();
    }

    // Verify forward solution; if it doesn't solve, try reversed order and return that if it solves.
    private String buildSolutionVerified(CubieCube start) {
        String forward = buildSolution();

        // Apply forward to a copy
        CubieCube test = new CubieCube(start);
        for (int i = 0; i < solLength; i++) test.applyMove(solMoves[i], solPowers[i]);
        if (test.isSolved()) {
            return forward; // program-format
        }

        // Try reversed order (use inverse powers)
        CubieCube test2 = new CubieCube(start);
        for (int i = solLength - 1; i >= 0; i--) {
            int mv = solMoves[i];
            int p = solPowers[i];
            int inv = (p == 2) ? 2 : ((p == 1) ? 3 : 1);
            test2.applyMove(mv, inv);
        }
        if (test2.isSolved()) {
            StringBuilder sb = new StringBuilder();
            for (int i = solLength - 1; i >= 0; i--) {
                int mv = solMoves[i];
                int p = solPowers[i];
                int inv = (p == 2) ? 2 : ((p == 1) ? 3 : 1);
                sb.append(Moves.moveToString(mv, inv));
                if (i > 0) sb.append(" ");
            }
            return sb.toString();
        }

        // Fallback: return forward (program-format)
        return forward;
    }

    // Translate program moves into user's convention by inverting U,R,F,B
    private String translateToUser(String progSeq) {
        if (progSeq == null || progSeq.isEmpty()) return progSeq;
        StringBuilder out = new StringBuilder();
        String[] tokens = progSeq.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3; // apostrophe

            // invert faces U,R,F,B for user convention
            if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                if (power == 1) power = 3; else if (power == 3) power = 1;
            }

            out.append(face);
            if (power == 2) out.append('2');
            else if (power == 3) out.append('\'');
            if (i < tokens.length - 1) out.append(' ');
        }
        return out.toString();
    }
}
