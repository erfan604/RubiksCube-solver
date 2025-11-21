package rubikscube;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies the saved solutions01-05 against scrambles01-05 by applying the moves
 * and reporting whether the cube is solved.
 */
public class VerifySolutionsRunner {
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 5; i++) {
            String scrambleFile = String.format("testcases/scramble%02d.txt", i);
            String solutionFile = String.format("solution%02d.txt", i);

            List<String> scrambleLines = Files.readAllLines(Path.of(scrambleFile));
            char[] facelets = Solver.parseNetForVerify(scrambleLines);
            CubieCube start = NetToCubie.fromFacelets(facelets);

            String solution = Files.readString(Path.of(solutionFile)).trim();
            int[] moves = new int[256];
            int[] pows = new int[256];
            int len = new ApplySequenceTest().parseSequencePublic(solution, moves, pows);

            CubieCube c = new CubieCube(start);
            for (int j = 0; j < len; j++) c.applyMove(moves[j], pows[j]);

            System.out.printf("%s + %s -> solved? %b%n", scrambleFile, solutionFile, c.isSolved());
        }
    }
}
