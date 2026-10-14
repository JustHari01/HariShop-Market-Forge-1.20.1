package hari.harishop.v5;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Canonical variant identity plus fail-closed component routing. */
public final class VariantPolicy {
    public enum Status { AUTO, REVIEW, MANUAL }
    public record ComponentValue(String id, String canonicalValue) {}
    public record Decision(String key, Status status, List<String> unresolved) {}

    private final Set<String> understood = new HashSet<>();
    private final Set<String> cosmetic = new HashSet<>();

    public VariantPolicy understand(String componentId) { understood.add(componentId); return this; }
    public VariantPolicy cosmetic(String componentId) { cosmetic.add(componentId); return this; }

    public Decision classify(String itemId, List<ComponentValue> components) {
        if (itemId == null || itemId.isBlank() || components == null) throw new IllegalArgumentException("variant");
        List<ComponentValue> meaningful = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (ComponentValue component : components) {
            if (component == null || component.id() == null || component.canonicalValue() == null) throw new IllegalArgumentException("component");
            if (cosmetic.contains(component.id())) continue;
            meaningful.add(component);
            if (!understood.contains(component.id())) unresolved.add(component.id());
        }
        meaningful.sort(Comparator.comparing(ComponentValue::id).thenComparing(ComponentValue::canonicalValue));
        byte[] canonical = canonicalBytes(itemId, meaningful);
        Status status = unresolved.isEmpty() ? Status.AUTO : unresolved.stream().anyMatch(id -> !id.startsWith("minecraft:")) ? Status.MANUAL : Status.REVIEW;
        return new Decision(itemId + "@v5@" + sha256(canonical), status, List.copyOf(unresolved));
    }

    /** Exposed for persistence and collision fallback; callers must retain all bytes. */
    public static byte[] canonicalBytes(String itemId, List<ComponentValue> components) {
        if (itemId == null || itemId.isBlank() || components == null) throw new IllegalArgumentException("variant");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            write(out, itemId);
            for (ComponentValue component : components) {
                if (component == null || component.id() == null || component.canonicalValue() == null) throw new IllegalArgumentException("component");
                write(out, component.id());
                write(out, component.canonicalValue());
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void write(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 16 * 1024 * 1024) throw new IllegalArgumentException("component too large");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String sha256(byte[] value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }
}


