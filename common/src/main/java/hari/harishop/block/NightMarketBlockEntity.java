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

import hari.harishop.client.ShopMenu;
import hari.harishop.init.ModBlocks;
import hari.harishop.shop.CreativeTabShopManager;
import hari.harishop.shop.ItemValueCalculator;
import hari.harishop.shop.ShopTrade;
import java.util.ArrayList;
import java.util.List;
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

public class NightMarketBlockEntity
extends BlockEntity
implements MenuProvider {
    private ResourceLocation selectedTab = null;
    private List<ShopTrade> cachedTrades = null;

    public NightMarketBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlocks.NIGHT_MARKET_BLOCK_ENTITY.get(), pos, state);
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
            // A newly placed night market has no selected tab yet.  It must
            // still be usable by players; generate the safe, priced catalog
            // once and let the normal client-side filters do the navigation.
            this.cachedTrades = this.selectedTab == null
                    ? this.generateTradesForAllItems()
                    : this.generateTradesForTab(this.selectedTab);
        }
        return this.cachedTrades;
    }

    public List<ResourceLocation> getAvailableTabs() {
        return CreativeTabShopManager.getAvailableTabs();
    }

    public String getTabName(ResourceLocation tabId) {
        return CreativeTabShopManager.getTabName(tabId);
    }

    private List<ShopTrade> generateTradesForTab(ResourceLocation tabId) {
        ArrayList<ShopTrade> trades = new ArrayList<ShopTrade>();
        List<ItemStack> items = CreativeTabShopManager.getItemsForTab(tabId);
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            long price = ItemValueCalculator.calculatePrice(stack);
            if (price <= 0L) {
                // Unknown/unsafe variants are deliberately not sold.
                continue;
            }
            trades.add(new ShopTrade(stack, price, -1));
        }
        return trades;
    }

    /** Build a tab-independent catalog for a fresh block.  Exact stack
     * identity is used so variants cannot collapse into one another or be
     * duplicated across tabs. */
    private List<ShopTrade> generateTradesForAllItems() {
        ArrayList<ShopTrade> trades = new ArrayList<ShopTrade>();
        ArrayList<ItemStack> seen = new ArrayList<ItemStack>();
        for (ItemStack stack : CreativeTabShopManager.getAllItems()) {
            if (stack == null || stack.isEmpty()
                    || seen.stream().anyMatch(existing -> ItemStack.isSameItemSameTags(existing, stack))) {
                continue;
            }
            long price = ItemValueCalculator.calculatePrice(stack);
            if (price <= 0L) {
                continue;
            }
            seen.add(stack.copy());
            trades.add(new ShopTrade(stack, price, -1));
        }
        return trades;
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
            return Component.literal((String)this.getTabName(this.selectedTab));
        }
        return Component.literal((String)"Night Market");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShopMenu(containerId, playerInventory, this);
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

