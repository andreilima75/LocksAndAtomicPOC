package org.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocksAndAtomicPOC {

    static void main() throws InterruptedException {
        int numThreads = 10;
        int incrementsPerThread = 100_000;

        testCounter(new UnsafeCounter(), numThreads, incrementsPerThread, "Unsafe Counter");
        testCounter(new SyncCounter(), numThreads, incrementsPerThread, "Synchronized Counter");

    }

    private static void testCounter(Object counter, int numThreads, int increments, String name)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long start = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < increments; j++) {
                    if (counter instanceof UnsafeCounter u) u.increment();
                    else if (counter instanceof SyncCounter s) s.increment();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long duration = (System.nanoTime() - start) / 1_000_000;

        int finalCount = 0;
        if (counter instanceof UnsafeCounter u) finalCount = u.getCount();
        else if (counter instanceof SyncCounter s) finalCount = s.getCount();

        long expected = (long) numThreads * increments;

        System.out.printf("%-25s | Final: %d | Esperado: %d | %s | Tempo: %d ms%n",
                name,
                finalCount,
                expected,
                (finalCount == expected) ? "OK" : "Race Condition",
                duration);
    }

    static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    static class SyncCounter {
        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }
}
