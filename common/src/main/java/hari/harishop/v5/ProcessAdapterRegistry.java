package hari.harishop.v5;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;

/**
 * Deterministic process-adapter registry used by the reindex/certificate
 * pipeline.  It rejects duplicate ownership and any adapter that fails its
 * startup self-test.  This is deliberately not a fallback dispatcher: an
 * unknown process type remains unsupported and must be denied by policy.
 */
public final class ProcessAdapterRegistry {
    public record Decision(boolean supported, String adapterId, String reason) {
        public Decision {
            adapterId = adapterId == null ? "" : adapterId;
            reason = reason == null ? "" : reason;
        }
    }

    private final Map<String, ProcessAdapter> byType;
    private final List<ProcessAdapter> adapters;
    private final String coverageDigest;

    public ProcessAdapterRegistry(Collection<? extends ProcessAdapter> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("at least one process adapter is required");
        }
        LinkedHashMap<String, ProcessAdapter> types = new LinkedHashMap<>();
        ArrayList<ProcessAdapter> normalized = new ArrayList<>();
        for (ProcessAdapter adapter : source) {
            if (adapter == null || blank(adapter.id()) || blank(adapter.version())
                    || adapter.typeIds() == null || adapter.typeIds().isEmpty()
                    || adapter.semanticContract() == null) {
                throw new IllegalArgumentException("invalid process adapter");
            }
            Set<String> ids = new LinkedHashSet<>();
            for (String type : adapter.typeIds()) {
                if (blank(type) || !ids.add(type)) throw new IllegalArgumentException("invalid adapter type");
                if (types.putIfAbsent(type, adapter) != null) {
                    throw new IllegalArgumentException("duplicate process adapter type: " + type);
                }
            }
            normalized.add(adapter);
        }
        normalized.sort(Comparator.comparing(ProcessAdapter::id).thenComparing(ProcessAdapter::version));
        this.adapters = List.copyOf(normalized);
        this.byType = Map.copyOf(types);
        this.coverageDigest = digest(this.adapters);
    }

    public Decision decide(String typeId) {
        ProcessAdapter adapter = this.byType.get(typeId);
        if (adapter == null) return new Decision(false, "", "no adapter registered for " + String.valueOf(typeId));
        if (!adapter.selfTest()) return new Decision(false, adapter.id(), "adapter self-test failed: " + adapter.id());
        if (!adapter.semanticSelfTest()) {
            String missing = String.join(",", adapter.semanticContract().missing());
            return new Decision(false, adapter.id(), "semantic contract incomplete: " + missing);
        }
        return new Decision(true, adapter.id(), "");
    }

    public boolean selfTestsPass() {
        return adapters.stream().allMatch(ProcessAdapter::semanticSelfTest);
    }

    public List<ProcessAdapter> adapters() { return adapters; }

    public String coverageDigest() { return coverageDigest; }

    /** Canonical text is included in graph/certificate evidence for audit. */
    public String canonicalText() {
        StringBuilder text = new StringBuilder();
        for (ProcessAdapter adapter : adapters) {
            List<String> types = new ArrayList<>(adapter.typeIds());
            types.sort(String::compareTo);
            text.append(adapter.id()).append('|').append(adapter.version()).append('|')
                    .append(adapter.semanticSelfTest()).append('|')
                    .append(adapter.semanticContract().canonical()).append('|')
                    .append(String.join(",", types)).append('\n');
        }
        return text.toString();
    }

    public static ProcessAdapterRegistry standard() {
        return new ProcessAdapterRegistry(List.of(
                new FixedAdapter("minecraft-declarative-v1", "1.21.1",
                        Set.of("minecraft:crafting", "minecraft:smelting", "minecraft:blasting",
                                "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting",
                                "minecraft:smithing"), completeContract("minecraft-recipe-codec-v1")),
                new FixedAdapter("create-item-process-v1", "0.5.x-item-only",
                        Set.of("create:milling", "create:crushing", "create:pressing", "create:cutting",
                                "create:splashing", "create:haunting", "create:sandpaper_polishing",
                                "create:deploying", "create:item_application", "create:mixing",
                                "create:compacting", "create:mechanical_crafting"),
                        completeContract("create-item-process-codec-v1"))));
    }

    private static ProcessAdapter.SemanticContract completeContract(String version) {
        return new ProcessAdapter.SemanticContract(true, true, true, true, true, true,
                true, true, version, version);
    }

    private static String digest(List<ProcessAdapter> adapters) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder canonical = new StringBuilder();
            for (ProcessAdapter adapter : adapters) {
                List<String> types = new ArrayList<>(adapter.typeIds());
                types.sort(String::compareTo);
                canonical.append(adapter.id()).append('|').append(adapter.version()).append('|')
                        .append(adapter.semanticSelfTest()).append('|')
                        .append(adapter.semanticContract().canonical()).append('|')
                        .append(String.join(",", types)).append('\n');
            }
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException("cannot hash process adapter coverage", failure);
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private record FixedAdapter(String id, String version, Set<String> typeIds,
                                SemanticContract semanticContract) implements ProcessAdapter {
        private FixedAdapter {
            typeIds = Set.copyOf(typeIds);
        }

        @Override
        public boolean selfTest() {
            return !id.isBlank() && !version.isBlank() && !typeIds.isEmpty()
                    && typeIds.stream().noneMatch(String::isBlank);
        }
    }
}


