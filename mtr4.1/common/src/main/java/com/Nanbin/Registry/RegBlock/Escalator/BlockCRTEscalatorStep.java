package com.Nanbin.Registry.RegBlock.Escalator;

import com.Nanbin.Items.Items;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import org.mtr.block.BlockEscalatorStep;

/**
 * CRT 自动扶梯梯级（会运送玩家的踏板）。
 * 站立时按朝向自动上下行、软着陆、每段的朝向与状态整段同步，
 * 外观换成 {@code nanbin:block/crt_escalator_step_*}，掉落物换成 CRT 扶梯物品。
 */
public class BlockCRTEscalatorStep extends BlockEscalatorStep {

	public BlockCRTEscalatorStep(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public Item asItem() {
		return Items.CRT_ESCALATOR.get();
	}
}
