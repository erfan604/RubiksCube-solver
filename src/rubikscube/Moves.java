package rubikscube;

public class Moves {

    public static final int U = 0;
    public static final int R = 1;
    public static final int F = 2;
    public static final int D = 3;
    public static final int L = 4;
    public static final int B = 5;

    // Move names for output
    public static final String[] MOVE_NAMES = {
            "U", "R", "F", "D", "L", "B"
    };

    // Inverse moves for pruning
    public static final int[] INV_MOVE = {
            3, 4, 5, 0, 1, 2
    };

    // For phase-1 and phase-2 restrictions
    public static boolean sameAxis(int a, int b) {
        if (a == U || a == D) return (b == U || b == D);
        if (a == R || a == L) return (b == R || b == L);
        return (b == F || b == B);
    }

    // Generate the move string (simple)
    public static String moveToString(int move, int power) {
        switch (power) {
            case 1: return MOVE_NAMES[move];
            case 2: return MOVE_NAMES[move] + "2";
            default: return MOVE_NAMES[move] + "'";
        }
    }
}
