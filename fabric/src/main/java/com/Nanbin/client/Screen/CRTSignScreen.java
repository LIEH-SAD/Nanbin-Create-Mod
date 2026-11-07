package com.Nanbin.client.Screen;

import com.Nanbin.client.ClientData.CRTSignClipboard;
import com.Nanbin.client.ClientData.CRTSignRegistry;
import com.Nanbin.client.Drawing.CRTSignPreview;
import com.Nanbin.client.Drawing.CRTSignView;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignEngine;
import com.Nanbin.packet.ClientPacketHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.resource.SignResource;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.mtr.mod.screen.EditStationScreen;
import org.mtr.mod.screen.PIDSConfigScreen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.LongFunction;

/**
 * CRT 铁路指示牌专用编辑界面（暗色主题）。
 *
 * <h2>来源与许可</h2>
 * <p>界面布局、元素分类（预览 / 分类标志调色板 / 当前格属性 / 底部工具栏）参考了开源项目
 * <b>CRT-Sign-Web-Editor</b>（Apache License 2.0）：原作者 endcreeper861 的
 * JR-Guidance-Sign-Web-Editor，后由 PrisamaX0124 fork 为 CRT-Sign-Web-Editor：
 * https://github.com/prisamax0124/CRT-Sign-Web-Editor 。
 * 本类是用 MTR 映射层（GuiDrawing / GraphicsHolder）重写的 Minecraft 端 Java 实现，
 * 只沿用其界面组织方式与重庆轨道交通导视风格，未复制其源码；字体与图标素材也未打包进本 mod，
 * 仍归各自权利人。完整声明见 docs/THIRD_PARTY_NOTICES.md。</p>
 *
 * <h2>设计要点</h2>
 * <ul>
 *   <li>本屏幕只负责编辑「CRT 铁路指示牌」，与 MTR 原版 {@code RailwaySignScreen} 的数据、
 *       剪贴板（{@link CRTSignClipboard}）完全独立，原版导视牌行为不受影响。</li>
 *   <li>构造时把调用方传入的数据整体拷贝到屏幕内部，所有修改都只作用于内部副本；
 *       每次改动后通过 {@link BiConsumer} 回调把「当前格子内容 + 选中数据」交给调用方保存。</li>
 *   <li>顶部预览直接调用 {@link CRTSignPreview}，与世界中的实际渲染同源，所见即所得。</li>
 *   <li>暗色主题全部自绘（GuiDrawing），不使用原版浅色按钮；配色：背景 #14161A、
 *       面板 #1E2228、强调 #4FC3F7、正文 #E6E6E6。</li>
 *   <li>启用 JS 样式（{@link JSSignConfig#hasJSStyle(String[])}）后整块牌由脚本渲染，
 *       此时隐藏调色板与属性编辑，只保留预览、JS 样式按钮、数据选择（{@link JSSignDataScreen}）
 *       以及复制／粘贴／清空／撤销。</li>
 * </ul>
 */
public class CRTSignScreen extends ScreenExtension {

	// ---------------------------------------------------------------- 配色

	/** 全屏背景。 */
	private static final int COLOR_BACKGROUND = 0xFF14161A;
	/** 顶栏背景。 */
	private static final int COLOR_TOP_BAR = 0xFF101216;
	/** 面板背景。 */
	private static final int COLOR_PANEL = 0xFF1E2228;
	/** 面板悬停背景。 */
	private static final int COLOR_PANEL_HOVER = 0xFF272E36;
	/** 禁用控件背景。 */
	private static final int COLOR_PANEL_DISABLED = 0xFF191C20;
	/** 普通描边。 */
	private static final int COLOR_BORDER = 0xFF2C333B;
	/** 高亮描边。 */
	private static final int COLOR_BORDER_LIGHT = 0xFF3A444E;
	/** 强调色（#4FC3F7）。 */
	private static final int COLOR_ACCENT = 0xFF4FC3F7;
	/** 强调色背景。 */
	private static final int COLOR_ACCENT_DIM = 0xFF24343E;
	/** 强调控件悬停背景。 */
	private static final int COLOR_ACCENT_HOVER = 0xFF31586B;
	/** 正文文字。 */
	private static final int COLOR_TEXT = 0xFFE6E6E6;
	/** 次要文字。 */
	private static final int COLOR_TEXT_DIM = 0xFF8A939C;
	/** 禁用文字。 */
	private static final int COLOR_TEXT_DISABLED = 0xFF565D66;
	/** 警告色（二次确认）。 */
	private static final int COLOR_WARNING = 0xFFFFB74D;
	/** 警告控件背景。 */
	private static final int COLOR_WARNING_DIM = 0xFF3A2C1A;
	/** 警告控件悬停背景。 */
	private static final int COLOR_WARNING_HOVER = 0xFF5C4522;

	// ---------------------------------------------------------------- 布局尺寸

	private static final int MARGIN = 6;
	private static final int TOP_BAR_HEIGHT = 20;
	private static final int TOOLBAR_HEIGHT = 22;
	private static final int PALETTE_TAB_HEIGHT = 14;
	private static final int PAGE_ROW_HEIGHT = 14;
	private static final int CELL_STEP = 20;
	private static final int ICON_SIZE = 16;
	private static final int BUTTON_HEIGHT = 18;

	// ---------------------------------------------------------------- 标志分类

	private static final int CATEGORY_ALL = 0;
	private static final int CATEGORY_ARROW = 1;
	private static final int CATEGORY_FACILITY = 2;
	private static final int CATEGORY_EXIT = 3;
	private static final int CATEGORY_ROUTE = 4;
	private static final int CATEGORY_TEXT = 5;
	private static final int CATEGORY_OTHER = 6;
	private static final int CATEGORY_COUNT = 7;
	private static final String[] CATEGORY_LANG_KEYS = {
			"gui.nanbin.crt_sign.category.all",
			"gui.nanbin.crt_sign.category.arrow",
			"gui.nanbin.crt_sign.category.facility",
			"gui.nanbin.crt_sign.category.exit",
			"gui.nanbin.crt_sign.category.route",
			"gui.nanbin.crt_sign.category.text",
			"gui.nanbin.crt_sign.category.other"
	};

	// ---------------------------------------------------------------- 数据

	@Nullable
	private final ScreenExtension parent;
	private final BlockPos signPos;
	private final int length;
	/** 屏幕内部的格子内容副本；所有修改只写这里。 */
	private final String[] signIds;
	/** 屏幕内部的选中数据副本；所有修改只写这里。 */
	private final LongAVLTreeSet selectedIds;
	@Nullable
	private final LongFunction<String> routeNumberGetter;
	@Nullable
	private final long[][] cellColors;
	private final BiConsumer<String[], LongAVLTreeSet> saveAction;
	private final String cacheKey;

