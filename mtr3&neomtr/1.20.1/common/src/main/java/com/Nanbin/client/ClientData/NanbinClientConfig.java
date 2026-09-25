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
 * 目前只有一项：全局覆写线路图的图片网址（见
 * {@link com.Nanbin.client.Drawing.RouteMapOverride}）。
 */
public final class NanbinClientConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "nanbin-client.json";
	private static final String KEY_ROUTE_MAP_URL = "routeMapUrl";

	private static boolean loaded;
	private static String routeMapUrl = "";

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
			Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.error("NanbinClientConfig: failed to write {}", file, e);
		}
	}

	private static Path getFile() {
		return Platform.getConfigFolder().resolve(FILE_NAME);
	}
}
