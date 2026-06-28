package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.RegMean.VoxelShapes;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockBehavioralVertical extends BlockExtension implements DirectionHelper {

public BlockBehavioralVertical(BlockSettings blockSettings) {
    super(blockSettings);
}

    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        return this.getDefaultState2().with(new Property<>(FACING.data), ctx.getPlayerFacing().data);
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape1 = IBlock.getVoxelShapeByDirection(0, 12, 0, 16, 16, 7, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape2 = IBlock.getVoxelShapeByDirection(0, 0, 4, 4, 12, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape3 = IBlock.getVoxelShapeByDirection(0, 12, 7, 7, 16, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape shape4 = IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 12, 4, IBlock.getStatePropertySafe(state, FACING));
        return VoxelShapes.union(shape1, shape2, shape3, shape4);
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}