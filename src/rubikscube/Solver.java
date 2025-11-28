package rubikscube;

import java.nio.file.*;
import java.util.*;

public class Solver {

    /**
     * If args are provided: args[0]=scramble file, args[1]=output file.
     * Batch mode (no args): solves testcases/scramble01..40 into solutionXX.txt with no timeouts.
     */
    public static void main(String[] args) {
        MoveTables.init();
        // build light in-memory tables each run (no disk cache)
        LightPruningTables.buildAllBlocking();

        if (args.length < 2) {
            solveBatch();
            return;
        }

        String inFile = args[0];
        String outFile = args[1];

        try {
            List<String> lines = Files.readAllLines(Paths.get(inFile));
            char[] facelets = parseNet(lines);
            CubieCube cc = NetToCubie.fromFacelets(facelets);
            long t0 = System.nanoTime();
            String sol = solveOne(cc);
            double elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0;
            String user = (sol == null || sol.isEmpty()) ? "" : programToCompact(sol);
            Files.write(Paths.get(outFile), Arrays.asList(user));
            String label = Paths.get(inFile).getFileName().toString();
            System.out.printf("%s %s in %.3f seconds%n", label, user.isEmpty() ? "unsolved" : "solved", elapsedSec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Batch harness: testcases/scramble01..40 -> solutionXX.txt, no timeout limit
    private static void solveBatch() {
        for (int i = 1; i <= 40; i++) {
            long t0 = System.nanoTime();
            String scrambleFile = String.format("testcases/scramble%02d.txt", i);
            String outFile = String.format("solution%02d.txt", i);
            String userSolution = "";
            try {
                List<String> lines = Files.readAllLines(Paths.get(scrambleFile));
                CubieCube cc = NetToCubie.fromFacelets(parseNetForVerify(lines));
                String prog = new TwoPhaseIDA().solve(new CubieCube(cc));
                if (prog != null && !prog.isEmpty()) userSolution = programToCompact(prog);
            } catch (Exception e) {
                userSolution = "";
            }
            double elapsedSec = (System.nanoTime() - t0) / 1_000_000_000.0;
            try {
                Files.write(Paths.get(outFile), Arrays.asList(userSolution));
            } catch (Exception ignored) { }
            System.out.printf("scramble%02d %s in %.3f seconds%n", i, userSolution.isEmpty() ? "unsolved" : "solved", elapsedSec);
        }
    }

    // Solve a single cube with no timeout; direct TwoPhase search
    private static String solveOne(CubieCube cc) {
        TwoPhaseIDA twoPhase = new TwoPhaseIDA();
        String sol = twoPhase.solve(new CubieCube(cc));
        return sol == null ? "" : sol;
    }


    // convert program-format seq to user-format (no special inversion)
    private static String programToUser(String seq) {
        if (seq == null) return "";
        StringBuilder out = new StringBuilder();
        String[] toks = seq.trim().split("\\s+");
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i];
            if (t.isEmpty()) continue;
            char face = t.charAt(0);
            int power = 1;
            if (t.length() == 1) power = 1;
            else if (t.charAt(1) == '2') power = 2;
            else power = 3;
            out.append(face);
            if (power == 2) out.append('2');
            else if (power == 3) out.append('\'');
            if (i < toks.length - 1) out.append(' ');
        }
        return out.toString();
    }

    // convert program-format sequence to compact letters-only (no spaces, no suffixes)
    private static String programToCompact(String seq) {
        return CompactMoveEncoder.programToCompact(seq);
    }

    // Public helpers used by other test/driver classes
    public static char[] parseNetForVerify(List<String> n) { return parseNet(n); }
    public static String programToUserPublic(String seq) { return programToUser(seq); }
    public static String programToCompactPublic(String seq) { return programToCompact(seq); }

    private static char[] parseNet(List<String> n) {
        char[] f = new char[54];

        // U
        for (int r = 0; r < 3; r++) {
            String line = n.get(r).strip();
            f[r*3+0]=line.charAt(0);
            f[r*3+1]=line.charAt(1);
            f[r*3+2]=line.charAt(2);
        }

        // middle rows
        for (int r = 0; r < 3; r++) {
            String raw = n.get(3+r).replaceAll("\\s+", "");

            f[9 + r*3 + 0] = raw.charAt(0);
            f[9 + r*3 + 1] = raw.charAt(1);
            f[9 + r*3 + 2] = raw.charAt(2);

            f[18 + r*3 + 0] = raw.charAt(3);
            f[18 + r*3 + 1] = raw.charAt(4);
            f[18 + r*3 + 2] = raw.charAt(5);

            f[27 + r*3 + 0] = raw.charAt(6);
            f[27 + r*3 + 1] = raw.charAt(7);
            f[27 + r*3 + 2] = raw.charAt(8);

            f[36 + r*3 + 0] = raw.charAt(9);
            f[36 + r*3 + 1] = raw.charAt(10);
            f[36 + r*3 + 2] = raw.charAt(11);
        }

        // D
        for (int r = 0; r < 3; r++) {
            String line = n.get(6+r).strip();
            f[45 + r*3+0]=line.charAt(0);
            f[45 + r*3+1]=line.charAt(1);
            f[45 + r*3+2]=line.charAt(2);
        }

        return f;
    }
}
