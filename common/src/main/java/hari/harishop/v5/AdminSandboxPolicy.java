package hari.harishop.v5;

/** Pure validation helpers for TEST lot namespaces; safe to property-test without Minecraft classes. */
public final class AdminSandboxPolicy {
    private AdminSandboxPolicy() {}

    public static String lotId(String session) {
        if (!validSession(session)) throw new IllegalArgumentException("sandbox session");
        return "test:" + session;
    }

    public static boolean isLot(String lotId) {
        return lotId != null && lotId.startsWith("test:") && validSession(lotId.substring(5));
    }

    public static boolean isLotOf(String lotId, String session) {
        return validSession(session) && lotId != null && lotId.equals(lotId(session));
    }

    public static boolean validSession(String value) {
        return value != null && value.length() >= 8 && value.length() <= 96
                && value.matches("[A-Za-z0-9_-]+") && value.startsWith("test-");
    }
}


