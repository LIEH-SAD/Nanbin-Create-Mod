package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.block.PlatformHelper;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockCRTPlatform extends BlockExtension implements PlatformHelper {
    private final boolean isIndented;

    public BlockCRTPlatform(BlockSettings blockSettings, boolean isIndented) {
        super(blockSettings);
        this.isIndented = isIndented;
    }

    @Nonnull
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return PlatformHelper.getActualState(BlockView.cast(world), pos, state);
    }

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property((net.minecraft.state.property.Property)FACING.data), ctx.getPlayerFacing().data);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (this.isIndented) {
            Direction facing = IBlock.getStatePropertySafe(state, FACING);
            return VoxelShapes.union(IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)6.0F, (double)16.0F, (double)13.0F, (double)16.0F, facing), Block.createCuboidShape((double)0.0F, (double)13.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)16.0F));
        } else {
            return super.getOutlineShape2(state, world, pos, context);
        }
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(DOOR_TYPE);
        properties.add(SIDE);
    }
}
