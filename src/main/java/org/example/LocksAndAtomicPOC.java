package org.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LocksAndAtomicPOC {

    static void main() throws InterruptedException {
        int numThreads = 10;
        int incrementsPerThread = 100_000;

        testCounter(new UnsafeCounter(), numThreads, incrementsPerThread, "Unsafe Counter");
        testCounter(new SyncCounter(), numThreads, incrementsPerThread, "Synchronized Counter");
        testCounter(new LockCounter(), numThreads, incrementsPerThread, "ReentrantLock Counter");
        testCounter(new AtomicCounter(), numThreads, incrementsPerThread, "AtomicInteger Counter");

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
                    else if (counter instanceof LockCounter l) l.increment();
                    else if (counter instanceof AtomicCounter a) a.increment();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long duration = (System.nanoTime() - start) / 1_000_000;

        int finalCount = 0;
        if (counter instanceof UnsafeCounter u) finalCount = u.getCount();
        else if (counter instanceof SyncCounter s) finalCount = s.getCount();
        else if (counter instanceof LockCounter l) finalCount = l.getCount();
        else if (counter instanceof AtomicCounter a) finalCount = a.getCount();

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

    static class LockCounter {
        private int count = 0;
        private final ReentrantLock lock = new ReentrantLock();

        public void increment() {
            lock.lock();
            try {
                count++;
            } finally {
                lock.unlock();
            }
        }

        public int getCount() {
            return count;
        }
    }

    static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }
    }
}
