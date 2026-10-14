package hari.harishop.v5;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Per-player serialisation for buy/sell mutations and stable tx ids. */
public final class TransactionGate {
    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private TransactionGate() {}

    public static int execute(Object player, String operation, Callable<Integer> body) {
        if (body == null) throw new IllegalArgumentException("transaction body");
        String key = playerKey(player);
        ReentrantLock lock = LOCKS.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            if (CURRENT.get() != null) return call(body);
            CURRENT.set(new Context("harishop:forge1201:" + safe(operation) + ':' + key + ':' + UUID.randomUUID(),
                    new AtomicInteger()));
            return call(body);
        } finally {
            CURRENT.remove();
            lock.unlock();
            if (!lock.hasQueuedThreads()) LOCKS.remove(key, lock);
        }
    }

    public static String nextTransactionId(String direction) {
        Context context = CURRENT.get();
        return context == null
                ? "harishop:forge1201:compat:" + safe(direction) + ':' + UUID.randomUUID()
                : context.id + ':' + safe(direction) + ':' + context.sequence.incrementAndGet();
    }

    private static int call(Callable<Integer> body) {
        try { return body.call(); }
        catch (RuntimeException runtime) { throw runtime; }
        catch (Exception checked) { throw new IllegalStateException(checked); }
    }

    private static String playerKey(Object player) {
        if (player == null) return "null";
        try {
            Method getUUID = player.getClass().getMethod("getUUID");
            return String.valueOf(getUUID.invoke(player));
        } catch (ReflectiveOperationException ignored) {
            return player.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(player));
        }
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "operation";
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private record Context(String id, AtomicInteger sequence) {}
}
