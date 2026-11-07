# CRT 导视牌编辑器：实现 ↔ 规格差距分析（只读审计）

> 审计对象：`CRT-Sign-Web-Editor` 的 `index.html` 编辑器（导视牌编辑器）在 Minecraft Java 端的复刻实现。
> 审计方式：**只读**代码 + 规格逐条对照，未运行游戏、未改任何代码（本文件是唯一新增文件）。
> 编译验证：`.\gradlew.bat :fabric:1.20.1:compileJava --configure-on-demand --offline --console=plain` → **BUILD SUCCESSFUL**（up-to-date）。
> 结论先行：**1 个 P0 崩溃点**（删除元素首点必 NPE）、**11 个 P1**（自动内边距缺失、textColor 失效、MTR 线路色条透明、换乘站双语错位、动态宽度世界渲染比例错误、浮动工具条/内边距拖拽缺失、预设/项目导入导出缺失等），另有约 40 个 P2 打磨项（按主题合并计数）。
> 覆盖率估计（按规格功能条目加权，不计 §F 明确延后项）：**≈80%**；「打开→选中→改属性→增删改→撤销→保存→世界渲染」核心闭环 **≈92%**；牌面渲染公式忠实度 **≈88%**；编辑器视觉还原度 **≈75%**。

## Lead 修复记录（审计后第一批，已编译验证）

| 审计项 | 状态 | 修复位置 |
|---|---|---|
| P0-1 删除元素首点 NPE | ✅ 已修 | `CRTSignEditorScreen.deleteSingleElement` 改为 `elementId.equals(armedDeleteElementId)` |
| P1-1 `applyPaddingAuto` 缺失 | ✅ 已修 | 新增 `CRTSignLayout.applyPaddingAuto()`（网页 state.js 算法移植：相邻 0.1 / 无邻居 0.2、空占位不算邻居、右通道左右映射、手动内边距钉住；幂等），`fromJson` 与 `CRTSignLayoutPainter.layoutInternal` 均调用 |
| P1-2 number-line/text-line `textColor` 无效 | ✅ 已修 | `CRTSignLayoutPainter.drawElement`：这两类元素的文字颜色取 `element.textColor` |
| P1-3 MTR 线路色条透明（RGB 当 ARGB） | ✅ 已修 | `applyResolvedRouteName/RouteNumber/RouteLines` 三处颜色 `\| 0xFF000000` |
| P1-4 STATION 绑定双语错位 | ✅ 已修 | `applyResolvedStation` 按 `getStationNames()` 约定直接取 [中文行, 英文行]，不再二次按 `\|` 拆 |
| P1-5 动态宽度世界渲染比例错误 | ✅ 已修 | 两个渲染器改用 `baked.width/height`（实际 frame 尺寸）计算等比适配 |
| P2 颜色常量偏差 | ✅ 已修 | 灰框 `#4A4A4A`（旧值回退 `#808080`）、出口黄 `#F7D917` |
| P2 静态牌每秒重烘 / 每帧 toJson | ✅ 已优化 | `JSSignTextureBaker` 新增可指定 refreshMs 的重载；布局改用 30s 内容不变复用；世界渲染把 NBT JSON 的 `hashCode()` 传给 `CRTSignLayoutPainter.bake` 避免每帧 `toJson` |
| P2 服务端无布局校验 | ✅ 已加 | `PacketUpdateCRTSignLayout` 服务端拒绝 >30000 字符的布局并打日志 |

> 仍待处理（第二批已补齐，见下）：P1-9（决定不实现：本 mod 图标是彩色 signs.json 贴图，不做单色替换）。

## Lead 修复记录（第二批：功能补齐，已编译验证）

| 审计项 | 状态 | 说明 |
|---|---|---|
| P1-6 浮动元素工具条 + 内边距拖拽 | ✅ 已实现 | 单选元素浮出 ←/→/复制/删除工具条（画布内夹取、禁用态）；选中元素左右 padding 拖拽点（8px=0.05，0–8 clamp，合并为一步撤销，Esc 可中断） |
| P1-7 预设系统 | ✅ 已实现 | 左栏「元素/预设」页签、存为预设（命名弹窗）、插入（新 id、自动选中）、两段点击删除；持久化 `<run>/config/nanbin_crt_sign_presets.json`（tmp+rename，失败仅 toast） |
| P1-8 项目 JSON 导入/导出 | ✅ 已实现 | 新建 `CRTSignJsonScreen`（TextField maxLength 32767、GLFW 剪贴板按钮、折行只读预览、字符计数）；导入经 `fromJson` 校验 + 长度检查 + 可撤销（pushHistory） |
| P1-10 左栏真实缩略图 | ✅ 已实现 | `bakeElementStandalone` 缓存（key=类型/图标+行高，失败 1s 退避）；左栏卡片与右栏 32 格图标选择器均为真实烘焙，等比缩放，失败回退手绘 |
| P2 UI 字号层级 | ✅ 已实现 | 面板头 1.35× / 分组 1.15× / 正文 1.0× / 说明 0.85×（新增 scaledText* helper），顶栏/面板头/工具条高度同步微调 |

> 剩余为打磨项：预设整行缩略图与「添加为新行」、工具条内对齐下拉/上下步进、Toast 白底样式、窄分辨率三栏挤压、platform.html（站台吊板/线路图）未移植（需要产品确认是否要）。

---

## 0. 证据缩写与审计方法

行号均为当前工作树（`git status` 中未提交实现）的行号；`file:line` 统一以下列缩写表示：

| 缩写 | 文件 |
|---|---|
| `ED` | `fabric/src/main/java/com/Nanbin/client/Screen/CRTSignEditorScreen.java` |
| `W` | `fabric/src/main/java/com/Nanbin/client/Screen/CRTEditorWidgets.java` |
| `TH` | `fabric/src/main/java/com/Nanbin/client/Screen/CRTEditorTheme.java` |
| `PV` | `fabric/src/main/java/com/Nanbin/client/Drawing/CRTSignLayoutPreview.java` |
| `PT` | `fabric/src/main/java/com/Nanbin/client/Drawing/CRTSignLayoutPainter.java` |
| `FR` | `fabric/src/main/java/com/Nanbin/client/Drawing/CRTSignLayoutFrame.java` |
| `TM` | `fabric/src/main/java/com/Nanbin/client/Drawing/CRTSignTextMetrics.java` |
| `BAKER` | `fabric/src/main/java/com/Nanbin/client/JavaScriptSupport/JSSignTextureBaker.java` |
| `FTR` | `fabric/src/main/java/com/Nanbin/client/Drawing/FittedTextRenderer.java` |
| `LAYOUT` | `fabric/src/main/java/com/Nanbin/signage/crt/CRTSignLayout.java` |
| `ELEM` | `fabric/src/main/java/com/Nanbin/signage/crt/CRTSignElement.java` |
| `CLIP` | `fabric/src/main/java/com/Nanbin/signage/crt/CRTSignClipboardData.java` |
| `HIST` | `fabric/src/main/java/com/Nanbin/signage/crt/CRTSignHistory.java` |
| `CTX` | `fabric/src/main/java/com/Nanbin/signage/crt/CRTSignDataContext.java` |
| `HOST` | `fabric/src/main/java/com/Nanbin/client/Screen/CRTSignEditorHostImpl.java` |
| `IMPORTER` | `fabric/src/main/java/com/Nanbin/client/signage/CRTSignLegacyImporter.java` |
| `RESOLVER` | `fabric/src/main/java/com/Nanbin/client/signage/CRTSignDataResolver.java` |
| `CPH` | `fabric/src/main/java/com/Nanbin/packet/ClientPacketHelper.java` |
| `PKT` | `fabric/src/main/java/com/Nanbin/packet/PacketUpdateCRTSignLayout.java` |
| `RRS` | `fabric/src/main/java/com/Nanbin/client/Render/RenderCRTRailwaySign.java` |
| `RRW` | `fabric/src/main/java/com/Nanbin/client/Render/RenderCRTRailwaySignWall1.java` |
| `BLK` | `fabric/src/main/java/com/Nanbin/Registry/RegBlock/BlockCRTRailwaySign.java` |
| `WBLK` | `fabric/src/main/java/com/Nanbin/Registry/RegBlock/BlockCRTRailwaySignWall1.java` |
| `SPEC` | `docs/WEB_EDITOR_SPEC.md`（章节号引用） |
| `EXTRACT` | `docs/WEB_EDITOR_SPEC.extract.md`（§号引用） |

状态记号：✅ 已实现且与规格一致；◐ 部分实现/有偏差；✗ 未实现；— 规格未要求或本轮不适用。

---

## 1. 功能覆盖矩阵（要求 A）

### 1.1 左栏：元素选择区（规格 §3.1）

| 规格条目 | 状态 | 证据 | 缺口说明 |
|---|---|---|---|
| 面板骨架 260 + 标题 + 收起按钮 | ◐ | `ED:504-521`（30% 屏宽 clamp 到 150–260）、`ED:2864-2872`（画头/标题）、`ED:673-687`（顶栏 toggle 代替 `.expander`） | 收起/展开改为顶栏按钮；无侧边 `.expander` 把手；窄逻辑分辨率下自动收窄（适配，非 bug） |
| 「元素」标签页三分组：方向指示 12、标识与文字 7、图标与间隔 2 | ✅ | `ED:87-91`（方向 12）、`ED:97-102`（服务 20）、`ED:777-829`（palette 构建） | 与规格一致（`arrow` 类型无入口，符合 §3.1.1 警告） |
| 卡片点击添加（当前行/末行） | ✅ | `ED:3827-3829` → `ED:1245-1263`；目标行 `ED:1265-1272` | 桌面端也会自动选中新元素（网页桌面不选中），小差异 |
| 卡片拖拽到画布（HTML5 DnD 的 MC 版） | ✅ | `ED:3882-3891`（阈值启动拖拽）、`ED:3660-3705`（`updateDropTarget`）、`ED:3813-3830`（释放落点）、`ED:2979-2991`（卡片幽灵） | 落点见 P2-9（非按通道） |
| 卡片缩略图 = 该元素的真实渲染 | ✗/◐ | `ED:899-945` 是手绘色块/字符示意图；`PT:640-688` 的 `drawElementStandalone`/`bakeElementStandalone` **全仓无调用** | 与规格「示例元素渲染成 mini SVG」不符，观感差距明显（P1-10） |
| 「预设」标签页 / 预设卡片 / 拖入空行 / 存为预设 | ✗ | `ED:767-831` 只构建元素分类；全仓 grep 无 preset 逻辑；`lang` 无 preset 键 | 整块缺失（P1-7） |
| 「站台吊板 / 线路图 ↗」链接 | — | 无 | 属 `platform.html`，本轮不建议做（§F） |

### 1.2 中栏：编辑区（规格 §3.2）

