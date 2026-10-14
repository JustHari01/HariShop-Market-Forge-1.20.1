package hari.harishop.init;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import hari.harishop.HariShopMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(HariShopMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> DAILYSHOP_TAB = TABS.register("main", () ->
        CreativeTabRegistry.create(
            Component.translatable("itemGroup.harishop.main"),
            () -> new ItemStack(ModBlocks.DAILY_DEALS_ITEM.get())
        )
    );

    public static void register() {
        TABS.register();
    }
}
