package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.Init;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class JSSignLoader {

	private static final Identifier JS_SIGNS_CONFIG_ID = new Identifier("nanbin", "js_signs_config.json");

	private JSSignLoader() {
	}

	public static void reload() {
		JSSignConfig.clear();
		JSSignEngine.clearCache();
		try {
			// MTR 3.2.2 没有 ResourceManagerHelper / JsonReader，改用 MC 原版资源管理器 + gson 解析
			final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
			final List<Resource> resources = resourceManager.getAllResources(JS_SIGNS_CONFIG_ID);
			for (final Resource resource : resources) {
				parseResource(resource);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load JS signs config from {}", JS_SIGNS_CONFIG_ID, e);
		}
	}

	private static void parseResource(Resource resource) {
		try (InputStream stream = resource.getInputStream()) {
			final JsonElement rootElement = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			final JsonObject root = rootElement.getAsJsonObject();
			final JsonArray scripts = root.has("scripts") ? root.getAsJsonArray("scripts") : null;
			if (scripts == null) {
				return;
			}
			for (final JsonElement scriptElement : scripts) {
				final JsonObject script = scriptElement.getAsJsonObject();
				final String id = script.has("id") ? script.get("id").getAsString() : "";
				final String path = script.has("path") ? script.get("path").getAsString() : "";
				final String icon = script.has("icon") ? script.get("icon").getAsString() : "";
				final String name = script.has("name") ? script.get("name").getAsString() : "";
				if (!id.isEmpty() && !path.isEmpty()) {
					JSSignConfig.register(id, path, icon, name);
					Init.LOGGER.info("Loaded JS sign script: {} -> {} (icon={}, name={})", id, path, icon, name);
				} else {
					Init.LOGGER.warn("Invalid JS sign script entry: id='{}', path='{}'", id, path);
				}
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to parse JS signs config resource {}", JS_SIGNS_CONFIG_ID, e);
		}
	}
}
