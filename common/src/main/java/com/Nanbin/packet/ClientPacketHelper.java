package com.Nanbin.packet;

import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign;
import com.Nanbin.Registry.RegBlock.BlockCRTRailwaySignWall1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1;
import com.Nanbin.Registry.RegBlock.BlockCRTStationInfo1Double;
import com.Nanbin.client.CRTRailwaySignEditor.editor.CRTSignEditorHost;
import com.Nanbin.client.CRTRailwaySignEditor.editor.CRTSignEditorHostImpl;
import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignLegacyImporter;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignDataResolver;
import com.Nanbin.client.Screen.BusTicketProcessorScreen;
import com.Nanbin.client.Screen.CRTSignDoubleScreen;
import com.Nanbin.client.Screen.CRTSignEditorScreen;
import com.Nanbin.client.Screen.RoadNameScreen;
import com.Nanbin.client.Screen.StationInfoScreen;
import com.Nanbin.client.Screen.TicketMachineScreen;
import com.Nanbin.mapping.Registry;
import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignLayout;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import mtr.block.BlockRailwaySign;
import mtr.client.ClientData;
import mtr.data.NameColorDataBase;
import mtr.data.Station;
import mtr.screen.DashboardListSelectorScreen;
import mtr.screen.RailwaySignScreen;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.Predicate;

public class ClientPacketHelper {

	/**
	 * 按方块实体类型分派打开对应的界面（来源 {@code openBlockEntityScreen}）。
	 *
	 * <p>不能用服务端世界读方块实体（客户端读不到）——这里用客户端世界按坐标判类型：
	 * 墙挂式 CRT 指示牌 → 整条牌的布局编辑器；CRT 铁路指示牌 → 布局编辑器；
	 * 其余情况回退到 MTR 原版铁路指示牌界面。</p>
	 */
	public static void openBlockEntityScreen(BlockPos blockPos) {
		final ClientWorld world = MinecraftClient.getInstance().world;
		if (world != null) {
			final BlockState state = world.getBlockState(blockPos);
			if (state.getBlock() instanceof BlockCRTRailwaySignWall1) {
				openCRTSignWallEditor(world, blockPos, state);
				return;
			}
			if (state.getBlock() instanceof BlockCRTRailwaySign) {
				openCRTSignEditor(world, blockPos);
				return;
			}
		}
		openRailwaySignScreen(blockPos);
	}

	/** 打开 CRT 站台/导视牌界面（来源 {@code PacketOpenCRTPlatformScreen}，语义等价于原版铁路指示牌界面）。 */
	public static void openCRTPlatformScreen(BlockPos blockPos) {
		openRailwaySignScreen(blockPos);
	}

	/**
	 * 打开 CRT 铁路指示牌的自由布局编辑器（{@link CRTSignEditorScreen}，网页编辑器风格）。
	 *
	 * <p>数据流：方块实体 NBT 里的 {@code crt_sign_layout} JSON → 布局模型 → 编辑器；
	 * 保存走 {@link PacketHandler#sendUpdateSignLayout}。旧存档没有布局时先用 signIds 自动转一份
	 * （关闭时若不修改则不回写，世界渲染继续走旧格子渲染，外观不变）。</p>
	 */
	public static void openCRTSignEditor(ClientWorld world, BlockPos blockPos) {
		final BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if (blockEntity instanceof BlockRailwaySign.TileEntityRailwaySign entity) {
			final LongFunction<String> routeNumberGetter;
			final String savedLayoutJson;
			if (entity instanceof BlockCRTRailwaySign.BlockEntityCRTRailwaySign crtEntity) {
				routeNumberGetter = crtEntity::getRouteNumber;
				savedLayoutJson = crtEntity.getSignLayout();
			} else {
				routeNumberGetter = color -> "";
				savedLayoutJson = "";
			}
			final boolean hasSavedLayout = savedLayoutJson != null && !savedLayoutJson.isEmpty();
			CRTSignLayout layout = hasSavedLayout ? CRTSignLayout.fromJson(savedLayoutJson) : null;
			if (layout == null) {
				layout = CRTSignLegacyImporter.convert(entity.getSignIds(), new LongAVLTreeSet(entity.getSelectedIds()), routeNumberGetter);
			}
			final CRTSignDataContext data = new CRTSignDataResolver(routeNumberGetter, findStationForSelection(blockPos));
			final String baselineJson = hasSavedLayout ? savedLayoutJson : layout.toJson();
			final CRTSignEditorHost host = new CRTSignEditorHostImpl(blockPos, baselineJson);
			final CRTSignEditorScreen screen = new CRTSignEditorScreen(null, blockPos, layout, data, host);
			openScreen(screen, currentScreen -> currentScreen instanceof CRTSignEditorScreen);
			return;
		}
		// 兜底：实体类型不符时仍用原版界面，避免刷子点击没有任何反应
		openRailwaySignScreen(blockPos);
	}

