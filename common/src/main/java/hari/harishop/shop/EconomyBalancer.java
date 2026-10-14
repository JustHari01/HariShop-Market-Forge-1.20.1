/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import hari.harishop.shop.MarketDynamics;
import hari.harishop.v5.MoneyMath;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

public class EconomyBalancer {
    public static final long COPPER = 1L;
    public static final long IRON = 10L;
    public static final long GOLD = 100L;
    public static final long EMERALD = 1000L;
    public static final long DIAMOND = 10000L;
    public static final long NETHERITE = 100000L;
    public static final double MOB_INCOME_T1 = 4.125;
    public static final double MOB_INCOME_T2 = 15.0;
    public static final double MOB_INCOME_T3 = 75.0;
    public static final double MOB_INCOME_T4 = 200.0;
    public static final double MOB_INCOME_T5 = 1000.0;
    public static final double MOB_INCOME_T6 = 5000.0;
    public static final double[] MOB_INCOME_BY_TIER = new double[]{0.0, 4.125, 19.125, 94.125, 294.125, 1294.125, 6294.125};
    public static final double MOBS_PER_HOUR = 60.0;
    public static final double AVG_MOB_TIER = 2.5;
    public static final double ESTIMATED_HOURLY_INCOME = 60.0 * MOB_INCOME_BY_TIER[2];
    private static final double MIN_FLUCTUATION = 0.8;
    private static final double MAX_FLUCTUATION = 1.2;
    private static final long NIGHT_MARKET_REFRESH_INTERVAL = 43200000L;
    private static final long DAILY_DEAL_REFRESH_INTERVAL = 21600000L;
    private static final Map<Item, Double> shopFluctuations = new ConcurrentHashMap<Item, Double>();
    private static final Map<Item, Double> dailyDealFluctuations = new ConcurrentHashMap<Item, Double>();
    private static long lastShopRefresh = 0L;
    private static long lastDailyDealRefresh = 0L;
    private static final Random random = new Random();

    public static void init() {
        long currentTime = System.currentTimeMillis();
        long daySeed = currentTime / 43200000L;
        long dealSeed = currentTime / 21600000L;
        EconomyBalancer.refreshShopPrices(daySeed);
        EconomyBalancer.refreshDailyDealPrices(dealSeed);
        HariShopMod.LOGGER.info("EconomyBalancer initialized - Shop refresh seed: {}, Deal refresh seed: {}", (Object)daySeed, (Object)dealSeed);
    }

    public static void checkRefresh() {
        long currentTime = System.currentTimeMillis();
        long daySeed = currentTime / 43200000L;
        long dealSeed = currentTime / 21600000L;
        if (daySeed != lastShopRefresh) {
            EconomyBalancer.refreshShopPrices(daySeed);
        }
        if (dealSeed != lastDailyDealRefresh) {
            EconomyBalancer.refreshDailyDealPrices(dealSeed);
        }
    }

