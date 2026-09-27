package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.AbstractBlock;
import org.mtr.block.BlockPlatform;

/**
 * CRT 站台方块。轮廓、加高站台的邻居联动完全沿用 MTR 4.1 的 {@link BlockPlatform}
 * （4.0 时代需要自己覆写 getStateForNeighborUpdate，4.1 的父类已经做了）。
 */
public class BlockCRTPlatform extends BlockPlatform {

	private final boolean isIndented;

	public BlockCRTPlatform(AbstractBlock.Settings settings, boolean isIndented) {
		super(settings, isIndented);
		this.isIndented = isIndented;
	}
}
