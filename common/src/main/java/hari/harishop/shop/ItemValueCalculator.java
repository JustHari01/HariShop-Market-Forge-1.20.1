/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.food.FoodProperties
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.CrossbowItem
 *  net.minecraft.world.item.DiggerItem
 *  net.minecraft.world.item.EnchantedBookItem
 *  net.minecraft.world.item.FishingRodItem
 *  net.minecraft.world.item.FlintAndSteelItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.PotionItem
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.item.ShearsItem
 *  net.minecraft.world.item.SpawnEggItem
 *  net.minecraft.world.item.SwordItem
 *  net.minecraft.world.item.Tier
 *  net.minecraft.world.item.TieredItem
 *  net.minecraft.world.item.TippedArrowItem
 *  net.minecraft.world.item.TridentItem
 *  net.minecraft.world.item.alchemy.PotionUtils
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import hari.harishop.config.ShopConfig;
import hari.harishop.config.TagPricing;
import hari.harishop.integration.JERHelper;
import hari.harishop.integration.EconomicItemGuardBridge;
import hari.harishop.shop.BaseItemValues;
import hari.harishop.shop.BreedingAnalyzer;
import hari.harishop.shop.EconomyBalancer;
import hari.harishop.shop.LootTableAnalyzer;
import hari.harishop.shop.ModPricingConfig;
import hari.harishop.shop.OreGenAnalyzer;
import hari.harishop.shop.RarityAnalyzer;
import hari.harishop.shop.RecipeAnalyzer;
import hari.harishop.shop.VillagerTradeAnalyzer;
import hari.harishop.v5.MoneyMath;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;

public class ItemValueCalculator {
    private static final int BASE_FOOD = 10;
    private static final int BASE_WEAPON = 200;
    private static final int BASE_ARMOR = 150;
    private static final int BASE_TOOL = 100;
    private static final int BASE_POTION = 50;
    private static final int BASE_BLOCK = 5;
    private static final int BASE_OTHER = 20;
    private static final double COMMON_MULT = 1.0;
    private static final double UNCOMMON_MULT = 1.5;
    private static final double RARE_MULT = 3.0;
    private static final double EPIC_MULT = 6.0;

    public static long getBuyPrice(Item item) {
        long basePrice = ItemValueCalculator.calculatePrice(item);
        return EconomyBalancer.applyShopFluctuation(item, basePrice);
    }

