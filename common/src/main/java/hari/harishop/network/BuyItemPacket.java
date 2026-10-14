/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  dev.architectury.networking.NetworkManager
 */
package hari.harishop.network;

import hari.harishop.client.ShopMenu;
import hari.harishop.integration.LightmansHelper;
import hari.harishop.shop.MarketDynamics;
import hari.harishop.shop.ShopTrade;
import hari.harishop.v5.MoneyMath;
import hari.harishop.v5.ShopRequestJournal;
import hari.harishop.v5.TransactionGate;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import dev.architectury.networking.NetworkManager;
import java.util.UUID;

public class BuyItemPacket {
    private final int tradeIndex;
    private final int quantity;
    private final UUID transactionId;

    public BuyItemPacket(int tradeIndex, int quantity) {
        this(tradeIndex, quantity, UUID.randomUUID());
    }

    /** Client-generated nonce; the server treats it only as an idempotency
     * key and recomputes every price/identity from its current menu. */
    public BuyItemPacket(int tradeIndex, int quantity, UUID transactionId) {
        this.tradeIndex = tradeIndex;
        this.quantity = quantity;
        this.transactionId = transactionId == null ? UUID.randomUUID() : transactionId;
    }

    public BuyItemPacket(FriendlyByteBuf buffer) {
        this.tradeIndex = buffer.readInt();
        this.quantity = buffer.readInt();
        this.transactionId = buffer.readUUID();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.tradeIndex);
        buffer.writeInt(this.quantity);
        buffer.writeUUID(this.transactionId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player rawPlayer = context.getPlayer();
            if (!(rawPlayer instanceof ServerPlayer player)) {
                return;
            }
            handleServer(player);
        });
    }

    public void handleServer(ServerPlayer player) {
        // The client may send several clicks in parallel.  Serialise all
        // mutations for this player so two requests cannot spend the same
        // balance or consume the same stock snapshot.
        try {
            TransactionGate.execute(player, "buy", () -> {
                handleServerLocked(player);
                return 1;
            });
        } catch (RuntimeException ignored) {
            // Invalid/stale packets are deliberately fail-closed.  Never let
            // malformed client input take down the server thread.
        }
    }

    private void handleServerLocked(ServerPlayer player) {
        if (!(player.containerMenu instanceof ShopMenu)) {
            return;
        }
        ShopMenu menu = (ShopMenu)player.containerMenu;
        if (this.tradeIndex < 0 || this.tradeIndex >= menu.getTrades().size()
                || this.quantity <= 0 || this.quantity > 64) {
            return;
        }
        ShopTrade trade = menu.getTrades().get(this.tradeIndex);
        if (trade == null || !trade.isAvailable() || trade.getPriceInBronze() <= 0L) {
            return;
        }
        String requestId = this.transactionId.toString();
        String fingerprint = "buy:" + player.getUUID() + ':' + this.tradeIndex + ':' + this.quantity;
        if (ShopRequestJournal.begin(requestId, fingerprint) != ShopRequestJournal.Decision.NEW) {
            return;
        }
        boolean committed = false;
        try {
        if (trade.getStock() >= 0 && this.quantity > trade.getStock()) {
            return;
        }
        long price;
        try {
            price = MoneyMath.multiplyExact(trade.getPriceInBronze(), (long)this.quantity, "buy total");
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return;
        }
        ItemStack result = trade.getItemStack().copy();
        result.setCount(this.quantity);
        Inventory inventory = player.getInventory();
        if (!canFit(inventory, result)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Không đủ chỗ trống trong túi đồ."), true);
            return;
        }
        // Keep an exact slot snapshot.  Insert first, then debit; either
        // failure restores every slot byte-for-byte and cannot remove a
        // pre-existing identical stack during rollback.
        java.util.List<ItemStack> before = snapshot(inventory);
        if (!inventory.add(result)) {
            restoreSnapshot(inventory, before);
            return;
        }
        if (!LightmansHelper.deductMoney((Player)player, price, "harishop:buy:" + this.transactionId)) {
            restoreSnapshot(inventory, before);
            return;
        }
        // Currency + item insertion is the commit point.  If a later
        // bookkeeping callback throws, the request must still remain
        // completed so a retry cannot mint a second item.
        committed = true;
        ShopRequestJournal.complete(requestId, fingerprint);
        for (int i = 0; i < this.quantity; i++) {
            trade.purchase();
        }
        MarketDynamics.recordTransaction(trade.getItemStack().getItem(), this.quantity, true);
        } finally {
            if (!committed) ShopRequestJournal.abort(requestId, fingerprint);
        }
    }

    private static boolean canFit(Inventory inventory, ItemStack wanted) {
        long capacity = 0L;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack existing = inventory.getItem(i);
            if (existing.isEmpty()) {
                capacity += wanted.getMaxStackSize();
            } else if (ItemStack.isSameItemSameTags(existing, wanted)) {
                capacity += Math.max(0, existing.getMaxStackSize() - existing.getCount());
            }
            if (capacity >= wanted.getCount()) {
                return true;
            }
        }
        return false;
    }

    private static java.util.List<ItemStack> snapshot(Inventory inventory) {
        java.util.List<ItemStack> before = new java.util.ArrayList<ItemStack>(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            before.add(inventory.getItem(i).copy());
        }
        return before;
    }

    private static void restoreSnapshot(Inventory inventory, java.util.List<ItemStack> snapshot) {
        int limit = Math.min(inventory.getContainerSize(), snapshot.size());
        for (int i = 0; i < limit; i++) {
            inventory.setItem(i, snapshot.get(i).copy());
        }
    }
}
