package com.Nanbin.neoforge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * NeoForge 侧的方块实体类型构造。
 * <p>
 * 原版 {@code BlockEntityType} 的构造器在两个版本之间形态不同，而且 1.21.4 把它改成了 private
 * （MTR 自己是靠 access widener 访问的），所以这里用反射挑一个可用的构造器：
 * <ul>
 *     <li>1.21.1：{@code (BlockEntityFactory, Set<Block>, Type)}，Type 传 null</li>
 *     <li>1.21.4：{@code (BlockEntityFactory, Set<Block>)}</li>
 * </ul>
 */
final class BlockEntityTypeFactory {

	private BlockEntityTypeFactory() {
	}

	@SuppressWarnings("unchecked")
	static <T extends BlockEntity> BlockEntityType<T> create(BiFunction<BlockPos, BlockState, T> factory, Supplier<Block>[] blocks) {
		final Set<Block> blockSet = new HashSet<>();
		for (final Supplier<Block> block : blocks) {
			blockSet.add(block.get());
		}

		final BlockEntityType.BlockEntityFactory<T> blockEntityFactory = factory::apply;

		// 1.21.4：(factory, Set)
		final Constructor<?> twoArgument = findConstructor(2);
		if (twoArgument != null) {
			try {
				return (BlockEntityType<T>) twoArgument.newInstance(blockEntityFactory, blockSet);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("无法创建方块实体类型", e);
			}
		}

		// 1.21.1：(factory, Set, Type)，datafixer Type 传 null
		final Constructor<?> threeArgument = findConstructor(3);
		if (threeArgument != null) {
			try {
				return (BlockEntityType<T>) threeArgument.newInstance(blockEntityFactory, blockSet, null);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("无法创建方块实体类型", e);
			}
		}

		throw new IllegalStateException("当前版本的 BlockEntityType 没有可用的构造器");
	}

	private static Constructor<?> findConstructor(int parameterCount) {
		for (final Constructor<?> constructor : BlockEntityType.class.getDeclaredConstructors()) {
			if (constructor.getParameterCount() == parameterCount && Set.class.isAssignableFrom(constructor.getParameterTypes()[1])) {
				constructor.setAccessible(true);
				return constructor;
			}
		}
		return null;
	}
}
