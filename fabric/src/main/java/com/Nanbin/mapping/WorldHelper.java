package com.Nanbin.mapping;

import org.mtr.mapping.holder.World;

public final class WorldHelper {

	private WorldHelper() {
	}

	public static int getTopY(World world) {
		//? if <1.17 {
		return 256;
		//?} else {
		/*return ((net.minecraft.world.World) world.data).getTopY();
		*///?}
	}

	public static int getBottomY(World world) {
		//? if <1.17 {
		return 0;
		//?} else {
		/*return ((net.minecraft.world.World) world.data).getBottomY();
		*///?}
	}
}
