package rubikscube;

/**
 * IDA* over the full cube state using apply/undo so goal check uses isSolved().
 * Heuristic: max of CO/EO/SLICE/CP pruning tables and UD-edge perm pruning table.
 */
public class FullCubeIDA {
    private static final int MAX_DEPTH = 25;
    private final int[] moves = new int[MAX_DEPTH];
    private final int[] powers = new int[MAX_DEPTH];

    public String solve(CubieCube start) {
        MoveTables.init();
        if (!PruningTables.initialized) {
            PruningTables.buildAllBlocking();
        }

        if (start.isSolved()) return "";

        for (int limit = heuristic(start); limit <= MAX_DEPTH; limit++) {
            if (dfs(start, 0, limit, -1)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < limit; i++) {
                    if (i > 0) sb.append(' ');
                    sb.append(Moves.moveToString(moves[i], powers[i]));
                }
                return sb.toString();
            }
        }
        return "";
    }

    private boolean dfs(CubieCube cube, int depth, int limit, int lastMove) {
        int h = heuristic(cube);
        if (depth + h > limit) return false;
        if (cube.isSolved()) return true;

        for (int move = 0; move < 6; move++) {
            if (lastMove != -1 && Moves.sameAxis(lastMove, move)) continue;
            for (int p = 1; p <= 3; p++) {
                cube.applyMove(move, p);
                moves[depth] = move;
                powers[depth] = p;
                if (dfs(cube, depth + 1, limit, move)) return true;
                // undo
                int invP = (p == 2) ? 2 : (p == 1 ? 3 : 1);
                cube.applyMove(move, invP);
            }
        }
        return false;
    }

    private int heuristic(CubieCube c) {
        int co = c.getCornerOriCoord();
        int eo = c.getEdgeOriCoord();
        int sl = c.getUDSliceCoord();
        int cp = c.getCornerPermCoord();
        int ud = c.getUDEdgePermCoord();
        int h = Math.max(
                Math.max(PruningTables.coPrun[co], PruningTables.eoPrun[eo]),
                Math.max(Math.max(PruningTables.slicePrun[sl], PruningTables.cpPrun[cp]),
                        PruningTables.udEpPrun[ud])
        );
        return (h < 0) ? 0 : h;
    }
}
