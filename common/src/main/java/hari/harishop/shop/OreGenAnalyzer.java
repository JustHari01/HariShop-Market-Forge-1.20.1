/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.core.RegistryAccess$Frozen
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.levelgen.feature.ConfiguredFeature
 *  net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
 *  net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
 *  net.minecraft.world.level.levelgen.heightproviders.HeightProvider
 *  net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight
 *  net.minecraft.world.level.levelgen.heightproviders.UniformHeight
 *  net.minecraft.world.level.levelgen.placement.CountPlacement
 *  net.minecraft.world.level.levelgen.placement.HeightRangePlacement
 *  net.minecraft.world.level.levelgen.placement.PlacedFeature
 *  net.minecraft.world.level.levelgen.placement.PlacementModifier
 *  net.minecraft.world.level.levelgen.placement.RarityFilter
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class OreGenAnalyzer {
    private static boolean initialized = false;
    private static final Map<Item, OreData> oreDataMap = new ConcurrentHashMap<Item, OreData>();

    public static void init(MinecraftServer server) {
        if (initialized) {
            return;
        }
        initialized = true;
        HariShopMod.LOGGER.info("OreGenAnalyzer: Loading ore data from registry...");
        long start = System.currentTimeMillis();
        try {
            RegistryAccess.Frozen access = server.registryAccess();
            Registry<PlacedFeature> registry = access.registryOrThrow(Registries.PLACED_FEATURE);
            registry.entrySet().forEach(entry -> {
                try {
                    PlacedFeature pf = (PlacedFeature)entry.getValue();
                    ResourceLocation id = ((ResourceKey)entry.getKey()).location();
                    FeatureConfiguration patt5585$temp = ((ConfiguredFeature)pf.feature().value()).config();
                    if (patt5585$temp instanceof OreConfiguration) {
                        OreConfiguration oreConfig = (OreConfiguration)patt5585$temp;
                        OreData data = OreGenAnalyzer.extractOreData(pf, oreConfig);
                        oreConfig.targetStates.forEach(target -> {
                            Block block = target.state.getBlock();
                            Item item = block.asItem();
                            if (item != null) {
                                oreDataMap.put(item, data);
                            }
                        });
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            });
            HariShopMod.LOGGER.info("OreGenAnalyzer: Loaded {} ore types in {}ms", (Object)oreDataMap.size(), (Object)(System.currentTimeMillis() - start));
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("OreGenAnalyzer: Failed to load ore data", (Throwable)e);
        }
    }

    private static OreData extractOreData(PlacedFeature pf, OreConfiguration oreConfig) {
        int minY = -64;
        int maxY = 320;
        int countMin = 1;
        int countMax = 1;
        int rarityChance = 1;
        for (PlacementModifier modifier : pf.placement()) {
            if (modifier instanceof HeightRangePlacement) {
                HeightRangePlacement heightRange = (HeightRangePlacement)modifier;
                HeightProvider hp = OreGenAnalyzer.getHeightProvider(heightRange);
                if (hp == null) continue;
                int[] range = OreGenAnalyzer.extractHeightRange(hp);
                minY = range[0];
                maxY = range[1];
                continue;
            }
            if (modifier instanceof CountPlacement) {
                CountPlacement countPlacement = (CountPlacement)modifier;
                int[] counts = OreGenAnalyzer.extractCount(countPlacement);
                countMin = counts[0];
                countMax = counts[1];
                continue;
            }
            if (!(modifier instanceof RarityFilter)) continue;
            RarityFilter rarityFilter = (RarityFilter)modifier;
            rarityChance = OreGenAnalyzer.extractRarity(rarityFilter);
        }
        return new OreData(minY, maxY, oreConfig.size, countMin, countMax, rarityChance, oreConfig.discardChanceOnAirExposure);
    }

    private static HeightProvider getHeightProvider(HeightRangePlacement placement) {
        try {
            Field field = HeightRangePlacement.class.getDeclaredField("height");
            field.setAccessible(true);
            return (HeightProvider)field.get(placement);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static int[] extractHeightRange(HeightProvider hp) {
        try {
            if (hp instanceof UniformHeight) {
                UniformHeight uniform = (UniformHeight)hp;
                Field minField = UniformHeight.class.getDeclaredField("minInclusive");
                Field maxField = UniformHeight.class.getDeclaredField("maxInclusive");
                minField.setAccessible(true);
                maxField.setAccessible(true);
                return new int[]{-64, 320};
            }
            if (hp instanceof TrapezoidHeight) {
                TrapezoidHeight trapezoid = (TrapezoidHeight)hp;
                Field minField = TrapezoidHeight.class.getDeclaredField("minInclusive");
                Field maxField = TrapezoidHeight.class.getDeclaredField("maxInclusive");
                minField.setAccessible(true);
                maxField.setAccessible(true);
                return new int[]{-64, 320};
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return new int[]{-64, 320};
    }

    private static int[] extractCount(CountPlacement placement) {
        try {
            Field countField = CountPlacement.class.getDeclaredField("count");
            countField.setAccessible(true);
            Object intProvider = countField.get(placement);
            if (intProvider != null) {
                Method minMethod = intProvider.getClass().getMethod("getMinValue", new Class[0]);
                Method maxMethod = intProvider.getClass().getMethod("getMaxValue", new Class[0]);
                int min = (Integer)minMethod.invoke(intProvider, new Object[0]);
                int max = (Integer)maxMethod.invoke(intProvider, new Object[0]);
                return new int[]{min, max};
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return new int[]{1, 1};
    }

    private static int extractRarity(RarityFilter filter) {
        try {
            Field chanceField = RarityFilter.class.getDeclaredField("chance");
            chanceField.setAccessible(true);
            return chanceField.getInt(filter);
        }
        catch (Exception e) {
            return 1;
        }
    }

    public static boolean hasOreData(Item item) {
        return oreDataMap.containsKey(item);
    }

    public static OreData getOreData(Item item) {
        return oreDataMap.get(item);
    }

    public static double getOrePriceMultiplier(Item item) {
        OreData data = oreDataMap.get(item);
        return data != null ? data.getPriceMultiplier() : 1.0;
    }

    public static Map<Item, OreData> getAllOreData() {
        return oreDataMap;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static class OreData {
        public final int minY;
        public final int maxY;
        public final int avgY;
        public final int oreSize;
        public final int countMin;
        public final int countMax;
        public final int rarityChance;
        public final float discardOnAir;

        public OreData(int minY, int maxY, int oreSize, int countMin, int countMax, int rarityChance, float discardOnAir) {
            this.minY = minY;
            this.maxY = maxY;
            this.avgY = (minY + maxY) / 2;
            this.oreSize = oreSize;
            this.countMin = countMin;
            this.countMax = countMax;
            this.rarityChance = rarityChance;
            this.discardOnAir = discardOnAir;
        }

        public float getBlocksPerChunk() {
            float avgVeins = (float)(this.countMin + this.countMax) / 2.0f;
            if (this.rarityChance > 0) {
                avgVeins /= (float)this.rarityChance;
            }
            float blocksPerVein = (float)this.oreSize * (1.0f - this.discardOnAir);
            return avgVeins * blocksPerVein;
        }

        public double getPriceMultiplier() {
            float blocksPerChunk = this.getBlocksPerChunk();
            double depthMult = this.avgY < -48 ? 5.0 : (this.avgY < 0 ? 3.0 : (this.avgY < 32 ? 2.0 : (this.avgY < 64 ? 1.5 : 1.0)));
            double rarityMult = (double)blocksPerChunk <= 0.1 ? 20.0 : ((double)blocksPerChunk <= 0.5 ? 10.0 : (blocksPerChunk <= 2.0f ? 5.0 : (blocksPerChunk <= 5.0f ? 3.0 : (blocksPerChunk <= 10.0f ? 2.0 : 1.0))));
            return depthMult * rarityMult;
        }

        public String toString() {
            return String.format("OreData[Y=%d~%d, size=%d, veins=%d~%d, rarity=1/%d, blocksPerChunk=%.2f, mult=%.1fx]", this.minY, this.maxY, this.oreSize, this.countMin, this.countMax, this.rarityChance, Float.valueOf(this.getBlocksPerChunk()), this.getPriceMultiplier());
        }
    }
}

