package rubikscube;

public class Util {

    public static int[] copyArray(int[] arr) {
        int[] out = new int[arr.length];
        System.arraycopy(arr, 0, out, 0, arr.length);
        return out;
    }

    public static byte[] copyArray(byte[] arr) {
        byte[] out = new byte[arr.length];
        System.arraycopy(arr, 0, out, 0, arr.length);
        return out;
    }

    public static boolean isSolved(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] != arr[0]) return false;
        }
        return true;
    }

    public static int[] concat(int[] a, int[] b) {
        int[] out = new int[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
