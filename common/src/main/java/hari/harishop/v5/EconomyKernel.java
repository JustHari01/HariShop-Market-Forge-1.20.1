package hari.harishop.v5;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/**
 * Shared, typed economy authority for HariShop/Numismatic/ProClaim.
 *
 * <p>All mutations are serialized, exact and idempotent.  A transaction ID is
 * bound to its operation fingerprint; replaying the same operation returns the
 * original result, while reusing the ID for another operation fails closed.</p>
 */
public final class EconomyKernel implements AutoCloseable {
    public enum Status { APPLIED, REJECTED }
    public enum Reason { NONE, INVALID_INPUT, INSUFFICIENT_FUNDS, BALANCE_LIMIT, TAX_INVALID,
        TRANSACTION_REPLAY, TRANSACTION_COLLISION, PERSISTENCE_FAILURE }

    public record TransactionId(String value) {
        public TransactionId { validateId(value, "transactionId"); }
    }

    /**
     * Typed economic identifiers used at module boundaries.  The legacy
     * String/long overloads remain for compatibility with old integrations,
     * but new code can no longer accidentally swap an account, asset or
     * transaction value at the call site.
     */
    public record AccountId(String value) {
        public AccountId { validateId(value, "accountId"); }
    }

    public record AssetKey(String value) {
        public AssetKey { validateId(value, "assetKey"); }
    }

    public record Nonce(String value) {
        public Nonce { validateId(value, "nonce"); }
    }

    public record Money(long value) {
        public Money { MoneyMath.nonNegative(value, "money"); }
        public static Money of(long value) { return new Money(value); }
        public static Money positive(long value) { return new Money(MoneyMath.positive(value, "money")); }
    }

    public record IssuerPermit(String issuerId, String scope, long maxAmount, long expiresAtMillis,
                               String signature) {
        public IssuerPermit {
            if (issuerId == null || issuerId.isBlank() || scope == null || scope.isBlank()
                    || expiresAtMillis < 0L || signature == null || signature.isBlank()) {
                throw new IllegalArgumentException("issuerPermit");
            }
            MoneyMath.nonNegative(maxAmount, "issuerPermit.maxAmount");
        }

        public boolean allows(Money amount, long nowMillis) {
            return amount != null && amount.value() <= maxAmount && nowMillis <= expiresAtMillis;
        }
    }

    public record TaxPolicy(long tax) {
        public TaxPolicy { MoneyMath.nonNegative(tax, "tax"); }
    }

    public record ActorContext(AccountId actor, Nonce nonce) {
        public ActorContext {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(nonce, "nonce");
        }
    }

    public record SubjectRef(String value) {
        public SubjectRef { validateId(value, "subjectRef"); }
    }

    public enum ReasonCode { UNKNOWN, INVALID_PROVENANCE, INCONSISTENT_RECEIPT, PHYSICAL_COMMIT_MISSING }

    public record QuoteRequest(AccountId actor, String action, AssetKey asset, Money unitPrice,
                               long quantity, long marketRevision, Nonce nonce) {
        public QuoteRequest {
            Objects.requireNonNull(actor, "actor");
            if (action == null || action.isBlank()) throw new IllegalArgumentException("action");
            Objects.requireNonNull(asset, "asset");
            Objects.requireNonNull(unitPrice, "unitPrice");
            MoneyMath.positive(unitPrice.value(), "unitPrice");
            if (quantity <= 0L || marketRevision < 0L) throw new IllegalArgumentException("quote bounds");
            Objects.requireNonNull(nonce, "nonce");
        }

        /** Stable payload used by a durable runtime authority.  It is not a
         * client assertion; the authority recomputes it from this typed
         * request before issuing or consuming the quote. */
        public String canonicalPayload() {
            return actor.value() + "\u0000" + action + "\u0000" + asset.value()
                    + "\u0000" + unitPrice.value() + "\u0000" + quantity
                    + "\u0000" + marketRevision + "\u0000" + nonce.value();
        }
    }

    public record Quote(QuoteId id, Nonce nonce, QuoteRequest request, Money total, long expiresAtMillis) {
        public Quote {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(nonce, "nonce");
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(total, "total");
            if (expiresAtMillis < 0L) throw new IllegalArgumentException("expiresAtMillis");
        }
    }

