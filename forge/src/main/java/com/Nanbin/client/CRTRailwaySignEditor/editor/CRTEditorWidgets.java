package com.Nanbin.client.CRTRailwaySignEditor.editor;

import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * CRT 导视牌编辑器的自绘控件库（浅色现代风）。
 *
 * <p>视觉规范来自开源项目 CRT-Sign-Web-Editor 的 style.css（浅色 :root），
 * 由 {@link CRTEditorTheme} 统一提供颜色与尺寸 token：
 * 白面板 + 浅灰画布、蓝色强调 #2563EB、8px 圆角、危险操作红色、次级文字灰色。
 * 所有控件都用 {@link GuiDrawing} 画矩形、{@link GraphicsHolder} 画文字，
 * 不使用原版 ButtonWidget 的贴图外观。</p>
 *
 * <p>控件采用轻量保留模式：屏幕在状态变化时重建控件列表，鼠标/键盘事件转发给控件，
 * 控件自行维护输入框光标、滑条拖动、下拉展开等交互状态。</p>
 *
 * <p>来源与许可：界面组织参考 CRT-Sign-Web-Editor（Apache License 2.0），
 * 详见 docs/THIRD_PARTY_NOTICES.md。本类为 Minecraft 端 Java 独立实现，未复制其源码。</p>
 */
public final class CRTEditorWidgets {

	private CRTEditorWidgets() {
	}

	// ================================================================ 尺寸 token

	/** 控件之间的默认间距。 */
	public static final int GAP = 4;
	public static final int GAP_SMALL = 2;
	/** 普通按钮/输入框高度（MC 逻辑像素，网页 36px 控件折半）。 */
	public static final int CONTROL_HEIGHT = 16;
	public static final int INPUT_HEIGHT = 16;
	/** 一行说明文字的高度。 */
	public static final int LINE_HEIGHT = 10;
	/** 面板内边距。 */
	public static final int PANEL_PADDING = 8;
	/** 卡片圆角（网页 8px 在 MC 逻辑坐标下取 6）。 */
	public static final float RADIUS = 6.0F;
	public static final float RADIUS_SMALL = 4.0F;

	/** 字体行高：MC 默认字体固定 9px（含下伸）。 */
	public static final int FONT_HEIGHT = 9;

	// ── 字号层级（相对 9px 底字；网页 style.css 的 14/12/11px 体系在 MC 里的近似） ──

	/** 面板标题（网页 .panel-title 14px）≈ 1.35×。 */
	public static final float FONT_SCALE_TITLE = 1.35F;
	/** 分组/小标题（网页 .palette-category-title 12px）≈ 1.15×。 */
	public static final float FONT_SCALE_GROUP = 1.15F;
	/** 正文（字段标签/卡片名）1.0×。 */
	public static final float FONT_SCALE_BODY = 1.0F;
	/** 说明文字（网页 11px hint）≈ 0.85×，搭配 {@link CRTEditorTheme#TEXT_DIM}。 */
	public static final float FONT_SCALE_HINT = 0.85F;

	// ================================================================ 预设调色板

	/** 颜色选择器展开后的预设色板（网页色板简化版，12 色）。 */
	public static final int[] PALETTE_COLORS = {
			0xFF000000, 0xFFFFFFFF, 0xFFE4002B, 0xFFFCD600,
			0xFF00A650, 0xFF00B5AD, 0xFF2563EB, 0xFF7C3AED,
			0xFFF97316, 0xFFF7D917, 0xFF9AA3AD, 0xFF4A4A4A
	};
	public static final String[] PALETTE_NAMES = {
			"黑", "白", "红", "黄", "绿", "青", "蓝", "紫", "橙", "出口黄", "灰", "深灰"
	};

	// ================================================================ 焦点宿主

	/** 屏幕实现的焦点/弹层管理回调。 */
	public interface FocusHost {

		void requestFocus(Widget widget);

		@Nullable
		Widget getFocusedWidget();

		void clearFocus();

		/** 某个控件的尺寸/结构变化，要求屏幕重建控件布局。 */
		void widgetLayoutChanged();

		void openDropdown(Dropdown dropdown);

		@Nullable
		Dropdown getOpenDropdown();

		void closeDropdown();
	}

	// ================================================================ 绘制上下文

	/**
	 * 一帧的绘制上下文：除了绘图工具，还负责 tooltip 收集与焦点查询。
	 * 鼠标/键盘事件里用一个 graphics 为 null 的上下文即可（绘制方法会自动跳过）。
	 */
	public static final class Ctx {

		@Nullable
		public final GraphicsHolder graphics;
		@Nullable
		public final TextRenderer textRenderer;
		public final int mouseX;
		public final int mouseY;
		public final float delta;
		public final FocusHost host;
		@Nullable
		private GuiDrawing guiDrawing;
		@Nullable
		public String tooltip;

		public Ctx(@Nullable GraphicsHolder graphics, @Nullable TextRenderer textRenderer, int mouseX, int mouseY, float delta, FocusHost host) {
			this.graphics = graphics;
			this.textRenderer = textRenderer;
			this.mouseX = mouseX;
			this.mouseY = mouseY;
			this.delta = delta;
			this.host = host;
			if (graphics != null) {
				guiDrawing = new GuiDrawing(graphics);
			}
		}

		private GuiDrawing drawing() {
			if (guiDrawing == null && graphics != null) {
				guiDrawing = new GuiDrawing(graphics);
			}
			//noinspection ConstantConditions — 调用方都先检查 graphics != null
			return guiDrawing;
		}

		public int textWidth(@Nullable String text) {
			if (text == null || text.isEmpty()) {
				return 0;
			}
			return GraphicsHolder.getTextWidth(text);
		}

		public int textWidth(@Nullable TextRenderer renderer, @Nullable String text) {
			if (text == null || text.isEmpty()) {
				return 0;
			}
			return renderer == null ? textWidth(text) : renderer.getWidth(text);
		}

		public void fill(int x1, int y1, int x2, int y2, int color) {
			if (graphics == null || x2 <= x1 || y2 <= y1) {
				return;
			}
			final GuiDrawing d = drawing();
			d.beginDrawingRectangle();
			d.drawRectangle(x1, y1, x2, y2, color);
			d.finishDrawingRectangle();
		}

		/** 圆角矩形填充：用横向 1px 条带近似圆角（MC 没有圆角矩形 API）。 */
		public void fillRound(int x, int y, int w, int h, float radius, int color) {
			if (graphics == null || w <= 0 || h <= 0) {
				return;
			}
			final GuiDrawing d = drawing();
			d.beginDrawingRectangle();
			roundRectPieces(d, x, y, w, h, radius, color);
			d.finishDrawingRectangle();
		}

