package rubikscube;

public class TwoPhaseIDA {

    private static final int MAX_DEPTH = 20;

    private int[] solutionMoves = new int[MAX_DEPTH];
    private int[] solutionPowers = new int[MAX_DEPTH];
    private int solutionLength;

    // ------------------------------------------------------
    // PUBLIC ENTRY
    // ------------------------------------------------------
    public String solve(CubieCube start) {
        PruningTables.init();

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            if (search(start, 0, depth, -1)) {
                return buildSolution();
            }
        }
        return "";
    }

    // ------------------------------------------------------
    // BUILD SOLUTION STRING
    // ------------------------------------------------------
    private String buildSolution() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solutionLength; i++) {
            sb.append(Moves.moveToString(solutionMoves[i], solutionPowers[i]));
            if (i < solutionLength - 1) sb.append(" ");
        }
        return sb.toString();
    }

    // ------------------------------------------------------
    // IDA* RECURSIVE SEARCH
    // ------------------------------------------------------
    private boolean search(CubieCube node, int depth, int limit, int lastMove) {
        int h = heuristic(node);
        if (depth + h > limit) return false;

        if (node.isSolved()) {
            solutionLength = depth;
            return true;
        }

        for (int move = 0; move < 6; move++) {
            if (lastMove != -1 && Moves.sameAxis(lastMove, move)) continue;

            for (int power = 1; power <= 3; power++) {
                CubieCube next = new CubieCube(node);
                next.applyMove(move, power);

                solutionMoves[depth] = move;
                solutionPowers[depth] = power;

                if (search(next, depth + 1, limit, move)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ------------------------------------------------------
    // HEURISTIC (Phase-1 style)
    // ------------------------------------------------------
    private int heuristic(CubieCube c) {
        int co = c.getCornerOriCoord();
        int eo = c.getEdgeOriCoord();
        int sl = c.getUDSliceCoord();

        int h1 = PruningTables.coPrun[co];
        int h2 = PruningTables.eoPrun[eo];
        int h3 = PruningTables.slicePrun[sl];

        int h = Math.max(h1, Math.max(h2, h3));
        return (h < 0) ? 0 : h;
    }
}
