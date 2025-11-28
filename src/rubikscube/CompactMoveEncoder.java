package rubikscube;

/**
 * Converts move sequences into a compact format that uses only the letters
 * F, B, R, L, U, D with no spaces or suffixes.
 * Rules:
 *  - Quarter turn: single letter (e.g., "R")
 *  - Half turn: letter twice (e.g., "RR")
 *  - Prime/inverse: letter three times (e.g., "RRR")
 *
 * The solver never emits consecutive moves on the same face, so repeating
 * a letter per power stays unambiguous.
 */
public final class CompactMoveEncoder {
    private CompactMoveEncoder() {}

    /** Convert a program-format sequence ("U R2 F'") into compact letters only ("URRFFF"). */
    public static String programToCompact(String programSeq) {
        if (programSeq == null || programSeq.trim().isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        String[] toks = programSeq.trim().split("\\s+");
        for (String t : toks) {
            if (t.isEmpty()) continue;
            char face = Character.toUpperCase(t.charAt(0));
            int repeat = 1;
            if (t.length() >= 2) {
                char suffix = t.charAt(1);
                if (suffix == '2') repeat = 2;
                else repeat = 3; // treat anything else as inverse
            }
            for (int i = 0; i < repeat; i++) out.append(face);
        }
        return out.toString();
    }

    /**
     * Parse a compact sequence (letters only, no spaces) into move/power arrays.
     * Returns the number of moves parsed.
     */
    public static int parseCompact(String compactSeq, int[] mv, int[] pw) {
        if (compactSeq == null) return 0;
        String s = compactSeq.trim();
        if (s.isEmpty()) return 0;

        int idx = 0;
        int i = 0;
        while (i < s.length()) {
            char face = Character.toUpperCase(s.charAt(i));
            int move = faceToMove(face);
            int count = 1;
            i++;
            while (i < s.length() && Character.toUpperCase(s.charAt(i)) == face) {
                count++;
                i++;
            }
            int power = count % 4;
            if (move < 0 || power == 0) continue;
            if (mv != null && pw != null) {
                mv[idx] = move;
                pw[idx] = power;
            }
            idx++;
        }
        return idx;
    }

    /** Heuristic to detect if a string is in compact form (letters only, no digits/apostrophes/spaces). */
    public static boolean looksCompact(String seq) {
        if (seq == null) return false;
        String s = seq.trim();
        if (s.isEmpty()) return false;
        if (s.contains(" ") || s.contains("'") || s.matches(".*\\d.*")) return false;
        return s.matches("(?i)[URFDLB]+");
    }

    private static int faceToMove(char face) {
        return switch (face) {
            case 'U' -> Moves.U;
            case 'R' -> Moves.R;
            case 'F' -> Moves.F;
            case 'D' -> Moves.D;
            case 'L' -> Moves.L;
            case 'B' -> Moves.B;
            default -> -1;
        };
    }
}
