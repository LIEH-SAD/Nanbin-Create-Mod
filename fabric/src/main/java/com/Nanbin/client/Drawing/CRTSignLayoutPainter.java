package com.Nanbin.client.Drawing;

import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.client.JavaScriptSupport.JSDrawSink;
import com.Nanbin.client.JavaScriptSupport.JSSignTextureBaker;
import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignElement;
import com.Nanbin.signage.crt.CRTSignElementType;
import com.Nanbin.signage.crt.CRTSignLayout;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.NativeImage;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.resource.SignResource;

import javax.annotation.Nullable;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CRT 导视牌「自由布局」渲染引擎：把网页项目 CRT-Sign-Web-Editor 的 SVG 渲染配方
 * （docs/WEB_EDITOR_SPEC.md §5）复刻成 Minecraft 端的布局 + 绘制管线。
 *
 * <p>布局与绘制分离，与世界渲染、编辑界面预览共用同一套实现：</p>
 * <ul>
 *   <li>{@link #layout(CRTSignLayout, CRTSignDataContext)}：纯布局（对应网页 layoutSign/
 *       十种元素的 *Metrics），按 §5 的盒模型 / 三通道 / 墨区公式算出一帧
 *       {@link CRTSignLayoutFrame}；</li>
 *   <li>{@link #draw(JSDrawSink, CRTSignLayoutFrame, CRTSignDataContext)}：把一帧画进
 *       {@link JSDrawSink}（sign-bg → 行背景 → 元素盒背景 → 元素内容 → 灰框），
 *       文字用 {@link FittedTextRenderer} 生成 NativeImage 后 {@code sink.drawImage}，
 *       图标用 signs.json 贴图，箭头/徽标用 {@code sink.fillPolygon}；</li>
 *   <li>{@link #bake}：把 {@code draw} 的结果烘焙成一张贴图（1 设计像素 ≈ 1 贴图像素），
 *       世界渲染与编辑预览共用同一张贴图；</li>
 *   <li>{@link #drawElementStandalone}：左栏元素卡片/预设缩略图的单元素绘制。</li>
 * </ul>
 *
 * <p>文本测量全部走 {@link CRTSignTextMetrics}（java.awt.Font 真实墨区，不是等宽近似），
 * 因此元素宽度、数字间距、光学居中、墨区垂直锚定与网页端一致。MTR 数据绑定按
 * {@link CRTSignElement.DataBinding} 用 {@link CRTSignDataContext} 解析实际内容，
 * 取不到数据时回退元素字面文本。</p>
 *
 * <p>来源：CRT-Sign-Web-Editor（Apache License 2.0，原作者 endcreeper861 的
 * JR-Guidance-Sign-Web-Editor，PrisamaX0124 fork）。本类是 Minecraft 端的 Java 重实现，
 * 几何/常量取自 docs/WEB_EDITOR_SPEC.md 与 WEB_EDITOR_SPEC.extract.md。</p>
 */
public final class CRTSignLayoutPainter {

	private CRTSignLayoutPainter() {
	}

	// ─── 网页 render.js 的布局常量（§5.7 记号） ──────────────────────

	/** 双语中文字号 / 内容尺寸 = 506/800。 */
	public static final float ZH_FONT_RATIO = 506.0F / 800.0F;
	/** 双语英文字号 / 内容尺寸 = 285/800。 */
	public static final float EN_FONT_RATIO = 285.0F / 800.0F;
	/** 大文本 / 编号字号 / 内容尺寸 = 1.3。 */
	public static final float NUM_FONT_RATIO = 1.3F;
	/** 线路色条宽 / 内容尺寸 = 10/28。 */
	public static final float STRIPE_RATIO = 10.0F / 28.0F;
	/** 组合标签内容盒 / 宿主内容尺寸 = 0.75。 */
	public static final float LABEL_BOX_RATIO = 0.75F;
	/** 双语英文左右对齐时的额外缩进 / 内容尺寸 = 1/30。 */
	public static final float EN_ALIGN_OFFSET = 1.0F / 30.0F;
	/** 文本墨区顶到内容顶的引导 = 0.036。 */
	public static final float INK_TOP_LEADING = 0.036F;
	/** 中文墨区底 → 英文墨区顶的间隙 = 0.123。 */
	public static final float ZH_EN_GAP = 0.123F;

	// 固定标签文本（网页里不可编辑的「号线 / Line」「线」「出入口 / Entrance」「出口 / Exit」）
	private static final String LABEL_NUMBER_ZH = "号线";
	private static final String LABEL_NUMBER_EN = "Line";
	private static final String LABEL_TEXT_LINE_ZH = "线";
	private static final String LABEL_ENTRANCE_ZH = "出入口";
	private static final String LABEL_ENTRANCE_EN = "Entrance";
	private static final String LABEL_EXIT_ZH = "出口";
	private static final String LABEL_EXIT_EN = "Exit";

	// ─── 烘焙参数（布局贴图：设计像素 1:1，超宽再整体降密度） ─────────

	/** 烘焙画布最大宽（像素）。 */
	public static final int MAX_BAKE_WIDTH = 3072;
	/** 烘焙画布最大高（像素）。 */
	public static final int MAX_BAKE_HEIGHT = 4096;
	/** 最小烘焙密度（再小文字会明显糊）。 */
	public static final float MIN_BAKE_DENSITY = 0.5F;
	/** 极端牌面（超多行）时的硬上限，避免 NativeImage 尺寸失控。 */
	private static final int HARD_MAX_CANVAS = 8192;

	/** 图标兜底标志（docs/EDITOR_MODULE_CONTRACTS.md 约定：未知图标回退 crt_railway）。 */
	private static final String FALLBACK_SIGN_ID = "crt_railway";

	/** 文字贴图缓存上限。 */
	private static final int TEXT_IMAGE_CACHE_LIMIT = 256;

	/** 文字贴图缓存：key = 文本 + 字体 + 字重 + 烘焙字号 + 颜色 + 分辨率。 */
	private static final Map<String, TextImage> TEXT_IMAGES = new LinkedHashMap<String, TextImage>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, TextImage> eldest) {
			return size() > TEXT_IMAGE_CACHE_LIMIT;
		}
	};

	/** 网页图标 id → 本 mod signs.json 标志 id 的映射（未知回退 {@link #FALLBACK_SIGN_ID}）。 */
	private static final Map<String, String> WEB_ICON_TO_SIGN = createIconMapping();

	/** 世界渲染的「最近一帧布局」缓存上限。 */
	private static final int FRAME_CACHE_LIMIT = 64;
	/** 帧缓存有效期：MTR 实时数据最多滞后这么久（烘焙本身也会重烘）。 */
	private static final long FRAME_CACHE_MAX_AGE_MS = 250L;
	/** 布局贴图在内容不变时的复用时间：静态导视牌不需要每秒重烘（内容一变仍立即重烘）。 */
	private static final long LAYOUT_BAKE_REFRESH_MS = 30000L;

	/** 最近一帧布局缓存：key = cacheKey，value = 布局 JSON 哈希 + 时间 + 帧。 */
	private static final Map<String, FrameCache> FRAME_CACHE = new LinkedHashMap<String, FrameCache>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, FrameCache> eldest) {
			return size() > FRAME_CACHE_LIMIT;
		}
	};

	/** 一帧布局缓存项。 */
	private static final class FrameCache {
		private final int layoutHash;
		private final long time;
		private final CRTSignLayoutFrame frame;

		private FrameCache(int layoutHash, long time, CRTSignLayoutFrame frame) {
			this.layoutHash = layoutHash;
			this.time = time;
			this.frame = frame;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// 布局
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 计算一帧布局（对应网页 layoutSign）。
	 *
	 * <p>固定宽度模式：三通道（left/center/right）各自独立排布；动态宽度模式：宽度由最宽行
	 * 决定（下限一个行高），全部左起。元素的宽度/基线/逐字坐标按 §5.7 十种配方计算。</p>
	 */
	public static CRTSignLayoutFrame layout(@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data) {
		return layoutInternal(layout, data, -1);
	}

	/**
	 * 实际布局实现。
	 *
	 * @param knownLayoutHash 已算好的 layout JSON 哈希（&lt; 0 表示内部自行计算），
	 *                        世界渲染的帧缓存用它省掉重复 toJson
	 */
	private static CRTSignLayoutFrame layoutInternal(@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data, int knownLayoutHash) {
		if (layout == null) {
			return new CRTSignLayoutFrame(1.0F, 1.0F);
		}
		// 相邻自动内边距（网页 applyPaddingAuto）：每次布局前重算，保证编辑中的牌面与导出结果一致
		layout.applyPaddingAuto();
		final CRTSignDataContext context = data == null ? CRTSignDataContext.EMPTY : data;
		final float rowHeight = Math.max(1.0F, layout.rowHeight);
		final int rowCount = layout.rows.size();
		final float frameHeight = Math.max(rowHeight, rowHeight * Math.max(1, rowCount));

		// ── 第一遍：逐元素解析内容 + 计算 metrics（宽度只依赖行高与文本） ──
		final List<RowBuild> rowBuilds = new ArrayList<>(rowCount);
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			final CRTSignLayout.Row row = layout.rows.get(rowIndex);
			final RowBuild build = new RowBuild(row, rowIndex);
			for (int index = 0; index < row.elements.size(); index++) {
				final CRTSignElement element = row.elements.get(index);
				final ResolvedContent content = resolve(element, context);
				final CRTSignLayoutFrame.Metrics metrics = metrics(element, rowHeight, content);
				build.elements.add(element);
				build.contents.add(content);
				build.metrics.add(metrics);
				build.widths.add(metrics.width);
				build.contentWidth += metrics.width;
			}
			rowBuilds.add(build);
		}

		// ── 牌面宽度：固定 = sign.width；动态 = max(rowHeight, 最宽行内容宽) 四舍五入 ──
		final boolean fixed = layout.widthMode != CRTSignLayout.WidthMode.DYNAMIC;
		final float frameWidth;
		if (fixed) {
			frameWidth = Math.max(1.0F, layout.width);
		} else {
			float widest = rowHeight;
			for (final RowBuild build : rowBuilds) {
				widest = Math.max(widest, build.contentWidth);
			}
			frameWidth = Math.max(1.0F, Math.round(widest));
		}

		final CRTSignLayoutFrame frame = new CRTSignLayoutFrame(frameWidth, frameHeight);
		frame.layout = layout;
		frame.backgroundColor = layout.backgroundColor;
		frame.frameWidth = layout.frameWidth;
		frame.frameColor = layout.frameColor;

		long fingerprint = 1125899906842597L;

		// ── 第二遍：行内三通道水平布局（网页 render.js:547-580） ──
		for (final RowBuild build : rowBuilds) {
			final int elementCount = build.elements.size();
			float centerTotal = 0.0F;
			if (fixed) {
				for (int index = 0; index < elementCount; index++) {
					if (build.elements.get(index).elementAlign == CRTSignElement.ElementAlign.CENTER) {
						centerTotal += build.widths.get(index);
					}
				}
			}
			final CRTSignLayoutFrame.RowFrame rowFrame = new CRTSignLayoutFrame.RowFrame(
					build.row, build.rowIndex, build.rowIndex * rowHeight, rowHeight, build.contentWidth);
			float leftX = 0.0F;
			float rightX = frameWidth;
			float centerX = (frameWidth - centerTotal) / 2.0F;
			for (int index = 0; index < elementCount; index++) {
				final CRTSignElement element = build.elements.get(index);
				final float width = build.widths.get(index);
				final CRTSignElement.ElementAlign lane = fixed ? element.elementAlign : CRTSignElement.ElementAlign.LEFT;
				final float x;
				if (lane == CRTSignElement.ElementAlign.RIGHT) {
					rightX -= width;
					x = rightX;
				} else if (lane == CRTSignElement.ElementAlign.CENTER) {
					x = centerX;
					centerX += width;
				} else {
					x = leftX;
					leftX += width;
				}
				final CRTSignLayoutFrame.ElementFrame elementFrame = new CRTSignLayoutFrame.ElementFrame(
						element, build.row.id, build.rowIndex, index, x, build.rowIndex * rowHeight,
						width, rowHeight, build.metrics.get(index));
				rowFrame.elements.add(elementFrame);
				fingerprint = fingerprint * 31L + contentHash(build.contents.get(index));
				fingerprint = fingerprint * 31L + safe(element.id).hashCode();
			}
			frame.rows.add(rowFrame);
		}
		frame.contentFingerprint = fingerprint;
		frame.layoutJsonHash = knownLayoutHash >= 0 ? knownLayoutHash : layout.toJson().hashCode();
		return frame;
	}

	// ═══════════════════════════════════════════════════════════════
	// 绘制
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 把一帧布局画进 {@link JSDrawSink}（设计像素坐标，原点在牌面左上角）。
	 *
	 * <p>叠放顺序（WEB_EDITOR_SPEC §5.10）：牌面背景 → 每行 [行背景 → 元素盒背景 → 元素内容
	 * → 灰框]。灰框在该行元素之后绘制，因此会遮挡越界元素（与网页一致）。</p>
	 */
	public static void draw(@Nullable JSDrawSink sink, @Nullable CRTSignLayoutFrame frame, @Nullable CRTSignDataContext data) {
		drawInternal(sink, frame, 1.0F);
	}

	/**
	 * 同 {@link #draw}，但指定「烘焙像素 / 设计像素」的密度，文字贴图按该密度生成，
	 * 从而在降密度烘焙时仍然是 1:1 像素（不糊）。
	 */
	static void drawInternal(@Nullable JSDrawSink sink, @Nullable CRTSignLayoutFrame frame, float pixelDensity) {
		if (sink == null || frame == null) {
			return;
		}
		final float density = pixelDensity > 0.0F ? pixelDensity : 1.0F;

		// 牌面背景（sign-bg）
		sink.fillRect(0.0F, 0.0F, frame.width, frame.height, frame.backgroundColor);

		for (final CRTSignLayoutFrame.RowFrame rowFrame : frame.rows) {
			// 行背景（row-bg）：未单独设色时继承牌面背景
			final Integer rowBackground = rowFrame.row == null ? null : rowFrame.row.backgroundColor;
			sink.fillRect(0.0F, rowFrame.y, frame.width, rowFrame.height,
					rowBackground == null ? frame.backgroundColor : rowBackground);

			for (final CRTSignLayoutFrame.ElementFrame elementFrame : rowFrame.elements) {
				drawElement(sink, elementFrame, density);
			}

			// 灰框（sign-frame）：画在本行元素之后，遮挡越界内容；相邻行各承担共享分割线一半
			if (frame.frameWidth > 0) {
				final float fw = frame.frameWidth;
				final boolean first = rowFrame.rowIndex == 0;
				final boolean last = rowFrame.rowIndex == frame.rows.size() - 1;
				final float top = first ? fw : fw / 2.0F;
				final float bottom = last ? fw : fw / 2.0F;
				final int frameColor = frame.frameColor;
				sink.fillRect(0.0F, rowFrame.y, fw, rowFrame.height, frameColor);
				sink.fillRect(frame.width - fw, rowFrame.y, fw, rowFrame.height, frameColor);
				sink.fillRect(0.0F, rowFrame.y, frame.width, top, frameColor);
				sink.fillRect(0.0F, rowFrame.y + rowFrame.height - bottom, frame.width, bottom, frameColor);
			}
		}
	}

	/** 画单个元素（盒背景 + 内容），坐标为设计像素、元素自身原点在左上。 */
	private static void drawElement(JSDrawSink sink, CRTSignLayoutFrame.ElementFrame elementFrame, float density) {
		final CRTSignLayoutFrame.Metrics metrics = elementFrame.metrics;
		final CRTSignElement element = elementFrame.element;
		if (metrics == null || element == null) {
			return;
		}
		final float originX = elementFrame.drawX();
		final float originY = elementFrame.y;

		// 元素盒背景（仅 backgroundColor != null；width 用 ceil 消除亚像素缝）
		if (element.backgroundColor != null) {
			sink.fillRect(originX, originY, elementFrame.boxWidth(), elementFrame.height, element.backgroundColor);
		}

		// number-line / text-line 的文字颜色来自 props.textColor（见 WEB_EDITOR_SPEC §4.4.5/§4.4.6），其余元素用 color
		final int contentColor = element.type == CRTSignElementType.NUMBER_LINE || element.type == CRTSignElementType.TEXT_LINE
				? element.textColor : element.color;
		switch (element.type) {
			case ARROW:
				drawArrow(sink, metrics, originX, originY, element.color);
				break;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				drawBilingualElement(sink, metrics, originX, originY, element, density);
				break;
			case BIG_NUMBER:
				drawGlyphs(sink, metrics.glyphs, metrics.fontType, false, metrics.fontSize,
						originX + metrics.padLeft, originY + metrics.base, contentColor, density);
				break;
			case NUMBER_LINE:
				drawNumberLine(sink, metrics, originX, originY, contentColor, density);
				break;
			case TEXT_LINE:
				drawTextLine(sink, metrics, originX, originY, element, contentColor, density);
				break;
			case ENTRANCE:
				drawCodeLabel(sink, metrics, originX, originY, contentColor, density, LABEL_ENTRANCE_ZH, LABEL_ENTRANCE_EN);
				break;
			case EXIT:
				drawCodeLabel(sink, metrics, originX, originY, contentColor, density, LABEL_EXIT_ZH, LABEL_EXIT_EN);
				break;
			case ICON:
				drawIcon(sink, metrics, originX, originY, element);
				break;
			case SPACE:
			default:
				break;
		}
	}

	// ── arrow ──────────────────────────────────────────────────────

	private static void drawArrow(JSDrawSink sink, CRTSignLayoutFrame.Metrics metrics, float originX, float originY, int color) {
		final float offsetX = originX + metrics.padLeft;
		final float offsetY = originY + metrics.padTop;
		for (final float[] polygon : metrics.polygons) {
			final int pointCount = polygon.length / 2;
			final float[] xs = new float[pointCount];
			final float[] ys = new float[pointCount];
			for (int i = 0; i < pointCount; i++) {
				xs[i] = polygon[i * 2] + offsetX;
				ys[i] = polygon[i * 2 + 1] + offsetY;
			}
			sink.fillPolygon(xs, ys, color);
		}
	}

	// ── bilingual text ─────────────────────────────────────────────

	private static void drawBilingualElement(JSDrawSink sink, CRTSignLayoutFrame.Metrics metrics, float originX, float originY, CRTSignElement element, float density) {
		final CRTSignLayoutFrame.BilingualLabel label = metrics.label;
		if (label == null) {
			return;
		}
		drawBilingualBox(sink, label, metrics.textZh, metrics.textEn,
				originX + metrics.padLeft, originY + metrics.padTop,
				element.color, element.align, metrics.contentSize * EN_ALIGN_OFFSET, density);
	}

	/** 通用双语双行文本绘制（网页 drawBilingualBox）：x 为纯文本区左缘、top 为内容盒顶。 */
	private static void drawBilingualBox(JSDrawSink sink, CRTSignLayoutFrame.BilingualLabel label, String textZh, String textEn,
			float x, float top, int color, CRTSignElement.TextAlign align, float indent, float density) {
		if (label == null) {
			return;
		}
		final float zhX;
		final float enX;
		if (align == CRTSignElement.TextAlign.LEFT) {
			zhX = x;
			enX = x + indent;
		} else if (align == CRTSignElement.TextAlign.RIGHT) {
			zhX = x + label.width - label.zhW;
			enX = x + label.width - label.enW - indent;
		} else {
			zhX = x + (label.width - label.zhW) / 2.0F;
			enX = x + (label.width - label.enW) / 2.0F;
		}
		drawTextStart(sink, textZh, label.fontType, label.zhSize, zhX, top + label.zhBase, color, density);
		drawTextStart(sink, textEn, label.fontType, label.enSize, enX, top + label.enBase, color, density);
	}

	// ── number-line ────────────────────────────────────────────────

	private static void drawNumberLine(JSDrawSink sink, CRTSignLayoutFrame.Metrics metrics, float originX, float originY, int textColor, float density) {
		for (final CRTSignLayoutFrame.NumberEntry entry : metrics.numberEntries) {
			sink.fillRect(originX + entry.stripeX, originY + metrics.padTop, metrics.stripeW, metrics.stripeH, entry.color);
			drawGlyphs(sink, entry.digits, entry.fontType, false, metrics.fontSize,
					originX + entry.digitsX, originY + entry.base, textColor, density);
		}
		drawBilingualBox(sink, metrics.label, LABEL_NUMBER_ZH, LABEL_NUMBER_EN,
				originX + metrics.labelX, originY + metrics.padTop, textColor,
				metrics.labelAlignRight ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT, 0.0F, density);
	}

	// ── text-line ──────────────────────────────────────────────────

	private static void drawTextLine(JSDrawSink sink, CRTSignLayoutFrame.Metrics metrics, float originX, float originY, CRTSignElement element, int textColor, float density) {
		sink.fillRect(originX + metrics.stripeX, originY + metrics.padTop, metrics.stripeW, metrics.stripeH, element.blockColor);
		final CRTSignElement.TextAlign labelAlign = metrics.labelAlignRight
				? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT;
		if (metrics.sink) {
			// 下沉模式：色条右侧的大字（逐字墨区排版）+「线 / 英文名」标签
			drawGlyphs(sink, metrics.glyphs, metrics.fontType, false, metrics.fontSize,
					originX + metrics.textX, originY + metrics.base, textColor, density);
			drawBilingualBox(sink, metrics.label, LABEL_TEXT_LINE_ZH, metrics.textEn,
					originX + metrics.labelX, originY + metrics.padTop, textColor, labelAlign, 0.0F, density);
		} else {
			// 平铺模式：完整线路名 + 英文名，同一 0.75s 双语标签排版，无「线」后缀
			drawBilingualBox(sink, metrics.label, metrics.text, metrics.textEn,
					originX + metrics.labelX, originY + metrics.padTop, textColor, labelAlign, 0.0F, density);
		}
		// 内容对齐为右时色条在右缘，标签对齐也随内容对齐（web 的 labelAlign 语义）
	}

	// ── entrance / exit ────────────────────────────────────────────

	private static void drawCodeLabel(JSDrawSink sink, CRTSignLayoutFrame.Metrics metrics, float originX, float originY, int color, float density, String labelZh, String labelEn) {
		if (!metrics.glyphs.isEmpty()) {
			drawGlyphs(sink, metrics.glyphs, metrics.fontType, false, metrics.codeSize,
					originX + metrics.codeX, originY + metrics.codeBase, color, density);
		} else if (!metrics.code.isEmpty()) {
			// 空编号墨区布局退化：整串以 text-anchor = middle 居中（网页 render.js:750-764）
			final float advance = CRTSignTextMetrics.ink(metrics.code, metrics.fontType, false, metrics.codeSize).adv;
			drawTextStart(sink, metrics.code, metrics.fontType, metrics.codeSize,
					originX + metrics.codeCenterX - advance / 2.0F, originY + metrics.codeBase, color, density);
		}
		drawBilingualBox(sink, metrics.label, labelZh, labelEn,
				originX + metrics.labelX, originY + metrics.padTop, color,
				metrics.labelAlignRight ? CRTSignElement.TextAlign.RIGHT : CRTSignElement.TextAlign.LEFT, 0.0F, density);
	}

	// ── icon ───────────────────────────────────────────────────────

	private static void drawIcon(JSDrawSink sink, CRTSignLayoutFrame.Metrics metrics, float originX, float originY, CRTSignElement element) {
		final SignResource sign = resolveIconSign(element);
		if (sign == null) {
			return;
		}
		final Identifier texture = sign.getTexture();
		if (texture == null) {
			return;
		}
		final float size = metrics.contentSize;
		if (size <= 0.0F) {
			return;
		}
		final float x = originX + metrics.padLeft;
		final float y = originY + metrics.padTop;
		if (metrics.iconRotation != 0) {
			sink.drawTextureRotated(texture, x + size / 2.0F, y + size / 2.0F, size, size,
					(float) Math.toRadians(metrics.iconRotation), -1);
		} else if (sign.getFlipTexture()) {
			sink.drawTextureFlipped(texture, x, y, size, size, -1);
		} else {
			sink.drawTexture(texture, x, y, size, size, -1);
		}
	}

	/** 图标 id → signs.json 标志（以 crt_/nanbin_ 开头时直接使用该标志，网页图标 id 走映射，未知回退 crt_railway）。 */
	@Nullable
	private static SignResource resolveIconSign(CRTSignElement element) {
		final String rawId = element.icon == null ? "" : element.icon.trim();
		if (!rawId.isEmpty()) {
			final boolean directSign = rawId.startsWith("crt_") || rawId.startsWith("nanbin_");
			if (directSign) {
				final SignResource direct = RenderRailwaySign.getSign(rawId);
				if (direct != null) {
					return direct;
				}
			}
			final String mappedId = WEB_ICON_TO_SIGN.get(rawId.toLowerCase(Locale.ROOT));
			if (mappedId != null) {
				final SignResource mapped = RenderRailwaySign.getSign(mappedId);
				if (mapped != null) {
					return mapped;
				}
			}
			if (!directSign) {
				final SignResource direct = RenderRailwaySign.getSign(rawId);
				if (direct != null) {
					return direct;
				}
			}
		}
		return RenderRailwaySign.getSign(FALLBACK_SIGN_ID);
	}

	// ── 文字绘制（FittedTextRenderer 生成 NativeImage → sink.drawImage） ──

	/**
	 * 以「整串前进宽起点」绘制一行文字（SVG text-anchor = start 语义）：
	 * {@code x} 为排版原点，{@code baselineY} 为基线。
	 */
	private static void drawTextStart(JSDrawSink sink, String text, FontType fontType, float designFontSize,
			float x, float baselineY, int color, float density) {
		if (text == null || text.isEmpty() || designFontSize <= 0.0F) {
			return;
		}
		final TextImage textImage = textImage(text, fontType, false, designFontSize, density, color);
		if (textImage == null) {
			return;
		}
		// 贴图像素 → 设计像素 的换算：imageFontPx 对应 designFontSize
		final float designPerImagePixel = designFontSize / textImage.imageFontPx;
		final float imageX = x - textImage.paddingPx * designPerImagePixel;
		final float imageY = baselineY - (textImage.paddingPx + textImage.ascentPx) * designPerImagePixel;
		final float imageWidth = textImage.image.getWidth() * designPerImagePixel;
		final float imageHeight = textImage.image.getHeight() * designPerImagePixel;
		sink.drawImage(textImage.image, imageX, imageY, imageWidth, imageHeight, -1);
	}

	/**
	 * 以「每个字形的 text-anchor = middle 绘制点」绘制一行文字：{@code centerX} 为
	 * 字形前进盒中心（网页逐字墨区排版的坐标语义）。
	 */
	private static void drawTextMiddle(JSDrawSink sink, String text, FontType fontType, float designFontSize,
			float centerX, float baselineY, int color, float density) {
		if (text == null || text.isEmpty() || designFontSize <= 0.0F) {
			return;
		}
		final float advance = CRTSignTextMetrics.ink(text, fontType, false, designFontSize).adv;
		drawTextStart(sink, text, fontType, designFontSize, centerX - advance / 2.0F, baselineY, color, density);
	}

	/** 逐字绘制（tx 为整组原点；每个 glyph 的 x 是 text-anchor = middle 的点）。 */
	private static void drawGlyphs(JSDrawSink sink, List<CRTSignLayoutFrame.Glyph> glyphs, FontType fontType,
			boolean bold, float fontSize, float originX, float baselineY, int color, float density) {
		if (glyphs == null || glyphs.isEmpty()) {
			return;
		}
		for (final CRTSignLayoutFrame.Glyph glyph : glyphs) {
			drawTextMiddle(sink, glyph.ch, fontType, fontSize, originX + glyph.x, baselineY, color, density);
		}
	}

	/**
	 * 取（并缓存）一行文字的贴图。按烘焙像素生成 1:1 的文字贴图
	 * （{@link FittedTextRenderer} 内部再按 MTR 动态贴图分辨率放大）。
	 */
	@Nullable
	private static TextImage textImage(String text, FontType fontType, boolean bold, float designFontSize, float density, int color) {
		final float resolutionScale = FontRenderUtils.getResolutionScale();
		final int targetPixelFontSize = Math.max(1, Math.round(designFontSize * density));
		final int fittedFontSize = Math.max(1, Math.round(targetPixelFontSize / resolutionScale));
		final String key = fontType.name() + '|' + bold + '|' + fittedFontSize + '|' + color + '|'
				+ Float.floatToIntBits(resolutionScale) + '|' + text;
		synchronized (TEXT_IMAGES) {
			final TextImage cached = TEXT_IMAGES.get(key);
			if (cached != null) {
				return cached;
			}
		}
		final Font rawFont = CustomFontTextureCache.instance.getJavaFont(fontType);
		if (rawFont == null) {
			return null;
		}
		final NativeImage image = FittedTextRenderer.render(text, rawFont, fittedFontSize, new Color(color, true), bold);
		if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
			return null;
		}
		// 复刻 FittedTextRenderer 内部的字号放大 / padding / 基线，用于精确摆放贴图
		final int imageFontPx = Math.max(1, Math.round(fittedFontSize * resolutionScale));
		final int paddingPx = Math.max(2, Math.round(fittedFontSize * 0.08F * resolutionScale));
		final int ascentPx = measureAscentPx(rawFont, bold, imageFontPx);
		final TextImage textImage = new TextImage(image, paddingPx, ascentPx, imageFontPx);
		synchronized (TEXT_IMAGES) {
			TEXT_IMAGES.put(key, textImage);
		}
		return textImage;
	}

	/** 与 FittedTextRenderer 内部一致：用同一派生字体的 FontMetrics.getAscent()。 */
	private static int measureAscentPx(Font rawFont, boolean bold, int imageFontPx) {
		try {
			final Font textFont = rawFont.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) imageFontPx);
			final BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = temp.createGraphics();
			graphics.setFont(textFont);
			final FontMetrics metrics = graphics.getFontMetrics(textFont);
			final int ascent = metrics.getAscent();
			graphics.dispose();
			return ascent;
		} catch (Exception ignored) {
			return Math.round(imageFontPx * 0.8F);
		}
	}

	/** 一行文字的烘焙结果 + 摆放所需的像素信息。 */
	private static final class TextImage {
		private final NativeImage image;
		/** FittedTextRenderer 画布内的 padding（像素）。 */
		private final int paddingPx;
		/** 字体上伸（像素，基线到画布顶的距离 = padding + ascent）。 */
		private final int ascentPx;
		/** 贴图实际使用的字号（像素）。 */
		private final int imageFontPx;

		private TextImage(NativeImage image, int paddingPx, int ascentPx, int imageFontPx) {
			this.image = image;
			this.paddingPx = paddingPx;
			this.ascentPx = ascentPx;
			this.imageFontPx = imageFontPx;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// 单元素绘制（左栏卡片 / 预设缩略图）
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 把一个元素画进 {@link JSDrawSink}（设计像素坐标，元素原点在 (0, 0)）。
	 * 与整牌绘制共用同一套 metrics 与绘制函数，左栏缩略图因此与牌面一致。
	 */
	public static void drawElementStandalone(@Nullable JSDrawSink sink, @Nullable CRTSignElement element,
			float rowHeight, @Nullable CRTSignDataContext data) {
		if (sink == null || element == null || rowHeight <= 0.0F) {
			return;
		}
		final CRTSignDataContext context = data == null ? CRTSignDataContext.EMPTY : data;
		final ResolvedContent content = resolve(element, context);
		final CRTSignLayoutFrame.Metrics metrics = metrics(element, rowHeight, content);
		final CRTSignLayoutFrame.ElementFrame elementFrame = new CRTSignLayoutFrame.ElementFrame(
				element, "", 0, 0, 0.0F, 0.0F, metrics.width, rowHeight, metrics);
		drawElement(sink, elementFrame, 1.0F);
	}

	/** 单元素渲染宽度（设计像素，含左右 padding）；与 {@link #drawElementStandalone} 配套。 */
	public static float standaloneWidth(@Nullable CRTSignElement element, float rowHeight, @Nullable CRTSignDataContext data) {
		if (element == null || rowHeight <= 0.0F) {
			return 0.0F;
		}
		final CRTSignDataContext context = data == null ? CRTSignDataContext.EMPTY : data;
		return metrics(element, rowHeight, resolve(element, context)).width;
	}

	/**
	 * 把单个元素烘焙成贴图（左栏卡片缩略图用）：贴图宽 = 元素宽度、高 = 行高，
	 * 1 设计像素 ≈ 1 贴图像素，背景透明。
	 */
	@Nullable
	public static JSSignTextureBaker.Baked bakeElementStandalone(@Nullable CRTSignElement element, float rowHeight,
			@Nullable CRTSignDataContext data, String cacheKey) {
		if (element == null || rowHeight <= 0.0F) {
			return null;
		}
		final CRTSignDataContext context = data == null ? CRTSignDataContext.EMPTY : data;
		final ResolvedContent content = resolve(element, context);
		final CRTSignLayoutFrame.Metrics metrics = metrics(element, rowHeight, content);
		final float width = Math.max(1.0F, metrics.width);
		final float height = Math.max(1.0F, rowHeight);
		final float density = bakeDensity(width, height);
		final long contentKey = contentHash(content) ^ ((long) Float.floatToIntBits(width) << 32);
		final CRTSignLayoutFrame.ElementFrame elementFrame = new CRTSignLayoutFrame.ElementFrame(
				element, "", 0, 0, 0.0F, 0.0F, width, height, metrics);
		final String signKey = "crt_layout_element:" + (cacheKey == null ? "default" : cacheKey);
		return JSSignTextureBaker.bake(signKey, contentKey, 0.0F, 0.0F, width, height, density, () -> {
			final JSDrawSink sink = JSDrawSink.current();
			if (sink != null) {
				drawElement(sink, elementFrame, density);
			}
		});
	}

	// ═══════════════════════════════════════════════════════════════
	// 烘焙（世界渲染 + 编辑预览共用）
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 把一块自由布局牌烘焙成一张贴图（世界渲染与编辑预览共用同一张贴图）。
	 *
	 * <p>贴图 UV 覆盖 (0,0)-(1,1)，宽高比 = 布局宽高比；按设计像素 1:1 烘焙
	 * （超宽/超高再整体降密度，最低 {@link #MIN_BAKE_DENSITY}）。内容指纹 = 布局 JSON +
	 * 绑定数据解析结果，内容变化立即重烘，否则沿用 JSSignTextureBaker 的 1s 节流刷新。</p>
	 *
	 * <p>世界渲染每帧都会调用本方法：这里有一层「最近一帧布局」缓存（按 cacheKey + 布局 JSON
	 * 哈希，250ms 内复用），避免每帧重新测量文本；MTR 数据变化最多 250ms 后生效
	 * （JSSignTextureBaker 本身也是 1s 节流重烘，不会更慢）。</p>
	 */
	@Nullable
	public static JSSignTextureBaker.Baked bake(@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data, @Nullable String cacheKey) {
		return bake(layout, data, cacheKey, -1);
	}

	/**
	 * 同上，但可由调用方提供布局 JSON 的哈希（世界渲染每帧都有 NBT 里的 JSON 字符串，
	 * {@code String.hashCode()} 有缓存，避免每帧重新 {@code toJson()}）。
	 *
	 * @param layoutJsonHash 布局 JSON 的 {@code hashCode()}；&lt; 0 表示内部自行计算
	 */
	@Nullable
	public static JSSignTextureBaker.Baked bake(@Nullable CRTSignLayout layout, @Nullable CRTSignDataContext data, @Nullable String cacheKey, int layoutJsonHash) {
		if (layout == null) {
			return null;
		}
		final String signKey = cacheKey == null ? "default" : cacheKey;
		final int layoutHash = layoutJsonHash >= 0 ? layoutJsonHash : layout.toJson().hashCode();
		final long now = System.currentTimeMillis();
		CRTSignLayoutFrame frame = null;
		synchronized (FRAME_CACHE) {
			final FrameCache cached = FRAME_CACHE.get(signKey);
			if (cached != null && cached.layoutHash == layoutHash && now - cached.time < FRAME_CACHE_MAX_AGE_MS) {
				frame = cached.frame;
			}
		}
		if (frame == null) {
			frame = layoutInternal(layout, data, layoutHash);
			synchronized (FRAME_CACHE) {
				FRAME_CACHE.put(signKey, new FrameCache(layoutHash, now, frame));
			}
		}
		return bakeFrame(frame, layout, data, cacheKey);
	}

	/** 已算好布局时直接烘焙（编辑预览每帧需要 frame 做命中测试，避免重复 layout）。 */
	@Nullable
	static JSSignTextureBaker.Baked bakeFrame(@Nullable CRTSignLayoutFrame frame, @Nullable CRTSignLayout layout,
			@Nullable CRTSignDataContext data, @Nullable String cacheKey) {
		if (frame == null || frame.width <= 0.0F || frame.height <= 0.0F) {
			return null;
		}
		final float density = bakeDensity(frame.width, frame.height);
		final long contentKey = layoutFingerprint(frame);
		final String signKey = "crt_layout:" + (cacheKey == null ? "default" : cacheKey);
		return JSSignTextureBaker.bake(signKey, contentKey, 0.0F, 0.0F, frame.width, frame.height, density, LAYOUT_BAKE_REFRESH_MS, () -> {
			final JSDrawSink sink = JSDrawSink.current();
			if (sink != null) {
				drawInternal(sink, frame, density);
			}
		});
	}

	/** 计算烘焙密度：设计像素 1:1，超过画布上限再整体降；不低于 {@link #MIN_BAKE_DENSITY}。 */
	public static float bakeDensity(float width, float height) {
		if (width <= 0.0F || height <= 0.0F) {
			return 1.0F;
		}
		float density = 1.0F;
		if (width > MAX_BAKE_WIDTH) {
			density = Math.min(density, MAX_BAKE_WIDTH / width);
		}
		if (height > MAX_BAKE_HEIGHT) {
			density = Math.min(density, MAX_BAKE_HEIGHT / height);
		}
		density = Math.max(density, MIN_BAKE_DENSITY);
		// 极端牌面（行数极多/宽度极大）时再压到硬上限，避免 NativeImage 尺寸失控
		if (width * density > HARD_MAX_CANVAS || height * density > HARD_MAX_CANVAS) {
			density = Math.min(HARD_MAX_CANVAS / width, HARD_MAX_CANVAS / height);
		}
		return Math.max(0.01F, density);
	}

	/** 布局 + 绑定数据的内容指纹（frame.layoutJsonHash 覆盖所有设置/元素属性，frame 内容指纹覆盖数据解析结果）。 */
	private static long layoutFingerprint(CRTSignLayoutFrame frame) {
		long hash = 1125899906842597L;
		hash = hash * 31L + frame.layoutJsonHash;
		hash = hash * 31L + frame.contentFingerprint;
		return hash;
	}

	/** 清空文字贴图缓存（资源重载/字体变化后调用）。 */
	public static void clearCaches() {
		synchronized (TEXT_IMAGES) {
			TEXT_IMAGES.clear();
		}
		synchronized (FRAME_CACHE) {
			FRAME_CACHE.clear();
		}
		CRTSignTextMetrics.clearCache();
	}

	// ═══════════════════════════════════════════════════════════════
	// 各元素 metrics（纯几何；对应网页 render.js 的 *Metrics 函数）
	// ═══════════════════════════════════════════════════════════════

	/** 计算某元素在行高 rowHeight 下的 metrics（宽度 + 全部绘制坐标，设计像素）。 */
	private static CRTSignLayoutFrame.Metrics metrics(CRTSignElement element, float rowHeight, ResolvedContent content) {
		switch (element.type) {
			case ARROW:
				return arrowMetrics(element, rowHeight);
			case BILINGUAL_TEXT:
				return bilingualElementMetrics(element, rowHeight, content, 1.0F);
			case SMALL_BILINGUAL_TEXT:
				return bilingualElementMetrics(element, rowHeight, content, LABEL_BOX_RATIO);
			case BIG_NUMBER:
				return bigNumberMetrics(element, rowHeight, content);
			case NUMBER_LINE:
				return numberLineMetrics(element, rowHeight, content);
			case TEXT_LINE:
				return textLineMetrics(element, rowHeight, content);
			case ENTRANCE:
				return codeLabelMetrics(element, rowHeight, content, false);
			case EXIT:
				return codeLabelMetrics(element, rowHeight, content, true);
			case ICON:
				return iconMetrics(element, rowHeight);
			case SPACE:
			default:
				return spaceMetrics(element, rowHeight);
		}
	}

	/** 内容盒（网页 contentBox）：比例 padding × 行高；s = 行高 − 上下 padding。 */
	private static CRTSignLayoutFrame.Metrics baseMetrics(CRTSignElement element, float rowHeight, ResolvedContent content) {
		final CRTSignLayoutFrame.Metrics metrics = new CRTSignLayoutFrame.Metrics();
		metrics.padTop = element.paddingTop * rowHeight;
		metrics.padRight = element.paddingRight * rowHeight;
		metrics.padBottom = element.paddingBottom * rowHeight;
		metrics.padLeft = element.paddingLeft * rowHeight;
		metrics.contentSize = rowHeight - metrics.padTop - metrics.padBottom;
		metrics.textZh = content.textZh;
		metrics.textEn = content.textEn;
		metrics.text = content.text;
		metrics.code = content.code;
		metrics.lines.addAll(content.lines);
		return metrics;
	}

	// ── arrow（§5.7.1） ────────────────────────────────────────────

	private static CRTSignLayoutFrame.Metrics arrowMetrics(CRTSignElement element, float rowHeight) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, EMPTY_CONTENT);
		final float s = metrics.contentSize;
		final float thickness = clamp(element.thicknessRatio, 0.05F, 0.95F) * s;
		metrics.polygons.addAll(arrowGeometry(s, thickness, element.arrowDirection));
		metrics.width = s + metrics.padLeft + metrics.padRight;
		return metrics;
	}

	/** 箭头几何（网页 render.js:56-127）：返回 head1 / head2 / shaft 三块多边形（交错 x,y）。 */
	private static List<float[]> arrowGeometry(float size, float thickness, CRTSignElement.ArrowDirection direction) {
		if (direction == null) {
			return axisArrowGeometry(size, thickness, CRTSignElement.ArrowDirection.LEFT);
		}
		switch (direction) {
			case LEFT_UP:
				return diagonalArrowGeometry(size, thickness, CRTSignElement.ArrowDirection.LEFT, Math.PI / 4.0D, 0.0D, 0.0D);
			case RIGHT_UP:
				return diagonalArrowGeometry(size, thickness, CRTSignElement.ArrowDirection.RIGHT, -Math.PI / 4.0D, 1.0D, 0.0D);
			case RIGHT_DOWN:
				return diagonalArrowGeometry(size, thickness, CRTSignElement.ArrowDirection.RIGHT, Math.PI / 4.0D, 1.0D, 1.0D);
			case LEFT_DOWN:
				return diagonalArrowGeometry(size, thickness, CRTSignElement.ArrowDirection.LEFT, -Math.PI / 4.0D, 0.0D, 1.0D);
			default:
				return axisArrowGeometry(size, thickness, direction);
		}
	}

	/** 轴向箭头（left/right/up/down）三块多边形。 */
	private static List<float[]> axisArrowGeometry(float size, float thickness, CRTSignElement.ArrowDirection direction) {
		final float c = size / 2.0F;
		final float k = thickness / (2.0F * (float) Math.sqrt(2.0D));
		final List<float[]> polygons = new ArrayList<>(3);
		switch (direction) {
			case RIGHT:
				polygons.add(polygon(
						size, c, size - thickness, c, size - thickness - c, 0.0F, size - c, 0.0F));
				polygons.add(polygon(
						size, c, size - thickness, c, size - thickness - c, size, size - c, size));
				polygons.add(polygon(
						0.0F, c - k, size - thickness / 2.0F, c - k, size - thickness / 2.0F, c + k, 0.0F, c + k));
				break;
			case UP:
				polygons.add(polygon(
						c, 0.0F, c, thickness, 0.0F, thickness + c, 0.0F, c));
				polygons.add(polygon(
						c, 0.0F, c, thickness, size, thickness + c, size, c));
				polygons.add(polygon(
						c - k, thickness / 2.0F, c + k, thickness / 2.0F, c + k, size, c - k, size));
				break;
			case DOWN:
				polygons.add(polygon(
						c, size, c, size - thickness, 0.0F, size - thickness - c, 0.0F, size - c));
				polygons.add(polygon(
						c, size, c, size - thickness, size, size - thickness - c, size, size - c));
				polygons.add(polygon(
						c - k, 0.0F, c + k, 0.0F, c + k, size - thickness / 2.0F, c - k, size - thickness / 2.0F));
				break;
			case LEFT:
			default:
				polygons.add(polygon(
						0.0F, c, thickness, c, thickness + c, 0.0F, c, 0.0F));
				polygons.add(polygon(
						0.0F, c, thickness, c, thickness + c, size, c, size));
				polygons.add(polygon(
						thickness / 2.0F, c - k, size, c - k, size, c + k, thickness / 2.0F, c + k));
				break;
		}
		return polygons;
	}

	/**
	 * 斜向箭头：轴对齐基底在虚拟盒（L = s√2 − t）内构建，绕中心旋转 ±45°，
	 * 再把箭尖平移到内容盒目标角（网页 render.js:93-127）。
	 */
	private static List<float[]> diagonalArrowGeometry(float size, float thickness, CRTSignElement.ArrowDirection base, double phi, double cornerX, double cornerY) {
		final float virtualSize = (float) (size * Math.sqrt(2.0D) - thickness);
		final List<float[]> axis = axisArrowGeometry(virtualSize, thickness, base);
		final float center = virtualSize / 2.0F;
		final double cos = Math.cos(phi);
		final double sin = Math.sin(phi);
		final double tipX = base == CRTSignElement.ArrowDirection.LEFT ? 0.0D : virtualSize;
		final double tipY = center;
		final double tipRelativeX = (tipX - center) * cos - (tipY - center) * sin;
		final double tipRelativeY = (tipX - center) * sin + (tipY - center) * cos;
		final double offsetX = cornerX * size - tipRelativeX;
		final double offsetY = cornerY * size - tipRelativeY;
		final List<float[]> polygons = new ArrayList<>(3);
		for (final float[] axisPolygon : axis) {
			final float[] polygon = new float[axisPolygon.length];
			for (int i = 0; i < axisPolygon.length; i += 2) {
				final double dx = axisPolygon[i] - center;
				final double dy = axisPolygon[i + 1] - center;
				polygon[i] = (float) (dx * cos - dy * sin + offsetX);
				polygon[i + 1] = (float) (dx * sin + dy * cos + offsetY);
			}
			polygons.add(polygon);
		}
		return polygons;
	}

	private static float[] polygon(float... coordinates) {
		return coordinates;
	}

	// ── bilingual（§5.7.2 / §5.7.3） ───────────────────────────────

	private static CRTSignLayoutFrame.Metrics bilingualElementMetrics(CRTSignElement element, float rowHeight, ResolvedContent content, float boxRatio) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, content);
		final float s = metrics.contentSize;
		final CRTSignLayoutFrame.BilingualLabel label = bilingualMetrics(
				content.textZh, content.textEn, s * boxRatio, element.bold, s);
		// 只有大号双语文本（boxRatio = 1）做整组两行文字的墨区垂直居中；
		// 小号双语沿用组合标签的顶对齐（网页 render.js:188-213）
		if (boxRatio == 1.0F) {
			float topMin = Float.MAX_VALUE;
			float bottomMax = -Float.MAX_VALUE;
			boolean any = false;
			if (!content.textZh.trim().isEmpty()) {
				topMin = Math.min(topMin, label.zhBase - label.zhAscent);
				bottomMax = Math.max(bottomMax, label.zhBase + label.zhDescent);
				any = true;
			}
			if (!content.textEn.trim().isEmpty()) {
				topMin = Math.min(topMin, label.enBase - CRTSignTextMetrics.ascent(content.textEn, label.fontType, false, label.enSize));
				bottomMax = Math.max(bottomMax, label.enBase + CRTSignTextMetrics.descent(content.textEn, label.fontType, false, label.enSize));
				any = true;
			}
			if (any) {
				final float shift = (s - bottomMax - topMin) / 2.0F;
				label.zhBase += shift;
				label.enBase += shift;
			}
		}
		metrics.label = label;
		metrics.textW = label.width;
		metrics.width = label.width + metrics.padLeft + metrics.padRight;
		return metrics;
	}

	/**
	 * 组合双语标签测量（网页 bilingualMetrics）：
	 * 字号 = 自身内容盒 boxSize × 字体比例；垂直锚定用宿主内容尺寸 hostContentSize。
	 */
	private static CRTSignLayoutFrame.BilingualLabel bilingualMetrics(String textZh, String textEn, float boxSize, boolean bold, float hostContentSize) {
		final FontType fontType = textFontType(bold);
		final float zhSize = boxSize * ZH_FONT_RATIO;
		final float enSize = boxSize * EN_FONT_RATIO;
		final float zhW = CRTSignTextMetrics.advance(textZh, fontType, false, zhSize);
		final float enW = CRTSignTextMetrics.advance(textEn, fontType, false, enSize);
		final float zhAscent = CRTSignTextMetrics.ascent(textZh, fontType, false, zhSize);
		final float zhDescent = CRTSignTextMetrics.descent(textZh, fontType, false, zhSize);
		final float enAscent = CRTSignTextMetrics.ascent(textEn, fontType, false, enSize);
		final float zhBase = INK_TOP_LEADING * hostContentSize + zhAscent;
		final float enBase = zhBase + zhDescent + ZH_EN_GAP * hostContentSize + enAscent;
		return new CRTSignLayoutFrame.BilingualLabel(zhSize, enSize, zhW, enW, Math.max(zhW, enW),
				zhBase, enBase, zhAscent, zhDescent, fontType);
	}

	// ── big-number（§5.7.4） ───────────────────────────────────────

	private static CRTSignLayoutFrame.Metrics bigNumberMetrics(CRTSignElement element, float rowHeight, ResolvedContent content) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, content);
		final float s = metrics.contentSize;
		final float fontSize = s * NUM_FONT_RATIO;
		final FontType fontType = numberFontType(content.text);
		final float base = metrics.padTop + INK_TOP_LEADING * s
				+ CRTSignTextMetrics.ascent(content.text, fontType, false, fontSize);
		final InkTextLayout textLayout = inkTextLayout(content.text, s, fontSize, fontType, false);
		// 光学水平居中：首末墨区的左右边距差的一半（数字「1」等窄字形的视觉居中对齐）
		final float optical = (textLayout.firstInkLeft - (textLayout.textW - textLayout.lastInkRight)) / 2.0F;
		for (final CRTSignLayoutFrame.Glyph glyph : textLayout.chars) {
			metrics.glyphs.add(new CRTSignLayoutFrame.Glyph(glyph.ch, glyph.x - optical));
		}
		metrics.fontType = fontType;
		metrics.fontSize = fontSize;
		metrics.base = base;
		metrics.textW = textLayout.textW;
		metrics.width = textLayout.textW + metrics.padLeft + metrics.padRight;
		return metrics;
	}

	// ── number-line（§5.7.5） ─────────────────────────────────────

	private static CRTSignLayoutFrame.Metrics numberLineMetrics(CRTSignElement element, float rowHeight, ResolvedContent content) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, content);
		final float s = metrics.contentSize;
		final float h = rowHeight;
		final float stripeWidth = s * STRIPE_RATIO;
		final CRTSignLayoutFrame.BilingualLabel label = bilingualMetrics(LABEL_NUMBER_ZH, LABEL_NUMBER_EN, s * LABEL_BOX_RATIO, false, s);
		final float labelTop = metrics.padTop + label.zhBase - label.zhAscent;
		final boolean right = element.align == CRTSignElement.TextAlign.RIGHT;
		final float fontSize = s * NUM_FONT_RATIO;

		final List<CRTSignElement.LineItem> lines = new ArrayList<>();
		for (final CRTSignElement.LineItem line : content.lines) {
			if (line != null) {
				lines.add(line);
			}
		}
		if (right && lines.size() > 1) {
			lines.subList(1, lines.size()).clear();
		}

		float advanceSum = 0.0F;
		final List<EntryBuild> builds = new ArrayList<>(lines.size());
		for (final CRTSignElement.LineItem line : lines) {
			final String number = line.number == null ? "" : line.number;
			final FontType fontType = numberFontType(number);
			final InkTextLayout textLayout = inkTextLayout(number, s, fontSize, fontType, true);
			final float base = labelTop + CRTSignTextMetrics.ascent(number, fontType, false, fontSize);
			builds.add(new EntryBuild(number, line.color, textLayout, base, fontType));
			advanceSum += textLayout.textW;
		}
		final float stripesWidth = builds.size() * stripeWidth;
		final float firstMargin = builds.isEmpty() ? 0.0F : builds.get(0).layout.margin;
		final float labelX;
		final float stripesX0;
		if (right) {
			labelX = metrics.padLeft + advanceSum;
			stripesX0 = labelX + label.width + firstMargin;
		} else {
			labelX = metrics.padLeft + advanceSum + stripesWidth;
			stripesX0 = metrics.padLeft;
		}
		float advanceAccumulator = 0.0F;
		for (int index = 0; index < builds.size(); index++) {
			final EntryBuild build = builds.get(index);
			final float digitsX;
			final float stripeX;
			if (right) {
				digitsX = metrics.padLeft + advanceAccumulator;
				stripeX = stripesX0 + index * stripeWidth;
			} else {
				stripeX = metrics.padLeft + index * stripeWidth + advanceAccumulator;
				digitsX = stripeX + stripeWidth;
			}
			metrics.numberEntries.add(new CRTSignLayoutFrame.NumberEntry(
					build.number, build.color, build.layout.textW, build.layout.margin, build.base,
					digitsX, stripeX, build.layout.chars, build.fontType));
			advanceAccumulator += build.layout.textW;
		}
		metrics.label = label;
		metrics.labelX = labelX;
		metrics.labelAlignRight = right;
		metrics.stripeW = stripeWidth;
		metrics.stripeH = h - metrics.padTop;
		metrics.fontSize = fontSize;
		metrics.width = right
				? stripesX0 + stripesWidth + metrics.padRight
				: labelX + label.width + metrics.padRight;
		return metrics;
	}

	// ── text-line（§5.7.6） ───────────────────────────────────────

	private static CRTSignLayoutFrame.Metrics textLineMetrics(CRTSignElement element, float rowHeight, ResolvedContent content) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, content);
		final float s = metrics.contentSize;
		final float h = rowHeight;
		final float stripeWidth = s * STRIPE_RATIO;
		final float gap = s / 8.0F;
		final boolean right = element.align == CRTSignElement.TextAlign.RIGHT;
		metrics.stripeW = stripeWidth;
		metrics.stripeH = h - metrics.padTop;
		metrics.gap = gap;
		metrics.labelAlignRight = right;
		metrics.sink = element.nameSink;
		if (element.nameSink) {
			final float fontSize = s;
			final FontType fontType = textFontType(false);
			final InkTextLayout textLayout = inkTextLayout(content.text, s, fontSize, fontType, false);
			final CRTSignLayoutFrame.BilingualLabel label = bilingualMetrics(LABEL_TEXT_LINE_ZH, content.textEn, s * LABEL_BOX_RATIO, false, s);
			final float textX;
			final float labelX;
			final float stripeX;
			if (right) {
				textX = metrics.padLeft;
				labelX = metrics.padLeft + textLayout.textW + gap;
				stripeX = labelX + label.width + gap;
			} else {
				textX = metrics.padLeft + stripeWidth + gap;
				labelX = metrics.padLeft + stripeWidth + gap + textLayout.textW + gap;
				stripeX = metrics.padLeft;
			}
			metrics.fontType = fontType;
			metrics.fontSize = fontSize;
			metrics.base = metrics.padTop + label.zhBase - label.zhAscent
					+ CRTSignTextMetrics.ascent(content.text, fontType, false, fontSize);
			metrics.glyphs.addAll(textLayout.chars);
			metrics.textW = textLayout.textW;
			metrics.textX = textX;
			metrics.label = label;
			metrics.labelX = labelX;
			metrics.stripeX = stripeX;
			metrics.width = right
					? stripeX + stripeWidth + metrics.padRight
					: metrics.padLeft + stripeWidth + gap + textLayout.textW + gap + label.width + metrics.padRight;
		} else {
			final CRTSignLayoutFrame.BilingualLabel flat = bilingualMetrics(content.text, content.textEn, s * LABEL_BOX_RATIO, false, s);
			final float labelX;
			final float stripeX;
			if (right) {
				labelX = metrics.padLeft;
				stripeX = labelX + flat.width + gap;
			} else {
				labelX = metrics.padLeft + stripeWidth + gap;
				stripeX = metrics.padLeft;
			}
			metrics.label = flat;
			metrics.labelX = labelX;
			metrics.stripeX = stripeX;
			metrics.width = right
					? stripeX + stripeWidth + metrics.padRight
					: metrics.padLeft + stripeWidth + gap + flat.width + metrics.padRight;
		}
		return metrics;
	}

	// ── entrance / exit（§5.7.7 / §5.7.8） ────────────────────────

	private static CRTSignLayoutFrame.Metrics codeLabelMetrics(CRTSignElement element, float rowHeight, ResolvedContent content, boolean alignable) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, content);
		final float s = metrics.contentSize;
		final float codeSize = s * NUM_FONT_RATIO;
		final CRTSignLayoutFrame.BilingualLabel label = bilingualMetrics(
				alignable ? LABEL_EXIT_ZH : LABEL_ENTRANCE_ZH,
				alignable ? LABEL_EXIT_EN : LABEL_ENTRANCE_EN,
				s * LABEL_BOX_RATIO, false, s);
		final boolean right = alignable && element.align == CRTSignElement.TextAlign.RIGHT;
		final String code = content.code == null ? "" : content.code;
		final FontType fontType = numberFontType(code);
		final float codeBase = metrics.padTop + label.zhBase - label.zhAscent
				+ CRTSignTextMetrics.ascent(code, fontType, false, codeSize);
		final InkTextLayout codeLayout = inkTextLayout(code, s, codeSize, fontType, false);
		final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink ink3 = CRTSignTextMetrics.ink("3", fontType, false, codeSize);
		final float trailingMargin = (s - ink3.adv) / 2.0F - ink3.abl;
		final float codeWidth = codeLayout.lastInkRight + trailingMargin;
		metrics.code = code;
		metrics.codeSize = codeSize;
		metrics.codeW = codeWidth;
		metrics.codeBase = codeBase;
		metrics.fontType = fontType;
		metrics.label = label;
		if (right) {
			final float rightCodeX = metrics.padLeft + label.width + trailingMargin;
			metrics.codeX = rightCodeX;
			metrics.codeCenterX = rightCodeX + codeWidth / 2.0F;
			metrics.labelX = metrics.padLeft;
			metrics.labelAlignRight = true;
			metrics.width = metrics.padLeft + label.width + codeWidth + metrics.padRight;
		} else {
			metrics.codeX = metrics.padLeft;
			metrics.codeCenterX = metrics.padLeft + codeWidth / 2.0F;
			metrics.labelX = metrics.padLeft + codeWidth;
			metrics.labelAlignRight = false;
			metrics.width = metrics.padLeft + codeWidth + label.width + metrics.padRight;
		}
		metrics.glyphs.addAll(codeLayout.chars);
		return metrics;
	}

	// ── space / icon（§5.7.9 / §5.7.10） ──────────────────────────

	private static CRTSignLayoutFrame.Metrics spaceMetrics(CRTSignElement element, float rowHeight) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, EMPTY_CONTENT);
		// 空白占位：宽度 = widthRatio × 行高（与 padding 无关，无可见内容）
		metrics.width = element.widthRatio * rowHeight;
		return metrics;
	}

	private static CRTSignLayoutFrame.Metrics iconMetrics(CRTSignElement element, float rowHeight) {
		final CRTSignLayoutFrame.Metrics metrics = baseMetrics(element, rowHeight, EMPTY_CONTENT);
		final float s = metrics.contentSize;
		metrics.width = s + metrics.padLeft + metrics.padRight;
		// 网页 §5.7.10：只有服务设施图标响应 rotation，方向箭头图标忽略
		final String iconId = element.icon == null ? "" : element.icon.trim().toLowerCase(Locale.ROOT);
		final boolean arrowIcon = iconId.startsWith("nanbin_arrow") || iconId.startsWith("arrow_");
		final int rotation = element.rotation;
		if (!arrowIcon && (rotation == -90 || rotation == 90 || rotation == 180)) {
			metrics.iconRotation = rotation;
		}
		return metrics;
	}

	// ═══════════════════════════════════════════════════════════════
	// 墨区文本排版（网页 inkTextLayout / digitLayout）
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 逐字墨区排版（网页 inkTextLayout）：
	 * {@code marginEdge = true}（网页 edge='margin'）用于数字线路的线路号，首尾各留基准墨边距 M₃；
	 * {@code marginEdge = false}（edge='lsb'）用于独立文本，首位墨区锚定自身左字肩。
	 */
	private static InkTextLayout inkTextLayout(String text, float slotSize, float fontSize, FontType fontType, boolean marginEdge) {
		final List<CRTSignLayoutFrame.Glyph> chars = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			final float emptyWidth = marginEdge ? slotSize : 0.0F;
			return new InkTextLayout(chars, emptyWidth, 0.0F, emptyWidth, emptyWidth);
		}
		final List<String> codePoints = splitCodePoints(text);
		if (codePoints.size() == 1 && !marginEdge) {
			final float width = CRTSignTextMetrics.advance(text, fontType, false, fontSize);
			final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink ink = CRTSignTextMetrics.ink(text, fontType, false, fontSize);
			chars.add(new CRTSignLayoutFrame.Glyph(text, width / 2.0F));
			final float firstInkLeft = width / 2.0F - ink.adv / 2.0F - ink.abl;
			final float lastInkRight = width / 2.0F - ink.adv / 2.0F + ink.abr;
			return new InkTextLayout(chars, width, 0.0F, firstInkLeft, lastInkRight);
		}

		final DigitLayout digitLayout = digitLayout(text, slotSize, fontSize, fontType);
		if (marginEdge) {
			chars.addAll(digitLayout.digits);
			return new InkTextLayout(chars, digitLayout.advance, digitLayout.margin,
					digitLayout.margin, digitLayout.advance - digitLayout.margin);
		}

		// edge = 'lsb'：整体左移，使首位墨区紧贴自身字距边距，去掉 M₃ 的左边距
		final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink firstInk = CRTSignTextMetrics.ink(codePoints.get(0), fontType, false, fontSize);
		final float shift = -firstInk.abl - digitLayout.margin;
		for (final CRTSignLayoutFrame.Glyph glyph : digitLayout.digits) {
			chars.add(new CRTSignLayoutFrame.Glyph(glyph.ch, glyph.x + shift));
		}
		final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink lastInk = CRTSignTextMetrics.ink(codePoints.get(codePoints.size() - 1), fontType, false, fontSize);
		final CRTSignLayoutFrame.Glyph lastGlyph = chars.get(chars.size() - 1);
		final float textWidth = lastGlyph.x + lastInk.adv / 2.0F;
		final float firstInkLeft = chars.get(0).x - firstInk.adv / 2.0F - firstInk.abl;
		final float lastInkRight = lastGlyph.x - lastInk.adv / 2.0F + lastInk.abr;
		return new InkTextLayout(chars, textWidth, digitLayout.margin, firstInkLeft, lastInkRight);
	}

	/**
	 * 数字组墨区布局（网页 digitLayout）：「3」标定基准墨边距 M₃ 与固定墨区间距，单字两侧各留 M₃。
	 */
	private static DigitLayout digitLayout(String text, float slotSize, float fontSize, FontType fontType) {
		final List<String> chars = splitCodePoints(text);
		if (chars.isEmpty()) {
			return new DigitLayout(new ArrayList<CRTSignLayoutFrame.Glyph>(), slotSize, 0.0F);
		}
		final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink reference = CRTSignTextMetrics.ink("3", fontType, false, fontSize);
		final float margin = (slotSize - reference.adv) / 2.0F - reference.abl;
		if (chars.size() == 1) {
			final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink ink = CRTSignTextMetrics.ink(chars.get(0), fontType, false, fontSize);
			final float inkWidth = ink.abr + ink.abl;
			final List<CRTSignLayoutFrame.Glyph> digits = new ArrayList<>(1);
			digits.add(new CRTSignLayoutFrame.Glyph(chars.get(0), margin + ink.adv / 2.0F + ink.abl));
			return new DigitLayout(digits, margin * 2.0F + inkWidth, margin);
		}
		final float gap = (-reference.abl) + (reference.adv - reference.abr);
		float inkLeft = margin;
		final List<CRTSignLayoutFrame.Glyph> digits = new ArrayList<>(chars.size());
		for (final String ch : chars) {
			final com.Nanbin.client.Drawing.CRTSignTextMetrics.Ink ink = CRTSignTextMetrics.ink(ch, fontType, false, fontSize);
			digits.add(new CRTSignLayoutFrame.Glyph(ch, inkLeft + ink.adv / 2.0F + ink.abl));
			inkLeft += (ink.abr + ink.abl) + gap;
		}
		return new DigitLayout(digits, inkLeft - gap + margin, margin);
	}

	/** 逐字拆分（按 Unicode 码点，避免代理对拆坏；BMP 文本与网页按 UTF-16 拆分结果一致）。 */
	private static List<String> splitCodePoints(String text) {
		final List<String> result = new ArrayList<>();
		if (text == null) {
			return result;
		}
		for (int i = 0; i < text.length(); ) {
			final int codePoint = text.codePointAt(i);
			result.add(new String(Character.toChars(codePoint)));
			i += Character.charCount(codePoint);
		}
		return result;
	}

	private static final class DigitLayout {
		private final List<CRTSignLayoutFrame.Glyph> digits;
		private final float advance;
		private final float margin;

		private DigitLayout(List<CRTSignLayoutFrame.Glyph> digits, float advance, float margin) {
			this.digits = digits;
			this.advance = advance;
			this.margin = margin;
		}
	}

	private static final class InkTextLayout {
		private final List<CRTSignLayoutFrame.Glyph> chars;
		private final float textW;
		private final float margin;
		private final float firstInkLeft;
		private final float lastInkRight;

		private InkTextLayout(List<CRTSignLayoutFrame.Glyph> chars, float textW, float margin, float firstInkLeft, float lastInkRight) {
			this.chars = chars;
			this.textW = textW;
			this.margin = margin;
			this.firstInkLeft = firstInkLeft;
			this.lastInkRight = lastInkRight;
		}
	}

	private static final class EntryBuild {
		private final String number;
		private final int color;
		private final InkTextLayout layout;
		private final float base;
		private final FontType fontType;

		private EntryBuild(String number, int color, InkTextLayout layout, float base, FontType fontType) {
			this.number = number;
			this.color = color;
			this.layout = layout;
			this.base = base;
			this.fontType = fontType;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// MTR 数据绑定
	// ═══════════════════════════════════════════════════════════════

	/** 元素解析后的实际内容（绑定数据优先，取不到时即字面文本）。 */
	private static final class ResolvedContent {
		private String textZh = "";
		private String textEn = "";
		private String text = "";
		private String code = "";
		private final List<CRTSignElement.LineItem> lines = new ArrayList<>();
	}

	private static final ResolvedContent EMPTY_CONTENT = new ResolvedContent();

	private static ResolvedContent resolve(CRTSignElement element, CRTSignDataContext data) {
		final ResolvedContent content = new ResolvedContent();
		content.textZh = safe(element.textZh);
		content.textEn = safe(element.textEn);
		content.text = safe(element.text);
		content.code = safe(element.code);
		for (final CRTSignElement.LineItem line : element.lines) {
			if (line != null) {
				content.lines.add(new CRTSignElement.LineItem(safe(line.number), line.color));
			}
		}

		final CRTSignElement.DataBinding binding = element.binding == null ? CRTSignElement.DataBinding.NONE : element.binding;
		if (binding == CRTSignElement.DataBinding.NONE || data == null || element.bindingIds.isEmpty()) {
			return content;
		}
		try {
			final long firstId = element.bindingIds.get(0);
			switch (binding) {
				case ROUTE_NAME:
					applyResolvedRouteName(element, content, data, firstId);
					break;
				case ROUTE_NUMBER:
					applyResolvedRouteNumber(element, content, data, firstId);
					break;
				case ROUTE_LINES:
					applyResolvedRouteLines(element, content, data);
					break;
				case STATION:
					applyResolvedStation(element, content, data);
					break;
				case EXIT:
					applyResolvedExit(element, content, data, firstId);
					break;
				case PLATFORM:
					applyBilingualName(element, content, splitBilingualName(data.getPlatformName(firstId)));
					break;
				case NEXT_STATION:
					applyBilingualName(element, content, splitBilingualName(data.getNextStation(firstId)));
					break;
				case PREV_STATION:
					applyBilingualName(element, content, splitBilingualName(data.getPrevStation(firstId)));
					break;
				default:
					break;
			}
		} catch (Exception ignored) {
			// MTR 数据接口异常时保持字面文本，避免渲染崩溃
		}
		return content;
	}

	private static void applyResolvedRouteName(CRTSignElement element, ResolvedContent content, CRTSignDataContext data, long color) {
		final String routeName = data.getRouteName(color);
		final String routeNameEn = data.getRouteNameEn(color);
		if (isEmpty(routeName) && isEmpty(routeNameEn)) {
			return;
		}
		if (element.type == CRTSignElementType.TEXT_LINE) {
			if (!isEmpty(routeName)) {
				content.text = routeName;
			}
			if (!isEmpty(routeNameEn)) {
				content.textEn = routeNameEn;
			}
		} else if (element.type == CRTSignElementType.NUMBER_LINE) {
			// 数字线路显示线路名时用「色条 + 名称」的文本线路更合适；这里退化为单条线路名
			content.lines.clear();
			content.lines.add(new CRTSignElement.LineItem(isEmpty(routeName) ? routeNameEn : routeName, (int) color | 0xFF000000));
		} else if (element.type == CRTSignElementType.BIG_NUMBER || element.type == CRTSignElementType.ENTRANCE || element.type == CRTSignElementType.EXIT) {
			content.text = isEmpty(routeName) ? routeNameEn : routeName;
			content.code = content.text;
		} else {
			applyBilingualName(element, content, new String[]{routeName, routeNameEn});
		}
	}

	private static void applyResolvedRouteNumber(CRTSignElement element, ResolvedContent content, CRTSignDataContext data, long color) {
		final String number = data.getRouteNumber(color);
		if (isEmpty(number)) {
			return;
		}
		switch (element.type) {
			case NUMBER_LINE:
				content.lines.clear();
				content.lines.add(new CRTSignElement.LineItem(number, (int) color | 0xFF000000));
				break;
			case ENTRANCE:
			case EXIT:
				content.code = number;
				break;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				content.textZh = number;
				content.textEn = "";
				break;
			case TEXT_LINE:
				content.text = number;
				break;
			case BIG_NUMBER:
			default:
				content.text = number;
				break;
		}
	}

	private static void applyResolvedRouteLines(CRTSignElement element, ResolvedContent content, CRTSignDataContext data) {
		final List<CRTSignElement.LineItem> lines = new ArrayList<>();
		final StringBuilder namesZh = new StringBuilder();
		final StringBuilder namesEn = new StringBuilder();
		for (final Long id : element.bindingIds) {
			if (id == null) {
				continue;
			}
			final long color = id;
			final String number = data.getRouteNumber(color);
			final String name = data.getRouteName(color);
			final String nameEn = data.getRouteNameEn(color);
			final String displayNumber = !isEmpty(number) ? number : name;
			if (!isEmpty(displayNumber)) {
				lines.add(new CRTSignElement.LineItem(displayNumber, (int) color | 0xFF000000));
			}
			appendWithSeparator(namesZh, isEmpty(name) ? displayNumber : name);
			appendWithSeparator(namesEn, nameEn);
		}
		if (lines.isEmpty()) {
			return;
		}
		switch (element.type) {
			case NUMBER_LINE:
				content.lines.clear();
				content.lines.addAll(lines);
				break;
			case TEXT_LINE:
				if (!isEmpty(namesZh.toString())) {
					content.text = namesZh.toString();
				}
				if (!isEmpty(namesEn.toString())) {
					content.textEn = namesEn.toString();
				}
				break;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				if (!isEmpty(namesZh.toString())) {
					content.textZh = namesZh.toString();
				}
				if (!isEmpty(namesEn.toString())) {
					content.textEn = namesEn.toString();
				}
				break;
			case BIG_NUMBER:
				if (!isEmpty(lines.get(0).number)) {
					content.text = lines.get(0).number;
				}
				break;
			case ENTRANCE:
			case EXIT:
				content.code = lines.get(0).number;
				break;
			default:
				break;
		}
	}

	private static void applyResolvedStation(CRTSignElement element, ResolvedContent content, CRTSignDataContext data) {
		final List<String> stationNames = data.getStationNames(element.bindingIds);
		if (stationNames == null || stationNames.isEmpty()) {
			return;
		}
		// 约定：第 0 项 = 中文行（多个站用 " / " 合并），可选第 1 项 = 英文行；不要再按 "|" 拆
		final String zh = stationNames.get(0) == null ? "" : stationNames.get(0);
		final String en = stationNames.size() > 1 && stationNames.get(1) != null ? stationNames.get(1) : "";
		if (isEmpty(zh) && isEmpty(en)) {
			return;
		}
		switch (element.type) {
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				if (!isEmpty(zh)) {
					content.textZh = zh;
				}
				if (!isEmpty(en)) {
					content.textEn = en;
				}
				break;
			case BIG_NUMBER:
				content.text = isEmpty(zh) ? en : zh;
				break;
			case ENTRANCE:
			case EXIT:
				content.code = isEmpty(zh) ? en : zh;
				break;
			case TEXT_LINE:
				content.text = isEmpty(zh) ? en : zh;
				if (!isEmpty(en)) {
					content.textEn = en;
				}
				break;
			default:
				content.text = isEmpty(zh) ? en : zh;
				break;
		}
	}

	private static void applyResolvedExit(CRTSignElement element, ResolvedContent content, CRTSignDataContext data, long exitId) {
		final String code = data.getExitCode(exitId);
		if (isEmpty(code)) {
			return;
		}
		switch (element.type) {
			case ENTRANCE:
			case EXIT:
				content.code = code;
				break;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				content.textZh = code;
				break;
			case BIG_NUMBER:
				content.text = code;
				break;
			case TEXT_LINE:
				content.text = code;
				break;
			default:
				content.text = code;
				break;
		}
	}

	/** 把一段「中文|英文」名称填到对应文本字段（双语类填两行，其余填单行/编号）。 */
	private static void applyBilingualName(CRTSignElement element, ResolvedContent content, @Nullable String[] bilingualName) {
		if (bilingualName == null || (isEmpty(bilingualName[0]) && isEmpty(bilingualName[1]))) {
			return;
		}
		switch (element.type) {
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				if (!isEmpty(bilingualName[0])) {
					content.textZh = bilingualName[0];
				}
				if (!isEmpty(bilingualName[1])) {
					content.textEn = bilingualName[1];
				}
				break;
			case TEXT_LINE:
				content.text = isEmpty(bilingualName[0]) ? bilingualName[1] : bilingualName[0];
				if (!isEmpty(bilingualName[1])) {
					content.textEn = bilingualName[1];
				}
				break;
			case ENTRANCE:
			case EXIT:
				content.code = isEmpty(bilingualName[0]) ? bilingualName[1] : bilingualName[0];
				break;
			case BIG_NUMBER:
			default:
				content.text = isEmpty(bilingualName[0]) ? bilingualName[1] : bilingualName[0];
				break;
		}
	}

	/** 「中文|英文」拆分；没有分隔符时整串作为中文。 */
	private static String[] splitBilingualName(@Nullable String rawName) {
		if (rawName == null || rawName.isEmpty()) {
			return new String[]{"", ""};
		}
		final String[] parts = rawName.split("\\|", -1);
		final String zh = parts[0].trim();
		final String en = parts.length > 1 ? parts[1].trim() : "";
		return new String[]{zh, en};
	}

	private static void appendWithSeparator(StringBuilder builder, @Nullable String value) {
		if (isEmpty(value)) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(" / ");
		}
		builder.append(value);
	}

	private static long contentHash(ResolvedContent content) {
		long hash = 17L;
		hash = hash * 31L + content.textZh.hashCode();
		hash = hash * 31L + content.textEn.hashCode();
		hash = hash * 31L + content.text.hashCode();
		hash = hash * 31L + content.code.hashCode();
		for (final CRTSignElement.LineItem line : content.lines) {
			hash = hash * 31L + safe(line.number).hashCode();
			hash = hash * 31L + line.color;
		}
		return hash;
	}

	// ═══════════════════════════════════════════════════════════════
	// 工具
	// ═══════════════════════════════════════════════════════════════

	/** 文本字体：常规 = Alibaba PuHuiTi；加粗 = Source Han Sans Bold（与 mod 既有用字一致）。 */
	private static FontType textFontType(boolean bold) {
		return bold ? FontType.SOURCE_HAN : FontType.ALIBABA;
	}

	/** 数字字体：纯数字/拉丁用 Source Sans 3，含 CJK 时回退中文字体（网页 FONT_NUM 栈的 MiSans 回退）。 */
	private static FontType numberFontType(@Nullable String text) {
		if (text != null && FontRenderUtils.containsCjk(text)) {
			return FontType.ALIBABA;
		}
		return FontType.SOURCE_SANS_3;
	}

	private static boolean isEmpty(@Nullable String value) {
		return value == null || value.isEmpty();
	}

	private static String safe(@Nullable String value) {
		return value == null ? "" : value;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static Map<String, String> createIconMapping() {
		final Map<String, String> mapping = new LinkedHashMap<>();
		// 服务设施（网页 icons.js cat = 'service'，对应 crt_signs.json 里的设施标志）
		mapping.put("accessible", "crt_accessible_lift");
		mapping.put("accessible_elevator_platform", "crt_accessible_lift");
		mapping.put("accessible_passage", "crt_accessible_lift");
		mapping.put("accessible_ramp", "crt_accessible_lift");
		mapping.put("accessible_restroom", "crt_restroom");
		mapping.put("check_in", "crt_check");
		mapping.put("china_railway", "crt_railway");
		mapping.put("elevator", "crt_accessible_lift");
		mapping.put("exit", "crt_exit_up");
		mapping.put("exit_left", "crt_exit_left");
		mapping.put("exit_right", "crt_exit_right");
		mapping.put("metro", "crt_railway");
		mapping.put("monorail", "crt_monorail");
		mapping.put("no_entry", "nanbin_cross");
		mapping.put("nursing", "crt_restroom");
		mapping.put("restroom", "crt_restroom");
		mapping.put("stairs", "crt_stair");
		mapping.put("tickets", "crt_ticket_sell");
		mapping.put("train", "crt_railway");
		mapping.put("waiting", "crt_waiting_room");
		// 方向图标（网页 cat = 'arrows'，对应 nanbin_arrow* 贴图；翻转方向用 *_flip 标志）
		mapping.put("arrow_up", "nanbin_arrow_up");
		mapping.put("arrow_down", "nanbin_arrow_down");
		mapping.put("arrow_left", "nanbin_arrow");
		mapping.put("arrow_right", "nanbin_arrow_flip");
		mapping.put("arrow_left_up", "nanbin_arrow_up_left");
		mapping.put("arrow_right_up", "nanbin_arrow_up_right");
		mapping.put("arrow_right_down", "nanbin_arrow_down_right");
		mapping.put("arrow_left_down", "nanbin_arrow_down_left");
		mapping.put("arrow_ahead_left", "nanbin_arrow_up_left");
		mapping.put("arrow_ahead_right", "nanbin_arrow_up_right");
		mapping.put("arrow_back_left", "nanbin_arrow_turn_back");
		mapping.put("arrow_back_right", "nanbin_arrow_turn_back_flip");
		// 常见别名（编辑器/旧存档里可能出现的写法）
		mapping.put("toilet", "crt_restroom");
		mapping.put("female_toilet", "crt_female_toilot");
		mapping.put("male_toilet", "crt_male_toilot");
		mapping.put("escalator", "crt_escalator");
		mapping.put("check", "crt_check");
		mapping.put("server", "crt_server");
		mapping.put("exit_up", "crt_exit_up");
		mapping.put("exit_down", "crt_exit_down");
		mapping.put("cross", "nanbin_cross");
		return Collections.unmodifiableMap(mapping);
	}

	/** 布局第一遍的中间数据（行元素 + 解析内容 + metrics + 宽度）。 */
	private static final class RowBuild {
		private final CRTSignLayout.Row row;
		private final int rowIndex;
		private final List<CRTSignElement> elements = new ArrayList<>();
		private final List<ResolvedContent> contents = new ArrayList<>();
		private final List<CRTSignLayoutFrame.Metrics> metrics = new ArrayList<>();
		private final List<Float> widths = new ArrayList<>();
		private float contentWidth;

		private RowBuild(CRTSignLayout.Row row, int rowIndex) {
			this.row = row;
			this.rowIndex = rowIndex;
		}
	}
}
