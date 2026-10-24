package com.Nanbin.client.Render;

import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Route;
import org.mtr.resource.SignResource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告示牌所在位置可用的「线路颜色 → 线路编号 / 线路名称」缓存。
 * <p>
 * <b>为什么 4.1 不需要 4.0 那套 NBT 同步：</b>4.0 的客户端数据只有
 * {@code SimplifiedRoute}（没有 routeNumber 字段），所以线路编号只能在服务端解析后
 * 通过 NBT / 网络包送到客户端。MTR 4.1 的 {@code org.mtr.core.data.Route} 有公开的
 * {@link Route#getRouteNumber()}，而 {@link SignResource#getRoutesForRailwaySign(BlockPos)}
 * 正好给出「某块告示牌附近可用的线路」（内部带缓存），所以完全可以在客户端直接拿到真实编号。
 * <p>
 * 这里按方块位置做一层带 TTL 的缓存：{@code getRoutesForRailwaySign} 自身有缓存，
 * 但渲染是逐格逐帧调用的，仍然值得再挡一层。
 */
public final class SignRouteData {

	/** 缓存时长；MTR 的 {@code SIGN_ROUTES_CACHE} 也以自己的周期刷新，这里再短一点足够。 */
	private static final long TTL_MS = 1000L;
	private static final int MAX_ENTRIES = 512;

	private static final Map<Long, Entry> CACHE = new ConcurrentHashMap<>();

	private SignRouteData() {
	}

	private static final class Entry {

		private final Map<Long, String> numbers = new HashMap<>();
		private final Map<Long, String> names = new HashMap<>();
		private final long time;

		private Entry(BlockPos pos) {
			time = System.currentTimeMillis();
			try {
				for (final Route route : SignResource.getRoutesForRailwaySign(pos)) {
					final long key = route.getColor();
					numbers.put(key, route.getRouteNumber() == null ? "" : route.getRouteNumber());
					names.put(key, route.getName() == null ? "" : route.getName());
				}
			} catch (Exception ignored) {
				// 数据未同步时按空处理
			}
		}
	}

	private static Entry get(BlockPos pos) {
		if (pos == null) {
			return null;
		}
		final long key = pos.asLong();
		final long now = System.currentTimeMillis();
		final Entry cached = CACHE.get(key);
		if (cached != null && now - cached.time < TTL_MS) {
			return cached;
		}
		final Entry entry = new Entry(pos);
		CACHE.put(key, entry);
		if (CACHE.size() > MAX_ENTRIES) {
			CACHE.entrySet().removeIf(entry2 -> now - entry2.getValue().time >= TTL_MS);
		}
		return entry;
	}

	/** 线路颜色 → 线路编号（无则返回空串）。 */
	public static String getRouteNumber(BlockPos pos, long color) {
		final Entry entry = get(pos);
		final String number = entry == null ? null : entry.numbers.get(color);
		return number == null ? "" : number;
	}

	/** 线路颜色 → 线路原始名称（可能是「编号||名称」的双语格式，无则返回空串）。 */
	public static String getRouteName(BlockPos pos, long color) {
		final Entry entry = get(pos);
		final String name = entry == null ? null : entry.names.get(color);
		return name == null ? "" : name;
	}

	/** 全部「线路颜色 → 线路原始名称」，供 JS 脚本的 routeNumberMap 使用。 */
	public static Map<Long, String> getRouteNames(BlockPos pos) {
		final Entry entry = get(pos);
		return entry == null ? new HashMap<>() : new HashMap<>(entry.names);
	}

	/**
	 * 站台 id → 线路编号。
	 * <p>
	 * 站台本身没有编号，链路是：站台 → 经过它的 {@code SimplifiedRoute}（拿颜色）→
	 * 同颜色的 {@link Route#getRouteNumber()}。两步用的都是公开 API。
	 */
	public static String getRouteNumberForPlatform(BlockPos pos, long platformId) {
		try {
			for (final org.mtr.core.data.SimplifiedRoute route : org.mtr.client.MinecraftClientData.getInstance().simplifiedRoutes) {
				if (route.getPlatformIndex(platformId) >= 0) {
					final String number = getRouteNumber(pos, route.getColor());
					if (!number.isEmpty()) {
						return number;
					}
				}
			}
		} catch (Exception ignored) {
			// 数据未同步时按空处理
		}
		return "";
	}
}
