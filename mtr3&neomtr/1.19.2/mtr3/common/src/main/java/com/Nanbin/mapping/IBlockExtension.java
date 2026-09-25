package com.Nanbin.mapping;

import mtr.block.IBlock;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;

/**
 * Some methods similar to methods in IBlock.
 *
 * @see IBlock
 */
public interface IBlockExtension {

	/** CRT 矮屏蔽门玻璃的联动划分位置（1 块 / 左右 2 块 / 左中右 3 块）。 */
	EnumProperty<EnumGlassMode> GLASS_MODE = EnumProperty.of("mode", EnumGlassMode.class);

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
