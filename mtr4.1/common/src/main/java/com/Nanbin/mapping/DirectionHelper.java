package com.Nanbin.mapping;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.DirectionHelper}。
 * 4.1 里没有这个接口，附属 mod 直接用原版属性即可。
 * <p>
 * 注意属性类型写成 {@code Property<Direction>}（而不是 1.21.1 的 {@code DirectionProperty}）：
 * 1.21.4 把 {@code DirectionProperty} 换成了 {@code EnumProperty<Direction>}，
 * 只有 {@code Property<Direction>} 能在两个版本上同时编译通过。
 */
public interface DirectionHelper {

	/** 水平朝向属性，等价于 4.0 的 {@code DirectionHelper.FACING}。 */
	Property<Direction> FACING = Properties.HORIZONTAL_FACING;

	static Direction getFacing(BlockState state) {
		return state.get(FACING);
	}

	/**
	 * 朝向对应的水平旋转角度（度）。
	 * <p>
	 * 1.21.1 的方法名是 {@code Direction.asRotation()}，1.21.4 改名成
	 * {@code getPositiveHorizontalDegrees()}，所以这里在类初始化时解析一次并缓存。
	 */
	static float asRotation(Direction direction) {
		return DirectionRotation.asRotation(direction);
	}

	/** 内部持有解析好的旋转角度访问器。 */
	final class DirectionRotation {

		private static final MethodHandle HANDLE = resolve();

		private DirectionRotation() {
		}

		private static MethodHandle resolve() {
			for (final String methodName : new String[]{"asRotation", "getPositiveHorizontalDegrees"}) {
				try {
					return MethodHandles.publicLookup().findVirtual(Direction.class, methodName, MethodType.methodType(float.class));
				} catch (NoSuchMethodException | IllegalAccessException ignored) {
					// 试下一个方法名
				}
			}
			throw new IllegalStateException("当前版本的 Direction 没有可用的水平旋转角度方法");
		}

		static float asRotation(Direction direction) {
			try {
				return (float) HANDLE.invoke(direction);
			} catch (Throwable e) {
				throw new IllegalStateException("读取朝向旋转角度失败", e);
			}
		}
	}
}
