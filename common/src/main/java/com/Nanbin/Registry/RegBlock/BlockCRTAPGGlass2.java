package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.IBlock;

/**
 * CRT 屏蔽门玻璃（二代）：行为与一代一致（复用 {@link BlockCRTAPGGlass1} 的成组切换逻辑），
 * 只是「上半格」轮廓更薄、且成组判定只认同类方块。
 */
public class BlockCRTAPGGlass2 extends BlockCRTAPGGlass1 {

	public BlockCRTAPGGlass2(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected boolean isSameGlass(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockCRTAPGGlass2;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
		return upper
				? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 8, 4, facing)
				: IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}
}
