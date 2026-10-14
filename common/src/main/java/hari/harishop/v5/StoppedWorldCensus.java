package hari.harishop.v5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Offline, stopped-world census for the Forge 1.20.1 backport. The scanner
 * never mutates the world and is deliberately fail-closed: malformed NBT,
 * unreadable region sectors or resource limits produce UNKNOWN coverage,
 * which cannot alter a scarcity or inflation factor.
 */
public final class StoppedWorldCensus {
    private static final int MAX_FILES = 200_000;
    private static final int MAX_CHUNK_RECORDS = 2_000;
    private static final int MAX_CHUNK_BYTES = 16 * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 8L * 1024 * 1024 * 1024;

    public enum Coverage { COMPLETE, PARTIAL, UNKNOWN }

    public record Result(Map<String, Long> counts, Coverage coverage, int files, long bytes,
                         Instant observedAt, List<String> failures, String digest) {
        public Result {
            counts = Map.copyOf(counts == null ? Map.of() : counts);
            failures = List.copyOf(failures == null ? List.of() : failures);
            if (files < 0 || bytes < 0 || observedAt == null || digest == null) throw new IllegalArgumentException("census result");
        }
    }

    private StoppedWorldCensus() {}

    public static Result scan(Path worldRoot) {
        if (worldRoot == null) throw new IllegalArgumentException("worldRoot");
        Path root = worldRoot.toAbsolutePath().normalize();
        Map<String, Long> counts = new HashMap<>();
        List<String> failures = new ArrayList<>();
        List<Path> files = StoppedWorldSources.discover(root, failures);
        if (files.size() > MAX_FILES) failures.add("file-limit");
        long totalBytes = 0L;
        int[] chunks = {0};
        for (Path file : files.subList(0, Math.min(files.size(), MAX_FILES))) {
            try {
                long size = Files.size(file);
                totalBytes = Math.addExact(totalBytes, size);
                if (file.getFileName().toString().endsWith(".mca")) scanRegion(file, counts, failures, chunks);
                else if (file.getFileName().toString().endsWith(".dat")) scanCompressed(file, counts);
            } catch (Throwable failure) {
                failures.add(root.relativize(file) + ":" + failure.getClass().getSimpleName());
            }
            if (totalBytes > MAX_TOTAL_BYTES) { failures.add("byte-limit"); break; }
        }
        Coverage coverage = failures.isEmpty() ? Coverage.COMPLETE : Coverage.UNKNOWN;
        String digest = digest(counts, coverage, Math.min(files.size(), MAX_FILES), totalBytes);
        return new Result(counts, coverage, Math.min(files.size(), MAX_FILES), totalBytes,
                Instant.now(), failures, digest);
    }

