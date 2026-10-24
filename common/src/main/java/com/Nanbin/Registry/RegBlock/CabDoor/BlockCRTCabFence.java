package com.Nanbin.Registry.RegBlock.CabDoor;

import com.Nanbin.mapping.DirectionHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.mtr.block.BlockDirectionalDoubleBlockBase;
import org.mtr.block.IBlock;

public class BlockCRTCabFence extends BlockDirectionalDoubleBlockBase {

	public BlockCRTCabFence(AbstractBlock.Settings settings) {
		super(settings.nonOpaque());
	}

	@Override
	protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		builder.add(DirectionHelper.FACING, IBlock.HALF);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, IBlock.getStatePropertySafe(state, DirectionHelper.FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.union(getOutlineShape(state, world, pos, context),
				IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 3, IBlock.getStatePropertySafe(state, DirectionHelper.FACING)));
	}

	@Override
	public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}
}
