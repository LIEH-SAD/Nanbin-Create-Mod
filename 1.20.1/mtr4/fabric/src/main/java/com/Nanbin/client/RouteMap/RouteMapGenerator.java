package com.Nanbin.client.RouteMap;

import com.Nanbin.Init;
import org.apache.logging.log4j.util.BiConsumer;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.config.Config;
import org.mtr.mod.data.IGui;

public class RouteMapGenerator implements IGui {
    protected static int scale;
    protected static int lineSize;
    protected static int lineSpacing;
    protected static int fontSizeBig;
    protected static int fontSizeSmall;

    public static final int MIN_VERTICAL_SIZE = 5;

    public static void setConstants() {
        scale = (int) Math.pow(2, Config.getClient().getDynamicTextureResolution() + 5);
        lineSize = scale / 8;
        lineSpacing = lineSize * 3 / 2;
        fontSizeBig = lineSize * 2;
        fontSizeSmall = fontSizeBig / 2;
    }

    protected static String getStationName(long platformId) {
        final Platform platform = MinecraftClientData.getInstance().platformIdMap.get(platformId);
        final Station station = platform == null ? null : platform.area;
        return station == null ? "" : station.getName();
    }

    protected static IntArrayList getRouteStream(long platformId, BiConsumer<SimplifiedRoute, Integer> nonTerminatingCallback) {
        final IntArrayList colors = new IntArrayList();
        final IntArrayList terminatingColors = new IntArrayList();
        MinecraftClientData.getInstance().simplifiedRoutes.stream().filter(simplifiedRoute -> simplifiedRoute.getPlatformIndex(platformId) >= 0).sorted().forEach(simplifiedRoute -> {
            final int currentStationIndex = simplifiedRoute.getPlatformIndex(platformId);
            if (currentStationIndex < simplifiedRoute.getPlatforms().size() - 1) {
                nonTerminatingCallback.accept(simplifiedRoute, currentStationIndex);
                if (!colors.contains(simplifiedRoute.getColor())) {
                    colors.add(simplifiedRoute.getColor());
                }
            } else {
                if (!terminatingColors.contains(simplifiedRoute.getColor())) {
                    terminatingColors.add(simplifiedRoute.getColor());
                }
            }
        });
        if (colors.isEmpty()) {
            colors.addAll(terminatingColors);
        }
        return colors;
    }

    /**
     * 解析服务指定站台的线路信息（主题色、线路色、线路编号）。
     * 与线路颜色同源：simplifiedRoutes 采用增量更新，不会像 platform.routes 那样每次 sync 被清空重建。
     * 编号优先用服务端同步到方块实体的真实 routeNumber，其次完整 Route 的 routeNumber，
     * 都没有时按 MTR 名称规则从 simplifiedRoute.getName() 推导，保证编号与颜色同时可获取。
     *
     * @param platformId         保存的站台 ID，0 表示未配置
     * @param fallbackColor      无线路数据时的回退颜色（通常为站名颜色）
     * @param syncedRouteNumber  服务端同步到方块实体的线路编号，可能为 null 或空串
     * @return 解析结果，themeColor/routeColor/routeNumber 在数据缺失时回退
     */
    public static ResolvedRouteData resolveRouteData(long platformId, int fallbackColor, String syncedRouteNumber) {
        int themeColor = fallbackColor;
        int routeColor = fallbackColor;
        String routeNumber = syncedRouteNumber == null ? "" : syncedRouteNumber;

        try {
            final MinecraftClientData clientData = MinecraftClientData.getInstance();
            final Platform platform = (platformId == 0) ? null : clientData.platformIdMap.get(platformId);

            if (platformId != 0) {
                Route route = null;
                for (final SimplifiedRoute simplifiedRoute : clientData.simplifiedRoutes) {
                    if (simplifiedRoute.getPlatformIndex(platformId) >= 0) {
                        routeColor = simplifiedRoute.getColor();
                        if (routeNumber.isEmpty()) {
                            routeNumber = deriveRouteNumberFromName(simplifiedRoute.getName());
                        }
                        // 先查 routeIdMap（由 Data.sync 重建），未命中时再遍历 data.routes 兜底，
                        // 避免 routeIdMap 重建与 simplifiedRoutes 增量更新之间短暂不同步导致编号丢失
                        route = clientData.routeIdMap.get(simplifiedRoute.getId());
                        if (route == null) {
                            for (final Route candidate : clientData.routes) {
                                if (candidate.getId() == simplifiedRoute.getId()) {
                                    route = candidate;
                                    break;
                                }
                            }
                        }
                        if (route != null) {
                            // 完整 Route 可用时，优先使用其真实的线路编号字段
                            final String realNumber = route.getRouteNumber();
                            if (realNumber != null && !realNumber.isEmpty()) {
                                routeNumber = realNumber;
                            }
                            themeColor = route.getColor();
                        } else {
                            themeColor = simplifiedRoute.getColor();
                        }
                        break;
                    }
                }

                // 兜底：platform.routes 关联数据（sync 期间可能短暂为空，但会随后恢复）
                if (routeNumber.isEmpty() && platform != null && !platform.routes.isEmpty()) {
                    final Route fallbackRoute = platform.routes.iterator().next();
                    final String fallbackNumber = fallbackRoute.getRouteNumber();
                    if (fallbackNumber != null && !fallbackNumber.isEmpty()) {
                        routeNumber = fallbackNumber;
                    }
                    themeColor = fallbackRoute.getColor();
                    routeColor = fallbackRoute.getColor();
                }
            }
        } catch (Exception e) {
            Init.LOGGER.error("RouteMapGenerator: Error resolving route data for platform {}", platformId, e);
        }

        return new ResolvedRouteData(themeColor, routeColor, routeNumber);
    }

    /**
     * 与线路颜色同源的编号推导：完整 Route 数据未同步时，从 simplifiedRoute.getName() 提取编号。
     * 遵循 MTR 线路名规则：|| 之后为隐藏段，| 为中文/英文分隔符；取首个可见段作为编号。
     * 例如 "1号线|Line 1" -> "1号线"，"坏|Loop Line" -> "坏"。
     */
    public static String deriveRouteNumberFromName(String routeName) {
        if (routeName == null || routeName.isEmpty()) {
            return "";
        }
        // MTR 规则：|| 之后的内容隐藏，只取第一部分
        final String primary = routeName.split("\\|\\|", -1)[0];
        // 取主要段（第一个 | 之前），即线路名称的显式部分
        final int separatorIndex = primary.indexOf('|');
        final String mainSegment = separatorIndex >= 0 ? primary.substring(0, separatorIndex) : primary;
        return mainSegment.trim();
    }

    public record ResolvedRouteData(int themeColor, int routeColor, String routeNumber) { }

    protected record StationPosition(float x, float y, boolean isCommon) { }

    protected record StationPositionGrouped(StationPosition stationPosition, int stationOffset, IntArrayList interchangeColors, ObjectArrayList<String> interchangeNames) { }
}
