package com.Nanbin.client.RouteMap;

import com.Nanbin.InitClient;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把 MTR 的客户端线路数据解析成 {@link LineMapData}（吊板线路图用）。
 * 本站与其它站的车站编号都取站台号码（{@code Platform.getName()}），
 * 换乘线路取自 MTR 为每个线路车站维护的换乘颜色表并排除本线。
 * MTR 里一条线路可以有多条走向（{@code SimplifiedRoute}）：主线取经过本站台的第一条，
 * 其余与主线共用过一段、之后才分开的走向作为支线，交给渲染器画成第二条线路带；
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
            final MinecraftClientData clientData = MinecraftClientData.getInstance();
            final Platform platform = clientData.platformIdMap.get(platformId);
            if (platform == null) {
                return null;
            }

            // 经过本站台的所有走向：车站最多的那条当主线（支线通常比干线短），并列时取第一条
            final List<SimplifiedRoute> candidates = new ArrayList<>();
            SimplifiedRoute mainRoute = null;
            for (final SimplifiedRoute route : clientData.simplifiedRoutes) {
                if (route.getPlatformIndex(platformId) >= 0) {
                    candidates.add(route);
                    if (mainRoute == null || route.getPlatforms().size() > mainRoute.getPlatforms().size()) {
                        mainRoute = route;
                    }
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            final int currentIndex = mainRoute.getPlatformIndex(platformId);
            final List<Long> mainStationIds = new ArrayList<>();
            for (final SimplifiedRoutePlatform routePlatform : mainRoute.getPlatforms()) {
                mainStationIds.add(routePlatform.getStationId());
            }
            final List<LineMapData.Station> stations = buildStations(mainRoute, clientData);

            return new LineMapData(mainRoute.getColor(), resolveLineNumber(mainRoute, clientData), currentIndex, stations,
                    resolveBranches(mainRoute, mainStationIds, candidates, clientData));
        } catch (Exception e) {
            InitClient.LOGGER.error("LineMapDataResolver: failed to resolve line map for platform {}", platformId, e);
        }
        return null;
    }

    /**
     * 除主线之外的走向：同一条线路的另一分支（同一个 Route、同色或同号），
     * 或与主线共用过一段（≥2 个车站）的走向，都算支线；只在单一车站与主线相交的换乘线路不算。
     */
    private static List<LineMapData.Branch> resolveBranches(SimplifiedRoute mainRoute, List<Long> mainStationIds,
            List<SimplifiedRoute> candidates, MinecraftClientData clientData) {
        final List<LineMapData.Branch> branches = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        for (final SimplifiedRoute route : candidates) {
            if (route == mainRoute) {
                continue;
            }
            final List<SimplifiedRoutePlatform> routePlatforms = route.getPlatforms();

            // 主线上的分叉站 = 最后一条与本线共用的车站；它在本走向里的位置决定支线从哪接出去
            int forkIndex = -1;
            int forkRouteIndex = -1;
            int commonCount = 0;
            for (int j = 0; j < routePlatforms.size(); j++) {
                final int mainIndex = mainStationIds.indexOf(routePlatforms.get(j).getStationId());
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
            final boolean sameRoute = route.getId() == mainRoute.getId() || route.getColor() == mainRoute.getColor();
            if (!sameRoute && commonCount < 2) {
                continue;
            }

            // 支线专属车站（与主线不重复的车站），按它们在本走向里位于分叉站之前／之后分成两侧
            final List<LineMapData.Station> afterStations = new ArrayList<>();
            final List<LineMapData.Station> beforeStations = new ArrayList<>();
            for (int j = 0; j < routePlatforms.size(); j++) {
                final SimplifiedRoutePlatform routePlatform = routePlatforms.get(j);
                if (mainStationIds.contains(routePlatform.getStationId())) {
                    continue;
                }
                (j > forkRouteIndex ? afterStations : beforeStations).add(toStation(routePlatform, j, route.getColor(), clientData));
            }
            if (!afterStations.isEmpty() || !beforeStations.isEmpty()) {
                final String key = branchKey(forkIndex, afterStations, beforeStations);
                if (keys.add(key)) {
                    branches.add(new LineMapData.Branch(forkIndex, route.getColor(), resolveLineNumber(route, clientData),
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

    private static List<LineMapData.Station> buildStations(SimplifiedRoute route, MinecraftClientData clientData) {
        final List<LineMapData.Station> stations = new ArrayList<>();
        final List<SimplifiedRoutePlatform> routePlatforms = route.getPlatforms();
        for (int i = 0; i < routePlatforms.size(); i++) {
            stations.add(toStation(routePlatforms.get(i), i, route.getColor(), clientData));
        }
        return stations;
    }

    private static LineMapData.Station toStation(SimplifiedRoutePlatform routePlatform, int routeIndex, int lineColor,
            MinecraftClientData clientData) {
        final Platform routePlatformData = clientData.platformIdMap.get(routePlatform.getPlatformId());
        final String rawNumber = routePlatformData == null || routePlatformData.getName() == null ? "" : routePlatformData.getName().trim();
        // 站台没填号码时用在该走向上的序号补齐（01、02…），站圆不留空白
        final String stationNumber = rawNumber.isEmpty() ? String.format("%02d", routeIndex + 1) : rawNumber;
        return new LineMapData.Station(routePlatform.getStationName(), stationNumber, resolveInterchanges(routePlatform, lineColor, clientData));
    }

    /** 换乘线路：本站除本线以外的其它线路颜色与编号。 */
    private static List<LineMapData.Interchange> resolveInterchanges(SimplifiedRoutePlatform routePlatform, int lineColor, MinecraftClientData clientData) {
        final List<LineMapData.Interchange> interchanges = new ArrayList<>();
        routePlatform.forEach((color, routeNames) -> {
            if (color == lineColor) {
                return;
            }
            final StringBuilder numbers = new StringBuilder();
            routeNames.forEach(name -> {
                final String number = interchangeNumber(color, name, clientData);
                if (number.isEmpty()) {
                    return;
                }
                if (numbers.length() > 0) {
                    numbers.append('/');
                }
                numbers.append(number);
            });
            if (numbers.length() > 0) {
                interchanges.add(new LineMapData.Interchange(color, numbers.toString()));
            }
        });
        return interchanges;
    }

    /**
     * 换乘线路编号：优先按换乘颜色匹配到真实 Route 的编号字段，其次退回线路名并剥掉「号线／线」。
     * 中文编号（如「环线」）原样保留。
     */
    private static String interchangeNumber(int color, String name, MinecraftClientData clientData) {
        for (final Route route : clientData.routes) {
            if (route.getColor() == color) {
                final String realNumber = route.getRouteNumber();
                if (realNumber != null && !realNumber.isEmpty()) {
                    return RouteMapGenerator.cleanRouteNumber(realNumber);
                }
            }
        }
        return RouteMapGenerator.cleanRouteNumber(RouteMapGenerator.deriveRouteNumberFromName(name));
    }

    /** 线路编号：优先完整 Route 的真实编号，其次按 MTR 名称规则从线路名推导（与站名牌同源）。 */
    private static String resolveLineNumber(SimplifiedRoute simplifiedRoute, MinecraftClientData clientData) {
        Route fullRoute = clientData.routeIdMap.get(simplifiedRoute.getId());
        if (fullRoute == null) {
            for (final Route candidate : clientData.routes) {
                if (candidate.getId() == simplifiedRoute.getId()) {
                    fullRoute = candidate;
                    break;
                }
            }
        }
        if (fullRoute != null && fullRoute.getRouteNumber() != null && !fullRoute.getRouteNumber().isEmpty()) {
            return fullRoute.getRouteNumber();
        }
        return RouteMapGenerator.deriveRouteNumberFromName(simplifiedRoute.getName());
    }
}
