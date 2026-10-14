/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.storage.loot.LootParams
 *  net.minecraft.world.level.storage.loot.LootParams$Builder
 *  net.minecraft.world.level.storage.loot.LootTable
 *  net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.server.ServerLifecycleHooks
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import hari.harishop.shop.RecipeAnalyzer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.registries.BuiltInRegistries;


public class LootTableAnalyzer {
    private static final Map<Item, ItemSource> itemSources = new ConcurrentHashMap<Item, ItemSource>();
    private static final Map<Item, Double> dropRates = new ConcurrentHashMap<Item, Double>();
    private static boolean initialized = false;
    private static final Map<String, Integer> MOB_DIFFICULTY = new HashMap<String, Integer>();

    public static void init(MinecraftServer server) {
        if (initialized || server == null) {
            return;
        }
        HariShopMod.LOGGER.info("LootTableAnalyzer: Analyzing loot tables...");
        long start = System.currentTimeMillis();
        try {
            LootTableAnalyzer.analyzeEntityLootTables(server);
            LootTableAnalyzer.analyzeChestLootTables(server);
            initialized = true;
            HariShopMod.LOGGER.info("LootTableAnalyzer: Analyzed {} item sources in {}ms", (Object)itemSources.size(), (Object)(System.currentTimeMillis() - start));
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("LootTableAnalyzer: Failed to analyze loot tables", (Throwable)e);
        }
    }

