/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import hari.harishop.block.NightMarketBlockEntity;
import hari.harishop.block.DailyDealsBlockEntity;
import hari.harishop.block.PawnShopBlockEntity;
import hari.harishop.config.ShopConfig;
import hari.harishop.config.TagPricing;
import hari.harishop.init.ModBlocks;
import hari.harishop.shop.BaseItemValues;
import hari.harishop.shop.CreativeTabShopManager;
import hari.harishop.shop.ItemValueCalculator;
import hari.harishop.shop.OreGenAnalyzer;
import hari.harishop.shop.RecipeAnalyzer;
import hari.harishop.shop.ShopEditHistory;
import hari.harishop.shop.VillagerTradeAnalyzer;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.registries.BuiltInRegistries;

public class HariShopCommand {
    private static final SuggestionProvider<CommandSourceStack> TAB_SUGGESTIONS = (context, builder) -> {
        List tabNames = CreativeTabShopManager.getAvailableTabs().stream().map(id -> id.toString().replace(":", "_")).collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(tabNames, (SuggestionsBuilder)builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"harishop").requires(source -> source.hasPermission(2))).then(Commands.literal((String)"reload").executes(HariShopCommand::reloadConfig))).then(Commands.literal((String)"refresh").executes(HariShopCommand::refreshAllShops))).then(Commands.literal((String)"tabs").executes(HariShopCommand::listTabs))).then(Commands.literal((String)"price").executes(HariShopCommand::debugPrice))).then(Commands.literal((String)"settab").then(Commands.argument((String)"tab", (ArgumentType)StringArgumentType.string()).suggests(TAB_SUGGESTIONS).executes(HariShopCommand::setTab)))).then(((LiteralArgumentBuilder)Commands.literal((String)"history").executes(HariShopCommand::showAllHistory)).then(Commands.argument((String)"category", (ArgumentType)StringArgumentType.string()).suggests(TAB_SUGGESTIONS).executes(HariShopCommand::showCategoryHistory)))).then(Commands.literal((String)"restore").then(Commands.argument((String)"category", (ArgumentType)StringArgumentType.string()).suggests(TAB_SUGGESTIONS).then(Commands.argument((String)"index", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(HariShopCommand::restoreItem))))).then(Commands.literal((String)"clearhistory").executes(HariShopCommand::clearAllHistory)));
        // The Forge backport has no NeoForge V5 command dispatcher, but it
        // must still expose one audited distribution path for the three
        // HariShop blocks and ProClaim's real land office.  Keep the branch
        // operator-only and resolve the ProClaim BlockItem from the registry;
        // never create a look-alike block when ProClaim is absent.
        dispatcher.register(Commands.literal("harishop")
                .requires(source -> source.hasPermission(2))
                .then(giveBlocksBranch()));
        dispatcher.register(giveBlocksBranch());

        // Forge 1.20.1 keeps the same user-facing command aliases as the
        // NeoForge build. The command never trusts a client-supplied block
        // position: it only opens a real shop block currently under the
        // player's crosshair and fails closed otherwise.
        dispatcher.register(Commands.literal("shop").executes(ctx -> openLookedAt(ctx, ShopKind.RETAIL)));
        dispatcher.register(Commands.literal("cuahang").executes(ctx -> openLookedAt(ctx, ShopKind.RETAIL)));
        dispatcher.register(Commands.literal("pawn").executes(ctx -> openLookedAt(ctx, ShopKind.PAWN)));
        dispatcher.register(Commands.literal("thumua").executes(ctx -> openLookedAt(ctx, ShopKind.PAWN)));
        dispatcher.register(Commands.literal("nightmarket").executes(ctx -> openLookedAt(ctx, ShopKind.NIGHT)));
        dispatcher.register(Commands.literal("chodem").executes(ctx -> openLookedAt(ctx, ShopKind.NIGHT)));
        dispatcher.register(Commands.literal("harimarket")
                .then(Commands.literal("stats").executes(HariShopCommand::stats))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(2))
                        .executes(HariShopCommand::reloadConfig))
                .then(Commands.literal("reindex").requires(source -> source.hasPermission(2))
                        .executes(HariShopCommand::refreshAllShops))
                .then(giveBlocksBranch()));
    }

    private enum ShopKind { RETAIL, PAWN, NIGHT }

    private static int openLookedAt(CommandContext<CommandSourceStack> context, ShopKind kind) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendFailure(Component.literal("§cLệnh này cần người chơi đang nhìn vào block shop."));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayer();
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            context.getSource().sendFailure(Component.literal("§cHãy nhìn vào block shop rồi thử lại."));
            return 0;
        }
        BlockEntity entity = player.level().getBlockEntity(blockHit.getBlockPos());
        boolean valid = switch (kind) {
            case RETAIL -> entity instanceof DailyDealsBlockEntity;
            case PAWN -> entity instanceof PawnShopBlockEntity;
            case NIGHT -> entity instanceof NightMarketBlockEntity;
        };
        if (!valid) {
            context.getSource().sendFailure(Component.literal("§cBlock đang nhìn không đúng loại shop."));
            return 0;
        }
        MenuProvider provider = (MenuProvider) entity;
        MenuRegistry.openExtendedMenu(player, provider, buf -> buf.writeBlockPos(blockHit.getBlockPos()));
        return 1;
    }

    private static int stats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            int tabs = CreativeTabShopManager.getTabCount();
            int items = CreativeTabShopManager.getTotalItemCount();
            int recipes = RecipeAnalyzer.getRecipeCount();
            int prices = RecipeAnalyzer.getCacheSize();
            source.sendSuccess(() -> Component.literal("§b========== HARI MARKET STATS (FORGE 1.20.1) =========="), false);
            source.sendSuccess(() -> Component.literal("§7Catalog: §f" + items + " §7items / §f" + tabs + " §7tabs"), false);
            source.sendSuccess(() -> Component.literal("§7Recipe cache: §f" + recipes + " §7recipes, §f" + prices + " §7priced"), false);
            source.sendSuccess(() -> Component.literal("§7Pawn: §f" + (ModBlocks.PAWN_SHOP_BLOCK_ENTITY != null ? "READY" : "UNAVAILABLE")
                    + " §7| NBT variants: §aFAIL-CLOSED"), false);
            source.sendSuccess(() -> Component.literal("§7Exact money: §flong 0.." + hari.harishop.v5.MoneyMath.MAX_BALANCE
                    + " §7| Buy=CEILING, Sell=FLOOR"), false);
            source.sendSuccess(() -> Component.literal("§eForge parity: core shop + secure packet gate; V5 SQLite/certification remains unavailable on 1.20.1."), false);
            return 1;
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("§cKhông đọc được trạng thái market: " + failure.getClass().getSimpleName()));
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> giveBlocksBranch() {
        return Commands.literal("giveblocks")
                .requires(source -> source.hasPermission(2))
                .executes(HariShopCommand::giveBlocks)
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(HariShopCommand::giveBlocks));
    }

    private static int giveBlocks(CommandContext<CommandSourceStack> context) {
        ServerPlayer target;
        try {
            target = context.getArgument("target", ServerPlayer.class);
        } catch (IllegalArgumentException missingTarget) {
            if (!context.getSource().isPlayer()) {
                context.getSource().sendFailure(Component.literal("§cDùng /harishop giveblocks <player> từ console."));
                return 0;
            }
            target = context.getSource().getPlayer();
        }
        if (target == null) return 0;

        int delivered = 0;
        delivered += giveBlock(target, ModBlocks.DAILY_DEALS_ITEM.get());
        delivered += giveBlock(target, ModBlocks.PAWN_SHOP_ITEM.get());
        delivered += giveBlock(target, ModBlocks.NIGHT_MARKET_ITEM.get());

        Item landOffice = BuiltInRegistries.ITEM.get(new ResourceLocation("proclaimauthforge", "land_office"));
        boolean hasLandOffice = landOffice != null && landOffice != Items.AIR;
        if (hasLandOffice) delivered += giveBlock(target, landOffice);

        String offices = hasLandOffice
                ? "Cửa Hàng, Thu Mua, Chợ Đêm, Cục Đất Đai"
                : "Cửa Hàng, Thu Mua, Chợ Đêm";
        String suffix = hasLandOffice
                ? ""
                : " §7(ProClaim chưa được nạp nên chưa có Cục Đất Đai.)";
        String message = "§a[Hari Market] Đã cấp " + delivered + " block cho "
                + target.getGameProfile().getName() + ": " + offices + "." + suffix;
        target.sendSystemMessage(Component.literal(message));
        if (context.getSource().getEntity() != target) {
            context.getSource().sendSuccess(() -> Component.literal(message), true);
        }
        return delivered > 0 ? 1 : 0;
    }

    private static int giveBlock(ServerPlayer target, Item item) {
        if (target == null || item == null || item == Items.AIR) return 0;
        ItemStack stack = new ItemStack(item);
        int before = stack.getCount();
        target.getInventory().add(stack);
        return Math.max(0, before - stack.getCount());
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            ShopConfig.reload();
            CreativeTabShopManager.reload();
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"\u00a7aHariShop config reloaded!"), true);
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77Tabs: " + CreativeTabShopManager.getTabCount() + ", Items: " + CreativeTabShopManager.getTotalItemCount())), false);
            return 1;
        }
        catch (Exception e) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)("\u00a7cFailed: " + e.getMessage())));
            return 0;
        }
    }

    private static int refreshAllShops(CommandContext<CommandSourceStack> context) {
        CreativeTabShopManager.reload();
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"\u00a7aRefreshed all shops!"), true);
        return 1;
    }

    private static int listTabs(CommandContext<CommandSourceStack> context) {
        List<ResourceLocation> tabs = CreativeTabShopManager.getAvailableTabs();
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7b=== Creative Tabs (" + tabs.size() + ") ===")), false);
        for (ResourceLocation tabId : tabs) {
            String name = CreativeTabShopManager.getTabName(tabId);
            String id = tabId.toString().replace(":", "_");
            int count = CreativeTabShopManager.getItemsForTab(tabId).size();
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77- \u00a7e" + name + " \u00a78[" + id + "] \u00a77(" + count + ")")), false);
        }
        return 1;
    }

    private static int setTab(CommandContext<CommandSourceStack> context) {
        String tabArg = StringArgumentType.getString(context, (String)"tab");
        Object tabIdStr = tabArg.replace("_", ":");
        if (!((String)tabIdStr).contains(":")) {
            tabIdStr = "minecraft:" + (String)tabIdStr;
        }
        ResourceLocation targetTab = null;
        String tabName = null;
        for (ResourceLocation tabId : CreativeTabShopManager.getAvailableTabs()) {
            if (!tabId.toString().equals(tabIdStr) && !tabId.toString().replace(":", "_").equalsIgnoreCase(tabArg)) continue;
            targetTab = tabId;
            tabName = CreativeTabShopManager.getTabName(tabId);
            break;
        }
        if (targetTab == null) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)("\u00a7cTab not found: " + tabArg)));
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a77Use /harishop tabs to see available tabs"));
            return 0;
        }
        if (!((CommandSourceStack)context.getSource()).isPlayer()) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a7cMust be a player"));
            return 0;
        }
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        HitResult hit = player.pick(5.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a7cLook at a Night Market block"));
            return 0;
        }
        BlockPos pos = ((BlockHitResult)hit).getBlockPos();
        BlockEntity entity = player.level().getBlockEntity(pos);
        if (!(entity instanceof NightMarketBlockEntity)) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a7cNot a Night Market block"));
            return 0;
        }
        NightMarketBlockEntity shop = (NightMarketBlockEntity)entity;
        shop.setSelectedTab(targetTab);
        String finalTabName = tabName;
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aSet shop to: \u00a7e" + finalTabName)), true);
        return 1;
    }

    private static int debugPrice(CommandContext<CommandSourceStack> context) {
        if (!((CommandSourceStack)context.getSource()).isPlayer()) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a7cMust be a player"));
            return 0;
        }
        ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a7cHold an item to check its price"));
            return 0;
        }
        Item item = heldItem.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7b=== Price Debug: " + String.valueOf(itemId) + " ===")), false);
        long baseValue = BaseItemValues.getBaseValue(item);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77BaseItemValues: " + (String)(baseValue > 0L ? "\u00a7a" + baseValue + " copper" : "\u00a7cnot found"))), false);
        long recipePrice = RecipeAnalyzer.isInitialized() ? RecipeAnalyzer.calculatePrice(item) : -1L;
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77RecipeAnalyzer: " + (String)(recipePrice > 0L ? "\u00a7a" + recipePrice + " copper" : "\u00a7cnot craftable"))), false);
        long tagPrice = TagPricing.getPriceByTags(item);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77TagPricing: " + (String)(tagPrice > 0L ? "\u00a7a" + tagPrice + " copper" : "\u00a7cnot tagged"))), false);
        long villagerPrice = VillagerTradeAnalyzer.getSuggestedPrice(item);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77VillagerTrade: " + (String)(villagerPrice > 0L ? "\u00a7a" + villagerPrice + " copper" : "\u00a7cno trade"))), false);
        boolean hasOre = OreGenAnalyzer.hasOreData(item);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77OreGenData: " + (hasOre ? "\u00a7ayes" : "\u00a7cno"))), false);
        long finalPrice = ItemValueCalculator.calculatePrice(item);
        long buyPrice = ItemValueCalculator.getBuyPrice(item);
        long sellPrice = ItemValueCalculator.getSellPrice(item);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"\u00a7e---"), false);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aBase Price: \u00a7f" + finalPrice + " copper")), false);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aBuy Price: \u00a7f" + buyPrice + " copper \u00a77(with fluctuation)")), false);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7aSell Price: \u00a7f" + sellPrice + " copper \u00a77(50% of buy)")), false);
        String coinFormat = HariShopCommand.formatAsCoin(buyPrice);
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7bDisplay: \u00a7f" + coinFormat)), false);
        return 1;
    }

    private static String formatAsCoin(long copper) {
        if (copper >= 100000L) {
            return String.format("%.1f Netherite", (double)copper / 100000.0);
        }
        if (copper >= 10000L) {
            return String.format("%.1f Diamond", (double)copper / 10000.0);
        }
        if (copper >= 1000L) {
            return String.format("%.1f Emerald", (double)copper / 1000.0);
        }
        if (copper >= 100L) {
            return String.format("%.1f Gold", (double)copper / 100.0);
        }
        if (copper >= 10L) {
            return String.format("%.1f Iron", (double)copper / 10.0);
        }
        return copper + " Copper";
    }

    private static int showAllHistory(CommandContext<CommandSourceStack> context) {
        List<ShopEditHistory.DeletedItemRecord> history = ShopEditHistory.getAllHistory();
        if (history.isEmpty()) {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)"\u00a77No deleted items in history."), false);
            return 1;
        }
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7b=== Deleted Items History (" + history.size() + ") ===")), false);
        int shown = 0;
        for (ShopEditHistory.DeletedItemRecord record : history) {
            if (shown >= 20) {
                ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77... and " + (history.size() - 20) + " more. Use /harishop history <category>")), false);
                break;
            }
            String itemName = record.itemId.split(":")[1];
            String catName = record.category.split(":")[1];
            String time = record.getFormattedTime();
            int idx = shown++;
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)String.format("\u00a77[%d] \u00a7e%s \u00a78from \u00a76%s \u00a78by \u00a7f%s \u00a78@ %s", idx, itemName, catName, record.deletedBy, time)), false);
        }
        return 1;
    }

    private static int showCategoryHistory(CommandContext<CommandSourceStack> context) {
        String category = StringArgumentType.getString(context, (String)"category").replace("_", ":");
        List<ShopEditHistory.DeletedItemRecord> history = ShopEditHistory.getHistory(category);
        if (history.isEmpty()) {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77No deleted items for category: " + category)), false);
            return 1;
        }
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7b=== Deleted Items: " + category + " (" + history.size() + ") ===")), false);
        int i = 0;
        while (i < history.size()) {
            ShopEditHistory.DeletedItemRecord record = history.get(i);
            String itemName = record.itemId.split(":")[1];
            String time = record.getFormattedTime();
            String price = HariShopCommand.formatAsCoin(record.price);
            int idx = i++;
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)String.format("\u00a77[%d] \u00a7e%s \u00a78- %s \u00a77by \u00a7f%s \u00a78@ %s", idx, itemName, price, record.deletedBy, time)), false);
        }
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a77Use: /harishop restore " + category.replace(":", "_") + " <index>")), false);
        return 1;
    }

    private static int restoreItem(CommandContext<CommandSourceStack> context) {
        int index;
        String category = StringArgumentType.getString(context, (String)"category").replace("_", ":");
        ShopEditHistory.DeletedItemRecord record = ShopEditHistory.restoreItem(category, index = IntegerArgumentType.getInteger(context, (String)"index"));
        if (record == null) {
            ((CommandSourceStack)context.getSource()).sendFailure((Component)Component.literal((String)"\u00a7cInvalid index or category!"));
            return 0;
        }
        ResourceLocation catId = new ResourceLocation(category);
        ItemStack stack = record.toItemStack();
        if (!stack.isEmpty()) {
            CreativeTabShopManager.addItemToTab(catId, stack.getItem(), record.price);
        }
        String itemName = record.itemId.split(":")[1];
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7a\u2713 Restored: \u00a7e" + itemName + " \u00a77to \u00a76" + category)), true);
        return 1;
    }

    private static int clearAllHistory(CommandContext<CommandSourceStack> context) {
        int count = ShopEditHistory.getTotalHistoryCount();
        ShopEditHistory.clearAllHistory();
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal((String)("\u00a7a\u2713 Cleared " + count + " history records!")), true);
        return 1;
    }
}

