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

import hari.harishop.block.PawnShopBlockEntity;
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

public class SellShopMenu
extends AbstractContainerMenu {
    private final BlockEntity shopEntity;
    private final BlockPos blockPos;
    private List<ShopTrade> trades = new ArrayList<ShopTrade>();
    private List<ResourceLocation> availableTabs = new ArrayList<ResourceLocation>();
    private ResourceLocation selectedTab = null;

    public SellShopMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, SellShopMenu.getPawnShopEntity(playerInventory, extraData));
    }

    public SellShopMenu(int containerId, Inventory playerInventory, PawnShopBlockEntity entity) {
        super((MenuType)ModMenuTypes.SELL_SHOP_MENU.get(), containerId);
        this.shopEntity = entity;
        BlockPos blockPos = this.blockPos = entity != null ? entity.getBlockPos() : BlockPos.ZERO;
        if (entity != null) {
            this.availableTabs = new ArrayList<ResourceLocation>(entity.getAvailableTabs());
            this.selectedTab = entity.getSelectedTab();
            // Include exact variants from the opening player's inventory so
            // pawn quotes are useful for the item actually being held.
            this.trades = new ArrayList<ShopTrade>(entity.getTrades(playerInventory.player));
        }
    }

    private static PawnShopBlockEntity getPawnShopEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockEntity entity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (entity instanceof PawnShopBlockEntity) {
            PawnShopBlockEntity pawn = (PawnShopBlockEntity)entity;
            return pawn;
        }
        return null;
    }

    public List<ShopTrade> getTrades() {
        return this.trades;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
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
        if (blockEntity instanceof PawnShopBlockEntity) {
            PawnShopBlockEntity pawnShop = (PawnShopBlockEntity)blockEntity;
            pawnShop.setSelectedTab(tab);
            this.trades = new ArrayList<ShopTrade>(pawnShop.getTrades());
        }
    }

    public String getTabName(ResourceLocation tabId) {
        BlockEntity blockEntity = this.shopEntity;
        if (blockEntity instanceof PawnShopBlockEntity) {
            PawnShopBlockEntity pawnShop = (PawnShopBlockEntity)blockEntity;
            return pawnShop.getTabName(tabId);
        }
        return tabId != null ? tabId.getPath() : "";
    }

    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public boolean stillValid(Player player) {
        return player.distanceToSqr((double)this.blockPos.getX() + 0.5, (double)this.blockPos.getY() + 0.5, (double)this.blockPos.getZ() + 0.5) <= 64.0;
    }
}

