package com.Nanbin.mapping;

import org.mtr.mapping.holder.*;

import java.util.function.Consumer;

public abstract class DoorBlockExtension extends org.mtr.mapping.mapper.DoorBlockExtension {

	protected DoorBlockExtension(boolean canOpenByHand, Consumer<org.mtr.mapping.holder.BlockSettings> settingsConsumer) {
		super(canOpenByHand, settingsConsumer);
	}

	@Override
	public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
		final VoxelShape result = getOutlineShape2(new BlockState(state), new BlockView(world), new BlockPos(pos), new ShapeContext(context));
		return result == null ? super.getShape(state, world, pos, context) : result.data;
	}

	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return null;
	}

	protected static boolean isOpen(BlockState state) {
		return state.get(new org.mtr.mapping.holder.Property<>(DoorBlock.getOpenMapped().data));
	}

	protected static BlockState cycleOpen(BlockState state) {
		return state.cycle(new org.mtr.mapping.holder.Property<>(DoorBlock.getOpenMapped().data));
	}

	protected static boolean isRightHinge(BlockState state) {
		return state.get(new org.mtr.mapping.holder.Property<>(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOOR_HINGE)) == net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT;
	}

	protected static boolean isUpperHalf(BlockState state) {
		return state.get(new org.mtr.mapping.holder.Property<>(net.minecraft.world.level.block.DoorBlock.HALF)) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
	}
}
