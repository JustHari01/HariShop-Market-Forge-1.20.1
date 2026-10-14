package hari.harishop.v5;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader-neutral, authenticated provenance ledger shared by the two ports.
 * A stack marker is never trusted by itself: only a server-issued reference
 * retained in this ledger can become economically eligible.
 */
public final class ProvenanceLedger {
    public enum OriginClass {
        SURVIVAL_CERTIFIED,
        MARKET_ISSUED,
        PLAYER_TRADED,
        ADMIN_INJECTED,
        ADMIN_DERIVED,
        TEST,
        LEGACY_CENSUS,
        UNKNOWN,
        REVOKED
    }

    public record Ref(String lotId, OriginClass origin, String parentDigest, long committedQuantity,
                      long issuedAtMillis, String keyId, String signature) {
        public Ref {
            if (lotId == null || lotId.isBlank() || lotId.length() > 160) throw new IllegalArgumentException("lotId");
            Objects.requireNonNull(origin, "origin");
            if (parentDigest == null || parentDigest.length() > 128) throw new IllegalArgumentException("parentDigest");
            MoneyMath.nonNegative(committedQuantity, "committedQuantity");
            if (issuedAtMillis < 0L || keyId == null || keyId.isBlank() || signature == null || signature.isBlank()) {
                throw new IllegalArgumentException("provenance metadata");
            }
        }

        public boolean economicallyEligible() {
            return origin == OriginClass.SURVIVAL_CERTIFIED || origin == OriginClass.MARKET_ISSUED
                    || origin == OriginClass.PLAYER_TRADED || origin == OriginClass.LEGACY_CENSUS;
        }
    }

    private final byte[] secret;
    private final String keyId;
    private final ConcurrentHashMap<String, Ref> refs = new ConcurrentHashMap<>();

    public ProvenanceLedger(byte[] secret, String keyId) {
        if (secret == null || secret.length < 32) throw new IllegalArgumentException("HMAC secret must be >=256 bits");
        if (keyId == null || keyId.isBlank() || keyId.length() > 64) throw new IllegalArgumentException("keyId");
        this.secret = secret.clone();
        this.keyId = keyId;
    }

    public static ProvenanceLedger random() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return new ProvenanceLedger(secret, "k1");
    }

    public Ref issue(OriginClass origin, long quantity, String parentDigest, long nowMillis) {
        Objects.requireNonNull(origin, "origin");
        MoneyMath.nonNegative(quantity, "quantity");
        if (nowMillis < 0L) throw new IllegalArgumentException("nowMillis");
        String lotId = "lot-" + UUID.randomUUID();
        Ref unsigned = new Ref(lotId, origin, parentDigest == null ? "" : parentDigest,
                quantity, nowMillis, keyId, "pending");
        Ref signed = new Ref(unsigned.lotId(), unsigned.origin(), unsigned.parentDigest(), unsigned.committedQuantity(),
                unsigned.issuedAtMillis(), keyId, sign(unsigned));
        refs.put(lotId, signed);
        return signed;
    }

    public Ref derive(List<Ref> inputs, long quantity, long nowMillis) {
        if (inputs == null || inputs.isEmpty()) throw new IllegalArgumentException("inputs");
        MoneyMath.nonNegative(quantity, "quantity");
        OriginClass origin = OriginClass.SURVIVAL_CERTIFIED;
        for (Ref input : inputs) {
            if (input == null || !verify(input)) return issue(OriginClass.UNKNOWN, quantity, "", nowMillis);
            if (input.origin() == OriginClass.ADMIN_INJECTED || input.origin() == OriginClass.ADMIN_DERIVED) origin = OriginClass.ADMIN_DERIVED;
            else if (input.origin() == OriginClass.TEST) origin = OriginClass.TEST;
            else if (input.origin() == OriginClass.UNKNOWN || input.origin() == OriginClass.REVOKED) origin = OriginClass.UNKNOWN;
            else if (input.origin() == OriginClass.LEGACY_CENSUS && origin == OriginClass.SURVIVAL_CERTIFIED) origin = OriginClass.LEGACY_CENSUS;
        }
        StringBuilder parents = new StringBuilder();
        inputs.stream().map(Ref::lotId).sorted().forEach(id -> parents.append(id).append(';'));
        return issue(origin, quantity, digest(parents.toString()), nowMillis);
    }

    public boolean verify(Ref ref) {
        if (ref == null || !keyId.equals(ref.keyId()) || "pending".equals(ref.signature())) return false;
        Ref unsigned = new Ref(ref.lotId(), ref.origin(), ref.parentDigest(), ref.committedQuantity(),
                ref.issuedAtMillis(), ref.keyId(), "pending");
        Ref stored = refs.get(ref.lotId());
        return stored != null
                && MessageDigest.isEqual(ref.signature().getBytes(StandardCharsets.US_ASCII), sign(unsigned).getBytes(StandardCharsets.US_ASCII))
                && stored.equals(ref);
    }

    public boolean economicallyEligible(Ref ref) { return verify(ref) && ref.economicallyEligible(); }

    public void revoke(String lotId) {
        Ref current = refs.get(lotId);
        if (current != null) {
            Ref unsigned = new Ref(current.lotId(), OriginClass.REVOKED, current.parentDigest(), current.committedQuantity(),
                    current.issuedAtMillis(), current.keyId(), "pending");
            refs.put(lotId, new Ref(unsigned.lotId(), unsigned.origin(), unsigned.parentDigest(), unsigned.committedQuantity(),
                    unsigned.issuedAtMillis(), unsigned.keyId(), sign(unsigned)));
        }
    }

    public Ref resolve(String lotId) { return lotId == null ? null : refs.get(lotId); }
    public int size() { return refs.size(); }

    private String sign(Ref ref) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String canonical = ref.lotId() + '\u0000' + ref.origin() + '\u0000' + ref.parentDigest() + '\u0000'
                    + ref.committedQuantity() + '\u0000' + ref.issuedAtMillis() + '\u0000' + ref.keyId();
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) { throw new IllegalStateException("HMAC unavailable", failure); }
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
