package rubikscube;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

public class RunDeepCheck {
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 5; i++) {
            String id = String.format("%02d", i);
            System.out.println("\n=== Solve scramble " + id + " ===");
            List<String> net = Files.readAllLines(Paths.get("testcases/scramble" + id + ".txt"));
            char[] facelets = Solver.parseNetForVerify(net);
            CubieCube cc = NetToCubie.fromFacelets(facelets);

            // We'll try TwoPhase first with a per-solve timeout, then fallback to SimpleIDA
            long timeoutSec = 20;
            String phase1Seq = "";
            String phase2Seq = "";
            String fullSeq = "";
            long phase1Time = -1;
            long phase2Time = -1;
            long fullTime = -1;

            ExecutorService ex = Executors.newSingleThreadExecutor();
            try {
                TwoPhaseIDA tp = new TwoPhaseIDA();
                Callable<String> call = () -> tp.solve(new CubieCube(cc));
                Future<String> fut = ex.submit(call);
                long tStart = System.nanoTime();
                try {
                    String sol = fut.get(timeoutSec, TimeUnit.SECONDS);
                    long tEnd = System.nanoTime();
                    fullTime = (tEnd - tStart) / 1_000_000; // ms

                    if (sol != null && !sol.isEmpty()) {
                        // Extract phase sequences from solution arrays
                        int p1len = tp.getPhase1Length();
                        int[] moves = tp.getSolutionMovesArray();
                        int[] powers = tp.getSolutionPowersArray();
                        phase1Seq = buildUserFromMoves(moves, powers, 0, p1len);
                        phase2Seq = buildUserFromMoves(moves, powers, p1len, tp.getPhase2TimeMs() >= 0 ? (sol.split("\\s+").length - p1len) : (moves.length - p1len));
                        // safer: use phase2 length from tp.phase2Length via probing: compute total by counting non-zero until end
                        int totalMoves = 0; for (int k=0;k<moves.length;k++) if (powers[k]!=0) totalMoves++; // approximate
                        int p2len = totalMoves - p1len;
                        phase2Seq = buildUserFromMoves(moves, powers, p1len, p2len);

                        phase1Time = tp.getPhase1TimeMs();
                        phase2Time = tp.getPhase2TimeMs();
                        fullSeq = buildUserFromMoves(moves, powers, 0, p1len + p2len);
                    } else {
                        // TwoPhase failed; mark as such
                        fullSeq = "";
                    }
                } catch (TimeoutException te) {
                    fut.cancel(true);
                    System.out.println("TwoPhase timed out after " + timeoutSec + "s");
                }
            } finally {
                ex.shutdownNow();
            }

            // If TwoPhase didn't produce a solution, try SimpleIDA (user-format returning solver)
            if (fullSeq == null || fullSeq.isEmpty()) {
                ExecutorService ex2 = Executors.newSingleThreadExecutor();
                try {
                    SimpleIDA s = new SimpleIDA();
                    Callable<String> call2 = () -> s.solve(new CubieCube(cc));
                    Future<String> fut2 = ex2.submit(call2);
                    long tStart2 = System.nanoTime();
                    try {
                        String userSol = fut2.get(timeoutSec, TimeUnit.SECONDS);
                        long tEnd2 = System.nanoTime();
                        fullTime = (tEnd2 - tStart2) / 1_000_000;
                        if (userSol != null) {
                            fullSeq = userSol;
                        }
                    } catch (TimeoutException te) {
                        fut2.cancel(true);
                        System.out.println("SimpleIDA timed out after " + timeoutSec + "s");
                    }
                } finally {
                    ex2.shutdownNow();
                }
            }

            // Prepare file output lines — only the requested items
            List<String> outLines = new ArrayList<>();
            outLines.add("PHASE1: " + (phase1Seq == null ? "" : phase1Seq));
            outLines.add("PHASE1_TIME_MS: " + (phase1Time >= 0 ? phase1Time : ""));
            outLines.add("PHASE2: " + (phase2Seq == null ? "" : phase2Seq));
            outLines.add("PHASE2_TIME_MS: " + (phase2Time >= 0 ? phase2Time : ""));
            outLines.add("FULL: " + (fullSeq == null ? "" : fullSeq));
            outLines.add("FULL_TIME_MS: " + (fullTime >= 0 ? fullTime : ""));

            // Print to terminal
            outLines.forEach(System.out::println);

            // Write to file out_runNN.txt
            Path outPath = Paths.get("out_run" + id + ".txt");
            Files.write(outPath, outLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    // Build a user-format sequence string from move/power arrays in range [start, start+len)
    private static String buildUserFromMoves(int[] moves, int[] powers, int start, int len) {
        if (moves == null || powers == null || len <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int end = Math.min(moves.length, start + len);
        for (int i = start; i < end; i++) {
            int mv = moves[i];
            int p = powers[i];
            if (p == 0) break;
            char face = Moves.MOVE_NAMES[mv].charAt(0);
            int outPower = p;
            if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                if (p == 1) outPower = 3; else if (p == 3) outPower = 1;
            }
            sb.append(face);
            if (outPower == 2) sb.append('2');
            else if (outPower == 3) sb.append('\'');
            if (i < end - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private static void applyProgram(CubieCube c, String seq) {
        if (seq == null || seq.isBlank()) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            int move = faceToMove(face);
            c.applyMove(move, power);
        }
    }

    private static void applyWithUserToggle(CubieCube c, String seq, boolean tokensAreUser) {
        if (seq == null || seq.isBlank()) return;
        String[] toks = seq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            if (tokensAreUser) {
                if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                    if (power == 1) power = 3; else if (power == 3) power = 1;
                }
            }
            int move = faceToMove(face);
            c.applyMove(move, power);
        }
    }

    private static String reverseInvert(String seq) {
        if (seq == null) return null;
        String[] toks = seq.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = toks.length - 1; i >= 0; i--) {
            String t = toks[i]; if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1; if (t.length() == 1) power = 1; else if (t.charAt(1) == '2') power = 2; else power = 3;
            int inv = (power == 2) ? 2 : ((power == 1) ? 3 : 1);
            sb.append(Moves.moveToString(faceToMove(face), inv));
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    private static String interpretAsUser(String progSeq) {
        // Convert program-format sequence into a program-format sequence that corresponds to user-format tokens
        if (progSeq == null) return null;
        String[] toks = progSeq.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i]; if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1; if (t.length() == 1) power = 1; else if (t.charAt(1) == '2') power = 2; else power = 3;
            // if original tokens were user, they would invert URFB; invert back to program here
            if (face == 'U' || face == 'R' || face == 'F' || face == 'B') {
                if (power == 1) power = 3; else if (power == 3) power = 1;
            }
            sb.append(Moves.moveToString(faceToMove(face), power));
            if (i < toks.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private static int faceToMove(char f) {
        switch (f) { case 'U': return Moves.U; case 'R': return Moves.R; case 'F': return Moves.F; case 'D': return Moves.D; case 'L': return Moves.L; case 'B': return Moves.B; }
        return -1;
    }

    private static void printCube(String label, CubieCube c) {
        System.out.println("--- " + label + " ---");
        System.out.print("cp: "); for (int i=0;i<8;i++) System.out.print(c.cp[i] + " "); System.out.println();
        System.out.print("co: "); for (int i=0;i<8;i++) System.out.print(c.co[i] + " "); System.out.println();
        System.out.print("ep: "); for (int i=0;i<12;i++) System.out.print(c.ep[i] + " "); System.out.println();
        System.out.print("eo: "); for (int i=0;i<12;i++) System.out.print(c.eo[i] + " "); System.out.println();
    }
}