    /** Variant-aware buy quote used by persisted trades and packet validation. */
    public static long getBuyPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        long basePrice = ItemValueCalculator.calculatePrice(stack);
        return basePrice <= 0L ? 0L : EconomyBalancer.applyShopFluctuation(stack.getItem(), basePrice);
    }

    public static long getDailyDealPrice(Item item) {
        long basePrice = ItemValueCalculator.calculatePrice(item);
        return EconomyBalancer.applyDailyDealFluctuation(item, basePrice);
    }

    public static long getSellPrice(Item item) {
        long buyPrice = ItemValueCalculator.getBuyPrice(item);
        return EconomyBalancer.getSellPrice(buyPrice);
    }

    public static long getSellPrice(ItemStack stack) {
        long buyPrice = ItemValueCalculator.getBuyPrice(stack);
        return buyPrice <= 0L ? 0L : EconomyBalancer.getSellPrice(buyPrice);
    }

    public static long getBasePriceWithoutFluctuation(Item item) {
        return ItemValueCalculator.calculatePrice(item);
    }

    public static long getBasePriceWithoutFluctuation(ItemStack stack) {
        return ItemValueCalculator.calculatePrice(stack);
    }

    /**
     * Prices a concrete stack variant.  The old Forge API has no Data
     * Components, so the authoritative variant data is its NBT: enchantment
     * levels, potion effects and remaining durability.  Unknown custom tags
     * are deliberately not guessed; they retain the base item quote and are
     * kept distinct by ShopTrade's stack identity.
     */
    public static long calculatePrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        // Creative/admin provenance is a hard economic boundary.  It must be
        // checked before any heuristic or recipe price can turn a quarantined
        // stack into a positive pawn payout.
        if (EconomicItemGuardBridge.isNonEconomic(stack)) return 0L;
        if (hasUnsupportedVariantData(stack)) return 0L;
        Item item = stack.getItem();
        long base = calculatePrice(item);
        if (base <= 0L) return 0L;
        try {
            if (item instanceof EnchantedBookItem) {
                long book = calculateEnchantedBookPrice(stack);
                return applyModMultiplier(item, book);
            }
            long priced = base;
            if (item instanceof PotionItem || item instanceof TippedArrowItem) {
                double potion = getPotionMultiplier(stack);
                priced = MoneyMath.rounded(base, java.math.BigDecimal.valueOf(potion), java.math.RoundingMode.CEILING);
            }
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            if (!enchants.isEmpty() && !(item instanceof EnchantedBookItem)) {
                BigInteger premium = BigInteger.ZERO;
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    int level = entry.getValue() == null ? 0 : entry.getValue();
                    if (level <= 0 || level > 255 || entry.getKey() == null) return 0L;
                    long unit = enchantUnit(entry.getKey());
                    premium = premium.add(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(level)));
                }
                priced = MoneyMath.checked(BigInteger.valueOf(priced).add(premium));
            }
            int maxDamage = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            if (maxDamage > 0 && (damage < 0 || damage > maxDamage)) {
                return 0L;
            }
            if (maxDamage > 0 && damage > 0) {
                long remaining = Math.max(0L, (long) maxDamage - damage);
                priced = MoneyMath.floorRatio(Math.max(1L, priced), remaining, maxDamage);
                priced = Math.max(1L, priced);
            }
            // calculatePrice(item) already includes the configured namespace
            // multiplier; applying it again would inflate variants twice.
            return MoneyMath.nonNegative(priced, "variant price");
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            // A malformed variant is not a free item and never becomes a
            // positive pawn payout through a wrapped number.
            return 0L;
        }
    }

    /**
     * Forge 1.20.1 stores item variants in NBT.  Only fields whose economic
     * meaning is understood by this calculator are accepted; container,
     * capability and custom-mod payloads fail closed instead of receiving a
     * guessed pawn value.  Cosmetic display/model fields do not affect value.
     */
    private static boolean hasUnsupportedVariantData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) {
            return false;
        }
        for (String key : tag.getAllKeys()) {
            if ("Damage".equals(key)
                    || "Enchantments".equals(key)
                    || "StoredEnchantments".equals(key)
                    || "Potion".equals(key)
                    || "CustomPotionEffects".equals(key)
                    || "CustomPotionColor".equals(key)
                    || "RepairCost".equals(key)
                    || "Trim".equals(key)
                    || "display".equals(key)
                    || "CustomModelData".equals(key)
                    || "HideFlags".equals(key)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static long applyModMultiplier(Item item, long value) {
        if (value <= 0L) return 0L;
        double multiplier = ShopConfig.get().getModMultiplier(BuiltInRegistries.ITEM.getKey(item).getNamespace());
        try {
            return MoneyMath.rounded(value, java.math.BigDecimal.valueOf(multiplier), java.math.RoundingMode.CEILING);
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return 0L;
        }
    }

    private static long enchantUnit(Enchantment enchantment) {
        if (enchantment.isCurse()) return 0L;
        if (enchantment.isTreasureOnly()) return 5000L;
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        String key = id == null ? "" : id.toString();
        if (key.contains("mending")) return 50000L;
        if (key.contains("infinity")) return 20000L;
        return switch (enchantment.getRarity()) {
            case COMMON -> 1500L;
            case UNCOMMON -> 3000L;
            case RARE -> 6000L;
            case VERY_RARE -> 15000L;
        };
    }

    public static long calculatePrice(Item item) {
        if (item == null) {
            return 0L;
        }
        double enchantMultiplier;
        double durabilityMultiplier;
        double rarityMultiplier;
        double statMultiplier;
        LootTableAnalyzer.ItemSource source;
        SpawnEggItem egg;
        long breedingPrice;
        long jerPrice;
        long recipePrice;
        OreGenAnalyzer.OreData oreData;
        double oreMult;
        long orePrice;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return 20L;
        }
        String itemIdStr = itemId.toString();
        String modId = itemId.getNamespace();
        double modMult = ShopConfig.get().getModMultiplier(modId);
        long customPrice = ShopConfig.get().getCustomPrice(itemIdStr);
        if (customPrice > 0L) {
            return customPrice;
        }
        if (OreGenAnalyzer.hasOreData(item) && (orePrice = (long)(100.0 * (oreMult = (oreData = OreGenAnalyzer.getOreData(item)).getPriceMultiplier()) * modMult)) > 50L) {
            return Math.max(50L, orePrice);
        }
        long villagerPrice = VillagerTradeAnalyzer.getSuggestedPrice(item);
        if (villagerPrice > 0L) {
            return Math.max(1L, (long)((double)villagerPrice * modMult));
        }
        if (RecipeAnalyzer.isInitialized() && (recipePrice = RecipeAnalyzer.calculatePrice(item)) > 0L) {
            double modPricingMult = ModPricingConfig.getModMultiplier(item);
            return Math.max(1L, (long)((double)recipePrice * modMult * modPricingMult));
        }
        long tagPrice = TagPricing.getPriceByTags(item);
        if (tagPrice > 0L) {
            double oreMult2 = OreGenAnalyzer.getOrePriceMultiplier(item);
            return Math.max(1L, (long)((double)tagPrice * modMult * oreMult2));
        }
        if (JERHelper.isAvailable() && (jerPrice = JERHelper.calculatePriceFromJER(item)) > 0L) {
            return Math.max(50L, (long)((double)jerPrice * modMult));
        }
        if (item instanceof SpawnEggItem && (breedingPrice = BreedingAnalyzer.getSpawnEggPrice(egg = (SpawnEggItem)item)) > 0L) {
            return Math.max(100L, (long)((double)breedingPrice * modMult));
        }
        if (LootTableAnalyzer.isInitialized() && (source = LootTableAnalyzer.getItemSource(item)) != LootTableAnalyzer.ItemSource.CRAFTABLE && source != LootTableAnalyzer.ItemSource.UNKNOWN) {
            double dropMult = LootTableAnalyzer.getSimulatedDropMultiplier(item);
            if (dropMult <= 1.0) {
                dropMult = source.priceMultiplier;
            }
            long dropPrice = (long)(50.0 * dropMult * modMult);
            return Math.max(50L, dropPrice);
        }
        long baseValue = BaseItemValues.getBaseValue(item);
        if (baseValue > 0L) {
            return Math.max(1L, (long)((double)baseValue * modMult));
        }
        if (item instanceof EnchantedBookItem) {
            return ItemValueCalculator.calculateEnchantedBookPrice(new ItemStack((ItemLike)item));
        }
        ItemStack stack = new ItemStack((ItemLike)item);
        long basePrice = ItemValueCalculator.getBasePrice(item);
        long attrPrice = (long)((double)basePrice * (statMultiplier = ItemValueCalculator.getStatMultiplier(item, stack)) * (rarityMultiplier = ItemValueCalculator.getRarityMultiplier(item.getRarity(stack))) * (durabilityMultiplier = ItemValueCalculator.getDurabilityMultiplier(item)) * (enchantMultiplier = ItemValueCalculator.getEnchantabilityMultiplier(item)));
        if (attrPrice <= 10L) {
            attrPrice = ItemValueCalculator.applyNameHeuristics(item, itemIdStr, attrPrice);
        }
        if ((attrPrice = (long)((double)attrPrice * modMult)) < 100L && !modId.equals("minecraft")) {
            int rarityScore = RarityAnalyzer.analyzeRarity(item);
            double rarityMult = RarityAnalyzer.getPriceMultiplierFromRarity(rarityScore);
            attrPrice = Math.max(attrPrice, (long)(20.0 * rarityMult));
        }
        return Math.max(1L, attrPrice);
    }

    private static long calculateEnchantedBookPrice(ItemStack stack) {
        Map enchants = EnchantmentHelper.getEnchantments((ItemStack)stack);
        if (enchants.isEmpty()) {
            return 1000L;
        }
        BigInteger totalPrice = BigInteger.ZERO;
        for (Map.Entry<Enchantment, Integer> entry : (Set<Map.Entry<Enchantment, Integer>>)(Set<?>)enchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            
            long basePrice = 1000L;
            switch (enchant.getRarity()) {
                case COMMON:
                    basePrice = 1500L;
                    break;
                case UNCOMMON:
                    basePrice = 3000L;
                    break;
                case RARE:
                    basePrice = 6000L;
                    break;
                case VERY_RARE:
                    basePrice = 15000L;
                    break;
            }

            if (enchant.isTreasureOnly()) basePrice = Math.multiplyExact(basePrice, 2L);
            if (enchant.isCurse()) {
                totalPrice = totalPrice.subtract(BigInteger.valueOf(2000L)).max(BigInteger.valueOf(100L));
                continue;
            }
            
            String enchantId = BuiltInRegistries.ENCHANTMENT.getKey(enchant) != null ? BuiltInRegistries.ENCHANTMENT.getKey(enchant).toString() : "";
            if (enchantId.contains("mending")) {
                totalPrice = totalPrice.add(BigInteger.valueOf(50000L));
            } else if (enchantId.contains("infinity")) {
                totalPrice = totalPrice.add(BigInteger.valueOf(20000L));
            } else {
                if (level <= 0 || level > 255) return 0L;
                totalPrice = totalPrice.add(BigInteger.valueOf(basePrice).multiply(BigInteger.valueOf(level)));
            }
        }
        try {
            return Math.max(500L, MoneyMath.checked(totalPrice));
        } catch (ArithmeticException invalid) {
            return 0L;
        }
    }

    private static long applyNameHeuristics(Item item, String itemId, long currentPrice) {
        ItemStack stack;
        String displayName;
        String lowerName = itemId.toLowerCase();
        String combined = lowerName + " " + (displayName = (stack = new ItemStack((ItemLike)item)).getHoverName().getString().toLowerCase());
        if (combined.contains("netherite")) {
            return Math.max(currentPrice, 50000L);
        }
        if (combined.contains("dragon") || combined.contains("ender_dragon")) {
            return Math.max(currentPrice, 30000L);
        }
        if (combined.contains("wither")) {
            return Math.max(currentPrice, 20000L);
        }
        if (combined.contains("ancient") || combined.contains("relic")) {
            return Math.max(currentPrice, 10000L);
        }
        if (combined.contains("boss") || combined.contains("legendary")) {
            return Math.max(currentPrice, 8000L);
        }
        if (combined.contains("echo") || combined.contains("sculk")) {
            return Math.max(currentPrice, 5000L);
        }
        if (combined.contains("diamond")) {
            return Math.max(currentPrice, 5000L);
        }
        if (combined.contains("emerald")) {
            return Math.max(currentPrice, 3000L);
        }
        if (combined.contains("enchanted") || combined.contains("golden_apple")) {
            return Math.max(currentPrice, 10000L);
        }
        if (combined.contains("totem") || combined.contains("undying")) {
            return Math.max(currentPrice, 15000L);
        }
        if (combined.contains("elytra")) {
            return Math.max(currentPrice, 50000L);
        }
        if (combined.contains("beacon")) {
            return Math.max(currentPrice, 100000L);
        }
        if (combined.contains("conduit")) {
            return Math.max(currentPrice, 30000L);
        }
        if (combined.contains("trident")) {
            return Math.max(currentPrice, 20000L);
        }
        if (combined.contains("gold") || combined.contains("golden")) {
            return Math.max(currentPrice, 500L);
        }
        if (combined.contains("iron")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("copper")) {
            return Math.max(currentPrice, 50L);
        }
        if (combined.contains("lapis") || combined.contains("lazuli")) {
            return Math.max(currentPrice, 200L);
        }
        if (combined.contains("redstone")) {
            return Math.max(currentPrice, 150L);
        }
        if (combined.contains("quartz")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("amethyst")) {
            return Math.max(currentPrice, 150L);
        }
        if (combined.contains("prismarine")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("obsidian")) {
            return Math.max(currentPrice, 200L);
        }
        if (combined.contains("blaze")) {
            return Math.max(currentPrice, 300L);
        }
        if (combined.contains("ghast")) {
            return Math.max(currentPrice, 500L);
        }
        if (combined.contains("magma")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("nether")) {
            return Math.max(currentPrice, 80L);
        }
        if (combined.contains("end_")) {
            return Math.max(currentPrice, 150L);
        }
        if (combined.contains("ender")) {
            return Math.max(currentPrice, 500L);
        }
        if (combined.contains("shulker")) {
            return Math.max(currentPrice, 5000L);
        }
        if (combined.contains("ore")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("ingot")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("nugget")) {
            return Math.max(currentPrice, 15L);
        }
        if (combined.contains("block_of") || combined.contains("_block")) {
            return Math.max(currentPrice, 50L);
        }
        if (combined.contains("crystal")) {
            return Math.max(currentPrice, 200L);
        }
        if (combined.contains("gem")) {
            return Math.max(currentPrice, 300L);
        }
        if (combined.contains("pearl")) {
            return Math.max(currentPrice, 400L);
        }
        if (combined.contains("star")) {
            return Math.max(currentPrice, 1000L);
        }
        if (combined.contains("heart")) {
            return Math.max(currentPrice, 500L);
        }
        if (combined.contains("soul")) {
            return Math.max(currentPrice, 300L);
        }
        if (combined.contains("phantom")) {
            return Math.max(currentPrice, 200L);
        }
        if (combined.contains("coral")) {
            return Math.max(currentPrice, 100L);
        }
        if (combined.contains("sponge")) {
            return Math.max(currentPrice, 300L);
        }
        if (combined.contains("honeycomb") || combined.contains("honey")) {
            return Math.max(currentPrice, 80L);
        }
        if (combined.contains("slime")) {
            return Math.max(currentPrice, 50L);
        }
        return currentPrice;
    }

    private static long getBasePrice(Item item) {
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) {
            return 200L;
        }
        if (item instanceof ArmorItem) {
            return 150L;
        }
        if (item instanceof DiggerItem || item instanceof ShearsItem || item instanceof FlintAndSteelItem || item instanceof FishingRodItem) {
            return 100L;
        }
        if (item instanceof PotionItem || item instanceof TippedArrowItem) {
            return 50L;
        }
        if (item.isEdible()) {
            return 10L;
        }
        if (item instanceof BlockItem) {
            return 5L;
        }
        return 20L;
    }

    private static double getStatMultiplier(Item item, ItemStack stack) {
        FoodProperties food;
        double multiplier = 1.0;
        if (item instanceof SwordItem) {
            SwordItem sword = (SwordItem)item;
            float damage = sword.getDamage();
            multiplier = 1.0 + (double)damage / 4.0;
        }
        if (item instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem)item;
            int defense = armor.getDefense();
            double toughness = armor.getToughness();
            multiplier = 1.0 + (double)defense / 8.0 + toughness / 2.0;
        }
        if (item instanceof TieredItem) {
            TieredItem tiered = (TieredItem)item;
            Tier tier = tiered.getTier();
            float speed = tier.getSpeed();
            float damage = tier.getAttackDamageBonus();
            multiplier = 1.0 + (double)speed / 8.0 + (double)damage / 4.0;
        }
        if (item.isEdible() && (food = item.getFoodProperties()) != null) {
            int nutrition = food.getNutrition();
            float saturation = food.getSaturationModifier();
            multiplier = 1.0 + (double)nutrition / 8.0 + (double)(saturation * 2.0f);
            if (!food.getEffects().isEmpty()) {
                multiplier *= 2.0;
            }
        }
        if (item instanceof PotionItem) {
            multiplier = ItemValueCalculator.getPotionMultiplier(stack);
        }
        return multiplier;
    }

    private static double getPotionMultiplier(ItemStack stack) {
        List<MobEffectInstance> effects = PotionUtils.getMobEffects((ItemStack)stack);
        if (effects.isEmpty()) {
            return 1.0;
        }
        double totalValue = 0.0;
        for (MobEffectInstance effect : effects) {
            double durationValue = (double)effect.getDuration() / 1200.0;
            double amplifierValue = 1.0 + (double)effect.getAmplifier() * 1.5;
            double effectType = effect.getEffect().isBeneficial() ? 1.5 : 0.8;
            totalValue += durationValue * amplifierValue * effectType;
        }
        return Math.max(1.0, totalValue);
    }

    private static double getRarityMultiplier(Rarity rarity) {
        if (rarity == Rarity.EPIC) {
            return 6.0;
        }
        if (rarity == Rarity.RARE) {
            return 3.0;
        }
        if (rarity == Rarity.UNCOMMON) {
            return 1.5;
        }
        return 1.0;
    }

    private static double getDurabilityMultiplier(Item item) {
        int maxDamage = item.getMaxDamage();
        if (maxDamage <= 0) {
            return 1.0;
        }
        return 1.0 + Math.log10(maxDamage) / 3.0;
    }

    private static double getEnchantabilityMultiplier(Item item) {
        int enchant = item.getEnchantmentValue();
        if (enchant <= 0) {
            return 1.0;
        }
        return 1.0 + (double)enchant / 50.0;
    }

    public static String getPriceTier(long price) {
        if (price < 10L) {
            return "Dirt Cheap";
        }
        if (price < 50L) {
            return "Common";
        }
        if (price < 200L) {
            return "Uncommon";
        }
        if (price < 500L) {
            return "Rare";
        }
        if (price < 1000L) {
            return "Epic";
        }
        if (price < 5000L) {
            return "Legendary";
        }
        if (price < 50000L) {
            return "Mythic";
        }
        return "Priceless";
    }
}

