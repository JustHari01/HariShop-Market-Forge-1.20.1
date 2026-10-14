package hari.harishop.v5;

import java.time.Instant;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Event-sourced supply counters with replacement snapshots per source. */
public final class WorldSupplyCensus {
    public enum Metric { CIRCULATING, INSTALLED, RESERVED, PHYSICAL_TOTAL, ECONOMIC_ELIGIBLE }
    public enum Coverage { COMPLETE, PARTIAL, UNKNOWN }

    public record Snapshot(String sourceId, Map<String, EnumMap<Metric, Long>> counts, Instant observedAt,
                           Coverage coverage, String digest) {
        public Snapshot {
            if (sourceId == null || sourceId.isBlank() || observedAt == null || coverage == null || digest == null || digest.isBlank()) throw new IllegalArgumentException("snapshot");
            Objects.requireNonNull(counts, "counts");
            Map<String, EnumMap<Metric, Long>> copy = new HashMap<>();
            counts.forEach((asset, metrics) -> {
                if (asset == null || asset.isBlank() || metrics == null) throw new IllegalArgumentException("asset");
                EnumMap<Metric, Long> metricCopy = new EnumMap<>(Metric.class);
                metrics.forEach((metric, count) -> {
                    if (metric == null || count == null || count < 0L) throw new IllegalArgumentException("count");
                    metricCopy.put(metric, count);
                });
                copy.put(asset, metricCopy);
            });
            counts = Map.copyOf(copy);
        }

        /** Defensive copy prevents callers from mutating a published snapshot. */
        @Override public Map<String, EnumMap<Metric, Long>> counts() {
            Map<String, EnumMap<Metric, Long>> copy = new HashMap<>();
            for (var entry : superCounts().entrySet()) copy.put(entry.getKey(), new EnumMap<>(entry.getValue()));
            return Map.copyOf(copy);
        }

        // Records do not expose a separate super accessor; this helper reads
        // the canonical component generated backing field through the
        // explicitly named accessor replacement above.
        private Map<String, EnumMap<Metric, Long>> superCounts() { return counts; }
    }

    public record Observation(Map<Metric, Long> totals, Coverage coverage, Instant newestAt, int sources) {
        public Observation {
            if (totals == null) totals = Map.of();
            EnumMap<Metric, Long> checked = new EnumMap<>(Metric.class);
            totals.forEach((metric, count) -> {
                if (metric == null || count == null || count < 0L) throw new IllegalArgumentException("totals");
                checked.put(metric, count);
            });
            totals = Map.copyOf(checked);
            coverage = Objects.requireNonNull(coverage);
            if (sources < 0) throw new IllegalArgumentException("sources");
        }
        public long get(Metric metric) { return totals.getOrDefault(metric, 0L); }
    }

    private final Map<String, Snapshot> snapshots = new HashMap<>();
    private final Duration maxAge;

    public WorldSupplyCensus() { this(Duration.ofMinutes(45)); }

    public WorldSupplyCensus(Duration maxAge) {
        if (maxAge == null || maxAge.isZero() || maxAge.isNegative()) throw new IllegalArgumentException("maxAge");
        this.maxAge = maxAge;
    }

    public synchronized void replace(Snapshot snapshot) { snapshots.put(snapshot.sourceId(), snapshot); }
    public synchronized void remove(String sourceId) { snapshots.remove(sourceId); }
    public synchronized int sourceCount() { return snapshots.size(); }

    public synchronized Observation observe(String asset, Instant now) {
        if (asset == null || asset.isBlank() || now == null) throw new IllegalArgumentException("observation");
        EnumMap<Metric, Long> totals = new EnumMap<>(Metric.class);
        Coverage combined = Coverage.COMPLETE;
        Instant newest = null;
        int sources = 0;
        for (Snapshot snapshot : snapshots.values()) {
            if (snapshot.observedAt().isAfter(now) || Duration.between(snapshot.observedAt(), now).compareTo(maxAge) > 0) continue;
            EnumMap<Metric, Long> values = snapshot.counts().get(asset);
            if (values == null && snapshot.coverage() != Coverage.COMPLETE) continue;
            sources++;
            newest = newest == null || snapshot.observedAt().isAfter(newest) ? snapshot.observedAt() : newest;
            if (values != null) for (var entry : values.entrySet()) totals.merge(entry.getKey(), entry.getValue(), WorldSupplyCensus::saturatingAdd);
            if (snapshot.coverage() == Coverage.UNKNOWN) combined = Coverage.UNKNOWN;
            else if (snapshot.coverage() == Coverage.PARTIAL && combined == Coverage.COMPLETE) combined = Coverage.PARTIAL;
        }
        if (sources == 0) combined = Coverage.UNKNOWN;
        return new Observation(totals, combined, newest, sources);
    }

    /** Only certified complete observations may affect a scarcity/fairness input. */
    public static boolean usableForScarcity(Observation observation) {
        return observation != null && observation.sources() > 0 && observation.coverage() == Coverage.COMPLETE;
    }

    private static long saturatingAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}


