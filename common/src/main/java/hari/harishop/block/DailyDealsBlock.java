/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraftforge.network.NetworkHooks
 *  org.jetbrains.annotations.Nullable
 */
package hari.harishop.block;

import hari.harishop.block.DailyDealsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import dev.architectury.registry.menu.MenuRegistry;
import org.jetbrains.annotations.Nullable;

public class DailyDealsBlock
extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    public DailyDealsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Nullable
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DailyDealsBlockEntity(pos, state);
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity entity;
        if (!level.isClientSide && (entity = level.getBlockEntity(pos)) instanceof DailyDealsBlockEntity) {
            DailyDealsBlockEntity shopEntity = (DailyDealsBlockEntity)entity;
            MenuRegistry.openExtendedMenu((ServerPlayer)player, (net.minecraft.world.MenuProvider)shopEntity, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess((boolean)level.isClientSide);
    }
}

