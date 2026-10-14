package hari.harishop.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Optional bridge to the secure Numismatic provenance guard.
 *
 * <p>HariShop is built for several loaders and must not link the Numismatic
 * classes at compile time.  The bridge therefore checks the namespaced NBT
 * marker first (the Forge 1.20.1 representation) and then calls the optional
 * guard when it is available.  A marker is always non-economic; an absent
 * optional class does not make ordinary survival items unsellable.</p>
 */
public final class EconomicItemGuardBridge {
    private static final String PROVENANCE_ROOT = "harishop_provenance";
    private static final String ADMIN_ROOT = "harishop_admin";
    private static volatile boolean lookedUp;
    private static volatile Method isNonEconomic;

    private EconomicItemGuardBridge() {}

    /** Returns true when the stack is explicitly quarantined by provenance. */
    public static boolean isNonEconomic(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        if (tag != null && (tag.contains(PROVENANCE_ROOT, CompoundTag.TAG_COMPOUND)
                || tag.contains(ADMIN_ROOT, CompoundTag.TAG_COMPOUND))) {
            return true;
        }
        Method method = guardMethod();
        if (method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(null, stack));
        } catch (Throwable ignored) {
            // An optional integration failure must not crash a server.  A
            // present explicit marker was already handled above; unmarked
            // stacks remain on the legacy calculator path.
            return false;
        }
    }

    private static Method guardMethod() {
        if (lookedUp) return isNonEconomic;
        synchronized (EconomicItemGuardBridge.class) {
            if (lookedUp) return isNonEconomic;
            lookedUp = true;
            try {
                Class<?> guard = Class.forName(
                        "tallestred.numismaticoverhaul.item.EconomicItemGuard",
                        false, EconomicItemGuardBridge.class.getClassLoader());
                isNonEconomic = guard.getMethod("isNonEconomic", ItemStack.class);
            } catch (Throwable ignored) {
                isNonEconomic = null;
            }
            return isNonEconomic;
        }
    }
}
