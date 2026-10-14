/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 */
package hari.harishop.shop;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class BaseItemValues {
    private static final long COPPER_PER_MINUTE = 25L;
    private static final Map<Item, Long> BASE_VALUES = new HashMap<Item, Long>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        BaseItemValues.put(Items.COAL, 4L);
        BaseItemValues.put(Items.CHARCOAL, 3L);
        BaseItemValues.put(Items.RAW_COPPER, 1L);
        BaseItemValues.put(Items.COPPER_INGOT, 2L);
        BaseItemValues.put(Items.RAW_IRON, 7L);
        BaseItemValues.put(Items.IRON_INGOT, 9L);
        BaseItemValues.put(Items.IRON_NUGGET, 1L);
        BaseItemValues.put(Items.RAW_GOLD, 70L);
        BaseItemValues.put(Items.GOLD_INGOT, 90L);
        BaseItemValues.put(Items.GOLD_NUGGET, 9L);
        BaseItemValues.put(Items.LAPIS_LAZULI, 15L);
        BaseItemValues.put(Items.REDSTONE, 5L);
        BaseItemValues.put(Items.QUARTZ, 8L);
        BaseItemValues.put(Items.AMETHYST_SHARD, 20L);
        BaseItemValues.put(Items.DIAMOND, 9000L);
        BaseItemValues.put(Items.EMERALD, 900L);
        BaseItemValues.put(Items.NETHERITE_SCRAP, 20000L);
        BaseItemValues.put(Items.NETHERITE_INGOT, 90000L);
        BaseItemValues.put(Items.ANCIENT_DEBRIS, 18000L);
        BaseItemValues.put(Items.ROTTEN_FLESH, 1L);
        BaseItemValues.put(Items.BONE, 2L);
        BaseItemValues.put(Items.STRING, 2L);
        BaseItemValues.put(Items.FEATHER, 1L);
        BaseItemValues.put(Items.SPIDER_EYE, 4L);
        BaseItemValues.put(Items.GUNPOWDER, 15L);
        BaseItemValues.put(Items.LEATHER, 8L);
        BaseItemValues.put(Items.ARROW, 3L);
        BaseItemValues.put(Items.INK_SAC, 5L);
        BaseItemValues.put(Items.GLOW_INK_SAC, 10L);
        BaseItemValues.put(Items.SLIME_BALL, 25L);
        BaseItemValues.put(Items.ENDER_PEARL, 80L);
        BaseItemValues.put(Items.PRISMARINE_SHARD, 15L);
        BaseItemValues.put(Items.BLAZE_ROD, 150L);
        BaseItemValues.put(Items.BLAZE_POWDER, 70L);
        BaseItemValues.put(Items.MAGMA_CREAM, 60L);
        BaseItemValues.put(Items.GHAST_TEAR, 400L);
        BaseItemValues.put(Items.WITHER_SKELETON_SKULL, 5000L);
        BaseItemValues.put(Items.NETHER_STAR, 75000L);
        BaseItemValues.put(Items.DRAGON_EGG, 100000L);
        BaseItemValues.put(Items.DRAGON_BREATH, 2000L);
        BaseItemValues.put(Items.ELYTRA, 50000L);
        BaseItemValues.put(Items.TOTEM_OF_UNDYING, 15000L);
        BaseItemValues.put(Items.TRIDENT, 10000L);
        BaseItemValues.put(Items.PHANTOM_MEMBRANE, 150L);
        BaseItemValues.put(Items.SHULKER_SHELL, 3000L);
        BaseItemValues.put(Items.NAUTILUS_SHELL, 1000L);
        BaseItemValues.put(Items.HEART_OF_THE_SEA, 10000L);
        BaseItemValues.put(Items.MUSIC_DISC_13, 2000L);
        BaseItemValues.put(Items.MUSIC_DISC_CAT, 2000L);
        BaseItemValues.put(Items.MUSIC_DISC_PIGSTEP, 5000L);
        BaseItemValues.put(Items.MUSIC_DISC_OTHERSIDE, 7500L);
        BaseItemValues.put(Items.SKELETON_SKULL, 1500L);
        BaseItemValues.put(Items.ZOMBIE_HEAD, 1500L);
        BaseItemValues.put(Items.CREEPER_HEAD, 2000L);
        BaseItemValues.put(Items.DRAGON_HEAD, 25000L);
        BaseItemValues.put(Items.PIGLIN_HEAD, 2000L);
        BaseItemValues.put(Items.WHEAT, 3L);
        BaseItemValues.put(Items.WHEAT_SEEDS, 1L);
        BaseItemValues.put(Items.CARROT, 5L);
        BaseItemValues.put(Items.POTATO, 5L);
        BaseItemValues.put(Items.BEETROOT, 5L);
        BaseItemValues.put(Items.BEETROOT_SEEDS, 3L);
        BaseItemValues.put(Items.MELON_SLICE, 2L);
        BaseItemValues.put(Items.PUMPKIN, 10L);
        BaseItemValues.put(Items.SUGAR_CANE, 5L);
        BaseItemValues.put(Items.COCOA_BEANS, 15L);
        BaseItemValues.put(Items.CACTUS, 8L);
        BaseItemValues.put(Items.BAMBOO, 3L);
        BaseItemValues.put(Items.KELP, 3L);
        BaseItemValues.put(Items.SWEET_BERRIES, 6L);
        BaseItemValues.put(Items.GLOW_BERRIES, 20L);
        BaseItemValues.put(Items.NETHER_WART, 80L);
        BaseItemValues.put(Items.CHORUS_FRUIT, 100L);
        BaseItemValues.put(Items.CHORUS_FLOWER, 200L);
        BaseItemValues.put(Items.OAK_SAPLING, 5L);
        BaseItemValues.put(Items.SPRUCE_SAPLING, 5L);
        BaseItemValues.put(Items.BIRCH_SAPLING, 5L);
        BaseItemValues.put(Items.JUNGLE_SAPLING, 15L);
        BaseItemValues.put(Items.ACACIA_SAPLING, 10L);
        BaseItemValues.put(Items.DARK_OAK_SAPLING, 10L);
        BaseItemValues.put(Items.CHERRY_SAPLING, 20L);
        BaseItemValues.put(Items.MANGROVE_PROPAGULE, 15L);
        BaseItemValues.put(Items.POPPY, 3L);
        BaseItemValues.put(Items.DANDELION, 3L);
        BaseItemValues.put(Items.BLUE_ORCHID, 10L);
        BaseItemValues.put(Items.ALLIUM, 10L);
        BaseItemValues.put(Items.AZURE_BLUET, 10L);
        BaseItemValues.put(Items.LILY_OF_THE_VALLEY, 15L);
        BaseItemValues.put(Items.WITHER_ROSE, 500L);
        BaseItemValues.put(Items.SPORE_BLOSSOM, 200L);
        BaseItemValues.put(Items.COBBLESTONE, 1L);
        BaseItemValues.put(Items.STONE, 2L);
        BaseItemValues.put(Items.GRANITE, 2L);
        BaseItemValues.put(Items.DIORITE, 2L);
        BaseItemValues.put(Items.ANDESITE, 2L);
        BaseItemValues.put(Items.DEEPSLATE, 3L);
        BaseItemValues.put(Items.TUFF, 3L);
        BaseItemValues.put(Items.CALCITE, 5L);
        BaseItemValues.put(Items.DRIPSTONE_BLOCK, 8L);
        BaseItemValues.put(Items.DIRT, 1L);
        BaseItemValues.put(Items.GRASS_BLOCK, 3L);
        BaseItemValues.put(Items.SAND, 2L);
        BaseItemValues.put(Items.RED_SAND, 4L);
        BaseItemValues.put(Items.GRAVEL, 2L);
        BaseItemValues.put(Items.CLAY_BALL, 8L);
        BaseItemValues.put(Items.CLAY, 32L);
        BaseItemValues.put(Items.OAK_LOG, 3L);
        BaseItemValues.put(Items.SPRUCE_LOG, 3L);
        BaseItemValues.put(Items.BIRCH_LOG, 3L);
        BaseItemValues.put(Items.JUNGLE_LOG, 5L);
        BaseItemValues.put(Items.ACACIA_LOG, 4L);
        BaseItemValues.put(Items.DARK_OAK_LOG, 4L);
        BaseItemValues.put(Items.CHERRY_LOG, 8L);
        BaseItemValues.put(Items.MANGROVE_LOG, 5L);
        BaseItemValues.put(Items.CRIMSON_STEM, 40L);
        BaseItemValues.put(Items.WARPED_STEM, 40L);
        BaseItemValues.put(Items.OBSIDIAN, 150L);
        BaseItemValues.put(Items.CRYING_OBSIDIAN, 400L);
        BaseItemValues.put(Items.NETHERRACK, 2L);
        BaseItemValues.put(Items.SOUL_SAND, 15L);
        BaseItemValues.put(Items.SOUL_SOIL, 15L);
        BaseItemValues.put(Items.BASALT, 10L);
        BaseItemValues.put(Items.BLACKSTONE, 15L);
        BaseItemValues.put(Items.GILDED_BLACKSTONE, 300L);
        BaseItemValues.put(Items.GLOWSTONE_DUST, 50L);
        BaseItemValues.put(Items.GLOWSTONE, 200L);
        BaseItemValues.put(Items.SHROOMLIGHT, 150L);
        BaseItemValues.put(Items.CRIMSON_NYLIUM, 75L);
        BaseItemValues.put(Items.WARPED_NYLIUM, 75L);
        BaseItemValues.put(Items.END_STONE, 30L);
        BaseItemValues.put(Items.PURPUR_BLOCK, 60L);
        BaseItemValues.put(Items.END_ROD, 150L);
        BaseItemValues.put(Items.ENDER_EYE, 600L);
        BaseItemValues.put(Items.PRISMARINE, 400L);
        BaseItemValues.put(Items.DARK_PRISMARINE, 500L);
        BaseItemValues.put(Items.SEA_LANTERN, 600L);
        BaseItemValues.put(Items.SPONGE, 1000L);
        BaseItemValues.put(Items.WET_SPONGE, 800L);
        BaseItemValues.put(Items.CONDUIT, 15000L);
        BaseItemValues.put(Items.SCULK, 200L);
        BaseItemValues.put(Items.SCULK_VEIN, 100L);
        BaseItemValues.put(Items.SCULK_SENSOR, 750L);
        BaseItemValues.put(Items.SCULK_CATALYST, 1500L);
        BaseItemValues.put(Items.SCULK_SHRIEKER, 2500L);
        BaseItemValues.put(Items.ECHO_SHARD, 3000L);
        BaseItemValues.put(Items.RECOVERY_COMPASS, 15000L);
        BaseItemValues.put(Items.ENCHANTED_GOLDEN_APPLE, 25000L);
        BaseItemValues.put(Items.GOLDEN_APPLE, 3500L);
        BaseItemValues.put(Items.BEACON, 77000L);
        BaseItemValues.put(Items.END_CRYSTAL, 3000L);
        BaseItemValues.put(Items.LODESTONE, 26000L);
        BaseItemValues.put(Items.RESPAWN_ANCHOR, 2500L);
        BaseItemValues.put(Items.ENCHANTED_BOOK, 1000L);
        BaseItemValues.put(Items.ZOMBIE_SPAWN_EGG, 5000L);
        BaseItemValues.put(Items.SKELETON_SPAWN_EGG, 5000L);
        BaseItemValues.put(Items.CREEPER_SPAWN_EGG, 7500L);
        BaseItemValues.put(Items.ENDERMAN_SPAWN_EGG, 10000L);
        BaseItemValues.put(Items.BLAZE_SPAWN_EGG, 15000L);
        BaseItemValues.put(Items.GHAST_SPAWN_EGG, 20000L);
        BaseItemValues.put(Items.WITHER_SKELETON_SPAWN_EGG, 25000L);
    }

    private static void put(Item item, long value) {
        BASE_VALUES.put(item, value);
    }

    public static long getBaseValue(Item item) {
        BaseItemValues.init();
        return BASE_VALUES.getOrDefault(item, -1L);
    }

    public static boolean hasBaseValue(Item item) {
        BaseItemValues.init();
        return BASE_VALUES.containsKey(item);
    }

    public static Map<Item, Long> getAllBaseValues() {
        BaseItemValues.init();
        return new HashMap<Item, Long>(BASE_VALUES);
    }
}

