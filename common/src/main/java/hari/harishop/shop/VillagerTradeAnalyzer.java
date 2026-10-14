/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  net.minecraft.world.entity.npc.VillagerProfession
 *  net.minecraft.world.entity.npc.VillagerTrades
 *  net.minecraft.world.entity.npc.VillagerTrades$ItemListing
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public class VillagerTradeAnalyzer {
    private static boolean initialized = false;
    private static final Map<Item, TradeInfo> villagerBuys = new ConcurrentHashMap<Item, TradeInfo>();
    private static final Map<Item, TradeInfo> villagerSells = new ConcurrentHashMap<Item, TradeInfo>();
    private static final long EMERALD_VALUE = 3000L;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        HariShopMod.LOGGER.info("VillagerTradeAnalyzer: Loading villager trades...");
        long start = System.currentTimeMillis();
        try {
            for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
                Int2ObjectMap trades = (Int2ObjectMap)VillagerTrades.TRADES.get(profession);
                if (trades == null) continue;
                for (int level = 1; level <= 5; ++level) {
                    VillagerTrades.ItemListing[] listings = (VillagerTrades.ItemListing[])trades.get(level);
                    if (listings == null) continue;
                    for (VillagerTrades.ItemListing listing : listings) {
                        try {
                            VillagerTradeAnalyzer.extractTradeInfo(listing, level, profession);
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                    }
                }
            }
            HariShopMod.LOGGER.info("VillagerTradeAnalyzer: Loaded {} buy trades, {} sell trades in {}ms", (Object)villagerBuys.size(), (Object)villagerSells.size(), (Object)(System.currentTimeMillis() - start));
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("VillagerTradeAnalyzer: Failed to load trades", (Throwable)e);
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static void extractTradeInfo(VillagerTrades.ItemListing listing, int level, VillagerProfession profession) {
        try {
            Class<?> clazz = listing.getClass();
            if (clazz.getSimpleName().contains("EmeraldForItems") || clazz.getName().contains("EmeraldForItems")) {
                VillagerTradeAnalyzer.extractEmeraldForItems(listing, level, profession);
                return;
            }
            if (clazz.getSimpleName().contains("ItemsForEmeralds") || clazz.getName().contains("ItemsForEmeralds")) {
                VillagerTradeAnalyzer.extractItemsForEmeralds(listing, level, profession);
                return;
            }
            if (!clazz.getSimpleName().contains("ItemsAndEmeralds")) return;
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void extractEmeraldForItems(VillagerTrades.ItemListing listing, int level, VillagerProfession profession) {
        try {
            Class<?> clazz = listing.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(listing);
                if (value instanceof Item) {
                    Item item = (Item)value;
                    int cost = 1;
                    int count = 1;
                    for (Field f2 : clazz.getDeclaredFields()) {
                        int v;
                        f2.setAccessible(true);
                        if (f2.getType() != Integer.TYPE || (v = f2.getInt(listing)) < 1 || v > 64) continue;
                        if (f2.getName().toLowerCase().contains("cost") || f2.getName().toLowerCase().contains("emerald")) {
                            cost = v;
                            continue;
                        }
                        if (!f2.getName().toLowerCase().contains("count") && !f2.getName().toLowerCase().contains("number")) continue;
                        count = v;
                    }
                    TradeInfo info = new TradeInfo(item, cost, count, level, profession);
                    villagerBuys.putIfAbsent(item, info);
                } else {
                    ItemStack stack;
                    if (!(value instanceof ItemStack) || (stack = (ItemStack)value).isEmpty()) continue;
                    TradeInfo info = new TradeInfo(stack.getItem(), 1, stack.getCount(), level, profession);
                    villagerBuys.putIfAbsent(stack.getItem(), info);
                }
                break;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void extractItemsForEmeralds(VillagerTrades.ItemListing listing, int level, VillagerProfession profession) {
        try {
            Class<?> clazz = listing.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                ItemStack stack;
                field.setAccessible(true);
                Object value = field.get(listing);
                if (!(value instanceof ItemStack) || (stack = (ItemStack)value).isEmpty()) continue;
                int cost = 1;
                for (Field f2 : clazz.getDeclaredFields()) {
                    int v;
                    f2.setAccessible(true);
                    if (f2.getType() != Integer.TYPE || (v = f2.getInt(listing)) < 1 || v > 64) continue;
                    cost = v;
                    break;
                }
                TradeInfo info = new TradeInfo(stack.getItem(), cost, stack.getCount(), level, profession);
                villagerSells.putIfAbsent(stack.getItem(), info);
                break;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static boolean doVillagersBuy(Item item) {
        VillagerTradeAnalyzer.init();
        return villagerBuys.containsKey(item);
    }

    public static boolean doVillagersSell(Item item) {
        VillagerTradeAnalyzer.init();
        return villagerSells.containsKey(item);
    }

    public static TradeInfo getBuyTradeInfo(Item item) {
        VillagerTradeAnalyzer.init();
        return villagerBuys.get(item);
    }

    public static TradeInfo getSellTradeInfo(Item item) {
        VillagerTradeAnalyzer.init();
        return villagerSells.get(item);
    }

    public static long getSuggestedPrice(Item item) {
        VillagerTradeAnalyzer.init();
        TradeInfo sellInfo = villagerSells.get(item);
        if (sellInfo != null) {
            return sellInfo.getCopperValuePerItem();
        }
        TradeInfo buyInfo = villagerBuys.get(item);
        if (buyInfo != null) {
            return buyInfo.getCopperValuePerItem() / 3L;
        }
        return -1L;
    }

    public static Map<Item, TradeInfo> getAllBuyTrades() {
        VillagerTradeAnalyzer.init();
        return villagerBuys;
    }

    public static Map<Item, TradeInfo> getAllSellTrades() {
        VillagerTradeAnalyzer.init();
        return villagerSells;
    }

    public static class TradeInfo {
        public final Item item;
        public final int emeraldPrice;
        public final int itemCount;
        public final int level;
        public final VillagerProfession profession;

        public TradeInfo(Item item, int emeraldPrice, int itemCount, int level, VillagerProfession profession) {
            this.item = item;
            this.emeraldPrice = emeraldPrice;
            this.itemCount = itemCount;
            this.level = level;
            this.profession = profession;
        }

        public long getCopperValuePerItem() {
            if (this.itemCount <= 0) {
                return 0L;
            }
            long totalValue = (long)this.emeraldPrice * 3000L;
            return totalValue / (long)this.itemCount;
        }

        public String toString() {
            return String.format("TradeInfo[%dx for %d emeralds, level=%d, %s]", this.itemCount, this.emeraldPrice, this.level, this.profession.name());
        }
    }
}

