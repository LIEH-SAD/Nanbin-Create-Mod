package com.Nanbin.client.JavaScriptSupport;

import com.Nanbin.Init;
import org.mtr.core.data.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;

public final class JSSignEngine {

	private static final Map<String, String> SCRIPT_CACHE = new HashMap<>();
	private static final Map<String, Object> RESULT_CACHE = new HashMap<>();
	private static final Map<String, ScriptEngine> ENGINE_CACHE = new HashMap<>();
	private static final Map<String, JSSignInstance> INSTANCE_CACHE = new HashMap<>();
	private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager(JSSignEngine.class.getClassLoader());
	/** ServiceLoader 发现失败时的直接实例化 fallback（Minecraft 特殊类加载器下 ServiceLoader 可能找不到服务文件）。 */
	private static ScriptEngine nashornEngine; // lazily created
	/** 每个脚本实际调用的数据接口类型（供数据选择屏幕按需显示按钮）。 */
	private static final Map<String, Set<String>> USED_DATA_TYPES = new java.util.concurrent.ConcurrentHashMap<>();
	/** 自定义文本在 selectedIds 中的编码标记（bit 62，避免与站点/站台ID、线路颜色冲突）。 */
	private static final long CUSTOM_TEXT_MARKER = 0x4000000000000000L;
	/** 实例闲置超过该时长（毫秒）即回收，防止指示牌拆除/区块卸载后 state 泄漏。 */
	private static final long INSTANCE_IDLE_TIMEOUT_MS = 10_000L;
	private static long lastPruneTime = 0;

	private JSSignEngine() {
	}

	public static void clearCache() {
		SCRIPT_CACHE.clear();
		RESULT_CACHE.clear();
		ENGINE_CACHE.clear();
		USED_DATA_TYPES.clear();
		INSTANCE_CACHE.values().forEach(JSSignInstance::dispose);
		INSTANCE_CACHE.clear();
	}

	/** 仅清空旧 execute 接口的结果缓存（定时刷新用），不影响每帧脚本实例与 state。 */
	public static void clearLegacyResultCache() {
		RESULT_CACHE.clear();
	}

