package com.Nanbin.signeditor;

import com.Nanbin.client.ClientData.SignClipboard;
import com.Nanbin.client.JavaScriptSupport.JSSignConfig;
import com.Nanbin.client.Screen.CustomTextScreen;
import com.Nanbin.client.Screen.JSSignSelectorScreen;
import com.Nanbin.client.Screen.MtrSignSelectors;
import com.Nanbin.client.Screen.RailwaySignClearConfirmScreen;
import com.Nanbin.packet.ClientPacketHelper;

import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.constraints.AdditiveConstraint;
import gg.essential.elementa.constraints.CenterConstraint;
import gg.essential.elementa.constraints.CoerceAtMostConstraint;
import gg.essential.elementa.constraints.FillConstraint;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.elementa.constraints.RelativeConstraint;
import gg.essential.elementa.constraints.ScaleConstraint;
import gg.essential.elementa.constraints.SiblingConstraint;
import gg.essential.elementa.constraints.SubtractiveConstraint;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.mtr.client.CustomResourceLoader;
import org.mtr.generated.lang.TranslationProvider;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.resource.SignResource;
import org.mtr.screen.WindowBase;
import org.mtr.tool.GuiHelper;
import org.mtr.widget.BackgroundComponent;
import org.mtr.widget.ButtonComponent;
import org.mtr.widget.ScrollPanelComponent;
import org.mtr.widget.SignButtonsComponent;
import org.mtr.widget.SlotBackgroundComponent;
import org.mtr.widget.TextInputComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 指示牌编辑器（elementa 版），同时服务两种方块：
 * <ul>
 *     <li>CRT 站台信息屏的双层指示牌（2 行 × 7 格，{@code selectedIds} 按「行」组织）；</li>
 *     <li>CRT 铁路告示牌（1 行 × length 格，{@code selectedIds} 按「格」组织，支持逐格配色）。</li>
 * </ul>
 * <p>
 * 布局照抄 MTR 4.1 自己的 {@code org.mtr.screen.RailwaySignScreen}，因为它的尺寸约束是
 * 按窗口相对计算的（{@code BackgroundComponent} 把面板限在
 * {@code min(窗口 - 80, 320 × 240)}，其余控件全部相对它算），换成固定像素就会在
 * 「窗口逻辑高度只有 190」这类情况下溢出面板——下部选择区会被裁掉、按钮点不到。
 * <p>
 * 关键约束链（与 MTR 相同，注意不要出现 Fill 套 Fill，那会让 elementa 报
 * {@code Cyclic constraint structure detected}）：
 * <pre>
 * background            = min(窗口 - 80, 320 × 240)，居中
 * previewRow            = 100% 宽 × 34×行数 高
 *   preview(line)       = min(100% - 72, 32×格数 + 2) 宽，高按 1/格数 的等比约束
 *   search              = min(100% - 8, 64) 宽 × 20 高，右上角
 * panelRow              = y = 34×行数 + 4，高 = Fill - 8
 *   signArea            = 100% - (工具栏宽 + 4) 宽，100% 高
 *     smallPanel        = (signArea - 64) × 8/24 + 38 宽，100% 高
 *     largePanel        = (signArea - 64) × 16/24 + 22 宽，与上一面板间距 4
 *   toolColumn          = 40 宽，100% 高，右对齐
 * </pre>
 * 两个面板的宽度加起来正好等于 signArea 的宽度，所以不会超出面板。
 * <p>
 * 这个类放在独立的 {@code com.Nanbin.signeditor} 包里：NeoForge 的模块系统不允许两个模块
 * 导出同一个包，而 common 与各平台模块是分开的模块。源码由两端各自编译
 * （Fabric 的 elementa 是 {@code gg.essential.*}，NeoForge 被 MTR 重定位成
 * {@code org.mtr.libraries.gg.essential.*}，见各自 build.gradle 的 generateElementaSources）。
 */
