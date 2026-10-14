/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screens.MenuScreens
 *  net.minecraft.world.inventory.MenuType
 *  dev.architectury.registry.menu.MenuRegistry
 *  dev.architectury.event.events.client.ClientPlayerEvent
 */
package hari.harishop.client;

import hari.harishop.HariShopMod;
import hari.harishop.client.SellShopScreen;
import hari.harishop.client.ShopScreen;
import hari.harishop.init.ModMenuTypes;
import hari.harishop.shop.CreativeTabShopManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.event.events.client.ClientPlayerEvent;

public class ClientEvents {
    public static void init() {
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> {
            MenuRegistry.registerScreenFactory(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
            MenuRegistry.registerScreenFactory(ModMenuTypes.SELL_SHOP_MENU.get(), SellShopScreen::new);
            HariShopMod.LOGGER.info("Registered shop screens");
        });
        
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            HariShopMod.LOGGER.info("Client joined world, initializing shop data...");
            CreativeTabShopManager.init();
        });
    }
}

