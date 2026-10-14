package hari.harishop.v5;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Content fingerprint for every static input that may alter item acquisition.
 * Mutable HariShop journals/holdings are excluded because they are outputs of
 * the economy, not rules that define it.
 */
public final class ClosedWorldFingerprint {
    private static final int MAX_FILES = 100_000;
    private static final long MAX_FILE_BYTES = 1L << 30;
    private static final int BUFFER_SIZE = 1 << 20;
    /**
     * Config files written by performance/client helpers during every boot.
     * They are not economic rules; including them would make the exact-world
     * certificate depend on a runtime tuning counter rather than the pack
     * semantics.  Keep this list explicit instead of accepting a broad
     * wildcard so a newly discovered mutable file fails closed until reviewed.
     */
    private static final Set<String> RUNTIME_MUTABLE_CONFIGS = Set.of(
            "/config/asyncparticles/asyncparticles-mixin.properties",
            "/config/vmp.properties",
            "/config/worldedit/worldedit.properties");

    public record Result(String sha256, int files, long bytes, boolean complete, String failure) {}

    private record Entry(String logicalPath, Path file, long size) {}

    private ClosedWorldFingerprint() {}

    public static Result calculate(Path gameDirectory, Path worldDirectory) {
        if (gameDirectory == null) return failed("Không xác định được game directory");
        Path game = gameDirectory.toAbsolutePath().normalize();
        ArrayList<Entry> entries = new ArrayList<>();
        try {
            collect(entries, game.resolve("mods"), "mods", ClosedWorldFingerprint::activeMod);
            collect(entries, game.resolve("config"), "config", ClosedWorldFingerprint::economicConfig);
            collect(entries, game.resolve("defaultconfigs"), "defaultconfigs", path -> true);
            collect(entries, game.resolve("scripts"), "scripts", path -> true);
            collect(entries, game.resolve("kubejs").resolve("server_scripts"), "kubejs/server_scripts", path -> true);
            collect(entries, game.resolve("kubejs").resolve("startup_scripts"), "kubejs/startup_scripts", path -> true);
            collect(entries, game.resolve("kubejs").resolve("data"), "kubejs/data", path -> true);
            if (worldDirectory != null) {
                Path world = worldDirectory.toAbsolutePath().normalize();
                collect(entries, world.resolve("datapacks"), "world/datapacks", path -> true);
                // NeoForge copies per-world COMMON/SERVER configs here. They can
                // alter machine costs, drops and generation without changing a JAR.
                collect(entries, world.resolve("serverconfig"), "world/serverconfig", path -> true);
            }
            if (entries.size() > MAX_FILES) return failed("Quá nhiều file đầu vào kinh tế: " + entries.size());
            entries.sort(Comparator.comparing(Entry::logicalPath));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytes = 0L;
            for (Entry entry : entries) {
                if (Thread.currentThread().isInterrupted()) return failed("Fingerprint bị hủy");
                updateLengthPrefixed(digest, entry.logicalPath().getBytes(StandardCharsets.UTF_8));
                digest.update(ByteBuffer.allocate(Long.BYTES).putLong(entry.size()).array());
                try (InputStream input = Files.newInputStream(entry.file())) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        if (read == 0) continue;
                        digest.update(buffer, 0, read);
                        bytes = Math.addExact(bytes, read);
                        if (Thread.currentThread().isInterrupted()) return failed("Fingerprint bị hủy");
                    }
                }
            }
            return new Result(hex(digest.digest()), entries.size(), bytes, true, "");
        }
        catch (Throwable failure) {
            return failed(failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage()));
        }
    }

    private static void collect(List<Entry> out, Path root, String label, FileFilter filter) throws IOException {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return;
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(dir)) return FileVisitResult.SKIP_SUBTREE;
                if (out.size() > MAX_FILES) throw new IOException("file limit");
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Atomic writers create these transient siblings while
                // replacing JSON/TOML. They are not a gameplay input and may
                // disappear between directory walk and hashing; including
                // them made a valid server fail certification nondeterministically.
                if (!attrs.isRegularFile() || Files.isSymbolicLink(file)
                        || isTransient(file) || !filter.include(file)) return FileVisitResult.CONTINUE;
                long size = attrs.size();
                if (size < 0L || size > MAX_FILE_BYTES) throw new IOException("file too large: " + file.getFileName());
                String relative = normalizedRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
                out.add(new Entry(label + '/' + relative, file, size));
                if (out.size() > MAX_FILES) throw new IOException("file limit");
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean activeMod(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    private static boolean economicConfig(Path path) {
        String logical = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        // These are runtime state/diagnostic artefacts, not rules.  Hashing
        // them made an otherwise identical world produce a new certificate
        // after every boot (for example bountiful errors, generated caches,
        // ProClaim's mutable claim data and config backups/checksums).
        if (fileName.endsWith(".log") || fileName.endsWith(".bak")
                || fileName.endsWith(".sha256") || fileName.endsWith(".tmp")
                || logical.contains("/cache/") || logical.contains("/tmp-client/")) {
            return false;
        }
        if (logical.endsWith("/config/proclaimauthforge/data.json")
                || logical.contains("/config/proclaimauthforge/data.json.")) {
            return false;
        }
        // These files are rewritten by the loaded performance/client mods at
        // boot.  They do not affect item acquisition, recipes, drops or
        // currency policy, so they belong to runtime state rather than the
        // closed-world economic input set.
        if (RUNTIME_MUTABLE_CONFIGS.stream().anyMatch(logical::endsWith)) return false;
        if (!logical.contains("/harishop-market/")) return true;
        // Release evidence is an assertion about the already-fingerprinted
        // inputs, not an economic rule.  Including it here would create a
        // circular certificate (placing the manifest changes the fingerprint
        // that the manifest itself must contain).
        if (logical.endsWith("/release-gates.properties")) return false;
        if (logical.endsWith("/market-state.properties")
                || logical.endsWith("/variant-v4-state.properties")) return false;
        return logical.endsWith("/overrides.properties")
                || logical.endsWith("/valuation-adapters.properties")
                || logical.endsWith("/economy-market.properties");
    }

    private static boolean isTransient(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".tmp") || name.endsWith(".part") || name.endsWith(".swp")
                || name.endsWith(".lck") || name.endsWith("~") || name.startsWith(".~lock.");
    }

    private static void updateLengthPrefixed(MessageDigest digest, byte[] value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
        digest.update(value);
    }

    private static Result failed(String message) {
        return new Result("", 0, 0L, false, message == null ? "unknown" : message);
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) builder.append(String.format("%02x", value));
        return builder.toString();
    }

    @FunctionalInterface
    private interface FileFilter {
        boolean include(Path path);
    }
}


