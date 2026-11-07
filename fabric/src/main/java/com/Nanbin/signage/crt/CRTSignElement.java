package com.Nanbin.signage.crt;

import org.mtr.libraries.com.google.gson.JsonArray;
import org.mtr.libraries.com.google.gson.JsonElement;
import org.mtr.libraries.com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * CRT 导视牌上的一个元素（对应网页 JS 的 {@code Element}）。
 *
 * <p>字段与默认值对齐开源项目 CRT-Sign-Web-Editor 的 js/state.js
 * （{@code DEFAULT_PROPS} / {@code sanitizeElement}），并在此基础上增加了
 * <b>MTR 数据绑定</b>字段（{@link #binding} / {@link #bindingIds}），
 * 让文字、线路编号、车站名、出口编号等可以直接取自 MTR 模组的数据。</p>
 *
 * <p>内部颜色统一用不透明 ARGB int；JSON 序列化时写成网页端的 {@code #RRGGBB} / {@code #AARRGGBB}。</p>
 */
public final class CRTSignElement {

	// ─── 网页端对齐的枚举 ────────────────────────────────────────────

	/** 元素在行内的对齐（固定宽度模式生效），对应网页 {@code elementAlign}。 */
	public enum ElementAlign {
		LEFT("left"), CENTER("center"), RIGHT("right");

		public final String id;

		ElementAlign(String id) {
			this.id = id;
		}

		public static ElementAlign fromId(@Nullable String id) {
			if (id != null) {
				for (final ElementAlign value : values()) {
					if (value.id.equals(id.toLowerCase(Locale.ROOT))) {
						return value;
					}
				}
			}
			return LEFT;
		}
	}

	/** 文字内容对齐，对应网页的 {@code align}（双语文本为 left/center/right，number-line/text-line/exit 只有 left/right）。 */
	public enum TextAlign {
		LEFT("left"), CENTER("center"), RIGHT("right");

		public final String id;

		TextAlign(String id) {
			this.id = id;
		}

		public static TextAlign fromId(@Nullable String id, TextAlign fallback) {
			if (id != null) {
				for (final TextAlign value : values()) {
					if (value.id.equals(id.toLowerCase(Locale.ROOT))) {
						return value;
					}
				}
			}
			return fallback;
		}
	}

	/** 箭头方向（网页为 up/down/left/right 与四个斜向）。 */
	public enum ArrowDirection {
		UP("up"), DOWN("down"), LEFT("left"), RIGHT("right"),
		LEFT_UP("left-up"), RIGHT_UP("right-up"), RIGHT_DOWN("right-down"), LEFT_DOWN("left-down");

		public final String id;

		ArrowDirection(String id) {
			this.id = id;
		}

		public static ArrowDirection fromId(@Nullable String id) {
			if (id != null) {
				for (final ArrowDirection value : values()) {
					if (value.id.equals(id.toLowerCase(Locale.ROOT))) {
						return value;
					}
				}
			}
			return LEFT;
		}
	}

	/**
	 * 元素内容与 MTR 模组数据的绑定方式（本 mod 扩展，网页端没有）。
	 * {@link #NONE} 表示使用元素自身的字面文本/颜色。
	 */
	public enum DataBinding {
		/** 字面内容（默认）。 */
		NONE("none"),
		/** 绑定一条线路：{@code bindingIds[0]} = 线路颜色，取线路名（中|英）。 */
		ROUTE_NAME("route_name"),
		/** 绑定一条线路：取线路编号（如 1、环线、S1）。 */
		ROUTE_NUMBER("route_number"),
		/** 多条线路（线路色条/数字条/多线换乘）：{@code bindingIds} 依次为线路颜色。 */
		ROUTE_LINES("route_lines"),
		/** 绑定车站：{@code bindingIds} 为车站 id，渲染时合并为多站名（换乘站样式）。 */
		STATION("station"),
		/** 绑定出口：{@code bindingIds} 为出口 id，取出口字母（如 A/B/C）。 */
		EXIT("exit"),
		/** 绑定站台：{@code bindingIds} 为站台 id，取站台名/编号。 */
		PLATFORM("platform"),
		/** 下一站：需要线路上下文，{@code bindingIds[0]} 为站台 id。 */
		NEXT_STATION("next_station"),
		/** 上一站：{@code bindingIds[0]} 为站台 id。 */
		PREV_STATION("prev_station");

		public final String id;

		DataBinding(String id) {
			this.id = id;
		}

		public static DataBinding fromId(@Nullable String id) {
			if (id != null) {
				for (final DataBinding value : values()) {
					if (value.id.equals(id.toLowerCase(Locale.ROOT))) {
						return value;
					}
				}
			}
			return NONE;
		}
	}

	// ─── 通用字段 ────────────────────────────────────────────────────

	public String id = newId();
	public CRTSignElementType type = CRTSignElementType.SPACE;
	/** 内容颜色（文字、箭头、图标线条等）。 */
	public int color = 0xFF000000;
	/** 元素自身背景色；null = 透明。 */
	@Nullable
	public Integer backgroundColor;
	/** 行内对齐（固定宽度模式）。 */
	public ElementAlign elementAlign = ElementAlign.LEFT;
	/** 相邻自动内边距开关（网页 paddingAuto）。 */
	public boolean paddingAuto = true;
	/** 内边距，单位为行高比例（网页 padding 语义）。 */
	public float paddingTop = 0.2F;
	public float paddingRight = 0.2F;
	public float paddingBottom = 0.2F;
	public float paddingLeft = 0.2F;

	// ─── 文本类 ──────────────────────────────────────────────────────

	public String textZh = "";
	public String textEn = "";
	public String text = "";
	public boolean bold = false;
	public TextAlign align = TextAlign.CENTER;

	// ─── 箭头 ────────────────────────────────────────────────────────

	public ArrowDirection arrowDirection = ArrowDirection.LEFT;
	/** 箭头粗细相对内容尺寸的比例（0.05~0.95）。 */
	public float thicknessRatio = 0.25F;

	// ─── 线路条（number-line / text-line） ───────────────────────────

	/** number-line 的多条线路（字面内容；绑定 ROUTE_LINES 时用 MTR 数据覆盖）。 */
	public final List<LineItem> lines = new ArrayList<>();
	public int textColor = 0xFF000000;
	/** text-line 的线路名是否下沉显示（与「线」组合）。 */
	public boolean nameSink = true;
	/** text-line 的贯穿色条颜色。 */
	public int blockColor = 0xFFF2A900;

	// ─── 出入口 ──────────────────────────────────────────────────────

	public String code = "1";

	// ─── 空白 ────────────────────────────────────────────────────────

	/** 空白占位宽度 = 行高 × 该比例。 */
	public float widthRatio = 0.5F;

	// ─── 图标 ────────────────────────────────────────────────────────

	/** 服务设施图标 id（crtsign:... 由渲染端映射到贴图），网页端图标清单见 js/icons.js。 */
	public String icon = "elevator";
	/** 图标旋转角度：-90 / 0 / 90 / 180。 */
	public int rotation = 0;

	// ─── MTR 数据绑定（本 mod 扩展） ──────────────────────────────────

	public DataBinding binding = DataBinding.NONE;
	/** 绑定目标 id：线路颜色（有符号 int 扩展为 long）、车站 id、出口 id、站台 id 等。 */
	public final List<Long> bindingIds = new ArrayList<>();

	// ─── 构造与工厂 ──────────────────────────────────────────────────

	public CRTSignElement() {
	}

	public CRTSignElement(CRTSignElementType type) {
		this.type = type;
		applyDefaults(type);
	}

	private static String newId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/** 按网页 DEFAULT_PROPS 填充默认值（保留 id/type）。 */
	public void applyDefaults(CRTSignElementType type) {
		this.type = type;
		color = 0xFF000000;
		backgroundColor = null;
		elementAlign = ElementAlign.LEFT;
		paddingAuto = type != CRTSignElementType.SPACE;
		paddingTop = paddingRight = paddingBottom = paddingLeft = type == CRTSignElementType.SPACE ? 0.0F : 0.2F;
		textZh = "";
		textEn = "";
		text = "";
		bold = false;
		align = TextAlign.CENTER;
		arrowDirection = ArrowDirection.LEFT;
		thicknessRatio = 0.25F;
		lines.clear();
		textColor = 0xFF000000;
		nameSink = true;
		blockColor = 0xFFF2A900;
		code = "1";
		widthRatio = 0.5F;
		icon = "elevator";
		rotation = 0;
		binding = DataBinding.NONE;
		bindingIds.clear();
		switch (type) {
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				textZh = "站名";
				textEn = "Station";
				break;
			case BIG_NUMBER:
				text = "1";
				break;
			case NUMBER_LINE:
				lines.add(new LineItem("1", 0xFFE4002B));
				align = TextAlign.LEFT;
				break;
			case TEXT_LINE:
				text = "环";
				textEn = "Loop Line";
				align = TextAlign.LEFT;
				break;
			case ENTRANCE:
				code = "1";
				break;
			case EXIT:
				code = "1";
				align = TextAlign.LEFT;
				// 出口底色固定为网页 EXIT_COLOR（#F7D917），不允许改色
				backgroundColor = 0xFFF7D917;
				break;
			default:
				break;
		}
	}

	/** 复制一份（深拷贝，剪贴板/撤销用）。 */
	public CRTSignElement copy() {
		final CRTSignElement copy = fromJson(toJson());
		copy.id = newId();
		return copy;
	}

	// ─── JSON ────────────────────────────────────────────────────────

	public JsonObject toJson() {
		final JsonObject json = new JsonObject();
		json.addProperty("id", id == null || id.isEmpty() ? newId() : id);
		json.addProperty("type", type.id);
		final JsonObject props = new JsonObject();
		json.add("props", props);

		props.addProperty("color", toHex(color));
		if (backgroundColor != null) {
			props.addProperty("backgroundColor", toHex(backgroundColor));
		}
		props.addProperty("elementAlign", elementAlign.id);
		props.addProperty("paddingAuto", paddingAuto);
		final JsonObject padding = new JsonObject();
		padding.addProperty("top", round4(paddingTop));
		padding.addProperty("right", round4(paddingRight));
		padding.addProperty("bottom", round4(paddingBottom));
		padding.addProperty("left", round4(paddingLeft));
		props.add("padding", padding);

		switch (type) {
			case ARROW:
				props.addProperty("direction", arrowDirection.id);
				props.addProperty("thicknessRatio", round4(thicknessRatio));
				break;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				props.addProperty("textZh", textZh);
				props.addProperty("textEn", textEn);
				props.addProperty("bold", bold);
				props.addProperty("align", align.id);
				break;
			case BIG_NUMBER:
				props.addProperty("text", text);
				break;
			case NUMBER_LINE:
				props.addProperty("textColor", toHex(textColor));
				props.addProperty("align", align == TextAlign.RIGHT ? "right" : "left");
				final JsonArray linesJson = new JsonArray();
				for (final LineItem item : lines) {
					linesJson.add(item.toJson());
				}
				props.add("lines", linesJson);
				break;
			case TEXT_LINE:
				props.addProperty("text", text);
				props.addProperty("textEn", textEn);
				props.addProperty("nameSink", nameSink);
				props.addProperty("blockColor", toHex(blockColor));
				props.addProperty("textColor", toHex(textColor));
				props.addProperty("align", align == TextAlign.RIGHT ? "right" : "left");
				break;
			case ENTRANCE:
			case EXIT:
				props.addProperty("code", code);
				if (type == CRTSignElementType.EXIT) {
					props.addProperty("align", align == TextAlign.RIGHT ? "right" : "left");
				}
				break;
			case SPACE:
				props.addProperty("widthRatio", round4(widthRatio));
				break;
			case ICON:
				props.addProperty("icon", icon);
				props.addProperty("rotation", rotation);
				break;
			default:
				break;
		}

		// MTR 绑定（网页端会忽略这些额外字段，导出到网页项目仍可打开）
		if (binding != DataBinding.NONE) {
			props.addProperty("mtrBinding", binding.id);
			final JsonArray idsJson = new JsonArray();
			for (final long value : bindingIds) {
				idsJson.add(value);
			}
			props.add("mtrBindingIds", idsJson);
		}
		return json;
	}

	/** 解析并修补单个元素；未知类型返回 null（与网页 sanitizeElement 一致）。 */
	@Nullable
	public static CRTSignElement fromJson(@Nullable JsonElement element) {
		if (element == null || !element.isJsonObject()) {
			return null;
		}
		final JsonObject json = element.getAsJsonObject();
		final CRTSignElementType type = CRTSignElementType.fromId(getString(json, "type", ""));
		if (type == null) {
			return null;
		}
		final CRTSignElement result = new CRTSignElement(type);
		final String savedId = getString(json, "id", "");
		if (!savedId.isEmpty()) {
			result.id = savedId;
		}
		final JsonObject props = json.has("props") && json.get("props").isJsonObject() ? json.getAsJsonObject("props") : new JsonObject();

		result.color = parseColor(getString(props, "color", "#000000"), 0xFF000000);
		if (props.has("backgroundColor") && !props.get("backgroundColor").isJsonNull()) {
			result.backgroundColor = parseColor(getString(props, "backgroundColor", ""), null);
		}
		result.elementAlign = ElementAlign.fromId(getString(props, "elementAlign", "left"));
		if (props.has("paddingAuto")) {
			result.paddingAuto = props.get("paddingAuto").getAsBoolean();
		}
		if (props.has("padding") && props.get("padding").isJsonObject()) {
			final JsonObject padding = props.getAsJsonObject("padding");
			result.paddingTop = clamp(getFloat(padding, "top", result.paddingTop), 0.0F, 1.0F);
			result.paddingRight = clamp(getFloat(padding, "right", result.paddingRight), 0.0F, 8.0F);
			result.paddingBottom = clamp(getFloat(padding, "bottom", result.paddingBottom), 0.0F, 1.0F);
			result.paddingLeft = clamp(getFloat(padding, "left", result.paddingLeft), 0.0F, 8.0F);
		}

		switch (type) {
			case ARROW:
				result.arrowDirection = ArrowDirection.fromId(getString(props, "direction", "left"));
				result.thicknessRatio = clamp(getFloat(props, "thicknessRatio", 0.25F), 0.05F, 0.95F);
				break;
			case BILINGUAL_TEXT:
			case SMALL_BILINGUAL_TEXT:
				result.textZh = getString(props, "textZh", "站名");
				result.textEn = getString(props, "textEn", "Station");
				result.bold = props.has("bold") && props.get("bold").getAsBoolean();
				result.align = TextAlign.fromId(getString(props, "align", "center"), TextAlign.CENTER);
				break;
			case BIG_NUMBER:
				result.text = getString(props, "text", "1");
				break;
			case NUMBER_LINE:
				result.textColor = parseColor(getString(props, "textColor", "#000000"), 0xFF000000);
				result.align = TextAlign.fromId(getString(props, "align", "left"), TextAlign.LEFT);
				if (result.align == TextAlign.CENTER) {
					result.align = TextAlign.LEFT;
				}
				result.lines.clear();
				if (props.has("lines") && props.get("lines").isJsonArray()) {
					for (final JsonElement item : props.getAsJsonArray("lines")) {
						final LineItem line = LineItem.fromJson(item);
						if (line != null) {
							result.lines.add(line);
						}
					}
				}
				if (result.lines.isEmpty() && result.binding == DataBinding.NONE) {
					result.lines.add(new LineItem("1", 0xFFE4002B));
				}
				break;
			case TEXT_LINE:
				result.text = getString(props, "text", "环");
				result.textEn = getString(props, "textEn", "Loop Line");
				result.nameSink = !props.has("nameSink") || props.get("nameSink").getAsBoolean();
				result.blockColor = parseColor(getString(props, "blockColor", "#F2A900"), 0xFFF2A900);
				result.textColor = parseColor(getString(props, "textColor", "#000000"), 0xFF000000);
				result.align = TextAlign.fromId(getString(props, "align", "left"), TextAlign.LEFT);
				if (result.align == TextAlign.CENTER) {
					result.align = TextAlign.LEFT;
				}
				break;
			case ENTRANCE:
				result.code = getString(props, "code", "1");
				break;
			case EXIT:
				result.code = getString(props, "code", "1");
				result.align = TextAlign.fromId(getString(props, "align", "left"), TextAlign.LEFT);
				// 出口底色固定出口黄（网页 sanitizeElement 强制）
				result.backgroundColor = 0xFFF7D917;
				break;
			case SPACE:
				result.widthRatio = clamp(getFloat(props, "widthRatio", 0.5F), 0.0F, 8.0F);
				result.paddingAuto = false;
				break;
			case ICON:
				result.icon = getString(props, "icon", "elevator");
				final int rotation = getInt(props, "rotation", 0);
				result.rotation = rotation == -90 || rotation == 0 || rotation == 90 || rotation == 180 ? rotation : 0;
				break;
			default:
				break;
		}

		result.binding = DataBinding.fromId(getString(props, "mtrBinding", "none"));
		if (props.has("mtrBindingIds") && props.get("mtrBindingIds").isJsonArray()) {
			for (final JsonElement idElement : props.getAsJsonArray("mtrBindingIds")) {
				try {
					result.bindingIds.add(idElement.getAsLong());
				} catch (Exception ignored) {
				}
			}
		}
		return result;
	}

	// ─── 工具 ────────────────────────────────────────────────────────

	/** 是否支持内容对齐（网页 CONTENT_ALIGN_TYPES）。 */
	public boolean supportsContentAlign() {
		return type == CRTSignElementType.NUMBER_LINE || type == CRTSignElementType.TEXT_LINE || type == CRTSignElementType.EXIT;
	}

	/** 是否使用字面文本（用于属性面板决定是否显示文本输入框）。 */
	public boolean hasEditableText() {
		return type == CRTSignElementType.BILINGUAL_TEXT || type == CRTSignElementType.SMALL_BILINGUAL_TEXT
				|| type == CRTSignElementType.BIG_NUMBER || type == CRTSignElementType.TEXT_LINE;
	}

	/** number-line 的一条线路（颜色 + 编号）。 */
	public static final class LineItem {
		public String number;
		public int color;

		public LineItem(String number, int color) {
			this.number = number;
			this.color = color;
		}

		public JsonObject toJson() {
			final JsonObject json = new JsonObject();
			json.addProperty("number", number == null ? "" : number);
			json.addProperty("color", toHex(color));
			return json;
		}

		@Nullable
		public static LineItem fromJson(@Nullable JsonElement element) {
			if (element == null || !element.isJsonObject()) {
				return null;
			}
			final JsonObject json = element.getAsJsonObject();
			return new LineItem(getString(json, "number", ""), parseColor(getString(json, "color", "#424A52"), 0xFF424A52));
		}
	}

	// ─── 颜色/数值工具 ───────────────────────────────────────────────

	/** 解析 #RRGGBB / #AARRGGBB（# 可省）为不透明 ARGB int；失败返回 fallback。 */
	public static int parseColor(@Nullable String value, int fallback) {
		final Integer parsed = parseColorNullable(value);
		return parsed == null ? fallback : parsed;
	}

	/** 同上，但失败返回 null（用于可为空的背景色）。 */
	@Nullable
	public static Integer parseColor(@Nullable String value, @Nullable Integer fallback) {
		final Integer parsed = parseColorNullable(value);
		return parsed == null ? fallback : parsed;
	}

	@Nullable
	private static Integer parseColorNullable(@Nullable String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		try {
			String hex = value.startsWith("#") ? value.substring(1) : value;
			if (hex.length() == 6) {
				return 0xFF000000 | (int) Long.parseLong(hex, 16);
			} else if (hex.length() == 8) {
				return (int) Long.parseLong(hex, 16);
			}
		} catch (NumberFormatException ignored) {
		}
		return null;
	}

	/** ARGB int -> 网页端颜色字符串（不透明时用 #RRGGBB，半透明时用 #AARRGGBB）。 */
	public static String toHex(int argb) {
		final int alpha = (argb >>> 24) & 0xFF;
		return alpha == 0xFF
				? String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF)
				: String.format(Locale.ROOT, "#%08X", argb);
	}

	private static float round4(float value) {
		return Math.round(value * 10000.0F) / 10000.0F;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	static String getString(JsonObject json, String key, String fallback) {
		try {
			return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	static int getInt(JsonObject json, String key, int fallback) {
		try {
			return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : fallback;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	static float getFloat(JsonObject json, String key, float fallback) {
		try {
			return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsFloat() : fallback;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	static boolean getBoolean(JsonObject json, String key, boolean fallback) {
		try {
			return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsBoolean() : fallback;
		} catch (Exception ignored) {
			return fallback;
		}
	}
}
