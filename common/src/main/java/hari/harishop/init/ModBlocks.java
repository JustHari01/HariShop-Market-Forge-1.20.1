/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.material.MapColor
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 */
package hari.harishop.init;

import hari.harishop.block.DailyDealsBlock;
import hari.harishop.block.DailyDealsBlockEntity;
import hari.harishop.block.NightMarketBlock;
import hari.harishop.block.NightMarketBlockEntity;
import hari.harishop.block.PawnShopBlock;
import hari.harishop.block.PawnShopBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create("harishop", Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create("harishop", Registries.ITEM);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create("harishop", Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<NightMarketBlock> NIGHT_MARKET = BLOCKS.register("night_market", () -> new NightMarketBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).noOcclusion()));
    public static final RegistrySupplier<BlockItem> NIGHT_MARKET_ITEM = ITEMS.register("night_market", () -> new BlockItem(NIGHT_MARKET.get(), new Item.Properties().arch$tab(hari.harishop.init.ModCreativeTabs.DAILYSHOP_TAB)));
    public static final RegistrySupplier<PawnShopBlock> PAWN_SHOP = BLOCKS.register("pawn_shop", () -> new PawnShopBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0f, 3600000.0f).noOcclusion()));
    public static final RegistrySupplier<BlockItem> PAWN_SHOP_ITEM = ITEMS.register("pawn_shop", () -> new BlockItem(PAWN_SHOP.get(), new Item.Properties().arch$tab(hari.harishop.init.ModCreativeTabs.DAILYSHOP_TAB)));
    public static final RegistrySupplier<DailyDealsBlock> DAILY_DEALS = BLOCKS.register("daily_deals", () -> new DailyDealsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0f, 3600000.0f).noOcclusion()));
    public static final RegistrySupplier<BlockItem> DAILY_DEALS_ITEM = ITEMS.register("daily_deals", () -> new BlockItem(DAILY_DEALS.get(), new Item.Properties().arch$tab(hari.harishop.init.ModCreativeTabs.DAILYSHOP_TAB)));
    public static final RegistrySupplier<BlockEntityType<NightMarketBlockEntity>> NIGHT_MARKET_BLOCK_ENTITY = BLOCK_ENTITIES.register("night_market_block_entity", () -> BlockEntityType.Builder.of(NightMarketBlockEntity::new, NIGHT_MARKET.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<PawnShopBlockEntity>> PAWN_SHOP_BLOCK_ENTITY = BLOCK_ENTITIES.register("pawn_shop_block_entity", () -> BlockEntityType.Builder.of(PawnShopBlockEntity::new, PAWN_SHOP.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<DailyDealsBlockEntity>> DAILY_DEALS_BLOCK_ENTITY = BLOCK_ENTITIES.register("daily_deals_block_entity", () -> BlockEntityType.Builder.of(DailyDealsBlockEntity::new, DAILY_DEALS.get()).build(null));

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
    }
}
