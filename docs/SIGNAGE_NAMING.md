# 多城市导视系统命名约定（CRT 及以后）

本 mod 目前包含的是**重庆轨道交通（CRT, Chongqing Rail Transit）**风格的导视系统。
以后接入其他城市（例如上海、北京等）时，**每套导视系统使用独立前缀**，避免类名/资源互相污染。

## 1. 前缀

| 系统 | 前缀 | 示例 |
| --- | --- | --- |
| 重庆轨道交通 | `CRT` | `CRTSignScreen`、`RenderCRTRailwaySign`、`BlockCRTRailwaySign` |
| 以后的其他城市 | 该城市约定的前缀 | `SHTSignScreen`、`SHTRegistry` … |

## 2. 资源文件（`assets/nanbin/`）

每套系统一份自己的定义文件，文件名带前缀：

- `crt_signs.json` —— CRT 导视标志定义（原 `signs.json`，字段与 whitelist 语义见文件内 `_comment`）
- `crt_js_signs_config.json` —— CRT 导视的 JS 样式脚本清单（原 `js_signs_config.json`）

以后新增城市时使用 `sht_signs.json`、`sht_js_signs_config.json` 等，**不要复用** `crt_*` 文件。

> `signs.json` 的 `whitelist` 仍表示"哪类导视可以启用该标志"：取值 `"crt"` 即只给 CRT 类导视，
> 以后可以写 `"sht"`。未声明或空数组表示不启用（不会再混进原版 MTR 导视牌）。

## 3. 类名

- **城市专属**的类必须带前缀：屏幕、绘制、剪贴板、注册表、渲染器、方块、数据包等。
  - 例：`CRTSignScreen` / `CRTSignDrawing` / `CRTSignView` / `CRTSignPreview` /
    `CRTSignClipboard` / `CRTSignRegistry` / `CRTSignDoubleScreen` /
    `PacketUpdateCRTRailwaySignWallData1` / `RenderCRTRailwaySign`。
- **通用基础设施**不加前缀（可被任意城市系统复用）：
  - MTR 映射层与 `IDrawing` 等既有能力；
  - JS 脚本引擎族：`JSSignEngine`、`JSSignConfig`、`JSSignTextureBaker`、`JSDrawSink` 等
    （`CRTJsSignLoader` 只是加载 CRT 的那份配置清单，引擎本身与城市无关）；
  - UI 工具：`NanbinUIScreen`、`NanbinHorizontalScrollBar` 等。
- 对 MTR 原版类的 mixin 按目标类命名（`RailwaySignScreenMixin`、`RenderRailwaySignMixin`），
  与城市无关；城市专属的加载钩子用 `CRTSignLoaderMixin` 这类带前缀的名字。

## 4. 包名

沿用仓库现有的**按职责分包**（`client.Screen` / `client.Drawing` / `client.ClientData` /
`client.Render` / `registry.RegBlock` / `packet` / `mixin`），**不按城市分包**；
城市区分由类名前缀 + 资源文件前缀承担。这样同一职责的代码（比如所有城市的指示牌编辑器）
都在同一目录下，便于横向复用。

## 5. 当前 CRT 导视入口一览

| 职责 | 类 |
| --- | --- |
| 编辑界面（暗色） | `com.Nanbin.client.Screen.CRTSignScreen` |
| 多行指示牌编辑界面（车站信息牌） | `com.Nanbin.client.Screen.CRTSignDoubleScreen` |
| 共享绘制 / 视图 / 预览 | `CRTSignDrawing` / `CRTSignView` / `CRTSignPreview` |
| 独立剪贴板 | `com.Nanbin.client.ClientData.CRTSignClipboard` |
| 标志注册表 | `com.Nanbin.client.ClientData.CRTSignRegistry` |
| 资源重载钩子 | `com.Nanbin.mixin.CRTSignLoaderMixin` |
| JS 样式清单加载 | `com.Nanbin.client.JavaScriptSupport.CRTJsSignLoader` |
| 世界渲染 | `RenderCRTRailwaySign` / `RenderCRTRailwaySignWall1` / `RenderCRTStationInfo1(Double)` |

## 6. 新增城市的接入步骤（概要）

1. 新建 `xxx_signs.json`、`xxx_js_signs_config.json`，标志 `whitelist` 写 `"xxx"`。
2. 新建 `XXXSignRegistry`（或复用同类注册表的多文件加载能力），在资源重载钩子里加一行 `reload()`。
3. 新建 `XXXSignScreen`（可参考 `CRTSignScreen` 的信息架构）与对应的绘制/视图类。
4. 方块、方块实体、渲染器、数据包全部带 `XXX` 前缀，并在 `ClientPacketHelper` 里按方块类型路由到新屏幕。
5. 该城市的编辑界面只列自己 `whitelist` 允许的标志，**不要**把两套城市的标志混进同一个调色板。