	/** MTR 内置标志 + 本 mod crt_signs.json 中 whitelist=crt 的标志。 */
	private final ObjectArrayList<String> allSignIds = new ObjectArrayList<>();
	private final ObjectImmutableList<DashboardListItem> exitsForList;
	private final ObjectImmutableList<DashboardListItem> platformsForList;
	private final ObjectImmutableList<DashboardListItem> routesForList;
	private final ObjectImmutableList<DashboardListItem> stationsForList;
	private final Map<Long, String> exitNames = new LinkedHashMap<>();
	private final Map<Long, String> platformNames = new LinkedHashMap<>();
	private final Map<Long, String> routeNames = new LinkedHashMap<>();
	private final Map<Long, String> stationNames = new LinkedHashMap<>();

	// ---------------------------------------------------------------- 渲染视图

	/** 传给预览的视图；随数据变化重建。 */
	@Nullable
	private CRTSignView view;
	private String[] routeNumbers = new String[0];

	// ---------------------------------------------------------------- 界面状态

	private final List<DarkButton> buttons = new ArrayList<>();
	private final List<DarkButton> paletteButtons = new ArrayList<>();
	/** 当前选中的格子；-1 表示未选。 */
	private int editingIndex = -1;
	private int category = CATEGORY_ALL;
	private int palettePage;
	private int pageCount = 1;
	private String[] filteredSignIds = new String[0];
	private boolean clearArmed;
	@Nullable
	private DarkButton clearButton;
	private int propertyButtonCount;
	@Nullable
	private String statusMessage;
	private long statusUntilMillis;
	@Nullable
	private String tooltip;
	private long lastSavedSignature;

	// ---------------------------------------------------------------- 布局缓存

	private int previewX;
	private int previewY;
	private int previewWidth;
	private int previewHeight;
	private int bodyY;
	private int bodyHeight;
	private int paletteX;
	private int paletteY;
	private int paletteWidth;
	private int paletteHeight;
	private int paletteTabsY;
	private int paletteGridX;
	private int paletteGridY;
	private int paletteGridWidth;
	private int paletteGridHeight;
	private int paletteColumns;
	private int paletteRows;
	private int palettePageSize;
	private int propsX;
	private int propsY;
	private int propsWidth;
	private int propsHeight;
	private int toolbarX;
	private int toolbarY;
	private int toolbarWidth;