| 规格条目 | 状态 | 证据 | 缺口说明 |
|---|---|---|---|
| 选择/剪贴板工具条 4 按钮 + 状态文本 | ✅ | `ED:699-731`（全选/复制/剪切/粘贴）、`ED:2901-2913`（状态文本） | 粘贴按钮空剪贴板时禁用（网页始终可用），见 P2-15 |
| 画布：真实牌面预览（非线框） | ✅ | `ED:3350-3352`（`CRTSignLayoutPreview.draw`）、`PT:706-745`（与游戏内同一张 bake） | 每帧重复布局，见 P2-28 |
| 行带 hover | ◐ | `PV:91-95`（hover 半透明行带）；`ED:3266-3281` | 颜色/层级为简化实现 |
| 行手柄「≡」（行数 > 1） | ✅ | `ED:3175-3183`、`ED:3515-3519`、拖拽 `ED:3847-3862` / `ED:3903-3910` | 可用；命中区 12×16（小于规格 28×40） |
| 行按钮「⭐ 存为预设」「✕ 删除行」 | ◐ | `ED:424-432` 定义按钮动作；`ED:3515-3531` 绘制；`ED:3641-3657` 处理 | 只有 `↑/↓/色/✕`；**没有「存为预设」**；多了上/下移/行背景快捷（适配） |
| 行拖拽排序（上半/下半判定） | ✅ | `ED:3847-3862`、`ED:3903-3910`、指示线 `ED:3579-3583` | 与规格 §6.8 一致 |
| 「＋ 添加行」 | ✅ | `ED:736-739`、`ED:1538-1541` | 同规格：追加到末尾并走撤销 |
| 元素拖拽排序/跨行 | ◐ | 会话 `ED:3893-3901`、落点 `ED:3660-3705`、应用 `ED:3832-3846`→`ED:1456-1489` | 可拖拽、可跨行；但落点/指示线不按通道计算、无拖拽幽灵（P2-8/P2-9） |
| 竖直插入指示线 | ✅ | `ED:3568-3577`（`drawDropIndicators`） | 简单竖条排列 |
| 浮动元素工具条（←/→、内边距、对齐、删除） | ✗ | 无该控件；替代入口：键盘 ←/→ `ED:4023-4034`、右侧面板删除 `ED:2493-2499`、内边距步进 `ED:2387-2426` | 整块缺失（P1-6） |
| 画布缩放徽标 / 复位 / 双击复位 | ✅ | `ED:742-753`（+/-/徽标）、`ED:3707-3718`、`ED:3627-3637` | 范围 0.25–6（规格 0.5–4）；常显（规格仅移动端显示） |
| 空状态提示 | ◐ | `ED:3363-3368`、`ED:3423-3428`；条件 `layout.isBlank()` (`ED:3363`) | 新牌会被旧存档转换预填 `space`，导致「空态」不出现（P2-24） |
| 画布滚动/平移 | ◐ | `ED:3916-3942`（滚轮：Ctrl 缩放 / zoom>1 纵向滚动） | 无横向平移入口，放大后无法查看右侧内容（P2-6） |

### 1.3 右栏：标识牌设置（规格 §3.3.1）

| 规格条目 | 状态 | 证据 | 缺口说明 |
|---|---|---|---|
| 行高（32–2048，step 4） | ✅ | `ED:2099-2115` | 锁宽高比联动也已实现（`ED:2109-2111`） |
| 行数（只读显示） | ✅ | `ED:2116` | 显示为文字（规格为只读数字框），可接受 |
| 宽度模式（固定/动态） | ✅ | `ED:2118-2122` | — |
| 宽度 1–15 格 ×256 / 固定 px 输入 | ✅ | `ED:2124-2161` | 选格数会关掉比例锁，符合 §4.7 |
| 动态宽度提示 | ✅ | `ED:2162-2164` | — |
| 锁定宽高比 | ✅ | `ED:2166-2168` | 联动算法与 §4.7 一致 |
| 牌面背景色（不可透明） | ✅ | `ED:2170-2172`（nullable=false） | — |
| 行背景色 + 恢复 + 状态提示 | ✅ | `ED:2182-2218` | 仅 >1 行或已有行色时显示，符合规格 |
| 灰框宽（0–512）/ 灰框色 | ✅ | `ED:2174-2180` | — |
| 「输入线路号自动匹配线路色」偏好 | ✗ | 无 | 规格 §3.3.1 #9；P2-35 |
| 「🗑 清空标识牌」+ 确认 | ◐ | `ED:2221-2225`、`ED:1576-1586` | 用「再点一次」代替模态确认（P2-3） |
| 导出 SVG / PNG | ✗ | 无 | 本轮不建议（§F） |
| 导出/导入项目 JSON | ✗ | 无 | 数据层 `toJson/fromJson` 已有（`LAYOUT:210-269`），仅缺 UI/文件通道（P1-8） |

### 1.4 右栏：元素属性 / 批量属性（规格 §3.3.2/§3.3.3）

| 元素 | 内容字段 | 排版字段 | 颜色字段 | 状态 |
|---|---|---|---|---|
| `arrow` | — | 8 向方向 ✅ / 粗细滑条 ◐ | 颜色 ✅ | ◐ 滑条范围 0.05–0.95（规格 UI 0.1–0.5，`ED:2359-2362`）；无左栏入口符合规格 |
| `bilingual-text` | 中/英 ✅ | 加粗 ✅ / 左中右 ✅ | 文字/背景 ✅ | ✅ `ED:2250-2264` |
| `small-bilingual-text` | 同上 | 同上 | 同上 | ✅（共用分支） |
| `big-number` | 内容 ✅ | — | 文字/背景 ✅ | ✅ `ED:2266-2271` |
| `number-line` | 线路列表增删/编号/颜色 ✅ | 内容左/右 ✅（**重复出现两处**） | **textColor 控件有但渲染无效** ◐；背景/颜色 ✅ | ◐ `ED:2289-2322`、`ED:2443-2446`；P1-2 |
| `text-line` | 下沉 ✅ / 中文 ✅ / 英文 ✅ | 内容左/右 ✅ | **textColor 控件有但渲染无效** ◐；色条色 ✅ | ◐ `ED:2273-2288`、`ED:2448-2455`；P1-2 |
| `entrance` | 编号 ✅ | — | 颜色/背景 ✅ | ✅ `ED:2323-2335` |
| `exit` | 编号 ✅ | 内容左/右 ✅（重复两处） | 无背景 ✅ + 固定黄提示 ✅ | ✅ 结构；背景色值偏差见 P2-22 |
| `space` | 宽度滑条 ✅ | 无内边距 ✅ | 背景 ✅ | ✅ `ED:2336-2340`、`ED:2384` |
| `icon` | 32 图标网格 ✅（用文字符号） | 旋转仅服务图标 ✅ | **颜色控件有但渲染忽略** ◐ | ◐ `ED:2342-2353`、`ED:2433-2435`；P1-9 |
| 通用：元素对齐（仅 fixed） | — | ✅ | — | ✅ `ED:2372-2378` |
| 通用：四向内边距 + 自动内边距开关 | — | ✅（步进器，无拖拽） | — | ◐ `ED:2384-2426`；**全局自动内边距算法缺失**（P1-1） |
| 删除元素按钮 | — | — | — | ✗→**P0-1 首点 NPE**（`ED:2493-2499`→`ED:1323-1343`） |
| MTR 数据绑定区（本 mod 扩展，契约 B 要求） | 绑定类型下拉 ✅ / 当前值 ✅ / 选择/清除 ✅ | — | — | ✅ `ED:2458-2491`、`ED:1590-1659`；对 space/arrow/icon 也显示（无意义，P2-20） |
| 批量模式（≥2 选中） | 计数 ✅ | 元素对齐/文字对齐/内容对齐/左右内边距 ✅ | 背景色 ✅（无混合态）/ 删除 ✅ | ◐ `ED:2541-2609`；混合值无「不同」提示、会改出口黄、无出口提示（P2-17/18） |
| 预设预览模式（第 4 种右栏模式） | ✗ | — | — | 无（P1-7） |
| 右栏面板头「返回」/模式标题 | ✅ | `ED:2004-2015`、`ED:2919-2941` | — | 无「预设预览」返回分支 |

### 1.5 交互（规格 §6）

| 交互 | 状态 | 证据 | 说明 |
|---|---|---|---|
| 单击选中 / 点击空白取消 | ✅ | `ED:3608-3626`、`ED:3627-3638` | — |
| Ctrl 加/减选 | ✅ | `ED:3610-3611`、`ED:992-1002` | — |
| Shift 范围选择（视觉序） | ✅ | `ED:1004-1026`、`ED:1054-1078` | — |
| Ctrl+Shift 范围并集 | ✗ | `ED:3610` 先判 Ctrl，Shift 分支不可达 | P2-10 |
| Esc 取消选择 / 取消拖拽 | ◐/✗ | `ED:3971-3982` 只清选择；**拖拽状态只在 `mouseReleased` 清理** `ED:3832-3846` | Esc 不能取消进行中的元素/行拖拽（P2-11） |
| Delete/Backspace 删除 | ✅ | `ED:4015-4022` | — |
| Ctrl+A 全选 | ✅ | `ED:3985-3987`、`ED:1036-1040` | — |
| ←/→ 微调（浮动工具条） | ◐ | 用键盘实现 `ED:4023-4034`→`ED:1361-1391`；无浮动按钮 | 功能可达但无规格要求的工具条入口 |
| 内边距拖拽（8px=0.05） | ✗ | 无；只有步进器 `ED:2387-2426` | P1-6 |
| 行添加/删除/移动 | ✅ | `ED:1538-1586`、`ED:3847-3862` | 删除确认用两段点击 |
| 存为预设 / 应用到空行 / 添加为新行 | ✗ | 无 | P1-7 |
| 撤销/重做 | ◐ | `ED:1093-1140`、`HIST:32-75` | 无选择集恢复、无 no-op 去重、blur 不断组（P2-13） |
| Ctrl+Z / Ctrl+Shift+Z / Ctrl+Y | ✅ | `ED:3997-4006` | Ctrl+Y 为增强（规格无） |
| 复制/剪切/粘贴（JSON） | ◐ | `ED:1160-1227`、`CLIP:42-180` | JSON 与网页格式一致；仅内部剪贴板，无系统剪贴板/外部 JSON 入口（P2-15） |
| 键盘快捷键在输入框内不拦截 | ✅ | `ED:3949-3969` | 与规格 §6.14 一致 |
| 输入即生效 / 800ms 合并 | ✅ | `ED:1152-1156`、`ED:1101` | 但失焦不断组（P2-13） |
| 缩放（按钮/双击/Ctrl+滚轮） | ✅ | `ED:3707-3718`、`ED:3627-3637`、`ED:3932-3939` | 滚轮缩放为增强（规格桌面无），范围偏大 |
| 自动保存 | ✅ | `ED:464-470`（200ms 节流）、`ED:477-485`（关闭即存）、`HOST:77-87` | 存到服务端 NBT；无本地偏好存储 |
| 多选整体拖动 | ✅ | `ED:3618-3624`（收集选择集） | 暂未验证跨通道落点正确性 |

### 1.6 导入导出 / 持久化（规格 §7）