public class RailwaySignEditorScreen extends WindowBase {

	/** 预览里每格的像素边长，与 MTR 的 SignPreviewComponent 一致。 */
	private static final float PREVIEW_CELL = 32.0F;
	/** 每行预览的整体高度 = 格子边长 + 上下各 1px 边框。 */
	private static final float PREVIEW_ROW_HEIGHT = PREVIEW_CELL + 2.0F;
	/** 各区块之间的间距。 */
	private static final float SECTION_GAP = 4.0F;
	/** MTR 的 createMainComponents 里写死的 64：两个面板的宽度公式靠它刚好铺满。 */
	private static final float PANEL_WIDTH_BASE = 64.0F;
	private static final int SMALL_SIGN_COLUMNS = 8;
	private static final int SMALL_SIGN_WIDTH_UNITS = 1;
	private static final int LARGE_SIGN_COLUMNS = 4;
	private static final int LARGE_SIGN_WIDTH_UNITS = 4;
	/** 右侧工具栏宽度（比 4.0 的 30 略宽，够放两个汉字）。 */
	private static final float TOOL_COLUMN_WIDTH = 40.0F;
	/** 工具栏按钮个数：JS 样式 / 复制 / 粘贴 / 清空 / 撤回 / 完成。 */
	private static final float TOOL_BUTTON_COUNT = 6.0F;

	private final BlockPos signPos;
	private final int lines;
	private final int length;
	private final String[][] signIds;
	private final List<LongAVLTreeSet> selectedIds;
	private final String[][] routeNumbers;
	private final boolean selectedIdsPerCell;
	private final Runnable onSave;

	/** 当前正在编辑的格：行 + 格号。未点过预览时默认落在第 0 行第 0 格。 */
	private int editingLine;
	private int editingIndex;

	private SignButtonsComponent smallButtonsComponent;
	private SignButtonsComponent largeButtonsComponent;
	private final java.util.List<CrtSignPreviewComponent> previews = new ArrayList<>();
	private ButtonComponent styleButton;
	private ButtonComponent copyButton;
	private ButtonComponent pasteButton;
	private ButtonComponent clearButton;
	private ButtonComponent undoButton;
	private boolean copyDisabled;
	private boolean pasteDisabledState;
	private boolean clearDisabled;
	private boolean undoDisabled;

	public RailwaySignEditorScreen(Screen parent, BlockPos signPos, int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds, boolean selectedIdsPerCell, String[][] routeNumbers, Runnable onSave) {
		super(parent);
		this.signPos = signPos;
		this.lines = Math.max(lines, 1);
		this.length = Math.max(length, 1);
		this.signIds = signIds != null && signIds.length >= this.lines ? signIds : new String[this.lines][this.length];
		this.selectedIdsPerCell = selectedIdsPerCell;
		this.selectedIds = normalizeSelectedIds(selectedIds, this.lines, this.length, selectedIdsPerCell);
		this.routeNumbers = routeNumbers != null && routeNumbers.length >= this.lines ? routeNumbers : new String[this.lines][];
		this.onSave = onSave;
		buildUi();
	}

	/** 把选中数据补足成「行」或「格」的规模，并保证每一项都非空。 */
	private List<LongAVLTreeSet> normalizeSelectedIds(List<LongAVLTreeSet> source, int lines, int length, boolean perCell) {
		final int expectedSize = perCell ? length : lines;
		final List<LongAVLTreeSet> result = new ArrayList<>(expectedSize);
		for (int i = 0; i < expectedSize; i++) {
			final LongAVLTreeSet set = source != null && i < source.size() ? source.get(i) : null;
			result.add(set == null ? new LongAVLTreeSet() : set);
		}
		return result;
	}

	/** 第 line 行第 cell 格对应的选中数据集合。单行（铁路告示牌）时按格取。 */
	private LongAVLTreeSet selectedSet(int line, int cell) {
		final int index = selectedIdsPerCell ? cell : line;
		return index >= 0 && index < selectedIds.size() ? selectedIds.get(index) : new LongAVLTreeSet();
	}

