package rubikscube;

import java.util.*;

public class TwoPhaseIDA {

    private static final int MAX_DEPTH = 45;
    private static final int SLICE_SOLVED = CubieCube.SLICE_SOLVED_COORD;

    private int[] solutionMoves = new int[ MAX_DEPTH * 2 ];
    private int[] solutionPowers = new int[ MAX_DEPTH * 2 ];
    private int phase1Length;
    private int phase2Length;
    private Map<Long, Integer> phase2Visited = new HashMap<>();

    private CubieCube startCube = null;

    private long phase1TimeMs = 0;
    private long phase2TimeMs = 0;

    public static boolean TRACE = false;
    public static boolean QUIET = true;

    public static boolean IGNORE_EO_IN_H1 = false;
    public static boolean IGNORE_SLICE_IN_H1 = false;

    public static boolean LOG_PHASE2_FAILS = false;
    public static boolean LOG_SOLUTION_DETAILS = false;
    public static boolean USE_PHASE2_VISITED = false;
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
        long tPhase1Start = System.nanoTime();
        boolean phase1Found = false;
        int h1Start = heuristicPhase1Coord(startCO, startEO, startSL);
        if (!QUIET) System.err.println("TwoPhase: h1Start=" + h1Start);
        for (int depth1 = h1Start; depth1 <= MAX_DEPTH; depth1++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (searchPhase1Coord(startCO, startEO, startSL, 0, depth1, -1)) {
                phase1Found = true;
                break;
            }
        }
        long tPhase1End = System.nanoTime();
        phase1TimeMs = (tPhase1End - tPhase1Start) / 1_000_000L;

        if (!phase1Found) return "";


        CubieCube mid = new CubieCube(start);
        for (int i = 0; i < phase1Length; i++) mid.applyMove(solutionMoves[i], solutionPowers[i]);

        // Phase-2 iterative deepening on CP/SLICE with restricted moves (U/D any, others half-turn)
        long tPhase2Start = System.nanoTime();
        boolean phase2Found = false;
        phase2Length = 0;
        phase2Visited.clear();
        int midCP = mid.getCornerPermCoord();
        int midSL = mid.getUDSliceCoord();
        int midUD = mid.getUDEdgePermCoord();
        int midUE = mid.getUEdgePermCoord();
        int midDE = mid.getDEdgePermCoord();
        int h2Start = heuristicPhase2Coord(midCP, midSL, midUD, midUE, midDE);
        if (!QUIET) System.err.println("TwoPhase: h2Start=" + h2Start + " after phase1Length=" + phase1Length);
        for (int depth2 = h2Start; depth2 <= MAX_DEPTH; depth2++) {
            if (Thread.currentThread().isInterrupted()) break;
            if (searchPhase2Coord(midCP, midSL, midUD, midUE, midDE, 0, depth2, -1)) {
                phase2Found = true;
                break;
            }
        }
        long tPhase2End = System.nanoTime();
        phase2TimeMs = (tPhase2End - tPhase2Start) / 1_000_000L;

