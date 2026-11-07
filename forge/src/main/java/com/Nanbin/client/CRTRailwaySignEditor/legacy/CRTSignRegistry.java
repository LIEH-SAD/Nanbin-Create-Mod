package com.Nanbin.client.CRTRailwaySignEditor.legacy;

import com.Nanbin.client.CRTRailwaySignEditor.legacy.SignTextStyleConfig.SignTextStyle;
import org.mtr.core.serializer.JsonReader;
import org.mtr.libraries.com.google.gson.JsonElement;
import org.mtr.libraries.com.google.gson.JsonParser;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.Init;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.config.Config;
import org.mtr.mod.resource.SignResource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 本 mod 自定义导视标志（{@code nanbin:crt_signs.json}）的注册表。
 *
 * <p>与原版 MTR 的加载方式不同，这里不再把 crt_signs.json 的标志混进 MTR 的全局标志列表
 * （{@code CustomResourceLoader.SIGNS}，即原版导视牌编辑界面的可选列表），
 * 因此原版 MTR 导视牌上不会再出现这些标志。</p>
 *
 * <p>标志是否出现由 crt_signs.json 里新增的 {@code whitelist} 数组决定：
 * 只有 whitelist 里显式列出 {@code "crt"} 的标志才会出现在 CRT 类导视
 * （CRT 铁路指示牌、墙挂牌、站名牌、导向牌等）上；未声明或空数组的标志不会被任何导视启用，
 * 也不会出现在原版 MTR 导视牌上。</p>
 *
 * <p>渲染侧仍需要 MTR 的 {@code SIGNS_CACHE} 才能取到贴图（{@code RenderRailwaySign.getSign}
 * 只查这个 map），所以这些标志只写进查询缓存，不写进列表，从而既不影响原版界面也不丢渲染。</p>
 */
public final class CRTSignRegistry {

	/** whitelist 取值：CRT 类导视。 */
	public static final String SURFACE_CRT = "crt";

	private static final Identifier SIGNS_ID = new Identifier("nanbin", "crt_signs.json");
	/** 旧文件名（重命名前）：继续支持旧追加包。 */
	private static final Identifier LEGACY_SIGNS_ID = new Identifier("nanbin", "signs.json");
	/** 本 mod 方块类的包名前缀，用于识别「CRT 类导视」。 */
	private static final String NANBIN_BLOCK_PACKAGE_PREFIX = "com.Nanbin.";

	private static final Map<String, SignResource> SIGNS = new LinkedHashMap<>();
	private static final Map<String, Set<String>> WHITELISTS = new LinkedHashMap<>();
	/** 代码内置的 27 个基础图标 id（与编辑器网页图标一一对应）。 */
	private static final Set<String> BUILT_IN_IDS = new LinkedHashSet<>();
	/** 所有从 signs JSON（旧单文件或追加包目录）加载的 id。 */
	private static final Set<String> JSON_IDS = new LinkedHashSet<>();

	private CRTSignRegistry() {
	}

	/** 资源重载时调用：重新读取 crt_signs.json，并把标志写进渲染查询缓存。 */
	public static void reload() {
		SIGNS.clear();
		WHITELISTS.clear();
		SignTextStyleConfig.clear();
		BUILT_IN_IDS.clear();
		JSON_IDS.clear();

		// 1) 与编辑器网页图标一一对应的内置图标：定义已从 crt_signs.json 合并进代码（永远可用）
		try {
			loadSigns(JsonParser.parseString(CRTBuiltInSignIcons.JSON), true, false);
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load built-in CRT sign icons", e);
		}

		// 2) 旧格式单文件：资源包可覆盖/追加（assets/nanbin/crt_signs.json，多包会合并）
		ResourceManagerHelper.readAllResources(SIGNS_ID, inputStream -> {
			try {
				loadSigns(Config.readResource(inputStream), false, false);
			} catch (Exception e) {
				Init.LOGGER.error("Failed to load custom railway signs from {}", SIGNS_ID, e);
			}
		});

		// 2b) 旧文件名兼容：assets/nanbin/signs.json（重命名前的追加包）
		ResourceManagerHelper.readAllResources(LEGACY_SIGNS_ID, inputStream -> {
			try {
				loadSigns(Config.readResource(inputStream), false, false);
			} catch (Exception e) {
				Init.LOGGER.error("Failed to load legacy railway signs from {}", LEGACY_SIGNS_ID, e);
			}
		});

		// 3) 追加包目录：任意命名空间的 assets/<namespace>/crt_signs/*.json
		//    （不声明 whitelist 时默认启用 crt，方便第三方追加包直接使用）
		ResourceManagerHelper.readDirectory("crt_signs", (identifier, inputStream) -> {
			try {
				loadSigns(Config.readResource(inputStream), false, true);
			} catch (Exception e) {
				Init.LOGGER.error("Failed to load CRT sign add-on pack {}", identifier, e);
			}
		});

		registerLookupCache();
		Init.LOGGER.info("Loaded {} custom railway signs ({} enabled for CRT signage)", SIGNS.size(), countCrtSigns());
	}

