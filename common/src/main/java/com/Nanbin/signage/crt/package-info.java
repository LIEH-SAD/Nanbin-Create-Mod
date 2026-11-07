/**
 * CRT（重庆轨道交通）导视牌的自由布局数据模型与编辑算法。
 *
 * <p>本包是<b>公共端</b>代码（服务端/客户端都能加载），不依赖 Minecraft 客户端类：
 * <ul>
 *   <li>{@link com.Nanbin.signage.crt.CRTSignLayout}：整块牌（宽度/行高/背景/灰框 + 多行）；</li>
 *   <li>{@link com.Nanbin.signage.crt.CRTSignElement}：单个元素（十种类型 + MTR 数据绑定）；</li>
 *   <li>{@link com.Nanbin.signage.crt.CRTSignElementType}：元素类型枚举（id 与网页项目一致）；</li>
 *   <li>{@link com.Nanbin.signage.crt.CRTSignHistory}：撤销/重做（JSON 快照）；</li>
 *   <li>{@link com.Nanbin.signage.crt.CRTSignClipboardData}：元素剪贴板（与网页项目同格式）；</li>
 *   <li>{@link com.Nanbin.signage.crt.CRTSignDataContext}：渲染时需要的 MTR 数据访问接口
 *       （客户端实现在 {@code com.Nanbin.client.crt.render.CRTSignDataResolver}）。</li>
 * </ul>
 *
 * <p>数据以 JSON 字符串存放在 CRT 指示牌方块实体的 NBT（{@code crt_sign_layout}）里，
 * 通过 {@code com.Nanbin.packet.PacketUpdateCRTSignLayout} 同步。没有布局的旧存档仍按
 * MTR 格子数据（signIds）渲染，见 {@link com.Nanbin.client.crt.legacy}。
 *
 * <p>命名约定（为以后接入其他城市导视系统）：城市前缀（CRT）+ 领域包，见
 * {@code docs/SIGNAGE_NAMING.md}。
 */
package com.Nanbin.signage.crt;