		/** 1px 圆角描边 + 填充。 */
		public void fillRoundBordered(int x, int y, int w, int h, float radius, int fill, int border) {
			if (graphics == null || w <= 0 || h <= 0) {
				return;
			}
			final GuiDrawing d = drawing();
			d.beginDrawingRectangle();
			roundRectPieces(d, x, y, w, h, radius, border);
			if (w > 2 && h > 2) {
				roundRectPieces(d, x + 1, y + 1, w - 2, h - 2, Math.max(0.0F, radius - 1.0F), fill);
			}
			d.finishDrawingRectangle();
		}

		public void border(int x1, int y1, int x2, int y2, int color) {
			if (graphics == null || x2 <= x1 || y2 <= y1) {
				return;
			}
			final GuiDrawing d = drawing();
			d.beginDrawingRectangle();
			d.drawRectangle(x1, y1, x2, y1 + 1, color);
			d.drawRectangle(x1, y2 - 1, x2, y2, color);
			d.drawRectangle(x1, y1, x1 + 1, y2, color);
			d.drawRectangle(x2 - 1, y1, x2, y2, color);
			d.finishDrawingRectangle();
		}

		/** 虚线矩形（用于「空白占位」缩略图、拖放无效高亮等）。 */
		public void dashedRect(int x1, int y1, int x2, int y2, int color) {
			if (graphics == null || x2 <= x1 || y2 <= y1) {
				return;
			}
			final GuiDrawing d = drawing();
			d.beginDrawingRectangle();
			dashHorizontal(d, x1, x2, y1, color);
			dashHorizontal(d, x1, x2, y2 - 1, color);
			dashVertical(d, y1 + 2, y2 - 2, x1, color);
			dashVertical(d, y1 + 2, y2 - 2, x2 - 1, color);
			d.finishDrawingRectangle();
		}

		private static void dashHorizontal(GuiDrawing d, int x1, int x2, int y, int color) {
			for (int x = x1; x < x2; x += 7) {
				d.drawRectangle(x, y, Math.min(x2, x + 4), y + 1, color);
			}
		}

		private static void dashVertical(GuiDrawing d, int y1, int y2, int x, int color) {
			for (int y = y1; y < y2; y += 7) {
				d.drawRectangle(x, y, x + 1, Math.min(y2, y + 4), color);
			}
		}

		private static void roundRectPieces(GuiDrawing d, float x, float y, float w, float h, float radius, int color) {
			final float r = Math.min(radius, Math.min(w / 2.0F, h / 2.0F));
			if (r <= 0.5F) {
				d.drawRectangle(x, y, x + w, y + h, color);
				return;
			}
			d.drawRectangle(x, y + r, x + w, y + h - r, color);
			final int rows = (int) Math.ceil(r);
			for (int i = 0; i < rows; i++) {
				final float dy = r - i - 0.5F;
				final float inset = r - (float) Math.sqrt(Math.max(0.0F, r * r - dy * dy));
				d.drawRectangle(x + inset, y + i, x + w - inset, y + i + 1, color);
				d.drawRectangle(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
			}
		}

		public void text(@Nullable String text, int x, int y, int color) {
			if (graphics == null || text == null || text.isEmpty()) {
				return;
			}
			graphics.drawText(text, x, y, color, false, GraphicsHolder.getDefaultLight());
		}

		public void textCentered(@Nullable String text, int centerX, int y, int color) {
			text(text, centerX - textWidth(text) / 2, y, color);
		}

		public void textRight(@Nullable String text, int right, int y, int color) {
			text(text, right - textWidth(text), y, color);
		}

		/** 单行裁剪绘制（超宽用省略号不必要，直接 trim 到宽度）。 */
		public void textClipped(@Nullable String text, int x, int y, int maxWidth, int color) {
			if (graphics == null || text == null || text.isEmpty() || maxWidth <= 0) {
				return;
			}
			final TextRenderer renderer = textRenderer;
			final String clipped = renderer == null ? text : renderer.trimToWidth(text, maxWidth);
			text(clipped, x, y, color);
		}

		public void textCenteredClipped(@Nullable String text, int centerX, int y, int maxWidth, int color) {
			if (graphics == null || text == null || maxWidth <= 0) {
				return;
			}
			final TextRenderer renderer = textRenderer;
			final String clipped = renderer == null ? text : renderer.trimToWidth(text, Math.max(0, maxWidth - 2));
			textCentered(clipped, centerX, y, color);
		}

		/** 缩放绘制文字（卡片图标、画布内元素文字用）。 */
		public void scaledText(@Nullable String text, int x, int y, float scale, int color) {
			if (graphics == null || text == null || text.isEmpty() || scale <= 0.0F) {
				return;
			}
			graphics.push();
			graphics.translate(x, y, 0);
			graphics.scale(scale, scale, 1.0F);
			graphics.drawText(text, 0, 0, color, false, GraphicsHolder.getDefaultLight());
			graphics.pop();
		}

		public int scaledTextWidth(@Nullable String text, float scale) {
			return Math.round(textWidth(text) * scale);
		}

		/** 缩放绘制 + 宽度裁剪（用于面板标题/分组标题等可能超长的文字）。 */
		public void scaledTextClipped(@Nullable String text, int x, int y, int maxWidth, float scale, int color) {
			if (graphics == null || text == null || text.isEmpty() || maxWidth <= 0 || scale <= 0.0F) {
				return;
			}
			final TextRenderer renderer = textRenderer;
			final int allowed = Math.max(1, Math.round(maxWidth / scale));
			final String clipped = renderer == null ? text : renderer.trimToWidth(text, allowed);
			scaledText(clipped, x, y, scale, color);
		}

		/** 缩放绘制 + 居中 + 宽度裁剪。 */
		public void scaledTextCenteredClipped(@Nullable String text, int centerX, int y, int maxWidth, float scale, int color) {
			if (graphics == null || text == null || text.isEmpty() || maxWidth <= 0 || scale <= 0.0F) {
				return;
			}
			final TextRenderer renderer = textRenderer;
			final int allowed = Math.max(1, Math.round(maxWidth / scale));
			final String clipped = renderer == null ? text : renderer.trimToWidth(text, allowed);
			scaledText(clipped, centerX - scaledTextWidth(clipped, scale) / 2, y, scale, color);
		}

		/** 缩放绘制 + 右对齐。 */
		public void scaledTextRight(@Nullable String text, int right, int y, float scale, int color) {
			scaledText(text, right - scaledTextWidth(text, scale), y, scale, color);
		}

		public void setTooltip(@Nullable String value) {
			if (value != null && !value.isEmpty()) {
				tooltip = value;
			}
		}

		public boolean isFocused(Widget widget) {
			return host.getFocusedWidget() == widget;
		}
	}

	// ================================================================ 控件基类

	/** 所有自绘控件的基类；坐标均为屏幕绝对坐标（含面板滚动偏移）。 */
	public abstract static class Widget {

		public int x;
		public int y;
		public int w;
		public int h;
		public boolean visible = true;
		public boolean enabled = true;
		@Nullable
		public String tooltip;

		public Widget(int w, int h) {
			this.w = w;
			this.h = h;
		}

