/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Multimap
 *  com.mojang.datafixers.util.Pair
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectCategory
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.food.FoodProperties
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.CrossbowItem
 *  net.minecraft.world.item.EnchantedBookItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.PotionItem
 *  net.minecraft.world.item.Rarity
 *  net.minecraft.world.item.ShieldItem
 *  net.minecraft.world.item.SpawnEggItem
 *  net.minecraft.world.item.SwordItem
 *  net.minecraft.world.item.Tier
 *  net.minecraft.world.item.TieredItem
 *  net.minecraft.world.item.TridentItem
 *  net.minecraft.world.item.alchemy.PotionUtils
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import hari.harishop.shop.ModPricingConfig;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

public class RarityAnalyzer {
    public static int analyzeRarity(Item item) {
        int rarityScore = 0;
        ItemStack stack = new ItemStack((ItemLike)item);
        rarityScore += RarityAnalyzer.getRarityScore(item.getRarity(stack));
        rarityScore += RarityAnalyzer.getAttributeScore(item, stack);
        rarityScore += RarityAnalyzer.getToolTierScore(item);
        rarityScore += RarityAnalyzer.getEnchantmentScore(item, stack);
        rarityScore += RarityAnalyzer.getFoodEffectScore(item);
        rarityScore += RarityAnalyzer.getPotionEffectScore(item, stack);
        rarityScore += RarityAnalyzer.getDurabilityScore(item);
        rarityScore += RarityAnalyzer.getKeywordScore(item);
        rarityScore += RarityAnalyzer.getEnchantabilityScore(item);
        rarityScore += RarityAnalyzer.getCreativeTabScore(item, stack);
        rarityScore += RarityAnalyzer.getBlockScore(item);
        rarityScore += RarityAnalyzer.getOreDifficultyScore(item);
        rarityScore += RarityAnalyzer.getMobDropScore(item);
        return Math.min(100, Math.max(0, rarityScore += ModPricingConfig.getKeywordScore(item)));
    }

    public static double getPriceMultiplierFromRarity(int rarityScore) {
        if (rarityScore <= 10) {
            return 1.0;
        }
        if (rarityScore <= 30) {
            return 2.5;
        }
        if (rarityScore <= 50) {
            return 7.0;
        }
        if (rarityScore <= 70) {
            return 20.0;
        }
        if (rarityScore <= 90) {
            return 100.0;
        }
        return 500.0;
    }

    private static int getRarityScore(Rarity rarity) {
        if (rarity == null) {
            return 5;
        }
        String name = rarity.name();
        if ("COMMON".equals(name)) {
            return 5;
        }
        if ("UNCOMMON".equals(name)) {
            return 20;
        }
        if ("RARE".equals(name)) {
            return 40;
        }
        if ("EPIC".equals(name)) {
            return 60;
        }
        return 70;
    }

