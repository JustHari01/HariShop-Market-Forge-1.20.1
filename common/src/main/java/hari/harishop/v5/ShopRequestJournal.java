package hari.harishop.v5;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded request de-duplication for the legacy shop packet path.
 *
 * The Numismatic wallet journal protects the currency side of a transaction,
 * but an already-applied wallet transaction must not cause the item side of a
 * replayed packet to run again.  This small server-process journal closes that
 * window for duplicate clicks and packet replays while a server is running.
 * It is intentionally bounded and time limited; durable crash recovery remains
 * the responsibility of the persistence-backed V5 transaction journal.
 */
public final class ShopRequestJournal {
    public enum Decision { NEW, IN_FLIGHT, COMPLETE, CONFLICT }

    private static final int MAX_ENTRIES = 4096;
    private static final long TTL_MILLIS = 15L * 60L * 1000L;
    private static final LinkedHashMap<String, Entry> ENTRIES =
            new LinkedHashMap<String, Entry>(256, 0.75f, true);

    private ShopRequestJournal() {}

    public static synchronized Decision begin(String transactionId, String fingerprint) {
        if (transactionId == null || transactionId.isBlank()
                || fingerprint == null || fingerprint.isBlank()) {
            return Decision.CONFLICT;
        }
        purgeExpired(System.currentTimeMillis());
        Entry existing = ENTRIES.get(transactionId);
        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint)) return Decision.CONFLICT;
            return existing.state;
        }
        trimToCapacity();
        ENTRIES.put(transactionId, new Entry(fingerprint, Decision.IN_FLIGHT,
                System.currentTimeMillis() + TTL_MILLIS));
        return Decision.NEW;
    }

    public static synchronized void complete(String transactionId, String fingerprint) {
        Entry entry = ENTRIES.get(transactionId);
        if (entry != null && entry.fingerprint.equals(fingerprint)) {
            ENTRIES.put(transactionId, new Entry(entry.fingerprint, Decision.COMPLETE,
                    System.currentTimeMillis() + TTL_MILLIS));
        }
    }

    public static synchronized void abort(String transactionId, String fingerprint) {
        Entry entry = ENTRIES.get(transactionId);
        if (entry != null && entry.fingerprint.equals(fingerprint)
                && entry.state == Decision.IN_FLIGHT) {
            ENTRIES.remove(transactionId);
        }
    }

    static synchronized void clearForTests() {
        ENTRIES.clear();
    }

    private static void purgeExpired(long now) {
        Iterator<Map.Entry<String, Entry>> iterator = ENTRIES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < now) iterator.remove();
        }
    }

    private static void trimToCapacity() {
        while (ENTRIES.size() >= MAX_ENTRIES) {
            Iterator<String> iterator = ENTRIES.keySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    private record Entry(String fingerprint, Decision state, long expiresAt) {}
}
