# 多城市导视系统命名与代码结构约定（CRT 及以后）

本 mod 目前包含的是**重庆轨道交通（CRT, Chongqing Rail Transit）**风格的导视系统。
以后接入其他城市（例如上海、北京等）时，**每套导视系统使用独立前缀与独立包**，避免类名/资源互相污染。

## 1. 前缀

| 系统 | 前缀 | 示例 |
| --- | --- | --- |
| 重庆轨道交通 | `CRT` | `CRTSignEditorScreen`、`RenderCRTRailwaySign`、`BlockCRTRailwaySign` |
| 以后的其他城市 | 该城市约定的前缀 | `SHTSignEditorScreen`、`SHTRegistry` … |

## 2. 资源文件（`assets/nanbin/`）

每套系统一份自己的定义文件，文件名带前缀：

- `crt_signs.json` —— CRT 导视标志定义（**只保留 49 个「图标+文字」文字牌预设**；27 个与编辑器网页图标重复的基础图标
  已合并进代码内置表 `com.Nanbin.client.crt.legacy.CRTBuiltInSignIcons`，由 `CRTSignRegistry` 先注册；
  `whitelist` 语义见文件内 `_comment`）
- `crt_js_signs_config.json` —— CRT 导视的 JS 样式脚本清单

以后新增城市时使用 `sht_signs.json`、`sht_js_signs_config.json` 等，**不要复用** `crt_*` 文件。

> `whitelist` 表示"哪类导视可以启用该标志"：取值 `"crt"` 即只给 CRT 类导视，
> 以后可以写 `"sht"`。未声明或空数组表示不启用（不会混进原版 MTR 导视牌）。

## 3. 代码结构（当前 CRT）

按「数据模型 / 渲染 / 编辑器 / 旧版兼容」四层分包，客户端代码集中在 `client.crt` 下：

```
com.Nanbin
├── signage.crt                       公共数据模型（服务端可加载，无客户端依赖）
│   ├── CRTSignLayout                 整块牌（宽度/行高/背景/灰框 + 多行 rows）
│   ├── CRTSignElement               元素（10 种类型 + 颜色/对齐/内距/旋转… + MTR 数据绑定）
│   ├── CRTSignElementType           元素类型枚举（id 与网页项目一致）
│   ├── CRTSignHistory               撤销/重做（JSON 快照）
│   ├── CRTSignClipboardData         元素剪贴板（与网页项目同格式）
│   ├── CRTSignDataContext           MTR 数据访问接口（渲染/属性面板共用）
│   └── package-info.java
│
├── client.crt.editor                 编辑器界面（浅色网页风）
│   ├── CRTSignEditorScreen          编辑器主屏幕（三栏：元素卡片/画布/属性）
│   ├── CRTEditorWidgets             自绘控件库
│   ├── CRTEditorTheme               颜色/尺寸/字号规范（取自网页 style.css）
│   ├── CRTEditorPresets             预设持久化（<run>/config/nanbin_crt_sign_presets.json）
│   ├── CRTPresetCatalog             crt_signs.json 未被网页图标覆盖的文字牌 → 「CRT 预设」分类
│   ├── CRTSignJsonScreen            项目 JSON 查看/导入导出
│   ├── CRTSignEditorHost(Impl)      保存与 MTR 选择列表的宿主接口/实现
│   ├── CRTSignDoubleScreen          车站信息牌的多行格子编辑器（旧样式，保留使用）
│   └── package-info.java
│
├── client.crt.render                 渲染引擎 + MTR 数据解析
│   ├── CRTSignLayoutPainter         布局计算 + 绘制（预览/世界共用；bake 成贴图）
│   ├── CRTJsLayoutBake             整牌 JS 样式：脚本 → 贴图（世界/预览共用）
│   ├── CRTSignLayoutFrame           一帧布局结果（设计像素坐标 + 元素命中框）
│   ├── CRTSignLayoutPreview         编辑器画布预览（烘焙贴图 + 选中框/行带）
│   ├── CRTSignTextMetrics           文本墨区测量（java.awt，复刻网页 Canvas 语义）
│   ├── CRTSignDataResolver          CRTSignDataContext 的客户端实现（线路/车站/出口/站台/上下站）
│   └── package-info.java
│
├── client.crt.legacy                 旧格子数据兼容层
│   ├── CRTLegacySignDrawing         旧格子绘制（RenderCRTRailwaySign* 的 fallback）
│   ├── CRTLegacySignView            旧格子渲染视图
│   ├── CRTSignRegistry              nanbin:crt_signs.json 标志注册表（旧格子标志/whitelist）
│   ├── CRTBuiltInSignIcons          27 个基础图标（从 crt_signs.json 合并进代码；网页图标/旧存档共用）
│   ├── SignTextStyleConfig          crt_signs.json 的自定义文字样式
│   ├── CRTSignLegacyImporter        旧格子数据 → CRTSignLayout 的一次性转换
│   └── package-info.java
│
├── Registry.RegBlock.BlockCRT*      CRT 方块（铁路牌/墙挂牌/站名牌/…）
├── client.Render.RenderCRT*         CRT 渲染器（指示牌/站台/门…）
├── packet.PacketUpdateCRTSignLayout 布局保存包（NBT: crt_sign_layout）
└── mixin.CRTSignLoaderMixin         MTR 资源重载时加载 CRT 标志与 JS 脚本
```

