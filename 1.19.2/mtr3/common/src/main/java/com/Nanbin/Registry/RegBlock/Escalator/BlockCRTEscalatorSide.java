package com.Nanbin.Registry.RegBlock.Escalator;

import com.Nanbin.Items.Items;
import mtr.block.BlockEscalatorSide;
import net.minecraft.item.Item;

/**
 * CRT 自动扶梯侧板（含扶手带）。放置、朝向判定、与梯级的联动破坏等行为
 * 全部沿用 MTR 扶梯的机制，只把外观换成 nanbin 自己的模型与贴图：
 * {@code nanbin:block/crt_escalator_side_*}。
 * <p>
 * 侧板与梯级成对出现（梯级在上、侧板在下），破坏任意一格都会连带处理整段。
 * 刷子右键梯级可切换「上行 / 下行 / 停止」。
 *
 * @see BlockCRTEscalatorStep
 * @see com.Nanbin.Registry.RegItem.ItemCRTEscalator
 */
public class BlockCRTEscalatorSide extends BlockEscalatorSide {

	/** 破坏或中键取块时掉落本模组的扶梯物品。 */
	@Override
	public Item asItem() {
		return Items.CRT_ESCALATOR.get();
	}
}
