package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.Init;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mtr.client.CustomResources;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRT 导视牌整牌 JS 样式配置加载器：读取 {@code nanbin:crt_js_signs_config.json}，
 * 把脚本 id / 路径 / 图标 / 显示名注册进 {@link JSSignConfig}。
 *
 * <p>与 {@link JSSignLoader}（{@code js_signs_config.json}，旧格子编辑器沿用）是**叠加关系**：
 * 本类只负责新增的 {@code crt_js_signs_config.json}，不清空 {@link JSSignConfig}——
 * 两个配置文件里的脚本会合并，同名 id 由后加载者覆盖（与 MTR 资源包覆盖语义一致）。
 * 资源包重载时两者都应被调用（调用点由主编排统一接入）。</p>
 */
public final class CRTJsSignLoader {

	private static final Identifier CRT_JS_SIGNS_CONFIG_ID = new Identifier("nanbin", "crt_js_signs_config.json");

	private CRTJsSignLoader() {
	}

	/**
	 * 注册资源重载监听器（客户端初始化时调用一次）。
	 *
	 * <p>与 {@link JSSignLoader#register()} 同理：初始化阶段 {@code MinecraftClient} 还不存在，
	 * 必须延迟到资源重载时再读取配置，否则 CRT JS 预设不会出现在编辑器/选择器里。</p>
	 */
	public static void register() {
		CustomResources.registerReloadListener(resourceManager -> reload());
	}

	/** 加载 {@code crt_js_signs_config.json}（可重复调用；资源重载后应再次调用）。 */
	public static void reload() {
		// 资源可能换了，烘焙贴图与贴图像素缓存一起作废
		JSSignTextureBaker.clearCaches();
		if (MinecraftClient.getInstance() == null) {
			Init.LOGGER.warn("CRTJsSignLoader: MinecraftClient not ready, CRT JS sign scripts will load on resource reload");
			return;
		}
		try {
			final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
			final List<Resource> resources = resourceManager.getAllResources(CRT_JS_SIGNS_CONFIG_ID);
			for (final Resource resource : resources) {
				parseResource(resource);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load CRT JS signs config from {}", CRT_JS_SIGNS_CONFIG_ID, e);
		}
		warnDuplicateScripts();
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
					Init.LOGGER.info("Loaded CRT JS sign script: {} -> {} (icon={}, name={})", id, path, icon, name);
				} else {
					Init.LOGGER.warn("Invalid CRT JS sign script entry: id='{}', path='{}'", id, path);
				}
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to parse CRT JS signs config resource {}", CRT_JS_SIGNS_CONFIG_ID, e);
		}
	}

	/**
	 * 多个脚本 id 指向同一个脚本文件时给出提示：它们在样式选择器里会渲染出一模一样的画面，
	 * 看起来就像「切换没生效」。多半是配置里复制粘贴后忘了改 path。
	 */
	private static void warnDuplicateScripts() {
		final Map<String, String> firstIdByPath = new LinkedHashMap<>();
		for (final Map.Entry<String, String> entry : JSSignConfig.getAllScripts().entrySet()) {
			final String previous = firstIdByPath.putIfAbsent(entry.getValue(), entry.getKey());
			if (previous != null) {
				Init.LOGGER.warn("JS sign scripts '{}' and '{}' both use '{}' - they will render identically",
						previous, entry.getKey(), entry.getValue());
			}
		}
	}
}
