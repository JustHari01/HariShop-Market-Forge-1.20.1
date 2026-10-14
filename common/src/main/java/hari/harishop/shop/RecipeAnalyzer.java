/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.NonNullList
 *  net.minecraft.core.RegistryAccess$Frozen
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.BlastingRecipe
 *  net.minecraft.world.item.crafting.CampfireCookingRecipe
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeManager
 *  net.minecraft.world.item.crafting.ShapedRecipe
 *  net.minecraft.world.item.crafting.ShapelessRecipe
 *  net.minecraft.world.item.crafting.SmeltingRecipe
 *  net.minecraft.world.item.crafting.SmithingTransformRecipe
 *  net.minecraft.world.item.crafting.SmithingTrimRecipe
 *  net.minecraft.world.item.crafting.SmokingRecipe
 *  net.minecraft.world.item.crafting.StonecutterRecipe
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import hari.harishop.integration.JERHelper;
import hari.harishop.shop.BaseItemValues;
import hari.harishop.shop.LootTableAnalyzer;
import hari.harishop.shop.RarityAnalyzer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public class RecipeAnalyzer {
    private static final Map<Item, Long> priceCache = new ConcurrentHashMap<Item, Long>();
    private static final Set<Item> calculating = Collections.synchronizedSet(new HashSet());
    private static boolean initialized = false;
    private static MinecraftServer initializedServer;
    private static List<Recipe<?>> allRecipes = new ArrayList();
    /**
     * Recipe results in Forge 1.20.1 must be resolved with the live
     * RegistryAccess.  Passing null happens to work for a few vanilla recipe
     * implementations, but it throws for registry-backed recipes and aborts
     * the pre-calculation pass.  Keep the access captured for the lifetime of
     * the server recipe cache and use it consistently in every lookup.
     */
    private static RegistryAccess.Frozen registryAccess;
    private static long lastCacheRefresh = 0L;
    private static final long CACHE_REFRESH_INTERVAL = 21600000L;
    private static final long FUEL_COST_PER_SMELT = 10L;

    public static void clearCache() {
        priceCache.clear();
        lastCacheRefresh = System.currentTimeMillis();
        HariShopMod.LOGGER.info("RecipeAnalyzer: Price cache cleared");
    }

    private static void checkCacheRefresh() {
        if (System.currentTimeMillis() - lastCacheRefresh > 21600000L) {
            RecipeAnalyzer.clearCache();
        }
    }

    public static void init(MinecraftServer server) {
        if (server == null) {
            HariShopMod.LOGGER.warn("RecipeAnalyzer: server is null; skipping initialization");
            return;
        }
        if (initialized && initializedServer == server) {
            return;
        }
        // An integrated server can be stopped and recreated without unloading
        // the mod classes.  Do not retain the previous world's RegistryAccess,
        // recipe list or prices across that boundary.
        initialized = false;
        initializedServer = server;
        registryAccess = null;
        allRecipes = new ArrayList();
        priceCache.clear();
        calculating.clear();
        HariShopMod.LOGGER.info("RecipeAnalyzer: Loading recipes...");
        long start = System.currentTimeMillis();
        try {
            RecipeManager recipeManager = server.getRecipeManager();
            registryAccess = server.registryAccess();
            allRecipes = new ArrayList(recipeManager.getRecipes());
            HariShopMod.LOGGER.info("RecipeAnalyzer: Loaded {} recipes in {}ms", (Object)allRecipes.size(), (Object)(System.currentTimeMillis() - start));
            initialized = true;
            RecipeAnalyzer.precalculateCommonItems(registryAccess);
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("RecipeAnalyzer: Failed to load recipes", (Throwable)e);
        }
    }

    private static void precalculateCommonItems(RegistryAccess.Frozen access) {
        HariShopMod.LOGGER.info("RecipeAnalyzer: Pre-calculating common item prices...");
        long start = System.currentTimeMillis();
        if (access == null) {
            HariShopMod.LOGGER.warn("RecipeAnalyzer: RegistryAccess unavailable; deferring pre-calculation");
            return;
        }
        int count = 0;
        for (Recipe<?> recipe : allRecipes) {
            ItemStack result = recipe.getResultItem(access);
            if (result.isEmpty()) continue;
            RecipeAnalyzer.calculatePrice(result.getItem());
            if (++count < 500) continue;
            break;
        }
        HariShopMod.LOGGER.info("RecipeAnalyzer: Cached {} item prices in {}ms", (Object)priceCache.size(), (Object)(System.currentTimeMillis() - start));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static long calculatePrice(Item item) {
        RecipeAnalyzer.checkCacheRefresh();
        if (priceCache.containsKey(item)) {
            return priceCache.get(item);
        }
        long baseValue = BaseItemValues.getBaseValue(item);
        if (baseValue > 0L) {
            priceCache.put(item, baseValue);
            return baseValue;
        }
        if (calculating.contains(item)) {
            return -1L;
        }
        calculating.add(item);
        try {
            long price = RecipeAnalyzer.calculateFromRecipes(item);
            if (price > 0L) {
                priceCache.put(item, price);
                long l = price;
                return l;
            }
            long jerPrice = JERHelper.calculatePriceFromJER(item);
            if (jerPrice > 0L) {
                priceCache.put(item, jerPrice);
                long l = jerPrice;
                return l;
            }
            double sourceMultiplier = LootTableAnalyzer.getSourceMultiplier(item);
            int rarityScore = RarityAnalyzer.analyzeRarity(item);
            double rarityMult = RarityAnalyzer.getPriceMultiplierFromRarity(rarityScore);
            price = (long)(20.0 * rarityMult * sourceMultiplier);
            priceCache.put(item, price);
            long l = price;
            return l;
        }
        finally {
            calculating.remove(item);
        }
    }

    private static long calculateFromRecipes(Item item) {
        if (!initialized || allRecipes.isEmpty()) {
            return -1L;
        }
        List<Recipe<?>> itemRecipes = RecipeAnalyzer.findRecipesFor(item);
        if (itemRecipes.isEmpty()) {
            return -1L;
        }
        long cheapestPrice = Long.MAX_VALUE;
        for (Recipe<?> recipe : itemRecipes) {
            long recipePrice = RecipeAnalyzer.calculateRecipePrice(recipe);
            if (recipePrice <= 0L || recipePrice >= cheapestPrice) continue;
            cheapestPrice = recipePrice;
        }
        return cheapestPrice == Long.MAX_VALUE ? -1L : cheapestPrice;
    }

    private static List<Recipe<?>> findRecipesFor(Item item) {
        ArrayList recipes = new ArrayList();
        for (Recipe<?> recipe : allRecipes) {
            try {
                ItemStack result = recipe.getResultItem(registryAccess);
                if (result.isEmpty() || result.getItem() != item) continue;
                recipes.add(recipe);
            }
            catch (Exception exception) {}
        }
        return recipes;
    }

    private static long calculateRecipePrice(Recipe<?> recipe) {
        try {
            NonNullList<Ingredient> ingredients;
            ItemStack result = recipe.getResultItem(registryAccess);
            int resultCount = Math.max(1, result.getCount());
            long totalCost = 0L;
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shaped = (ShapedRecipe)recipe;
                ingredients = shaped.getIngredients();
            } else if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapeless = (ShapelessRecipe)recipe;
                ingredients = shapeless.getIngredients();
            } else if (recipe instanceof SmeltingRecipe || recipe instanceof BlastingRecipe || recipe instanceof SmokingRecipe || recipe instanceof CampfireCookingRecipe) {
                ingredients = recipe.getIngredients();
                totalCost += 10L;
            } else if (recipe instanceof StonecutterRecipe) {
                ingredients = recipe.getIngredients();
            } else {
                if (recipe instanceof SmithingTransformRecipe) {
                    SmithingTransformRecipe smithing = (SmithingTransformRecipe)recipe;
                    NonNullList<Ingredient> smithIngredients = smithing.getIngredients();
                    long totalSmithCost = 0L;
                    for (Ingredient ing : smithIngredients) {
                        if (ing.isEmpty()) continue;
                        long cost = RecipeAnalyzer.getIngredientCost(ing);
                        if (cost < 0L) {
                            return -1L;
                        }
                        totalSmithCost += cost;
                    }
                    return totalSmithCost > 0L ? totalSmithCost : -1L;
                }
                if (recipe instanceof SmithingTrimRecipe) {
                    return -1L;
                }
                ingredients = recipe.getIngredients();
                if (ingredients == null || ingredients.isEmpty()) {
                    return -1L; // Extremely custom recipe, fail gracefully
                }
            }
            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) continue;
                long ingredientCost = RecipeAnalyzer.getIngredientCost(ingredient);
                if (ingredientCost < 0L) {
                    return -1L;
                }
                totalCost += ingredientCost;
            }
            return totalCost / (long)resultCount;
        }
        catch (Exception e) {
            return -1L;
        }
    }

    private static long getIngredientCost(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return 0L;
        }
        long cheapest = Long.MAX_VALUE;
        for (ItemStack stack : stacks) {
            long price;
            if (stack.isEmpty() || (price = RecipeAnalyzer.calculatePrice(stack.getItem())) <= 0L || price >= cheapest) continue;
            cheapest = price;
        }
        return cheapest == Long.MAX_VALUE ? -1L : cheapest;
    }

    public static boolean hasRecipe(Item item) {
        if (!initialized) {
            return false;
        }
        return !RecipeAnalyzer.findRecipesFor(item).isEmpty();
    }

    public static int getRecipeCount() {
        return allRecipes.size();
    }

    public static int getCacheSize() {
        return priceCache.size();
    }

    public static void recalculateAll() {
        priceCache.clear();
        RecipeAnalyzer.precalculateCommonItems(registryAccess);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

