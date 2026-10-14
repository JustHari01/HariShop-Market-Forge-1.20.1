/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.server.MinecraftServer
 *  dev.architectury.event.events.common.CommandRegistrationEvent
 *  dev.architectury.event.events.common.LifecycleEvent
 */
package hari.harishop;

import com.mojang.brigadier.CommandDispatcher;
import hari.harishop.command.HariShopCommand;
import hari.harishop.config.ShopConfig;
import hari.harishop.init.ModBlocks;
import hari.harishop.init.ModMenuTypes;
import hari.harishop.integration.JERHelper;
import hari.harishop.network.ModPackets;
import hari.harishop.shop.BreedingAnalyzer;
import hari.harishop.shop.CreativeTabShopManager;
import hari.harishop.shop.EconomyBalancer;
import hari.harishop.shop.LootTableAnalyzer;
import hari.harishop.shop.OreGenAnalyzer;
import hari.harishop.shop.RecipeAnalyzer;
import hari.harishop.shop.ShopEditHistory;
import hari.harishop.shop.VillagerTradeAnalyzer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HariShopMod {
    public static final String MOD_ID = "harishop";
    public static final Logger LOGGER = LogManager.getLogger("harishop");

    public static void init() {
        ModBlocks.register();
        ModMenuTypes.register();
        hari.harishop.init.ModCreativeTabs.register();
        LifecycleEvent.SETUP.register(HariShopMod::commonSetup);
        LifecycleEvent.SERVER_STARTED.register(HariShopMod::onServerStarted);
        CommandRegistrationEvent.EVENT.register(HariShopMod::onRegisterCommands);
        
        try {
            if (dev.architectury.platform.Platform.getEnvironment().name().equals("CLIENT")) {
                hari.harishop.client.ClientEvents.init();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load ClientEvents", e);
        }
        
        LOGGER.info("HariShop mod initialized!");
    }

    private static void commonSetup() {
        ModPackets.register();
        ShopConfig config = ShopConfig.get();
        LOGGER.info("Loaded HariShop config with {} blacklisted mods", config.blacklistedMods.size());
    }

    public static void onServerStarted(MinecraftServer server) {
        long start = System.currentTimeMillis();
        OreGenAnalyzer.init(server);
        LOGGER.info("OreGenAnalyzer: {} ore types loaded", OreGenAnalyzer.getAllOreData().size());
        VillagerTradeAnalyzer.init();
        LOGGER.info("VillagerTradeAnalyzer: {} buy trades, {} sell trades", VillagerTradeAnalyzer.getAllBuyTrades().size(), VillagerTradeAnalyzer.getAllSellTrades().size());
        LootTableAnalyzer.init(server);
        RecipeAnalyzer.init(server);
        LOGGER.info("RecipeAnalyzer: {} recipes cached, {} prices calculated", RecipeAnalyzer.getRecipeCount(), RecipeAnalyzer.getCacheSize());
        JERHelper.init();
        EconomyBalancer.init();
        BreedingAnalyzer.init(server);
        ShopEditHistory.init();
        CreativeTabShopManager.init();
        LOGGER.info("Shop initialized: {} Creative Tabs, {} items (took {}ms)", CreativeTabShopManager.getTabCount(), CreativeTabShopManager.getTotalItemCount(), System.currentTimeMillis() - start);
    }

    public static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher, net.minecraft.commands.CommandBuildContext registry, net.minecraft.commands.Commands.CommandSelection selection) {
        HariShopCommand.register(dispatcher);
        LOGGER.info("Registered /harishop command");
    }
}

