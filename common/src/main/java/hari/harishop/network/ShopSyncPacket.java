/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  dev.architectury.networking.NetworkManager
 */
package hari.harishop.network;

import hari.harishop.shop.ShopTrade;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import dev.architectury.networking.NetworkManager;

public class ShopSyncPacket {
    private final BlockPos pos;
    private final ResourceLocation categoryId;
    private final List<ShopTrade> trades;

    public ShopSyncPacket(BlockPos pos, ResourceLocation categoryId, List<ShopTrade> trades) {
        this.pos = pos;
        this.categoryId = categoryId;
        this.trades = trades;
    }

    public ShopSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.categoryId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.trades = new ArrayList<ShopTrade>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; ++i) {
            ShopTrade trade;
            CompoundTag tag = buf.readNbt();
            if (tag == null || (trade = ShopTrade.fromNbt(tag)) == null) continue;
            this.trades.add(trade);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos != null ? this.pos : BlockPos.ZERO);
        buf.writeBoolean(this.categoryId != null);
        if (this.categoryId != null) {
            buf.writeResourceLocation(this.categoryId);
        }
        List<ShopTrade> safeTrades = this.trades != null ? this.trades : new ArrayList<>();
        buf.writeVarInt(safeTrades.size());
        for (ShopTrade trade : safeTrades) {
            if (trade != null) {
                buf.writeNbt(trade.toNbt());
                continue;
            }
            buf.writeNbt(new CompoundTag());
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            handleClient();
        });
    }

    public void handleClient() {
        try {
            Screen patt2480$temp;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && (patt2480$temp = mc.screen) instanceof ShopSyncHandler handler) {
                handler.onShopSync(this.pos, this.categoryId, this.trades);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static interface ShopSyncHandler {
        public void onShopSync(BlockPos var1, ResourceLocation var2, List<ShopTrade> var3);
    }
}
