//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.Nanbin.Registry.RegBlock.CabDoor;

import jakarta.annotation.Nonnull;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockDirectionalDoubleBlockBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class BlockCRTCabFence extends BlockDirectionalDoubleBlockBase {
    public static final IntegerProperty NUMBER = IntegerProperty.of("number", 1, 7);

    public BlockCRTCabFence() {
        super(Blocks.createDefaultBlockSettings(true).nonOpaque());
    }

    @Nonnull
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return IBlock.getStatePropertySafe(state, HALF) == DoubleBlockHalf.UPPER ? IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)8.0F, (double)3.0F, facing) :
                IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)16.0F, (double)3.0F, facing);
    }

    @Nonnull
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return VoxelShapes.union(this.getOutlineShape2(state, world, pos, context), IBlock.getVoxelShapeByDirection((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)8.0F, (double)3.0F, facing));
    }

    @Nonnull
    public VoxelShape getCameraCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    public void addBlockProperties(List<HolderBase<?>> properties) {
        properties.add(FACING);
        properties.add(HALF);
        properties.add(NUMBER);
    }

    protected BlockState getAdditionalState(BlockPos pos, Direction facing) {
        return this.getDefaultState2().with(new Property((net.minecraft.state.property.Property)NUMBER.data), getNumber(pos, facing));
    }

    private static int getNumber(BlockPos pos, Direction facing) {
        int x = (pos.getX() % 7 + 7) % 7;
        int z = (pos.getZ() % 7 + 7) % 7;
        return facing != Direction.NORTH && facing != Direction.EAST ? (-x - z) % 7 + 7 : (x + z) % 7 + 1;
    }
}