	// ---- 界面搭建 ----

	private void buildUi() {
		final BackgroundComponent background = new BackgroundComponent(getWindow(), ObjectImmutableList.of());

		// 顶部预览行：高度 = 每行 34px
		final UIContainer previewRow = new UIContainer();
		previewRow.setChildOf(background);
		previewRow.setX(new PixelConstraint(0.0F));
		previewRow.setY(new PixelConstraint(0.0F));
		previewRow.setWidth(new RelativeConstraint());
		previewRow.setHeight(new PixelConstraint(PREVIEW_ROW_HEIGHT * lines));

		for (int i = 0; i < lines; i++) {
			final int lineIndex = i;
			// 预览必须和世界里的实际渲染一致，所以用本项目的 CRT 绘制入口，
			// 而不是 MTR 自带的 SignPreviewComponent（那是 MTR 风格的导向标志）
			final CrtSignPreviewComponent preview = new CrtSignPreviewComponent(signPos, signIds[lineIndex], previewSelectedIds(lineIndex), routeNumbers[lineIndex]);
			preview.setChildOf(previewRow);
			preview.setX(new PixelConstraint(0.0F));
			preview.setY(new PixelConstraint(i * PREVIEW_ROW_HEIGHT));
			preview.setWidth(new CoerceAtMostConstraint(
					new SubtractiveConstraint(new RelativeConstraint(), new PixelConstraint(72.0F)),
					new PixelConstraint(PREVIEW_CELL * length + 2.0F)));
			// 高度按「1 格 = 1/格数」等比推出，窗口变窄时预览整体缩小，永远塞得进预览行
			preview.setHeight(GuiHelper.createAspectConstraintWithPadding(1.0F / length, 1.0F));
			preview.onEdit(cellIndex -> editCell(lineIndex, cellIndex));
			previews.add(preview);
		}
		// 与 MTR 一致：默认选中第 0 格，箭头先落在这一格
		updatePreviewSelection();

		final TextInputComponent search = new TextInputComponent();
		search.setChildOf(previewRow);
		search.setX(new PixelConstraint(0.0F, true));
		search.setY(new PixelConstraint(0.0F));
		search.setWidth(new CoerceAtMostConstraint(
				new SubtractiveConstraint(new RelativeConstraint(), new PixelConstraint(8.0F)),
				new PixelConstraint(64.0F)));
		search.setHeight(new PixelConstraint(20.0F));
		search.setPlaceholderText(TranslationProvider.GUI_MTR_SEARCH.getString());

		// 下方内容行：从预览行下面开始，高度吃掉剩余空间（Fill 只减兄弟高度，所以还要再减两个间距）
		final UIContainer panelRow = new UIContainer();
		panelRow.setChildOf(background);
		panelRow.setX(new PixelConstraint(0.0F));
		panelRow.setY(new PixelConstraint(PREVIEW_ROW_HEIGHT * lines + SECTION_GAP));
		panelRow.setWidth(new RelativeConstraint());
		panelRow.setHeight(new SubtractiveConstraint(new FillConstraint(), new PixelConstraint(SECTION_GAP * 2.0F)));

		final UIContainer signArea = new UIContainer();
		signArea.setChildOf(panelRow);
		signArea.setX(new PixelConstraint(0.0F));
		signArea.setY(new PixelConstraint(0.0F));
		signArea.setWidth(new SubtractiveConstraint(new RelativeConstraint(), new PixelConstraint(TOOL_COLUMN_WIDTH + SECTION_GAP)));
		signArea.setHeight(new RelativeConstraint());

		// 标志选择分两栏：普通标志（8 列 × 1 单位）与带自定义文字的标志（4 列 × 4 单位）。
		// 两者的单元格宽高比不同，混在一栏里就会出现「比例不对、配套文字画不出来」。
		final ObjectArrayList<SignResource> smallSigns = new ObjectArrayList<>();
		final ObjectArrayList<SignResource> largeSigns = new ObjectArrayList<>();
		for (final SignResource sign : CustomResourceLoader.getSortedSigns()) {
			(sign.hasCustomText ? largeSigns : smallSigns).add(sign);
		}

		smallButtonsComponent = createSignPanel(signArea, smallSigns, false, SMALL_SIGN_COLUMNS, SMALL_SIGN_WIDTH_UNITS);
		largeButtonsComponent = createSignPanel(signArea, largeSigns, true, LARGE_SIGN_COLUMNS, LARGE_SIGN_WIDTH_UNITS);

		search.onChange(() -> {
			final String text = search.getText();
			smallButtonsComponent.setSearch(text);
			largeButtonsComponent.setSearch(text);
		});

		final UIContainer toolColumn = new UIContainer();
		toolColumn.setChildOf(panelRow);
		toolColumn.setX(new PixelConstraint(0.0F, true));
		toolColumn.setY(new PixelConstraint(0.0F));
		toolColumn.setWidth(new PixelConstraint(TOOL_COLUMN_WIDTH));
		toolColumn.setHeight(new RelativeConstraint());
		buildToolColumn(toolColumn);

		refreshToolState();
	}