	/** 墙挂指示牌：任意一格用刷子打开整条牌的编辑器；布局存在整条牌的起始方块实体上。 */
	public static void openCRTSignWallEditor(ClientWorld world, BlockPos blockPos, BlockState state) {
		final BlockPos start = BlockCRTRailwaySignWall1.findStart(world, blockPos, state);
		final List<BlockPos> run = BlockCRTRailwaySignWall1.collectRun(world, start);
		if (run.isEmpty()) {
			return;
		}
		// 每个方块两格内容：旧数据转换时按格展开，长度 = 方块数 × 2
		final int cellsPerBlock = BlockCRTRailwaySignWall1.CELLS_PER_BLOCK;
		final String[] cells = new String[run.size() * cellsPerBlock];
		final LongAVLTreeSet selectedIds = new LongAVLTreeSet();
		LongFunction<String> routeNumberGetter = color -> "";
		String savedLayoutJson = "";
		for (int i = 0; i < run.size(); i++) {
			final BlockCRTRailwaySignWall1.BlockEntity entity = BlockCRTRailwaySignWall1.getBlockEntity(world, run.get(i));
			if (entity != null) {
				for (int j = 0; j < cellsPerBlock; j++) {
					cells[i * cellsPerBlock + j] = entity.getSignId(j);
				}
				selectedIds.addAll(entity.getSelectedIds());
				if (i == 0) {
					routeNumberGetter = entity::getRouteNumber;
					savedLayoutJson = entity.getSignLayout();
				}
			}
		}
		final boolean hasSavedLayout = savedLayoutJson != null && !savedLayoutJson.isEmpty();
		CRTSignLayout layout = hasSavedLayout ? CRTSignLayout.fromJson(savedLayoutJson) : null;
		if (layout == null) {
			layout = CRTSignLegacyImporter.convert(cells, selectedIds, routeNumberGetter);
		}
		final CRTSignDataContext data = new CRTSignDataResolver(routeNumberGetter, findStationForSelection(start));
		final String baselineJson = hasSavedLayout ? savedLayoutJson : layout.toJson();
		final CRTSignEditorHost host = new CRTSignEditorHostImpl(start, baselineJson);
		final CRTSignEditorScreen screen = new CRTSignEditorScreen(null, start, layout, data, host);
		openScreen(screen, currentScreen -> currentScreen instanceof CRTSignEditorScreen);
	}

	public static void openRailwaySignScreen(BlockPos blockPos) {
		openScreen(new RailwaySignScreen(blockPos), screen -> screen instanceof RailwaySignScreen);
	}

	public static void openRoadNameScreen(BlockPos blockPos, String[] texts) {
		openScreen(new RoadNameScreen(blockPos, texts), screen -> screen instanceof RoadNameScreen);
	}

