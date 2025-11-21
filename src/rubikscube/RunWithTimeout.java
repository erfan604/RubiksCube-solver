package rubikscube;

import java.util.concurrent.*;

public class RunWithTimeout {
    public static void main(String[] args) {
        System.out.println("Running TestSuite with 20s timeout...");
        runWithTimeout(() -> {
            try { TestSuite.main(new String[0]); } catch (Exception e) { throw new RuntimeException(e); }
        }, 20, TimeUnit.SECONDS, "TestSuite");

        System.out.println("Running DebugApply with 20s timeout...");
        runWithTimeout(() -> {
            try { DebugApply.main(new String[0]); } catch (Exception e) { throw new RuntimeException(e); }
        }, 20, TimeUnit.SECONDS, "DebugApply");

        System.out.println("Done.");
    }

    private static void runWithTimeout(Runnable task, long timeout, TimeUnit unit, String name) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(task);
        try {
            f.get(timeout, unit);
            System.out.println(name + " completed within timeout.");
        } catch (TimeoutException te) {
            f.cancel(true);
            System.out.println(name + " timed out after " + timeout + " " + unit.toString().toLowerCase());
        } catch (InterruptedException ie) {
            f.cancel(true);
            System.out.println(name + " interrupted.");
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            System.out.println(name + " threw exception: " + ee.getCause());
        } finally {
            ex.shutdownNow();
        }
    }
}
