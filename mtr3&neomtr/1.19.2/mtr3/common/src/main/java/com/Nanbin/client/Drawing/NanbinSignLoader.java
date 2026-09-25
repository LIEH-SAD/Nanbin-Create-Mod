package com.Nanbin.client.Drawing;

import com.Nanbin.Init;
import com.Nanbin.client.JavaScriptSupport.JSSignLoader;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 自定义铁路告示牌加载器：从 nanbin:signs.json 读取自定义标志，注册到
 * {@link CustomResources#CUSTOM_SIGNS}，并解析 MTR 不识别的扩展字段
 * （textColor/textSize/textBold）到 {@link SignTextStyleConfig}。
 * <p>
 * 通过 {@link CustomResources#registerReloadListener} 挂载到 MTR 的资源重载流程，
 * 资源包重载时自动刷新。
 */
public final class NanbinSignLoader {

	private static final Identifier SIGNS_ID = new Identifier("nanbin", "signs.json");

	/** 上次加载时注册的自定义标志 id（用于重载时先清理，避免重复/残留）。 */
	private static final Set<String> LOADED_KEYS = new HashSet<>();

	private NanbinSignLoader() {
	}

	/** 注册资源重载监听器。应在客户端初始化时调用一次。 */
	public static void register() {
		CustomResources.registerReloadListener(resourceManager -> reload());
	}

	/** 立即执行一次加载（通常在资源重载时由监听器触发）。 */
	public static void reload() {
		try {
			// 清理上一次注册的自定义标志与文字样式
			for (final String key : LOADED_KEYS) {
				CustomResources.CUSTOM_SIGNS.remove(key);
			}
			LOADED_KEYS.clear();
			SignTextStyleConfig.clear();

			final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
			final List<Resource> resources = resourceManager.getAllResources(SIGNS_ID);
			for (final Resource resource : resources) {
				parseResource(resource);
			}

			// 加载 JS 指示牌配置
			JSSignLoader.reload();
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load custom railway signs from {}", SIGNS_ID, e);
		}
	}

	private static void parseResource(Resource resource) {
		try (InputStream stream = resource.getInputStream()) {
			final JsonElement rootElement = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			final JsonObject root = rootElement.getAsJsonObject();
			final JsonArray signs = root.has("signs") ? root.getAsJsonArray("signs") : null;
			if (signs == null) {
				return;
			}
			for (final JsonElement signElement : signs) {
				final JsonObject sign = signElement.getAsJsonObject();
				final String id = getString(sign, "id", "");
				if (id.isEmpty()) {
					continue;
				}
				final String textureResource = getString(sign, "textureResource", "");
				if (textureResource.isEmpty()) {
					continue;
				}
				// MC 资源查找是精确匹配，textureResource 可能未带 .png 后缀，需补全
				final String texturePath = textureResource.endsWith(".png") ? textureResource : textureResource + ".png";
				final Identifier textureId = new Identifier(texturePath);
				final boolean flipTexture = getBool(sign, "flipTexture", false);
				final String customText = getString(sign, "customText", "");
				final boolean flipCustomText = getBool(sign, "flipCustomText", false);
				final boolean small = getBool(sign, "small", false);
				final int backgroundColor = parseColor(getString(sign, "backgroundColor", ""));

				final CustomResources.CustomSign customSign = new CustomResources.CustomSign(
						textureId, flipTexture, customText, flipCustomText, small, backgroundColor
				);
				CustomResources.CUSTOM_SIGNS.put(id, customSign);
				LOADED_KEYS.add(id);

				// 自定义文字样式（MTR 不识别这些键，只由本 mod 使用）
				SignTextStyleConfig.put(id, new SignTextStyleConfig.SignTextStyle(
						SignTextStyleConfig.parseColor(getString(sign, "textColor", "")),
						(float) getDouble(sign, "textSize", 1.0),
						getBool(sign, "textBold", false)
				));
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to parse custom railway signs resource {}", SIGNS_ID, e);
		}
	}

	private static String getString(JsonObject obj, String key, String def) {
		if (obj.has(key)) {
			return obj.get(key).getAsString();
		}
		return def;
	}

	private static boolean getBool(JsonObject obj, String key, boolean def) {
		if (obj.has(key)) {
			return obj.get(key).getAsBoolean();
		}
		return def;
	}

	private static double getDouble(JsonObject obj, String key, double def) {
		if (obj.has(key)) {
			return obj.get(key).getAsDouble();
		}
		return def;
	}

	/** 解析 "#RRGGBB" / "#AARRGGBB" 为 ARGB int；未设置或非法返回 0。 */
	private static int parseColor(String color) {
		if (color == null || color.isEmpty()) {
			return 0;
		}
		try {
			final String hex = color.startsWith("#") ? color.substring(1) : color;
			if (hex.length() == 6) {
				return (int) Long.parseLong(hex, 16) | 0xFF000000;
			} else if (hex.length() == 8) {
				return (int) Long.parseLong(hex, 16);
			}
		} catch (NumberFormatException ignored) {
		}
		return 0;
	}
}
