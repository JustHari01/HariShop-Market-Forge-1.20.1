package hari.harishop.v5;

import java.util.Locale;

/**
 * Pure Java command classification used by the provenance boundary.  Keeping
 * this policy independent from Minecraft classes makes it testable in the
 * source-only suite and prevents a class-loading failure from weakening the
 * fail-closed command hook.
 */
public final class AdminCommandPolicy {
    private AdminCommandPolicy() {}

    public static boolean isInjectionCommand(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String command = raw.stripLeading().toLowerCase(Locale.ROOT);
        if (command.startsWith("/")) command = command.substring(1);
        return command.matches(".*(^|\\s)(give|item|loot|replaceitem|summon|function|data|place|structure|enchant)(\\s|$).*");
    }
}


