package rubikscube;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SequenceTranslator {
    // Mapping discovered: user->program: U->U(inv), R->R(inv), F->F(inv), D->D, L->L, B->B(inv)
    // So program->user is same faces, with inversion flags for U,R,F,B

    private static final char[] faces = {'U','R','F','D','L','B'};
    private static final boolean[] invertedUserFace = {true, true, true, false, false, true};

    private static int faceIndex(char f) {
        for (int i = 0; i < faces.length; i++) if (faces[i] == f) return i;
        return -1;
    }

    private static String translate(String seq) {
        StringBuilder out = new StringBuilder();
        String[] tokens = seq.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.length() == 0) continue;
            char f = t.charAt(0);
            int idx = faceIndex(f);
            if (idx == -1) {
                out.append(t);
            } else {
                int power = 1;
                if (t.length() == 1) power = 1;
                else if (t.charAt(1) == '2') power = 2;
                else power = 3;

                // If face is inverted in user convention, invert power
                if (invertedUserFace[idx]) {
                    if (power == 1) power = 3;
                    else if (power == 3) power = 1;
                }

                out.append(f);
                if (power == 2) out.append('2');
                else if (power == 3) out.append('\'');

                if (i < tokens.length - 1) out.append(' ');
            }
        }
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        // Read solver outputs for scramble01 and scramble02
        String s1 = Files.readString(Paths.get("out01.txt")).trim();
        String s2 = Files.readString(Paths.get("out02.txt")).trim();

        System.out.println("solver out01: '" + s1 + "'");
        System.out.println("translated: '" + translate(s1) + "'");

        System.out.println("solver out02: '" + s2 + "'");
        System.out.println("translated: '" + translate(s2) + "'");

        // test by applying translated sequences
        System.out.println("\nTesting application on scrambles:");
        java.lang.Runtime.getRuntime().exec(new String[]{"/bin/sh","-c","java -cp src rubikscube.ApplyAndCheck testcases/scramble01.txt \"" + translate(s1) + "\""}).waitFor();
        java.lang.Runtime.getRuntime().exec(new String[]{"/bin/sh","-c","java -cp src rubikscube.ApplyAndCheck testcases/scramble02.txt \"" + translate(s2) + "\""}).waitFor();
        System.out.println("Done tests (see ApplyAndCheck printed output above).");
    }
}
