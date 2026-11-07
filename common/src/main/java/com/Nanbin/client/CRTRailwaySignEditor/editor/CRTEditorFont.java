package com.Nanbin.client.CRTRailwaySignEditor.editor;

import com.Nanbin.client.Drawing.CustomFontTextureCache;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FittedTextTexture;
import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.Drawing.FontRenderUtils;
import com.Nanbin.mapping.GraphicsHolder;
import com.Nanbin.mapping.GuiDrawing;
import net.minecraft.client.MinecraftClient;

import javax.annotation.Nullable;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 编辑器界面字体：正文用阿里巴巴普惠体（Alibaba PuHuiTi），标题/强调用思源黑体 Bold。
 *
 * <p>与导视牌渲染使用同一套字体文件（{@code assets/nanbin/font/*}），保证屏幕内 UI 与牌面视觉一致。
 *
 * <p><b>逐字回退</b>：这两套中文字体不含箭头（←↑↖…）、emoji、部分符号与特殊字形。
 * 因此文字会被切成「自定义字体能显示的 run」和「必须回退 MC 原字体的 run」；
 * 后者直接用 Minecraft 自带字体绘制。这样从网页项目移植的图标字符（箭头、✕、≡、↕、emoji 等）
 * 不会再变成空白/方框，同时普通中英文仍是普惠体/思源黑体。</p>
 */
public final class CRTEditorFont {

	/** 正文字号（与原来的 MC 字体 9px 对齐）。 */
	private static final float BODY_SIZE = 9.0F;
	/** 视为“标题/强调”的缩放阈值：达到后改用思源黑体 Bold。 */
	private static final float BOLD_SCALE_THRESHOLD = 1.12F;
	/** 测量用的固定物理字号倍率（与 GUI scale / 动态贴图分辨率解耦，保证缓存稳定）。 */
	private static final float MEASURE_SCALE = 10.0F;

	private static final Map<String, Integer> WIDTH_CACHE = new LinkedHashMap<String, Integer>(256, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
			return size() > 2048;
		}
	};
	private static final Map<String, String> TRIM_CACHE = new HashMap<>();
	private static final Map<String, List<TextRun>> RUN_CACHE = new LinkedHashMap<String, List<TextRun>>(128, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, List<TextRun>> eldest) {
			return size() > 512;
		}
	};

	/** 一段文字：要么用某个自定义字体（fontType != null），要么回退 MC 原字体（fontType == null）。 */
	private static final class TextRun {
		private final String text;
		@Nullable
		private final FontType fontType;
		/** 逻辑宽度（GUI 像素，已含 scale）。 */
		private final float width;
		/** 是否按粗体渲染（仅自定义字体 run 使用）。 */
		private final boolean bold;

		private TextRun(String text, @Nullable FontType fontType, float width, boolean bold) {
			this.text = text;
			this.fontType = fontType;
			this.width = width;
			this.bold = bold;
		}
	}

	private CRTEditorFont() {
	}

	private static float guiScale() {
		try {
			final double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
			return scale <= 0.0D ? 2.0F : (float) scale;
		} catch (Throwable ignored) {
			return 2.0F;
		}
	}

	private static FontType primaryType(boolean bold) {
		return bold ? FontType.SOURCE_HAN : FontType.ALIBABA;
	}

	private static FontType secondaryType(boolean bold) {
		return bold ? FontType.ALIBABA : FontType.SOURCE_HAN;
	}

	/** 逻辑字号（GUI 像素）。 */
	private static float logicalSize(float scale) {
		return Math.max(1.0F, BODY_SIZE * scale);
	}

	private static boolean isBold(float scale) {
		return scale >= BOLD_SCALE_THRESHOLD;
	}

	@Nullable
	private static Font rawFont(@Nullable FontType type) {
		if (type == null) {
			return null;
		}
		try {
			return CustomFontTextureCache.instance.getRawFontForUi(type);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 逻辑宽度（GUI 像素）。 */
	public static int width(@Nullable String text, float scale) {
		if (text == null || text.isEmpty() || scale <= 0.0F) {
			return 0;
		}
		final String key = scale + "|" + text;
		final Integer cached = WIDTH_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		float total = 0.0F;
		for (final TextRun run : runs(text, scale)) {
			total += run.width;
		}
		final int width = Math.round(total);
		WIDTH_CACHE.put(key, width);
		return width;
	}

	/** 按逻辑宽度裁剪文字；中英文与图标字符统一按 run 宽度计算。 */
	public static String trimToWidth(@Nullable String text, float scale, int maxWidth) {
		if (text == null || text.isEmpty() || maxWidth <= 0) {
			return "";
		}
		if (width(text, scale) <= maxWidth) {
			return text;
		}
		final String key = scale + "|" + maxWidth + "|" + text;
		final String cached = TRIM_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		String result = "";
		int bestEnd = 0;
		float accumulated = 0.0F;
		for (int i = 0; i < text.length(); ) {
			final int codePoint = text.codePointAt(i);
			final int next = i + Character.charCount(codePoint);
			final float charWidth = width(text.substring(i, next), scale);
			if (accumulated + charWidth > maxWidth) {
				break;
			}
			accumulated += charWidth;
			bestEnd = next;
			i = next;
		}
		if (bestEnd > 0) {
			result = text.substring(0, bestEnd);
		}
		if (TRIM_CACHE.size() > 4096) {
			TRIM_CACHE.clear();
		}
		TRIM_CACHE.put(key, result);
		return result;
	}

	/** 把文字切成自定义字体 run / MC 原字体 run（结果按 text+scale 缓存）。 */
	private static List<TextRun> runs(String text, float scale) {
		final String key = scale + "|" + text;
		final List<TextRun> cached = RUN_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		final boolean bold = isBold(scale);
		final Font primaryFont = rawFont(primaryType(bold));
		final Font secondaryFont = rawFont(secondaryType(bold));
		final List<TextRun> result = new ArrayList<>();
		if (primaryFont == null && secondaryFont == null) {
			result.add(new TextRun(text, null, Math.max(0.0F, GraphicsHolder.getTextWidth(text) * scale), false));
			RUN_CACHE.put(key, result);
			return result;
		}
		final StringBuilder current = new StringBuilder();
		FontType currentType = null; // null 表示 MC 原字体
		for (int i = 0; i < text.length(); ) {
			final int codePoint = text.codePointAt(i);
			final int next = i + Character.charCount(codePoint);
			FontType chosen = null;
			if (primaryFont != null && canDisplay(primaryFont, codePoint)) {
				chosen = primaryType(bold);
			} else if (secondaryFont != null && canDisplay(secondaryFont, codePoint)) {
				chosen = secondaryType(bold);
			}
			if (chosen != currentType) {
				flushRun(result, current, currentType, bold, scale);
				currentType = chosen;
			}
			current.appendCodePoint(codePoint);
			i = next;
		}
		flushRun(result, current, currentType, bold, scale);
		if (result.isEmpty()) {
			result.add(new TextRun(text, null, Math.max(0.0F, GraphicsHolder.getTextWidth(text) * scale), false));
		}
		RUN_CACHE.put(key, result);
		return result;
	}

	private static boolean canDisplay(Font font, int codePoint) {
		try {
			return font.canDisplay(codePoint);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void flushRun(List<TextRun> result, StringBuilder builder, @Nullable FontType type, boolean bold, float scale) {
		if (builder.length() == 0) {
			return;
		}
		final String runText = builder.toString();
		builder.setLength(0);
		if (type == null) {
			result.add(new TextRun(runText, null, Math.max(0.0F, GraphicsHolder.getTextWidth(runText) * scale), false));
			return;
		}
		float width = 0.0F;
		final Font font = rawFont(type);
		if (font != null) {
			try {
				final float physical = logicalSize(scale) * MEASURE_SCALE;
				final Font sized = font.deriveFont(bold ? Font.BOLD : Font.PLAIN, physical);
				final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				final Graphics2D graphics = probe.createGraphics();
				graphics.setFont(sized);
				final FontMetrics metrics = graphics.getFontMetrics(sized);
				width = Math.max(0.0F, metrics.stringWidth(runText) / MEASURE_SCALE);
				graphics.dispose();
			} catch (Throwable ignored2) {
				width = Math.max(0.0F, GraphicsHolder.getTextWidth(runText) * scale);
			}
		}
		result.add(new TextRun(runText, type, width, bold));
	}

	/** 绘制一行文字；{@code y} 为行顶部。可显示字符用自定义字体，缺失字符自动回退 MC 字体。 */
	public static void draw(@Nullable GraphicsHolder graphics, @Nullable String text, int x, int y, float scale, int color) {
		if (graphics == null || text == null || text.isEmpty() || scale <= 0.0F) {
			return;
		}
		try {
			final float guiScale = guiScale();
			float cursorX = x;
			for (final TextRun run : runs(text, scale)) {
				if (run.fontType == null) {
					// MC 原字体：矩阵缩放后再画，保留箭头/emoji 等项目移植字符
					graphics.push();
					graphics.translate(cursorX, y, 0.0F);
					graphics.scale(scale, scale, 1.0F);
					graphics.drawText(run.text, 0, 0, color, false, GraphicsHolder.getDefaultLight());
					graphics.pop();
				} else {
					drawCustomRun(graphics, run, cursorX, y, scale, color, guiScale);
				}
				cursorX += run.width;
			}
		} catch (Throwable ignored) {
			try {
				graphics.drawText(text, x, y, color, false, GraphicsHolder.getDefaultLight());
			} catch (Throwable ignoredAgain) {
				// 绘制失败时静默（界面不能因为字体问题崩溃）
			}
		}
	}

	private static void drawCustomRun(GraphicsHolder graphics, TextRun run, float x, int y, float scale, int color, float guiScale) {
		final float resolutionScale = Math.max(0.01F, FontRenderUtils.getResolutionScale());
		final int physicalSize = Math.max(1, Math.round(logicalSize(scale) * guiScale));
		final FittedTextTexture texture = CustomFontTextureCache.instance.getFittedTextTexture(run.text,
				run.fontType, physicalSize, new Color(color, true), run.bold);
		if (texture.identifier == null || texture.width <= 1 || texture.height <= 1) {
			graphics.push();
			graphics.translate(x, y, 0.0F);
			graphics.scale(scale, scale, 1.0F);
			graphics.drawText(run.text, 0, 0, color, false, GraphicsHolder.getDefaultLight());
			graphics.pop();
			return;
		}
		final float logicalHeight = texture.height / (guiScale * resolutionScale);
		final float drawY = y - Math.max(0.0F, (logicalHeight - logicalSize(scale) * 1.2F) / 2.0F);
		final GuiDrawing drawing = new GuiDrawing(graphics);
		drawing.beginDrawingTexture(texture.identifier);
		drawing.drawTexture(x, drawY, x + run.width, drawY + logicalHeight, 0.0F, 0.0F, 1.0F, 1.0F);
		drawing.finishDrawingTexture();
	}
}