    private static int getAttributeScore(Item item, ItemStack stack) {
        int score = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> mods = stack.getAttributeModifiers(slot);
            if (mods.containsKey(Attributes.ATTACK_DAMAGE)) {
                for (AttributeModifier mod : mods.get(Attributes.ATTACK_DAMAGE)) score += (int)(mod.getAmount() * 3.0);
            }
            if (mods.containsKey(Attributes.ARMOR)) {
                for (AttributeModifier mod : mods.get(Attributes.ARMOR)) score += (int)(mod.getAmount() * 2.0);
            }
            if (mods.containsKey(Attributes.ARMOR_TOUGHNESS)) {
                for (AttributeModifier mod : mods.get(Attributes.ARMOR_TOUGHNESS)) score += (int)(mod.getAmount() * 5.0);
            }
            if (mods.containsKey(Attributes.ATTACK_SPEED)) {
                for (AttributeModifier mod : mods.get(Attributes.ATTACK_SPEED)) {
                    if (mod.getAmount() > 0.0) score += (int)(mod.getAmount() * 5.0);
                }
            }
            if (mods.containsKey(Attributes.MAX_HEALTH)) {
                for (AttributeModifier mod : mods.get(Attributes.MAX_HEALTH)) score += (int)(mod.getAmount() * 3.0);
            }
            if (mods.containsKey(Attributes.MOVEMENT_SPEED)) {
                for (AttributeModifier mod : mods.get(Attributes.MOVEMENT_SPEED)) {
                    if (mod.getAmount() > 0.0) score += (int)(mod.getAmount() * 100.0);
                }
            }
            if (mods.containsKey(Attributes.KNOCKBACK_RESISTANCE)) {
                for (AttributeModifier mod : mods.get(Attributes.KNOCKBACK_RESISTANCE)) score += (int)(mod.getAmount() * 20.0);
            }
        }
        return Math.min(score, 40);
    }

    private static int getToolTierScore(Item item) {
        if (item instanceof TieredItem) {
            TieredItem tiered = (TieredItem)item;
            Tier tier = tiered.getTier();
            float damage = tier.getAttackDamageBonus();
            int durability = tier.getUses();
            if (durability > 2000) {
                return 30;
            }
            if (durability > 1500) {
                return 25;
            }
            if (durability > 500) {
                return 15;
            }
            if (durability > 200) {
                return 10;
            }
            if (durability > 50) {
                return 5;
            }
            return 0;
        }
        return 0;
    }

    private static int getEnchantmentScore(Item item, ItemStack stack) {
        if (!(item instanceof EnchantedBookItem)) {
            return 0;
        }
        Map enchants = EnchantmentHelper.getEnchantments((ItemStack)stack);
        int score = 0;
        for (Map.Entry<Enchantment, Integer> entry : (Set<Map.Entry<Enchantment, Integer>>)(Set<?>)enchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            if (enchant.isTreasureOnly()) {
                score += 30;
            }
            int maxLevel = enchant.getMaxLevel();
            score += level * 5;
            String name = enchant.getDescriptionId().toLowerCase();
            if (name.contains("mending")) {
                score += 40;
            }
            if (name.contains("infinity")) {
                score += 20;
            }
            if (!name.contains("silk_touch")) continue;
            score += 15;
        }
        return Math.min(score, 60);
    }

    private static int getFoodEffectScore(Item item) {
        if (!item.isEdible()) {
            return 0;
        }
        FoodProperties food = item.getFoodProperties();
        if (food == null) {
            return 0;
        }
        int score = 0;
        score += food.getNutrition() / 2;
        score += (int)(food.getSaturationModifier() * 10.0f);
        if (food.canAlwaysEat()) {
            score += 5;
        }
        if (food.isFastFood()) {
            score += 3;
        }
        for (Pair effect : food.getEffects()) {
            MobEffectInstance instance = (MobEffectInstance)effect.getFirst();
            MobEffect mobEffect = instance.getEffect();
            if (mobEffect.getCategory() == MobEffectCategory.BENEFICIAL) {
                score += 5 + instance.getAmplifier() * 3;
                score += instance.getDuration() / 600;
                continue;
            }
            if (mobEffect.getCategory() != MobEffectCategory.HARMFUL) continue;
            score -= 3;
        }
        return Math.max(0, Math.min(score, 30));
    }

    private static int getPotionEffectScore(Item item, ItemStack stack) {
        if (!(item instanceof PotionItem)) {
            return 0;
        }
        List effects = PotionUtils.getMobEffects((ItemStack)stack);
        int score = 0;
        for (MobEffectInstance effect : (List<MobEffectInstance>)effects) {
            MobEffect mobEffect = effect.getEffect();
            if (mobEffect.getCategory() != MobEffectCategory.BENEFICIAL) continue;
            score += 10;
            score += effect.getAmplifier() * 8;
            score += effect.getDuration() / 1200 * 2;
            String name = mobEffect.getDescriptionId().toLowerCase();
            if (name.contains("regeneration")) {
                score += 10;
            }
            if (name.contains("strength")) {
                score += 8;
            }
            if (name.contains("resistance")) {
                score += 8;
            }
            if (name.contains("speed")) {
                score += 5;
            }
            if (name.contains("invisibility")) {
                score += 10;
            }
            if (name.contains("fire_resistance")) {
                score += 8;
            }
            if (name.contains("water_breathing")) {
                score += 5;
            }
            if (!name.contains("night_vision")) continue;
            score += 3;
        }
        return Math.min(score, 50);
    }

    private static int getDurabilityScore(Item item) {
        int maxDamage = item.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }
        if (maxDamage >= 2000) {
            return 15;
        }
        if (maxDamage >= 1500) {
            return 10;
        }
        if (maxDamage >= 500) {
            return 5;
        }
        return 2;
    }

    private static int getKeywordScore(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        String name = id.toString().toLowerCase();
        int score = 0;
        if (name.contains("netherite")) {
            score += 30;
        }
        if (name.contains("dragon")) {
            score += 35;
        }
        if (name.contains("wither")) {
            score += 30;
        }
        if (name.contains("elytra")) {
            score += 40;
        }
        if (name.contains("beacon")) {
            score += 50;
        }
        if (name.contains("totem")) {
            score += 35;
        }
        if (name.contains("nether_star")) {
            score += 45;
        }
        if (name.contains("heart_of")) {
            score += 30;
        }
        if (name.contains("boss")) {
            score += 25;
        }
        if (name.contains("legendary")) {
            score += 30;
        }
        if (name.contains("mythic")) {
            score += 40;
        }
        if (name.contains("ancient")) {
            score += 20;
        }
        if (name.contains("ultimate")) {
            score += 25;
        }
        if (name.contains("diamond")) {
            score += 15;
        }
        if (name.contains("emerald")) {
            score += 12;
        }
        if (name.contains("echo_shard")) {
            score += 20;
        }
        if (name.contains("enchanted")) {
            score += 15;
        }
        if (name.contains("sculk")) {
            score += 10;
        }
        if (name.contains("end_")) {
            score += 8;
        }
        if (name.contains("shulker")) {
            score += 18;
        }
        if (name.contains("gold")) {
            score += 8;
        }
        if (name.contains("iron")) {
            score += 4;
        }
        if (name.contains("lapis")) {
            score += 5;
        }
        if (name.contains("redstone")) {
            score += 3;
        }
        return Math.min(score, 50);
    }

    private static int getEnchantabilityScore(Item item) {
        int enchant = item.getEnchantmentValue();
        if (enchant == 0) {
            return 0;
        }
        if (enchant >= 22) {
            return 2;
        }
        if (enchant >= 15) {
            return 5;
        }
        if (enchant >= 10) {
            return 10;
        }
        return 15;
    }

    private static int getCreativeTabScore(Item item, ItemStack stack) {
        if (item instanceof SwordItem || item instanceof AxeItem) {
            return 5;
        }
        if (item instanceof ArmorItem) {
            return 5;
        }
        if (item instanceof TridentItem || item instanceof CrossbowItem || item instanceof BowItem) {
            return 8;
        }
        if (item instanceof ShieldItem) {
            return 3;
        }
        if (item instanceof PotionItem) {
            return 5;
        }
        if (item instanceof EnchantedBookItem) {
            return 10;
        }
        if (item instanceof SpawnEggItem) {
            return 15;
        }
        return 0;
    }

    public static String getRarityTierName(int rarityScore) {
        if (rarityScore <= 10) {
            return "Common";
        }
        if (rarityScore <= 30) {
            return "Uncommon";
        }
        if (rarityScore <= 50) {
            return "Rare";
        }
        if (rarityScore <= 70) {
            return "Epic";
        }
        if (rarityScore <= 90) {
            return "Legendary";
        }
        return "Mythic";
    }

    public static String getRarityColor(int rarityScore) {
        if (rarityScore <= 10) {
            return "\u00a77";
        }
        if (rarityScore <= 30) {
            return "\u00a7f";
        }
        if (rarityScore <= 50) {
            return "\u00a79";
        }
        if (rarityScore <= 70) {
            return "\u00a75";
        }
        if (rarityScore <= 90) {
            return "\u00a76";
        }
        return "\u00a7c";
    }

    private static int getBlockScore(Item item) {
        if (!(item instanceof BlockItem)) {
            return 0;
        }
        BlockItem blockItem = (BlockItem)item;
        int score = 0;
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        float hardness = state.getDestroySpeed(null, null);
        if (hardness < 0.0f) {
            score += 50;
        } else if (hardness > 50.0f) {
            score += 25;
        } else if (hardness > 20.0f) {
            score += 15;
        } else if (hardness > 5.0f) {
            score += 8;
        } else if (hardness > 2.0f) {
            score += 3;
        }
        float resistance = block.getExplosionResistance();
        if (resistance > 1000.0f) {
            score += 20;
        } else if (resistance > 100.0f) {
            score += 10;
        } else if (resistance > 10.0f) {
            score += 3;
        }
        int light = state.getLightEmission();
        if (light > 10) {
            score += 10;
        } else if (light > 5) {
            score += 5;
        } else if (light > 0) {
            score += 2;
        }
        return Math.min(score, 40);
    }

    private static int getOreDifficultyScore(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        String name = id.toString().toLowerCase();
        int score = 0;
        if (name.contains("deepslate")) {
            score += 15;
            if (name.contains("diamond")) {
                score += 10;
            }
            if (name.contains("emerald")) {
                score += 8;
            }
            if (name.contains("gold")) {
                score += 5;
            }
            if (name.contains("lapis")) {
                score += 4;
            }
            if (name.contains("redstone")) {
                score += 3;
            }
        }
        if (name.contains("nether") || name.contains("crimson") || name.contains("warped")) {
            score += 12;
            if (name.contains("quartz")) {
                score += 3;
            }
            if (name.contains("gold")) {
                score += 5;
            }
            if (name.contains("ancient_debris")) {
                score += 40;
            }
            if (name.contains("netherite")) {
                score += 30;
            }
        }
        if (name.contains("basalt") || name.contains("blackstone")) {
            score += 8;
        }
        if (name.contains("end_stone") || name.contains("purpur") || name.contains("chorus")) {
            score += 18;
        }
        if (name.contains("prismarine") || name.contains("sea_lantern") || name.contains("conduit")) {
            score += 15;
        }
        if (name.contains("sculk")) {
            score += 20;
            if (name.contains("shrieker")) {
                score += 10;
            }
            if (name.contains("catalyst")) {
                score += 8;
            }
        }
        if (name.contains("amethyst") || name.contains("budding")) {
            score += 10;
        }
        if (name.contains("raw_") && (name.contains("iron") || name.contains("gold") || name.contains("copper"))) {
            score += 3;
        }
        if (name.contains("crying_obsidian")) {
            score += 12;
        }
        if (name.contains("gilded_blackstone")) {
            score += 15;
        }
        if (name.contains("lodestone")) {
            score += 18;
        }
        if (name.contains("respawn_anchor")) {
            score += 20;
        }
        return Math.min(score, 50);
    }

    private static int getMobDropScore(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        String name = id.toString().toLowerCase();
        int score = 0;
        if (name.contains("rotten_flesh") || name.contains("bone") || name.contains("string")) {
            score += 3;
        }
        if (name.contains("gunpowder") || name.contains("spider_eye")) {
            score += 5;
        }
        if (name.contains("slime_ball")) {
            score += 8;
        }
        if (name.contains("magma_cream")) {
            score += 10;
        }
        if (name.contains("ender_pearl")) {
            score += 15;
        }
        if (name.contains("blaze_rod") || name.contains("blaze_powder")) {
            score += 18;
        }
        if (name.contains("ghast_tear")) {
            score += 25;
        }
        if (name.contains("phantom_membrane")) {
            score += 15;
        }
        if (name.contains("shulker_shell")) {
            score += 30;
        }
        if (name.contains("wither_skeleton_skull") || name.contains("wither_skull")) {
            score += 35;
        }
        if (name.contains("nether_star")) {
            score += 50;
        }
        if (name.contains("dragon_egg") || name.contains("dragon_breath")) {
            score += 45;
        }
        if (name.contains("elytra")) {
            score += 50;
        }
        if (name.contains("totem_of_undying") || name.contains("totem")) {
            score += 40;
        }
        if (name.contains("trident")) {
            score += 35;
        }
        if (name.contains("nautilus_shell")) {
            score += 20;
        }
        if (name.contains("heart_of_the_sea")) {
            score += 35;
        }
        if (name.contains("_head") || name.contains("skull")) {
            score += 25;
            if (name.contains("dragon")) {
                score += 20;
            }
        }
        if (name.contains("beacon")) {
            score += 40;
        }
        if (name.contains("conduit")) {
            score += 35;
        }
        if (name.contains("end_crystal")) {
            score += 30;
        }
        if (name.contains("respawn_anchor")) {
            score += 25;
        }
        if (name.contains("lodestone")) {
            score += 25;
        }
        if (name.contains("enchanting_table")) {
            score += 20;
        }
        if (name.contains("anvil")) {
            score += 15;
        }
        if (name.contains("brewing_stand")) {
            score += 12;
        }
        if (name.contains("eye_of_ender") || name.contains("ender_eye")) {
            score += 20;
        }
        if (name.contains("firework_star")) {
            score += 8;
        }
        if (name.contains("golden_apple")) {
            score += 15;
            if (name.contains("enchanted")) {
                score += 30;
            }
        }
        if (name.contains("netherite_ingot")) {
            score += 35;
        }
        if (name.contains("netherite_scrap")) {
            score += 25;
        }
        if (name.contains("wither_rose")) {
            score += 30;
        }
        return Math.min(score, 50);
    }
}

