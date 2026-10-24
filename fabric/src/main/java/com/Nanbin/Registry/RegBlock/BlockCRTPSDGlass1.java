package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Items.Items;
import org.mtr.mapping.holder.Item;
import org.mtr.mod.block.BlockPSDGlass;

/**
 * CRT 第一代高屏蔽门玻璃。
 *
 * <p>沿用 MTR 原版屏蔽门玻璃的逻辑：继承 {@link BlockPSDGlass}（style 0 = MTR 的 psd_glass 一代），
 * 属性与 MTR 的 psd_glass 一致（facing / side / half），模型也直接沿用 MTR 的
 * {@code mtr:block/psd_glass_*_base}（见 assets/nanbin/models/block/crt_psd_glass_1），
 * 与门、端玻璃、吊板成套。
 *
 * <p><b>为什么必须继承 {@link BlockPSDGlass} 而不是 {@code BlockPSDAPGGlassBase}：</b>
 * 原版 {@code BlockPSDTop.getActualState()} 只认 {@link BlockPSDGlass} / {@code BlockPSDDoor} /
 * {@code BlockPSDGlassEnd} 三种方块，靠它把下方门/玻璃的 facing 与左右分组写到吊板状态上。
 * 若直接继承基类，吊板上方分组的朝向/左右就不会被写入 —— 表现为「放置玻璃后吊板方向错误」。
 */
public class BlockCRTPSDGlass1 extends BlockPSDGlass {

    /** MTR psd_glass 一代的样式编号，与贴图 psd_glass_*_1 对应。 */
    private static final int STYLE = 0;

    public BlockCRTPSDGlass1() {
        super(STYLE);
    }

    @Override
    public Item asItem2() {
        return Items.CRT_PSD_GLASS_1.get();
    }
}