	private LongAVLTreeSet[] previewSelectedIds(int line) {
		final LongAVLTreeSet[] result = new LongAVLTreeSet[length];
		for (int cell = 0; cell < length; cell++) {
			result[cell] = selectedSet(line, cell);
		}
		return result;
	}

	/**
	 * 一个标志选择面板：外框 + 滚动区 + 标志按钮网格。
	 * <p>
	 * 宽度公式与 MTR 的 {@code createMainComponents} 完全一致：
	 * {@code (父宽 - 64) × (列数 × 单位宽 / 24) + (列数 × 4 + 6)}，
	 * 两栏宽度的和刚好等于父宽，所以不会超出面板。
	 */
	private SignButtonsComponent createSignPanel(UIContainer parent, ObjectArrayList<SignResource> signs, boolean second, int columns, int widthUnits) {
		final float fraction = columns * (float) widthUnits / 24.0F;

		final SlotBackgroundComponent panel = new SlotBackgroundComponent();
		panel.setChildOf(parent);
		panel.setX(second ? new SiblingConstraint(SECTION_GAP) : new PixelConstraint(0.0F));
		panel.setY(new PixelConstraint(0.0F));
		panel.setWidth(new AdditiveConstraint(
				new ScaleConstraint(new SubtractiveConstraint(new RelativeConstraint(), new PixelConstraint(PANEL_WIDTH_BASE)), fraction),
				new PixelConstraint(columns * 4.0F + 6.0F)));
		panel.setHeight(new RelativeConstraint());

		final ScrollPanelComponent scrollPanel = new ScrollPanelComponent(false);
		scrollPanel.setChildOf(panel);
		scrollPanel.setX(new CenterConstraint());
		scrollPanel.setY(new CenterConstraint());
		scrollPanel.setWidth(new SubtractiveConstraint(new RelativeConstraint(), new PixelConstraint(2.0F)));
		scrollPanel.setHeight(new SubtractiveConstraint(new RelativeConstraint(), new PixelConstraint(2.0F)));

		final SignButtonsComponent buttons = new SignButtonsComponent(signPos, signs, columns, widthUnits);
		buttons.setChildOf(scrollPanel.contentContainer);
		buttons.setWidth(new RelativeConstraint());
		buttons.setHeight(new RelativeConstraint());
		// 高度由 SignButtonsComponent 自己在 draw() 里按内容重算
		buttons.onClick(this::onSignSelected);
		return buttons;
	}

