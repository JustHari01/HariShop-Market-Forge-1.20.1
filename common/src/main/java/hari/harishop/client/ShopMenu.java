/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package hari.harishop.client;

import hari.harishop.block.DailyDealsBlockEntity;
import hari.harishop.block.NightMarketBlockEntity;
import hari.harishop.init.ModMenuTypes;
import hari.harishop.shop.ShopTrade;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ShopMenu
extends AbstractContainerMenu {
    private final BlockEntity shopEntity;
    private final BlockPos blockPos;
    private List<ShopTrade> trades = new ArrayList<ShopTrade>();
    private boolean isDailyDeals = false;
    private String timeUntilRefresh = "";
    private List<ResourceLocation> availableTabs = new ArrayList<ResourceLocation>();
    private ResourceLocation selectedTab = null;
    private boolean isPlayerOP = false;

    public ShopMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, ShopMenu.getBlockEntity(playerInventory, extraData));
    }

    public ShopMenu(int containerId, Inventory playerInventory, NightMarketBlockEntity entity) {
        super((MenuType)ModMenuTypes.SHOP_MENU.get(), containerId);
        this.shopEntity = entity;
        this.blockPos = entity != null ? entity.getBlockPos() : BlockPos.ZERO;
        this.isDailyDeals = false;
        if (entity != null) {
            this.availableTabs = new ArrayList<ResourceLocation>(entity.getAvailableTabs());
            this.selectedTab = entity.getSelectedTab();
            // A newly placed shop has no selected tab.  It must still expose
            // the server-generated default catalog instead of opening empty.
            this.trades = new ArrayList<ShopTrade>(entity.getTrades());
        }
    }

    public ShopMenu(int containerId, Inventory playerInventory, DailyDealsBlockEntity entity) {
        super((MenuType)ModMenuTypes.SHOP_MENU.get(), containerId);
        this.shopEntity = entity;
        this.blockPos = entity != null ? entity.getBlockPos() : BlockPos.ZERO;
        this.isDailyDeals = true;
        if (entity != null) {
            this.trades = new ArrayList<ShopTrade>(entity.getTrades());
            this.timeUntilRefresh = entity.getFormattedTimeUntilRefresh();
        }
    }

    private ShopMenu(int containerId, Inventory playerInventory, BlockEntity entity) {
        super((MenuType)ModMenuTypes.SHOP_MENU.get(), containerId);
        this.shopEntity = entity;
        BlockPos blockPos = this.blockPos = entity != null ? entity.getBlockPos() : BlockPos.ZERO;
        if (entity instanceof NightMarketBlockEntity) {
            NightMarketBlockEntity nightMarket = (NightMarketBlockEntity)entity;
            this.availableTabs = new ArrayList<ResourceLocation>(nightMarket.getAvailableTabs());
            this.selectedTab = nightMarket.getSelectedTab();
            this.trades = new ArrayList<ShopTrade>(nightMarket.getTrades());
            this.isDailyDeals = false;
        } else if (entity instanceof DailyDealsBlockEntity) {
            DailyDealsBlockEntity dailyShop = (DailyDealsBlockEntity)entity;
            this.trades = new ArrayList<ShopTrade>(dailyShop.getTrades());
            this.isDailyDeals = true;
            this.timeUntilRefresh = dailyShop.getFormattedTimeUntilRefresh();
        }
    }

    private static BlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf data) {
        if (data == null) {
            return null;
        }
        try {
            BlockPos pos = data.readBlockPos();
            return playerInventory.player.level().getBlockEntity(pos);
        }
        catch (Exception e) {
            return null;
        }
    }

    public List<ShopTrade> getTrades() {
        return this.trades;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public BlockEntity getShopEntity() {
        return this.shopEntity;
    }

    public boolean isDailyDeals() {
        return this.isDailyDeals;
    }

    public String getTimeUntilRefresh() {
        return this.timeUntilRefresh;
    }

    public List<ResourceLocation> getAvailableTabs() {
        return this.availableTabs;
    }

    public ResourceLocation getSelectedTab() {
        return this.selectedTab;
    }

    public void setSelectedTab(ResourceLocation tab) {
        this.selectedTab = tab;
        BlockEntity blockEntity = this.shopEntity;
        if (blockEntity instanceof NightMarketBlockEntity) {
            NightMarketBlockEntity nightMarket = (NightMarketBlockEntity)blockEntity;
            nightMarket.setSelectedTab(tab);
            this.trades = new ArrayList<ShopTrade>(nightMarket.getTrades());
        }
    }

    public String getTabName(ResourceLocation tabId) {
        BlockEntity blockEntity = this.shopEntity;
        if (blockEntity instanceof NightMarketBlockEntity) {
            NightMarketBlockEntity nightMarket = (NightMarketBlockEntity)blockEntity;
            return nightMarket.getTabName(tabId);
        }
        return tabId != null ? tabId.getPath() : "";
    }

    public boolean isPlayerOP() {
        return this.isPlayerOP;
    }

    public void setPlayerOP(boolean op) {
        this.isPlayerOP = op;
    }

    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public boolean stillValid(Player player) {
        return player.distanceToSqr((double)this.blockPos.getX() + 0.5, (double)this.blockPos.getY() + 0.5, (double)this.blockPos.getZ() + 0.5) <= 64.0;
    }
}

