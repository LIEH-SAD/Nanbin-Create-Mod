package com.Nanbin.packet;

import com.Nanbin.Init;
import com.Nanbin.mapping.Registry;
import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Position;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.client.MinecraftClientData;
import org.mtr.MTRClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 网络包的客户端落地逻辑。
 * <p>
 * 4.1 适配：{@code org.mtr.mod.InitClient.findStation/findClosePlatform} → {@link MTRClient}；
 * {@code org.mtr.mod.client.MinecraftClientData} → {@code org.mtr.client.MinecraftClientData}；
 * {@code org.mtr.mod.screen.*} → {@code org.mtr.screen.*}；
 * 4.0 的 {@code ScreenExtension} 包装层在 4.1 被删掉了，界面统一改成原版 {@code Screen}。
 * <p>
 * <b>关于 4.0 的 {@code RailwaySignScreenMixin}（未移植，原因如下）：</b>
 * 那个 mixin 往 MTR 自己的 {@code org.mtr.mod.screen.RailwaySignScreen} 里注入
 * 复制/粘贴/清空/撤销、JS 样式选择、线路/车站选择器等功能，依赖它的字段
 * （{@code editingIndex}/{@code buttonsEdit}/{@code buttonsSelection}/{@code buttonClear}/
 * {@code routesForList}）与方法（{@code edit}/{@code setNewSignId}/{@code init2}）。
 * MTR 4.1 把这个界面<b>整个重写</b>成 elementa（{@code WindowBase} + {@code SignPreviewComponent} +
 * {@code SignButtonsComponent}），类变成 {@code final}，上面那些字段与方法一个都不存在，
 * 注入点已无处可挂，因此无法照搬。
 * <p>
 * 4.1 下的等效能力分布为：
 * <ul>
 *     <li>普通 / CRT 铁路告示牌：由 MTR 4.1 自己的新界面负责（搜索、预览、逐格配色、
 *     出口/站台/线路/车站选择器都是它的原生能力），见 {@link #openBlockEntityScreen}；</li>
 *     <li>CRT 站台信息屏这类多行指示牌：由本项目的
 *     {@link com.Nanbin.client.Screen.RailwaySignDoubleScreen} 负责，
 *     复制/粘贴/清空/撤销与 JS 样式选择都保留在那里。</li>
 * </ul>
 */
public final class ClientPacketHelper {

	private ClientPacketHelper() {
	}

	public static void openBlockEntityScreen(BlockPos blockPos) {
		openRailwaySignScreen(blockPos);
	}

	/**
	 * 打开方块实体对应的界面。
	 * <p>
	 * 4.0 是打开 MTR 自己的 {@code RailwaySignScreen}（再由 mixin 加料）；4.1 里 MTR 的
	 * {@link org.mtr.packet.ClientPacketHelper#openBlockEntityScreen} 已经按方块实体类型分派好了
	 * （告示牌 → 新的导向标志界面，PIDS → PIDS 配置界面……）。
	 * <p>
	 * 但 CRT 告示牌用自己那套界面（{@link com.Nanbin.signeditor.RailwaySignEditorScreen}），
	 * 原因见类注释：MTR 4.1 把自己的 {@code RailwaySignScreen} 写成 {@code final} 的 elementa 界面，
	 * 4.0 那个往里面注入复制/粘贴/清空/撤销、JS 样式、CRT 专属数据选择的 mixin 已经无处可挂。
	 * 其余告示牌继续交给 MTR。
	 */
	public static void openRailwaySignScreen(BlockPos blockPos) {
		final net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (client != null && client.world != null
				&& client.world.getBlockEntity(blockPos) instanceof com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity) {
			openRailwaySignEditor(client.currentScreen, blockPos, entity);
			return;
		}
		org.mtr.packet.ClientPacketHelper.openBlockEntityScreen(blockPos);
	}

	/** CRT 铁路告示牌：数据直接改方块实体里的活数组，保存时整块发回服务端。 */
	private static void openRailwaySignEditor(net.minecraft.client.gui.screen.Screen parent, BlockPos blockPos, com.Nanbin.Registry.RegBlock.BlockCRTRailwaySign.BlockEntityCRTRailwaySign entity) {
		final String[] signIdArray = entity.getSignIds();
		if (signIdArray == null || signIdArray.length == 0) {
			return;
		}
		// signIds 是活引用，界面直接改它；selectedIds 逐格，原样交给界面（逐格配色要保住）
		final String[][] signIds = new String[][]{signIdArray};
		final List<LongAVLTreeSet> selectedIds = new ArrayList<>(Arrays.asList(entity.getSelectedIds()));
		com.Nanbin.mapping.PlatformHolder.get().openRailwaySignEditor(parent, blockPos, signIdArray.length, signIds, selectedIds, entity.getRouteNumbers(), () ->
				Registry.sendPacketToServer(new org.mtr.packet.PacketUpdateRailwaySignConfig(blockPos, selectedIds.toArray(new LongAVLTreeSet[0]), signIds[0])));
	}

	public static void openRoadNameScreen(BlockPos blockPos, String[] texts) {
		openScreen(new com.Nanbin.client.Screen.RoadNameScreen(blockPos, texts));
	}

	public static void openTicketMachineScreen(int balance) {
		openScreen(new com.Nanbin.client.Screen.TicketMachineScreen(balance));
	}

	public static void openBusTicketProcessorScreen(BlockPos blockPos, int mode, int amount) {
		openScreen(new com.Nanbin.client.Screen.BusTicketProcessorScreen(blockPos, mode, amount));
	}

	public static void syncTicketMachineBalance(int balance) {
		final net.minecraft.client.gui.screen.Screen screen = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
		if (screen instanceof com.Nanbin.client.Screen.TicketMachineScreen ticketMachineScreen) {
			ticketMachineScreen.updateBalance(balance);
		}
	}

	public static void saveRoadNameScreen(BlockPos blockPos, String[] texts) {
		Registry.sendPacketToServer(new PacketUpdateRoadNameData(blockPos, texts));
	}

	public static void openStationInfoScreen(BlockPos blockPos, String url, boolean isFront) {
		openScreen(new com.Nanbin.client.Screen.StationInfoScreen(blockPos, url, isFront));
	}

	public static void saveStationInfoScreen(BlockPos blockPos, String url, LongAVLTreeSet selectedIds, boolean isFront) {
		Registry.sendPacketToServer(new PacketUpdateStationInfoData(blockPos, url, selectedIds, isFront));
	}

	/**
	 * 打开站台选择界面。
	 * <p>
	 * 直接用 MTR 自带的 {@link org.mtr.screen.PlatformListSelectorScreen}（用法与 MTR 自己的
	 * {@code PIDSConfigScreen.openPlatformFilter} 完全一致），不再用本项目自建的
	 * {@code DashboardListSelectorScreen}：MTR 那个自带搜索、按线路分组与多选，外观和操作也与其他
	 * MTR 界面统一。
	 * <p>
	 * 站台候选来源也照抄 MTR：先取方块所在站的全部站台，取不到再按 {@code (pos.below(4), 5)}
	 * 找附近的站台。选完后由 {@link LegacyScreenWindow} 记录的上一屏自动返回父界面。
	 */
	public static void openPlatformSelectionScreen(BlockPos blockPos, LongAVLTreeSet selectedIds, Object parentScreen) {
		final ObjectArraySet<org.mtr.core.data.Platform> availablePlatforms = new ObjectArraySet<>();
		final Station station = MTRClient.findStation(blockPos);
		if (station != null) {
			availablePlatforms.addAll(station.savedRails);
		} else {
			MTRClient.findClosePlatform(blockPos.down(4), 5, availablePlatforms::add);
		}
		if (availablePlatforms.isEmpty()) {
			// 附近仍无可识别站台时，退回「最近的站」的站台列表（4.0 的 findStationForSelection 语义）
			final Station fallback = findStationForSelection(blockPos);
			if (fallback != null) {
				availablePlatforms.addAll(fallback.savedRails);
			}
		}

		final net.minecraft.client.gui.screen.Screen parent = parentScreen instanceof net.minecraft.client.gui.screen.Screen screen ? screen : null;
		final org.mtr.screen.PlatformListSelectorScreen selector = new org.mtr.screen.PlatformListSelectorScreen(platforms -> {
			selectedIds.clear();
			for (final org.mtr.core.data.Platform platform : platforms) {
				selectedIds.add(platform.getId());
			}
		}, new com.Nanbin.client.Screen.MtrSignSelectors.ParentWindow(parent));
		selector.setAvailableList(availablePlatforms);
		availablePlatforms.forEach(platform -> {
			if (selectedIds.contains(platform.getId())) {
				selector.selectData(platform);
			}
		});
		// MTR 的 elementa 界面最终也继承原版 Screen（WindowScreen → UScreen → Screen），
		// 所以直接用原版 setScreen 即可 —— 不需要碰 UMinecraft：
		// 那个类在两端包名不同（Fabric 是 gg.essential.*，NeoForge 被重定位成 org.mtr.libraries.gg.essential.*）。
		openScreen(selector);
	}

	/** 优先精确匹配方块所在站区域；其次找最近的站台所属站；最后取全图最近的站。 */
	private static Station findStationForSelection(BlockPos blockPos) {
		final Station exact = MTRClient.findStation(blockPos);
		if (exact != null) {
			return exact;
		}
		final Station[] closeStation = {null};
		MTRClient.findClosePlatform(blockPos, 256, platform -> closeStation[0] = platform.area);
		if (closeStation[0] != null) {
			return closeStation[0];
		}
		final Position position = org.mtr.MTR.blockPosToPosition(blockPos);
		Station nearest = null;
		long nearestDistance = Long.MAX_VALUE;
		for (final Station station : MinecraftClientData.getInstance().stations) {
			final long distance = station.getCenter().manhattanDistance(position);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = station;
			}
		}
		return nearest;
	}

	/** 打开 StationInfo 专用的多行指示牌编辑器（2 行 × 7 格）。 */
	public static void openRailwaySignDoubleScreen(BlockPos blockPos, boolean isFront) {
		final net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (client == null || client.world == null) {
			return;
		}

		final int lines;
		final int length;
		final String[][] signIds;
		final List<LongAVLTreeSet> selectedIds;
		boolean hasData = false;

		final net.minecraft.block.entity.BlockEntity blockEntity = client.world.getBlockEntity(blockPos);
		if (blockEntity instanceof com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase.BlockEntityBase entity) {
			lines = com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase.BlockEntityBase.SIGN_LINES;
			length = com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase.BlockEntityBase.SIGN_LENGTH;
			signIds = entity.getSignIds(isFront);
			selectedIds = entity.getSelectedIds(isFront);
			hasData = true;
		} else {
			lines = com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase.BlockEntityBase.SIGN_LINES;
			length = com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase.BlockEntityBase.SIGN_LENGTH;
			signIds = new String[lines][length];
			selectedIds = new ArrayList<>();
			for (int i = 0; i < lines; i++) {
				selectedIds.add(new LongAVLTreeSet());
			}
		}

		// signIds / selectedIds 都是方块实体里的活引用，界面直接改它们，保存时整块发回服务端
		// parent 用当前界面，这样关掉编辑器能回到站台信息屏
		// 线路编号（供预览里的「线路色竖条 + 编号」用）按行传：双层屏的两行共用整块的编号表
		final String[][] routeNumbers = new String[lines][];
		final String[] flatRouteNumbers = blockEntity instanceof com.Nanbin.Registry.RegBlock.BlockCRTStationInfoBase.BlockEntityBase stationInfoEntity
				? stationInfoEntity.getRouteNumbers(isFront) : new String[0];
		for (int i = 0; i < lines; i++) {
			routeNumbers[i] = flatRouteNumbers;
		}
		com.Nanbin.mapping.PlatformHolder.get().openSignEditor(client.currentScreen, blockPos, lines, length, signIds, selectedIds, routeNumbers, hasData,
				() -> Registry.sendPacketToServer(new PacketUpdateStationInfoSignData(blockPos, signIds, selectedIds, isFront)));
	}

	/** 打开一个已经建好的原版界面（供界面之间跳转使用）。 */
	public static void openScreen(net.minecraft.client.gui.screen.Screen screen) {
		final net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(screen);
		}
	}
}
