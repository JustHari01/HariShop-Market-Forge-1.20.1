package hari.harishop.v5;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side quote authority: one-use, actor-bound and revision-bound. */
public final class QuoteService {
    public enum Action { BUY, SELL, CLAIM_CREATE, CLAIM_RESIZE, CLAIM_ABANDON, CLAIM_LIST, CLAIM_BUY }

    public record Quote(String id, String actor, Action action, String assetKey, long unitPrice, long maxQuantity,
                        long issuedAtMillis, long expiresAtMillis, long marketRevision, String nonce,
                        String payloadDigest) {
        /** Compatibility constructor for legacy in-memory callers. */
        public Quote(String id, String actor, Action action, String assetKey, long unitPrice, long maxQuantity,
                     long issuedAtMillis, long expiresAtMillis, long marketRevision, String payloadDigest) {
            this(id, actor, action, assetKey, unitPrice, maxQuantity, issuedAtMillis, expiresAtMillis,
                    marketRevision, id, payloadDigest);
        }

        public Quote {
            if (id == null || id.isBlank() || actor == null || actor.isBlank() || action == null
                    || assetKey == null || assetKey.isBlank() || nonce == null || nonce.isBlank()
                    || payloadDigest == null || payloadDigest.isBlank()) throw new IllegalArgumentException("quote identity");
            MoneyMath.positive(unitPrice, "unitPrice");
            if (maxQuantity <= 0L || issuedAtMillis < 0L || expiresAtMillis < issuedAtMillis || expiresAtMillis - issuedAtMillis > 30_000L || marketRevision < 0L) throw new IllegalArgumentException("quote bounds");
        }
    }

    private final Clock clock;
    private final DurableStore durableStore;
    private final ConcurrentHashMap<String, Quote> active = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> consumed = new ConcurrentHashMap<>();

    public QuoteService() { this(Clock.systemUTC(), null); }
    public QuoteService(Clock clock) { this(clock, null); }

