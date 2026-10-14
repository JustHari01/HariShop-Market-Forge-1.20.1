/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.integration;

import hari.harishop.HariShopMod;
import hari.harishop.v5.MoneyMath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;

public class LightmansHelper {
    private static boolean initialized = false;
    private static boolean lightmansAvailable = false;
    private static Item coinCopper;
    private static Item coinIron;
    private static Item coinGold;
    private static Item coinEmerald;
    private static Item coinDiamond;
    private static Item coinNetherite;
    private static Item pileCopper;
    private static Item pileIron;
    private static Item pileGold;
    private static Item pileEmerald;
    private static Item pileDiamond;
    private static Item pileNetherite;
    private static Item blockCopper;
    private static Item blockIron;
    private static Item blockGold;
    private static Item blockEmerald;
    private static Item blockDiamond;
    private static Item blockNetherite;
    private static final long COPPER_VALUE = 1L;
    private static final long IRON_VALUE = 10L;
    private static final long GOLD_VALUE = 100L;
    private static final long EMERALD_VALUE = 1000L;
    private static final long DIAMOND_VALUE = 10000L;
    private static final long NETHERITE_VALUE = 100000L;

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            coinCopper = LightmansHelper.getItem("lightmanscurrency", "coin_copper");
            coinIron = LightmansHelper.getItem("lightmanscurrency", "coin_iron");
            coinGold = LightmansHelper.getItem("lightmanscurrency", "coin_gold");
            coinEmerald = LightmansHelper.getItem("lightmanscurrency", "coin_emerald");
            coinDiamond = LightmansHelper.getItem("lightmanscurrency", "coin_diamond");
            coinNetherite = LightmansHelper.getItem("lightmanscurrency", "coin_netherite");
            pileCopper = LightmansHelper.getItem("lightmanscurrency", "coinpile_copper");
            pileIron = LightmansHelper.getItem("lightmanscurrency", "coinpile_iron");
            pileGold = LightmansHelper.getItem("lightmanscurrency", "coinpile_gold");
            pileEmerald = LightmansHelper.getItem("lightmanscurrency", "coinpile_emerald");
            pileDiamond = LightmansHelper.getItem("lightmanscurrency", "coinpile_diamond");
            pileNetherite = LightmansHelper.getItem("lightmanscurrency", "coinpile_netherite");
            blockCopper = LightmansHelper.getItem("lightmanscurrency", "coinblock_copper");
            blockIron = LightmansHelper.getItem("lightmanscurrency", "coinblock_iron");
            blockGold = LightmansHelper.getItem("lightmanscurrency", "coinblock_gold");
            blockEmerald = LightmansHelper.getItem("lightmanscurrency", "coinblock_emerald");
            blockDiamond = LightmansHelper.getItem("lightmanscurrency", "coinblock_diamond");
            blockNetherite = LightmansHelper.getItem("lightmanscurrency", "coinblock_netherite");
            boolean bl = lightmansAvailable = coinCopper != null;
            if (lightmansAvailable) {
                HariShopMod.LOGGER.info("LightmansCurrency integration enabled");
            } else {
                HariShopMod.LOGGER.warn("LightmansCurrency coins not found");
            }
        }
        catch (Exception e) {
            lightmansAvailable = false;
            HariShopMod.LOGGER.error("Failed to init LightmansCurrency integration", (Throwable)e);
        }
    }

    private static Item getItem(String namespace, String path) {
        try {
            return (Item)BuiltInRegistries.ITEM.get(new ResourceLocation(namespace, path));
        }
        catch (Exception e) {
            return null;
        }
    }

    public static boolean isAvailable() {
        LightmansHelper.init();
        return lightmansAvailable || isNumismaticAvailable();
    }

    private static boolean isNumismaticAvailable() {
        try {
            Class.forName("tallestred.numismaticoverhaul.currency.CurrencyHelper", false,
                    LightmansHelper.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static long getCoinValue(Item item) {
        if (item == null) {
            return 0L;
        }
        if (item == coinCopper) {
            return 1L;
        }
        if (item == coinIron) {
            return 10L;
        }
        if (item == coinGold) {
            return 100L;
        }
        if (item == coinEmerald) {
            return 1000L;
        }
        if (item == coinDiamond) {
            return 10000L;
        }
        if (item == coinNetherite) {
            return 100000L;
        }
        if (item == pileCopper) {
            return 9L;
        }
        if (item == pileIron) {
            return 90L;
        }
        if (item == pileGold) {
            return 900L;
        }
        if (item == pileEmerald) {
            return 9000L;
        }
        if (item == pileDiamond) {
            return 90000L;
        }
        if (item == pileNetherite) {
            return 900000L;
        }
        if (item == blockCopper) {
            return 36L;
        }
        if (item == blockIron) {
            return 360L;
        }
        if (item == blockGold) {
            return 3600L;
        }
        if (item == blockEmerald) {
            return 36000L;
        }
        if (item == blockDiamond) {
            return 360000L;
        }
        if (item == blockNetherite) {
            return 3600000L;
        }
        return 0L;
    }

    public static long getPlayerMoney(Player player) {
        LightmansHelper.init();
        if (!lightmansAvailable || player == null) {
            return NumismaticHelper.getPlayerMoney(player);
        }
        long total = 0L;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            long coinValue;
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || (coinValue = LightmansHelper.getCoinValue(stack.getItem())) <= 0L) continue;
            try {
                total = Math.addExact(total, Math.multiplyExact(coinValue, (long)stack.getCount()));
                if (total > MoneyMath.MAX_BALANCE) {
                    return MoneyMath.MAX_BALANCE;
                }
            } catch (ArithmeticException ex) {
                return MoneyMath.MAX_BALANCE;
            }
        }
        return total;
    }

    public static boolean canAfford(Player player, long copperPrice) {
        return copperPrice > 0L && copperPrice <= MoneyMath.MAX_BALANCE
                && LightmansHelper.getPlayerMoney(player) >= copperPrice;
    }

    public static boolean deductMoney(Player player, long copperAmount) {
        return deductMoney(player, copperAmount, "harishop:debit:" + (player == null ? "null" : player.getUUID()) + ":" + copperAmount + ":" + System.nanoTime());
    }

    /** Forwards a stable shop transaction id to Numismatic when available. */
    public static boolean deductMoney(Player player, long copperAmount, String transactionId) {
        Item[] coinOrder;
        LightmansHelper.init();
        if (player == null || copperAmount <= 0L || copperAmount > MoneyMath.MAX_BALANCE) {
            return false;
        }
        if (!lightmansAvailable) {
            return NumismaticHelper.deductMoney(player, copperAmount, transactionId);
        }
        long currentMoney = LightmansHelper.getPlayerMoney(player);
        if (currentMoney < copperAmount) {
            return false;
        }
        long remaining = copperAmount;
        Inventory inv = player.getInventory();
        List<ItemStack> before = new ArrayList<ItemStack>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            before.add(inv.getItem(i).copy());
        }
        for (Item coin : coinOrder = new Item[]{coinCopper, pileCopper, blockCopper, coinIron, pileIron, blockIron, coinGold, pileGold, blockGold, coinEmerald, pileEmerald, blockEmerald, coinDiamond, pileDiamond, blockDiamond, coinNetherite, pileNetherite, blockNetherite}) {
            long coinValue;
            if (coin == null || remaining <= 0L || (coinValue = LightmansHelper.getCoinValue(coin)) <= 0L) continue;
            for (int i = 0; i < inv.getContainerSize() && remaining > 0L; ++i) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty() || stack.getItem() != coin) continue;
                // Never round a denomination up: overcharging a player would
                // make a failed/partial transaction economically destructive.
                int neededCount = (int)Math.min((long)stack.getCount(), remaining / coinValue);
                if (neededCount <= 0) continue;
                long valueRemoved = (long)neededCount * coinValue;
                stack.shrink(neededCount);
                remaining -= valueRemoved;
            }
        }
        if (remaining == 0L) {
            return true;
        }
        // The physical inventory cannot make exact change.  Restore the
        // original slots and let the caller retry with a different payment
        // source instead of silently burning a larger coin.
        for (int i = 0; i < before.size(); i++) {
            inv.setItem(i, before.get(i).copy());
        }
        return false;
    }

    public static String formatPrice(long copperAmount) {
        if (copperAmount <= 0L) {
            return "Free";
        }
        StringBuilder sb = new StringBuilder();
        if (copperAmount >= 100000L) {
            long n = copperAmount / 100000L;
            copperAmount %= 100000L;
            sb.append(n).append("N");
        }
        if (copperAmount >= 10000L) {
            long d = copperAmount / 10000L;
            copperAmount %= 10000L;
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(d).append("D");
        }
        if (copperAmount >= 1000L) {
            long e = copperAmount / 1000L;
            copperAmount %= 1000L;
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(e).append("E");
        }
        if (copperAmount >= 100L) {
            long g = copperAmount / 100L;
            copperAmount %= 100L;
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(g).append("G");
        }
        if (copperAmount >= 10L) {
            long i = copperAmount / 10L;
            copperAmount %= 10L;
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(i).append("I");
        }
        if (copperAmount > 0L) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(copperAmount).append("C");
        }
        return sb.length() > 0 ? sb.toString() : "Free";
    }

    public static String formatPriceShort(long copperAmount) {
        if (copperAmount >= 100000L) {
            return String.format("%.1fN", (double)copperAmount / 100000.0);
        }
        if (copperAmount >= 10000L) {
            return String.format("%.1fD", (double)copperAmount / 10000.0);
        }
        if (copperAmount >= 1000L) {
            return String.format("%.1fE", (double)copperAmount / 1000.0);
        }
        if (copperAmount >= 100L) {
            return String.format("%.1fG", (double)copperAmount / 100.0);
        }
        if (copperAmount >= 10L) {
            return copperAmount / 10L + "I";
        }
        return copperAmount + "C";
    }

    public static boolean addMoney(Player player, long copperAmount) {
        return addMoney(player, copperAmount, "harishop:credit:" + (player == null ? "null" : player.getUUID()) + ":" + copperAmount + ":" + System.nanoTime());
    }

    /** Forwards a stable shop transaction id to Numismatic when available. */
    public static boolean addMoney(Player player, long copperAmount, String transactionId) {
        LightmansHelper.init();
        if (player == null || copperAmount <= 0L || copperAmount > MoneyMath.MAX_BALANCE) {
            return false;
        }
        if (!lightmansAvailable) {
            return NumismaticHelper.addMoney(player, copperAmount, transactionId);
        }
        Inventory inv = player.getInventory();
        if (copperAmount > 262400000L) {
            // A 41-slot inventory cannot safely receive a larger physical
            // payout using Lightman's single-coin denominations.
            return false;
        }
        List<ItemStack> grants = new ArrayList<ItemStack>();
        long remaining = copperAmount;
        if (remaining >= 100000L && coinNetherite != null) {
            int count = (int)(remaining / 100000L);
            remaining %= 100000L;
            grants.add(new ItemStack((ItemLike)coinNetherite, count));
        } else if (remaining >= 100000L) {
            return false;
        }
        if (remaining >= 10000L && coinDiamond != null) {
            int count = (int)(remaining / 10000L);
            remaining %= 10000L;
            grants.add(new ItemStack((ItemLike)coinDiamond, count));
        } else if (remaining >= 10000L) {
            return false;
        }
        if (remaining >= 1000L && coinEmerald != null) {
            int count = (int)(remaining / 1000L);
            remaining %= 1000L;
            grants.add(new ItemStack((ItemLike)coinEmerald, count));
        } else if (remaining >= 1000L) {
            return false;
        }
        if (remaining >= 100L && coinGold != null) {
            int count = (int)(remaining / 100L);
            remaining %= 100L;
            grants.add(new ItemStack((ItemLike)coinGold, count));
        } else if (remaining >= 100L) {
            return false;
        }
        if (remaining >= 10L && coinIron != null) {
            int count = (int)(remaining / 10L);
            remaining %= 10L;
            grants.add(new ItemStack((ItemLike)coinIron, count));
        } else if (remaining >= 10L) {
            return false;
        }
        if (remaining > 0L && coinCopper != null) {
            grants.add(new ItemStack((ItemLike)coinCopper, (int)remaining));
        } else if (remaining > 0L) {
            return false;
        }
        if (!canFit(inv, grants)) {
            return false;
        }
        List<ItemStack> before = new ArrayList<ItemStack>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            before.add(inv.getItem(i).copy());
        }
        for (ItemStack grant : grants) {
            ItemStack pending = grant.copy();
            if (!inv.add(pending)) {
                // Restore the exact pre-credit inventory.  Removing by item
                // identity alone could consume a player's existing coin
                // stack if a custom Inventory implementation partially adds.
                for (int i = 0; i < before.size(); i++) {
                    inv.setItem(i, before.get(i).copy());
                }
                return false;
            }
        }
        return true;
    }

    private static boolean canFit(Inventory inventory, List<ItemStack> grants) {
        List<ItemStack> virtual = new ArrayList<ItemStack>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            virtual.add(inventory.getItem(i).copy());
        }
        for (ItemStack grant : grants) {
            int remaining = grant.getCount();
            for (ItemStack slot : virtual) {
                if (remaining <= 0) break;
                if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, grant)) continue;
                int moved = Math.min(Math.max(0, slot.getMaxStackSize() - slot.getCount()), remaining);
                slot.grow(moved);
                remaining -= moved;
            }
            for (int i = 0; i < virtual.size() && remaining > 0; i++) {
                if (!virtual.get(i).isEmpty()) continue;
                int moved = Math.min(grant.getMaxStackSize(), remaining);
                ItemStack placed = grant.copy();
                placed.setCount(moved);
                virtual.set(i, placed);
                remaining -= moved;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static void giveCoins(Inventory inv, Item coin, int count) {
        int toGive;
        ItemStack stack;
        while (count > 0 && inv.add(stack = new ItemStack((ItemLike)coin, toGive = Math.min(count, coin.getMaxStackSize())))) {
            count -= toGive;
        }
    }

    public static List<CoinDisplay> getCoinDisplayInfo(long copperAmount) {
        int displayCount;
        Item icon;
        long count;
        LightmansHelper.init();
        ArrayList<CoinDisplay> displays = new ArrayList<CoinDisplay>();
        if (!lightmansAvailable || copperAmount <= 0L) {
            return displays;
        }
        long remaining = copperAmount;
        if (remaining >= 100000L) {
            count = remaining / 100000L;
            remaining %= 100000L;
            if (count >= 81L && blockNetherite != null) {
                icon = blockNetherite;
                displayCount = (int)(count / 81L);
                remaining += count % 81L * 100000L;
            } else if (count >= 9L && pileNetherite != null) {
                icon = pileNetherite;
                displayCount = (int)(count / 9L);
                remaining += count % 9L * 100000L;
            } else {
                icon = coinNetherite;
                displayCount = (int)count;
            }
            if (icon != null && displayCount > 0) {
                displays.add(new CoinDisplay(icon, displayCount, "" + displayCount));
            }
        }
        if (remaining >= 10000L) {
            count = remaining / 10000L;
            remaining %= 10000L;
            if (count >= 81L && blockDiamond != null) {
                icon = blockDiamond;
                displayCount = (int)(count / 81L);
                remaining += count % 81L * 10000L;
            } else if (count >= 9L && pileDiamond != null) {
                icon = pileDiamond;
                displayCount = (int)(count / 9L);
                remaining += count % 9L * 10000L;
            } else {
                icon = coinDiamond;
                displayCount = (int)count;
            }
            if (icon != null && displayCount > 0) {
                displays.add(new CoinDisplay(icon, displayCount, "" + displayCount));
            }
        }
        if (remaining >= 1000L) {
            count = remaining / 1000L;
            remaining %= 1000L;
            if (count >= 81L && blockEmerald != null) {
                icon = blockEmerald;
                displayCount = (int)(count / 81L);
                remaining += count % 81L * 1000L;
            } else if (count >= 9L && pileEmerald != null) {
                icon = pileEmerald;
                displayCount = (int)(count / 9L);
                remaining += count % 9L * 1000L;
            } else {
                icon = coinEmerald;
                displayCount = (int)count;
            }
            if (icon != null && displayCount > 0) {
                displays.add(new CoinDisplay(icon, displayCount, "" + displayCount));
            }
        }
        if (remaining >= 100L) {
            count = remaining / 100L;
            remaining %= 100L;
            if (count >= 81L && blockGold != null) {
                icon = blockGold;
                displayCount = (int)(count / 81L);
                remaining += count % 81L * 100L;
            } else if (count >= 9L && pileGold != null) {
                icon = pileGold;
                displayCount = (int)(count / 9L);
                remaining += count % 9L * 100L;
            } else {
                icon = coinGold;
                displayCount = (int)count;
            }
            if (icon != null && displayCount > 0) {
                displays.add(new CoinDisplay(icon, displayCount, "" + displayCount));
            }
        }
        if (remaining >= 10L) {
            count = remaining / 10L;
            remaining %= 10L;
            if (count >= 81L && blockIron != null) {
                icon = blockIron;
                displayCount = (int)(count / 81L);
                remaining += count % 81L * 10L;
            } else if (count >= 9L && pileIron != null) {
                icon = pileIron;
                displayCount = (int)(count / 9L);
                remaining += count % 9L * 10L;
            } else {
                icon = coinIron;
                displayCount = (int)count;
            }
            if (icon != null && displayCount > 0) {
                displays.add(new CoinDisplay(icon, displayCount, "" + displayCount));
            }
        }
        if (remaining >= 1L) {
            count = remaining;
            if (count >= 81L && blockCopper != null) {
                icon = blockCopper;
                displayCount = (int)(count / 81L);
            } else if (count >= 9L && pileCopper != null) {
                icon = pileCopper;
                displayCount = (int)(count / 9L);
            } else {
                icon = coinCopper;
                displayCount = (int)count;
            }
            if (icon != null && displayCount > 0) {
                displays.add(new CoinDisplay(icon, displayCount, "" + displayCount));
            }
        }
        return displays;
    }

    public static Item getBestCoinIcon(long copperAmount) {
        LightmansHelper.init();
        if (copperAmount >= 8100000L && blockNetherite != null) {
            return blockNetherite;
        }
        if (copperAmount >= 900000L && pileNetherite != null) {
            return pileNetherite;
        }
        if (copperAmount >= 100000L && coinNetherite != null) {
            return coinNetherite;
        }
        if (copperAmount >= 810000L && blockDiamond != null) {
            return blockDiamond;
        }
        if (copperAmount >= 90000L && pileDiamond != null) {
            return pileDiamond;
        }
        if (copperAmount >= 10000L && coinDiamond != null) {
            return coinDiamond;
        }
        if (copperAmount >= 81000L && blockEmerald != null) {
            return blockEmerald;
        }
        if (copperAmount >= 9000L && pileEmerald != null) {
            return pileEmerald;
        }
        if (copperAmount >= 1000L && coinEmerald != null) {
            return coinEmerald;
        }
        if (copperAmount >= 8100L && blockGold != null) {
            return blockGold;
        }
        if (copperAmount >= 900L && pileGold != null) {
            return pileGold;
        }
        if (copperAmount >= 100L && coinGold != null) {
            return coinGold;
        }
        if (copperAmount >= 810L && blockIron != null) {
            return blockIron;
        }
        if (copperAmount >= 90L && pileIron != null) {
            return pileIron;
        }
        if (copperAmount >= 10L && coinIron != null) {
            return coinIron;
        }
        if (copperAmount >= 81L && blockCopper != null) {
            return blockCopper;
        }
        if (copperAmount >= 9L && pileCopper != null) {
            return pileCopper;
        }
        return coinCopper;
    }

    public static Item getCoinCopper() {
        LightmansHelper.init();
        return coinCopper;
    }

    public static Item getCoinIron() {
        LightmansHelper.init();
        return coinIron;
    }

    public static Item getCoinGold() {
        LightmansHelper.init();
        return coinGold;
    }

    public static Item getCoinEmerald() {
        LightmansHelper.init();
        return coinEmerald;
    }

    public static Item getCoinDiamond() {
        LightmansHelper.init();
        return coinDiamond;
    }

    public static Item getCoinNetherite() {
        LightmansHelper.init();
        return coinNetherite;
    }

    public static Item getPileCopper() {
        LightmansHelper.init();
        return pileCopper;
    }

    public static Item getPileIron() {
        LightmansHelper.init();
        return pileIron;
    }

    public static Item getPileGold() {
        LightmansHelper.init();
        return pileGold;
    }

    public static Item getPileEmerald() {
        LightmansHelper.init();
        return pileEmerald;
    }

    public static Item getPileDiamond() {
        LightmansHelper.init();
        return pileDiamond;
    }

    public static Item getPileNetherite() {
        LightmansHelper.init();
        return pileNetherite;
    }

    public static Item getBlockCopper() {
        LightmansHelper.init();
        return blockCopper;
    }

    public static Item getBlockIron() {
        LightmansHelper.init();
        return blockIron;
    }

    public static Item getBlockGold() {
        LightmansHelper.init();
        return blockGold;
    }

    public static Item getBlockEmerald() {
        LightmansHelper.init();
        return blockEmerald;
    }

    public static Item getBlockDiamond() {
        LightmansHelper.init();
        return blockDiamond;
    }

    public static Item getBlockNetherite() {
        LightmansHelper.init();
        return blockNetherite;
    }

    public static class CoinDisplay {
        public final Item icon;
        public final int count;
        public final String label;

        public CoinDisplay(Item icon, int count, String label) {
            this.icon = icon;
            this.count = count;
            this.label = label;
        }
    }
}

