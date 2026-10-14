package hari.harishop.v5;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Source snapshots replace older observations. This prevents repeated scans from
 * manufacturing supply. Counts are exact only for sources that are observable.
 */
public final class SupplyLedger {
    public enum Coverage { COMPLETE, PARTIAL, UNKNOWN }

    public record Snapshot(Map<String, Long> counts, Instant observedAt, Coverage coverage, double confidence) {
        public Snapshot {
            Objects.requireNonNull(counts, "counts");
            Objects.requireNonNull(observedAt, "observedAt");
            Objects.requireNonNull(coverage, "coverage");
            if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) throw new IllegalArgumentException("confidence");
            Map<String, Long> copy = new HashMap<>();
            counts.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null || value < 0) throw new IllegalArgumentException("invalid supply entry");
                copy.put(key, value);
            });
            counts = Map.copyOf(copy);
        }
    }

    public record Observation(long count, Coverage coverage, double confidence, boolean stale, int sources) {
        public Observation {
            if (count < 0L || sources < 0 || !Double.isFinite(confidence) || confidence < 0d || confidence > 1d) throw new IllegalArgumentException("observation");
        }
        public boolean usableForPrice() { return !stale && sources > 0 && coverage != Coverage.UNKNOWN && confidence >= .75; }
    }

    private final Map<String, Snapshot> sources = new HashMap<>();
    private final Duration maxAge;

    public SupplyLedger(Duration maxAge) {
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) throw new IllegalArgumentException("maxAge");
        this.maxAge = maxAge;
    }

    public synchronized void replace(String sourceId, Snapshot snapshot) {
        if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId");
        sources.put(sourceId, Objects.requireNonNull(snapshot));
    }

    public synchronized void remove(String sourceId) { sources.remove(sourceId); }

    public synchronized Observation observe(String variantKey, Instant now) {
        Objects.requireNonNull(variantKey, "variantKey");
        Objects.requireNonNull(now, "now");
        long total = 0;
        double weightedConfidence = 0;
        int used = 0;
        Coverage coverage = Coverage.COMPLETE;
        boolean anyFresh = false;
        for (Snapshot snapshot : sources.values()) {
            if (snapshot.observedAt().isAfter(now) || Duration.between(snapshot.observedAt(), now).compareTo(maxAge) > 0) continue;
            anyFresh = true;
            long count = snapshot.counts().getOrDefault(variantKey, 0L);
            // Saturating aggregation prevents a corrupt/hostile source from
            // crashing the market thread.  MAX is abundance evidence only;
            // it can never manufacture a scarcity premium.
            total = saturatingAdd(total, count);
            weightedConfidence += snapshot.confidence();
            used++;
            if (snapshot.coverage() == Coverage.UNKNOWN) coverage = Coverage.UNKNOWN;
            else if (snapshot.coverage() == Coverage.PARTIAL && coverage == Coverage.COMPLETE) coverage = Coverage.PARTIAL;
        }
        if (!anyFresh) return new Observation(0, Coverage.UNKNOWN, 0, true, 0);
        return new Observation(total, coverage, weightedConfidence / used, false, used);
    }

    public synchronized int sourceCount() { return sources.size(); }

    public synchronized Map<String, Snapshot> snapshotSources() { return Map.copyOf(sources); }

    /** Unknown/stale observations are deliberately neutral, never artificial scarcity. */
    public static double boundedSupplyFactor(Observation observation, long referenceStock) {
        if (observation == null || !observation.usableForPrice() || referenceStock <= 0) return 1.0;
        double ratio = (observation.count() + 1d) / (referenceStock + 1d);
        // Observable inventories are not proof that the rest of the world is
        // scarce (unloaded chunks and opaque mod networks may still contain the
        // item). Supply evidence may therefore discount abundance, but may not
        // manufacture a scarcity premium.
        return clamp(Math.pow(ratio, -0.12), .80, 1.00);
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static long saturatingAdd(long left, long right) {
        if (right < 0L || left < 0L) throw new IllegalArgumentException("negative supply");
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}


