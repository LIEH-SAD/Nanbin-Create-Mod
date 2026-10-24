package com.Nanbin.Registry.RegBlock;

import com.Nanbin.mapping.BlockEntityHelper;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.IBlock;

import java.util.List;

/**
 * CRT 屏蔽门方块的公共基类（一代/二代只差上半格轮廓与 BE 类型，源项目里是两个近乎逐行相同的类）。
 * <p>
 * 4.1 适配：{@code BlockWithEntity} → 原版 {@link BlockEntityProvider}；因为是继承 MTR 的
 * {@link BlockPSDAPGDoorBase}（不是自己的 BlockExtension），所以钩子直接覆写原版
 * {@code onUse}/{@code appendProperties}，而不是 {@code onUse2}/{@code addBlockProperties}。
 */
public abstract class BlockCRTAPGDoorBase extends BlockPSDAPGDoorBase implements BlockEntityProvider {

	/** 屏蔽门自动排序方向：true = 从右端开始编号，false = 从左端开始编号。 */
	public static final BooleanProperty SORT_FROM_RIGHT = BooleanProperty.of("sort_from_right");

	protected BlockCRTAPGDoorBase(net.minecraft.block.AbstractBlock.Settings settings) {
		super(settings);
	}

	/** 上半格轮廓，由子类给出。 */
	protected abstract VoxelShape getUpperHalfShape(Direction facing);

	@Override
	protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(SORT_FROM_RIGHT);
	}

	/** 供外部读取属性列表（保持与 4.0 一致的调用点）。 */
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(IBlock.HALF);
		properties.add(IBlock.SIDE_EXTENDED);
		properties.add(SORT_FROM_RIGHT);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		// 顶端（上半格）：切换整排门的自动排序方向；底端（下半格）：保留原来的锁定/解锁
		if (IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER) {
			return IBlock.checkHoldingBrush(world, player, () -> toggleSortDirection(world, pos, state, player));
		}
		return super.onUse(state, world, pos, player, hit);
	}

	/** 服务端：切换排序方向，并同步到同一排的所有门单元（上下两格 × 左右两块）。 */
	private static void toggleSortDirection(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final boolean newValue = !IBlock.getStatePropertySafe(state, SORT_FROM_RIGHT);
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final Direction axis = facing.rotateYClockwise();
		for (final BlockPos unitStart : APGDoorNumbering.findRowUnitStarts(world, pos)) {
			for (int x = 0; x < 2; x++) {
				final BlockPos base = unitStart.offset(axis, x);
				for (int y = 0; y < 2; y++) {
					final BlockPos targetPos = base.up(y);
					final BlockState targetState = world.getBlockState(targetPos);
					if (APGDoorNumbering.isCRTAPGDoor(targetState)) {
						world.setBlockState(targetPos, targetState.with(SORT_FROM_RIGHT, newValue));
					}
				}
			}
		}
		player.sendMessage((newValue ? TranslationProvider.APG_DOOR_SORT_FROM_RIGHT : TranslationProvider.APG_DOOR_SORT_FROM_LEFT).getText(), true);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
		return upper ? getUpperHalfShape(facing) : IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 4, facing);
	}

	/** 屏蔽门方块实体的公共逻辑（到站时间）。 */
	public abstract static class DoorBlockEntity extends BlockEntityBase {

		private static final String ARRIVAL_TIME_ID = "nextTrainArrivalTime";
		private static final long SYNC_INTERVAL = 1000;
		private long nextTrainArrivalTime = 0;
		private long lastSyncTime = 0;

		protected DoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
		}

		@Override
		protected void readNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
			nextTrainArrivalTime = nbt.getLong(ARRIVAL_TIME_ID);
			super.readNbt(nbt, registries);
		}

		@Override
		protected void writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
			nbt.putLong(ARRIVAL_TIME_ID, nextTrainArrivalTime);
			super.writeNbt(nbt, registries);
		}

		@Override
		public void tick(float tickDelta) {
			super.tick(tickDelta);
			if (getWorld() != null && !getWorld().isClient) {
				final long currentTime = System.currentTimeMillis();
				if (currentTime - lastSyncTime >= SYNC_INTERVAL) {
					lastSyncTime = currentTime;
					final long predictedArrival = predictNextArrival();
					if (predictedArrival != nextTrainArrivalTime) {
						setNextTrainArrivalTime(predictedArrival);
					}
				}
			}
		}

		public boolean isTrainApproaching(double warningTime) {
			if (nextTrainArrivalTime <= 0) {
				return false;
			}
			final long timeUntilArrival = nextTrainArrivalTime - System.currentTimeMillis();
			return timeUntilArrival > 0 && timeUntilArrival <= warningTime * 1000;
		}

		public void setNextTrainArrivalTime(long arrivalTime) {
			this.nextTrainArrivalTime = arrivalTime;
			BlockEntityHelper.markDirty(this);
		}

		public long getNextTrainArrivalTime() {
			return nextTrainArrivalTime;
		}

		/**
		 * 服务端专用：把「下一次列车到站时间」写入方块实体。
		 * <p>
		 * <b>说明：</b>4.0 这里也是反射读 simulator 内部结构来预测的，本移植不再解析。
		 * 但这<b>不影响显示</b>：屏蔽门的「列车接近」提示灯由渲染器
		 * （{@link com.Nanbin.client.Render.RenderCRTAPGDoorBase#isTrainApproaching}）
		 * 直接查客户端的到站缓存 {@code ArrivalsCacheClient} 判断，用的是 MTR 的公开 API；
		 * 而 4.0 用来把这里的结果送到客户端的 {@code PacketUpdateTrainArrival} 从来没有发送方，
		 * 所以这条服务端通路在源项目里本来就没有生效。
		 */
		protected long predictNextArrival() {
			return 0;
		}
	}
}