| 能力 | 状态 | 证据 | 说明 |
|---|---|---|---|
| 项目 JSON 导出/导入（`{version,sign}`） | ✗ | `LAYOUT:210-269` 有数据层；编辑器无入口 | P1-8 |
| 元素剪贴板 JSON（`kind:'jr-sign-elements'`） | ✅ | `CLIP:22-148` | 格式、offset、id 重生成、上限（rows≤1000/元素≤10000）均实现 |
| 预设（localStorage 等价物） | ✗ | 无 | P1-7 |
| 自定义色板/城市色板管理 | ✗ | 只有 12 色固定色板 `W:61-68` | P2-34 |
| 自动保存（项目级） | ✅ | 服务端 NBT `crt_sign_layout`（`BLK:140,257-269`、`WBLK:183,206-213`） | 与网页 localStorage 语义等价（服务端权威） |
| 编辑器偏好（autoLineColor 等） | ✗ | 无 | P2-35 |
| SVG / PNG 导出 | ✗ | 无 | 本轮不建议（§F） |

### 1.7 数据模型 / 序列化（规格 §4/§7.1）

| 条目 | 状态 | 证据 | 说明 |
|---|---|---|---|
| 顶层字段（widthMode/width/rowHeight/aspectLocked/backgroundColor/frameWidth/frameColor/rows） | ✅ | `LAYOUT:59-68` | — |
| 默认值 | ◐ | `LAYOUT:29,31,40`、`ELEM:216-269` | `frameColor` 默认 `#9AA3AD`（规格 `#4A4A4A`/旧档 `#808080`）；`createBlank` rowHeight=256（规格 192）；exit 黄 `#FCED22`（规格 `#F7D917`）→ P2-22 |
| Row 背景可空 / 至少一行 | ✅ | `LAYOUT:121-136,295-346` | — |
| Element 十类型字段与默认值 | ✅ | `ELEM:216-269,280-360` | — |
| padding 比例与 clamp（上下 0–1、左右 0–8） | ✅ | `ELEM:388-394` | — |
| `paddingAuto` 语义 | ◐ | `ELEM:385-387` | 缺省（字段不存在）时保留 `true`；规格要求**只有显式 true 才保留**（旧档应视为手动） |
| 反序列化清洗（未知类型丢弃、颜色回退、数值 clamp、重复 id 重生成） | ✅ | `LAYOUT:232-286`、`ELEM:364-473` | 基本齐全；未知 `props` 键会被丢弃（网页会保留）→ P2-23 |
| 元素顺序与视觉序（贴右通道反序） | ✅ | `PT:210-253`、`ED:1508-1534` | 渲染侧正确；拖拽落点侧不完整 |
| JSON 结构互操作（version/sign/props/id/剪贴板） | ◐ | `LAYOUT:210-229`、`ELEM:280-360`、`CLIP:68-84` | 结构一致；未知扩展字段丢失、8 位 hex 输出（P2-23） |
| aspectLocked 联动规则 | ✅ | `ED:2105-2111,2152-2159` | 与 §4.7 一致 |

---

## 2. 视觉规范核对（要求 B）

### 2.1 颜色变量：**完全一致** ✅

`TH:22-52` 与规格 §2.1 的 `:root` 逐项相等：

| token | 规格 | 实现 |
|---|---|---|
| `--bg` | `#EEF1F4` | `TH:22` `0xFFEEF1F4` ✅ |
| `--panel-bg` | `#FFFFFF` | `TH:24` ✅ |
| `--border` | `#DCE1E8` | `TH:26` ✅ |
| `--text` | `#1F2733` | `TH:28` ✅ |
| `--text-dim` | `#6B7684` | `TH:30` ✅ |
| `--accent` | `#2563EB` | `TH:32` ✅ |
| `--accent-soft` | `rgba(37,99,235,.12)` | `TH:34` `0x1F2563EB`（≈12%）✅ |
| `--danger` | `#DC2626` | `TH:36` ✅ |
| `--ok` | `#16A34A` | `TH:38` ✅ |
| `--control-bg` | `#FFFFFF` | `TH:40` ✅ |
| `--surface-soft` | `#F6F8FB` | `TH:42` ✅ |
| 滚动条滑块 | `#C3CCD6` | `ED:2977,4115` ✅ |

### 2.2 尺寸/字号/圆角/间距 token：**主题表与实现脱节** ◐

- `TH:56-96` 定义了 `RADIUS=8`、`HEADER_HEIGHT=40`、`CONTROL_HEIGHT=20`、`FONT_SIZE=14/12/10/15/20` 等，但 **`TH` 的这些尺寸常量全仓无引用**（grep 仅 `PANEL_WIDTH` 和颜色被使用）。
- 实际控件尺寸在 `W:41-56` 另起一套：`CONTROL_HEIGHT=16`、`INPUT_HEIGHT=16`、`LINE_HEIGHT=10`、`FONT_HEIGHT=9`、`RADIUS=6`。
- 后果：**整个编辑器没有字号层级**（标题/正文/说明/提示全部用 MC 默认 9px 字体，`W:238-243` 直接 `drawText`，只有卡片图标做了缩放）；与规格 22/15/14/13.5/12/11 的字号体系不符，是最明显的「不像网页」之处（P2-1）。

### 2.3 三栏布局：**比例适配，非固定 260|auto|260** ◐

- 规格：左右栏固定 `260px`（`SPEC §2.3`、§9.8 建议 220–260 GUI px）。
- 实现：`ED:504` `clamp(screenWidth*30%, 150, 260)`；`ED:508-521` 再按最小中栏宽度挤压。
- 结果：1920×1080@GUI2（960 逻辑宽）时左右各 260 ✅；GUI3（640 逻辑宽）时各 192；GUI4（480）时各 150，中栏仅约 180 逻辑像素——画布会非常小（P2-32）。规格要求的「中栏 auto 为主」在高 GUI Scale 下失效。

### 2.4 按钮体系：**5+1 类状态基本齐全** ✅/◐

`W:378-507` 实现了 PRIMARY / SECONDARY / DANGER / TOOL / ICON / DASHED 六类，含 hover、selected、disabled、按下态（`W:424-470`），与规格 §2.5 的（a）(b) (c) (e) 基本对应；分段按钮 `W:561-614`、复选 `W:511-556`、虚线按钮 `W:447-451` 也符合。差异：
- 无过渡动画（CSS transition 无法直接复刻，可接受）；
- 按钮高度 16 逻辑像素，内边距比规格 5px 9px 更紧；
- 危险按钮的主/次样式与规格 `.btn-danger` 一致。

### 2.5 卡片 / 选中 / hover

| 项 | 规格 | 实现 | 结论 |
|---|---|---|---|
| 元素卡片 | 白/`#FAFBFC` 底、1px 边、hover 主色 + 阴影 + 上浮、选中浅蓝底 | `W:1517-1568`（hover 主色边、白色底/`#FAFBFC`、选中 `ACCENT_SOFT`）、`ED:874-879` | ◐ 无阴影/上浮；图标为字符示意 |
| 预设卡片 | 有缩略图/删除圆钮 | 无 | ✗ |
| 选中元素 | 主色 2.5px 虚线 7/4 + 5% 填充 | `PV:112-127`（实线 2px）+ `ED:3474-3482`（双虚线框近似） | ◐ 观感接近 |
| hover 元素 | 主色 45% 2px 描边 | `ED:3378-3379,3469-3470` | ✅ |
| 行手柄/行按钮 | 28×40、白底圆角 7；按钮 24 高 | `ED:3164-3183`（14px 按钮、12×16 手柄） | ◐ 尺寸偏小、位置简化 |
| 面板分组卡片 `.panel-section` | 1px 边框、圆角 10、padding 10 | `ED:2065-2072`、`ED:2943-2948`（圆角 8、`BORDER_SOFT`） | ◐ |
| 空状态 | 22px、居中、`rgba(120,130,145,.75)`、桌面文案 | `ED:3363-3368`（单行小字 + 副行） | ◐ |
| Toast | 深色 `#1F2733` 白字、右上、2400/4200ms | `ED:3001-3018`（**白底 + 绿/红竖条**、右上、时长一致） | ◐ 颜色不符 P2-3 |
| 模态确认 | 全屏遮罩 + 白卡 | 无模态；两段点击 + toast | ◐ P2-3 |
| 画布牌面阴影 | `0 2px 14px rgba(15,25,40,.13)` | `PV:65-110` 只有底色 + 1px 边框，无阴影 | ✗ P2-4 |
| 滚动条 | 10px、圆角 6、`#C3CCD6` | `ED:2965-2978,4100-4116`（5px、圆角 2.5、同色） | ◐ |

**明显不符合「网页项目观感」的地方（按显著度）**：
1. **无字号层级**（全部 9px 默认字体，标题不突出）——P2-1；
2. **左栏/右栏卡片与图标全部是手绘色块/字符**，不是元素真实缩略图（`ED:899-945`、`ED:2502-2527`）——P1-10；
3. 无画布阴影、无过渡、无浮动工具条（少了一整块网页特征 UI）——P1-6/P2-4；
4. Toast/确认框风格与网页不一致——P2-3；
5. 三栏在窄逻辑分辨率下被挤压——P2-32。

---

## 3. 渲染正确性抽查（要求 C）

### 3.1 已核对与 EXTRACT 公式一致的项 ✅

| 项 | 实现证据 | 结论 |
|---|---|---|
| 内容盒 `s=h−t−b`、各 padding 比例 | `PT:818-831` | ✅ 与 EXTRACT §2 一致 |
| 三通道行布局 / 动态模式左起 / 动态宽度 round | `PT:189-253` | ✅ 与 §4 一致 |
| 元素 `x` 浮点、绘制 `floor(x)`、背景/命中框 `ceil(max(w,1))` | `FR:105-113`、`PT:315-321` | ✅ |
| 灰框几何（首行/末行完整 fw、行间各半）与「行元素后绘制」 | `PT:292-304` | ✅ |
| 箭头轴/斜向几何（含 L=s√2−th、旋转+尖角对齐） | `PT:835-938` | ✅ 逐点核对一致 |
| 双语字号 0.6325/0.35625、baseline 0.036s+ascent、en 基线 +descent+0.123s | `PT:979-992` | ✅ |
| 大号双语垂直墨区居中 / 小号不居中 | `PT:942-972` | ✅ |
| 大文本 1.3 倍字号、逐字墨区、光学居中（width 用平移前 textW） | `PT:996-1015` | ✅ |
| 数字线路（色条 10/28、`stripeH=h−p.t`、右对齐只第 1 条、margin/advance 公式） | `PT:1019-1087` | ✅ |
| 文本线路（sink/flat、gap=s/8、文字基线锚定「线」墨顶） | `PT:1091-1152` | ✅ |
| 出入口/出口（编号墨顶对齐标签墨顶、codeW=lastInkRight+M₃、右对齐镜像） | `PT:1156-1195` | ✅ |
| space 宽度 = ratio×行高、图标宽 s+pads | `PT:1199-1218` | ✅ |
| 文本测量：整串 advance、ascent/descent/ink 语义、ceil 取整点 | `TM:74-111,202-250` | ✅ 与 §5.8/EXTRACT §1 一致 |
| 文字贴图定位（padding/ascent 与 `FTR:49-60` 同公式） | `PT:567-596,520-536`；`FTR:33-68` | ✅ 基线定位自洽 |
| 图标旋转仅服务设施（方向图标忽略） | `PT:1206-1218`（按 `nanbin_arrow*`/`arrow_*` 判定） | ✅ 语义一致 |
| 线路标签/固定文案不可编辑（号线/Line、线、出入口/Entrance、出口/Exit） | `PT:80-86` | ✅ |

