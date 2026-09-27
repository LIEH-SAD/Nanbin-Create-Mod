package com.Nanbin.client.ClientData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.Nanbin.Init.LOGGER;

// 客户端配置
public final class NanbinClientConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "nanbin-client.json";
	private static final String KEY_ROUTE_MAP_URL = "routeMapUrl";
	private static final String CONFIG_DIRECTORY = "config";

	private static boolean loaded;
	private static String routeMapUrl = "";

	private NanbinClientConfig() {
	}

	// 全局覆写线路图
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

	// 配置文件位于游戏目录下的 {@code config/}（映射层没有 architectury，直接取运行目录）。
	private static Path getFile() {
		final MinecraftClient client = MinecraftClient.getInstance();
		final File runDirectory = client == null ? null : client.runDirectory;
		final File configDirectory = new File(runDirectory == null ? new File(".") : runDirectory, CONFIG_DIRECTORY);
		return configDirectory.toPath().resolve(FILE_NAME);
	}
}
