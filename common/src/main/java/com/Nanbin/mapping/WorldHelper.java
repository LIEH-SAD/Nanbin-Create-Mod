package com.Nanbin.mapping;

import net.minecraft.world.World;

/**
 * MTR 4.0 {@code com.Nanbin.mapping.WorldHelper} 的 Architectury/1.20.1 版本。
 *
 * <p>来源文件里 {@code getTopY/getBottomY} 各有一处 Stonecutter {@code //? if <1.17} 分支；
 * 本工程固定 1.20.1，按 else 分支展开，直接调用原版 {@link World#getTopY()} 与 {@link World#getBottomY()}。
 * 因为目标侧方块传入的是原版 {@link World}，这里不再需要 MTR 4 的 holder 拆包。</p>
 */
public final class WorldHelper {

	private WorldHelper() {
	}

	public static int getTopY(World world) {
		return world.getTopY();
	}

	public static int getBottomY(World world) {
		return world.getBottomY();
	}
}