		public Widget setBounds(int x, int y, int w) {
			this.x = x;
			this.y = y;
			this.w = w;
			return this;
		}

		/** 控件当前占用的高度（展开色板等可变高度控件会覆盖）。 */
		public int getHeight() {
			return h;
		}

		public boolean isOver(double mouseX, double mouseY) {
			return visible && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		}

		public abstract void render(Ctx c);

		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			return false;
		}

		public void mouseReleased(Ctx c, double mouseX, double mouseY, int button) {
		}

		public boolean mouseDragged(Ctx c, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
			return false;
		}

		public boolean mouseScrolled(Ctx c, double mouseX, double mouseY, double amount) {
			return false;
		}

		public boolean keyPressed(Ctx c, int keyCode, int scanCode, int modifiers) {
			return false;
		}

		public boolean charTyped(Ctx c, char chr, int modifiers) {
			return false;
		}

		public boolean wantsKeyboard() {
			return false;
		}

		public void onBlur(Ctx c) {
		}

		public void cancelPress() {
		}

		protected void hoverTooltip(Ctx c) {
			if (tooltip != null && isOver(c.mouseX, c.mouseY)) {
				c.setTooltip(tooltip);
			}
		}
	}

	// ================================================================ 按钮

	public enum ButtonStyle {
		/** 主按钮：蓝底白字。 */
		PRIMARY,
		/** 次按钮：白底、灰边、hover 变蓝。 */
		SECONDARY,
		/** 危险按钮：红底白字。 */
		DANGER,
		/** 工具条小按钮：白底灰边，hover 蓝字浅蓝底。 */
		TOOL,
		/** 纯图标按钮：无边框，hover 浅蓝底。 */
		ICON,
		/** 虚线按钮（添加行 / 添加线路）。 */
		DASHED
	}

	/** 自绘按钮；按下后在同一次点击的抬起时触发（避免拖拽误触）。 */
	public static final class Button extends Widget {

		@Nullable
		public String label;
		public ButtonStyle style;
		@Nullable
		public Runnable action;
		public boolean selected;
		private boolean pressed;

		public Button(int w, int h, @Nullable String label, ButtonStyle style, @Nullable Runnable action) {
			super(w, h);
			this.label = label;
			this.style = style;
			this.action = action;
		}

		public Button setSelected(boolean selected) {
			this.selected = selected;
			return this;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			final boolean down = pressed && hover;
			int bg;
			int border;
			int fg;
			switch (style) {
				case PRIMARY:
					bg = !enabled ? 0xFFE8ECF1 : (hover ? 0xFF1D4FD7 : CRTEditorTheme.ACCENT);
					border = bg;
					fg = !enabled ? CRTEditorTheme.BUTTON_DISABLED_TEXT : 0xFFFFFFFF;
					break;
				case DANGER:
					bg = !enabled ? 0xFFF4E4E4 : (hover ? 0xFFC02020 : CRTEditorTheme.DANGER);
					border = bg;
					fg = !enabled ? 0xFFC9A9A9 : 0xFFFFFFFF;
					break;
				case TOOL:
					bg = selected || hover ? CRTEditorTheme.ACCENT_SOFT : CRTEditorTheme.BUTTON_BG;
					border = selected || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER;
					fg = selected || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM;
					break;
				case ICON:
					bg = selected || hover ? CRTEditorTheme.ACCENT_SOFT : 0x00000000;
					border = 0x00000000;
					fg = selected || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM;
					break;
				case DASHED:
					bg = hover ? 0xFFFFFFFF : 0xB8FFFFFF;
					border = hover ? CRTEditorTheme.ACCENT : 0xFFB6C0CC;
					fg = hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM;
					break;
				case SECONDARY:
				default:
					bg = !enabled ? CRTEditorTheme.SURFACE_SOFT : (hover ? CRTEditorTheme.BUTTON_BG_HOVER : CRTEditorTheme.BUTTON_BG);
					border = hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER;
					fg = !enabled ? CRTEditorTheme.BUTTON_DISABLED_TEXT : (hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
					break;
			}
			if (style == ButtonStyle.ICON) {
				if (bg != 0) {
					c.fillRound(x, y, w, h, RADIUS_SMALL, bg);
				}
			} else if (style == ButtonStyle.DASHED) {
				c.fillRound(x, y, w, h, RADIUS_SMALL, bg);
				c.dashedRect(x, y, x + w, y + h, border);
			} else {
				c.fillRoundBordered(x, y, w, h, RADIUS, bg, border);
				if (down) {
					c.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFE9EDF3);
				}
			}
			if (label != null && !label.isEmpty()) {
				final int textColor = fg;
				if (style == ButtonStyle.ICON) {
					c.textCentered(label, x + w / 2, y + (h - FONT_HEIGHT) / 2 + 1, textColor);
				} else {
					c.textCenteredClipped(label, x + w / 2, y + (h - FONT_HEIGHT) / 2 + 1, w - 6, textColor);
				}
			}
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			pressed = true;
			return true;
		}

		@Override
		public void mouseReleased(Ctx c, double mouseX, double mouseY, int button) {
			if (button == 0) {
				final boolean activate = pressed && enabled && isOver(mouseX, mouseY);
				pressed = false;
				if (activate && action != null) {
					action.run();
				}
			}
		}

		@Override
		public void cancelPress() {
			pressed = false;
		}
	}

	// ================================================================ 复选框

	public static final class Checkbox extends Widget {

		public final String label;
		private final BooleanSupplier getter;
		private final Consumer<Boolean> setter;

		public Checkbox(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
			super(0, CONTROL_HEIGHT);
			this.label = label;
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final boolean checked = getter.getAsBoolean();
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			final int boxY = y + (h - 11) / 2;
			if (checked) {
				c.fillRoundBordered(x, boxY, 11, 11, 2.5F, CRTEditorTheme.ACCENT, CRTEditorTheme.ACCENT);
				// 白色对勾（像素点近似）
				for (int i = 0; i < 3; i++) {
					c.fill(x + 2 + i, boxY + 4 + i, x + 3 + i, boxY + 6 + i, 0xFFFFFFFF);
				}
				for (int i = 0; i < 4; i++) {
					c.fill(x + 4 + i, boxY + 7 - i, x + 5 + i, boxY + 9 - i, 0xFFFFFFFF);
				}
			} else {
				c.fillRoundBordered(x, boxY, 11, 11, 2.5F, hover ? CRTEditorTheme.BUTTON_BG_HOVER : 0xFFFFFFFF, hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			}
			c.textClipped(label, x + 16, y + (h - FONT_HEIGHT) / 2 + 1, Math.max(0, w - 16), enabled ? CRTEditorTheme.TEXT : CRTEditorTheme.BUTTON_DISABLED_TEXT);
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			setter.accept(!getter.getAsBoolean());
			return true;
		}
	}

	// ================================================================ 分段选择

	/** 分段按钮组（对齐方式、宽度模式、旋转角度等）。 */
	public static final class Segmented extends Widget {

