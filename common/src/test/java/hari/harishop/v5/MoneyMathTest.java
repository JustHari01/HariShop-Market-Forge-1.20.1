package hari.harishop.v5;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class MoneyMathTest {
    @Test
    void boundariesNeverWrapOrBecomeNegative() {
        assertEquals(MoneyMath.MAX_BALANCE, MoneyMath.nonNegative(MoneyMath.MAX_BALANCE, "x"));
        assertThrows(IllegalArgumentException.class, () -> MoneyMath.nonNegative(-1, "x"));
        assertThrows(IllegalArgumentException.class, () -> MoneyMath.nonNegative(Long.MIN_VALUE, "x"));
        assertThrows(ArithmeticException.class,
                () -> MoneyMath.addExact(MoneyMath.MAX_BALANCE, 1, "balance"));
        assertThrows(ArithmeticException.class,
                () -> MoneyMath.subtractExact(0, 1, "balance"));
        assertThrows(ArithmeticException.class,
                () -> MoneyMath.multiplyExact(MoneyMath.MAX_BALANCE, 2, "total"));
    }

    @Test
    void roundingIsDirectionalAndIntegerOnly() {
        assertEquals(2, MoneyMath.rounded(3, new BigDecimal("0.51"), RoundingMode.CEILING));
        assertEquals(1, MoneyMath.rounded(3, new BigDecimal("0.51"), RoundingMode.FLOOR));
        assertEquals(1, MoneyMath.taxCeiling(1, 5, 100));
        assertEquals(0, MoneyMath.taxFloor(1, 5, 100));
        assertEquals(2, MoneyMath.ceilRatio(3, 1, 2));
        assertEquals(1, MoneyMath.floorRatio(3, 1, 2));
    }

    @Test
    void maxBalanceIsAppliedToComputedValues() {
        assertThrows(ArithmeticException.class,
                () -> MoneyMath.rounded(MoneyMath.MAX_BALANCE, BigDecimal.valueOf(2), RoundingMode.CEILING));
        assertThrows(IllegalArgumentException.class,
                () -> MoneyMath.positive(0, "price"));
    }
}
