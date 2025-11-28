package rubikscube;

import java.util.*;

public class TwoPhaseIDA {

    private static final int MAX_DEPTH = 45;
    private static final int SLICE_SOLVED = CubieCube.SLICE_SOLVED_COORD;

    private int[] solutionMoves = new int[ MAX_DEPTH * 2 ];
    private int[] solutionPowers = new int[ MAX_DEPTH * 2 ];
    private int phase1Length;
    private int phase2Length;

    private CubieCube startCube = null;

    public static boolean BLOCK_OPPOSITE_IN_PHASE2 = false;

    public String solve(CubieCube start) {

        MoveTables.init();
        LightPruningTables.buildAllBlocking();


        this.startCube = new CubieCube(start);

        if (start.isSolved()) return "";

        int startCO = start.getCornerOriCoord();
        int startEO = start.getEdgeOriCoord();
        int startSL = start.getUDSliceCoord();

        // Phase-1 iterative deepening on CO/EO/SLICE
        boolean phase1Found = false;
        int h1Start = heuristicPhase1Coord(startCO, startEO, startSL);
        for (int depth1 = h1Start; depth1 <= MAX_DEPTH; depth1++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (searchPhase1Coord(startCO, startEO, startSL, 0, depth1, -1)) {
                phase1Found = true;
                break;
            }
        }

        if (!phase1Found) return "";


        CubieCube mid = new CubieCube(start);
        for (int i = 0; i < phase1Length; i++) mid.applyMove(solutionMoves[i], solutionPowers[i]);

        // Phase-2 iterative deepening on CP/SLICE with restricted moves (U/D any, others half-turn)
        boolean phase2Found = false;
        phase2Length = 0;
        int midCP = mid.getCornerPermCoord();
        int midSL = mid.getUDSliceCoord();
        int midUD = mid.getUDEdgePermCoord();
        int midUE = mid.getUEdgePermCoord();
        int midDE = mid.getDEdgePermCoord();
        int h2Start = heuristicPhase2Coord(midCP, midSL, midUD, midUE, midDE);
        for (int depth2 = h2Start; depth2 <= MAX_DEPTH; depth2++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (searchPhase2Coord(midCP, midSL, midUD, midUE, midDE, 0, depth2, -1)) {
                phase2Found = true;
                break;
            }
        }

        if (!phase2Found) {
            return "";
        }

        int total = phase1Length + phase2Length;

        for (int i = total; i < solutionMoves.length; i++) { solutionMoves[i] = 0; solutionPowers[i] = 0; }


        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            sb.append(Moves.moveToString(solutionMoves[i], solutionPowers[i]));
            if (i < total - 1) sb.append(' ');
        }
        String sol = sb.toString();

        CubieCube test = new CubieCube(start);
        for (int i = 0; i < total; i++) test.applyMove(solutionMoves[i], solutionPowers[i]);
        if (!test.isSolved()) return "";

