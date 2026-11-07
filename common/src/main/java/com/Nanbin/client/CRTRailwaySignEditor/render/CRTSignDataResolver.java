package com.Nanbin.client.CRTRailwaySignEditor.render;

import com.Nanbin.mapping.MtrData;
import com.Nanbin.signage.crt.CRTSignDataContext;
import com.Nanbin.signage.crt.CRTSignLayout;
import mtr.data.Platform;
import mtr.data.Station;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongFunction;

/**
 * {@link CRTSignDataContext} 的客户端实现：把布局里的"数据绑定"解析成 MTR 模组里的真实内容。
 *
 * <p>数据访问全部收敛到 {@link MtrData}（MTR 4.0 {@code SimplifiedRoute}/{@code StationExit}
 * 在 YMTR 3.6.3 没有对应类型，由 MtrData 重建）：</p>
 * <ul>
 *   <li>线路名/颜色：{@link MtrData.RouteView}（完整 Route + 数据缓存重建，MTR 改名/改色后自动生效）；</li>
 *   <li>线路编号：客户端数据没有 MTR 4.0 的 {@code routeNumber} 字段，优先由方块实体从服务端解析后
 *       随 NBT 同步（构造时传入 getter），再用 {@link MtrData#routeNumberByColor(int)} 兜底；</li>
 *   <li>车站名：{@link MtrData#station(long)}（多站 = 换乘站，中/英各拼一行）；</li>
 *   <li>出口：选择的 id 是 {@link MtrData#serializeExit(String)}，用
 *       {@link MtrData#deserializeExit(long)} 还原；目的地列表来自
 *       {@link MtrData#stationExits(Station)}；</li>
 *   <li>站台：{@link MtrData#platformName(long)}；上一站/下一站：{@link MtrData.RouteView}
 *       + {@link MtrData.RouteView#getPlatformIndex(long)}。</li>
 * </ul>
 * 查询结果有 1 秒缓存，避免属性面板/预览高频调用时反复遍历数据。</p>
 */
public final class CRTSignDataResolver implements CRTSignDataContext {

	private static final long CACHE_TTL_MS = 1000L;

	/** 车站出口列表缓存（按车站 id，1 秒）：出口信息条每次布局都要查，避免反复重建列表。 */
	private static final long EXITS_CACHE_TTL_MS = 1000L;
	private static long exitsCacheStationId = Long.MIN_VALUE;
	private static long exitsCacheTime = 0L;
	private static final List<MtrData.StationExit> EXITS_CACHE = new ArrayList<>();

	@Nullable
	private final LongFunction<String> routeNumberGetter;

	/** 指示牌所在车站（出口目的地只在该车站的数据里查）；null = 没有车站上下文。 */
	@Nullable
	private final Station station;

	private long cacheTime = 0L;
	private final List<MtrData.RouteView> routeCache = new ArrayList<>();
	private final List<Station> stationCache = new ArrayList<>();
	private final List<Platform> platformCache = new ArrayList<>();

	/** @param routeNumberGetter 线路颜色 -> 线路编号（来自方块实体 NBT），可为 null */
	public CRTSignDataResolver(@Nullable LongFunction<String> routeNumberGetter) {
		this(routeNumberGetter, null);
	}

	/**
	 * @param routeNumberGetter 线路颜色 -> 线路编号（来自方块实体 NBT），可为 null
	 * @param station           指示牌所在车站（用于出口目的地查询），可为 null
	 */
	public CRTSignDataResolver(@Nullable LongFunction<String> routeNumberGetter, @Nullable Station station) {
		this.routeNumberGetter = routeNumberGetter;
		this.station = station;
	}

	/** 按方块实体构造（铁路牌/墙挂牌的路线编号都来自 NBT）。 */
	public static CRTSignDataResolver forEntity(@Nullable LongFunction<String> routeNumberGetter) {
		return new CRTSignDataResolver(routeNumberGetter);
	}

	// ─── 线路 ────────────────────────────────────────────────────────

	@Nullable
	@Override
	public String getRouteName(long color) {
		final MtrData.RouteView route = findRoute(color);
		if (route == null) {
			return null;
		}
		return MtrData.splitName(route.name(), true);
	}

