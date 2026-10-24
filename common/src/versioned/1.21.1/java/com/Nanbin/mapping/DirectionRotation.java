package com.Nanbin.mapping;

import net.minecraft.util.math.Direction;

/**
 * 朝向对应的水平旋转角度（度），1.21.1 版：{@code Direction#asRotation()}。
 * <p>
 * 必须<b>直接调用</b>，不能按方法名反射：发布后的 Fabric 用 intermediary 名，
 * NeoForge 用 Mojang 官方名（{@code toYRot}），都不是 Yarn 的名字，
 * 反射找不到就会抛「当前版本的 Direction 没有可用的水平旋转角度方法」。
 * <p>
 * 1.21.4 把方法改名成 {@code getPositiveHorizontalDegrees()}，
 * 见 {@code common/src/versioned/1.21.4}。
 */
public final class DirectionRotation {

	private DirectionRotation() {
	}

	public static float asRotation(Direction direction) {
		return direction.asRotation();
	}
}
