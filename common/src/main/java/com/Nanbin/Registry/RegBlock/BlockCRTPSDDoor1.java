package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Items.Items;
import com.Nanbin.entity.BlockEntityTypes;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPSDDoor;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

/**
 * CRT 第一代高屏蔽门门体。
 *
 * <p>完全沿用 MTR 原版屏蔽门的逻辑：继承 {@link BlockPSDDoor}（style 0 = MTR 的 psd_door 一代），
 * 属性、开关门动画、红石/解锁行为、放置方向都由父类决定；模型贴图由 MTR 的
 * {@code RenderPSDAPGDoor}（type 0）渲染，因此门体外观与 MTR 的 psd_door 一致。
 *
 * <p>只替换方块实体类型（挂本模组自己的 BlockEntity）与掉落物品（本模组的物品），
 * 便于与 CRT 屏蔽门玻璃/吊板成套使用。
 */
public class BlockCRTPSDDoor1 extends BlockPSDDoor implements EntityBlockMapper {

	/** MTR psd_door 一代的样式编号，与 RenderPSDAPGDoor 的 type 参数一致。 */
	private static final int STYLE = 0;

	public BlockCRTPSDDoor1() {
		super(STYLE);
	}

	@Override
	public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	/** 父类会返回 MTR 的 psd_door 物品，这里改回本模组的屏蔽门物品。 */
	@Override
	public Item asItem() {
		return Items.CRT_PSD_DOOR_1.get();
	}

	/**
	 * 方块实体。
	 *
	 * <p>注意：不能继承 {@link BlockPSDDoor.TileEntityPSDDoor} —— 它的构造器会把方块实体类型硬编码成
	 * MTR 自己的 {@code mtr:psd_door_*}，与本方块注册的 BE 类型不一致，
	 * 结果是原版判定「invalid for ticking」（不计 tick、门不会开关）。
	 * 因此直接继承 MTR 的 {@link BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase}，
	 * 构造时传入本模组自己的 BE 类型。
	 */
	public static class BlockEntity extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_PSD_DOOR_1.get(), pos, state);
		}
	}
}
