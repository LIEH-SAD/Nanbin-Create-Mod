package com.Nanbin.client.CRTRailwaySignEditor.editor;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import com.Nanbin.mapping.Registry;
import com.Nanbin.packet.ClientPacketHelper;
import com.Nanbin.packet.PacketUpdateCRTSignLayout;
import com.Nanbin.signage.crt.CRTSignLayout;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.EditStationScreen;
import org.mtr.mod.screen.PIDSConfigScreen;

/**
 * {@link CRTSignEditorHost} 的标准实现：保存布局 → 发 {@link PacketUpdateCRTSignLayout} 给服务端；
 * 选择列表 → 取方块所属车站的 MTR 客户端数据（与 MTR 自己的选择屏同一份数据）。
 */
public final class CRTSignEditorHostImpl implements CRTSignEditorHost {

	private final BlockPos signPos;
	private final ObjectImmutableList<DashboardListItem> routesForList;
	private final ObjectImmutableList<DashboardListItem> stationsForList;
	private final ObjectImmutableList<DashboardListItem> exitsForList;
	private final ObjectImmutableList<DashboardListItem> platformsForList;
	private String lastSavedJson = "";

	@Override
	public BlockPos getSignPos() {
		return signPos;
	}

	@Override
	public int getSignCells() {
		try {
			final org.mtr.mapping.holder.ClientWorld world = org.mtr.mapping.holder.MinecraftClient.getInstance().getWorldMapped();
			if (world == null) {
				return 0;
			}
			final org.mtr.mapping.holder.BlockState state = world.getBlockState(signPos);
			if (state.getBlock().data instanceof org.mtr.mod.block.BlockRailwaySign) {
				return ((org.mtr.mod.block.BlockRailwaySign) state.getBlock().data).length;
			}
			if (state.getBlock().data instanceof BlockCRTRailwaySignWall1) {
				final java.util.List<BlockPos> run = BlockCRTRailwaySignWall1.collectRun(
						org.mtr.mapping.holder.BlockView.cast(world), signPos);
				return Math.max(1, run.size() * BlockCRTRailwaySignWall1.CELLS_PER_BLOCK);
			}
		} catch (Exception ignored) {
		}
		return 0;
	}

	@Override
	public Direction getFacing() {
		try {
			final org.mtr.mapping.holder.ClientWorld world = org.mtr.mapping.holder.MinecraftClient.getInstance().getWorldMapped();
			if (world != null) {
				final org.mtr.mapping.holder.BlockState state = world.getBlockState(signPos);
				if (state.getBlock().data instanceof org.mtr.mod.block.BlockRailwaySign) {
					return org.mtr.mod.block.IBlock.getStatePropertySafe(state, org.mtr.mod.block.BlockStationNameBase.FACING);
				}
			}
		} catch (Exception ignored) {
		}
		return Direction.NORTH;
	}

	/**
	 * @param signPos 保存布局的目标方块位置（铁路牌 = 牌端；墙挂牌 = 整条牌的起始方块）
	 */
	public CRTSignEditorHostImpl(BlockPos signPos) {
		this(signPos, "");
	}

	/**
	 * @param baselineJson 打开编辑器时的布局 JSON 基线；界面关闭时若内容与基线相同则不回写，
	 *                     避免"旧存档只是打开看一眼"就把它转成布局模式
	 */
	public CRTSignEditorHostImpl(BlockPos signPos, String baselineJson) {
		this.signPos = signPos;
		this.lastSavedJson = baselineJson == null ? "" : baselineJson;
		final Station station = ClientPacketHelper.findStationForSelection(signPos);
		if (station == null) {
			exitsForList = ObjectImmutableList.of();
			platformsForList = ObjectImmutableList.of();
			routesForList = ObjectImmutableList.of();
			stationsForList = ObjectImmutableList.of();
		} else {
			exitsForList = new ObjectImmutableList<>(EditStationScreen.getExitsForDashboardList(EditStationScreen.getStationExits(station, true)));
			platformsForList = PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(station.savedRails));

			final ObjectArraySet<Station> connectingStationsIncludingThisOne = new ObjectArraySet<>(station.connectedStations);
			connectingStationsIncludingThisOne.add(station);
			stationsForList = new ObjectImmutableList<>(MinecraftClientData.convertDataSet(connectingStationsIncludingThisOne));

			final LongAVLTreeSet platformIds = new LongAVLTreeSet();
			connectingStationsIncludingThisOne.forEach(connectingStation -> connectingStation.savedRails.forEach(platform -> platformIds.add(platform.getId())));
			final ObjectArraySet<DashboardListItem> routes = new ObjectArraySet<>();
			final IntAVLTreeSet addedColors = new IntAVLTreeSet();
			MinecraftClientData.getInstance().simplifiedRoutes.forEach(simplifiedRoute -> {
				final int color = simplifiedRoute.getColor();
				if (!addedColors.contains(color) && simplifiedRoute.getPlatforms().stream().anyMatch(platform -> platformIds.contains(platform.getPlatformId()))) {
					routes.add(new DashboardListItem(color, simplifiedRoute.getName().split("\\|\\|")[0], color));
					addedColors.add(color);
				}
			});
			routesForList = new ObjectImmutableList<>(routes);
		}
	}

	@Override
	public void save(CRTSignLayout layout) {
		if (layout == null) {
			return;
		}
		final String json = layout.toJson();
		if (json.equals(lastSavedJson)) {
			return;
		}
		lastSavedJson = json;
		Registry.sendPacketToServer(new PacketUpdateCRTSignLayout(signPos, json));
	}

	@Override
	public ObjectImmutableList<DashboardListItem> getRoutesForList() {
		return routesForList;
	}

	@Override
	public ObjectImmutableList<DashboardListItem> getStationsForList() {
		return stationsForList;
	}

	@Override
	public ObjectImmutableList<DashboardListItem> getExitsForList() {
		return exitsForList;
	}

	@Override
	public ObjectImmutableList<DashboardListItem> getPlatformsForList() {
		return platformsForList;
	}

	/** 判断坐标是否为墙挂牌（墙挂牌布局存在整条牌的起始方块上）。 */
	public static boolean isWallSign(BlockPos pos, org.mtr.mapping.holder.World world) {
		return world != null && world.getBlockState(pos).getBlock().data instanceof BlockCRTRailwaySignWall1;
	}
}
