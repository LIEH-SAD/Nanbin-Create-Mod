package com.Nanbin.mapping;

import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * MTR 4.0 数据访问语义在本工程（YMTR 3.6.3）的收敛门面。
 *
 * <p>MTR 4.0 移植过来的绘制/编辑代码大量使用 {@code org.mtr.core.data.*} 上在 3.6.3 里
 * 不存在的语义，集中在这里补齐，避免每个渲染类各写一份：</p>
 *
 * <ul>
 *   <li><b>线路编号</b>：MTR 4.0 的 {@code Route.getRouteNumber()} 在 3.6.3 没有对应字段，
 *       退化为 {@code lightRailRouteNumber} → 从线路名推导（{@link #deriveRouteNumberFromName}）
 *       → 去掉「号线／线」后缀（{@link #cleanRouteNumber}）；</li>
 *   <li><b>线路走向（SimplifiedRoute）</b>：3.6.3 没有 {@code SimplifiedRoute} 类，
 *       站台维度用 {@link ClientCache#requestPlatformIdToRoutes(long)}（返回
 *       {@code PlatformRouteDetails}），线路维度用 {@code ClientData.ROUTES} +
 *       {@code Route.platformIds} + 数据缓存里的站台/车站表重建；</li>
 *   <li><b>车站出口</b>：3.6.3 没有 {@code StationExit} 类，用
 *       {@code Station.getGeneratedExits()}（出口名 → 目的地列表）+ {@code Station.exits}
 *       （真实出口名集合）还原；编号复用 {@code Station.serializeExit/deserializeExit}。</li>
 *
 * <p><b>注意</b>：本类全部方法都在客户端数据 {@link ClientData#DATA_CACHE} 上工作，
 * 只能在客户端调用；数据未同步完成时一律返回空/null，调用方需按缺失处理（渲染回退字面文本）。</p>
 *
 * <p>数据来源与语义见 docs/MTR_DATA_BINDING.md（来源工程 docs 同名文档）。</p>
 */
public final class MtrData {

	private MtrData() {
	}

	// ═══════════════════════════════════════════════════════════════
	// 数据模型（镜像 MTR 4.0 的类型）
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 镜像 MTR 4.0 {@code StationExit}：出口名（如 "A" / "A1" / "B2"）+ 目的地列表。
	 * MTR 3.6.3 的出口没有独立类型，只有 {@code Map<出口名, 目的地列表>}。
	 */
	public record StationExit(String name, List<String> destinations) {

		public StationExit {
			name = name == null ? "" : name;
			destinations = destinations == null ? Collections.emptyList() : List.copyOf(destinations);
		}
	}

	/** 镜像 MTR 4.0 {@code ClientCache.ColorNameTuple}：换乘线路的颜色 + 名称。 */
	public record ColorName(int color, String name) {

		public ColorName {
			color = color & 0xFFFFFF;
			name = name == null ? "" : name;
		}
	}

	/** 镜像 MTR 4.0 {@code PlatformRouteDetails.StationDetails}：线路图上一个车站及其换乘线路。 */
	public record StationDetail(String stationName, List<ColorName> interchangeRoutes) {

		public StationDetail {
			stationName = stationName == null ? "" : stationName;
			interchangeRoutes = interchangeRoutes == null ? Collections.emptyList() : List.copyOf(interchangeRoutes);
		}
	}

	/**
	 * 镜像 MTR 4.0 {@code SimplifiedRoute}（站台视角的线路走向信息），字段与
	 * {@code ClientCache.PlatformRouteDetails} 一一对应。
	 *
	 * @param routeName           线路名（可能含 {@code |} 中英分隔）
	 * @param routeColor          线路颜色（RGB，无 alpha）
	 * @param circularState       环线状态
	 * @param currentStationIndex 本站在该走向中的下标（0 = 首站；-1 = 未知）
	 * @param stationDetails      该走向按行车顺序的全部车站
	 */
	public record PlatformRoute(String routeName, int routeColor, Route.CircularState circularState,
			int currentStationIndex, List<StationDetail> stationDetails) {

		public PlatformRoute {
			routeName = routeName == null ? "" : routeName;
			routeColor = routeColor & 0xFFFFFF;
			stationDetails = stationDetails == null ? Collections.emptyList() : List.copyOf(stationDetails);
		}

		/** 是否环线（MTR 的线路走向标了 clockwise / anticlockwise）。 */
		public boolean isCircular() {
			return circularState != null && circularState != Route.CircularState.NONE;
		}
	}

	/** 一条线路走向上的一个站台（{@code Route.platformIds} 的一项 + 解析出的车站）。 */
	public record RoutePlatformView(long platformId, long stationId, String stationName,
			List<ColorName> interchangeRoutes) {

		public RoutePlatformView {
			stationName = stationName == null ? "" : stationName;
			interchangeRoutes = interchangeRoutes == null ? Collections.emptyList() : List.copyOf(interchangeRoutes);
		}
	}

	/**
	 * 从 3.6.3 的完整 {@code Route} 重建出的线路走向（镜像 MTR 4.0 {@code SimplifiedRoute} 的
	 * 线路视角：有序站台序列 + 环线状态），供线路图 / 上下站查询使用。
	 */
	public record RouteView(long routeId, String name, int color, Route.CircularState circularState,
			List<RoutePlatformView> platforms) {

		public RouteView {
			name = name == null ? "" : name;
			color = color & 0xFFFFFF;
			platforms = platforms == null ? Collections.emptyList() : List.copyOf(platforms);
		}

		/** 该走向里包含的站台 id 列表（按行车顺序）。 */
		public List<Long> platformIds() {
			final List<Long> ids = new ArrayList<>(platforms.size());
			for (final RoutePlatformView platform : platforms) {
				ids.add(platform.platformId());
			}
			return ids;
		}

		/** 该站台在本走向中的下标；不存在返回 -1（等价 MTR 4.0 {@code SimplifiedRoute#getPlatformIndex}）。 */
		public int getPlatformIndex(long platformId) {
			for (int i = 0; i < platforms.size(); i++) {
				if (platforms.get(i).platformId() == platformId) {
					return i;
				}
			}
			return -1;
		}

		/** 是否环线。 */
		public boolean isCircular() {
			return circularState != null && circularState != Route.CircularState.NONE;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// 线路基础查询
	// ═══════════════════════════════════════════════════════════════

	/** 全部线路（{@code ClientData.ROUTES} 的副本；数据未同步时为空列表）。 */
	public static List<Route> allRoutes() {
		try {
			return new ArrayList<>(ClientData.ROUTES);
		} catch (Throwable ignored) {
			return Collections.emptyList();
		}
	}

	/** 按线路颜色（RGB / ARGB 均可）找完整线路；找不到返回 null。 */
	@Nullable
	public static Route routeByColor(int color) {
		final int rgb = color & 0xFFFFFF;
		try {
			for (final Route route : ClientData.ROUTES) {
				if ((route.color & 0xFFFFFF) == rgb) {
					return route;
				}
			}
			// ROUTES 与数据缓存不同步时（sync 重建期间）再查 routeIdMap 兜底
			for (final Route route : ClientData.DATA_CACHE.routeIdMap.values()) {
				if (route != null && (route.color & 0xFFFFFF) == rgb) {
					return route;
				}
			}
		} catch (Throwable ignored) {
			// 数据尚未同步完成
		}
		return null;
	}

	/** 按线路 id 找完整线路；找不到返回 null。 */
	@Nullable
	public static Route routeById(long routeId) {
		try {
			final Route route = ClientData.DATA_CACHE.routeIdMap.get(routeId);
			if (route != null) {
				return route;
			}
			for (final Route candidate : ClientData.ROUTES) {
				if (candidate.id == routeId) {
					return candidate;
				}
			}
		} catch (Throwable ignored) {
			// 数据尚未同步完成
		}
		return null;
	}

	/** 按站台 id 找站台；找不到返回 null。 */
	@Nullable
	public static Platform platform(long platformId) {
		if (platformId == 0L) {
			return null;
		}
		try {
			return ClientData.DATA_CACHE.platformIdMap.get(platformId);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 按车站 id 找车站；找不到返回 null。 */
	@Nullable
	public static Station station(long stationId) {
		if (stationId == 0L) {
			return null;
		}
		try {
			return ClientData.DATA_CACHE.stationIdMap.get(stationId);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 全部车站（{@code stationIdMap} 的副本；数据未同步时为空列表）。 */
	public static List<Station> allStations() {
		try {
			return new ArrayList<>(ClientData.DATA_CACHE.stationIdMap.values());
		} catch (Throwable ignored) {
			return Collections.emptyList();
		}
	}

	/** 全部站台（{@code platformIdMap} 的副本；数据未同步时为空列表）。 */
	public static List<Platform> allPlatforms() {
		try {
			return new ArrayList<>(ClientData.DATA_CACHE.platformIdMap.values());
		} catch (Throwable ignored) {
			return Collections.emptyList();
		}
	}

	/** 站台所属车站；找不到返回 null。 */
	@Nullable
	public static Station stationForPlatform(long platformId) {
		if (platformId == 0L) {
			return null;
		}
		try {
			return ClientData.DATA_CACHE.platformIdToStation.get(platformId);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 站台名（如 "1" / "2A"）；找不到返回 null。 */
	@Nullable
	public static String platformName(long platformId) {
		final Platform platform = platform(platformId);
		return platform == null || platform.name == null || platform.name.isEmpty() ? null : platform.name;
	}

	// ═══════════════════════════════════════════════════════════════
	// 线路走向（SimplifiedRoute 等价物）
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 经过某站台的全部线路走向（MTR 4.0 {@code ClientData.simplifiedRoutes} 按站台过滤后的等价物）。
	 *
	 * <p>数据源 = {@link ClientCache#requestPlatformIdToRoutes(long)}；返回值里
	 * {@code currentStationIndex} 就是该站台在本走向中的下标。</p>
	 */
	public static List<PlatformRoute> routesForPlatform(long platformId) {
		if (platformId == 0L) {
			return Collections.emptyList();
		}
		final List<PlatformRoute> result = new ArrayList<>();
		try {
			final List<ClientCache.PlatformRouteDetails> details = ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId);
			if (details == null) {
				return Collections.emptyList();
			}
			for (final ClientCache.PlatformRouteDetails detail : details) {
				if (detail == null) {
					continue;
				}
				final List<StationDetail> stations = new ArrayList<>();
				if (detail.stationDetails != null) {
					for (final ClientCache.PlatformRouteDetails.StationDetails stationDetail : detail.stationDetails) {
						if (stationDetail == null) {
							continue;
						}
						final List<ColorName> interchanges = new ArrayList<>();
						if (stationDetail.interchangeRoutes != null) {
							for (final ClientCache.ColorNameTuple tuple : stationDetail.interchangeRoutes) {
								if (tuple != null) {
									interchanges.add(new ColorName(tuple.color, tuple.name));
								}
							}
						}
						stations.add(new StationDetail(stationDetail.stationName, interchanges));
					}
				}
				result.add(new PlatformRoute(detail.routeName, detail.routeColor, detail.circularState,
						detail.currentStationIndex, stations));
			}
		} catch (Throwable ignored) {
			// 客户端数据尚未同步完成时按空数据处理
		}
		return result;
	}

	/**
	 * 全部线路走向（从 3.6.3 的完整 {@link Route} 重建）：{@code Route.platformIds} 决定站序，
	 * 站名/换乘线路从数据缓存的 {@code platformIdMap} / {@code platformIdToStation} /
	 * {@code stationIdToRoutes} 补齐。
	 */
	public static List<RouteView> allRouteViews() {
		final List<RouteView> views = new ArrayList<>();
		for (final Route route : allRoutes()) {
			final RouteView view = routeView(route);
			if (view != null) {
				views.add(view);
			}
		}
		return views;
	}

	/** 经过某站台的全部线路走向（线路视角，等价 MTR 4.0 里 {@code getPlatformIndex(platformId) >= 0} 的筛子）。 */
	public static List<RouteView> routeViewsForPlatform(long platformId) {
		if (platformId == 0L) {
			return Collections.emptyList();
		}
		final List<RouteView> views = new ArrayList<>();
		for (final RouteView view : allRouteViews()) {
			if (view.getPlatformIndex(platformId) >= 0) {
				views.add(view);
			}
		}
		return views;
	}

	/** 按线路颜色找线路走向；找不到返回 null。 */
	@Nullable
	public static RouteView routeViewByColor(int color) {
		final Route route = routeByColor(color);
		return route == null ? null : routeView(route);
	}

	/** 把完整 {@link Route} 重建成 {@link RouteView}；route 为 null 时返回 null。 */
	@Nullable
	public static RouteView routeView(@Nullable Route route) {
		if (route == null) {
			return null;
		}
		try {
			final List<RoutePlatformView> platforms = new ArrayList<>();
			final ClientCache cache = ClientData.DATA_CACHE;
			for (final Route.RoutePlatform routePlatform : route.platformIds) {
				if (routePlatform == null) {
					continue;
				}
				final long platformId = routePlatform.platformId;
				final Platform platform = cache.platformIdMap.get(platformId);
				final Station station = cache.platformIdToStation.get(platformId);
				final long stationId = station != null ? station.id : 0L;
				final String stationName = station != null && station.name != null && !station.name.isEmpty()
						? station.name
						: platform != null && platform.name != null ? platform.name : "";
				final List<ColorName> interchanges = new ArrayList<>();
				if (stationId != 0L) {
					final Map<Integer, ClientCache.ColorNameTuple> routesAtStation = cache.stationIdToRoutes.get(stationId);
					if (routesAtStation != null) {
						for (final Map.Entry<Integer, ClientCache.ColorNameTuple> entry : routesAtStation.entrySet()) {
							if (entry.getValue() != null) {
								interchanges.add(new ColorName(entry.getValue().color, entry.getValue().name));
							}
						}
					}
				}
				platforms.add(new RoutePlatformView(platformId, stationId, stationName, interchanges));
			}
			return new RouteView(route.id, route.name, route.color, route.circularState, platforms);
		} catch (Throwable ignored) {
			// 数据尚未同步完成
			return null;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// 线路编号
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 线路编号（MTR 4.0 {@code Route.getRouteNumber()} 的等价物）。
	 *
	 * <p>3.6.3 没有 {@code routeNumber} 字段，回退顺序：</p>
	 * <ol>
	 *   <li>{@code route.lightRailRouteNumber}（3.6.3 里唯一的编号字段，轻轨线路用）；</li>
	 *   <li>从 {@code route.name} 按 MTR 名称规则推导（{@link #deriveRouteNumberFromName}）；</li>
	 *   <li>去掉「号线／线」后缀（{@link #cleanRouteNumber}）。</li>
	 * </ol>
	 *
	 * <p>返回空串表示未知（不会返回 null，方便直接拼文本）。</p>
	 */
	public static String routeNumber(@Nullable Route route) {
		if (route == null) {
			return "";
		}
		final String lightRailNumber = route.lightRailRouteNumber;
		if (lightRailNumber != null && !lightRailNumber.trim().isEmpty()) {
			return cleanRouteNumber(lightRailNumber);
		}
		return cleanRouteNumber(deriveRouteNumberFromName(route.name));
	}

	/** 按线路颜色取线路编号（等价 MTR 4.0 里 {@code findRoute(color).getRouteNumber()}）；未知返回空串。 */
	public static String routeNumberByColor(int color) {
		return routeNumber(routeByColor(color));
	}

	/**
	 * 按 MTR 名称规则从线路名推导编号：先截 {@code ||}（隐藏的复合键），
	 * 再取 {@code |} 前的中文段。与来源工程 {@code RouteMapGenerator.deriveRouteNumberFromName} 逐字一致。
	 */
	public static String deriveRouteNumberFromName(@Nullable String routeName) {
		if (routeName == null || routeName.isEmpty()) {
			return "";
		}
		// MTR 规则：|| 之后的内容隐藏，只取第一部分
		final String primary = routeName.split("\\|\\|", -1)[0];
		final int separatorIndex = primary.indexOf('|');
		final String mainSegment = separatorIndex >= 0 ? primary.substring(0, separatorIndex) : primary;
		return mainSegment.trim();
	}

	/**
	 * 圆徽/编号里只放纯线路编号：MTR 的编号字段为空时会退回线路名（如「8线」「1号线」），
	 * 这里剥掉「号线／线」后缀。与来源工程 {@code RouteMapGenerator.cleanRouteNumber} 逐字一致。
	 */
	public static String cleanRouteNumber(@Nullable String routeNumber) {
		if (routeNumber == null) {
			return "";
		}
		final String trimmed = routeNumber.trim();
		if (trimmed.endsWith("号线")) {
			return trimmed.substring(0, trimmed.length() - 2);
		}
		return trimmed.length() > 1 && trimmed.endsWith("线") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	// ═══════════════════════════════════════════════════════════════
	// 车站出口
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 车站出口列表（MTR 4.0 {@code Station.getExits()} 的等价物：出口名 + 目的地列表）。
	 *
	 * <p>3.6.3 的数据结构是 {@code Station.exits}（出口名 → 目的地列表）与
	 * {@link Station#getGeneratedExits()}（在真实出口之外额外生成「单字母父出口」汇总项，
	 * 父项的目的地是子出口目的地的并集）。这里用 {@code getGeneratedExits()} 取目的地，
	 * 但只用 {@code Station.exits} 里的真实出口名（过滤掉父字母汇总项，与 MTR 4.0
	 * {@code getStationExits(station, false)} 语义一致），按名称排序。</p>
	 */
	public static List<StationExit> stationExits(@Nullable Station station) {
		if (station == null) {
			return Collections.emptyList();
		}
		final Map<String, List<String>> generated;
		try {
			generated = station.getGeneratedExits();
		} catch (Throwable ignored) {
			return Collections.emptyList();
		}
		if (generated == null || generated.isEmpty()) {
			return Collections.emptyList();
		}
		final List<StationExit> result = new ArrayList<>();
		final Map<String, List<String>> rawExits = station.exits;
		if (rawExits != null && !rawExits.isEmpty()) {
			// 只用真实出口名（getGeneratedExits 里同时含单字母父出口与真实出口）
			for (final Map.Entry<String, List<String>> entry : generated.entrySet()) {
				if (entry.getKey() == null || !rawExits.containsKey(entry.getKey())) {
					continue;
				}
				final List<String> destinations = entry.getValue() == null
						? Collections.emptyList() : new ArrayList<>(entry.getValue());
				result.add(new StationExit(entry.getKey(), destinations));
			}
		}
		if (result.isEmpty()) {
			// 极端兜底：generated 的键集合与 exits 不同步时直接用完整出口表
			for (final Map.Entry<String, List<String>> entry : generated.entrySet()) {
				if (entry.getKey() == null) {
					continue;
				}
				final List<String> destinations = entry.getValue() == null
						? Collections.emptyList() : new ArrayList<>(entry.getValue());
				result.add(new StationExit(entry.getKey(), destinations));
			}
		}
		result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return result;
	}

	/** 在车站出口列表里按名称找出口；找不到返回 null。 */
	@Nullable
	public static StationExit findStationExit(@Nullable Station station, @Nullable String exitName) {
		if (station == null || exitName == null || exitName.isEmpty()) {
			return null;
		}
		for (final StationExit exit : stationExits(station)) {
			if (exitName.equals(exit.name())) {
				return exit;
			}
		}
		return null;
	}

	/** 出口名 → 出口 id（复用 MTR 的 {@link Station#serializeExit(String)}；名称非法时返回 0）。 */
	public static long serializeExit(@Nullable String exitName) {
		if (exitName == null || exitName.isEmpty()) {
			return 0L;
		}
		try {
			return Station.serializeExit(exitName);
		} catch (Throwable ignored) {
			return 0L;
		}
	}

	/** 出口 id → 出口名（复用 MTR 的 {@link Station#deserializeExit(long)}；未知返回空串）。 */
	public static String deserializeExit(long exitId) {
		try {
			final String name = Station.deserializeExit(exitId);
			return name == null ? "" : name;
		} catch (Throwable ignored) {
			return "";
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// 工具
	// ═══════════════════════════════════════════════════════════════

	/**
	 * 合并多座车站的站名（中/英各一行，多站用 " / " 连接），等价
	 * {@code CRTSignDataResolver.getStationNames} 的取值语义但纯数据化；
	 * 取不到数据时返回空列表。
	 */
	public static List<String> stationNames(List<Long> stationIds) {
		if (stationIds == null || stationIds.isEmpty()) {
			return Collections.emptyList();
		}
		final StringBuilder zh = new StringBuilder();
		final StringBuilder en = new StringBuilder();
		final LinkedHashSet<Long> seen = new LinkedHashSet<>();
		for (final Long stationId : stationIds) {
			if (stationId == null || !seen.add(stationId)) {
				continue;
			}
			final Station station = station(stationId);
			if (station == null) {
				continue;
			}
			appendSeparated(zh, splitName(station.name, true));
			appendSeparated(en, splitName(station.name, false));
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

	/**
	 * MTR 名称的两个约定：{@code ||} 之后是内部复合键（不显示）；
	 * {@code |} 分隔多语言，第 1 段中文、第 2 段英文。与来源工程
	 * {@code CRTSignDataResolver.splitName} 逐字一致。
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

	private static void appendSeparated(StringBuilder builder, @Nullable String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(" / ");
		}
		builder.append(value);
	}
}