		public final String[] labels;
		private final IntSupplier getter;
		private final IntConsumer setter;
		public boolean compact;

		public Segmented(String[] labels, IntSupplier getter, IntConsumer setter) {
			super(0, CONTROL_HEIGHT);
			this.labels = labels;
			this.getter = getter;
			this.setter = setter;
		}

		public Segmented setCompact(boolean compact) {
			this.compact = compact;
			return this;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			c.fillRoundBordered(x, y, w, h, RADIUS, CRTEditorTheme.SURFACE_SOFT, CRTEditorTheme.BORDER);
			final int selected = getter.getAsInt();
			final int segmentWidth = labels.length == 0 ? w : (w - 2) / labels.length;
			for (int i = 0; i < labels.length; i++) {
				final int sx = x + 1 + i * segmentWidth;
				final int ex = i == labels.length - 1 ? x + w - 1 : sx + segmentWidth;
				final boolean active = i == selected;
				final boolean hover = enabled && !active && c.mouseX >= sx && c.mouseX < ex && c.mouseY >= y && c.mouseY < y + h;
				if (active) {
					c.fillRound(sx + 1, y + 2, ex - sx - 2, h - 4, RADIUS_SMALL - 1, CRTEditorTheme.ACCENT);
				} else if (hover) {
					c.fillRound(sx + 1, y + 2, ex - sx - 2, h - 4, RADIUS_SMALL - 1, 0xFFFFFFFF);
				}
				final int color = active ? 0xFFFFFFFF : (hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
				c.textCenteredClipped(labels[i], (sx + ex) / 2, y + (h - FONT_HEIGHT) / 2 + 1, ex - sx - 4, color);
			}
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY) || labels.length == 0) {
				return false;
			}
			final int segmentWidth = (w - 2) / labels.length;
			final int index = Math.max(0, Math.min(labels.length - 1, (int) ((mouseX - x - 1) / Math.max(1, segmentWidth))));
			setter.accept(index);
			return true;
		}
	}

	// ================================================================ 滑条

	/** 水平滑条：左侧轨道 + 右侧数值文本。 */
	public static final class Slider extends Widget {

		private final double min;
		private final double max;
		private final double step;
		private final int decimals;
		private final DoubleSupplier getter;
		private final DoubleConsumer setter;
		private final int valueWidth;
		private boolean dragging;

		public Slider(double min, double max, double step, int decimals, int valueWidth, DoubleSupplier getter, DoubleConsumer setter) {
			super(0, CONTROL_HEIGHT);
			this.min = min;
			this.max = max;
			this.step = step;
			this.decimals = decimals;
			this.valueWidth = valueWidth;
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final int trackX = x;
			final int trackW = Math.max(12, w - valueWidth - GAP);
			final int trackY = y + h / 2 - 2;
			final double value = clamp(getter.getAsDouble(), min, max);
			final float ratio = max <= min ? 0.0F : (float) ((value - min) / (max - min));
			c.fillRound(trackX, trackY, trackW, 4, 2.0F, CRTEditorTheme.SURFACE_SOFT);
			c.fillRound(trackX, trackY, Math.max(2, Math.round(trackW * ratio)), 4, 2.0F, enabled ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			final int knobX = trackX + Math.round((trackW - 6) * ratio);
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			c.fillRoundBordered(knobX, y + 3, 6, h - 6, 3.0F, 0xFFFFFFFF, hover || dragging ? CRTEditorTheme.ACCENT : 0xFFC3CCD6);
			c.textRight(format(value), x + w, y + (h - FONT_HEIGHT) / 2 + 1, enabled ? CRTEditorTheme.TEXT_DIM : CRTEditorTheme.BUTTON_DISABLED_TEXT);
			hoverTooltip(c);
		}

		private String format(double value) {
			if (decimals <= 0) {
				return String.valueOf((int) Math.round(value));
			}
			return String.format(Locale.ROOT, "%." + decimals + "f", value);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			dragging = true;
			apply(mouseX);
			return true;
		}

		@Override
		public boolean mouseDragged(Ctx c, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
			if (dragging && button == 0) {
				apply(mouseX);
				return true;
			}
			return false;
		}

		@Override
		public void mouseReleased(Ctx c, double mouseX, double mouseY, int button) {
			dragging = false;
		}

		@Override
		public void cancelPress() {
			dragging = false;
		}

		private void apply(double mouseX) {
			final int trackW = Math.max(12, w - valueWidth - GAP);
			final double ratio = clamp((mouseX - x) / Math.max(1, trackW), 0.0, 1.0);
			double value = min + ratio * (max - min);
			if (step > 0.0) {
				value = Math.round(value / step) * step;
			}
			setter.accept(clamp(value, min, max));
		}
	}

	// ================================================================ 数字步进

	/**
	 * 数字步进控件：[-] 数值 [+]；点击数值区可键盘输入，上下键步进。
	 * 用于行高、宽度、灰框宽度、内边距等数值字段。
	 */
	public static final class Stepper extends Widget {

		private final boolean decimalMode;
		private final double min;
		private final double max;
		private final double step;
		private final int decimals;
		private final DoubleSupplier getter;
		private final DoubleConsumer setter;
		public String suffix = "";
		@Nullable
		private String edit;
		private boolean pressedMinus;
		private boolean pressedPlus;

		private Stepper(boolean decimalMode, double min, double max, double step, int decimals, DoubleSupplier getter, DoubleConsumer setter) {
			super(0, CONTROL_HEIGHT);
			this.decimalMode = decimalMode;
			this.min = min;
			this.max = max;
			this.step = step;
			this.decimals = decimals;
			this.getter = getter;
			this.setter = setter;
		}

		public static Stepper integer(int min, int max, int step, IntSupplier getter, IntConsumer setter) {
			return new Stepper(false, min, max, step, 0, () -> getter.getAsInt(), value -> setter.accept((int) Math.round(value)));
		}

		public static Stepper decimal(double min, double max, double step, int decimals, DoubleSupplier getter, DoubleConsumer setter) {
			return new Stepper(true, min, max, step, decimals, getter, setter);
		}

		public Stepper setSuffix(String suffix) {
			this.suffix = suffix == null ? "" : suffix;
			return this;
		}

		private double current() {
			return clamp(getter.getAsDouble(), min, max);
		}

