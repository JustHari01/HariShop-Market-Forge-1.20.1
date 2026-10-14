package hari.harishop.v5;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Exact, integer-only money policy shared by every Forge transaction path.
 * Values are Đồng (Bronze); there is no floating point conversion at a
 * transaction boundary and every result is bounded before it is committed.
 */
public final class MoneyMath {
    public static final long MAX_BALANCE = 9_000_000_000_000_000L;
    public static final long BRONZE = 1L;
    public static final long SILVER = 100L;
    public static final long GOLD = 10_000L;

    private MoneyMath() {}

    public static long nonNegative(long value, String field) {
        if (value < 0L || value > MAX_BALANCE) {
            throw new IllegalArgumentException(field + " must be in 0.." + MAX_BALANCE);
        }
        return value;
    }

    public static long positive(long value, String field) {
        if (value <= 0L || value > MAX_BALANCE) {
            throw new IllegalArgumentException(field + " must be in 1.." + MAX_BALANCE);
        }
        return value;
    }

    public static long addExact(long left, long right, String field) {
        nonNegative(left, field + ".left");
        nonNegative(right, field + ".right");
        try {
            return nonNegative(Math.addExact(left, right), field);
        } catch (ArithmeticException | IllegalArgumentException overflow) {
            throw new ArithmeticException(field + " overflow");
        }
    }

    public static long subtractExact(long left, long right, String field) {
        nonNegative(left, field + ".left");
        nonNegative(right, field + ".right");
        try {
            return nonNegative(Math.subtractExact(left, right), field);
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            throw new ArithmeticException(field + " would become negative");
        }
    }

    public static long multiplyExact(long left, long right, String field) {
        positive(left, field + ".left");
        positive(right, field + ".right");
        try {
            return positive(Math.multiplyExact(left, right), field);
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            throw new ArithmeticException(field + " overflow");
        }
    }

    /** Treasury-favouring percentage charge, rounded up to one Đồng. */
    public static long taxCeiling(long amount, long numerator, long denominator) {
        nonNegative(amount, "amount");
        nonNegative(numerator, "tax numerator");
        positive(denominator, "tax denominator");
        BigInteger n = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(numerator));
        BigInteger d = BigInteger.valueOf(denominator);
        return checked(n.add(d.subtract(BigInteger.ONE)).divide(d));
    }

    /** Payout-favouring percentage, rounded down to one Đồng. */
    public static long taxFloor(long amount, long numerator, long denominator) {
        nonNegative(amount, "amount");
        nonNegative(numerator, "tax numerator");
        positive(denominator, "tax denominator");
        return checked(BigInteger.valueOf(amount).multiply(BigInteger.valueOf(numerator))
                .divide(BigInteger.valueOf(denominator)));
    }

    public static long ceilRatio(long value, long numerator, long denominator) {
        positive(value, "value");
        positive(numerator, "numerator");
        positive(denominator, "denominator");
        BigInteger[] qr = BigInteger.valueOf(value).multiply(BigInteger.valueOf(numerator))
                .divideAndRemainder(BigInteger.valueOf(denominator));
        return checked(qr[0].add(qr[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE));
    }

    public static long floorRatio(long value, long numerator, long denominator) {
        nonNegative(value, "value");
        nonNegative(numerator, "numerator");
        positive(denominator, "denominator");
        return checked(BigInteger.valueOf(value).multiply(BigInteger.valueOf(numerator))
                .divide(BigInteger.valueOf(denominator)));
    }

    public static long rounded(long base, BigDecimal multiplier, RoundingMode mode) {
        nonNegative(base, "base");
        if (multiplier == null || multiplier.signum() < 0 || mode == null) {
            throw new IllegalArgumentException("invalid multiplier");
        }
        return checked(BigDecimal.valueOf(base).multiply(multiplier).setScale(0, mode).toBigIntegerExact());
    }

    /** Treasury-favouring ceiling charge used by quote and drop models. */
    public static long charge(long base, BigDecimal multiplier) {
        return rounded(base, multiplier, RoundingMode.CEILING);
    }

    /** Player-favouring floor payout used by pawn and reward paths. */
    public static long payout(long base, BigDecimal multiplier) {
        return rounded(base, multiplier, RoundingMode.FLOOR);
    }

    public static long checked(BigInteger value) {
        if (value == null || value.signum() < 0
                || value.compareTo(BigInteger.valueOf(MAX_BALANCE)) > 0) {
            throw new ArithmeticException("money result out of range");
        }
        return value.longValueExact();
    }
}