### 3.2 可疑实现与偏差（按严重度）

| # | 项 | 实现 | 与公式/规格的偏差 | 级别 |
|---|---|---|---|---|
| C1 | number-line / text-line 文字颜色 | `PT:323` 取 `contentColor=element.color`；`PT:336-337,339-340` 传入；`PT:410-419,423-439` 使用该值 | 规格 §4.4.5/§4.4.6/§5.7.5/§5.7.6 使用 **`props.textColor`**；现在 `textColor` 完全无效、`color` 反而生效（UI 两个颜色控件都出现，用户会混乱） | **P1-2** |
| C2 | MTR 线路绑定色条 | `PT:1435,1452,1487` 直接 `(int) color` 作为 `LineItem.color` | MTR 颜色是 **RGB（alpha=0）**（`docs/MTR_DATA_BINDING.md:626-627`）；`BAKER:567-571` 的 `blendPixel` 对 alpha≤0 直接跳过 → 绑定线路的色条**完全不可见**。修法：赋色时 `((int) id) | 0xFF000000` | **P1-3** |
| C3 | STATION 绑定双语拆行 | `RESOLVER:121-148` 返回 `[zhJoined, enJoined]`；`PT:1531-1575` 对每项再 `splitBilingualName`（`PT:1636-1644`） | 英文串没有 `|`，被当作中文追加到 `zhBuilder`；`enBuilder` 永远空 → **英文被拼进中文行、英文行不更新**（单站已错，换乘站更乱）。应直接把 list[0]/list[1] 当 zh/en，或改成 per-station 的 `zh|en` 列表 | **P1-4** |
| C4 | 动态宽度世界渲染比例 | `RRS:151-157`、`RRW:137-143` 用 `layout.layoutWidth()/layoutHeight()` 算 `fitScale` | 动态模式实际宽度 = `frame.width`（`PT:189-200`），而 `LAYOUT:108-110` 的 `layoutWidth()` 返回**存储 width（固定 2048）** → 动态牌在世界里被错误缩放/横向拉伸（编辑器预览正常，因用 frame） | **P1-5** |
| C5 | 出口元素固定背景色 | `ELEM:264,448` `0xFFFCED22` | 规格 `EXIT_COLOR=#F7D917`（`SPEC §4.4.8`/附录 B、`EXTRACT §11` 明确「做错点」）；且同文件 palette 缩略图又用 `#F7D917`（`ED:931`）自相矛盾 | P2-22 |
| C6 | 默认灰框色 | `LAYOUT:40,251`、`FR:46` `#9AA3AD` | 规格新牌默认 `#4A4A4A`、旧档缺色回退 `#808080`（`SPEC §4.1/4.8.4`） | P2-22 |
| C7 | 图标颜色 | `PT:460-483` `drawTexture(...,-1)`/`drawTextureFlipped(...,-1)` | 规格 §5.7.10 用 `props.color` 替换 `fill="black"`；实现完全不染色（`BAKER` 的 `CanvasSink` 其实支持 tint，见 `BAKER:622-627`）→ UI 的图标颜色控件无效 | **P1-9** |
| C8 | 图标映射有损 | `PT:1698-1745` | 20 个服务图标里 `accessible*` 四个全部映射到 `crt_accessible_lift`、`nursing/restroom/accessible_restroom` 映射到同一贴图、`metro/train` 都是 `crt_railway`；与网页 32 图标不完全对应 | P2-25 |
| C9 | 字体替换（无法避免，但影响宽度/观感） | `CustomFontTextureCache FontType`：ALIBABA regular / SOURCE_HAN **Bold** / SOURCE_SANS_3 **Bold** | 数字字体是 Source Sans 3 **Bold**，网页是 Frutiger 400；中文粗体用 Source Han Bold。会让数字比网页粗、墨区宽度略不同（内部自洽，但与网页不逐像素） | P2-9（记录） |
| C10 | 命中测试与绘制取整 | `FR:147-148` 用 `element.x`（浮点）；绘制用 `floor(x)`（`FR:111-113`） | 命中框与视觉框最多差 1 设计像素；重叠元素时可能选错边缘 | P2-27 |
| C11 | 小牌被放大 | `PV:58-63` `contentRatio=min(w/fw,h/fh)` 可 >1 | 规格 `fitSignDisplay` **永不放大**（`SPEC §2.9`、§9.8），小牌（如 1 格牌）会被放大到画布大小，文字失真 | P2-5 |
| C12 | 左栏/预设缩略图 API 未接线 | `PT:640-688` 的 `drawElementStandalone/bakeElementStandalone/standaloneWidth` 全仓无调用 | 本该用于卡片/预设缩略图（契约 A3），现在卡片是示意图 | P1-10 |
| C13 | 数字线路空 `lines` 被补默认 | `ELEM:426-428` | 网页允许空数组（`SPEC §4.4.5` 可空）；Java 会补一条「1 号线红」，导入空数组项目时内容凭空出现 | P2-22 |
| C14 | `paddingAuto` 反序列化默认 | `ELEM:385-387` | 网页只在显式 `true` 时保留，旧档/未标记视为手动（`SPEC §4.5.2`）；Java 缺省保留 true，会悄悄改旧档内边距 | P2-22 |
| C15 | 文本基线/宽度的字体测量 | `TM:223-250` 全部来自同一 `GlyphVector` | 与 EXTRACT §1 要求一致；`ascent` 在无墨时回退 0.75em 与网页一致。**未发现测量器自洽性问题** | ✅ |

**C 结论**：几何/计量公式复刻质量较高（逐条对照未发现公式级错误），真正的正确性缺陷集中在**颜色通道**（textColor/线路色/图标色）、**数据拆行**（车站）、**动态宽度**三处，以及两处规格常量（出口黄、灰框色）。

---

## 4. 运行时风险（要求 D）

> 标注：🔴 必现/崩溃；🟠 条件性崩溃或明显劣化；🟡 可接受但建议修。

### 4.1 崩溃/异常

| # | 风险 | 证据 | 触发条件 | 级别 |
|---|---|---|---|---|
| D1 | **删除元素首点 NPE** | `ED:223` 字段初始 null；`ED:1328` `!armedDeleteElementId.equals(elementId)` | 右栏选中元素 → 点「删除元素」→ 必崩（Screen 事件里未捕获，会弹崩溃报告） | 🔴 **P0-1** |
| D2 | 极大牌面烘焙内存爆掉 | `PT:748-765` 单维上限 8192；`BAKER:130,354-361` `new int[w*h]` + `NativeImage`；UI 允许 width 32768、rowHeight 2048、行数无上限（`ED:2148`、`ED:1538-1541`） | 极端设置（如 32768 宽 × 多行）→ 8192×4096 级别画布，int[]+NativeImage 各约 134MB，两倍分配 → 可能 OOM | 🟠 P1-11 |
| D3 | 大字号长文本的贴图 | `FTR:40-53` 先按 stringWidth+padding 开 `BufferedImage`；行高 2048 + 大文本 24 字符时字体 2662px，图宽可达数万像素 | 大行高 + 长大文本 → `BufferedImage` 分配过大；`FTR` 只 catch `Exception`，OOM 不被捕获 | 🟠 同 P1-11 |
| D4 | 布局 JSON 大小无上限（**非保存包问题**） | `ED` 无元素数/行数/JSON 长度限制（`ED:1245-1262`、`ED:1538-1541`）；已用 `javap` 核实 MTR 的 `PacketBufferSender`/`PacketBufferReceiver` 支持跨包拆分与重组（`MAX_PACKET_BYTES=32751`，接收端按随机 key 合并），**C2S 保存包本身不会因 32KB 失败**。真正的边界是：超大 JSON 会写进方块实体 NBT，经 vanilla `getUpdateTag/getUpdatePacket` 同步（单包 >2MB 会出问题），以及每次保存服务端反射遍历的耗时 | 约 6000+ 元素才会触发；仍建议加软上限与提示（防御性） | 🟡 P2-41 |
| D5 | 第三方数据异常 | `RESOLVER:246-267`（try/catch）、`PT:1367-1417`（try/catch）、`RRS:172-175`/`RRW:157-160`（布局渲染 try/catch） | 已有多处兜底，整块牌渲染不会把世界渲染搞崩 ✅ | ✅ |
| D6 | 编辑器预览布局异常 | `PV:77-80` 只 catch bake，`layout()` 在 `PV:57` try 之外；`PT.layout` 本身几乎不会抛（除 OOM） | 低 | 🟡 |

### 4.2 越界 / 空指针 / 除零（已逐类排查）

- **唯一确定 NPE**：`ED:1328`（D1）。其余 `.equals` 调用均做了 null 侧或有非空保证（`ED:1560,1577,2495,3376…`）。
- 数组/列表下标：`ED` 内 `rows.get(i)`、`elements.get(i)` 均有边界判断或循环约束；`CRTSignLayout.moveElement`（`LAYOUT:183-200`）下标修正逻辑与网页 `moveElements` 不完全等价，但编辑器主路径不走它。
- 除零：`PT:748-750`（0 返回 1）、`PV:58`（`Math.max(1,…)`）、`ED:2965-2977`（`Math.max(1,…)`）等均有保护；`FR:160` `Math.max(1.0F, rows.get(0).height)` 已保护。
- **除零风险点**：`ED:515-516` 的右栏宽度修正式含除法式逻辑（整数运算，无 /0）；`W:610` `segmentWidth` 用 `Math.max(1,…)` 保护 ✅。
- 空 `rows`：`LAYOUT.fromJson` 保证 ≥1 行（`LAYOUT:261-263`），`IMPORTER` 也至少 1 行 ✅。

### 4.3 性能 / 每帧开销

| # | 风险 | 证据 | 说明 | 级别 |
|---|---|---|---|---|
| D7 | 编辑器每帧 **两次全量布局** | `ED:3331`（drawCanvas 先算一次）→ `PV:57`（Preview 内部再算一次）；`PT:251` 每次 `layout` 都做 `layout.toJson().hashCode()` | 元素多/文本多时每帧双倍测量与 JSON 序列化；`layoutJsonHash` 本身可用缓存字段优化 | 🟡 P2-28 |
| D8 | 世界渲染每帧 `fromJson` + `toJson` | `RRS:99`/`RRW:92` 每帧解析整牌 JSON；`PT:711` 每帧 `layout.toJson().hashCode()`；`RRS:147`/`RRW:132` 每帧 `new CRTSignDataResolver(...)` | 站点牌多时产生持续 GC 压力（对象/字符串）；建议 BE 缓存「字符串引用 + 解析结果」，resolver 缓存在 BE/牌实例上 | 🟡 P2-28 |
| D9 | 静态牌每秒全量重烘 | `BAKER:31,108-111` `REFRESH_MS=1000` 无条件重烘；编辑器预览同样走这里 | 一块牌每秒重跑 AWT 文字栅格化 + 全画布合成 + 纹理上传；站内多牌时持续开销 | 🟡 P2-29 |
| D10 | 大量动态纹理 | `BAKER:34,38,157-165`：每块牌一张 `NativeImageBackedTexture`，CACHE 上限 64 | 2048×256 牌约 2MB/张，64 张 ≈ 128MB 显存 + CPU 侧 NativeImage；大牌更甚 | 🟡 P2-30 |
| D11 | 每键保存/重烘 | `ED:2253-2259` 输入框每次 `notifyChange`→`editContinuous`；`PT:731-745` 每帧 `bakeFrame`（内容指纹变→立即重烘） | 打字时每字符重烘整牌（编辑预览），大牌会有输入延迟；建议输入 debounce 或仅重烘脏区域 | 🟡 P2-29 |
| D12 | 服务端每次保存跑反射遍历 | `BLK:261-269` `setSignLayout`→`updateRouteNumbersFromServer`（`BLK:355-410` 遍历所有 simulator/station/platform/route） | 编辑时离散操作每次保存都会触发；大型存档/多站会有服务端卡顿 | 🟡 P2-26 |
| D13 | 编辑器每帧重建命中几何 | `ED:2844` 每帧 `layoutCanvas()`→`ED:3057-3087` 清空并重建 `canvasRows/canvasElements/rowChrome` 列表（再在 drawCanvas 里重建一次） | 元素多时产生分配压力 | 🟡 P2-28 |