    public record QuoteId(String value) {
        public QuoteId { validateId(value, "quoteId"); }
    }

    public record Receipt(TransactionId id, Status status, Reason reason, long amount, long tax,
                          long revision, String fingerprint, Instant at) {
        public Receipt {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(reason, "reason");
            MoneyMath.nonNegative(amount, "amount");
            MoneyMath.nonNegative(tax, "tax");
            if (revision < 0L || fingerprint == null || fingerprint.isBlank() || at == null) throw new IllegalArgumentException("receipt");
        }
        public boolean applied() { return status == Status.APPLIED; }
    }

    public record Balance(String account, long amount, long revision) {
        public Balance {
            if (account == null || account.isBlank()) throw new IllegalArgumentException("account");
            MoneyMath.nonNegative(amount, "amount");
            if (revision < 0L) throw new IllegalArgumentException("revision");
        }
    }

    private final Path stateFile;
    private final Authority sharedAuthority;
    private final Map<String, Long> balances = new LinkedHashMap<>();
    private final Map<String, Receipt> transactions = new LinkedHashMap<>();
    private final Map<String, Quote> typedQuotes = new LinkedHashMap<>();
    private final Map<String, ReasonCode> quarantined = new LinkedHashMap<>();
    private long revision;
    private String auditHash = "0";
    private boolean closed;

    /** Memory-only constructor retained for pure unit tests. */
    public EconomyKernel() { this(null, null); }

    /**
     * Runtime constructor.  The adapter is supplied by HariShop's server
     * integration and is backed by Numismatic's SQLite ledger.  Keeping this
     * small interface free of Minecraft classes also lets the pure unit-test
     * source set compile without a game runtime on its classpath.
     */
    public EconomyKernel(Authority authority) { this(null, Objects.requireNonNull(authority, "authority")); }

    public EconomyKernel(Path stateFile) {
        this(stateFile, null);
    }

    private EconomyKernel(Path stateFile, Authority sharedAuthority) {
        this.stateFile = stateFile == null ? null : stateFile.toAbsolutePath().normalize();
        this.sharedAuthority = sharedAuthority;
        if (this.stateFile != null && sharedAuthority != null) throw new IllegalArgumentException("kernel cannot use two authorities");
        if (this.stateFile != null) load();
    }

    public synchronized long getBalance(String account) {
        ensureOpen();
        requireAccount(account);
        if (sharedAuthority != null) return sharedAuthority.balance(account);
        return balances.getOrDefault(account, 0L);
    }

    /** Typed balance API required by the shared economy contract. */
    public synchronized Money getBalance(AccountId account) {
        Objects.requireNonNull(account, "account");
        return Money.of(getBalance(account.value()));
    }

