/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  dev.architectury.networking.NetworkManager
 */
package hari.harishop.network;

import hari.harishop.block.NightMarketBlockEntity;
import hari.harishop.block.PawnShopBlockEntity;
import hari.harishop.shop.ShopEditHistory;
import hari.harishop.shop.ShopTrade;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.architectury.networking.NetworkManager;

public class RemoveShopItemPacket {
    private final BlockPos pos;
    private final int itemIndex;

    public RemoveShopItemPacket(BlockPos pos, int itemIndex) {
        this.pos = pos;
        this.itemIndex = itemIndex;
    }

    public RemoveShopItemPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.itemIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.itemIndex);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            net.minecraft.world.entity.player.Player rawPlayer = context.getPlayer();
            if (!(rawPlayer instanceof ServerPlayer player)) {
                return;
            }
            handleServer(player);
        });
    }

    public void handleServer(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("\u00a7cYou don't have permission to edit shops!"));
            return;
        }
        BlockEntity be = player.level().getBlockEntity(this.pos);
        if (be instanceof NightMarketBlockEntity nmbe) {
            List<ShopTrade> trades = nmbe.getTrades();
            if (this.itemIndex >= 0 && this.itemIndex < trades.size()) {
                ShopTrade trade = trades.get(this.itemIndex);
                String category = nmbe.getSelectedTab() != null ? nmbe.getSelectedTab().toString() : "unknown";
                ShopEditHistory.recordDeletion(category, trade.getItemStack(), trade.getPriceInBronze(), player.getName().getString());
            }
            if (nmbe.removeItem(this.itemIndex)) {
                player.sendSystemMessage(Component.literal("\u00a7a\u2713 Item removed from shop!"));
            } else {
                player.sendSystemMessage(Component.literal("\u00a7cFailed to remove item!"));
            }
        } else if (be instanceof PawnShopBlockEntity psbe) {
            List<ShopTrade> trades = psbe.getTrades();
            if (this.itemIndex >= 0 && this.itemIndex < trades.size()) {
                ShopTrade trade = trades.get(this.itemIndex);
                String category = psbe.getSelectedTab() != null ? psbe.getSelectedTab().toString() : "unknown";
                ShopEditHistory.recordDeletion(category, trade.getItemStack(), trade.getPriceInBronze(), player.getName().getString());
            }
            if (psbe.removeItem(this.itemIndex)) {
                player.sendSystemMessage(Component.literal("\u00a7a\u2713 Item removed from shop!"));
            } else {
                player.sendSystemMessage(Component.literal("\u00a7cFailed to remove item!"));
            }
        } else {
            player.sendSystemMessage(Component.literal("\u00a7cInvalid shop block!"));
        }
    }
}