### 4.4 编辑器状态错乱风险

| # | 风险 | 证据 | 说明 | 级别 |
|---|---|---|---|---|
| D14 | 撤销不恢复选择集 | `HIST:20-29` 只存 JSON；`ED:1112-1140` `normalizeSelection` 仅剔除失效 id | 规格 §6.9 要求恢复完整选择；撤销后右栏会掉回设置面板，编辑流断裂 | 🟡 P2-13 |
| D15 | 无效操作也入撤销栈 | `ED:1093-1106` 无条件 `push`；网页会先比较新旧 JSON（`SPEC §6.9`） | 重复点击当前对齐、滑条回到原值等会留下“空撤销步”，用户按 Ctrl+Z 看起来没反应 | 🟡 P2-13 |
| D16 | 文本输入失焦不断组 | `W:961` `onCommit` 从未被赋值（`ED` 构造 TextField 时未 set）；`ED` 仅在拖拽/选择/撤销处 `breakHistoryGroup`（`ED:1114,1129,3762,3840,3886,3896`） | 同一字段「输入→失焦→800ms 内再输入」会并成一步（网页 blur 断组） | 🟡 P2-13 |
| D17 | 粘贴失败仍留下空历史 | `ED:1211-1215` 先 pushHistory 再 paste，失败直接 return | 撤销一次无效果，用户困惑 | 🟡 |
| D18 | 文本字段会截断已有长文本 | `W:996-1006`（聚焦时按 maxLength 截断本地副本）、`W:1167-1175`（失焦提交）；`ED:2254,2258,2269,2278,2282,2300,2327` 设了 12–64 的上限 | 导入网页项目里较长的文本后，只要点进该字段再点走，就会被截断（数据丢失） | 🟡 P2-16 |
| D19 | 危险操作 armed 状态可能残留 | `ED:218-223` 三个 armed 字段；`ED:1551-1568`/`ED:1576-1586` | 第一次点击「清空标识牌」后去做了很多别的事，很久后再点一次会直接执行（不再二次确认） | 🟡 P2-21 |
| D20 | 点击画布不收缩输入框焦点 | `ED:3772-3774`（画布分支直接 return，未 `blurFocused`）；只有空白点击 `ED:3634` 会 blur | 在输入框聚焦时点击画布元素，之后敲键盘仍改旧字段 | 🟡 P2-12 |
| D21 | Esc 行为错位 | `ED:3949-3953` 在 widget 之前拦截 Esc → `W:841-844`（Stepper 取消编辑）/`W:1125-1128`（TextField 取消）成为**死代码**；Esc 实际触发 onBlur→commit（提交而非取消）；另外 `ED:3971-3980` 在无选择时直接关屏 | 与网页「Esc 取消输入/Esc 只取消选择」不一致；用户想撤销输入按 Esc 反而保存了 | 🟡 P2-11 |
| D22 | Esc 不取消进行中的拖拽 | `ED:3944-4051` 无拖拽清理；`elementDragging/rowDragging` 只在 `mouseReleased`（`ED:3832-3862`）复位 | 拖拽中按 Esc 会先清选择/关屏，但随后松手仍会执行移动；极端下状态残留 | 🟡 P2-11 |
| D23 | 绑定的 MTR 数据不刷新 | `HOST:44-74` 构造时快照列表；`ED:464-470` tick 只处理保存；`ED:1661-1758` 解析值在面板重建时才刷新 | 刚进服/数据未同步时打开编辑器，列表会一直为空；MTR 改名/改线后需重开屏幕 | 🟡 P2-27 |
| D24 | 多选跨通道拖拽落点 | `ED:1466` 用第一个选中元素决定 lane；`ED:1485-1489` 的数组下标换算只处理同 lane 情形 | 选中集合跨左/中/右通道时，跨行拖拽可能落到错误位置 | 🟡 P2-9 |

### 4.5 网络 / 存档

- ✅ 保存链路：`ED.saveNow` → `HOST.save`（JSON 去重）→ `PKT`（C2S）→ 服务端 `setSignLayout` → `markDirty2`（MTR 的 `markDirty2` 反汇编确认会调用 `Level.sendBlockUpdated`，且 `BlockEntityExtension` 覆写 `getUpdatePacket/getUpdateTag`）→ 其他客户端收到 BE NBT → 渲染。**链路完整**（`HOST:77-87`、`PKT:42-64`、`BLK:261-269`；注册见 `Init:57`）。
- 🟠 服务端 `PacketUpdateCRTSignLayout` 无权限/距离/JSON 校验（`PKT:42-64`），任意客户端可向已加载区块的任意方块实体写入任意长度的布局字符串 → 滥用/DoS 面（P2-31）。
- 🟠 无并发编辑保护：两名玩家同时编辑同一牌，后保存者覆盖（无锁/无版本号）——网页有多实例守卫，MC 端目前没有（P2-31）。
- 🟠 旧存档迁移是「打开编辑器首次修改即整牌切换渲染模型」：关闭不改不写回（baseline 机制 `CPH:74`、`HOST:77-87` ✅），但首次编辑后外观会从旧格子渲染切到布局渲染，可能出现肉眼可见变化（尤其线路色条透明、间距变化）——需要回归测试。

---

## 5. MTR 联动核对（要求 E）

### 5.1 链路与选择/显示逐项

| 能力 | 选择 | 显示解析 | 结论 |
|---|---|---|---|
| 线路名 | `HOST:44-72` 用当前站线路色构造路由列表；绑定 `DataBinding.ROUTE_NAME` | `RESOLVER:56-74` → `RESOLVER.splitName`；`PT:1419-1442` | ✅ 可选可显示 |
| 线路编号 | 同上（线路色作为 id） | `RESOLVER:76-101`：优先 BE NBT `getRouteNumber(color)`，否则从线路名取前导数字/字母 | ◐：**layout 里的 bindingIds 不会被服务端解析**（`BLK:355-410` 只收集 legacy `selectedIds/cellColors`），新布局绑定的线路编号依赖名称推导；「环线/机场线」等无前导数字的线路显示不出编号（P2-26） |
| 线路色条 | 通过线路绑定 | `PT:1473-1529` → `LineItem.color` | ❌ **P1-3**：RGB 未补 alpha → 色条不可见 |
| 车站（单站） | `HOST:54-59`（本站+连接站） | `RESOLVER:121-148` → `PT:1531-1575` | ❌ **P1-4**：英文被并入中文行、textEn 不更新 |
| 车站（换乘多站） | 同上 | 同上 | ❌ 同上（更明显：中英全挤在中文行） |
| 出口 | `HOST:54` `getStationExits(station,true)`（含父字母+真实出口，符合 MTR 官方） | `RESOLVER:150-161` `deserializeExit` → `PT` EXIT code | ◐ 可选可显示；显示为整串 code（未按 MTR「首字母大、后续编号小」排版，P2-36）；选择器会出现 A 与 A1 两项（P2） |
| 站台 | `HOST:55` `PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(...))` ✅（注意副本，符合 doc §10.6 陷阱） | `RESOLVER:162-173`（只取中文主名）→ `PT` PLATFORM | ◐ 可选、中文可显示；英文不解析（英文行保留字面文本，P2-37） |
| 上一站 / 下一站 | 用站台列表选择（`ED:1610-1618`） | `RESOLVER:175-217`（`getPlatformIndex` + 环线回绕）→ `PT applyBilingualName` | ✅ 可选择/可显示；同一站台属于多条线时取第一条（P2-38） |
| 实时性 | — | `CTX` 由屏幕/世界渲染每次 layout 重新调用 | ✅ 世界渲染 250ms 帧缓存 + 1s 烘焙节流内会刷新；编辑器绑定面板的「当前值」不主动刷新（P2-27） |
| 保存/同步 | — | `PKT` → NBT → BE update → 客户端 | ✅ 完整（见 4.5） |
| 旧 `signIds` 存档兼容 | — | `CPH:68-78`（无布局时 `IMPORTER.convert(signIds, selectedIds, routeNumberGetter)`）、`CPH:111-118`（墙牌） | ◐：一行布局、每格 256px、空格转 `space`、per-cell 颜色未导入（`IMPORTER:33-124`）、非文本/线路/出口的 signId 全变 icon（`IMPORTER:115-123`）；首次保存切换为布局渲染 |

### 5.2 关键结论

1. **选择与显示链路「有」，但三处渲染/数据缺陷会让常见绑定（线路色条、车站双语）看起来是坏的**——这三项是 MTR 联动验收的拦路虎。
2. 服务端 routeNumber 解析只服务 legacy `selectedIds`，**布局内 bindingIds 没有进入解析范围**，需要补：解析 `crt_sign_layout` 里所有线路色并写入 `routeNumbers`，或在保存时把 layout 用到的颜色并入 `selectedIds` 后解析（注意不要把不同语义的 id 混进同一个集合，参照 `docs/MTR_DATA_BINDING.md §10.6`）。
3. 换乘站请改用 MTR 官方 `IGui.mergeStations` 或把 `CRTSignDataContext.getStationNames` 的返回语义改清晰（当前「已合并的 [zh,en]」与 Painter 的「逐站名」假设不一致）。

---

## 6. 问题清单（按优先级，含修改建议）

### 6.1 P0（必崩 / 核心功能不可用）

#### P0-1 删除元素按钮首次点击空指针崩溃 🔴
- **证据**：`ED:223` `@Nullable private String armedDeleteElementId;`（初始 null）；`ED:1328` `if (!armedDeleteElementId.equals(elementId))`；按钮入口 `ED:1323-1343`（右栏 `ED:2493-2499`）。
- **影响**：选中任意元素 → 点右栏「删除元素」→ `NullPointerException`，Screen 事件未捕获，游戏崩溃/踢回主界面。
- **修改建议**：改为 `if (!elementId.equals(armedDeleteElementId))`（或用 `java.util.Objects.equals`）；同时建议顺手把三处 armed 状态在「模式切换/选择变化/其他危险操作」时清空（见 P2-21）。

### 6.2 P1（规格要求但缺失 / 明显功能缺陷）

