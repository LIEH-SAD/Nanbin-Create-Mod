package com.Nanbin.Registry.RegBlock.Escalator;

import com.Nanbin.Items.Items;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import org.mtr.block.BlockEscalatorSide;

/**
 * CRT 自动扶梯侧板（含扶手带）。
 * 放置、朝向判定、与梯级的联动破坏等行为全部沿用 MTR 扶梯的机制，
 * 只把外观换成 nanbin 自己的模型与贴图（{@code nanbin:block/crt_escalator_side_*}），
 * 并把掉落物换成 CRT 扶梯物品。
 */
public class BlockCRTEscalatorSide extends BlockEscalatorSide {

	public BlockCRTEscalatorSide(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public Item asItem() {
		return Items.CRT_ESCALATOR.get();
	}
}
