package com.Nanbin.Registry.RegBlock;

import net.minecraft.block.AbstractBlock;
import org.mtr.block.BlockPSDAPGGlassEndBase;

/**
 * CRT 屏蔽门玻璃端块（一代）。
 * <p>
 * MTR 4.1 的 {@link BlockPSDAPGGlassEndBase} 已经内置了方向 / 贴合检测的轮廓计算，
 * 所以这里只需要提供方块设置。
 */
public class BlockCRTAPGGlassEnd1 extends BlockPSDAPGGlassEndBase {

	public BlockCRTAPGGlassEnd1(AbstractBlock.Settings settings) {
		super(settings);
	}
}
