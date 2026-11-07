package com.Nanbin.mapping;

import mtr.block.IBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Some methods similar to methods in IBlock.
 *
 * @see IBlock
 */
public interface IBlockExtension {

	/** CRT 矮屏蔽门玻璃的联动划分位置（1 块 / 左右 2 块 / 左中右 3 块）。 */
	EnumProperty<EnumGlassMode> GLASS_MODE = EnumProperty.of("mode", EnumGlassMode.class);

	/**
	 * Replace block with air.
	 *
	 * @param pos block's position
	 * @author ZiYueCommentary
	 * @since 1.0.0-beta-1
	 */
	static void breakBlock(World world, BlockPos pos) {
		if (world.isClient()) {
			return;
		}
		world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
	}

	/**
	 * Specify a block, if a block in pos is a specified block, then replace it with air.
	 *
	 * @param pos   block's position
	 * @param block specified block
	 * @author ZiYueCommentary
	 * @since 1.0.0-beta-1
	 */
	static void breakBlock(World world, BlockPos pos, Block block) {
		if (world.isClient()) {
			return;
		}
		if (isBlock(world.getBlockState(pos), block)) {
			world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
		}
	}

	static boolean isBlock(BlockState state, Block block) {
		return state.isOf(block);
	}

	static ActionResult checkHoldingBrush(World world, PlayerEntity player, Runnable callback) {
		return IBlock.checkHoldingBrush(world, player, callback);
	}

	static ActionResult checkHoldingBasicKey(World world, PlayerEntity player, Runnable callback) {
		return IBlock.checkHoldingBrush(world, player, callback);
	}

	static ActionResult checkHoldingAdvancedKey(World world, PlayerEntity player, Runnable callback) {
		return IBlock.checkHoldingBrush(world, player, callback);
	}

	static <T extends Enum<T> & StringIdentifiable> BlockState cycleBlockState(BlockState state, EnumProperty<T> property, Predicate<T> includes) {
		return cycleBlockState(state, property, property.getValues().stream().filter(includes).collect(Collectors.toList()));
	}

	@SafeVarargs
	static <T extends Enum<T> & StringIdentifiable> BlockState cycleBlockState(BlockState state, EnumProperty<T> property, T... includes) {
		return cycleBlockState(state, property, Arrays.asList(includes));
	}

	static <T extends Enum<T> & StringIdentifiable> BlockState cycleBlockState(BlockState state, EnumProperty<T> property, List<T> includes) {
		int index = includes.indexOf(IBlock.getStatePropertySafe(state, property));
		if (index < 0 || index == includes.size() - 1) {
			index = -1;
		}
		return state.with(property, includes.get(index + 1));
	}

	enum EnumGlassMode implements StringIdentifiable {
		SINGLE("single"),
		LEFT("left"),
		MIDDLE("middle"),
		RIGHT("right");

		private final String name;

		EnumGlassMode(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
