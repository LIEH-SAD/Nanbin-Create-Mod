package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.Init;
import org.mtr.core.serializer.JsonReader;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.config.Config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JSSignLoader {

	private static final Identifier JS_SIGNS_CONFIG_ID = new Identifier("nanbin", "js_signs_config.json");

	private JSSignLoader() {
	}

	public static void reload() {
		JSSignConfig.clear();
		JSSignEngine.clearCache();
		ResourceManagerHelper.readAllResources(JS_SIGNS_CONFIG_ID, inputStream -> {
			try {
				final JsonReader reader = new JsonReader(Config.readResource(inputStream));
				reader.iterateReaderArray("scripts", () -> {
				}, scriptReader -> {
					final String id = scriptReader.getString("id", "");
					final String path = scriptReader.getString("path", "");
					final String icon = scriptReader.getString("icon", "");
					final String name = scriptReader.getString("name", "");
					if (!id.isEmpty() && !path.isEmpty()) {
						JSSignConfig.register(id, path, icon, name);
						Init.LOGGER.info("Loaded JS sign script: {} -> {} (icon={}, name={})", id, path, icon, name);
					} else {
						Init.LOGGER.warn("Invalid JS sign script entry: id='{}', path='{}'", id, path);
					}
				});
			} catch (Exception e) {
				Init.LOGGER.error("Failed to load JS signs config from {}", JS_SIGNS_CONFIG_ID, e);
			}
		});
		warnDuplicateScripts();
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