        if (!phase2Found) {
            if (!QUIET) {

                System.err.println("TwoPhase: phase-2 failed — dumping mid-state after phase-1");
                dumpMidState(mid);
                System.err.println("Phase1Length=" + phase1Length + " heuristicP2Start=" + h2Start);
            }
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

        if (LOG_SOLUTION_DETAILS) {
            try {
                System.err.println("TwoPhase: program-format solution: " + sol);
                String userForm = Solver.programToUserPublic(sol);
                System.err.println("TwoPhase: user-format solution: " + userForm);

                StringBuilder rev = new StringBuilder();
                for (int i = total - 1; i >= 0; i--) {
                    int mv = solutionMoves[i]; int pw = solutionPowers[i];
                    int invPw = (pw == 2) ? 2 : (pw == 1 ? 3 : 1);
                    if (rev.length() > 0) rev.append(' ');
                    rev.append(Moves.moveToString(mv, invPw));
                }
                System.err.println("TwoPhase: reversed+inverted program-format: " + rev.toString());
                System.err.println("TwoPhase: reversed+inverted user-format: " + Solver.programToUserPublic(rev.toString()));
            } catch (Throwable t) { /* ignore */ }
        }

        CubieCube test = new CubieCube(start);
        for (int i = 0; i < total; i++) test.applyMove(solutionMoves[i], solutionPowers[i]);
        if (!test.isSolved()) {
            if (LOG_PHASE2_FAILS) {

                System.err.println("TwoPhase assembled solution failed final verification — dumping diagnostics");
                System.err.println("phase1Length=" + phase1Length + " phase2Length=" + phase2Length + " total=" + total);
                System.err.println("SolutionMoves: " + Arrays.toString(Arrays.copyOf(solutionMoves, total)));
                System.err.println("SolutionPowers: " + Arrays.toString(Arrays.copyOf(solutionPowers, total)));

                int pco = start.getCornerOriCoord();
                int peo = start.getEdgeOriCoord();
                int psl = start.getUDSliceCoord();
                int pcp = start.getCornerPermCoord();
                CubieCube actual = new CubieCube(start);

                System.err.println("Step | mv pw | predCO predEO predSL predCP || actCO actEO actSL actCP");
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
                    System.err.printf("%3d | %2d %1d | %6d %6d %6d %6d || %6d %6d %6d %6d\n", i, mv, pw, pco, peo, psl, pcp, aco, aeo, asl, acp);
                }

                System.err.println("Final assembled cube differs:");
                System.err.print("final cp: "); for (int i=0;i<8;i++) System.err.print(test.cp[i] + " "); System.err.println();
                System.err.print("final co: "); for (int i=0;i<8;i++) System.err.print(test.co[i] + " "); System.err.println();
                System.err.print("final ep: "); for (int i=0;i<12;i++) System.err.print(test.ep[i] + " "); System.err.println();
                System.err.print("final eo: "); for (int i=0;i<12;i++) System.err.print(test.eo[i] + " "); System.err.println();

                System.err.println("Mid-cube (after phase1):");
                CubieCube midDump = new CubieCube(start);
                for (int i = 0; i < phase1Length; i++) midDump.applyMove(solutionMoves[i], solutionPowers[i]);
                System.err.print("mid cp: "); for (int i=0;i<8;i++) System.err.print(midDump.cp[i] + " "); System.err.println();
                System.err.print("mid co: "); for (int i=0;i<8;i++) System.err.print(midDump.co[i] + " "); System.err.println();
                System.err.print("mid ep: "); for (int i=0;i<12;i++) System.err.print(midDump.ep[i] + " "); System.err.println();
                System.err.print("mid eo: "); for (int i=0;i<12;i++) System.err.print(midDump.eo[i] + " "); System.err.println();
            }

            return "";
        }

