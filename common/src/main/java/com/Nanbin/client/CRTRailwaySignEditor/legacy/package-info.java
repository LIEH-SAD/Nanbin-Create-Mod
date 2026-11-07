/**
 * CRT 导视牌的「旧版格子数据」兼容层（仅客户端）。
 *
 * <p>历史存档里的 CRT 指示牌只有 MTR 的格子数据（每格一个标志 id + selectedIds），
 * 没有自由布局 JSON。新编辑器打开时会用 {@link com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignLegacyImporter}
 * 把它转成布局；未打开的旧牌仍走这里的格子渲染路径：
 * <ul>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTLegacySignDrawing} / {@link com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTLegacySignView}：
 *       世界渲染用的旧格子绘制（RenderCRTRailwaySign* 的 fallback）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignRegistry}：{@code nanbin:crt_signs.json} 标志注册表
 *       （旧格子标志 + whitelist 过滤；渲染查询缓存仍由它写入）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.legacy.SignTextStyleConfig}：crt_signs.json 里的自定义文字样式；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.legacy.CRTSignLegacyImporter}：旧格子数据 → {@code CRTSignLayout} 的一次性转换。</li>
 * </ul>
 *
 * <p>旧的格子编辑屏幕（CRTSignScreen/CRTSignClipboard/CRTSignPreview）已随自由布局编辑器上线删除。
 */
package com.Nanbin.client.CRTRailwaySignEditor.legacy;