**已删除的旧实现**（随自由布局编辑器上线）：
`CRTSignScreen`（旧格子编辑屏）、`CRTSignClipboard`（旧格子剪贴板）、`CRTSignPreview`（旧格子预览）、
`PacketUpdateCRTRailwaySignWallData1`（旧墙挂牌保存包，墙挂牌现在存布局）。

## 4. 类名约定

- **城市专属**的类必须带前缀：编辑器、绘制、剪贴板、注册表、渲染器、方块、数据包等。
  例：`CRTSignEditorScreen` / `CRTSignLayoutPainter` / `CRTSignLayoutPreview` /
  `CRTSignClipboardData` / `CRTSignRegistry` / `CRTSignDoubleScreen` /
  `PacketUpdateCRTSignLayout` / `RenderCRTRailwaySign`。
- **通用基础设施**不加前缀（可被任意城市复用）：
  - MTR 映射层与 `IDrawing` 等既有能力；
  - JS 脚本引擎族：`JSSignEngine`、`JSSignConfig`、`JSSignTextureBaker`、`JSDrawSink` 等
    （`CRTJsSignLoader` 只加载 CRT 的那份配置清单，引擎本身与城市无关）；
  - UI 工具：`NanbinUIScreen`、`NanbinHorizontalScrollBar` 等。
- 对 MTR 原版类的 mixin 按目标类命名（`RailwaySignScreenMixin`、`RenderRailwaySignMixin`），
  与城市无关；城市专属的加载钩子用 `CRTSignLoaderMixin` 这类带前缀的名字。
- 旧版兼容代码统一放 `client.crt.legacy`，类名带 `Legacy` 或在注释中说明其历史来源。

## 5. 新增城市的接入步骤（概要）

1. 新建 `xxx_signs.json`、`xxx_js_signs_config.json`，标志 `whitelist` 写 `"xxx"`。
2. 数据模型：复制/抽取 `signage.crt` 的布局/元素模型（或先抽象公共基类）；
   新增 `client.xxx.render`（绘制/预览/数据解析）与 `client.xxx.editor`（编辑器屏幕）。
3. 新建 `XXXSignRegistry`（旧格子标志注册表）与 `XXXSignLoaderMixin` 的加载入口。
4. 方块、方块实体、渲染器、数据包全部带 `XXX` 前缀，并在 `ClientPacketHelper` 里按方块类型路由到新屏幕。
5. 该城市的编辑界面只列自己 `whitelist` 允许的标志，**不要**把两套城市的标志混进同一个调色板。

## 6. 相关文档

- [WEB_EDITOR_SPEC.md](WEB_EDITOR_SPEC.md) —— 网页编辑器完整规格（视觉/数据/渲染/交互）
- [MTR_DATA_BINDING.md](MTR_DATA_BINDING.md) —— MTR 数据与选择屏 API 梳理
- [EDITOR_MODULE_CONTRACTS.md](EDITOR_MODULE_CONTRACTS.md) —— 各模块公开契约
- [EDITOR_GAP_ANALYSIS.md](EDITOR_GAP_ANALYSIS.md) —— 实现与规格差距、修复记录
- [EDITOR_TEST_CHECKLIST.md](EDITOR_TEST_CHECKLIST.md) —— 实机验证清单
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) —— 第三方来源与许可
