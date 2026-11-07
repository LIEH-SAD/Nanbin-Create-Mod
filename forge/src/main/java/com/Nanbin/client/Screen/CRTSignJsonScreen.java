package com.Nanbin.client.Screen;

import com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorTheme;
import com.Nanbin.signage.crt.CRTSignLayout;
import org.lwjgl.glfw.GLFW;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.data.IGui;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 项目 JSON 的查看 / 编辑小屏幕（对应网页编辑器 §3.3.1 #11「项目」与 §7.1）。
 *
 * <p>导出：把 {@link CRTSignLayout#toJson()} 放进输入框，点「复制到剪贴板」或
 * Ctrl+A / Ctrl+C 复制（MC 原版输入框原生支持）。
 * 导入：粘贴/编辑 JSON 后点「应用导入」，由 {@link CRTSignLayout#fromJson(String)} 校验，
 * 通过后回调编辑器替换布局；超过数据包 {@code writeString} 上限（32767）的 JSON 直接拒绝。</p>
 *
 * <p>视觉沿用 {@link CRTEditorTheme} 的浅色网页风；输入框/按钮用 MTR 映射层控件，
 * 下方 JSON 全文预览是自绘的只读折行文本（单行输入框看不到全文）。</p>
 */
public final class CRTSignJsonScreen extends ScreenExtension implements IGui {

	/** 与 MTR 数据包 {@code PacketBuffer#writeString} 的默认上限一致。 */
	public static final int MAX_JSON_LENGTH = 32767;

	/** 屏幕用途：导出（复制布局 JSON）或导入（编辑后应用）。 */
	public enum Mode {
		EXPORT,
		IMPORT
	}

	private static final int MARGIN = 12;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 6;
	private static final int PREVIEW_TOP = 84;
	private static final int PREVIEW_LINE_HEIGHT = 10;
	private static final int STATUS_COLOR_ERROR = 0xFFB91C1C;
	private static final long STATUS_DURATION_MS = 4200L;

	private final ScreenExtension parent;
	private final Mode mode;
	private final String initialJson;
	private final Consumer<String> onApply;
	private final List<String> wrappedLines = new ArrayList<>();

	@Nullable
	private TextFieldWidgetExtension jsonField;
	@Nullable
	private String wrappedSource;
	private int wrappedWidth = -1;
	private int previewScroll;
	private int previewLeft;
	private int previewWidth;
	private int previewTop;
	private int previewHeight;
	private int buttonsTop;
	@Nullable
	private String statusText;
	private int statusColor = CRTEditorTheme.TEXT_DIM;
	private long statusUntil;

	/**
	 * @param parent      返回时重新打开的屏幕（编辑器）
	 * @param mode        导出 / 导入
	 * @param initialJson 输入框初始内容（导出 = 当前布局 JSON；导入 = 当前布局 JSON 供参考）
	 * @param onApply     导入确认回调；导出模式不会调用
	 */
	public CRTSignJsonScreen(ScreenExtension parent, Mode mode, @Nullable String initialJson, @Nullable Consumer<String> onApply) {
		super(TextHelper.translatable(mode == Mode.EXPORT ? "gui.nanbin.crt_editor.json.title_export" : "gui.nanbin.crt_editor.json.title_import"));
		this.parent = parent;
		this.mode = mode;
		this.initialJson = initialJson == null ? "" : initialJson;
		this.onApply = onApply;
	}

	@Override
	protected void init2() {
		super.init2();
		final int contentWidth = Math.max(80, width - MARGIN * 2);
		jsonField = new TextFieldWidgetExtension(MARGIN, 34, contentWidth, FIELD_HEIGHT, MAX_JSON_LENGTH, TextCase.DEFAULT, null, null);
		jsonField.setMaxLength2(MAX_JSON_LENGTH);
		jsonField.setText2(initialJson);
		jsonField.setChangedListener2(text -> invalidateWrap());
		addChild(new ClickableWidget(jsonField));

		buttonsTop = height - 30;
		final int buttonCount = 4;
		final int buttonWidth = Math.max(56, Math.min(150, (contentWidth - BUTTON_GAP * (buttonCount - 1)) / buttonCount));
		final int totalWidth = buttonWidth * buttonCount + BUTTON_GAP * (buttonCount - 1);
		int buttonX = Math.max(MARGIN, (width - totalWidth) / 2);
		final ButtonWidgetExtension copyButton = new ButtonWidgetExtension(buttonX, buttonsTop, buttonWidth, BUTTON_HEIGHT,
				TextHelper.translatable("gui.nanbin.crt_editor.json.copy"), button -> copyToClipboard());
		addChild(new ClickableWidget(copyButton));
		buttonX += buttonWidth + BUTTON_GAP;
		final ButtonWidgetExtension pasteButton = new ButtonWidgetExtension(buttonX, buttonsTop, buttonWidth, BUTTON_HEIGHT,
				TextHelper.translatable("gui.nanbin.crt_editor.json.paste"), button -> pasteFromClipboard());
		addChild(new ClickableWidget(pasteButton));
		buttonX += buttonWidth + BUTTON_GAP;
		final String applyLabel = mode == Mode.IMPORT ? "gui.nanbin.crt_editor.json.apply" : "gui.nanbin.crt_editor.json.done";
		final ButtonWidgetExtension applyButton = new ButtonWidgetExtension(buttonX, buttonsTop, buttonWidth, BUTTON_HEIGHT,
				TextHelper.translatable(applyLabel), button -> applyOrDone());
		addChild(new ClickableWidget(applyButton));
		buttonX += buttonWidth + BUTTON_GAP;
		final ButtonWidgetExtension cancelButton = new ButtonWidgetExtension(buttonX, buttonsTop, buttonWidth, BUTTON_HEIGHT,
				TextHelper.translatable("gui.nanbin.crt_editor.json.cancel"), button -> onClose2());
		addChild(new ClickableWidget(cancelButton));
	}

	@Override
	public void tick2() {
		if (jsonField != null) {
			jsonField.tick2();
		}
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
		renderBackground(graphicsHolder);
		drawLightBackground(graphicsHolder);
		super.render(graphicsHolder, mouseX, mouseY, delta);
		drawHeaderAndPreview(graphicsHolder, mouseX, mouseY);
	}

	/** 浅色网页风底：浅灰页面 + 白色卡片。 */
	private void drawLightBackground(GraphicsHolder graphicsHolder) {
		final GuiDrawing drawing = new GuiDrawing(graphicsHolder);
		drawing.beginDrawingRectangle();
		drawing.drawRectangle(0, 0, width, height, CRTEditorTheme.BG);
		drawing.drawRectangle(MARGIN - 4, 6, width - MARGIN + 4, height - 6, CRTEditorTheme.PANEL_BG);
		drawing.finishDrawingRectangle();
	}

	private void drawHeaderAndPreview(GraphicsHolder graphicsHolder, int mouseX, int mouseY) {
		final String title = TextHelper.translatable(mode == Mode.EXPORT
				? "gui.nanbin.crt_editor.json.title_export" : "gui.nanbin.crt_editor.json.title_import").getString();
		graphicsHolder.push();
		graphicsHolder.translate(width / 2.0F - GraphicsHolder.getTextWidth(title) * 0.675F, 10.0F, 0.0F);
		graphicsHolder.scale(1.35F, 1.35F, 1.0F);
		graphicsHolder.drawText(title, 0, 0, CRTEditorTheme.TEXT, false, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		final String hint = TextHelper.translatable("gui.nanbin.crt_editor.json.hint", MAX_JSON_LENGTH).getString();
		graphicsHolder.drawText(hint, MARGIN, 60, CRTEditorTheme.TEXT_DIM, false, GraphicsHolder.getDefaultLight());

		final TextFieldWidgetExtension field = jsonField;
		final String text = field == null ? "" : field.getText2();
		final String countText = String.format(Locale.ROOT, "%s / %s", text.length(), MAX_JSON_LENGTH);
		final int countWidth = GraphicsHolder.getTextWidth(countText);
		graphicsHolder.drawText(countText, width - MARGIN - countWidth, 60,
				text.length() > MAX_JSON_LENGTH ? STATUS_COLOR_ERROR : CRTEditorTheme.TEXT_DIM, false, GraphicsHolder.getDefaultLight());

		drawPreview(graphicsHolder);

		if (statusText != null && System.currentTimeMillis() <= statusUntil) {
			graphicsHolder.drawText(statusText, MARGIN, buttonsTop - 12, statusColor, false, GraphicsHolder.getDefaultLight());
		}
	}

	/** 自绘只读 JSON 全文预览（单行输入框看不全，这里按宽度折行 + 滚轮滚动）。 */
	private void drawPreview(GraphicsHolder graphicsHolder) {
		previewLeft = MARGIN;
		previewWidth = width - MARGIN * 2;
		previewTop = PREVIEW_TOP;
		previewHeight = Math.max(0, buttonsTop - 16 - previewTop);
		if (previewHeight < 16 || previewWidth < 24) {
			return;
		}
		final GuiDrawing drawing = new GuiDrawing(graphicsHolder);
		drawing.beginDrawingRectangle();
		drawing.drawRectangle(previewLeft, previewTop, previewLeft + previewWidth, previewTop + previewHeight, CRTEditorTheme.SURFACE_SOFT);
		drawing.drawRectangle(previewLeft, previewTop, previewLeft + previewWidth, previewTop + 1, CRTEditorTheme.BORDER);
		drawing.drawRectangle(previewLeft, previewTop + previewHeight - 1, previewLeft + previewWidth, previewTop + previewHeight, CRTEditorTheme.BORDER);
		drawing.drawRectangle(previewLeft, previewTop, previewLeft + 1, previewTop + previewHeight, CRTEditorTheme.BORDER);
		drawing.drawRectangle(previewLeft + previewWidth - 1, previewTop, previewLeft + previewWidth, previewTop + previewHeight, CRTEditorTheme.BORDER);
		drawing.finishDrawingRectangle();

		rebuildWrappedLines(previewWidth - 12);
		final String text = jsonField == null ? "" : jsonField.getText2();
		final int innerHeight = Math.max(1, previewHeight - 8);
		final int visibleLines = Math.max(1, innerHeight / PREVIEW_LINE_HEIGHT);
		previewScroll = Math.max(0, Math.min(previewScroll, Math.max(0, wrappedLines.size() - visibleLines)));
		final int textX = previewLeft + 6;
		final int firstY = previewTop + 5;
		if (text.isEmpty()) {
			graphicsHolder.drawText(TextHelper.translatable("gui.nanbin.crt_editor.json.empty").getString(), textX, firstY,
					CRTEditorTheme.BUTTON_DISABLED_TEXT, false, GraphicsHolder.getDefaultLight());
			return;
		}
		for (int i = 0; i < visibleLines && previewScroll + i < wrappedLines.size(); i++) {
			graphicsHolder.drawText(wrappedLines.get(previewScroll + i), textX, firstY + i * PREVIEW_LINE_HEIGHT,
					CRTEditorTheme.TEXT, false, GraphicsHolder.getDefaultLight());
		}
	}

	/** 按像素宽度把 JSON 折行（JSON 无空格语义，逐字符断行即可）。 */
	private void rebuildWrappedLines(int maxWidth) {
		final String text = jsonField == null ? "" : jsonField.getText2();
		if (text.equals(wrappedSource) && maxWidth == wrappedWidth) {
			return;
		}
		wrappedSource = text;
		wrappedWidth = maxWidth;
		wrappedLines.clear();
		if (text.isEmpty() || maxWidth <= 0) {
			return;
		}
		final int widthLimit = Math.max(8, maxWidth);
		final StringBuilder line = new StringBuilder();
		int lineWidth = 0;
		for (int i = 0; i < text.length() && wrappedLines.size() < 6000; i++) {
			final char chr = text.charAt(i);
			if (chr == '\n') {
				wrappedLines.add(line.toString());
				line.setLength(0);
				lineWidth = 0;
				continue;
			}
			final String chunk = String.valueOf(chr);
			final int charWidth = Math.max(1, GraphicsHolder.getTextWidth(chunk));
			if (lineWidth + charWidth > widthLimit && line.length() > 0) {
				wrappedLines.add(line.toString());
				line.setLength(0);
				lineWidth = 0;
			}
			line.append(chr);
			lineWidth += charWidth;
			if (line.length() >= 480) {
				wrappedLines.add(line.toString());
				line.setLength(0);
				lineWidth = 0;
			}
		}
		if (line.length() > 0) {
			wrappedLines.add(line.toString());
		}
	}

	private void invalidateWrap() {
		wrappedSource = null;
		wrappedWidth = -1;
		previewScroll = 0;
	}

	// ================================================================ 动作

	private void copyToClipboard() {
		final String text = jsonField == null ? "" : jsonField.getText2();
		if (text.isEmpty()) {
			setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.empty").getString(), true);
			return;
		}
		if (setClipboard(text)) {
			setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.copied").getString(), false);
		} else {
			setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.copy_failed").getString(), true);
		}
	}

	private void pasteFromClipboard() {
		final String value = getClipboard();
		if (value == null || value.trim().isEmpty()) {
			setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.paste_empty").getString(), true);
			return;
		}
		if (value.length() > MAX_JSON_LENGTH) {
			setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.too_long", MAX_JSON_LENGTH).getString(), true);
			return;
		}
		if (jsonField != null) {
			jsonField.setText2(value);
		}
		invalidateWrap();
		setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.pasted").getString(), false);
	}

	private void applyOrDone() {
		final String text = jsonField == null ? "" : jsonField.getText2();
		if (mode == Mode.IMPORT) {
			if (text.length() > MAX_JSON_LENGTH) {
				setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.too_long", MAX_JSON_LENGTH).getString(), true);
				return;
			}
			if (text.trim().isEmpty() || CRTSignLayout.fromJson(text) == null) {
				setStatus(TextHelper.translatable("gui.nanbin.crt_editor.json.invalid").getString(), true);
				return;
			}
			if (onApply != null) {
				onApply.accept(text);
			}
		}
		onClose2();
	}

	private void setStatus(String text, boolean error) {
		statusText = text;
		statusColor = error ? STATUS_COLOR_ERROR : CRTEditorTheme.OK;
		statusUntil = System.currentTimeMillis() + STATUS_DURATION_MS;
	}

	/** 系统剪贴板（GLFW；跨 MC 1.16.5–1.20.4 的稳定入口）。 */
	@Nullable
	private static String getClipboard() {
		try {
			final MinecraftClient client = MinecraftClient.getInstance();
			if (client == null) {
				return null;
			}
			return GLFW.glfwGetClipboardString(client.getWindow().getHandle());
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static boolean setClipboard(String text) {
		try {
			final MinecraftClient client = MinecraftClient.getInstance();
			if (client == null) {
				return false;
			}
			GLFW.glfwSetClipboardString(client.getWindow().getHandle(), text == null ? "" : text);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	// ================================================================ 事件

	@Override
	public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
		if (mouseX >= previewLeft && mouseX < previewLeft + previewWidth
				&& mouseY >= previewTop && mouseY < previewTop + previewHeight && previewHeight > 0) {
			previewScroll = Math.max(0, previewScroll - (int) Math.signum(amount) * 3);
			return true;
		}
		return super.mouseScrolled2(mouseX, mouseY, amount);
	}

	@Override
	public void onClose2() {
		if (parent != null) {
			MinecraftClient.getInstance().openScreen(new Screen(parent));
		} else {
			super.onClose2();
		}
	}

	@Override
	public boolean isPauseScreen2() {
		return false;
	}
}
