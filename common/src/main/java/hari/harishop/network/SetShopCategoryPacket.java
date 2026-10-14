/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  dev.architectury.networking.NetworkManager
 */
package hari.harishop.network;

import hari.harishop.block.NightMarketBlockEntity;
import hari.harishop.block.PawnShopBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.architectury.networking.NetworkManager;

public class SetShopCategoryPacket {
    private final BlockPos pos;
    private final ResourceLocation categoryId;

    public SetShopCategoryPacket(BlockPos pos, ResourceLocation categoryId) {
        this.pos = pos;
        this.categoryId = categoryId;
    }

    public SetShopCategoryPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.categoryId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeBoolean(this.categoryId != null);
        if (this.categoryId != null) {
            buf.writeResourceLocation(this.categoryId);
        }
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext ctx = contextSupplier.get();
        ctx.queue(() -> {
            net.minecraft.world.entity.player.Player rawPlayer = ctx.getPlayer();
            if (!(rawPlayer instanceof ServerPlayer player)) {
                return;
            }
            handleServer(player);
        });
    }

    public void handleServer(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("\u00a7cYou don't have permission to configure shops!"));
            return;
        }
        BlockEntity be = player.level().getBlockEntity(this.pos);
        if (this.categoryId != null && !hari.harishop.shop.CreativeTabShopManager.getAvailableTabs().contains(this.categoryId)) {
            player.sendSystemMessage(Component.literal("\u00a7cInvalid shop category."));
            return;
        }
        if (be instanceof NightMarketBlockEntity nmbe) {
            nmbe.setCategory(this.categoryId);
            player.sendSystemMessage(Component.literal("\u00a7a\u2713 Shop category saved!"));
        } else if (be instanceof PawnShopBlockEntity psbe) {
            psbe.setCategory(this.categoryId);
            player.sendSystemMessage(Component.literal("\u00a7a\u2713 Shop category saved!"));
        } else {
            player.sendSystemMessage(Component.literal("\u00a7cInvalid shop block!"));
        }
    }
}