    private static void analyzeEntityLootTables(MinecraftServer server) {
        Registry entityRegistry = server.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            try {
                LootTable lootTable;
                ResourceLocation lootTableId;
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                if (entityId == null || (lootTableId = entityType.getDefaultLootTable()) == null || (lootTable = server.getLootData().getLootTable(lootTableId)) == LootTable.EMPTY) continue;
                boolean isBoss = MOB_DIFFICULTY.getOrDefault(entityId.toString(), 0) >= 100;
                ItemSource source = isBoss ? ItemSource.BOSS_DROP : ItemSource.MOB_DROP;
                LootTableAnalyzer.extractItemsFromLootTable(server, lootTable, source, entityId.toString());
            }
            catch (Exception exception) {}
        }
    }

    private static void analyzeChestLootTables(MinecraftServer server) {
        String[] chestTables;
        for (String tableId : chestTables = new String[]{"minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft", "minecraft:chests/buried_treasure", "minecraft:chests/desert_pyramid", "minecraft:chests/end_city_treasure", "minecraft:chests/jungle_temple", "minecraft:chests/nether_bridge", "minecraft:chests/stronghold_corridor", "minecraft:chests/stronghold_crossing", "minecraft:chests/stronghold_library", "minecraft:chests/bastion_treasure", "minecraft:chests/ancient_city", "minecraft:chests/shipwreck_treasure", "minecraft:chests/village/village_weaponsmith", "minecraft:chests/village/village_toolsmith"}) {
            try {
                ResourceLocation id = new ResourceLocation(tableId);
                LootTable lootTable = server.getLootData().getLootTable(id);
                if (lootTable == LootTable.EMPTY) continue;
                LootTableAnalyzer.extractItemsFromLootTable(server, lootTable, ItemSource.CHEST_LOOT, tableId);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private static void extractItemsFromLootTable(MinecraftServer server, LootTable table, ItemSource source, String sourceId) {
        if (table == LootTable.EMPTY) {
            return;
        }
        try {
            if (server == null || server.overworld() == null) {
                return;
            }
            ServerLevel level = server.overworld();
            LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
            int simulations = 1000;
            HashMap<Item, Integer> dropCounts = new HashMap<Item, Integer>();
            for (int i = 0; i < simulations; ++i) {
                ObjectArrayList<ItemStack> drops = table.getRandomItems(params);
                for (ItemStack stack : drops) {
                    if (stack.isEmpty()) continue;
                    dropCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
            for (Map.Entry<Item, Integer> entry : dropCounts.entrySet()) {
                Item item = (Item)entry.getKey();
                int totalDrops = (Integer)entry.getValue();
                double avgAmount = (double)totalDrops / (double)simulations;
                if (!(avgAmount > 0.0)) continue;
                itemSources.put(item, source);
                double existing = dropRates.getOrDefault(item, 0.0);
                if (!(avgAmount > existing)) continue;
                dropRates.put(item, avgAmount);
            }
        }
        catch (Exception e) {
            HariShopMod.LOGGER.debug("Failed to simulate loot table " + sourceId, (Throwable)e);
        }
    }

    public static double getSimulatedDropMultiplier(Item item) {
        if (!dropRates.containsKey(item)) {
            return 1.0;
        }
        double avgAmount = dropRates.get(item);
        if (avgAmount <= 0.0) {
            return 1.0;
        }
        double baseMult = 1.0 / avgAmount;
        return Math.min(100.0, Math.max(0.5, baseMult));
    }

    public static ItemSource getItemSource(Item item) {
        LootTableAnalyzer.init(null);
        if (itemSources.containsKey(item)) {
            return itemSources.get(item);
        }
        if (RecipeAnalyzer.hasRecipe(item)) {
            return ItemSource.CRAFTABLE;
        }
        return LootTableAnalyzer.detectSourceFromKeywords(item);
    }

    private static ItemSource detectSourceFromKeywords(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return ItemSource.UNKNOWN;
        }
        String name = id.toString().toLowerCase();
        if (name.contains("nether_star") || name.contains("dragon_egg") || name.contains("dragon_breath") || name.contains("elytra")) {
            return ItemSource.BOSS_DROP;
        }
        if (name.contains("ender_pearl") || name.contains("blaze_rod") || name.contains("ghast_tear") || name.contains("phantom_membrane") || name.contains("slime_ball") || name.contains("shulker_shell") || name.contains("rotten_flesh") || name.contains("spider_eye") || name.contains("bone") || name.contains("gunpowder") || name.contains("_skull") || name.contains("_head") || name.contains("trident") || name.contains("totem")) {
            return ItemSource.MOB_DROP;
        }
        if (name.contains("heart_of_the_sea") || name.contains("nautilus") || name.contains("music_disc") || name.contains("enchanted_golden_apple") || name.contains("echo_shard") || name.contains("recovery_compass")) {
            return ItemSource.CHEST_LOOT;
        }
        if (name.contains("tropical_fish") || name.contains("pufferfish") || name.contains("cod") || name.contains("salmon") || name.contains("fishing")) {
            return ItemSource.FISHING;
        }
        if (name.contains("pottery_shard") || name.contains("suspicious")) {
            return ItemSource.ARCHAEOLOGY;
        }
        if (name.contains("_ore") || name.contains("raw_")) {
            return ItemSource.BLOCK_DROP;
        }
        return ItemSource.UNKNOWN;
    }

    public static double getSourceMultiplier(Item item) {
        return LootTableAnalyzer.getItemSource((Item)item).priceMultiplier;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    static {
        MOB_DIFFICULTY.put("minecraft:zombie", 5);
        MOB_DIFFICULTY.put("minecraft:skeleton", 6);
        MOB_DIFFICULTY.put("minecraft:spider", 5);
        MOB_DIFFICULTY.put("minecraft:creeper", 8);
        MOB_DIFFICULTY.put("minecraft:slime", 4);
        MOB_DIFFICULTY.put("minecraft:witch", 15);
        MOB_DIFFICULTY.put("minecraft:phantom", 12);
        MOB_DIFFICULTY.put("minecraft:enderman", 20);
        MOB_DIFFICULTY.put("minecraft:cave_spider", 10);
        MOB_DIFFICULTY.put("minecraft:drowned", 8);
        MOB_DIFFICULTY.put("minecraft:pillager", 12);
        MOB_DIFFICULTY.put("minecraft:vindicator", 18);
        MOB_DIFFICULTY.put("minecraft:evoker", 25);
        MOB_DIFFICULTY.put("minecraft:guardian", 20);
        MOB_DIFFICULTY.put("minecraft:blaze", 30);
        MOB_DIFFICULTY.put("minecraft:ghast", 35);
        MOB_DIFFICULTY.put("minecraft:magma_cube", 15);
        MOB_DIFFICULTY.put("minecraft:wither_skeleton", 40);
        MOB_DIFFICULTY.put("minecraft:piglin", 15);
        MOB_DIFFICULTY.put("minecraft:piglin_brute", 30);
        MOB_DIFFICULTY.put("minecraft:hoglin", 25);
        MOB_DIFFICULTY.put("minecraft:shulker", 35);
        MOB_DIFFICULTY.put("minecraft:endermite", 8);
        MOB_DIFFICULTY.put("minecraft:wither", 500);
        MOB_DIFFICULTY.put("minecraft:ender_dragon", 1000);
        MOB_DIFFICULTY.put("minecraft:elder_guardian", 100);
        MOB_DIFFICULTY.put("minecraft:warden", 200);
        MOB_DIFFICULTY.put("minecraft:iron_golem", 50);
    }

    public static enum ItemSource {
        CRAFTABLE(1.0),
        MOB_DROP(3.0),
        BOSS_DROP(10.0),
        CHEST_LOOT(5.0),
        FISHING(2.0),
        BLOCK_DROP(1.5),
        ARCHAEOLOGY(7.0),
        UNKNOWN(2.0);

        public final double priceMultiplier;

        private ItemSource(double mult) {
            this.priceMultiplier = mult;
        }
    }
}

