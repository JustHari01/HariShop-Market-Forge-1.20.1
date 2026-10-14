/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.resources.ResourceLocation
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

import hari.harishop.client.SellShopMenu;
import hari.harishop.init.ModBlocks;
import hari.harishop.shop.CreativeTabShopManager;
import hari.harishop.shop.ItemValueCalculator;
import hari.harishop.shop.ShopTrade;
import hari.harishop.v5.ExactStackIdentity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PawnShopBlockEntity
extends BlockEntity
implements MenuProvider {
    public static final double SELL_RATIO = 0.4;
    private ResourceLocation selectedTab = null;
    private List<ShopTrade> cachedTrades = null;

    public PawnShopBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.PAWN_SHOP_BLOCK_ENTITY.get(), pos, state);
    }

    public ResourceLocation getSelectedTab() {
        return this.selectedTab;
    }

    public void setSelectedTab(ResourceLocation tab) {
        this.selectedTab = tab;
        this.cachedTrades = null;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public List<ShopTrade> getTrades() {
        if (this.cachedTrades == null) {
            // A newly placed pawn shop is immediately usable.  Admins can
            // still choose a narrower tab from the selector, but players are
            // never shown an empty shop merely because setup was skipped.
            this.cachedTrades = this.selectedTab == null
                    ? this.generateSellTradesAll()
                    : this.generateSellTrades(this.selectedTab);
        }
        return this.cachedTrades;
    }

    /**
     * Builds the pawn catalog for the opening player.  The static creative-tab
     * catalog contains only default item instances, so enchanted books,
     * potions, damaged tools and other legal variants held by a player were
     * previously invisible in Thu Mua.  Add a bounded set of exact inventory
     * variants to the server-side snapshot; pricing still goes through the
     * fail-closed variant calculator and the packet re-checks exact tags.
     */
    public List<ShopTrade> getTrades(Player player) {
        ArrayList<ShopTrade> result = new ArrayList<ShopTrade>(this.getTrades());
        if (player == null || this.selectedTab != null) {
            return result;
        }
        final int maxInventoryVariants = 256;
        for (int slot = 0; slot < player.getInventory().getContainerSize()
                && result.size() < this.getTrades().size() + maxInventoryVariants; slot++) {
            ItemStack held = player.getInventory().getItem(slot);
            if (held == null || held.isEmpty()
                    || result.stream().anyMatch(existing -> ExactStackIdentity.same(existing.getItemStack(), held))) {
                continue;
            }
            long buyPrice = ItemValueCalculator.calculatePrice(held);
            if (buyPrice <= 0L) {
                // Unknown/custom critical data is intentionally not assigned a
                // guessed positive pawn value.
                continue;
            }
            final long sellPrice;
            try {
                sellPrice = Math.max(1L, Math.multiplyExact(buyPrice, 2L) / 5L);
            } catch (ArithmeticException overflow) {
                continue;
            }
            result.add(new ShopTrade(held.copyWithCount(1), sellPrice, -1));
        }
        return result;
    }

    private List<ShopTrade> generateSellTradesAll() {
        ArrayList<ShopTrade> trades = new ArrayList<ShopTrade>();
        List<ItemStack> seen = new ArrayList<ItemStack>();
        for (ItemStack stack : CreativeTabShopManager.getAllItems()) {
            if (stack == null || stack.isEmpty() || seen.stream().anyMatch(existing -> ExactStackIdentity.same(existing, stack))) continue;
            seen.add(stack.copy());
            long buyPrice = ItemValueCalculator.calculatePrice(stack);
            if (buyPrice <= 0L) continue;
            long sellPrice;
            try {
                sellPrice = Math.max(1L, Math.multiplyExact(buyPrice, 2L) / 5L);
            } catch (ArithmeticException ex) {
                // A malformed/overflowing valuation is not a valid payout.
                continue;
            }
            trades.add(new ShopTrade(stack, sellPrice, -1));
        }
        return trades;
    }

    private List<ShopTrade> generateSellTrades(ResourceLocation tabId) {
        ArrayList<ShopTrade> trades = new ArrayList<ShopTrade>();
        List<ItemStack> items = CreativeTabShopManager.getItemsForTab(tabId);
        for (ItemStack stack : items) {
            long buyPrice = ItemValueCalculator.calculatePrice(stack);
            if (buyPrice <= 0L) {
                // Unknown/admin/unsupported variants are retail-only (or
                // quarantined), never a one-Đồng pawn route.
                continue;
            }
            long sellPrice;
            try {
                sellPrice = Math.max(1L, Math.multiplyExact(buyPrice, 2L) / 5L);
            } catch (ArithmeticException ex) {
                continue;
            }
            if (sellPrice <= 0L) continue;
            trades.add(new ShopTrade(stack, sellPrice, -1));
        }
        return trades;
    }

    public List<ResourceLocation> getAvailableTabs() {
        return CreativeTabShopManager.getAvailableTabs();
    }

    public String getTabName(ResourceLocation tabId) {
        return CreativeTabShopManager.getTabName(tabId);
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.selectedTab != null) {
            tag.putString("selectedTab", this.selectedTab.toString());
        }
    }

    public void load(CompoundTag tag) {
        String tabStr;
        super.load(tag);
        if (tag.contains("selectedTab") && !(tabStr = tag.getString("selectedTab")).isEmpty()) {
            this.selectedTab = new ResourceLocation(tabStr);
            this.cachedTrades = null;
        }
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (this.selectedTab != null) {
            tag.putString("selectedTab", this.selectedTab.toString());
        }
        return tag;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public Component getDisplayName() {
        if (this.selectedTab != null) {
            return Component.literal((String)("Pawn Shop - " + this.getTabName(this.selectedTab)));
        }
        return Component.literal((String)"Pawn Shop");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SellShopMenu(containerId, playerInventory, this);
    }

    public void setCategory(ResourceLocation categoryId) {
        this.setSelectedTab(categoryId);
    }

    public boolean removeItem(int index) {
        if (this.cachedTrades == null || index < 0 || index >= this.cachedTrades.size()) {
            return false;
        }
        this.cachedTrades.remove(index);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
        return true;
    }

    public void addItem(ShopTrade trade) {
        if (this.cachedTrades == null) {
            this.cachedTrades = new ArrayList<ShopTrade>();
        }
        this.cachedTrades.add(trade);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public BlockPos getShopPos() {
        return this.getBlockPos();
    }
}

