package hari.harishop.v5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigInteger;

/** Hypergraph valuation for crafting, transformation, loot and trade routes. */
public final class AcquisitionGraph {
    public record Route(String id, Map<String, Long> inputs, String output, long outputCount, long effortCost) {
        public Route {
            if (id == null || id.isBlank() || output == null || output.isBlank()) throw new IllegalArgumentException("route identity");
            if (inputs == null || inputs.isEmpty() || outputCount <= 0 || effortCost < 0) throw new IllegalArgumentException("route amounts");
            Map<String, Long> copy = new HashMap<>();
            inputs.forEach((item, count) -> {
                if (item == null || item.isBlank() || count == null || count <= 0) throw new IllegalArgumentException("input");
                copy.put(item, count);
            });
            inputs = Map.copyOf(copy);
        }
    }

    public record Result(Map<String, Long> minimumCosts, Set<String> cyclicItems, Set<String> lockedRoutes) {}

    /** A process edge with every simultaneous output and consumed resource. */
    public record HyperRoute(String id, Map<String, Long> inputs, Map<String, Long> outputs, long consumedBudget) {
        public HyperRoute {
            if (id == null || id.isBlank() || inputs == null || inputs.isEmpty() || outputs == null || outputs.isEmpty()) throw new IllegalArgumentException("route identity");
            inputs = clean(inputs, "inputs");
            outputs = clean(outputs, "outputs");
            MoneyMath.nonNegative(consumedBudget, "consumedBudget");
        }
        private static Map<String, Long> clean(Map<String, Long> values, String field) {
            Map<String, Long> copy = new HashMap<>();
            values.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null || value <= 0L) throw new IllegalArgumentException(field);
                copy.merge(key, value, (left, right) -> Math.addExact(left, right));
            });
            return Map.copyOf(copy);
        }
    }

    public record HyperResult(Map<String, Long> lowerBounds, Set<String> cyclicItems, Set<String> lockedRoutes) {}

    private final List<Route> routes = new ArrayList<>();
    private final Map<String, Long> sourceCosts = new HashMap<>();
    private final List<HyperRoute> hyperRoutes = new ArrayList<>();

    public void source(String item, long cost) { sourceCosts.merge(item, MoneyMath.positive(cost, "cost"), Math::min); }
    public void route(Route route) { routes.add(route); }
    public void hyperRoute(HyperRoute route) { hyperRoutes.add(route); }

    /** Exact multi-output analysis used by the sealed release gate. */
    public HyperResult solveHyper(Map<String, Long> guaranteedPayouts) {
        Map<String, Long> lower = new HashMap<>(sourceCosts);
        Set<String> cyclic = new HashSet<>();
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (HyperRoute edge : hyperRoutes) {
            for (String input : edge.inputs().keySet()) for (String output : edge.outputs().keySet()) {
                adjacency.computeIfAbsent(input, ignored -> new HashSet<>()).add(output);
            }
        }
        for (String node : adjacency.keySet()) if (reaches(node, node, adjacency, new HashSet<>())) cyclic.add(node);
        int limit = Math.max(8, (hyperRoutes.size() + lower.size()) * 4);
        for (int pass = 0; pass < limit; pass++) {
            boolean changed = false;
            for (HyperRoute edge : hyperRoutes) {
                BigInteger input = BigInteger.valueOf(edge.consumedBudget());
                boolean known = true;
                for (var part : edge.inputs().entrySet()) {
                    Long value = lower.get(part.getKey());
                    if (value == null) { known = false; break; }
                    input = input.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(part.getValue())));
                }
                if (!known) continue;
                for (var part : edge.outputs().entrySet()) {
                    BigInteger[] qr = input.divideAndRemainder(BigInteger.valueOf(part.getValue()));
                    BigInteger candidate = qr[0].add(qr[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE);
                    if (candidate.compareTo(BigInteger.valueOf(MoneyMath.MAX_BALANCE)) > 0) continue;
                    long value = candidate.longValueExact();
                    if (!lower.containsKey(part.getKey()) || value < lower.get(part.getKey())) { lower.put(part.getKey(), value); changed = true; }
                }
            }
            if (!changed) break;
        }
        Set<String> locked = new HashSet<>();
        for (HyperRoute edge : hyperRoutes) {
            BigInteger lhs = BigInteger.ZERO;
            boolean known = true;
            for (var output : edge.outputs().entrySet()) {
                Long value = guaranteedPayouts == null ? lower.get(output.getKey()) : guaranteedPayouts.get(output.getKey());
                if (value == null) { known = false; break; }
                lhs = lhs.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(output.getValue())));
            }
            BigInteger rhs = BigInteger.valueOf(edge.consumedBudget());
            if (known) for (var input : edge.inputs().entrySet()) {
                Long value = lower.get(input.getKey());
                if (value == null) { known = false; break; }
                rhs = rhs.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(input.getValue())));
            }
            if (!known || lhs.compareTo(rhs) > 0) locked.add(edge.id());
        }
        return new HyperResult(Map.copyOf(lower), Set.copyOf(cyclic), Set.copyOf(locked));
    }

    public Result solve(Map<String, Long> guaranteedShopPayouts) {
        Map<String, Long> costs = new HashMap<>(sourceCosts);
        Set<String> cyclic = structuralCycles();
        Set<String> locked = new HashSet<>();
        int limit = Math.max(16, (routes.size() + costs.size()) * 4);
        for (int pass = 0; pass < limit; pass++) {
            boolean changed = false;
            for (Route route : routes) {
                Long candidate = routeCost(route, costs);
                if (candidate == null) continue;
                long old = costs.getOrDefault(route.output(), Long.MAX_VALUE);
                if (candidate < old) { costs.put(route.output(), candidate); changed = true; }
                long payout = guaranteedShopPayouts == null ? 0 : guaranteedShopPayouts.getOrDefault(route.output(), 0L);
                if (payout > 0 && payout > candidate) locked.add(route.id());
            }
            if (!changed) break;
        }
        // A cyclic item never receives automatic payout from a route whose deterministic output beats its inputs.
        for (Route route : routes) if (cyclic.contains(route.output())) {
            Long candidate = routeCost(route, costs);
            long payout = guaranteedShopPayouts == null ? 0 : guaranteedShopPayouts.getOrDefault(route.output(), 0L);
            if (candidate != null && payout > candidate) locked.add(route.id());
        }
        return new Result(Map.copyOf(costs), Set.copyOf(cyclic), Set.copyOf(locked));
    }

    private static Long routeCost(Route route, Map<String, Long> costs) {
        long total = route.effortCost();
        try {
            for (var input : route.inputs().entrySet()) {
                Long unit = costs.get(input.getKey());
                if (unit == null) return null;
                total = Math.addExact(total, Math.multiplyExact(unit, input.getValue()));
            }
            return MoneyMath.ceilRatio(total, 1, route.outputCount());
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return null;
        }
    }

    private Set<String> structuralCycles() {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (Route route : routes) for (String input : route.inputs().keySet()) {
            adjacency.computeIfAbsent(input, ignored -> new HashSet<>()).add(route.output());
        }
        Set<String> cyclic = new HashSet<>();
        for (String start : adjacency.keySet()) if (reaches(start, start, adjacency, new HashSet<>())) cyclic.add(start);
        return cyclic;
    }

    private static boolean reaches(String node, String target, Map<String, Set<String>> adjacency, Set<String> visited) {
        if (!visited.add(node)) return false;
        for (String next : adjacency.getOrDefault(node, Set.of())) {
            if (next.equals(target) || reaches(next, target, adjacency, visited)) return true;
        }
        return false;
    }
}