#### P1-1 相邻自动内边距 `applyPaddingAuto` 完全未实现
- **证据**：规格 §4.5.2 / EXTRACT §14；全仓 grep 无 `applyPaddingAuto`；数据层只有字段（`ELEM:147-148,385-387`），编辑器只有开关（`ED:2385-2386`），绘制直接使用 `element.padding*`（`PT:818-824`）。
- **影响**：默认新建元素全部 0.2/0.2，相邻同通道元素视觉间距 = 0.4h（规格应为 0.2h），整牌排版整体偏松；`paddingAuto` 开关是假的。
- **修改建议**：新增 `CRTSignLayout.applyPaddingAuto()`（公共端纯函数，照 EXTRACT §14 伪代码：逐行、逐元素、只看数组 i±1、同通道、贴右通道左右互换、space 跳过、未开 auto 跳过、邻侧 0.1/外侧 0.2），在每次编辑 mutation 后、进历史/保存前调用（在 `ED.editDiscrete/editContinuous` 或统一 `markEdited` 前执行），并让手动改左右内边距继续置 `paddingAuto=false`。补单元测试用 EXTRACT §21 的用例。

#### P1-2 number-line / text-line 的 `textColor` 无效（画的是 `color`）
- **证据**：`PT:323` `contentColor = element.color`；`PT:336-337` `NUMBER_LINE` → `drawNumberLine(..., contentColor)`；`PT:339-340` `TEXT_LINE` → `drawTextLine(..., contentColor)`；`PT:410-419`/`PT:423-439` 用该颜色画数字/标签/大字。UI 同时暴露 `color` 与 `textColor`（`ED:2433-2446,2448-2455`）。
- **影响**：用户改「文字颜色」无任何效果；改「元素颜色」反而改文字。与 §4.4.5/§4.4.6、§5.7.5/§5.7.6 不符。
- **修改建议**：`drawElement` 对 `NUMBER_LINE`/`TEXT_LINE` 传 `element.textColor`（色条仍用 `entry.color` / `element.blockColor`）；对这两种类型隐藏通用“元素颜色”控件，避免双色控件困惑。

#### P1-3 MTR 线路绑定的色条透明不可见（RGB 未补 alpha）
- **证据**：`PT:1435`（`applyResolvedRouteName` 的 number-line 分支）、`PT:1452`（`applyResolvedRouteNumber`）、`PT:1487`（`applyResolvedRouteLines`）均 `new LineItem(..., (int) color)`；MTR 颜色为 `getColor()&0xFFFFFF`（`docs/MTR_DATA_BINDING.md:626-627`）；`BAKER:567-571` 的 `blendPixel` 对 alpha=0 直接 return；`BAKER:371-382`/`617-619` 的 ARGB→ABGR 会保留 alpha。
- **影响**：任何通过 UI 绑定线路的 number-line / legacy ROUTE_LINES 转换，色条全部透明；牌面只剩数字。核心 MTR 联动不可用。
- **修改建议**：所有把 MTR 线路颜色写入 `LineItem.color`/元素颜色的地方统一 `| 0xFF000000`（或抽一个 `resolveArgbColor(long id)` 工具），并在 `CRTSignLayoutPainter.resolve` 入口做一次归一化；补测试：绑定一条 MTR 线路 → 色条不透明。

#### P1-4 STATION 绑定双语拆行错误（英文混入中文行 / 英文行不更新）
- **证据**：`RESOLVER:121-148` 返回 `List<String>`，其中 `[0]=多站中文合并串`、`[1]=多站英文合并串`；`PT:1531-1575` 对每个串调用 `splitBilingualName`（`PT:1636-1644` 按 `|` 拆）→ 英文串无 `|` 被视为中文；`enBuilder` 永远为空。
- **影响**：车站/换乘站绑定的双语文本（bilingual / small-bilingual / big-number / entrance / exit / text-line）全部错误：中文行出现「南京东路 / Nanjing East Road」，英文行停留在元素的字面默认值（如 “Station”）；换乘站更乱。
- **修改建议**：二选一并统一契约：
  1. 让 `CRTSignDataContext.getStationNames` 返回「每站一条 `zh|en`」的列表（推荐，语义清晰），Painter 逐站拆行后再合并；
  2. 或保留 `[zh,en]` 语义，Painter 直接 `content.textZh = list.get(0); content.textEn = list.size()>1?list.get(1):""`，不再调用 `splitBilingualName`。
  同时 `describeBindingValue`（`ED:1697-1710`）也按同一语义调整显示。

#### P1-5 动态宽度牌在游戏世界里的缩放/比例错误
- **证据**：`RRS:151-157`、`RRW:137-143` 用 `layout.layoutWidth()/layoutHeight()` 计算 `fitScale`；而 `LAYOUT:107-110` 的 `layoutWidth()` 明确「动态模式暂时按固定 width 处理」；实际帧宽是 `PT:189-200` 计算出的 `frame.width`（bake 结果 `Baked.width` 就是它，`BAKER:57-65,160`）。
- **影响**：动态宽度牌的编辑器预览正常、进世界后被横向拉伸/错位（宽高比不一致时既变形又有大留白），动态模式实际不可用。
- **修改建议**：世界渲染直接用 bake 结果：`fitScale = min(plateWidth / baked.width, plateHeight / baked.height)`（`baked.width/height` 已是 frame 尺寸），或给 `CRTSignLayout` 增加 `layoutWidth(data,measure)` 与 Painter 共用；墙牌同理。

#### P1-6 浮动元素工具条 + 内边距拖拽缺失
- **证据**：无 `#element-toolbar` 对应控件；微调只有键盘 ←/→（`ED:4023-4034`）、内边距只有 ±步进（`ED:2387-2426`）、删除在右栏（`ED:2493-2499`）；`W` 无横向拖拽控件，`ED` 无 `padding-scrub` 会话。
- **影响**：规格 §3.2.6/§6.6/§6.7 的核心编辑交互不可达；用户在画布上改间距/微调/删除的路径缺失，必须绕右侧面板，体验与网页差距大。
- **修改建议**（可拆两步）：
  1. **低成本**：在选中元素框下方（放不下则上方）自绘一个迷你工具条：`← / →`、左右 padding 值（点击可输入）、`删除`；数据走 `moveSelectionBy`/`editContinuous`（键 `id:paddingLeft/right`）。对齐下拉可暂仍放在右栏。
  2. **内边距 scrubbing**：在 `Slider`/`Stepper` 的左右值区域加 `mouseDragged` 档位（8 GUI px = 0.05，按 GUI 缩放折算），使用固定 continuous key 合并为一步撤销，结束/开始时 `breakHistoryGroup`。
- 若本轮确实不做，建议至少在文档/验收里标为“延后”，避免验收误判。

#### P1-7 预设系统整体缺失
- **证据**：`ED:767-831` 只构建元素分类，无预设 tab；行按钮无“存为预设”（`ED:424-432`）；右栏无 `preset` 模式（`ED:1080-1089` 仅 3 种）；全仓无 preset 代码，`lang` 也无 preset 键。
- **影响**：规格 §3.1.3（预设标签页/预设卡片/拖入空行）、§3.3.4（预设预览/添加为新行）、§7.3 全部不可用；行按钮缺一个规格交互。
- **修改建议**：产品先定存储位置（推荐 `config/nanbin/crt_sign_presets.json`，结构照 §7.3），实现：①行按钮「存为预设」+ 命名输入；②左栏「预设」tab 卡片（缩略图用 P1-10 的离屏渲染）；③右栏第 4 种模式 `PRESET`（缩略图 + 元素说明 + 添加为新行 + 删除）；④拖入空行。可复用 `CLIP` 的 JSON 元素结构与 `IMPORTER` 的命名工具。若本轮不做，需在范围清单中明示。

#### P1-8 项目 JSON 导入/导出缺失（跨端互操作目标未闭环）
- **证据**：数据层 `LAYOUT:210-269` 已实现 `toJson/fromJson`（结构与 §7.1 一致），但编辑器没有任何导出/导入入口（右栏设置到 `ED:2221-2225` 结束；顶栏只有保存/撤销/收起，`ED:660-688`）。
- **影响**：规格 §3.3.1 #11 与 §7.1 未实现；网页项目 JSON 无法带进游戏（只能靠旧 signIds 转换），游戏牌面也无法导出给网页另存/分享；这是本任务“把网页编辑器功能复刻进游戏”的明显缺口。
- **修改建议**（成本可控）：
  1. 最小闭环：右栏加「复制布局 JSON / 从剪贴板导入 JSON」两个按钮，用 `GLFW.glfwGetClipboardString/glfwSetClipboardString`，导入前 `CRTSignLayout.fromJson` 校验并 toast 中文错误；这条同时解决与网页的互通。
  2. 完整版：`config/nanbin/signs/*.json` 文件列表 Screen（导入/导出/删除），文件名时间戳，格式 `{version:1,sign:{…}}`。
- 若本轮不做，建议列为“明确延后”并在 §F 说明，避免验收 #14 判失败。

#### P1-9 图标颜色控件无效（规格有、实现忽略）
- **证据**：`PT:460-483` `drawIcon` 对所有贴图传 `-1`（原色），旋转/翻转分支同样；UI 仍显示图标颜色选择器（`ED:2433-2435`）；`BAKER:622-627` 的 `CanvasSink.applyTint` 支持染色但未被调用。
- **影响**：用户改图标颜色无效果（且规格 §5.7.10 的 `fill="black"` 替换语义缺失）；与 number-line/text-line 的无效颜色控件叠加，会让人觉得「颜色面板坏了」。
- **修改建议**：二选一：①用 tint 实现（`drawIcon` 传 `element.color`；注意 signs.json 贴图可能是多色，tint 是乘法染色，视觉与网页的「只换黑色路径」不完全等价，需实机看）；②若不做，隐藏 icon 的「颜色」控件并加提示「图标使用贴图原色」，避免误导。推荐②作为本轮折中，①列入后续。

#### P1-10 左栏元素卡片/图标选择器不是真实元素渲染（观感硬伤）
- **证据**：`ED:899-945` 手绘示意图（数字线路画一个黄块 + “3”、出入口画蓝块 + “C”……）；`PT:640-688` 的 `drawElementStandalone/bakeElementStandalone/standaloneWidth` 全仓无调用（本应服务卡片/预设缩略图，见契约 A3）；图标选择器用 32 个文字符号，其中 ♿×4、卫/婴等重复（`ED:97-106`、`ED:2502-2527`）。
- **影响**：与规格 §3.1.1「示例元素渲染成 mini SVG」、§2.8「图标网格」差距明显；同组图标无法区分（只能靠 tooltip 里的 id）。
- **修改建议**：卡片/图标格改用离屏渲染：对每个元素/图标调 `PT.bakeElementStandalone`（图标可用 `renderElement` 单元素烘焙，rowHeight 取卡片高度），结果按 `(type, icon, rowHeight)` 缓存 `Identifier`，卡片绘制时贴图；图标格用真实 signs.json 贴图（`RenderRailwaySign.getSign` + `sink.drawTexture` 到临时 NativeImage 或直接 `drawTexture` 在 GUI 里绘制）。成本中等、观感提升最大。

