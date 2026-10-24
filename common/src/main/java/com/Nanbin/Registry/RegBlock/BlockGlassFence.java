package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.DirectionHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.mtr.block.BlockDirectionalDoubleBlockBase;
import org.mtr.block.IBlock;

public class BlockGlassFence extends BlockDirectionalDoubleBlockBase {

	public BlockGlassFence(AbstractBlock.Settings settings) {
		super(settings.nonOpaque());
	}

	@Override
	protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		builder.add(DirectionHelper.FACING, IBlock.HALF);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final var facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
		return IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER
				? IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 3, 3, facing)
				: IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, facing);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final var facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
		return VoxelShapes.union(getOutlineShape(state, world, pos, context), IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 8, 3, facing));
	}

	@Override
	public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}
}
