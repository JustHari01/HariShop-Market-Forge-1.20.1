package hari.harishop.v5;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Collision-safe identity for an ItemStack variant.
 *
 * <p>The digest is only an index.  The complete canonical bytes are retained
 * and compared whenever two records share a digest, so a hash collision can
 * never merge economic identities.</p>
 */
public final class AssetKey {
    private final String registryId;
    private final String schema;
    private final String registryFingerprint;
    private final String modpackFingerprint;
    private final byte[] canonicalBytes;
    private final byte[] digest;

    private AssetKey(String registryId, String schema, String registryFingerprint,
                     String modpackFingerprint, byte[] canonicalBytes) {
        this.registryId = require(registryId, "registryId");
        this.schema = require(schema, "schema");
        this.registryFingerprint = require(registryFingerprint, "registryFingerprint");
        this.modpackFingerprint = require(modpackFingerprint, "modpackFingerprint");
        if (canonicalBytes == null || canonicalBytes.length == 0 || canonicalBytes.length > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("canonicalBytes must contain 1..16MiB");
        }
        this.canonicalBytes = canonicalBytes.clone();
        // The digest is an index for the complete economic identity, not
        // merely for the component payload. Registry/schema/modpack changes
        // must never reuse a price or provenance row accidentally.
        this.digest = sha256(identityCanonicalBytes());
    }

    public static AssetKey of(String registryId, String schema, String registryFingerprint,
                              String modpackFingerprint, byte[] canonicalBytes) {
        return new AssetKey(registryId, schema, registryFingerprint, modpackFingerprint, canonicalBytes);
    }

    /** Deterministic length-prefixed encoding for component maps. */
    public static byte[] canonicalBytes(Map<String, byte[]> components) {
        Objects.requireNonNull(components, "components");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            components.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                    throw new IllegalArgumentException("invalid component");
                }
                try {
                    byte[] id = entry.getKey().getBytes(StandardCharsets.UTF_8);
                    out.writeInt(id.length);
                    out.write(id);
                    out.writeInt(entry.getValue().length);
                    out.write(entry.getValue());
                } catch (IOException impossible) {
                    throw new IllegalStateException(impossible);
                }
            });
            out.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public String registryId() { return registryId; }
    public String schema() { return schema; }
    public String registryFingerprint() { return registryFingerprint; }
    public String modpackFingerprint() { return modpackFingerprint; }
    public byte[] canonicalBytes() { return canonicalBytes.clone(); }
    public byte[] digest() { return digest.clone(); }
    public String hexDigest() { return hex(digest); }

    /** Length-prefixed canonical bytes used for the identity digest. */
    public byte[] identityCanonicalBytes() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            write(out, registryId);
            write(out, schema);
            write(out, registryFingerprint);
            write(out, modpackFingerprint);
            out.writeInt(canonicalBytes.length);
            out.write(canonicalBytes);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof AssetKey that)) return false;
        return registryId.equals(that.registryId) && schema.equals(that.schema)
                && registryFingerprint.equals(that.registryFingerprint)
                && modpackFingerprint.equals(that.modpackFingerprint)
                && Arrays.equals(canonicalBytes, that.canonicalBytes);
    }

    @Override public int hashCode() {
        int result = Objects.hash(registryId, schema, registryFingerprint, modpackFingerprint);
        return 31 * result + Arrays.hashCode(canonicalBytes);
    }

    @Override public String toString() {
        return registryId + "#" + schema + "#" + hexDigest();
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 512) throw new IllegalArgumentException(field);
        return value;
    }

    private static void write(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 16 * 1024 * 1024) throw new IllegalArgumentException("identity field too large");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static byte[] sha256(byte[] value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value); }
        catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    private static String hex(byte[] value) {
        StringBuilder out = new StringBuilder(value.length * 2);
        for (byte b : value) out.append(String.format("%02x", b));
        return out.toString();
    }

    /**
     * A digest bucket that preserves full bytes.  It is intentionally small
     * and immutable-at-read so catalog workers can publish snapshots safely.
     */
    public static final class CollisionSafeIndex {
        private final java.util.concurrent.ConcurrentHashMap<String, List<AssetKey>> buckets = new java.util.concurrent.ConcurrentHashMap<>();

        public void add(AssetKey key) {
            Objects.requireNonNull(key, "key");
            buckets.compute(key.hexDigest(), (ignored, old) -> {
                List<AssetKey> next = new ArrayList<>(old == null ? List.of() : old);
                if (!next.contains(key)) next.add(key);
                return List.copyOf(next);
            });
        }

        public List<AssetKey> lookup(String digestHex) {
            if (digestHex == null || digestHex.isBlank()) return List.of();
            return buckets.getOrDefault(digestHex.toLowerCase(java.util.Locale.ROOT), List.of());
        }

        public boolean contains(AssetKey key) { return lookup(key.hexDigest()).contains(key); }
        public int size() { return buckets.values().stream().mapToInt(List::size).sum(); }
    }
}


