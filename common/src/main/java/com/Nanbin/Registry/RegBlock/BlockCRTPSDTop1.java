package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Items.Items;
import com.Nanbin.entity.BlockEntityTypes;
import mtr.block.BlockPSDTop;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

/**
 * CRT 第一代高屏蔽门吊板（屏蔽门上方的横贯板），继承 MTR 的 {@link BlockPSDTop}：
 * 属性、左右悬空判断、persistent 切换、碰撞箱与贴图均与 MTR 的 psd_top 一致。
 *
 * <p>区别在于吊板本身承载 MTR 标准导视，由 {@link com.Nanbin.client.Render.RenderCRTPSDTop1}
 * 绘制本站台的线路图 / 方向箭头。
 */
public class BlockCRTPSDTop1 extends BlockPSDTop implements EntityBlockMapper {

	public BlockCRTPSDTop1() {
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	/** 父类会返回 MTR 的 psd_top 物品，这里改回本模组的吊板物品。 */
	@Override
	public Item asItem() {
		return Items.CRT_PSD_TOP_1.get();
	}

	/**
	 * 方块实体：直接继承 {@link BlockPSDTop.TileEntityRouteBase}（可传自定义 BE 类型，并保留
	 * getPlatformId 的平台解析能力）。
	 */
	public static class BlockEntity extends BlockPSDTop.TileEntityRouteBase {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_PSD_TOP_1.get(), pos, state);
		}
	}
}
