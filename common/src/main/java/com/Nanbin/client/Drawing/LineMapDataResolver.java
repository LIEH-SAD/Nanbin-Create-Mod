package com.Nanbin.client.Drawing;

import com.Nanbin.mapping.MtrData;
import com.Nanbin.Init;
import mtr.data.Platform;
import mtr.data.Route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把 MTR 的客户端线路数据解析成 {@link LineMapData}（吊板线路图用）。
 * 本站与其它站的车站编号都取站台号码（{@code Platform.getName()}），
 * 换乘线路取自 MTR 为每个线路车站维护的换乘颜色表并排除本线。
 * MTR 里一条线路可以有多条走向（MTR 4.0 的 {@code SimplifiedRoute}，本工程由
 * {@link MtrData.RouteView} 重建）：主线取经过本站台的第一条，其余与主线共用过一段、
 * 之后才分开的走向作为支线，交给渲染器画成第二条线路带；
 * 只在本站与主线相交的换乘线路不算支线（仍由换乘支线圆徽表示）。
 */
public final class LineMapDataResolver {

	private LineMapDataResolver() {
	}

	/**
	 * 解析指定站台所在线路的整条线路数据（含支线）。
	 *
	 * @return 站台不存在或不在任何线路上时返回 null
	 */
	public static LineMapData resolve(long platformId) {
		if (platformId == 0) {
			return null;
		}
		try {
			final Platform platform = MtrData.platform(platformId);
			if (platform == null) {
				return null;
			}

			// 经过本站台的所有走向：车站最多的那条当主线（支线通常比干线短），并列时取第一条
			final List<MtrData.RouteView> candidates = MtrData.routeViewsForPlatform(platformId);
			MtrData.RouteView mainRoute = null;
			for (final MtrData.RouteView route : candidates) {
				if (mainRoute == null || route.platforms().size() > mainRoute.platforms().size()) {
					mainRoute = route;
				}
			}
			if (mainRoute == null || candidates.isEmpty()) {
				return null;
			}
			final int currentIndex = mainRoute.getPlatformIndex(platformId);
			final List<Long> mainStationIds = new ArrayList<>();
			for (final MtrData.RoutePlatformView routePlatform : mainRoute.platforms()) {
				mainStationIds.add(routePlatform.stationId());
			}
			final List<LineMapData.Station> stations = buildStations(mainRoute);

			return new LineMapData(mainRoute.color(), resolvePlatformLineNumber(platformId, mainRoute), currentIndex, stations,
					resolveBranches(mainRoute, mainStationIds, candidates),
					mainRoute.isCircular());
		} catch (Exception e) {
			Init.LOGGER.error("LineMapDataResolver: failed to resolve line map for platform {}", platformId, e);
		}
		return null;
	}

	/**
	 * 除主线之外的走向：同一条线路的另一分支（同一个 Route、同色或同号），
	 * 或与主线共用过一段（≥2 个车站）的走向，都算支线；只在单一车站与主线相交的换乘线路不算。
	 */
	private static List<LineMapData.Branch> resolveBranches(MtrData.RouteView mainRoute, List<Long> mainStationIds,
			List<MtrData.RouteView> candidates) {
		final List<LineMapData.Branch> branches = new ArrayList<>();
		final Set<String> keys = new HashSet<>();
		for (final MtrData.RouteView route : candidates) {
			if (route.routeId() == mainRoute.routeId()) {
				continue;
			}
			final List<MtrData.RoutePlatformView> routePlatforms = route.platforms();

			// 主线上的分叉站 = 最后一条与本线共用的车站；它在本走向里的位置决定支线从哪接出去
			int forkIndex = -1;
			int forkRouteIndex = -1;
			int commonCount = 0;
			for (int j = 0; j < routePlatforms.size(); j++) {
				final int mainIndex = mainStationIds.indexOf(routePlatforms.get(j).stationId());
				if (mainIndex >= 0) {
					commonCount++;
					if (mainIndex > forkIndex || mainIndex == forkIndex && j > forkRouteIndex) {
						forkIndex = mainIndex;
						forkRouteIndex = j;
					}
				}
			}
			if (forkIndex < 0 || forkRouteIndex < 0) {
				continue;
			}
			// 「支线」通常就是同一条线路的同色同号走向；只共用一个车站的异色异号走向是换乘，交给换乘支线圆徽
			final boolean sameRoute = route.routeId() == mainRoute.routeId() || route.color() == mainRoute.color();
			if (!sameRoute && commonCount < 2) {
				continue;
			}

			// 支线专属车站（与主线不重复的车站），按它们在本走向里位于分叉站之前／之后分成两侧
			final List<LineMapData.Station> afterStations = new ArrayList<>();
			final List<LineMapData.Station> beforeStations = new ArrayList<>();
			for (int j = 0; j < routePlatforms.size(); j++) {
				final MtrData.RoutePlatformView routePlatform = routePlatforms.get(j);
				if (mainStationIds.contains(routePlatform.stationId())) {
					continue;
				}
				(j > forkRouteIndex ? afterStations : beforeStations).add(toStation(routePlatform, j, route.color()));
			}
			if (!afterStations.isEmpty() || !beforeStations.isEmpty()) {
				final String key = branchKey(forkIndex, afterStations, beforeStations);
				if (keys.add(key)) {
					branches.add(new LineMapData.Branch(forkIndex, route.color(), resolveLineNumber(route),
							afterStations, beforeStations));
				}
			}
		}
		return branches;
	}

