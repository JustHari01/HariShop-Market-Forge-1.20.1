package hari.harishop.v5;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/** Pure, immutable recipe math safe to run away from the Minecraft server thread. */
public final class RecipeEconomics {
    private RecipeEconomics() {}

    public record IngredientOption(String itemId, String remainderId) {
        public IngredientOption {
            if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("ingredient item");
            if (remainderId != null && remainderId.isBlank()) remainderId = null;
        }
    }

    /**
     * Caps all pawn prices in a worst-case multi-output basket so even a roll
     * in which every probabilistic byproduct succeeds pays less than its input.
     */
    public static Map<String, Long> capPayoutBasket(Map<String, Long> proposed,
                                                    Map<String, Integer> worstCaseOutputs,
                                                    long inputCost) {
        if (worstCaseOutputs == null || worstCaseOutputs.isEmpty()) return Map.of();
        HashMap<String, Long> clean = new HashMap<>();
        BigInteger total = BigInteger.ZERO;
        boolean valid = proposed != null && inputCost >= 0L;
        for (Map.Entry<String, Integer> entry : worstCaseOutputs.entrySet()) {
            String id = entry.getKey();
            Integer count = entry.getValue();
            if (id == null || id.isBlank() || count == null || count <= 0) valid = false;
            long unit = valid ? Math.max(0L, proposed.getOrDefault(id, 0L)) : 0L;
            if (id != null && !id.isBlank()) clean.put(id, unit);
            if (count != null && count > 0) {
                total = total.add(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(count)));
            }
        }
        if (!valid) {
            clean.replaceAll((ignored, value) -> 0L);
            return Map.copyOf(clean);
        }
        BigInteger budget = BigInteger.valueOf(Math.max(0L, inputCost - 1L));
        if (total.signum() <= 0 || total.compareTo(budget) <= 0) return Map.copyOf(clean);
        BigInteger finalTotal = total;
        clean.replaceAll((ignored, unit) -> BigInteger.valueOf(unit).multiply(budget).divide(finalTotal).longValue());
        return Map.copyOf(clean);
    }

    public static final class Route {
        private final List<List<IngredientOption>> slots;
        private final int outputCount;
        private final long processPpm;

        public Route(List<List<IngredientOption>> slots, int outputCount, long processPpm) {
            if (slots == null || slots.isEmpty() || outputCount <= 0 || processPpm <= 0L) {
                throw new IllegalArgumentException("recipe route");
            }
            ArrayList<List<IngredientOption>> copy = new ArrayList<>();
            for (List<IngredientOption> slot : slots) {
                if (slot == null || slot.isEmpty()) throw new IllegalArgumentException("empty ingredient slot");
                copy.add(List.copyOf(slot));
            }
            this.slots = List.copyOf(copy);
            this.outputCount = outputCount;
            this.processPpm = processPpm;
        }

        public List<List<IngredientOption>> slots() {
            return slots;
        }

        /** Deterministic route metadata used by the release certificate. */
        public int outputCount() {
            return outputCount;
        }

        /** Process cost in parts-per-million; one million is neutral. */
        public long processPpm() {
            return processPpm;
        }

        /** Acquisition valuation may include a configured process/effort premium. */
        public OptionalLong acquisitionCost(Map<String, Long> costs) {
            OptionalLong material = materialCost(costs);
            if (material.isEmpty()) return OptionalLong.empty();
            if (material.getAsLong() == 0L) return OptionalLong.of(0L);
            try {
                return OptionalLong.of(MoneyMath.ceilRatio(material.getAsLong(), processPpm,
                        Math.multiplyExact(1_000_000L, outputCount)));
            }
            catch (ArithmeticException | IllegalArgumentException invalid) {
                return OptionalLong.empty();
            }
        }

        /**
         * Currency-loop valuation excludes process time: a player's time is not
         * money debited by the shop. Returned containers/catalysts are credited
         * conservatively at their full reachable acquisition cost.
         */
        public OptionalLong arbitrageCost(Map<String, Long> costs) {
            OptionalLong material = materialCost(costs);
            if (material.isEmpty() || material.getAsLong() == 0L) return material;
            try {
                return OptionalLong.of(MoneyMath.ceilRatio(material.getAsLong(), 1L, outputCount));
            }
            catch (ArithmeticException | IllegalArgumentException invalid) {
                return OptionalLong.empty();
            }
        }

        private long netCost(IngredientOption option, Map<String, Long> costs) {
            Long input = costs.get(option.itemId());
            if (input == null || input < 0L) return -1L;
            if (option.remainderId() == null) return input;
            Long remainder = costs.get(option.remainderId());
            if (remainder == null || remainder < 0L) return -1L;
            try {
                return Math.max(0L, Math.subtractExact(input, remainder));
            }
            catch (ArithmeticException invalid) {
                return -1L;
            }
        }

        private OptionalLong materialCost(Map<String, Long> costs) {
            if (costs == null) return OptionalLong.empty();
            long total = 0L;
            try {
                for (List<IngredientOption> slot : slots) {
                    long cheapest = Long.MAX_VALUE;
                    for (IngredientOption option : slot) {
                        long net = netCost(option, costs);
                        if (net >= 0L && net < cheapest) cheapest = net;
                    }
                    if (cheapest == Long.MAX_VALUE) return OptionalLong.empty();
                    total = Math.addExact(total, cheapest);
                    if (total > MoneyMath.MAX_BALANCE) return OptionalLong.empty();
                }
                return OptionalLong.of(total);
            }
            catch (ArithmeticException invalid) {
                return OptionalLong.empty();
            }
        }
    }
}