		private String display() {
			if (edit != null) {
				return edit;
			}
			final double value = current();
			if (decimalMode) {
				return String.format(Locale.ROOT, "%." + decimals + "f", value) + suffix;
			}
			return String.valueOf((int) Math.round(value)) + suffix;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final boolean focused = c.isFocused(this) && edit != null;
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			c.fillRoundBordered(x, y, w, h, RADIUS, focused ? 0xFFFFFFFF : (enabled ? CRTEditorTheme.CONTROL_BG : CRTEditorTheme.SURFACE_SOFT),
					focused ? CRTEditorTheme.ACCENT : (hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER));
			final int buttonWidth = Math.min(h, w / 3);
			final boolean overMinus = enabled && c.mouseX >= x && c.mouseX < x + buttonWidth && c.mouseY >= y && c.mouseY < y + h;
			final boolean overPlus = enabled && c.mouseX >= x + w - buttonWidth && c.mouseX < x + w && c.mouseY >= y && c.mouseY < y + h;
			if (overMinus || pressedMinus) {
				c.fillRound(x + 1, y + 1, buttonWidth - 1, h - 2, RADIUS_SMALL - 1, CRTEditorTheme.ACCENT_SOFT);
			}
			if (overPlus || pressedPlus) {
				c.fillRound(x + w - buttonWidth, y + 1, buttonWidth - 1, h - 2, RADIUS_SMALL - 1, CRTEditorTheme.ACCENT_SOFT);
			}
			c.textCentered("−", x + buttonWidth / 2, y + (h - FONT_HEIGHT) / 2 + 1, enabled ? CRTEditorTheme.TEXT_DIM : CRTEditorTheme.BUTTON_DISABLED_TEXT);
			c.textCentered("+", x + w - buttonWidth / 2, y + (h - FONT_HEIGHT) / 2 + 1, enabled ? CRTEditorTheme.TEXT_DIM : CRTEditorTheme.BUTTON_DISABLED_TEXT);
			final int textX = x + buttonWidth + 3;
			final int maxTextWidth = Math.max(0, w - buttonWidth * 2 - 6);
			c.textClipped(display(), textX, y + (h - FONT_HEIGHT) / 2 + 1, maxTextWidth, enabled ? CRTEditorTheme.TEXT : CRTEditorTheme.BUTTON_DISABLED_TEXT);
			if (focused) {
				final int caret = textX + Math.min(c.textWidth(edit) + (suffix.isEmpty() ? 0 : 0), maxTextWidth);
				c.fill(caret, y + 3, caret + 1, y + h - 3, CRTEditorTheme.ACCENT);
			}
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			final int buttonWidth = Math.min(h, w / 3);
			if (mouseX < x + buttonWidth) {
				pressedMinus = true;
				apply(current() - step);
				return true;
			}
			if (mouseX >= x + w - buttonWidth) {
				pressedPlus = true;
				apply(current() + step);
				return true;
			}
			if (c.host.getFocusedWidget() != this) {
				c.host.requestFocus(this);
			}
			edit = decimalMode ? String.format(Locale.ROOT, "%." + decimals + "f", current()) : String.valueOf((int) Math.round(current()));
			return true;
		}

		@Override
		public void mouseReleased(Ctx c, double mouseX, double mouseY, int button) {
			pressedMinus = false;
			pressedPlus = false;
		}

		@Override
		public boolean keyPressed(Ctx c, int keyCode, int scanCode, int modifiers) {
			if (edit == null) {
				return false;
			}
			switch (keyCode) {
				case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:
					if (!edit.isEmpty()) {
						edit = edit.substring(0, edit.length() - 1);
					}
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER:
				case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER:
					commit(c);
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE:
					edit = null;
					c.host.clearFocus();
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_UP:
					apply(current() + step);
					edit = decimalMode ? String.format(Locale.ROOT, "%." + decimals + "f", current()) : String.valueOf((int) Math.round(current()));
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN:
					apply(current() - step);
					edit = decimalMode ? String.format(Locale.ROOT, "%." + decimals + "f", current()) : String.valueOf((int) Math.round(current()));
					return true;
				default:
					return false;
			}
		}

