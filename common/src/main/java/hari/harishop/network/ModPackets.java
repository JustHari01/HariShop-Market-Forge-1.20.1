/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.level.ServerPlayer
 *  dev.architectury.networking.NetworkManager
 *  io.netty.buffer.Unpooled
 */
package hari.harishop.network;

import hari.harishop.HariShopMod;
import hari.harishop.network.BuyItemPacket;
import hari.harishop.network.RemoveShopItemPacket;
import hari.harishop.network.SellItemPacket;
import hari.harishop.network.SetShopCategoryPacket;
import hari.harishop.network.ShopSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;

public class ModPackets {
    public static final ResourceLocation SET_SHOP_CATEGORY_ID = new ResourceLocation("harishop", "set_shop_category");
    public static final ResourceLocation REMOVE_SHOP_ITEM_ID = new ResourceLocation("harishop", "remove_shop_item");
    public static final ResourceLocation SHOP_SYNC_ID = new ResourceLocation("harishop", "shop_sync");
    public static final ResourceLocation BUY_ITEM_ID = new ResourceLocation("harishop", "buy_item");
    public static final ResourceLocation SELL_ITEM_ID = new ResourceLocation("harishop", "sell_item");

    // Delegate interface for platform-specific implementations
    private static PacketSender sender = null;

    public static void setPlatformSender(PacketSender platformSender) {
        sender = platformSender;
    }

    public static void register() {
        // This method is now a no-op for Forge - registration happens in ForgeModPackets
        // Kept for Fabric/Quilt compatibility
        if (sender != null) {
            HariShopMod.LOGGER.info("ModPackets.register() skipped - platform-specific networking active");
            return;
        }

        // Fallback to Architectury networking (for Fabric/Quilt)
        try {
            dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.Side.C2S, SET_SHOP_CATEGORY_ID, (buf, context) -> {
                SetShopCategoryPacket packet = new SetShopCategoryPacket(buf);
                packet.handle(() -> context);
            });
            dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.Side.C2S, REMOVE_SHOP_ITEM_ID, (buf, context) -> {
                RemoveShopItemPacket packet = new RemoveShopItemPacket(buf);
                packet.handle(() -> context);
            });
            dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.Side.C2S, BUY_ITEM_ID, (buf, context) -> {
                BuyItemPacket packet = new BuyItemPacket(buf);
                packet.handle(() -> context);
            });
            dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.Side.C2S, SELL_ITEM_ID, (buf, context) -> {
                SellItemPacket packet = new SellItemPacket(buf);
                packet.handle(() -> context);
            });
            dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.Side.S2C, SHOP_SYNC_ID, (buf, context) -> {
                ShopSyncPacket packet = new ShopSyncPacket(buf);
                packet.handle(() -> context);
            });
            HariShopMod.LOGGER.info("Registered HariShop network packets (Architectury)");
        } catch (Exception e) {
            HariShopMod.LOGGER.error("Failed to register Architectury networking, falling back", e);
        }
    }

    public static void sendToServer(SetShopCategoryPacket message) {
        if (sender != null) { sender.sendSetShopCategory(message); return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        message.encode(buf);
        dev.architectury.networking.NetworkManager.sendToServer(SET_SHOP_CATEGORY_ID, buf);
    }
    
    public static void sendToServer(BuyItemPacket message) {
        if (sender != null) { sender.sendBuyItem(message); return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        message.encode(buf);
        dev.architectury.networking.NetworkManager.sendToServer(BUY_ITEM_ID, buf);
    }
    
    public static void sendToServer(SellItemPacket message) {
        if (sender != null) { sender.sendSellItem(message); return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        message.encode(buf);
        dev.architectury.networking.NetworkManager.sendToServer(SELL_ITEM_ID, buf);
    }
    
    public static void sendToServer(RemoveShopItemPacket message) {
        if (sender != null) { sender.sendRemoveShopItem(message); return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        message.encode(buf);
        dev.architectury.networking.NetworkManager.sendToServer(REMOVE_SHOP_ITEM_ID, buf);
    }

    public static void sendToPlayer(ShopSyncPacket message, ServerPlayer player) {
        if (sender != null) { sender.sendShopSync(message, player); return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        message.encode(buf);
        dev.architectury.networking.NetworkManager.sendToPlayer(player, SHOP_SYNC_ID, buf);
    }

    public static void sendToAllPlayers(ShopSyncPacket message, Iterable<ServerPlayer> players) {
        if (sender != null) { sender.sendShopSyncAll(message, players); return; }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        message.encode(buf);
        dev.architectury.networking.NetworkManager.sendToPlayers(players, SHOP_SYNC_ID, buf);
    }

    public static interface PacketSender {
        void sendSetShopCategory(SetShopCategoryPacket msg);
        void sendBuyItem(BuyItemPacket msg);
        void sendSellItem(SellItemPacket msg);
        void sendRemoveShopItem(RemoveShopItemPacket msg);
        void sendShopSync(ShopSyncPacket msg, ServerPlayer player);
        void sendShopSyncAll(ShopSyncPacket msg, Iterable<ServerPlayer> players);
    }
}
