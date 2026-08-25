package com.Nanbin.client.JavaScriptSupport;

import org.mtr.mapping.holder.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class JSSignConfig {

	/** 全局 JS 样式标记前缀：放在每行第 0 格（signIds[i][0]），表示整块指示牌由该 JS 脚本渲染。 */
	public static final String JS_STYLE_PREFIX = "crt_js_style_";
	/** 脚本未配置 icon 时使用的默认图标。 */
	public static final Identifier DEFAULT_ICON = new Identifier("nanbin", "textures/gui/js_icon.png");

	private static final Map<String, String> SCRIPT_PATHS = new HashMap<>();
	private static final Map<String, Identifier> SCRIPT_ICONS = new HashMap<>();
	private static final Map<String, String> SCRIPT_NAMES = new HashMap<>();

	private JSSignConfig() {
	}

	public static void clear() {
		SCRIPT_PATHS.clear();
		SCRIPT_ICONS.clear();
		SCRIPT_NAMES.clear();
	}

	public static void register(String scriptId, String path, String icon, String name) {
		SCRIPT_PATHS.put(scriptId, path);
		if (icon != null && !icon.isEmpty()) {
			try {
				SCRIPT_ICONS.put(scriptId, new Identifier(icon));
			} catch (Exception e) {
				SCRIPT_ICONS.remove(scriptId);
			}
		} else {
			SCRIPT_ICONS.remove(scriptId);
		}
		if (name != null && !name.isEmpty()) {
			SCRIPT_NAMES.put(scriptId, name);
		} else {
			SCRIPT_NAMES.remove(scriptId);
		}
	}

	public static String getScriptPath(String scriptId) {
		return SCRIPT_PATHS.get(scriptId);
	}

	public static Identifier getIcon(String scriptId) {
		return SCRIPT_ICONS.getOrDefault(scriptId, DEFAULT_ICON);
	}

	/** 脚本显示名称的语言键；未在配置中指定 name 时回退为 gui.nanbin.js_sign.<id>。 */
	public static String getDisplayNameKey(String scriptId) {
		return SCRIPT_NAMES.getOrDefault(scriptId, "gui.nanbin.js_sign." + scriptId);
	}

	public static Map<String, String> getAllScripts() {
		return new HashMap<>(SCRIPT_PATHS);
	}

	public static boolean hasScript(String scriptId) {
		return SCRIPT_PATHS.containsKey(scriptId);
	}

	// ---- 全局 JS 样式标记 ----

	public static boolean isJSStyle(String signId) {
		return signId != null && signId.startsWith(JS_STYLE_PREFIX);
	}

	/** 判断一整行是否启用了 JS 样式（行内第 0 格为样式标记）。 */
	public static boolean hasJSStyle(String[] signIds) {
		return signIds != null && signIds.length > 0 && isJSStyle(signIds[0]);
	}

	/** 从行内第 0 格样式标记中取出脚本 id；未启用 JS 样式时返回 null。 */
	public static String getStyleScriptId(String[] signIds) {
		if (!hasJSStyle(signIds)) {
			return null;
		}
		return signIds[0].substring(JS_STYLE_PREFIX.length());
	}
}