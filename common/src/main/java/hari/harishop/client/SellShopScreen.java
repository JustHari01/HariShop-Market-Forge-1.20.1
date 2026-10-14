/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import hari.harishop.client.SellShopMenu;
import hari.harishop.network.ModPackets;
import hari.harishop.network.SellItemPacket;
import hari.harishop.network.SetShopCategoryPacket;
import hari.harishop.shop.EconomyBalancer;
import hari.harishop.shop.ShopTrade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;

public class SellShopScreen
extends AbstractContainerScreen<SellShopMenu> {
    private static final int TRADE_HEIGHT = 22;
    private static final int VISIBLE_TRADES = 6;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean isOP = false;
    private boolean showTabSelector = false;
    private int tabScrollOffset = 0;
    private EditBox searchBox;
    private List<ShopTrade> filteredTrades = new ArrayList<ShopTrade>();
    private int sortMode = 0;
    private EditBox categorySearchBox;
    private List<ResourceLocation> filteredCategories = new ArrayList<ResourceLocation>();
    private String categorySearchQuery = "";
    private static Item coinCopper;
    private static Item coinIron;
    private static Item coinGold;
    private static Item coinEmerald;
    private static Item coinDiamond;
    private static Item coinNetherite;
    private ResourceLocation pendingTab = null;
    private String statusMessage = null;
    private long statusMessageTime = 0L;
    private long initialRefreshSeed = 0L;
    private boolean isDraggingScrollbar = false;

    public SellShopScreen(SellShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 200;
        SellShopScreen.initCoinItems();
    }

    private static void initCoinItems() {
        if (coinCopper != null) {
            return;
        }
        coinCopper = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation("lightmanscurrency", "coin_copper"));
        coinIron = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation("lightmanscurrency", "coin_iron"));
        coinGold = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation("lightmanscurrency", "coin_gold"));
        coinEmerald = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation("lightmanscurrency", "coin_emerald"));
        coinDiamond = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation("lightmanscurrency", "coin_diamond"));
        coinNetherite = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation("lightmanscurrency", "coin_netherite"));
    }

    protected void init() {
        super.init();
        this.isOP = Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2);
        this.filteredTrades = new ArrayList<ShopTrade>(((SellShopMenu)this.menu).getTrades());
        this.applySorting();
        this.initialRefreshSeed = EconomyBalancer.getCurrentShopSeed();
        if (((SellShopMenu)this.menu).getSelectedTab() == null && this.isOP) {
            this.showTabSelector = true;
            this.categorySearchBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 20, 180, 14, (Component)Component.literal((String)""));
            this.categorySearchBox.setMaxLength(30);
            this.categorySearchBox.setBordered(true);
            this.categorySearchBox.setTextColor(0xFFFFFF);
            this.categorySearchBox.setHint((Component)Component.literal((String)"Search categories..."));
            this.categorySearchBox.setResponder(this::onCategorySearch);
            this.addRenderableWidget(this.categorySearchBox);
            this.filteredCategories = new ArrayList<ResourceLocation>(this.getUniqueTabs());
        } else {
            this.showTabSelector = false;
            this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 20, 70, 14, (Component)Component.literal((String)""));
            this.searchBox.setMaxLength(20);
            this.searchBox.setBordered(true);
            this.searchBox.setTextColor(0xFFFFFF);
            this.searchBox.setResponder(this::onSearch);
            this.addRenderableWidget(this.searchBox);
        }
    }

    private void onCategorySearch(String query) {
        this.categorySearchQuery = query.toLowerCase();
        this.tabScrollOffset = 0;
        List<ResourceLocation> allTabs = this.getUniqueTabs();
        this.filteredCategories = query.isEmpty() ? new ArrayList<ResourceLocation>(allTabs) : allTabs.stream().filter(tab -> {
            String name = ((SellShopMenu)this.menu).getTabName((ResourceLocation)tab).toLowerCase();
            String id = tab.toString().toLowerCase();
            return name.contains(this.categorySearchQuery) || id.contains(this.categorySearchQuery);
        }).collect(Collectors.toList());
    }

    private void onSearch(String query) {
        this.scrollOffset = 0;
        this.selectedIndex = -1;
        if (query.isEmpty()) {
            this.filteredTrades = new ArrayList<ShopTrade>(((SellShopMenu)this.menu).getTrades());
        } else {
            String q = query.toLowerCase();
            this.filteredTrades = ((SellShopMenu)this.menu).getTrades().stream().filter(t -> t.matchesSearch(q)).collect(Collectors.toList());
        }
        this.applySorting();
    }

    private void applySorting() {
        switch (this.sortMode) {
            case 1: {
                this.filteredTrades.sort(Comparator.comparingLong(ShopTrade::getPriceInBronze));
                break;
            }
            case 2: {
                this.filteredTrades.sort((a, b) -> Long.compare(b.getPriceInBronze(), a.getPriceInBronze()));
                break;
            }
            case 3: {
                this.filteredTrades.sort((a, b) -> a.getItemStack().getHoverName().getString().compareToIgnoreCase(b.getItemStack().getHoverName().getString()));
            }
        }
    }

    private String getSortLabel() {
        return switch (this.sortMode) {
            case 1 -> "$ \u2191";
            case 2 -> "$ \u2193";
            case 3 -> "A-Z";
            default -> "Sort";
        };
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        long currentSeed = EconomyBalancer.getCurrentShopSeed();
        if (currentSeed != this.initialRefreshSeed) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage((Component)Component.literal((String)"\u00a7a\ud83d\udcb0 Pawn Shop \u0111\u00e3 \u0111\u01b0\u1ee3c refresh! Gi\u00e1 mua b\u00e1n \u0111\u00e3 thay \u0111\u1ed5i. Vui l\u00f2ng m\u1edf l\u1ea1i."), false);
            }
            this.onClose();
            return;
        }
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        if (this.showTabSelector) {
            this.renderTabs(g, mx, my);
        } else {
            this.renderItems(g, mx, my);
            this.renderScrollButtons(g, mx, my);
            this.renderFilterButton(g, mx, my);
            if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredTrades.size()) {
                this.renderDetail(g, mx, my);
            }
        }
        this.renderCountdownTimer(g);
        this.renderTooltip(g, mx, my);
    }

    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -15062502);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 2, -12277180);
        g.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.imageHeight, -12277180);
        g.fill(this.leftPos, this.topPos + this.imageHeight - 2, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -14522846);
        g.fill(this.leftPos + this.imageWidth - 2, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -14522846);
        if (!this.showTabSelector) {
            g.fill(this.leftPos + 5, this.topPos + 38, this.leftPos + 115, this.topPos + 175, -14338779);
            g.fill(this.leftPos + 120, this.topPos + 18, this.leftPos + 295, this.topPos + 195, -13812435);
        }
    }

    private void renderCountdownTimer(GuiGraphics g) {
        boolean urgent;
        long timeRemaining = EconomyBalancer.getTimeUntilShopRefresh();
        String label = "\u00a7a\ud83d\udcb0 Sell Prices Update: ";
        String timeStr = EconomyBalancer.formatTimeRemaining(timeRemaining);
        boolean bl = urgent = timeRemaining < 1800000L;
        int textColor = urgent ? (System.currentTimeMillis() / 500L % 2L == 0L ? -43691 : -22016) : -11141291;
        String fullText = label + "\u00a7f" + timeStr;
        int textWidth = this.font.width(fullText.replaceAll("\u00a7.", ""));
        int x = this.leftPos + this.imageWidth / 2;
        int y = this.topPos - 18;
        int bgX = x - textWidth / 2 - 8;
        int bgY = y - 3;
        int bgW = textWidth + 16;
        int bgH = 14;
        g.fill(bgX, bgY, bgX + bgW, bgY + bgH, -585487846);
        int borderColor = urgent ? -1426102784 : -1438340540;
        g.fill(bgX, bgY, bgX + bgW, bgY + 1, borderColor);
        g.fill(bgX, bgY + bgH - 1, bgX + bgW, bgY + bgH, borderColor);
        g.fill(bgX, bgY, bgX + 1, bgY + bgH, borderColor);
        g.fill(bgX + bgW - 1, bgY, bgX + bgW, bgY + bgH, borderColor);
        g.drawString(this.font, fullText, x - textWidth / 2, y, textColor, true);
    }

    private void renderFilterButton(GuiGraphics g, int mx, int my) {
        int x = this.leftPos + 82;
        int y = this.topPos + 20;
        int w = 30;
        int h = 14;
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hover ? -12294588 : -13413069);
        g.drawCenteredString(this.font, this.getSortLabel(), x + w / 2, y + 3, 0xFFFFFF);
    }

    private void renderScrollButtons(GuiGraphics g, int mx, int my) {
        boolean hoverDown;
        boolean hoverUp;
        int x = this.leftPos + 103;
        int upY = this.topPos + 38;
        int downY = this.topPos + 163;
        int w = 12;
        int h = 12;
        boolean canUp = this.scrollOffset > 0;
        boolean bl = hoverUp = mx >= x && mx < x + w && my >= upY && my < upY + h;
        g.fill(x, upY, x + w, upY + h, canUp ? (hoverUp ? -12294588 : -13413069) : -14540254);
        g.drawCenteredString(this.font, "\u25b2", x + w / 2, upY + 2, canUp ? 0xFFFFFF : 0x555555);
        boolean canDown = this.scrollOffset < Math.max(0, this.filteredTrades.size() - 6);
        boolean bl2 = hoverDown = mx >= x && mx < x + w && my >= downY && my < downY + h;
        g.fill(x, downY, x + w, downY + h, canDown ? (hoverDown ? -12294588 : -13413069) : -14540254);
        g.drawCenteredString(this.font, "\u25bc", x + w / 2, downY + 2, canDown ? 0xFFFFFF : 0x555555);

        // Draw scroll track & thumb
        int trackStart = upY + h + 1;
        int trackEnd = downY - 1;
        int maxScroll = Math.max(0, this.filteredTrades.size() - 6);
        g.fill(x + 1, trackStart, x + w - 1, trackEnd, -14540254); // Dark track background
        
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (int)((float)(trackEnd - trackStart) * (6.0f / (float)this.filteredTrades.size())));
            float scrollPercent = (float)this.scrollOffset / (float)maxScroll;
            int thumbY = trackStart + (int)(scrollPercent * (float)(trackEnd - trackStart - thumbH));
            boolean thumbHover = mx >= x + 1 && mx < x + w - 1 && my >= thumbY && my < thumbY + thumbH;
            g.fill(x + 1, thumbY, x + w - 1, thumbY + thumbH, this.isDraggingScrollbar ? -5609950 : (thumbHover ? -12294588 : -13413069));
        }
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        int idx;
        if (this.pendingTab != null) {
            this.renderPendingSetUI(g, mx, my);
            return;
        }
        List<ResourceLocation> tabs = this.categorySearchQuery.isEmpty() ? this.getUniqueTabs() : this.filteredCategories;
        int totalCount = this.getUniqueTabs().size();
        String header = "\u00a76\u00a7lSelect Category to Sell: \u00a77(" + tabs.size() + "/" + totalCount + ")";
        g.drawString(this.font, header, this.leftPos + 10, this.topPos + 38, 0xFFFFFF, false);
        int cols = 2;
        int btnW = 135;
        int btnH = 20;
        int gap = 4;
        int startX = this.leftPos + 10;
        int startY = this.topPos + 52;
        int maxVisible = 12;
        for (int i = 0; i < Math.min(tabs.size() - this.tabScrollOffset, maxVisible) && (idx = this.tabScrollOffset + i) < tabs.size(); ++i) {
            ResourceLocation tabId = tabs.get(idx);
            Object name = ((SellShopMenu)this.menu).getTabName(tabId);
            if (((String)name).length() > 18) {
                name = ((String)name).substring(0, 16) + "..";
            }
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (btnW + gap);
            int y = startY + row * (btnH + gap);
            boolean hover = mx >= x && mx < x + btnW && my >= y && my < y + btnH;
            int bg = hover ? -5609950 : -10075102;
            g.fill(x, y, x + btnW, y + btnH, bg);
            g.fill(x + 1, y + 1, x + btnW - 1, y + btnH - 1, hover ? -7842543 : -11193583);
            g.drawString(this.font, (String)name, x + 6, y + 6, 0xFFFFFF, false);
        }
        if (tabs.size() > maxVisible) {
            String scrollInfo = "Page " + (this.tabScrollOffset / maxVisible + 1) + "/" + (tabs.size() + maxVisible - 1) / maxVisible;
            g.drawCenteredString(this.font, scrollInfo, this.leftPos + this.imageWidth / 2, this.topPos + this.imageHeight - 30, 0x888888);
        }
    }

    private List<ResourceLocation> getUniqueTabs() {
        LinkedHashMap<String, ResourceLocation> unique = new LinkedHashMap<String, ResourceLocation>();
        for (ResourceLocation tabId : ((SellShopMenu)this.menu).getAvailableTabs()) {
            String name = ((SellShopMenu)this.menu).getTabName(tabId);
            if (unique.containsKey(name)) continue;
            unique.put(name, tabId);
        }
        return new ArrayList<ResourceLocation>(unique.values());
    }

    private void renderPendingSetUI(GuiGraphics g, int mx, int my) {
        int centerX = this.leftPos + this.imageWidth / 2;
        int y = this.topPos + 50;
        g.drawCenteredString(this.font, "\u00a7e\u00a7lConfirm Category", centerX, y, 0xFFFFFF);
        String catName = ((SellShopMenu)this.menu).getTabName(this.pendingTab);
        g.drawCenteredString(this.font, "\u00a7f" + catName, centerX, y += 20, 0xFFFFFF);
        int setBtnX = centerX - 70;
        int setBtnY = y += 30;
        int btnW = 60;
        int btnH = 20;
        boolean setHover = mx >= setBtnX && mx < setBtnX + btnW && my >= setBtnY && my < setBtnY + btnH;
        g.fill(setBtnX, setBtnY, setBtnX + btnW, setBtnY + btnH, setHover ? -12277180 : -14514142);
        g.drawCenteredString(this.font, "\u00a7a\u00a7lSET", setBtnX + btnW / 2, setBtnY + 6, 0xFFFFFF);
        int cancelBtnX = centerX + 10;
        boolean cancelHover = mx >= cancelBtnX && mx < cancelBtnX + btnW && my >= setBtnY && my < setBtnY + btnH;
        g.fill(cancelBtnX, setBtnY, cancelBtnX + btnW, setBtnY + btnH, cancelHover ? -5618620 : -7855582);
        g.drawCenteredString(this.font, "\u00a7cCANCEL", cancelBtnX + btnW / 2, setBtnY + 6, 0xFFFFFF);
        if (this.statusMessage != null && System.currentTimeMillis() - this.statusMessageTime < 3000L) {
            g.drawCenteredString(this.font, this.statusMessage, centerX, y + 40, 0xFFFFFF);
        }
    }

    private void renderItems(GuiGraphics g, int mx, int my) {
        int idx;
        int listX = this.leftPos + 7;
        int listY = this.topPos + 40;
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 0; i < 6 && (idx = this.scrollOffset + i) < this.filteredTrades.size(); ++i) {
            boolean hasItem;
            ShopTrade trade = this.filteredTrades.get(idx);
            int y = listY + i * 22;
            boolean hover = mx >= listX && mx < listX + 94 && my >= y && my < y + 22 - 2;
            boolean sel = idx == this.selectedIndex;
            boolean bl = hasItem = player != null && this.hasItemInInventory((Player)player, trade.getItemStack());
            int bg = sel ? (hasItem ? -15047142 : -11908582) : (hover ? -12298940 : -14009814);
            g.fill(listX, y, listX + 94, y + 22 - 2, bg);
            if (sel) {
                g.fill(listX, y, listX + 94, y + 1, hasItem ? -12255420 : -188);
                g.fill(listX, y + 22 - 3, listX + 94, y + 22 - 2, hasItem ? -12255420 : -188);
            }
            ItemStack stack = trade.getItemStack();
            g.renderItem(stack, listX + 2, y + 2);
            Object name = stack.getHoverName().getString();
            if (((String)name).length() > 9) {
                name = ((String)name).substring(0, 7) + "..";
            }
            g.drawString(this.font, (String)name, listX + 21, y + 2, hasItem ? 0xFFFFFF : 0x888888, false);
            this.renderPriceWithIcon(g, listX + 21, y + 12, trade.getPriceInBronze(), true);
        }
    }

    private boolean hasItemInInventory(Player player, ItemStack wanted) {
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, wanted)) continue;
            return true;
        }
        return false;
    }

    private int countItemInInventory(Player player, ItemStack wanted) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, wanted)) continue;
            count = (int)Math.min(Integer.MAX_VALUE, (long)count + stack.getCount());
        }
        return count;
    }

    private void renderPriceWithIcon(GuiGraphics g, int x, int y, long price, boolean positive) {
        String priceText;
        Item coinIcon = coinCopper;
        if (price >= 100000L && coinNetherite != null) {
            coinIcon = coinNetherite;
            priceText = String.format("%.1f", (double)price / 100000.0);
        } else if (price >= 10000L && coinDiamond != null) {
            coinIcon = coinDiamond;
            priceText = String.format("%.1f", (double)price / 10000.0);
        } else if (price >= 1000L && coinEmerald != null) {
            coinIcon = coinEmerald;
            priceText = String.format("%.1f", (double)price / 1000.0);
        } else if (price >= 100L && coinGold != null) {
            coinIcon = coinGold;
            priceText = String.format("%.1f", (double)price / 100.0);
        } else if (price >= 10L && coinIron != null) {
            coinIcon = coinIron;
            priceText = String.valueOf(price / 10L);
        } else {
            priceText = String.valueOf(price);
        }
        if (coinIcon != null) {
            g.renderItem(new ItemStack((ItemLike)coinIcon), x - 2, y - 4);
        }
        g.drawString(this.font, "+" + priceText, x + 15, y, -11141291, false);
    }

    private void renderDetail(GuiGraphics g, int mx, int my) {
        boolean btnHover;
        ShopTrade trade = this.filteredTrades.get(this.selectedIndex);
        ItemStack stack = trade.getItemStack();
        long sellPrice = trade.getPriceInBronze();
        LocalPlayer player = Minecraft.getInstance().player;
        boolean hasItem = player != null && this.hasItemInInventory((Player)player, trade.getItemStack());
        int itemCount = player != null ? this.countItemInInventory((Player)player, trade.getItemStack()) : 0;
        int x = this.leftPos + 125;
        int y = this.topPos + 22;
        if (((SellShopMenu)this.menu).getSelectedTab() != null) {
            g.drawString(this.font, "\u00a7a" + ((SellShopMenu)this.menu).getTabName(((SellShopMenu)this.menu).getSelectedTab()), x, y, 0xFFFFFF, false);
            y += 16;
        }
        g.fill(x, y, x + 24, y + 24, -12953030);
        g.renderItem(stack, x + 4, y + 4);
        Object itemName = stack.getHoverName().getString();
        if (((String)itemName).length() > 20) {
            itemName = ((String)itemName).substring(0, 18) + "..";
        }
        g.drawString(this.font, (String)itemName, x + 30, y + 8, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a77You have: \u00a7f" + itemCount, x, y += 30, 0xFFFFFF, false);
        g.drawString(this.font, "\u00a77You get:", x, y += 14, 0xAAAAAA, false);
        this.renderFullPrice(g, x, y += 12, sellPrice);
        int btnX = x + 30;
        int btnY = y += 22;
        int btnW = 80;
        int btnH = 22;
        boolean canSell = hasItem;
        boolean bl = btnHover = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
        if (canSell) {
            g.fill(btnX - 3, btnY - 3, btnX + btnW + 3, btnY + btnH + 3, -12255420);
        }
        boolean isShift = SellShopScreen.hasShiftDown();
        int quantityToSell = isShift ? Math.min(64, itemCount) : 1;
        if (quantityToSell < 1) quantityToSell = 1;
        
        int btnBg = canSell ? (btnHover ? -13391309 : -14514142) : -11184862;
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
        String btnText = !hasItem ? "\u00a7eNo items" : (isShift ? "\u00a7a\u00a7lSELL x" + quantityToSell : "\u00a7a\u00a7lSELL");
        g.drawCenteredString(this.font, btnText, btnX + btnW / 2, btnY + 7, 0xFFFFFF);
        if (this.isOP && ((SellShopMenu)this.menu).getSelectedTab() != null) {
            int opY = btnY + 28;
            boolean changeHover = mx >= x && mx < x + 60 && my >= opY && my < opY + 16;
            g.fill(x, opY, x + 60, opY + 16, changeHover ? -12294588 : -13413069);
            g.drawString(this.font, "\u00a77Change", x + 8, opY + 4, 0xFFFFFF, false);
        }
    }

    private void renderFullPrice(GuiGraphics g, int x, int y, long price) {
        long count;
        int offset = 0;
        long remaining = price;
        int color = 0x55FF55;
        if (remaining >= 100000L && coinNetherite != null) {
            count = remaining / 100000L;
            remaining %= 100000L;
            g.renderItem(new ItemStack((ItemLike)coinNetherite), x + offset, y - 2);
            g.drawString(this.font, String.valueOf(count), x + offset + 18, y + 2, color, false);
            offset += 32;
        }
        if (remaining >= 10000L && coinDiamond != null) {
            count = remaining / 10000L;
            remaining %= 10000L;
            g.renderItem(new ItemStack((ItemLike)coinDiamond), x + offset, y - 2);
            g.drawString(this.font, String.valueOf(count), x + offset + 18, y + 2, color, false);
            offset += 32;
        }
        if (remaining >= 1000L && coinEmerald != null) {
            count = remaining / 1000L;
            remaining %= 1000L;
            g.renderItem(new ItemStack((ItemLike)coinEmerald), x + offset, y - 2);
            g.drawString(this.font, String.valueOf(count), x + offset + 18, y + 2, color, false);
            offset += 32;
        }
        if (remaining >= 100L && coinGold != null) {
            count = remaining / 100L;
            remaining %= 100L;
            g.renderItem(new ItemStack((ItemLike)coinGold), x + offset, y - 2);
            g.drawString(this.font, String.valueOf(count), x + offset + 18, y + 2, color, false);
            offset += 32;
        }
        if (remaining >= 10L && coinIron != null) {
            count = remaining / 10L;
            remaining %= 10L;
            g.renderItem(new ItemStack((ItemLike)coinIron), x + offset, y - 2);
            g.drawString(this.font, String.valueOf(count), x + offset + 18, y + 2, color, false);
            offset += 32;
        }
        if (remaining > 0L && coinCopper != null) {
            g.renderItem(new ItemStack((ItemLike)coinCopper), x + offset, y - 2);
            g.drawString(this.font, String.valueOf(remaining), x + offset + 18, y + 2, color, false);
        }
    }

    protected void renderLabels(GuiGraphics g, int mx, int my) {
        Object title = this.showTabSelector ? "\u00a7e\u00a7l\ud83d\udcb0 Pawn Shop - Setup" : (((SellShopMenu)this.menu).getSelectedTab() != null ? "\u00a7e\ud83d\udcb0 Pawn Shop - \u00a7f" + ((SellShopMenu)this.menu).getTabName(((SellShopMenu)this.menu).getSelectedTab()) : "\u00a7e\u00a7l\ud83d\udcb0 Pawn Shop");
        g.drawString(this.font, (String)title, 8, 6, 0xFFFFFF, false);
        if (!this.showTabSelector && !this.filteredTrades.isEmpty()) {
            int totalItems = this.filteredTrades.size();
            int currentPage = this.scrollOffset / 6 + 1;
            int totalPages = Math.max(1, (int)Math.ceil((double)totalItems / 6.0));
            int infoColor = -8858;
            String pageInfo = String.format("\u00a77[%d/%d] \u00a78| \u00a77%d items", currentPage, totalPages, totalItems);
            g.drawString(this.font, pageInfo, 8, 178, infoColor, false);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int idx;
        if (this.showTabSelector && this.pendingTab != null) {
            int centerX = this.leftPos + this.imageWidth / 2;
            int btnY = this.topPos + 100;
            int btnW = 60;
            int btnH = 20;
            int setBtnX = centerX - 70;
            if (mx >= (double)setBtnX && mx < (double)(setBtnX + btnW) && my >= (double)btnY && my < (double)(btnY + btnH)) {
                BlockPos pos = ((SellShopMenu)this.menu).getBlockPos();
                if (pos != null) {
                    ModPackets.sendToServer(new SetShopCategoryPacket(pos, this.pendingTab));
                }
                ((SellShopMenu)this.menu).setSelectedTab(this.pendingTab);
                this.showTabSelector = false;
                this.pendingTab = null;
                this.filteredTrades = new ArrayList<ShopTrade>(((SellShopMenu)this.menu).getTrades());
                this.applySorting();
                this.statusMessage = "\u00a7a\u2713 Category set!";
                this.statusMessageTime = System.currentTimeMillis();
                this.playClick();
                this.rebuildWidgets();
                return true;
            }
            int cancelBtnX = centerX + 10;
            if (mx >= (double)cancelBtnX && mx < (double)(cancelBtnX + btnW) && my >= (double)btnY && my < (double)(btnY + btnH)) {
                this.pendingTab = null;
                this.playClick();
                return true;
            }
            return super.mouseClicked(mx, my, btn);
        }
        if (this.showTabSelector) {
            int idx2;
            List<ResourceLocation> tabs = this.getUniqueTabs();
            int cols = 2;
            int btnW = 135;
            int btnH = 20;
            int gap = 4;
            int startX = this.leftPos + 10;
            int startY = this.topPos + 52;
            int maxVisible = 12;
            for (int i = 0; i < Math.min(tabs.size() - this.tabScrollOffset, maxVisible) && (idx2 = this.tabScrollOffset + i) < tabs.size(); ++i) {
                int col = i % cols;
                int row = i / cols;
                int x = startX + col * (btnW + gap);
                int y = startY + row * (btnH + gap);
                if (!(mx >= (double)x) || !(mx < (double)(x + btnW)) || !(my >= (double)y) || !(my < (double)(y + btnH))) continue;
                this.pendingTab = tabs.get(idx2);
                this.playClick();
                return true;
            }
            return super.mouseClicked(mx, my, btn);
        }
        int filterX = this.leftPos + 82;
        int filterY = this.topPos + 20;
        if (mx >= (double)filterX && mx < (double)(filterX + 30) && my >= (double)filterY && my < (double)(filterY + 14)) {
            this.sortMode = (this.sortMode + 1) % 4;
            this.applySorting();
            this.playClick();
            return true;
        }
        int scrollBtnX = this.leftPos + 103;
        if (mx >= (double)scrollBtnX && mx < (double)(scrollBtnX + 12)) {
            if (my >= (double)(this.topPos + 38) && my < (double)(this.topPos + 50) && this.scrollOffset > 0) {
                --this.scrollOffset;
                this.playClick();
                return true;
            }
            if (my >= (double)(this.topPos + 163) && my < (double)(this.topPos + 175) && this.scrollOffset < this.filteredTrades.size() - 6) {
                ++this.scrollOffset;
                this.playClick();
                return true;
            }
            
            // Allow clicking in the scroll track to jump
            int trackStart = this.topPos + 51;
            int trackEnd = this.topPos + 162;
            int maxScroll = Math.max(0, this.filteredTrades.size() - 6);
            if (maxScroll > 0 && my >= (double)trackStart && my < (double)trackEnd) {
                this.isDraggingScrollbar = true;
                double percent = (my - (double)trackStart) / (double)(trackEnd - trackStart);
                this.scrollOffset = (int)Math.round(percent * (double)maxScroll);
                this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset));
                return true;
            }
        }
        int listX = this.leftPos + 7;
        int listY = this.topPos + 40;
        for (int i = 0; i < 6 && (idx = this.scrollOffset + i) < this.filteredTrades.size(); ++i) {
            int y = listY + i * 22;
            if (!(mx >= (double)listX) || !(mx < (double)(listX + 94)) || !(my >= (double)y) || !(my < (double)(y + 22 - 2))) continue;
            this.selectedIndex = idx;
            this.playClick();
            return true;
        }
        if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredTrades.size()) {
            ShopTrade trade = this.filteredTrades.get(this.selectedIndex);
            long sellPrice = trade.getPriceInBronze();
            LocalPlayer player = Minecraft.getInstance().player;
            int detailX = this.leftPos + 125;
            int detailY = this.topPos + 22;
            if (((SellShopMenu)this.menu).getSelectedTab() != null) {
                detailY += 16;
            }
            int btnX = detailX + 30;
            int btnY = detailY += 78;
            if (mx >= (double)btnX && mx < (double)(btnX + 80) && my >= (double)btnY && my < (double)(btnY + 22)) {
                int itemCount = player != null ? this.countItemInInventory((Player)player, trade.getItemStack()) : 0;
                if (itemCount > 0) {
                    int quantity = SellShopScreen.hasShiftDown() ? Math.min(64, itemCount) : 1;
                    if (quantity < 1) quantity = 1;
                    int serverIndex = ((SellShopMenu)this.menu).getTrades().indexOf(trade);
                    if (serverIndex < 0) {
                        this.statusMessage = "\u00a7cTrade expired; reopen shop";
                        this.statusMessageTime = System.currentTimeMillis();
                        this.playFail();
                    } else {
                        // The server is authoritative.  Avoid client-side
                        // inventory/money prediction that could desync after a
                        // rejected or replayed packet.
                        ModPackets.sendToServer(new SellItemPacket(serverIndex, quantity));
                        this.statusMessage = "\u00a7eSale sent; balance will refresh";
                        this.statusMessageTime = System.currentTimeMillis();
                        this.playSuccess();
                    }
                } else {
                    this.playFail();
                }
                return true;
            }
            if (this.isOP && ((SellShopMenu)this.menu).getSelectedTab() != null) {
                int opY = btnY + 28;
                if (mx >= (double)detailX && mx < (double)(detailX + 60) && my >= (double)opY && my < (double)(opY + 16)) {
                    this.showTabSelector = true;
                    this.selectedIndex = -1;
                    this.filteredTrades.clear();
                    BlockPos pos = ((SellShopMenu)this.menu).getBlockPos();
                    if (pos != null) {
                        ModPackets.sendToServer(new SetShopCategoryPacket(pos, null));
                    }
                    ((SellShopMenu)this.menu).setSelectedTab(null);
                    this.playClick();
                    this.rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean removeItemFromInventory(Player player, ItemStack wanted, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, wanted)) continue;
            int toRemove = Math.min(amount, stack.getCount());
            stack.shrink(toRemove);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        this.isDraggingScrollbar = false;
        return super.mouseReleased(mx, my, btn);
    }

    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (this.isDraggingScrollbar) {
            int trackStart = this.topPos + 51;
            int trackEnd = this.topPos + 162;
            int maxScroll = Math.max(0, this.filteredTrades.size() - 6);
            if (maxScroll > 0) {
                double percent = (my - (double)trackStart) / (double)(trackEnd - trackStart);
                this.scrollOffset = (int)Math.round(percent * (double)maxScroll);
                this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset));
            }
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.showTabSelector) {
            List<ResourceLocation> tabs = this.getUniqueTabs();
            int maxScroll = Math.max(0, tabs.size() - 14);
            this.tabScrollOffset = delta > 0.0 ? Math.max(0, this.tabScrollOffset - 2) : Math.min(maxScroll, this.tabScrollOffset + 2);
            return true;
        }
        int maxScroll = Math.max(0, this.filteredTrades.size() - 6);
        this.scrollOffset = delta > 0.0 ? Math.max(0, this.scrollOffset - 1) : Math.min(maxScroll, this.scrollOffset + 1);
        return true;
    }

    public boolean keyPressed(int key, int scan, int mod) {
        if (this.searchBox != null && this.searchBox.keyPressed(key, scan, mod)) {
            return true;
        }
        if (this.searchBox != null && this.searchBox.isFocused() && key != 256) {
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    public boolean charTyped(char c, int mod) {
        if (this.searchBox != null && this.searchBox.charTyped(c, mod)) {
            return true;
        }
        return super.charTyped(c, mod);
    }

    private void playClick() {
        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
    }

    private void playSuccess() {
        Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private void playFail() {
        Minecraft.getInstance().player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
    }
}

