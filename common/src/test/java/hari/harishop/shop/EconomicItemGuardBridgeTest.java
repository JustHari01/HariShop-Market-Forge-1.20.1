package hari.harishop.shop;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The admin/provenance marker can never become a pawn payout. */
class EconomicItemGuardBridgeTest {
    @Test
    void namespacedProvenanceMarkerIsQuarantined() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemStack stack = new ItemStack(Items.STONE);
        stack.getOrCreateTag().put("harishop_provenance", new net.minecraft.nbt.CompoundTag());
        assertTrue(hari.harishop.integration.EconomicItemGuardBridge.isNonEconomic(stack));
        assertEquals(0L, ItemValueCalculator.calculatePrice(stack));
    }

    @Test
    void ordinarySurvivalStackIsNotQuarantinedByMissingOptionalMod() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemStack stack = new ItemStack(Items.STONE);
        assertFalse(hari.harishop.integration.EconomicItemGuardBridge.isNonEconomic(stack));
        assertTrue(ItemValueCalculator.calculatePrice(stack) > 0L);
    }
}
