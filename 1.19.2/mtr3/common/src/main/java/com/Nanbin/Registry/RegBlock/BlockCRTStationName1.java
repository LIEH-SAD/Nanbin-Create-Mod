package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Init;
import com.Nanbin.entity.BlockEntityTypes;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.PacketHandler;
import mtr.Items;
import mtr.block.BlockRouteSignBase;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * CRT 站名牌方块（样式 1）：单块薄板站名牌。
 * 使用刷子右键打开 MTR 的 {@link mtr.screen.RailwaySignScreen} 选择站台，
 * 由 MTR 服务端把选中的站台 ID 写回方块实体（BlockRouteSignBase.TileEntityRouteSignBase#setPlatformId）。
 * 渲染由 {@link com.Nanbin.client.Render.RenderCRTStationName1} 完成。
 */
public class BlockCRTStationName1 extends HorizontalFacingBlock implements BlockEntityProvider {

	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

	public BlockCRTStationName1(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getPlayerFacing());
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (player.isHolding(Items.BRUSH.get())) {
			if (!world.isClient) {
				// 与 MTR 的 BlockRouteSignBase 一致：通过自定义包告知客户端打开 RailwaySignScreen
				Registry.sendPacketToClient((ServerPlayerEntity) player, PacketHandler.PACKET_OPEN_CRT_STATION_NAME, buf -> buf.writeBlockPos(pos));
			}
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction direction = state.get(FACING);
		// 薄板：模型 z 0..0.6（约 1 像素厚），左右留边（1..15）
		return IBlock.getVoxelShapeByDirection(1, 0, 0, 15, 16, 1, direction);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BlockEntity(pos, state);
	}

	/**
	 * 方块实体：继承 MTR 的 BlockRouteSignBase.TileEntityRouteSignBase，
	 * 这样 MTR 服务端收到 RailwaySignScreen 的保存包后会自动调用 {@link #setPlatformId(long)}
	 * 并同步到客户端（platformId 的 NBT 持久化也由父类完成）。
	 */
	public static class BlockEntity extends BlockRouteSignBase.TileEntityRouteSignBase {

		private static final String KEY_ROUTE_NUMBER = "routeNumber";

		private String routeNumber = "";

		public BlockEntity(BlockPos pos, BlockState state) {
			super(BlockEntityTypes.CRT_STATION_NAME_1.get(), pos, state);
		}

		public String getRouteNumber() {
			return routeNumber;
		}

		/** 直接设置站台 ID 与线路编号（自定义包路径用），并同步到客户端。 */
		public void setData(long platformId, String routeNumber) {
			setPlatformId(platformId);
			this.routeNumber = routeNumber == null ? "" : routeNumber;
			markDirty();
			syncToClients();
		}

		/**
		 * 解析本方块实体的线路渲染数据（主题色、线路色、线路编号）。
		 * 内部基于当前已保存的站台 ID 与线路编号解析。
		 *
		 * @param fallbackColor 无线路数据时的回退颜色（通常为站名颜色）
		 */
		public ResolvedRouteData getResolvedData(int fallbackColor) {
			return resolveRouteData(getPlatformId(), fallbackColor, routeNumber);
		}

