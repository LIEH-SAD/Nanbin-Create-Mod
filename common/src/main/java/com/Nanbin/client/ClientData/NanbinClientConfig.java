package com.Nanbin.client.ClientData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.Nanbin.Init.LOGGER;

/**
 * Nanbin 的纯客户端配置，存放在 {@code config/nanbin-client.json}。
 * 目前有两项：全局覆写线路图的图片网址（见
 * {@link com.Nanbin.client.Drawing.RouteMapOverride}）与编辑器深色模式。
 */
public final class NanbinClientConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "nanbin-client.json";
	private static final String KEY_ROUTE_MAP_URL = "routeMapUrl";
	private static final String KEY_DARK_MODE = "darkMode";

	private static boolean loaded;
	private static String routeMapUrl = "";
	private static boolean darkMode = false;

	private NanbinClientConfig() {
	}

	/** 全局覆写线路图的图片网址；空字符串表示使用默认线路图。 */
	public static String getRouteMapUrl() {
		load();
		return routeMapUrl;
	}

	public static void setRouteMapUrl(String url) {
		load();
		routeMapUrl = url == null ? "" : url.trim();
		save();
	}

	/** 编辑器界面深色模式（默认浅色，与网页项目一致）。 */
	public static boolean isDarkMode() {
		load();
		return darkMode;
	}

	public static void setDarkMode(boolean value) {
		load();
		darkMode = value;
		save();
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;

		final Path file = getFile();
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			final JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			if (json.has(KEY_ROUTE_MAP_URL)) {
				routeMapUrl = json.get(KEY_ROUTE_MAP_URL).getAsString().trim();
			}
			if (json.has(KEY_DARK_MODE)) {
				darkMode = json.get(KEY_DARK_MODE).getAsBoolean();
			}
		} catch (Exception e) {
			LOGGER.error("NanbinClientConfig: failed to read {}", file, e);
		}
	}

	private static void save() {
		final Path file = getFile();
		try {
			Files.createDirectories(file.getParent());
			final JsonObject json = new JsonObject();
			json.addProperty(KEY_ROUTE_MAP_URL, routeMapUrl);
			json.addProperty(KEY_DARK_MODE, darkMode);
			Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.error("NanbinClientConfig: failed to write {}", file, e);
		}
	}

	private static Path getFile() {
		return Platform.getConfigFolder().resolve(FILE_NAME);
	}
}
