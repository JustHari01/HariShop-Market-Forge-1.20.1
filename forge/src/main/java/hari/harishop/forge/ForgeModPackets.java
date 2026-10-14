package hari.harishop.forge;

import hari.harishop.HariShopMod;
import hari.harishop.network.BuyItemPacket;
import hari.harishop.network.RemoveShopItemPacket;
import hari.harishop.network.SellItemPacket;
import hari.harishop.network.SetShopCategoryPacket;
import hari.harishop.network.ShopSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ForgeModPackets {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("harishop", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, SetShopCategoryPacket.class,
                SetShopCategoryPacket::encode,
                SetShopCategoryPacket::new,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            msg.handleServer(player);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(packetId++, RemoveShopItemPacket.class,
                RemoveShopItemPacket::encode,
                RemoveShopItemPacket::new,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            msg.handleServer(player);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(packetId++, BuyItemPacket.class,
                BuyItemPacket::encode,
                BuyItemPacket::new,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            msg.handleServer(player);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(packetId++, SellItemPacket.class,
                SellItemPacket::encode,
                SellItemPacket::new,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            msg.handleServer(player);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(packetId++, ShopSyncPacket.class,
                ShopSyncPacket::encode,
                ShopSyncPacket::new,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        msg.handleClient();
                    });
                    ctx.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        HariShopMod.LOGGER.info("Registered HariShop Forge network packets (bypassing Architectury)");
    }

    public static void sendToServer(SetShopCategoryPacket message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(BuyItemPacket message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(SellItemPacket message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(RemoveShopItemPacket message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ShopSyncPacket message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToAllPlayers(ShopSyncPacket message, Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}
