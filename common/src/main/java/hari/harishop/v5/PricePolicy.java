package hari.harishop.v5;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Deterministic quote rules separated from the safety certificate. */
public final class PricePolicy {
    public record Quote(long potential, long buy, long sell, BigDecimal factor, String status, String reason) {
        public Quote {
            MoneyMath.nonNegative(potential, "potential");
            MoneyMath.nonNegative(buy, "buy");
            MoneyMath.nonNegative(sell, "sell");
            if (factor == null || factor.signum() < 0 || status == null || reason == null) throw new IllegalArgumentException("quote");
        }
        public boolean retailEnabled() { return buy > 0L && !"BLOCKED".equals(status); }
        public boolean pawnEnabled() { return sell > 0L && !"BLOCKED".equals(status); }
    }

    private PricePolicy() {}

    /**
     * Buy is rounded up and sell is rounded down.  The pawn floor is strictly
     * below the certified potential, so a deterministic buy→sell loop loses at
     * least one Đồng even when the multiplier is exactly one.
     */
    public static Quote quote(long potential, BigDecimal marketFactor, BigDecimal buyMarkup,
                              BigDecimal sellMarkdown, boolean certified) {
        MoneyMath.nonNegative(potential, "potential");
        Objects.requireNonNull(marketFactor, "marketFactor");
        Objects.requireNonNull(buyMarkup, "buyMarkup");
        Objects.requireNonNull(sellMarkdown, "sellMarkdown");
        if (marketFactor.signum() < 0 || buyMarkup.signum() < 0 || sellMarkdown.signum() < 0) throw new IllegalArgumentException("negative price factor");
        if (!certified || potential <= 0L) return new Quote(potential, 0L, 0L, marketFactor, "BLOCKED", "chưa có economic certificate");
        long adjusted = MoneyMath.charge(potential, marketFactor);
        long buy = MoneyMath.charge(adjusted, buyMarkup);
        long theoreticalSell = MoneyMath.payout(adjusted, sellMarkdown);
        long sell = theoreticalSell <= 1L ? 0L : Math.min(theoreticalSell, potential - 1L);
        if (sell <= 0L) return new Quote(potential, buy, 0L, marketFactor, "RETAIL_ONLY", "payout lý thuyết dưới 1 Đồng hoặc không thấp hơn potential");
        return new Quote(potential, buy, sell, marketFactor, "CERTIFIED", "exact ceiling/floor");
    }

    public static long feeCeiling(long amount, BigDecimal rate) {
        MoneyMath.positive(amount, "amount");
        if (rate == null || rate.signum() < 0) throw new IllegalArgumentException("rate");
        return MoneyMath.charge(amount, rate);
    }

    public static BigDecimal clampFactor(BigDecimal factor, BigDecimal min, BigDecimal max) {
        Objects.requireNonNull(factor); Objects.requireNonNull(min); Objects.requireNonNull(max);
        if (min.signum() < 0 || max.compareTo(min) < 0 || factor.signum() < 0) throw new IllegalArgumentException("factor bounds");
        return factor.max(min).min(max).setScale(Math.max(0, factor.scale()), RoundingMode.HALF_EVEN);
    }
}


