package com.Nanbin.client.RouteMap;


import com.Nanbin.InitClient;
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
     * 解析指定站台的线路信息（主题色、线路色、线路编号）。
     * 数据源用 simplifiedRoutes：它是增量更新，不像 platform.routes 那样每次 sync 被清空重建。
     * 编号优先级：服务端同步到方块实体的 routeNumber → 完整 Route 的 routeNumber →
     * 按 MTR 名称规则从 simplifiedRoute.getName() 推导。
     *
     * @param platformId        保存的站台 ID，0 表示未配置
     * @param fallbackColor     无线路数据时的回退颜色（通常为站名颜色）
     * @param syncedRouteNumber 服务端同步到方块实体的线路编号，可能为 null 或空串
     * @return themeColor/routeColor/routeNumber 在数据缺失时回退
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
            InitClient.LOGGER.error("RouteMapGenerator: Error resolving route data for platform {}", platformId, e);
        }

        return new ResolvedRouteData(themeColor, routeColor, routeNumber);
    }

    /**
     * 与线路颜色同源的编号推导：完整 Route 数据未同步时，从 simplifiedRoute.getName() 提取编号。
     */
    public static String deriveRouteNumberFromName(String routeName) {
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
     * 圆徽里只放纯线路编号：MTR 的编号字段为空时会退回线路名（如「8线」「1号线」），
     * 这里剥掉「号线／线」后缀；本来就没有后缀的编号原样保留。
     */
    public static String cleanRouteNumber(String routeNumber) {
        if (routeNumber == null) {
            return "";
        }
        final String trimmed = routeNumber.trim();
        if (trimmed.endsWith("号线")) {
            return trimmed.substring(0, trimmed.length() - 2);
        }
        return trimmed.length() > 1 && trimmed.endsWith("线") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public record ResolvedRouteData(int themeColor, int routeColor, String routeNumber) { }

    /**
     * 吊板门上方站牌所需的显示数据，全部由站台 ID 解析（数据源与 RenderCRTStationName1 同源）。
     *
     * @param previousStationName 线路上一站名，线路首站（非环线）时为 null
     * @param nextStationName     线路下一站名，线路末站（非环线）时为 null
     * @param lineColor           线路颜色；无线路数据时回退为站色
     */
    public record PSDStationData(String stationName, String previousStationName, String nextStationName, String routeNumber, String stationNumber, int lineColor) { }

    /**
     * 解析吊板门上方站牌所需数据。站名/车站编号取自站台与所属车站，
     * 前/后一站取自 platformId 所在 simplifiedRoute 的前后索引（环线首尾相接）。
     *
     * @return 站台不存在时返回 null（渲染器据此不绘制站牌）
     */
    public static PSDStationData resolvePSDStationData(long platformId) {
        if (platformId == 0) {
            return null;
        }
        try {
            final MinecraftClientData clientData = MinecraftClientData.getInstance();
            final Platform platform = clientData.platformIdMap.get(platformId);
            if (platform == null) {
                return null;
            }

            final Station station = platform.area;
            final String stationName = station == null ? "" : station.getName();
            final String stationNumber = platform.getName() == null ? "" : platform.getName();
            String previousStationName = null;
            String nextStationName = null;
            String routeNumber = "";
            boolean hasRealNumber = false;
            int lineColor = station == null ? 0 : station.getColor();

            for (final SimplifiedRoute simplifiedRoute : clientData.simplifiedRoutes) {
                final int index = simplifiedRoute.getPlatformIndex(platformId);
                if (index < 0) {
                    continue;
                }

                lineColor = simplifiedRoute.getColor();
                routeNumber = deriveRouteNumberFromName(simplifiedRoute.getName());
                final boolean circular = simplifiedRoute.getCircularState() != Route.CircularState.NONE;
                final int stationCount = simplifiedRoute.getPlatforms().size();

                if (index > 0) {
                    previousStationName = simplifiedRoute.getPlatforms().get(index - 1).getStationName();
                } else if (index == 0 && circular && stationCount > 0) {
                    // 环线：首站的前一站是末站
                    previousStationName = simplifiedRoute.getPlatforms().get(stationCount - 1).getStationName();
                }

                if (index + 1 < stationCount) {
                    nextStationName = simplifiedRoute.getPlatforms().get(index + 1).getStationName();
                } else if (circular && stationCount > 0) {
                    // 环线：末站的后一站是首站
                    nextStationName = simplifiedRoute.getPlatforms().get(0).getStationName();
                }

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
                    routeNumber = fullRoute.getRouteNumber();
                    hasRealNumber = true;
                }
                break;
            }

            // 兜底：routeIdMap/data.routes 尚未同步时，用 platform.routes 里真实的线路编号字段
            // （与 RenderCRTStationName 的 resolveRouteData 同源，避免圆徽只拿到「8线」这类线路名）
            if (!hasRealNumber && platform.routes != null) {
                for (final Route fallbackRoute : platform.routes) {
                    final String fallbackNumber = fallbackRoute.getRouteNumber();
                    if (fallbackNumber != null && !fallbackNumber.isEmpty()) {
                        routeNumber = fallbackNumber;
                        break;
                    }
                }
            }

            return new PSDStationData(stationName, previousStationName, nextStationName, routeNumber, stationNumber, lineColor);
        } catch (Exception e) {
            InitClient.LOGGER.error("RouteMapGenerator: Error resolving PSD sign data for platform {}", platformId, e);
            return null;
        }
    }

    protected record StationPosition(float x, float y, boolean isCommon) { }

    protected record StationPositionGrouped(StationPosition stationPosition, int stationOffset, IntArrayList interchangeColors, ObjectArrayList<String> interchangeNames) { }
}