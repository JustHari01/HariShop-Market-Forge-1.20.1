package hari.harishop.v5;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Strict, external evidence manifest for the release gates that cannot be
 * established by a normal server boot.  In particular, a clean SQLite crash
 * test is not evidence of atomicity across playerdata/chunk files, and a
 * client functional harness is not evidence of native pixel coverage.  Those
 * distinctions are kept in a separate manifest so a stale PASS record cannot
 * accidentally open the economy certificate.
 *
 * <p>The file is deliberately operator/test generated.  Runtime code never
 * infers PASS from a missing field, a successful Gradle build, or a prose
 * note.  The artifact hashes and runtime fingerprint are checked again by the
 * caller against the loader's actual manifest.</p>
 */
public final class ReleaseGateEvidence {
    public static final String FILE_NAME = "release-gates.properties";
    private static final String REVISION = "2";

    public record Result(boolean present,
                         boolean physicalBoundary,
                         boolean crossResourceProtocol,
                         boolean nativePixel,
                         boolean jfrBudget,
                         boolean fullPackRestart,
                         boolean closedWorldInventory,
                         String fingerprint,
                         int runtimeFileCount,
                         int modpackJarCount,
                         Map<String, String> artifactHashes,
                         String digest,
                         List<String> failures) {
        public Result {
            fingerprint = fingerprint == null ? "" : fingerprint;
            artifactHashes = Map.copyOf(artifactHashes == null ? Map.of() : artifactHashes);
            digest = digest == null ? "" : digest;
            failures = List.copyOf(failures == null ? List.of() : failures);
            if (runtimeFileCount < 0 || modpackJarCount < 0) throw new IllegalArgumentException("inventory count");
        }

        public boolean complete() {
            return present && failures.isEmpty()
                    && physicalBoundary && crossResourceProtocol && nativePixel
                    && jfrBudget && fullPackRestart && closedWorldInventory;
        }

        /**
         * Bind the evidence to the exact running loader view.  A PASS file
         * copied from another modpack, world or artifact build therefore
         * remains unusable.
         */
        public boolean matches(String runtimeFingerprint, int runtimeFileCount,
                               int runtimeModpackJarCount,
                               Map<String, String> runtimeArtifacts) {
            if (!complete() || runtimeFingerprint == null
                    || !fingerprint.equalsIgnoreCase(runtimeFingerprint)
                    || this.runtimeFileCount() != runtimeFileCount
                    || modpackJarCount != runtimeModpackJarCount
                    || runtimeArtifacts == null) {
                return false;
            }
            for (String modId : REQUIRED_ARTIFACTS) {
                String expected = artifactHashes.get(modId);
                String actual = runtimeArtifacts.get(modId);
                if (expected == null || actual == null || !expected.equalsIgnoreCase(actual)) return false;
            }
            return true;
        }
    }

    private static final List<String> REQUIRED_STATUS_KEYS = List.of(
            "physicalBoundary", "crossResourceProtocol", "nativePixel",
            "jfrBudget", "fullPackRestart", "closedWorldInventory");
    private static final List<String> REQUIRED_ARTIFACTS = List.of(
            "harishop", "numismaticoverhaul", "proclaimauthforge");

    private ReleaseGateEvidence() {}

    public static Result load(Path root) {
        if (root == null) return missing("root không tồn tại");
        Path path = root.toAbsolutePath().normalize().resolve(FILE_NAME);
        if (!Files.isRegularFile(path)) return missing("thiếu " + FILE_NAME);
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (Throwable failure) {
            return missing("không đọc được " + FILE_NAME + ": " + shortError(failure));
        }

        List<String> failures = new ArrayList<>();
        if (!"PASS".equals(properties.getProperty("status", ""))) {
            failures.add("status không phải PASS");
        }
        if (!REVISION.equals(properties.getProperty("evidenceRevision", ""))) {
            failures.add("evidenceRevision không được hỗ trợ");
        }
        if (properties.getProperty("generatedAtUtc", "").isBlank()) {
            failures.add("thiếu generatedAtUtc");
        }
        // The design explicitly does not claim a single physical ACID
        // transaction across SQLite and Minecraft playerdata/chunk files.
        // A release record must say so, while proving the receipt/escrow
        // protocol that prevents a free item or free payout.
        if (!"UNPROVEN".equals(properties.getProperty("physicalAcidProof", ""))) {
            failures.add("physicalAcidProof phải là UNPROVEN");
        }
        boolean[] statuses = new boolean[REQUIRED_STATUS_KEYS.size()];
        for (int i = 0; i < REQUIRED_STATUS_KEYS.size(); i++) {
            String key = REQUIRED_STATUS_KEYS.get(i);
            statuses[i] = "PASS".equals(properties.getProperty(key, ""));
            if (!statuses[i]) failures.add(key + " chưa PASS");
        }

        String fingerprint = properties.getProperty("fingerprint", "");
        if (!isSha256(fingerprint)) failures.add("fingerprint không hợp lệ");
        int runtimeFileCount = parseCount("runtimeFiles", properties.getProperty("runtimeFiles", ""), failures);
        int modpackJarCount = parseCount("modpackJars", properties.getProperty("modpackJars", ""), failures);
        if (runtimeFileCount > 4096) failures.add("runtimeFiles vượt giới hạn");
        if (modpackJarCount > runtimeFileCount && runtimeFileCount > 0) {
            failures.add("modpackJars lớn hơn runtimeFiles");
        }
        Map<String, String> artifacts = new LinkedHashMap<>();
        for (String modId : REQUIRED_ARTIFACTS) {
            String hash = properties.getProperty("artifact." + modId, "");
            if (!isSha256(hash)) failures.add("artifact hash không hợp lệ: " + modId);
            artifacts.put(modId, hash.toLowerCase(java.util.Locale.ROOT));
        }
        String digest = canonicalDigest(properties);
        return new Result(true, statuses[0], statuses[1], statuses[2], statuses[3],
                statuses[4], statuses[5], fingerprint, runtimeFileCount, modpackJarCount,
                artifacts, digest, failures);
    }

    private static Result missing(String failure) {
        return new Result(false, false, false, false, false, false, false,
                "", 0, 0, Map.of(), "", List.of(failure));
    }

    private static int parseCount(String key, String value, List<String> failures) {
        try {
            int count = Integer.parseInt(value);
            if (count <= 0) throw new NumberFormatException("non-positive");
            return count;
        } catch (NumberFormatException failure) {
            failures.add(key + " không hợp lệ");
            return 0;
        }
    }

    private static String canonicalDigest(Properties properties) {
        try {
            StringBuilder canonical = new StringBuilder();
            properties.stringPropertyNames().stream().sorted(Comparator.naturalOrder())
                    .forEach(key -> canonical.append(key).append('=').append(properties.getProperty(key, ""))
                            .append('\n'));
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) out.append(String.format("%02x", value));
            return out.toString();
        } catch (Throwable failure) {
            return "";
        }
    }

    private static boolean isSha256(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }

    private static String shortError(Throwable failure) {
        if (failure == null) return "unknown";
        String message = failure.getMessage();
        return failure.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}


