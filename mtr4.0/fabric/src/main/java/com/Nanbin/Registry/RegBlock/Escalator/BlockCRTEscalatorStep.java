package com.Nanbin.Registry.RegBlock.Escalator;

import com.Nanbin.Items.Items;
import org.mtr.mapping.holder.Item;
import org.mtr.mod.block.BlockEscalatorStep;

import javax.annotation.Nonnull;

/**
 * CRT 自动扶梯梯级（会运送玩家的踏板）。行为沿用 MTR 扶梯的机制：
 * 站立时按朝向自动上/下行、软着陆、每段的朝向与状态整段同步，
 * 外观换成 nanbin 自己的模型与贴图：{@code nanbin:block/crt_escalator_step_*}。
 * <p>
 * 状态由 {@code direction} 与 {@code status} 两个属性表示：
 * 停止（status=false）、上行（direction=true, status=true）、
 * 下行（direction=false, status=true）；刷子右键可循环切换。
 *
 * @see BlockCRTEscalatorSide
 * @see com.Nanbin.Registry.RegItem.ItemCRTEscalator
 */
public class BlockCRTEscalatorStep extends BlockEscalatorStep {

    /** 破坏或中键取块时掉落本模组的扶梯物品。 */
    @Nonnull
    @Override
    public Item asItem2() {
        return Items.CRT_ESCALATOR.get();
    }
}
