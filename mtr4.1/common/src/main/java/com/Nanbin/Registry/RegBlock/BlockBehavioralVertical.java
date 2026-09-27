package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockExtension;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.mtr.block.IBlock;

import java.util.List;

public class BlockBehavioralVertical extends BlockExtension {

	public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	public BlockBehavioralVertical(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final var facing = IBlock.getStatePropertySafe(state, FACING);
		return VoxelShapes.union(
				IBlock.getVoxelShapeByDirection(0, 12, 0, 16, 16, 7, facing),
				IBlock.getVoxelShapeByDirection(0, 0, 4, 4, 12, 16, facing),
				IBlock.getVoxelShapeByDirection(0, 12, 7, 7, 16, 16, facing),
				IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 12, 4, facing)
		);
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(FACING);
	}
}
