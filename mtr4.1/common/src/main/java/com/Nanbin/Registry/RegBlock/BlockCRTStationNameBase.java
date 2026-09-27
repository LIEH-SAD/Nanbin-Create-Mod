package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.mapping.BlockEntityHelper;
import com.Nanbin.mapping.IBlockExtension;
import com.Nanbin.mapping.MtrServerData;
import com.Nanbin.mapping.Registry;
import com.Nanbin.mapping.TranslationProvider;
import com.Nanbin.packet.PacketOpenCRTPlatformScreen;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.mtr.block.BlockRouteSignBase;
import org.mtr.block.IBlock;

import java.util.List;

/**
 * CRT 站名牌方块的公共基类（站名方块 1/2 与普通站名方块共用同一套逻辑，
 * 源项目里是三个几乎逐行相同的类，这里抽出来）。
 * <p>
 * 4.1 适配：{@code BlockWithEntity} → 原版 {@link BlockEntityProvider}；
 * {@code readCompoundTag/writeCompoundTag} → {@code readNbt/writeNbt} + {@link NbtCompound}；
 * {@code getPos2/getWorld2/getCachedState2} → 原版方法名；{@code markDirty2} → {@link BlockEntityHelper#markDirty}。
 */
public abstract class BlockCRTStationNameBase extends BlockStationNameBase implements BlockEntityProvider {

	public BlockCRTStationNameBase(AbstractBlock.Settings settings) {
		super(settings);
	}

	// 注意：不要再在这里 appendProperties/add(Properties.HORIZONTAL_FACING) ——
	// MTR 的 BlockStationNameWallBase.createBlockStateDefinition 已经加了 facing，
	// 重复添加会让 StateManager.Builder 直接抛 IllegalArgumentException("duplicate property: facing")，
	// 进而在 Blocks 类初始化时崩掉整个游戏启动。

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		return IBlockExtension.checkHoldingBrush(world, player,
				() -> Registry.sendPacketToClient((ServerPlayerEntity) player, new PacketOpenCRTPlatformScreen(pos)));
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getMutableText().formatted(Formatting.DARK_GRAY));
		tooltip.add(TranslationProvider.BRUSH_USE.getMutableText().formatted(Formatting.DARK_GRAY));
	}

	/** 供外部读取属性列表（4.0 时代的调用点；facing 由 MTR 父类提供，这里不再重复声明）。 */
	public void addBlockProperties(List<Property<?>> properties) {
	}

	/** 站名牌方块实体的公共逻辑。 */
	public abstract static class StationNameBlockEntity extends BlockRouteSignBase.BlockEntityBase {

		private static final String KEY_ROUTE_NUMBER = "routeNumber";
		private String routeNumber = "";

		protected StationNameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
		}

		public int getColor(BlockState state) {
			return -1;
		}

		public String getRouteNumber() {
			return routeNumber;
		}

		@Override
		public void setPlatformId(long platformId) {
			super.setPlatformId(platformId);
			updateRouteNumberFromServer();
		}

		@Override
		protected void readNbt(NbtCompound nbt) {
			super.readNbt(nbt);
			routeNumber = nbt.getString(KEY_ROUTE_NUMBER);
		}

		@Override
		protected void writeNbt(NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putString(KEY_ROUTE_NUMBER, routeNumber);
		}

		/** 把站台 id 同时写入沿墙相邻的那一格。 */
		public void setData(long platformId) {
			setPlatformId(platformId);
			final BlockPos pos = getPos().offset(IBlock.getStatePropertySafe(getCachedState(), Properties.HORIZONTAL_FACING).rotateYClockwise());

			final BlockEntity blockEntity = getWorld().getBlockEntity(pos);
			if (blockEntity instanceof StationNameBlockEntity entity) {
				entity.setPlatformId(platformId);
				BlockEntityHelper.markDirty(entity);
			} else {
				Init.LOGGER.error("BlockCRTStationName: Unable to set data for block entity at {}", pos.toShortString());
			}
			BlockEntityHelper.markDirty(this);
		}

		/**
		 * 服务端专用：解析真实线路编号并写入 NBT，由 {@link BlockEntityHelper#markDirty} 同步给客户端。
		 * <p>
		 * 必须放在服务端做：MTR 客户端只有 {@code SimplifiedRoute}，没有 {@code routeNumber} 字段
		 * （见 {@link MtrServerData} 的说明），轻铁这类「名字与编号分开填」的线路在客户端取不到编号。
		 * 4.0 就是这么做的，4.1 移植时被写成了空实现导致编号丢失。
		 */
		protected void updateRouteNumberFromServer() {
			if (getWorld() == null || getWorld().isClient()) {
				return;
			}
			final long platformId = getPlatformId();
			if (platformId == 0) {
				return;
			}
			final String number = MtrServerData.getRouteNumber(MtrServerData.findPlatform(platformId));
			if (!number.isEmpty() && !number.equals(routeNumber)) {
				routeNumber = number;
				BlockEntityHelper.markDirty(this);
			}
		}
	}
}
