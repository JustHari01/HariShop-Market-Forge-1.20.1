package hari.harishop.v5;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionGateTest {
    @Test
    void samePlayerOperationsDoNotOverlap() throws Exception {
        Object player = new Object() { public java.util.UUID getUUID() { return java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"); } };
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(8);
        var inFlight = new AtomicInteger();
        var max = new AtomicInteger();
        List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                int result = TransactionGate.execute(player, "test", () -> {
                    int active = inFlight.incrementAndGet();
                    max.accumulateAndGet(active, Math::max);
                    Thread.sleep(2L);
                    inFlight.decrementAndGet();
                    done.countDown();
                    return 1;
                });
                return result;
            }));
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        for (var future : futures) assertEquals(1, future.get());
        assertEquals(1, max.get());
        pool.shutdownNow();
    }
}
