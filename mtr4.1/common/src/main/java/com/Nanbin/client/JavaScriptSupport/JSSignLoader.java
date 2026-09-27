package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.Init;
import net.minecraft.util.Identifier;
import org.mtr.config.Config;
import org.mtr.core.serializer.JsonReader;
import org.mtr.resource.ResourceManagerHelper;

/**
 * 读取 {@code assets/nanbin/js_signs_config.json}，把可用的 JS 站牌脚本登记到 {@link JSSignConfig}。
 * <p>
 * 4.1 适配：MTR 4.0 的映射层 {@code ResourceManagerHelper} 在 4.1 里是
 * {@code org.mtr.resource.ResourceManagerHelper}（收原版 {@link Identifier}）；
 * {@code Config.readResource} 现在返回 gson 的 {@code JsonElement}，直接喂给 {@code JsonReader}。
 */
public final class JSSignLoader {

	private static final Identifier JS_SIGNS_CONFIG_ID = Identifier.of("nanbin", "js_signs_config.json");

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
	}
}
