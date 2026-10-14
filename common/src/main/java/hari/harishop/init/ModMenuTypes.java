/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.inventory.MenuType
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.world.flag.FeatureFlags
 */
package hari.harishop.init;

import hari.harishop.client.SellShopMenu;
import hari.harishop.client.ShopMenu;
import net.minecraft.world.inventory.MenuType;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create("harishop", Registries.MENU);
    public static final RegistrySupplier<MenuType<ShopMenu>> SHOP_MENU = MENUS.register("shop_menu", () -> MenuRegistry.ofExtended(ShopMenu::new));
    public static final RegistrySupplier<MenuType<SellShopMenu>> SELL_SHOP_MENU = MENUS.register("sell_shop_menu", () -> MenuRegistry.ofExtended(SellShopMenu::new));

    public static void register() {
        MENUS.register();
    }
}