        return sol;
    }

    public int getPhase1Length() { return phase1Length; }
    public int getPhase2Length() { return phase2Length; }
    public int[] getSolutionMovesArray() { return solutionMoves; }
    public int[] getSolutionPowersArray() { return solutionPowers; }

    private int heuristicPhase1Coord(int co, int eo, int sl) {
        int hCo = LightPruningTables.coSlicePrun[co * LightPruningTables.N_SLICE + sl];
        int hEo = LightPruningTables.eoSlicePrun[eo * LightPruningTables.N_SLICE + sl];
        if (hCo < 0) hCo = 0;
        if (hEo < 0) hEo = 0;

        return Math.max(hCo, hEo);
    }

    private int heuristicPhase2Coord(int cp, int sl, int udEp, int ue, int de) {

        int parity = LightPruningTables.permParityFromCoord(udEp) & 1;
        int hCp = LightPruningTables.cpPrunP2[cp];
        int hCpSliceParity = LightPruningTables.cpUdSlicePrunP2[((cp * LightPruningTables.N_SLICE) + sl) * 2 + parity];
        int hCpParity = LightPruningTables.cpUdParityPrun[cp * 2 + parity];
        int hUd = LightPruningTables.udPrunP2[udEp];
        int hUe = LightPruningTables.uEdgePrun[ue];
        int hDe = LightPruningTables.dEdgePrun[de];

        if (hCp < 0) hCp = 0;
        if (hCpSliceParity < 0) hCpSliceParity = 0;
        if (hCpParity < 0) hCpParity = 0;
        if (hUd < 0) hUd = 0;
        if (hUe < 0) hUe = 0;
        if (hDe < 0) hDe = 0;
        int combo = Math.max(Math.max(hCpSliceParity, hCpParity), hCp);
        return Math.max(Math.max(combo, hUd), Math.max(hUe, hDe));
    }

    // Phase-1 search using coordinates
    private boolean searchPhase1Coord(int co, int eo, int sl, int depth, int limit, int lastMove) {
        if (Thread.currentThread().isInterrupted()) return false;
        int h = heuristicPhase1Coord(co, eo, sl);
        if (depth + h > limit) return false;

        if (co == 0 && eo == 0 && sl == SLICE_SOLVED) {
            phase1Length = depth;
            return true;
        }

        // generate moves with heuristic ordering
        ArrayList<MoveChoice> choices = new ArrayList<>();
        for (int move = 0; move < 6; move++) {
            // relaxed: only block turning the same face twice in a row
            if (lastMove >= 0 && Moves.sameAxis(lastMove, move)) continue;
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

    // Phase-2 search (restricted moves) using coordinates
    private boolean searchPhase2Coord(int cp, int sl, int udEp, int ue, int de, int depth, int limit, int lastMove) {
        if (Thread.currentThread().isInterrupted()) return false;
        int h = heuristicPhase2Coord(cp, sl, udEp, ue, de);
        if (depth + h > limit) return false;

         if (cp == 0 && sl == SLICE_SOLVED && udEp == 0 && ue == 0 && de == 0) {
            // Assemble full solution (phase1 + this phase2 prefix) and verify final cube
            // phase2 moves occupy indices [phase1Length .. phase1Length + depth - 1]
            CubieCube assembled = new CubieCube(startCube);
            for (int i = 0; i < phase1Length; i++) assembled.applyMove(solutionMoves[i], solutionPowers[i]);
            for (int i = 0; i < depth; i++) {
                int idx = phase1Length + i;
                assembled.applyMove(solutionMoves[idx], solutionPowers[idx]);
            }
            if (!assembled.isSolved()) {
                // do not accept this candidate; continue searching
                return false;
            }
            phase2Length = depth;
            return true;
        }

        ArrayList<MoveChoice> choices = new ArrayList<>();
        int baseH = h;
        for (int move = 0; move < 6; move++) {
            if (lastMove >= 0) {
                if (BLOCK_OPPOSITE_IN_PHASE2) {
                    if (Moves.blockPhase2Follow(lastMove, move)) continue;
                } else if (lastMove == move) continue;
            }
            boolean isUD = (move == Moves.U || move == Moves.D);
            for (int p = 1; p <= 3; p++) {
                if (!isUD && p != 2) continue; // restrict R/L/F/B to half-turns
                int ncp = MoveTables.applyCP(move, p, cp);
                int nsl = MoveTables.applySlice(move, p, sl);
                int nud = MoveTables.applyUDEP(move, p, udEp);
                int nue = MoveTables.applyUEdge(move, p, ue);
                int nde = MoveTables.applyDEdge(move, p, de);
                int nh = heuristicPhase2Coord(ncp, nsl, nud, nue, nde);
                int delta = baseH - nh;
                boolean preferUD = (move == Moves.U || move == Moves.D);
                choices.add(new MoveChoice(move, p, nh, delta, preferUD));
            }
        }
        choices.sort((a,b) -> {
            if (a.h != b.h) return Integer.compare(a.h, b.h);
            if (a.delta != b.delta) return Integer.compare(b.delta, a.delta);
            if (a.preferUD != b.preferUD) return Boolean.compare(b.preferUD, a.preferUD);
            if (a.move != b.move) return Integer.compare(a.move, b.move);
            return Integer.compare(a.p, b.p);
        });

        for (MoveChoice mc : choices) {
            if (Thread.currentThread().isInterrupted()) return false;
            int ncp = MoveTables.applyCP(mc.move, mc.p, cp);
            int nsl = MoveTables.applySlice(mc.move, mc.p, sl);
            int nud = MoveTables.applyUDEP(mc.move, mc.p, udEp);
            int nue = MoveTables.applyUEdge(mc.move, mc.p, ue);
            int nde = MoveTables.applyDEdge(mc.move, mc.p, de);
            int idx = phase1Length + depth;
            solutionMoves[idx] = mc.move;
            solutionPowers[idx] = mc.p;
            if (searchPhase2Coord(ncp, nsl, nud, nue, nde, depth + 1, limit, mc.move)) return true;
        }

        return false;
    }

    private static class MoveChoice { int move; int p; int h; int delta; boolean preferUD; MoveChoice(int m, int p, int h) { this.move = m; this.p = p; this.h = h; this.delta = 0; this.preferUD = false; } MoveChoice(int m, int p, int h, int delta, boolean preferUD) { this.move = m; this.p = p; this.h = h; this.delta = delta; this.preferUD = preferUD; } }
}
