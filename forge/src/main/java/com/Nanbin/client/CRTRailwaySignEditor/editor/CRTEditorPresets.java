package com.Nanbin.client.CRTRailwaySignEditor.editor;

import com.Nanbin.signage.crt.CRTSignElement;
import org.mtr.libraries.com.google.gson.JsonArray;
import org.mtr.libraries.com.google.gson.JsonElement;
import org.mtr.libraries.com.google.gson.JsonObject;
import org.mtr.libraries.com.google.gson.JsonParser;
import org.mtr.mapping.holder.MinecraftClient;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CRT 导视牌编辑器的「预设」持久化（网页编辑器 localStorage['public-sign-presets'] 的 MC 版）。
 *
 * <p>结构照 {@code docs/WEB_EDITOR_SPEC.md} §7.3：
 * {@code [{id, name, createdAt, elements:[元素 JSON…]}]}，存到
 * {@code <游戏目录>/config/nanbin_crt_sign_presets.json}。</p>
 *
 * <p>所有读写都做了 try/catch：配置文件损坏/无权限时返回空列表或 false，
 * 由界面只弹一条 toast，不影响继续编辑（预设不是权威数据，牌面存档才是）。</p>
 */
public final class CRTEditorPresets {

	/** 预设文件格式版本（独立于牌面 SCHEMA_VERSION）。 */
	public static final int VERSION = 1;
	/** 预设数量上限（防止误操作写出超大文件）。 */
	public static final int MAX_PRESETS = 200;
	/** 单个预设保留的元素上限（网页剪贴板同样限制行/元素数量）。 */
	public static final int MAX_ELEMENTS = 200;
	/** 预设名最大长度（与网页输入框一致）。 */
	public static final int MAX_NAME_LENGTH = 40;
	/** 配置文件名（相对 config/）。 */
	public static final String FILE_NAME = "nanbin_crt_sign_presets.json";

	private CRTEditorPresets() {
	}

	/** 一个预设：命名 + 一组元素（单行）。 */
	public static final class Preset {
		public String id = newId();
		public String name = "";
		public long createdAt = System.currentTimeMillis();
		public final List<CRTSignElement> elements = new ArrayList<>();

		/** 序列化为网页 §7.3 的元素列表结构。 */
		public JsonObject toJson() {
			final JsonObject json = new JsonObject();
			json.addProperty("id", id == null || id.isEmpty() ? newId() : id);
			json.addProperty("name", name == null ? "" : name);
			json.addProperty("createdAt", createdAt);
			final JsonArray elementsJson = new JsonArray();
			for (final CRTSignElement element : elements) {
				if (element != null) {
					elementsJson.add(element.toJson());
				}
			}
			json.add("elements", elementsJson);
			return json;
		}

		/** 解析单个预设；结构非法返回 null（照网页读盘校验：过滤而非报错）。 */
		@Nullable
		public static Preset fromJson(@Nullable JsonElement element) {
			if (element == null || !element.isJsonObject()) {
				return null;
			}
			final JsonObject json = element.getAsJsonObject();
			final Preset preset = new Preset();
			final String savedId = getString(json, "id", "");
			if (!savedId.isEmpty()) {
				preset.id = savedId;
			}
			preset.name = getString(json, "name", "").trim();
			preset.createdAt = json.has("createdAt") ? json.get("createdAt").getAsLong() : System.currentTimeMillis();
			if (!json.has("elements") || !json.get("elements").isJsonArray()) {
				return null;
			}
			for (final JsonElement elementJson : json.getAsJsonArray("elements")) {
				final CRTSignElement parsed = CRTSignElement.fromJson(elementJson);
				if (parsed != null && preset.elements.size() < MAX_ELEMENTS) {
					preset.elements.add(parsed);
				}
			}
			if (preset.name.isEmpty() || preset.elements.isEmpty()) {
				return null;
			}
			return preset;
		}

