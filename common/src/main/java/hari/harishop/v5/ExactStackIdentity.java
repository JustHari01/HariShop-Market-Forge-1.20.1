package hari.harishop.v5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Canonical identity for a concrete Forge 1.20.1 item stack.
 *
 * <p>NeoForge 1.21.1 serializes Data Components through
 * {@code ItemStack.saveOptional(provider)}.  Forge 1.20.1 has no Data
 * Components, therefore the equivalent authoritative boundary is the
 * complete NBT returned by {@link ItemStack#save(CompoundTag)}.  The NBT is
 * framed recursively with sorted compound keys so the digest is stable across
 * map insertion order and can be used for variant equality, journal
 * fingerprints and server-side packet validation.  If encoding fails, the
 * caller must reject the variant instead of guessing an identity.</p>
 */
public final class ExactStackIdentity {
    private static final int MAX_BYTES = 16 * 1024 * 1024;

    public record Encoded(byte[] canonicalBytes, String sha256) {
        public Encoded {
            if (canonicalBytes == null || canonicalBytes.length == 0 || canonicalBytes.length > MAX_BYTES) {
                throw new IllegalArgumentException("canonical stack bytes");
            }
            canonicalBytes = canonicalBytes.clone();
            if (sha256 == null || sha256.length() != 64 || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("stack digest");
            }
        }

        @Override public byte[] canonicalBytes() { return canonicalBytes.clone(); }
    }

    private ExactStackIdentity() {}

    /** Encodes a non-empty stack using Forge's full NBT representation. */
    public static Encoded encode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("stack");
        CompoundTag saved = stack.save(new CompoundTag());
        byte[] canonical = canonicalTag(saved);
        try {
            return new Encoded(canonical, hex(MessageDigest.getInstance("SHA-256").digest(canonical)));
        } catch (Exception failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    /**
     * Exact, fail-closed variant comparison for a server-side operation. A
     * codec failure is never treated as equality: an unencodable custom
     * payload must be quarantined instead of being merged or sold under a
     * guessed identity.
     */
    public static boolean same(ItemStack first, ItemStack second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) return false;
        try { return encode(first).sha256().equals(encode(second).sha256()); }
        catch (Throwable invalid) { return false; }
    }

    /** Canonical framing for raw NBT observations and stopped-world scans. */
    public static byte[] canonicalTag(Tag saved) {
        if (saved == null) throw new IllegalArgumentException("NBT tag");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            writeString(out, "harishop:itemstack:v1:nbt");
            writeTag(out, saved, 0);
            out.flush();
            byte[] canonical = bytes.toByteArray();
            if (canonical.length == 0 || canonical.length > MAX_BYTES) {
                throw new IllegalArgumentException("NBT payload too large");
            }
            return canonical;
        } catch (IOException impossible) {
            throw new IllegalStateException("cannot encode NBT", impossible);
        }
    }

    private static void writeTag(DataOutputStream out, Tag tag, int depth) throws IOException {
        if (tag == null || depth > Tag.MAX_DEPTH) throw new IllegalArgumentException("invalid NBT depth");
        out.writeByte(tag.getId());
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            keys.sort(Comparator.naturalOrder());
            out.writeInt(keys.size());
            for (String key : keys) {
                writeString(out, key);
                writeTag(out, compound.get(key), depth + 1);
            }
            return;
        }
        if (tag instanceof ListTag list) {
            out.writeInt(list.size());
            for (int i = 0; i < list.size(); i++) writeTag(out, list.get(i), depth + 1);
            return;
        }
        // Primitive and array tags retain their exact Mojang numeric width.
        tag.write(out);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null || value.length() > 1_000_000) throw new IllegalArgumentException("NBT string");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}
