package com.Nanbin.Registry.RegBlock.TallFence;

import com.Nanbin.mapping.BlockExtension;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.IBlock;

import java.util.List;

public class BlockGreenFenceTop extends BlockExtension {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 10, 16, 16, 16);
	private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 6);
	private static final VoxelShape SHAPE_EAST = Block.createCuboidShape(0, 0, 0, 6, 16, 16);
	private static final VoxelShape SHAPE_WEST = Block.createCuboidShape(10, 0, 0, 16, 16, 16);

	public BlockGreenFenceTop(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(FACING);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
		return switch (IBlock.getStatePropertySafe(state, FACING)) {
			case SOUTH -> SHAPE_SOUTH;
			case EAST -> SHAPE_EAST;
			case WEST -> SHAPE_WEST;
			default -> SHAPE_NORTH;
		};
	}

	@Override
	public void onStateReplaced2(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())) {
			final BlockPos downPos = pos.down();
			if (world.getBlockState(downPos).getBlock() instanceof BlockGreenFence) {
				world.removeBlock(downPos, false);
			}
		}
	}
}
