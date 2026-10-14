package hari.harishop.v5;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic candidate solver for the economic potential inequalities.
 *
 * <p>This class is deliberately not an authority. It uses an integer-only
 * relaxation so the Forge backport has no native/solver dependency. Callers
 * must still pass the result through {@link EconomicCertificate#verify(List,
 * Map, Map)} before a bid or route is opened. Unknown or disconnected assets
 * are marked incomplete and must remain locked.</p>
 */
public final class PotentialSolver {
    private static final int MAX_ASSETS = 8192;
    private static final int MAX_EDGES = 32768;

    public record Edge(String id, Map<String, Long> inputs, Map<String, Long> outputs,
                       long consumedBudget) {
        public Edge {
            if (id == null || id.isBlank() || id.length() > 256) throw new IllegalArgumentException("edge id");
            inputs = clean(inputs, "inputs", false);
            outputs = clean(outputs, "outputs", true);
            MoneyMath.nonNegative(consumedBudget, "consumedBudget");
        }

        private static Map<String, Long> clean(Map<String, Long> source, String field, boolean output) {
            if (source == null || (output && source.isEmpty())) throw new IllegalArgumentException(field);
            Map<String, Long> result = new LinkedHashMap<>();
            source.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null || value <= 0L) {
                    throw new IllegalArgumentException(field);
                }
                result.merge(key, value, (left, right) -> {
                    try { return Math.addExact(left, right); }
                    catch (ArithmeticException overflow) { throw new IllegalArgumentException(field + " overflow"); }
                });
            });
            return Map.copyOf(result);
        }
    }

    public record Result(boolean complete, Map<String, Long> potential,
                         List<String> witnesses, String solverState) {
        public Result {
            potential = Map.copyOf(potential == null ? Map.of() : potential);
            witnesses = List.copyOf(witnesses == null ? List.of() : witnesses);
            solverState = solverState == null ? "UNKNOWN" : solverState;
        }
    }

    private PotentialSolver() {}

    /** Builds a bounded integer relaxation. Exact replay remains authoritative. */
    public static Result solve(List<Edge> edges, Map<String, Long> sourceCosts) {
        if (edges == null || sourceCosts == null) throw new IllegalArgumentException("potential input");
        if (edges.size() > MAX_EDGES || sourceCosts.size() > MAX_ASSETS) {
            return incomplete("MODEL_LIMIT", List.of("model exceeds deterministic LP limit"));
        }
        try {
            Map<String, Long> sources = cleanSources(sourceCosts);
            Set<String> assets = new LinkedHashSet<>(sources.keySet());
            for (Edge edge : edges) {
                assets.addAll(edge.inputs().keySet());
                assets.addAll(edge.outputs().keySet());
            }
            if (assets.isEmpty() || assets.size() > MAX_ASSETS) {
                return incomplete("MODEL_LIMIT", List.of("asset set is empty or too large"));
            }

            Set<String> reachable = reachableAssets(edges, sources.keySet());
            if (reachable.isEmpty()) return incomplete("NO_SOURCE_PATH", List.of("no asset is backed by a source"));

            Map<String, Long> integer = new LinkedHashMap<>(sources);
            for (String asset : assets) integer.putIfAbsent(asset, reachable.contains(asset) ? null : 0L);
            List<String> witnesses = new ArrayList<>();
            int limit = Math.max(16, (edges.size() + assets.size()) * 4);
            for (int pass = 0; pass < limit; pass++) {
                boolean changed = false;
                for (Edge edge : edges) {
                    java.math.BigInteger total = java.math.BigInteger.valueOf(edge.consumedBudget());
                    boolean known = true;
                    for (var input : edge.inputs().entrySet()) {
                        Long value = integer.get(input.getKey());
                        if (value == null) { known = false; break; }
                        total = total.add(java.math.BigInteger.valueOf(value)
                                .multiply(java.math.BigInteger.valueOf(input.getValue())));
                    }
                    if (!known) continue;
                    for (var output : edge.outputs().entrySet()) {
                        java.math.BigInteger[] qr = total.divideAndRemainder(
                                java.math.BigInteger.valueOf(output.getValue()));
                        long candidate;
                        try {
                            candidate = MoneyMath.checked(qr[0].add(qr[1].signum() == 0
                                    ? java.math.BigInteger.ZERO : java.math.BigInteger.ONE));
                        } catch (ArithmeticException invalid) {
                            witnesses.add(edge.id() + ":overflow");
                            continue;
                        }
                        Long old = integer.get(output.getKey());
                        if (old == null || candidate < old) { integer.put(output.getKey(), candidate); changed = true; }
                    }
                }
                if (!changed) break;
            }
            List<String> missing = new ArrayList<>();
            for (String asset : assets) if (integer.get(asset) == null) missing.add(asset);
            if (!missing.isEmpty()) {
                witnesses.add("unresolved:" + String.join(",", missing));
                return incomplete("NO_SOURCE_PATH", witnesses);
            }
            return new Result(true, integer, witnesses, "INTEGER_RELAXATION");
        } catch (Throwable failure) {
            return incomplete("ERROR:" + failure.getClass().getSimpleName(),
                    List.of(Objects.toString(failure.getMessage(), "LP failure")));
        }
    }

    private static Map<String, Long> cleanSources(Map<String, Long> sourceCosts) {
        Map<String, Long> clean = new LinkedHashMap<>();
        sourceCosts.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || value <= 0L) {
                throw new IllegalArgumentException("source cost");
            }
            MoneyMath.nonNegative(value, "source." + key);
            clean.put(key, value);
        });
        return clean;
    }

    /** Conservative hypergraph reachability: every input of an edge must be backed. */
    private static Set<String> reachableAssets(List<Edge> edges, Set<String> sources) {
        Set<String> reachable = new LinkedHashSet<>(sources);
        boolean changed;
        do {
            changed = false;
            for (Edge edge : edges) {
                if (!reachable.containsAll(edge.inputs().keySet())) continue;
                if (reachable.addAll(edge.outputs().keySet())) changed = true;
            }
        } while (changed);
        return reachable;
    }

    private static Result incomplete(String state, List<String> witnesses) {
        return new Result(false, Map.of(), witnesses, state);
    }
}

