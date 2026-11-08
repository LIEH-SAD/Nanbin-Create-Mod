package com.Nanbin.client.CRTRailwaySignEditor.legacy;

import com.Nanbin.Init;
import com.Nanbin.client.Drawing.SignTextStyleConfig;
import com.Nanbin.client.Drawing.SignTextStyleConfig.SignTextStyle;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mtr.client.CustomResources;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
 * （MTR 4.0 的 {@code CustomResourceLoader.SIGNS}，即原版导视牌编辑界面的可选列表），
 * 因此原版 MTR 导视牌上不会再出现这些标志。</p>
 *
 * <p>标志是否出现由 crt_signs.json 里新增的 {@code whitelist} 数组决定：
 * 只有 whitelist 里显式列出 {@code "crt"} 的标志才会出现在 CRT 类导视
 * （CRT 铁路指示牌、墙挂牌、站名牌、导向牌等）上；未声明或空数组的标志不会被任何导视启用，
 * 也不会出现在原版 MTR 导视牌上。</p>
 *
 * <p>渲染侧仍需要 MTR 的标志查询缓存才能取到贴图（{@code RenderRailwaySign.getSign}
 * 只查这个 map），所以这些标志只写进查询缓存，不写进列表，从而既不影响原版界面也不丢渲染。</p>
 *
 * <p><b>MTR 3.6.3 适配说明</b>：来源用 Mixin 钩 {@code CustomResourceLoader.reload()}，
 * 目标侧改用 MTR 3.6.3 的正规入口 {@link CustomResources#registerReloadListener}；资源读取改用
 * {@link MinecraftClient#getResourceManager()}（{@code getAllResources} / {@code findResources}）；
 * 3.6.3 没有 MTR 4.0 的 {@code SIGNS} 全局列表与反射写入的 {@code SIGNS_CACHE}，
 * 其查询缓存就是 {@link CustomResources#CUSTOM_SIGNS}，因此这里直接写入该 map。</p>
 */
public final class CRTSignRegistry {

	/** whitelist 取值：CRT 类导视。 */
	public static final String SURFACE_CRT = "crt";

	private static final Identifier SIGNS_ID = new Identifier("nanbin", "crt_signs.json");
	/** 旧文件名（重命名前）：继续支持旧追加包。 */
	private static final Identifier LEGACY_SIGNS_ID = new Identifier("nanbin", "signs.json");
	/** 本 mod 方块类的包名前缀，用于识别「CRT 类导视」。 */
	private static final String NANBIN_BLOCK_PACKAGE_PREFIX = "com.Nanbin.";

	private static final Map<String, CustomResources.CustomSign> SIGNS = new LinkedHashMap<>();
	private static final Map<String, Set<String>> WHITELISTS = new LinkedHashMap<>();
	/** 代码内置的 27 个基础图标 id（与编辑器网页图标一一对应）。 */
	private static final Set<String> BUILT_IN_IDS = new LinkedHashSet<>();
	/** 所有从 signs JSON（旧单文件或追加包目录）加载的 id。 */
	private static final Set<String> JSON_IDS = new LinkedHashSet<>();

	private CRTSignRegistry() {
	}

	/** 注册资源重载监听器。应在客户端初始化时调用一次。 */
	public static void register() {
		CustomResources.registerReloadListener(resourceManager -> reload());
	}

	/** 资源重载时调用：重新读取 crt_signs.json，并把标志写进渲染查询缓存。 */
	public static void reload() {
		SIGNS.clear();
		WHITELISTS.clear();
		SignTextStyleConfig.clear();
		BUILT_IN_IDS.clear();
		JSON_IDS.clear();

		final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

		// 1) 先加载旧文件名兼容资源（assets/nanbin/signs.json）。
		//    它包含部分旧存档仍在使用的 signId，也与代码内置的 27 个图标有重名。
		//    必须先加载：后续内置表会覆盖重复定义并恢复 whitelist=["crt"]，避免旧 JSON 清空内置图标白名单。
		try {
			for (final Resource resource : resourceManager.getAllResources(LEGACY_SIGNS_ID)) {
				loadResource(resource, LEGACY_SIGNS_ID, false);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load legacy railway signs from {}", LEGACY_SIGNS_ID, e);
		}

		// 2) 与编辑器网页图标一一对应的内置图标：覆盖旧文件中的同名定义，始终可用
		try {
			loadSigns(JsonParser.parseString(CRTBuiltInSignIcons.JSON), true, false);
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load built-in CRT sign icons", e);
		}

		// 3) CRT 专属文字牌预设：CRTPresetCatalog 中的 49 个 ID 由此提供贴图、文字和底色定义
		try {
			for (final Resource resource : resourceManager.getAllResources(SIGNS_ID)) {
				loadResource(resource, SIGNS_ID, false);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load custom railway signs from {}", SIGNS_ID, e);
		}

		// 4) 追加包目录：任意命名空间的 assets/<namespace>/crt_signs/*.json
		//    （不声明 whitelist 时默认启用 crt，方便第三方追加包直接使用）
		try {
			for (final Map.Entry<Identifier, Resource> entry : resourceManager.findResources("crt_signs", path -> path.getPath().endsWith(".json")).entrySet()) {
				loadResource(entry.getValue(), entry.getKey(), true);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load CRT sign add-on packs", e);
		}

		registerLookupCache();
		Init.LOGGER.info("Loaded {} custom railway signs ({} enabled for CRT signage)", SIGNS.size(), countCrtSigns());
	}

	/** 解析单个资源文件；单个文件失败不影响其余文件。 */
	private static void loadResource(Resource resource, Identifier identifier, boolean defaultCrtWhitelist) {
		try (InputStream inputStream = resource.getInputStream()) {
			loadSigns(JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), false, defaultCrtWhitelist);
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load custom railway signs from {}", identifier, e);
		}
	}

	/** 解析一份 signs JSON（结构同 crt_signs.json），把标志写进注册表。 */
	private static void loadSigns(JsonElement element, boolean builtIn, boolean defaultCrtWhitelist) {
		if (element == null || !element.isJsonObject()) {
			return;
		}
		final JsonObject root = element.getAsJsonObject();
		if (!root.has("signs") || !root.get("signs").isJsonArray()) {
			return;
		}
		for (final JsonElement signElement : root.getAsJsonArray("signs")) {
			if (signElement == null || !signElement.isJsonObject()) {
				continue;
			}
			final JsonObject signReader = signElement.getAsJsonObject();
			// MTR 4.0 SignResource：默认标志的 id 以 "!" 前缀标记，signId 会去掉该前缀
			final String rawId = getString(signReader, "id", "");
			final String signId = rawId.startsWith("!") ? rawId.substring(1) : rawId;
			final CustomResources.CustomSign sign = new CustomResources.CustomSign(
					parseTextureId(getString(signReader, "textureResource", "")),
					getBool(signReader, "flipTexture", false),
					getString(signReader, "customText", ""),
					getBool(signReader, "flipCustomText", false),
					getBool(signReader, "small", false),
					CustomResources.colorStringToInt(getString(signReader, "backgroundColor", ""))
			);
			final Set<String> whitelist = new LinkedHashSet<>();
			if (signReader.has("whitelist") && signReader.get("whitelist").isJsonArray()) {
				for (final JsonElement valueElement : signReader.getAsJsonArray("whitelist")) {
					final String value = valueElement != null && valueElement.isJsonPrimitive() ? valueElement.getAsString() : null;
					if (value != null && !value.trim().isEmpty()) {
						whitelist.add(value.trim().toLowerCase(Locale.ROOT));
					}
				}
			}
			if (defaultCrtWhitelist && whitelist.isEmpty()) {
				whitelist.add(SURFACE_CRT);
			}
			SIGNS.put(signId, sign);
			WHITELISTS.put(signId, whitelist);
			if (builtIn) {
				BUILT_IN_IDS.add(signId);
			} else {
				JSON_IDS.add(signId);
			}
			// 自定义文字样式（MTR 不识别这些键，只由本 mod 使用）
			SignTextStyleConfig.put(signId, new SignTextStyle(
					SignTextStyleConfig.parseColor(getString(signReader, "textColor", "")),
					(float) getDouble(signReader, "textSize", 1.0),
					getBool(signReader, "textBold", false)
			));
		}
	}

	/**
	 * 复刻 MTR 4.0 {@code CustomResourceTools.formatIdentifierWithDefault(textureResource, "png")}：
	 * 小写化、去掉已有扩展名后补 {@code .png}；空值退回 {@code mtr:textures/block/transparent.png}。
	 */
	private static Identifier parseTextureId(String textureResource) {
		if (textureResource == null || textureResource.isEmpty()) {
			return new Identifier("mtr", "textures/block/transparent.png");
		}
		final String lowerCase = textureResource.toLowerCase(Locale.ROOT);
		final int dotIndex = lowerCase.indexOf('.');
		final String withoutExtension = dotIndex >= 0 ? lowerCase.substring(0, dotIndex) : lowerCase;
		final String withExtension = withoutExtension + ".png";
		return new Identifier(withExtension.contains(":") ? withExtension : "minecraft:" + withExtension);
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

	/** 追加包标志在编辑器里的显示名：优先用标志的自定义文字，否则用 id 美化。 */
	public static String getSignLabel(String signId) {
		final CustomResources.CustomSign sign = getSign(signId);
		if (sign != null) {
			try {
				// MTR 4.0 用 TextHelper.translatable(customText).getString()；无翻译键时两者结果一致
				final String value = sign.customText;
				if (value != null && !value.trim().isEmpty()) {
					return value.trim();
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
	public static CustomResources.CustomSign getSign(String signId) {
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
			final ClientWorld world = client == null ? null : client.world;
			if (world == null) {
				return false;
			}
			final Object block = world.getBlockState(pos).getBlock();
			return block != null && block.getClass().getName().startsWith(NANBIN_BLOCK_PACKAGE_PREFIX);
		} catch (Exception e) {
			Init.LOGGER.warn("Failed to detect CRT sign block at {}", pos, e);
			return false;
		}
	}

	/**
	 * 只写渲染查询缓存，不写 MTR 的全局标志列表。
	 *
	 * <p>MTR 3.6.3 没有 MTR 4.0 的独立 {@code SIGNS_CACHE}：{@code RenderRailwaySign.getSign}
	 * 查的就是 {@link CustomResources#CUSTOM_SIGNS}。该 map 在每次
	 * {@code CustomResources.reload} 时会被清空后重新收集，因此这里直接 putAll 即可。</p>
	 */
	private static void registerLookupCache() {
		try {
			CustomResources.CUSTOM_SIGNS.putAll(SIGNS);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register Nanbin custom railway signs", e);
		}
	}

	private static long countCrtSigns() {
		return SIGNS.keySet().stream().filter(signId -> isAllowed(signId, SURFACE_CRT)).count();
	}

	private static String getString(JsonObject json, String key, String fallback) {
		try {
			if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
				return json.get(key).getAsString();
			}
		} catch (Exception ignored) {
		}
		return fallback;
	}

	private static boolean getBool(JsonObject json, String key, boolean fallback) {
		try {
			if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
				return json.get(key).getAsBoolean();
			}
		} catch (Exception ignored) {
		}
		return fallback;
	}

	private static double getDouble(JsonObject json, String key, double fallback) {
		try {
			if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
				return json.get(key).getAsDouble();
			}
		} catch (Exception ignored) {
		}
		return fallback;
	}
}
