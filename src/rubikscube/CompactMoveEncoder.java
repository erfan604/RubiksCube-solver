package rubikscube;

// Translate moves into only clockwise notation
public final class CompactMoveEncoder {
    private CompactMoveEncoder() {}

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
                else repeat = 3;
            }
            for (int i = 0; i < repeat; i++) out.append(face);
        }
        return out.toString();
    }


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
