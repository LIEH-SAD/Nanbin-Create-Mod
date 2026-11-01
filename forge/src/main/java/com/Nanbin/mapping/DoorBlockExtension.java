package com.Nanbin.mapping;

import org.mtr.mapping.holder.*;

import java.util.function.Consumer;

public abstract class DoorBlockExtension extends org.mtr.mapping.mapper.DoorBlockExtension {

	protected DoorBlockExtension(boolean canOpenByHand, Consumer<BlockSettings> settingsConsumer) {
		super(canOpenByHand, settingsConsumer);
	}

	@Override
	//? if <1.17 {
	/*public net.minecraft.util.math.shapes.VoxelShape getShape(net.minecraft.block.BlockState state, net.minecraft.world.IBlockReader world, net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.shapes.ISelectionContext context) {
	*///?} else {
	public net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
	//?}
		final VoxelShape result = getOutlineShape2(new BlockState(state), new BlockView(world), new BlockPos(pos), new ShapeContext(context));
		return result == null ? super.getShape(state, world, pos, context) : result.data;
	}

	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return null;
	}

	protected static boolean isOpen(BlockState state) {
		return state.get(new Property<>(DoorBlock.getOpenMapped().data));
	}

	protected static BlockState cycleOpen(BlockState state) {
		return state.cycle(new Property<>(DoorBlock.getOpenMapped().data));
	}

	protected static boolean isRightHinge(BlockState state) {
		//? if <1.17 {
		/*return state.get(new Property<>(net.minecraft.state.properties.BlockStateProperties.DOOR_HINGE)) == net.minecraft.state.properties.DoorHingeSide.RIGHT;
		*///?} else {
		return state.get(new Property<>(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOOR_HINGE)) == net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT;
		//?}
	}

	protected static boolean isUpperHalf(BlockState state) {
		//? if <1.17 {
		/*return state.get(new Property<>(net.minecraft.block.DoorBlock.HALF)) == net.minecraft.state.properties.DoubleBlockHalf.UPPER;
		*///?} else {
		return state.get(new Property<>(net.minecraft.world.level.block.DoorBlock.HALF)) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
		//?}
	}
}
