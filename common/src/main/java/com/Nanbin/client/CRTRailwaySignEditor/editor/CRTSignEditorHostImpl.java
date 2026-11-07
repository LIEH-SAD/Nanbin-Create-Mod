package com.Nanbin.client.CRTRailwaySignEditor.editor;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import com.Nanbin.mapping.MtrData;
import com.Nanbin.packet.ClientPacketHelper;
import com.Nanbin.packet.PacketHandler;
import com.Nanbin.signage.crt.CRTSignLayout;
import mtr.block.BlockRailwaySign;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.DataConverter;
import mtr.data.IGui;
import mtr.data.NameColorDataBase;
import mtr.data.Platform;
import mtr.data.Station;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link CRTSignEditorHost} 的标准实现：保存布局 → 发 {@code PacketHandler#sendUpdateSignLayout}
 * 给服务端；选择列表 → 取方块所属车站的 MTR 客户端数据（与 MTR 自己的选择屏同一份数据）。
 */
public final class CRTSignEditorHostImpl implements CRTSignEditorHost {

	private final BlockPos signPos;
	private final List<NameColorDataBase> routesForList;
	private final List<NameColorDataBase> stationsForList;
	private final List<NameColorDataBase> exitsForList;
	private final List<NameColorDataBase> platformsForList;
	private String lastSavedJson = "";

	@Override
	public BlockPos getSignPos() {
		return signPos;
	}

	@Override
	public int getSignCells() {
		try {
			final World world = MinecraftClient.getInstance().world;
			if (world == null) {
				return 0;
			}
			final BlockState state = world.getBlockState(signPos);
			if (state.getBlock() instanceof BlockRailwaySign railwaySign) {
				return railwaySign.length;
			}
			if (state.getBlock() instanceof BlockCRTRailwaySignWall1) {
				final List<BlockPos> run = BlockCRTRailwaySignWall1.collectRun(world, signPos);
				return Math.max(1, run.size() * BlockCRTRailwaySignWall1.CELLS_PER_BLOCK);
			}
		} catch (Exception ignored) {
		}
		return 0;
	}

	@Override
	public Direction getFacing() {
		try {
			final World world = MinecraftClient.getInstance().world;
			if (world != null) {
				final BlockState state = world.getBlockState(signPos);
				if (state.getBlock() instanceof BlockRailwaySign) {
					return IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
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
		// 与数据选择屏一致：车站出入口外的指示牌也要能找到所属车站
		final Station station = ClientPacketHelper.findStationForSelection(signPos);
		if (station == null) {
			exitsForList = Collections.emptyList();
			platformsForList = Collections.emptyList();
			routesForList = Collections.emptyList();
			stationsForList = Collections.emptyList();
		} else {
			final List<NameColorDataBase> exits = new ArrayList<>();
			for (final MtrData.StationExit exit : MtrData.stationExits(station)) {
				exits.add(new DataConverter(MtrData.serializeExit(exit.name()), exit.name() + " " + (!exit.destinations().isEmpty() ? exit.destinations().get(0) : ""), 0));
			}
			exitsForList = exits;

			final List<Platform> platforms = new ArrayList<>(ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
			Collections.sort(platforms);
			final List<NameColorDataBase> platformItems = new ArrayList<>();
			for (final Platform platform : platforms) {
				platformItems.add(new DataConverter(platform.id, platform.name + " " + IGui.mergeStations(ClientData.DATA_CACHE.requestPlatformIdToRoutes(platform.id).stream().map(route -> route.stationDetails.get(route.stationDetails.size() - 1).stationName).collect(Collectors.toList())), 0));
			}
			platformsForList = platformItems;

			final List<NameColorDataBase> stations = new ArrayList<>();
			ClientData.DATA_CACHE.getConnectingStationsIncludingThisOne(station).forEach(connectingStation -> stations.add(new DataConverter(connectingStation.id, connectingStation.name, connectingStation.color)));
			stationsForList = stations;

			final List<NameColorDataBase> routes = new ArrayList<>();
			ClientData.DATA_CACHE.getAllRoutesIncludingConnectingStations(station).forEach((color, route) -> routes.add(new DataConverter(route.color, route.name, route.color)));
			routesForList = routes;
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
		PacketHandler.sendUpdateSignLayout(signPos, json);
	}

	@Override
	public List<NameColorDataBase> getRoutesForList() {
		return routesForList;
	}

	@Override
	public List<NameColorDataBase> getStationsForList() {
		return stationsForList;
	}

	@Override
	public List<NameColorDataBase> getExitsForList() {
		return exitsForList;
	}

	@Override
	public List<NameColorDataBase> getPlatformsForList() {
		return platformsForList;
	}

	/** 判断坐标是否为墙挂牌（墙挂牌布局存在整条牌的起始方块上）。 */
	public static boolean isWallSign(BlockPos pos, @Nullable World world) {
		return world != null && world.getBlockState(pos).getBlock() instanceof BlockCRTRailwaySignWall1;
	}
}
