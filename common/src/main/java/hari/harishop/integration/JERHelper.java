/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 * import dev.architectury.platform.Platform;
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.integration;

import hari.harishop.HariShopMod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;

public class JERHelper {
    private static boolean initialized = false;
    private static boolean jerAvailable = false;
    private static final Map<Item, Float> mobDropRates = new ConcurrentHashMap<Item, Float>();
    private static final Map<Item, String> mobDropSources = new ConcurrentHashMap<Item, String>();
    private static final Map<Item, Integer[]> worldGenLevels = new ConcurrentHashMap<Item, Integer[]>();
    private static final Map<Item, Float> dungeonDropRates = new ConcurrentHashMap<Item, Float>();
    private static final Map<Item, String> dungeonSources = new ConcurrentHashMap<Item, String>();
    private static final Set<Item> obtainableItems = Collections.newSetFromMap(new ConcurrentHashMap());
    private static final Map<Item, String> worldGenDimensions = new ConcurrentHashMap<Item, String>();
    private static final Map<Item, List<String>> worldGenBiomes = new ConcurrentHashMap<Item, List<String>>();
    private static final Map<Item, Boolean> worldGenSilkTouch = new ConcurrentHashMap<Item, Boolean>();
    private static final Map<Item, Float> worldGenBlocksPerChunk = new ConcurrentHashMap<Item, Float>();

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            jerAvailable = Platform.isModLoaded("jeresources");
        } catch (Throwable platformUnavailable) {
            // The integration is optional.  A headless test or a loader that
            // has not finished platform setup must not turn missing JER into
            // a shop crash; the normal fallback pricing remains active.
            jerAvailable = false;
            HariShopMod.LOGGER.debug("JERHelper: platform lookup unavailable; using fallback pricing", platformUnavailable);
        }
        if (!jerAvailable) {
            HariShopMod.LOGGER.info("JERHelper: JER not found, using fallback pricing");
            return;
        }
        HariShopMod.LOGGER.info("JERHelper: Initializing JER integration...");
        long start = System.currentTimeMillis();
        try {
            JERHelper.loadMobDrops();
            JERHelper.loadWorldGenData();
            JERHelper.loadDungeonLoot();
            HariShopMod.LOGGER.info("JERHelper: Loaded {} mob drops, {} world gen, {} dungeon in {}ms", (Object)mobDropRates.size(), (Object)worldGenLevels.size(), (Object)dungeonDropRates.size(), (Object)(System.currentTimeMillis() - start));
        }
        catch (Exception e) {
            HariShopMod.LOGGER.warn("JERHelper: Failed to init", (Throwable)e);
            jerAvailable = false;
        }
    }

    private static void loadMobDrops() {
        try {
            Class<?> mobRegistryClass = Class.forName("jeresources.registry.MobRegistry");
            Method getInstance = mobRegistryClass.getMethod("getInstance", new Class[0]);
            Method getMobs = mobRegistryClass.getMethod("getMobs", new Class[0]);
            Object registry = getInstance.invoke(null, new Object[0]);
            List mobs = (List)getMobs.invoke(registry, new Object[0]);
            Class<?> mobEntryClass = Class.forName("jeresources.entry.MobEntry");
            Method getMobName = mobEntryClass.getMethod("getMobName", new Class[0]);
            Method getDrops = mobEntryClass.getMethod("getDrops", new Class[0]);
            Class<?> lootDropClass = Class.forName("jeresources.api.drop.LootDrop");
            Field itemField = lootDropClass.getField("item");
            Field chanceField = lootDropClass.getField("chance");
            for (Object mobEntry : mobs) {
                String mobName = (String)getMobName.invoke(mobEntry, new Object[0]);
                List drops = (List)getDrops.invoke(mobEntry, new Object[0]);
                for (Object drop : drops) {
                    ItemStack stack = (ItemStack)itemField.get(drop);
                    float chance = chanceField.getFloat(drop);
                    if (stack == null || stack.isEmpty()) continue;
                    Item item = stack.getItem();
                    Float existing = mobDropRates.get(item);
                    if (existing == null || chance > existing.floatValue()) {
                        mobDropRates.put(item, Float.valueOf(chance));
                        mobDropSources.put(item, mobName);
                    }
                    obtainableItems.add(item);
                }
            }
            HariShopMod.LOGGER.debug("JERHelper: {} mob drops loaded", (Object)mobDropRates.size());
        }
        catch (Exception e) {
            HariShopMod.LOGGER.debug("JERHelper: Could not load mob drops", (Throwable)e);
        }
    }

    private static void loadWorldGenData() {
        try {
            Class<?> worldGenRegistryClass = Class.forName("jeresources.registry.WorldGenRegistry");
            Method getInstance = worldGenRegistryClass.getMethod("getInstance", new Class[0]);
            Method getWorldGen = worldGenRegistryClass.getMethod("getWorldGen", new Class[0]);
            Object registry = getInstance.invoke(null, new Object[0]);
            List entries = (List)getWorldGen.invoke(registry, new Object[0]);
            Class<?> worldGenEntryClass = Class.forName("jeresources.entry.WorldGenEntry");
            Method getBlock = worldGenEntryClass.getMethod("getBlock", new Class[0]);
            Method getMinY = worldGenEntryClass.getMethod("getMinY", new Class[0]);
            Method getMaxY = worldGenEntryClass.getMethod("getMaxY", new Class[0]);
            Method getDropsMethod = worldGenEntryClass.getMethod("getDrops", new Class[0]);
            Method getDimension = worldGenEntryClass.getMethod("getDimension", new Class[0]);
            Method getBiomeRestrictions = worldGenEntryClass.getMethod("getBiomeRestrictions", new Class[0]);
            Method isSilkTouchNeeded = worldGenEntryClass.getMethod("isSilkTouchNeeded", new Class[0]);
            Method getAverageBlockCountPerChunk = worldGenEntryClass.getMethod("getAverageBlockCountPerChunk", new Class[0]);
            for (Object entry : entries) {
                ItemStack block = (ItemStack)getBlock.invoke(entry, new Object[0]);
                int minY = (Integer)getMinY.invoke(entry, new Object[0]);
                int maxY = (Integer)getMaxY.invoke(entry, new Object[0]);
                if (block == null || block.isEmpty()) continue;
                Item item = block.getItem();
                worldGenLevels.put(item, new Integer[]{minY, maxY});
                obtainableItems.add(item);
                try {
                    String dimension = (String)getDimension.invoke(entry, new Object[0]);
                    List biomes = (List)getBiomeRestrictions.invoke(entry, new Object[0]);
                    boolean silkTouch = (Boolean)isSilkTouchNeeded.invoke(entry, new Object[0]);
                    float blocksPerChunk = ((Float)getAverageBlockCountPerChunk.invoke(entry, new Object[0])).floatValue();
                    worldGenDimensions.put(item, dimension);
                    if (biomes != null && !biomes.isEmpty()) {
                        worldGenBiomes.put(item, biomes);
                    }
                    worldGenSilkTouch.put(item, silkTouch);
                    worldGenBlocksPerChunk.put(item, Float.valueOf(blocksPerChunk));
                }
                catch (Exception dimension) {
                    // empty catch block
                }
                List drops = (List)getDropsMethod.invoke(entry, new Object[0]);
                for (Object dropStack : drops) {
                    ItemStack drop = (ItemStack)dropStack;
                    if (drop.isEmpty()) continue;
                    obtainableItems.add(drop.getItem());
                }
            }
            HariShopMod.LOGGER.debug("JERHelper: {} world gen entries loaded", (Object)worldGenLevels.size());
        }
        catch (Exception e) {
            HariShopMod.LOGGER.debug("JERHelper: Could not load world gen", (Throwable)e);
        }
    }

    private static void loadDungeonLoot() {
        try {
            Class<?> dungeonRegistryClass = Class.forName("jeresources.registry.DungeonRegistry");
            Method getInstance = dungeonRegistryClass.getMethod("getInstance", new Class[0]);
            Method getDungeons = dungeonRegistryClass.getMethod("getDungeons", new Class[0]);
            Object registry = getInstance.invoke(null, new Object[0]);
            List dungeons = (List)getDungeons.invoke(registry, new Object[0]);
            Class<?> dungeonEntryClass = Class.forName("jeresources.entry.DungeonEntry");
            Method getName = dungeonEntryClass.getMethod("getName", new Class[0]);
            Method containsItem = dungeonEntryClass.getMethod("containsItem", ItemStack.class);
            Method getChestDrop = dungeonEntryClass.getMethod("getChestDrop", ItemStack.class);
            Class<?> lootDropClass = Class.forName("jeresources.api.drop.LootDrop");
            Field chanceField = lootDropClass.getField("chance");
            for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
                ItemStack stack = new ItemStack((ItemLike)item);
                for (Object dungeon : dungeons) {
                    boolean contains = (Boolean)containsItem.invoke(dungeon, stack);
                    if (!contains) continue;
                    String dungeonName = (String)getName.invoke(dungeon, new Object[0]);
                    Object drop = getChestDrop.invoke(dungeon, stack);
                    if (drop != null) {
                        float chance = chanceField.getFloat(drop);
                        Float existing = dungeonDropRates.get(item);
                        if (existing == null || chance > existing.floatValue()) {
                            dungeonDropRates.put(item, Float.valueOf(chance));
                            dungeonSources.put(item, dungeonName);
                        }
                    }
                    obtainableItems.add(item);
                }
            }
            HariShopMod.LOGGER.debug("JERHelper: {} dungeon items loaded", (Object)dungeonDropRates.size());
        }
        catch (Exception e) {
            HariShopMod.LOGGER.debug("JERHelper: Could not load dungeon loot", (Throwable)e);
        }
    }

    public static boolean isAvailable() {
        JERHelper.init();
        return jerAvailable;
    }

    public static float getMobDropRate(Item item) {
        JERHelper.init();
        return mobDropRates.getOrDefault(item, Float.valueOf(-1.0f)).floatValue();
    }

    public static String getMobDropSource(Item item) {
        JERHelper.init();
        return mobDropSources.get(item);
    }

    public static Integer[] getWorldGenLevels(Item item) {
        JERHelper.init();
        return worldGenLevels.get(item);
    }

    public static float getDungeonDropRate(Item item) {
        JERHelper.init();
        return dungeonDropRates.getOrDefault(item, Float.valueOf(-1.0f)).floatValue();
    }

    public static String getDungeonSource(Item item) {
        JERHelper.init();
        return dungeonSources.get(item);
    }

    public static boolean isObtainable(Item item) {
        JERHelper.init();
        if (jerAvailable) {
            return obtainableItems.contains(item);
        }
        return !JERHelper.isCreativeOnly(item);
    }

    public static boolean isCreativeOnly(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String name = id.getPath().toLowerCase();
        if (name.endsWith("_spawn_egg")) {
            return true;
        }
        if (name.contains("command_block")) {
            return true;
        }
        if (name.equals("structure_block") || name.equals("structure_void")) {
            return true;
        }
        if (name.equals("debug_stick")) {
            return true;
        }
        if (name.equals("barrier") || name.equals("light")) {
            return true;
        }
        if (name.equals("jigsaw")) {
            return true;
        }
        return name.equals("knowledge_book");
    }

    public static long calculatePriceFromJER(Item item) {
        JERHelper.init();
        if (!jerAvailable) {
            return -1L;
        }
        float mobRate = JERHelper.getMobDropRate(item);
        if (mobRate > 0.0f) {
            long basePrice = (long)(100.0f / mobRate);
            String mobSource = JERHelper.getMobDropSource(item);
            double mobMult = JERHelper.getMobDifficultyMultiplier(mobSource);
            return (long)((double)basePrice * mobMult);
        }
        float dungeonRate = JERHelper.getDungeonDropRate(item);
        if (dungeonRate > 0.0f) {
            return (long)(500.0f / dungeonRate);
        }
        long worldGenPrice = JERHelper.calculateEnhancedWorldGenPrice(item);
        if (worldGenPrice > 0L) {
            return worldGenPrice;
        }
        return -1L;
    }

    private static double getMobDifficultyMultiplier(String mobName) {
        if (mobName == null) {
            return 1.0;
        }
        String name = mobName.toLowerCase();
        if (name.contains("wither") && !name.contains("skeleton")) {
            return 100.0;
        }
        if (name.contains("dragon")) {
            return 200.0;
        }
        if (name.contains("elder_guardian")) {
            return 50.0;
        }
        if (name.contains("warden")) {
            return 80.0;
        }
        if (name.contains("blaze")) {
            return 5.0;
        }
        if (name.contains("ghast")) {
            return 8.0;
        }
        if (name.contains("enderman")) {
            return 3.0;
        }
        if (name.contains("shulker")) {
            return 10.0;
        }
        if (name.contains("wither_skeleton")) {
            return 15.0;
        }
        if (name.contains("creeper")) {
            return 2.0;
        }
        if (name.contains("witch")) {
            return 3.0;
        }
        return 1.5;
    }

    public static double getDropRatePriceMultiplier(Item item) {
        float dungeonRate;
        float mobRate = JERHelper.getMobDropRate(item);
        float bestRate = Math.max(mobRate, dungeonRate = JERHelper.getDungeonDropRate(item));
        if (bestRate <= 0.0f) {
            return 1.0;
        }
        return Math.min(100.0, 1.0 / (double)bestRate);
    }

    public static double getDepthPriceMultiplier(Item item) {
        Integer[] levels = JERHelper.getWorldGenLevels(item);
        if (levels == null) {
            return 1.0;
        }
        int avgY = (levels[0] + levels[1]) / 2;
        if (avgY >= 64) {
            return 1.0;
        }
        if (avgY >= 0) {
            return 1.5;
        }
        if (avgY >= -30) {
            return 2.5;
        }
        return 4.0;
    }

    public static String getWorldGenDimension(Item item) {
        JERHelper.init();
        return worldGenDimensions.get(item);
    }

    public static List<String> getWorldGenBiomes(Item item) {
        JERHelper.init();
        return worldGenBiomes.get(item);
    }

    public static boolean needsSilkTouch(Item item) {
        JERHelper.init();
        return worldGenSilkTouch.getOrDefault(item, false);
    }

    public static float getBlocksPerChunk(Item item) {
        JERHelper.init();
        return worldGenBlocksPerChunk.getOrDefault(item, Float.valueOf(-1.0f)).floatValue();
    }

    public static double getDimensionMultiplier(Item item) {
        String dimension = JERHelper.getWorldGenDimension(item);
        if (dimension == null) {
            return 1.0;
        }
        if ((dimension = dimension.toLowerCase()).contains("nether")) {
            return 3.0;
        }
        if (dimension.contains("end")) {
            return 5.0;
        }
        return 1.0;
    }

    public static double getRarityMultiplier(Item item) {
        float blocksPerChunk = JERHelper.getBlocksPerChunk(item);
        if (blocksPerChunk <= 0.0f) {
            return 1.0;
        }
        if (blocksPerChunk > 20.0f) {
            return 0.5;
        }
        if (blocksPerChunk > 10.0f) {
            return 1.0;
        }
        if (blocksPerChunk > 5.0f) {
            return 2.0;
        }
        if (blocksPerChunk > 1.0f) {
            return 5.0;
        }
        return 10.0;
    }

    public static long calculateEnhancedWorldGenPrice(Item item) {
        Integer[] levels = JERHelper.getWorldGenLevels(item);
        if (levels == null) {
            return -1L;
        }
        int avgY = (levels[0] + levels[1]) / 2;
        long basePrice = avgY < -30 ? 3000L : (avgY < 0 ? 500L : (avgY < 30 ? 100L : 50L));
        double dimMult = JERHelper.getDimensionMultiplier(item);
        double rarityMult = JERHelper.getRarityMultiplier(item);
        double silkMult = JERHelper.needsSilkTouch(item) ? 1.5 : 1.0;
        return (long)((double)basePrice * dimMult * rarityMult * silkMult);
    }
}

