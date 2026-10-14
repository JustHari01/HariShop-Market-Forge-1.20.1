package hari.harishop.v5;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;

/** Versioned, checksummed, recoverable persistence for HariShop market state. */
public final class AtomicStateStore {
    public static final String SCHEMA_VERSION = "5";

    private AtomicStateStore() {}

    public static Properties load(Path target) throws IOException {
        if (!Files.exists(target)) return new Properties();
        Path checksum = sibling(target, ".sha256");
        Path backup = sibling(target, ".bak");
        Path chosen = target;
        if (Files.exists(checksum) && !verify(target, checksum)) {
            Path backupChecksum = sibling(target, ".bak.sha256");
            if (!Files.exists(backup) || !Files.exists(backupChecksum) || !verify(backup, backupChecksum)) {
                throw new IOException("market state checksum mismatch and no valid backup");
            }
            chosen = backup;
        }
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(Files.readAllBytes(chosen)));
        return properties;
    }

    public static void save(Path target, Properties properties) throws IOException {
        Files.createDirectories(target.toAbsolutePath().normalize().getParent());
        properties.setProperty("schemaVersion", SCHEMA_VERSION);
        byte[] data = encode(properties);
        Path migrationBackup = sibling(target, ".v4.bak");
        if (Files.exists(target) && !Files.exists(migrationBackup)) Files.copy(target, migrationBackup);

        Path backup = sibling(target, ".bak");
        Path checksum = sibling(target, ".sha256");
        Path backupChecksum = sibling(target, ".bak.sha256");
        if (Files.exists(target)) {
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(backupChecksum, sha256(Files.readAllBytes(backup)), StandardCharsets.US_ASCII,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        atomicWrite(target, data);
        atomicWrite(checksum, sha256(data).getBytes(StandardCharsets.US_ASCII));
    }

    public static void appendAudit(Path root, String jsonLine) throws IOException {
        Files.createDirectories(root);
        Path audit = root.resolve("audit-v5.jsonl");
        try (FileChannel channel = FileChannel.open(audit, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            channel.write(StandardCharsets.UTF_8.encode(jsonLine + System.lineSeparator()));
            channel.force(true);
        }
    }

    private static byte[] encode(Properties properties) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(output, "HariShop V5 atomic market state");
        return output.toByteArray();
    }

    private static void atomicWrite(Path target, byte[] data) throws IOException {
        Path temp = sibling(target, ".tmp");
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(java.nio.ByteBuffer.wrap(data));
            channel.force(true);
        }
        try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static boolean verify(Path data, Path checksum) throws IOException {
        return sha256(Files.readAllBytes(data)).equalsIgnoreCase(Files.readString(checksum, StandardCharsets.US_ASCII).trim());
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    private static Path sibling(Path path, String suffix) { return path.resolveSibling(path.getFileName() + suffix); }
}


