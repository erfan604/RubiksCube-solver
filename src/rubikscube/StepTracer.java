package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class StepTracer {
    private static void printCubie(String label, CubieCube c) {
        System.out.println(label);
        System.out.print("cp: "); for (int i=0;i<8;i++) System.out.print(c.cp[i]+" "); System.out.println();
        System.out.print("co: "); for (int i=0;i<8;i++) System.out.print(c.co[i]+" "); System.out.println();
        System.out.print("ep: "); for (int i=0;i<12;i++) System.out.print(c.ep[i]+" "); System.out.println();
        System.out.print("eo: "); for (int i=0;i<12;i++) System.out.print(c.eo[i]+" "); System.out.println();
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java StepTracer <scrambleFile> <sequence>");
            return;
        }
        List<String> lines = Files.readAllLines(Paths.get(args[0]));
        char[] f = ParserTester.parseNetOnly(lines);
        CubieCube c = NetToCubie.fromFacelets(f);

        System.out.println("Initial facelets:");
        for (int i=0;i<54;i++) {
            System.out.print(f[i]);
            if ((i+1)%3==0) System.out.print(' ');
            if ((i+1)%9==0) System.out.println();
        }
        System.out.println();

        printCubie("Initial cubie:", c);

        String seq = args[1];
        String[] tokens = seq.split("\\s+");
        for (int i=0;i<tokens.length;i++) {
            String t = tokens[i];
            int move=-1, power=1;
            char face = t.charAt(0);
            switch(face) {
                case 'U': move = Moves.U; break;
                case 'R': move = Moves.R; break;
                case 'F': move = Moves.F; break;
                case 'D': move = Moves.D; break;
                case 'L': move = Moves.L; break;
                case 'B': move = Moves.B; break;
                default: throw new IllegalArgumentException("Unknown face: " + face);
            }
            if (t.length()==1) power=1;
            else if (t.charAt(1)=='2') power=2;
            else if (t.charAt(1)=='\'') power=3;
            else power=1;

            for (int p=0;p<power;p++) c.applyMove(move,1);
            printCubie("After " + t + ":", c);
        }

        System.out.println("Final isSolved=" + c.isSolved());
    }
}
