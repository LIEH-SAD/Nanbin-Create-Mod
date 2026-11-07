package com.Nanbin.client.Drawing;

import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;

import javax.annotation.Nullable;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRT 导视牌自由布局的文本测量器。
 *
 * <p>网页编辑器（CRT-Sign-Web-Editor）的所有布局尺寸（元素宽度、字号、基线、逐字墨区排位）
 * 都来自浏览器 Canvas 2D 的 {@code measureText()}（见 docs/WEB_EDITOR_SPEC.md §5.8）。
 * 浏览器不可用，这里用 {@link java.awt.Font} + {@link GlyphVector} 复刻同一套语义：</p>
 *
 * <ul>
 *   <li>{@link #advance}：整串前进宽（含 kerning），对应 {@code ceil(measureText().width)}；</li>
 *   <li>{@link #ascent}：墨区上伸（墨顶到基线），对应 {@code ceil(actualBoundingBoxAscent)}，
 *       墨区顶不高于基线时按 {@code 0.75 × 字号} 兜底；</li>
 *   <li>{@link #descent}：墨区下伸，对应 {@code ceil(actualBoundingBoxDescent)}，无墨区时为 0；</li>
 *   <li>{@link #ink}：{@code {abl, abr, adv}}，对应 Canvas 的
 *       {@code actualBoundingBoxLeft/Right + measureText().width}（abl 通常为负 = 左字肩）。</li>
 * </ul>
 *
 * <p>所有测量都开启分数度量（fractional metrics），并且四个值来自同一个 {@link GlyphVector}，
 * 保证「测量器自洽」（见 docs/WEB_EDITOR_SPEC.extract.md §1 的 Java 移植提醒）。</p>
 *
 * <p>字体来自 {@link CustomFontTextureCache} 已加载的三个 TTF/OTF
 * （Alibaba PuHuiTi / Source Han Sans SC Bold / Source Sans 3），字体加载失败时退化为
 * 0.75em / 0.2em / -0.05em / 0.85em 的近似值，保证渲染不会因为缺字体而崩溃。</p>
 */
public final class CRTSignTextMetrics {

	private CRTSignTextMetrics() {
	}

	/** 分数度量开启的 FontRenderContext（与浏览器 Canvas 的测量语义最接近）。 */
	private static final FontRenderContext FRACTIONAL_METRICS = new FontRenderContext(new AffineTransform(), true, true);

	/** 测量缓存上限：布局每次重算都会查同一批文本，缓存后预览逐帧重算也几乎无开销。 */
	private static final int CACHE_LIMIT = 8192;
	private static final int FONT_CACHE_LIMIT = 512;

	/** 测量缓存：key = 字体 + 字重 + 字号 + 文本。 */
	private static final Map<String, Measurement> MEASUREMENTS = new LinkedHashMap<String, Measurement>(256, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Measurement> eldest) {
			return size() > CACHE_LIMIT;
		}
	};

	/** 派生字体缓存（deriveFont 本身不便宜，按 字号×1000 取整缓存）。 */
	private static final Map<String, Font> FONTS = new LinkedHashMap<String, Font>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Font> eldest) {
			return size() > FONT_CACHE_LIMIT;
		}
	};

	/** 空文本的测量结果。 */
	private static final Measurement EMPTY = new Measurement(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	// ─── 对外 API（对齐 core.js createCanvasMeasurer 的四个接口） ──────────

	/**
	 * 整串前进宽（含字距/kerning），对齐网页 {@code m(text, family, weight, sizePx)}：
	 * {@code ceil(ctx.measureText(text).width)}。空文本返回 0。
	 */
	public static float advance(@Nullable String text, FontType fontType, boolean weightBold, float sizePx) {
		final Measurement measurement = measure(text, fontType, weightBold, sizePx);
		return (float) Math.ceil(measurement.advance);
	}

	/**
	 * 墨区上伸（墨顶到基线），对齐网页 {@code m.ascent(...)}：
	 * {@code ceil(actualBoundingBoxAscent)}；墨区顶不高于基线时回退 {@code 0.75 × 字号}。
	 */
	public static float ascent(@Nullable String text, FontType fontType, boolean weightBold, float sizePx) {
		if (text == null || text.isEmpty()) {
			return 0.0F;
		}
		final Measurement measurement = measure(text, fontType, weightBold, sizePx);
		return (float) Math.ceil(measurement.ascent);
	}

	/**
	 * 墨区下伸（基线以下的墨区深度），对齐网页 {@code m.descent(...)}：
	 * {@code ceil(actualBoundingBoxDescent)}；无下伸部时为 0。
	 */
	public static float descent(@Nullable String text, FontType fontType, boolean weightBold, float sizePx) {
		if (text == null || text.isEmpty()) {
			return 0.0F;
		}
		final Measurement measurement = measure(text, fontType, weightBold, sizePx);
		return (float) Math.ceil(measurement.descent);
	}

	/**
	 * 墨区度量，对齐网页 {@code m.ink(text, family, weight, sizePx)}。
	 * 语义：对齐点在原点、文本从 x = 0 开始排版时，墨区左缘为 {@code abl}（通常为负）、
	 * 右缘为 {@code abr}、前进宽为 {@code adv}；墨区宽 = {@code abr + abl}。
	 */
	public static Ink ink(@Nullable String text, FontType fontType, boolean weightBold, float sizePx) {
		final Measurement measurement = measure(text, fontType, weightBold, sizePx);
		return new Ink(measurement.abl, measurement.abr, measurement.advance);
	}

	/** 取（并缓存）某字体在某字号/字重下的 java.awt.Font；字体缺失返回 null。 */
	@Nullable
	public static Font derivedFont(FontType fontType, boolean weightBold, float sizePx) {
		if (fontType == null || sizePx <= 0.0F) {
			return null;
		}
		final String key = fontType.name() + '|' + weightBold + '|' + Math.round(sizePx * 1000.0F);
		synchronized (FONTS) {
			final Font cached = FONTS.get(key);
			if (cached != null) {
				return cached;
			}
		}
		final Font rawFont = CustomFontTextureCache.instance.getJavaFont(fontType);
		if (rawFont == null) {
			return null;
		}
		final Font derived = rawFont.deriveFont(weightBold ? Font.BOLD : Font.PLAIN, sizePx);
		synchronized (FONTS) {
			FONTS.put(key, derived);
		}
		return derived;
	}

	/** 字体是否可用（不可用时所有测量走近似值）。 */
	public static boolean isFontAvailable(FontType fontType) {
		return fontType != null && CustomFontTextureCache.instance.getJavaFont(fontType) != null;
	}

	/** 清空测量缓存（资源重载后调用；字体可能被重新加载）。 */
	public static void clearCache() {
		synchronized (MEASUREMENTS) {
			MEASUREMENTS.clear();
		}
		synchronized (FONTS) {
			FONTS.clear();
		}
	}

	// ─── 测量结果 ────────────────────────────────────────────────────

	/** 墨区度量（对应网页 measure.ink 的返回值）。 */
	public static final class Ink {
		/** 墨区左缘相对排版原点的坐标；通常为负 = 左字肩向右缩进。 */
		public final float abl;
		/** 墨区右缘相对排版原点的坐标。 */
		public final float abr;
		/** 前进宽（未取整，对应 measureText().width）。 */
		public final float adv;

		public Ink(float abl, float abr, float adv) {
			this.abl = abl;
			this.abr = abr;
			this.adv = adv;
		}

		/** 墨区宽 = abr + abl（abl 为负时才是正的墨宽）。 */
		public float inkWidth() {
			return abr + abl;
		}
	}

	/** 一次测量得到的全部量（体积小，缓存友好）。 */
	public static final class Measurement {
		/** 前进宽（未取整）。 */
		public final float advance;
		/** 墨区上伸（未取整，可能为 0）。 */
		public final float ascent;
		/** 墨区下伸（未取整，>= 0）。 */
		public final float descent;
		/** 墨区左缘（通常为负）。 */
		public final float abl;
		/** 墨区右缘。 */
		public final float abr;
		/** 前进宽（与 advance 相同，便于与 ink 语义一致）。 */
		public final float inkAdvance;

		private Measurement(float advance, float ascent, float descent, float abl, float abr, float inkAdvance) {
			this.advance = advance;
			this.ascent = ascent;
			this.descent = descent;
			this.abl = abl;
			this.abr = abr;
			this.inkAdvance = inkAdvance;
		}
	}

	// ─── 内部实现 ────────────────────────────────────────────────────

	private static Measurement measure(@Nullable String text, FontType fontType, boolean weightBold, float sizePx) {
		if (text == null || text.isEmpty() || sizePx <= 0.0F) {
			return EMPTY;
		}
		final String key = (fontType == null ? "?" : fontType.name()) + '|' + weightBold + '|'
				+ Math.round(sizePx * 1000.0F) + '|' + text;
		synchronized (MEASUREMENTS) {
			final Measurement cached = MEASUREMENTS.get(key);
			if (cached != null) {
				return cached;
			}
		}

		final Font font = derivedFont(fontType, weightBold, sizePx);
		final Measurement measurement = font == null ? approximate(text, sizePx) : measureWithFont(text, font, sizePx);
		synchronized (MEASUREMENTS) {
			MEASUREMENTS.put(key, measurement);
		}
		return measurement;
	}

	private static Measurement measureWithFont(String text, Font font, float sizePx) {
		try {
			final GlyphVector glyphVector = font.createGlyphVector(FRACTIONAL_METRICS, text);
			final int glyphCount = glyphVector.getNumGlyphs();
			// 整串前进宽：最后一个字形的排版位置，包含 kerning / tracking，等价 measureText().width
			final float advance = glyphCount <= 0 ? 0.0F : (float) glyphVector.getGlyphPosition(glyphCount).getX();
			// 墨区（不是字距盒）：GlyphVector 的 visualBounds 与 Canvas actualBoundingBox* 对应
			final Rectangle2D visualBounds = glyphVector.getVisualBounds();
			final boolean hasInk = visualBounds.getWidth() > 0.0D || visualBounds.getHeight() > 0.0D;
			final float inkTop = (float) -visualBounds.getMinY();
			final float inkBottom = (float) visualBounds.getMaxY();
			final float ascent = hasInk && inkTop > 0.0F ? inkTop : sizePx * 0.75F;
			final float descent = hasInk ? Math.max(0.0F, inkBottom) : 0.0F;
			final float abl;
			final float abr;
			if (hasInk) {
				abl = (float) -visualBounds.getMinX();
				abr = (float) visualBounds.getMaxX();
			} else {
				// 无墨区字形（空格等）：Canvas 这里左右边界退化为字距盒，避免除零/负墨宽
				abl = 0.0F;
				abr = advance;
			}
			return new Measurement(advance, ascent, descent, abl, abr, advance);
		} catch (Exception ignored) {
			return approximate(text, sizePx);
		}
	}

	/** 字体缺失时的近似值（语义与网页无 API 时的兜底一致）。 */
	private static Measurement approximate(String text, float sizePx) {
		float advance = 0.0F;
		for (int i = 0; i < text.length(); ) {
			final int codePoint = text.codePointAt(i);
			i += Character.charCount(codePoint);
			advance += isWide(codePoint) ? sizePx : sizePx * 0.6F;
		}
		final float ascent = sizePx * 0.75F;
		final float descent = sizePx * 0.2F;
		final float abl = -sizePx * 0.05F;
		final float abr = sizePx * 0.85F;
		return new Measurement(advance, ascent, descent, abl, abr, advance);
	}

	/** 宽字符（CJK / 全角）近似：一个 em。 */
	private static boolean isWide(int codePoint) {
		return Character.isIdeographic(codePoint)
				|| (codePoint >= 0x1100 && codePoint <= 0x115F)
				|| (codePoint >= 0x2E80 && codePoint <= 0xA4CF)
				|| (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
				|| (codePoint >= 0xF900 && codePoint <= 0xFAFF)
				|| (codePoint >= 0xFE30 && codePoint <= 0xFE4F)
				|| (codePoint >= 0xFF00 && codePoint <= 0xFF60)
				|| (codePoint >= 0xFFE0 && codePoint <= 0xFFE6);
	}
}
