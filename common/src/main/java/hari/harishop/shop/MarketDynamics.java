/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 */
package hari.harishop.shop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.Item;

public class MarketDynamics {
    // Volume is state, not an unbounded client-controlled counter.  A long
    // plus a hard cap prevents int wraparound from turning a buy burst into a
    // negative demand discount (or vice versa).
    private static final Map<Item, Long> tradeVolume = new ConcurrentHashMap<Item, Long>();
    private static final long MAX_VOLUME = 1_000_000_000L;
    private static final double SENSITIVITY = 5.0E-4;
    private static final double MAX_MULTIPLIER = 5.0;
    private static final double MIN_MULTIPLIER = 0.1;

    public static void init() {
    }

    public static void recordTransaction(Item item, int quantity, boolean isBuy) {
        if (item == null || quantity <= 0) return;
        long change = isBuy ? quantity : -((long) quantity);
        tradeVolume.merge(item, change, MarketDynamics::boundedAdd);
    }

    public static double getMarketMultiplier(Item item) {
        long volume = tradeVolume.getOrDefault(item, 0L);
        if (volume == 0) {
            return 1.0;
        }
        double multiplier = Math.exp(volume * Math.log1p(0.0005));
        return Math.min(5.0, Math.max(0.1, multiplier));
    }

    public static void decayMarket() {
        for (Item item : tradeVolume.keySet()) {
            long volume = tradeVolume.get(item);
            if (volume == 0) continue;
            long newVolume = (long)((double)volume * 0.8);
            if (Math.abs(newVolume) < 5L) {
                newVolume = 0;
            }
            tradeVolume.put(item, newVolume);
        }
    }

    private static long boundedAdd(long left, long right) {
        if (right > 0L && left > MAX_VOLUME - right) return MAX_VOLUME;
        if (right < 0L && left < -MAX_VOLUME - right) return -MAX_VOLUME;
        long next = left + right;
        return Math.max(-MAX_VOLUME, Math.min(MAX_VOLUME, next));
    }

    public static String getMarketState(Item item) {
        double mult = MarketDynamics.getMarketMultiplier(item);
        if (mult > 1.2) {
            return "High Demand (+" + (int)((mult - 1.0) * 100.0) + "%)";
        }
        if (mult > 1.05) {
            return "Rising Demand";
        }
        if (mult < 0.5) {
            return "Market Crashed";
        }
        if (mult < 0.8) {
            return "Oversupplied (-" + (int)((1.0 - mult) * 100.0) + "%)";
        }
        return "Stable";
    }
}

