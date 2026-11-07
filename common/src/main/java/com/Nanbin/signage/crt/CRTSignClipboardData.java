package com.Nanbin.signage.crt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 元素剪贴板：复制/粘贴布局里选中的元素。
 *
 * <p>JSON 结构与网页项目 CRT-Sign-Web-Editor 的元素剪贴板完全一致
 * （{@code {kind:'jr-sign-elements', version:1, rows:[{offset, elements}]}}），
 * 因此在网页项目和游戏之间可以互相复制粘贴。</p>
 */
public final class CRTSignClipboardData {

	public static final String KIND = "jr-sign-elements";
	public static final int VERSION = 1;
	private static final int MAX_ROWS = 1000;
	private static final int MAX_ELEMENTS = 10000;

	public static final class RowGroup {
		public final int offset;
		public final List<CRTSignElement> elements;

		public RowGroup(int offset, List<CRTSignElement> elements) {
			this.offset = offset;
			this.elements = elements;
		}
	}

	private CRTSignClipboardData() {
	}

	/** 把 layout 中选中的元素序列化成剪贴板 JSON；没有选中元素时返回 null。 */
	@Nullable
	public static String copy(CRTSignLayout layout, List<String> elementIds) {
		if (layout == null || elementIds == null || elementIds.isEmpty()) {
			return null;
		}
		final List<RowGroup> groups = new ArrayList<>();
		int firstRow = -1;
		for (int rowIndex = 0; rowIndex < layout.rows.size(); rowIndex++) {
			final CRTSignLayout.Row row = layout.rows.get(rowIndex);
			final List<CRTSignElement> selected = new ArrayList<>();
			for (final CRTSignElement element : row.elements) {
				if (elementIds.contains(element.id)) {
					selected.add(element);
				}
			}
			if (selected.isEmpty()) {
				continue;
			}
			if (firstRow < 0) {
				firstRow = rowIndex;
			}
			groups.add(new RowGroup(rowIndex - firstRow, selected));
		}
		if (groups.isEmpty()) {
			return null;
		}

		final JsonObject root = new JsonObject();
		root.addProperty("kind", KIND);
		root.addProperty("version", VERSION);
		final JsonArray rowsJson = new JsonArray();
		for (final RowGroup group : groups) {
			final JsonObject groupJson = new JsonObject();
			groupJson.addProperty("offset", group.offset);
			final JsonArray elementsJson = new JsonArray();
			for (final CRTSignElement element : group.elements) {
				elementsJson.add(element.toJson());
			}
			groupJson.add("elements", elementsJson);
			rowsJson.add(groupJson);
		}
		root.add("rows", rowsJson);
		return root.toString();
	}

	/** 解析剪贴板 JSON；非法数据返回 null。 */
	@Nullable
	public static List<RowGroup> parse(@Nullable String json) {
		if (json == null || json.trim().isEmpty()) {
			return null;
		}
		try {
			final JsonElement parsed = JsonParser.parseString(json);
			if (!parsed.isJsonObject()) {
				return null;
			}
			final JsonObject root = parsed.getAsJsonObject();
			if (!KIND.equals(CRTSignElement.getString(root, "kind", "")) || CRTSignElement.getInt(root, "version", 0) != VERSION) {
				return null;
			}
			if (!root.has("rows") || !root.get("rows").isJsonArray()) {
				return null;
			}
			final JsonArray rowsJson = root.getAsJsonArray("rows");
			if (rowsJson.size() == 0 || rowsJson.size() > MAX_ROWS) {
				return null;
			}
			final List<RowGroup> groups = new ArrayList<>();
			int previousOffset = -1;
			int elementCount = 0;
			for (final JsonElement rowElement : rowsJson) {
				if (!rowElement.isJsonObject()) {
					return null;
				}
				final JsonObject groupJson = rowElement.getAsJsonObject();
				final int offset = CRTSignElement.getInt(groupJson, "offset", -1);
				if (offset < 0 || offset > 999 || offset <= previousOffset) {
					return null;
				}
				previousOffset = offset;
				if (!groupJson.has("elements") || !groupJson.get("elements").isJsonArray()) {
					return null;
				}
				final List<CRTSignElement> elements = new ArrayList<>();
				for (final JsonElement elementJson : groupJson.getAsJsonArray("elements")) {
					final CRTSignElement element = CRTSignElement.fromJson(elementJson);
					if (element == null) {
						return null;
					}
					elements.add(element);
					elementCount++;
					if (elementCount > MAX_ELEMENTS) {
						return null;
					}
				}
				if (elements.isEmpty()) {
					return null;
				}
				groups.add(new RowGroup(offset, elements));
			}
			if (groups.get(0).offset != 0) {
				return null;
			}
			return groups;
		} catch (Exception ignored) {
			return null;
		}
	}

	/**
	 * 把剪贴板内容粘贴到 layout 的指定行/间隙；返回是否成功。
	 * 元素会被深拷贝并分配新 id（与网页端 pasteElements 一致）。
	 */
	public static boolean paste(CRTSignLayout layout, @Nullable String clipboardJson, String targetRowId, int gapIndex) {
		final List<RowGroup> groups = parse(clipboardJson);
		if (layout == null || groups == null) {
			return false;
		}
		final int targetIndex = layout.indexOfRow(targetRowId);
		if (targetIndex < 0) {
			return false;
		}
		for (final RowGroup group : groups) {
			final int rowIndex = targetIndex + group.offset;
			while (layout.rows.size() <= rowIndex) {
				layout.rows.add(new CRTSignLayout.Row());
			}
			final CRTSignLayout.Row row = layout.rows.get(rowIndex);
			final int insertIndex = group.offset == 0
					? Math.max(0, Math.min(row.elements.size(), gapIndex))
					: row.elements.size();
			int cursor = insertIndex;
			for (final CRTSignElement source : group.elements) {
				final CRTSignElement copy = source.copy();
				copy.id = UUID.randomUUID().toString().replace("-", "");
				row.elements.add(cursor++, copy);
			}
		}
		return true;
	}
}