        return sol;
    }

    public long getPhase1TimeMs() { return phase1TimeMs; }
    public long getPhase2TimeMs() { return phase2TimeMs; }
    public int getPhase1Length() { return phase1Length; }
    public int getPhase2Length() { return phase2Length; }
    public int[] getSolutionMovesArray() { return solutionMoves; }
    public int[] getSolutionPowersArray() { return solutionPowers; }

    private int heuristicPhase1Coord(int co, int eo, int sl) {
        int hCo = IGNORE_EO_IN_H1 ? 0 : LightPruningTables.coSlicePrun[co * LightPruningTables.N_SLICE + sl];
        int hEo = IGNORE_EO_IN_H1 ? 0 : LightPruningTables.eoSlicePrun[eo * LightPruningTables.N_SLICE + sl];
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
        if (TRACE) System.err.println("P1: depth="+depth+" limit="+limit+" h="+h+" co="+co+" eo="+eo+" sl="+sl);
        if (depth + h > limit) {
            if (TRACE) System.err.println("P1: PRUNED at depth="+depth+" (d+h="+(depth+h)+">"+limit+")");
            return false;
        }

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
        if (TRACE) {
            System.err.print("P1 choices at depth="+depth+": ");
            for (MoveChoice c: choices) System.err.print(Moves.moveToString(c.move,c.p)+"(h="+c.h+") ");
            System.err.println();
        }

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
        if (TRACE) System.err.println("P2: depth="+depth+" limit="+limit+" h="+h+" cp="+cp+" sl="+sl+" udEp="+udEp);
        if (depth + h > limit) {
            if (TRACE) System.err.println("P2: PRUNED at depth="+depth+" (d+h="+(depth+h)+">"+limit+")");
            return false;
        }

        if (USE_PHASE2_VISITED) {
            // Include lastMove in the key to respect move-pruning that depends on prior move.
            long key = (((((long)cp * LightPruningTables.N_SLICE + sl) * LightPruningTables.N_UD_EP + udEp) * 24 + ue) * 24 + de) * 7L + (lastMove + 1);
            Integer bestDepth = phase2Visited.get(key);
            if (bestDepth != null && bestDepth <= depth) return false;
            phase2Visited.put(key, depth);
        }

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
                if (LOG_PHASE2_FAILS) {
                    // Dump diagnostics for this coord-consistent but assembled-invalid candidate
                    System.err.println("TwoPhaseIDA: coord-consistent candidate failed assembled verification (phase2 depth=" + depth + ")");
                    System.err.println("phase1Length=" + phase1Length + " currentDepth=" + depth + " limit=" + limit);
                    System.err.print("mid cp: "); CubieCube midDump = new CubieCube(startCube); for (int i=0;i<phase1Length;i++){ midDump.applyMove(solutionMoves[i], solutionPowers[i]); } for (int j=0;j<8;j++) System.err.print(midDump.cp[j] + " "); System.err.println();
                    System.err.print("mid co: "); for (int j=0;j<8;j++) System.err.print(midDump.co[j] + " "); System.err.println();
                    System.err.print("mid ep: "); for (int j=0;j<12;j++) System.err.print(midDump.ep[j] + " "); System.err.println();
                    System.err.print("mid eo: "); for (int j=0;j<12;j++) System.err.print(midDump.eo[j] + " "); System.err.println();
                    System.err.print("candidate phase2 moves: "); for (int i=0;i<depth;i++){ int idx=phase1Length+i; System.err.print(Moves.moveToString(solutionMoves[idx], solutionPowers[idx]) + " "); } System.err.println();
                    System.err.print("assembled cp: "); for (int j=0;j<8;j++) System.err.print(assembled.cp[j] + " "); System.err.println();
                    System.err.print("assembled co: "); for (int j=0;j<8;j++) System.err.print(assembled.co[j] + " "); System.err.println();
                    System.err.print("assembled ep: "); for (int j=0;j<12;j++) System.err.print(assembled.ep[j] + " "); System.err.println();
                    System.err.print("assembled eo: "); for (int j=0;j<12;j++) System.err.print(assembled.eo[j] + " "); System.err.println();
                }
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


    // diagnostic helper to print coords and raw arrays for the mid state
    private void dumpMidState(CubieCube mid) {
        System.err.println("  mid cp: " + mid.getCornerPermCoord() + " sl: " + mid.getUDSliceCoord() + " ud: " + mid.getUDEdgePermCoord() + " ue: " + mid.getUEdgePermCoord() + " de: " + mid.getDEdgePermCoord());
        System.err.print("  mid cp arr: "); for (int i=0;i<8;i++) System.err.print(mid.cp[i] + " "); System.err.println();
        System.err.print("  mid co arr: "); for (int i=0;i<8;i++) System.err.print(mid.co[i] + " "); System.err.println();
        System.err.print("  mid ep arr: "); for (int i=0;i<12;i++) System.err.print(mid.ep[i] + " "); System.err.println();
        System.err.print("  mid eo arr: "); for (int i=0;i<12;i++) System.err.print(mid.eo[i] + " "); System.err.println();
    }
    private static class MoveChoice { int move; int p; int h; int delta; boolean preferUD; MoveChoice(int m, int p, int h) { this.move = m; this.p = p; this.h = h; this.delta = 0; this.preferUD = false; } MoveChoice(int m, int p, int h, int delta, boolean preferUD) { this.move = m; this.p = p; this.h = h; this.delta = delta; this.preferUD = preferUD; } }
}