	/** 记录脚本使用了某种数据接口（站台/车站/线路/出口/文本）。 */
	public static void markDataUsed(String scriptId, String type) {
		if (scriptId == null || type == null) {
			return;
		}
		USED_DATA_TYPES.computeIfAbsent(scriptId, key -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(type);
	}

	/** 脚本实际调用的数据接口类型集合（可能为空，表示脚本未调用任何数据接口）。 */
	public static Set<String> getUsedDataTypes(String scriptId) {
		final Set<String> used = USED_DATA_TYPES.get(scriptId);
		return used == null ? java.util.Collections.emptySet() : new java.util.HashSet<>(used);
	}

	/** 清空全部缓存时也清空数据接口使用记录。 */
	public static void clearDataUsage() {
		USED_DATA_TYPES.clear();
	}

	/**
	 * 每帧渲染入口：调用脚本的 create（首次）/ render（每帧）生命周期。
	 *
	 * @param signKey 用于区分同一脚本的不同指示牌实例（不同位置/不同行的 state 各自独立）
	 */
	public static void render(String scriptId, String signKey, JSDrawContext ctx, JSSignAPI api) {
		try {
			pruneStaleInstances();
			final JSSignInstance instance = getOrCreateInstance(scriptId, signKey);
			if (instance == null) {
				return;
			}
			instance.lastRenderTime = System.currentTimeMillis();
			instance.lastCtx = ctx;
			instance.lastApi = api;
			// 同步全局 api 绑定，兼容脚本直接引用全局 api 变量（引擎共享，渲染线程串行，安全）
			instance.engine.put("api", api);
			if (!instance.created) {
				instance.created = true;
				if (instance.hasFunction("create")) {
					instance.invoke("create", ctx, instance.state, api);
				}
			}
			if (instance.hasFunction("render")) {
				instance.invoke("render", ctx, instance.state, api);
			} else if (instance.hasFunction("execute")) {
				// 旧接口兼容：execute 返回结果，由渲染器逐格绘制
				instance.execute(ctx, api);
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to render JS sign script: {}", scriptId, e);
		}
	}

	/** 回收长时间未渲染的实例（指示牌被拆除 / 区块卸载 / 取消样式后释放 state 与引擎引用）。 */
	private static void pruneStaleInstances() {
		final long now = System.currentTimeMillis();
		if (now - lastPruneTime < INSTANCE_IDLE_TIMEOUT_MS) {
			return;
		}
		lastPruneTime = now;
		INSTANCE_CACHE.entrySet().removeIf(entry -> {
			final JSSignInstance instance = entry.getValue();
			if (now - instance.lastRenderTime > INSTANCE_IDLE_TIMEOUT_MS) {
				instance.dispose();
				return true;
			}
			return false;
		});
	}

	private static JSSignInstance getOrCreateInstance(String scriptId, String signKey) throws Exception {
		final String scriptPath = JSSignConfig.getScriptPath(scriptId);
		if (scriptPath == null) {
			Init.LOGGER.warn("JS sign script not found: {}", scriptId);
			return null;
		}

		String scriptContent = SCRIPT_CACHE.computeIfAbsent(scriptPath, JSSignEngine::loadScript);
		if (scriptContent == null) {
			Init.LOGGER.warn("Failed to load JS sign script: {}", scriptPath);
			return null;
		}

		JSSignInstance instance = INSTANCE_CACHE.get(signKey);
		if (instance != null) {
			return instance;
		}

		ScriptEngine engine = ENGINE_CACHE.get(scriptId);
		if (engine == null) {
			engine = createNashornEngine();
			if (engine == null) {
				Init.LOGGER.error("Nashorn script engine not available");
				return null;
			}
			engine.put("api", null);
			engine.put("Text", new JSText());
			engine.put("Texture", new JSTexture());
			engine.put("Rect", new JSRect());
			engine.put("Line", new JSLine());
			installChatWriter(engine);
			engine.eval(scriptContent);
			ENGINE_CACHE.put(scriptId, engine);
		}

		final Object state = engine.eval("({})");
		instance = new JSSignInstance(engine, state);
		INSTANCE_CACHE.put(signKey, instance);
		return instance;
	}

	/**
	 * 创建 Nashorn ScriptEngine。
	 * <p>
	 * 优先直接通过 {@code NashornScriptEngineFactory.getScriptEngine("--language=es6")} 创建
	 * —— Nashorn 15.4 默认仅支持 ES5，必须显式开启 ES6 才能使用 const/let/箭头函数/class 等语法。
	 * 失败时退回 ScriptEngineManager 的 ServiceLoader 发现（默认 ES5）。
	 * <p>
	 * 用反射实现，避免编译期对 Nashorn 具体类的依赖（fabric 侧为 include 打包，forge 侧复用环境自带的 nashorn-core）。
	 */
	private static ScriptEngine createNashornEngine() {
		try {
			if (nashornEngine == null) {
				nashornEngine = createNashornEngineDirect();
				if (nashornEngine == null) {
					// 极端 fallback：标准 ServiceLoader 发现（默认 ES5，仅保证引擎可用）
					Init.LOGGER.warn("Nashorn factory not available, falling back to ScriptEngineManager");
					nashornEngine = ENGINE_MANAGER.getEngineByName("nashorn");
				}
			}
			return nashornEngine;
		} catch (Exception e) {
			Init.LOGGER.error("Failed to create Nashorn script engine", e);
			return null;
		}
	}

	/** 反射创建开启 ES6 的 Nashorn 引擎，使脚本可使用现代语法并调用注入的 Java 对象方法。 */
	private static ScriptEngine createNashornEngineDirect() {
		try {
			final Class<?> factoryClass = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory", true, JSSignEngine.class.getClassLoader());
			final Object factory = factoryClass.getDeclaredConstructor().newInstance();
			// --language=es6：开启 const/let/箭头函数/class 等现代语法（Nashorn 15.4 默认仅 ES5）
			final ScriptEngine engine = (ScriptEngine) factoryClass.getMethod("getScriptEngine", String[].class).invoke(factory, (Object) new String[]{"--language=es6"});
			Init.LOGGER.info("Nashorn engine created via NashornScriptEngineFactory (ES6)");
			return engine;
		} catch (Exception e) {
			Init.LOGGER.warn("Failed to create Nashorn engine via factory", e);
			return null;
		}
	}

	private static String loadScript(String path) {
		try {
			final String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
			final InputStream stream = JSSignEngine.class.getClassLoader().getResourceAsStream(normalizedPath);
			if (stream == null) {
				return null;
			}
			return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			Init.LOGGER.error("Failed to load script from path: {}", path, e);
			return null;
		}
	}

	/** 让脚本的 print 输出到玩家聊天框。 */
	private static void installChatWriter(ScriptEngine engine) {
		engine.getContext().setWriter(new ChatPrintWriter());
	}

	/** 把 print 的一行输出转发到玩家聊天框（带防刷屏：相同文本 2 秒内不重复发送）。 */
	private static final class ChatPrintWriter extends Writer {

		private static final long DEDUP_MS = 2000;
		private final StringBuilder buffer = new StringBuilder();
		private final Map<String, Long> lastSentTimes = new HashMap<>();

		@Override
		public void write(char[] cbuf, int off, int len) {
			buffer.append(cbuf, off, len);
			drain();
		}

		@Override
		public void write(String str) {
			buffer.append(str);
			drain();
		}

		@Override
		public void flush() {
			drain();
		}

		@Override
		public void close() {
			drain();
		}

		private void drain() {
			int idx;
			while ((idx = buffer.indexOf("\n")) >= 0) {
				final String line = buffer.substring(0, idx);
				buffer.delete(0, idx + 1);
				sendLine(line);
			}
		}

		private void sendLine(String line) {
			final String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				return;
			}
			final long now = System.currentTimeMillis();
			final Long lastTime = lastSentTimes.get(trimmed);
			if (lastTime != null && now - lastTime < DEDUP_MS) {
				return;
			}
			if (lastSentTimes.size() > 64) {
				lastSentTimes.clear();
			}
			lastSentTimes.put(trimmed, now);
			final MinecraftClient client = MinecraftClient.getInstance();
			client.execute(() -> {
				final ClientPlayerEntity player = client.getPlayerMapped();
				if (player != null) {
					player.sendMessage(Text.cast(TextHelper.literal("[JS] " + trimmed)), false);
				}
			});
		}
	}

	/**
	 * 执行 JS 脚本（旧接口，逐格结果模式）。
	 *
	 * @return {@link JSSignResult}（单格/整块）或 {@link JSSignResult}[]（每格独立结果，来自 { cells: [...] } 或数组）
	 */
	public static Object execute(String scriptId, JSSignContext context) {
		final String cacheKey = scriptId + "_" + context.getCacheKey();
		if (RESULT_CACHE.containsKey(cacheKey)) {
			return RESULT_CACHE.get(cacheKey);
		}

		final String scriptPath = JSSignConfig.getScriptPath(scriptId);
		if (scriptPath == null) {
			Init.LOGGER.warn("JS sign script not found: {}", scriptId);
			return JSSignResult.error("Script not found: " + scriptId);
		}

		try {
			String scriptContent = SCRIPT_CACHE.computeIfAbsent(scriptPath, JSSignEngine::loadScript);
			if (scriptContent == null) {
				Init.LOGGER.warn("Failed to load JS sign script: {}", scriptPath);
				return JSSignResult.error("Failed to load script: " + scriptPath);
			}

			ScriptEngine engine = ENGINE_CACHE.get(scriptId);
			if (engine == null) {
				engine = createNashornEngine();
				if (engine == null) {
					Init.LOGGER.error("Nashorn script engine not available");
					return JSSignResult.error("Nashorn engine not available");
				}
				engine.put("Text", new JSText());
				engine.put("Texture", new JSTexture());
				engine.put("Rect", new JSRect());
				engine.put("Line", new JSLine());
				installChatWriter(engine);
				engine.eval(scriptContent);
				ENGINE_CACHE.put(scriptId, engine);
			}

			final JSSignAPI api = new JSSignAPI(context, scriptId);
			engine.put("api", api);

			if (engine instanceof Invocable) {
				final Invocable invocable = (Invocable) engine;
				final Object result = invocable.invokeFunction("execute", api);
				final Object parsed = parseResult(result);
				RESULT_CACHE.put(cacheKey, parsed);
				return parsed;
			} else {
				Init.LOGGER.warn("JS sign script engine not invocable: {}", scriptId);
				return JSSignResult.error("Script engine not invocable");
			}
		} catch (Exception e) {
			Init.LOGGER.error("Failed to execute JS sign script: {}", scriptId, e);
			return JSSignResult.error("Execution error: " + e.getMessage());
		}
	}

	private static Object parseResult(Object result) {
		if (result == null) {
			return JSSignResult.error("Script returned null");
		}

		if (result instanceof JSSignResult) {
			return result;
		}

		if (result instanceof String) {
			return new JSSignResult((String) result);
		}

		if (result instanceof Map) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> resultMap = (Map<String, Object>) result;
			if (resultMap.containsKey("cells")) {
				return parseCellResults(resultMap.get("cells"));
			}
			return parseCell(resultMap);
		}

		if (result instanceof List) {
			final List<?> list = (List<?>) result;
			final JSSignResult[] results = new JSSignResult[list.size()];
			for (int i = 0; i < list.size(); i++) {
				results[i] = parseCellValue(list.get(i));
			}
			return results;
		}

		if (result instanceof Object[]) {
			final Object[] array = (Object[]) result;
			final JSSignResult[] results = new JSSignResult[array.length];
			for (int i = 0; i < array.length; i++) {
				results[i] = parseCellValue(array[i]);
			}
			return results;
		}

		return JSSignResult.error("Invalid result format");
	}

