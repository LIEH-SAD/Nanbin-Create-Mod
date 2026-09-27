package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.AbstractBlock;
import org.mtr.block.BlockStationNameWallBase;

/**
 * 站名方块基类 —— 继承 MTR 的 {@link BlockStationNameWallBase} 以获得墙面安装行为。
 */
public abstract class BlockStationNameBase extends BlockStationNameWallBase {

	public BlockStationNameBase(AbstractBlock.Settings settings) {
		super(settings);
	}
}
