package hari.harishop.v5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrashRecoveryPolicyTest {
    @Test
    void ambiguousExternalBoundaryIsQuarantined() {
        assertEquals(CrashRecoveryPolicy.Action.QUARANTINE,
                CrashRecoveryPolicy.decide(false, false, true,
                        CrashRecoveryPolicy.MoneyStatus.APPLIED,
                        CrashRecoveryPolicy.MoneyStatus.MISSING));
    }

    @Test
    void clearBuyReceiptCompletesOnlyAfterDebit() {
        assertEquals(CrashRecoveryPolicy.Action.COMPLETE_BUY,
                CrashRecoveryPolicy.decide(false, true, false,
                        CrashRecoveryPolicy.MoneyStatus.APPLIED,
                        CrashRecoveryPolicy.MoneyStatus.MISSING));
        assertEquals(CrashRecoveryPolicy.Action.REFUND_BUY,
                CrashRecoveryPolicy.decide(false, false, false,
                        CrashRecoveryPolicy.MoneyStatus.APPLIED,
                        CrashRecoveryPolicy.MoneyStatus.MISSING));
    }
}
