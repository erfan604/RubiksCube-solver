package rubikscube;

import java.util.Arrays;

/**
 * Single-phase IDA* using the existing pruning tables (CO/EO/Slice/CP).
 * Returns a program-format move string or empty if no solution found within the depth limit.
 */
public class SimpleIDA {

    // modest cap because scrambles are short; can be raised if needed
    private static final int MAX_DEPTH = 25;

    private final int[] moves = new int[MAX_DEPTH];
    private final int[] powers = new int[MAX_DEPTH];

    public String solve(CubieCube start) {
        // ensure tables are ready
        MoveTables.init();
        if (!PruningTables.initialized) {
            PruningTables.buildAllBlocking();
        }

        int co = start.getCornerOriCoord();
        int eo = start.getEdgeOriCoord();
        int sl = start.getUDSliceCoord();
        int cp = start.getCornerPermCoord();
        int ud = start.getUDEdgePermCoord();

        if (co == 0 && eo == 0 && sl == 0 && cp == 0 && ud == 0) return "";

        for (int limit = heuristic(co, eo, sl, cp, ud); limit <= MAX_DEPTH; limit++) {
            if (search(0, limit, -1, co, eo, sl, cp, ud)) {
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

    private int heuristic(int co, int eo, int sl, int cp, int ud) {
        int h1 = PruningTables.coPrun[co];
        int h2 = PruningTables.eoPrun[eo];
        int h3 = PruningTables.slicePrun[sl];
        int h4 = PruningTables.cpPrun[cp];
        int h5 = PruningTables.udEpPrun[ud];
        int h = Math.max(Math.max(h1, h2), Math.max(Math.max(h3, h4), h5));
        return (h < 0) ? 0 : h;
    }

    private boolean search(int depth, int limit, int lastMove, int co, int eo, int sl, int cp, int ud) {
        int h = heuristic(co, eo, sl, cp, ud);
        if (depth + h > limit) return false;
        if (co == 0 && eo == 0 && sl == 0 && cp == 0 && ud == 0) return true;

        for (int move = 0; move < 6; move++) {
            if (lastMove != -1 && Moves.sameAxis(lastMove, move)) continue;
            for (int p = 1; p <= 3; p++) {
                int nco = MoveTables.applyCO(move, p, co);
                int neo = MoveTables.applyEO(move, p, eo);
                int nsl = MoveTables.applySlice(move, p, sl);
                int ncp = MoveTables.applyCP(move, p, cp);
                int nud = MoveTables.applyUDEP(move, p, ud);

                moves[depth] = move;
                powers[depth] = p;

                if (search(depth + 1, limit, move, nco, neo, nsl, ncp, nud)) return true;
            }
        }
        return false;
    }
}
