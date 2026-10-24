package com.Nanbin.client.RouteMap;

import java.util.List;

/**
 * 吊板线路图所需的整条线路数据：由站台 ID 解析出所在线路从首站到末站的全部车站。
 * 与重庆轨道交通屏蔽门吊板导视一致：车站编号取该站站台号码（与站名牌、站牌圆徽同源），
 * 换乘信息取自 MTR 为每个线路车站维护的换乘颜色表并排除本线自身颜色。
 * 支线（{@link Branch}）指经过本站台、但在某个共用车站之后走向不同的另一条
 * {@code SimplifiedRoute}（MTR 里一条线路可以有多条走向），逐条画成下方第二条线路带。
 * 解析逻辑见 {@link LineMapDataResolver}。
 *
 * @param lineColor    本线颜色
 * @param lineNumber   本线编号（线路号）
 * @param currentIndex 本站在该线路中的下标（0 = 首站）
 * @param stations     该线路全部车站，按行车顺序
 * @param branches     支线，可为空列表
 */
public record LineMapData(int lineColor, String lineNumber, int currentIndex, List<Station> stations, List<Branch> branches) {

    /**
     * @param name         车站名（{@code |} 分隔中英段）
     * @param number       车站编号（该站站台号码）
     * @param interchanges 换乘线路，可为空列表
     */
    public record Station(String name, String number, List<Interchange> interchanges) {
    }

    /**
     * @param color  换乘线路颜色
     * @param number 换乘线路编号（多个时用 / 连接）
     */
    public record Interchange(int color, String number) {
    }

    /**
     * 一条支线：与主线共用若干车站，共用段之外自成一支（MTR 里同一条线路的支线通常是
     * 另一条同色同号的 {@code SimplifiedRoute}，可能只在本站与主线相交）。
     *
     * @param forkIndex      分叉车站在主线 {@link #stations} 中的下标（最后一个共用车站）
     * @param lineColor      支线颜色（同一条线路分叉时与主线同色）
     * @param lineNumber     支线线路号
     * @param afterStations  分叉之后（行进方向前方）的支线车站，按行车顺序
     * @param beforeStations 分叉之前（反方向）的支线车站，按行车顺序
     */
    public record Branch(int forkIndex, int lineColor, String lineNumber, List<Station> afterStations, List<Station> beforeStations) {
    }
}