	@Nullable
	@Override
	public String getRouteNameEn(long color) {
		final MtrData.RouteView route = findRoute(color);
		if (route == null) {
			return null;
		}
		return MtrData.splitName(route.name(), false);
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
		// 兜底 1：完整 Route 的编号字段（MTR 3.6.3 只有轻轨编号）+ 从线路名推导
		final String mtrNumber = MtrData.routeNumberByColor((int) color);
		if (!mtrNumber.isEmpty()) {
			return mtrNumber;
		}
		// 兜底 2：从线路名里取开头的数字/字母（例如「1号线」→「1」）
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
	private MtrData.RouteView findRoute(long color) {
		refreshCacheIfNeeded();
		for (final MtrData.RouteView route : routeCache) {
			if ((route.color() & 0xFFFFFF) == (color & 0xFFFFFF)) {
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
		final StringBuilder zh = new StringBuilder();
		final StringBuilder en = new StringBuilder();
		for (final long stationId : stationIds) {
			final Station station = MtrData.station(stationId);
			if (station == null) {
				continue;
			}
			final String nameZh = MtrData.splitName(station.name, true);
			final String nameEn = MtrData.splitName(station.name, false);
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
			final String name = MtrData.deserializeExit(exitId);
			return name.isEmpty() ? null : name;
		} catch (Exception ignored) {
			return null;
		}
	}

	@Nullable
	@Override
	public String getExitName(long exitId) {
		// 出口名称与「编号」在 MTR 是同一个字段（出口名），
		// 这里保留两个接口是为了让调用方语义清晰（EXIT 取编号 / EXIT_INFO 取名称）。
		return getExitCode(exitId);
	}

	/**
	 * 出口目的地列表：在 {@link MtrData#stationExits(Station)}（3.6.3 的
	 * {@code Station.getGeneratedExits()}）里按 {@link #getExitName(long)} 匹配同名出口，
	 * 取其目的地列表。
	 *
	 * <p>构造时没有传入车站、或车站里匹配不到该出口时返回空列表
	 * （出口信息条据此回退到 EXIT 样式，不会抛异常）。</p>
	 */
	@Override
	public List<String> getExitDestinations(long exitId) {
		if (station == null) {
			return Collections.emptyList();
		}
		try {
			final String name = getExitName(exitId);
			if (name == null || name.isEmpty()) {
				return Collections.emptyList();
			}
			final MtrData.StationExit exit = findStationExit(station, name);
			if (exit == null) {
				return Collections.emptyList();
			}
			final List<String> destinations = exit.destinations();
			if (destinations.isEmpty()) {
				return Collections.emptyList();
			}
			final List<String> result = new ArrayList<>(destinations.size());
			for (final String destination : destinations) {
				if (destination == null || destination.trim().isEmpty()) {
					continue;
				}
				result.add(destination.trim());
			}
			return result;
		} catch (Exception ignored) {
			// MTR 数据接口异常时按空数据处理，渲染端会回退到 EXIT 样式
			return Collections.emptyList();
		}
	}

	/** 在车站出口列表里按名称找出口（列表有 1 秒缓存，出口信息条每次布局都会查）。 */
	@Nullable
	private static MtrData.StationExit findStationExit(Station station, String name) {
		final List<MtrData.StationExit> exits = stationExits(station);
		for (final MtrData.StationExit exit : exits) {
			if (exit != null && name.equals(exit.name())) {
				return exit;
			}
		}
		return null;
	}

	/** 车站出口列表（含缓存；MTR 的 getGeneratedExits 会重建列表，避免每帧重复调用）。 */
	private static List<MtrData.StationExit> stationExits(Station station) {
		final long stationId = station.id;
		final long now = System.currentTimeMillis();
		synchronized (EXITS_CACHE) {
			if (exitsCacheStationId != stationId || now - exitsCacheTime >= EXITS_CACHE_TTL_MS) {
				EXITS_CACHE.clear();
				exitsCacheStationId = stationId;
				exitsCacheTime = now;
				try {
					EXITS_CACHE.addAll(MtrData.stationExits(station));
				} catch (Exception ignored) {
					// 数据未同步完成时按空出口列表处理，下一次调用会重试
				}
			}
			// 返回副本：缓存可能在其它调用里被清空，调用方遍历时不能与写入并发
			return new ArrayList<>(EXITS_CACHE);
		}
	}

	@Nullable
	@Override
	public String getPlatformName(long platformId) {
		refreshCacheIfNeeded();
		final String name = MtrData.platformName(platformId);
		return name == null ? null : MtrData.splitName(name, true);
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
		for (final MtrData.RouteView route : routeCache) {
			final int index = route.getPlatformIndex(platformId);
			if (index < 0) {
				continue;
			}
			final int size = route.platforms().size();
			if (size == 0) {
				return null;
			}
			int target = index + direction;
			if (target < 0) {
				if (!route.isCircular()) {
					return null;
				}
				target = size - 1;
			} else if (target >= size) {
				if (!route.isCircular()) {
					return null;
				}
				target = 0;
			}
			return route.platforms().get(target).stationName();
		}
		return null;
	}

	// ─── 工具 ────────────────────────────────────────────────────────

	/**
	 * MTR 名称的两个约定：{@code ||} 之后是内部复合键（不显示）；
	 * {@code |} 分隔多语言，第 1 段中文、第 2 段英文。
	 */
	public static String splitName(@Nullable String name, boolean primary) {
		return MtrData.splitName(name, primary);
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
			routeCache.addAll(MtrData.allRouteViews());
			stationCache.addAll(MtrData.allStations());
			platformCache.addAll(MtrData.allPlatforms());
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
