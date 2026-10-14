/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraftforge.fml.loading.FMLPaths
 */
package hari.harishop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hari.harishop.HariShopMod;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.architectury.platform.Platform;

public class ShopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "harishop-config.json";
    private static ShopConfig INSTANCE;
    public List<String> blacklistedMods = new ArrayList<String>();
    public List<String> blacklistedItems = new ArrayList<String>();
    public Map<String, Double> modPriceMultipliers = new HashMap<String, Double>();
    public Map<String, Long> customItemPrices = new HashMap<String, Long>();
    public Map<String, Long> materialBasePrices = new HashMap<String, Long>();
    public int dailyDealsMaxDiscount = 50;
    public int dailyDealsMinDiscount = 10;
    public int dailyDealsItemCount = 40;
    public int dailyDealsResetHours = 6;
    public double sellPriceRatio = 0.2;

    public static ShopConfig get() {
        if (INSTANCE == null) {
            INSTANCE = ShopConfig.load();
        }
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = ShopConfig.load();
    }

    private static ShopConfig load() {
        final Path configPath;
        try {
            configPath = Platform.getConfigFolder().resolve(CONFIG_FILE);
        } catch (Throwable platformUnavailable) {
            // Headless contract tests and a partially initialised loader may
            // not expose Architectury's config directory yet.  Keep the
            // economic defaults in memory and do not attempt a write through
            // an unavailable platform service.
            HariShopMod.LOGGER.warn("HariShop config folder unavailable; using in-memory defaults", platformUnavailable);
            return ShopConfig.createDefault();
        }
        if (Files.exists(configPath, new LinkOption[0])) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                ShopConfig config = GSON.fromJson(reader, ShopConfig.class);
                HariShopMod.LOGGER.info("Loaded HariShop config");
                return config;
            } catch (Exception e) {
                HariShopMod.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        ShopConfig config = ShopConfig.createDefault();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = Platform.getConfigFolder().resolve(CONFIG_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(configPath, new OpenOption[0]);){
            GSON.toJson((Object)this, (Appendable)writer);
            HariShopMod.LOGGER.info("Saved HariShop config");
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("Failed to save config", (Throwable)e);
        }
    }

    private static ShopConfig createDefault() {
        ShopConfig config = new ShopConfig();
        config.blacklistedMods.addAll(Arrays.asList("c2me", "modernfix", "ferritecore", "starlight", "radium", "noisium", "badoptimizations", "alternate_current", "krypton_fnp", "fastload", "lightspeed", "clumps", "getittogetherdrops", "carpet", "chunky", "spark", "neruina", "crash_assistant", "logbegone", "packetfixer", "connectivity", "packet_tweaker", "luckperms", "bluemap", "voicechat", "placeholder_api", "grieflogger", "flan", "openpartiesandclaims", "cloth_config", "architectury", "geckolib", "citadel", "resourcefullib", "resourcefulconfig", "puzzleslib", "balm", "moonlight", "blueprint", "zeta", "collective", "kotlinforforge", "fabric_api", "connectormod"));
        config.blacklistedItems.addAll(Arrays.asList("minecraft:bedrock", "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block", "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:barrier", "minecraft:light", "minecraft:debug_stick", "minecraft:spawner"));
        config.modPriceMultipliers.put("ad_astra", 2.0);
        config.modPriceMultipliers.put("cataclysm", 3.0);
        config.modPriceMultipliers.put("born_in_chaos_v1", 2.0);
        config.modPriceMultipliers.put("alexscaves", 1.8);
        config.modPriceMultipliers.put("bosses_of_mass_destruction", 2.5);
        config.modPriceMultipliers.put("mowziesmobs", 1.5);
        config.modPriceMultipliers.put("aquamirae", 1.8);
        config.modPriceMultipliers.put("alexsmobs", 1.3);
        config.modPriceMultipliers.put("create", 1.5);
        config.modPriceMultipliers.put("simplyswords", 2.0);
        config.modPriceMultipliers.put("artifacts", 2.5);
        config.modPriceMultipliers.put("valkyrienskies", 2.0);
        config.modPriceMultipliers.put("immersive_aircraft", 1.8);
        config.modPriceMultipliers.put("scguns", 2.0);
        config.modPriceMultipliers.put("rats", 1.2);
        config.modPriceMultipliers.put("deep_dark_regrowth", 1.5);
        config.modPriceMultipliers.put("luminous_nether", 1.3);
        config.modPriceMultipliers.put("regions_unexplored", 1.2);
        config.modPriceMultipliers.put("another_furniture", 0.8);
        config.modPriceMultipliers.put("amendments", 0.8);
        config.modPriceMultipliers.put("mcwholidays", 0.7);
        config.modPriceMultipliers.put("dustydecorations", 0.7);
        config.modPriceMultipliers.put("cozy_home", 0.8);
        config.modPriceMultipliers.put("cluttered", 0.8);
        config.materialBasePrices.put("coal", 30L);
        config.materialBasePrices.put("copper", 50L);
        config.materialBasePrices.put("iron", 100L);
        config.materialBasePrices.put("gold", 500L);
        config.materialBasePrices.put("lapis", 200L);
        config.materialBasePrices.put("redstone", 150L);
        config.materialBasePrices.put("diamond", 5000L);
        config.materialBasePrices.put("emerald", 3000L);
        config.materialBasePrices.put("netherite", 50000L);
        config.materialBasePrices.put("netherite_scrap", 15000L);
        config.materialBasePrices.put("ancient_debris", 15000L);
        config.materialBasePrices.put("quartz", 100L);
        config.materialBasePrices.put("amethyst", 150L);
        config.materialBasePrices.put("echo_shard", 8000L);
        return config;
    }

    public boolean isModBlacklisted(String modId) {
        return this.blacklistedMods.contains(modId.toLowerCase());
    }

    public boolean isItemBlacklisted(String itemId) {
        return this.blacklistedItems.contains(itemId.toLowerCase());
    }

    public double getModMultiplier(String modId) {
        return this.modPriceMultipliers.getOrDefault(modId.toLowerCase(), 1.0);
    }

    public long getCustomPrice(String itemId) {
        return this.customItemPrices.getOrDefault(itemId.toLowerCase(), -1L);
    }

    public long getMaterialBasePrice(String materialType) {
        return this.materialBasePrices.getOrDefault(materialType.toLowerCase(), 100L);
    }
}