#### P1-11 极端尺寸下的烘焙内存/文本贴图 OOM 风险
- **证据**：`PT:748-765` 只压单维到 8192；`BAKER:130,354-361` `int[]` + `NativeImage` 双份内存；`FTR:40-53` 按 `stringWidth+padding` 开 BufferedImage，大行高 + 长文本可达数万像素宽；UI 允许 width 32768（`ED:2148`）、rowHeight 2048（`ED:2100`）、行数无上限（`ED:1538-1541`）。
- **影响**：误操作/恶意数据可让客户端 OOM（不是普通崩溃，恢复更差）。
- **修改建议**：①`bakeDensity` 加**总像素上限**（如 `width*height*density² ≤ 16M`，超过则继续降密度或拒绝烘焙并 toast）；②`FTR.render` 前置检查 `textWidth/textHeight` 上限（如 8192），超限缩字号或返回 null（并让上层有兜底）；③编辑设置面板对 `width×rowHeight×rows` 给软上限与提示；④长文本字段仍建议保留长度上限（见 P2-16 的数据丢失注意）。

> 注：曾怀疑“布局 JSON 超过 MTR 32KB 包上限会导致保存失败”，经 `javap` 复核 `PacketBufferSender/PacketBufferReceiver` 支持跨包拆分与重组，**该怀疑不成立**；相关防御性建议降级为 P2-41。

### 6.3 P2（细节 / 打磨）

> 按主题分组，均为「不改也能跑，但影响观感/一致性/可维护性」。P2 编号保持首次出现顺序，组内排列不代表优先级。

**数据与互操作**
- **P2-22 模型常量与规格不符**：`frameColor` 默认 `#9AA3AD`（`LAYOUT:40,251`，`FR:46`）应为 `#4A4A4A`/旧档 `#808080`；exit 背景 `#FCED22`（`ELEM:264,448`）应为 `#F7D917`；`createBlank` rowHeight=256（`LAYOUT:78-83`）与规格默认 192 不同（内联注释说明是为贴合方块比例，属有意适配，但应在文档里写明）；number-line 空 `lines` 会被补默认（`ELEM:426-428`）；`paddingAuto` 缺省 true（`ELEM:385-387`）。建议：常量集中改为规格值（exit/灰框），rowHeight 适配保留但注释升级为「协议差异」；空 lines 保持空；`paddingAuto` 只在显式 true 时 true。
- **P2-23 JSON 互操作细节**：未知 `props` 字段在 Java 反向序列化时丢失（`ELEM:364-473` 只读取已知字段；网页会保留，`SPEC §4.4.11`）；`toHex` 会输出 `#AARRGGBB`（`ELEM:548-553`），网页只认 `#RRGGBB`；导出后回导可能丢扩展字段/颜色格式不标准。建议：保留原始 `props` JSON 中未识别的键（或至少保留 `mtrBinding` 之外的未知键），颜色统一输出 6 位。
- **P2-24 旧存档转换体验**：空 signIds 全转成 `space`（`IMPORTER:54-61`），导致新牌打开即“非空”、空态提示不出现（`ED:3363`）；per-cell 颜色未导入（`IMPORTER` 未读 `cellColors`）；非文本 signId 全变 icon（`IMPORTER:115-123`）。建议：全空牌直接给空行；cellColors 映射到线路元素颜色；对 `crt_*` 图标保留原 signId（已做）；在转换后给一次性 toast 说明“已转换为可编辑布局”。
- **P2-25 图标库映射有损/选择器不可辨**：`PT:1698-1745` 多个图标合并到同一贴图；`ED:97-106` 的符号重复。建议：补真实贴图缩略图（同 P1-10），并逐个校对 20 个服务图标映射（`accessible_*` 四种至少用不同贴图或标注同名）。

**UI/视觉**
- **P2-1 无字号层级 & 主题常量为死代码**：`TH:67-96` 的尺寸/字号常量**全部未被引用**（另有 `TH:56,59-62` 的 CANVAS_SHADOW/ROW_BAND/ROW_BAND_HOVER/DRAG_INDICATOR/HITBOX 也未用）；实际全部文字使用 9px 默认字体（`W:56,238-243`）。建议：至少给面板标题/分组标题/正文做 1.2–1.35 倍缩放（`Ctx.scaledText` 已有），并删除或统一 `TH` 尺寸常量，避免“主题看起来做了但没用”。
- **P2-2 面板头/圆角/间距 token**：`ED:70-78`（PANEL_HEADER 22、TOP_BAR 20）与规格 40px 面板头；`W:42-53` 圆角 6/4 与规格 8/6。属 GUI 逻辑像素适配，可保留，但建议在 `TH` 注释里说明与 CSS px 的换算。
- **P2-3 Toast/确认框**：`ED:3001-3018` 白底 toast（规格深色 `#1F2733`）；危险操作两段点击（`ED:218-223,1551-1586`）而非模态。建议：toast 改深色或至少深色文字+浅色底按规格；若继续用两段点击，需确保 armed 状态在其它操作后重置（P2-21）。
- **P2-4 画布细节**：无牌面阴影（`PV:65-110`）、选中框实线/双虚线替代 2.5px 虚线（`PV:112-127`、`ED:3474-3482`）、行带颜色与规格不同（`PV:94`）；建议至少补外层阴影与虚线绘制（`Ctx.dashedRect` 已有）。
- **P2-5 小牌被放大**：`PV:58` `contentRatio` 可 >1；建议 `ratio = min(..., 1.0f)` 且缩放留 1.0 上限（与网页 `fitSignDisplay` 一致），用户可通过 zoom 主动放大。
- **P2-6 放大后无横向平移**：`ED:3935-3938` 只改 `canvasScrollY`，`canvasScrollX` 无输入路径；建议 Shift+滚轮或中键拖拽平移，或画布空白处拖拽平移。
- **P2-7 缩放范围**：`ED:3708` 0.25–6；规格 0.5–4。建议对齐规格（或保留增强但文档标注）。
- **P2-32 三栏在窄逻辑分辨率下挤压**：`ED:504-521`；GUI3/4 时侧栏占比过半、画布过小。建议：侧栏宽度改为 `min(260, screenWidth*0.22)` 之类的比例上限，或提供“专注画布”一键收起两侧（顶栏已有 toggle，可加提示）。
- **P2-33 多语言**：仅 `en_us.json`/`zh_cn.json` 含 185 个 `gui.nanbin.crt_editor.*` 键；de/fr/ja/ko/pt/ru/es 无（`fabric/src/main/resources/assets/nanbin/lang/`）。MC 会回退 en_us，可接受；若目标用户含其他语言，至少给 zh_cn/en_us 之外的 0 键加注。

**交互**
- **P2-8 元素拖拽无幽灵**：`ED:3893-3901` 只有 drop 指示；规格 §2.10 `.drag-ghost`。建议：拖拽激活时绘制半透明元素快照（可用 `bakeElementStandalone` 或简单色块）跟随鼠标。
- **P2-9 落点不按通道**：`ED:3660-3705` 对所有通道元素按 x 排序算 visualGap；`ED:1456-1489` 用第一个选中元素决定 lane；`ED:3686` 用中点判定。建议照 EXTRACT §15 重写 `dropTargetAt`（按 lane 过滤、贴右通道数组序反转、`draggedWidth` 用于空通道居中），并在多选时分别处理 lane；指示线 x 也按 lane 计算。
- **P2-10 Ctrl+Shift 范围并集缺失**：`ED:3608-3625` ctrl 分支优先。建议：`if (ctrl && shift) → rangeSelect + 并集`，或统一在 `handleCanvasClick` 里先算模式。
- **P2-11 Esc 语义**：见 D21/D22。建议：Esc 优先级 = 取消拖拽 > 取消控件编辑（不要自动 commit）> 取消选择 > 关屏；屏幕不要把 Esc 无条件先吞掉（可让控件先处理）。
- **P2-12 点击画布不 blur 输入框**：`ED:3772-3774`/`ED:3588-3639`。建议在 `handleCanvasClick` 开头 `blurFocused()`（除非点击的是画布内可编辑控件）。
- **P2-13 撤销细节**：选择集恢复（`HIST.Entry` 加 selection 快照）、no-op 不 push（比较 `layout.toJson()`）、blur 断组（给 `TextField` set `onCommit` → `breakHistoryGroup`）、`Ctrl+Y` 可保留为增强。
- **P2-14 内边距步进多步合并**：`ED:2387-2426` 走 `editContinuous` 同 key 800ms 合并；规格 §6.7 要求 ▲▼ 每次点击单独一步。建议步进按钮改走 `editDiscrete`（或给 stepper 单独 key 带时间戳强制 push）。
- **P2-15 剪贴板入口/系统剪贴板**：`ED:727-731` 空剪贴板禁用粘贴（规格始终可用 + 报错 toast）；`ED:136-141,1160-1227` 只有内部副本。建议：按钮始终可用；提供“复制 JSON 到系统剪贴板/从系统剪贴板导入”两个动作（与 P1-8 共用）。
- **P2-16 文本长度上限导致截断**：`W:996-1006,1167-1175` + `ED` 的 maxLength（12–64）。建议：maxLength 放大到规格无上限的近似值（如 256），或聚焦时若超长不截断、只限制新增字符；避免“点一下字段就丢字”。
- **P2-17/18 批量面板**：混合值无「不同」显示（`ED:2580-2595,2612-2614`）；批量背景会改出口黄（`ED:2602-2603`，规格要求出口固定）；缺“出口保持固定黄色”提示。建议：比较所有选中值，不同则显示占位/禁用；批量背景跳过 EXIT 并提示。
- **P2-19 控件重复/范围**：内容对齐在 NUMBER_LINE 内容区与排版区各出现一次（`ED:2290-2292` vs `2379-2383`）、EXIT 同样（`2329-2333` vs `2379-2383`）；arrow 粗细 0.05–0.95 与规格 UI 0.1–0.5（`ED:2359-2362`）。建议去重、对齐规格范围。
- **P2-20 无意义绑定项**：`ED:2458-2491` 对 SPACE/ARROW/ICON 也显示 MTR 绑定下拉，但 Painter 无对应消费（`PT:1367-1417` 只填 content 字段，icon/arrow/space 的 metrics 不用）。建议按类型隐藏绑定区（或只给可绑定的类型）。
- **P2-21 armed 状态残留**：`ED:218-223` 三个 armed 字段只在再次点击同一目标时重置。建议：任何模式切换/选择变化/其它危险操作时清空；或统一改成 `ConfirmScreen`（规格更接近模态）。
- **P2-26 服务端 routeNumber 解析范围**：见 5.1/§4.5；建议 `setSignLayout` 时同时解析 layout 用到的线路色，写入 `routeNumbers`（注意集合按语义分桶）。
- **P2-27 MTR 数据异步刷新**：`HOST:44-74` 列表快照；`ED:464-470` tick 无刷新。建议：`HOST` 提供 `refresh()`（或在 screen tick 里用数据签名变化重建列表/刷新绑定当前值），参考 `docs/MTR_DATA_BINDING.md §10.7`。
- **P2-28 每帧布局/JSON/resolver 性能**：`ED:3331`+`PV:57`（双布局）、`PT:251`（每帧 toJson hash）、`RRS:99`/`RRW:92`（每帧 fromJson）、`PT:711`（每帧 toJson hash）、`RRS:147`/`RRW:132`（每帧 new resolver）。建议：BE 缓存 `layoutJson` 字符串引用 + 解析结果（字符串没变直接复用），Painter 增加 `layoutHash` 缓存参数，编辑器 Preview 接受外部 frame 或加 1 帧缓存，resolver 挂到 BE/牌实例。
- **P2-29 静态牌每秒重烘**：`BAKER:31,108-111`；建议给 `JSSignTextureBaker` 增加“静态模式”（只在 contentKey 变化时烘，不按 1s 刷新），CRT 布局路径用静态模式，JS 动画路径保持原行为。
- **P2-30 动态纹理内存**：`BAKER:34,38,157-165`；建议 CACHE 上限按“总像素”而非条目数（如 ≤64MB），或对世界渲染用更低密度（编辑器预览可高密度）。`PT:91-97` 的 3072×4096 上限也建议改为总面积上限。
- **P2-31 服务端校验/并发**：`PKT:42-64` 增加 JSON 长度上限、`CRTSignLayout.fromJson` 校验失败拒绝、玩家距离/权限检查（如 `serverPlayerEntity` 与 pos 距离 ≤ 32、或要求方块所在区块已加载已满足，但权限可参考 MTR 其他包）；可选加版本号/时间戳避免旧包覆盖新布局。
- **P2-34 死代码/资产清理**：`TH` 尺寸常量、`PT:640-688` standalone API（若 P1-10 不做就会一直闲置）、`W:939-946 blend`、`W:1519-1521 Card.Thumb`、`ED:325-345 TextLine.tooltipOnly`、`HOST:109-112 isWallSign` 未用；`build_compile.log` 是 14:55 的**旧失败日志**（当前源码已验证编译通过），建议删除/更新以免误导。 
- **P2-35 自动线路号配色偏好缺失**：规格 §3.3.1 #9；若实现需要“线路号→线路色”映射（MTR 数据里 routeColor 可直接从 selector 得到，价值有限），可标注为延后。
- **P2-36 出口显示方式**：`PT:443-456` 把 `code` 整串同字号绘制；MTR 出口习惯是“首字母大 + 余下编号小”（`docs/MTR_DATA_BINDING.md §4.3`）。若绑定 EXIT 需要更像 MTR，可在 `drawCodeLabel` 对 EXIT 做拆分渲染（可选）。
- **P2-37 站台英文名**：`RESOLVER:162-173` 只返回中文主名，`PT applyBilingualName` 的英文为空 → 站台绑定的英文行保留字面值。建议 `CRTSignDataContext` 增加 `getPlatformNameEn` 或返回 `zh|en`。
- **P2-38 上一站/下一站多线路歧义**：`RESOLVER:191-217` 取第一个包含该站台的 simplifiedRoute；若站台属于多条线（支线/共线）结果可能不是用户期望。建议把选择器改为「线路+站台」两个字段，或在 UI 显示命中的线路名。
- **P2-39 数字线路空数组/线路号输入显示**：`ED:2298-2301` 输入框本地保存原始串（如 “-1”），模型是 `filterLineNumber` 后的 “1”，失焦前显示与模型不一致；建议在 `TextField` onChange 后回写模型值或对 numeric 输入做即时过滤显示。
- **P2-40 编辑体验**：`ED:2242-2243` 类型标签带短 id；`ED:2493-2498` 删除按钮文本在 armed 时变化 ✅；但无键盘 Tab 焦点遍历、无 tooltip 延迟等（可选）。
- **P2-41 布局大小软上限（防御性）**：`ED` 没有元素/行/JSON 长度上限（`ED:1245-1262`、`ED:1538-1541`）；C2S 保存包由 MTR 自动分片+重组（已 `javap` 核实，不是问题），但超大 JSON 会写入方块实体 NBT 并通过 vanilla `getUpdateTag/getUpdatePacket` 同步（单包 >2MB 会出问题），且每次保存服务端都会反射遍历解析线路编号。建议：限制单牌元素数（如 1000）并在超限时 toast；服务端 `setSignLayout` 对 JSON 长度做检查。

