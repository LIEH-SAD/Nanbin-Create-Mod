package com.Nanbin.client.signage;

import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignLayout;
import org.mtr.core.data.ClientData;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.EditStationScreen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongFunction;

/**
 * {@link CRTSignDataContext} 的客户端实现：把布局里的"数据绑定"解析成 MTR 模组里的真实内容。
 *
 * <p>数据来源与注意事项（详见 docs/MTR_DATA_BINDING.md）：
 * <ul>
 *   <li>线路名/颜色：{@code ClientData.simplifiedRoutes}（客户端数据，MTR 改名/改色后自动生效）；</li>
 *   <li>线路编号：客户端数据没有，必须由方块实体从服务端解析后随 NBT 同步（构造时传入 getter）；</li>
 *   <li>车站名：{@code MinecraftClientData.stationIdMap}（多站 = 换乘站，中/英各拼一行）；</li>
 *   <li>出口：选择的 id 是 {@code EditStationScreen.serializeExit(name)}，用 {@code deserializeExit} 还原；</li>
 *   <li>站台：{@code platformIdMap}；上一站/下一站：{@code simplifiedRoutes} + {@code getPlatformIndex}。</li>
 * </ul>
 * 查询结果有 1 秒缓存，避免属性面板/预览高频调用时反复遍历数据。</p>
 */
public final class CRTSignDataResolver implements CRTSignDataContext {

	private static final long CACHE_TTL_MS = 1000L;

	@Nullable
	private final LongFunction<String> routeNumberGetter;

	private long cacheTime = 0L;
	private final List<SimplifiedRoute> routeCache = new ArrayList<>();
	private final List<Station> stationCache = new ArrayList<>();
	private final List<Platform> platformCache = new ArrayList<>();

	/** @param routeNumberGetter 线路颜色 -> 线路编号（来自方块实体 NBT），可为 null */
	public CRTSignDataResolver(@Nullable LongFunction<String> routeNumberGetter) {
		this.routeNumberGetter = routeNumberGetter;
	}

	/** 按方块实体构造（铁路牌/墙挂牌的路线编号都来自 NBT）。 */
	public static CRTSignDataResolver forEntity(@Nullable LongFunction<String> routeNumberGetter) {
		return new CRTSignDataResolver(routeNumberGetter);
	}

	// ─── 线路 ────────────────────────────────────────────────────────

	@Nullable
	@Override
	public String getRouteName(long color) {
		final SimplifiedRoute route = findRoute(color);
		if (route == null) {
			return null;
		}
		return splitName(route.getName(), true);
	}

	@Nullable
	@Override
	public String getRouteNameEn(long color) {
		final SimplifiedRoute route = findRoute(color);
		if (route == null) {
			return null;
		}
		return splitName(route.getName(), false);
	}

	@Nullable
	@Override
	public String getRouteNumber(long color) {
		if (routeNumberGetter != null) {
			final String number = routeNumberGetter.apply(color);
			if (number != null && !number.isEmpty()) {
				return number;
			}
		}
		// 兜底：从线路名里取开头的数字/字母（例如「1号线」→「1」）
		final String name = getRouteName(color);
		if (name != null) {
			final StringBuilder builder = new StringBuilder();
			for (final char character : name.toCharArray()) {
				if (Character.isLetterOrDigit(character) || character == '-') {
					builder.append(character);
				} else {
					break;
				}
			}
			if (builder.length() > 0) {
				return builder.toString();
			}
		}
		return null;
	}

	/** 线路颜色是否存在于当前 MTR 数据（属性面板显示状态用）。 */
	public boolean hasRoute(long color) {
		return findRoute(color) != null;
	}

	@Nullable
	private SimplifiedRoute findRoute(long color) {
		refreshCacheIfNeeded();
		for (final SimplifiedRoute route : routeCache) {
			if ((long) route.getColor() == color) {
				return route;
			}
		}
		return null;
	}

	// ─── 车站 ────────────────────────────────────────────────────────

	@Override
	public List<String> getStationNames(List<Long> stationIds) {
		if (stationIds == null || stationIds.isEmpty()) {
			return Collections.emptyList();
		}
		refreshCacheIfNeeded();
		final StringBuilder zh = new StringBuilder();
		final StringBuilder en = new StringBuilder();
		for (final long stationId : stationIds) {
			final Station station = stationCache.stream().filter(s -> s.getId() == stationId).findFirst().orElse(null);
			if (station == null) {
				continue;
			}
			final String nameZh = splitName(station.getName(), true);
			final String nameEn = splitName(station.getName(), false);
			appendSeparated(zh, nameZh);
			appendSeparated(en, nameEn);
		}
		if (zh.length() == 0 && en.length() == 0) {
			return Collections.emptyList();
		}
		final List<String> result = new ArrayList<>();
		result.add(zh.toString());
		if (en.length() > 0) {
			result.add(en.toString());
		}
		return result;
	}

	// ─── 出口 / 站台 ─────────────────────────────────────────────────

	@Nullable
	@Override
	public String getExitCode(long exitId) {
		try {
			final String name = EditStationScreen.deserializeExit(exitId);
			return name == null || name.isEmpty() ? null : name;
		} catch (Exception ignored) {
			return null;
		}
	}

