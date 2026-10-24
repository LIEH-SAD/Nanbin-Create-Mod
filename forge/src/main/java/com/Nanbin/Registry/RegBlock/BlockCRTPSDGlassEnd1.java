package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Items.Items;
import org.mtr.mapping.holder.Item;
import org.mtr.mod.block.BlockPSDGlassEnd;

/**
 * CRT 第一代高屏蔽门端玻璃（屏蔽门两端的收口玻璃）。
 *
 * <p>沿用 MTR 原版逻辑：继承 {@link BlockPSDGlassEnd}（style 0 = MTR 的 psd_glass_end 一代），
 * TOUCHING_LEFT / TOUCHING_RIGHT 由父类按相邻方块自动更新，碰撞箱与端头收口形状也与 MTR 一致；
 * 模型直接沿用 MTR 的 {@code mtr:block/psd_glass_end_*_base}，贴图换成本模组的玻璃贴图。
 */
public class BlockCRTPSDGlassEnd1 extends BlockPSDGlassEnd {

    /** MTR psd_glass_end 一代的样式编号，与贴图 psd_glass_*_1 对应。 */
    private static final int STYLE = 0;

    public BlockCRTPSDGlassEnd1() {
        super(STYLE);
    }

    @Override
    public Item asItem2() {
        return Items.CRT_PSD_GLASS_END_1.get();
    }}
