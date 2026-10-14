package hari.harishop.forge;

import dev.architectury.platform.forge.EventBuses;
import hari.harishop.HariShopMod;
import hari.harishop.network.ModPackets;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HariShopMod.MOD_ID)
public class HariShopForgeMod {

    public HariShopForgeMod() {
        EventBuses.registerModEventBus(HariShopMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Register Forge-native networking BEFORE init, so ModPackets.register() will skip Architectury
        ForgeModPackets.register();
        ModPackets.setPlatformSender(new ModPackets.PacketSender() {
            @Override public void sendSetShopCategory(hari.harishop.network.SetShopCategoryPacket msg) { ForgeModPackets.sendToServer(msg); }
            @Override public void sendBuyItem(hari.harishop.network.BuyItemPacket msg) { ForgeModPackets.sendToServer(msg); }
            @Override public void sendSellItem(hari.harishop.network.SellItemPacket msg) { ForgeModPackets.sendToServer(msg); }
            @Override public void sendRemoveShopItem(hari.harishop.network.RemoveShopItemPacket msg) { ForgeModPackets.sendToServer(msg); }
            @Override public void sendShopSync(hari.harishop.network.ShopSyncPacket msg, net.minecraft.server.level.ServerPlayer player) { ForgeModPackets.sendToPlayer(msg, player); }
            @Override public void sendShopSyncAll(hari.harishop.network.ShopSyncPacket msg, Iterable<net.minecraft.server.level.ServerPlayer> players) { ForgeModPackets.sendToAllPlayers(msg, players); }
        });

        HariShopMod.init();
    }
}
