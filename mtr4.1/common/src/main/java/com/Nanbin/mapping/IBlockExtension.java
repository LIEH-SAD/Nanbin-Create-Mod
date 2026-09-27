package com.Nanbin.mapping;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.mtr.block.IBlock;
import org.mtr.registry.Items;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.BlockHelper} 以及自建的同名工具接口。
 * 原实现包在 4.0 的 mapping 包装上，这里改用原版类型 + MTR 4.1 的 {@code org.mtr.block.IBlock}。
 */
public interface IBlockExtension {

	EnumProperty<BlockThirdProperty> THIRD = EnumProperty.of("third", BlockThirdProperty.class);
	EnumProperty<EnumGlassMode> GLASS_MODE = EnumProperty.of("mode", EnumGlassMode.class);

	static void breakBlock(World world, BlockPos pos) {
		if (world.isClient) {
			return;
		}
		world.setBlockState(pos, Blocks.AIR.getDefaultState());
	}

	static void breakBlock(World world, BlockPos pos, Block block) {
		if (world.isClient) {
			return;
		}
		if (isBlock(world.getBlockState(pos), block)) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
		}
	}

	static boolean isBlock(BlockState state, Block block) {
		return state.isOf(block);
	}

	static ActionResult checkHoldingBrush(World world, PlayerEntity player, Runnable callback) {
		return IBlock.checkHoldingItem(world, player, item -> callback.run(), null, Items.BRUSH.get());
	}

	static ActionResult checkHoldingBasicKey(World world, PlayerEntity player, Runnable callback) {
		return checkHoldingBrush(world, player, callback);
	}

	static ActionResult checkHoldingAdvancedKey(World world, PlayerEntity player, Runnable callback) {
		return checkHoldingBrush(world, player, callback);
	}

	@SafeVarargs
	static <T extends Enum<T> & StringIdentifiable> BlockState cycleBlockState(BlockState state, EnumProperty<T> property, T... includes) {
		return cycleBlockState(state, property, Arrays.asList(includes));
	}

	static <T extends Enum<T> & StringIdentifiable> BlockState cycleBlockState(BlockState state, EnumProperty<T> property, Predicate<T> includes) {
		final List<T> values = property.getValues().stream().filter(includes).toList();
		return cycleBlockState(state, property, values);
	}

	static <T extends Enum<T> & StringIdentifiable> BlockState cycleBlockState(BlockState state, EnumProperty<T> property, List<T> includes) {
		int index = includes.indexOf(IBlock.getStatePropertySafe(state, property));
		if (index < 0 || index == includes.size() - 1) {
			index = -1;
		}
		return state.with(property, includes.get(index + 1));
	}

	/** 三向属性，对应 4.0 的 {@code IBlockExtension.BlockThirdProperty}。 */
	enum BlockThirdProperty implements StringIdentifiable {
		LEFT("left"),
		RIGHT("right"),
		BOTH("both");

		private final String name;

		BlockThirdProperty(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	/** 玻璃连接模式。 */
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
