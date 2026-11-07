/**
 * CRT 导视牌的渲染引擎与数据解析（仅客户端）。
 *
 * <p>预览与世界渲染共用同一套绘制实现：
 * <ul>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutPainter}：按网页规格计算布局（{@code layout}）
 *       并绘制到 {@code JSDrawSink}（{@code draw}/{@code bake}），几何见 docs/WEB_EDITOR_SPEC.md §5；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutFrame}：一帧布局结果（设计像素坐标 + 元素命中框）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignLayoutPreview}：编辑器画布预览（烘焙贴图 + 选中框/行带叠加层）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignTextMetrics}：文本墨区测量（java.awt 字体，复刻网页 Canvas 测量语义）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.render.CRTSignDataResolver}：把元素绑定解析成 MTR 实时数据
 *       （线路名/编号、车站、出口、站台、上下站），接口定义在 {@code com.Nanbin.signage.crt.CRTSignDataContext}。</li>
 * </ul>
 *
 * <p>世界渲染入口：{@code RenderCRTRailwaySign} / {@code RenderCRTRailwaySignWall1} 读取方块实体里的布局 JSON，
 * 调用 {@code CRTSignLayoutPainter.bake(...)} 得到贴图后按牌面尺寸等比绘制；失败回退旧格子渲染。
 */
package com.Nanbin.client.CRTRailwaySignEditor.render;
