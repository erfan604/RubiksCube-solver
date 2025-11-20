package rubikscube;

public class SimpleIDA {

    private static final int MAX_DEPTH = 22;

    private int[] solMoves = new int[MAX_DEPTH];
    private int[] solPowers = new int[MAX_DEPTH];
    private int solLength;

    public String solve(CubieCube start) {
        Tables.init();

        if (start.isSolved()) return "";

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            if (search(start, 0, depth, -1)) {
                return buildSolution();
            }
        }
        return "";
    }

    private boolean search(CubieCube node, int depth, int limit, int lastMove) {
        int h = heuristic(node);
        if (depth + h > limit) return false;

        if (node.isSolved()) {
            solLength = depth;
            return true;
        }

        for (int move = 0; move < 6; move++) {
            if (move == lastMove) continue;

            for (int p = 1; p <= 3; p++) {
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

        int h1 = Tables.coPrun[co];
        int h2 = Tables.eoPrun[eo];
        int h3 = Tables.cpPrun[cp];
        int h4 = Tables.slicePrun[sl];

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
}
