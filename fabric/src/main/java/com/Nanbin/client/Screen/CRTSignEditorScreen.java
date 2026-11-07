package com.Nanbin.client.Screen;

import com.Nanbin.client.CRTRailwaySignEditor.editor.*;
import com.Nanbin.client.ClientData.NanbinClientConfig;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutFrame;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutPainter;
import com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignRegistry;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTJsLayoutBake;
import com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutPreview;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import com.Nanbin.signage.crt.CRTSignClipboardData;
import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignElement;
import com.Nanbin.signage.crt.CRTSignElementType;
import com.Nanbin.signage.crt.CRTSignHistory;
import com.Nanbin.signage.crt.CRTSignLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * CRT 导视牌可视化编辑器（浅色现代风，三栏布局）。
 *
 * <h2>来源与许可</h2>
 * <p>界面组织、三栏信息架构、十种元素与交互规则复刻自开源项目
 * <b>CRT-Sign-Web-Editor</b>（Apache License 2.0）：原作者 endcreeper861 的
 * JR-Guidance-Sign-Web-Editor，PrisamaX0124 fork：
 * https://github.com/prisamax0124/CRT-Sign-Web-Editor 。
 * 本类是用 MTR 映射层（{@link GraphicsHolder}/{@link org.mtr.mapping.mapper.GuiDrawing}）
 * 重写的 Minecraft 端 Java 实现，只沿用其界面组织方式与视觉规范，未复制其源码；
 * 完整声明见 docs/THIRD_PARTY_NOTICES.md。</p>
 *
 * <h2>结构</h2>
 * <ul>
 *   <li>左 260：元素选择区（方向图标 12、标识与文字、图标与间隔）。</li>
 *   <li>中：选择/剪贴板工具条 + 画布（行带、行控件、选中框、拖放指示器）+ 添加行/缩放。</li>
 *   <li>右 260：未选中 → 标识牌设置；选中 1 个 → 元素属性；多选 → 批量属性。</li>
 * </ul>
 *
 * <p>所有改动都会写入 {@link CRTSignHistory}（撤销/重做），并通过
 * {@link CRTSignEditorHost#save(CRTSignLayout)} 节流保存。MTR 数据通过
 * {@link CRTSignDataContext} 实时解析，绑定选择走 MTR 的
 * {@link DashboardListSelectorScreen}。</p>
 *
 * <p><b>说明：</b>渲染层（{@code CRTSignLayoutPreview} 等）由并行模块实现；
 * 在它就绪前，本屏幕用一套自带的线框画布完成选中/拖拽/行控件所需的几何与命中测试，
 * 保证"选中 / 改属性 / 增删元素 / 保存 / 撤销"的最小闭环可编译可运行。</p>
 */
public class CRTSignEditorScreen extends ScreenExtension implements CRTEditorWidgets.FocusHost {

	// ================================================================ 常量

	/** 顶栏高度。 */
	private static final int TOP_BAR_HEIGHT = 26;
	/** 面板头高度。 */
	private static final int PANEL_HEADER_HEIGHT = 26;
	/** 中栏工具条高度。 */
	private static final int TOOLBAR_HEIGHT = 26;
	/** 中栏底部「添加行 + 缩放」区域高度。 */
	private static final int CENTER_BOTTOM_HEIGHT = 46;
	/** 中栏画布内边距。 */
	private static final int CANVAS_PADDING = 8;
	/** 画布右侧给“行控件”（↑↓色✕）预留的固定栏宽，避免盖住牌面内容。 */
	private static final int CANVAS_RIGHT_GUTTER = 74;
	/** 右栏滚动条宽度。 */
	private static final int SCROLLBAR_WIDTH = 5;
	/** 拖拽判定阈值（像素）。 */
	private static final double DRAG_THRESHOLD = 4.0;
	/** 编辑历史合并窗口（毫秒）。 */
	private static final long HISTORY_GROUP_MS = 800L;
	/** 保存节流（毫秒）。 */
	private static final long SAVE_THROTTLE_MS = 200L;

	/** 12 个方向图标 id（与网页 js/icons.js 一致）。 */
	private static final String[] DIRECTION_ICONS = {
			"arrow_up", "arrow_down", "arrow_left", "arrow_right",
			"arrow_left_up", "arrow_right_up", "arrow_right_down", "arrow_left_down",
			"arrow_ahead_left", "arrow_ahead_right", "arrow_back_left", "arrow_back_right"
	};
	/** 方向图标显示字符（MC 字体可能缺字时退化为文字标签）。 */
	private static final String[] DIRECTION_GLYPHS = {
			"↑", "↓", "←", "→", "↖", "↗", "↘", "↙", "⇠", "⇢", "↺", "↻"
	};
	/** 20 个服务设施图标 id（与网页 js/icons.js 一致）。 */
	private static final String[] SERVICE_ICONS = {
			"accessible", "accessible_elevator_platform", "accessible_passage", "accessible_ramp",
			"accessible_restroom", "check_in", "china_railway", "elevator", "exit", "exit_left",
			"exit_right", "metro", "monorail", "no_entry", "nursing", "restroom",
			"stairs", "tickets", "train", "waiting"
	};
	/** 服务图标显示字符（缩略图占位，图标选择网格里也会用贴图，由渲染层负责最终显示）。 */
	private static final String[] SERVICE_GLYPHS = {
			"♿", "♿", "♿", "♿", "♿", "✓", "铁", "↕", "出", "←", "→", "地", "单", "⊘", "婴", "卫", "梯", "票", "车", "候"
	};

	/** 右栏三种模式（与网页 panelMode 一致）。 */
	private enum RightMode {
		SETTINGS, ELEMENT, BATCH
	}

	// ================================================================ 宿主与数据

	@Nullable
	private final ScreenExtension parent;
	private final BlockPos signPos;
	private final CRTSignDataContext data;
	private final CRTSignEditorHost host;
	private final String cacheKey;
	/** 当前布局（撤销/重做时会整体替换引用）。 */
	private CRTSignLayout layout;
	private final CRTSignHistory history = new CRTSignHistory();

	// ================================================================ 选择状态

	/** 选中元素 id（按选中先后；主选中单独保存）。 */
	private final List<String> selection = new ArrayList<>();
	@Nullable
	private String primaryId;
	@Nullable
	private String anchorId;

	// ================================================================ 剪贴板 / 撤销分组 / 保存

	@Nullable
	private String clipboardJson;
	private int clipboardCount;
	@Nullable
	private String cutRowId;
	private int cutGap;
	@Nullable
	private String historyKey;
	private long historyTime;
	private boolean layoutDirty;
	private long lastEditTime;
	private long lastSaveTime;

	// ================================================================ 布局矩形

	private final Rect topBarRect = new Rect();
	private final Rect leftPanelRect = new Rect();
	private final Rect leftHeaderRect = new Rect();
	private final Rect leftBodyRect = new Rect();
	private final Rect rightPanelRect = new Rect();
	private final Rect rightHeaderRect = new Rect();
	private final Rect rightBodyRect = new Rect();
	private final Rect centerPanelRect = new Rect();
	private final Rect centerToolbarRect = new Rect();
	private final Rect canvasViewportRect = new Rect();
	private final Rect centerBottomRect = new Rect();
	private boolean leftCollapsed;
	private boolean rightCollapsed;

	// ================================================================ 滚动 / 缩放 / 画布几何

	private int leftScroll;
	private int rightScroll;
	private int leftContentHeight;
	private int rightContentHeight;
	public float canvasZoom = 1.0F;
	private float canvasScrollX;
	private float canvasScrollY;
	private float canvasScale = 1.0F;
	private float signScreenX;
	private float signScreenY;
	private float signScreenW;
	private float signScreenH;
	private float signContentW;
	private float signContentH;
	private final List<CanvasRow> canvasRows = new ArrayList<>();
	private final List<CanvasElement> canvasElements = new ArrayList<>();
	private final List<RowChromeButton> rowChromeButtons = new ArrayList<>();
	private final List<RowHandle> rowHandles = new ArrayList<>();
	private int hoverRowIndex = -1;
	@Nullable
	private String hoverElementId;
	/** 渲染层返回的坐标换算（真实布局帧）；渲染层未就绪时退回自带的近似几何。 */
	@Nullable
	private CRTSignLayoutPreview.Transform previewTransform;
	private int lastMouseX;
	private int lastMouseY;
	/** 当前帧布局 JSON 哈希（与 preview 的布局缓存共用）。 */
	private int currentLayoutHash;
	/** 上一次同步到屏幕坐标的几何签名（相同则跳过重建，省掉每帧分配）。 */
	private float lastSyncX = Float.NaN;
	private float lastSyncY = Float.NaN;
	private float lastSyncW = Float.NaN;
	private float lastSyncH = Float.NaN;
	private float lastSyncScale = Float.NaN;
	private int lastSyncLayoutHash;
	private int lastSyncSelectionHash;
	/** 手动拖拽过的侧栏宽度（-1 = 自动比例）；双击分栏线恢复 -1。 */
	private int customLeftWidth = -1;
	private int customRightWidth = -1;
	private boolean resizingLeft;
	private boolean resizingRight;
	private int resizeStartMouseX;
	private int resizeStartWidth;
	private long lastDividerClickTime;
	private int lastDividerKind;
	/** 画布拖动平移：按下空白/中键后，拖动超过阈值才平移；松手未移动则按“空白单击”处理。 */
	private boolean canvasPanCandidate;
	private boolean canvasPanning;
	private double canvasPanPressX;
	private double canvasPanPressY;
	private double canvasPanStartScrollX;
	private double canvasPanStartScrollY;
	private long lastCanvasClickTime;
	private double lastCanvasClickX;
	private double lastCanvasClickY;

	// ================================================================ 控件

	private final List<CRTEditorWidgets.Widget> topWidgets = new ArrayList<>();
	private final List<CRTEditorWidgets.Widget> centerWidgets = new ArrayList<>();
	private final List<CRTEditorWidgets.Widget> rightWidgets = new ArrayList<>();
	private final List<TextLine> rightTexts = new ArrayList<>();
	private final List<Section> rightSections = new ArrayList<>();
	private boolean widgetsDirty = true;
	@Nullable
	private CRTEditorWidgets.Widget focusedWidget;
	@Nullable
	private CRTEditorWidgets.Widget pressedWidget;
	@Nullable
	private CRTEditorWidgets.Dropdown openDropdown;
	@Nullable
	private String toastText;
	private boolean toastError;
	private long toastUntil;

	// ================================================================ 确认态（危险操作两段点击）

	@Nullable
	private String armedDeleteRowId;
	@Nullable
	private String armedClear;
	@Nullable
	private String armedDeleteElementId;

	// ================================================================ 左栏元素卡片

	private final List<PaletteItem> paletteItems = new ArrayList<>();
	@Nullable
	private PaletteItem pressedPaletteItem;
	@Nullable
	private PaletteItem draggingPaletteItem;
	private double palettePressX;
	private double palettePressY;

	// ================================================================ 左栏页签 / 预设（§3.1.3 / §7.3）

	private static final int LEFT_TAB_ELEMENTS = 0;
	private static final int LEFT_TAB_PRESETS = 1;
	/** 页签高度。 */
	private static final int LEFT_TAB_HEIGHT = 22;
	/** 页签区总高（页签固定不滚动，内容从这里往下画）。 */
	private static final int LEFT_TAB_AREA = LEFT_TAB_HEIGHT + 8;
	/** 预设页「存为预设」按钮区高度。 */
	private static final int PRESET_HEADER_HEIGHT = 26;
	private static final int PRESET_CARD_HEIGHT = 44;
	private static final int PRESET_CARD_GAP = 6;
	private int leftTab = LEFT_TAB_ELEMENTS;
	private final List<CRTEditorWidgets.Widget> leftWidgets = new ArrayList<>();
	private final List<CRTEditorPresets.Preset> presets = new ArrayList<>();
	private final List<PresetCard> presetCards = new ArrayList<>();
	private boolean presetsLoaded;
	private boolean presetModalOpen;
	private String presetNameDraft = "";
	@Nullable
	private String armedDeletePresetId;
	private final List<CRTEditorWidgets.Widget> modalWidgets = new ArrayList<>();
	private final Rect presetModalRect = new Rect();

	/** 预设页签里的一张卡片（x 为屏幕坐标，y 为内容坐标）。 */
	private static final class PresetCard {
		private final CRTEditorPresets.Preset preset;
		private final int x;
		private final int y;
		private final int w;
		private final int h;

		private PresetCard(CRTEditorPresets.Preset preset, int x, int y, int w, int h) {
			this.preset = preset;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		private Rect deleteRect(int screenY) {
			return new Rect(x + w - 18, screenY + 4, 12, 12);
		}
	}

	// ================================================================ 画布拖拽会话

	private boolean elementPressActive;
	@Nullable
	private String elementPressId;
	private double elementPressX;
	private double elementPressY;
	private boolean elementDragging;
	private final List<String> elementDragIds = new ArrayList<>();
	@Nullable
	private String dropRowId;
	private int dropRowIndex = -1;
	private int dropGap;
	private float dropIndicatorX;
	private boolean dropValid;

	private boolean rowHandlePressed;
	private int rowDragFrom = -1;
	private int rowDragBoundary = -1;
	private boolean rowDragging;

	// ================================================================ 左栏卡片缩略图（真实元素烘焙，带缓存）

	/** 卡片缩略图烘焙行高（设计像素；绘制时按卡片尺寸等比缩放）。 */
	private static final float THUMB_ROW_HEIGHT = 64.0F;
	/** 图标选择器缩略图烘焙行高（图标是正方形，画到小格子里）。 */
	private static final float ICON_THUMB_ROW_HEIGHT = 32.0F;
	private static final long THUMB_BAKE_RETRY_MS = 1000L;

	/** 一张缩略图烘焙结果及其失败退避时间。 */
	private static final class ThumbnailEntry {
		@Nullable
		private JSSignTextureBaker.Baked baked;
		private long lastAttempt;
	}

	private final Map<String, ThumbnailEntry> thumbnailCache = new HashMap<>();
	private final Map<String, CRTSignElement> thumbnailPrototypes = new HashMap<>();

	// ================================================================ MTR 绑定选择回传

	@Nullable
	private String pendingBindingElementId;
	@Nullable
	private LongAVLTreeSet pendingBindingIds;

	// ================================================================ 项目 JSON 导入回传

	/** JSON 导入屏幕返回后要应用的文本（消耗一次）。 */
	@Nullable
	private String pendingImportJson;
	/** 整牌剪贴板（同一游戏进程内跨牌共享，对应旧格子编辑器的“复制整牌/粘贴整牌”）。 */
	private static String boardClipboardJson;

	// ================================================================ 右栏标识牌设置里的行选择

	private int settingsRowIndex;
	/** 颜色选择器展开状态的共享存储（控件重建后保持展开）。 */
	private final java.util.Map<String, boolean[]> colorPickerStates = new java.util.HashMap<>();

	// ================================================================ 构造

	/**
	 * @param parent  返回时重新打开的父屏幕（可为 null，表示关闭回游戏）
	 * @param signPos 指示牌方块坐标（预览缓存键 / 日志用）
	 * @param layout  当前布局（屏幕内直接编辑这个实例；撤销时替换引用）
	 * @param data    MTR 数据访问接口（解析绑定内容）
	 * @param host    保存回调与 MTR 选择列表
	 */
	public CRTSignEditorScreen(@Nullable ScreenExtension parent, BlockPos signPos, CRTSignLayout layout,
			CRTSignDataContext data, CRTSignEditorHost host) {
		super(TextHelper.translatable("gui.nanbin.crt_editor.title"));
		this.parent = parent;
		this.signPos = signPos == null ? BlockPos.fromLong(0L) : signPos;
		this.layout = layout == null ? CRTSignLayout.createBlank(8) : layout;
		this.data = data == null ? CRTSignDataContext.EMPTY : data;
		this.host = host;
		this.cacheKey = "crt_editor:" + this.signPos.toShortString();
		// 配置屏幕里的「深色模式」开关（默认浅色）
		CRTEditorTheme.applyDarkMode(NanbinClientConfig.isDarkMode());
	}

	// ================================================================ 内部数据类

	/** 屏幕绝对坐标矩形。 */
	private static final class Rect {
		private int x;
		private int y;
		private int w;
		private int h;

		private Rect() {
		}

		private Rect(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		private void set(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		private boolean contains(double px, double py) {
			return px >= x && px < x + w && py >= y && py < y + h;
		}

		private boolean isEmpty() {
			return w <= 0 || h <= 0;
		}
	}

	/** 右栏纯文字行（标签 / 提示 / 分组标题）。 */
	private static final class TextLine {
		private final String text;
		private final int x;
		private final int y;
		private final int color;
		private final int maxWidth;
		private final boolean tooltipOnly;
		private final float scale;

		private TextLine(String text, int x, int y, int color, int maxWidth) {
			this(text, x, y, color, maxWidth, false, CRTEditorWidgets.FONT_SCALE_BODY);
		}

		private TextLine(String text, int x, int y, int color, int maxWidth, boolean tooltipOnly) {
			this(text, x, y, color, maxWidth, tooltipOnly, CRTEditorWidgets.FONT_SCALE_BODY);
		}

		private TextLine(String text, int x, int y, int color, int maxWidth, float scale) {
			this(text, x, y, color, maxWidth, false, scale);
		}

		private TextLine(String text, int x, int y, int color, int maxWidth, boolean tooltipOnly, float scale) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.color = color;
			this.maxWidth = maxWidth;
			this.tooltipOnly = tooltipOnly;
			this.scale = scale;
		}
	}

	/** 右栏属性分组卡片（.panel-section）。 */
	private static final class Section {
		private final int x;
		private final int y;
		private final int w;
		private final int h;

		private Section(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}

	/** 左栏卡片/分类标题（内容坐标；渲染时加左栏滚动偏移）。 */
	private static final class PaletteItem {
		private final boolean header;
		private final String label;
		@Nullable
		private final CRTSignElementType type;
		@Nullable
		private final String iconId;
		@Nullable
		private final String glyph;
		@Nullable
		private final String subLabel;
		/** 卡片缩略图用的示例元素（真实渲染，添加到画布时深拷贝）。 */
		@Nullable
		private CRTSignElement prototype;
		private int x;
		private int y;
		private int w;
		private int h;

		private PaletteItem(boolean header, String label, @Nullable CRTSignElementType type, @Nullable String iconId,
				@Nullable String glyph, @Nullable String subLabel) {
			this.header = header;
			this.label = label;
			this.type = type;
			this.iconId = iconId;
			this.glyph = glyph;
			this.subLabel = subLabel;
		}
	}

	/** 画布上一行的屏幕几何。 */
	private static final class CanvasRow {
		private final CRTSignLayout.Row row;
		private final int index;
		private float x;
		private float y;
		private float w;
		private float h;

		private CanvasRow(CRTSignLayout.Row row, int index) {
			this.row = row;
			this.index = index;
		}
	}

	/** 画布上一个元素的屏幕几何。 */
	private static final class CanvasElement {
		private final CRTSignElement element;
		private final CRTSignLayout.Row row;
		private final int rowIndex;
		private final int index;
		private float x;
		private float y;
		private float w;
		private float h;

		private CanvasElement(CRTSignElement element, CRTSignLayout.Row row, int rowIndex, int index) {
			this.element = element;
			this.row = row;
			this.rowIndex = rowIndex;
			this.index = index;
		}
	}

	/** 行右上角的小按钮（上移/下移/行背景色/删除）。 */
	private static final class RowChromeButton {
		private static final int ACTION_UP = 0;
		private static final int ACTION_DOWN = 1;
		private static final int ACTION_BG = 2;
		private static final int ACTION_DELETE = 3;
		private final Rect rect;
		private final int rowIndex;
		private final int action;

		private RowChromeButton(Rect rect, int rowIndex, int action) {
			this.rect = rect;
			this.rowIndex = rowIndex;
			this.action = action;
		}
	}

	/** 行左侧拖动排序手柄。 */
	private static final class RowHandle {
		private final Rect rect;
		private final int rowIndex;

		private RowHandle(Rect rect, int rowIndex) {
			this.rect = rect;
			this.rowIndex = rowIndex;
		}
	}

	// ================================================================ 浮动元素工具条 / 内边距拖拽

	/** 工具条按钮动作。 */
	private static final int TOOLBAR_MOVE_LEFT = 0;
	private static final int TOOLBAR_MOVE_RIGHT = 1;
	private static final int TOOLBAR_COPY = 2;
	private static final int TOOLBAR_DELETE = 3;
	private static final int TOOLBAR_BUTTON_SIZE = 15;
	private static final int TOOLBAR_GAP = 2;
	private static final int TOOLBAR_PADDING = 2;
	/** 内边距拖拽：8 个 GUI 像素 = 0.05 行高比例（网页 §6.7）。 */
	private static final double PADDING_SCRUB_PIXELS_PER_STEP = 8.0;
	private static final float PADDING_SCRUB_STEP = 0.05F;

	/** 浮动工具条上的一个按钮（屏幕坐标）。 */
	private static final class ElementToolbarButton {
		private final Rect rect;
		private final int action;

		private ElementToolbarButton(Rect rect, int action) {
			this.rect = rect;
			this.action = action;
		}
	}

	private final List<ElementToolbarButton> elementToolbarButtons = new ArrayList<>();
	private final Rect paddingLeftHandle = new Rect();
	private final Rect paddingRightHandle = new Rect();
	private boolean paddingDragActive;
	private int paddingDragSide;
	@Nullable
	private String paddingDragElementId;
	private double paddingDragLastX;
	private double paddingDragAccumulator;

	// ================================================================ 生命周期

	@Override
	protected void init2() {
		super.init2();
		updateLayout();
		rebuildAllWidgets();
		rebuildPalette();
		layoutCanvas();
		applyPendingBinding();
		applyPendingJsonImport();
	}

	@Override
	public void tick2() {
		super.tick2();
		if (layoutDirty && System.currentTimeMillis() - lastEditTime >= SAVE_THROTTLE_MS) {
			saveNow();
		}
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

	@Override
	public void resize2(MinecraftClient client, int width, int height) {
		super.resize2(client, width, height);
		updateLayout();
		rebuildAllWidgets();
		rebuildPalette();
		layoutCanvas();
	}

	// ================================================================ 布局

	private void updateLayout() {
		final int screenWidth = Math.max(1, getWidthMapped());
		final int screenHeight = Math.max(1, getHeightMapped());
		topBarRect.set(0, 0, screenWidth, TOP_BAR_HEIGHT);

		// 侧栏宽度：网页固定 260；屏幕较小时按比例压缩，中栏永远保留可编辑的最小宽度
		final int preferredSide = CRTEditorWidgets.clamp(screenWidth * 26 / 100, 132, CRTEditorTheme.PANEL_WIDTH);
		int leftWidth = leftCollapsed ? 0 : preferredSide;
		int rightWidth = rightCollapsed ? 0 : preferredSide;
		// 中栏至少占 1/3（上限 320、下限 140），两侧栏平分剩余空间
		final int minimumCenter = CRTEditorWidgets.clamp(screenWidth / 3, 140, 320);
		final int spaceForSides = Math.max(0, screenWidth - minimumCenter);
		if (leftWidth + rightWidth > spaceForSides) {
			if (leftWidth > 0 && rightWidth > 0) {
				final int half = Math.max(110, spaceForSides / 2);
				leftWidth = Math.min(leftWidth, half);
				rightWidth = Math.min(rightWidth, Math.max(110, spaceForSides - leftWidth));
				if (leftWidth + rightWidth > spaceForSides) {
					leftWidth = Math.max(0, spaceForSides - rightWidth);
				}
			} else if (leftWidth > 0) {
				leftWidth = spaceForSides;
			} else {
				rightWidth = spaceForSides;
			}
		}
		// 用户手动拖拽过的分栏宽度优先（双击分栏线恢复自动比例）
		if (!leftCollapsed && customLeftWidth > 0) {
			leftWidth = CRTEditorWidgets.clamp(customLeftWidth, 120, 420);
		}
		if (!rightCollapsed && customRightWidth > 0) {
			rightWidth = CRTEditorWidgets.clamp(customRightWidth, 120, 420);
		}
		// 中栏保底：两侧总宽超过可用空间时优先压缩较宽的一侧
		final int centerFloor = CRTEditorWidgets.clamp(screenWidth / 4, 120, 280);
		int guard = 0;
		while (leftWidth + rightWidth + centerFloor > screenWidth && (leftWidth > 0 || rightWidth > 0) && guard++ < screenWidth) {
			if (rightWidth >= leftWidth) {
				rightWidth = Math.max(0, rightWidth - 2);
			} else {
				leftWidth = Math.max(0, leftWidth - 2);
			}
		}
		final int bodyY = TOP_BAR_HEIGHT;
		final int bodyHeight = Math.max(40, screenHeight - TOP_BAR_HEIGHT);

		leftPanelRect.set(0, bodyY, leftWidth, bodyHeight);
		leftHeaderRect.set(0, bodyY, leftWidth, PANEL_HEADER_HEIGHT);
		leftBodyRect.set(0, bodyY + PANEL_HEADER_HEIGHT, leftWidth, bodyHeight - PANEL_HEADER_HEIGHT);

		rightPanelRect.set(screenWidth - rightWidth, bodyY, rightWidth, bodyHeight);
		rightHeaderRect.set(screenWidth - rightWidth, bodyY, rightWidth, PANEL_HEADER_HEIGHT);
		rightBodyRect.set(screenWidth - rightWidth, bodyY + PANEL_HEADER_HEIGHT, rightWidth, bodyHeight - PANEL_HEADER_HEIGHT);

		centerPanelRect.set(leftWidth, bodyY, Math.max(40, screenWidth - leftWidth - rightWidth), bodyHeight);
		centerToolbarRect.set(centerPanelRect.x, bodyY, centerPanelRect.w, TOOLBAR_HEIGHT);
		centerBottomRect.set(centerPanelRect.x, bodyY + bodyHeight - CENTER_BOTTOM_HEIGHT, centerPanelRect.w, CENTER_BOTTOM_HEIGHT);
		final int canvasY = centerToolbarRect.y + TOOLBAR_HEIGHT + 1;
		canvasViewportRect.set(centerPanelRect.x, canvasY, centerPanelRect.w,
				Math.max(30, centerBottomRect.y - canvasY - 1));
	}

	// ================================================================ 焦点宿主

	@Override
	public void requestFocus(CRTEditorWidgets.Widget widget) {
		if (focusedWidget != widget) {
			blurFocused();
			focusedWidget = widget;
		}
	}

	@Nullable
	@Override
	public CRTEditorWidgets.Widget getFocusedWidget() {
		return focusedWidget;
	}

	@Override
	public void clearFocus() {
		focusedWidget = null;
	}

	@Override
	public void widgetLayoutChanged() {
		widgetsDirty = true;
	}

	@Override
	public void openDropdown(CRTEditorWidgets.Dropdown dropdown) {
		blurFocused();
		openDropdown = dropdown;
	}

	@Nullable
	@Override
	public CRTEditorWidgets.Dropdown getOpenDropdown() {
		return openDropdown;
	}

	@Override
	public void closeDropdown() {
		openDropdown = null;
	}

	/** 失焦：先清空引用再回调，避免 onBlur 里的 clearFocus 递归。 */
	private void blurFocused() {
		final CRTEditorWidgets.Widget widget = focusedWidget;
		focusedWidget = null;
		if (widget != null) {
			widget.onBlur(eventCtx());
		}
	}

	// ================================================================ 上下文 / 保存 / 提示

	private CRTEditorWidgets.Ctx frameCtx(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
		return new CRTEditorWidgets.Ctx(graphics, getTextRendererMapped(), mouseX, mouseY, delta, this);
	}

	private CRTEditorWidgets.Ctx eventCtx() {
		return new CRTEditorWidgets.Ctx(null, null, lastMouseX, lastMouseY, 0.0F, this);
	}

	private void markEdited(boolean structural) {
		// 内容变化：命中测试几何/预览缓存立即失效，避免拖动用的是旧坐标
		previewTransform = null;
		layoutDirty = true;
		lastEditTime = System.currentTimeMillis();
		if (structural) {
			widgetsDirty = true;
		}
		layoutCanvas();
	}

	private void saveNow() {
		if (layoutDirty) {
			layoutDirty = false;
			lastSaveTime = System.currentTimeMillis();
			if (host != null) {
				host.save(layout);
			}
		}
	}

	private void toast(String message) {
		toast(message, false);
	}

	private void toast(String message, boolean error) {
		toastText = message;
		toastError = error;
		toastUntil = System.currentTimeMillis() + (error ? 4200L : 2400L);
	}

	private static String tr(String key) {
		return TextHelper.translatable(key).getString();
	}

	private static String tr(String key, Object... args) {
		return TextHelper.translatable(key, args).getString();
	}

	// ================================================================ 控件重建

	private void ensureWidgets() {
		if (widgetsDirty) {
			rebuildAllWidgets();
		}
	}

	private void rebuildAllWidgets() {
		topWidgets.clear();
		centerWidgets.clear();
		rebuildTopBar();
		rebuildCenterWidgets();
		rebuildRightWidgets();
		rebuildPalette();
		widgetsDirty = false;
	}

	// ================================================================ 顶栏控件

	private void rebuildTopBar() {
		topWidgets.clear();
		int right = getWidthMapped() - 4;
		final CRTEditorWidgets.Button close = addTopButton(right, 38, tr("gui.nanbin.crt_editor.close"), CRTEditorWidgets.ButtonStyle.SECONDARY, this::onClose2);
		right = close.x - 4;
		final CRTEditorWidgets.Button redo = addTopButton(right, 34, tr("gui.nanbin.crt_editor.redo"), CRTEditorWidgets.ButtonStyle.TOOL, this::redo);
		redo.enabled = history.canRedo();
		right = redo.x - 4;
		final CRTEditorWidgets.Button undo = addTopButton(right, 34, tr("gui.nanbin.crt_editor.undo"), CRTEditorWidgets.ButtonStyle.TOOL, this::undo);
		undo.enabled = history.canUndo();

		// 侧栏折叠按钮移到面板标题栏右侧（与网页的面板头图标按钮一致），顶栏只留编辑动作
		final int headerY = TOP_BAR_HEIGHT + (PANEL_HEADER_HEIGHT - 16) / 2;
		if (leftCollapsed) {
			final CRTEditorWidgets.Button expandLeft = new CRTEditorWidgets.Button(14, 16, "›", CRTEditorWidgets.ButtonStyle.ICON, () -> {
				leftCollapsed = false;
				updateLayout();
				widgetsDirty = true;
			});
			expandLeft.setBounds(2, headerY, 14);
			expandLeft.tooltip = tr("gui.nanbin.crt_editor.toggle_elements");
			topWidgets.add(expandLeft);
		} else if (leftHeaderRect.w > 30) {
			final CRTEditorWidgets.Button collapseLeft = new CRTEditorWidgets.Button(14, 16, "‹", CRTEditorWidgets.ButtonStyle.ICON, () -> {
				leftCollapsed = true;
				updateLayout();
				widgetsDirty = true;
			});
			collapseLeft.setBounds(leftHeaderRect.x + leftHeaderRect.w - 18, headerY, 14);
			collapseLeft.tooltip = tr("gui.nanbin.crt_editor.toggle_elements");
			topWidgets.add(collapseLeft);
		}
		if (rightCollapsed) {
			final CRTEditorWidgets.Button expandRight = new CRTEditorWidgets.Button(14, 16, "‹", CRTEditorWidgets.ButtonStyle.ICON, () -> {
				rightCollapsed = false;
				updateLayout();
				widgetsDirty = true;
			});
			expandRight.setBounds(getWidthMapped() - 16, headerY, 14);
			expandRight.tooltip = tr("gui.nanbin.crt_editor.toggle_props");
			topWidgets.add(expandRight);
		} else if (rightHeaderRect.w > 30) {
			final CRTEditorWidgets.Button collapseRight = new CRTEditorWidgets.Button(14, 16, "›", CRTEditorWidgets.ButtonStyle.ICON, () -> {
				rightCollapsed = true;
				updateLayout();
				widgetsDirty = true;
			});
			collapseRight.setBounds(rightHeaderRect.x + rightHeaderRect.w - 18, headerY, 14);
			collapseRight.tooltip = tr("gui.nanbin.crt_editor.toggle_props");
			topWidgets.add(collapseRight);
		}
	}

	private CRTEditorWidgets.Button addTopButton(int rightEdge, int width, String label, CRTEditorWidgets.ButtonStyle style, Runnable action) {
		final CRTEditorWidgets.Button button = new CRTEditorWidgets.Button(width, 18, label, style, action);
		button.setBounds(rightEdge - width, (TOP_BAR_HEIGHT - 18) / 2, width);
		topWidgets.add(button);
		return button;
	}

	// ================================================================ 中栏工具条控件

	private void rebuildCenterWidgets() {
		centerWidgets.clear();
		final int y = centerToolbarRect.y + 5;
		int x = centerToolbarRect.x + 6;
		final boolean hasElements = layout.elementCount() > 0;
		final boolean hasSelection = !selection.isEmpty();

		final CRTEditorWidgets.Button selectAll = new CRTEditorWidgets.Button(40, 18, tr("gui.nanbin.crt_editor.select_all"), CRTEditorWidgets.ButtonStyle.TOOL, this::selectAll);
		selectAll.setBounds(x, y, 40);
		selectAll.enabled = hasElements;
		selectAll.tooltip = tr("gui.nanbin.crt_editor.select_all.tip");
		centerWidgets.add(selectAll);
		x += 43;

		final CRTEditorWidgets.Button copy = new CRTEditorWidgets.Button(38, 18, tr("gui.nanbin.crt_editor.copy"), CRTEditorWidgets.ButtonStyle.TOOL, () -> copySelection(false));
		copy.setBounds(x, y, 38);
		copy.enabled = hasSelection;
		copy.tooltip = tr("gui.nanbin.crt_editor.copy.tip");
		centerWidgets.add(copy);
		x += 41;

		final CRTEditorWidgets.Button cut = new CRTEditorWidgets.Button(38, 18, tr("gui.nanbin.crt_editor.cut"), CRTEditorWidgets.ButtonStyle.TOOL, () -> copySelection(true));
		cut.setBounds(x, y, 38);
		cut.enabled = hasSelection;
		cut.tooltip = tr("gui.nanbin.crt_editor.cut.tip");
		centerWidgets.add(cut);
		x += 41;

		final CRTEditorWidgets.Button paste = new CRTEditorWidgets.Button(38, 18, tr("gui.nanbin.crt_editor.paste"), CRTEditorWidgets.ButtonStyle.TOOL, this::pasteClipboard);
		paste.setBounds(x, y, 38);
		paste.enabled = clipboardJson != null;
		paste.tooltip = tr("gui.nanbin.crt_editor.paste.tip");
		centerWidgets.add(paste);

		// 底栏第一行：整牌级四按钮（复制整牌 / 粘贴整牌 / 清空整牌 / 撤销）
		final int boardRowY = centerBottomRect.y + 4;
		int boardX = centerBottomRect.x + 8;
		final CRTEditorWidgets.Button copyBoard = new CRTEditorWidgets.Button(54, 18, tr("gui.nanbin.crt_editor.board.copy"), CRTEditorWidgets.ButtonStyle.TOOL, this::copyBoardAction);
		copyBoard.setBounds(boardX, boardRowY, 54);
		copyBoard.tooltip = tr("gui.nanbin.crt_editor.board.copy.tip");
		centerWidgets.add(copyBoard);
		boardX += 57;
		final CRTEditorWidgets.Button pasteBoard = new CRTEditorWidgets.Button(54, 18, tr("gui.nanbin.crt_editor.board.paste"), CRTEditorWidgets.ButtonStyle.TOOL, this::pasteBoardAction);
		pasteBoard.setBounds(boardX, boardRowY, 54);
		pasteBoard.enabled = boardClipboardJson != null;
		pasteBoard.tooltip = tr("gui.nanbin.crt_editor.board.paste.tip");
		centerWidgets.add(pasteBoard);
		boardX += 57;
		final CRTEditorWidgets.Button clearBoard = new CRTEditorWidgets.Button(54, 18, tr("gui.nanbin.crt_editor.board.clear"), CRTEditorWidgets.ButtonStyle.TOOL, this::clearSignAction);
		clearBoard.setBounds(boardX, boardRowY, 54);
		clearBoard.enabled = layout.elementCount() > 0;
		clearBoard.tooltip = tr("gui.nanbin.crt_editor.board.clear.tip");
		centerWidgets.add(clearBoard);
		boardX += 57;
		final CRTEditorWidgets.Button undoBoard = new CRTEditorWidgets.Button(40, 18, tr("gui.nanbin.crt_editor.board.undo"), CRTEditorWidgets.ButtonStyle.TOOL, this::undo);
		undoBoard.setBounds(boardX, boardRowY, 40);
		undoBoard.enabled = history.canUndo();
		undoBoard.tooltip = tr("gui.nanbin.crt_editor.board.undo.tip");
		centerWidgets.add(undoBoard);

		// 底栏第二行：添加行（左）+ 缩放（右）
		final int bottomY = centerBottomRect.y + 26;
		final CRTEditorWidgets.Button addRow = new CRTEditorWidgets.Button(96, 18, tr("gui.nanbin.crt_editor.add_row"), CRTEditorWidgets.ButtonStyle.DASHED, this::addRowAction);
		addRow.setBounds(centerBottomRect.x + 8, bottomY, 96);
		addRow.tooltip = tr("gui.nanbin.crt_editor.add_row.tip");
		centerWidgets.add(addRow);

		int zoomRight = centerBottomRect.x + centerBottomRect.w - 6;
		final CRTEditorWidgets.Button zoomBadge = new CRTEditorWidgets.Button(46, 16, Math.round(canvasZoom * 100.0F) + "%", CRTEditorWidgets.ButtonStyle.TOOL, this::resetCanvasZoom);
		zoomBadge.setBounds(zoomRight - 46, bottomY, 46);
		zoomBadge.tooltip = tr("gui.nanbin.crt_editor.zoom.tip");
		centerWidgets.add(zoomBadge);
		zoomRight -= 49;
		final CRTEditorWidgets.Button zoomIn = new CRTEditorWidgets.Button(16, 16, "+", CRTEditorWidgets.ButtonStyle.TOOL, () -> changeCanvasZoom(1.15F));
		zoomIn.setBounds(zoomRight - 16, bottomY, 16);
		centerWidgets.add(zoomIn);
		zoomRight -= 18;
		final CRTEditorWidgets.Button zoomOut = new CRTEditorWidgets.Button(16, 16, "−", CRTEditorWidgets.ButtonStyle.TOOL, () -> changeCanvasZoom(1.0F / 1.15F));
		zoomOut.setBounds(zoomRight - 16, bottomY, 16);
		centerWidgets.add(zoomOut);
	}

	// ================================================================ 左栏元素卡片（内容坐标）

	private void addPaletteHeader(String label, int x, int y) {
		final PaletteItem item = new PaletteItem(true, label, null, null, null, null);
		item.x = x;
		item.y = y;
		item.w = Math.max(1, leftPanelRect.w - CRTEditorWidgets.PANEL_PADDING * 2);
		item.h = PALETTE_HEADER_HEIGHT;
		paletteItems.add(item);
	}

	/** 左栏分类标题高度（1.15× 字号，比正文行高略高）。 */
	private static final int PALETTE_HEADER_HEIGHT = 13;

	private void rebuildPalette() {
		paletteItems.clear();
		presetCards.clear();
		leftWidgets.clear();
		if (leftPanelRect.w <= 0) {
			leftContentHeight = 0;
			return;
		}
		final int contentW = Math.max(60, leftPanelRect.w - CRTEditorWidgets.PANEL_PADDING * 2);
		buildLeftTabs(contentW);
		if (leftTab == LEFT_TAB_PRESETS) {
			buildPresetTab(contentW);
		} else {
			buildElementsTab(contentW);
		}
	}

	/** 左栏顶部「元素 / 预设」页签 + 预设页的「存为预设」按钮（固定在顶部，不随内容滚动）。 */
	private void buildLeftTabs(int contentW) {
		// 给右侧滚动条留出 2px 余量，避免页签/按钮盖住滑块
		final int usableWidth = Math.max(60, contentW - SCROLLBAR_WIDTH - 2);
		final int tabWidth = Math.max(40, (usableWidth - 3) / 2);
		final int tabY = leftBodyRect.y + 4;
		final CRTEditorWidgets.Button elementsTab = new CRTEditorWidgets.Button(tabWidth, LEFT_TAB_HEIGHT,
				tr("gui.nanbin.crt_editor.tab.elements"), CRTEditorWidgets.ButtonStyle.TOOL, () -> switchLeftTab(LEFT_TAB_ELEMENTS));
		elementsTab.setBounds(leftPanelRect.x + CRTEditorWidgets.PANEL_PADDING, tabY, tabWidth);
		elementsTab.setSelected(leftTab == LEFT_TAB_ELEMENTS);
		leftWidgets.add(elementsTab);
		final CRTEditorWidgets.Button presetsTab = new CRTEditorWidgets.Button(tabWidth, LEFT_TAB_HEIGHT,
				tr("gui.nanbin.crt_editor.tab.presets"), CRTEditorWidgets.ButtonStyle.TOOL, () -> switchLeftTab(LEFT_TAB_PRESETS));
		presetsTab.setBounds(leftPanelRect.x + CRTEditorWidgets.PANEL_PADDING + tabWidth + 3, tabY, tabWidth);
		presetsTab.setSelected(leftTab == LEFT_TAB_PRESETS);
		presetsTab.tooltip = tr("gui.nanbin.crt_editor.preset.tab_tip");
		leftWidgets.add(presetsTab);
		if (leftTab == LEFT_TAB_PRESETS) {
			final CRTEditorWidgets.Button savePreset = new CRTEditorWidgets.Button(usableWidth, 18,
					tr("gui.nanbin.crt_editor.preset.save"), CRTEditorWidgets.ButtonStyle.DASHED, this::openPresetNameModal);
			savePreset.setBounds(leftPanelRect.x + CRTEditorWidgets.PANEL_PADDING, leftBodyRect.y + LEFT_TAB_AREA + 1, usableWidth);
			savePreset.tooltip = tr("gui.nanbin.crt_editor.preset.save_tip");
			leftWidgets.add(savePreset);
		}
	}

	private void switchLeftTab(int tab) {
		if (leftTab == tab) {
			return;
		}
		leftTab = tab;
		leftScroll = 0;
		pressedPaletteItem = null;
		draggingPaletteItem = null;
		rebuildPalette();
	}

	/** 「元素」页签：分类卡片（真实渲染缩略图）。 */
	private void buildElementsTab(int contentW) {
		final int x0 = 0;
		int y = 0;

		// ── 方向指示（12 个图标，4 列） ──
		addPaletteHeader(tr("gui.nanbin.crt_editor.category.direction"), x0, y);
		y += PALETTE_HEADER_HEIGHT + 4;
		final int directionGap = 3;
		final int directionWidth = (contentW - directionGap * 3) / 4;
		final int directionHeight = 30;
		for (int i = 0; i < DIRECTION_ICONS.length; i++) {
			final PaletteItem item = new PaletteItem(false, directionName(DIRECTION_ICONS[i]), CRTSignElementType.ICON,
					DIRECTION_ICONS[i], DIRECTION_GLYPHS[i], null);
			item.x = x0 + (i % 4) * (directionWidth + directionGap);
			item.y = y + (i / 4) * (directionHeight + directionGap);
			item.w = directionWidth;
			item.h = directionHeight;
			paletteItems.add(item);
		}
		y += 3 * (directionHeight + directionGap) + 6;

		// ── 标识与文字 ──
		addPaletteHeader(tr("gui.nanbin.crt_editor.category.signs"), x0, y);
		y += PALETTE_HEADER_HEIGHT + 4;
		final CRTSignElementType[] textTypes = {
				CRTSignElementType.NUMBER_LINE, CRTSignElementType.TEXT_LINE, CRTSignElementType.BIG_NUMBER,
				CRTSignElementType.ENTRANCE, CRTSignElementType.EXIT, CRTSignElementType.EXIT_INFO,
				CRTSignElementType.ROUTE_INFO,
				CRTSignElementType.BILINGUAL_TEXT, CRTSignElementType.SMALL_BILINGUAL_TEXT
		};
		final int cardWidth = (contentW - directionGap) / 2;
		final int cardHeight = 36;
		for (int i = 0; i < textTypes.length; i++) {
			final PaletteItem item = new PaletteItem(false, elementTypeName(textTypes[i]), textTypes[i], null, null, null);
			item.x = x0 + (i % 2) * (cardWidth + directionGap);
			item.y = y + (i / 2) * (cardHeight + directionGap);
			item.w = cardWidth;
			item.h = cardHeight;
			paletteItems.add(item);
		}
		y += ((textTypes.length + 1) / 2) * (cardHeight + directionGap) + 6;

		// ── 图标与间隔 ──
		addPaletteHeader(tr("gui.nanbin.crt_editor.category.misc"), x0, y);
		y += PALETTE_HEADER_HEIGHT + 4;
		final PaletteItem iconItem = new PaletteItem(false, tr("gui.nanbin.crt_editor.element.icon"), CRTSignElementType.ICON, "elevator", "↕", null);
		iconItem.x = x0;
		iconItem.y = y;
		iconItem.w = cardWidth;
		iconItem.h = cardHeight;
		paletteItems.add(iconItem);
		final PaletteItem spaceItem = new PaletteItem(false, tr("gui.nanbin.crt_editor.element.space"), CRTSignElementType.SPACE, null, null, null);
		spaceItem.x = x0 + cardWidth + directionGap;
		spaceItem.y = y;
		spaceItem.w = cardWidth;
		spaceItem.h = cardHeight;
		paletteItems.add(spaceItem);
		y += cardHeight + 8;

		// ── CRT 预设：仅显示当前资源包里真实存在的“图标+文字”牌（crt_signs.json 被清空时自动隐藏） ──
		final java.util.List<CRTPresetCatalog.Entry> crtPresets = new java.util.ArrayList<>();
		for (final CRTPresetCatalog.Entry entry : CRTPresetCatalog.entries()) {
			if (CRTSignRegistry.getSign(entry.signId) != null) {
				crtPresets.add(entry);
			}
		}
		final int presetWidth = (contentW - directionGap) / 2;
		final int presetHeight = 40;
		if (!crtPresets.isEmpty()) {
			addPaletteHeader(tr("gui.nanbin.crt_editor.category.crt_presets"), x0, y);
			y += PALETTE_HEADER_HEIGHT + 4;
			for (int i = 0; i < crtPresets.size(); i++) {
				final CRTPresetCatalog.Entry preset = crtPresets.get(i);
				final PaletteItem presetItem = new PaletteItem(false, crtPresetLabel(preset), CRTSignElementType.ICON,
						preset.signId, null, null);
				presetItem.x = x0 + (i % 2) * (presetWidth + directionGap);
				presetItem.y = y + (i / 2) * (presetHeight + directionGap);
				presetItem.w = presetWidth;
				presetItem.h = presetHeight;
				paletteItems.add(presetItem);
			}
			y += ((crtPresets.size() + 1) / 2) * (presetHeight + directionGap) + 6;
		}

		// ── 追加包标志：从 assets/<namespace>/crt_signs/*.json 或旧 crt_signs.json 加载、
		//    且不属于内置表/预设的额外标志，直接读注册表动态生成卡片 ──
		final java.util.Set<String> presetIds = new java.util.HashSet<>();
		for (final CRTPresetCatalog.Entry entry : CRTPresetCatalog.entries()) {
			presetIds.add(entry.signId);
		}
		final java.util.List<String> extraSignIds = new java.util.ArrayList<>();
		for (final String signId : CRTSignRegistry.getExtraSignIds()) {
			if (!presetIds.contains(signId) && CRTSignRegistry.getSign(signId) != null) {
				extraSignIds.add(signId);
			}
		}
		if (!extraSignIds.isEmpty()) {
			addPaletteHeader(tr("gui.nanbin.crt_editor.category.addon_signs"), x0, y);
			y += PALETTE_HEADER_HEIGHT + 4;
			for (int i = 0; i < extraSignIds.size(); i++) {
				final String signId = extraSignIds.get(i);
				final PaletteItem addonItem = new PaletteItem(false, CRTSignRegistry.getSignLabel(signId),
						CRTSignElementType.ICON, signId, null, null);
				addonItem.x = x0 + (i % 2) * (presetWidth + directionGap);
				addonItem.y = y + (i / 2) * (presetHeight + directionGap);
				addonItem.w = presetWidth;
				addonItem.h = presetHeight;
				paletteItems.add(addonItem);
			}
			y += ((extraSignIds.size() + 1) / 2) * (presetHeight + directionGap) + 6;
		}
		leftContentHeight = LEFT_TAB_AREA + y;

		// 每张卡片准备一个示例元素，用真实渲染烘焙缩略图（添加时深拷贝）
		for (final PaletteItem item : paletteItems) {
			assignPalettePrototype(item);
		}
	}

	/** 「预设」页签：竖向预设卡片列表（缩略信息 + 名称 + 删除按钮）。 */
	private void buildPresetTab(int contentW) {
		loadPresetsIfNeeded();
		final int cardWidth = Math.max(60, contentW - SCROLLBAR_WIDTH - 2);
		int y = 0;
		for (final CRTEditorPresets.Preset preset : presets) {
			final PresetCard card = new PresetCard(preset, leftPanelRect.x + CRTEditorWidgets.PANEL_PADDING, y, cardWidth, PRESET_CARD_HEIGHT);
			presetCards.add(card);
			y += PRESET_CARD_HEIGHT + PRESET_CARD_GAP;
		}
		leftContentHeight = LEFT_TAB_AREA + PRESET_HEADER_HEIGHT + Math.max(0, y) + CRTEditorWidgets.PANEL_PADDING;
	}

	private void loadPresetsIfNeeded() {
		if (presetsLoaded) {
			return;
		}
		presetsLoaded = true;
		try {
			presets.clear();
			presets.addAll(CRTEditorPresets.load());
		} catch (Throwable ignored) {
			presets.clear();
		}
	}

	/** 给左栏卡片准备示例元素（方向图标/服务图标/各元素类型各一份）。 */
	private static void assignPalettePrototype(PaletteItem item) {
		if (item.header || item.type == null || item.type == CRTSignElementType.SPACE) {
			return;
		}
		final CRTSignElement prototype = new CRTSignElement(item.type);
		if (item.iconId != null) {
			prototype.icon = item.iconId;
		}
		item.prototype = prototype;
	}

	private int paletteScreenY(PaletteItem item) {
		return leftBodyRect.y + LEFT_TAB_AREA + item.y - leftScroll;
	}

	private int paletteScreenX(PaletteItem item) {
		return leftPanelRect.x + CRTEditorWidgets.PANEL_PADDING + item.x;
	}

	private int presetScreenY(PresetCard card) {
		return leftBodyRect.y + LEFT_TAB_AREA + PRESET_HEADER_HEIGHT + card.y - leftScroll;
	}

	@Nullable
	private PresetCard presetCardAt(double mouseX, double mouseY) {
		if (leftPanelRect.w <= 0 || !leftBodyRect.contains(mouseX, mouseY)) {
			return null;
		}
		for (final PresetCard card : presetCards) {
			final int screenY = presetScreenY(card);
			if (mouseX >= card.x && mouseX < card.x + card.w && mouseY >= screenY && mouseY < screenY + card.h) {
				return card;
			}
		}
		return null;
	}

	// ================================================================ 预设：保存 / 插入 / 删除

	/** 打开「存为预设」命名弹窗（网页 §3.1.3 的 prompt 等价物）。 */
	private void openPresetNameModal() {
		try {
			openPresetNameModalInternal();
		} catch (Throwable e) {
			closePresetNameModal();
			com.Nanbin.InitClient.LOGGER.warn("CRT editor: failed to open preset modal", e);
		}
	}

	private void openPresetNameModalInternal() {
		final int cardWidth = Math.min(264, Math.max(170, getWidthMapped() - 40));
		final int cardHeight = 92;
		presetModalRect.set((getWidthMapped() - cardWidth) / 2, (getHeightMapped() - cardHeight) / 2, cardWidth, cardHeight);
		presetModalOpen = true;
		presetNameDraft = "";
		modalWidgets.clear();
		final CRTEditorWidgets.TextField nameField = new CRTEditorWidgets.TextField(() -> presetNameDraft, value -> presetNameDraft = value);
		nameField.setPlaceholder(tr("gui.nanbin.crt_editor.preset.name_placeholder")).setMaxLength(CRTEditorPresets.MAX_NAME_LENGTH);
		nameField.setBounds(presetModalRect.x + 12, presetModalRect.y + 30, cardWidth - 24);
		modalWidgets.add(nameField);
		final int buttonWidth = (cardWidth - 24 - 6) / 2;
		final int buttonY = presetModalRect.y + cardHeight - 26;
		final CRTEditorWidgets.Button save = new CRTEditorWidgets.Button(buttonWidth, 16,
				tr("gui.nanbin.crt_editor.preset.save_button"), CRTEditorWidgets.ButtonStyle.PRIMARY, this::confirmPresetSave);
		save.setBounds(presetModalRect.x + 12, buttonY, buttonWidth);
		modalWidgets.add(save);
		final CRTEditorWidgets.Button cancel = new CRTEditorWidgets.Button(buttonWidth, 16,
				tr("gui.nanbin.crt_editor.preset.cancel"), CRTEditorWidgets.ButtonStyle.SECONDARY, this::closePresetNameModal);
		cancel.setBounds(presetModalRect.x + 12 + buttonWidth + 6, buttonY, buttonWidth);
		modalWidgets.add(cancel);
		blurFocused();
		requestFocus(nameField);
	}

	private void closePresetNameModal() {
		presetModalOpen = false;
		presetNameDraft = "";
		modalWidgets.clear();
		// 直接清引用，避免 TextField.onBlur 再次触发保存
		clearFocus();
	}

	/** 保存当前选中元素（或当前行）为命名预设。 */
	private void confirmPresetSave() {
		try {
			confirmPresetSaveInternal();
		} catch (Throwable e) {
			// 预设只是便捷功能：任何异常都不允许把游戏带崩
			closePresetNameModal();
			toast(tr("gui.nanbin.crt_editor.preset.save_failed"), true);
			com.Nanbin.InitClient.LOGGER.warn("CRT editor: failed to save preset", e);
		}
	}

	private void confirmPresetSaveInternal() {
		final List<CRTSignElement> source = presetSourceElements();
		if (source.isEmpty()) {
			toast(tr("gui.nanbin.crt_editor.preset.no_elements"), true);
			focusPresetNameField();
			return;
		}
		final String name = presetNameDraft == null ? "" : presetNameDraft.trim();
		if (name.isEmpty()) {
			toast(tr("gui.nanbin.crt_editor.preset.name_required"), true);
			focusPresetNameField();
			return;
		}
		final CRTEditorPresets.Preset preset = CRTEditorPresets.create(name, source);
		presets.add(0, preset);
		final boolean saved = CRTEditorPresets.save(presets);
		closePresetNameModal();
		rebuildPalette();
		if (saved) {
			toast(tr("gui.nanbin.crt_editor.preset.saved", name));
		} else {
			toast(tr("gui.nanbin.crt_editor.preset.save_failed"), true);
		}
	}

	/** 保存失败/参数缺失时把焦点还给弹窗里的名称输入框（避免还要再点一次）。 */
	private void focusPresetNameField() {
		if (presetModalOpen && !modalWidgets.isEmpty()) {
			requestFocus(modalWidgets.get(0));
		}
	}

	/** 预设来源：有选中 → 选中的元素（视觉序）；否则当前行（最后一行）。 */
	private List<CRTSignElement> presetSourceElements() {
		final List<CRTSignElement> result = new ArrayList<>();
		try {
			collectPresetSourceElements(result);
		} catch (Throwable ignored) {
		}
		return result;
	}

	private void collectPresetSourceElements(List<CRTSignElement> result) {
		if (!selection.isEmpty()) {
			for (final String id : visualElementIds()) {
				if (!selection.contains(id)) {
					continue;
				}
				final CRTSignLayout.ElementRef ref = layout.findElement(id);
				if (ref != null) {
					result.add(ref.element);
				}
			}
			return;
		}
		if (!layout.rows.isEmpty()) {
			final CRTSignLayout.Row row = layout.rows.get(layout.rows.size() - 1);
			result.addAll(row.elements);
		}
		return;
	}

	/** 插入预设：追加到当前行（最后一行）末尾，复制元素并选中新元素。 */
	private void insertPreset(CRTEditorPresets.Preset preset) {
		try {
			insertPresetInternal(preset);
		} catch (Throwable e) {
			toast(tr("gui.nanbin.crt_editor.preset.empty_preset"), true);
			com.Nanbin.InitClient.LOGGER.warn("CRT editor: failed to insert preset", e);
		}
	}

	private void insertPresetInternal(CRTEditorPresets.Preset preset) {
		if (preset == null) {
			return;
		}
		final CRTSignLayout.Row target = targetRowForAdd();
		if (target == null) {
			toast(tr("gui.nanbin.crt_editor.preset.no_target_row"), true);
			return;
		}
		final List<CRTSignElement> copies = preset.materialize();
		if (copies.isEmpty()) {
			toast(tr("gui.nanbin.crt_editor.preset.empty_preset"), true);
			return;
		}
		pushHistory(null);
		target.elements.addAll(copies);
		final List<String> ids = new ArrayList<>();
		for (final CRTSignElement element : copies) {
			ids.add(element.id);
		}
		setSelection(ids, ids.get(ids.size() - 1));
		anchorId = ids.get(0);
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.preset.inserted", preset.name));
	}

	/** 删除预设（沿用编辑器的两段点击确认风格）。 */
	private void deletePreset(CRTEditorPresets.Preset preset) {
		if (preset == null) {
			return;
		}
		try {
			deletePresetInternal(preset);
		} catch (Throwable e) {
			toast(tr("gui.nanbin.crt_editor.preset.save_failed"), true);
			com.Nanbin.InitClient.LOGGER.warn("CRT editor: failed to delete preset", e);
		}
	}

	private void deletePresetInternal(CRTEditorPresets.Preset preset) {
		if (!preset.id.equals(armedDeletePresetId)) {
			armedDeletePresetId = preset.id;
			toast(tr("gui.nanbin.crt_editor.preset.delete_confirm", preset.name), true);
			return;
		}
		armedDeletePresetId = null;
		presets.remove(preset);
		final boolean saved = CRTEditorPresets.save(presets);
		rebuildPalette();
		if (saved) {
			toast(tr("gui.nanbin.crt_editor.preset.deleted", preset.name));
		} else {
			toast(tr("gui.nanbin.crt_editor.preset.save_failed"), true);
		}
	}

	private void renderLeftWidgets(CRTEditorWidgets.Ctx c) {
		if (leftPanelRect.w <= 0) {
			return;
		}
		for (final CRTEditorWidgets.Widget widget : leftWidgets) {
			widget.render(c);
		}
	}

	private void renderPresetTab(CRTEditorWidgets.Ctx c) {
		enableScissor(leftBodyRect.x, leftBodyRect.y + LEFT_TAB_AREA,
				leftBodyRect.x + leftBodyRect.w - SCROLLBAR_WIDTH - 1, leftBodyRect.y + leftBodyRect.h);
		if (presetCards.isEmpty()) {
			final int centerX = leftPanelRect.x + leftPanelRect.w / 2 - SCROLLBAR_WIDTH / 2;
			final int top = leftBodyRect.y + LEFT_TAB_AREA + PRESET_HEADER_HEIGHT + 12;
			c.scaledTextCenteredClipped(tr("gui.nanbin.crt_editor.preset.empty"), centerX, top,
					Math.max(20, leftPanelRect.w - 24), CRTEditorWidgets.FONT_SCALE_HINT, CRTEditorTheme.TEXT_DIM);
			c.scaledTextCenteredClipped(tr("gui.nanbin.crt_editor.preset.empty_sub"), centerX, top + 14,
					Math.max(20, leftPanelRect.w - 24), CRTEditorWidgets.FONT_SCALE_HINT, CRTEditorTheme.TEXT_DIM);
		}
		final int mouseX = lastMouseX;
		final int mouseY = lastMouseY;
		for (final PresetCard card : presetCards) {
			final int screenY = presetScreenY(card);
			if (screenY + card.h < leftBodyRect.y || screenY > leftBodyRect.y + leftBodyRect.h) {
				continue;
			}
			final boolean hover = mouseX >= card.x && mouseX < card.x + card.w && mouseY >= screenY && mouseY < screenY + card.h;
			final boolean deleteHover = hover && card.deleteRect(screenY).contains(mouseX, mouseY);
			final boolean armed = card.preset.id.equals(armedDeletePresetId);
			c.fillRoundBordered(card.x, screenY, card.w, card.h, CRTEditorWidgets.RADIUS,
					hover ? CRTEditorTheme.CARD_BG_HOVER : CRTEditorTheme.CARD_BG, deleteHover || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			// 名称 + 元素数量
			c.textClipped(card.preset.name, card.x + 6, screenY + 5, card.w - 28, hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT);
			c.scaledTextClipped(tr("gui.nanbin.crt_editor.preset.count", card.preset.elements.size()),
					card.x + 6, screenY + card.h - 13, card.w - 20, CRTEditorWidgets.FONT_SCALE_HINT, CRTEditorTheme.TEXT_DIM);
			// 元素类型小色块（最多 6 个，看得出预设大概是什么）
			final int chipY = screenY + 17;
			int chipX = card.x + 6;
			final int chipLimit = Math.min(6, card.preset.elements.size());
			for (int i = 0; i < chipLimit; i++) {
				final CRTSignElement element = card.preset.elements.get(i);
				c.fillRoundBordered(chipX, chipY, 13, 13, 2.0F, elementChipColor(element), CRTEditorTheme.BORDER_SOFT);
				c.textCentered(elementChipGlyph(element), chipX + 7, chipY + 2, chipColor(elementChipColor(element)));
				chipX += 15;
			}
			if (chipLimit > 0) {
				chipX += 2;
			}
			if (card.preset.elements.size() > chipLimit) {
				c.scaledTextClipped("…", chipX, chipY + 3, card.w - (chipX - card.x) - 20,
						CRTEditorWidgets.FONT_SCALE_HINT, CRTEditorTheme.TEXT_DIM);
			}
			// 删除圆钮（右上角）
			final Rect deleteRect = card.deleteRect(screenY);
			c.fillRoundBordered(deleteRect.x, deleteRect.y, deleteRect.w, deleteRect.h, 3.0F,
					armed || deleteHover ? 0xFFFEE2E2 : CRTEditorTheme.CONTROL_BG,
					armed || deleteHover ? CRTEditorTheme.DANGER : CRTEditorTheme.BORDER);
			c.textCentered("✕", deleteRect.x + deleteRect.w / 2, deleteRect.y + (deleteRect.h - CRTEditorWidgets.FONT_HEIGHT) / 2 + 1,
					armed || deleteHover ? CRTEditorTheme.DANGER : CRTEditorTheme.TEXT_DIM);
			if (deleteHover) {
				c.setTooltip(tr("gui.nanbin.crt_editor.preset.delete_tip", card.preset.name));
			} else if (hover) {
				c.setTooltip(tr("gui.nanbin.crt_editor.preset.insert_tip"));
			}
		}
		disableScissor();
	}

	/** 预设卡片里的小色块底色（元素背景色或类型代表色）。 */
	private static int elementChipColor(CRTSignElement element) {
		if (element == null) {
			return CRTEditorTheme.SURFACE_SOFT;
		}
		if (element.backgroundColor != null) {
			return element.backgroundColor;
		}
		switch (element.type) {
			case NUMBER_LINE:
				return 0xFFFCD600;
			case TEXT_LINE:
				return 0xFFF2A900;
			case EXIT:
				return 0xFFF7D917;
			case EXIT_INFO:
				return 0xFFF7D917;
			case ROUTE_INFO:
				return 0xFFF1F5F9;
			case ENTRANCE:
				return 0xFFE8F0FE;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
			case BIG_NUMBER:
				return 0xFFF1F5F9;
			case ICON:
			default:
				return 0xFFF1F5F9;
		}
	}

	private static int chipColor(int background) {
		return ((background >> 24) & 0xFF) < 0x80 ? CRTEditorTheme.TEXT : contrastColor(background);
	}

	/** 预设卡片色块里的单字提示（类型缩写）。 */
	private static String elementChipGlyph(CRTSignElement element) {
		if (element == null) {
			return "";
		}
		switch (element.type) {
			case NUMBER_LINE:
				return "线";
			case TEXT_LINE:
				return "文";
			case BIG_NUMBER:
				return "大";
			case BILINGUAL_TEXT:
				return "双";
			case SMALL_BILINGUAL_TEXT:
				return "小";
			case ENTRANCE:
				return "入";
			case EXIT:
				return "出";
			case EXIT_INFO:
				return "讯";
			case ROUTE_INFO:
				return "线";
			case ICON:
				return "图";
			case ARROW:
				return "箭";
			case SPACE:
			default:
				return "空";
		}
	}

	@Nullable
	private PaletteItem paletteItemAt(double mouseX, double mouseY) {
		if (leftPanelRect.w <= 0 || !leftBodyRect.contains(mouseX, mouseY)) {
			return null;
		}
		for (final PaletteItem item : paletteItems) {
			if (item.header) {
				continue;
			}
			final int screenX = paletteScreenX(item);
			final int screenY = paletteScreenY(item);
			if (mouseX >= screenX && mouseX < screenX + item.w && mouseY >= screenY && mouseY < screenY + item.h) {
				return item;
			}
		}
		return null;
	}

	private void renderPalette(CRTEditorWidgets.Ctx c) {
		if (leftPanelRect.w <= 0) {
			return;
		}
		if (leftTab == LEFT_TAB_PRESETS) {
			renderPresetTab(c);
			return;
		}
		enableScissor(leftBodyRect.x, leftBodyRect.y + LEFT_TAB_AREA,
				leftBodyRect.x + leftBodyRect.w - SCROLLBAR_WIDTH - 1, leftBodyRect.y + leftBodyRect.h);
		final int mouseX = lastMouseX;
		final int mouseY = lastMouseY;
		for (final PaletteItem item : paletteItems) {
			final int screenX = paletteScreenX(item);
			final int screenY = paletteScreenY(item);
			if (screenY + item.h < leftBodyRect.y + LEFT_TAB_AREA || screenY > leftBodyRect.y + leftBodyRect.h) {
				continue;
			}
			if (item.header) {
				// 分组标题：1.15×（网页 .palette-category-title 12px），后接一条 1px 延伸横线
				c.scaledTextClipped(item.label, screenX, screenY, item.w, CRTEditorWidgets.FONT_SCALE_GROUP, CRTEditorTheme.TEXT_DIM);
				c.fill(screenX + c.scaledTextWidth(item.label, CRTEditorWidgets.FONT_SCALE_GROUP) + 6, screenY + 6,
						leftPanelRect.x + leftPanelRect.w - CRTEditorWidgets.PANEL_PADDING, screenY + 7, CRTEditorTheme.BORDER_SOFT);
				continue;
			}
			final boolean hover = mouseX >= screenX && mouseX < screenX + item.w && mouseY >= screenY && mouseY < screenY + item.h;
			final boolean dragging = draggingPaletteItem == item;
			final int background = hover ? CRTEditorTheme.CARD_BG_HOVER : CRTEditorTheme.CARD_BG;
			c.fillRoundBordered(screenX, screenY, item.w, item.h, CRTEditorWidgets.RADIUS, background,
					hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			if (dragging) {
				c.fillRound(screenX + 1, screenY + 1, item.w - 2, item.h - 2, CRTEditorWidgets.RADIUS - 1, 0xFFDCE1E8);
			}
			if (item.type == CRTSignElementType.ICON && item.glyph != null && item.subLabel == null) {
				// 方向图标小卡：整格显示真实图标缩略图（烘焙失败时退回字符）
				if (!drawPaletteBakedThumb(c, item, screenX + 4, screenY + 3, item.w - 8, item.h - 6)) {
					final float scale = 1.5F;
					final int glyphWidth = c.scaledTextWidth(item.glyph, scale);
					final int glyphHeight = Math.round(CRTEditorWidgets.FONT_HEIGHT * scale);
					c.scaledText(item.glyph, screenX + (item.w - glyphWidth) / 2, screenY + (item.h - glyphHeight) / 2 + 1, scale,
							hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT);
				}
			} else {
				final int thumbX = screenX + 4;
				final int thumbY = screenY + 3;
				final int thumbW = item.w - 8;
				final int thumbH = item.h - 15;
				if (!drawPaletteBakedThumb(c, item, thumbX, thumbY, thumbW, thumbH)) {
					drawPaletteThumbnailFallback(c, item, thumbX, thumbY, thumbW, thumbH);
				}
				c.textCenteredClipped(item.label, screenX + item.w / 2, screenY + item.h - CRTEditorWidgets.LINE_HEIGHT - 1, item.w - 5,
						hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
			}
			if (hover) {
				c.setTooltip(tr("gui.nanbin.crt_editor.card.tip", item.label));
			}
		}
		disableScissor();
	}

	// ================================================================ 缩略图烘焙（真实元素渲染）

	/**
	 * 取（必要时烘焙）一个元素的缩略图贴图。
	 *
	 * <p>与整牌预览共用 {@link CRTSignLayoutPainter#bakeElementStandalone}，因此卡片里看到的就是
	 * 元素放进牌面后的真实样子。按 cacheKey + 行高缓存，烘焙失败 1 秒内不重试，避免每帧重烘。</p>
	 */
	@Nullable
	private JSSignTextureBaker.Baked elementThumbnail(@Nullable CRTSignElement element, float rowHeight, String cacheKey) {
		if (element == null || rowHeight <= 0.0F) {
			return null;
		}
		final String key = cacheKey + '|' + Math.round(rowHeight);
		ThumbnailEntry entry = thumbnailCache.get(key);
		if (entry == null) {
			entry = new ThumbnailEntry();
			thumbnailCache.put(key, entry);
		}
		if (entry.baked != null) {
			return entry.baked;
		}
		final long now = System.currentTimeMillis();
		if (now - entry.lastAttempt < THUMB_BAKE_RETRY_MS) {
			return null;
		}
		entry.lastAttempt = now;
		try {
			entry.baked = CRTSignLayoutPainter.bakeElementStandalone(element, rowHeight, data, key);
		} catch (Throwable ignored) {
			entry.baked = null;
		}
		return entry.baked;
	}

	/** 取（并缓存）一个缩略图示例元素；同一 id 复用，避免每次重建控件都 new。 */
	private CRTSignElement thumbnailPrototype(String key, CRTSignElementType type, @Nullable String iconId) {
		CRTSignElement prototype = thumbnailPrototypes.get(key);
		if (prototype == null) {
			prototype = new CRTSignElement(type);
			if (iconId != null) {
				prototype.icon = iconId;
			}
			thumbnailPrototypes.put(key, prototype);
		}
		return prototype;
	}

	/** 把烘焙结果按卡片可用区域等比缩放居中画出；没有绘制上下文/贴图时返回 false（调用方回退）。 */
	private boolean drawBakedTexture(CRTEditorWidgets.Ctx c, @Nullable JSSignTextureBaker.Baked baked, int x, int y, int w, int h) {
		if (c.graphics == null || baked == null || w <= 0 || h <= 0 || baked.width <= 0.0F || baked.height <= 0.0F) {
			return false;
		}
		final float fit = Math.min(w / baked.width, h / baked.height);
		final float drawWidth = baked.width * fit;
		final float drawHeight = baked.height * fit;
		final float drawX = x + (w - drawWidth) / 2.0F;
		final float drawY = y + (h - drawHeight) / 2.0F;
		try {
			final GuiDrawing guiDrawing = new GuiDrawing(c.graphics);
			guiDrawing.beginDrawingTexture(baked.identifier);
			guiDrawing.drawTexture(drawX, drawY, drawX + drawWidth, drawY + drawHeight, 0.0F, 0.0F, 1.0F, 1.0F);
			guiDrawing.finishDrawingTexture();
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/** 左栏卡片真实缩略图；返回 false 表示还没烘焙好（调用方画回退示意图）。 */
	private boolean drawPaletteBakedThumb(CRTEditorWidgets.Ctx c, PaletteItem item, int x, int y, int w, int h) {
		if (item.prototype == null) {
			return false;
		}
		final JSSignTextureBaker.Baked baked = elementThumbnail(item.prototype, THUMB_ROW_HEIGHT, paletteThumbCacheKey(item));
		return drawBakedTexture(c, baked, x, y, w, h);
	}

	private static String paletteThumbCacheKey(PaletteItem item) {
		return "pal:" + (item.type == null ? "?" : item.type.id) + (item.iconId == null ? "" : ":" + item.iconId);
	}

	/** 左栏卡片缩略图回退：烘焙不可用时的简化示意图（网页 .card-icon 的手绘近似）。 */
	private void drawPaletteThumbnailFallback(CRTEditorWidgets.Ctx c, PaletteItem item, int x, int y, int w, int h) {
		if (item.type == null) {
			return;
		}
		final int centerY = y + h / 2;
		switch (item.type) {
			case NUMBER_LINE:
				c.fillRound(x + 2, centerY - 6, 9, 12, 2.0F, 0xFFFCD600);
				c.textCenteredClipped("3", x + 6, centerY - 4, 9, 0xFF1F2733);
				c.textClipped(tr("gui.nanbin.crt_editor.thumb.line"), x + 14, centerY - 4, w - 14, CRTEditorTheme.TEXT_DIM);
				break;
			case TEXT_LINE:
				c.fillRound(x + 2, centerY - 6, 20, 12, 2.0F, 0xFFF2A900);
				c.textCenteredClipped(tr("gui.nanbin.crt_editor.thumb.loop"), x + 12, centerY - 4, 19, 0xFF1F2733);
				break;
			case BIG_NUMBER:
				c.scaledText("4", x + w / 2 - 4, centerY - 7, 1.4F, CRTEditorTheme.TEXT);
				break;
			case BILINGUAL_TEXT:
				c.textCenteredClipped(tr("gui.nanbin.crt_editor.thumb.station_zh"), x + w / 2, centerY - 7, w - 4, CRTEditorTheme.TEXT);
				c.textCenteredClipped("Station", x + w / 2, centerY + 2, w - 4, CRTEditorTheme.TEXT_DIM);
				break;
			case SMALL_BILINGUAL_TEXT:
				c.textCenteredClipped(tr("gui.nanbin.crt_editor.thumb.station_zh"), x + w / 2, centerY - 3, w - 4, CRTEditorTheme.TEXT);
				c.textCenteredClipped("Station", x + w / 2, centerY + 4, w - 4, CRTEditorTheme.TEXT_DIM);
				break;
			case ENTRANCE:
				c.fillRound(x + 2, centerY - 6, 13, 12, 2.0F, 0xFF2563EB);
				c.textCenteredClipped("C", x + 8, centerY - 4, 13, 0xFFFFFFFF);
				c.textClipped(tr("gui.nanbin.crt_editor.thumb.entrance"), x + 18, centerY - 4, w - 18, CRTEditorTheme.TEXT_DIM);
				break;
			case EXIT:
				c.fillRound(x + 2, centerY - 6, 12, 12, 2.0F, 0xFFF7D917);
				c.textCenteredClipped("1", x + 8, centerY - 4, 12, 0xFF1F2733);
				c.textClipped(tr("gui.nanbin.crt_editor.thumb.exit"), x + 17, centerY - 4, w - 17, CRTEditorTheme.TEXT_DIM);
				break;
						case ROUTE_INFO: {
				// 白底线路信息条：红条 1 + 蓝条 3 + 号线 / Line（真实缩略图烘焙不可用时的示意）
				c.fillRound(x + 1, centerY - 7, w - 2, 14, 2.0F, 0xFFFFFFFF);
				c.fillRound(x + 3, centerY - 6, 4, 12, 1.0F, 0xFFEE3C3E);
				c.textClipped("1", x + 8, centerY - 4, 10, 0xFF221F1F);
				c.fillRound(x + 20, centerY - 6, 4, 12, 1.0F, 0xFF0066B1);
				c.textClipped("3", x + 25, centerY - 4, 10, 0xFF221F1F);
				c.textClipped(tr("gui.nanbin.crt_editor.thumb.route_info"), x + 38, centerY - 4, Math.max(8, w - 40), 0xFF221F1F);
				break;
			}
case EXIT_INFO:
				// 黄底信息条：↑ + 1/2 + 出口 / Exit（真实缩略图烘焙不可用时的示意）
				c.fillRound(x + 1, centerY - 7, w - 2, 14, 2.0F, 0xFFF7D917);
				c.textClipped("↑", x + 3, centerY - 4, 8, 0xFF1F2733);
				c.textClipped("1/2", x + 12, centerY - 5, 22, 0xFF1F2733);
				c.textClipped(tr("gui.nanbin.crt_editor.thumb.exit_info"), x + 32, centerY - 4, Math.max(8, w - 34), 0xFF1F2733);
				break;
			case ICON:
				c.fillRound(x + 2, centerY - 6, 12, 12, 2.0F, CRTEditorTheme.SURFACE_SOFT);
				c.textClipped(tr("gui.nanbin.crt_editor.thumb.icon"), x + 17, centerY - 4, w - 17, CRTEditorTheme.TEXT_DIM);
				break;
			case SPACE:
			default:
				c.dashedRect(x + 3, y + 1, x + w - 3, y + h - 1, 0xFFB6C0CC);
				break;
		}
	}

	// ================================================================ 选择

	private List<CRTSignElement> selectedElements() {
		final List<CRTSignElement> elements = new ArrayList<>();
		for (final String id : selection) {
			final CRTSignLayout.ElementRef ref = layout.findElement(id);
			if (ref != null) {
				elements.add(ref.element);
			}
		}
		return elements;
	}

	@Nullable
	private CRTSignElement primaryElement() {
		return primaryId == null ? null : (layout.findElement(primaryId) == null ? null : layout.findElement(primaryId).element);
	}

	@Nullable
	private CRTSignLayout.ElementRef primaryRef() {
		return primaryId == null ? null : layout.findElement(primaryId);
	}

	private boolean isSelected(String id) {
		return selection.contains(id);
	}

	private void setSelection(List<String> ids, @Nullable String primary) {
		selection.clear();
		selection.addAll(ids);
		primaryId = primary != null && selection.contains(primary) ? primary : (selection.isEmpty() ? null : selection.get(selection.size() - 1));
		normalizeSelection();
		widgetsDirty = true;
		layoutCanvas();
	}

	private void selectSingle(String id) {
		selection.clear();
		selection.add(id);
		primaryId = id;
		anchorId = id;
		widgetsDirty = true;
		layoutCanvas();
	}

	private void toggleSelection(String id) {
		if (selection.remove(id)) {
			primaryId = selection.isEmpty() ? null : selection.get(selection.size() - 1);
		} else {
			selection.add(id);
			primaryId = id;
			anchorId = id;
		}
		widgetsDirty = true;
		layoutCanvas();
	}

	private void rangeSelect(String id) {
		final List<String> order = visualElementIds();
		int targetIndex = order.indexOf(id);
		if (targetIndex < 0) {
			selectSingle(id);
			return;
		}
		if (anchorId == null || !order.contains(anchorId)) {
			anchorId = id;
			selectSingle(id);
			return;
		}
		final int anchorIndex = order.indexOf(anchorId);
		final int from = Math.min(anchorIndex, targetIndex);
		final int to = Math.max(anchorIndex, targetIndex);
		final List<String> ids = new ArrayList<>(selection);
		for (int i = from; i <= to; i++) {
			if (!ids.contains(order.get(i))) {
				ids.add(order.get(i));
			}
		}
		setSelection(ids, id);
	}

	private void clearSelection() {
		selection.clear();
		primaryId = null;
		anchorId = null;
		widgetsDirty = true;
		layoutCanvas();
	}

	private void selectAll() {
		final List<String> order = visualElementIds();
		setSelection(order, order.isEmpty() ? null : order.get(order.size() - 1));
		anchorId = order.isEmpty() ? null : order.get(0);
	}

	/** 规范化选择：剔除已不存在的 id；主选中无效时回退。 */
	private void normalizeSelection() {
		selection.removeIf(id -> layout.findElement(id) == null);
		if (primaryId != null && !selection.contains(primaryId)) {
			primaryId = selection.isEmpty() ? null : selection.get(selection.size() - 1);
		}
		if (anchorId != null && layout.findElement(anchorId) == null) {
			anchorId = null;
		}
	}

	/** 视觉序（行顺序 + 行内 x 升序）；画布几何未就绪时退回数组序。 */
	private List<String> visualElementIds() {
		final List<String> ids = new ArrayList<>();
		if (!canvasElements.isEmpty()) {
			final List<CanvasElement> sorted = new ArrayList<>(canvasElements);
			Collections.sort(sorted, new Comparator<CanvasElement>() {
				@Override
				public int compare(CanvasElement a, CanvasElement b) {
					if (a.rowIndex != b.rowIndex) {
						return Integer.compare(a.rowIndex, b.rowIndex);
					}
					return Float.compare(a.x, b.x);
				}
			});
			for (final CanvasElement element : sorted) {
				ids.add(element.element.id);
			}
			return ids;
		}
		for (final CRTSignLayout.Row row : layout.rows) {
			for (final CRTSignElement element : row.elements) {
				ids.add(element.id);
			}
		}
		return ids;
	}

	private RightMode rightMode() {
		final int count = selection.size();
		if (count >= 2) {
			return RightMode.BATCH;
		}
		if (count == 1 && primaryElement() != null) {
			return RightMode.ELEMENT;
		}
		return RightMode.SETTINGS;
	}

	// ================================================================ 历史

	private void pushHistory(@Nullable String key) {
		final long now = System.currentTimeMillis();
		if (key == null) {
			history.push(layout, null);
			historyKey = null;
			historyTime = now;
			return;
		}
		if (!key.equals(historyKey) || now - historyTime > HISTORY_GROUP_MS) {
			history.push(layout, key);
			historyKey = key;
			historyTime = now;
		}
	}

	private void breakHistoryGroup() {
		historyKey = null;
	}

	private void undo() {
		blurFocused();
		breakHistoryGroup();
		final CRTSignLayout previous = history.undo(layout);
		if (previous == null) {
			toast(tr("gui.nanbin.crt_editor.undo.empty"), true);
			return;
		}
		layout = previous;
		normalizeSelection();
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.undo.done"));
	}

	private void redo() {
		blurFocused();
		breakHistoryGroup();
		final CRTSignLayout next = history.redo(layout);
		if (next == null) {
			toast(tr("gui.nanbin.crt_editor.redo.empty"), true);
			return;
		}
		layout = next;
		normalizeSelection();
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.redo.done"));
	}

	/** 一次离散改动（增删/移动/绑定等）：先快照再执行。 */
	private void editDiscrete(@Nullable String label, Runnable mutation) {
		pushHistory(null);
		mutation.run();
		normalizeSelection();
		markEdited(true);
		saveNow();
	}

	/** 连续改动（文本输入/滑条）：同一 key 在合并窗口内只记一次快照。 */
	private void editContinuous(String key, Runnable mutation) {
		pushHistory(key);
		mutation.run();
		markEdited(false);
		// 自动保存：同一个节流窗口内最多一次，避免拖动/输入时每帧写方块数据
		if (System.currentTimeMillis() - lastSaveTime >= SAVE_THROTTLE_MS) {
			saveNow();
		}
	}

	// ================================================================ 剪贴板

	private void copySelection(boolean cut) {
		if (selection.isEmpty()) {
			toast(tr("gui.nanbin.crt_editor.clipboard.no_selection"), true);
			return;
		}
		final String json = CRTSignClipboardData.copy(layout, selection);
		if (json == null) {
			toast(tr("gui.nanbin.crt_editor.clipboard.copy_failed"), true);
			return;
		}
		clipboardJson = json;
		clipboardCount = selection.size();
		if (cut) {
			final CRTSignLayout.ElementRef first = primaryRef();
			if (first != null) {
				cutRowId = first.row.id;
				cutGap = first.index;
			}
			deleteSelectionInternal(true);
			toast(tr("gui.nanbin.crt_editor.clipboard.cut_done", clipboardCount));
		} else {
			toast(tr("gui.nanbin.crt_editor.clipboard.copy_done", clipboardCount));
		}
		widgetsDirty = true;
	}

	private void pasteClipboard() {
		if (clipboardJson == null) {
			toast(tr("gui.nanbin.crt_editor.clipboard.empty"), true);
			return;
		}
		String rowId = null;
		int gap = 0;
		final CRTSignLayout.ElementRef primary = primaryRef();
		if (primary != null) {
			rowId = primary.row.id;
			gap = primary.index + 1;
		} else if (cutRowId != null && layout.indexOfRow(cutRowId) >= 0) {
			rowId = cutRowId;
			gap = cutGap;
		}
		if (rowId == null && !layout.rows.isEmpty()) {
			final CRTSignLayout.Row lastRow = layout.rows.get(layout.rows.size() - 1);
			rowId = lastRow.id;
			gap = lastRow.elements.size();
		}
		if (rowId == null) {
			toast(tr("gui.nanbin.crt_editor.clipboard.paste_failed"), true);
			return;
		}
		final Set<String> before = allElementIds();
		pushHistory(null);
		if (!CRTSignClipboardData.paste(layout, clipboardJson, rowId, gap)) {
			toast(tr("gui.nanbin.crt_editor.clipboard.paste_failed"), true);
			return;
		}
		final List<String> added = new ArrayList<>();
		forEachElement(element -> {
			if (!before.contains(element.id)) {
				added.add(element.id);
			}
		});
		setSelection(added, added.isEmpty() ? null : added.get(added.size() - 1));
		cutRowId = null;
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.clipboard.paste_done", added.size()));
	}

	private Set<String> allElementIds() {
		final Set<String> ids = new HashSet<>();
		forEachElement(element -> ids.add(element.id));
		return ids;
	}

	private void forEachElement(Consumer<CRTSignElement> consumer) {
		for (final CRTSignLayout.Row row : layout.rows) {
			for (final CRTSignElement element : row.elements) {
				consumer.accept(element);
			}
		}
	}

	// ================================================================ 元素增删移动

	private void addElementFromPalette(PaletteItem item) {
		if (item.type == null) {
			return;
		}
		if (layout.hasJsStyle()) {
			toast(tr("gui.nanbin.crt_editor.js.locked_edit"), true);
			return;
		}
		final CRTSignElement element = createPaletteElement(item);
		final CRTSignLayout.Row targetRow = targetRowForAdd();
		pushHistory(null);
		if (targetRow != null) {
			targetRow.elements.add(element);
		}
		setSelection(Collections.singletonList(element.id), element.id);
		anchorId = element.id;
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.toast.added", elementTypeName(item.type)));
	}

	/** 视图层：从卡片示例元素深拷贝新元素（保留类型默认值）。 */
	private static CRTSignElement createPaletteElement(PaletteItem item) {
		if (item.prototype != null) {
			return item.prototype.copy();
		}
		final CRTSignElement element = new CRTSignElement(item.type);
		if (item.iconId != null) {
			element.icon = item.iconId;
		}
		return element;
	}

	@Nullable
	private CRTSignLayout.Row targetRowForAdd() {
		final CRTSignLayout.ElementRef primary = primaryRef();
		if (primary != null) {
			return primary.row;
		}
		return layout.rows.isEmpty() ? null : layout.rows.get(layout.rows.size() - 1);
	}

	/** 拖放添加：在 targetRow 的视觉间隙插入新元素。 */
	private void addElementAt(PaletteItem item, CRTSignLayout.Row row, int visualGap) {
		if (item.type == null) {
			return;
		}
		final CRTSignElement element = createPaletteElement(item);
		pushHistory(null);
		final int lane = laneOf(element);
		final int arrayIndex = visualGapToArrayIndex(row, visualGap, lane, Collections.<String>emptyList());
		row.elements.add(CRTEditorWidgets.clamp(arrayIndex, 0, row.elements.size()), element);
		setSelection(Collections.singletonList(element.id), element.id);
		anchorId = element.id;
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.toast.added", elementTypeName(item.type)));
	}

	private void deleteSelection() {
		if (selection.isEmpty()) {
			return;
		}
		final int count = selection.size();
		deleteSelectionInternal(false);
		toast(tr("gui.nanbin.crt_editor.toast.deleted", count));
		widgetsDirty = true;
	}

	private void deleteSelectionInternal(boolean duringCut) {
		if (selection.isEmpty()) {
			return;
		}
		final int count = selection.size();
		pushHistory(null);
		for (final CRTSignLayout.Row row : layout.rows) {
			row.elements.removeIf(element -> selection.contains(element.id));
		}
		selection.clear();
		primaryId = null;
		anchorId = null;
		markEdited(true);
		saveNow();
		if (!duringCut) {
			toast(tr("gui.nanbin.crt_editor.toast.deleted", count));
		}
	}

	private void deleteSingleElement(String elementId) {
		final CRTSignLayout.ElementRef ref = layout.findElement(elementId);
		if (ref == null) {
			return;
		}
		if (!elementId.equals(armedDeleteElementId)) {
			armedDeleteElementId = elementId;
			toast(tr("gui.nanbin.crt_editor.confirm.delete_element"), true);
			return;
		}
		armedDeleteElementId = null;
		pushHistory(null);
		ref.row.elements.remove(ref.element);
		selection.remove(elementId);
		if (elementId.equals(primaryId)) {
			primaryId = selection.isEmpty() ? null : selection.get(selection.size() - 1);
		}
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.toast.element_deleted"));
	}

	private boolean canMoveSelectionBy(int direction) {
		for (final CRTSignLayout.Row row : layout.rows) {
			for (final CRTSignElement.ElementAlign lane : CRTSignElement.ElementAlign.values()) {
				final List<Integer> order = laneIndicesInVisualOrder(row, lane);
				for (int i = 1; i < order.size(); i++) {
					final int before = direction > 0 ? order.get(i - 1) : order.get(i);
					final int after = direction > 0 ? order.get(i) : order.get(i - 1);
					if (selection.contains(row.elements.get(before).id) && !selection.contains(row.elements.get(after).id)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void moveSelectionBy(int direction) {
		if (selection.isEmpty() || !canMoveSelectionBy(direction)) {
			toast(tr("gui.nanbin.crt_editor.toast.cannot_move"), true);
			return;
		}
		pushHistory(null);
		for (final CRTSignLayout.Row row : layout.rows) {
			for (final CRTSignElement.ElementAlign lane : CRTSignElement.ElementAlign.values()) {
				final List<Integer> order = laneIndicesInVisualOrder(row, lane);
				if (direction > 0) {
					for (int i = 1; i < order.size(); i++) {
						final int a = order.get(i - 1);
						final int b = order.get(i);
						if (selection.contains(row.elements.get(a).id) && !selection.contains(row.elements.get(b).id)) {
							Collections.swap(row.elements, a, b);
						}
					}
				} else {
					for (int i = order.size() - 2; i >= 0; i--) {
						final int a = order.get(i);
						final int b = order.get(i + 1);
						if (selection.contains(row.elements.get(a).id) && !selection.contains(row.elements.get(b).id)) {
							Collections.swap(row.elements, a, b);
						}
					}
				}
			}
		}
		markEdited(false);
		saveNow();
	}

	/** 方向键上下：把选中元素移到相邻行。 */
	private void moveSelectionToRow(int direction) {
		if (selection.isEmpty() || !canMoveSelectionToRow(direction)) {
			return;
		}
		pushHistory(null);
		if (direction < 0) {
			for (int rowIndex = 1; rowIndex < layout.rows.size(); rowIndex++) {
				final CRTSignLayout.Row row = layout.rows.get(rowIndex);
				final List<CRTSignElement> moved = new ArrayList<>();
				row.elements.removeIf(element -> {
					if (selection.contains(element.id)) {
						moved.add(element);
						return true;
					}
					return false;
				});
				if (!moved.isEmpty()) {
					layout.rows.get(rowIndex - 1).elements.addAll(moved);
				}
			}
		} else {
			for (int rowIndex = layout.rows.size() - 2; rowIndex >= 0; rowIndex--) {
				final CRTSignLayout.Row row = layout.rows.get(rowIndex);
				final List<CRTSignElement> moved = new ArrayList<>();
				row.elements.removeIf(element -> {
					if (selection.contains(element.id)) {
						moved.add(element);
						return true;
					}
					return false;
				});
				if (!moved.isEmpty()) {
					layout.rows.get(rowIndex + 1).elements.addAll(moved);
				}
			}
		}
		markEdited(false);
		saveNow();
	}

	private boolean canMoveSelectionToRow(int direction) {
		if (direction < 0) {
			for (int rowIndex = 1; rowIndex < layout.rows.size(); rowIndex++) {
				for (final CRTSignElement element : layout.rows.get(rowIndex).elements) {
					if (selection.contains(element.id)) {
						return true;
					}
				}
			}
		} else {
			for (int rowIndex = 0; rowIndex < layout.rows.size() - 1; rowIndex++) {
				for (final CRTSignElement element : layout.rows.get(rowIndex).elements) {
					if (selection.contains(element.id)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** 把选中元素移动到目标行/视觉间隙（多选保持组内相对顺序）。 */
	private void moveSelectionTo(@Nullable String targetRowId, int visualGap) {
		if (selection.isEmpty() || targetRowId == null) {
			return;
		}
		final int targetIndex = layout.indexOfRow(targetRowId);
		if (targetIndex < 0) {
			return;
		}
		final CRTSignLayout.Row targetRow = layout.rows.get(targetIndex);
		final CRTSignElement first = selectedElements().isEmpty() ? null : selectedElements().get(0);
		final int lane = first == null ? 0 : laneOf(first);
		pushHistory(null);
		final List<CRTSignElement> moved = new ArrayList<>();
		for (final CRTSignLayout.Row row : layout.rows) {
			row.elements.removeIf(element -> {
				if (selection.contains(element.id)) {
					moved.add(element);
					return true;
				}
				return false;
			});
		}
		final int arrayIndex = visualGapToArrayIndex(targetRow, visualGap, lane, selection);
		targetRow.elements.addAll(CRTEditorWidgets.clamp(arrayIndex, 0, targetRow.elements.size()), moved);
		markEdited(true);
		saveNow();
	}

	/** 视觉间隙 -> 数组下标（贴右通道数组序与视觉序相反）。 */
	private int visualGapToArrayIndex(CRTSignLayout.Row row, int visualGap, int lane, List<String> ignoredIds) {
		final int remaining = row.elements.size() - countIgnoredInRow(row, ignoredIds);
		final int gap = CRTEditorWidgets.clamp(visualGap, 0, remaining);
		return lane == LANE_RIGHT ? remaining - gap : gap;
	}

	private int countIgnoredInRow(CRTSignLayout.Row row, List<String> ignoredIds) {
		if (ignoredIds.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (final CRTSignElement element : row.elements) {
			if (ignoredIds.contains(element.id)) {
				count++;
			}
		}
		return count;
	}

	private static final int LANE_LEFT = 0;
	private static final int LANE_CENTER = 1;
	private static final int LANE_RIGHT = 2;

	private int laneOf(CRTSignElement element) {
		if (layout.widthMode != CRTSignLayout.WidthMode.FIXED) {
			return LANE_LEFT;
		}
		switch (element.elementAlign) {
			case CENTER:
				return LANE_CENTER;
			case RIGHT:
				return LANE_RIGHT;
			case LEFT:
			default:
				return LANE_LEFT;
		}
	}

	private List<Integer> laneIndicesInVisualOrder(CRTSignLayout.Row row, CRTSignElement.ElementAlign lane) {
		final List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < row.elements.size(); i++) {
			if (row.elements.get(i).elementAlign == lane) {
				indices.add(i);
			}
		}
		if (lane == CRTSignElement.ElementAlign.RIGHT) {
			Collections.reverse(indices);
		}
		return indices;
	}

	// ================================================================ 行操作

	private void addRowAction() {
		editDiscrete(null, () -> layout.addRow(layout.rows.size()));
		toast(tr("gui.nanbin.crt_editor.toast.row_added"));
	}

	private void moveRowBy(int rowIndex, int delta) {
		final int target = rowIndex + delta;
		if (rowIndex < 0 || rowIndex >= layout.rows.size() || target < 0 || target >= layout.rows.size()) {
			return;
		}
		editDiscrete(null, () -> layout.moveRow(rowIndex, target));
	}

	private void deleteRowAction(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= layout.rows.size()) {
			return;
		}
		if (layout.rows.size() <= 1) {
			toast(tr("gui.nanbin.crt_editor.row.last"), true);
			return;
		}
		final CRTSignLayout.Row row = layout.rows.get(rowIndex);
		if (!row.elements.isEmpty() && !row.id.equals(armedDeleteRowId)) {
			armedDeleteRowId = row.id;
			toast(tr("gui.nanbin.crt_editor.confirm.delete_row", row.elements.size()), true);
			return;
		}
		armedDeleteRowId = null;
		editDiscrete(null, () -> layout.deleteRow(row.id));
		toast(tr("gui.nanbin.crt_editor.toast.row_deleted"));
	}

	private void selectRowForSettings(int rowIndex) {
		settingsRowIndex = CRTEditorWidgets.clamp(rowIndex, 0, Math.max(0, layout.rows.size() - 1));
		clearSelection();
		widgetsDirty = true;
	}

	/** 预设卡片显示名：设施名 + 变体后缀（语言键见 lang/*.json）。 */
	private static String crtPresetLabel(CRTPresetCatalog.Entry entry) {
		final StringBuilder label = new StringBuilder(tr("gui.nanbin.crt_editor.crt_sign." + entry.baseId));
		if (entry.withText) {
			label.append(tr("gui.nanbin.crt_editor.crt_sign.suffix.text"));
		}
		if (entry.flipped) {
			label.append(tr("gui.nanbin.crt_editor.crt_sign.suffix.flipped"));
		}
		if (entry.gray) {
			label.append(tr("gui.nanbin.crt_editor.crt_sign.suffix.gray"));
		}
		return label.toString();
	}

	/** 复制整块牌面（可粘贴到其它 CRT 指示牌，跨牌共享）。 */
	private void copyBoardAction() {
		final String json = layout.toJson();
		if (json == null || json.isEmpty()) {
			toast(tr("gui.nanbin.crt_editor.board.copy_failed"), true);
			return;
		}
		boardClipboardJson = json;
		widgetsDirty = true;
		toast(tr("gui.nanbin.crt_editor.board.copied"));
	}

	/** 粘贴整块牌面：整体替换当前布局（可撤销，立即保存）。 */
	private void pasteBoardAction() {
		if (boardClipboardJson == null) {
			toast(tr("gui.nanbin.crt_editor.board.paste_empty"), true);
			return;
		}
		final CRTSignLayout pasted = CRTSignLayout.fromJson(boardClipboardJson);
		if (pasted == null) {
			toast(tr("gui.nanbin.crt_editor.board.invalid"), true);
			return;
		}
		clearSelection();
		pushHistory(null);
		layout = pasted;
		breakHistoryGroup();
		normalizeSelection();
		markEdited(true);
		saveNow();
		widgetsDirty = true;
		toast(tr("gui.nanbin.crt_editor.board.pasted"));
	}

	private void clearSignAction() {
		if (!"clear".equals(armedClear)) {
			armedClear = "clear";
			toast(tr("gui.nanbin.crt_editor.confirm.clear_sign"), true);
			return;
		}
		armedClear = null;
		editDiscrete(null, layout::clearElements);
		clearSelection();
		toast(tr("gui.nanbin.crt_editor.toast.sign_cleared"));
	}

	// ================================================================ MTR 数据绑定

	private void setBinding(CRTSignElement element, CRTSignElement.DataBinding binding) {
		if (element.binding == binding) {
			return;
		}
		editDiscrete(null, () -> {
			element.binding = binding;
			element.bindingIds.clear();
		});
		toast(tr("gui.nanbin.crt_editor.binding.changed", bindingName(binding)));
	}

	@Nullable
	private ObjectImmutableList<DashboardListItem> listForBinding(CRTSignElement.DataBinding binding) {
		if (host == null) {
			return null;
		}
		switch (binding) {
			case ROUTE_NAME:
			case ROUTE_NUMBER:
			case ROUTE_LINES:
				return host.getRoutesForList();
			case STATION:
				return host.getStationsForList();
			case EXIT:
				return host.getExitsForList();
			case PLATFORM:
			case NEXT_STATION:
			case PREV_STATION:
				return host.getPlatformsForList();
			default:
				return null;
		}
	}

	private void openBindingSelector(CRTSignElement element) {
		if (element.binding == CRTSignElement.DataBinding.NONE) {
			toast(tr("gui.nanbin.crt_editor.binding.choose_kind_first"), true);
			return;
		}
		final ObjectImmutableList<DashboardListItem> list = listForBinding(element.binding);
		if (list == null || list.isEmpty()) {
			toast(tr("gui.nanbin.crt_editor.binding.no_data"), true);
			return;
		}
		pendingBindingElementId = element.id;
		pendingBindingIds = new LongAVLTreeSet(element.bindingIds);
		MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(list, pendingBindingIds, false, false, this)));
	}

	/**
	 * 出口信息条（EXIT_INFO）的出口多选：MTR 选择屏以「非单选」（{@code isSingleSelect = false}）打开，
	 * 返回时把选中的出口 id 追加回 {@code bindingIds}（{@link LongAVLTreeSet} 自动去重）；
	 * 旁边的「清除绑定」按钮负责清空。
	 *
	 * <p>出口信息条的编号/目的地都来自 MTR 出口数据，所以选择前先把绑定类型切到出口
	 * （切换时清掉旧 id，避免车站/线路 id 混进出口列表）。</p>
	 */
	private void openExitInfoExitSelector(CRTSignElement element) {
		if (element.binding != CRTSignElement.DataBinding.EXIT) {
			editDiscrete(null, () -> {
				element.binding = CRTSignElement.DataBinding.EXIT;
				element.bindingIds.clear();
			});
		}
		openBindingSelector(element);
	}

	/** 从 MTR 选择屏返回：把选中的 id 写回元素。 */
	private void applyPendingBinding() {
		if (pendingBindingElementId == null) {
			return;
		}
		final String elementId = pendingBindingElementId;
		final LongAVLTreeSet ids = pendingBindingIds;
		pendingBindingElementId = null;
		pendingBindingIds = null;
		final CRTSignLayout.ElementRef ref = layout.findElement(elementId);
		if (ref == null) {
			return;
		}
		editDiscrete(null, () -> {
			ref.element.bindingIds.clear();
			if (ids != null) {
				ref.element.bindingIds.addAll(ids);
			}
		});
		toast(tr("gui.nanbin.crt_editor.binding.saved", ref.element.bindingIds.size()));
	}

	/** 绑定后解析出的当前值（属性面板显示 / 线框画布预览共用）。 */
	private String describeBindingValue(CRTSignElement element) {
		if (element.binding == CRTSignElement.DataBinding.NONE) {
			return "";
		}
		final List<Long> ids = element.bindingIds;
		switch (element.binding) {
			case ROUTE_NAME: {
				if (ids.isEmpty()) {
					return "";
				}
				final String name = data.getRouteName(ids.get(0));
				if (name != null) {
					return name;
				}
				return nameFromHost(host == null ? null : host.getRoutesForList(), ids.get(0));
			}
			case ROUTE_NUMBER: {
				if (ids.isEmpty()) {
					return "";
				}
				final String number = data.getRouteNumber(ids.get(0));
				return number != null ? number : nameFromHost(host == null ? null : host.getRoutesForList(), ids.get(0));
			}
			case ROUTE_LINES: {
				final List<String> names = new ArrayList<>();
				for (final long id : ids) {
					final String number = data.getRouteNumber(id);
					final String name = data.getRouteName(id);
					final String value = number != null ? number : (name != null ? name : nameFromHost(host == null ? null : host.getRoutesForList(), id));
					if (value != null && !value.isEmpty()) {
						names.add(value);
					}
				}
				return join(names);
			}
			case STATION: {
				final List<String> stationNames = data.getStationNames(ids);
				if (stationNames != null && !stationNames.isEmpty()) {
					return join(stationNames);
				}
				final List<String> fallback = new ArrayList<>();
				for (final long id : ids) {
					final String name = nameFromHost(host == null ? null : host.getStationsForList(), id);
					if (name != null) {
						fallback.add(name);
					}
				}
				return join(fallback);
			}
			case EXIT: {
				final List<String> codes = new ArrayList<>();
				for (final long id : ids) {
					final String code = data.getExitCode(id);
					if (code != null) {
						codes.add(code);
					} else {
						final String name = nameFromHost(host == null ? null : host.getExitsForList(), id);
						if (name != null) {
							codes.add(name);
						}
					}
				}
				return join(codes);
			}
			case PLATFORM: {
				final List<String> names = new ArrayList<>();
				for (final long id : ids) {
					final String name = data.getPlatformName(id);
					if (name != null) {
						names.add(name);
					} else {
						final String fallback = nameFromHost(host == null ? null : host.getPlatformsForList(), id);
						if (fallback != null) {
							names.add(fallback);
						}
					}
				}
				return join(names);
			}
			case NEXT_STATION: {
				if (ids.isEmpty()) {
					return "";
				}
				final String name = data.getNextStation(ids.get(0));
				return name == null ? "" : name;
			}
			case PREV_STATION: {
				if (ids.isEmpty()) {
					return "";
				}
				final String name = data.getPrevStation(ids.get(0));
				return name == null ? "" : name;
			}
			default:
				return "";
		}
	}

	@Nullable
	private static String nameFromHost(@Nullable ObjectImmutableList<DashboardListItem> list, long id) {
		if (list == null) {
			return null;
		}
		for (final DashboardListItem item : list) {
			if (item.id == id) {
				final String name = item.getName(false);
				return name == null || name.isEmpty() ? null : name;
			}
		}
		return null;
	}

	private static String join(List<String> values) {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.size() && i < 4; i++) {
			if (i > 0) {
				builder.append(" / ");
			}
			builder.append(values.get(i));
		}
		if (values.size() > 4) {
			builder.append(" ...");
		}
		return builder.toString();
	}

	// ================================================================ 文案

	private static String elementTypeName(CRTSignElementType type) {
		if (type == null) {
			return "";
		}
		switch (type) {
			case ARROW:
				return tr("gui.nanbin.crt_editor.element.arrow");
			case BILINGUAL_TEXT:
				return tr("gui.nanbin.crt_editor.element.bilingual");
			case SMALL_BILINGUAL_TEXT:
				return tr("gui.nanbin.crt_editor.element.small_bilingual");
			case BIG_NUMBER:
				return tr("gui.nanbin.crt_editor.element.big_number");
			case NUMBER_LINE:
				return tr("gui.nanbin.crt_editor.element.number_line");
			case TEXT_LINE:
				return tr("gui.nanbin.crt_editor.element.text_line");
			case ENTRANCE:
				return tr("gui.nanbin.crt_editor.element.entrance");
			case EXIT:
				return tr("gui.nanbin.crt_editor.element.exit");
			case EXIT_INFO:
				return tr("gui.nanbin.crt_editor.element.exit_info");
			case ROUTE_INFO:
				return tr("gui.nanbin.crt_editor.element.route_info");
			case SPACE:
				return tr("gui.nanbin.crt_editor.element.space");
			case ICON:
				return tr("gui.nanbin.crt_editor.element.icon");
			default:
				return "";
		}
	}

	private static String bindingName(CRTSignElement.DataBinding binding) {
		switch (binding) {
			case ROUTE_NAME:
				return tr("gui.nanbin.crt_editor.binding.route_name");
			case ROUTE_NUMBER:
				return tr("gui.nanbin.crt_editor.binding.route_number");
			case ROUTE_LINES:
				return tr("gui.nanbin.crt_editor.binding.route_lines");
			case STATION:
				return tr("gui.nanbin.crt_editor.binding.station");
			case EXIT:
				return tr("gui.nanbin.crt_editor.binding.exit");
			case PLATFORM:
				return tr("gui.nanbin.crt_editor.binding.platform");
			case NEXT_STATION:
				return tr("gui.nanbin.crt_editor.binding.next_station");
			case PREV_STATION:
				return tr("gui.nanbin.crt_editor.binding.prev_station");
			case NONE:
			default:
				return tr("gui.nanbin.crt_editor.binding.none");
		}
	}

	private static String directionName(String iconId) {
		if (iconId == null) {
			return "";
		}
		switch (iconId) {
			case "arrow_up":
				return tr("gui.nanbin.crt_editor.dir.up");
			case "arrow_down":
				return tr("gui.nanbin.crt_editor.dir.down");
			case "arrow_left":
				return tr("gui.nanbin.crt_editor.dir.left");
			case "arrow_right":
				return tr("gui.nanbin.crt_editor.dir.right");
			case "arrow_left_up":
				return tr("gui.nanbin.crt_editor.dir.left_up");
			case "arrow_right_up":
				return tr("gui.nanbin.crt_editor.dir.right_up");
			case "arrow_right_down":
				return tr("gui.nanbin.crt_editor.dir.right_down");
			case "arrow_left_down":
				return tr("gui.nanbin.crt_editor.dir.left_down");
			case "arrow_ahead_left":
				return tr("gui.nanbin.crt_editor.dir.ahead_left");
			case "arrow_ahead_right":
				return tr("gui.nanbin.crt_editor.dir.ahead_right");
			case "arrow_back_left":
				return tr("gui.nanbin.crt_editor.dir.back_left");
			case "arrow_back_right":
				return tr("gui.nanbin.crt_editor.dir.back_right");
			default:
				return iconId;
		}
	}

	/** 元素内容在画布线框里的显示文本（绑定元素显示解析后的 MTR 数据）。 */
	private String elementDisplayText(CRTSignElement element) {
		if (element.binding != CRTSignElement.DataBinding.NONE) {
			final String resolved = describeBindingValue(element);
			return resolved == null || resolved.isEmpty() ? tr("gui.nanbin.crt_editor.binding.unresolved") : resolved;
		}
		switch (element.type) {
			case ARROW:
				return directionGlyphFor(element.arrowDirection);
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				return element.textZh == null || element.textZh.isEmpty() ? element.textEn : element.textZh;
			case BIG_NUMBER:
				return element.text;
			case NUMBER_LINE: {
				final StringBuilder builder = new StringBuilder();
				for (final CRTSignElement.LineItem line : element.lines) {
					builder.append(line.number);
					builder.append(' ');
				}
				return builder.toString().trim();
			}
			case TEXT_LINE:
				return element.text;
			case ENTRANCE:
			case EXIT:
				return element.code;
			case EXIT_INFO:
				return element.code;
			case ICON:
				return iconGlyphFor(element.icon);
			case SPACE:
			default:
				return "";
		}
	}

	private static String directionGlyphFor(CRTSignElement.ArrowDirection direction) {
		switch (direction) {
			case UP:
				return "↑";
			case DOWN:
				return "↓";
			case RIGHT:
				return "→";
			case LEFT_UP:
				return "↖";
			case RIGHT_UP:
				return "↗";
			case RIGHT_DOWN:
				return "↘";
			case LEFT_DOWN:
				return "↙";
			case LEFT:
			default:
				return "←";
		}
	}

	private static String iconGlyphFor(String iconId) {
		switch (iconId) {
			case "elevator":
				return "↕";
			case "stairs":
				return "梯";
			case "restroom":
			case "accessible_restroom":
				return "卫";
			case "tickets":
				return "票";
			case "train":
				return "车";
			case "metro":
			case "monorail":
				return "轨";
			case "exit":
			case "exit_left":
			case "exit_right":
				return "出";
			case "waiting":
				return "候";
			case "nursing":
				return "婴";
			case "china_railway":
				return "铁";
			case "no_entry":
				return "⊘";
			default:
				if (iconId != null && iconId.startsWith("arrow_")) {
					for (int i = 0; i < DIRECTION_ICONS.length; i++) {
						if (DIRECTION_ICONS[i].equals(iconId)) {
							return DIRECTION_GLYPHS[i];
						}
					}
				}
				return "▣";
		}
	}

	// ================================================================ 右栏控件构建

	private final List<CRTEditorWidgets.Widget> rightHeaderWidgets = new ArrayList<>();
	private int rfX;
	private int rfY;
	private int rfW;
	private int rfContentTop;
	private int rfSectionStart;

	private void rebuildRightWidgets() {
		rightHeaderWidgets.clear();
		// 用两遍构建处理滚动夹取（控件位置依赖滚动偏移）
		for (int pass = 0; pass < 2; pass++) {
			rightWidgets.clear();
			rightTexts.clear();
			rightSections.clear();
			buildRightHeader();
			buildRightContent();
			final int contentHeight = (rfY - rfContentTop) + CRTEditorWidgets.PANEL_PADDING;
			rightContentHeight = contentHeight;
			final int maxScroll = Math.max(0, contentHeight - rightBodyRect.h);
			final int clamped = CRTEditorWidgets.clamp(rightScroll, 0, maxScroll);
			if (clamped == rightScroll) {
				break;
			}
			rightScroll = clamped;
		}
	}

	private void buildRightHeader() {
		if (rightPanelRect.w <= 0) {
			return;
		}
		final RightMode mode = rightMode();
		if (mode != RightMode.SETTINGS) {
			final CRTEditorWidgets.Button back = new CRTEditorWidgets.Button(16, 16, "‹", CRTEditorWidgets.ButtonStyle.ICON, this::clearSelection);
			back.setBounds(rightPanelRect.x + 4, rightHeaderRect.y + 4, 16);
			back.tooltip = tr("gui.nanbin.crt_editor.back_to_settings");
			rightHeaderWidgets.add(back);
		}
	}

	private void buildRightContent() {
		rfX = rightPanelRect.x + CRTEditorWidgets.PANEL_PADDING;
		rfW = Math.max(80, rightPanelRect.w - CRTEditorWidgets.PANEL_PADDING * 2 - SCROLLBAR_WIDTH - 2);
		rfY = rightBodyRect.y + CRTEditorWidgets.PANEL_PADDING - rightScroll;
		rfContentTop = rfY;
		if (rightPanelRect.w <= 0) {
			return;
		}
		switch (rightMode()) {
			case ELEMENT:
				buildElementFields();
				break;
			case BATCH:
				buildBatchFields();
				break;
			case SETTINGS:
			default:
				buildSettingsFields();
				break;
		}
	}

	// ================================================================ 右栏构建辅助

	private void rfGap(int pixels) {
		rfY += pixels;
	}

	private void rfLabel(String text) {
		rightTexts.add(new TextLine(text, rfX, rfY + 1, CRTEditorTheme.TEXT_DIM, rfW));
		rfY += CRTEditorWidgets.LINE_HEIGHT;
	}

	private void rfValueLabel(String text, int color) {
		rightTexts.add(new TextLine(text, rfX, rfY + 1, color, rfW));
		rfY += CRTEditorWidgets.LINE_HEIGHT;
	}

	/** 数值/计数标题（1.15×），用于「已选择 N 个元素」之类的小标题。 */
	private void rfGroupValueLabel(String text, int color) {
		rightTexts.add(new TextLine(text, rfX, rfY + 1, color, rfW, CRTEditorWidgets.FONT_SCALE_GROUP));
		rfY += CRTEditorWidgets.LINE_HEIGHT + 3;
	}

	/** 说明文字：0.85× + TEXT_DIM（网页 11px hint 的近似）。 */
	private void rfHint(String text) {
		rightTexts.add(new TextLine(text, rfX, rfY + 2, CRTEditorTheme.TEXT_DIM, rfW, CRTEditorWidgets.FONT_SCALE_HINT));
		rfY += CRTEditorWidgets.LINE_HEIGHT + 1;
	}

	/** 分组标题：1.15× + 主文字色（网页 .palette-category-title / .panel-section 标题）。 */
	private void rfSectionTitle(String text) {
		rightTexts.add(new TextLine(text, rfX, rfY + 1, CRTEditorTheme.TEXT, rfW, CRTEditorWidgets.FONT_SCALE_GROUP));
		rfY += CRTEditorWidgets.LINE_HEIGHT + 4;
	}

	private void rfBeginSection() {
		rfSectionStart = rfY;
	}

	private void rfEndSection() {
		rightSections.add(new Section(rfX - 4, rfSectionStart - 2, rfW + 8, rfY - rfSectionStart + 1));
		rfY += 6;
	}


	/** 控件展开（如色板）后把它滚到右栏可视区域内，避免“看不到/点不到色板”。 */
	private void ensureRightWidgetVisible(@Nullable CRTEditorWidgets.Widget widget) {
		if (widget == null || rightPanelRect.w <= 0) {
			return;
		}
		widget.h = widget.getHeight();
		final int padding = 6;
		final int bodyTop = rightBodyRect.y + padding;
		final int bodyBottom = rightBodyRect.y + rightBodyRect.h - padding;
		boolean changed = false;
		final int bottom = widget.y + widget.h;
		if (bottom > bodyBottom) {
			rightScroll = Math.max(0, rightScroll + (bottom - bodyBottom));
			changed = true;
		} else if (widget.y < bodyTop) {
			rightScroll = Math.max(0, rightScroll - (bodyTop - widget.y));
			changed = true;
		}
		if (changed) {
			widgetsDirty = true;
			rebuildRightWidgets();
		}
	}

	private <T extends CRTEditorWidgets.Widget> T rfAdd(T widget) {
		widget.setBounds(rfX, rfY, rfW);
		widget.h = widget.getHeight();
		rightWidgets.add(widget);
		rfY += widget.getHeight() + CRTEditorWidgets.GAP;
		return widget;
	}

	/** 标签 + 控件（常见字段组合）。 */
	private <T extends CRTEditorWidgets.Widget> T rfField(String label, T widget) {
		rfLabel(label);
		return rfAdd(widget);
	}

	private CRTEditorWidgets.Stepper rfIntStepper(int min, int max, int step, java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
		return rfAdd(CRTEditorWidgets.Stepper.integer(min, max, step, getter, setter));
	}

	// ================================================================ 标识牌设置面板

	private void buildSettingsFields() {
		settingsRowIndex = CRTEditorWidgets.clamp(settingsRowIndex, 0, Math.max(0, layout.rows.size() - 1));
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.settings.title"));

		rfLabel(tr("gui.nanbin.crt_editor.settings.row_height"));
		final CRTEditorWidgets.Stepper rowHeight = CRTEditorWidgets.Stepper.integer(CRTSignLayout.MIN_ROW_HEIGHT, CRTSignLayout.MAX_ROW_HEIGHT, 4,
				() -> layout.rowHeight, value -> {
					if (value == layout.rowHeight) {
						return;
					}
					editDiscrete(null, () -> {
						final int oldHeight = Math.max(1, layout.rowHeight);
						final int oldWidth = Math.max(1, layout.width);
						layout.rowHeight = value;
						if (layout.aspectLocked) {
							layout.width = Math.max(CRTSignLayout.MIN_WIDTH, Math.round(oldWidth * (float) value / oldHeight));
						}
					});
				});
		rowHeight.setSuffix("px");
		rfAdd(rowHeight);
		rfValueLabel(tr("gui.nanbin.crt_editor.settings.row_count", layout.rows.size()), CRTEditorTheme.TEXT_DIM);

		rfLabel(tr("gui.nanbin.crt_editor.settings.width_mode"));
		rfAdd(new CRTEditorWidgets.Segmented(
				new String[]{tr("gui.nanbin.crt_editor.settings.width_fixed"), tr("gui.nanbin.crt_editor.settings.width_dynamic")},
				() -> layout.widthMode == CRTSignLayout.WidthMode.FIXED ? 0 : 1,
				index -> editDiscrete(null, () -> layout.widthMode = index == 0 ? CRTSignLayout.WidthMode.FIXED : CRTSignLayout.WidthMode.DYNAMIC)));
		if (layout.widthMode == CRTSignLayout.WidthMode.FIXED) {
			rfLabel(tr("gui.nanbin.crt_editor.settings.width_units"));
			final int currentUnits = Math.max(1, Math.min(15, Math.round(layout.width / (float) CRTSignLayout.GRID_SIZE)));
			final CRTEditorWidgets.Dropdown units = new CRTEditorWidgets.Dropdown(
					() -> String.valueOf(Math.max(1, Math.min(15, Math.round(layout.width / (float) CRTSignLayout.GRID_SIZE)))),
					id -> {
						try {
							final int selected = Integer.parseInt(id);
							if (selected * CRTSignLayout.GRID_SIZE != layout.width) {
								editDiscrete(null, () -> {
									layout.width = selected * CRTSignLayout.GRID_SIZE;
									layout.aspectLocked = false;
								});
							}
						} catch (NumberFormatException ignored) {
						}
					});
			for (int i = 1; i <= 15; i++) {
				units.addOption(String.valueOf(i), tr("gui.nanbin.crt_editor.settings.width_units_value", i, i * CRTSignLayout.GRID_SIZE));
			}
			units.h = CRTEditorWidgets.CONTROL_HEIGHT;
			rfAdd(units);
			rfHint(tr("gui.nanbin.crt_editor.settings.width_units_hint", CRTSignLayout.GRID_SIZE));

			rfLabel(tr("gui.nanbin.crt_editor.settings.width_px"));
			rfAdd(CRTEditorWidgets.Stepper.integer(CRTSignLayout.MIN_WIDTH, CRTSignLayout.MAX_WIDTH, 16, () -> layout.width, value -> {
				if (value == layout.width) {
					return;
				}
				editDiscrete(null, () -> {
					final int oldWidth = Math.max(1, layout.width);
					final int oldHeight = Math.max(1, layout.rowHeight);
					layout.width = value;
					if (layout.aspectLocked) {
						layout.rowHeight = CRTEditorWidgets.clamp(Math.round(oldHeight * (float) value / oldWidth),
								CRTSignLayout.MIN_ROW_HEIGHT, CRTSignLayout.MAX_ROW_HEIGHT);
					}
				});
			}));
		} else {
			rfHint(tr("gui.nanbin.crt_editor.settings.width_dynamic_hint", Math.max(1, Math.round(signContentW))));
		}

		rfAdd(new CRTEditorWidgets.Checkbox(tr("gui.nanbin.crt_editor.settings.aspect_lock"),
				() -> layout.aspectLocked,
				value -> editDiscrete(null, () -> layout.aspectLocked = value)));

		rfLabel(tr("gui.nanbin.crt_editor.settings.background"));
		rfAdd(createColorPicker("settings.bg", () -> layout.backgroundColor,
				value -> editContinuous("settings.bg", () -> layout.backgroundColor = value), false));

		rfLabel(tr("gui.nanbin.crt_editor.settings.frame_width"));
		rfAdd(CRTEditorWidgets.Stepper.integer(0, 512, 1, () -> layout.frameWidth,
				value -> editDiscrete(null, () -> layout.frameWidth = value)).setSuffix("px"));
		rfHint(tr("gui.nanbin.crt_editor.settings.frame_hint"));
		rfLabel(tr("gui.nanbin.crt_editor.settings.frame_color"));
		rfAdd(createColorPicker("settings.frameColor", () -> layout.frameColor,
				value -> editContinuous("settings.frameColor", () -> layout.frameColor = value), false));

		if (layout.rows.size() > 1 || rowBackgroundSelected() != null) {
			rfLabel(tr("gui.nanbin.crt_editor.settings.row_background"));
			final CRTEditorWidgets.Dropdown rowSelector = new CRTEditorWidgets.Dropdown(
					() -> layout.rows.isEmpty() ? "" : layout.rows.get(CRTEditorWidgets.clamp(settingsRowIndex, 0, layout.rows.size() - 1)).id,
					id -> {
						final int index = layout.indexOfRow(id);
						if (index >= 0) {
							settingsRowIndex = index;
							widgetsDirty = true;
						}
					});
			for (int i = 0; i < layout.rows.size(); i++) {
				rowSelector.addOption(layout.rows.get(i).id, tr("gui.nanbin.crt_editor.settings.row_n", i + 1));
			}
			rowSelector.h = CRTEditorWidgets.CONTROL_HEIGHT;
			rfAdd(rowSelector);
			rfAdd(createColorPicker("settings.rowBg", () -> rowBackgroundSelected(), value -> {
				final int index = CRTEditorWidgets.clamp(settingsRowIndex, 0, layout.rows.size() - 1);
				if (index < 0 || index >= layout.rows.size()) {
					return;
				}
				editContinuous("settings.rowBg", () -> layout.rows.get(index).backgroundColor = value < 0 ? null : value);
			}, true));
			if (rowBackgroundSelected() != null) {
				rfHint(tr("gui.nanbin.crt_editor.settings.row_bg_custom"));
			} else {
				rfHint(tr("gui.nanbin.crt_editor.settings.row_bg_inherit"));
			}
			rfAdd(new CRTEditorWidgets.Button(0, 16, tr("gui.nanbin.crt_editor.settings.row_bg_reset"), CRTEditorWidgets.ButtonStyle.SECONDARY,
					() -> {
						final int index = CRTEditorWidgets.clamp(settingsRowIndex, 0, layout.rows.size() - 1);
						if (index < 0 || index >= layout.rows.size()) {
							return;
						}
						editDiscrete(null, () -> layout.rows.get(index).backgroundColor = null);
					}));
		}
		rfEndSection();

		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.settings.editor"));
		rfAdd(new CRTEditorWidgets.Button(0, 16, tr("gui.nanbin.crt_editor.settings.clear_sign"), CRTEditorWidgets.ButtonStyle.DANGER, this::clearSignAction));
		rfHint("clear".equals(armedClear) ? tr("gui.nanbin.crt_editor.confirm.click_again") : tr("gui.nanbin.crt_editor.settings.clear_sign_hint"));
		rfEndSection();

		// ── JS 样式（旧格子编辑器的整牌 JS 支持移植） ──
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.js.section"));
		rfLabel(tr("gui.nanbin.crt_editor.js.style"));
		final CRTEditorWidgets.Dropdown jsStyle = new CRTEditorWidgets.Dropdown(
				() -> layout.hasJsStyle() ? layout.jsStyleId : "none",
				id -> {
					final String styleId = "none".equals(id) ? null : id;
					if (Objects.equals(styleId, layout.jsStyleId)) {
						return;
					}
					if (styleId != null && !JSSignConfig.hasScript(styleId)) {
						toast(tr("gui.nanbin.crt_editor.js.invalid"), true);
						return;
					}
					clearSelection();
					editDiscrete(null, () -> {
						layout.jsStyleId = styleId;
						if (styleId == null) {
							layout.jsSelectedIds.clear();
						}
					});
					widgetsDirty = true;
				});
		jsStyle.addOption("none", tr("gui.nanbin.crt_editor.js.none"));
		for (final String scriptId : JSSignConfig.getAllScripts().keySet()) {
			jsStyle.addOption(scriptId, tr(JSSignConfig.getDisplayNameKey(scriptId)));
		}
		jsStyle.h = CRTEditorWidgets.CONTROL_HEIGHT;
		rfAdd(jsStyle);
		if (layout.hasJsStyle()) {
			rfHint(tr("gui.nanbin.crt_editor.js.locked_hint"));
			rfAdd(new CRTEditorWidgets.Button(0, 16, tr("gui.nanbin.crt_editor.js.data"),
					CRTEditorWidgets.ButtonStyle.SECONDARY, this::openJsDataScreen));
			rfHint(tr("gui.nanbin.crt_editor.js.data_hint"));
		} else {
			rfHint(tr("gui.nanbin.crt_editor.js.off_hint"));
		}
		rfEndSection();

		// ── 项目（§3.3.1 #11 / §7.1）：与网页项目 JSON 互导 ──
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.settings.project"));
		rfAdd(new CRTEditorWidgets.Button(0, 16, tr("gui.nanbin.crt_editor.settings.export_json"),
				CRTEditorWidgets.ButtonStyle.SECONDARY, this::openJsonExportScreen));
		rfAdd(new CRTEditorWidgets.Button(0, 16, tr("gui.nanbin.crt_editor.settings.import_json"),
				CRTEditorWidgets.ButtonStyle.SECONDARY, this::openJsonImportScreen));
		rfHint(tr("gui.nanbin.crt_editor.settings.project_hint"));
		rfEndSection();
	}

	// ================================================================ 项目 JSON 导入导出（§7.1）

	private void openJsonExportScreen() {
		blurFocused();
		final String json = layout.toJson();
		MinecraftClient.getInstance().openScreen(new Screen(new CRTSignJsonScreen(this, CRTSignJsonScreen.Mode.EXPORT, json, null)));
	}

	private void openJsonImportScreen() {
		blurFocused();
		final String json = layout.toJson();
		MinecraftClient.getInstance().openScreen(new Screen(new CRTSignJsonScreen(this,
				CRTSignJsonScreen.Mode.IMPORT, json, text -> pendingImportJson = text)));
	}

	/** 从 JSON 屏幕返回：校验长度/结构后整体替换布局（可撤销，并立即保存）。 */
	private void applyPendingJsonImport() {
		if (pendingImportJson == null) {
			return;
		}
		final String json = pendingImportJson;
		pendingImportJson = null;
		if (json.length() > CRTSignJsonScreen.MAX_JSON_LENGTH) {
			toast(tr("gui.nanbin.crt_editor.json.too_long", CRTSignJsonScreen.MAX_JSON_LENGTH), true);
			return;
		}
		final CRTSignLayout imported = CRTSignLayout.fromJson(json);
		if (imported == null) {
			toast(tr("gui.nanbin.crt_editor.json.invalid"), true);
			return;
		}
		clearSelection();
		pushHistory(null);
		layout = imported;
		breakHistoryGroup();
		markEdited(true);
		saveNow();
		toast(tr("gui.nanbin.crt_editor.json.imported"));
	}

	@Nullable
	private Integer rowBackgroundSelected() {
		final int index = CRTEditorWidgets.clamp(settingsRowIndex, 0, Math.max(0, layout.rows.size() - 1));
		return layout.rows.isEmpty() ? null : layout.rows.get(index).backgroundColor;
	}

	// ================================================================ 元素属性面板

	private void buildElementFields() {
		final CRTSignElement element = primaryElement();
		if (element == null) {
			return;
		}
		final boolean aligned = layout.widthMode == CRTSignLayout.WidthMode.FIXED;
		rightTexts.add(new TextLine(elementTypeName(element.type) + "  ·  " + tr("gui.nanbin.crt_editor.element_id_short", shortId(element.id)),
				rfX, rfY + 1, CRTEditorTheme.TEXT, rfW, CRTEditorWidgets.FONT_SCALE_GROUP));
		rfY += CRTEditorWidgets.LINE_HEIGHT + 4;

		// ── 内容 ──
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.group.content"));
		switch (element.type) {
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT: {
				rfLabel(tr("gui.nanbin.crt_editor.field.text_zh"));
				final CRTEditorWidgets.TextField zh = new CRTEditorWidgets.TextField(() -> element.textZh, value -> editContinuous(element.id + ":textZh", () -> element.textZh = value));
				zh.setPlaceholder(tr("gui.nanbin.crt_editor.placeholder.text_zh")).setMaxLength(48);
				rfAdd(zh);
				rfLabel(tr("gui.nanbin.crt_editor.field.text_en"));
				final CRTEditorWidgets.TextField en = new CRTEditorWidgets.TextField(() -> element.textEn, value -> editContinuous(element.id + ":textEn", () -> element.textEn = value));
				en.setPlaceholder(tr("gui.nanbin.crt_editor.placeholder.text_en")).setMaxLength(64);
				rfAdd(en);
				rfAdd(new CRTEditorWidgets.Checkbox(tr("gui.nanbin.crt_editor.field.bold"), () -> element.bold,
						value -> editDiscrete(null, () -> element.bold = value)));
				rfLabel(tr("gui.nanbin.crt_editor.field.text_align"));
				rfAdd(new CRTEditorWidgets.Segmented(alignLabels(), () -> alignIndex(element.align), index -> editDiscrete(null, () -> element.align = alignFromIndex(index))));
				break;
			}
			case BIG_NUMBER: {
				rfLabel(tr("gui.nanbin.crt_editor.field.content"));
				final CRTEditorWidgets.TextField text = new CRTEditorWidgets.TextField(() -> element.text, value -> editContinuous(element.id + ":text", () -> element.text = value));
				text.setPlaceholder(tr("gui.nanbin.crt_editor.placeholder.big_number")).setMaxLength(24);
				rfAdd(text);
				break;
			}
			case TEXT_LINE: {
				rfAdd(new CRTEditorWidgets.Checkbox(tr("gui.nanbin.crt_editor.field.name_sink"), () -> element.nameSink,
						value -> editDiscrete(null, () -> element.nameSink = value)));
				rfLabel(tr("gui.nanbin.crt_editor.field.text_zh"));
				final CRTEditorWidgets.TextField text = new CRTEditorWidgets.TextField(() -> element.text, value -> editContinuous(element.id + ":text", () -> element.text = value));
				text.setPlaceholder(tr("gui.nanbin.crt_editor.placeholder.line_name")).setMaxLength(24);
				rfAdd(text);
				rfLabel(tr("gui.nanbin.crt_editor.field.text_en"));
				final CRTEditorWidgets.TextField textEn = new CRTEditorWidgets.TextField(() -> element.textEn, value -> editContinuous(element.id + ":textEn", () -> element.textEn = value));
				textEn.setPlaceholder("Loop Line").setMaxLength(48);
				rfAdd(textEn);
				rfLabel(tr("gui.nanbin.crt_editor.field.content_align"));
				rfAdd(new CRTEditorWidgets.Segmented(leftRightLabels(), () -> element.align == CRTSignElement.TextAlign.RIGHT ? 1 : 0,
						index -> editDiscrete(null, () -> element.align = index == 1 ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT)));
				break;
			}
			case NUMBER_LINE: {
				rfLabel(tr("gui.nanbin.crt_editor.field.content_align"));
				rfAdd(new CRTEditorWidgets.Segmented(leftRightLabels(), () -> element.align == CRTSignElement.TextAlign.RIGHT ? 1 : 0,
						index -> editDiscrete(null, () -> element.align = index == 1 ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT)));
				for (int i = 0; i < element.lines.size(); i++) {
					final int lineIndex = i;
					final CRTSignElement.LineItem line = element.lines.get(i);
					rfLabel(tr("gui.nanbin.crt_editor.field.line_n", i + 1));
					final int rowY = rfY;
					final CRTEditorWidgets.TextField number = new CRTEditorWidgets.TextField(() -> line.number,
							value -> editContinuous(element.id + ":line" + lineIndex + ":number", () -> line.number = filterLineNumber(value)));
					number.setPlaceholder("1").setMaxLength(2).setNumericOnly(true);
					number.setBounds(rfX, rowY, 46);
					rightWidgets.add(number);
					final CRTEditorWidgets.ColorPicker color = createColorPicker("line.color." + lineIndex, () -> line.color,
							value -> editContinuous(element.id + ":line" + lineIndex + ":color", () -> line.color = value), false);
					color.setBounds(rfX + 50, rowY, Math.max(60, rfW - 50 - 20 - CRTEditorWidgets.GAP));
					color.h = color.getHeight();
					rightWidgets.add(color);
					final CRTEditorWidgets.Button remove = new CRTEditorWidgets.Button(18, 16, "✕", CRTEditorWidgets.ButtonStyle.ICON,
							() -> editDiscrete(null, () -> element.lines.remove(lineIndex)));
					remove.setBounds(rfX + rfW - 18, rowY, 18);
					remove.tooltip = tr("gui.nanbin.crt_editor.field.remove_line");
					rightWidgets.add(remove);
					rfY += Math.max(16, color.getHeight()) + CRTEditorWidgets.GAP;
				}
				final CRTEditorWidgets.Button addLine = new CRTEditorWidgets.Button(0, 16,
						tr("gui.nanbin.crt_editor.field.add_line"), CRTEditorWidgets.ButtonStyle.DASHED,
						() -> editDiscrete(null, () -> element.lines.add(new CRTSignElement.LineItem("1", 0xFFE4002B))));
				addLine.enabled = element.align != CRTSignElement.TextAlign.RIGHT;
				addLine.tooltip = element.align == CRTSignElement.TextAlign.RIGHT ? tr("gui.nanbin.crt_editor.field.add_line_disabled") : null;
				rfAdd(addLine);
				break;
			}
			case ENTRANCE:
			case EXIT: {
				rfLabel(tr("gui.nanbin.crt_editor.field.code"));
				final CRTEditorWidgets.TextField code = new CRTEditorWidgets.TextField(() -> element.code, value -> editContinuous(element.id + ":code", () -> element.code = value));
				code.setPlaceholder(tr("gui.nanbin.crt_editor.placeholder.code")).setMaxLength(12);
				rfAdd(code);
				if (element.type == CRTSignElementType.EXIT) {
					rfLabel(tr("gui.nanbin.crt_editor.field.content_align"));
					rfAdd(new CRTEditorWidgets.Segmented(leftRightLabels(), () -> element.align == CRTSignElement.TextAlign.RIGHT ? 1 : 0,
							index -> editDiscrete(null, () -> element.align = index == 1 ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT)));
				}
				break;
			}
			case SPACE: {
				rfLabel(tr("gui.nanbin.crt_editor.field.width_ratio"));
				rfAdd(new CRTEditorWidgets.Slider(0.1, 8.0, 0.1, 1, 40, () -> element.widthRatio,
						value -> editContinuous(element.id + ":widthRatio", () -> element.widthRatio = (float) value)));
				break;
			}
			case ROUTE_INFO: {
				rfHint(tr("gui.nanbin.crt_editor.field.route_info_hint"));
				rfLabel(tr("gui.nanbin.crt_editor.field.route_info_label_zh"));
				final CRTEditorWidgets.TextField routeLabelZh = new CRTEditorWidgets.TextField(() -> element.textZh,
						value -> editContinuous(element.id + ":textZh", () -> element.textZh = value));
				routeLabelZh.setPlaceholder("号线").setMaxLength(12);
				rfAdd(routeLabelZh);
				rfLabel(tr("gui.nanbin.crt_editor.field.route_info_label_en"));
				final CRTEditorWidgets.TextField routeLabelEn = new CRTEditorWidgets.TextField(() -> element.textEn,
						value -> editContinuous(element.id + ":textEn", () -> element.textEn = value));
				routeLabelEn.setPlaceholder("Line").setMaxLength(24);
				rfAdd(routeLabelEn);
				break;
			}
			case EXIT_INFO: {
				// 箭头：方向复用 ARROW 的八向分段按钮，另加显示开关（关闭后编号/标签/目的地左移）
				rfLabel(tr("gui.nanbin.crt_editor.field.direction"));
				rfAdd(new CRTEditorWidgets.Segmented(new String[]{"←", "↖", "↑", "↗", "→", "↘", "↓", "↙"},
						() -> arrowDirectionIndex(element.arrowDirection),
						index -> editDiscrete(null, () -> element.arrowDirection = arrowDirectionFromIndex(index))));
				rfAdd(new CRTEditorWidgets.Checkbox(tr("gui.nanbin.crt_editor.field.show_arrow"), () -> element.showArrow,
						value -> editDiscrete(null, () -> element.showArrow = value)));
				// 出口多选：追加进 bindingIds（去重），旁边的按钮清空
				rfLabel(tr("gui.nanbin.crt_editor.field.exit_info_exits"));
				final int exitRowY = rfY;
				final int exitHalfWidth = Math.max(30, (rfW - CRTEditorWidgets.GAP) / 2);
				final CRTEditorWidgets.Button chooseExits = new CRTEditorWidgets.Button(exitHalfWidth, 16,
						tr("gui.nanbin.crt_editor.field.exit_info_choose"), CRTEditorWidgets.ButtonStyle.SECONDARY,
						() -> openExitInfoExitSelector(element));
				chooseExits.setBounds(rfX, exitRowY, exitHalfWidth);
				chooseExits.tooltip = tr("gui.nanbin.crt_editor.field.exit_info_choose_tip");
				rightWidgets.add(chooseExits);
				final CRTEditorWidgets.Button clearExits = new CRTEditorWidgets.Button(exitHalfWidth, 16,
						tr("gui.nanbin.crt_editor.binding.clear"), CRTEditorWidgets.ButtonStyle.TOOL,
						() -> editDiscrete(null, element.bindingIds::clear));
				clearExits.setBounds(rfX + exitHalfWidth + CRTEditorWidgets.GAP, exitRowY, exitHalfWidth);
				clearExits.enabled = !element.bindingIds.isEmpty();
				rightWidgets.add(clearExits);
				rfY += 16 + CRTEditorWidgets.GAP;
				rfHint(tr("gui.nanbin.crt_editor.field.exit_info_hint"));
				// 标签文字（默认「出口 / Exit」，清空则按默认值显示）
				rfLabel(tr("gui.nanbin.crt_editor.field.exit_info_label_zh"));
				final CRTEditorWidgets.TextField labelZh = new CRTEditorWidgets.TextField(() -> element.textZh,
						value -> editContinuous(element.id + ":textZh", () -> element.textZh = value));
				labelZh.setPlaceholder("出口").setMaxLength(12);
				rfAdd(labelZh);
				rfLabel(tr("gui.nanbin.crt_editor.field.exit_info_label_en"));
				final CRTEditorWidgets.TextField labelEn = new CRTEditorWidgets.TextField(() -> element.textEn,
						value -> editContinuous(element.id + ":textEn", () -> element.textEn = value));
				labelEn.setPlaceholder("Exit").setMaxLength(24);
				rfAdd(labelEn);
				rfHint(tr("gui.nanbin.crt_editor.field.exit_info_dest_hint"));
				break;
			}
			case ICON: {
				rfLabel(tr("gui.nanbin.crt_editor.field.icon"));
				rfAddIconGrid(element);
				final boolean serviceIcon = isServiceIcon(element.icon);
				if (serviceIcon) {
					rfLabel(tr("gui.nanbin.crt_editor.field.rotation"));
					final int[] rotations = {-90, 0, 90, 180};
					rfAdd(new CRTEditorWidgets.Segmented(new String[]{"-90°", "0°", "90°", "180°"},
							() -> rotationIndex(element.rotation),
							index -> editDiscrete(null, () -> element.rotation = rotations[CRTEditorWidgets.clamp(index, 0, 3)])));
				}
				rfLabel(tr("gui.nanbin.crt_editor.field.icon_color"));
				rfAdd(createColorPicker("element.icon_color", () -> element.iconColor,
						value -> editContinuous("element.icon_color", () -> element.iconColor = value <= 0 ? null : value), true));
				rfAdd(new CRTEditorWidgets.SwatchRow(() -> element.iconColor,
						value -> editContinuous("element.icon_color", () -> element.iconColor = value)));
				rfHint(tr("gui.nanbin.crt_editor.field.icon_color_hint"));
				break;
			}
			case ARROW: {
				rfLabel(tr("gui.nanbin.crt_editor.field.direction"));
				rfAdd(new CRTEditorWidgets.Segmented(new String[]{"←", "↖", "↑", "↗", "→", "↘", "↓", "↙"},
						() -> arrowDirectionIndex(element.arrowDirection), index -> editDiscrete(null, () -> element.arrowDirection = arrowDirectionFromIndex(index))));
				rfLabel(tr("gui.nanbin.crt_editor.field.thickness"));
				rfAdd(new CRTEditorWidgets.Slider(0.05, 0.95, 0.05, 2, 40, () -> element.thicknessRatio,
						value -> editContinuous(element.id + ":thickness", () -> element.thicknessRatio = (float) value)));
				break;
			}
			default:
				break;
		}
		rfEndSection();

		// ── 排版 ──
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.group.layout"));
		if (aligned) {
			rfLabel(tr("gui.nanbin.crt_editor.field.element_align"));
			rfAdd(new CRTEditorWidgets.Segmented(alignLabels(), () -> elementAlignIndex(element.elementAlign),
					index -> editDiscrete(null, () -> element.elementAlign = elementAlignFromIndex(index))));
		} else {
			rfHint(tr("gui.nanbin.crt_editor.field.element_align_dynamic"));
		}
		if (element.supportsContentAlign()) {
			rfLabel(tr("gui.nanbin.crt_editor.field.content_align"));
			rfAdd(new CRTEditorWidgets.Segmented(leftRightLabels(), () -> element.align == CRTSignElement.TextAlign.RIGHT ? 1 : 0,
					index -> editDiscrete(null, () -> element.align = index == 1 ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT)));
		}
		if (element.type != CRTSignElementType.SPACE) {
			rfAdd(new CRTEditorWidgets.Checkbox(tr("gui.nanbin.crt_editor.field.padding_auto"), () -> element.paddingAuto,
					value -> editDiscrete(null, () -> element.paddingAuto = value)));
			rfLabel(tr("gui.nanbin.crt_editor.field.padding_top_bottom"));
			final int paddingRowY = rfY;
			final int paddingWidth = (rfW - CRTEditorWidgets.GAP) / 2;
			final CRTEditorWidgets.Stepper top = CRTEditorWidgets.Stepper.decimal(0.0, 1.0, 0.05, 2,
					() -> element.paddingTop, value -> editContinuous(element.id + ":paddingTop", () -> {
						element.paddingTop = (float) value;
						element.paddingAuto = false;
					}));
			top.setBounds(rfX, paddingRowY, paddingWidth);
			top.tooltip = tr("gui.nanbin.crt_editor.field.padding_top");
			rightWidgets.add(top);
			final CRTEditorWidgets.Stepper bottom = CRTEditorWidgets.Stepper.decimal(0.0, 1.0, 0.05, 2,
					() -> element.paddingBottom, value -> editContinuous(element.id + ":paddingBottom", () -> {
						element.paddingBottom = (float) value;
						element.paddingAuto = false;
					}));
			bottom.setBounds(rfX + paddingWidth + CRTEditorWidgets.GAP, paddingRowY, paddingWidth);
			bottom.tooltip = tr("gui.nanbin.crt_editor.field.padding_bottom");
			rightWidgets.add(bottom);
			rfY += 16 + CRTEditorWidgets.GAP;
			rfLabel(tr("gui.nanbin.crt_editor.field.padding_left_right"));
			final int paddingRowY2 = rfY;
			final CRTEditorWidgets.Stepper left = CRTEditorWidgets.Stepper.decimal(0.0, 8.0, 0.05, 2,
					() -> element.paddingLeft, value -> editContinuous(element.id + ":paddingLeft", () -> {
						element.paddingLeft = (float) value;
						element.paddingAuto = false;
					}));
			left.setBounds(rfX, paddingRowY2, paddingWidth);
			left.tooltip = tr("gui.nanbin.crt_editor.field.padding_left");
			rightWidgets.add(left);
			final CRTEditorWidgets.Stepper right = CRTEditorWidgets.Stepper.decimal(0.0, 8.0, 0.05, 2,
					() -> element.paddingRight, value -> editContinuous(element.id + ":paddingRight", () -> {
						element.paddingRight = (float) value;
						element.paddingAuto = false;
					}));
			right.setBounds(rfX + paddingWidth + CRTEditorWidgets.GAP, paddingRowY2, paddingWidth);
			right.tooltip = tr("gui.nanbin.crt_editor.field.padding_right");
			rightWidgets.add(right);
			rfY += 16 + CRTEditorWidgets.GAP;
			rfHint(tr("gui.nanbin.crt_editor.field.padding_hint"));
		}
		rfEndSection();

		// ── 颜色 ──
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.group.colors"));
		rfLabel(tr("gui.nanbin.crt_editor.field.color"));
		rfAdd(createColorPicker(element.id + ".color", () -> element.color,
				value -> editContinuous(element.id + ":color", () -> element.color = value), false));
		rfAdd(new CRTEditorWidgets.SwatchRow(() -> element.color,
				value -> editContinuous(element.id + ":color", () -> element.color = value)));
		if (element.type == CRTSignElementType.EXIT) {
			rfHint(tr("gui.nanbin.crt_editor.field.exit_bg_fixed"));
		} else {
			rfLabel(tr("gui.nanbin.crt_editor.field.background"));
			rfAdd(createColorPicker(element.id + ".bg", () -> element.backgroundColor,
					value -> editContinuous(element.id + ":bg", () -> element.backgroundColor = value < 0 ? null : value), true));
			rfAdd(new CRTEditorWidgets.SwatchRow(() -> element.backgroundColor,
					value -> editContinuous(element.id + ":bg", () -> element.backgroundColor = value)));
		}
		if (element.type == CRTSignElementType.NUMBER_LINE) {
			rfLabel(tr("gui.nanbin.crt_editor.field.text_color"));
			rfAdd(createColorPicker(element.id + ".textColor", () -> element.textColor,
					value -> editContinuous(element.id + ":textColor", () -> element.textColor = value), false));
		}
		if (element.type == CRTSignElementType.TEXT_LINE) {
			rfLabel(tr("gui.nanbin.crt_editor.field.block_color"));
			rfAdd(createColorPicker(element.id + ".blockColor", () -> element.blockColor,
					value -> editContinuous(element.id + ":blockColor", () -> element.blockColor = value), false));
			rfLabel(tr("gui.nanbin.crt_editor.field.text_color"));
			rfAdd(createColorPicker(element.id + ".textColor", () -> element.textColor,
					value -> editContinuous(element.id + ":textColor", () -> element.textColor = value), false));
		}
		rfEndSection();

		// ── MTR 数据绑定 ──
		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.group.binding"));
		rfLabel(tr("gui.nanbin.crt_editor.binding.kind"));
		final CRTEditorWidgets.Dropdown bindingDropdown = new CRTEditorWidgets.Dropdown(
				() -> element.binding.id,
				id -> setBinding(element, CRTSignElement.DataBinding.fromId(id)));
		for (final CRTSignElement.DataBinding binding : CRTSignElement.DataBinding.values()) {
			bindingDropdown.addOption(binding.id, bindingName(binding));
		}
		bindingDropdown.h = CRTEditorWidgets.CONTROL_HEIGHT;
		rfAdd(bindingDropdown);
		if (element.binding != CRTSignElement.DataBinding.NONE) {
			final int count = element.bindingIds.size();
			final String resolved = describeBindingValue(element);
			final String resolvedText = resolved == null || resolved.isEmpty() ? tr("gui.nanbin.crt_editor.binding.unresolved") : resolved;
			rfLabel(tr("gui.nanbin.crt_editor.binding.current", element.bindingIds.size(), resolvedText));
			// 出口信息条的选择/清空按钮在「内容」区（多选出口），这里不再重复
			if (element.type != CRTSignElementType.EXIT_INFO) {
				final int buttonRowY = rfY;
				final int halfWidth = (rfW - CRTEditorWidgets.GAP) / 2;
				final CRTEditorWidgets.Button choose = new CRTEditorWidgets.Button(halfWidth, 16, tr("gui.nanbin.crt_editor.binding.choose"),
						CRTEditorWidgets.ButtonStyle.SECONDARY, () -> openBindingSelector(element));
				choose.setBounds(rfX, buttonRowY, halfWidth);
				rightWidgets.add(choose);
				final CRTEditorWidgets.Button clear = new CRTEditorWidgets.Button(halfWidth, 16, tr("gui.nanbin.crt_editor.binding.clear"),
						CRTEditorWidgets.ButtonStyle.TOOL, () -> editDiscrete(null, element.bindingIds::clear));
				clear.setBounds(rfX + halfWidth + CRTEditorWidgets.GAP, buttonRowY, halfWidth);
				clear.enabled = count > 0;
				rightWidgets.add(clear);
				rfY += 16 + CRTEditorWidgets.GAP;
			}
			rfHint(tr("gui.nanbin.crt_editor.binding.choose_hint"));
		} else {
			rfHint(tr("gui.nanbin.crt_editor.binding.none_hint"));
		}
		rfEndSection();

		// ── 删除 ──
		rfAdd(new CRTEditorWidgets.Button(0, 16,
				armedDeleteElementId != null && armedDeleteElementId.equals(element.id)
						? tr("gui.nanbin.crt_editor.confirm.click_again")
						: tr("gui.nanbin.crt_editor.element.delete"),
				CRTEditorWidgets.ButtonStyle.DANGER, () -> deleteSingleElement(element.id)));
		rfGap(4);
	}

	private void rfAddIconGrid(CRTSignElement element) {
		final int columns = 4;
		final int gap = 3;
		final int cardWidth = Math.max(24, (rfW - gap * (columns - 1)) / columns);
		final int cardHeight = 30;
		int row = 0;
		for (int i = 0; i < SERVICE_ICONS.length; i++) {
			final String iconId = SERVICE_ICONS[i];
			final String glyph = SERVICE_GLYPHS[i];
			addIconCard(element, iconId, glyph, glyph, rfX + (i % columns) * (cardWidth + gap), rfY + row * (cardHeight + gap), cardWidth, cardHeight);
			if (i % columns == columns - 1) {
				row++;
			}
		}
		if (SERVICE_ICONS.length % columns != 0) {
			row++;
		}
		for (int i = 0; i < DIRECTION_ICONS.length; i++) {
			final String iconId = DIRECTION_ICONS[i];
			addIconCard(element, iconId, DIRECTION_GLYPHS[i], DIRECTION_GLYPHS[i], rfX + (i % columns) * (cardWidth + gap),
					rfY + row * (cardHeight + gap), cardWidth, cardHeight);
			if (i % columns == columns - 1) {
				row++;
			}
		}
		rfY += row * (cardHeight + gap) + 2;
	}

	private void addIconCard(CRTSignElement element, String iconId, String glyph, String label, int x, int y, int w, int h) {
		final String cacheKey = "icon:" + iconId;
		final CRTEditorWidgets.Card.Thumb thumb = (c, thumbX, thumbY, thumbW, thumbH) -> {
			// 图标选择器：用真实烘焙的元素缩略图（与牌面渲染同一套贴图映射）
			final JSSignTextureBaker.Baked baked = elementThumbnail(
					thumbnailPrototype(cacheKey, CRTSignElementType.ICON, iconId), ICON_THUMB_ROW_HEIGHT, cacheKey);
			if (drawBakedTexture(c, baked, thumbX, thumbY, thumbW, thumbH)) {
				return;
			}
			final float scale = Math.max(0.75F, Math.min(2.0F, thumbH / 12.0F));
			final int glyphWidth = c.scaledTextWidth(glyph, scale);
			final int glyphHeight = Math.round(CRTEditorWidgets.FONT_HEIGHT * scale);
			c.scaledText(glyph, thumbX + (thumbW - glyphWidth) / 2, thumbY + (thumbH - glyphHeight) / 2 + 1, scale,
					CRTEditorTheme.TEXT);
		};
		final CRTEditorWidgets.Card card = new CRTEditorWidgets.Card(w, h, label, glyph, thumb,
				() -> editDiscrete(null, () -> element.icon = iconId));
		card.selected = iconId.equals(element.icon);
		card.tooltip = label + " (" + iconId + ")";
		card.setBounds(x, y, w);
		rightWidgets.add(card);
	}

	// ================================================================ 批量属性面板

	private void buildBatchFields() {
		final List<CRTSignElement> elements = selectedElements();
		if (elements.isEmpty()) {
			return;
		}
		final boolean aligned = layout.widthMode == CRTSignLayout.WidthMode.FIXED;
		rfGroupValueLabel(tr("gui.nanbin.crt_editor.batch.count", elements.size()), CRTEditorTheme.TEXT);
		rfGap(2);

		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.batch.layout"));
		if (aligned) {
			rfLabel(tr("gui.nanbin.crt_editor.field.element_align"));
			rfAdd(new CRTEditorWidgets.Segmented(alignLabels(), () -> elementAlignIndex(elements.get(0).elementAlign),
					index -> applyToSelection(null, target -> target.elementAlign = elementAlignFromIndex(index))));
		} else {
			rfHint(tr("gui.nanbin.crt_editor.field.element_align_dynamic"));
		}
		if (anySupportsTextAlign(elements)) {
			rfLabel(tr("gui.nanbin.crt_editor.field.text_align"));
			rfAdd(new CRTEditorWidgets.Segmented(alignLabels(), () -> alignIndex(firstTextAlign(elements)),
					index -> applyToSelection(null, target -> {
						if (isTextElement(target)) {
							target.align = alignFromIndex(index);
						}
					})));
		}
		if (anySupportsContentAlign(elements)) {
			rfLabel(tr("gui.nanbin.crt_editor.field.content_align"));
			rfAdd(new CRTEditorWidgets.Segmented(leftRightLabels(), () -> firstContentAlign(elements) == CRTSignElement.TextAlign.RIGHT ? 1 : 0,
					index -> applyToSelection(null, target -> {
						if (target.supportsContentAlign()) {
							target.align = index == 1 ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT;
						}
					})));
		}
		rfLabel(tr("gui.nanbin.crt_editor.batch.padding_left_right"));
		final int paddingRowY = rfY;
		final int paddingWidth = (rfW - CRTEditorWidgets.GAP) / 2;
		final CRTEditorWidgets.Stepper left = CRTEditorWidgets.Stepper.decimal(0.0, 8.0, 0.05, 2,
				() -> elements.get(0).paddingLeft, value -> applyToSelection(null, target -> {
					target.paddingLeft = (float) value;
					target.paddingAuto = false;
				}));
		left.setBounds(rfX, paddingRowY, paddingWidth);
		left.tooltip = tr("gui.nanbin.crt_editor.field.padding_left");
		rightWidgets.add(left);
		final CRTEditorWidgets.Stepper right = CRTEditorWidgets.Stepper.decimal(0.0, 8.0, 0.05, 2,
				() -> elements.get(0).paddingRight, value -> applyToSelection(null, target -> {
					target.paddingRight = (float) value;
					target.paddingAuto = false;
				}));
		right.setBounds(rfX + paddingWidth + CRTEditorWidgets.GAP, paddingRowY, paddingWidth);
		right.tooltip = tr("gui.nanbin.crt_editor.field.padding_right");
		rightWidgets.add(right);
		rfY += 16 + CRTEditorWidgets.GAP;
		rfEndSection();

		rfBeginSection();
		rfSectionTitle(tr("gui.nanbin.crt_editor.group.colors"));
		rfLabel(tr("gui.nanbin.crt_editor.batch.background"));
		rfAdd(createColorPicker("batch.bg", () -> mixedBackground(elements),
				value -> applyToSelectionContinuous("batch.bg", target -> target.backgroundColor = value < 0 ? null : value), true));
		rfHint(tr("gui.nanbin.crt_editor.batch.background_hint"));
		rfEndSection();

		rfAdd(new CRTEditorWidgets.Button(0, 16, tr("gui.nanbin.crt_editor.batch.delete"), CRTEditorWidgets.ButtonStyle.DANGER, this::deleteSelection));
		rfGap(4);
	}

	@Nullable
	private Integer mixedBackground(List<CRTSignElement> elements) {
		return elements.isEmpty() ? null : elements.get(0).backgroundColor;
	}

	private boolean anySupportsTextAlign(List<CRTSignElement> elements) {
		for (final CRTSignElement element : elements) {
			if (isTextElement(element)) {
				return true;
			}
		}
		return false;
	}

	private boolean anySupportsContentAlign(List<CRTSignElement> elements) {
		for (final CRTSignElement element : elements) {
			if (element.supportsContentAlign()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isTextElement(CRTSignElement element) {
		return element.type == CRTSignElementType.BILINGUAL_TEXT || element.type == CRTSignElementType.SMALL_BILINGUAL_TEXT;
	}

	private CRTSignElement.TextAlign firstTextAlign(List<CRTSignElement> elements) {
		for (final CRTSignElement element : elements) {
			if (isTextElement(element)) {
				return element.align;
			}
		}
		return CRTSignElement.TextAlign.CENTER;
	}

	private CRTSignElement.TextAlign firstContentAlign(List<CRTSignElement> elements) {
		for (final CRTSignElement element : elements) {
			if (element.supportsContentAlign()) {
				return element.align;
			}
		}
		return CRTSignElement.TextAlign.LEFT;
	}

	private void applyToSelection(@Nullable String label, Consumer<CRTSignElement> mutation) {
		if (selection.isEmpty()) {
			return;
		}
		pushHistory(null);
		for (final CRTSignElement element : selectedElements()) {
			mutation.accept(element);
		}
		markEdited(true);
		saveNow();
	}

	/** 连续型批量改动（颜色输入等），合并为一次撤销。 */
	private void applyToSelectionContinuous(String key, Consumer<CRTSignElement> mutation) {
		if (selection.isEmpty()) {
			return;
		}
		pushHistory(key);
		for (final CRTSignElement element : selectedElements()) {
			mutation.accept(element);
		}
		markEdited(false);
	}

	// ================================================================ 颜色选择器工厂

	private CRTEditorWidgets.ColorPicker createColorPicker(String key, final java.util.function.Supplier<Integer> getter,
			final java.util.function.IntConsumer setter, boolean nullable) {
		boolean[] state = colorPickerStates.get(key);
		if (state == null) {
			state = new boolean[1];
			colorPickerStates.put(key, state);
		}
		final CRTEditorWidgets.ColorPicker picker = new CRTEditorWidgets.ColorPicker(getter, setter, nullable, state);
		picker.h = picker.getHeight();
		return picker;
	}

	// ================================================================ 枚举与标签辅助

	private static String[] alignLabels() {
		return new String[]{tr("gui.nanbin.crt_editor.align.left"), tr("gui.nanbin.crt_editor.align.center"), tr("gui.nanbin.crt_editor.align.right")};
	}

	private static String[] leftRightLabels() {
		return new String[]{tr("gui.nanbin.crt_editor.align.left"), tr("gui.nanbin.crt_editor.align.right")};
	}

	private static int alignIndex(CRTSignElement.TextAlign align) {
		switch (align) {
			case RIGHT:
				return 2;
			case LEFT:
				return 0;
			case CENTER:
			default:
				return 1;
		}
	}

	private static CRTSignElement.TextAlign alignFromIndex(int index) {
		if (index == 0) {
			return CRTSignElement.TextAlign.LEFT;
		}
		if (index == 2) {
			return CRTSignElement.TextAlign.RIGHT;
		}
		return CRTSignElement.TextAlign.CENTER;
	}

	private static int elementAlignIndex(CRTSignElement.ElementAlign align) {
		switch (align) {
			case CENTER:
				return 1;
			case RIGHT:
				return 2;
			case LEFT:
			default:
				return 0;
		}
	}

	private static CRTSignElement.ElementAlign elementAlignFromIndex(int index) {
		if (index == 1) {
			return CRTSignElement.ElementAlign.CENTER;
		}
		if (index == 2) {
			return CRTSignElement.ElementAlign.RIGHT;
		}
		return CRTSignElement.ElementAlign.LEFT;
	}

	private static int arrowDirectionIndex(CRTSignElement.ArrowDirection direction) {
		switch (direction) {
			case LEFT:
				return 0;
			case LEFT_UP:
				return 1;
			case UP:
				return 2;
			case RIGHT_UP:
				return 3;
			case RIGHT:
				return 4;
			case RIGHT_DOWN:
				return 5;
			case DOWN:
				return 6;
			case LEFT_DOWN:
			default:
				return 7;
		}
	}

	private static CRTSignElement.ArrowDirection arrowDirectionFromIndex(int index) {
		switch (index) {
			case 0:
				return CRTSignElement.ArrowDirection.LEFT;
			case 1:
				return CRTSignElement.ArrowDirection.LEFT_UP;
			case 2:
				return CRTSignElement.ArrowDirection.UP;
			case 3:
				return CRTSignElement.ArrowDirection.RIGHT_UP;
			case 4:
				return CRTSignElement.ArrowDirection.RIGHT;
			case 5:
				return CRTSignElement.ArrowDirection.RIGHT_DOWN;
			case 6:
				return CRTSignElement.ArrowDirection.DOWN;
			default:
				return CRTSignElement.ArrowDirection.LEFT_DOWN;
		}
	}

	private static int rotationIndex(int rotation) {
		switch (rotation) {
			case -90:
				return 0;
			case 90:
				return 2;
			case 180:
				return 3;
			case 0:
			default:
				return 1;
		}
	}

	private static boolean isServiceIcon(@Nullable String iconId) {
		if (iconId == null) {
			return false;
		}
		for (final String serviceIcon : SERVICE_ICONS) {
			if (serviceIcon.equals(iconId)) {
				return true;
			}
		}
		return false;
	}

	private static String shortId(@Nullable String id) {
		if (id == null || id.length() <= 6) {
			return id == null ? "" : id;
		}
		return id.substring(0, 6);
	}

	private static String filterLineNumber(String value) {
		final StringBuilder builder = new StringBuilder();
		if (value != null) {
			for (int i = 0; i < value.length() && builder.length() < 2; i++) {
				final char chr = value.charAt(i);
				if (chr >= '0' && chr <= '9') {
					builder.append(chr);
				}
			}
		}
		return builder.toString();
	}

	// ================================================================ 主渲染

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		ensureWidgets();
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		if (previewTransform == null) {
			// 首帧 / 编辑后的失效帧：用近似几何兜底；有 previewTransform 时由 drawCanvas 统一同步
			layoutCanvas();
		}
		updateHover();
		final CRTEditorWidgets.Ctx c = frameCtx(graphicsHolder, mouseX, mouseY, delta);
		drawScreenBackground(c);
		drawPanelResizeHints(c);
		drawCanvas(c);
		renderPalette(c);
		drawLeftScrollbar(c);
		renderLeftWidgets(c);
		renderRightPanel(c);
		renderCenterToolbarAndBottom(c);
		renderTopBarContent(c);
		drawOverlays(c);
	}

	private void drawScreenBackground(CRTEditorWidgets.Ctx c) {
		final int width = getWidthMapped();
		final int height = getHeightMapped();
		c.fill(0, 0, width, height, CRTEditorTheme.BG);
		// 顶栏
		c.fill(0, 0, width, TOP_BAR_HEIGHT, CRTEditorTheme.PANEL_BG);
		c.fill(0, TOP_BAR_HEIGHT - 1, width, TOP_BAR_HEIGHT, CRTEditorTheme.BORDER);
		// 左栏
		if (leftPanelRect.w > 0) {
			c.fill(leftPanelRect.x, leftPanelRect.y, leftPanelRect.x + leftPanelRect.w, leftPanelRect.y + leftPanelRect.h, CRTEditorTheme.PANEL_BG);
			c.fill(leftPanelRect.x + leftPanelRect.w - 1, leftPanelRect.y, leftPanelRect.x + leftPanelRect.w, leftPanelRect.y + leftPanelRect.h, CRTEditorTheme.BORDER);
			c.fill(leftHeaderRect.x, leftHeaderRect.y, leftHeaderRect.x + leftHeaderRect.w, leftHeaderRect.y + leftHeaderRect.h, CRTEditorTheme.PANEL_BG);
			c.fill(leftHeaderRect.x, leftHeaderRect.y + leftHeaderRect.h - 1, leftHeaderRect.x + leftHeaderRect.w, leftHeaderRect.y + leftHeaderRect.h, CRTEditorTheme.BORDER_SOFT);
			c.scaledTextClipped(tr("gui.nanbin.crt_editor.panel_elements_title"), leftHeaderRect.x + 8, leftHeaderRect.y + 7,
					leftHeaderRect.w - 36, CRTEditorWidgets.FONT_SCALE_TITLE, CRTEditorTheme.TEXT);
		}
		// 右栏
		if (rightPanelRect.w > 0) {
			c.fill(rightPanelRect.x, rightPanelRect.y, rightPanelRect.x + rightPanelRect.w, rightPanelRect.y + rightPanelRect.h, CRTEditorTheme.PANEL_BG);
			c.fill(rightPanelRect.x, rightPanelRect.y, rightPanelRect.x + 1, rightPanelRect.y + rightPanelRect.h, CRTEditorTheme.BORDER);
			c.fill(rightHeaderRect.x, rightHeaderRect.y, rightHeaderRect.x + rightHeaderRect.w, rightHeaderRect.y + rightHeaderRect.h, CRTEditorTheme.PANEL_BG);
			c.fill(rightHeaderRect.x, rightHeaderRect.y + rightHeaderRect.h - 1, rightHeaderRect.x + rightHeaderRect.w, rightHeaderRect.y + rightHeaderRect.h, CRTEditorTheme.BORDER_SOFT);
		}
		// 中栏工具条 / 底部
		c.fill(centerToolbarRect.x, centerToolbarRect.y, centerToolbarRect.x + centerToolbarRect.w, centerToolbarRect.y + centerToolbarRect.h, CRTEditorTheme.PANEL_BG);
		c.fill(centerToolbarRect.x, centerToolbarRect.y + centerToolbarRect.h - 1, centerToolbarRect.x + centerToolbarRect.w, centerToolbarRect.y + centerToolbarRect.h, CRTEditorTheme.BORDER);
		c.fill(centerBottomRect.x, centerBottomRect.y, centerBottomRect.x + centerBottomRect.w, centerBottomRect.y + centerBottomRect.h, CRTEditorTheme.PANEL_BG);
		c.fill(centerBottomRect.x, centerBottomRect.y, centerBottomRect.x + centerBottomRect.w, centerBottomRect.y + 1, CRTEditorTheme.BORDER);
	}

	private void renderTopBarContent(CRTEditorWidgets.Ctx c) {
		final int width = getWidthMapped();
		c.scaledTextClipped(tr("gui.nanbin.crt_editor.title"), 10, 7, Math.max(40, width - 300), CRTEditorWidgets.FONT_SCALE_TITLE, CRTEditorTheme.TEXT);
		int minX = width - 8;
		for (final CRTEditorWidgets.Widget widget : topWidgets) {
			// 只统计顶栏内的动作按钮；面板头部的折叠按钮也放在 topWidgets 里，不能参与定位
			if (widget.y < TOP_BAR_HEIGHT) {
				minX = Math.min(minX, widget.x);
			}
		}
		final String status = layoutDirty ? tr("gui.nanbin.crt_editor.status.unsaved") : tr("gui.nanbin.crt_editor.status.saved");
		c.scaledTextRight(status, minX - 6, 9, CRTEditorWidgets.FONT_SCALE_HINT, layoutDirty ? 0xFFE08A00 : CRTEditorTheme.TEXT_DIM);
		for (final CRTEditorWidgets.Widget widget : topWidgets) {
			widget.render(c);
		}
	}

	private void renderCenterToolbarAndBottom(CRTEditorWidgets.Ctx c) {
		// 选择状态文本（工具条右侧，与网页 #selection-status 一致）
		int statusX = centerToolbarRect.x + 6;
		for (final CRTEditorWidgets.Widget widget : centerWidgets) {
			if (widget.y < centerToolbarRect.y + TOOLBAR_HEIGHT) {
				statusX = Math.max(statusX, widget.x + widget.w + 8);
			}
		}
		final String status = selection.isEmpty()
				? tr("gui.nanbin.crt_editor.status.hint")
				: tr("gui.nanbin.crt_editor.status.selected", selection.size());
		final int statusMaxWidth = centerToolbarRect.x + centerToolbarRect.w - statusX - 10;
		if (statusMaxWidth >= 140) {
			c.scaledTextClipped(status, statusX + 2, centerToolbarRect.y + 9, statusMaxWidth,
					CRTEditorWidgets.FONT_SCALE_HINT, CRTEditorTheme.TEXT_DIM);
		}
		for (final CRTEditorWidgets.Widget widget : centerWidgets) {
			widget.render(c);
		}
	}

	private void renderRightPanel(CRTEditorWidgets.Ctx c) {
		if (rightPanelRect.w <= 0) {
			return;
		}
		final RightMode mode = rightMode();
		final String title;
		switch (mode) {
			case ELEMENT:
				title = tr("gui.nanbin.crt_editor.panel.element_title");
				break;
			case BATCH:
				title = tr("gui.nanbin.crt_editor.panel.batch_title");
				break;
			case SETTINGS:
			default:
				title = tr("gui.nanbin.crt_editor.panel.settings_title");
				break;
		}
		final int titleX = rightHeaderRect.x + (mode == RightMode.SETTINGS ? 8 : 26);
		c.scaledTextClipped(title, titleX, rightHeaderRect.y + 7, Math.max(20, rightHeaderRect.w - (titleX - rightHeaderRect.x) - 26),
				CRTEditorWidgets.FONT_SCALE_TITLE, CRTEditorTheme.TEXT);
		for (final CRTEditorWidgets.Widget widget : rightHeaderWidgets) {
			widget.render(c);
		}
		enableScissor(rightBodyRect);
		for (final Section section : rightSections) {
			if (section.y + section.h < rightBodyRect.y || section.y > rightBodyRect.y + rightBodyRect.h) {
				continue;
			}
			c.fillRoundBordered(section.x, section.y, section.w, section.h, 8.0F, CRTEditorTheme.PANEL_BG, CRTEditorTheme.BORDER_SOFT);
		}
		for (final TextLine line : rightTexts) {
			final int lineHeight = Math.max(CRTEditorWidgets.LINE_HEIGHT, Math.round(CRTEditorWidgets.FONT_HEIGHT * line.scale) + 1);
			if (line.y + lineHeight < rightBodyRect.y || line.y > rightBodyRect.y + rightBodyRect.h) {
				continue;
			}
			if (line.scale != CRTEditorWidgets.FONT_SCALE_BODY) {
				c.scaledTextClipped(line.text, line.x, line.y, line.maxWidth, line.scale, line.color);
			} else {
				c.textClipped(line.text, line.x, line.y, line.maxWidth, line.color);
			}
		}
		for (final CRTEditorWidgets.Widget widget : rightWidgets) {
			if (widget.y + widget.h < rightBodyRect.y - 4 || widget.y > rightBodyRect.y + rightBodyRect.h + 4) {
				continue;
			}
			widget.render(c);
		}
		disableScissor();
		drawRightScrollbar(c);
	}

	private void drawRightScrollbar(CRTEditorWidgets.Ctx c) {
		final int trackHeight = Math.max(1, rightBodyRect.h - 4);
		rightScrollTrack.set(rightPanelRect.x + rightPanelRect.w - SCROLLBAR_WIDTH - 1, rightBodyRect.y + 2, SCROLLBAR_WIDTH, trackHeight);
		rightScrollThumb.set(0, 0, 0, 0);
		if (rightPanelRect.w <= 0 || rightContentHeight <= rightBodyRect.h) {
			return;
		}
		c.fillRound(rightScrollTrack.x, rightScrollTrack.y, rightScrollTrack.w, rightScrollTrack.h, 2.5F, CRTEditorTheme.SURFACE_SOFT);
		final int maxScroll = Math.max(1, rightContentHeight - rightBodyRect.h);
		final int thumbHeight = Math.max(14, (int) ((float) trackHeight * rightBodyRect.h / Math.max(1, rightContentHeight)));
		final int thumbY = rightScrollTrack.y + (int) ((trackHeight - thumbHeight) * (rightScroll / (float) maxScroll));
		rightScrollThumb.set(rightScrollTrack.x, thumbY, SCROLLBAR_WIDTH, thumbHeight);
		c.fillRound(rightScrollThumb.x, rightScrollThumb.y, rightScrollThumb.w, rightScrollThumb.h, 2.5F, 0xFFC3CCD6);
	}

	private void drawOverlays(CRTEditorWidgets.Ctx c) {
		// 从左栏拖出的卡片幽灵
		if (draggingPaletteItem != null) {
			final int ghostWidth = 82;
			final int ghostHeight = 42;
			final int ghostX = CRTEditorWidgets.clamp((int) lastMouseX - ghostWidth / 2, 0, Math.max(0, getWidthMapped() - ghostWidth));
			final int ghostY = CRTEditorWidgets.clamp((int) lastMouseY - ghostHeight / 2, 0, Math.max(0, getHeightMapped() - ghostHeight));
			c.fillRound(ghostX - 1, ghostY - 1, ghostWidth + 2, ghostHeight + 2, 7.0F, 0xFFDDE2E9);
			c.fillRoundBordered(ghostX, ghostY, ghostWidth, ghostHeight, 6.0F, CRTEditorTheme.PANEL_BG, CRTEditorTheme.ACCENT);
			c.textCenteredClipped(draggingPaletteItem.label, ghostX + ghostWidth / 2, ghostY + 8, ghostWidth - 8, CRTEditorTheme.TEXT);
			c.textCentered(tr("gui.nanbin.crt_editor.drag.drop_hint"), ghostX + ghostWidth / 2, ghostY + 22, CRTEditorTheme.TEXT_DIM);
		}
		if (openDropdown != null) {
			openDropdown.renderPopup(c, getHeightMapped());
		}
		if (presetModalOpen) {
			drawPresetNameModal(c);
		}
		drawToast(c);
		if (c.tooltip != null) {
			drawTooltip(c, c.tooltip);
		}
	}

	private void drawToast(CRTEditorWidgets.Ctx c) {
		if (toastText == null || System.currentTimeMillis() > toastUntil) {
			return;
		}
		final TextRenderer renderer = getTextRendererMapped();
		final String text = renderer == null ? toastText : renderer.trimToWidth(toastText, 220);
		final int textWidth = c.textWidth(renderer, text);
		final int boxWidth = textWidth + 16;
		final int x = getWidthMapped() - boxWidth - 8;
		final int y = TOP_BAR_HEIGHT + 6;
		if (x < 4) {
			return;
		}
		c.fillRound(x - 1, y - 1, boxWidth + 2, 16, 6.0F, 0xFFDDE2E9);
		c.fillRoundBordered(x, y, boxWidth, 14, 5.0F, toastError ? 0xFFFEF2F2 : CRTEditorTheme.PANEL_BG, toastError ? 0xFFFECACA : CRTEditorTheme.BORDER);
		c.fill(x, y + 2, x + 2, y + 12, toastError ? CRTEditorTheme.DANGER : CRTEditorTheme.OK);
		c.text(text, x + 7, y + 3, toastError ? 0xFF991B1B : CRTEditorTheme.TEXT);
	}

	private void drawPresetNameModal(CRTEditorWidgets.Ctx c) {
		if (presetModalRect.w <= 0 || presetModalRect.h <= 0) {
			return;
		}
		final int width = getWidthMapped();
		final int height = getHeightMapped();
		// 全屏遮罩（网页 modal-mask 的 MC 近似）+ 白色卡片
		c.fill(0, 0, width, height, 0xFFE9EDF2);
		final Rect rect = presetModalRect;
		c.fillRound(rect.x - 2, rect.y - 1, rect.w + 4, rect.h + 3, 9.0F, 0xFFDDE2E9);
		c.fillRoundBordered(rect.x, rect.y, rect.w, rect.h, 8.0F, CRTEditorTheme.PANEL_BG, CRTEditorTheme.BORDER);
		c.scaledTextClipped(tr("gui.nanbin.crt_editor.preset.name_title"), rect.x + 12, rect.y + 10, rect.w - 24,
				CRTEditorWidgets.FONT_SCALE_TITLE, CRTEditorTheme.TEXT);
		for (final CRTEditorWidgets.Widget widget : modalWidgets) {
			widget.render(c);
		}
	}

	private void drawTooltip(CRTEditorWidgets.Ctx c, String text) {
		final TextRenderer renderer = getTextRendererMapped();
		final String clipped = renderer == null ? text : renderer.trimToWidth(text, 240);
		final int textWidth = c.textWidth(renderer, clipped);
		final int x = CRTEditorWidgets.clamp(lastMouseX + 8, 2, Math.max(2, getWidthMapped() - textWidth - 10));
		final int y = CRTEditorWidgets.clamp(lastMouseY - 13, 2, Math.max(2, getHeightMapped() - 14));
		c.fillRound(x - 1, y - 1, textWidth + 10, 13, 4.0F, 0xFFDDE2E9);
		c.fillRoundBordered(x, y, textWidth + 8, 11, 3.0F, CRTEditorTheme.PANEL_BG, CRTEditorTheme.BORDER);
		c.text(clipped, x + 4, y + 1, CRTEditorTheme.TEXT);
	}

	// ================================================================ 画布几何

	private final Rect rightScrollTrack = new Rect();
	private final Rect rightScrollThumb = new Rect();
	private final Rect leftScrollTrack = new Rect();
	private final Rect leftScrollThumb = new Rect();
	private boolean rightScrollbarDragging;
	private boolean leftScrollbarDragging;
	private double rowPressY;

	private void layoutCanvas() {
		if (layout.hasJsStyle()) {
			// JS 样式模式：元素编辑锁定（画布只有脚本渲染结果），清掉命中测试几何
			canvasRows.clear();
			canvasElements.clear();
			rowChromeButtons.clear();
			rowHandles.clear();
			elementToolbarButtons.clear();
			previewTransform = null;
			return;
		}
		if (previewTransform != null) {
			syncCanvasFromTransform(previewTransform);
			return;
		}
		canvasRows.clear();
		canvasElements.clear();
		rowChromeButtons.clear();
		rowHandles.clear();
		elementToolbarButtons.clear();
		paddingLeftHandle.set(0, 0, 0, 0);
		paddingRightHandle.set(0, 0, 0, 0);
		if (canvasViewportRect.isEmpty() || layout.rows.isEmpty()) {
			return;
		}
		layoutCanvasFallback();
	}

	/** 从渲染层返回的真实布局帧同步命中测试几何（行/元素屏幕矩形、行控件位置）。 */
	private void syncCanvasFromTransform(CRTSignLayoutPreview.Transform transform) {
		// 几何/选择/布局都没变时跳过重建（鼠标移动不会触发重新分配）
		final int selectionHash = selection.hashCode();
		if (transform.x == lastSyncX && transform.y == lastSyncY && transform.width == lastSyncW
				&& transform.height == lastSyncH && transform.scale == lastSyncScale
				&& currentLayoutHash == lastSyncLayoutHash && selectionHash == lastSyncSelectionHash
				&& transform.frame.rows.size() == canvasRows.size()) {
			return;
		}
		lastSyncX = transform.x;
		lastSyncY = transform.y;
		lastSyncW = transform.width;
		lastSyncH = transform.height;
		lastSyncScale = transform.scale;
		lastSyncLayoutHash = currentLayoutHash;
		lastSyncSelectionHash = selectionHash;
		canvasRows.clear();
		canvasElements.clear();
		rowChromeButtons.clear();
		rowHandles.clear();
		signScreenX = transform.x;
		signScreenY = transform.y;
		signScreenW = transform.width;
		signScreenH = transform.height;
		signContentW = Math.max(1.0F, transform.frame.width);
		signContentH = Math.max(1.0F, transform.frame.height);
		canvasScale = transform.scale;
		for (final CRTSignLayoutFrame.RowFrame rowFrame : transform.frame.rows) {
			final CanvasRow canvasRow = new CanvasRow(rowFrame.row, rowFrame.rowIndex);
			canvasRow.x = transform.toScreenX(0.0F);
			canvasRow.y = transform.toScreenY(rowFrame.y);
			canvasRow.w = transform.width;
			canvasRow.h = rowFrame.height * transform.scale;
			canvasRows.add(canvasRow);
			for (final CRTSignLayoutFrame.ElementFrame elementFrame : rowFrame.elements) {
				final CanvasElement canvasElement = new CanvasElement(elementFrame.element, rowFrame.row,
						rowFrame.rowIndex, elementFrame.index);
				canvasElement.x = transform.toScreenX(elementFrame.drawX());
				canvasElement.y = transform.toScreenY(elementFrame.y);
				canvasElement.w = elementFrame.boxWidth() * transform.scale;
				canvasElement.h = elementFrame.height * transform.scale;
				canvasElements.add(canvasElement);
			}
			buildRowChrome(canvasRow);
		}
		buildElementOverlays();
	}

	/** 渲染层不可用时的近似几何（保证界面在渲染层就绪前也能选中/拖拽）。 */
	private void layoutCanvasFallback() {
		final float contentWidth = computeContentWidth();
		final float contentHeight = Math.max(1.0F, layout.layoutHeight());
		signContentW = contentWidth;
		signContentH = contentHeight;
		final float availableWidth = Math.max(24.0F, canvasViewportRect.w - CANVAS_PADDING * 2.0F - CANVAS_RIGHT_GUTTER);
		final float availableHeight = Math.max(24.0F, canvasViewportRect.h - CANVAS_PADDING * 2.0F);
		final float fitScale = Math.min(availableWidth / contentWidth, availableHeight / contentHeight);
		canvasScale = Math.max(0.005F, fitScale * canvasZoom);
		signScreenW = contentWidth * canvasScale;
		signScreenH = contentHeight * canvasScale;
		final float maxScrollX = Math.max(0.0F, (signScreenW - availableWidth) / 2.0F);
		final float maxScrollY = Math.max(0.0F, (signScreenH - availableHeight) / 2.0F);
		canvasScrollX = (float) CRTEditorWidgets.clamp(canvasScrollX, -maxScrollX, maxScrollX);
		canvasScrollY = (float) CRTEditorWidgets.clamp(canvasScrollY, -maxScrollY, maxScrollY);
		signScreenX = canvasViewportRect.x + (canvasViewportRect.w - CANVAS_RIGHT_GUTTER) / 2.0F - signScreenW / 2.0F + canvasScrollX;
		signScreenY = canvasViewportRect.y + canvasViewportRect.h / 2.0F - signScreenH / 2.0F + canvasScrollY;

		final float rowHeight = (float) layout.rowHeight * canvasScale;
		final boolean alignEnabled = layout.widthMode == CRTSignLayout.WidthMode.FIXED;
		for (int rowIndex = 0; rowIndex < layout.rows.size(); rowIndex++) {
			final CRTSignLayout.Row row = layout.rows.get(rowIndex);
			final CanvasRow canvasRow = new CanvasRow(row, rowIndex);
			canvasRow.x = signScreenX;
			canvasRow.y = signScreenY + rowIndex * rowHeight;
			canvasRow.w = signScreenW;
			canvasRow.h = rowHeight;
			canvasRows.add(canvasRow);

			final int elementCount = row.elements.size();
			final float[] widths = new float[elementCount];
			float centerTotal = 0.0F;
			for (int i = 0; i < elementCount; i++) {
				final CRTSignElement element = row.elements.get(i);
				widths[i] = approxElementWidth(element, layout.rowHeight) * canvasScale;
				if (alignEnabled && element.elementAlign == CRTSignElement.ElementAlign.CENTER) {
					centerTotal += widths[i];
				}
			}
			float leftX = signScreenX;
			float rightX = signScreenX + signScreenW;
			float centerX = signScreenX + (signScreenW - centerTotal) / 2.0F;
			for (int i = 0; i < elementCount; i++) {
				final CRTSignElement element = row.elements.get(i);
				final CRTSignElement.ElementAlign lane = alignEnabled ? element.elementAlign : CRTSignElement.ElementAlign.LEFT;
				final CanvasElement canvasElement = new CanvasElement(element, row, rowIndex, i);
				switch (lane) {
					case RIGHT:
						rightX -= widths[i];
						canvasElement.x = rightX;
						break;
					case CENTER:
						canvasElement.x = centerX;
						centerX += widths[i];
						break;
					case LEFT:
					default:
						canvasElement.x = leftX;
						leftX += widths[i];
						break;
				}
				canvasElement.y = canvasRow.y;
				canvasElement.w = Math.max(1.0F, widths[i]);
				canvasElement.h = canvasRow.h;
				canvasElements.add(canvasElement);
			}
			buildRowChrome(canvasRow);
		}
		buildElementOverlays();
	}

	// ================================================================ 浮动元素工具条 / 内边距拖拽（§3.2.6 / §6.6 / §6.7）

	/**
	 * 计算选中元素的浮动工具条按钮与左右内边距拖拽点（屏幕坐标）。
	 * 只在恰好选中 1 个元素时出现；工具条优先放在元素下方，放不下再放到上方，并夹在画布可视区内。
	 */
	private void buildElementOverlays() {
		elementToolbarButtons.clear();
		paddingLeftHandle.set(0, 0, 0, 0);
		paddingRightHandle.set(0, 0, 0, 0);
		if (selection.size() != 1 || canvasViewportRect.isEmpty()) {
			return;
		}
		final CanvasElement target = canvasElementById(primaryId);
		if (target == null) {
			return;
		}
		final int toolbarWidth = TOOLBAR_PADDING * 2 + 4 * TOOLBAR_BUTTON_SIZE + 3 * TOOLBAR_GAP;
		final int toolbarHeight = TOOLBAR_PADDING * 2 + TOOLBAR_BUTTON_SIZE;
		int toolbarX = Math.round(target.x + target.w / 2.0F - toolbarWidth / 2.0F);
		toolbarX = CRTEditorWidgets.clamp(toolbarX, canvasViewportRect.x + 2,
				canvasViewportRect.x + canvasViewportRect.w - 2 - toolbarWidth);
		int toolbarY = Math.round(target.y + target.h + 3.0F);
		if (toolbarY + toolbarHeight > canvasViewportRect.y + canvasViewportRect.h - 2) {
			toolbarY = Math.round(target.y - toolbarHeight - 3.0F);
		}
		toolbarY = CRTEditorWidgets.clamp(toolbarY, canvasViewportRect.y + 2,
				canvasViewportRect.y + canvasViewportRect.h - 2 - toolbarHeight);
		for (int i = 0; i < 4; i++) {
			final Rect rect = new Rect(toolbarX + TOOLBAR_PADDING + i * (TOOLBAR_BUTTON_SIZE + TOOLBAR_GAP),
					toolbarY + TOOLBAR_PADDING, TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE);
			elementToolbarButtons.add(new ElementToolbarButton(rect, i));
		}
		if (target.element.type == CRTSignElementType.SPACE) {
			// 空白占位没有内边距编辑器（网页 §3.3.2：space 无 padding 字段）
			return;
		}
		final int handleHeight = 14;
		int handleY = Math.round(target.y + target.h / 2.0F - handleHeight / 2.0F);
		handleY = CRTEditorWidgets.clamp(handleY, canvasViewportRect.y + 2,
				canvasViewportRect.y + canvasViewportRect.h - 2 - handleHeight);
		final int leftX = CRTEditorWidgets.clamp(Math.round(target.x) - 3, canvasViewportRect.x + 1,
				canvasViewportRect.x + canvasViewportRect.w - 7);
		final int rightX = CRTEditorWidgets.clamp(Math.round(target.x + target.w) - 2, canvasViewportRect.x + 1,
				canvasViewportRect.x + canvasViewportRect.w - 7);
		paddingLeftHandle.set(leftX, handleY, 5, handleHeight);
		paddingRightHandle.set(rightX, handleY, 5, handleHeight);
	}

	@Nullable
	private CanvasElement canvasElementById(@Nullable String elementId) {
		if (elementId == null) {
			return null;
		}
		for (final CanvasElement element : canvasElements) {
			if (element.element.id.equals(elementId)) {
				return element;
			}
		}
		return null;
	}

	/** 工具条/拖拽点是否处于可交互状态（拖拽元素/从左栏拖放时暂时隐藏，避免误触）。 */
	private boolean elementOverlaysActive() {
		return selection.size() == 1 && !elementDragging && !elementPressActive && draggingPaletteItem == null
				&& pressedPaletteItem == null && !paddingDragActive;
	}

	private void drawElementOverlays(CRTEditorWidgets.Ctx c) {
		final CanvasElement target = canvasElementById(primaryId);
		if (target == null || canvasViewportRect.isEmpty()) {
			return;
		}
		if (!elementOverlaysActive()) {
			// 拖拽中：仍然把内边距拖拽中的高亮画出来，其余隐藏
			if (paddingDragActive) {
				drawPaddingHandles(c, target);
			}
			return;
		}
		drawPaddingHandles(c, target);
		drawElementToolbar(c, target);
	}

	private void drawPaddingHandles(CRTEditorWidgets.Ctx c, CanvasElement target) {
		drawPaddingHandle(c, paddingLeftHandle, target, -1);
		drawPaddingHandle(c, paddingRightHandle, target, 1);
	}

	private void drawPaddingHandle(CRTEditorWidgets.Ctx c, Rect rect, CanvasElement target, int side) {
		if (rect.isEmpty()) {
			return;
		}
		final boolean dragging = paddingDragActive && paddingDragSide == side
				&& target.element.id.equals(paddingDragElementId);
		final boolean hover = !dragging && rect.contains(lastMouseX, lastMouseY) && elementOverlaysActive();
		final int background = dragging ? CRTEditorTheme.ACCENT : (hover ? CRTEditorTheme.ACCENT_SOFT : CRTEditorTheme.CONTROL_BG);
		final int border = dragging || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.SELECTION_SOFT;
		c.fillRoundBordered(rect.x, rect.y, rect.w, rect.h, 2.0F, background, border);
		final int grip = dragging ? 0xFFFFFFFF : CRTEditorTheme.ACCENT;
		for (int i = 0; i < 3; i++) {
			final int gy = rect.y + 3 + i * 3;
			c.fill(rect.x + 1, gy, rect.x + rect.w - 1, gy + 1, grip);
		}
		if (hover) {
			final float value = side < 0 ? target.element.paddingLeft : target.element.paddingRight;
			c.setTooltip(tr(side < 0 ? "gui.nanbin.crt_editor.toolbar.padding_left_tip"
					: "gui.nanbin.crt_editor.toolbar.padding_right_tip", String.format(Locale.ROOT, "%.2f", value)));
		}
	}

	private void drawElementToolbar(CRTEditorWidgets.Ctx c, CanvasElement target) {
		if (elementToolbarButtons.isEmpty()) {
			return;
		}
		final Rect first = elementToolbarButtons.get(0).rect;
		final Rect last = elementToolbarButtons.get(elementToolbarButtons.size() - 1).rect;
		final int x = first.x - TOOLBAR_PADDING;
		final int y = first.y - TOOLBAR_PADDING;
		final int w = last.x + last.w + TOOLBAR_PADDING - x;
		final int h = first.h + TOOLBAR_PADDING * 2;
		// 网页 #element-toolbar：白底、12px 圆角、投影
		c.fillRound(x - 1, y - 1, w + 2, h + 2, 7.0F, 0xFFDDE2E9);
		c.fillRoundBordered(x, y, w, h, 6.0F, 0xFFFFFFFF, CRTEditorTheme.BORDER);
		for (final ElementToolbarButton button : elementToolbarButtons) {
			final boolean enabled = toolbarActionEnabled(button.action);
			final boolean hover = enabled && button.rect.contains(lastMouseX, lastMouseY);
			final boolean danger = button.action == TOOLBAR_DELETE;
			if (hover) {
				c.fillRound(button.rect.x, button.rect.y, button.rect.w, button.rect.h, 3.0F,
						danger ? 0xFFFBE4E4 : CRTEditorTheme.ACCENT_SOFT);
			}
			final int color = !enabled ? 0xFFC8CFD8 : (hover ? (danger ? CRTEditorTheme.DANGER : CRTEditorTheme.ACCENT) : (danger ? 0xFFB91C1C : CRTEditorTheme.TEXT_DIM));
			c.textCentered(toolbarActionGlyph(button.action), button.rect.x + button.rect.w / 2,
					button.rect.y + (button.rect.h - CRTEditorWidgets.FONT_HEIGHT) / 2 + 1, color);
			if (hover) {
				c.setTooltip(toolbarActionTooltip(button.action));
			}
		}
	}

	private static String toolbarActionGlyph(int action) {
		switch (action) {
			case TOOLBAR_MOVE_LEFT:
				return "←";
			case TOOLBAR_MOVE_RIGHT:
				return "→";
			case TOOLBAR_COPY:
				return "复";
			case TOOLBAR_DELETE:
			default:
				return "✕";
		}
	}

	private static String toolbarActionTooltip(int action) {
		switch (action) {
			case TOOLBAR_MOVE_LEFT:
				return tr("gui.nanbin.crt_editor.toolbar.move_left");
			case TOOLBAR_MOVE_RIGHT:
				return tr("gui.nanbin.crt_editor.toolbar.move_right");
			case TOOLBAR_COPY:
				return tr("gui.nanbin.crt_editor.toolbar.copy");
			case TOOLBAR_DELETE:
			default:
				return tr("gui.nanbin.crt_editor.toolbar.delete");
		}
	}

	private boolean toolbarActionEnabled(int action) {
		switch (action) {
			case TOOLBAR_MOVE_LEFT:
				return canMoveSelectionBy(-1);
			case TOOLBAR_MOVE_RIGHT:
				return canMoveSelectionBy(1);
			case TOOLBAR_COPY:
			case TOOLBAR_DELETE:
			default:
				return !selection.isEmpty();
		}
	}

	private void performElementToolbarAction(int action) {
		// 全部走既有编辑管线：pushHistory / markEdited / saveNow 都在 moveSelectionBy 等方法内部
		switch (action) {
			case TOOLBAR_MOVE_LEFT:
				moveSelectionBy(-1);
				break;
			case TOOLBAR_MOVE_RIGHT:
				moveSelectionBy(1);
				break;
			case TOOLBAR_COPY:
				copySelection(false);
				break;
			case TOOLBAR_DELETE:
			default:
				deleteSelection();
				break;
		}
	}

	private void beginPaddingDrag(int side, double mouseX) {
		final CRTSignElement element = primaryElement();
		if (element == null || element.type == CRTSignElementType.SPACE) {
			return;
		}
		paddingDragActive = true;
		paddingDragSide = side;
		paddingDragElementId = element.id;
		paddingDragLastX = mouseX;
		paddingDragAccumulator = 0.0;
		blurFocused();
		breakHistoryGroup();
		// 只在拖动开始时压一次快照，拖拽过程中的所有步进合并为一步撤销
		pushHistory(element.id + ":padding-scrub:" + side);
	}

	private void applyPaddingDrag(double mouseX) {
		paddingDragAccumulator += mouseX - paddingDragLastX;
		paddingDragLastX = mouseX;
		final int steps = (int) (paddingDragAccumulator / PADDING_SCRUB_PIXELS_PER_STEP);
		if (steps == 0) {
			return;
		}
		paddingDragAccumulator -= steps * PADDING_SCRUB_PIXELS_PER_STEP;
		final CRTSignLayout.ElementRef ref = paddingDragElementId == null ? null : layout.findElement(paddingDragElementId);
		if (ref == null) {
			paddingDragActive = false;
			paddingDragElementId = null;
			return;
		}
		final float current = paddingDragSide < 0 ? ref.element.paddingLeft : ref.element.paddingRight;
		final float next = (float) CRTEditorWidgets.clamp(current + steps * PADDING_SCRUB_STEP, 0.0, 8.0);
		if (Float.compare(current, next) == 0) {
			return;
		}
		if (paddingDragSide < 0) {
			ref.element.paddingLeft = next;
		} else {
			ref.element.paddingRight = next;
		}
		ref.element.paddingAuto = false;
		markEdited(false);
	}

	private void endPaddingDrag() {
		if (!paddingDragActive) {
			return;
		}
		paddingDragActive = false;
		paddingDragElementId = null;
		paddingDragSide = 0;
		breakHistoryGroup();
		saveNow();
	}

	private void buildRowChrome(CanvasRow row) {
		if (row.y + row.h < canvasViewportRect.y || row.y > canvasViewportRect.y + canvasViewportRect.h) {
			return;
		}
		final int buttonSize = 14;
		final int gap = 1;
		final int totalWidth = buttonSize * 4 + gap * 3;
		// 固定放在画布右侧 gutter：宽牌面时也不会盖住牌面内容
		final int buttonX = canvasViewportRect.x + canvasViewportRect.w - CANVAS_RIGHT_GUTTER + 4;
		int buttonY = Math.round(row.y + row.h / 2.0F - buttonSize / 2.0F);
		buttonY = CRTEditorWidgets.clamp(buttonY, canvasViewportRect.y + 2, canvasViewportRect.y + canvasViewportRect.h - buttonSize - 2);
		for (int i = 0; i < 4; i++) {
			final Rect rect = new Rect(buttonX + i * (buttonSize + gap), buttonY, buttonSize, buttonSize);
			rowChromeButtons.add(new RowChromeButton(rect, row.index, i));
		}
		if (layout.rows.size() > 1) {
			final int handleX = canvasViewportRect.x + canvasViewportRect.w - CANVAS_RIGHT_GUTTER - 12;
			int handleY = Math.round(row.y + row.h / 2.0F - 8);
			handleY = CRTEditorWidgets.clamp(handleY, canvasViewportRect.y + 2, canvasViewportRect.y + canvasViewportRect.h - 16);
			rowHandles.add(new RowHandle(new Rect(handleX, handleY, 12, 16), row.index));
		}
	}

	private float computeContentWidth() {
		if (layout.widthMode == CRTSignLayout.WidthMode.FIXED) {
			return layout.layoutWidth();
		}
		float widest = Math.max(1.0F, layout.rowHeight);
		for (final CRTSignLayout.Row row : layout.rows) {
			float sum = 0.0F;
			for (final CRTSignElement element : row.elements) {
				sum += approxElementWidth(element, layout.rowHeight);
			}
			widest = Math.max(widest, sum);
		}
		return Math.round(widest);
	}

	/** 线框画布的元素宽度近似（网页 render.js 的 metrics.width 简化版，只用于编辑期命中测试/拖拽）。 */
	private float approxElementWidth(CRTSignElement element, float rowHeight) {
		final float paddingLeft = element.paddingLeft * rowHeight;
		final float paddingRight = element.paddingRight * rowHeight;
		final float paddingTop = element.paddingTop * rowHeight;
		final float paddingBottom = element.paddingBottom * rowHeight;
		final float size = Math.max(1.0F, rowHeight - paddingTop - paddingBottom);
		switch (element.type) {
			case SPACE:
				return Math.max(1.0F, rowHeight * element.widthRatio);
			case ARROW:
			case ICON:
				return size + paddingLeft + paddingRight;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT: {
				final float ratio = element.type == CRTSignElementType.SMALL_BILINGUAL_TEXT ? 0.75F : 1.0F;
				final float zhWidth = approxTextWidth(element.textZh, size * 0.6325F * ratio);
				final float enWidth = approxTextWidth(element.textEn, size * 0.35625F * ratio);
				return Math.max(zhWidth, enWidth) + paddingLeft + paddingRight;
			}
			case BIG_NUMBER:
				return approxTextWidth(element.text, size * 1.3F) + paddingLeft + paddingRight;
			case NUMBER_LINE: {
				float width = 0.0F;
				final int lineCount = element.align == CRTSignElement.TextAlign.RIGHT ? Math.min(1, element.lines.size()) : element.lines.size();
				for (int i = 0; i < lineCount; i++) {
					final CRTSignElement.LineItem line = element.lines.get(i);
					width += size * 0.357F + approxTextWidth(line.number, size * 0.7F) + size * 0.05F;
				}
				width += approxTextWidth("号线", size * 0.6325F * 0.75F) + size * 0.05F;
				return width + paddingLeft + paddingRight;
			}
			case TEXT_LINE: {
				final float stripe = size * 0.3F;
				final float label = approxTextWidth("Line", size * 0.35625F);
				final float text = approxTextWidth(element.text, size * 0.6325F);
				return paddingLeft + stripe + size * 0.08F + text + size * 0.08F + label + paddingRight;
			}
			case ENTRANCE: {
				final float code = approxTextWidth(element.code, size * 0.6325F);
				final float label = approxTextWidth("出入口", size * 0.6325F) + size * 0.2F + approxTextWidth("Entrance", size * 0.35625F);
				return paddingLeft + code + size * 0.05F + label + paddingRight;
			}
			case EXIT: {
				final float code = approxTextWidth(element.code, size * 0.6325F);
				final float label = approxTextWidth("出口", size * 0.6325F) + size * 0.2F + approxTextWidth("Exit", size * 0.35625F);
				return paddingLeft + code + size * 0.05F + label + paddingRight;
			}
						case ROUTE_INFO:
				try {
					return CRTSignLayoutPainter.standaloneWidth(element, rowHeight, data);
				} catch (Throwable ignored) {
					return paddingLeft + approxTextWidth("1", size * 0.62F) + approxTextWidth("号线", size * 0.26F) + paddingRight;
				}
case EXIT_INFO:
				// 出口信息条的宽度取决于 MTR 出口名称与目的地，直接用渲染端的真实度量（文本测量有缓存）
				try {
					return CRTSignLayoutPainter.standaloneWidth(element, rowHeight, data);
				} catch (Throwable ignored) {
					return paddingLeft + approxTextWidth("1/2", size * 0.55F) + paddingRight;
				}
			default:
				return size + paddingLeft + paddingRight;
		}
	}

	private static float approxTextWidth(@Nullable String text, float fontSize) {
		if (text == null || text.isEmpty()) {
			return 0.0F;
		}
		float width = 0.0F;
		for (int i = 0; i < text.length(); i++) {
			final char chr = text.charAt(i);
			width += chr < 0x2E80 ? fontSize * 0.55F : fontSize;
		}
		return width;
	}

	private void updateHover() {
		hoverRowIndex = -1;
		hoverElementId = null;
		if (!canvasViewportRect.contains(lastMouseX, lastMouseY) || canvasRows.isEmpty()) {
			return;
		}
		final int rowIndex = rowIndexAtY(lastMouseY);
		if (rowIndex >= 0) {
			hoverRowIndex = rowIndex;
		}
		final CanvasElement element = elementAt(lastMouseX, lastMouseY);
		if (element != null) {
			hoverElementId = element.element.id;
			hoverRowIndex = element.rowIndex;
		}
	}

	@Nullable
	private CanvasElement elementAt(double mouseX, double mouseY) {
		for (int i = canvasElements.size() - 1; i >= 0; i--) {
			final CanvasElement element = canvasElements.get(i);
			if (mouseX >= element.x && mouseX < element.x + element.w && mouseY >= element.y && mouseY < element.y + element.h) {
				return element;
			}
		}
		return null;
	}

	private int rowIndexAtY(double mouseY) {
		if (canvasRows.isEmpty()) {
			return -1;
		}
		if (mouseY < signScreenY) {
			return 0;
		}
		if (mouseY >= signScreenY + signScreenH) {
			return layout.rows.size() - 1;
		}
		final float rowHeight = signScreenH / Math.max(1.0F, layout.rows.size());
		final int index = (int) ((mouseY - signScreenY) / rowHeight);
		return CRTEditorWidgets.clamp(index, 0, layout.rows.size() - 1);
	}

	private int rowBoundaryAt(double mouseY) {
		if (canvasRows.isEmpty()) {
			return 0;
		}
		final float rowHeight = signScreenH / Math.max(1.0F, layout.rows.size());
		final int boundary = Math.round((float) ((mouseY - signScreenY) / Math.max(1.0F, rowHeight)));
		return CRTEditorWidgets.clamp(boundary, 0, layout.rows.size());
	}

	// ================================================================ 画布绘制

	/** JS 样式数据选择屏幕（等价旧格子编辑器的 JS 数据界面，保存回布局的 jsSelectedIds）。 */
	private void openJsDataScreen() {
		if (!layout.hasJsStyle()) {
			toast(tr("gui.nanbin.crt_editor.js.need_style"), true);
			return;
		}
		if (host == null) {
			toast(tr("gui.nanbin.crt_editor.js.need_host"), true);
			return;
		}
		blurFocused();
		final LongAVLTreeSet ids = new LongAVLTreeSet();
		if (layout.jsSelectedIds.isEmpty()) {
			for (final Long id : layout.collectBindingIds()) {
				ids.add(id);
			}
		} else {
			for (final Long id : layout.jsSelectedIds) {
				ids.add(id);
			}
		}
		final String scriptId = layout.jsStyleId;
		MinecraftClient.getInstance().openScreen(new Screen(new JSSignDataScreen(this, host.getSignPos(), ids, () -> {
			// 先快照再改，保证撤销能回到改动前的数据选择
			pushHistory(null);
			layout.jsSelectedIds.clear();
			layout.jsSelectedIds.addAll(ids);
			markEdited(true);
			saveNow();
			widgetsDirty = true;
			toast(tr("gui.nanbin.crt_editor.js.data_saved"));
		}, scriptId)));
	}

	/** JS 整牌样式画布：按脚本烘焙整张贴图绘制；元素编辑锁定（与旧 JS 编辑一致）。 */

	/** 当前牌面实际格数：host 优先，未知时按布局宽/行高估算。 */
	private int resolvePreviewSignCells() {
		final int fromHost = host == null ? 0 : host.getSignCells();
		if (fromHost > 0) {
			return fromHost;
		}
		return Math.max(1, Math.round(layout.layoutWidth() / Math.max(1.0F, layout.rowHeight)));
	}

	private void drawJsCanvas(CRTEditorWidgets.Ctx c) {
		previewTransform = null;
		canvasRows.clear();
		canvasElements.clear();
		rowChromeButtons.clear();
		rowHandles.clear();
		elementToolbarButtons.clear();
		signContentW = Math.max(1.0F, layout.layoutWidth());
		signContentH = Math.max(1.0F, layout.layoutHeight());
		final int jsCells = resolvePreviewSignCells();
		if (CRTJsLayoutBake.isJsStyle(layout)) {
			// JS 整牌样式：内容区比例必须等于牌面（cells×0.5 : 0.5），否则预览会被拉伸/世界会留边
			final float unit = Math.max(1.0F, layout.rowHeight);
			signContentW = Math.max(1, jsCells) * unit;
			signContentH = unit;
		}
		final float availableWidth = Math.max(24.0F, canvasViewportRect.w - CANVAS_PADDING * 2.0F - CANVAS_RIGHT_GUTTER);
		final float availableHeight = Math.max(24.0F, canvasViewportRect.h - CANVAS_PADDING * 2.0F);
		final float fitScale = Math.min(availableWidth / signContentW, availableHeight / signContentH);
		canvasScale = Math.max(0.005F, fitScale * canvasZoom);
		signScreenW = signContentW * canvasScale;
		signScreenH = signContentH * canvasScale;
		final float maxScrollX = Math.max(0.0F, (signScreenW - availableWidth) / 2.0F);
		final float maxScrollY = Math.max(0.0F, (signScreenH - availableHeight) / 2.0F);
		canvasScrollX = (float) CRTEditorWidgets.clamp(canvasScrollX, -maxScrollX, maxScrollX);
		canvasScrollY = (float) CRTEditorWidgets.clamp(canvasScrollY, -maxScrollY, maxScrollY);
		signScreenX = canvasViewportRect.x + (canvasViewportRect.w - CANVAS_RIGHT_GUTTER) / 2.0F - signScreenW / 2.0F + canvasScrollX;
		signScreenY = canvasViewportRect.y + canvasViewportRect.h / 2.0F - signScreenH / 2.0F + canvasScrollY;
		final String layoutJson = layout.toJson();
		// 朝向必须世界/预览一致：否则按朝向绘制内容的 JS 脚本会在两边显示不同
		final Direction previewFacing = host == null ? Direction.NORTH : host.getFacing();
		final JSSignTextureBaker.Baked baked = CRTJsLayoutBake.bake(layout, data, cacheKey + ":facing:" + previewFacing.asRotation(),
				layoutJson == null ? 0L : layoutJson.hashCode(), previewFacing, jsCells);
		enableScissor(canvasViewportRect);
		// 与世界渲染一致：先画整牌底色，再叠 JS 烘焙贴图（避免脚本背景之外的透明区域在预览里显示成灰色画布）
		c.fill(Math.round(signScreenX), Math.round(signScreenY), Math.round(signScreenX + signScreenW),
				Math.round(signScreenY + signScreenH), layout.backgroundColor | 0xFF000000);
		if (baked != null) {
			drawBakedTexture(c, baked, Math.round(signScreenX), Math.round(signScreenY),
					Math.max(1, Math.round(signScreenW)), Math.max(1, Math.round(signScreenH)));
		} else {
			c.fill(Math.round(signScreenX), Math.round(signScreenY), Math.round(signScreenX + signScreenW),
					Math.round(signScreenY + signScreenH), layout.backgroundColor | 0xFF000000);
			c.scaledTextCenteredClipped(tr("gui.nanbin.crt_editor.js.waiting"),
					canvasViewportRect.x + (canvasViewportRect.w - CANVAS_RIGHT_GUTTER) / 2,
					Math.round(signScreenY + signScreenH / 2) - 4,
					Math.max(40, canvasViewportRect.w - 24 - CANVAS_RIGHT_GUTTER),
					CRTEditorWidgets.FONT_SCALE_HINT, 0xFF9AA3AF);
		}
		drawPlateChrome(c);
		final String badge = tr("gui.nanbin.crt_editor.js.badge", tr(JSSignConfig.getDisplayNameKey(layout.jsStyleId)));
		final int badgeTextWidth = c.textWidth(getTextRendererMapped(), badge);
		final int badgeX = canvasViewportRect.x + 6;
		final int badgeY = canvasViewportRect.y + 6;
		c.fillRound(badgeX, badgeY, badgeTextWidth + 12, 14, 4.0F, 0xFF1F2733);
		c.text(badge, badgeX + 6, badgeY + 3, 0xFFFFFFFF);
		disableScissor();
	}

	private void drawCanvas(CRTEditorWidgets.Ctx c) {
		if (canvasViewportRect.isEmpty()) {
			return;
		}
		c.fill(canvasViewportRect.x, canvasViewportRect.y, canvasViewportRect.x + canvasViewportRect.w,
				canvasViewportRect.y + canvasViewportRect.h, CRTEditorTheme.CANVAS_BG);
		if (layout.rows.isEmpty()) {
			previewTransform = null;
			return;
		}
		if (layout.hasJsStyle()) {
			drawJsCanvas(c);
			return;
		}
		// 先用渲染层布局帧确定牌面尺寸（预览据此等比缩放），再交给渲染层画贴图
		final String layoutJson = layout.toJson();
		currentLayoutHash = layoutJson == null ? 0 : layoutJson.hashCode();
		final CRTSignLayoutFrame frame = CRTSignLayoutPainter.layoutCached(layout, data, cacheKey, currentLayoutHash);
		signContentW = Math.max(1.0F, frame.width);
		signContentH = Math.max(1.0F, frame.height);
		final float availableWidth = Math.max(24.0F, canvasViewportRect.w - CANVAS_PADDING * 2.0F - CANVAS_RIGHT_GUTTER);
		final float availableHeight = Math.max(24.0F, canvasViewportRect.h - CANVAS_PADDING * 2.0F);
		final float fitScale = Math.min(availableWidth / signContentW, availableHeight / signContentH);
		canvasScale = Math.max(0.005F, fitScale * canvasZoom);
		final float displayWidth = signContentW * canvasScale;
		final float displayHeight = signContentH * canvasScale;
		final float maxScrollX = Math.max(0.0F, (displayWidth - availableWidth) / 2.0F);
		final float maxScrollY = Math.max(0.0F, (displayHeight - availableHeight) / 2.0F);
		canvasScrollX = (float) CRTEditorWidgets.clamp(canvasScrollX, -maxScrollX, maxScrollX);
		canvasScrollY = (float) CRTEditorWidgets.clamp(canvasScrollY, -maxScrollY, maxScrollY);
		// 把「缩放后的画布区域」当作虚拟矩形传给预览：预览会等比居中，等效于 fit × canvasZoom
		final float virtualWidth = availableWidth * canvasZoom;
		final float virtualHeight = availableHeight * canvasZoom;
		final float virtualX = canvasViewportRect.x + (canvasViewportRect.w - CANVAS_RIGHT_GUTTER) / 2.0F + canvasScrollX - virtualWidth / 2.0F;
		final float virtualY = canvasViewportRect.y + canvasViewportRect.h / 2.0F + canvasScrollY - virtualHeight / 2.0F;
		enableScissor(canvasViewportRect);
		previewTransform = CRTSignLayoutPreview.draw(c.graphics, virtualX, virtualY, virtualWidth, virtualHeight,
				layout, data, cacheKey, selection.isEmpty() ? null : primaryId,
				elementDragging || draggingPaletteItem != null ? -1 : hoverRowIndex, true);
		disableScissor();
		if (previewTransform == null) {
			drawCanvasFallback(c);
			return;
		}
		syncCanvasFromTransform(previewTransform);
		enableScissor(canvasViewportRect);
		drawPlateChrome(c);
		drawCanvasSelectionOverlays(c);
		drawDropIndicators(c);
		drawRowChrome(c);
		drawElementOverlays(c);
		if (layout.isBlank()) {
			drawCanvasEmptyState(c);
		}
		disableScissor();
	}

	/** 预览贴图之上的编辑器叠加层：多选框、hover 框、空白占位提示。 */
	/** 牌面投影 + 描边：白色牌面落在浅灰画布上，必须显式描边才看得出边界。 */
	private void drawPlateChrome(CRTEditorWidgets.Ctx c) {
		final int x = Math.round(signScreenX);
		final int y = Math.round(signScreenY);
		final int w = Math.max(1, Math.round(signScreenW));
		final int h = Math.max(1, Math.round(signScreenH));
		// 右下浅色投影（不用半透明色：MC 自绘矩形对 alpha 不可靠）
		c.fill(x + 2, y + h + 1, x + w + 3, y + h + 3, 0xFFD5DBE3);
		c.fill(x + w + 1, y + 6, x + w + 3, y + h + 3, 0xFFD5DBE3);
		// 1px 描边
		c.border(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFC3CCD8);
	}

	private void drawCanvasSelectionOverlays(CRTEditorWidgets.Ctx c) {
		for (final CanvasElement element : canvasElements) {
			final boolean selected = isSelected(element.element.id);
			if (selected && !element.element.id.equals(primaryId)) {
				drawSelectionOutline(c, element);
			} else if (!selected && element.element.id.equals(hoverElementId)) {
				c.border(Math.round(element.x), Math.round(element.y), Math.round(element.x + element.w), Math.round(element.y + element.h), 0xFF7AA3F0);
			}
			if (element.element.type == CRTSignElementType.SPACE && element.element.id.equals(hoverElementId)) {
				c.setTooltip(tr("gui.nanbin.crt_editor.canvas.space_tip"));
			}
		}
	}

	/** 空态提示：主文案 1.35×、副文案 0.85×（网页 #empty-hint 22px 的 MC 近似）。 */
	private void drawCanvasEmptyState(CRTEditorWidgets.Ctx c) {
		final int centerX = canvasViewportRect.x + canvasViewportRect.w / 2;
		final int centerY = canvasViewportRect.y + canvasViewportRect.h / 2;
		final int maxWidth = Math.max(20, canvasViewportRect.w - 16);
		c.scaledTextCenteredClipped(tr("gui.nanbin.crt_editor.canvas.empty"), centerX, centerY - 8, maxWidth,
				CRTEditorWidgets.FONT_SCALE_TITLE, 0xFF9AA3AF);
		c.scaledTextCenteredClipped(tr("gui.nanbin.crt_editor.canvas.empty_sub"), centerX, centerY + 7, maxWidth,
				CRTEditorWidgets.FONT_SCALE_HINT, 0xFFB0B7C1);
	}

	/** 渲染层不可用时的线框画布（近似布局）。 */
	private void drawCanvasFallback(CRTEditorWidgets.Ctx c) {
		if (canvasViewportRect.isEmpty() || canvasRows.isEmpty()) {
			return;
		}
		enableScissor(canvasViewportRect);
		final int signX = Math.round(signScreenX);
		final int signY = Math.round(signScreenY);
		final int signWidth = Math.max(1, Math.round(signScreenW));
		final int signHeight = Math.max(1, Math.round(signScreenH));
		// 牌面阴影 + 底板
		c.fillRound(signX - 2, signY - 1, signWidth + 4, signHeight + 4, 4.0F, 0xFFDCE1E8);
		c.fillRound(signX, signY, signWidth, signHeight, 3.0F, layout.backgroundColor);
		// 行底色 + 分隔线
		for (final CanvasRow row : canvasRows) {
			if (row.row.backgroundColor != null) {
				c.fill(Math.round(row.x), Math.round(row.y), Math.round(row.x + row.w), Math.round(row.y + row.h), row.row.backgroundColor);
			}
			final int rowTop = Math.round(row.y);
			if (row.index < canvasRows.size() - 1) {
				c.fill(Math.round(row.x), rowTop + Math.round(row.h) - 1, Math.round(row.x + row.w), rowTop + Math.round(row.h), 0xFFE6E9EE);
			}
		}
		// 悬停行带（网页 .row-band）
		if (hoverRowIndex >= 0 && hoverRowIndex < canvasRows.size() && !elementDragging && draggingPaletteItem == null) {
			final CanvasRow row = canvasRows.get(hoverRowIndex);
			c.fill(Math.round(row.x), Math.round(row.y), Math.round(row.x + row.w), Math.round(row.y + row.h), 0xFFEFF4FF);
		}
		// 元素
		for (final CanvasElement element : canvasElements) {
			drawCanvasElement(c, element);
		}
		drawCanvasFrame(c);
		drawDropIndicators(c);
		drawRowChrome(c);
		drawElementOverlays(c);
		// 空态
		if (layout.isBlank()) {
			drawCanvasEmptyState(c);
		}
		disableScissor();
	}

	private void drawCanvasElement(CRTEditorWidgets.Ctx c, CanvasElement canvasElement) {
		final CRTSignElement element = canvasElement.element;
		final int x1 = Math.round(canvasElement.x) + 1;
		final int y1 = Math.round(canvasElement.y) + 1;
		final int x2 = Math.round(canvasElement.x + canvasElement.w) - 1;
		final int y2 = Math.round(canvasElement.y + canvasElement.h) - 1;
		if (x2 <= x1 || y2 <= y1) {
			return;
		}
		final boolean selected = isSelected(element.id);
		final boolean hovered = element.id.equals(hoverElementId);
		if (element.type == CRTSignElementType.SPACE) {
			c.dashedRect(x1, y1, x2, y2, selected ? CRTEditorTheme.ACCENT : 0xFF8FB3F2);
			if (selected) {
				drawSelectionOutline(c, canvasElement);
			}
			if (hovered) {
				c.setTooltip(tr("gui.nanbin.crt_editor.canvas.space_tip"));
			}
			return;
		}
		final int fill = element.backgroundColor != null ? element.backgroundColor : 0xFFEDF2FE;
		c.fillRound(x1, y1, x2 - x1, y2 - y1, 2.0F, fill);
		c.border(x1, y1, x2, y2, 0xFFBBD0F7);
		final String text = element.type == CRTSignElementType.ICON ? "" : elementDisplayText(element);
		if (element.type == CRTSignElementType.ICON && x2 - x1 > 6 && y2 - y1 > 6) {
			final JSSignTextureBaker.Baked thumb = elementThumbnail(element, Math.max(8.0F, layout.rowHeight),
					cacheKey + ":canvas:" + element.id + ":" + iconCanvasSignature(element));
			drawBakedTexture(c, thumb, x1 + 1, y1 + 1, x2 - x1 - 2, y2 - y1 - 2);
		}
		if (!text.isEmpty() && x2 - x1 > 6 && y2 - y1 > 6) {
			float scale = Math.min((y2 - y1) * 0.42F / CRTEditorWidgets.FONT_HEIGHT, (x2 - x1 - 4.0F) / Math.max(1, c.textWidth(text)));
			scale = Math.max(0.4F, Math.min(2.2F, scale));
			final int textWidth = c.scaledTextWidth(text, scale);
			final int textHeight = Math.round(CRTEditorWidgets.FONT_HEIGHT * scale);
			c.scaledText(text, x1 + Math.max(1, (x2 - x1 - textWidth) / 2), y1 + Math.max(0, (y2 - y1 - textHeight) / 2), scale, contrastColor(fill));
		}
		if (element.binding != CRTSignElement.DataBinding.NONE) {
			c.fillRound(x2 - 4, y1 + 2, 2, 2, 1.0F, CRTEditorTheme.ACCENT);
		}
		if (hovered && element.type == CRTSignElementType.ICON) {
			c.setTooltip(tr("gui.nanbin.crt_editor.canvas.icon_tip", iconDisplayName(element)));
		}
		if (selected) {
			drawSelectionOutline(c, canvasElement);
		} else if (hovered) {
			c.border(x1, y1, x2, y2, 0xFF7AA3F0);
		}
	}


	/** 画布图标缩略图的缓存签名（图标/旋转/颜色变化才重新烘焙）。 */
	private static String iconCanvasSignature(CRTSignElement element) {
		return (element.icon == null ? "" : element.icon) + "|" + element.rotation + "|"
				+ (element.iconColor == null ? "none" : Integer.toHexString(element.iconColor));
	}

	/** 图标元素悬浮提示里显示的名字（追加包优先用 customText）。 */
	private static String iconDisplayName(CRTSignElement element) {
		if (element.icon == null || element.icon.trim().isEmpty()) {
			return tr("gui.nanbin.crt_editor.element.icon");
		}
		final String label = CRTSignRegistry.getSignLabel(element.icon.trim());
		return label == null || label.isEmpty() ? element.icon.trim() : label;
	}

	private void drawSelectionOutline(CRTEditorWidgets.Ctx c, CanvasElement canvasElement) {
		final int x1 = Math.round(canvasElement.x);
		final int y1 = Math.round(canvasElement.y);
		final int x2 = Math.round(canvasElement.x + canvasElement.w);
		final int y2 = Math.round(canvasElement.y + canvasElement.h);
		c.dashedRect(x1, y1, x2, y2, CRTEditorTheme.ACCENT);
		c.dashedRect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, CRTEditorTheme.ACCENT);
	}

	private static int contrastColor(int background) {
		final int red = (background >> 16) & 0xFF;
		final int green = (background >> 8) & 0xFF;
		final int blue = background & 0xFF;
		final int alpha = (background >>> 24) & 0xFF;
		if (alpha < 0x40) {
			return CRTEditorTheme.TEXT;
		}
		return red * 299 + green * 587 + blue * 114 > 150 * 1000 ? 0xFF1F2733 : 0xFFFFFFFF;
	}

	private void drawCanvasFrame(CRTEditorWidgets.Ctx c) {
		if (layout.frameWidth <= 0) {
			return;
		}
		final float frame = layout.frameWidth * canvasScale;
		if (frame < 0.5F) {
			return;
		}
		for (final CanvasRow row : canvasRows) {
			final int rowTop = Math.round(row.y);
			final int rowBottom = Math.round(row.y + row.h);
			final float top = row.index == 0 ? frame : frame / 2.0F;
			final float bottom = row.index == canvasRows.size() - 1 ? frame : frame / 2.0F;
			c.fill(Math.round(row.x), rowTop, Math.round(row.x + row.w), rowTop + Math.max(1, Math.round(top)), layout.frameColor);
			c.fill(Math.round(row.x), rowBottom - Math.max(1, Math.round(bottom)), Math.round(row.x + row.w), rowBottom, layout.frameColor);
			c.fill(Math.round(row.x), rowTop, Math.round(row.x) + Math.max(1, Math.round(frame)), rowBottom, layout.frameColor);
			c.fill(Math.round(row.x + row.w) - Math.max(1, Math.round(frame)), rowTop, Math.round(row.x + row.w), rowBottom, layout.frameColor);
		}
	}

	private void drawRowChrome(CRTEditorWidgets.Ctx c) {
		for (final RowHandle handle : rowHandles) {
			c.fillRoundBordered(handle.rect.x, handle.rect.y, handle.rect.w, handle.rect.h, 4.0F, 0xFFFFFFFF, CRTEditorTheme.BORDER);
			c.textCentered("≡", handle.rect.x + handle.rect.w / 2, handle.rect.y + (handle.rect.h - CRTEditorWidgets.FONT_HEIGHT) / 2 + 1, CRTEditorTheme.TEXT_DIM);
		}
		for (final RowChromeButton button : rowChromeButtons) {
			final boolean enabled = rowChromeEnabled(button);
			final boolean hover = enabled && button.rect.contains(lastMouseX, lastMouseY);
			final boolean danger = button.action == RowChromeButton.ACTION_DELETE && isRowArmed(button.rowIndex);
			final int background = danger ? 0xFFFEE2E2 : (hover ? (button.action == RowChromeButton.ACTION_DELETE ? 0xFFFFF1F1 : CRTEditorTheme.ACCENT_SOFT) : 0xFFFFFFFF);
			final int border = danger ? CRTEditorTheme.DANGER : (hover ? (button.action == RowChromeButton.ACTION_DELETE ? CRTEditorTheme.DANGER : CRTEditorTheme.ACCENT) : CRTEditorTheme.BORDER);
			final int foreground = danger ? CRTEditorTheme.DANGER : (hover ? (button.action == RowChromeButton.ACTION_DELETE ? CRTEditorTheme.DANGER : CRTEditorTheme.ACCENT) : (enabled ? CRTEditorTheme.TEXT_DIM : 0xFFC8CFD8));
			c.fillRoundBordered(button.rect.x, button.rect.y, button.rect.w, button.rect.h, 4.0F, background, border);
			c.textCentered(rowChromeLabel(button.action), button.rect.x + button.rect.w / 2,
					button.rect.y + (button.rect.h - CRTEditorWidgets.FONT_HEIGHT) / 2 + 1, foreground);
		}
	}

	private static String rowChromeLabel(int action) {
		switch (action) {
			case RowChromeButton.ACTION_UP:
				return "↑";
			case RowChromeButton.ACTION_DOWN:
				return "↓";
			case RowChromeButton.ACTION_BG:
				return "色";
			case RowChromeButton.ACTION_DELETE:
			default:
				return "✕";
		}
	}

	private boolean rowChromeEnabled(RowChromeButton button) {
		switch (button.action) {
			case RowChromeButton.ACTION_UP:
				return button.rowIndex > 0;
			case RowChromeButton.ACTION_DOWN:
				return button.rowIndex < layout.rows.size() - 1;
			case RowChromeButton.ACTION_DELETE:
				return layout.rows.size() > 1;
			case RowChromeButton.ACTION_BG:
			default:
				return true;
		}
	}

	private boolean isRowArmed(int rowIndex) {
		if (armedDeleteRowId == null || rowIndex < 0 || rowIndex >= layout.rows.size()) {
			return false;
		}
		return armedDeleteRowId.equals(layout.rows.get(rowIndex).id);
	}

	private void drawDropIndicators(CRTEditorWidgets.Ctx c) {
		if ((elementDragging || draggingPaletteItem != null) && dropValid && dropRowIndex >= 0 && dropRowIndex < canvasRows.size()) {
			final CanvasRow row = canvasRows.get(dropRowIndex);
			final int x = Math.round(dropIndicatorX);
			final int from = Math.round(row.y) + 2;
			final int to = Math.round(row.y + row.h) - 2;
			for (int y = from; y < to; y += 6) {
				c.fill(x, y, x + 2, Math.min(to, y + 3), CRTEditorTheme.ACCENT);
			}
			c.textCentered(tr("gui.nanbin.crt_editor.drag.insert"), x + 14, Math.round(row.y) + 3, CRTEditorTheme.ACCENT);
		}
		if (rowDragging && rowDragBoundary >= 0) {
			final float rowHeight = signScreenH / Math.max(1.0F, layout.rows.size());
			final int y = Math.round(signScreenY + rowDragBoundary * rowHeight);
			c.dashedRect(Math.round(signScreenX) - 8, y - 1, Math.round(signScreenX + signScreenW) + 8, y + 1, CRTEditorTheme.ACCENT);
		}
	}

	// ================================================================ 输入：画布

	private void handleCanvasClick(double mouseX, double mouseY, boolean control, boolean shift) {
		// 行控件按钮
		for (final RowChromeButton button : rowChromeButtons) {
			if (button.rect.contains(mouseX, mouseY)) {
				if (rowChromeEnabled(button)) {
					performRowChromeAction(button);
				}
				return;
			}
		}
		// 行拖动排序手柄
		for (final RowHandle handle : rowHandles) {
			if (handle.rect.contains(mouseX, mouseY)) {
				rowHandlePressed = true;
				rowDragFrom = handle.rowIndex;
				rowDragBoundary = handle.rowIndex;
				rowPressY = mouseY;
				return;
			}
		}
		final CanvasElement element = elementAt(mouseX, mouseY);
		if (element != null) {
			if (control) {
				toggleSelection(element.element.id);
			} else if (shift) {
				rangeSelect(element.element.id);
			} else {
				if (!isSelected(element.element.id)) {
					selectSingle(element.element.id);
				}
				elementPressActive = true;
				elementPressId = element.element.id;
				elementPressX = mouseX;
				elementPressY = mouseY;
				elementDragIds.clear();
				elementDragIds.addAll(selection);
			}
			return;
		}
		// 空白：双击复位缩放，否则取消选中
		beginCanvasPanCandidate(mouseX, mouseY);
	}

	/** 记录一次可能的画布拖动（空白左键 / 中键）。 */

	/** 分栏拖拽把手的命中半宽（GUI 像素）。 */
	private static final int DIVIDER_GRAB = 3;

	private boolean isOverLeftDivider(double mouseX, double mouseY) {
		return leftPanelRect.w > 0 && Math.abs(mouseX - (leftPanelRect.x + leftPanelRect.w)) <= DIVIDER_GRAB
				&& mouseY >= TOP_BAR_HEIGHT && mouseY <= getHeightMapped();
	}

	private boolean isOverRightDivider(double mouseX, double mouseY) {
		return rightPanelRect.w > 0 && Math.abs(mouseX - rightPanelRect.x) <= DIVIDER_GRAB
				&& mouseY >= TOP_BAR_HEIGHT && mouseY <= getHeightMapped();
	}

	/** 鼠标悬停/拖拽分栏线时画一条强调色竖线（不占内容区）。 */
	private void drawPanelResizeHints(CRTEditorWidgets.Ctx c) {
		if (leftPanelRect.w > 0 && (resizingLeft || isOverLeftDivider(lastMouseX, lastMouseY))) {
			final int x = leftPanelRect.x + leftPanelRect.w - 1;
			c.fill(x - 1, TOP_BAR_HEIGHT, x + 1, getHeightMapped(), CRTEditorTheme.ACCENT);
		}
		if (rightPanelRect.w > 0 && (resizingRight || isOverRightDivider(lastMouseX, lastMouseY))) {
			final int x = rightPanelRect.x;
			c.fill(x, TOP_BAR_HEIGHT, x + 2, getHeightMapped(), CRTEditorTheme.ACCENT);
		}
	}

	private void beginCanvasPanCandidate(double mouseX, double mouseY) {
		canvasPanCandidate = true;
		canvasPanning = false;
		canvasPanPressX = mouseX;
		canvasPanPressY = mouseY;
		canvasPanStartScrollX = canvasScrollX;
		canvasPanStartScrollY = canvasScrollY;
	}

	/** 松手时处理空白单击语义（未拖动时）：双击复位缩放，否则取消选中。 */
	private void finishCanvasPanCandidate(double mouseX, double mouseY, int button) {
		final boolean moved = Math.abs(mouseX - canvasPanPressX) + Math.abs(mouseY - canvasPanPressY) > 2.0;
		canvasPanCandidate = false;
		if (canvasPanning) {
			canvasPanning = false;
			return;
		}
		if (moved || button != 0) {
			return;
		}
		final long now = System.currentTimeMillis();
		final double distance = Math.abs(mouseX - lastCanvasClickX) + Math.abs(mouseY - lastCanvasClickY);
		if (now - lastCanvasClickTime < 300L && distance < 10.0) {
			resetCanvasZoom();
		} else {
			clearSelection();
			blurFocused();
			lastCanvasClickTime = now;
			lastCanvasClickX = mouseX;
			lastCanvasClickY = mouseY;
		}
	}

	private void performRowChromeAction(RowChromeButton button) {
		switch (button.action) {
			case RowChromeButton.ACTION_UP:
				moveRowBy(button.rowIndex, -1);
				break;
			case RowChromeButton.ACTION_DOWN:
				moveRowBy(button.rowIndex, 1);
				break;
			case RowChromeButton.ACTION_BG:
				selectRowForSettings(button.rowIndex);
				break;
			case RowChromeButton.ACTION_DELETE:
				deleteRowAction(button.rowIndex);
				break;
			default:
				break;
		}
	}

	private void updateDropTarget(double mouseX, double mouseY, List<String> draggedIds) {
		dropRowId = null;
		dropValid = false;
		dropRowIndex = -1;
		if (!canvasViewportRect.contains(mouseX, mouseY) || canvasRows.isEmpty()) {
			return;
		}
		final int rowIndex = rowIndexAtY(mouseY);
		if (rowIndex < 0 || rowIndex >= layout.rows.size()) {
			return;
		}
		final CRTSignLayout.Row row = layout.rows.get(rowIndex);
		final CanvasRow canvasRow = canvasRows.get(rowIndex);
		final List<CanvasElement> candidates = new ArrayList<>();
		for (final CanvasElement element : canvasElements) {
			if (element.rowIndex == rowIndex && !draggedIds.contains(element.element.id)) {
				candidates.add(element);
			}
		}
		Collections.sort(candidates, new Comparator<CanvasElement>() {
			@Override
			public int compare(CanvasElement a, CanvasElement b) {
				return Float.compare(a.x, b.x);
			}
		});
		int visualGap = 0;
		while (visualGap < candidates.size() && mouseX > candidates.get(visualGap).x + candidates.get(visualGap).w / 2.0) {
			visualGap++;
		}
		float indicatorX;
		if (candidates.isEmpty()) {
			indicatorX = canvasRow.x + 2;
		} else if (visualGap <= 0) {
			indicatorX = candidates.get(0).x;
		} else if (visualGap >= candidates.size()) {
			final CanvasElement last = candidates.get(candidates.size() - 1);
			indicatorX = last.x + last.w;
		} else {
			indicatorX = candidates.get(visualGap).x;
		}
		dropRowId = row.id;
		dropRowIndex = rowIndex;
		dropGap = visualGap;
		dropIndicatorX = indicatorX;
		dropValid = true;
	}

	private void changeCanvasZoom(float factor) {
		canvasZoom = (float) CRTEditorWidgets.clamp(canvasZoom * factor, 0.25F, 6.0F);
		layoutCanvas();
		widgetsDirty = true;
	}

	private void resetCanvasZoom() {
		canvasZoom = 1.0F;
		canvasScrollX = 0.0F;
		canvasScrollY = 0.0F;
		layoutCanvas();
	}

	// ================================================================ 输入事件

	@Override
	public boolean mouseClicked2(double mouseX, double mouseY, int button) {
		lastMouseX = (int) mouseX;
		lastMouseY = (int) mouseY;
		ensureWidgets();
		layoutCanvas();
		if (button == 2 && canvasViewportRect.contains(mouseX, mouseY)) {
			// 中键拖动 = 平移画布（缩放后常用）
			beginCanvasPanCandidate(mouseX, mouseY);
			return true;
		}
		if (button != 0) {
			return super.mouseClicked2(mouseX, mouseY, button);
		}
		final CRTEditorWidgets.Ctx c = eventCtx();
		if (presetModalOpen) {
			// 预设命名弹窗：只响应弹窗内控件，点击遮罩空白关闭
			if (dispatchWidgets(c, modalWidgets, mouseX, mouseY)) {
				return true;
			}
			if (!presetModalRect.contains(mouseX, mouseY)) {
				closePresetNameModal();
			}
			return true;
		}
		if (openDropdown != null && openDropdown.popupClicked(c, mouseX, mouseY)) {
			return true;
		}
		// 分栏拖拽把手：左右栏边缘 ±3px，双击恢复自动比例
		final long dividerNow = System.currentTimeMillis();
		if (isOverLeftDivider(mouseX, mouseY)) {
			if (dividerNow - lastDividerClickTime < 300L && lastDividerKind == 1) {
				customLeftWidth = -1;
				lastDividerKind = 0;
				updateLayout();
				rebuildAllWidgets();
				layoutCanvas();
				return true;
			}
			lastDividerClickTime = dividerNow;
			lastDividerKind = 1;
			resizingLeft = true;
			resizeStartMouseX = (int) mouseX;
			resizeStartWidth = leftPanelRect.w;
			return true;
		}
		if (isOverRightDivider(mouseX, mouseY)) {
			if (dividerNow - lastDividerClickTime < 300L && lastDividerKind == 2) {
				customRightWidth = -1;
				lastDividerKind = 0;
				updateLayout();
				rebuildAllWidgets();
				layoutCanvas();
				return true;
			}
			lastDividerClickTime = dividerNow;
			lastDividerKind = 2;
			resizingRight = true;
			resizeStartMouseX = (int) mouseX;
			resizeStartWidth = rightPanelRect.w;
			return true;
		}
		// 右栏滚动条
		if (!rightScrollbarDragging && rightScrollbarHit(mouseX, mouseY)) {
			rightScrollbarDragging = true;
			updateRightScrollFromScrollbar(mouseY);
			return true;
		}
		// 左栏滚动条
		if (!leftScrollbarDragging && leftScrollbarHit(mouseX, mouseY)) {
			leftScrollbarDragging = true;
			updateLeftScrollFromScrollbar(mouseY);
			return true;
		}
		if (dispatchWidgets(c, topWidgets, mouseX, mouseY)) {
			return true;
		}
		if (dispatchWidgets(c, rightHeaderWidgets, mouseX, mouseY)) {
			return true;
		}
		if (dispatchWidgets(c, centerWidgets, mouseX, mouseY)) {
			return true;
		}
		if (rightBodyRect.contains(mouseX, mouseY) && dispatchWidgets(c, rightWidgets, mouseX, mouseY)) {
			ensureRightWidgetVisible(pressedWidget);
			return true;
		}
		if (leftPanelRect.w > 0 && leftPanelRect.contains(mouseX, mouseY) && dispatchWidgets(c, leftWidgets, mouseX, mouseY)) {
			return true;
		}
		if (leftBodyRect.contains(mouseX, mouseY)) {
			if (leftTab == LEFT_TAB_PRESETS) {
				final PresetCard presetCard = presetCardAt(mouseX, mouseY);
				if (presetCard != null) {
					if (presetCard.deleteRect(presetScreenY(presetCard)).contains(mouseX, mouseY)) {
						deletePreset(presetCard.preset);
					} else {
						insertPreset(presetCard.preset);
					}
					return true;
				}
				blurFocused();
				return true;
			}
			final PaletteItem item = paletteItemAt(mouseX, mouseY);
			if (item != null) {
				breakHistoryGroup();
				pressedPaletteItem = item;
				palettePressX = mouseX;
				palettePressY = mouseY;
				draggingPaletteItem = null;
				return true;
			}
			blurFocused();
			return true;
		}
		if (elementOverlaysActive() && canvasViewportRect.contains(mouseX, mouseY)) {
			for (final ElementToolbarButton toolbarButton : elementToolbarButtons) {
				if (toolbarButton.rect.contains(mouseX, mouseY)) {
					if (toolbarActionEnabled(toolbarButton.action)) {
						performElementToolbarAction(toolbarButton.action);
					}
					return true;
				}
			}
			if (paddingLeftHandle.contains(mouseX, mouseY)) {
				beginPaddingDrag(-1, mouseX);
				return true;
			}
			if (paddingRightHandle.contains(mouseX, mouseY)) {
				beginPaddingDrag(1, mouseX);
				return true;
			}
		}
		if (canvasViewportRect.contains(mouseX, mouseY)) {
			handleCanvasClick(mouseX, mouseY, ScreenExtension.hasControlDown2(), ScreenExtension.hasShiftDown2());
			return true;
		}
		blurFocused();
		return true;
	}

	private boolean dispatchWidgets(CRTEditorWidgets.Ctx c, List<CRTEditorWidgets.Widget> widgets, double mouseX, double mouseY) {
		for (int i = widgets.size() - 1; i >= 0; i--) {
			final CRTEditorWidgets.Widget widget = widgets.get(i);
			if (!widget.visible || !widget.enabled || !widget.isOver(mouseX, mouseY)) {
				continue;
			}
			if (widget.mouseClicked(c, mouseX, mouseY, 0)) {
				pressedWidget = widget;
				if (!widget.wantsKeyboard()) {
					blurFocused();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased2(double mouseX, double mouseY, int button) {
		lastMouseX = (int) mouseX;
		lastMouseY = (int) mouseY;
		if (resizingLeft || resizingRight) {
			resizingLeft = false;
			resizingRight = false;
			return true;
		}
		if (rightScrollbarDragging || leftScrollbarDragging) {
			rightScrollbarDragging = false;
			leftScrollbarDragging = false;
			return true;
		}
		if (canvasPanCandidate) {
			finishCanvasPanCandidate(mouseX, mouseY, button);
			return true;
		}
		if (paddingDragActive) {
			endPaddingDrag();
			return true;
		}
		final CRTEditorWidgets.Ctx c = eventCtx();
		if (pressedWidget != null) {
			final CRTEditorWidgets.Widget widget = pressedWidget;
			pressedWidget = null;
			widget.mouseReleased(c, mouseX, mouseY, button);
			return true;
		}
		if (pressedPaletteItem != null) {
			final PaletteItem item = pressedPaletteItem;
			final boolean dragged = draggingPaletteItem != null;
			pressedPaletteItem = null;
			draggingPaletteItem = null;
			if (dragged) {
				if (dropValid && dropRowId != null) {
					final int rowIndex = layout.indexOfRow(dropRowId);
					if (rowIndex >= 0 && rowIndex < layout.rows.size()) {
						addElementAt(item, layout.rows.get(rowIndex), dropGap);
					}
				}
				dropValid = false;
				dropRowId = null;
			} else if (item == paletteItemAt(mouseX, mouseY)) {
				addElementFromPalette(item);
			}
			return true;
		}
		if (elementPressActive) {
			final boolean dragged = elementDragging;
			elementPressActive = false;
			elementDragging = false;
			if (dragged) {
				if (dropValid && dropRowId != null) {
					moveSelectionTo(dropRowId, dropGap);
				} else {
					breakHistoryGroup();
				}
			}
			dropValid = false;
			dropRowId = null;
			return true;
		}
		if (rowHandlePressed) {
			rowHandlePressed = false;
			if (rowDragging && rowDragFrom >= 0 && rowDragBoundary >= 0) {
				final int from = rowDragFrom;
				final int boundary = rowDragBoundary;
				final int candidate = boundary > from ? boundary - 1 : boundary;
				final int to = CRTEditorWidgets.clamp(candidate, 0, Math.max(0, layout.rows.size() - 1));
				if (to != from) {
					editDiscrete(null, () -> layout.moveRow(from, to));
				}
			}
			rowDragging = false;
			rowDragFrom = -1;
			rowDragBoundary = -1;
			return true;
		}
		return super.mouseReleased2(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		lastMouseX = (int) mouseX;
		lastMouseY = (int) mouseY;
		if (resizingLeft) {
			customLeftWidth = CRTEditorWidgets.clamp((int) (resizeStartWidth + (mouseX - resizeStartMouseX)), 120, 420);
			updateLayout();
			rebuildAllWidgets();
			layoutCanvas();
			return true;
		}
		if (resizingRight) {
			customRightWidth = CRTEditorWidgets.clamp((int) (resizeStartWidth - (mouseX - resizeStartMouseX)), 120, 420);
			updateLayout();
			rebuildAllWidgets();
			layoutCanvas();
			return true;
		}
		if (rightScrollbarDragging) {
			updateRightScrollFromScrollbar(mouseY);
			return true;
		}
		if (leftScrollbarDragging) {
			updateLeftScrollFromScrollbar(mouseY);
			return true;
		}
		final CRTEditorWidgets.Ctx c = eventCtx();
		if (paddingDragActive) {
			applyPaddingDrag(mouseX);
			return true;
		}
		if (pressedWidget != null && pressedWidget.mouseDragged(c, mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		}
		if (canvasPanCandidate) {
			final double dx = mouseX - canvasPanPressX;
			final double dy = mouseY - canvasPanPressY;
			if (!canvasPanning && Math.abs(dx) + Math.abs(dy) > 2.0) {
				canvasPanning = true;
			}
			if (canvasPanning) {
				canvasScrollX = (float) (canvasPanStartScrollX + dx);
				canvasScrollY = (float) (canvasPanStartScrollY + dy);
				layoutCanvas();
				return true;
			}
		}
		if (pressedPaletteItem != null) {
			if (draggingPaletteItem == null
					&& Math.abs(mouseX - palettePressX) + Math.abs(mouseY - palettePressY) > DRAG_THRESHOLD) {
				draggingPaletteItem = pressedPaletteItem;
				breakHistoryGroup();
			}
			if (draggingPaletteItem != null) {
				updateDropTarget(mouseX, mouseY, Collections.<String>emptyList());
			}
			return true;
		}
		if (elementPressActive) {
			if (!elementDragging && Math.abs(mouseX - elementPressX) + Math.abs(mouseY - elementPressY) > DRAG_THRESHOLD) {
				elementDragging = true;
				breakHistoryGroup();
			}
			if (elementDragging) {
				updateDropTarget(mouseX, mouseY, elementDragIds);
			}
			return true;
		}
		if (rowHandlePressed) {
			if (!rowDragging && Math.abs(mouseY - rowPressY) > DRAG_THRESHOLD) {
				rowDragging = true;
			}
			if (rowDragging) {
				rowDragBoundary = rowBoundaryAt(mouseY);
			}
			return true;
		}
		return super.mouseDragged2(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
		lastMouseX = (int) mouseX;
		lastMouseY = (int) mouseY;
		final CRTEditorWidgets.Ctx c = eventCtx();
		if (presetModalOpen) {
			return true;
		}
		if (openDropdown != null && openDropdown.popupScrolled(c, mouseX, mouseY, amount)) {
			return true;
		}
		if (leftBodyRect.contains(mouseX, mouseY) && leftMaxScroll() > 0) {
			leftScroll = CRTEditorWidgets.clamp(leftScroll - (int) Math.signum(amount) * 14, 0, leftMaxScroll());
			return true;
		}
		if (rightBodyRect.contains(mouseX, mouseY) && rightMaxScroll() > 0) {
			rightScroll = CRTEditorWidgets.clamp(rightScroll - (int) Math.signum(amount) * 14, 0, rightMaxScroll());
			widgetsDirty = true;
			return true;
		}
		if (canvasViewportRect.contains(mouseX, mouseY)) {
			if (ScreenExtension.hasShiftDown2()) {
				canvasScrollX += (float) (amount * 18.0);
			} else {
				// 滚轮直接缩放画布（与常见编辑器一致），Ctrl 为更大步进
				final float step = ScreenExtension.hasControlDown2() ? 1.3F : 1.15F;
				changeCanvasZoom(amount > 0 ? step : 1.0F / step);
			}
			return true;
		}
		return super.mouseScrolled2(mouseX, mouseY, amount);
	}

	@Override
	public boolean keyPressed2(int keyCode, int scanCode, int modifiers) {
		final CRTEditorWidgets.Ctx c = eventCtx();
		final boolean control = ScreenExtension.hasControlDown2();
		final boolean shift = ScreenExtension.hasShiftDown2();
		if (presetModalOpen) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				closePresetNameModal();
				return true;
			}
			// 回车直接保存（Text 输入框本身的 Enter=失焦会绕过保存按钮，这里统一拦截）
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				confirmPresetSave();
				return true;
			}
			if (focusedWidget != null) {
				if (focusedWidget.keyPressed(c, keyCode, scanCode, modifiers)) {
					return true;
				}
				if (focusedWidget.wantsKeyboard()) {
					return true;
				}
			}
			return true;
		}
		if (paddingDragActive) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				endPaddingDrag();
				return true;
			}
		}
		if (focusedWidget != null) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				blurFocused();
				return true;
			}
			if (focusedWidget.keyPressed(c, keyCode, scanCode, modifiers)) {
				return true;
			}
			if (focusedWidget.wantsKeyboard()) {
				// 输入控件内不触发全局快捷键（与网页一致），避免打字时误删/误移动元素
				if (control || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
						|| keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
					return true;
				}
			}
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			if (openDropdown != null) {
				openDropdown = null;
				return true;
			}
			if (!selection.isEmpty()) {
				clearSelection();
				return true;
			}
			onClose2();
			return true;
		}
		if (control) {
			switch (keyCode) {
				case org.lwjgl.glfw.GLFW.GLFW_KEY_A:
					selectAll();
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_C:
					copySelection(false);
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_X:
					copySelection(true);
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_V:
					pasteClipboard();
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_Z:
					if (shift) {
						redo();
					} else {
						undo();
					}
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_Y:
					redo();
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_S:
					saveNow();
					toast(tr("gui.nanbin.crt_editor.status.saved"));
					return true;
				default:
					break;
			}
		}
		switch (keyCode) {
			case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE:
			case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:
				if (!selection.isEmpty()) {
					deleteSelection();
					return true;
				}
				break;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT:
				if (!selection.isEmpty()) {
					moveSelectionBy(-1);
					return true;
				}
				break;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT:
				if (!selection.isEmpty()) {
					moveSelectionBy(1);
					return true;
				}
				break;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_UP:
				if (!selection.isEmpty()) {
					moveSelectionToRow(-1);
					return true;
				}
				break;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN:
				if (!selection.isEmpty()) {
					moveSelectionToRow(1);
					return true;
				}
				break;
			default:
				break;
		}
		return super.keyPressed2(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped2(char chr, int modifiers) {
		if (focusedWidget != null) {
			return focusedWidget.charTyped(eventCtx(), chr, modifiers);
		}
		return super.charTyped2(chr, modifiers);
	}

	// ================================================================ 滚动条

	private int leftMaxScroll() {
		return Math.max(0, leftContentHeight - leftBodyRect.h);
	}

	private int rightMaxScroll() {
		return Math.max(0, rightContentHeight - rightBodyRect.h);
	}

	private boolean rightScrollbarHit(double mouseX, double mouseY) {
		if (rightPanelRect.w <= 0 || rightContentHeight <= rightBodyRect.h) {
			return false;
		}
		return mouseX >= rightScrollTrack.x - 1 && mouseX < rightScrollTrack.x + rightScrollTrack.w + 1
				&& mouseY >= rightScrollTrack.y && mouseY < rightScrollTrack.y + rightScrollTrack.h;
	}

	private boolean leftScrollbarHit(double mouseX, double mouseY) {
		if (leftPanelRect.w <= 0 || leftContentHeight <= leftBodyRect.h) {
			return false;
		}
		return mouseX >= leftScrollTrack.x - 1 && mouseX < leftScrollTrack.x + leftScrollTrack.w + 1
				&& mouseY >= leftScrollTrack.y && mouseY < leftScrollTrack.y + leftScrollTrack.h;
	}

	private void updateRightScrollFromScrollbar(double mouseY) {
		final int trackHeight = Math.max(1, rightScrollTrack.h);
		final float ratio = (float) ((mouseY - rightScrollTrack.y) / trackHeight);
		rightScroll = CRTEditorWidgets.clamp(Math.round(ratio * rightMaxScroll()), 0, rightMaxScroll());
		widgetsDirty = true;
	}

	private void updateLeftScrollFromScrollbar(double mouseY) {
		final int trackHeight = Math.max(1, leftScrollTrack.h);
		final float ratio = (float) ((mouseY - leftScrollTrack.y) / trackHeight);
		leftScroll = CRTEditorWidgets.clamp(Math.round(ratio * leftMaxScroll()), 0, leftMaxScroll());
	}

	private void drawLeftScrollbar(CRTEditorWidgets.Ctx c) {
		if (leftPanelRect.w <= 0) {
			return;
		}
		leftScrollTrack.set(leftPanelRect.x + leftPanelRect.w - SCROLLBAR_WIDTH - 1, leftBodyRect.y + 2, SCROLLBAR_WIDTH,
				Math.max(1, leftBodyRect.h - 4));
		leftScrollThumb.set(0, 0, 0, 0);
		if (leftContentHeight <= leftBodyRect.h) {
			return;
		}
		c.fillRound(leftScrollTrack.x, leftScrollTrack.y, leftScrollTrack.w, leftScrollTrack.h, 2.5F, CRTEditorTheme.SURFACE_SOFT);
		final int maxScroll = leftMaxScroll();
		final int thumbHeight = Math.max(14, (int) ((float) leftScrollTrack.h * leftBodyRect.h / Math.max(1, leftContentHeight)));
		final int thumbY = leftScrollTrack.y + (int) ((leftScrollTrack.h - thumbHeight) * (leftScroll / (float) Math.max(1, maxScroll)));
		leftScrollThumb.set(leftScrollTrack.x, thumbY, SCROLLBAR_WIDTH, thumbHeight);
		c.fillRound(leftScrollThumb.x, leftScrollThumb.y, leftScrollThumb.w, leftScrollThumb.h, 2.5F, 0xFFC3CCD6);
	}

	// ================================================================ 裁剪

	private void enableScissor(Rect rect) {
		enableScissor(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h);
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
}