	public static void saveRoadNameScreen(BlockPos blockPos, String[] texts) {
		Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_ROAD_NAME, buf -> {
			buf.writeBlockPos(blockPos);
			buf.writeVarInt(texts.length);
			for (String text : texts) {
				buf.writeString(text);
			}
		});
	}

	public static void openTicketMachineScreen(int balance) {
		openScreen(new TicketMachineScreen(balance), screen -> screen instanceof TicketMachineScreen);
	}

	public static void openBusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
		openScreen(new BusTicketProcessorScreen(blockPos, mode, amount), screen -> screen instanceof BusTicketProcessorScreen);
	}

	public static void openStationInfoScreen(BlockPos blockPos, String url, boolean isFront) {
		openScreen(new StationInfoScreen(blockPos, url, isFront), screen -> screen instanceof StationInfoScreen);
	}

	/** 保存站台信息屏的 URL 与所选站台。 */
	public static void saveStationInfoScreen(BlockPos blockPos, String url, LongAVLTreeSet selectedIds, boolean isFront) {
		Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_STATION_INFO, buf -> {
			buf.writeBlockPos(blockPos);
			buf.writeString(url == null ? "" : url);
			buf.writeVarInt(selectedIds.size());
			for (final long id : selectedIds) {
				buf.writeLong(id);
			}
			buf.writeBoolean(isFront);
		});
	}

	/** 打开站台选择器（选中站台的 ID 直接写回传入的集合）；选完立即回到配置界面并保存。 */
	public static void openPlatformSelectionScreen(BlockPos blockPos, LongAVLTreeSet selectedIds, StationInfoScreen parent) {
		final Station station = findStationForSelection(blockPos);
		final List<NameColorDataBase> platformsForList = new ArrayList<>();
		if (station != null) {
			platformsForList.addAll(ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
			platformsForList.sort(Comparator.comparing(data -> data.name));
		}
		MinecraftClient.getInstance().setScreen(new DashboardListSelectorScreen(() -> {
			MinecraftClient.getInstance().setScreen(parent);
			saveStationInfoScreen(blockPos, parent.getUrl(), selectedIds, parent.isFront());
		}, platformsForList, selectedIds, false, false));
	}

	/**
	 * 优先精确匹配方块所在站区域；其次找最近的站台所属站；最后取全图最近的站。
	 * <p>
	 * 指示牌常被挂在车站外的出入口处，只做区域精确匹配会得到 null，
	 * 选择屏就会一个数据按钮都不显示，所以统一走这里的兜底链。
	 */
	public static Station findStationForSelection(BlockPos blockPos) {
		final Station direct = ClientData.DATA_CACHE.blockPosToStation.get(blockPos);
		if (direct != null) {
			return direct;
		}
		for (final Station station : ClientData.STATIONS) {
			if (station.inArea(blockPos.getX(), blockPos.getZ())) {
				return station;
			}
		}
		// 其次：最近站台所属的车站（MTR 3 的 getClosePlatformId 即来源 InitClient.findClosePlatform）。
		try {
			final long closePlatformId = mtr.data.RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, blockPos);
			if (closePlatformId != 0L) {
				final Station closeStation = ClientData.DATA_CACHE.platformIdToStation.get(closePlatformId);
				if (closeStation != null) {
					return closeStation;
				}
			}
		} catch (Exception ignored) {
			// 客户端数据尚未同步完成时继续走最后一层
		}
		// 最后：全图曼哈顿距离最近的站。
		Station nearest = null;
		long nearestDistance = Long.MAX_VALUE;
		for (final Station station : ClientData.STATIONS) {
			final BlockPos center = station.getCenter();
			if (center == null) {
				continue;
			}
			final long distance = Math.abs((long) center.getX() - blockPos.getX())
					+ Math.abs((long) center.getY() - blockPos.getY())
					+ Math.abs((long) center.getZ() - blockPos.getZ());
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = station;
			}
		}
		return nearest;
	}

	// ─── 车站查询缓存（渲染端用） ───────────────────────────────────────────

	/** 车站查询缓存有效期（毫秒）：出口信息条每次布局都会问同一坐标，避免反复遍历 MTR 数据。 */
	private static final long STATION_CACHE_TTL_MS = 2000L;
	/** 车站查询缓存条目上限（一个世界里同时可见的指示牌数量级）。 */
	private static final int STATION_CACHE_LIMIT = 64;
	private static final java.util.Map<Long, StationCacheEntry> STATION_CACHE =
			new java.util.LinkedHashMap<Long, StationCacheEntry>(16, 0.75F, true) {
				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<Long, StationCacheEntry> eldest) {
					return size() > STATION_CACHE_LIMIT;
				}
			};

	/** 一次车站查询的结果（{@code station} 允许为 null：查询过但当前没有车站）。 */
	private static final class StationCacheEntry {
		private final Station station;
		private final long time;

		private StationCacheEntry(Station station, long time) {
			this.station = station;
			this.time = time;
		}
	}

	/**
	 * {@link #findStationForSelection(BlockPos)} 的缓存版：世界渲染每个牌子每帧都会调用，
	 * 而精确匹配/最近站台/全图最近站三层兜底并不便宜，因此按坐标缓存 2 秒。
	 *
	 * <p>MTR 数据同步完成前可能返回 null，缓存也会把 null 记下来（2 秒后重试）。</p>
	 */
	public static Station findStationForSelectionCached(BlockPos blockPos) {
		if (blockPos == null) {
			return null;
		}
		// 用 asLong() 当键：渲染每个牌子每帧都会调用，避免反复生成坐标字符串
		final long key = blockPos.asLong();
		final long now = System.currentTimeMillis();
		synchronized (STATION_CACHE) {
			final StationCacheEntry cached = STATION_CACHE.get(key);
			if (cached != null && now - cached.time < STATION_CACHE_TTL_MS) {
				return cached.station;
			}
		}
		final Station station = findStationForSelection(blockPos);
		synchronized (STATION_CACHE) {
			STATION_CACHE.put(key, new StationCacheEntry(station, now));
		}
		return station;
	}

	/** 打开 StationInfo 专用的双行指示牌编辑器（2 行 × N 格，行/列/数据由调用方传入）。 */
	public static void openCRTSignDoubleScreen(BlockPos blockPos, boolean isFront) {
		final ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) {
			return;
		}
		final BlockEntity blockEntity = world.getBlockEntity(blockPos);
		final int lines;
		final int length;
		final String[][] signIds;
		final List<LongAVLTreeSet> selectedIds;
		if (blockEntity instanceof BlockCRTStationInfo1.BlockEntity entity) {
			lines = BlockCRTStationInfo1.BlockEntity.SIGN_LINES;
			length = BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
			signIds = entity.getSignIds();
			selectedIds = entity.getSelectedIds();
		} else if (blockEntity instanceof BlockCRTStationInfo1Double.BlockEntity entity) {
			lines = BlockCRTStationInfo1Double.BlockEntity.SIGN_LINES;
			length = BlockCRTStationInfo1Double.BlockEntity.SIGN_LENGTH;
			signIds = entity.getSignIds(isFront);
			selectedIds = entity.getSelectedIds(isFront);
		} else {
			lines = BlockCRTStationInfo1.BlockEntity.SIGN_LINES;
			length = BlockCRTStationInfo1.BlockEntity.SIGN_LENGTH;
			signIds = new String[lines][length];
			selectedIds = new ArrayList<>();
			for (int i = 0; i < lines; i++) {
				selectedIds.add(new LongAVLTreeSet());
			}
		}
		final boolean hasData = blockEntity != null;
		final String[][] finalSignIds = signIds;
		final List<LongAVLTreeSet> finalSelectedIds = selectedIds;
		openScreen(new CRTSignDoubleScreen(blockPos, lines, length, finalSignIds, finalSelectedIds, hasData,
				() -> saveStationInfoSign(blockPos, finalSignIds, finalSelectedIds, isFront)),
				screen -> screen instanceof CRTSignDoubleScreen);
	}

	private static void saveStationInfoSign(BlockPos blockPos, String[][] signIds, List<LongAVLTreeSet> selectedIds, boolean isFront) {
		Registry.sendPacketToServer(PacketHandler.PACKET_UPDATE_STATION_INFO_SIGN, buf -> {
			buf.writeBlockPos(blockPos);
			buf.writeVarInt(signIds.length);
			for (final String[] lineIds : signIds) {
				buf.writeVarInt(lineIds.length);
				for (final String signId : lineIds) {
					buf.writeString(signId == null ? "" : signId);
				}
			}
			for (int i = 0; i < signIds.length; i++) {
				final LongAVLTreeSet lineIds = i < selectedIds.size() ? selectedIds.get(i) : new LongAVLTreeSet();
				buf.writeVarInt(lineIds.size());
				for (final long id : lineIds) {
					buf.writeLong(id);
				}
			}
			buf.writeBoolean(isFront);
		});
	}

	public static void syncTicketMachineBalance(int balance) {
		final Screen currentScreen = MinecraftClient.getInstance().currentScreen;
		if (currentScreen instanceof TicketMachineScreen screen) {
			screen.updateBalance(balance);
		}
	}

	public static void openScreen(Screen screen, Predicate<Screen> isInstance) {
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final Screen currentScreen = minecraftClient.currentScreen;
		if (currentScreen == null || !isInstance.test(currentScreen)) {
			minecraftClient.setScreen(screen);
		}
	}
}
