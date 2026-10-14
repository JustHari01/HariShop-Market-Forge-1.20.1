package hari.harishop.v5;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exact verifier for the no-free-money invariant.  Solver output is never
 * trusted: this class replays each route using integer arithmetic and returns
 * the first failing witness.  Unknown inputs/outputs are blocked rather than
 * priced from a heuristic.
 */
public final class EconomicCertificate {
    public record Edge(String id, Map<String, Long> inputs, Map<String, Long> outputs, long consumedBudget) {
        public Edge {
            if (id == null || id.isBlank() || id.length() > 256) throw new IllegalArgumentException("edge id");
            inputs = validate(inputs, "inputs", false);
            outputs = validate(outputs, "outputs", true);
            MoneyMath.nonNegative(consumedBudget, "consumedBudget");
        }

        private static Map<String, Long> validate(Map<String, Long> value, String name, boolean nonEmpty) {
            if (value == null || (nonEmpty && value.isEmpty())) throw new IllegalArgumentException(name);
            Map<String, Long> copy = new LinkedHashMap<>();
            value.forEach((key, amount) -> {
                if (key == null || key.isBlank() || amount == null || amount <= 0L) throw new IllegalArgumentException(name);
                copy.merge(key, amount, (left, right) -> {
                    try { return Math.addExact(left, right); }
                    catch (ArithmeticException overflow) { throw new IllegalArgumentException(name + " overflow"); }
                });
            });
            return Map.copyOf(copy);
        }
    }

    public record Result(boolean passed, Map<String, Long> potential, List<String> blockedEdges, List<String> witnesses) {
        public Result {
            potential = Map.copyOf(potential == null ? Map.of() : potential);
            blockedEdges = List.copyOf(blockedEdges == null ? List.of() : blockedEdges);
            witnesses = List.copyOf(witnesses == null ? List.of() : witnesses);
        }
    }

    private EconomicCertificate() {}

    /**
     * Checks every route against one common potential.  A route is safe only
     * when the worst-case value of all outputs (including by-products) is no
     * greater than all inputs plus explicitly consumed resources.
     */
    public static Result verify(List<Edge> edges, Map<String, Long> potential, Map<String, Long> payouts) {
        if (edges == null || potential == null || payouts == null) throw new IllegalArgumentException("certificate input");
        Map<String, Long> cleanPotential = cleanValues(potential, "potential");
        Map<String, Long> cleanPayouts = cleanValues(payouts, "payouts");
        List<String> blocked = new ArrayList<>();
        List<String> witnesses = new ArrayList<>();
        for (Edge edge : edges) {
            try {
                BigInteger lhs = BigInteger.ZERO;
                for (var output : edge.outputs().entrySet()) {
                    Long known = cleanPotential.get(output.getKey());
                    if (known == null) known = cleanPayouts.get(output.getKey());
                    if (known == null) throw new UnknownAsset(output.getKey());
                    long value = known;
                    lhs = lhs.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(output.getValue())));
                }
                // A consumed resource/energy budget is an additional input
                // potential, not an output.  Putting it on the wrong side
                // would reject safe machine routes and, worse, distort the
                // common potential used to prove cycles.
                BigInteger rhs = BigInteger.valueOf(edge.consumedBudget());
                for (var input : edge.inputs().entrySet()) {
                    Long value = cleanPotential.get(input.getKey());
                    if (value == null) throw new UnknownAsset(input.getKey());
                    rhs = rhs.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(input.getValue())));
                }
                if (lhs.compareTo(rhs) > 0) {
                    blocked.add(edge.id());
                    witnesses.add(edge.id() + ": output potential " + lhs + " > input potential " + rhs);
                }
            } catch (UnknownAsset unknown) {
                blocked.add(edge.id());
                witnesses.add(edge.id() + ": unknown asset " + unknown.asset);
            } catch (ArithmeticException | IllegalArgumentException invalid) {
                blocked.add(edge.id());
                witnesses.add(edge.id() + ": invalid arithmetic " + invalid.getMessage());
            }
        }
        return new Result(blocked.isEmpty(), cleanPotential, blocked, witnesses);
    }

    /**
     * Computes conservative integer lower bounds.  Cycles without a source
     * remain unknown; they are intentionally not assigned a value.
     */
    public static Map<String, Long> lowerBounds(List<Edge> edges, Map<String, Long> sourceCosts) {
        if (edges == null || sourceCosts == null) throw new IllegalArgumentException("lower bound input");
        Map<String, Long> values = new HashMap<>(cleanValues(sourceCosts, "sourceCosts"));
        int limit = Math.max(1, (edges.size() + values.size()) * 4);
        for (int pass = 0; pass < limit; pass++) {
            boolean changed = false;
            for (Edge edge : edges) {
                BigInteger input = BigInteger.valueOf(edge.consumedBudget());
                boolean known = true;
                for (var part : edge.inputs().entrySet()) {
                    Long value = values.get(part.getKey());
                    if (value == null) { known = false; break; }
                    input = input.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(part.getValue())));
                }
                if (!known) continue;
                for (var output : edge.outputs().entrySet()) {
                    BigInteger candidate = input.divide(BigInteger.valueOf(output.getValue()));
                    if (input.mod(BigInteger.valueOf(output.getValue())).signum() != 0) candidate = candidate.add(BigInteger.ONE);
                    if (candidate.compareTo(BigInteger.valueOf(MoneyMath.MAX_BALANCE)) > 0) continue;
                    long next = candidate.longValueExact();
                    Long old = values.get(output.getKey());
                    if (old == null || next < old) { values.put(output.getKey(), next); changed = true; }
                }
            }
            if (!changed) break;
        }
        return Map.copyOf(values);
    }

    private static Map<String, Long> cleanValues(Map<String, Long> source, String name) {
        Map<String, Long> clean = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) throw new IllegalArgumentException(name);
            MoneyMath.nonNegative(value, name + "." + key);
            clean.put(key, value);
        });
        return clean;
    }

    private static final class UnknownAsset extends RuntimeException {
        private final String asset;
        private UnknownAsset(String asset) { this.asset = Objects.requireNonNull(asset); }
    }
}