    private static void scanCompressed(Path file, Map<String, Long> counts) throws IOException {
        try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ);
             DataInputStream data = new DataInputStream(new GZIPInputStream(input))) {
            countInventories(NbtIo.read(data, new NbtAccounter(MAX_CHUNK_BYTES)), counts);
        }
    }

    private static void scanRegion(Path file, Map<String, Long> counts, List<String> failures, int[] chunks) throws IOException {
        if (Files.size(file) == 0L) return;
        try (var input = Files.newInputStream(file, StandardOpenOption.READ)) {
            byte[] header = input.readNBytes(8192);
            if (header.length != 8192) throw new IOException("short region header");
            ByteBuffer locations = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
            for (int index = 0; index < 1024; index++) {
                if (chunks[0] >= MAX_CHUNK_RECORDS) { failures.add("chunk-limit"); return; }
                int location = locations.getInt(index * 4);
                int sector = location & 0xFFFFFF;
                int sectors = (location >>> 24) & 0xFF;
                if (sector < 2 || sectors == 0) continue;
                chunks[0]++;
                long offset = (long) sector * 4096L;
                if (offset > Files.size(file) - 5L || sectors > 4096) {
                    failures.add("region-sector:" + file.getFileName() + ':' + index);
                    continue;
                }
                try (var channel = java.nio.channels.FileChannel.open(file, StandardOpenOption.READ)) {
                    channel.position(offset);
                    ByteBuffer lengthBytes = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN);
                    if (channel.read(lengthBytes) != 5) throw new IOException("short chunk header");
                    lengthBytes.flip();
                    int length = lengthBytes.getInt();
                    int compression = lengthBytes.get() & 0xFF;
                    if (length <= 1 || length > sectors * 4096 - 4 || length > MAX_CHUNK_BYTES) throw new IOException("invalid chunk length");
                    ByteBuffer compressed = ByteBuffer.allocate(length - 1);
                    while (compressed.hasRemaining()) {
                        int read = channel.read(compressed);
                        if (read < 0) throw new IOException("short chunk payload");
                        if (read == 0) throw new IOException("zero-byte chunk read");
                    }
                    try (InputStream raw = new ByteArrayInputStream(compressed.array());
                         InputStream decoded = compression == 1 ? new GZIPInputStream(raw)
                                 : compression == 2 ? new InflaterInputStream(raw) : raw;
                         DataInputStream data = new DataInputStream(decoded)) {
                        countInventories(NbtIo.read(data, new NbtAccounter(MAX_CHUNK_BYTES)), counts);
                    }
                } catch (Throwable failure) {
                    failures.add("chunk:" + file.getFileName() + ':' + index + ':' + failure.getClass().getSimpleName());
                }
            }
        }
    }

    static void countInventories(CompoundTag root, Map<String, Long> counts) { walk(root, counts, 0, false); }

    private static void walk(Tag tag, Map<String, Long> counts, int depth, boolean inInventory) {
        if (tag == null || depth > 32) return;
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag child = compound.get(key);
                boolean childInInventory = InventoryContextPolicy.child(inInventory, key);
                if (childInInventory && child instanceof CompoundTag item && item.contains("id", Tag.TAG_STRING)
                        && (item.contains("Count") || item.contains("count"))) addItem(item, counts);
                walk(child, counts, depth + 1, childInInventory);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) walk(list.get(i), counts, depth + 1, inInventory);
        }
    }

    private static void addItem(CompoundTag item, Map<String, Long> counts) {
        String id = item.getString("id");
        if (id == null || id.isBlank() || id.length() > 512) return;
        try {
            long count = itemQuantity(item);
            if (count <= 0L || count > 99_999L) return;
            String key = itemIdentityKey(item);
            if (key == null) return;
            counts.merge(key, count, (left, right) -> left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right);
        } catch (RuntimeException ignored) { }
    }

    public static long itemQuantity(CompoundTag item) {
        if (item == null) return 0L;
        Tag countTag = item.contains("count") ? item.get("count") : item.get("Count");
        return countTag instanceof net.minecraft.nbt.NumericTag numeric ? numeric.getAsLong() : 0L;
    }

    public static String itemIdentityKey(CompoundTag item) {
        if (item == null || !item.contains("id", Tag.TAG_STRING)) return null;
        String id = item.getString("id");
        if (id == null || id.isBlank() || id.length() > 512) return null;
        CompoundTag identity = item.copy();
        identity.remove("Count");
        identity.remove("count");
        return id + "@raw-v2@" + hex(sha256(ExactStackIdentity.canonicalTag(identity)));
    }

    private static String digest(Map<String, Long> counts, Coverage coverage, int files, long bytes) {
        StringBuilder canonical = new StringBuilder(coverage.name()).append('|').append(files).append('|').append(bytes);
        counts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> canonical.append('|').append(entry.getKey()).append('=').append(entry.getValue()));
        return hex(sha256(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sha256(byte[] value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value); }
        catch (Exception failure) { throw new IllegalStateException("SHA-256 unavailable", failure); }
    }

    private static String hex(byte[] value) {
        StringBuilder output = new StringBuilder(value.length * 2);
        for (byte b : value) output.append(String.format("%02x", b));
        return output.toString();
    }
}