		/**
		 * 解析指定站台的线路信息（主题色、线路色、线路编号）。
		 * 选中站台（platformId != 0）即视为有线路接入：从途经线路解析线路颜色，
		 * 即使未输入编号也显示线路色；圆形序号优先级：
		 * ① 用户输入编号 > ② 轻轨线路编号（lightRailRouteNumber）> ③ 从线路名推导。
		 *
		 * @param platformId        保存的站台 ID，0 表示未配置
		 * @param fallbackColor     无线路数据时的回退颜色（通常为站名颜色）
		 * @param syncedRouteNumber 服务端同步到方块实体的线路编号（自定义输入的值），可能为 null 或空串
		 */
		public static ResolvedRouteData resolveRouteData(long platformId, int fallbackColor, String syncedRouteNumber) {
			int themeColor = fallbackColor;
			int routeColor = fallbackColor;
			String routeNumber = syncedRouteNumber == null ? "" : syncedRouteNumber;

			try {
				// 有线路接入（已选中站台）：解析途经线路的颜色与编号（优先匹配与输入编号一致的线路）
				if (platformId != 0) {
					Route matchedRoute = null;
					for (final Route route : ClientData.ROUTES) {
						if (route.getPlatformIdIndex(platformId) >= 0) {
							if (!routeNumber.isEmpty() && routeNumber.equals(routeNumberOf(route))) {
								matchedRoute = route;
								break;
							}
							if (matchedRoute == null) {
								matchedRoute = route;
							}
						}
					}
					if (matchedRoute != null) {
						routeColor = matchedRoute.color;
						// 未输入线路编号时，优先取轻轨编号；没有则从线路名推导编号
						if (routeNumber.isEmpty()) {
							routeNumber = routeNumberOf(matchedRoute);
						}
					} else {
						Init.LOGGER.info("CRTStationName: platform {} selected but no route passes it (routes={})", platformId, ClientData.ROUTES.size());
					}
				}

				// 主题色：优先取站台所属车站的颜色（无线路信息时作为基础显示色）
				if (platformId != 0) {
					final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
					final Station station = platform == null ? null : ClientData.DATA_CACHE.platformIdToStation.get(platformId);
					if (station != null) {
						themeColor = station.color;
					}
				}
			} catch (Exception e) {
				Init.LOGGER.error("CRTStationName: Error resolving route data for platform {}", platformId, e);
			}

			return new ResolvedRouteData(themeColor, routeColor, routeNumber);
		}

		/**
		 * 取线路的显示编号。优先级：轻轨线路编号（lightRailRouteNumber）> 从线路名推导。
		 * 兼容 MTR 的不规范命名：部分线路编号存于 lightRailRouteNumber 而非名称。
		 */
		public static String routeNumberOf(Route route) {
			if (route == null) {
				return "";
			}
			if (route.lightRailRouteNumber != null && !route.lightRailRouteNumber.isEmpty()) {
				return route.lightRailRouteNumber;
			}
			return deriveRouteNumberFromName(route.name);
		}

		/**
		 * 与线路颜色同源的编号推导：从线路名称提取编号。
		 * 遵循 MTR 线路名规则：|| 之后为隐藏段，| 为中文/英文分隔符；取首个可见段作为编号。
		 * 例如 "1号线|Line 1" -> "1号线"，"坏|Bad Line" -> "坏"。
		 */
		public static String deriveRouteNumberFromName(String routeName) {
			if (routeName == null || routeName.isEmpty()) {
				return "";
			}
			// MTR 规则：|| 之后的内容隐藏，只取第一部分
			final String primary = routeName.split("\\|\\|", -1)[0];
			// 取主要段（第一个 | 之前），即线路名称的显式部分
			final int separatorIndex = primary.indexOf('|');
			final String mainSegment = separatorIndex >= 0 ? primary.substring(0, separatorIndex) : primary;
			return mainSegment.trim();
		}

		/** 根据站台 ID 获取站名（找不到返回空串）。 */
		public static String getStationName(long platformId) {
			final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
			final Station station = platform == null ? null : ClientData.DATA_CACHE.platformIdToStation.get(platformId);
			return station == null ? "" : station.name;
		}

		/** 线路渲染数据解析结果：主题色、线路色、线路编号。 */
		public record ResolvedRouteData(int themeColor, int routeColor, String routeNumber) { }

		private void syncToClients() {
			if (world != null && !world.isClient) {
				world.updateListeners(getPos(), getCachedState(), getCachedState(), 3);
			}
		}

		@Override
		public void readCompoundTag(NbtCompound nbt) {
			super.readCompoundTag(nbt);
			routeNumber = nbt.getString(KEY_ROUTE_NUMBER);
		}

		@Override
		public void writeCompoundTag(NbtCompound nbt) {
			super.writeCompoundTag(nbt);
			nbt.putString(KEY_ROUTE_NUMBER, routeNumber);
		}
	}
}