---

## 7. 本轮不建议做（要求 F）

### 7.1 明确剔除（避免范围失控）

1. **`platform.html` 站台吊板/线路图编辑器**：独立编辑器，与导视牌编辑器共享很少代码；本任务规格明确只做 `index.html`（`SPEC §1.1/§3.1` 也把它标为“本轮不要求”）。
2. **SVG / PNG 导出（§7.6/§7.7）**：需要字体内嵌、SVG 序列化、Canvas 光栅化；游戏内世界渲染/预览已用烘焙贴图满足需求。如果以后要分享/印刷再单独立项（注意 §9.4 已建议用离屏烘焙作为导出基础）。
3. **移动端 / 触屏手势 / 行芯片 / 属性带折叠（§2.13、§6.13）**：MC Java 端无此设备形态；如做手柄/触屏适配另立项。
4. **深色模式（§2.14）**：契约明确“浅色主题，不要改成暗色”。
5. **多实例守卫 / localStorage 偏好 / 自动保存到本地（§6.15、§7.5）**：MC 是单实例、数据以服务端 NBT 为权威；偏好可未来改 `config/nanbin/*.json`。
6. **自定义色板管理对话框 / 城市线路色板（§2.8、§7.4、附录 C）**：需要 4 城市 100+ 色数据与 UI，对游戏内输入帮助有限；先保留 12 色固定板 + hex 输入即可。
7. **JS 动画/时钟类牌面能力的实时刷新**：当前 1s 全量重烘已是性能负担（P2-29），不建议把“每帧/每秒动画”引入布局牌。
8. **过渡动画 / 精确虚线样式 / 阴影等纯 CSS 观感细节**：MC 自绘成本高、收益低，按 P2 逐项挑重点即可。

### 7.2 建议延后但需产品决策（不要静默跳过）

- **预设系统（P1-7）** 与 **项目 JSON 导入/导出（P1-8）** 是规格内功能，但都需要“存哪里/如何选文件”的产品决策；如果本轮不实现，请在发布说明和验收清单里明确标为“下一轮”，以免被当作漏做。
- **浮动元素工具条（P1-6）** 建议至少实现最小版（←/→、左右 padding 值、删除），否则网页老用户体验落差最大；完整版（内边距拖拽、对齐下拉）可延后。
- **图标颜色（P1-9）**：若不想做染色，先隐藏控件并注释“贴图原色”，比留一个无效控件好。

---

## 8. 附录：规格 §9.9「复刻验收对照清单」逐条结果

| # | 验收项 | 结果 | 说明 |
|---|---|---|---|
| 1 | 新建默认牌 2048×192、1 行、白底、无灰框；行高锁定联动 | ◐ | 新建走旧存档转换：width=格数×256、rowHeight=256；空牌会被 `space` 填满（`IMPORTER:33-61`）；`createBlank` 也是 2048×256（`LAYOUT:78-83`）；锁比例联动 ✅ |
| 2 | 三栏布局/面板头/收起展开/空态/添加行位置 | ◐ | 三栏/添加行 ✅；收起改为顶栏按钮；空态因空格预填不显示；面板头高度/字号与网页有差 |
| 3 | 十种元素默认渲染（宽度/字号/基线/颜色） | ◐ | 几何/基线按公式 ✅；number-line/text-line 文字颜色 ❌（P1-2）、exit 黄偏差（P2-22）、图标颜色无效（P1-9）、图标映射有损 |
| 4 | 三通道 / 动态模式 | ◐ | 渲染布局 ✅；**动态模式在世界渲染比例错误**（P1-5）；编辑器预览正常 |
| 5 | padding 比例换算（h=192、0.2 → 115.2） | ✅ | `PT:818-831`；推导正确 |
| 6 | paddingAuto（相邻 0.1/外侧 0.2、手改钉住） | ✗ | **算法完全缺失**（P1-1） |
| 7 | 灰框（首末 fw、行间 fw/2、覆盖越界） | ✅ | `PT:292-304`；颜色默认值偏差见 P2-22 |
| 8 | 选中（单选/Ctrl/Shift/全选/空白取消/Esc/批量面板） | ◐ | 全部可达；Ctrl+Shift 并集缺失（P2-10）；Esc 语义偏差（P2-11） |
| 9 | 拖拽（行内/跨行/贴右反转/阈值/幽灵/指示线/撤销一步） | ◐ | 行内/跨行/撤销 ✅；无幽灵（P2-8）、落点不按通道（P2-9） |
| 10 | 内边距拖动（8px=0.05、整段一步、0–8） | ✗ | 只有步进器（P1-6） |
| 11 | 行操作（添加/手柄排序/删除确认/单行不可删/存预设/应用预设） | ◐ | 行操作 ✅；删除为两段点击；**预设相关全 ✗**（P1-7） |
| 12 | 撤销/重做 20 步、800ms 合并、恢复选择 | ◐ | 撤销/重做/800ms ✅（实现上限 64 步，`HIST:15`）；不恢复选择、无 no-op 去重、blur 不断组（P2-13） |
| 13 | 复制/剪切/粘贴 JSON 一致、落点、跨行 offset | ◐ | 数据层 JSON ✅（`CLIP`）；无系统剪贴板/外部 JSON 入口（P2-15）；落点逻辑基本符合 |
| 14 | 项目 JSON 导入/导出往返（含清洗） | ✗ | 无入口（P1-8）；数据层清洗基本可用（未知 props 会丢，P2-23） |
| 15 | 导出 SVG/PNG 或烘焙纹理一致（无编辑器装饰） | ◐ | 烘焙纹理 ✅（游戏内/预览）；无导出能力（本轮剔除，§F） |
| 16 | 移动端/窄窗口 | — | MC 端不适用（§F） |

---

## 9. 附录：审计未覆盖 / 需实机验证

本次为静态审计，以下需实机（或写自动化布局测试）确认：

1. **P0-1 复现**：打开编辑器 → 选中元素 → 点「删除元素」，确认崩溃栈在 `CRTSignEditorScreen:1328`。
2. **P1-2/P1-3/P1-4/P1-9 视觉验证**：改 textColor、绑定线路、绑定车站（含换乘）、改图标颜色，观察牌面/预览与世界渲染。
3. **P1-5 动态宽度**：切动态模式 → 保存 → 关屏 → 看世界牌面比例。
4. **P1-11 极端尺寸**：width 32768/rowHeight 2048 下是否 OOM（建议先修再试）。
5. **P2-41 大布局**：用脚本生成超大布局（数千元素）→ 保存/重载/区块重进，观察方块实体同步与编辑器卡顿（C2S 保存包本身已确认可分片）。
6. **编辑器输入手感**：拖拽阈值（4 vs 5px，且未按 GUI scale 折算）、滚动、缩放、焦点切换（P2-6/11/12）。
7. **世界渲染性能**：站内多牌（>10）时 FPS/GC（P2-28/29/30）。
8. **旧存档回归**：旧 CRT 牌不打开编辑器时外观不变；打开并做一次修改后外观符合预期（P2-24）。

> 说明：`build_compile.log`（仓库根目录）记录的是 2026-11-07 14:55 的一次**失败**编译（32 个旧错误），而当前源码（15:01/15:02 之后）已通过编译；该日志为历史残留，建议删除或重新生成（见 P2-34）。
