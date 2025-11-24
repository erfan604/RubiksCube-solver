package rubikscube;

public class FullBruteSolver {
    private int[] mv = new int[64];
    private int[] pw = new int[64];
    private int solLen = 0;

    public String solve(CubieCube start, int maxDepth, long deadlineMs) {
        MoveTables.init();
        for (int depth = 0; depth <= maxDepth; depth++) {
            if (System.currentTimeMillis() > deadlineMs) break;
            CubieCube c = new CubieCube(start);
            if (dfs(c, 0, depth, -1, deadlineMs)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < solLen; i++) {
                    if (i>0) sb.append(' ');
                    sb.append(Moves.moveToString(mv[i], pw[i]));
                }
                return sb.toString();
            }
        }
        return null;
    }

    private boolean dfs(CubieCube c, int depth, int limit, int lastMove, long deadlineMs) {
        if (System.currentTimeMillis() > deadlineMs) return false;
        if (c.isSolved()) { solLen = depth; return true; }
        if (depth == limit) return false;
        for (int move = 0; move < 6; move++) {
            if (lastMove != -1 && Moves.sameAxis(lastMove, move)) continue; // avoid repeating same face
            for (int p = 1; p <= 3; p++) {
                CubieCube next = new CubieCube(c);
                next.applyMove(move, p);
                mv[depth] = move; pw[depth] = p;
                if (dfs(next, depth+1, limit, move, deadlineMs)) return true;
            }
        }
        return false;
    }
}
