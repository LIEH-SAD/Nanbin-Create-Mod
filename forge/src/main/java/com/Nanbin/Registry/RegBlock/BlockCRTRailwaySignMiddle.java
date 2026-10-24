package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.packet.PacketOpenBlockEntityScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mod.block.BlockRailwaySign;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * CRT 版铁路告示牌的中间方块（对应原版 MTR 的 RAILWAY_SIGN_MIDDLE）。
 * 无方块实体，仅作为告示牌中段的填充模型；放置逻辑见 {@link BlockCRTRailwaySign#onPlaced2}。
 */
public class BlockCRTRailwaySignMiddle extends BlockRailwaySign {

    public BlockCRTRailwaySignMiddle() {
        super(0, false);
    }

    @Nullable
    @Override
    public BlockEntityExtension createBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Nonnull
    @Override
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        final boolean isNext = direction == facing.rotateYClockwise() || direction == facing.rotateYCounterclockwise();
        if (isNext && !(neighborState.getBlock().data instanceof BlockRailwaySign)) {
            return Blocks.getAirMapped().getDefaultState();
        } else {
            return state;
        }
    }

    @Override
    public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        final BlockPos checkPos = BlockCRTRailwaySign.findEndWithDirectionCRT(world, pos, facing, true);
        if (checkPos != null) {
            IBlock.onBreakCreative(world, player, checkPos);
        }
        super.onBreak2(world, pos, state, player);
    }

    @Nonnull
    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            final Direction facing = IBlock.getStatePropertySafe(state, FACING);
            final Direction hitSide = hit.getSide();
            if (hitSide == facing || hitSide == facing.getOpposite()) {
                final BlockPos checkPos = BlockCRTRailwaySign.findEndWithDirectionCRT(world, pos, hitSide.getOpposite(), false);
                if (checkPos != null) {
                    Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenBlockEntityScreen(checkPos));
                }
            }
        });
    }

    @Nonnull
    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getVoxelShapeByDirection(0, 0, 7, 16, 12, 9, facing);
    }
}
