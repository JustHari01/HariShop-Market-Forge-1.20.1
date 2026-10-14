/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 */
package hari.harishop.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;


public class TagPricing {
    private static final Map<String, Long> TAG_PRICES = new HashMap<String, Long>();

    public static long getPriceByTags(Item item) {
        long highestPrice = -1L;
        for (TagKey<Item> tagKey : item.builtInRegistryHolder().tags().toList()) {
            String tagPath = tagKey.location().toString();
            Long price = TAG_PRICES.get(tagPath);
            if (price != null && price > highestPrice) {
                highestPrice = price;
            }
            for (Map.Entry<String, Long> entry : TAG_PRICES.entrySet()) {
                if (!tagPath.startsWith(entry.getKey()) && !entry.getKey().startsWith(tagPath) || entry.getValue() <= highestPrice) continue;
                highestPrice = entry.getValue();
            }
        }
        return highestPrice;
    }

    public static boolean hasValuableTag(Item item) {
        return TagPricing.getPriceByTags(item) > 0L;
    }

    static {
        TAG_PRICES.put("forge:ores/coal", 30L);
        TAG_PRICES.put("forge:ores/copper", 50L);
        TAG_PRICES.put("forge:ores/iron", 100L);
        TAG_PRICES.put("forge:ores/gold", 500L);
        TAG_PRICES.put("forge:ores/lapis", 200L);
        TAG_PRICES.put("forge:ores/redstone", 150L);
        TAG_PRICES.put("forge:ores/diamond", 5000L);
        TAG_PRICES.put("forge:ores/emerald", 3000L);
        TAG_PRICES.put("forge:ores/quartz", 100L);
        TAG_PRICES.put("forge:ores/netherite_scrap", 15000L);
        TAG_PRICES.put("forge:raw_materials/coal", 25L);
        TAG_PRICES.put("forge:raw_materials/copper", 40L);
        TAG_PRICES.put("forge:raw_materials/iron", 80L);
        TAG_PRICES.put("forge:raw_materials/gold", 400L);
        TAG_PRICES.put("forge:ingots/copper", 50L);
        TAG_PRICES.put("forge:ingots/iron", 100L);
        TAG_PRICES.put("forge:ingots/gold", 500L);
        TAG_PRICES.put("forge:ingots/netherite", 50000L);
        TAG_PRICES.put("forge:gems/diamond", 5000L);
        TAG_PRICES.put("forge:gems/emerald", 3000L);
        TAG_PRICES.put("forge:gems/lapis", 50L);
        TAG_PRICES.put("forge:gems/quartz", 80L);
        TAG_PRICES.put("forge:gems/amethyst", 150L);
        TAG_PRICES.put("forge:gems/prismarine", 100L);
        TAG_PRICES.put("forge:storage_blocks/coal", 250L);
        TAG_PRICES.put("forge:storage_blocks/copper", 450L);
        TAG_PRICES.put("forge:storage_blocks/iron", 900L);
        TAG_PRICES.put("forge:storage_blocks/gold", 4500L);
        TAG_PRICES.put("forge:storage_blocks/lapis", 450L);
        TAG_PRICES.put("forge:storage_blocks/redstone", 1350L);
        TAG_PRICES.put("forge:storage_blocks/diamond", 45000L);
        TAG_PRICES.put("forge:storage_blocks/emerald", 27000L);
        TAG_PRICES.put("forge:storage_blocks/netherite", 450000L);
        TAG_PRICES.put("forge:storage_blocks/quartz", 360L);
        TAG_PRICES.put("forge:storage_blocks/amethyst", 1350L);
        TAG_PRICES.put("forge:dusts/redstone", 15L);
        TAG_PRICES.put("forge:dusts/glowstone", 30L);
        TAG_PRICES.put("forge:dusts/prismarine", 50L);
        TAG_PRICES.put("forge:nuggets/iron", 11L);
        TAG_PRICES.put("forge:nuggets/gold", 55L);
        TAG_PRICES.put("forge:rods/blaze", 300L);
        TAG_PRICES.put("forge:rods/breeze", 500L);
        TAG_PRICES.put("forge:ender_pearls", 500L);
        TAG_PRICES.put("forge:nether_stars", 100000L);
        TAG_PRICES.put("forge:shulker_shells", 5000L);
        TAG_PRICES.put("forge:heads", 3000L);
        TAG_PRICES.put("forge:eggs", 50L);
        TAG_PRICES.put("forge:foods", 50L);
        TAG_PRICES.put("forge:foods/golden", 1000L);
        TAG_PRICES.put("minecraft:logs", 10L);
        TAG_PRICES.put("minecraft:planks", 5L);
        TAG_PRICES.put("forge:fence_gates/wooden", 15L);
        TAG_PRICES.put("forge:stone", 5L);
        TAG_PRICES.put("forge:cobblestone", 3L);
        TAG_PRICES.put("forge:sandstone", 8L);
        TAG_PRICES.put("forge:obsidian", 200L);
        TAG_PRICES.put("forge:end_stones", 100L);
        TAG_PRICES.put("forge:crops", 20L);
        TAG_PRICES.put("forge:seeds", 10L);
        TAG_PRICES.put("forge:dyes", 30L);
        TAG_PRICES.put("forge:leather", 80L);
        TAG_PRICES.put("forge:bones", 25L);
        TAG_PRICES.put("forge:string", 20L);
        TAG_PRICES.put("forge:slimeballs", 50L);
        TAG_PRICES.put("forge:feathers", 15L);
        TAG_PRICES.put("forge:glass", 20L);
        TAG_PRICES.put("forge:glass_panes", 8L);
        TAG_PRICES.put("minecraft:wool", 40L);
        TAG_PRICES.put("minecraft:terracotta", 50L);
        TAG_PRICES.put("minecraft:concrete", 30L);
    }
}