		/** 生成可直接插入布局的深拷贝（重新生成 id，避免与牌面已有元素冲突）。 */
		public List<CRTSignElement> materialize() {
			final List<CRTSignElement> copies = new ArrayList<>();
			for (final CRTSignElement element : elements) {
				if (element != null) {
					copies.add(element.copy());
				}
			}
			return copies;
		}
	}

	/** 新建预设（元素深拷贝，保留当前外观）。 */
	public static Preset create(String name, List<CRTSignElement> elements) {
		final Preset preset = new Preset();
		preset.name = name == null ? "" : name.trim();
		final int limit = Math.min(elements == null ? 0 : elements.size(), MAX_ELEMENTS);
		for (int i = 0; i < limit; i++) {
			final CRTSignElement element = elements.get(i);
			if (element != null) {
				preset.elements.add(element.copy());
			}
		}
		return preset;
	}

	/** 预设文件；游戏目录不可用时返回 null。 */
	@Nullable
	public static File file() {
		try {
			final MinecraftClient client = MinecraftClient.getInstance();
			if (client == null) {
				return null;
			}
			final File runDirectory = client.getRunDirectoryMapped();
			if (runDirectory == null) {
				return null;
			}
			final File configDirectory = new File(runDirectory, "config");
			if (!configDirectory.isDirectory() && !configDirectory.mkdirs()) {
				return null;
			}
			return new File(configDirectory, FILE_NAME);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 读取全部预设；文件不存在/损坏时返回空列表（不抛异常）。 */
	public static List<Preset> load() {
		final List<Preset> presets = new ArrayList<>();
		final File file = file();
		if (file == null || !file.isFile()) {
			return presets;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			final StringBuilder builder = new StringBuilder();
			final char[] buffer = new char[4096];
			int read;
			while ((read = reader.read(buffer)) >= 0) {
				builder.append(buffer, 0, read);
				// 配置损坏时避免把超大内容读进内存
				if (builder.length() > 4 * 1024 * 1024) {
					return presets;
				}
			}
			final JsonElement parsed = JsonParser.parseString(builder.toString());
			if (parsed == null || !parsed.isJsonArray()) {
				return presets;
			}
			for (final JsonElement element : parsed.getAsJsonArray()) {
				final Preset preset = Preset.fromJson(element);
				if (preset != null && presets.size() < MAX_PRESETS) {
					presets.add(preset);
				}
			}
		} catch (Throwable ignored) {
			presets.clear();
		}
		return presets;
	}

	/**
	 * 写盘（先写临时文件再改名，避免半截文件）；失败返回 false，不影响内存里的预设列表。
	 */
	public static boolean save(List<Preset> presets) {
		final File file = file();
		if (file == null) {
			return false;
		}
		final JsonArray root = new JsonArray();
		final int limit = Math.min(presets == null ? 0 : presets.size(), MAX_PRESETS);
		for (int i = 0; i < limit; i++) {
			final Preset preset = presets.get(i);
			if (preset != null && preset.name != null && !preset.name.trim().isEmpty()) {
				root.add(preset.toJson());
			}
		}
		File temporary = null;
		try {
			temporary = new File(file.getParentFile(), file.getName() + ".tmp");
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
				writer.write(root.toString());
			}
			if (!temporary.renameTo(file)) {
				// Windows 上目标存在时 renameTo 可能失败：退化为直接覆盖写
				try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
					writer.write(root.toString());
				}
			}
			return true;
		} catch (Throwable ignored) {
			return false;
		} finally {
			if (temporary != null && temporary.isFile() && !temporary.equals(file)) {
				// 临时文件残留不影响功能，静默清理
				//noinspection ResultOfMethodCallIgnored
				temporary.delete();
			}
		}
	}

	/** 新预设 id（与网页 uuid 语义一致，去掉连字符）。 */
	public static String newId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private static String getString(JsonObject json, String key, String fallback) {
		try {
			if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
				return json.get(key).getAsString();
			}
		} catch (Throwable ignored) {
		}
		return fallback;
	}
}