	/** 解析一份 signs JSON（结构同 crt_signs.json），把标志写进注册表。 */
	private static void loadSigns(JsonElement element, boolean builtIn, boolean defaultCrtWhitelist) {
		final JsonReader reader = new JsonReader(element);
		reader.iterateReaderArray("signs", () -> {
		}, signReader -> {
			final SignResource sign = new SignResource(signReader);
			final Set<String> whitelist = new LinkedHashSet<>();
			signReader.iterateStringArray("whitelist", () -> {
			}, value -> {
				if (value != null && !value.trim().isEmpty()) {
					whitelist.add(value.trim().toLowerCase(Locale.ROOT));
				}
			});
			if (defaultCrtWhitelist && whitelist.isEmpty()) {
				whitelist.add(SURFACE_CRT);
			}
			SIGNS.put(sign.signId, sign);
			WHITELISTS.put(sign.signId, whitelist);
			if (builtIn) {
				BUILT_IN_IDS.add(sign.signId);
			} else {
				JSON_IDS.add(sign.signId);
			}
			// 自定义文字样式（MTR 不识别这些键，只由本 mod 使用）
			SignTextStyleConfig.put(sign.signId, new SignTextStyle(
					SignTextStyleConfig.parseColor(signReader.getString("textColor", "")),
					(float) signReader.getDouble("textSize", 1.0),
					signReader.getBoolean("textBold", false)
			));
		});
	}

	/** 所有已加载的标志 id（内置 + 旧单文件 + 追加包），按字母序。 */
	public static List<String> getAllSignIds() {
		final List<String> ids = new ArrayList<>(SIGNS.keySet());
		Collections.sort(ids);
		return ids;
	}

	/** 从 signs JSON（旧单文件或追加包）加载、且不在代码内置表里的额外标志 id。 */
	public static List<String> getExtraSignIds() {
		final List<String> ids = new ArrayList<>();
		for (final String signId : JSON_IDS) {
			if (!BUILT_IN_IDS.contains(signId)) {
				ids.add(signId);
			}
		}
		Collections.sort(ids);
		return ids;
	}

	/** 追加包标志在编辑器里的显示名：优先用 SignResource 的自定义文字，否则用 id 美化。 */
	public static String getSignLabel(String signId) {
		final SignResource sign = getSign(signId);
		if (sign != null) {
			try {
				final MutableText text = sign.getCustomText();
				if (text != null) {
					final String value = text.getString();
					if (value != null && !value.trim().isEmpty()) {
						return value.trim();
					}
				}
			} catch (Exception ignored) {
			}
		}
		String label = signId == null ? "" : signId;
		if (label.startsWith("crt_")) {
			label = label.substring(4);
		} else if (label.startsWith("nanbin_")) {
			label = label.substring(7);
		}
		return label.replace('_', ' ');
	}

	/** crt_signs.json / 内置表里的标志；不存在时返回 null。 */
	public static SignResource getSign(String signId) {
		return signId == null ? null : SIGNS.get(signId);
	}

	/**
	 * 指定标志是否允许出现在某类导视上。
	 *
	 * <p>严格按 whitelist 判断：必须显式列出 surface 才启用；未声明/空数组的路标不会被
	 * 任何导视启用（避免默认放行后混进原版导视牌）。</p>
	 */
	public static boolean isAllowed(String signId, String surface) {
		if (signId == null || surface == null) {
			return false;
		}
		final Set<String> whitelist = WHITELISTS.get(signId);
		return whitelist != null && whitelist.contains(surface);
	}

	/** 指定导视类型可选的标志 id（按字母序）。 */
	public static List<String> getSignIdsFor(String surface) {
		final List<String> signIds = new ArrayList<>();
		for (final String signId : SIGNS.keySet()) {
			if (isAllowed(signId, surface)) {
				signIds.add(signId);
			}
		}
		Collections.sort(signIds);
		return signIds;
	}

	/** 把指定导视类型可选的标志追加到编辑界面的标志列表里（已存在的跳过）。 */
	public static void appendSignsFor(List<String> target, String surface) {
		if (target == null) {
			return;
		}
		for (final String signId : getSignIdsFor(surface)) {
			if (!target.contains(signId)) {
				target.add(signId);
			}
		}
	}

	/**
	 * 判断该坐标上的方块是否属于本 mod 的 CRT 类导视。
	 * 本 mod 的方块都在 {@code com.Nanbin} 包下，原版 MTR 导视牌是 {@code org.mtr.mod.block.*} 下的类，
	 * 因此按包名判断可以覆盖现在和以后新增的所有 CRT 导视方块。
	 */
	public static boolean isCrtSignPosition(BlockPos pos) {
		if (pos == null) {
			return false;
		}
		try {
			final MinecraftClient client = MinecraftClient.getInstance();
			final ClientWorld world = client == null ? null : client.getWorldMapped();
			if (world == null) {
				return false;
			}
			final Object block = world.getBlockState(pos).getBlock().data;
			return block != null && block.getClass().getName().startsWith(NANBIN_BLOCK_PACKAGE_PREFIX);
		} catch (Exception e) {
			Init.LOGGER.warn("Failed to detect CRT sign block at {}", pos, e);
			return false;
		}
	}

	/** 只写渲染查询缓存，不写 MTR 的全局标志列表。 */
	private static void registerLookupCache() {
		try {
			final Field signsCacheField = CustomResourceLoader.class.getDeclaredField("SIGNS_CACHE");
			signsCacheField.setAccessible(true);
			@SuppressWarnings("unchecked")
			final Map<String, SignResource> signsCache = (Map<String, SignResource>) signsCacheField.get(null);
			signsCache.putAll(SIGNS);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to register Nanbin custom railway signs", e);
		}
	}

	private static long countCrtSigns() {
		return SIGNS.keySet().stream().filter(signId -> isAllowed(signId, SURFACE_CRT)).count();
	}
}