    /** Runtime servers inject Numismatic's SQLite-backed store. */
    public QuoteService(Clock clock, DurableStore durableStore) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.durableStore = durableStore;
    }

    public Quote issue(String actor, Action action, String assetKey, long unitPrice, long maxQuantity, long marketRevision, String payload) {
        if (actor == null || actor.isBlank() || action == null || assetKey == null || assetKey.isBlank() || payload == null) throw new IllegalArgumentException("quote identity");
        MoneyMath.positive(unitPrice, "unitPrice");
        if (maxQuantity <= 0L || marketRevision < 0L) throw new IllegalArgumentException("quote bounds");
        long now = clock.millis();
        String id = "q-" + UUID.randomUUID();
        String nonce = "n-" + UUID.randomUUID();
        Quote quote = new Quote(id, actor, action, assetKey, unitPrice, maxQuantity, now, Math.addExact(now, 30_000L),
                marketRevision, nonce, digest(actor + "\u0000" + action + "\u0000" + assetKey + "\u0000"
                        + unitPrice + "\u0000" + maxQuantity + "\u0000" + marketRevision + "\u0000"
                        + nonce + "\u0000" + payload));
        if (durableStore == null) active.put(id, quote);
        else durableStore.issue(quote);
        EconomyJfrEvents.Quote event = new EconomyJfrEvents.Quote();
        if (event.isEnabled()) {
            event.action = action.name(); event.outcome = "ISSUED";
            event.revision = marketRevision; event.price = unitPrice; event.commit();
        }
        return quote;
    }

    /**
     * Issues and consumes a short-lived quote in one durable transaction.
     *
     * <p>The normal GUI path never needs to expose an intermediate quote to a
     * client: all fields are re-derived on the server and the authorization is
     * immediately consumed.  Keeping that path as one SQLite commit removes a
     * needless issue/consume window and materially reduces TradeGate CPU and
     * FULL-commit latency without weakening the one-use nonce contract.  The
     * two-step {@link #issue} / {@link #consume} API remains available for
     * flows that actually hand a quote to another server-side component.</p>
     */
    public Quote authorize(String actor, Action action, String assetKey, long unitPrice,
                           long quantity, long marketRevision, String payload) {
        if (actor == null || actor.isBlank() || action == null || assetKey == null || assetKey.isBlank()
                || payload == null || quantity <= 0L) throw new IllegalArgumentException("quote request");
        MoneyMath.positive(unitPrice, "unitPrice");
        if (marketRevision < 0L) throw new IllegalArgumentException("marketRevision");
        long now = clock.millis();
        String id = "q-" + UUID.randomUUID();
        String nonce = "n-" + UUID.randomUUID();
        Quote quote = new Quote(id, actor, action, assetKey, unitPrice, quantity, now,
                Math.addExact(now, 30_000L), marketRevision, nonce,
                digest(actor + "\u0000" + action + "\u0000" + assetKey + "\u0000"
                        + unitPrice + "\u0000" + quantity + "\u0000" + marketRevision + "\u0000"
                        + nonce + "\u0000" + payload));
        Quote authorized;
        if (durableStore == null) {
            active.put(quote.id(), quote);
            authorized = consume(quote.id(), quote.nonce(), actor, action, assetKey, quantity, marketRevision);
        } else {
            authorized = durableStore.authorize(quote, quantity, now);
        }
        if (authorized == null) throw new IllegalStateException("quote authorization failed");
        EconomyJfrEvents.Quote event = new EconomyJfrEvents.Quote();
        if (event.isEnabled()) {
            event.action = action.name(); event.outcome = "AUTHORIZED";
            event.revision = marketRevision; event.price = unitPrice; event.commit();
        }
        return authorized;
    }

    /** Atomically consumes a quote after all server-side fields are revalidated. */
    public Quote consume(String id, String actor, Action action, String assetKey, long quantity, long marketRevision) {
        Quote known = active.get(id);
        if (known == null) throw new IllegalStateException("quote expired or already used");
        return consume(id, known.nonce(), actor, action, assetKey, quantity, marketRevision);
    }

    /** Atomically consumes an actor-bound quote and checks its nonce. */
    public Quote consume(String id, String nonce, String actor, Action action, String assetKey,
                         long quantity, long marketRevision) {
        if (id == null || actor == null || action == null || assetKey == null || quantity <= 0L) throw new IllegalArgumentException("quote request");
        if (durableStore != null) {
            throw new IllegalArgumentException("durable quote payload required");
        }
        Quote quote = active.remove(id);
        if (quote == null || consumed.putIfAbsent(id, Boolean.TRUE) != null) throw new IllegalStateException("quote expired or already used");
        long now = clock.millis();
        if (!quote.nonce().equals(nonce) || !quote.actor().equals(actor) || quote.action() != action || !quote.assetKey().equals(assetKey)
                || quantity > quote.maxQuantity() || now < quote.issuedAtMillis() || now > quote.expiresAtMillis()
                || marketRevision != quote.marketRevision()) throw new IllegalStateException("quote authorization failed");
        EconomyJfrEvents.Quote event = new EconomyJfrEvents.Quote();
        if (event.isEnabled()) {
            event.action = action.name(); event.outcome = "CONSUMED";
            event.revision = marketRevision; event.price = quote.unitPrice(); event.commit();
        }
        return quote;
    }

    /** Durable variant; payload is re-derived by the server and hashed again. */
    public Quote consume(String id, String actor, Action action, String assetKey, long quantity,
                         long marketRevision, String payload) {
        return consume(id, null, actor, action, assetKey, quantity, marketRevision, payload);
    }

    /** Durable variant with an explicit nonce; null is never accepted for new rows. */
    public Quote consume(String id, String nonce, String actor, Action action, String assetKey, long quantity,
                         long marketRevision, String payload) {
        if (durableStore == null) return consume(id, actor, action, assetKey, quantity, marketRevision);
        if (payload == null) throw new IllegalArgumentException("quote payload required");
        long now = clock.millis();
        Quote before = durableStore.read(id);
        if (before == null) throw new IllegalStateException("quote expired or authorization failed");
        String effectiveNonce = nonce == null ? before.nonce() : nonce;
        String expected = digest(actor + "\u0000" + action + "\u0000" + assetKey + "\u0000"
                + before.unitPrice() + "\u0000" + before.maxQuantity() + "\u0000"
                + marketRevision + "\u0000" + effectiveNonce + "\u0000" + payload);
        // The store validates the immutable quote fields and payload digest;
        // unit price/max quantity are never accepted from the client.
        Quote quote = durableStore.consume(id, effectiveNonce, actor, action, assetKey, quantity, marketRevision, now, expected);
        if (quote == null) throw new IllegalStateException("quote expired or authorization failed");
        return quote;
    }

    public int activeCount() { return active.size(); }
    public void invalidateAll() { active.clear(); }
    public void invalidateRevision(long revision) { active.entrySet().removeIf(entry -> entry.getValue().marketRevision() != revision); }
    public Map<String, Quote> snapshot() { return Map.copyOf(active); }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    public interface DurableStore {
        Quote issue(Quote quote);
        Quote read(String id);
        Quote consume(String id, String nonce, String actor, Action action, String assetKey, long quantity,
                      long marketRevision, long now, String payloadDigest);

        /**
         * Atomic issue+consume primitive.  The default is deliberately
         * correct, albeit two commits, so non-SQLite test stores cannot
         * accidentally bypass authorization semantics.
         */
        default Quote authorize(Quote quote, long quantity, long now) {
            issue(quote);
            return consume(quote.id(), quote.nonce(), quote.actor(), quote.action(), quote.assetKey(),
                    quantity, quote.marketRevision(), now, quote.payloadDigest());
        }
    }
}


