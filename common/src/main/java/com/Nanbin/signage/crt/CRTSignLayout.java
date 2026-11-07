package com.Nanbin.signage.crt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 一块 CRT 导视牌的"自由布局"数据（对应网页 JS 的 {@code SignState}）。
 *
 * <p>数据来源：CRT-Sign-Web-Editor（Apache License 2.0，原作者 endcreeper861 的
 * JR-Guidance-Sign-Web-Editor，PrisamaX0124 修改版；见 docs/THIRD_PARTY_NOTICES.md）。
 * 本类是 Java 端数据模型，字段与网页项目一致，并额外支持每个元素绑定 MTR 数据。</p>
 *
 * <p>尺寸单位沿用网页的"像素"（px）：每 1 个格 {@link #GRID_SIZE} = 256px，
 * 行高默认 {@link #DEFAULT_ROW_HEIGHT} = 256px；渲染到世界时按牌面实际大小等比缩放。</p>
 */
public final class CRTSignLayout {

	/** 网页端一格 = 256px。 */
	public static final int GRID_SIZE = 256;
	/** 默认行高（px，与网页 Core.DEFAULT_ROW_HEIGHT 一致：0.75 格）。 */
	public static final int DEFAULT_ROW_HEIGHT = 192;
	/** 默认牌宽（px） = 8 格。 */
	public static final int DEFAULT_WIDTH = GRID_SIZE * 8;
	public static final int MIN_ROW_HEIGHT = 32;
	public static final int MAX_ROW_HEIGHT = 2048;
	public static final int MIN_WIDTH = 32;
	public static final int MAX_WIDTH = 32768;
	/** 当前数据版本（网页 SCHEMA_VERSION = 1）。 */
	public static final int SCHEMA_VERSION = 1;

	public static final int DEFAULT_BACKGROUND = 0xFFFFFFFF;
	/** 新牌默认灰框色（网页 SIGN_FRAME_COLOR #4A4A4A）。 */
	public static final int DEFAULT_FRAME_COLOR = 0xFF4A4A4A;
	/** 旧版/非法灰框色的回退值（网页 LEGACY_SIGN_FRAME_COLOR #808080）。 */
	public static final int LEGACY_FRAME_COLOR = 0xFF808080;
	/** 相邻自动内边距：无邻居 / 有邻居（网页 state.js:530-531）。 */
	public static final float PADDING_AUTO_DEFAULT = 0.2F;
	public static final float PADDING_AUTO_ADJACENT = 0.1F;

	/** 牌宽模式：固定（按格数）或动态（按内容自适应，渲染端按内容宽度换算）。 */
	public enum WidthMode {
		FIXED("fixed"), DYNAMIC("dynamic");

		public final String id;

		WidthMode(String id) {
			this.id = id;
		}

		public static WidthMode fromId(@Nullable String id) {
			return id != null && "dynamic".equalsIgnoreCase(id) ? DYNAMIC : FIXED;
		}
	}

	// ─── 整牌设置 ────────────────────────────────────────────────────

	public WidthMode widthMode = WidthMode.FIXED;
	/** 牌宽（px）；固定模式为 256 × 格数。 */
	public int width = DEFAULT_WIDTH;
	public int rowHeight = DEFAULT_ROW_HEIGHT;
	public boolean aspectLocked = false;
	public int backgroundColor = DEFAULT_BACKGROUND;
	/** 灰框宽度（px），0 = 不显示。 */
	public int frameWidth = 0;
	public int frameColor = DEFAULT_FRAME_COLOR;
	public final List<Row> rows = new ArrayList<>();

	// ─── JS 整牌样式（旧格子编辑器的 JS 支持移植） ─────────────────

	/** JS 样式脚本 id（crt_js_signs_config.json 里的 id）；null = 使用自由布局。 */
	@Nullable
	public String jsStyleId;
	/** JS 样式使用的 MTR 数据选择（线路颜色 / 车站 / 出口 id 等，等价旧 signIds 的 selectedIds）。 */
	public final List<Long> jsSelectedIds = new ArrayList<>();

	// ─── 构造 ────────────────────────────────────────────────────────

	public CRTSignLayout() {
		rows.add(new Row());
	}

	/** 按方块格数创建空白布局：每格算一个设计格（256px）。
	 *  行高取 256px（= 1 格），让布局长宽比与方块牌面的实际长宽比一致，渲染时不产生上下留白。 */
	public static CRTSignLayout createBlank(int cellCount) {
		final CRTSignLayout layout = new CRTSignLayout();
		layout.width = Math.max(MIN_WIDTH, cellCount * GRID_SIZE);
		layout.rowHeight = GRID_SIZE;
		return layout;
	}

	public boolean isBlank() {
		for (final Row row : rows) {
			if (!row.elements.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public int elementCount() {
		int count = 0;
		for (final Row row : rows) {
			count += row.elements.size();
		}
		return count;
	}

	/** 是否启用了整牌 JS 样式（旧格子编辑器的 JS 支持）。 */
	public boolean hasJsStyle() {
		return jsStyleId != null && !jsStyleId.isEmpty();
	}

	/** 收集所有元素绑定里出现过的 MTR id（JS 数据屏幕的初始选择：没有 jsSelectedIds 时使用）。 */
	public List<Long> collectBindingIds() {
		final List<Long> ids = new ArrayList<>();
		for (final Row row : rows) {
			for (final CRTSignElement element : row.elements) {
				for (final Long id : element.bindingIds) {
					if (!ids.contains(id)) {
						ids.add(id);
					}
				}
			}
		}
		return ids;
	}

	/** 布局总高（px）。 */
	public int layoutHeight() {
		return Math.max(1, rows.size() * rowHeight);
	}

	/** 布局总宽（px）；动态模式暂时按固定 width 处理，渲染端可再按内容收缩。 */
	public int layoutWidth() {
		return Math.max(1, width);
	}

	// ─── 行/元素编辑操作 ─────────────────────────────────────────────

	/** 在 index 处插入新空行（缺省追加到末尾）。 */
	public void addRow(int index) {
		final int target = index < 0 || index > rows.size() ? rows.size() : index;
		rows.add(target, new Row());
	}

	/** 删除行；至少保留一行。 */
	public boolean deleteRow(String rowId) {
		if (rows.size() <= 1) {
			return false;
		}
		return rows.removeIf(row -> row.id.equals(rowId));
	}

	/** 移动行顺序。 */
	public boolean moveRow(int fromIndex, int toIndex) {
		if (fromIndex < 0 || fromIndex >= rows.size() || toIndex < 0 || toIndex >= rows.size() || fromIndex == toIndex) {
			return false;
		}
		final Row row = rows.remove(fromIndex);
		rows.add(toIndex, row);
		return true;
	}

	/** 按 id 找行下标；不存在返回 -1。 */
	public int indexOfRow(String rowId) {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).id.equals(rowId)) {
				return i;
			}
		}
		return -1;
	}

	/** 在全牌范围内查找元素。 */
	@Nullable
	public ElementRef findElement(String elementId) {
		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
			final Row row = rows.get(rowIndex);
			for (int index = 0; index < row.elements.size(); index++) {
				if (row.elements.get(index).id.equals(elementId)) {
					return new ElementRef(row, rowIndex, index, row.elements.get(index));
				}
			}
		}
		return null;
	}

	/** 追加元素到指定行（rowId 为空时用第一行）。 */
	public void addElement(@Nullable String rowId, CRTSignElement element) {
		final int rowIndex = rowId == null ? 0 : Math.max(0, indexOfRow(rowId));
		if (rowIndex < 0 || rowIndex >= rows.size()) {
			return;
		}
		final Row row = rows.get(rowIndex);
		row.elements.add(element);
	}

	/** 删除元素（全牌搜索），返回是否删除成功。 */
	public boolean deleteElement(String elementId) {
		for (final Row row : rows) {
			if (row.elements.removeIf(element -> element.id.equals(elementId))) {
				return true;
			}
		}
		return false;
	}

	/** 移动元素到目标行、目标下标（自身移动场景按网页语义修正下标）。 */
	public boolean moveElement(String elementId, String targetRowId, int gapIndex) {
		final ElementRef found = findElement(elementId);
		if (found == null) {
			return false;
		}
		final int targetRowIndex = indexOfRow(targetRowId);
		if (targetRowIndex < 0) {
			return false;
		}
		found.row.elements.remove(found.index);
		final Row targetRow = rows.get(targetRowIndex);
		int index = Math.max(0, Math.min(targetRow.elements.size(), gapIndex));
		if (found.row == targetRow && found.index < gapIndex) {
			index = Math.max(0, Math.min(targetRow.elements.size(), gapIndex - 1));
		}
		targetRow.elements.add(index, found.element);
		return true;
	}

	/** 清空所有元素，只留一行空白（保留整牌设置）。 */
	public void clearElements() {
		rows.clear();
		rows.add(new Row());
	}

	/**
	 * 重新计算"相邻自动内边距"（网页 state.js applyPaddingAuto 的移植）。
	 *
	 * <p>开启 {@code paddingAuto} 的元素：同行同通道紧邻其它元素的一侧内边距折为
	 * {@link #PADDING_AUTO_ADJACENT}，空着的一侧回到 {@link #PADDING_AUTO_DEFAULT}；
	 * 空白占位不算邻居；贴右通道的数组序与视觉序相反，按视觉方向映射左右。
	 * 手动改过内边距（paddingAuto=false）的元素保持不变。幂等，可在渲染/保存前重复调用。</p>
	 */
	public void applyPaddingAuto() {
		final boolean fixed = widthMode != WidthMode.DYNAMIC;
		for (final Row row : rows) {
			final List<CRTSignElement> elements = row.elements;
			for (int i = 0; i < elements.size(); i++) {
				final CRTSignElement element = elements.get(i);
				if (element.type == CRTSignElementType.SPACE || !element.paddingAuto) {
					continue;
				}
				final CRTSignElement.ElementAlign lane = fixed ? element.elementAlign : CRTSignElement.ElementAlign.LEFT;
				final CRTSignElement previous = i > 0 ? elements.get(i - 1) : null;
				final CRTSignElement next = i + 1 < elements.size() ? elements.get(i + 1) : null;
				final CRTSignElement lanePrevious = previous != null && previous.type != CRTSignElementType.SPACE
						&& (!fixed || previous.elementAlign == lane) ? previous : null;
				final CRTSignElement laneNext = next != null && next.type != CRTSignElementType.SPACE
						&& (!fixed || next.elementAlign == lane) ? next : null;
				final boolean hasLeft = lane == CRTSignElement.ElementAlign.RIGHT ? laneNext != null : lanePrevious != null;
				final boolean hasRight = lane == CRTSignElement.ElementAlign.RIGHT ? lanePrevious != null : laneNext != null;
				element.paddingLeft = hasLeft ? PADDING_AUTO_ADJACENT : PADDING_AUTO_DEFAULT;
				element.paddingRight = hasRight ? PADDING_AUTO_ADJACENT : PADDING_AUTO_DEFAULT;
			}
		}
	}

	// ─── JSON ────────────────────────────────────────────────────────

	/** 序列化为存档用的紧凑 JSON 字符串。 */
	public String toJson() {
		final JsonObject root = new JsonObject();
		root.addProperty("version", SCHEMA_VERSION);
		final JsonObject sign = new JsonObject();
		root.add("sign", sign);
		sign.addProperty("widthMode", widthMode.id);
		sign.addProperty("width", width);
		sign.addProperty("rowHeight", rowHeight);
		sign.addProperty("aspectLocked", aspectLocked);
		sign.addProperty("backgroundColor", CRTSignElement.toHex(backgroundColor));
		sign.addProperty("frameWidth", frameWidth);
		sign.addProperty("frameColor", CRTSignElement.toHex(frameColor));
		if (jsStyleId != null && !jsStyleId.isEmpty()) {
			sign.addProperty("jsStyle", jsStyleId);
		}
		if (!jsSelectedIds.isEmpty()) {
			final JsonArray jsIds = new JsonArray();
			for (final Long jsId : jsSelectedIds) {
				jsIds.add(jsId);
			}
			sign.add("jsIds", jsIds);
		}
		final JsonArray rowsJson = new JsonArray();
		for (final Row row : rows) {
			rowsJson.add(row.toJson());
		}
		sign.add("rows", rowsJson);
		return root.toString();
	}

	/** 从存档 JSON 解析；非法数据返回空白布局（不抛异常，避免坏存档让渲染/界面崩溃）。 */
	public static CRTSignLayout fromJson(@Nullable String json) {
		if (json == null || json.trim().isEmpty()) {
			return null;
		}
		try {
			final JsonElement parsed = JsonParser.parseString(json);
			if (!parsed.isJsonObject()) {
				return null;
			}
			final JsonObject root = parsed.getAsJsonObject();
			final JsonObject sign = root.has("sign") && root.get("sign").isJsonObject() ? root.getAsJsonObject("sign") : root;
			final CRTSignLayout layout = new CRTSignLayout();
			layout.rows.clear();
			layout.widthMode = WidthMode.fromId(CRTSignElement.getString(sign, "widthMode", "fixed"));
			layout.width = clamp(CRTSignElement.getInt(sign, "width", DEFAULT_WIDTH), MIN_WIDTH, MAX_WIDTH);
			layout.rowHeight = clamp(CRTSignElement.getInt(sign, "rowHeight", DEFAULT_ROW_HEIGHT), MIN_ROW_HEIGHT, MAX_ROW_HEIGHT);
			layout.aspectLocked = CRTSignElement.getBoolean(sign, "aspectLocked", false);
			layout.backgroundColor = CRTSignElement.parseColor(CRTSignElement.getString(sign, "backgroundColor", "#FFFFFF"), DEFAULT_BACKGROUND);
			layout.frameWidth = clamp(CRTSignElement.getInt(sign, "frameWidth", 0), 0, 512);
			layout.frameColor = CRTSignElement.parseColor(CRTSignElement.getString(sign, "frameColor", "#808080"), LEGACY_FRAME_COLOR);
			layout.jsStyleId = sign.has("jsStyle") && sign.get("jsStyle").isJsonPrimitive()
					? sign.get("jsStyle").getAsString().trim() : null;
			if (layout.jsStyleId != null && layout.jsStyleId.isEmpty()) {
				layout.jsStyleId = null;
			}
			layout.jsSelectedIds.clear();
			if (sign.has("jsIds") && sign.get("jsIds").isJsonArray()) {
				for (final JsonElement jsId : sign.getAsJsonArray("jsIds")) {
					if (jsId.isJsonPrimitive() && jsId.getAsJsonPrimitive().isNumber()) {
						layout.jsSelectedIds.add(jsId.getAsLong());
					}
				}
			}

			if (sign.has("rows") && sign.get("rows").isJsonArray()) {
				for (final JsonElement rowElement : sign.getAsJsonArray("rows")) {
					final Row row = Row.fromJson(rowElement);
					if (row != null) {
						layout.rows.add(row);
					}
				}
			}
			if (layout.rows.isEmpty()) {
				layout.rows.add(new Row());
			}
			layout.ensureUniqueIds();
			layout.applyPaddingAuto();
			return layout;
		} catch (Exception ignored) {
			return null;
		}
	}

	/** 重复 id 会让选中态/查询歧义，解析后重新生成。 */
	private void ensureUniqueIds() {
		final List<String> seen = new ArrayList<>();
		for (final Row row : rows) {
			if (row.id == null || row.id.isEmpty() || seen.contains(row.id)) {
				row.id = Row.newId();
			}
			seen.add(row.id);
			for (final CRTSignElement element : row.elements) {
				if (element.id == null || element.id.isEmpty() || seen.contains(element.id)) {
					element.id = UUID.randomUUID().toString().replace("-", "");
				}
				seen.add(element.id);
			}
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	// ─── 内部类型 ────────────────────────────────────────────────────

	/** 一行元素（对应网页 JS 的 {@code Row}）。 */
	public static final class Row {
		public String id = newId();
		/** 该行背景色；null = 继承整牌背景。 */
		@Nullable
		public Integer backgroundColor;
		public final List<CRTSignElement> elements = new ArrayList<>();

		public static String newId() {
			return UUID.randomUUID().toString().replace("-", "");
		}

		public Row() {
		}

		public JsonObject toJson() {
			final JsonObject json = new JsonObject();
			json.addProperty("id", id);
			if (backgroundColor != null) {
				json.addProperty("backgroundColor", CRTSignElement.toHex(backgroundColor));
			}
			final JsonArray elementsJson = new JsonArray();
			for (final CRTSignElement element : elements) {
				elementsJson.add(element.toJson());
			}
			json.add("elements", elementsJson);
			return json;
		}

		@Nullable
		public static Row fromJson(@Nullable JsonElement element) {
			if (element == null || !element.isJsonObject()) {
				return null;
			}
			final JsonObject json = element.getAsJsonObject();
			final Row row = new Row();
			final String savedId = CRTSignElement.getString(json, "id", "");
			if (!savedId.isEmpty()) {
				row.id = savedId;
			}
			if (json.has("backgroundColor") && !json.get("backgroundColor").isJsonNull()) {
				row.backgroundColor = CRTSignElement.parseColor(CRTSignElement.getString(json, "backgroundColor", ""), (Integer) null);
			}
			if (json.has("elements") && json.get("elements").isJsonArray()) {
				for (final JsonElement elementJson : json.getAsJsonArray("elements")) {
					final CRTSignElement parsedElement = CRTSignElement.fromJson(elementJson);
					if (parsedElement != null) {
						row.elements.add(parsedElement);
					}
				}
			}
			return row;
		}
	}

	/** 元素在布局中的位置引用。 */
	public static final class ElementRef {
		public final Row row;
		public final int rowIndex;
		public final int index;
		public final CRTSignElement element;

		ElementRef(Row row, int rowIndex, int index, CRTSignElement element) {
			this.row = row;
			this.rowIndex = rowIndex;
			this.index = index;
			this.element = element;
		}
	}

	/** 兼容输出：网页端类型的字符串 id。 */
	public static String typeId(CRTSignElementType type) {
		return type == null ? "" : type.id.toLowerCase(Locale.ROOT);
	}
}