	private static Object parseCellResults(Object cells) {
		if (cells == null) {
			return JSSignResult.error("cells is null");
		}
		if (cells instanceof List) {
			final List<?> list = (List<?>) cells;
			final JSSignResult[] results = new JSSignResult[list.size()];
			for (int i = 0; i < list.size(); i++) {
				results[i] = parseCellValue(list.get(i));
			}
			return results;
		}
		if (cells instanceof Object[]) {
			final Object[] array = (Object[]) cells;
			final JSSignResult[] results = new JSSignResult[array.length];
			for (int i = 0; i < array.length; i++) {
				results[i] = parseCellValue(array[i]);
			}
			return results;
		}
		return JSSignResult.error("cells must be an array");
	}

	private static JSSignResult parseCellValue(Object value) {
		if (value == null) {
			return JSSignResult.error("null cell");
		}
		if (value instanceof JSSignResult) {
			return (JSSignResult) value;
		}
		if (value instanceof String) {
			return new JSSignResult((String) value);
		}
		if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> resultMap = (Map<String, Object>) value;
			return parseCell(resultMap);
		}
		return JSSignResult.error("Invalid cell result format");
	}

	@SuppressWarnings("unchecked")
	private static JSSignResult parseCell(Map<String, Object> resultMap) {
		final String text = (String) resultMap.getOrDefault("text", "");
		final int textColor = resultMap.containsKey("textColor") ? ((Number) resultMap.get("textColor")).intValue() : 0;
		final float textSize = resultMap.containsKey("textSize") ? ((Number) resultMap.get("textSize")).floatValue() : 1.0F;
		final boolean textBold = resultMap.containsKey("textBold") && (Boolean) resultMap.get("textBold");
		final int backgroundColor = resultMap.containsKey("backgroundColor") ? ((Number) resultMap.get("backgroundColor")).intValue() : 0;
		return new JSSignResult(text, textColor, textSize, textBold, backgroundColor);
	}

	/** 将脚本结果规整为每格一个结果的数组；单个结果时整块复用。 */
	public static JSSignResult[] toCellResults(Object result, int cellCount) {
		if (result instanceof JSSignResult[]) {
			return (JSSignResult[]) result;
		}
		final JSSignResult single = result instanceof JSSignResult ? (JSSignResult) result : JSSignResult.error("Invalid result format");
		final JSSignResult[] results = new JSSignResult[Math.max(cellCount, 1)];
		for (int i = 0; i < results.length; i++) {
			results[i] = single;
		}
		return results;
	}

	/**
	 * 把自定义文本写入 selectedIds：每个 long 存 3 个 UTF-16 字符（16 位/字符），
	 * 高位（bit 62）为标记，紧邻 15 位为块索引，避免与站点/站台 ID（正数）和线路颜色（可能负数）冲突。
	 * 先清除旧的自定义文本块，再写入新文本。
	 */
	public static void writeCustomText(LongAVLTreeSet selectedIds, String text) {
		if (selectedIds == null) {
			return;
		}
		// 清除旧的自定义文本块（使用CUSTOM_TEXT_MARKER标记的值）
		final LongAVLTreeSet filtered = new LongAVLTreeSet();
		for (final long value : selectedIds) {
			if ((value & CUSTOM_TEXT_MARKER) != CUSTOM_TEXT_MARKER) {
				filtered.add(value);
			}
		}
		selectedIds.clear();
		selectedIds.addAll(filtered);
		if (text == null || text.isEmpty()) {
			return;
		}
		final int length = Math.min(text.length(), 3 * 32767);
		for (int i = 0; i < length; i += 3) {
			final int index = i / 3;
			final long c0 = i < length ? text.charAt(i) : 0;
			final long c1 = i + 1 < length ? text.charAt(i + 1) : 0;
			final long c2 = i + 2 < length ? text.charAt(i + 2) : 0;
			selectedIds.add(CUSTOM_TEXT_MARKER | ((long) index << 48) | (c0 << 32) | (c1 << 16) | c2);
		}
	}

	/** 从 selectedIds 中读取自定义文本（无自定义文本时返回空串）。 */
	public static String readCustomText(LongAVLTreeSet selectedIds) {
		if (selectedIds == null || selectedIds.isEmpty()) {
			return "";
		}
		int maxIndex = -1;
		for (final long value : selectedIds) {
			if ((value & CUSTOM_TEXT_MARKER) == CUSTOM_TEXT_MARKER) {
				final int index = (int) ((value >>> 48) & 0x7FFF);
				if (index > maxIndex) {
					maxIndex = index;
				}
			}
		}
		if (maxIndex < 0) {
			return "";
		}
		final int maxAllowedIndex = 32767;
		if (maxIndex > maxAllowedIndex) {
			Init.LOGGER.warn("Custom text maxIndex {} exceeds limit {}, returning empty string", maxIndex, maxAllowedIndex);
			return "";
		}
		final char[] chars = new char[(maxIndex + 1) * 3];
		for (final long value : selectedIds) {
			if ((value & CUSTOM_TEXT_MARKER) == CUSTOM_TEXT_MARKER) {
				final int index = (int) ((value >>> 48) & 0x7FFF);
				final int base = index * 3;
				chars[base] = (char) ((value >>> 32) & 0xFFFF);
				chars[base + 1] = (char) ((value >>> 16) & 0xFFFF);
				chars[base + 2] = (char) (value & 0xFFFF);
			}
		}
		int length = chars.length;
		while (length > 0 && chars[length - 1] == 0) {
			length--;
		}
		return new String(chars, 0, length);
	}

	/** 每帧渲染的单个指示牌实例状态。 */
	private static final class JSSignInstance {

		private final ScriptEngine engine;
		private final Object state;
		private boolean created;
		private long lastRenderTime;
		private JSDrawContext lastCtx;
		private JSSignAPI lastApi;

		private JSSignInstance(ScriptEngine engine, Object state) {
			this.engine = engine;
			this.state = state;
		}

		private boolean hasFunction(String name) {
			final Object value = engine.get(name);
			return value != null;
		}

		private void invoke(String name, Object... args) {
			try {
				((Invocable) engine).invokeFunction(name, args);
			} catch (Exception e) {
				Init.LOGGER.error("Failed to invoke JS function {}: {}", name, e.getMessage());
			}
		}

		/** 旧接口兼容：execute 返回结果，通过 draw 回调把每格结果绘制到 ctx。 */
		private void execute(JSDrawContext ctx, JSSignAPI api) {
			try {
				final Object result = ((Invocable) engine).invokeFunction("execute", api);
				final Object parsed = parseResult(result);
				ctx.drawResults(parsed);
			} catch (Exception e) {
				Init.LOGGER.error("Failed to execute legacy JS sign script: {}", e.getMessage());
			}
		}

		private void dispose() {
			if (created && hasFunction("dispose")) {
				try {
					((Invocable) engine).invokeFunction("dispose", lastCtx, state, lastApi);
				} catch (Exception e) {
					Init.LOGGER.error("Failed to dispose JS sign script: {}", e.getMessage());
				}
			}
			created = false;
			lastCtx = null;
			lastApi = null;
		}
	}

	public static class JSSignContext {

		private final String[] signIds;
		private final int cellIndex;
		private final LongAVLTreeSet selectedIds;
		private final String[] routeNumbers;
		private final int backgroundColor;
		private final float cellSize;
		private final String screenKey;
		private final BlockPos pos;
		private final Map<Long, String> routeNumberMap;

		public JSSignContext(String[] signIds, int cellIndex, LongAVLTreeSet selectedIds, String[] routeNumbers, int backgroundColor, float cellSize) {
			this(signIds, cellIndex, selectedIds, routeNumbers, backgroundColor, cellSize, null);
		}

		public JSSignContext(String[] signIds, int cellIndex, LongAVLTreeSet selectedIds, String[] routeNumbers, int backgroundColor, float cellSize, String screenKey) {
			this(signIds, cellIndex, selectedIds, routeNumbers, backgroundColor, cellSize, screenKey, null, null);
		}

		public JSSignContext(String[] signIds, int cellIndex, LongAVLTreeSet selectedIds, String[] routeNumbers, int backgroundColor, float cellSize, String screenKey, BlockPos pos, Map<Long, String> routeNumberMap) {
			this.signIds = signIds;
			this.cellIndex = cellIndex;
			this.selectedIds = selectedIds;
			this.routeNumbers = routeNumbers;
			this.backgroundColor = backgroundColor;
			this.cellSize = cellSize;
			this.screenKey = screenKey;
			this.pos = pos;
			this.routeNumberMap = routeNumberMap;
		}

		public String[] getSignIds() {
			return signIds;
		}

		/** 读取该行/该块某个格子的 signId（含样式标记等原始值）。 */
		public String getCellSignId(int index) {
			if (index >= 0 && index < signIds.length) {
				return signIds[index];
			}
			return null;
		}

		public int getCellIndex() {
			return cellIndex;
		}

		public LongAVLTreeSet getSelectedIds() {
			return selectedIds;
		}

		public String[] getRouteNumbers() {
			return routeNumbers;
		}

		public int getBackgroundColor() {
			return backgroundColor;
		}

		/** 单格世界尺寸（RailwaySign 为 0.5F，StationInfo 双格为 0.3F）。 */
		public float getCellSize() {
			return cellSize;
		}

		/** 当前屏幕唯一标识（坐标 + 行位置 + 朝向），用于多屏区分。 */
		public String getScreenKey() {
			return screenKey;
		}

		/** 指示牌方块位置（可能为 null），用于出口等需要定位所属车站的数据。 */
		public BlockPos getPos() {
			return pos;
		}

		/** 线路编号映射：RailwaySign 为 颜色→编号，StationInfo 为 平台ID→编号（可能为 null）。 */
		public Map<Long, String> getRouteNumberMap() {
			return routeNumberMap;
		}

		public String getCacheKey() {
			return cellIndex + "_" + cellSize + "_" + java.util.Arrays.toString(signIds) + "_" + selectedIds.toString() + "_" + backgroundColor;
		}
	}

	public static class JSSignAPI {

		private final JSSignContext context;
		private final String scriptId;

		public JSSignAPI(JSSignContext context) {
			this(context, null);
		}

		public JSSignAPI(JSSignContext context, String scriptId) {
			this.context = context;
			this.scriptId = scriptId;
		}

		private void markUsed(String type) {
			markDataUsed(scriptId, type);
		}

		/** 当前屏幕的唯一标识（脚本坐标 + 行位置 + 朝向），同一脚本被多个屏幕使用时可用于区分。 */
		public String getScreenKey() {
			return context.getScreenKey();
		}

		public String[] getSignIds() {
			return context.getSignIds();
		}

		public String getCellSignId(int index) {
			return context.getCellSignId(index);
		}

		public int getCellIndex() {
			return context.getCellIndex();
		}

		public int getCellCount() {
			return context.getSignIds().length;
		}

		/** 单格世界尺寸（RailwaySign 为 0.5F，StationInfo 双格为 0.3F）。 */
		public float getCellSize() {
			return context.getCellSize();
		}

		/** 指示牌渲染宽度（世界单位）= 格数 × 单格尺寸，如 3 格 RailwaySign = 1.5。 */
		public float getWidth() {
			return context.getSignIds().length * context.getCellSize();
		}

		/** 指示牌渲染高度（世界单位）= 单格尺寸，与 render 中原版渲染高度一致（RailwaySign = 0.5）。 */
		public float getHeight() {
			return context.getCellSize();
		}

		/**
		 * 返回选中站台/站点所属线路的真实颜色（ARGB）。
		 * selectedIds 中可能是站台 ID、站点 ID 或线路颜色值（换乘/多线路选择器可能混存），
		 * 统一解析后遍历 simplifiedRoutes 收集线路颜色。站点会连同换乘站（connectedStations）一并收集，
		 * 与 CRT 原版线路色条逻辑一致。
		 */
		public long[] getSelectedColors() {
			markUsed("route");
			final LongAVLTreeSet colors = new LongAVLTreeSet();
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			for (final long id : context.getSelectedIds()) {
				final Station station = clientData.stationIdMap.get(id);
				if (station != null) {
					addStationRouteColors(colors, station, clientData);
					for (final Station connectedStation : station.connectedStations) {
						addStationRouteColors(colors, connectedStation, clientData);
					}
				} else {
					final Platform platform = clientData.platformIdMap.get(id);
					if (platform != null) {
						addRouteColors(colors, platform.getId(), clientData);
						if (platform.area instanceof Station) {
							final Station owningStation = (Station) platform.area;
							for (final Station connectedStation : owningStation.connectedStations) {
								addStationRouteColors(colors, connectedStation, clientData);
							}
						}
					} else {
						// 可能直接存了线路颜色值
						addColorIfRouteExists(colors, id, clientData);
					}
				}
			}
			return colors.toLongArray();
		}

		private static void addStationRouteColors(LongAVLTreeSet colors, Station station, MinecraftClientData clientData) {
			if (station == null) {
				return;
			}
			for (final Platform platform : station.savedRails) {
				addRouteColors(colors, platform.getId(), clientData);
			}
		}

		private static void addRouteColors(LongAVLTreeSet colors, long platformId, MinecraftClientData clientData) {
			for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
				if (route.getPlatformIndex(platformId) >= 0) {
					colors.add(route.getColor() | 0xFF000000L);
				}
			}
		}

		private static void addColorIfRouteExists(LongAVLTreeSet colors, long colorValue, MinecraftClientData clientData) {
			for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
				if ((route.getColor() | 0xFF000000L) == colorValue) {
					colors.add(colorValue);
					return;
				}
			}
		}

		public long[] getSelectedStationIds() {
			markUsed("station");
			return context.getSelectedIds().longStream().toArray();
		}

		public String getStationNames() {
			markUsed("station");
			final StringBuilder builder = new StringBuilder();
			for (final long platformId : context.getSelectedIds().longStream().sorted().toArray()) {
				final String name = getStationName(platformId);
				if (!name.isEmpty()) {
					if (builder.length() > 0) {
						builder.append("/");
					}
					builder.append(name);
				}
			}
			return builder.toString();
		}

		public String[] getRouteNumbers() {
			markUsed("route");
			return context.getRouteNumbers();
		}

		public int getBackgroundColor() {
			return context.getBackgroundColor();
		}

		public String getStationName(long platformId) {
			markUsed("station");
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			final Platform platform = clientData.platformIdMap.get(platformId);
			if (platform != null) {
				return platform.getStationName();
			}
			final Station station = clientData.stationIdMap.get(platformId);
			return station == null ? "" : station.getName();
		}

		/** 线路中文名称（"1号线|Line 1" 取第一段），未选中对应线路时返回空串。 */
		public String getRouteName(long color) {
			markUsed("route");
			return getRouteName(color, false);
		}

		/** 线路名称；english 为 true 时返回英文段（"|" 分隔的第二段）。 */
		public String getRouteName(long color, boolean english) {
			final SimplifiedRoute route = findRoute(color);
			if (route == null) {
				return "";
			}
			final String[] parts = route.getName().split("\\|\\|");
			if (english && parts.length > 1) {
				return parts[1].trim();
			}
			return parts[0].trim();
		}

		/**
		 * 线路编号：优先使用服务端解析的编号（颜色/平台ID → 编号映射），
		 * 缺失时回退从线路名提取第一串数字（如 "1号线|Line 1" → "1"，环线 → 空串）。
		 */
		public String getRouteNumber(long key) {
			markUsed("route");
			final Map<Long, String> numberMap = context.getRouteNumberMap();
			if (numberMap != null) {
				final String number = numberMap.get(key & 0xFFFFFFL);
				if (number != null && !number.isEmpty()) {
					return number;
				}
			}
			final SimplifiedRoute route = findRoute(key);
			if (route != null) {
				return extractNumber(route.getName());
			}
			return "";
		}

		/** 线路终点站名（线路最后一站的 destination），未选中线路时返回空串。 */
		public String getRouteDestination(long color) {
			markUsed("route");
			final SimplifiedRoute route = findRoute(color);
			if (route == null) {
				return "";
			}
			final ObjectArrayList<SimplifiedRoutePlatform> platforms = route.getPlatforms();
			if (platforms == null || platforms.isEmpty()) {
				return "";
			}
			final String destination = platforms.get(platforms.size() - 1).getDestination();
			if (destination == null) {
				return "";
			}
			final String[] parts = destination.split("\\|\\|");
			return parts[0].trim();
		}

		/** 是否环线线路。 */
		public boolean isLoopRoute(long color) {
			markUsed("route");
			final SimplifiedRoute route = findRoute(color);
			return route != null && route.getCircularState() != Route.CircularState.NONE;
		}

		/**
		 * 当前屏幕类型："platform" 站台屏、"route" 线路屏、"exit" 出口屏、"station" 车站屏、"custom" 其它。
		 * 依据本行/本块第一个非 JS 样式标记格子的槽位类型判断，同一个 JS 文件应用到多种屏幕时可据此分流。
		 */
		public String getScreenType() {
			for (final String signId : context.getSignIds()) {
				if (signId == null || signId.isEmpty() || JSSignConfig.isJSStyle(signId)) {
					continue;
				}
				if (signId.equals("platform") || signId.equals("platform_flipped")) {
					return "platform";
				}
				if (signId.equals("line") || signId.equals("line_flipped")
						|| signId.equals("crt_route_name") || signId.equals("crt_route_name_flipped") || signId.equals("crt_route_number")) {
					return "route";
				}
				if (signId.equals("exit_letter") || signId.equals("exit_letter_flipped")) {
					return "exit";
				}
				if (signId.equals("station") || signId.equals("station_flipped") || signId.equals("crt_station_name")) {
					return "station";
				}
				return "custom";
			}
			return "custom";
		}

		/** 出口数量（当前选中出口屏时有效，其它屏幕返回 0）。 */
		public int getExitCount() {
			markUsed("exit");
			return getExitNumbers().length;
		}

		/** 出口编号列表（如 ["1A", "2B"]），从 selectedIds 反序列化出口名得到。 */
		public String[] getExitNumbers() {
			markUsed("exit");
			final List<String> result = new ArrayList<>();
			for (final long id : context.getSelectedIds()) {
				final String exitName = deserializeExitName(id);
				if (!exitName.isEmpty()) {
					result.add(exitName);
				}
			}
			return result.toArray(new String[0]);
		}

		/** 第 index 个出口的目的地列表。 */
		public String[] getExitDestinations(int index) {
			final String[] numbers = getExitNumbers();
			if (index < 0 || index >= numbers.length) {
				return new String[0];
			}
			final Station station = findExitStation();
			if (station == null) {
				return new String[0];
			}
			for (final StationExit exit : station.getExits()) {
				if (exit.getName().equals(numbers[index])) {
					final ObjectArrayList<String> destinations = exit.getDestinations();
					return destinations == null ? new String[0] : destinations.toArray(new String[0]);
				}
			}
			return new String[0];
		}

		/** 第 index 个出口的完整信息："编号：目的地1/目的地2"；无目的地时只返回编号。 */
		public String getExitInfo(int index) {
			final String[] numbers = getExitNumbers();
			if (index < 0 || index >= numbers.length) {
				return "";
			}
			final String[] destinations = getExitDestinations(index);
			if (destinations.length == 0) {
				return numbers[index];
			}
			return numbers[index] + "：" + String.join("/", destinations);
		}

		private static SimplifiedRoute findRoute(long color) {
			// 兼容 ARGB（getSelectedColors 返回格式）与纯 RGB（route.getColor() 返回 color & 0xFFFFFF）
			final long rgb = color & 0xFFFFFFL;
			for (final SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
				if ((route.getColor() & 0xFFFFFF) == rgb) {
					return route;
				}
			}
			return null;
		}

		private static String extractNumber(String name) {
			final StringBuilder builder = new StringBuilder();
			for (final char c : name.toCharArray()) {
				if (c >= '0' && c <= '9') {
					builder.append(c);
				} else if (builder.length() > 0) {
					break;
				}
			}
			return builder.toString();
		}

		/** 原版 deserializeExit 的逻辑：每 8 位一个字符，低位在前。 */
		private static String deserializeExitName(long value) {
			if (value <= 0) {
				return "";
			}
			final StringBuilder builder = new StringBuilder();
			long remaining = value;
			while (remaining > 0) {
				builder.insert(0, (char) (remaining & 0xFFL));
				remaining >>>= 8;
			}
			return builder.toString();
		}

		private Station findExitStation() {
			final BlockPos pos = context.getPos();
			if (pos == null) {
				return null;
			}
			try {
				return InitClient.findStation(pos);
			} catch (Exception e) {
				Init.LOGGER.error("Failed to find exit station at {}", pos.toShortString(), e);
				return null;
			}
		}

		public int getRouteColor(int routeIndex) {
			markUsed("route");
			final long[] colors = getSelectedColors();
			if (routeIndex >= 0 && routeIndex < colors.length) {
				return (int) colors[routeIndex];
			}
			return 0;
		}

		// ---- 站台接口（站台编号） ----

		/** 选中站台的数量（selectedIds 中站台 ID 的个数）。 */
		public int getPlatformCount() {
			markUsed("platform");
			int count = 0;
			for (final long id : context.getSelectedIds()) {
				if (MinecraftClientData.getInstance().platformIdMap.get(id) != null) {
					count++;
				}
			}
			return count;
		}

		/** 选中站台的编号列表（如 ["1", "2"]），按 ID 排序；只统计站台 ID。 */
		public String[] getPlatformNumbers() {
			markUsed("platform");
			final List<String> result = new ArrayList<>();
			final MinecraftClientData clientData = MinecraftClientData.getInstance();
			for (final long id : context.getSelectedIds().longStream().sorted().toArray()) {
				final Platform platform = clientData.platformIdMap.get(id);
				if (platform != null) {
					final String number = extractPlatformNumber(platform);
					if (!number.isEmpty()) {
						result.add(number);
					}
				}
			}
			return result.toArray(new String[0]);
		}

		/** 从站台名称中提取编号：优先取纯数字部分，无数字时返回原名称。 */
		private static String extractPlatformNumber(Platform platform) {
			final String name = platform.getName();
			if (name == null || name.isEmpty()) {
				return "";
			}
			final String trimmed = name.trim();
			if (trimmed.matches("\\d+")) {
				return trimmed;
			}
			final StringBuilder builder = new StringBuilder();
			for (final char c : trimmed.toCharArray()) {
				if (c >= '0' && c <= '9') {
					builder.append(c);
				} else if (builder.length() > 0) {
					break;
				}
			}
			return builder.length() > 0 ? builder.toString() : trimmed;
		}

		// ---- 自定义文本接口 ----

		/** 读取本屏玩家输入的自定义文本（空字符串表示未设置）。 */
		public String getCustomText() {
			markUsed("text");
			return readCustomText(context.getSelectedIds());
		}

		public long getWorldTime() {
			return System.currentTimeMillis();
		}

		public String getFormattedTime(String format) {
			try {
				return new java.text.SimpleDateFormat(format).format(new java.util.Date());
			} catch (Exception e) {
				return "";
			}
		}

		public JSSignResult createResult(String text) {
			return new JSSignResult(text);
		}

		public JSSignResult createResult(String text, int textColor, float textSize, boolean textBold, int backgroundColor) {
			return new JSSignResult(text, textColor, textSize, textBold, backgroundColor);
		}
	}

	public static class JSSignResult {

		private final String text;
		private final int textColor;
		private final float textSize;
		private final boolean textBold;
		private final int backgroundColor;
		private final boolean error;

		public JSSignResult(String text) {
			this.text = text;
			this.textColor = 0;
			this.textSize = 1.0F;
			this.textBold = false;
			this.backgroundColor = 0;
			this.error = false;
		}

		public JSSignResult(String text, int textColor, float textSize, boolean textBold, int backgroundColor) {
			this.text = text;
			this.textColor = textColor;
			this.textSize = textSize;
			this.textBold = textBold;
			this.backgroundColor = backgroundColor;
			this.error = false;
		}

		private JSSignResult(String errorMessage, boolean errorFlag) {
			this.text = errorMessage;
			this.textColor = 0xFF0000;
			this.textSize = 1.0F;
			this.textBold = false;
			this.backgroundColor = 0;
			this.error = true;
		}

		public static JSSignResult error(String message) {
			return new JSSignResult(message, true);
		}

		public String getText() {
			return text;
		}

		public int getTextColor() {
			return textColor;
		}

		public float getTextSize() {
			return textSize;
		}

		public boolean isTextBold() {
			return textBold;
		}

		public int getBackgroundColor() {
			return backgroundColor;
		}

		public boolean isError() {
			return error;
		}
	}
}