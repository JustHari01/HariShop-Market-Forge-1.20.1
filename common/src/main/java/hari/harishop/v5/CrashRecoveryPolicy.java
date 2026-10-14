package hari.harishop.v5;

/** Fail-closed recovery decisions for the two-resource item/money boundary. */
public final class CrashRecoveryPolicy {
    public enum MoneyStatus { MISSING, REJECTED, APPLIED }
    public enum Action { CANCEL, REFUND_BUY, COMPLETE_BUY, PAY_SELL, COMPLETE_SELL, QUARANTINE }

    private CrashRecoveryPolicy() {}

    public static Action decide(boolean sell, boolean itemReceipt, boolean boundaryStarted,
                                MoneyStatus primary, MoneyStatus compensation) {
        if (primary == null || compensation == null) throw new IllegalArgumentException("money status");
        if (boundaryStarted && !itemReceipt) return Action.QUARANTINE;
        if (sell) {
            if (!itemReceipt) return primary == MoneyStatus.APPLIED ? Action.QUARANTINE : Action.CANCEL;
            return primary == MoneyStatus.APPLIED ? Action.COMPLETE_SELL : Action.PAY_SELL;
        }
        if (itemReceipt) return primary == MoneyStatus.APPLIED ? Action.COMPLETE_BUY : Action.QUARANTINE;
        return primary == MoneyStatus.APPLIED ? Action.REFUND_BUY : Action.CANCEL;
    }
}