    private static void refreshShopPrices(long seed) {
        lastShopRefresh = seed;
        shopFluctuations.clear();
        Random seededRandom = new Random(seed);
        for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
            double fluctuation = 0.8 + seededRandom.nextDouble() * 0.3999999999999999;
            shopFluctuations.put(item, fluctuation);
        }
        HariShopMod.LOGGER.info("Shop prices refreshed (seed: {})", (Object)seed);
        MarketDynamics.decayMarket();
    }

    private static void refreshDailyDealPrices(long seed) {
        lastDailyDealRefresh = seed;
        dailyDealFluctuations.clear();
        Random seededRandom = new Random(seed * 31L);
        for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
            double fluctuation = 0.5 + seededRandom.nextDouble() * 1.0;
            dailyDealFluctuations.put(item, fluctuation);
        }
        HariShopMod.LOGGER.info("Daily Deal prices refreshed (seed: {})", (Object)seed);
    }

    public static double getShopFluctuation(Item item) {
        EconomyBalancer.checkRefresh();
        return shopFluctuations.getOrDefault(item, 1.0);
    }

    public static double getDailyDealFluctuation(Item item) {
        EconomyBalancer.checkRefresh();
        return dailyDealFluctuations.getOrDefault(item, 1.0);
    }

    public static long applyShopFluctuation(Item item, long basePrice) {
        if (item == null || basePrice <= 0L) {
            return 0L;
        }
        double randomFluctuation = EconomyBalancer.getShopFluctuation(item);
        double marketMultiplier = MarketDynamics.getMarketMultiplier(item);
        try {
            // Buy quotes are rounded up and bounded with decimal arithmetic;
            // a large base value can never wrap through a double-to-long cast.
            BigDecimal result = BigDecimal.valueOf(basePrice)
                    .multiply(BigDecimal.valueOf(randomFluctuation))
                    .multiply(BigDecimal.valueOf(marketMultiplier));
            return Math.max(1L, MoneyMath.rounded(1L, result, RoundingMode.CEILING));
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return 0L;
        }
    }

    public static long applyDailyDealFluctuation(Item item, long basePrice) {
        if (item == null || basePrice <= 0L) {
            return 0L;
        }
        double fluctuation = EconomyBalancer.getDailyDealFluctuation(item);
        try {
            BigDecimal result = BigDecimal.valueOf(basePrice).multiply(BigDecimal.valueOf(fluctuation));
            return Math.max(1L, MoneyMath.rounded(1L, result, RoundingMode.CEILING));
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return 0L;
        }
    }

    public static long getSellPrice(long buyPrice) {
        if (buyPrice <= 0L) {
            return 0L;
        }
        try {
            BigDecimal ratio = BigDecimal.valueOf(hari.harishop.config.ShopConfig.get().sellPriceRatio);
            if (ratio.signum() <= 0) {
                return 0L;
            }
            // Pawn payouts are rounded down, never up to a value the market
            // did not earn.  A positive quote still has a one-Dong floor.
            return Math.max(1L, MoneyMath.rounded(buyPrice, ratio, RoundingMode.FLOOR));
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return 0L;
        }
    }

    public static long getSellPriceForItem(Item item, long basePrice) {
        long buyPrice = EconomyBalancer.applyShopFluctuation(item, basePrice);
        return EconomyBalancer.getSellPrice(buyPrice);
    }

    public static boolean isOnSale(Item item) {
        return EconomyBalancer.getDailyDealFluctuation(item) < EconomyBalancer.getShopFluctuation(item);
    }

    public static int getSalePercentage(Item item) {
        double shopFluc;
        double dealFluc = EconomyBalancer.getDailyDealFluctuation(item);
        if (dealFluc >= (shopFluc = EconomyBalancer.getShopFluctuation(item))) {
            return 0;
        }
        return (int)((1.0 - dealFluc / shopFluc) * 100.0);
    }

    public static long getTimeUntilShopRefresh() {
        long currentTime = System.currentTimeMillis();
        long nextRefresh = (currentTime / 43200000L + 1L) * 43200000L;
        return nextRefresh - currentTime;
    }

    public static long getTimeUntilDealRefresh() {
        long currentTime = System.currentTimeMillis();
        long nextRefresh = (currentTime / 21600000L + 1L) * 21600000L;
        return nextRefresh - currentTime;
    }

    public static long getCurrentShopSeed() {
        return System.currentTimeMillis() / 43200000L;
    }

    public static long getCurrentDealSeed() {
        return System.currentTimeMillis() / 21600000L;
    }

    public static String formatTimeRemaining(long millis) {
        long seconds = millis / 1000L;
        long minutes = seconds / 60L % 60L;
        long hours = seconds / 3600L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static long getEstimatedCombatIncomePerHour() {
        return 1500L;
    }

    public static long getEstimatedFarmerIncomePerHour() {
        return 1500L;
    }

    public static long getBalancedCropPrice() {
        return EconomyBalancer.getEstimatedFarmerIncomePerHour() / 100L;
    }
}

