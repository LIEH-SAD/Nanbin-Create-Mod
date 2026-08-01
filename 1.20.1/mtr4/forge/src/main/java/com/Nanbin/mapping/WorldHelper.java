package com.Nanbin.mapping;

import org.mtr.mapping.holder.World;

public final class WorldHelper {

	private WorldHelper() {
	}

	public static int getTopY(World world) {
		return ((net.minecraft.world.level.Level) world.data).getMaxBuildHeight();
	}

	public static int getBottomY(World world) {
		return ((net.minecraft.world.level.Level) world.data).getMinBuildHeight();
	}
}
