/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.SpawnEggItem
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeManager
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import hari.harishop.shop.ItemValueCalculator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

public class BreedingAnalyzer {
    private static final Map<EntityType<?>, Long> entityBreedingCosts = new ConcurrentHashMap();
    private static boolean initialized = false;
    private static final Map<EntityType<?>, Item[]> VANILLA_BREEDING_ITEMS = new HashMap();

    public static void init(MinecraftServer server) {
        if (initialized) {
            return;
        }
        long start = System.currentTimeMillis();
        HariShopMod.LOGGER.info("BreedingAnalyzer: Analyzing breeding costs...");
        try {
            BreedingAnalyzer.analyzeRecipes(server.getRecipeManager());
            if (entityBreedingCosts.size() < 5) {
                BreedingAnalyzer.loadVanillaFallbacks();
            }
            initialized = true;
            HariShopMod.LOGGER.info("BreedingAnalyzer: Analyzed {} breeding entries in {}ms", (Object)entityBreedingCosts.size(), (Object)(System.currentTimeMillis() - start));
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("BreedingAnalyzer: Failed to analyze", (Throwable)e);
        }
    }

    private static void analyzeRecipes(RecipeManager recipeManager) {
        for (Recipe recipe : recipeManager.getRecipes()) {
            try {
                ResourceLocation id = recipe.getId();
                String path = id.getPath();
                if (!path.contains("breeding") && !id.getNamespace().equals("justenoughbreeding")) continue;
            }
            catch (Exception exception) {}
        }
    }

    private static void loadVanillaFallbacks() {
        for (Map.Entry<EntityType<?>, Item[]> entry : VANILLA_BREEDING_ITEMS.entrySet()) {
            EntityType<?> entity = entry.getKey();
            Item[] foods = entry.getValue();
            long minFoodPrice = Long.MAX_VALUE;
            boolean valid = false;
            for (Item food : foods) {
                long price = ItemValueCalculator.calculatePrice(food);
                if (price <= 0L || price >= minFoodPrice) continue;
                minFoodPrice = price;
                valid = true;
            }
            if (!valid) continue;
            entityBreedingCosts.put(entity, minFoodPrice * 2L);
        }
    }

    public static long getBreedingCost(EntityType<?> entity) {
        return entityBreedingCosts.getOrDefault(entity, 0L);
    }

    public static long getSpawnEggPrice(SpawnEggItem egg) {
        try {
            EntityType type = egg.getType(new CompoundTag());
            if (type == null) {
                return 0L;
            }
            long breedingCost = BreedingAnalyzer.getBreedingCost(type);
            if (breedingCost > 0L) {
                return breedingCost * 5L;
            }
        }
        catch (Exception e) {
            HariShopMod.LOGGER.debug("BreedingAnalyzer: Failed to get spawn egg type", (Throwable)e);
        }
        return 0L;
    }

    static {
        VANILLA_BREEDING_ITEMS.put(EntityType.COW, new Item[]{Items.WHEAT});
        VANILLA_BREEDING_ITEMS.put(EntityType.SHEEP, new Item[]{Items.WHEAT});
        VANILLA_BREEDING_ITEMS.put(EntityType.CHICKEN, new Item[]{Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS});
        VANILLA_BREEDING_ITEMS.put(EntityType.PIG, new Item[]{Items.CARROT, Items.POTATO, Items.BEETROOT});
        VANILLA_BREEDING_ITEMS.put(EntityType.HORSE, new Item[]{Items.GOLDEN_CARROT, Items.GOLDEN_APPLE});
        VANILLA_BREEDING_ITEMS.put(EntityType.WOLF, new Item[]{Items.BONE, Items.PORKCHOP, Items.BEEF});
        VANILLA_BREEDING_ITEMS.put(EntityType.CAT, new Item[]{Items.COD, Items.SALMON});
        VANILLA_BREEDING_ITEMS.put(EntityType.OCELOT, new Item[]{Items.COD, Items.SALMON});
        VANILLA_BREEDING_ITEMS.put(EntityType.RABBIT, new Item[]{Items.CARROT, Items.DANDELION});
        VANILLA_BREEDING_ITEMS.put(EntityType.LLAMA, new Item[]{Items.HAY_BLOCK});
        VANILLA_BREEDING_ITEMS.put(EntityType.TURTLE, new Item[]{Items.SEAGRASS});
        VANILLA_BREEDING_ITEMS.put(EntityType.PANDA, new Item[]{Items.BAMBOO});
        VANILLA_BREEDING_ITEMS.put(EntityType.FOX, new Item[]{Items.SWEET_BERRIES});
        VANILLA_BREEDING_ITEMS.put(EntityType.BEE, new Item[]{Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID});
        VANILLA_BREEDING_ITEMS.put(EntityType.STRIDER, new Item[]{Items.WARPED_FUNGUS});
        VANILLA_BREEDING_ITEMS.put(EntityType.HOGLIN, new Item[]{Items.CRIMSON_FUNGUS});
    }
}