	/**
	 * @param parent            返回时重新打开的父屏幕（可为 null，表示直接关闭回游戏）
	 * @param signPos           指示牌方块坐标（用于撤销历史分桶与预览缓存键）
	 * @param length            格子数
	 * @param signIds           每格标志 id（屏幕构造时拷贝）
	 * @param selectedIds       选中的线路颜色 / 车站 / 站台 / 出口 / 自定义文本（屏幕构造时拷贝）
	 * @param routeNumberGetter 线路颜色 -> 线路编号（预览线路号用，可为 null）
	 * @param cellColors        每格独立线路颜色（铁路指示牌传 null 即可，保留给以后墙挂牌用）
	 * @param saveAction        数据变化后的保存回调，参数为屏幕内部最新的 (signIds, selectedIds)
	 */
	public CRTSignScreen(@Nullable ScreenExtension parent, BlockPos signPos, int length, String[] signIds, LongAVLTreeSet selectedIds, @Nullable LongFunction<String> routeNumberGetter, @Nullable long[][] cellColors, @Nullable BiConsumer<String[], LongAVLTreeSet> saveAction) {
		super(TextHelper.translatable("gui.nanbin.crt_sign.title"));
		this.parent = parent;
		this.signPos = signPos;
		this.length = Math.max(1, length);
		this.signIds = new String[this.length];
		if (signIds != null) {
			System.arraycopy(signIds, 0, this.signIds, 0, Math.min(this.length, signIds.length));
		}
		this.selectedIds = selectedIds == null ? new LongAVLTreeSet() : new LongAVLTreeSet(selectedIds);
		this.routeNumberGetter = routeNumberGetter;
		this.cellColors = copyCellColors(cellColors, this.length);
		this.saveAction = saveAction == null ? (ids, selected) -> {
		} : saveAction;
		this.cacheKey = "screen:" + (signPos == null ? "unknown" : signPos.toShortString());

		// 可选标志只来自 crt_signs.json 里 whitelist 含 crt 的自定义标志（CRT 专用图标集）。
		// 原版 MTR 内置标志不再出现在这里：本界面与原版导视牌完全独立，不再互相"混入"。
		// 旧存档里已经用到的原版内置标志仍能正常渲染（渲染端走 MTR 的 drawSign）。
		allSignIds.addAll(CRTSignRegistry.getSignIdsFor(CRTSignRegistry.SURFACE_CRT));

		// 站点数据：与原版指示牌编辑界面一致，能取到所属车站的线路 / 车站 / 站台 / 出口
		final Station station = signPos == null ? null : ClientPacketHelper.findStationForSelection(signPos);
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
				if (!addedColors.contains(color) && simplifiedRoute.getPlatforms().stream().anyMatch(simplifiedRoutePlatform -> platformIds.contains(simplifiedRoutePlatform.getPlatformId()))) {
					routes.add(new DashboardListItem(color, simplifiedRoute.getName().split("\\|\\|")[0], color));
					addedColors.add(color);
				}
			});
			routesForList = new ObjectImmutableList<>(routes);
		}
		collectNames(exitNames, exitsForList);
		collectNames(platformNames, platformsForList);
		collectNames(routeNames, routesForList);
		collectNames(stationNames, stationsForList);

		rebuildFilteredSignIds();
		rebuildView();
		lastSavedSignature = signature();
	}

	// ================================================================ 生命周期

	@Override
	protected void init2() {
		super.init2();
		layout();
		refreshIfStale();
		rebuildButtons();
	}

	@Override
	public void tick2() {
		super.tick2();
		refreshIfStale();
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}

	@Override
	public void onClose2() {
		saveNow();
		if (parent != null) {
			MinecraftClient.getInstance().openScreen(new Screen(parent));
		} else {
			super.onClose2();
		}
	}

	// ================================================================ 布局

	private void layout() {
		final int width = getWidthMapped();
		final int height = getHeightMapped();
		if (width <= 0 || height <= 0) {
			return;
		}

		previewX = MARGIN;
		previewY = TOP_BAR_HEIGHT + 4;
		previewWidth = Math.max(40, width - MARGIN * 2);
		previewHeight = clamp(height / 4, 44, 96);

		bodyY = previewY + previewHeight + 6;
		final int toolbarTop = height - MARGIN - TOOLBAR_HEIGHT;
		bodyHeight = Math.max(30, toolbarTop - 6 - bodyY);

		paletteX = MARGIN;
		paletteY = bodyY;
		paletteWidth = clamp((width - MARGIN * 2) * 56 / 100, 96, width - MARGIN * 2);
		paletteHeight = bodyHeight;

		propsX = paletteX + paletteWidth + 6;
		propsY = bodyY;
		propsWidth = Math.max(50, width - MARGIN - propsX);
		propsHeight = bodyHeight;

		toolbarX = MARGIN;
		toolbarY = toolbarTop;
		toolbarWidth = width - MARGIN * 2;

		// 调色板内部：顶部文字页签 + 图标网格 + 底部翻页行
		paletteTabsY = paletteY + 4;
		paletteGridX = paletteX + 4;
		paletteGridY = paletteTabsY + PALETTE_TAB_HEIGHT + 4;
		paletteGridWidth = Math.max(CELL_STEP, paletteWidth - 8);
		final int availableGridHeight = Math.max(CELL_STEP, paletteY + paletteHeight - PAGE_ROW_HEIGHT - 4 - paletteGridY);
		paletteColumns = Math.max(1, paletteGridWidth / CELL_STEP);
		paletteRows = Math.max(1, availableGridHeight / CELL_STEP);
		paletteGridHeight = paletteRows * CELL_STEP;
		palettePageSize = Math.max(1, paletteColumns * paletteRows);

		updatePageCount();
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	// ================================================================ 数据同步

	/** 当前数据指纹；用于发现子屏幕（选择器 / 文本 / JS 数据）造成的变动。 */
	private long signature() {
		long hash = 17L;
		for (final String signId : signIds) {
			hash = hash * 31L + (signId == null ? 0 : signId.hashCode());
		}
		for (final long id : selectedIds) {
			hash = hash * 31L + (int) (id ^ (id >>> 32));
		}
		return hash;
	}

	/** 子屏幕可能直接改写了 selectedIds，回到本屏幕时补一次重建与保存。 */
	private void refreshIfStale() {
		final long currentSignature = signature();
		if (currentSignature != lastSavedSignature) {
			rebuildView();
			lastSavedSignature = currentSignature;
			rebuildButtons();
			saveNow();
		}
	}

	/** 内部数据已修改：重建视图 -> 保存 -> 重建按钮。 */
	private void markChanged() {
		rebuildView();
		saveNow();
		lastSavedSignature = signature();
		rebuildButtons();
	}

	private void saveNow() {
		saveAction.accept(signIds, selectedIds);
	}

	/** 按当前内部数据重建预览视图（线路编号为 routeNumberGetter 的即时快照）。 */
	private void rebuildView() {
		final List<String> numbers = new ArrayList<>();
		if (routeNumberGetter != null) {
			for (final long color : selectedIds) {
				final String number = routeNumberGetter.apply(color);
				if (number != null && !number.isEmpty()) {
					numbers.add(number);
				}
			}
		}
		routeNumbers = numbers.toArray(new String[0]);
		final String jsStyleScriptId = JSSignConfig.getStyleScriptId(signIds);
		final CRTSignView newView = new CRTSignView(signIds, selectedIds, routeNumbers, routeNumberGetter, jsStyleScriptId);
		if (cellColors != null) {
			newView.cellOverrides(cellColors, null);
		}
		view = newView;
	}

	private boolean jsLocked() {
		return JSSignConfig.hasJSStyle(signIds);
	}

	@Nullable
	private String currentScriptId() {
		return JSSignConfig.getStyleScriptId(signIds);
	}

	@Nullable
	private String currentStyleMarker() {
		return JSSignConfig.hasJSStyle(signIds) ? signIds[0] : null;
	}

	/** 剪贴板操作后保护当前 JS 样式标记，避免粘贴／撤销把样式意外覆盖或关闭。 */
	private void restoreStyleMarker(@Nullable String styleMarker) {
		if (signIds.length == 0) {
			return;
		}
		if (styleMarker != null) {
			signIds[0] = styleMarker;
		} else if (JSSignConfig.isJSStyle(signIds[0])) {
			signIds[0] = null;
		}
	}

	// ================================================================ 调色板

	private void rebuildFilteredSignIds() {
		final List<String> filtered = new ArrayList<>();
		for (final String signId : allSignIds) {
			if (category == CATEGORY_ALL || categoryOf(signId) == category) {
				filtered.add(signId);
			}
		}
		filteredSignIds = filtered.toArray(new String[0]);
		updatePageCount();
	}

	private void updatePageCount() {
		final int pageSize = Math.max(1, palettePageSize);
		pageCount = Math.max(1, (filteredSignIds.length + pageSize - 1) / pageSize);
		palettePage = clamp(palettePage, 0, pageCount - 1);
	}

	private void selectCategory(int newCategory) {
		if (category == newCategory) {
			return;
		}
		category = newCategory;
		palettePage = 0;
		rebuildFilteredSignIds();
		layout();
		rebuildButtons();
	}

	private void changePage(int delta) {
		final int newPage = clamp(palettePage + delta, 0, pageCount - 1);
		if (newPage != palettePage) {
			palettePage = newPage;
			rebuildButtons();
		}
	}

	private static int categoryOf(@Nullable String signId) {
		final String id = normalizedSignId(signId);
		if (id.isEmpty()) {
			return CATEGORY_OTHER;
		}
		if (id.contains("arrow") || id.contains("turn_back")) {
			return CATEGORY_ARROW;
		}
		if (id.contains("exit")) {
			return CATEGORY_EXIT;
		}
		if (id.contains("route") || id.startsWith("crt_station_name")
				|| id.equals("station") || id.equals("station_flipped")
				|| id.equals("line") || id.equals("line_flipped")
				|| id.equals("platform") || id.equals("platform_flipped")) {
			return CATEGORY_ROUTE;
		}
		if (id.contains("custom_text") || id.contains("text") || id.contains("letter") || id.contains("bilingual")) {
			return CATEGORY_TEXT;
		}
		if (isFacilitySign(id)) {
			return CATEGORY_FACILITY;
		}
		return CATEGORY_OTHER;
	}

	private static boolean isFacilitySign(String id) {
		final String[] keywords = {
				"escalator", "stair", "lift", "elevator", "wheelchair", "toilet", "female", "male",
				"train", "rail", "boat", "airplane", "bus", "police", "help", "cross", "logo",
				"charging", "park", "health", "fire", "emergency", "accessible", "waiting", "restroom",
				"hotel", "info", "ticket", "check", "passage", "atm", "bank", "shop", "restaurant",
				"luggage", "bag"
		};
		for (final String keyword : keywords) {
			if (id.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private static String normalizedSignId(@Nullable String signId) {
		if (signId == null || signId.isEmpty()) {
			return "";
		}
		final String withoutPrefix = signId.startsWith("!") ? signId.substring(1) : signId;
		return withoutPrefix.toLowerCase(Locale.ROOT);
	}

	// ================================================================ 格子 / 数据操作

	private void applyPaletteSign(String signId) {
		if (jsLocked()) {
			return;
		}
		if (editingIndex < 0 || editingIndex >= length) {
			setStatus(tr("gui.nanbin.crt_sign.select_cell_first"));
			return;
		}
		signIds[editingIndex] = signId;
		markChanged();
		// 与 MTR 原版一致：放上需要站点数据的标志后立刻打开对应选择器
		if (isCustomTextSign(signId)) {
			openCustomTextScreen();
		} else if (isRouteSign(signId)) {
			openDataSelector(routesForList);
		} else if (isStationSign(signId)) {
			openDataSelector(stationsForList);
		} else if (isPlatformSign(signId)) {
			openDataSelector(platformsForList);
		} else if (isExitSign(signId)) {
			openDataSelector(exitsForList);
		}
	}

	private void clearSelectedCell() {
		if (editingIndex < 0 || editingIndex >= length) {
			return;
		}
		signIds[editingIndex] = null;
		markChanged();
		setStatus(tr("gui.nanbin.crt_sign.clear_cell_done"));
	}

	private void openDataSelector(ObjectImmutableList<DashboardListItem> list) {
		if (list == null || list.isEmpty()) {
			setStatus(tr("gui.nanbin.crt_sign.no_data_available"));
			return;
		}
		MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(list, selectedIds, false, false, this)));
	}

	private void openCustomTextScreen() {
		MinecraftClient.getInstance().openScreen(new Screen(new CustomTextScreen(this, selectedIds, this::markChanged)));
	}

	private void openJsDataScreen() {
		final String scriptId = currentScriptId();
		if (scriptId == null) {
			return;
		}
		MinecraftClient.getInstance().openScreen(new Screen(new JSSignDataScreen(this, signPos, selectedIds, this::markChanged, scriptId)));
	}

	/** 选择 JS 样式；切换后清空格子内容与选中数据（与原版扩展行为一致）。 */
	private void openJsStyleSelector() {
		MinecraftClient.getInstance().openScreen(new Screen(new JSSignSelectorScreen(this, currentScriptId(), scriptId -> {
			signIds[0] = scriptId == null ? null : JSSignConfig.JS_STYLE_PREFIX + scriptId;
			for (int i = 1; i < length; i++) {
				signIds[i] = null;
			}
			selectedIds.clear();
			editingIndex = -1;
			clearArmed = false;
			// 样式切换属于新起点，旧撤销记录不再适用
			CRTSignClipboard.clearHistory(signPos);
			markChanged();
		})));
	}

	// ================================================================ 剪贴板

	private void copySign() {
		CRTSignClipboard.copy(length, signIds, selectedIds);
		setStatus(tr("gui.nanbin.crt_sign.copied"));
		rebuildButtons();
	}

	private void pasteSign() {
		if (!CRTSignClipboard.canPaste(length)) {
			setStatus(tr("gui.nanbin.crt_sign.nothing_to_paste"));
			return;
		}
		final String styleMarker = currentStyleMarker();
		if (!CRTSignClipboard.paste(signPos, length, signIds, selectedIds)) {
			setStatus(tr("gui.nanbin.crt_sign.nothing_to_paste"));
			return;
		}
		restoreStyleMarker(styleMarker);
		markChanged();
		setStatus(tr("gui.nanbin.crt_sign.pasted"));
	}

	private void clearSign() {
		if (!clearArmed) {
			clearArmed = true;
			rebuildButtons();
			setStatus(tr("gui.nanbin.crt_sign.confirm_clear"));
			return;
		}
		clearArmed = false;
		final String styleMarker = currentStyleMarker();
		CRTSignClipboard.clear(signPos, length, signIds, selectedIds);
		restoreStyleMarker(styleMarker);
		markChanged();
		setStatus(tr("gui.nanbin.crt_sign.cleared"));
	}

	private void undoSigned() {
		final String styleMarker = currentStyleMarker();
		if (CRTSignClipboard.undo(signPos, length, signIds, selectedIds)) {
			restoreStyleMarker(styleMarker);
			markChanged();
			setStatus(tr("gui.nanbin.crt_sign.undone"));
		}
	}

	private void disarmClear() {
		if (clearArmed) {
			clearArmed = false;
			rebuildButtons();
		}
	}

	private void setStatus(String message) {
		statusMessage = message;
		statusUntilMillis = System.currentTimeMillis() + 2500L;
	}

	// ================================================================ 按钮构建

	private void rebuildButtons() {
		buttons.clear();
		paletteButtons.clear();
		clearButton = null;
		propertyButtonCount = 0;
		final int width = getWidthMapped();
		final int height = getHeightMapped();
		if (width <= 0 || height <= 0) {
			return;
		}

		// 顶栏：返回按钮
		buttons.add(new DarkButton(width - MARGIN - 34, 4, 34, 12, tr("gui.nanbin.crt_sign.close"), this::onClose2));

		final boolean locked = jsLocked();
		if (locked) {
			// JS 锁定：只保留预览、JS 样式按钮、数据选择与底部工具栏
			final int buttonWidth = Math.max(80, Math.min(200, width - MARGIN * 2 - 16));
			final int buttonX = (width - buttonWidth) / 2;
			final int buttonY = bodyY + Math.max(28, bodyHeight / 2 + 4);
			buttons.add(new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, tr("gui.nanbin.crt_sign.js_data"), this::openJsDataScreen).setAccent(true));
			buildToolbarButtons(true);
			return;
		}

		// 分类页签（在面板内居中）
		final int tabWidth = Math.max(8, Math.min(32, paletteGridWidth / CATEGORY_COUNT));
		final int tabsStartX = paletteX + Math.max(3, (paletteWidth - tabWidth * CATEGORY_COUNT) / 2);
		for (int i = 0; i < CATEGORY_COUNT; i++) {
			final int categoryIndex = i;
			final DarkButton tab = new DarkButton(tabsStartX + i * tabWidth, paletteTabsY, tabWidth - 1, PALETTE_TAB_HEIGHT, tr(CATEGORY_LANG_KEYS[i]), () -> selectCategory(categoryIndex));
			tab.setSelected(category == i).setCompact(true);
			buttons.add(tab);
		}

		// 调色板图标（页内）
		final int startIndex = palettePage * palettePageSize;
		final int endIndex = Math.min(filteredSignIds.length, startIndex + palettePageSize);
		for (int i = startIndex; i < endIndex; i++) {
			final int slot = i - startIndex;
			final int iconX = paletteGridX + (slot % paletteColumns) * CELL_STEP;
			final int iconY = paletteGridY + (slot / paletteColumns) * CELL_STEP;
			final String signId = filteredSignIds[i];
			final DarkButton iconButton = new DarkButton(iconX + 1, iconY + 1, ICON_SIZE, ICON_SIZE, null, () -> applyPaletteSign(signId));
			iconButton.setSignId(signId);
			iconButton.tooltip = displayNameForSign(signId);
			if (signId.equals(currentCellSignId())) {
				iconButton.setSelected(true);
			}
			paletteButtons.add(iconButton);
		}

		// 翻页
		final int pageButtonY = paletteY + paletteHeight - PAGE_ROW_HEIGHT - 2;
		buttons.add(new DarkButton(paletteX + 3, pageButtonY, 18, PAGE_ROW_HEIGHT, "<", () -> changePage(-1)).setEnabled(palettePage > 0));
		buttons.add(new DarkButton(paletteX + paletteWidth - 21, pageButtonY, 18, PAGE_ROW_HEIGHT, ">", () -> changePage(1)).setEnabled(palettePage < pageCount - 1));

		buildPropertyButtons();
		buildToolbarButtons(false);
	}

	private void buildPropertyButtons() {
		propertyButtonCount = 0;
		if (editingIndex < 0 || editingIndex >= length) {
			return;
		}
		final String signId = signIds[editingIndex];
		if (signId == null || signId.isEmpty() || jsLocked()) {
			return;
		}
		final int buttonX = propsX + 6;
		final int buttonWidth = Math.max(20, propsWidth - 12);
		int buttonY = propsY + 38;
		final Runnable dataAction = dataActionFor(signId);
		if (dataAction != null) {
			buttons.add(new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, dataActionLabel(signId), dataAction).setAccent(true));
			buttonY += BUTTON_HEIGHT + 4;
			propertyButtonCount++;
		}
		buttons.add(new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, tr("gui.nanbin.crt_sign.clear_cell"), this::clearSelectedCell));
		propertyButtonCount++;
	}

	private void buildToolbarButtons(boolean locked) {
		final int available = toolbarWidth - 8;
		final int gap = 3;
		final int jsMinWidth = 56;
		final int buttonWidth = Math.max(22, Math.min(34, (available - gap * 3 - jsMinWidth - 6) / 4));
		final int buttonY = toolbarY + (TOOLBAR_HEIGHT - BUTTON_HEIGHT) / 2;
		int buttonX = toolbarX + 4;

		buttons.add(new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, tr("gui.nanbin.copy"), this::copySign));
		buttonX += buttonWidth + gap;
		buttons.add(new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, tr("gui.nanbin.paste"), this::pasteSign).setEnabled(CRTSignClipboard.canPaste(length)));
		buttonX += buttonWidth + gap;
		final DarkButton newClearButton = new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, tr("gui.nanbin.clear"), this::clearSign).setWarning(clearArmed);
		clearButton = newClearButton;
		buttons.add(newClearButton);
		buttonX += buttonWidth + gap;
		buttons.add(new DarkButton(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, tr("gui.nanbin.undo"), this::undoSigned).setEnabled(CRTSignClipboard.canUndo(signPos)));
		buttonX += buttonWidth + gap + 4;

		final int remaining = toolbarX + toolbarWidth - 4 - buttonX;
		final int jsWidth = clamp(remaining, 40, 200);
		buttons.add(new DarkButton(buttonX, buttonY, jsWidth, BUTTON_HEIGHT, jsStyleLabel(), this::openJsStyleSelector).setAccent(true));
	}

	private String jsStyleLabel() {
		final String scriptId = currentScriptId();
		return scriptId == null ? tr("gui.nanbin.js_style.select") : JSSignSelectorScreen.getScriptName(scriptId).getString();
	}

	@Nullable
	private String currentCellSignId() {
		return editingIndex >= 0 && editingIndex < length ? signIds[editingIndex] : null;
	}

	// ================================================================ 标志分类辅助

	private static boolean isCustomTextSign(@Nullable String signId) {
		return signId != null && signId.startsWith("nanbin_custom_text");
	}

	private static boolean isRouteSign(@Nullable String signId) {
		return "crt_route_name".equals(signId) || "crt_route_name_flipped".equals(signId)
				|| "crt_route_number".equals(signId) || "crt_route_number_flipped".equals(signId)
				|| "line".equals(signId) || "line_flipped".equals(signId);
	}

	private static boolean isStationSign(@Nullable String signId) {
		return signId != null && signId.startsWith("crt_station_name")
				|| "station".equals(signId) || "station_flipped".equals(signId);
	}

	private static boolean isPlatformSign(@Nullable String signId) {
		return "platform".equals(signId) || "platform_flipped".equals(signId);
	}

	private static boolean isExitSign(@Nullable String signId) {
		return "exit_letter".equals(signId) || "exit_letter_flipped".equals(signId);
	}

	@Nullable
	private Runnable dataActionFor(String signId) {
		if (isCustomTextSign(signId)) {
			return this::openCustomTextScreen;
		}
		if (isRouteSign(signId)) {
			return () -> openDataSelector(routesForList);
		}
		if (isStationSign(signId)) {
			return () -> openDataSelector(stationsForList);
		}
		if (isPlatformSign(signId)) {
			return () -> openDataSelector(platformsForList);
		}
		if (isExitSign(signId)) {
			return () -> openDataSelector(exitsForList);
		}
		return null;
	}

	private String dataActionLabel(String signId) {
		if (isCustomTextSign(signId)) {
			return tr("gui.nanbin.crt_sign.edit_text");
		}
		if (isRouteSign(signId)) {
			return tr("gui.nanbin.crt_sign.select_route");
		}
		if (isStationSign(signId)) {
			return tr("gui.nanbin.crt_sign.select_station");
		}
		if (isPlatformSign(signId)) {
			return tr("gui.nanbin.crt_sign.select_platform");
		}
		if (isExitSign(signId)) {
			return tr("gui.nanbin.crt_sign.select_exit");
		}
		return tr("gui.nanbin.crt_sign.edit_text");
	}

	// ================================================================ 渲染

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		final int width = getWidthMapped();
		final int height = getHeightMapped();
		tooltip = null;

		// 全屏暗色背景与顶栏
		drawRect(graphicsHolder, 0, 0, width, height, COLOR_BACKGROUND);
		drawRect(graphicsHolder, 0, 0, width, TOP_BAR_HEIGHT, COLOR_TOP_BAR);
		drawRect(graphicsHolder, 0, TOP_BAR_HEIGHT, width, TOP_BAR_HEIGHT + 1, COLOR_ACCENT_DIM);
		drawTopBarContent(graphicsHolder);

		// 顶部预览
		drawFrame(graphicsHolder, previewX, previewY, previewWidth, previewHeight);
		if (view != null) {
			CRTSignPreview.draw(graphicsHolder, previewX, previewY, previewWidth, previewHeight, view, cacheKey, jsLocked() ? -1 : editingIndex);
		}
		if (!jsLocked() && editingIndex < 0 && view != null && view.isEmpty()) {
			drawCenteredText(graphicsHolder, tr("gui.nanbin.crt_sign.empty_hint"), previewX + previewWidth / 2, previewY + previewHeight - 11, COLOR_TEXT_DIM);
		}

		final boolean locked = jsLocked();
		final TextRenderer textRenderer = getTextRendererMapped();
		if (locked) {
			drawFrame(graphicsHolder, MARGIN, bodyY, width - MARGIN * 2, bodyHeight);
			drawJsPanelContent(graphicsHolder);
		} else {
			drawFrame(graphicsHolder, paletteX, paletteY, paletteWidth, paletteHeight);
			drawFrame(graphicsHolder, propsX, propsY, propsWidth, propsHeight);
			drawPaletteContent(graphicsHolder);
			drawPropsContent(graphicsHolder);

			// 调色板图标按网格裁剪，滚动/翻页时不会溢出面板
			enableScissor(paletteGridX, paletteGridY, paletteGridX + paletteGridWidth, paletteGridY + paletteGridHeight);
			for (final DarkButton paletteButton : paletteButtons) {
				paletteButton.render(graphicsHolder, textRenderer, mouseX, mouseY, delta);
				if (paletteButton.tooltip != null && paletteButton.isMouseOver(mouseX, mouseY)) {
					tooltip = paletteButton.tooltip;
				}
			}
			disableScissor();
		}

		// 底部工具栏
		drawFrame(graphicsHolder, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT);

		for (final DarkButton darkButton : buttons) {
			darkButton.render(graphicsHolder, textRenderer, mouseX, mouseY, delta);
			if (darkButton.tooltip != null && darkButton.enabled && darkButton.isMouseOver(mouseX, mouseY)) {
				tooltip = darkButton.tooltip;
			}
		}

		drawStatusMessage(graphicsHolder);
		drawTooltip(graphicsHolder, mouseX, mouseY);
	}

	private void drawTopBarContent(GraphicsHolder graphicsHolder) {
		final int width = getWidthMapped();
		drawText(graphicsHolder, tr("gui.nanbin.crt_sign.title"), MARGIN + 2, 6, COLOR_TEXT);

		final String countText = TextHelper.translatable("gui.nanbin.crt_sign.cell_count", String.valueOf(length)).getString();
		final int textRendererWidth = getTextRendererMapped().getWidth(countText);
		int rightEdge = width - MARGIN - 34 - 8;
		if (jsLocked()) {
			rightEdge -= 16;
			drawText(graphicsHolder, "JS", rightEdge + 4, 6, COLOR_ACCENT);
		}
		drawText(graphicsHolder, countText, rightEdge - textRendererWidth, 6, COLOR_TEXT_DIM);
	}

	private void drawPaletteContent(GraphicsHolder graphicsHolder) {
		if (filteredSignIds.length == 0) {
			drawCenteredText(graphicsHolder, tr("gui.nanbin.crt_sign.palette_empty"), paletteX + paletteWidth / 2, paletteGridY + paletteGridHeight / 2 - 4, COLOR_TEXT_DIM);
		}
		final String pageText = (palettePage + 1) + " / " + pageCount;
		drawCenteredText(graphicsHolder, pageText, paletteX + paletteWidth / 2, paletteY + paletteHeight - PAGE_ROW_HEIGHT, COLOR_TEXT_DIM);
	}

	private void drawPropsContent(GraphicsHolder graphicsHolder) {
		final TextRenderer textRenderer = getTextRendererMapped();
		drawText(graphicsHolder, tr("gui.nanbin.crt_sign.cell"), propsX + 6, propsY + 4, COLOR_ACCENT);
		if (editingIndex < 0 || editingIndex >= length) {
			drawText(graphicsHolder, tr("gui.nanbin.crt_sign.cell_hint"), propsX + 6, propsY + 20, COLOR_TEXT_DIM);
			return;
		}

		final String signId = signIds[editingIndex];
		final String indexText = TextHelper.translatable("gui.nanbin.crt_sign.cell_index", String.valueOf(editingIndex + 1)).getString();
		drawText(graphicsHolder, indexText, propsX + 6, propsY + 14, COLOR_TEXT_DIM);

		final boolean hasSign = signId != null && !signId.isEmpty();
		drawText(graphicsHolder, trimToWidth(textRenderer, hasSign ? displayNameForSign(signId) : tr("gui.nanbin.crt_sign.empty"), propsWidth - 12), propsX + 6, propsY + 24, hasSign ? COLOR_TEXT : COLOR_TEXT_DIM);
		if (!hasSign) {
			drawText(graphicsHolder, tr("gui.nanbin.crt_sign.empty_cell_hint"), propsX + 6, propsY + 38, COLOR_TEXT_DIM);
			return;
		}

		final int contentY = propsY + 38 + propertyButtonCount * (BUTTON_HEIGHT + 4) + 2;
		final String summaryLine = dataSummaryLine(signId);
		if (summaryLine == null) {
			drawText(graphicsHolder, tr("gui.nanbin.crt_sign.no_data_needed"), propsX + 6, contentY, COLOR_TEXT_DIM);
		} else {
			drawText(graphicsHolder, trimToWidth(textRenderer, summaryLine, propsWidth - 12), propsX + 6, contentY, COLOR_TEXT_DIM);
		}
	}

	private void drawJsPanelContent(GraphicsHolder graphicsHolder) {
		final int width = getWidthMapped();
		final int centerX = width / 2;
		drawCenteredText(graphicsHolder, tr("gui.nanbin.crt_sign.js_locked_title"), centerX, bodyY + 8, COLOR_ACCENT);
		final String scriptId = currentScriptId();
		final String scriptName = scriptId == null ? "" : JSSignSelectorScreen.getScriptName(scriptId).getString();
		drawCenteredText(graphicsHolder, scriptName, centerX, bodyY + 22, COLOR_TEXT);
		drawCenteredText(graphicsHolder, tr("gui.nanbin.crt_sign.js_locked_hint"), centerX, bodyY + 34, COLOR_TEXT_DIM);
	}

	private void drawStatusMessage(GraphicsHolder graphicsHolder) {
		if (statusMessage == null || System.currentTimeMillis() > statusUntilMillis) {
			return;
		}
		final TextRenderer textRenderer = getTextRendererMapped();
		final String text = trimToWidth(textRenderer, statusMessage, getWidthMapped() - MARGIN * 2 - 8);
		final int textWidth = textRenderer.getWidth(text);
		final int x = getWidthMapped() - MARGIN - textWidth;
		final int y = toolbarY - 13;
		if (x < MARGIN) {
			return;
		}
		drawRect(graphicsHolder, x - 4, y - 2, x + textWidth + 4, y + 10, 0xCC14161A);
		drawBorder(graphicsHolder, x - 4, y - 2, x + textWidth + 4, y + 10, COLOR_BORDER);
		drawText(graphicsHolder, text, x, y, COLOR_ACCENT);
	}

	private void drawTooltip(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
		if (tooltip == null || tooltip.isEmpty()) {
			return;
		}
		final TextRenderer textRenderer = getTextRendererMapped();
		final String text = trimToWidth(textRenderer, tooltip, 200);
		final int width = textRenderer.getWidth(text) + 8;
		final int height = 12;
		final int x = clamp(mouseX + 6, 0, Math.max(0, getWidthMapped() - width));
		final int y = clamp(mouseY - height - 4, 0, Math.max(0, getHeightMapped() - height));
		drawRect(graphicsHolder, x, y, x + width, y + height, 0xF014161A);
		drawBorder(graphicsHolder, x, y, x + width, y + height, COLOR_BORDER_LIGHT);
		drawText(graphicsHolder, text, x + 4, y + 2, COLOR_TEXT);
	}

	// ================================================================ 绘制辅助

	private void drawRect(GraphicsHolder graphicsHolder, int x1, int y1, int x2, int y2, int color) {
		if (x2 <= x1 || y2 <= y1) {
			return;
		}
		final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
		guiDrawing.beginDrawingRectangle();
		guiDrawing.drawRectangle(x1, y1, x2, y2, color);
		guiDrawing.finishDrawingRectangle();
	}

	private void drawBorder(GraphicsHolder graphicsHolder, int x1, int y1, int x2, int y2, int color) {
		drawRect(graphicsHolder, x1, y1, x2, y1 + 1, color);
		drawRect(graphicsHolder, x1, y2 - 1, x2, y2, color);
		drawRect(graphicsHolder, x1, y1, x1 + 1, y2, color);
		drawRect(graphicsHolder, x2 - 1, y1, x2, y2, color);
	}

	private void drawFrame(GraphicsHolder graphicsHolder, int x, int y, int frameWidth, int frameHeight) {
		drawRect(graphicsHolder, x, y, x + frameWidth, y + frameHeight, COLOR_PANEL);
		drawBorder(graphicsHolder, x, y, x + frameWidth, y + frameHeight, COLOR_BORDER);
	}

	private void drawText(GraphicsHolder graphicsHolder, String text, int x, int y, int color) {
		graphicsHolder.drawText(text, x, y, color, true, GraphicsHolder.getDefaultLight());
	}

	private void drawCenteredText(GraphicsHolder graphicsHolder, String text, int centerX, int y, int color) {
		final int textWidth = getTextRendererMapped().getWidth(text);
		drawText(graphicsHolder, text, centerX - textWidth / 2, y, color);
	}

	private static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
		if (text == null) {
			return "";
		}
		return maxWidth <= 0 ? "" : textRenderer.trimToWidth(text, maxWidth);
	}

	private void enableScissor(int x1, int y1, int x2, int y2) {
		final MinecraftClient client = getClientMapped();
		final double scale = client == null || client.data == null ? 1.0 : client.getWindow().getScaleFactor();
		final int x = (int) (x1 * scale);
		final int y = (int) ((getHeightMapped() - y2) * scale);
		final int width = (int) ((x2 - x1) * scale);
		final int height = (int) ((y2 - y1) * scale);
		if (width > 0 && height > 0) {
			RenderSystem.enableScissor(x, y, width, height);
		}
	}

	private void disableScissor() {
		RenderSystem.disableScissor();
	}

	// ================================================================ 输入

	@Override
	public boolean mouseClicked2(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return super.mouseClicked2(mouseX, mouseY, button);
		}

		// 1) 调色板图标（只在网格裁剪区域内响应）
		if (!jsLocked() && isInside(mouseX, mouseY, paletteGridX, paletteGridY, paletteGridWidth, paletteGridHeight)) {
			for (final DarkButton paletteButton : paletteButtons) {
				if (paletteButton.enabled && paletteButton.isMouseOver(mouseX, mouseY)) {
					disarmClear();
					paletteButton.action.run();
					return true;
				}
			}
		}

		// 2) 普通按钮
		for (final DarkButton darkButton : new ArrayList<>(buttons)) {
			if (!darkButton.isMouseOver(mouseX, mouseY)) {
				continue;
			}
			if (!darkButton.enabled) {
				return true;
			}
			if (darkButton != clearButton) {
				disarmClear();
			}
			darkButton.action.run();
			return true;
		}

		// 3) 点击预览选格
		if (!jsLocked() && view != null && isInside(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight)) {
			disarmClear();
			final int cell = CRTSignPreview.cellAt(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight, view);
			if (cell >= 0 && cell != editingIndex) {
				editingIndex = cell;
				rebuildButtons();
			}
			return true;
		}

		return super.mouseClicked2(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
		if (!jsLocked() && isInside(mouseX, mouseY, paletteX, paletteY, paletteWidth, paletteHeight)) {
			changePage(amount > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled2(mouseX, mouseY, amount);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	// ================================================================ 条目辅助

	private static void collectNames(Map<Long, String> target, List<DashboardListItem> items) {
		for (final DashboardListItem item : items) {
			final String name = item.getName(false);
			target.put(item.id, name == null ? "" : name);
		}
	}

	private String joinSelectedNames(Map<Long, String> names) {
		final List<String> selectedNames = new ArrayList<>();
		for (final long id : selectedIds) {
			final String name = names.get(id);
			if (name != null && !name.isEmpty()) {
				selectedNames.add(name);
			}
		}
		if (selectedNames.isEmpty()) {
			return null;
		}
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < selectedNames.size() && i < 3; i++) {
			if (i > 0) {
				builder.append(" / ");
			}
			builder.append(selectedNames.get(i));
		}
		if (selectedNames.size() > 3) {
			builder.append(" ...");
		}
		return builder.toString();
	}

	@Nullable
	private String dataSummaryLine(String signId) {
		if (isCustomTextSign(signId)) {
			final String text = JSSignEngine.readCustomText(selectedIds);
			final String display = text == null || text.isEmpty() ? "-" : text.replace('|', ' ');
			return TextHelper.translatable("gui.nanbin.crt_sign.text_data", display).getString();
		}
		final Map<Long, String> names;
		final String langKey;
		if (isRouteSign(signId)) {
			names = routeNames;
			langKey = "gui.nanbin.crt_sign.route_data";
		} else if (isStationSign(signId)) {
			names = stationNames;
			langKey = "gui.nanbin.crt_sign.station_data";
		} else if (isPlatformSign(signId)) {
			names = platformNames;
			langKey = "gui.nanbin.crt_sign.platform_data";
		} else if (isExitSign(signId)) {
			names = exitNames;
			langKey = "gui.nanbin.crt_sign.exit_data";
		} else {
			return null;
		}
		final String joined = joinSelectedNames(names);
		return TextHelper.translatable(langKey, joined == null ? "-" : joined).getString();
	}

	private static String displayNameForSign(@Nullable String signId) {
		if (signId == null || signId.isEmpty()) {
			return "";
		}
		return signId.startsWith("!") ? signId.substring(1) : signId;
	}

	@Nullable
	private static SignResource getSignResource(@Nullable String signId) {
		if (signId == null || signId.isEmpty()) {
			return null;
		}
		final SignResource sign = RenderRailwaySign.getSign(signId);
		return sign != null ? sign : CRTSignRegistry.getSign(signId);
	}

	private static String tr(String key) {
		return TextHelper.translatable(key).getString();
	}

	private static long[][] copyCellColors(@Nullable long[][] source, int length) {
		if (source == null) {
			return null;
		}
		final long[][] result = new long[length][];
		for (int i = 0; i < Math.min(length, source.length); i++) {
			result[i] = source[i] == null ? null : source[i].clone();
		}
		return result;
	}

	private static float approach(float current, float target, float step) {
		if (current < target) {
			return Math.min(target, current + step);
		}
		if (current > target) {
			return Math.max(target, current - step);
		}
		return target;
	}

	private static int lerpColor(int from, int to, float amount) {
		final float t = Math.max(0.0F, Math.min(1.0F, amount));
		final int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		final int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		final int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		final int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ================================================================ 自绘暗色按钮

	/** 屏幕自绘按钮：不依赖原版浅色按钮，支持图标（调色板）与纯文字两种形态。 */
	private final class DarkButton {

		private final int x;
		private final int y;
		private final int width;
		private final int height;
		@Nullable
		private final String label;
		@Nullable
		private final Runnable action;
		@Nullable
		private String signId;
		@Nullable
		private String tooltip;
		private boolean enabled = true;
		private boolean accent;
		private boolean selected;
		private boolean warning;
		private boolean compact;
		private float hover;

		private DarkButton(int x, int y, int width, int height, @Nullable String label, @Nullable Runnable action) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.label = label;
			this.action = action;
		}

		private DarkButton setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		private DarkButton setAccent(boolean accent) {
			this.accent = accent;
			return this;
		}

		private DarkButton setSelected(boolean selected) {
			this.selected = selected;
			return this;
		}

		private DarkButton setWarning(boolean warning) {
			this.warning = warning;
			return this;
		}

		private DarkButton setCompact(boolean compact) {
			this.compact = compact;
			return this;
		}

		private DarkButton setSignId(String signId) {
			this.signId = signId;
			return this;
		}

		private boolean isMouseOver(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}

		private void render(GraphicsHolder graphicsHolder, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
			final float target = enabled && isMouseOver(mouseX, mouseY) ? 1.0F : 0.0F;
			hover = approach(hover, target, delta / 4.0F);

			final int baseColor = !enabled ? COLOR_PANEL_DISABLED : (warning ? COLOR_WARNING_DIM : (accent ? COLOR_ACCENT_DIM : COLOR_PANEL));
			final int hoverColor = !enabled ? COLOR_PANEL_DISABLED : (warning ? COLOR_WARNING_HOVER : (accent ? COLOR_ACCENT_HOVER : COLOR_PANEL_HOVER));
			drawRect(graphicsHolder, x, y, x + width, y + height, lerpColor(baseColor, hoverColor, hover));
			final int borderColor = selected || (enabled && hover > 0.6F) ? (warning ? COLOR_WARNING : (accent || selected ? COLOR_ACCENT : COLOR_BORDER_LIGHT)) : COLOR_BORDER;
			drawBorder(graphicsHolder, x, y, x + width, y + height, borderColor);

			if (signId != null) {
				renderIcon(graphicsHolder);
			} else if (label != null) {
				renderLabel(graphicsHolder, textRenderer);
			}
		}

		private void renderIcon(GraphicsHolder graphicsHolder) {
			final SignResource sign = getSignResource(signId);
			final int iconX = x + (width - ICON_SIZE) / 2;
			final int iconY = y + (height - ICON_SIZE) / 2;
			if (sign == null) {
				drawCenteredText(graphicsHolder, "?", iconX + ICON_SIZE / 2, iconY + (ICON_SIZE - 8) / 2, COLOR_TEXT_DIM);
				return;
			}
			// 必须先重置着色器颜色，否则图标会被上一步的绘制染色（与原版 MTR 界面一致）
			//? if <1.17 {
			/*RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			*///?} else {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			//?}
			final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
			final Identifier texture = sign.getTexture();
			guiDrawing.beginDrawingTexture(texture);
			if (sign.getFlipTexture()) {
				guiDrawing.drawTexture(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 1.0F, 0.0F, 0.0F, 1.0F);
			} else {
				guiDrawing.drawTexture(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0.0F, 0.0F, 1.0F, 1.0F);
			}
			guiDrawing.finishDrawingTexture();
			// 带自定义文字 / 站点数据的标志右下角加一条强调色短横，避免空白贴图在调色板里难以辨认
			if (sign.hasCustomText) {
				drawRect(graphicsHolder, iconX + 2, iconY + ICON_SIZE - 3, iconX + ICON_SIZE - 2, iconY + ICON_SIZE - 1, COLOR_ACCENT);
			}
		}

		private void renderLabel(GraphicsHolder graphicsHolder, TextRenderer textRenderer) {
			final String text = trimToWidth(textRenderer, label, Math.max(0, width - (compact ? 1 : 6)));
			final int textWidth = textRenderer.getWidth(text);
			final int textX = x + (width - textWidth) / 2;
			final int textY = y + (height - 8) / 2;
			final int textColor = !enabled ? COLOR_TEXT_DISABLED : (warning ? COLOR_WARNING : (accent ? COLOR_ACCENT : COLOR_TEXT));
			drawText(graphicsHolder, text, textX, textY, textColor);
		}
	}
}
