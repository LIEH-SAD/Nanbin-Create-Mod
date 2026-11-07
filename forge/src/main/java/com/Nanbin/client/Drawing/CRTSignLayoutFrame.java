package com.Nanbin.client.Drawing;

import com.Nanbin.client.Drawing.CustomFontTextureCache.FontType;
import com.Nanbin.signage.crt.CRTSignElement;
import com.Nanbin.signage.crt.CRTSignLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一帧「CRT 导视牌自由布局」的布局结果（设计像素坐标，原点在牌面左上角）。
 *
 * <p>对应网页 {@code render.js} 的 {@code layoutSign()/layoutRowElements()} 返回值：
 * 牌面宽高 + 每行 y/高度/内容宽 + 每个元素的 x/宽度。布局与绘制分离，编辑界面的命中测试
 * 与选中框也基于这份数据（见 {@link CRTSignLayoutPreview}）。</p>
 *
 * <p>元素上的 {@link ElementFrame#metrics} 是各元素类型的绘制坐标（色条位置、基线、逐字
 * 墨区坐标、箭头多边形等），由 {@link CRTSignLayoutPainter#layout} 一次算好，
 * 绘制时不再重复测量（与网页「同一元素只测量一次」的约定一致）。</p>
 */
public final class CRTSignLayoutFrame {

	/** 牌面宽（设计像素）。 */
	public final float width;
	/** 牌面高（设计像素）。 */
	public final float height;
	/** 行（自上而下）。 */
	public final List<RowFrame> rows = new ArrayList<>();
	/**
	 * 内容指纹：布局字段 + 绑定数据解析后的实际文本/线路内容。
	 * 预览/世界渲染用它判断是否需要重烘焙贴图（{@link CRTSignLayoutPainter#bake} 使用）。
	 */
	public long contentFingerprint;

	/** 牌面数据源（布局设置：背景色 / 灰框等；可为 null 表示仅用于查询的临时帧）。 */
	@Nullable
	public CRTSignLayout layout;
	/** 生成这一帧时的布局 JSON 哈希（世界渲染的帧缓存用，避免每帧重复 layout/toJson）。 */
	public int layoutJsonHash;
	/** 牌面背景色（ARGB）。 */
	public int backgroundColor = 0xFFFFFFFF;
	/** 灰框宽度（px，0 = 不显示）。 */
	public int frameWidth;
	/** 灰框颜色（ARGB）。 */
	public int frameColor = 0xFF9AA3AD;

	public CRTSignLayoutFrame(float width, float height) {
		this.width = width;
		this.height = height;
	}

	/** 一块牌的一行。 */
	public static final class RowFrame {
		/** 数据层行对象（用于读取行背景色等）。 */
		public final CRTSignLayout.Row row;
		public final int rowIndex;
		/** 行顶 y（设计像素）。 */
		public final float y;
		/** 行高（设计像素，全牌统一）。 */
		public final float height;
		/** 本行元素宽度之和（固定宽度模式下仅作信息，不裁剪）。 */
		public final float contentWidth;
		public final List<ElementFrame> elements = new ArrayList<>();

		public RowFrame(CRTSignLayout.Row row, int rowIndex, float y, float height, float contentWidth) {
			this.row = row;
			this.rowIndex = rowIndex;
			this.y = y;
			this.height = height;
			this.contentWidth = contentWidth;
		}
	}

	/** 一行里的一个元素。 */
	public static final class ElementFrame {
		public final CRTSignElement element;
		public final String rowId;
		public final int rowIndex;
		/** 元素在行 elements 数组里的下标（排版与拖拽的权威顺序）。 */
		public final int index;
		/** 元素盒左缘 x（设计像素，浮点；绘制时按网页约定 floor）。 */
		public final float x;
		/** 元素盒顶 y（= 行 y，设计像素）。 */
		public final float y;
		/** 元素宽度（设计像素，浮点；盒背景/命中框按 ceil 取整）。 */
		public final float width;
		/** 元素盒高（= 行高）。 */
		public final float height;
		/** 该元素的绘制坐标（各类型专属，见 {@link Metrics}）。 */
		public final Metrics metrics;

		public ElementFrame(CRTSignElement element, String rowId, int rowIndex, int index, float x, float y, float width, float height, Metrics metrics) {
			this.element = element;
			this.rowId = rowId;
			this.rowIndex = rowIndex;
			this.index = index;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.metrics = metrics;
		}

		/** 盒背景/命中框宽度：{@code ceil(max(width, 1))}（网页 render.js:820-825）。 */
		public float boxWidth() {
			return (float) Math.ceil(Math.max(width, 1.0F));
		}

		/** 绘制用左缘：{@code floor(x)}（消除相邻元素背景间的亚像素透明缝）。 */
		public float drawX() {
			return (float) Math.floor(x);
		}
	}

	// ─── 查询（编辑界面命中测试用） ──────────────────────────────────

	/** 按元素 id 查布局结果；不存在返回 null。 */
	@Nullable
	public ElementFrame findElement(@Nullable String elementId) {
		if (elementId == null) {
			return null;
		}
		for (final RowFrame row : rows) {
			for (final ElementFrame element : row.elements) {
				if (elementId.equals(element.element.id)) {
					return element;
				}
			}
		}
		return null;
	}

	/**
	 * 命中测试：返回覆盖 (px, py) 的元素；重叠时返回最后绘制（视觉最上层）的那个。
	 * 命中框 = 元素盒（宽 {@link ElementFrame#boxWidth()}，高 = 行高）。
	 */
	@Nullable
	public ElementFrame hitTest(float px, float py) {
		for (int rowIndex = rows.size() - 1; rowIndex >= 0; rowIndex--) {
			final RowFrame row = rows.get(rowIndex);
			if (py < row.y || py >= row.y + row.height) {
				continue;
			}
			for (int elementIndex = row.elements.size() - 1; elementIndex >= 0; elementIndex--) {
				final ElementFrame element = row.elements.get(elementIndex);
				if (px >= element.x && px < element.x + element.boxWidth()) {
					return element;
				}
			}
		}
		return null;
	}

	/** (px, py) 所在行号；在牌面外返回 -1。 */
	public int rowAt(float py) {
		if (rows.isEmpty() || py < 0.0F || py >= height) {
			return -1;
		}
		final int rowIndex = (int) Math.floor(py / Math.max(1.0F, rows.get(0).height));
		return rowIndex < 0 || rowIndex >= rows.size() ? -1 : rowIndex;
	}

	// ─── 各元素类型的绘制坐标（与网页 metrics 对象一一对应） ────────

	/**
	 * 元素绘制坐标。不同类型的字段使用情况不同（与网页各 *Metrics 对象的字段一一对应）：
	 * <ul>
	 *   <li>所有类型：{@code padTop/padLeft/padRight/padBottom/contentSize}</li>
	 *   <li>arrow：{@link #polygons}</li>
	 *   <li>bilingual / small-bilingual：{@link #label}（zhBase/enBase 相对内容盒顶部）</li>
	 *   <li>big-number：{@link #glyphs} + {@link #fontSize}/{@link #fontType}/{@link #base}；
	 *       x 为「text-anchor = middle」的绘制点</li>
	 *   <li>number-line：{@link #numberEntries} + {@link #label}/{@link #labelX}/{@link #labelAlignRight}</li>
	 *   <li>text-line：{@link #stripeX}/{@link #stripeW}/{@link #stripeH}、
	 *       sink 模式的 {@link #glyphs}/{@link #textX}/{@link #base}，以及 {@link #label}/{@link #labelX}</li>
	 *   <li>entrance / exit：{@link #codeSize}/{@link #codeBase}/{@link #codeX}/{@link #codeCenterX}/{@link #glyphs} + label</li>
	 *   <li>icon：{@link #iconRotation}</li>
	 * </ul>
	 */
	public static final class Metrics {
		// ── 通用盒模型 ──
		public float padTop;
		public float padLeft;
		public float padRight;
		public float padBottom;
		/** 内容尺寸 s = rowHeight − padTop − padBottom。 */
		public float contentSize;
		/** 元素宽度（已含左右 padding）。 */
		public float width;

		// ── 绑定数据解析后的实际文本（绘制用，取不到数据时即字面文本） ──
		public String textZh = "";
		public String textEn = "";
		public String text = "";
		public String code = "";
		public final List<CRTSignElement.LineItem> lines = new ArrayList<>();

		// ── 文本样式 ──
		/** 组合标签/文本使用的字体（regular = Alibaba PuHuiTi，bold = Source Han Sans Bold，数字 = Source Sans 3）。 */
		public FontType fontType = FontType.ALIBABA;
		/** 大文本/编号字号（设计像素）。 */
		public float fontSize;
		/** 文本基线（相对元素盒顶部，设计像素）。 */
		public float base;
		/** 文本前进宽（大文本/编号用于布局宽度）。 */
		public float textW;
		/** 双语标签度量（bilingual / small / 组合标签共用）。 */
		public BilingualLabel label;

		// ── arrow ──
		/** 箭头三块多边形：每个 float[] 为 {x0,y0,x1,y1,...}，坐标相对内容盒左上角。 */
		public final List<float[]> polygons = new ArrayList<>();

		// ── 逐字墨区排版（big-number / 编号 / 文本线路大字） ──
		/** 逐字绘制点（text-anchor = middle）。 */
		public final List<Glyph> glyphs = new ArrayList<>();

		// ── number-line ──
		public final List<NumberEntry> numberEntries = new ArrayList<>();
		public float stripeW;
		public float stripeH;
		public float labelX;
		public boolean labelAlignRight;

		// ── text-line ──
		public boolean sink;
		public float stripeX;
		public float textX;
		public float gap;

		// ── entrance / exit ──
		public float codeSize;
		public float codeW;
		public float codeBase;
		public float codeX;
		public float codeCenterX;

		// ── icon ──
		public int iconRotation;
	}

	/** 组合双语标签的度量（网页 bilingualMetrics 的返回值）。 */
	public static final class BilingualLabel {
		public final float zhSize;
		public final float enSize;
		/** 中文前进宽（ceil）。 */
		public final float zhW;
		/** 英文前进宽（ceil）。 */
		public final float enW;
		/** 纯文本区宽度 = max(zhW, enW)（不含 padding）。 */
		public final float width;
		public float zhBase;
		public float enBase;
		public final float zhAscent;
		/** 中文墨区下伸（垂直居中/标签排版用）。 */
		public final float zhDescent;
		/** 标签字体（bold 时 = SOURCE_HAN）。 */
		public final FontType fontType;

		public BilingualLabel(float zhSize, float enSize, float zhW, float enW, float width, float zhBase, float enBase, float zhAscent, float zhDescent, FontType fontType) {
			this.zhSize = zhSize;
			this.enSize = enSize;
			this.zhW = zhW;
			this.enW = enW;
			this.width = width;
			this.zhBase = zhBase;
			this.enBase = enBase;
			this.zhAscent = zhAscent;
			this.zhDescent = zhDescent;
			this.fontType = fontType;
		}
	}

	/** 逐字绘制点：{@code x} 是 text-anchor = middle 的绘制坐标（相对文本盒左缘）。 */
	public static final class Glyph {
		public final String ch;
		public final float x;

		public Glyph(String ch, float x) {
			this.ch = ch;
			this.x = x;
		}
	}

	/** 数字线路里的一条线路（色条 + 数字 + 基线与绘制点）。 */
	public static final class NumberEntry {
		public final String number;
		public final int color;
		/** 数字组 advance（网页 inkTextLayout 的 textW）。 */
		public final float advance;
		/** 基准墨边距 M₃（首条线路用于「标签 ↔ 色块」间距）。 */
		public final float margin;
		/** 数字基线（相对元素盒顶部）。 */
		public final float base;
		/** 数字组的绘制起点（逐字 x 相对它）。 */
		public final float digitsX;
		/** 色条左缘（相对元素盒左缘）。 */
		public final float stripeX;
		/** 数字组字体（纯数字 = Source Sans 3；含 CJK 时回退中文字体）。 */
		public final FontType fontType;
		public final List<Glyph> digits;

		public NumberEntry(String number, int color, float advance, float margin, float base, float digitsX, float stripeX, List<Glyph> digits, FontType fontType) {
			this.number = number;
			this.color = color;
			this.advance = advance;
			this.margin = margin;
			this.base = base;
			this.digitsX = digitsX;
			this.stripeX = stripeX;
			this.digits = digits;
			this.fontType = fontType;
		}
	}
}
