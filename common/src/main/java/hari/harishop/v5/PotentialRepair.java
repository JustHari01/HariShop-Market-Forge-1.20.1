package hari.harishop.v5;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact integer repair for a candidate potential produced by ojAlgo.
 *
 * <p>The LP is only a candidate generator.  Floating point values and
 * flooring can leave a one-Dong witness on a route even though a conservative
 * integer potential exists.  This repair only decreases output potentials;
 * it never increases a value, invents an asset, or opens an unknown route.
 * The result is still replayed by {@link EconomicCertificate} before use.</p>
 */
public final class PotentialRepair {
    public record Result(boolean complete, Map<String, Long> potential, List<String> witnesses) {
        public Result {
            potential = Map.copyOf(potential == null ? Map.of() : potential);
            witnesses = List.copyOf(witnesses == null ? List.of() : witnesses);
        }
    }

    private PotentialRepair() {}

    public static Result repair(List<EconomicCertificate.Edge> edges, Map<String, Long> candidate) {
        if (edges == null || candidate == null || candidate.isEmpty()) {
            return failed("empty repair input");
        }
        Map<String, Long> values = new HashMap<>();
        try {
            candidate.forEach((id, value) -> {
                if (id == null || id.isBlank() || value == null || value < 0L) {
                    throw new IllegalArgumentException("invalid candidate");
                }
                values.put(id, value);
            });
        } catch (RuntimeException invalid) {
            return failed(invalid.getMessage());
        }

        // Each successful pass strictly lowers at least one non-negative long.
        // The deterministic bound prevents a pathological graph from consuming
        // the server thread forever; the exact verifier remains the authority.
        int limit = Math.max(1, Math.min(1_000_000, edges.size() * Math.max(1, values.size()) * 4));
        for (int pass = 0; pass < limit; pass++) {
            boolean changed = false;
            for (EconomicCertificate.Edge edge : edges) {
                BigInteger lhs = BigInteger.ZERO;
                for (var output : edge.outputs().entrySet()) {
                    Long value = values.get(output.getKey());
                    if (value == null) return failed("unknown output " + output.getKey());
                    lhs = lhs.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(output.getValue())));
                }
                BigInteger rhs = BigInteger.valueOf(edge.consumedBudget());
                for (var input : edge.inputs().entrySet()) {
                    Long value = values.get(input.getKey());
                    if (value == null) return failed("unknown input " + input.getKey());
                    rhs = rhs.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(input.getValue())));
                }
                if (lhs.compareTo(rhs) <= 0) continue;
                BigInteger excess = lhs.subtract(rhs);
                String chosen = null;
                long chosenCount = 0L;
                long chosenValue = 0L;
                // Prefer the largest positive output so one correction does not
                // unnecessarily erase cheap by-products.
                for (var output : edge.outputs().entrySet()) {
                    long value = values.get(output.getKey());
                    if (value <= 0L) continue;
                    if (chosen == null || value > chosenValue
                            || (value == chosenValue && output.getKey().compareTo(chosen) < 0)) {
                        chosen = output.getKey();
                        chosenCount = output.getValue();
                        chosenValue = value;
                    }
                }
                if (chosen == null) return failed(edge.id() + ": no positive output to reduce");
                BigInteger[] quotient = excess.divideAndRemainder(BigInteger.valueOf(chosenCount));
                BigInteger reduction = quotient[1].signum() == 0 ? quotient[0] : quotient[0].add(BigInteger.ONE);
                BigInteger next = BigInteger.valueOf(chosenValue).subtract(reduction);
                values.put(chosen, next.signum() <= 0 ? 0L : next.longValueExact());
                changed = true;
            }
            EconomicCertificate.Result check = EconomicCertificate.verify(edges, values, Map.of());
            if (check.passed()) return new Result(true, values, List.of());
            if (!changed) return new Result(false, values, check.witnesses());
        }
        EconomicCertificate.Result check = EconomicCertificate.verify(edges, values, Map.of());
        return check.passed() ? new Result(true, values, List.of()) : new Result(false, values, check.witnesses());
    }

    private static Result failed(String witness) {
        return new Result(false, Map.of(), witness == null || witness.isBlank() ? List.of("repair failed") : List.of(witness));
    }
}