	@Nullable
	@Override
	public String getPlatformName(long platformId) {
		refreshCacheIfNeeded();
		for (final Platform platform : platformCache) {
			if (platform.getId() == platformId) {
				return splitName(platform.getName(), true);
			}
		}
		return null;
	}

	// ─── 下一站 / 上一站 ─────────────────────────────────────────────

	@Nullable
	@Override
	public String getNextStation(long platformId) {
		return neighbourStation(platformId, 1);
	}

	@Nullable
	@Override
	public String getPrevStation(long platformId) {
		return neighbourStation(platformId, -1);
	}

	/** @param direction 1 = 下一站，-1 = 上一站（环线自动首尾相接） */
	@Nullable
	private String neighbourStation(long platformId, int direction) {
		refreshCacheIfNeeded();
		for (final SimplifiedRoute route : routeCache) {
			final int index = route.getPlatformIndex(platformId);
			if (index < 0) {
				continue;
			}
			final int size = route.getPlatforms().size();
			if (size == 0) {
				return null;
			}
			int target = index + direction;
			if (target < 0) {
				if (route.getCircularState() == Route.CircularState.NONE) {
					return null;
				}
				target = size - 1;
			} else if (target >= size) {
				if (route.getCircularState() == Route.CircularState.NONE) {
					return null;
				}
				target = 0;
			}
			return route.getPlatforms().get(target).getStationName();
		}
		return null;
	}

	// ─── 工具 ────────────────────────────────────────────────────────

	/**
	 * MTR 名称的两个约定：{@code ||} 之后是内部复合键（不显示）；
	 * {@code |} 分隔多语言，第 1 段中文、第 2 段英文。
	 */
	public static String splitName(@Nullable String name, boolean primary) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		final int hideIndex = name.indexOf("||");
		final String visible = hideIndex >= 0 ? name.substring(0, hideIndex) : name;
		final String[] parts = visible.split("\\|", -1);
		final String value = primary || parts.length < 2 ? parts[0] : parts[1];
		return value == null ? "" : value.trim();
	}

	private static void appendSeparated(StringBuilder builder, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(" / ");
		}
		builder.append(value);
	}

	private void refreshCacheIfNeeded() {
		final long now = System.currentTimeMillis();
		if (now - cacheTime < CACHE_TTL_MS && !routeCache.isEmpty()) {
			return;
		}
		cacheTime = now;
		routeCache.clear();
		stationCache.clear();
		platformCache.clear();
		try {
			final MinecraftClientData data = MinecraftClientData.getInstance();
			if (data == null) {
				return;
			}
			final ClientData clientData = data;
			routeCache.addAll(clientData.simplifiedRoutes);
			stationCache.addAll(data.stationIdMap.values());
			platformCache.addAll(data.platformIdMap.values());
		} catch (Exception ignored) {
			// 客户端数据尚未同步完成时按空数据处理，下一次调用会重试
		}
	}

	// ─── 供编辑界面显示"当前解析值"的便捷方法 ─────────────────────────

	/** 把元素当前绑定解析成一行可读文本（属性面板显示用）。 */
	public String describeBinding(com.Nanbin.signage.crt.CRTSignElement element) {
		if (element == null || element.binding == com.Nanbin.signage.crt.CRTSignElement.DataBinding.NONE) {
			return "";
		}
		switch (element.binding) {
			case ROUTE_NAME:
				return element.bindingIds.isEmpty() ? "" : nullToEmpty(getRouteName(element.bindingIds.get(0)));
			case ROUTE_NUMBER:
				return element.bindingIds.isEmpty() ? "" : nullToEmpty(getRouteNumber(element.bindingIds.get(0)));
			case ROUTE_LINES: {
				final StringBuilder builder = new StringBuilder();
				for (final long color : element.bindingIds) {
					if (builder.length() > 0) {
						builder.append(" / ");
					}
					builder.append(nullToEmpty(getRouteNumber(color)));
				}
				return builder.toString();
			}
			case STATION: {
				final List<String> names = getStationNames(element.bindingIds);
				return names.isEmpty() ? "" : names.get(0);
			}
			case EXIT:
				return element.bindingIds.isEmpty() ? "" : nullToEmpty(getExitCode(element.bindingIds.get(0)));
			case PLATFORM:
				return element.bindingIds.isEmpty() ? "" : nullToEmpty(getPlatformName(element.bindingIds.get(0)));
			case NEXT_STATION:
				return element.bindingIds.isEmpty() ? "" : nullToEmpty(getNextStation(element.bindingIds.get(0)));
			case PREV_STATION:
				return element.bindingIds.isEmpty() ? "" : nullToEmpty(getPrevStation(element.bindingIds.get(0)));
			default:
				return "";
		}
	}

	/** 便捷：布局里所有绑定 id 的线路颜色集合（用于选择器回显，预留）。 */
	public CRTSignLayout unusedLayoutReference() {
		return null;
	}

	private static String nullToEmpty(@Nullable String value) {
		return value == null ? "" : value;
	}
}
