/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.player.Player
 */
package hari.harishop.integration;

import hari.harishop.HariShopMod;
import hari.harishop.v5.MoneyMath;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NumismaticHelper {
    private static boolean initialized = false;
    private static boolean numismaticAvailable = false;
    private static Method getMoneyMethod;
    private static Method deduceMethod;
    private static Method offerMethod;
    private static Method validStacksMethod;
    private static Method holderLookupMethod;
    private static Method holderValueMethod;
    private static Method holderTryModifyMethod;
    private static Method holderModifyMethod;
    private static boolean walletAvailable;

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> currencyHelperClass = Class.forName("tallestred.numismaticoverhaul.currency.CurrencyHelper");
            getMoneyMethod = currencyHelperClass.getMethod("getMoneyInInventory", Player.class, Boolean.TYPE);
            deduceMethod = currencyHelperClass.getMethod("deduceFromInventory", Player.class, Long.TYPE);
            offerMethod = currencyHelperClass.getMethod("offerAsCoins", Player.class, Long.TYPE);
            try {
                Class<?> converterClass = Class.forName("tallestred.numismaticoverhaul.currency.CurrencyConverter");
                validStacksMethod = converterClass.getMethod("getAsValidStacks", Long.TYPE);
            } catch (ReflectiveOperationException ignored) {
                validStacksMethod = null;
            }
            try {
                Class<?> attacherClass = Class.forName("tallestred.numismaticoverhaul.cap.CurrencyHolderAttacher");
                holderLookupMethod = attacherClass.getMethod("getExampleHolderUnwrap", net.minecraft.world.entity.Entity.class);
                Class<?> holderClass = Class.forName("tallestred.numismaticoverhaul.cap.CurrencyHolder");
                holderValueMethod = holderClass.getMethod("getValue");
                try { holderTryModifyMethod = holderClass.getMethod("tryModify", Long.TYPE, String.class); }
                catch (ReflectiveOperationException ignored) { holderTryModifyMethod = null; }
                holderModifyMethod = holderClass.getMethod("modify", Long.TYPE);
                walletAvailable = true;
            } catch (ReflectiveOperationException ignored) {
                walletAvailable = false;
            }
            numismaticAvailable = true;
            HariShopMod.LOGGER.info("Numismatic Overhaul integration enabled");
        }
        catch (Exception e) {
            numismaticAvailable = false;
            HariShopMod.LOGGER.warn("Numismatic Overhaul not found: " + e.getMessage());
        }
    }

    public static long getPlayerMoney(Player player) {
        NumismaticHelper.init();
        if (!numismaticAvailable || getMoneyMethod == null) {
            return 0L;
        }
        if (walletAvailable) {
            try {
                Object holder = walletHolder(player);
                if (holder != null) {
                    long value = ((Number)holderValueMethod.invoke(holder)).longValue();
                    return value < 0L ? 0L : Math.min(MoneyMath.MAX_BALANCE, value);
                }
            } catch (Exception e) {
                HariShopMod.LOGGER.warn("Numismatic wallet read failed; using physical fallback: {}", e.toString());
            }
        }
        try {
            Long value = (Long)getMoneyMethod.invoke(null, player, false);
            if (value == null || value.longValue() < 0L) {
                return 0L;
            }
            return Math.min(MoneyMath.MAX_BALANCE, value.longValue());
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("Failed to get money from Numismatic", (Throwable)e);
            return 0L;
        }
    }

    public static boolean deductMoney(Player player, long amount) {
        return deductMoney(player, amount, "harishop:debit:" + (player == null ? "null" : player.getUUID()) + ":" + amount + ":" + System.nanoTime());
    }

    /**
     * Idempotent-aware debit path.  The transaction id is forwarded to the
     * secure Numismatic wallet when that API is present; legacy APIs still
     * receive the same validation but cannot provide replay persistence.
     */
    public static boolean deductMoney(Player player, long amount, String transactionId) {
        NumismaticHelper.init();
        if (!numismaticAvailable || deduceMethod == null || player == null
                || amount <= 0L || amount > MoneyMath.MAX_BALANCE) {
            return false;
        }
        // A present wallet is authoritative.  Falling back to the legacy
        // physical-coin API after a wallet rejection would allow a replay or
        // an insufficient-balance request to debit a second backend.
        if (walletAvailable) return walletModify(player, -amount, transactionId);
        try {
            return (Boolean)deduceMethod.invoke(null, player, amount);
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("Failed to deduct money from Numismatic", (Throwable)e);
            return false;
        }
    }

    public static boolean canAfford(Player player, long price) {
        return price > 0L && NumismaticHelper.getPlayerMoney(player) >= price;
    }

    /** Credits Numismatic currency when it is the active Forge 1.20.1 backend. */
    public static boolean addMoney(Player player, long amount) {
        return addMoney(player, amount, "harishop:credit:" + (player == null ? "null" : player.getUUID()) + ":" + amount + ":" + System.nanoTime());
    }

    /** Idempotent-aware credit path used by shop sell transactions. */
    public static boolean addMoney(Player player, long amount, String transactionId) {
        NumismaticHelper.init();
        if (!numismaticAvailable || offerMethod == null || player == null
                || amount <= 0L || amount > MoneyMath.MAX_BALANCE) {
            return false;
        }
        if (walletAvailable) return walletModify(player, amount, transactionId);
        if (!canReceive(player, amount)) {
            return false;
        }
        List<ItemStack> beforeInventory = snapshot(player.getInventory());
        try {
            long before = getPlayerMoney(player);
            offerMethod.invoke(null, player, amount);
            long after = getPlayerMoney(player);
            // offerAsCoins is a void API and historically could silently do
            // nothing when a slot changed between the capacity check and the
            // insert.  Confirm the exact delta before reporting success.
            if (after >= before && after - before == amount) {
                return true;
            }
            restoreSnapshot(player.getInventory(), beforeInventory);
            return false;
        } catch (Exception e) {
            restoreSnapshot(player.getInventory(), beforeInventory);
            HariShopMod.LOGGER.error("Failed to credit Numismatic", e);
            return false;
        }
    }

    /**
     * Numismatic's offerAsCoins API is void and silently ignores a full
     * inventory.  Resolve the exact payout first and reserve capacity so a
     * pawn sale can be rolled back instead of destroying the player's item.
     */
    public static boolean canReceive(Player player, long amount) {
        NumismaticHelper.init();
        if (!numismaticAvailable || player == null || amount <= 0L || validStacksMethod == null) {
            return false;
        }
        if (walletAvailable) {
            long current = getPlayerMoney(player);
            return current <= MoneyMath.MAX_BALANCE - amount;
        }
        try {
            Object raw = validStacksMethod.invoke(null, amount);
            if (!(raw instanceof List)) {
                return false;
            }
            List<ItemStack> grants = new ArrayList<ItemStack>();
            for (Object value : (List<?>)raw) {
                if (!(value instanceof ItemStack)) {
                    return false;
                }
                ItemStack stack = ((ItemStack)value).copy();
                if (stack.isEmpty() || stack.getCount() <= 0) {
                    continue;
                }
                grants.add(stack);
            }
            return canFit(player.getInventory(), grants);
        } catch (Exception e) {
            HariShopMod.LOGGER.error("Failed to validate Numismatic payout", e);
            return false;
        }
    }

    private static boolean canFit(Inventory inventory, List<ItemStack> grants) {
        List<ItemStack> virtual = new ArrayList<ItemStack>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            virtual.add(inventory.getItem(i).copy());
        }
        for (ItemStack grant : grants) {
            int remaining = grant.getCount();
            for (int i = 0; i < virtual.size() && remaining > 0; i++) {
                ItemStack slot = virtual.get(i);
                if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, grant)) {
                    continue;
                }
                int room = Math.max(0, slot.getMaxStackSize() - slot.getCount());
                int moved = Math.min(room, remaining);
                slot.grow(moved);
                remaining -= moved;
            }
            for (int i = 0; i < virtual.size() && remaining > 0; i++) {
                ItemStack slot = virtual.get(i);
                if (!slot.isEmpty()) {
                    continue;
                }
                int moved = Math.min(grant.getMaxStackSize(), remaining);
                ItemStack placed = grant.copy();
                placed.setCount(moved);
                virtual.set(i, placed);
                remaining -= moved;
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> snapshot(Inventory inventory) {
        List<ItemStack> before = new ArrayList<ItemStack>(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            before.add(inventory.getItem(i).copy());
        }
        return before;
    }

    private static void restoreSnapshot(Inventory inventory, List<ItemStack> snapshot) {
        int limit = Math.min(inventory.getContainerSize(), snapshot.size());
        for (int i = 0; i < limit; i++) {
            inventory.setItem(i, snapshot.get(i).copy());
        }
    }

    private static Object walletHolder(Player player) throws ReflectiveOperationException {
        if (player == null || holderLookupMethod == null) return null;
        return holderLookupMethod.invoke(null, player);
    }

    private static boolean walletModify(Player player, long delta) {
        return walletModify(player, delta, "harishop:wallet:" + (player == null ? "null" : player.getUUID()) + ":" + delta + ":" + System.nanoTime());
    }

    private static boolean walletModify(Player player, long delta, String transactionId) {
        if (!walletAvailable || delta == 0L || delta == Long.MIN_VALUE) return delta == 0L;
        try {
            Object holder = walletHolder(player);
            if (holder == null) return false;
            String tx = transactionId == null || transactionId.isBlank()
                    ? "harishop:wallet:" + player.getUUID() + ":" + delta + ":" + System.nanoTime()
                    : transactionId;
            if (holderTryModifyMethod != null) {
                Object result = holderTryModifyMethod.invoke(holder, delta, tx);
                return Boolean.TRUE.equals(result);
            }
            long before = ((Number)holderValueMethod.invoke(holder)).longValue();
            if (delta < 0L && (before < 0L || before < Math.abs(delta))) return false;
            if (delta > 0L && (before < 0L || before > MoneyMath.MAX_BALANCE - delta)) return false;
            holderModifyMethod.invoke(holder, delta);
            long after = ((Number)holderValueMethod.invoke(holder)).longValue();
            long expected = delta < 0L ? before - Math.abs(delta) : before + delta;
            return after == expected;
        } catch (Exception e) {
            HariShopMod.LOGGER.error("Numismatic wallet mutation failed", e);
            return false;
        }
    }

    public static String formatPrice(long bronzeAmount) {
        long gold = bronzeAmount / 10000L;
        long silver = bronzeAmount % 10000L / 100L;
        long bronze = bronzeAmount % 100L;
        StringBuilder sb = new StringBuilder();
        if (gold > 0L) {
            sb.append(gold).append("g ");
        }
        if (silver > 0L || gold > 0L) {
            sb.append(silver).append("s ");
        }
        sb.append(bronze).append("b");
        return sb.toString().trim();
    }
}

