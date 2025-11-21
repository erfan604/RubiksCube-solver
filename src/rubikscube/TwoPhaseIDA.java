package rubikscube;

import java.util.*;

public class TwoPhaseIDA {

    private static final int MAX_DEPTH = 22;

    private int[] solutionMoves = new int[ MAX_DEPTH * 2 ];
    private int[] solutionPowers = new int[ MAX_DEPTH * 2 ];
    private int phase1Length;
    private int phase2Length;

    // timing (milliseconds)
    private long phase1TimeMs = 0;
    private long phase2TimeMs = 0;

    public String solve(CubieCube start) {
        // Ensure move/coord tables are ready and start pruning tables
        PruningTables.initAsyncStart();
        MoveTables.init();

        if (start.isSolved()) return "";

        int startCO = start.getCornerOriCoord();
        int startEO = start.getEdgeOriCoord();
        int startSL = start.getUDSliceCoord();
        int startCP = start.getCornerPermCoord();

        // Phase-1 iterative deepening
        long tPhase1Start = System.nanoTime();
        boolean phase1Found = false;
        for (int depth1 = 0; depth1 <= MAX_DEPTH; depth1++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (searchPhase1Coord(startCO, startEO, startSL, 0, depth1, -1)) {
                phase1Found = true;
                break;
            }
        }
        long tPhase1End = System.nanoTime();
        phase1TimeMs = (tPhase1End - tPhase1Start) / 1_000_000;

        if (!phase1Found) return "";

        // reconstruct mid-cube by applying moves
        CubieCube mid = new CubieCube(start);
        for (int i = 0; i < phase1Length; i++) mid.applyMove(solutionMoves[i], solutionPowers[i]);

        int midCP = mid.getCornerPermCoord();
        int midSL = mid.getUDSliceCoord();

        // Phase-2 iterative deepening with restricted moves
        long tPhase2Start = System.nanoTime();
        boolean phase2Found = false;
        phase2Length = 0;
        for (int depth2 = 0; depth2 <= MAX_DEPTH; depth2++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (searchPhase2Coord(midCP, midSL, 0, depth2, -1)) {
                phase2Found = true;
                break;
            }
        }
        long tPhase2End = System.nanoTime();
        phase2TimeMs = (tPhase2End - tPhase2Start) / 1_000_000;

        if (!phase2Found) return "";

        int total = phase1Length + phase2Length;
        // clear any trailing leftover entries beyond total to avoid stale data
        for (int i = total; i < solutionMoves.length; i++) { solutionMoves[i] = 0; solutionPowers[i] = 0; }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            sb.append(Moves.moveToString(solutionMoves[i], solutionPowers[i]));
            if (i < total - 1) sb.append(' ');
        }
        String sol = sb.toString();
        // verify assembled solution actually solves the start cube
        CubieCube test = new CubieCube(start);
        for (int i = 0; i < total; i++) test.applyMove(solutionMoves[i], solutionPowers[i]);
        if (!test.isSolved()) {
            System.err.println("TwoPhase produced non-solving sequence. phase1Length=" + phase1Length + " phase2Length=" + phase2Length + " total=" + total);
            System.err.println("SolutionMoves: " + Arrays.toString(Arrays.copyOf(solutionMoves, total)));
            System.err.println("SolutionPowers: " + Arrays.toString(Arrays.copyOf(solutionPowers, total)));

            // Diagnostic: compare MoveTables-predicted coords vs actual Cube after each move
            int pco = start.getCornerOriCoord();
            int peo = start.getEdgeOriCoord();
            int psl = start.getUDSliceCoord();
            int pcp = start.getCornerPermCoord();
            CubieCube actual = new CubieCube(start);
            System.err.println("Step | move p | predCO predEO predSL predCP || actCO actEO actSL actCP");
            for (int i = 0; i < total; i++) {
                int mv = solutionMoves[i]; int pw = solutionPowers[i];
                pco = MoveTables.applyCO(mv, pw, pco);
                peo = MoveTables.applyEO(mv, pw, peo);
                psl = MoveTables.applySlice(mv, pw, psl);
                pcp = MoveTables.applyCP(mv, pw, pcp);
                actual.applyMove(mv, pw);
                int aco = actual.getCornerOriCoord();
                int aeo = actual.getEdgeOriCoord();
                int asl = actual.getUDSliceCoord();
                int acp = actual.getCornerPermCoord();
                System.err.printf("%3d | %d %d | %6d %6d %6d %6d || %6d %6d %6d %6d\n", i, mv, pw, pco, peo, psl, pcp, aco, aeo, asl, acp);
            }
            // print final check
            System.err.println("test.isSolved()=" + test.isSolved());
            System.err.print("final cp: "); for (int i=0;i<8;i++) System.err.print(test.cp[i] + " "); System.err.println();
            System.err.print("final co: "); for (int i=0;i<8;i++) System.err.print(test.co[i] + " "); System.err.println();
            System.err.print("final ep: "); for (int i=0;i<12;i++) System.err.print(test.ep[i] + " "); System.err.println();
            System.err.print("final eo: "); for (int i=0;i<12;i++) System.err.print(test.eo[i] + " "); System.err.println();
            return "";
        }
        return sol;
    }

    public long getPhase1TimeMs() { return phase1TimeMs; }
    public long getPhase2TimeMs() { return phase2TimeMs; }
    public int getPhase1Length() { return phase1Length; }
    public int[] getSolutionMovesArray() { return solutionMoves; }
    public int[] getSolutionPowersArray() { return solutionPowers; }

    // ---------------- heuristics using coord tables ----------------
    private int heuristicPhase1Coord(int co, int eo, int sl) {
        int h1 = PruningTables.coPrun[co];
        int h2 = PruningTables.eoPrun[eo];
        int h3 = PruningTables.slicePrun[sl];
        int h = Math.max(h1, Math.max(h2, h3));
        return (h < 0) ? 0 : h;
    }

    private int heuristicPhase2Coord(int cp, int sl) {
        int h1 = PruningTables.cpPrun[cp];
        int h2 = PruningTables.slicePrun[sl];
        int h = Math.max(h1, h2);
        return (h < 0) ? 0 : h;
    }

    // ---------------- phase-1 search using coordinates ----------------
    private boolean searchPhase1Coord(int co, int eo, int sl, int depth, int limit, int lastMove) {
        if (Thread.currentThread().isInterrupted()) return false;
        int h = heuristicPhase1Coord(co, eo, sl);
        if (depth + h > limit) return false;

        if (co == 0 && eo == 0 && sl == 0) {
            phase1Length = depth;
            return true;
        }

        // generate moves with heuristic ordering
        ArrayList<MoveChoice> choices = new ArrayList<>();
        for (int move = 0; move < 6; move++) {
            if (lastMove != -1 && Moves.sameAxis(lastMove, move)) continue;
            for (int p = 1; p <= 3; p++) {
                int nco = MoveTables.applyCO(move, p, co);
                int neo = MoveTables.applyEO(move, p, eo);
                int nsl = MoveTables.applySlice(move, p, sl);
                int nh = heuristicPhase1Coord(nco, neo, nsl);
                choices.add(new MoveChoice(move, p, nh));
            }
        }
        choices.sort(Comparator.comparingInt(a -> a.h));

        for (MoveChoice mc : choices) {
            if (Thread.currentThread().isInterrupted()) return false;
            int nco = MoveTables.applyCO(mc.move, mc.p, co);
            int neo = MoveTables.applyEO(mc.move, mc.p, eo);
            int nsl = MoveTables.applySlice(mc.move, mc.p, sl);
            solutionMoves[depth] = mc.move;
            solutionPowers[depth] = mc.p;
            if (searchPhase1Coord(nco, neo, nsl, depth + 1, limit, mc.move)) return true;
        }

        return false;
    }

    // ---------------- phase-2 search (restricted moves) using coordinates ----------------
    private boolean searchPhase2Coord(int cp, int sl, int depth, int limit, int lastMove) {
        if (Thread.currentThread().isInterrupted()) return false;
        int h = heuristicPhase2Coord(cp, sl);
        if (depth + h > limit) return false;

        if (cp == 0 && sl == 0) {
            phase2Length = depth;
            return true;
        }

        ArrayList<MoveChoice> choices = new ArrayList<>();
        for (int move = 0; move < 6; move++) {
            if (lastMove != -1 && Moves.sameAxis(lastMove, move)) continue;
            if (move == Moves.U || move == Moves.D) {
                for (int p = 1; p <= 3; p++) {
                    int ncp = MoveTables.applyCP(move, p, cp);
                    int nsl = MoveTables.applySlice(move, p, sl);
                    int nh = heuristicPhase2Coord(ncp, nsl);
                    choices.add(new MoveChoice(move, p, nh));
                }
            } else {
                int p = 2;
                int ncp = MoveTables.applyCP(move, p, cp);
                int nsl = MoveTables.applySlice(move, p, sl);
                int nh = heuristicPhase2Coord(ncp, nsl);
                choices.add(new MoveChoice(move, p, nh));
            }
        }
        choices.sort(Comparator.comparingInt(a -> a.h));

        for (MoveChoice mc : choices) {
            if (Thread.currentThread().isInterrupted()) return false;
            int ncp = MoveTables.applyCP(mc.move, mc.p, cp);
            int nsl = MoveTables.applySlice(mc.move, mc.p, sl);
            int idx = phase1Length + depth;
            solutionMoves[idx] = mc.move;
            solutionPowers[idx] = mc.p;
            if (searchPhase2Coord(ncp, nsl, depth + 1, limit, mc.move)) return true;
        }

        return false;
    }

    // ---------------- rest unchanged ----------------
    private int heuristicPhase1(CubieCube c) { return heuristicPhase1Coord(c.getCornerOriCoord(), c.getEdgeOriCoord(), c.getUDSliceCoord()); }
    private int heuristicPhase2(CubieCube c) { return heuristicPhase2Coord(c.getCornerPermCoord(), c.getUDSliceCoord()); }
    private static class MoveChoice { int move; int p; int h; MoveChoice(int m, int p, int h) { this.move = m; this.p = p; this.h = h; } }
}
