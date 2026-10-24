package com.Nanbin.mapping;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

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
	 * {@code getPositiveHorizontalDegrees()}，所以真正的调用放在按版本编译的
	 * {@link DirectionRotation} 里：不能再按方法名反射，
	 * 因为发布后的 Fabric / NeoForge 用的都不是 Yarn 的名字（详见该类的说明）。
	 */
	static float asRotation(Direction direction) {
		return DirectionRotation.asRotation(direction);
	}
}
