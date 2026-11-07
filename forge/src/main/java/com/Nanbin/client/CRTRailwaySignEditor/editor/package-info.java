/**
 * CRT 导视牌编辑器界面（仅客户端）。
 *
 * <p>入口是 {@link com.Nanbin.client.Screen.CRTSignEditorScreen}：
 * 刷子点击 CRT 铁路指示牌 / 墙挂牌时由 {@code ClientPacketHelper} 打开。
 * 界面布局对齐开源网页项目 CRT-Sign-Web-Editor（浅色三栏、260px 侧栏），
 * 但完全用 Minecraft GUI 自绘实现。
 *
 * <ul>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorTheme}：颜色/尺寸/字号规范（取自网页 style.css）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorWidgets}：自绘控件库（按钮/卡片/输入框/滑条/颜色选择/滚动区/tooltip）；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.editor.CRTEditorPresets}：预设持久化（{@code <run>/config/nanbin_crt_sign_presets.json}）；</li>
 *   <li>{@link com.Nanbin.client.Screen.CRTSignJsonScreen}：项目 JSON 查看 / 导入导出；</li>
 *   <li>{@link com.Nanbin.client.CRTRailwaySignEditor.editor.CRTSignEditorHost}：保存与 MTR 选择列表的宿主接口
 *       （实现 {@link com.Nanbin.client.CRTRailwaySignEditor.editor.CRTSignEditorHostImpl}）；</li>
 *   <li>{@link com.Nanbin.client.Screen.CRTSignDoubleScreen}：车站信息牌的多行格子编辑器（旧样式，保留给车站信息牌）。</li>
 * </ul>
 *
 * <p>规格与差距分析见 {@code docs/WEB_EDITOR_SPEC.md}、{@code docs/EDITOR_GAP_ANALYSIS.md}。
 */
package com.Nanbin.client.CRTRailwaySignEditor.editor;
