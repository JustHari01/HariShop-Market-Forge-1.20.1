/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModPricingConfig {
    private static final Map<String, Double> MOD_MULTIPLIERS = new HashMap<String, Double>();
    private static final Map<String, Set<String>> MOD_KEYWORDS = new HashMap<String, Set<String>>();
    private static final Set<String> BOSS_KEYWORDS = new HashSet<String>();
    private static final Set<String> DIMENSION_KEYWORDS = new HashSet<String>();

    private static void initModMultipliers() {
        MOD_MULTIPLIERS.put("alexscaves", 2.5);
        MOD_MULTIPLIERS.put("ad_astra", 3.0);
        MOD_MULTIPLIERS.put("regions_unexplored", 1.5);
        MOD_MULTIPLIERS.put("aquamirae", 2.0);
        MOD_MULTIPLIERS.put("deep_dark_regrowth", 2.0);
        MOD_MULTIPLIERS.put("luminous_nether", 1.8);
        MOD_MULTIPLIERS.put("luminousworld", 1.8);
        MOD_MULTIPLIERS.put("cataclysm", 4.0);
        MOD_MULTIPLIERS.put("bosses_of_mass_destruction", 3.5);
        MOD_MULTIPLIERS.put("mowziesmobs", 2.5);
        MOD_MULTIPLIERS.put("alexsmobs", 2.0);
        MOD_MULTIPLIERS.put("born_in_chaos_v1", 2.5);
        MOD_MULTIPLIERS.put("rottencreatures", 1.5);
        MOD_MULTIPLIERS.put("creeperoverhaul", 1.3);
        MOD_MULTIPLIERS.put("endermanoverhaul", 1.5);
        MOD_MULTIPLIERS.put("illageandspillage", 2.0);
        MOD_MULTIPLIERS.put("cave_dweller", 2.5);
        MOD_MULTIPLIERS.put("aquaculture", 1.5);
        MOD_MULTIPLIERS.put("hybrid_aquatic", 1.8);
        MOD_MULTIPLIERS.put("naturalist", 1.3);
        MOD_MULTIPLIERS.put("crittersandcompanions", 1.2);
        MOD_MULTIPLIERS.put("rats", 1.5);
        MOD_MULTIPLIERS.put("nyfsspiders", 1.5);
        MOD_MULTIPLIERS.put("benssharks", 1.8);
        MOD_MULTIPLIERS.put("artifacts", 3.0);
        MOD_MULTIPLIERS.put("simplyswords", 2.5);
        MOD_MULTIPLIERS.put("radiantgear", 2.0);
        MOD_MULTIPLIERS.put("scguns", 2.0);
        MOD_MULTIPLIERS.put("bettercombat", 1.5);
        MOD_MULTIPLIERS.put("create", 2.0);
        MOD_MULTIPLIERS.put("immersive_aircraft", 2.5);
        MOD_MULTIPLIERS.put("immersive_machinery", 2.0);
        MOD_MULTIPLIERS.put("simpleplanes", 2.0);
        MOD_MULTIPLIERS.put("automobility", 2.0);
        MOD_MULTIPLIERS.put("car", 2.0);
        MOD_MULTIPLIERS.put("valkyrienskies", 3.0);
        MOD_MULTIPLIERS.put("vs_eureka", 3.0);
        MOD_MULTIPLIERS.put("farmersdelight", 1.0);
        MOD_MULTIPLIERS.put("croptopia", 0.8);
        MOD_MULTIPLIERS.put("bakery", 1.0);
        MOD_MULTIPLIERS.put("beachparty", 1.0);
        MOD_MULTIPLIERS.put("brewery", 1.2);
        MOD_MULTIPLIERS.put("candlelight", 1.0);
        MOD_MULTIPLIERS.put("culturaldelights", 1.0);
        MOD_MULTIPLIERS.put("quality_food", 1.0);
        MOD_MULTIPLIERS.put("butchery", 1.0);
        MOD_MULTIPLIERS.put("displaydelight", 1.0);
        MOD_MULTIPLIERS.put("another_furniture", 0.8);
        MOD_MULTIPLIERS.put("amendments", 0.8);
        MOD_MULTIPLIERS.put("cozy_home", 0.8);
        MOD_MULTIPLIERS.put("dustydecorations", 0.8);
        MOD_MULTIPLIERS.put("cluttered", 0.8);
        MOD_MULTIPLIERS.put("supplementaries", 1.0);
        MOD_MULTIPLIERS.put("quark", 1.0);
        MOD_MULTIPLIERS.put("integrated_dungeons_arise", 2.0);
        MOD_MULTIPLIERS.put("idas", 2.0);
        MOD_MULTIPLIERS.put("betterdungeons", 1.8);
        MOD_MULTIPLIERS.put("betterfortresses", 1.8);
        MOD_MULTIPLIERS.put("betterstrongholds", 1.8);
        MOD_MULTIPLIERS.put("sophisticatedbackpacks", 1.5);
        MOD_MULTIPLIERS.put("ironchest", 1.3);
        MOD_MULTIPLIERS.put("backpacked", 1.3);
        MOD_MULTIPLIERS.put("carryon", 1.0);
    }

    private static void initModKeywords() {
        HashSet<String> alexscaves = new HashSet<String>();
        alexscaves.add("primordial");
        alexscaves.add("nuclear");
        alexscaves.add("tremorsaurus");
        alexscaves.add("luxtructosaurus");
        alexscaves.add("atlatitan");
        alexscaves.add("hullbreaker");
        alexscaves.add("forsaken");
        alexscaves.add("magnetron");
        alexscaves.add("radgill");
        alexscaves.add("nucleeper");
        alexscaves.add("corrodent");
        MOD_KEYWORDS.put("alexscaves", alexscaves);
        HashSet<String> alexsmobs = new HashSet<String>();
        alexsmobs.add("void_worm");
        alexsmobs.add("mimicube");
        alexsmobs.add("warped_toad");
        alexsmobs.add("bone_serpent");
        alexsmobs.add("dropbear");
        alexsmobs.add("soul_vulture");
        alexsmobs.add("straddler");
        alexsmobs.add("guster");
        MOD_KEYWORDS.put("alexsmobs", alexsmobs);
        HashSet<String> cataclysm = new HashSet<String>();
        cataclysm.add("ignis");
        cataclysm.add("netherite_monstrosity");
        cataclysm.add("leviathan");
        cataclysm.add("ender_guardian");
        cataclysm.add("ancient_remnant");
        cataclysm.add("harbinger");
        cataclysm.add("maledictus");
        cataclysm.add("void_forge");
        cataclysm.add("soul_render");
        cataclysm.add("wither_assault");
        cataclysm.add("bulwark");
        cataclysm.add("gauntlet");
        cataclysm.add("incinerator");
        MOD_KEYWORDS.put("cataclysm", cataclysm);
        HashSet<String> bomd = new HashSet<String>();
        bomd.add("lich");
        bomd.add("nether_gauntlet");
        bomd.add("obsidilith");
        bomd.add("void_blossom");
        bomd.add("night_lich");
        MOD_KEYWORDS.put("bosses_of_mass_destruction", bomd);
        HashSet<String> mowzies = new HashSet<String>();
        mowzies.add("ferrous_wroughtnaut");
        mowzies.add("frostmaw");
        mowzies.add("barako");
        mowzies.add("sun_chief");
        mowzies.add("umvuthi");
        mowzies.add("wrought");
        mowzies.add("foliaath");
        mowzies.add("naga");
        MOD_KEYWORDS.put("mowziesmobs", mowzies);
        HashSet<String> adastra = new HashSet<String>();
        adastra.add("rocket");
        adastra.add("space_suit");
        adastra.add("desh");
        adastra.add("ostrum");
        adastra.add("calorite");
        adastra.add("moon");
        adastra.add("mars");
        adastra.add("venus");
        adastra.add("mercury");
        adastra.add("glacio");
        MOD_KEYWORDS.put("ad_astra", adastra);
        HashSet<String> create = new HashSet<String>();
        create.add("brass");
        create.add("precision");
        create.add("mechanical");
        create.add("crushing_wheel");
        create.add("depot");
        create.add("cogwheel");
        create.add("shaft");
        create.add("gearbox");
        create.add("andesite_alloy");
        create.add("electron_tube");
        create.add("chromatic");
        create.add("propeller");
        create.add("train");
        MOD_KEYWORDS.put("create", create);
        HashSet<String> simplyswords = new HashSet<String>();
        simplyswords.add("runic");
        simplyswords.add("unique");
        simplyswords.add("watcher");
        simplyswords.add("emberlash");
        simplyswords.add("soulrender");
        simplyswords.add("thunderbrand");
        simplyswords.add("hearthflame");
        simplyswords.add("stormbringer");
        simplyswords.add("mjolnir");
        MOD_KEYWORDS.put("simplyswords", simplyswords);
        HashSet<String> artifacts = new HashSet<String>();
        artifacts.add("everlasting");
        artifacts.add("cloud_in_a_bottle");
        artifacts.add("pocket_piston");
        artifacts.add("obsidian_skull");
        artifacts.add("mimic");
        artifacts.add("crystal_heart");
        artifacts.add("bunny_hoppers");
        artifacts.add("vampiric");
        artifacts.add("feral_claws");
        artifacts.add("power_glove");
        artifacts.add("digging_claws");
        MOD_KEYWORDS.put("artifacts", artifacts);
        HashSet<String> chaos = new HashSet<String>();
        chaos.add("dark_young");
        chaos.add("pumpkin_spirit");
        chaos.add("direfolk");
        chaos.add("supreme");
        chaos.add("nightmare");
        chaos.add("siren");
        chaos.add("fallen");
        MOD_KEYWORDS.put("born_in_chaos_v1", chaos);
    }

    private static void initBossKeywords() {
        BOSS_KEYWORDS.add("boss");
        BOSS_KEYWORDS.add("trophy");
        BOSS_KEYWORDS.add("relic");
        BOSS_KEYWORDS.add("ancient");
        BOSS_KEYWORDS.add("legendary");
        BOSS_KEYWORDS.add("mythic");
        BOSS_KEYWORDS.add("supreme");
        BOSS_KEYWORDS.add("primordial");
        BOSS_KEYWORDS.add("artifact");
        BOSS_KEYWORDS.add("monstrosity");
        BOSS_KEYWORDS.add("guardian");
        BOSS_KEYWORDS.add("harbinger");
        BOSS_KEYWORDS.add("void");
        BOSS_KEYWORDS.add("soul_render");
        BOSS_KEYWORDS.add("gauntlet");
        BOSS_KEYWORDS.add("ignis");
        BOSS_KEYWORDS.add("leviathan");
        BOSS_KEYWORDS.add("maledictus");
        BOSS_KEYWORDS.add("netherite_monstrosity");
        BOSS_KEYWORDS.add("ender_guardian");
        BOSS_KEYWORDS.add("ferrous_wroughtnaut");
        BOSS_KEYWORDS.add("frostmaw");
        BOSS_KEYWORDS.add("barako");
        BOSS_KEYWORDS.add("tremorsaurus");
        BOSS_KEYWORDS.add("luxtructosaurus");
        BOSS_KEYWORDS.add("atlatitan");
        BOSS_KEYWORDS.add("hullbreaker");
        BOSS_KEYWORDS.add("lich");
        BOSS_KEYWORDS.add("obsidilith");
        BOSS_KEYWORDS.add("void_blossom");
    }

    private static void initDimensionKeywords() {
        DIMENSION_KEYWORDS.add("primordial");
        DIMENSION_KEYWORDS.add("magnetic");
        DIMENSION_KEYWORDS.add("toxic");
        DIMENSION_KEYWORDS.add("abyssal");
        DIMENSION_KEYWORDS.add("forlorn");
        DIMENSION_KEYWORDS.add("candy");
        DIMENSION_KEYWORDS.add("moon");
        DIMENSION_KEYWORDS.add("mars");
        DIMENSION_KEYWORDS.add("venus");
        DIMENSION_KEYWORDS.add("mercury");
        DIMENSION_KEYWORDS.add("glacio");
        DIMENSION_KEYWORDS.add("space");
        DIMENSION_KEYWORDS.add("orbit");
        DIMENSION_KEYWORDS.add("luminous_nether");
        DIMENSION_KEYWORDS.add("nether");
        DIMENSION_KEYWORDS.add("soul");
        DIMENSION_KEYWORDS.add("basalt");
        DIMENSION_KEYWORDS.add("crimson");
        DIMENSION_KEYWORDS.add("warped");
        DIMENSION_KEYWORDS.add("end");
        DIMENSION_KEYWORDS.add("chorus");
        DIMENSION_KEYWORDS.add("shulker");
        DIMENSION_KEYWORDS.add("purpur");
        DIMENSION_KEYWORDS.add("sculk");
        DIMENSION_KEYWORDS.add("warden");
        DIMENSION_KEYWORDS.add("ancient_city");
        DIMENSION_KEYWORDS.add("deep_dark");
        DIMENSION_KEYWORDS.add("aquamirae");
        DIMENSION_KEYWORDS.add("abyss");
        DIMENSION_KEYWORDS.add("trench");
        DIMENSION_KEYWORDS.add("deep_ocean");
    }

    public static double getModMultiplier(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 1.0;
        }
        String modId = id.getNamespace();
        return MOD_MULTIPLIERS.getOrDefault(modId, 1.0);
    }

    public static int getKeywordScore(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        String modId = id.getNamespace();
        String name = id.getPath().toLowerCase();
        int score = 0;
        Set<String> modKeys = MOD_KEYWORDS.get(modId);
        if (modKeys != null) {
            for (String keyword : modKeys) {
                if (!name.contains(keyword)) continue;
                score += 25;
            }
        }
        for (String keyword : BOSS_KEYWORDS) {
            if (!name.contains(keyword)) continue;
            score += 40;
            break;
        }
        for (String keyword : DIMENSION_KEYWORDS) {
            if (!name.contains(keyword)) continue;
            score += 15;
            break;
        }
        return Math.min(score, 80);
    }

    public static boolean isBossItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String name = id.getPath().toLowerCase();
        for (String keyword : BOSS_KEYWORDS) {
            if (!name.contains(keyword)) continue;
            return true;
        }
        return false;
    }

    public static boolean isDimensionItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String name = id.getPath().toLowerCase();
        for (String keyword : DIMENSION_KEYWORDS) {
            if (!name.contains(keyword)) continue;
            return true;
        }
        return false;
    }

    public static String getItemCategory(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return "unknown";
        }
        String modId = id.getNamespace();
        if (modId.equals("cataclysm") || modId.equals("bosses_of_mass_destruction")) {
            return "boss";
        }
        if (modId.equals("alexsmobs") || modId.equals("mowziesmobs") || modId.equals("born_in_chaos_v1") || modId.equals("alexscaves")) {
            return "creature";
        }
        if (modId.equals("ad_astra") || modId.equals("regions_unexplored")) {
            return "dimension";
        }
        if (modId.equals("farmersdelight") || modId.equals("croptopia") || modId.equals("bakery") || modId.equals("brewery")) {
            return "food";
        }
        if (modId.equals("create") || modId.equals("immersive_aircraft")) {
            return "tech";
        }
        if (modId.equals("simplyswords") || modId.equals("artifacts") || modId.equals("radiantgear")) {
            return "combat";
        }
        return "misc";
    }

    static {
        ModPricingConfig.initModMultipliers();
        ModPricingConfig.initModKeywords();
        ModPricingConfig.initBossKeywords();
        ModPricingConfig.initDimensionKeywords();
    }
}

