package hari.harishop.shop;

import hari.harishop.config.ShopConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Contract tests for the Forge 1.20.1 generated variant catalog. */
class VariantCatalogTest {
    @Test
    void generatedVariantsAreBoundedUniqueAndPriced() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ShopConfig config = new ShopConfig();
        var configField = ShopConfig.class.getDeclaredField("INSTANCE");
        configField.setAccessible(true);
        configField.set(null, config);
        List<ItemStack> variants = VariantCatalog.generate(config);
        assertFalse(variants.isEmpty(), "vanilla registry should provide potion/enchantment variants");
        assertTrue(variants.size() <= VariantCatalog.MAX_GENERATED_VARIANTS);

        Set<String> identities = new HashSet<>();
        for (ItemStack stack : variants) {
            assertFalse(stack.isEmpty());
            assertEquals(1, stack.getCount());
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            assertNotNull(id);
            assertTrue(ItemValueCalculator.calculatePrice(stack) > 0L,
                    () -> "generated variant was admitted without a positive quote: " + id + " " + stack.getTag());
            assertTrue(identities.add(id + "|" + stack.getTag()),
                    () -> "duplicate generated variant: " + id + " " + stack.getTag());
        }
    }
}
