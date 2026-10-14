/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  dev.architectury.networking.NetworkManager
 */
package hari.harishop.network;

import hari.harishop.client.SellShopMenu;
import hari.harishop.integration.LightmansHelper;
import hari.harishop.shop.MarketDynamics;
import hari.harishop.shop.ShopTrade;
import hari.harishop.v5.MoneyMath;
import hari.harishop.v5.ShopRequestJournal;
import hari.harishop.v5.TransactionGate;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import dev.architectury.networking.NetworkManager;
import java.util.UUID;

public class SellItemPacket {
    private final int tradeIndex;
    private final int quantity;
    private final UUID transactionId;

    public SellItemPacket(int tradeIndex, int quantity) {
        this(tradeIndex, quantity, UUID.randomUUID());
    }

    /** Client-generated nonce used only for server-side idempotency. */
    public SellItemPacket(int tradeIndex, int quantity, UUID transactionId) {
        this.tradeIndex = tradeIndex;
        this.quantity = quantity;
        this.transactionId = transactionId == null ? UUID.randomUUID() : transactionId;
    }

    public SellItemPacket(FriendlyByteBuf buffer) {
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
        try {
            TransactionGate.execute(player, "sell", () -> {
                handleServerLocked(player);
                return 1;
            });
        } catch (RuntimeException ignored) {
            // Stale or malformed packets must not escape onto the server
            // thread.  The item remains untouched on every rejected path.
        }
    }

    private void handleServerLocked(ServerPlayer player) {
        if (!(player.containerMenu instanceof SellShopMenu)) {
            return;
        }
        SellShopMenu menu = (SellShopMenu)player.containerMenu;
        if (this.tradeIndex < 0 || this.tradeIndex >= menu.getTrades().size()
                || this.quantity <= 0 || this.quantity > 64) {
            return;
        }
        ShopTrade trade = menu.getTrades().get(this.tradeIndex);
        if (trade == null || trade.getPriceInBronze() <= 0L) {
            return;
        }
        String requestId = this.transactionId.toString();
        String fingerprint = "sell:" + player.getUUID() + ':' + this.tradeIndex + ':' + this.quantity;
        if (ShopRequestJournal.begin(requestId, fingerprint) != ShopRequestJournal.Decision.NEW) {
            return;
        }
        boolean committed = false;
        try {
        ItemStack tradeItem = trade.getItemStack();
        Inventory inventory = player.getInventory();
        int available = countMatching(inventory, tradeItem);
        int toSell = Math.min(this.quantity, available);
        if (toSell <= 0) {
            return;
        }
        long earnings;
        try {
            earnings = MoneyMath.multiplyExact(trade.getPriceInBronze(), (long)toSell, "sell total");
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return;
        }

        // Snapshot the complete inventory so a failed credit can restore the
        // original slot layout without merging into the wrong variant.
        java.util.List<ItemStack> before = snapshot(inventory);
        if (!removeMatching(inventory, tradeItem, toSell)) {
            restoreSnapshot(inventory, before);
            return;
        }
        // Credit must succeed before the sale is committed.  Restore the exact
        // stacks if the currency backend is unavailable or out of capacity.
        if (!LightmansHelper.addMoney((Player)player, earnings, "harishop:sell:" + this.transactionId)) {
            restoreSnapshot(inventory, before);
            return;
        }
        // Removing the exact variant and receiving the payout is the commit
        // point.  Do not permit a replay to sell the same stack twice if
        // market bookkeeping fails afterwards.
        committed = true;
        ShopRequestJournal.complete(requestId, fingerprint);
        MarketDynamics.recordTransaction(tradeItem.getItem(), toSell, false);
        } finally {
            if (!committed) ShopRequestJournal.abort(requestId, fingerprint);
        }
    }

    private static int countMatching(Inventory inventory, ItemStack wanted) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, wanted)) {
                long next = (long)count + stack.getCount();
                count = (int)Math.min(Integer.MAX_VALUE, next);
            }
        }
        return count;
    }

    private static boolean removeMatching(Inventory inventory, ItemStack wanted, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, wanted)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining == 0;
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