	/** 工具列：每个按钮按 1/个数 的高度纵向平铺，随面板高度自动伸缩。 */
	private void buildToolColumn(UIContainer toolColumn) {
		styleButton = addToolButton(toolColumn, 0, this::getStyleButtonLabel, this::openJsStyleSelector);
		copyButton = addToolButton(toolColumn, 1, () -> Text.translatable("gui.nanbin.copy").getString(),
				() -> SignClipboard.copy(signClipboardLines(), length, signClipboardSignIds(), signClipboardSelectedIds()));
		pasteButton = addToolButton(toolColumn, 2, () -> Text.translatable("gui.nanbin.paste").getString(), () -> {
			SignClipboard.paste(signPos, signClipboardLines(), length, signClipboardSignIds(), signClipboardSelectedIds());
			refreshToolState();
			save();
		});
		clearButton = addToolButton(toolColumn, 3, () -> Text.translatable("gui.nanbin.clear").getString(),
				() -> ClientPacketHelper.openScreen(new RailwaySignClearConfirmScreen(this, Text.translatable("gui.nanbin.clear.question"), () -> {
					SignClipboard.clear(signPos, signClipboardLines(), length, signClipboardSignIds(), signClipboardSelectedIds());
					refreshToolState();
					save();
				})));
		undoButton = addToolButton(toolColumn, 4, () -> Text.translatable("gui.nanbin.undo").getString(), () -> {
			SignClipboard.undo(signPos, signClipboardLines(), length, signClipboardSignIds(), signClipboardSelectedIds());
			refreshToolState();
			save();
		});
		addToolButton(toolColumn, 5, () -> Text.translatable("gui.done").getString(), this::markScreenForClose);
	}

	private ButtonComponent addToolButton(UIContainer parent, int index, java.util.function.Supplier<String> labelSupplier, Runnable action) {
		final ButtonComponent button = new ButtonComponent(true);
		button.setChildOf(parent);
		button.setX(new PixelConstraint(0.0F));
		button.setY(new RelativeConstraint(index / TOOL_BUTTON_COUNT));
		button.setWidth(new PixelConstraint(TOOL_COLUMN_WIDTH));
		button.setHeight(new SubtractiveConstraint(new RelativeConstraint(1.0F / TOOL_BUTTON_COUNT), new PixelConstraint(1.0F)));
		button.setText(labelSupplier.get());
		button.onClick(action);
		return button;
	}

	// ---- 剪贴板桥接 ----
	// 站台信息屏按「行」存 selectedIds；铁路告示牌按「格」存。剪贴板统一按「行」比较，
	// 所以单行场景把格数据原样交给它（SignClipboard 会逐项复制）。

	private int signClipboardLines() {
		return selectedIdsPerCell ? 1 : lines;
	}

	private String[][] signClipboardSignIds() {
		return selectedIdsPerCell ? new String[][]{signIds[0]} : signIds;
	}

	private List<LongAVLTreeSet> signClipboardSelectedIds() {
		return selectedIds;
	}

	// ---- 交互 ----

	private void editCell(int line, int cellIndex) {
		if (line < 0 || line >= lines || cellIndex < 0 || cellIndex >= length) {
			return;
		}
		// 与 MTR 的预览一致：Shift + 左键删除该格
		if (net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
			signIds[line][cellIndex] = null;
			selectedSet(line, cellIndex).clear();
			refreshToolState();
			save();
			return;
		}
		editingLine = line;
		editingIndex = cellIndex;
		if (smallButtonsComponent != null) {
			smallButtonsComponent.setEditingIndex(cellIndex);
		}
		if (largeButtonsComponent != null) {
			largeButtonsComponent.setEditingIndex(cellIndex);
		}
		updatePreviewSelection();
	}

	/** 箭头只显示在当前编辑的那一行上（多行时其余行不显示，避免满屏箭头）。 */
	private void updatePreviewSelection() {
		for (int i = 0; i < previews.size(); i++) {
			previews.get(i).setEditingIndex(i == editingLine ? editingIndex : -1);
		}
	}