		@Override
		public boolean charTyped(Ctx c, char chr, int modifiers) {
			if (edit == null) {
				return false;
			}
			if (chr >= '0' && chr <= '9' || chr == '-' || chr == '.' || chr == ',') {
				final String candidate = edit + (chr == ',' ? '.' : chr);
				if (candidate.length() <= 9) {
					edit = candidate;
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean wantsKeyboard() {
			return edit != null;
		}

		@Override
		public void onBlur(Ctx c) {
			commit(c);
		}

		private void commit(Ctx c) {
			if (edit == null) {
				return;
			}
			try {
				final double value = Double.parseDouble(edit.replace(",", "."));
				apply(value);
			} catch (NumberFormatException ignored) {
			}
			edit = null;
			c.host.clearFocus();
		}

		private void apply(double value) {
			setter.accept(clamp(value, min, max));
		}
	}

	// ================================================================ 工具

	public static double clamp(double value, double min, double max) {
		return value < min ? min : (value > max ? max : value);
	}

	public static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	/** 数字转网页端颜色串（#RRGGBB / #AARRGGBB）。 */
	public static String toHex(int argb) {
		final int alpha = (argb >>> 24) & 0xFF;
		return alpha == 0xFF ? String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF) : String.format(Locale.ROOT, "#%08X", argb);
	}

	/** 解析 #RRGGBB / #AARRGGBB（# 可省）；失败返回 null。 */
	@Nullable
	public static Integer parseHex(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String hex = value.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		if (hex.length() != 6 && hex.length() != 8) {
			return null;
		}
		try {
			final long parsed = Long.parseLong(hex, 16);
			return hex.length() == 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	/** 混合颜色：用于批量属性显示「不同」状态。 */
	public static int blend(int from, int to, float amount) {
		final float t = Math.max(0.0F, Math.min(1.0F, amount));
		final int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		final int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		final int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		final int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ================================================================ 文本输入框

	/**
	 * 自绘单行输入框：点击聚焦、闪烁光标、退格/删除/左右移动、回车失焦、
	 * 支持按字符过滤（hex / 数字）与最大长度。
	 */
	public static final class TextField extends Widget {

		public String placeholder = "";
		public int maxLength = 64;
		public boolean hexOnly;
		public boolean numericOnly;
		@Nullable
		public Runnable onCommit;
		private final Supplier<String> getter;
		private final Consumer<String> onChange;
		@Nullable
		private String text;
		private int cursor;
		private long blinkStart;

		public TextField(Supplier<String> getter, Consumer<String> onChange) {
			super(0, INPUT_HEIGHT);
			this.getter = getter;
			this.onChange = onChange;
		}

		public TextField setPlaceholder(String placeholder) {
			this.placeholder = placeholder == null ? "" : placeholder;
			return this;
		}

		public TextField setMaxLength(int maxLength) {
			this.maxLength = Math.max(1, maxLength);
			return this;
		}

		public TextField setHexOnly(boolean hexOnly) {
			this.hexOnly = hexOnly;
			return this;
		}

		public TextField setNumericOnly(boolean numericOnly) {
			this.numericOnly = numericOnly;
			return this;
		}

		/** 当前编辑值（焦点内使用本地副本，失焦后回读外部状态）。 */
		private String currentText() {
			if (text == null) {
				final String value = getter.get();
				text = value == null ? "" : value;
				if (text.length() > maxLength) {
					text = text.substring(0, maxLength);
				}
				cursor = text.length();
			}
			return text;
		}

		private boolean acceptChar(char chr) {
			if (chr < ' ' || chr == 0x7F) {
				return false;
			}
			if (hexOnly) {
				return chr == '#' || chr >= '0' && chr <= '9' || chr >= 'a' && chr <= 'f' || chr >= 'A' && chr <= 'F';
			}
			if (numericOnly) {
				return chr >= '0' && chr <= '9' || chr == '-' || chr == '.' || chr == ',';
			}
			return true;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final boolean focused = c.isFocused(this);
			if (!focused) {
				text = null;
				cursor = 0;
			}
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			c.fillRoundBordered(x, y, w, h, RADIUS, enabled ? CRTEditorTheme.CONTROL_BG : CRTEditorTheme.SURFACE_SOFT,
					focused ? CRTEditorTheme.ACCENT : (hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER));
			final String value = currentText();
			final int innerX = x + 4;
			final int innerWidth = Math.max(1, w - 8);
			final int textY = y + (h - FONT_HEIGHT) / 2 + 1;
			if (value.isEmpty() && !focused) {
				c.textClipped(placeholder, innerX, textY, innerWidth, CRTEditorTheme.BUTTON_DISABLED_TEXT);
				return;
			}
			if (!focused) {
				c.textClipped(value, innerX, textY, innerWidth, enabled ? CRTEditorTheme.TEXT : CRTEditorTheme.BUTTON_DISABLED_TEXT);
				return;
			}
			// 焦点态：保证光标在可视范围内
			int start = 0;
			int cursorOffset = c.textWidth(value.substring(0, Math.min(cursor, value.length())));
			while (start < cursor && cursorOffset > innerWidth - 4) {
				start++;
				cursorOffset -= c.textWidth(String.valueOf(value.charAt(start - 1)));
			}
			final String visibleText = value.substring(Math.min(start, value.length()));
			c.textClipped(visibleText, innerX, textY, innerWidth - 1, CRTEditorTheme.TEXT);
			final boolean caretVisible = (System.currentTimeMillis() - blinkStart) % 1000L < 550L;
			if (caretVisible) {
				final int caretX = innerX + Math.max(0, Math.min(innerWidth - 1, cursorOffset));
				c.fill(caretX, y + 3, caretX + 1, y + h - 3, CRTEditorTheme.ACCENT);
			}
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			final String value = currentText();
			// 根据点击位置估算光标
			int best = value.length();
			int accumulated = 0;
			for (int i = 0; i < value.length(); i++) {
				final int charWidth = c.textWidth(String.valueOf(value.charAt(i)));
				if (mouseX < x + 4 + accumulated + charWidth / 2.0) {
					best = i;
					break;
				}
				accumulated += charWidth;
			}
			cursor = best;
			blinkStart = System.currentTimeMillis();
			c.host.requestFocus(this);
			return true;
		}

		@Override
		public boolean keyPressed(Ctx c, int keyCode, int scanCode, int modifiers) {
			final String value = currentText();
			switch (keyCode) {
				case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:
					if (cursor > 0) {
						final int pos = cursor;
						text = value.substring(0, pos - 1) + value.substring(pos);
						cursor = pos - 1;
						notifyChange();
					}
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE:
					if (cursor < value.length()) {
						final int pos = cursor;
						text = value.substring(0, pos) + value.substring(pos + 1);
						notifyChange();
					}
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT:
					if (cursor > 0) {
						cursor--;
					}
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT:
					if (cursor < value.length()) {
						cursor++;
					}
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME:
					cursor = 0;
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_END:
					cursor = value.length();
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER:
				case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER:
					commit(c);
					return true;
				case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE:
					text = null;
					c.host.clearFocus();
					return true;
				default:
					return false;
			}
		}

		@Override
		public boolean charTyped(Ctx c, char chr, int modifiers) {
			if (!enabled || !acceptChar(chr)) {
				return false;
			}
			final String value = currentText();
			if (value.length() >= maxLength) {
				return true;
			}
			final int pos = Math.min(cursor, value.length());
			text = value.substring(0, pos) + chr + value.substring(pos);
			cursor = pos + 1;
			blinkStart = System.currentTimeMillis();
			notifyChange();
			return true;
		}

		@Override
		public boolean wantsKeyboard() {
			return true;
		}

		@Override
		public void onBlur(Ctx c) {
			commit(c);
		}

		private void notifyChange() {
			if (onChange != null && text != null) {
				onChange.accept(text);
			}
		}

		private void commit(Ctx c) {
			notifyChange();
			text = null;
			cursor = 0;
			if (onCommit != null) {
				onCommit.run();
			}
			c.host.clearFocus();
		}
	}

	// ================================================================ 下拉选择

	/** 下拉选择控件：显示当前标签，展开后弹出选项列表（由屏幕在所有控件之上绘制）。 */
	public static final class Dropdown extends Widget {

		public static final class Option {
			public final String id;
			public final String label;

			public Option(String id, String label) {
				this.id = id;
				this.label = label;
			}
		}

		public final List<Option> options = new ArrayList<>();
		public String placeholder = "—";
		private final Supplier<String> getter;
		private final Consumer<String> setter;
		private int popupX;
		private int popupY;
		private int popupW;
		private int popupH;
		private int popupScroll;
		private int popupRowHeight = 13;

		public Dropdown(Supplier<String> getter, Consumer<String> setter) {
			super(0, CONTROL_HEIGHT);
			this.getter = getter;
			this.setter = setter;
		}

		public Dropdown addOption(String id, String label) {
			options.add(new Option(id, label == null ? id : label));
			return this;
		}

		public Dropdown setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
			return this;
		}

		@Nullable
		private Option selected() {
			final String id = getter.get();
			for (final Option option : options) {
				if (option.id.equals(id)) {
					return option;
				}
			}
			return null;
		}

		private boolean isOpen(Ctx c) {
			return c.host.getOpenDropdown() == this;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final boolean open = isOpen(c);
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			c.fillRoundBordered(x, y, w, h, RADIUS, enabled ? CRTEditorTheme.CONTROL_BG : CRTEditorTheme.SURFACE_SOFT,
					open || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			final Option option = selected();
			final String label = option == null ? placeholder : option.label;
			c.textClipped(label, x + 5, y + (h - FONT_HEIGHT) / 2 + 1, w - 18, option == null ? CRTEditorTheme.TEXT_DIM : CRTEditorTheme.TEXT);
			c.text("▾", x + w - 10, y + (h - FONT_HEIGHT) / 2 + 1, open ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			if (isOpen(c)) {
				c.host.closeDropdown();
			} else {
				c.host.openDropdown(this);
			}
			return true;
		}

		/** 弹出层绘制：屏幕在所有面板之后调用，保证浮在最上层。 */
		public void renderPopup(Ctx c, int screenHeight) {
			if (!isOpen(c)) {
				return;
			}
			popupRowHeight = 13;
			popupW = Math.max(80, w);
			final int maxVisible = Math.min(options.size(), 10);
			popupH = Math.max(popupRowHeight, maxVisible * popupRowHeight + 4);
			popupX = x;
			popupY = y + h + 1;
			if (popupY + popupH > screenHeight - 4) {
				popupY = Math.max(4, y - popupH - 1);
			}
			popupScroll = clamp(popupScroll, 0, Math.max(0, options.size() - maxVisible));
			c.fillRoundBordered(popupX, popupY, popupW, popupH, RADIUS_SMALL, 0xFFFFFFFF, CRTEditorTheme.BORDER);
			final int first = popupScroll;
			final int last = Math.min(options.size(), first + maxVisible);
			final String currentId = getter.get();
			for (int i = first; i < last; i++) {
				final int rowY = popupY + 2 + (i - first) * popupRowHeight;
				final Option option = options.get(i);
				final boolean active = option.id.equals(currentId);
				final boolean hover = c.mouseX >= popupX + 1 && c.mouseX < popupX + popupW - 1 && c.mouseY >= rowY && c.mouseY < rowY + popupRowHeight;
				if (active) {
					c.fillRound(popupX + 2, rowY, popupW - 4, popupRowHeight, RADIUS_SMALL - 1, CRTEditorTheme.ACCENT);
				} else if (hover) {
					c.fillRound(popupX + 2, rowY, popupW - 4, popupRowHeight, RADIUS_SMALL - 1, CRTEditorTheme.ACCENT_SOFT);
				}
				c.textClipped(option.label, popupX + 6, rowY + 2, popupW - 12,
						active ? 0xFFFFFFFF : (hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT));
			}
		}

		/** 弹出层点击；返回 true 表示事件已被弹出层消费。 */
		public boolean popupClicked(Ctx c, double mouseX, double mouseY) {
			if (!isOpen(c)) {
				return false;
			}
			if (mouseX < popupX || mouseX >= popupX + popupW || mouseY < popupY || mouseY >= popupY + popupH) {
				c.host.closeDropdown();
				return true;
			}
			final int index = popupScroll + (int) ((mouseY - popupY - 2) / popupRowHeight);
			if (index >= 0 && index < options.size()) {
				setter.accept(options.get(index).id);
				c.host.closeDropdown();
			}
			return true;
		}

		public boolean popupScrolled(Ctx c, double mouseX, double mouseY, double amount) {
			if (!isOpen(c) || mouseX < popupX || mouseX >= popupX + popupW || mouseY < popupY || mouseY >= popupY + popupH) {
				return false;
			}
			final int maxVisible = Math.min(options.size(), 10);
			popupScroll = clamp(popupScroll - (int) Math.signum(amount), 0, Math.max(0, options.size() - maxVisible));
			return true;
		}
	}

	// ================================================================ 颜色选择

	/**
	 * 颜色选择器：当前色块 + hex 输入 + 可选「透明」按钮，
	 * 下方可展开 12 色预设色板（网页色板简化版）。
	 */
	public static final class ColorPicker extends Widget {

		private final Supplier<Integer> getter;
		private final IntConsumer setter;
		public final boolean nullable;
		/** 展开状态放在共享数组里，屏幕重建控件后仍能保持展开。 */
		private final boolean[] expansionState;
		private final TextField hexField;
		private int swatchRowsHeight;
		private static final int SWATCH_COLUMNS = 6;
		private static final int SWATCH_SIZE = 12;

		public ColorPicker(Supplier<Integer> getter, IntConsumer setter, boolean nullable) {
			this(getter, setter, nullable, new boolean[1]);
		}

		public ColorPicker(Supplier<Integer> getter, IntConsumer setter, boolean nullable, boolean[] expansionState) {
			super(0, INPUT_HEIGHT);
			this.getter = getter;
			this.setter = setter;
			this.nullable = nullable;
			this.expansionState = expansionState;
			hexField = new TextField(() -> {
				final Integer value = getter.get();
				return value == null ? "" : toHex(value);
			}, value -> {
				final Integer parsed = parseHex(value);
				if (parsed != null) {
					setter.accept(parsed);
				}
			});
			hexField.setPlaceholder("#RRGGBB").setMaxLength(9).setHexOnly(true);
		}

		public ColorPicker setHexMaxLength(int maxLength) {
			hexField.setMaxLength(maxLength);
			return this;
		}

		@Override
		public int getHeight() {
			final int rows = (PALETTE_COLORS.length + SWATCH_COLUMNS - 1) / SWATCH_COLUMNS;
			swatchRowsHeight = rows * (SWATCH_SIZE + GAP_SMALL);
			return INPUT_HEIGHT + (expansionState[0] ? GAP_SMALL + LINE_HEIGHT + GAP_SMALL + swatchRowsHeight : GAP_SMALL + LINE_HEIGHT);
		}

		private int nullButtonWidth() {
			return nullable ? 26 : 0;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final Integer value = getter.get();
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			// 当前色块
			final int swatchX = x;
			c.fillRoundBordered(swatchX, y, 20, INPUT_HEIGHT, RADIUS_SMALL, value == null ? 0xFFFFFFFF : value, hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			if (value == null) {
				drawTransparentMark(c, swatchX, y, 20, INPUT_HEIGHT);
			} else if (((value >>> 24) & 0xFF) < 0xFF) {
				// 半透明颜色：左侧叠一层棋盘格提示（简化为一角深色）
				c.fill(swatchX + 1, y + INPUT_HEIGHT - 5, swatchX + 5, y + INPUT_HEIGHT - 1, 0xFFD7DCE3);
			}
			// hex 输入框
			final int fieldX = x + 20 + GAP;
			final int fieldW = Math.max(40, w - 20 - GAP - (nullable ? nullButtonWidth() + GAP : 0));
			hexField.setBounds(fieldX, y, fieldW);
			hexField.visible = visible;
			hexField.enabled = enabled;
			hexField.render(c);
			// 透明按钮
			if (nullable) {
				final int btnX = x + w - nullButtonWidth();
				final boolean active = value == null;
				final boolean btnHover = enabled && c.mouseX >= btnX && c.mouseX < x + w && c.mouseY >= y && c.mouseY < y + INPUT_HEIGHT;
				c.fillRoundBordered(btnX, y, nullButtonWidth(), INPUT_HEIGHT, RADIUS_SMALL,
						active ? CRTEditorTheme.ACCENT_SOFT : (btnHover ? CRTEditorTheme.BUTTON_BG_HOVER : 0xFFFFFFFF),
						active || btnHover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
				c.textCentered("透明", btnX + nullButtonWidth() / 2, y + (INPUT_HEIGHT - FONT_HEIGHT) / 2 + 1, active ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
			}
			// 展开色板开关
			final int toggleY = y + INPUT_HEIGHT + GAP_SMALL;
			c.textClipped(expansionState[0] ? "色板 ▴" : "色板 ▾", x, toggleY, w, hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
			c.textRight(String.format(Locale.ROOT, "预设 %d 色", PALETTE_COLORS.length), x + w, toggleY, CRTEditorTheme.BUTTON_DISABLED_TEXT);
			if (!expansionState[0]) {
				return;
			}
			final int paletteTop = toggleY + LINE_HEIGHT + GAP_SMALL;
			for (int i = 0; i < PALETTE_COLORS.length; i++) {
				final int column = i % SWATCH_COLUMNS;
				final int row = i / SWATCH_COLUMNS;
				final int sx = x + column * (SWATCH_SIZE + GAP_SMALL);
				final int sy = paletteTop + row * (SWATCH_SIZE + GAP_SMALL);
				final int color = PALETTE_COLORS[i];
				final boolean active = value != null && (value & 0xFFFFFF) == (color & 0xFFFFFF);
				final boolean swatchHover = enabled && c.mouseX >= sx && c.mouseX < sx + SWATCH_SIZE && c.mouseY >= sy && c.mouseY < sy + SWATCH_SIZE;
				c.fillRoundBordered(sx, sy, SWATCH_SIZE, SWATCH_SIZE, 2.0F, color,
						active || swatchHover ? CRTEditorTheme.ACCENT : 0xFFB9C1CB);
				if (swatchHover) {
					c.setTooltip(PALETTE_NAMES[i] + " " + toHex(color));
				}
			}
			hoverTooltip(c);
		}

		private void drawTransparentMark(Ctx c, int swatchX, int swatchY, int swatchW, int swatchH) {
			// 红色斜线（用小方块近似）
			for (int i = 0; i < Math.min(swatchW, swatchH); i++) {
				c.fill(swatchX + i, swatchH - i - 1 + swatchY, swatchX + i + 1, swatchH - i + swatchY, CRTEditorTheme.DANGER);
			}
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled) {
				return false;
			}
			if (mouseY >= y && mouseY < y + INPUT_HEIGHT) {
				if (mouseX >= x && mouseX < x + 20) {
					expansionState[0] = !expansionState[0];
					c.host.widgetLayoutChanged();
					return true;
				}
				if (nullable && mouseX >= x + w - nullButtonWidth()) {
					setter.accept(-1);
					return true;
				}
				if (hexField.isOver(mouseX, mouseY)) {
					return hexField.mouseClicked(c, mouseX, mouseY, button);
				}
				return isOver(mouseX, mouseY);
			}
			final int toggleY = y + INPUT_HEIGHT + GAP_SMALL;
			if (mouseX >= x && mouseX < x + w && mouseY >= toggleY && mouseY < toggleY + LINE_HEIGHT) {
				expansionState[0] = !expansionState[0];
				c.host.widgetLayoutChanged();
				return true;
			}
			if (expansionState[0]) {
				final int paletteTop = toggleY + LINE_HEIGHT + GAP_SMALL;
				for (int i = 0; i < PALETTE_COLORS.length; i++) {
					final int column = i % SWATCH_COLUMNS;
					final int row = i / SWATCH_COLUMNS;
					final int sx = x + column * (SWATCH_SIZE + GAP_SMALL);
					final int sy = paletteTop + row * (SWATCH_SIZE + GAP_SMALL);
					if (mouseX >= sx && mouseX < sx + SWATCH_SIZE && mouseY >= sy && mouseY < sy + SWATCH_SIZE) {
						setter.accept(PALETTE_COLORS[i]);
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean keyPressed(Ctx c, int keyCode, int scanCode, int modifiers) {
			return hexField.keyPressed(c, keyCode, scanCode, modifiers);
		}

		@Override
		public boolean charTyped(Ctx c, char chr, int modifiers) {
			return hexField.charTyped(c, chr, modifiers);
		}

		@Override
		public boolean wantsKeyboard() {
			return true;
		}

		@Override
		public void onBlur(Ctx c) {
			hexField.onBlur(c);
		}

		@Override
		public boolean isOver(double mouseX, double mouseY) {
			return visible && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + getHeight();
		}
	}

	// ================================================================ 卡片

	/** 左栏/图标选择网格里的卡片：白底圆角、hover 蓝框、选中蓝框浅蓝底。 */
	public static final class Card extends Widget {

		public interface Thumb {
			void draw(Ctx c, int x, int y, int w, int h);
		}

		public final String label;
		@Nullable
		public final String glyph;
		@Nullable
		public final Thumb thumb;
		@Nullable
		public Runnable action;
		public boolean selected;
		private boolean pressed;

		public Card(int w, int h, @Nullable String label, @Nullable String glyph, @Nullable Thumb thumb, @Nullable Runnable action) {
			super(w, h);
			this.label = label == null ? "" : label;
			this.glyph = glyph;
			this.thumb = thumb;
			this.action = action;
		}

		@Override
		public void render(Ctx c) {
			if (!visible) {
				return;
			}
			final boolean hover = enabled && isOver(c.mouseX, c.mouseY);
			final int bg = selected ? CRTEditorTheme.ACCENT_SOFT : (hover ? 0xFFFFFFFF : 0xFFFAFBFC);
			c.fillRoundBordered(x, y, w, h, RADIUS, enabled ? bg : CRTEditorTheme.SURFACE_SOFT,
					selected || hover ? CRTEditorTheme.ACCENT : CRTEditorTheme.BORDER);
			final boolean hasLabel = h >= 26;
			final int thumbX = x + 4;
			final int thumbY = y + 3;
			final int thumbW = w - 8;
			final int thumbH = hasLabel ? h - 16 : h - 6;
			if (thumb != null) {
				thumb.draw(c, thumbX, thumbY, thumbW, thumbH);
			} else if (glyph != null) {
				final float scale = Math.max(0.75F, Math.min(2.0F, thumbH / 12.0F));
				final int glyphWidth = c.scaledTextWidth(glyph, scale);
				final int glyphHeight = Math.round(FONT_HEIGHT * scale);
				c.scaledText(glyph, thumbX + (thumbW - glyphWidth) / 2, thumbY + (thumbH - glyphHeight) / 2 + 1, scale,
						selected ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT);
			}
			if (hasLabel) {
				c.textCenteredClipped(label, x + w / 2, y + h - LINE_HEIGHT, w - 5, selected ? CRTEditorTheme.ACCENT : CRTEditorTheme.TEXT_DIM);
			}
			hoverTooltip(c);
		}

		@Override
		public boolean mouseClicked(Ctx c, double mouseX, double mouseY, int button) {
			if (button != 0 || !enabled || !isOver(mouseX, mouseY)) {
				return false;
			}
			pressed = true;
			return true;
		}

		@Override
		public void mouseReleased(Ctx c, double mouseX, double mouseY, int button) {
			if (button == 0) {
				final boolean activate = pressed && enabled && isOver(mouseX, mouseY);
				pressed = false;
				if (activate && action != null) {
					action.run();
				}
			}
		}

		@Override
		public void cancelPress() {
			pressed = false;
		}
	}
}