    public synchronized boolean tryDebit(AccountId account, Money amount, TransactionId transactionId) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(transactionId, "transactionId");
        MoneyMath.positive(amount.value(), "amount");
        return tryDebit(account.value(), amount.value(), transactionId.value());
    }

    /**
     * Typed issuer path.  A runtime Authority must explicitly implement the
     * permit-aware method; silently falling back to an unbounded credit would
     * make an admin/test grant indistinguishable from minted money.
     */
    public synchronized boolean credit(AccountId account, Money amount, IssuerPermit permit,
                                       TransactionId transactionId) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(permit, "permit");
        Objects.requireNonNull(transactionId, "transactionId");
        MoneyMath.positive(amount.value(), "amount");
        if (!permit.allows(amount, System.currentTimeMillis())) return false;
        if (sharedAuthority != null) {
            if (!sharedAuthority.verifyIssuerPermit(permit, amount)) return false;
            return sharedAuthority.credit(account.value(), amount.value(), permit, transactionId.value());
        }
        return credit(account.value(), amount.value(), transactionId.value());
    }

    public synchronized boolean transfer(AccountId from, AccountId to, Money amount, TaxPolicy tax,
                                          TransactionId transactionId) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(tax, "tax");
        Objects.requireNonNull(transactionId, "transactionId");
        MoneyMath.positive(amount.value(), "amount");
        return transfer(from.value(), to.value(), amount.value(), tax.tax(), transactionId.value());
    }

    public synchronized Receipt getTransaction(TransactionId transactionId) {
        Objects.requireNonNull(transactionId, "transactionId");
        return receipt(transactionId.value());
    }

    /** Issues a quote through the durable runtime authority when one exists. */
    public synchronized Quote quote(QuoteRequest request) {
        ensureOpen();
        Objects.requireNonNull(request, "request");
        if (sharedAuthority != null) {
            Quote durable = sharedAuthority.issueQuote(request);
            if (durable == null) throw new IllegalStateException("runtime quote authority rejected request");
            // Keep only the typed request envelope locally so execute() can
            // bind actor/nonce before delegating.  Durability and one-use
            // state remain owned by the runtime authority.
            typedQuotes.put(durable.id().value(), durable);
            return durable;
        }
        long total = MoneyMath.multiplyExact(request.unitPrice().value(), request.quantity(), "quote total");
        long now = System.currentTimeMillis();
        long expires = Math.addExact(now, 30_000L);
        Quote quote = new Quote(new QuoteId("q-" + UUID.randomUUID()), request.nonce(), request,
                Money.of(total), expires);
        typedQuotes.put(quote.id().value(), quote);
        return quote;
    }

    /** Executes only a quote whose action explicitly denotes a debit. */
    public synchronized Receipt execute(QuoteId quoteId, Nonce nonce, ActorContext actor) {
        ensureOpen();
        Objects.requireNonNull(quoteId, "quoteId");
        Objects.requireNonNull(nonce, "nonce");
        Objects.requireNonNull(actor, "actor");
        Quote quote = typedQuotes.remove(quoteId.value());
        if (quote == null && sharedAuthority != null) quote = sharedAuthority.readQuote(quoteId);
        if (quote == null || !quote.nonce().equals(nonce)
                || !quote.nonce().equals(actor.nonce())
                || !quote.request().actor().equals(actor.actor())
                || System.currentTimeMillis() > quote.expiresAtMillis()) {
            throw new IllegalStateException("quote authorization failed");
        }
        if (!(quote.request().action().equals("BUY") || quote.request().action().equals("DEBIT"))) {
            throw new IllegalStateException("typed kernel cannot execute non-debit action");
        }
        String txId = "quote-" + quote.id().value();
        if (sharedAuthority != null) {
            Receipt runtimeReceipt = sharedAuthority.executeQuote(quote, actor,
                    new TransactionId(txId));
            if (runtimeReceipt == null) throw new IllegalStateException("runtime quote execution rejected");
            return runtimeReceipt;
        }
        tryDebit(actor.actor(), quote.total(), new TransactionId(txId));
        return getTransaction(new TransactionId(txId));
    }

    public synchronized boolean quarantine(SubjectRef subject, ReasonCode reason) {
        ensureOpen();
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(reason, "reason");
        if (sharedAuthority != null) return sharedAuthority.quarantine(subject.value(), reason.name());
        return quarantined.putIfAbsent(subject.value(), reason) == null;
    }

    public synchronized Map<String, ReasonCode> quarantines() {
        ensureOpen();
        return Map.copyOf(quarantined);
    }

    public synchronized Balance snapshot(String account) {
        // A live server kernel is backed by Numismatic's SQLite revision; do
        // not return the zero-valued test-only field here.  Reading the
        // balance and revision through the same typed authority keeps quote
        // invalidation honest after a concurrent debit/credit.
        return new Balance(account, getBalance(account), revision());
    }

    public synchronized long revision() {
        ensureOpen();
        if (sharedAuthority != null) return sharedAuthority.revision();
        return revision;
    }

    /** Debits only existing funds; zero, negative and Long.MIN_VALUE are rejected. */
    public synchronized boolean tryDebit(String account, long amount, String transactionId) {
        ensureOpen();
        requireAccount(account);
        MoneyMath.positive(amount, "amount");
        if (sharedAuthority != null) return sharedAuthority.debit(account, amount, transactionId);
        String fp = fingerprint("DEBIT", account, amount, 0L, "");
        Receipt replay = replay(transactionId, fp);
        if (replay != null) return replay.applied();
        long before = balances.getOrDefault(account, 0L);
        if (before < amount) return reject(transactionId, Reason.INSUFFICIENT_FUNDS, amount, 0L, fp);
        long after = MoneyMath.subtractExact(before, amount, "balance");
        balances.put(account, after);
        try {
            return commit(transactionId, Status.APPLIED, Reason.NONE, amount, 0L, fp);
        } catch (RuntimeException failure) {
            balances.put(account, before);
            throw failure;
        }
    }

    /**
     * Legacy in-memory credit retained only for pure unit fixtures.  A live
     * Numismatic-backed kernel cannot mint through an untyped call: callers
     * must provide an issuer permit or use a treasury-backed payout API.
     */
    public synchronized boolean credit(String account, long amount, String transactionId) {
        ensureOpen();
        requireAccount(account);
        MoneyMath.positive(amount, "amount");
        if (sharedAuthority != null) return false;
        String fp = fingerprint("CREDIT", account, amount, 0L, "");
        Receipt replay = replay(transactionId, fp);
        if (replay != null) return replay.applied();
        long before = balances.getOrDefault(account, 0L);
        try {
            balances.put(account, MoneyMath.addExact(before, amount, "balance"));
        } catch (ArithmeticException | IllegalArgumentException overflow) {
            return reject(transactionId, Reason.BALANCE_LIMIT, amount, 0L, fp);
        }
        try {
            return commit(transactionId, Status.APPLIED, Reason.NONE, amount, 0L, fp);
        } catch (RuntimeException failure) {
            balances.put(account, before);
            throw failure;
        }
    }

    /**
     * Credits a recipient from a sender and sinks tax in the same journal
     * entry.  The debit and recipient credit are committed together.
     */
    public synchronized boolean transfer(String from, String to, long amount, long tax, String transactionId) {
        ensureOpen();
        requireAccount(from); requireAccount(to);
        if (sharedAuthority != null) {
            // The live authority owns the single SQLite posting transaction.
            // Never emulate a transfer with debit+credit here: a crash between
            // those two calls would create or destroy currency.
            try {
                return sharedAuthority.transfer(from, to, amount, tax, transactionId);
            } catch (RuntimeException invalid) {
                // Invalid self/tax/account input is a normal rejected request
                // at this boundary; an adapter exception must not tear down
                // the server thread or leave callers assuming a partial move.
                return false;
            }
        }
        if (from.equals(to)) throw new IllegalArgumentException("from and to must differ");
        MoneyMath.positive(amount, "amount");
        MoneyMath.nonNegative(tax, "tax");
        // A transfer that is entirely tax (tax == amount) would credit no
        // value to the recipient and is indistinguishable from a sink-only
        // operation.  Keep this API strictly positive-net so callers cannot
        // accidentally create ambiguous or replayable zero-value transfers.
        if (tax >= amount) return reject(transactionId, Reason.TAX_INVALID, amount, tax,
                fingerprint("TRANSFER", from, amount, tax, to));
        String fp = fingerprint("TRANSFER", from, amount, tax, to);
        Receipt replay = replay(transactionId, fp);
        if (replay != null) return replay.applied();
        long sender = balances.getOrDefault(from, 0L);
        if (sender < amount) return reject(transactionId, Reason.INSUFFICIENT_FUNDS, amount, tax, fp);
        long recipient = balances.getOrDefault(to, 0L);
        long net = amount - tax;
        try {
            long senderAfter = MoneyMath.subtractExact(sender, amount, "sender balance");
            long recipientAfter = MoneyMath.addExact(recipient, net, "recipient balance");
            balances.put(from, senderAfter);
            balances.put(to, recipientAfter);
        } catch (ArithmeticException | IllegalArgumentException overflow) {
            return reject(transactionId, Reason.BALANCE_LIMIT, amount, tax, fp);
        }
        try {
            return commit(transactionId, Status.APPLIED, Reason.NONE, amount, tax, fp);
        } catch (RuntimeException failure) {
            balances.put(from, sender);
            balances.put(to, recipient);
            throw failure;
        }
    }

    public synchronized Receipt receipt(String transactionId) {
        ensureOpen();
        if (transactionId == null || transactionId.isBlank()) return null;
        if (sharedAuthority != null) return sharedAuthority.receipt(transactionId);
        return transactions.get(transactionId);
    }

    public synchronized List<Receipt> receipts() {
        ensureOpen();
        return List.copyOf(transactions.values());
    }

    /** Append-only hash chaining for external audit tooling. */
    public synchronized String auditHash() { ensureOpen(); return auditHash; }

    /**
     * Cheap read-only integrity pass used by the release gate.  It does not
     * repair anything: a malformed balance, receipt or revision fails closed
     * and keeps the market locked until the state is restored from backup.
     */
    public synchronized boolean integrityCheck() {
        try {
            ensureOpen();
            if (sharedAuthority != null) return sharedAuthority.integrity();
            if (revision < 0L || auditHash == null || auditHash.isBlank()) return false;
            for (Map.Entry<String, Long> entry : balances.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() < 0L
                        || entry.getValue() > MoneyMath.MAX_BALANCE) return false;
            }
            long previous = -1L;
            for (Receipt receipt : transactions.values().stream()
                    .sorted(java.util.Comparator.comparingLong(Receipt::revision)).toList()) {
                if (receipt == null || receipt.revision() < 0L
                        || (previous >= 0L && receipt.revision() <= previous)) return false;
                previous = receipt.revision();
            }
            return true;
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    @Override public synchronized void close() { closed = true; }

    private Receipt replay(String id, String fingerprint) {
        validateId(id, "transactionId");
        Receipt existing = transactions.get(id);
        if (existing == null) return null;
        if (!existing.fingerprint().equals(fingerprint)) throw new IllegalStateException("transaction ID collision");
        return existing;
    }

    private boolean reject(String id, Reason reason, long amount, long tax, String fingerprint) {
        Receipt existing = replay(id, fingerprint);
        if (existing != null) return existing.applied();
        commit(id, Status.REJECTED, reason, amount, tax, fingerprint);
        return false;
    }

    private boolean commit(String id, Status status, Reason reason, long amount, long tax, String fingerprint) {
        TransactionId tx = new TransactionId(id);
        long previousRevision = revision;
        String previousAuditHash = auditHash;
        Receipt previousReceipt = transactions.get(id);
        Receipt receipt = new Receipt(tx, status, reason, amount, tax, ++revision, fingerprint, Instant.now());
        transactions.put(id, receipt);
        auditHash = hash(auditHash + '|' + id + '|' + status + '|' + reason + '|' + revision);
        try {
            persist();
        } catch (RuntimeException failure) {
            if (previousReceipt == null) transactions.remove(id); else transactions.put(id, previousReceipt);
            revision = previousRevision;
            auditHash = previousAuditHash;
            throw failure;
        }
        return status == Status.APPLIED;
    }

    private void load() {
        try {
            Properties p = AtomicStateStore.load(stateFile);
            revision = parseNonNegative(p.getProperty("kernel.revision"), 0L);
            auditHash = p.getProperty("kernel.auditHash", "0");
            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("balance.")) continue;
                String account = decode(key.substring("balance.".length()));
                balances.put(account, MoneyMath.nonNegative(Long.parseLong(p.getProperty(key)), "balance"));
            }
            Map<String, Map<String, String>> raw = new LinkedHashMap<>();
            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("tx.")) continue;
                int dot = key.lastIndexOf('.');
                if (dot <= 3) throw new IOException("invalid transaction property");
                raw.computeIfAbsent(decode(key.substring(3, dot)), ignored -> new LinkedHashMap<>())
                        .put(key.substring(dot + 1), p.getProperty(key));
            }
            for (var entry : raw.entrySet()) {
                String id = entry.getKey();
                Receipt receipt = new Receipt(new TransactionId(id), Status.valueOf(required(entry.getValue(), "status")),
                        Reason.valueOf(required(entry.getValue(), "reason")), Long.parseLong(required(entry.getValue(), "amount")),
                        Long.parseLong(required(entry.getValue(), "tax")), Long.parseLong(required(entry.getValue(), "revision")),
                        required(entry.getValue(), "fingerprint"), Instant.parse(required(entry.getValue(), "at")));
                transactions.put(id, receipt);
            }
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("cannot load economy kernel", failure);
        }
    }

    private void persist() {
        if (stateFile == null) return;
        Properties p = new Properties();
        p.setProperty("kernel.schema", "6");
        p.setProperty("kernel.revision", Long.toString(revision));
        p.setProperty("kernel.auditHash", auditHash);
        balances.forEach((account, amount) -> p.setProperty("balance." + encode(account), Long.toString(amount)));
        transactions.forEach((id, receipt) -> {
            String prefix = "tx." + encode(id) + ".";
            p.setProperty(prefix + "status", receipt.status().name());
            p.setProperty(prefix + "reason", receipt.reason().name());
            p.setProperty(prefix + "amount", Long.toString(receipt.amount()));
            p.setProperty(prefix + "tax", Long.toString(receipt.tax()));
            p.setProperty(prefix + "revision", Long.toString(receipt.revision()));
            p.setProperty(prefix + "fingerprint", receipt.fingerprint());
            p.setProperty(prefix + "at", receipt.at().toString());
        });
        try { AtomicStateStore.save(stateFile, p); }
        catch (IOException failure) { throw new IllegalStateException("cannot persist economy kernel", failure); }
    }

    private void ensureOpen() { if (closed) throw new IllegalStateException("economy kernel closed"); }
    private static void requireAccount(String account) { if (account == null || account.isBlank() || account.length() > 256) throw new IllegalArgumentException("account"); }
    private static void validateId(String id, String field) { if (id == null || id.isBlank() || id.length() > 160) throw new IllegalArgumentException(field); }
    private static String required(Map<String, String> values, String key) { String value = values.get(key); if (value == null || value.isBlank()) throw new IllegalArgumentException("missing " + key); return value; }
    private static long parseNonNegative(String value, long fallback) { try { return value == null ? fallback : MoneyMath.nonNegative(Long.parseLong(value), "revision"); } catch (RuntimeException invalid) { throw new IllegalArgumentException("invalid revision", invalid); } }
    private static String fingerprint(String op, String account, long amount, long tax, String other) { return hash(op + '\u0000' + account + '\u0000' + amount + '\u0000' + tax + '\u0000' + other); }
    private static String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception failure) { throw new IllegalStateException(failure); } }
    private static String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String decode(String value) { try { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); } catch (IllegalArgumentException invalid) { throw new IllegalStateException("invalid encoded economy key", invalid); } }

    /** Typed bridge implemented only by the live server integration. */
    public interface Authority {
        long balance(String account);
        long revision();
        boolean integrity();
        boolean debit(String account, long amount, String transactionId);
        boolean credit(String account, long amount, String transactionId);

        /**
         * Atomic runtime transfer.  Implementations must journal the sender,
         * recipient and tax sink in one durable posting transaction; the
         * default rejects so an old adapter cannot silently split the move.
         */
        default boolean transfer(String from, String to, long amount, long tax, String transactionId) {
            return false;
        }

        /**
         * Permit-aware issuer hook.  The default deliberately rejects so an
         * older runtime adapter cannot accidentally turn the typed API into
         * an unbounded minting path.
         */
        default boolean credit(String account, long amount, IssuerPermit permit, String transactionId) {
            return false;
        }

        /** Runtime authorities must validate the permit signature/scope; a
         * caller-supplied non-empty string is not a credential. */
        default boolean verifyIssuerPermit(IssuerPermit permit, Money amount) { return false; }

        /** Durable quote issuance hook.  Unsupported runtimes fail closed. */
        default Quote issueQuote(QuoteRequest request) { return null; }

        /** Readback of an ACTIVE durable quote after a kernel restart. */
        default Quote readQuote(QuoteId quoteId) { return null; }

        /** Durable quote consumption plus the idempotent money mutation. */
        default Receipt executeQuote(Quote quote, ActorContext actor, TransactionId transactionId) {
            return null;
        }

        /** Readback of the single source-of-truth transaction journal. */
        default Receipt receipt(String transactionId) { return null; }

        /** Durable quarantine hook; unsupported runtimes fail closed. */
        default boolean quarantine(String subject, String reason) { return false; }
    }
}