	private void onSignSelected(int cellIndex, SignResource sign) {
		if (sign == null || cellIndex < 0 || cellIndex >= length) {
			return;
		}
		final String lineIds[] = signIds[editingLine];
		lineIds[cellIndex] = sign.signId;

		// 本项目的自定义文本标志
		if ("nanbin_custom_text".equals(sign.signId) || "nanbin_custom_text_flipped".equals(sign.signId)) {
			ClientPacketHelper.openScreen(new CustomTextScreen(this, selectedSet(editingLine, cellIndex), this::save));
			return;
		}

		final MtrSignSelectors.DataType dataType = MtrSignSelectors.getDataType(sign.signId);
		if (dataType != MtrSignSelectors.DataType.NONE) {
			MtrSignSelectors.open(this, dataType, signPos, selectedSet(editingLine, cellIndex), true, this::save);
		}
	}

	private String getStyleScriptId() {
		for (final String[] lineIds : signIds) {
			final String scriptId = JSSignConfig.getStyleScriptId(lineIds);
			if (scriptId != null) {
				return scriptId;
			}
		}
		return null;
	}

	private String getStyleButtonLabel() {
		final String scriptId = getStyleScriptId();
		return scriptId == null ? Text.translatable("gui.nanbin.js_style.select").getString() : JSSignSelectorScreen.getScriptName(scriptId).getString();
	}

	private boolean hasJSStyle() {
		for (final String[] lineIds : signIds) {
			if (JSSignConfig.hasJSStyle(lineIds)) {
				return true;
			}
		}
		return false;
	}

	private void openJsStyleSelector() {
		ClientPacketHelper.openScreen(new JSSignSelectorScreen(this, getStyleScriptId(), scriptId -> {
			for (final String[] lineIds : signIds) {
				lineIds[0] = scriptId == null ? null : JSSignConfig.JS_STYLE_PREFIX + scriptId;
				for (int i = 1; i < lineIds.length; i++) {
					lineIds[i] = null;
				}
			}
			for (final LongAVLTreeSet ids : selectedIds) {
				ids.clear();
			}
			updateStyleButtonLabel();
			refreshToolState();
			save();
		}));
	}

	/**
	 * 撤回/粘贴的可用状态只在数据变动时才会变，所以在这里统一刷新。
	 * 之前只在构造时算过一次，导致「撤回」永远是灰的。
	 */
	private void refreshToolState() {
		final boolean locked = hasJSStyle();
		if (locked) {
			editingIndex = -1;
		}
		final boolean undoDisabledNow = locked || !SignClipboard.canUndo(signPos);
		final boolean pasteDisabledNow = locked || !SignClipboard.canPaste(signClipboardLines(), length);
		setDisabledIfChanged(undoButton, undoDisabledNow, undoDisabled);
		setDisabledIfChanged(copyButton, locked, copyDisabled);
		setDisabledIfChanged(pasteButton, pasteDisabledNow, pasteDisabledState);
		setDisabledIfChanged(clearButton, locked, clearDisabled);
		undoDisabled = undoDisabledNow;
		copyDisabled = locked;
		pasteDisabledState = pasteDisabledNow;
		clearDisabled = locked;
	}

	/** ButtonComponent 没有 disabled 的读取接口，自己记一份避免每 tick 反复设纹理。 */
	private void setDisabledIfChanged(ButtonComponent button, boolean disabled, boolean previous) {
		if (button != null && disabled != previous) {
			button.setDisabled(disabled);
		}
	}

	private void updateStyleButtonLabel() {
		if (styleButton != null) {
			styleButton.setText(getStyleButtonLabel());
		}
	}

	// ---- 生命周期 ----

	@Override
	public void onTick() {
		super.onTick();
		refreshToolState();
	}

	@Override
	public void onScreenClose() {
		save();
		super.onScreenClose();
	}

	private void save() {
		if (onSave != null) {
			onSave.run();
		}
	}
}
