package com.Nanbin.mapping;

import net.minecraft.world.World;

/**
 * 取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.WorldHelper}。
 * 这里不用 {@code World#getTopY()}（1.21.4 改名了），改用 1.21.1 与 1.21.4 都稳定的
 * {@code getBottomY() + getHeight()} 计算。
 */
public final class WorldHelper {

	private WorldHelper() {
	}

	public static int getTopY(World world) {
		return world.getBottomY() + world.getHeight() - 1;
	}

	public static int getBottomY(World world) {
		return world.getBottomY();
	}
}
