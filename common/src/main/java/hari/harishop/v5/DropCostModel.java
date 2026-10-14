package hari.harishop.v5;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Conservative expected acquisition cost for loot sources. */
public final class DropCostModel {
    private static final MathContext MC = new MathContext(24, RoundingMode.CEILING);

    private DropCostModel() {}

    public record MobSource(
            BigDecimal dropChanceLowerBound,
            int minimumDrop,
            BigDecimal secondsPerKill,
            BigDecimal setupSeconds,
            BigDecimal travelSeconds,
            BigDecimal deathRisk,
            BigDecimal replacementLoss,
            boolean renewable,
            boolean requiresPlayerKill) {}

    public static long expectedCost(MobSource source, long valuePerSecond) {
        if (source == null) throw new IllegalArgumentException("source");
        MoneyMath.positive(valuePerSecond, "valuePerSecond");
        validProbability(source.dropChanceLowerBound(), false, "dropChanceLowerBound");
        validProbability(source.deathRisk(), true, "deathRisk");
        if (source.minimumDrop() <= 0) throw new IllegalArgumentException("minimumDrop");
        nonNegative(source.secondsPerKill(), "secondsPerKill");
        nonNegative(source.setupSeconds(), "setupSeconds");
        nonNegative(source.travelSeconds(), "travelSeconds");
        nonNegative(source.replacementLoss(), "replacementLoss");

        BigDecimal attempts = BigDecimal.ONE.divide(source.dropChanceLowerBound(), MC);
        BigDecimal time = source.secondsPerKill().multiply(attempts, MC)
                .add(source.setupSeconds(), MC).add(source.travelSeconds(), MC);
        BigDecimal risk = source.deathRisk().multiply(source.replacementLoss(), MC).multiply(attempts, MC);
        BigDecimal restrictions = BigDecimal.ONE;
        if (!source.renewable()) restrictions = restrictions.multiply(new BigDecimal("1.35"), MC);
        if (source.requiresPlayerKill()) restrictions = restrictions.multiply(new BigDecimal("1.08"), MC);
        BigDecimal total = time.multiply(BigDecimal.valueOf(valuePerSecond), MC).add(risk, MC)
                .multiply(restrictions, MC).divide(BigDecimal.valueOf(source.minimumDrop()), MC);
        return MoneyMath.charge(1, total.max(BigDecimal.ONE));
    }

    private static void nonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(name);
    }

    private static void validProbability(BigDecimal value, boolean zeroAllowed, String name) {
        if (value == null || value.compareTo(BigDecimal.ONE) > 0 || (zeroAllowed ? value.signum() < 0 : value.signum() <= 0)) {
            throw new IllegalArgumentException(name);
        }
    }
}


