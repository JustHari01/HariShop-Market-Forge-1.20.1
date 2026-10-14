/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package hari.harishop.block;

import hari.harishop.HariShopMod;
import hari.harishop.client.ShopMenu;
import hari.harishop.config.ShopConfig;
import hari.harishop.init.ModBlocks;
import hari.harishop.shop.CreativeTabShopManager;
import hari.harishop.shop.ItemValueCalculator;
import hari.harishop.shop.ShopTrade;
import hari.harishop.v5.MoneyMath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DailyDealsBlockEntity
extends BlockEntity
implements MenuProvider {
    private List<ShopTrade> trades = new ArrayList<ShopTrade>();
    private long lastRefreshTime = 0L;
    private final Random random = new Random();

    public DailyDealsBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.DAILY_DEALS_BLOCK_ENTITY.get(), pos, state);
    }

    public List<ShopTrade> getTrades() {
        long currentTime = System.currentTimeMillis();
        ShopConfig config = ShopConfig.get();
        long refreshInterval = (long)config.dailyDealsResetHours * 60L * 60L * 1000L;
        if (this.lastRefreshTime == 0L || currentTime - this.lastRefreshTime > refreshInterval) {
            this.refreshTrades();
        }
        return this.trades;
    }

    public void refreshTrades() {
        this.trades.clear();
        ShopConfig config = ShopConfig.get();
        int itemCount = config.dailyDealsItemCount;
        int minDiscount = config.dailyDealsMinDiscount;
        int maxDiscount = config.dailyDealsMaxDiscount;
        CreativeTabShopManager.init();
        List<ItemStack> allItems = CreativeTabShopManager.getAllItems();
        if (allItems.isEmpty()) {
            HariShopMod.LOGGER.warn("DailyDeals: No items available from CreativeTabShopManager!");
            return;
        }
        Collections.shuffle(allItems, this.random);
        int count = Math.min(itemCount, allItems.size());
        for (int i = 0; i < count; ++i) {
            ItemStack stack = allItems.get(i);
            long basePrice = ItemValueCalculator.calculatePrice(stack);
            if (basePrice <= 0L) {
                // A variant without a safe valuation must never become a
                // discounted/free item through the daily-deals path.
                continue;
            }
            int discountPercent = minDiscount + this.random.nextInt(Math.max(1, maxDiscount - minDiscount + 1));
            long remainingPercent = 100L - (long)discountPercent;
            if (remainingPercent <= 0L) {
                continue;
            }
            long discountedPrice;
            try {
                // Purchases are rounded up, so a sale can never mint a free
                // item and the result remains bounded by the money policy.
                discountedPrice = MoneyMath.ceilRatio(basePrice, remainingPercent, 100L);
            } catch (ArithmeticException | IllegalArgumentException ex) {
                continue;
            }
            if (discountedPrice <= 0L) {
                continue;
            }
            int stock = 1 + this.random.nextInt(5);
            this.trades.add(new ShopTrade(stack, discountedPrice, stock));
        }
        this.lastRefreshTime = System.currentTimeMillis();
        this.setChanged();
    }

    public long getTimeUntilRefresh() {
        ShopConfig config = ShopConfig.get();
        long refreshInterval = (long)config.dailyDealsResetHours * 60L * 60L * 1000L;
        long elapsed = System.currentTimeMillis() - this.lastRefreshTime;
        return Math.max(0L, refreshInterval - elapsed);
    }

    public String getFormattedTimeUntilRefresh() {
        long ms = this.getTimeUntilRefresh();
        long hours = ms / 3600000L;
        long minutes = ms % 3600000L / 60000L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0L) {
            return minutes + "m";
        }
        return "< 1m";
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("lastRefresh", this.lastRefreshTime);
        ListTag tradesTag = new ListTag();
        for (ShopTrade trade : this.trades) {
            tradesTag.add(trade.toNbt());
        }
        tag.put("trades", (Tag)tradesTag);
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        this.lastRefreshTime = tag.getLong("lastRefresh");
        this.trades.clear();
        if (tag.contains("trades", 9)) {
            ListTag tradesTag = tag.getList("trades", 10);
            for (int i = 0; i < tradesTag.size(); ++i) {
                ShopTrade trade = ShopTrade.fromNbt(tradesTag.getCompound(i));
                if (trade == null) continue;
                this.trades.add(trade);
            }
        }
    }

    public Component getDisplayName() {
        return Component.literal((String)"Daily Deals");
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShopMenu(containerId, playerInventory, this);
    }
}

