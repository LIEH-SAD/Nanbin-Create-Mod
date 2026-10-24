package com.Nanbin.Registry.RegBlock;

import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockPSDAPGGlassEndBase;
import org.mtr.mod.block.IBlock;

public class BlockCRTAPGGlassEnd2 extends BlockPSDAPGGlassEndBase {

    public BlockCRTAPGGlassEnd2() {
        super();
    }

    public static VoxelShape getEndOutlineShape(VoxelShape baseShape, BlockState state, int height, int thickness, boolean leftAir, boolean rightAir) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        if (facing == Direction.NORTH && leftAir || facing == Direction.SOUTH && rightAir) {
            baseShape = VoxelShapes.union(baseShape, Block.createCuboidShape((double)0.0F, (double)0.0F, (double)0.0F, (double)thickness, (double)height, (double)16.0F));
        }

        if (facing == Direction.EAST && leftAir || facing == Direction.WEST && rightAir) {
            baseShape = VoxelShapes.union(baseShape, Block.createCuboidShape((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)height, (double)thickness));
        }

        if (facing == Direction.SOUTH && leftAir || facing == Direction.NORTH && rightAir) {
            baseShape = VoxelShapes.union(baseShape, Block.createCuboidShape((double)(16 - thickness), (double)0.0F, (double)0.0F, (double)16.0F, (double)height, (double)16.0F));
        }

        if (facing == Direction.WEST && leftAir || facing == Direction.EAST && rightAir) {
            baseShape = VoxelShapes.union(baseShape, Block.createCuboidShape((double)0.0F, (double)0.0F, (double)(16 - thickness), (double)16.0F, (double)height, (double)16.0F));
        }

        return baseShape;
    }
}