	/** 去重键：多条走向可能给出完全相同的支线（分叉站 + 车站序列）。 */
	private static String branchKey(int forkIndex, List<LineMapData.Station> afterStations, List<LineMapData.Station> beforeStations) {
		final StringBuilder builder = new StringBuilder().append(forkIndex);
		for (final LineMapData.Station station : afterStations) {
			builder.append("|A").append(station.name()).append('~').append(station.number());
		}
		for (final LineMapData.Station station : beforeStations) {
			builder.append("|B").append(station.name()).append('~').append(station.number());
		}
		return builder.toString();
	}

	private static List<LineMapData.Station> buildStations(MtrData.RouteView route) {
		final List<LineMapData.Station> stations = new ArrayList<>();
		final List<MtrData.RoutePlatformView> routePlatforms = route.platforms();
		for (int i = 0; i < routePlatforms.size(); i++) {
			stations.add(toStation(routePlatforms.get(i), i, route.color()));
		}
		return stations;
	}

	private static LineMapData.Station toStation(MtrData.RoutePlatformView routePlatform, int routeIndex, int lineColor) {
		final String rawNumber = MtrData.platformName(routePlatform.platformId());
		final String stationNumber = rawNumber == null ? "" : rawNumber.trim();
		// 站台没填号码时用在该走向上的序号补齐（01、02…），站圆不留空白
		final String number = stationNumber.isEmpty() ? String.format("%02d", routeIndex + 1) : stationNumber;
		return new LineMapData.Station(routePlatform.stationName(), number, resolveInterchanges(routePlatform, lineColor));
	}

	/** 换乘线路：本站除本线以外的其它线路颜色与编号。 */
	private static List<LineMapData.Interchange> resolveInterchanges(MtrData.RoutePlatformView routePlatform, int lineColor) {
		final List<LineMapData.Interchange> interchanges = new ArrayList<>();
		for (final MtrData.ColorName interchangeRoute : routePlatform.interchangeRoutes()) {
			final int color = interchangeRoute.color();
			if ((color & 0xFFFFFF) == (lineColor & 0xFFFFFF)) {
				continue;
			}
			final String number = interchangeNumber(color, interchangeRoute.name());
			if (number.isEmpty()) {
				continue;
			}
			interchanges.add(new LineMapData.Interchange(color, number));
		}
		return interchanges;
	}

	/**
	 * 换乘线路编号：优先按换乘颜色匹配到真实 Route 的编号字段，其次退回线路名并剥掉「号线／线」。
	 * 中文编号（如「环线」）原样保留。
	 */
	private static String interchangeNumber(int color, String name) {
		final Route route = MtrData.routeByColor(color);
		if (route != null) {
			final String realNumber = MtrData.routeNumber(route);
			if (!realNumber.isEmpty()) {
				return MtrData.cleanRouteNumber(realNumber);
			}
		}
		return MtrData.cleanRouteNumber(MtrData.deriveRouteNumberFromName(name));
	}

	/** 线路编号：优先完整 Route 的真实编号，其次按 MTR 名称规则从线路名推导（与站名牌同源）。 */
	private static String resolveLineNumber(MtrData.RouteView routeView) {
		final Route fullRoute = MtrData.routeById(routeView.routeId());
		if (fullRoute != null) {
			final String number = MtrData.routeNumber(fullRoute);
			if (!number.isEmpty()) {
				return number;
			}
		}
		return MtrData.deriveRouteNumberFromName(routeView.name());
	}

	/**
	 * 主线线路编号：与站名牌 {@code RenderCRTStationName1} 用的
	 * {@code RouteMapGenerator.resolveRouteData} 保持同一优先级 —— 完整 Route 的真实编号 →
	 * 经过该站台的其它走向（优先同色线路）的真实编号 → 按 MTR 名称规则从线路名推导。
	 *
	 * <p>MTR 客户端的线路编号要等服务端 sync 之后才有（数据缓存重建期间还会短暂为空），
	 * 站名牌正是靠「同站台其它线路」兜底才稳定出编号，这里照搬同一套顺序
	 * （名称推导放到最后，否则会盖掉真实编号）。</p>
	 */
	private static String resolvePlatformLineNumber(long platformId, MtrData.RouteView mainRoute) {
		final Route fullRoute = MtrData.routeById(mainRoute.routeId());
		if (fullRoute != null) {
			final String number = MtrData.routeNumber(fullRoute);
			if (!number.isEmpty()) {
				return number;
			}
		}
		final List<MtrData.RouteView> platformRoutes = MtrData.routeViewsForPlatform(platformId);
		for (final MtrData.RouteView routeView : platformRoutes) {
			if (routeView.color() == mainRoute.color()) {
				final String number = MtrData.routeNumber(MtrData.routeById(routeView.routeId()));
				if (!number.isEmpty()) {
					return number;
				}
			}
		}
		for (final MtrData.RouteView routeView : platformRoutes) {
			final String number = MtrData.routeNumber(MtrData.routeById(routeView.routeId()));
			if (!number.isEmpty()) {
				return number;
			}
		}
		return MtrData.deriveRouteNumberFromName(mainRoute.name());
	}
}
