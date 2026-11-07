# CRT-Sign-Web-Editor（index.html 部分）完整规格文档

> **用途**：本文件是「另一个团队用 Minecraft Java 版 GUI 复刻该网页导视牌编辑器」的唯一规格来源。
> 内容全部由源码逐行抽取，不臆造；无法从源码确定的地方标注 **「源码未明示」**。
> **素材路径**：`Temp/crt_web_editor/CRT-Sign-Web-Editor-main/`（下文引用文件均相对该目录）。
> **范围**：`index.html` + `style.css` + `fonts.css` + `js/{core,palettes,palette-manager,icons,state,render,ui,clipboard,presets,storage,exporters,color-picker,panel,interact,main}.js`。
> **不在范围**：`platform.html` / `platform.css` / `js/platform/*`（站台吊板与线路图编辑器，本轮不需要）。
>
> **引用约定**：`文件:行号`（如 `render.js:245-274`）。行号以 2026-11 解压得到的该份源码为准。函数名/变量名保持源码英文原文。
> **相关文档**：`docs/EDITOR_MODULE_CONTRACTS.md`（模块契约、已实现的 Java 数据层 `com.Nanbin.signage.crt` 与渲染/编辑模块划分、`CRTEditorTheme`）；`docs/MTR_DATA_BINDING.md`（MTR 数据联动）。网页源码中**没有** MTR 绑定概念，MTR 绑定属 MC 端扩展，不在本规格范围。
> **阅读顺序建议**：第 4 章（数据模型）→ 第 5 章（渲染）→ 第 6 章（交互）；视觉还原查第 2 章；MC 落地查第 9 章与 `WEB_EDITOR_SPEC.extract.md`（伪代码）。

---

## 目录

1. 总览与术语
2. 视觉规范（Web 编辑器界面）
3. 布局与信息架构（三栏各自做什么）
4. 数据模型（SignState / Row / Element / 属性默认值）
5. 渲染规范（坐标系、盒模型、十种元素配方）
6. 交互规格（选择、拖拽、增删、撤销、剪贴板、缩放、快捷键）
7. 导入导出与持久化
8. 已知限制与「源码未明示」清单
9. MC Java 端复刻建议摘要
附录 A：源码文件与行号索引
附录 B：常量速查
附录 C：城市线路配色完整数据

---

# 1. 总览与术语

## 1.1 项目形态

- 纯前端静态页面，无构建、无依赖：`index.html` 直接按顺序加载 15 个 JS 脚本（`index.html:82-96`），样式为 `style.css`（`<link>` 在 `index.html:8`，第一行 `@import url('fonts.css')` 见 `style.css:7`）。
- 页面标题：**「导向标识生成器」**（`index.html:7`）。
- 领域层级：**标识牌 Sign → 行 Row → 元素 Element**（`core.js:7`、`state.js:5-9`）。
- 标识牌有自己的像素坐标系（SVG viewBox 用户单位），与屏幕显示尺寸解耦：牌面内部按「设计像素」排版，界面再等比缩放显示（`ui.js:118-135` `fitSignDisplay`）。
- 数据全部保存在浏览器 `localStorage`（自动保存 + 预设 + 偏好 + 自定义色板）。多个 JS 文件共同维护一个全局可变上下文 `App`（`ui.js:50-259`），状态本身是**不可变数据**：所有编辑函数返回新 `SignState`，通过 `App.update(pureFn)` 提交（`ui.js:67-90`）。

## 1.2 页面骨架

```
body
├─ #loading-overlay            字体/实例检查加载遮罩（index.html:13-16）
├─ #app                         三栏 flex 容器（index.html:18；style.css:132-136）
│  ├─ aside#left-panel          左栏 260px：元素选择区（index.html:21-30）
│  ├─ button#left-expander      左栏收起后的展开把手（index.html:31）
│  ├─ main#center-panel         中栏：自适应宽度编辑区（index.html:34-59）
│  │  ├─ #edit-actions          选择/剪贴板工具条（index.html:35-41）
│  │  ├─ #canvas-scroll         可滚动画布容器（index.html:42）
│  │  │  └─ #canvas-area
│  │  │     ├─ #canvas-wrap     牌面外框（index.html:44-50）
│  │  │     │  ├─ svg#sign-svg  SVG 牌面（index.html:45）
│  │  │     │  ├─ #sign-overlay HTML 覆盖层：行带/手柄/指示器（index.html:46-48）
│  │  │     │  └─ #empty-hint   空状态提示（index.html:49）
│  │  │     ├─ #row-strip       移动端行管理条（index.html:52）
│  │  │     └─ button#add-row-btn 添加行（index.html:53）
│  │  ├─ button#zoom-badge      缩放徽标（仅移动端显示，index.html:57）
│  │  └─ #element-toolbar       选中元素浮动工具条（index.html:58）
│  ├─ button#right-expander     右栏展开把手（index.html:62）
│  └─ aside#right-panel         右栏 260px：属性/设置/导出（index.html:63-74）
├─ #toasts                      Toast 容器（index.html:79）
└─ #modal-root                  模态对话框容器（index.html:80）
```

## 1.3 启动流程与全局约定

1. 多实例守卫（`main.js:172-205`、`storage.js:95-118`）：若 5 秒内有其它标签页心跳，弹确认框；拒绝则整页替换为「已取消启动…」提示（`main.js:163-170`）。
2. 预加载字体（`main.js:28-40`）：`SignCore.FONT_LOAD_SPECS` 四个字体面（`core.js:216-221`），全部就绪才进入界面；30s 超时（`main.js:10`）后退化为系统字体并 toast 错误（`main.js:196-198`）。
3. 创建 Canvas 文本测量器 `App.measure = SignCore.createCanvasMeasurer()`（`main.js:183`、`core.js:342-382`）——**所有元素宽度、字号/基线都依赖真实字体墨区测量**，这是复刻的第一难点（见 5.8、9.5）。
4. 恢复自动保存或新建默认牌（`main.js:184`：`SignStorage.loadAutosave() || SignState.createSign()`）。
5. 装配 `SignInteract.init()`、`SignClipboard.init()`、静态按钮、左栏、首帧渲染（`main.js:187-192`）；字体全部 ready 后再刷一帧（`main.js:201`）。
6. 变更管线（`ui.js:66-90`）：
   `App.update(pureFn, key?, options?)` → `next = pureFn(App.state)` → `next = SignState.applyPaddingAuto(next)` → 与旧状态 `JSON.stringify` 比较（相等则丢弃，不消耗撤销）→ 必要时压入撤销栈 → `commitState`：更新 `App.state`/选择 → `renderAll()` → `SignStorage.scheduleAutosave`（500ms 防抖，`storage.js:47-57`）。
7. 渲染管线 `App.renderAll()`（`ui.js:96-108`）：`SignRender.renderSignInto(svg, state, measure, {selectedElementId, selectedElementIds})` 返回 layout → `App.fitSignDisplay()` 计算显示宽度 → `SignInteract.renderOverlay(layout,state)` 重画行带/行条 → `SignPanel.syncRightPanel()` 同步右栏 → `SignClipboard.sync()` 同步工具条。

## 1.4 三栏布局总览

```
┌──────────────260px──────────────┬────────────── 自适应（flex:1） ──────────────┬──────────────260px──────────────┐
│ 左栏 aside#left-panel           │ 中栏 main#center-panel                        │ 右栏 aside#right-panel           │
│ ┌ panel-header「元素选择区」⟨ ┐ │ ┌ #edit-actions：全选/复制/剪切/粘贴 + 状态 ┐ │ ┌ panel-header「标识牌设置」⟩ ┐  │
│ └─────────────────────────────┘ │ └───────────────────────────────────────────┘ │ └─────────────────────────────┘  │
│ 编辑器切换链接（platform.html） │ #canvas-scroll（灰底可滚动，padding 34/24/24/48）│ panel-body（可滚动，padding 10/12/20）│
│ #palette：                     │   #canvas-area（居中列）                      │ 模式：                            │
│  ├ .palette-tabs 元素 | 预设    │     #canvas-wrap（白牌面 + 阴影，相对定位）    │  ├ 标识牌设置（默认）             │
│  ├ 元素分类卡片（桌面 3 组）    │       svg + overlay + empty-hint              │  ├ 元素属性（单选 1 个）           │
│  └ 预设卡片（缩略图）           │     #row-strip（仅移动端）                    │  ├ 批量属性（多选 ≥2）             │
│                                │     ＋ 添加行按钮                             │  └ 预设预览（点击预设卡片）        │
│                                │ #zoom-badge（仅移动端）                       │                                   │
│                                │ #element-toolbar（选中元素时浮出）             │                                   │
└────────────────────────────────┴───────────────────────────────────────────────┴───────────────────────────────────┘
```

三栏的宽度定义：`--panel-width: 260px`（`style.css:99`）；`.side-panel` 固定宽 260、`flex-direction: column`（`style.css:138-147`）；左栏右描边、右栏左描边（`style.css:142`、`style.css:149-152`）；`#center-panel` `flex:1; min-width:0; display:flex; flex-direction:column`（`style.css:219-225`）。两侧面板可收起为 0 宽（`.side-panel.collapsed`，`style.css:154-158`），此时页面边缘出现展开把手 `.expander`（`style.css:197-215`）。

## 1.5 术语表

| 术语 | 含义 | 出处 |
|---|---|---|
| 标识牌 / Sign | 顶层对象，含宽度模式、宽、行高、背景、灰框、行数组 | `state.js:7-8`、`state.js:141-152` |
| 行 / Row | 水平的行；含背景色与元素数组；行高由整牌统一 `rowHeight` | `state.js:8`、`state.js:122-124` |
| 元素 / Element | 牌面上的一个可编辑图元，`{id,type,props}` | `state.js:9`、`state.js:126-139` |
| 内容盒 / content box | 元素盒去掉 padding 后的区域；内容尺寸 `s = rowHeight − padding.top×h − padding.bottom×h` | `render.js:46-52` |
| 行高 / h | `sign.rowHeight`，所有 padding 的比例基准；默认 192 | `core.js:22`、`state.js:145` |
| padding | **比例值**（相对行高），渲染时 × 行高换算为 px；不是绝对 px | `state.js:11`、`render.js:46-52` |
| 通道 / lane | 固定宽度模式下，元素按 `elementAlign` 分左/中/右三条独立排版通道 | `render.js:547-580` |
| 墨区 / ink | 字形实际着色范围（不是字距盒）：左缘 abl、右缘 abr、上伸 ascent、下伸 descent | `core.js:366-380`、`render.js:30-42` |
| 前进宽 / advance | 字形字距宽度（`measureText().width`）；数字排版用墨区而非前进宽 | `render.js:234-274` |
| 预设 / Preset | 一整行元素的深拷贝快照，可拖入空行或添加为新行 | `presets.js:3-8` |
| 色板 / Palette | 线路配色集合（内置 4 城市 + 用户自定义） | `core.js:191-198`、`palettes.js` |
| 元素剪贴板 | 独立 JSON 格式 `{kind:'jr-sign-elements',version:1,rows:[…]}` | `state.js:385-417` |

---

# 2. 视觉规范（Web 编辑器界面）

> 本章只描述「编辑器界面」的外观。牌面内部（SVG 内容）的颜色/几何见第 5 章。深色模式只影响编辑器表面，不影响牌面与预览（`style.css:1369-1370` 注释）。

## 2.1 颜色变量表（`:root`，`style.css:88-103`）

| 变量 | 亮色默认值 | 深色模式覆盖值（`style.css:1370-1383`） | 用途 |
|---|---|---|---|
| `--bg` | `#eef1f4` | `#14181f` | 页面/画布区背景、滚动条外描边 |
| `--panel-bg` | `#ffffff` | `#1e232b` | 三栏面板、面板头、卡片弹层、工具条底色 |
| `--border` | `#dce1e8` | `#3b4553` | 所有 1px 描边、分割线、输入框边框 |
| `--text` | `#1f2733` | `#edf1f7` | 主文字 |
| `--text-dim` | `#6b7684` | `#abb6c5` | 次级文字、标签、提示、未激活按钮 |
| `--accent` | `#2563eb` | `#397be8` | 主色：激活态、选中描边、主按钮、焦点 |
| `--accent-soft` | `rgba(37,99,235,0.12)` | `rgba(86,149,255,0.18)` | 主色淡底（tab/激活卡片/hover 背景） |
| `--danger` | `#dc2626` | `#cb3737` | 删除/危险操作、错误 toast |
| `--ok` | `#16a34a` | 无覆盖（沿用亮色） | 成功 toast |
| `--panel-width` | `260px` | 同 | 左右面板宽度 |
| `--radius` | `8px` | 同 | 卡片等圆角基准 |
| `--control-bg` | `#ffffff` | `#262c36` | 输入框/按钮/控件底色 |
| `--surface-soft` | `#f6f8fb` | `#242a33` | 次级底色：颜色块底、分段组底、色板行 |

其它硬编码颜色（重要）：页面遮罩 `#14203380` + blur 3px（色板管理对话框，`style.css:16`）；Toast 深色底 `#1f2733`（`style.css:873`）；模态遮罩 `rgba(15,23,42,0.45)`（`style.css:894`）；拖放无效行遮罩 `rgba(120,120,120,0.25)` + 虚线 `#9aa3af`（`style.css:319`）；滚动条滑块 `#c3ccd6`/hover `#a5b0bc`（`style.css:961-966`），深色 `#505d6e`/`#6d7c90`（`style.css:1394-1395`）；白色卡面 `#fafbfc`（卡片底）、`#edf0f3`（缩略图边框）、`#b6c0cc`（虚线按钮边框）。

## 2.2 字号 / 圆角 / 间距 / 阴影 token

页面基础：`font-family: system-ui, -apple-system, 'Segoe UI', 'Microsoft YaHei', sans-serif; font-size:14px`（`style.css:107-114`）。`* { box-sizing:border-box; margin:0; padding:0 }`（`style.css:105`）。

| 类别 | 值 | 出处/用途 |
|---|---|---|
| 字号 | 22px | 空状态提示（桌面）`style.css:392`；移动端 15px（`style.css:1099`） |
| | 20px | 色板管理对话框 h2（`style.css:22`） |
| | 15px | 面板图标按钮字号（`.expander` `style.css:205`、行手柄 `style.css:278`）、模态 h3（`style.css:908`） |
| | 14px | 页面基础、面板标题（`.panel-title`，`style.css:169-172`）、添加行按钮（`style.css:406`） |
| | 13.5px | 主按钮 `.btn`（`style.css:661`）、模态正文（`style.css:910`） |
| | 13px | Toast（`style.css:875`） |
| | 12.5px | 元素卡片名/预设名（`style.css:506`、`style.css:558`）、虚线添加按钮（`style.css:819`） |
| | 12px | 字段 label 与提示（`style.css:596`、`style.css:689`）、行内按钮（`style.css:305`）、色板生成按钮（`style.css:816`） |
| | 11px | 选择状态文本（`style.css:1194`）、编辑器切换链接（`style.css:9`）、色板管理提示（`style.css:48`） |
| 圆角 | 4px | 画布外框 / SVG（`style.css:244`、`style.css:254`） |
| | 5px | 卡片缩略图（`style.css:496`、`style.css:546`）、线路删除按钮（`style.css:808`） |
| | 6px | 输入框、图标按钮、行按钮、色段按钮（`style.css:184`、`style.css:304`、`style.css:608`、`style.css:801`） |
| | 7px | 主按钮、分段组、颜色当前块、模态输入（`style.css:658`、`style.css:633`、`style.css:709`、`style.css:918`） |
| | 8px | `--radius` 卡片、展开把手、图标选择按钮、添加行按钮、浮动工具条组（`style.css:203`、`style.css:834`、`style.css:403`、`style.css:1217`） |
| | 9px | 左栏标签页容器、色板管理卡片（`style.css:426`、`style.css:32`） |
| | 10px | 属性分组 `.panel-section`、行芯片（`style.css:1283`、`style.css:1159`） |
| | 12px | 模态框、浮动元素工具条（`style.css:903`、`style.css:1205`） |
| | 14px | 缩放徽标（`style.css:1027`） |
| 阴影 | `0 2px 14px rgba(15,25,40,0.13)` | 牌面 `#canvas-wrap`（`style.css:243`） |
| | `0 2px 10px rgba(37,99,235,0.14)` | 卡片 hover（`style.css:483`、`style.css:541`） |
| | `0 1px 4px rgba(0,0,0,0.08)` | 行按钮（`style.css:310`） |
| | `0 8px 28px rgba(0,0,0,0.14), 0 2px 5px rgba(0,0,0,0.06)` | 浮动元素工具条（`style.css:1207`） |
| | `0 4px 14px rgba(0,0,0,0.25)` | Toast（`style.css:876`） |
| | `0 10px 40px rgba(0,0,0,0.25)` | 模态框（`style.css:906`） |
| | `0 2px 10px rgba(0,0,0,0.25)` 等 | 预设删除圆钮、拖拽幽灵等（`style.css:576`、`style.css:357`） |
| 过渡 | 0.1–0.25s ease | 卡片 hover/面板收展/按钮（`style.css:146`、`style.css:479`、`style.css:662` 等） |

**关键间距（高频值速查）**：面板头 padding `10px 12px`（`style.css:164`）；面板体 `10px 12px 20px`（`style.css:177`）；画布滚动区 `34px 24px 24px 48px`（`style.css:230`，左 48 留给行手柄）；标签页容器 padding 4 / 下边距 12 / 卡片网格 gap 8（`style.css:423,464`）；卡片内 padding 6–8、gap 4–6；字段下边距 12、label 下边距 5（`style.css:591-600`）；属性分组 padding 10、下边距 12（`style.css:1282-1285`）；行按钮 gap 6（`style.css:295`）；浮动工具条 gap 8、组内 gap 2、内边距控件宽 86 / 高 34（`style.css:1204,1216,1231`）；行芯片 gap 4–8、padding 10px 12px 0（`style.css:1141-1158`）；移动端画布 padding 12（`style.css:1067`）。

## 2.3 三栏布局与面板骨架

| 部位 | 规则 | 出处 |
|---|---|---|
| `#app` | `display:flex; height:100vh; overflow:hidden` | `style.css:132-136` |
| `.side-panel` | 宽/最小宽 `var(--panel-width)=260px`；白底；`border-right:1px solid var(--border)`；纵向 flex；`overflow:hidden`；过渡 width/min-width 0.25s、opacity 0.2s | `style.css:138-147` |
| `#right-panel` | 去掉右描边、改左描边 | `style.css:149-152` |
| `.side-panel.collapsed` | `width:0; min-width:0; opacity:0` | `style.css:154-158` |
| `.panel-header` | 高由内容决定（padding 10px 12px）；flex 两端对齐；下描边 1px；`flex:none` | `style.css:160-167` |
| `.panel-title` | `font-weight:600; font-size:14px` | `style.css:169-172` |
| `.panel-body` | `flex:1; overflow-y:auto; padding:10px 12px 20px` | `style.css:174-178` |
| `.expander` | 绝对定位 `top:12px`；26×40；白底；1px 边框；外角 8px 圆角；15px；z-index 30；阴影 `2px 1px 6px rgba(0,0,0,0.06)`；hover 主色 | `style.css:197-215` |
| 中栏 `#center-panel` | `flex:1; position:relative; min-width:0; flex column` | `style.css:219-225` |
| 左栏顶部链接 `.editor-page-link` | 块级、padding 8px 12px、下描边、主色、11px、不换行；文字「站台吊板 / 线路图 ↗」 | `style.css:9`、`index.html:26` |

## 2.4 面板头与图标按钮

- 右栏面板头是「返回按钮 + 标题」的 `.panel-heading`（`index.html:65-68`；`style.css:1275`）：返回按钮 `#close-element-btn` 仅在元素属性/批量属性/预设预览模式显示（`panel.js:493`），图标为左箭头 SVG（`index.html:66`），点击回「标识牌设置」：若有预设预览先退出预览，否则取消选择（`main.js:75-78`）。
- `.icon-btn`：26×26，圆角 6，`inline-flex` 居中，默认 `--text-dim`、14px；hover 变为 `--accent-soft` 底 + `--accent` 字（`style.css:180-193`）。面板头右侧收/展按钮为「⟨」/「⟩」（`index.html:24`、`index.html:69`）；右栏在移动端旋转成「∨/∧」（`style.css:1086-1087`）。
- 左栏面板头的收起按钮标题「收起元素选择区」，右栏「收起属性面板」；展开把手标题「展开元素选择区」/「展开属性面板」（`index.html:24,31,62,69`）。

## 2.5 按钮体系与状态矩阵

界面上共有 5 类按钮外观：

**(a) 主按钮 `.btn`（右栏通用）**（`style.css:654-677`）
| 状态 | 外观 |
|---|---|
| default | 块级、宽 100%、padding 8px 10px、圆角 7、1px `--border`、白底、13.5px、下间距 8px |
| hover | 边框/文字变 `--accent` |
| active（按下） | 全局 `button:active { transform: translateY(1px) }`（`style.css:123`） |
| primary `.btn-primary` | 底/边 `--accent`、白字；hover 底色 `#1d4fd7`（`style.css:666-671`） |
| danger `.btn-danger` | 底/边 `--danger`、白字；hover `#c02020`（`style.css:672-677`） |
| disabled | **源码未给 `.btn:disabled` 专门样式**；导出按钮忙碌时由 JS 置 `disabled` 并改文案（`panel.js:770-781`） |
| ghost/次按钮 | 无独立类；行内小按钮与工具栏按钮见 (b)(c) |

**(b) 工具条小按钮**：`#edit-actions button`（padding 5px 9px、1px 边框、圆角 6、12px、`--control-bg` 底；hover 主色；disabled `opacity:.4`，`style.css:1191-1193`）；`#element-toolbar button`（32×34、圆角 6、默认 `--text-dim`；hover 主色字+`--accent-soft` 底；disabled `opacity:.3`；危险按钮 `.toolbar-danger` 默认即 `--danger`，hover 红淡底 `rgba(220,38,38,0.12)`，`style.css:1219-1230`）。

**(c) 图标按钮 `.icon-btn`**：见 2.4；另有 `.format-btn`（36×36，圆角 6；`.active` 变主色底白字，`style.css:1333-1338`）、`.icon-pick`（图标选择格，见 2.8）、`.cp-null-btn`（「透明」按钮，active 变主色底白字，`style.css:744-753`）。

**(d) 分段按钮组 `.seg-group` / `.alignment-buttons`**（`style.css:630-650`、`style.css:1310-1319`）：相连按钮共享 1px 分隔；默认白底、`--text-dim`；hover `#f3f6fa`/`--surface-soft`；`.active` 主色底白字。`.alignment-buttons` 为独立小方钮（flex:1，最小高 36，20px 图标；active 主色边框+`--accent-soft` 底）。

**(e) 虚线/胶囊按钮**：`#add-row-btn`（1.5px 虚线 `#b6c0cc`、圆角 8、`rgba(255,255,255,.7)` 底、`--text-dim`、14px；hover 主色边框/字+纯白底，`style.css:399-413`）；`.le-add`（「＋ 添加线路」，1.5px 虚线、圆角 6、12.5px；disabled 透明度 .45 且 hover 不变，`style.css:813-823`）；`.palette-chip`/`.palette-tab` 见 2.6；行内按钮 `.row-actions button`（高 24、padding 0 8、12px、圆角 6、白底 1px 边框、阴影；hover 主色；删除 hover 红；disabled `visibility:hidden`，`style.css:302-314`）。

**(f) 触屏放大规则**（移动端 ≤768px）：`.icon-btn` 44×44、`.seg-group button` padding 10、`input[type=checkbox]` 20×20、`.btn` padding 11px 10px（`style.css:1092-1097`）；`@media (hover:none)` 下行手柄 44×44、行按钮高 44（`style.css:1117-1131`）。

## 2.6 卡片、标签页、chip、空态

**左栏标签页 `.palette-tabs`**（`style.css:417-440`）：sticky top:0、z-index 2；横向 flex、gap 4、padding 4、1px 边框、圆角 9、白底、下间距 12；两个 tab（「元素」「预设」）各 flex:1、最小高 34、圆角 6、600 字重、`--text-dim`；激活 `--accent-soft` 底+`--accent` 字（`style.css:436`）；键盘支持 ←/→/Home/End（`panel.js:163-170`）。

**分类区**：`.palette-category` 下间距 14（`style.css:442`）；`.palette-category-title` 12px/600/`--text-dim`/字距 1px，右侧用 `::after` 画一条 1px 延伸横线（`style.css:443-458`）。桌面卡片网格 `.palette-grid` 两列、gap 8（`style.css:460-464`）；方向组 `.direction-grid` 四列、gap 6，卡片最小高 48、padding 4，图标区高 38 且无边框，SVG 最高 32，卡片名用「视觉隐藏」样式（`style.css:511-525`）。

**元素卡片 `.palette-card`**（`style.css:466-509`）：1px 边框、`--radius` 圆角、`#fafbfc` 底、padding 6、纵向 flex 居中、gap 4、`cursor:grab`；hover 主色边框+阴影+`translateY(-1px)`；激活/拖拽中 `.dragging` 透明度 .4。内部 `.card-icon`：宽 100%、高 40、白底、圆角 5、1px `#edf0f3` 边框、裁切；SVG 最大宽 100%、最大高 42。`.card-name` 12.5px。空白占位卡片图标是自绘 36px 虚线方框（`panel.js:100-113`），其余卡片用「示例元素」渲染成 mini SVG（`panel.js:294-325`）。

**预设卡片 `.preset-card`**（`style.css:529-580`）：1px 边框、圆角 8、`#fafbfc`、padding 8；hover 主色边框+阴影；`.preset-thumb` 白底、1px `#edf0f3`、圆角 5，缩略 SVG 宽 100%、最高 56；`.preset-name` 上间距 6、12.5px、居中、单行省略号；右上角删除圆钮 20×20、红底白字、默认隐藏，hover/focus-within 出现（`style.css:564-580`），删除前确认框（`panel.js:371-383`）。

**空态**：`.palette-empty` 12px `--text-dim` 居中 padding 10px 4px（`style.css:582-587`），文案「还没有预设。使用行上的「存为预设」保存一行，方便下次使用。」（`panel.js:367`）。

**移动端 chip 行**（`style.css:977-999`）：横滚、gap 6、padding 8px 10px 6px、隐藏滚动条；chip 高约 31（padding 7px 14px）、圆角 16、1px 边框、白底、13px；激活主色底白字。

## 2.7 表单控件（右栏）

- 字段容器 `.field`：下间距 12（`style.css:591-600`）；label 12px/600/`--text-dim`/下间距 5px。
- 文本框/数字框/hex 框：宽 100%、padding 6px 8px、1px 边框、圆角 6、白底（`--control-bg`）、focus 边框主色（`style.css:602-614`）；hex 用 Consolas、大写（`style.css:614`）。
- 行内字段 `.field-inline`：两端对齐，label 无边距（`style.css:616-622`）。
- 复选框：`accent-color:var(--accent)`、15×15（`style.css:624`）；滑条 `input[type=range]`：宽 100%、主色（`style.css:626`）。
- 下拉 `select`：宽 100%、padding 7px 8px、1px 边框、圆角 6、`--control-bg`（`style.css:1289-1292`）；`select option` 显式不透明底色（`style.css:1304`）。
- 对齐控件 `.alignment-control`：自定义外观的 select——左侧 18px 对齐图标（主色）、右侧 14px 下拉箭头、中间文本；min-height 36、圆角 8、左内边距 34、右 28（`style.css:1293-1308`）；混合值显示「不同」灰色图标隐藏（`ui.js:317-331`）。
- 属性分组 `.panel-section`：1px 边框、圆角 10、padding 10、下间距 12；标题 12px、下间距 12（`style.css:1282-1287`）。
- 元素类型标签 `.element-type-label`：`--text-dim`、12px（`style.css:1281`）。
- `.divider`：1px `--border`、上下 14（`style.css:682-686`）；`.hint-text` 12px `--text-dim` 行高 1.5（`style.css:688-692`）；`.section-title` 13px/700（`style.css:694-698`）。
- 批量内边距 `.batch-padding-grid`：两列 gap 6（`style.css:1196`）；`.panel-padding-value`：图标触发钮 28×30 + 数字框（`style.css:1249-1257`）。

## 2.8 颜色选择器 / 图标选择器 / 线路编辑器

**颜色选择器 `.color-picker`**（结构 `color-picker.js:10-59`，样式 `style.css:702-787`、`1342-1350`）：
- 行 `.cp-row`：当前色块 `.cp-current`（30×30、圆角 7、1px 边框；`nullable` 时为灰白斜纹 + 红色斜杠表示「透明」）→ hex 输入（`flex:1`，约 76px 宽）→ 原生取色器按钮 `.cp-native`（30×30）→ 可选「透明」按钮 `.cp-null-btn`。
- 可展开区 `<details class="cp-options">`：`<summary>`「展开色板」（12px `--text-dim`）；内含城市下拉 `.cp-city-select`（预设城市线路配色 / 我的色板 optgroup，`color-picker.js:60-65`）、色板格子 `.cp-swatches`（10 列、gap 4；每格圆形、`aspect-ratio:1`、1px 黑 12% 描边；hover 放大 1.22 + 阴影；`active` 2px 主色 outline）、「管理自定义色板」按钮 `.cp-manage`（虚线边框、12px，打开色板管理对话框）。
- 包裹在字段里时加 `.color-field`（`--surface-soft` 底、圆角 7、padding 9，`style.css:1342-1345`）。
- 混合值：根加 `.mixed`，当前色块显示「不同」（`style.css:1320`、`color-picker.js:18-22`）。

**图标选择器**（`panel.js:1227-1272`，样式 `style.css:825-857`）：3 列网格 gap 8；每个 `.icon-pick` 是 1.5px 边框、圆角 8、白底按钮；active 主色边框 + `--accent-soft` 底；内部 `.icon-thumb` 用 `padding-top:100%` 撑正方形，SVG 绝对定位 12%/76%。

**数字线路编辑器**（`panel.js:1080-1158`，样式 `style.css:1351-1357` 为最终生效规则）：每条线路一个 `.line-entry`（两列 grid：`1fr 28px`；1px 边框、圆角 8、padding 8、下间距 8），内容为：跨两列的「第 N 条线路 · 线路号」标签、线路号输入（文本，输入时过滤非数字并限 2 位，`panel.js:1096-1110`）、「✕」移除按钮（22×22，hover 红底红字）、跨两列的一个颜色选择器（`.color-field`），底部「＋ 添加线路」虚线按钮（`style.css:813-823`，右对齐模式下禁用，`panel.js:1141-1150`）。

**左右内边距拖拽图标**：`ui.js:339-347` 生成 24×24 图标（左侧/右侧虚线框 + 边线），作为 `.padding-trigger` 按钮；`.padding-scrub` 光标 `ew-resize`，拖动时 `body` 加 `.padding-scrubbing` 强制 `ew-resize` 且禁选（`style.css:1245-1248`）。

## 2.9 中栏画布区域

| 部位 | 规格 | 出处 |
|---|---|---|
| `#canvas-scroll` | `flex:1; overflow:auto; padding:34px 24px 24px 48px`（左侧 48px 留给行手柄） | `style.css:227-231` |
| `#canvas-area` | 纵向 flex、`align-items:center`、`min-height:min-content` | `style.css:233-238` |
| `#canvas-wrap` | `position:relative; width:100%`；阴影 `0 2px 14px rgba(15,25,40,.13)`；圆角 4；`overflow:visible`（行手柄悬于外侧）；禁选/禁 iOS 长按菜单 | `style.css:240-248` |
| `svg#sign-svg` | `display:block; width:100%; height:auto; border-radius:4px`（实际宽度由 `fitSignDisplay` 写为 px） | `style.css:250-255` |
| `#sign-overlay` | 绝对定位 `inset:0`、`pointer-events:none`、圆角 4；内部行带/手柄/按钮单独开 `pointer-events:auto` | `style.css:259-264` |
| 画布区背景 | 页面 `--bg` 灰底；牌面本身背景 = `sign.backgroundColor`（默认白） | `style.css:112`、`render.js:861-864` |

显示尺寸算法 `App.fitSignDisplay()`（`ui.js:118-135`）：可用宽 `avail = min(#canvas-area.clientWidth, 1600)`；SVG 显示宽 = `min(avail, layout.width) × App.canvasZoom`，**永不超过牌面原始宽（不放大）**；同步 `#canvas-wrap`、`#sign-overlay` 宽度与 overlay 高度（用 getBoundingClientRect）。窗口尺寸/面板收展通过 ResizeObserver 重算（`main.js:81-85`）。

## 2.10 覆盖层：行带 / 行手柄 / 行按钮 / 指示器 / 拖拽幽灵

- `.row-band`：绝对定位覆盖一行（top/height 为百分比），`pointer-events:none`（`style.css:266`）。
- `.row-handle`：行左侧 `left:-38px`、垂直居中、28×40、白底、1px 边框、圆角 7、`--text-dim`、15px、「≡」；`cursor:grab`、`touch-action:none`；默认 `opacity:0`，`#canvas-wrap:hover` 才显形（`style.css:267-289`）；标题「拖动调整行顺序」；仅在**行数 >1** 时渲染（`interact.js:101`）。触屏（`hover:none`）改为常显、内移到 `left:8px`、44×44、半透明白底（`style.css:1117-1124`）。
- `.row-actions`：行右上角 `top:6/right:6`、横向 gap 6；默认 opacity 0，hover 牌面出现（`style.css:291-312`）；两个按钮：「⭐ 存为预设」与「✕ 删除行」（`interact.js:112-131`）。仅 1 行时删除按钮 `disabled` 且 CSS `visibility:hidden`（`style.css:314`）。
- 拖放高亮：`.row-band.drop-ok` = `--accent-soft` 底 + 2px 虚线主色内描边；`.drop-invalid` = 灰色半透明 + 灰虚线，并用 `::after` 居中显示「🚫 预设只能放入空行」白字带阴影（`style.css:318-331`）。
- 插入指示器：`#element-drop-indicator` 宽 0、3px 虚线主色左边框，竖直贯穿目标行（`style.css:333-339`）；`#row-drop-indicator` 高 0、3px 虚线主色上边框，横贯整宽（`style.css:341-348`）。两者默认 `display:none`。
- 拖拽幽灵 `.drag-ghost`：`position:fixed; pointer-events:none; opacity:.65; z-index:999; drop-shadow(0 4px 10px rgba(0,0,0,.25))`，内部放一张 SVG 克隆（去掉 hitbox 与选中类）（`style.css:352-359`、`interact.js:595-621`）。
- 元素交互态（`style.css:361-378`）：`.element{cursor:grab}`；hitbox 默认透明、2px 描边且 `vector-effect:non-scaling-stroke`；hover hitbox 描边 `rgba(37,99,235,.45)`；选中 `.element.selected .hitbox`：主色 2.5px 虚线 `7 4` + `rgba(37,99,235,.05)` 填充；拖拽源 `.element.dragging` 透明度 .35。

## 2.11 空状态 / 缩放徽标 / 浮动元素工具条

- **空状态** `#empty-hint`（`style.css:380-395`）：绝对铺满 `#canvas-wrap`、flex 居中、颜色 `rgba(120,130,145,.75)`、22px、字距 2px、`pointer-events:none`；`hidden` 时隐藏。文案由断点决定：桌面「从左侧拖放元素到此处开始编辑」/移动端「从顶部卡片点击添加元素开始编辑」（`main.js:89-93`）。隐藏条件：所有行都无元素时才显示（`interact.js:136-137`）。
- **缩放徽标** `#zoom-badge`（`style.css:1018-1034`、`1110`）：默认 `display:none`，仅移动端 `inline-flex`；绝对定位 `right:12px; bottom:12px`；胶囊形（圆角 14）、padding 6px 12px、1px 边框、`rgba(255,255,255,.92)` 底、12px、等宽数字、阴影；文本为 `Math.round(App.canvasZoom*100)+'%'`；点击复位 1（`interact.js:74`、`interact.js:1027-1030`）。
- **浮动元素工具条** `#element-toolbar`（`style.css:1202-1274`）：绝对定位（相对 `#center-panel`）、z-index 60、`width:max-content`、可换行、gap 8、padding 6、1px 边框、圆角 12、白底、阴影；`hidden` 时 `display:none`。内部：
  - `.toolbar-actions`：`[移动元素组 ←/→] [左内边距控件] [右内边距控件] [管理元素组：删除]`（DOM 顺序见 `interact.js:222-261`）。
  - 内边距控件 `.toolbar-padding`：`.padding-value`（86×34，内含 `.padding-trigger` 图标+当前值文本 `.padding-current`（12px 等宽数字）与一个绝对定位覆盖的 number 输入；`.editing` 时输入可见、触发钮隐藏）+ 右侧 `.padding-stepper` 两个 20×17 上下箭头按钮（±0.05）（`style.css:1231-1260`）。
  - `.toolbar-alignments`：三个自定义对齐下拉（元素对齐 / 文字对齐 / 内容对齐，按适用性隐藏）；全部隐藏时整行隐藏（`interact.js:292-298`）。
  - 按钮尺寸 32×34、图标 22×22；组容器 `.toolbar-group` 为 `--surface-soft` 底、圆角 8、padding 2（`style.css:1215-1225`）。
  - 位置：水平居中于选中元素外接框、限制在画布可视区内（±8px），优先在下方 8px，放不下则放到上方（`interact.js:309-332`）；用 CSS 变量 `--element-toolbar-gap` 把「添加行」按钮/行条下推（`style.css:1261`、`1272-1273`）。

## 2.12 Toast / 模态 / 加载 / 滚动条

- **Toast**（`ui.js:411-418`、`style.css:861-886`）：固定右上（`top:14;right:14`）、纵向 gap 8、z-index 1000；深色 `#1f2733` 白字、padding 10px 16px、圆角 8、13px、阴影、最大宽 320、入场动画 `toast-in`（右滑淡入 0.2s）；类型 `.success` 绿底、`.error` 红底；普通 2400ms、错误 4200ms 后加 `.leaving` 淡出 250ms 再移除。
- **模态**（`ui.js:426-484`、`style.css:888-930`）：`#modal-root:empty{display:none}`；`.modal-mask` 全屏 `rgba(15,23,42,.45)`、z-index 900；`.modal` 白底、圆角 12、padding 20、宽 `min(420px,90vw)`、阴影；h3 15px；正文 13.5px 行高 1.6；确认框按钮「取消 / 确定（危险）」右对齐、`min-width:84`；输入框 `.modal-input`；确认框默认聚焦确定按钮（`ui.js:446`），输入框 Enter 确认、Esc 取消（`ui.js:476-479`）。点击遮罩空白取消。
- **加载遮罩**（`index.html:13-16`、`style.css:932-955`）：全屏 `--bg` 底、z-index 2000、居中列、`--text-dim` 15px；34×34 旋转环（3px 边框、顶边主色、0.8s 线性旋转）+ 文案 `#loading-text`（「正在加载字体…」/「正在检查编辑器实例…」/「正在加载 MiSans 与编号字体…」，`main.js:13-40`）。
- **滚动条**（`style.css:957-966`）：宽/高 10px；轨道透明；滑块 `#c3ccd6`、圆角 6、2px `--bg` 描边；hover `#a5b0bc`；深色模式改用 `#505d6e`/`#6d7c90`。

## 2.13 移动端断点（≤768px）

断点常量为 `(max-width: 768px)`（`ui.js:12-14`，`main.js:119`，`style.css:1036`），断点值以 JS 与 CSS 两处保持一致。

| 变化 | 规格 | 出处 |
|---|---|---|
| 整体 | `#app` 改纵向、满高 `100dvh`（回退 100vh） | `style.css:1036-1041` |
| 左栏 | 变成顶部横条：宽 100%、无右描边、底部 1px 描边；面板头隐藏；panel-body 无内边距、不滚动 | `style.css:1043-1056` |
| 左栏内容 | 标签页静态、边距 6px 10px；元素页改为「分类 chip 横滚 + 当前分类卡片横条」；预设页为横向卡片条（最小高 102） | `style.css:1057-1059`、`977-1016`、`panel.js:181-198` |
| 中栏 | `min-height:0`；`#canvas-scroll` padding 12、`touch-action:none`（手势由 JS 接管） | `style.css:1065-1069` |
| 右栏 | 变成底部属性带：宽 100%、高 40vh、上描边；可折叠为 48px（`.band-collapsed`，此时面板体隐藏） | `style.css:1071-1087` |
| 展开把手 | 两侧 `.expander` 隐藏 | `style.css:1089-1090` |
| 触控目标 | 图标按钮 44、分段按钮 padding 10、复选框 20、主按钮 padding 11 | `style.css:1092-1097` |
| 空状态 | 15px、字距 1、水平居中留白 | `style.css:1099` |
| Toast | 移到底部属性带上方（`bottom:calc(40vh+14px)` 等），折叠时 JS 改为 60px | `style.css:1101-1108`、`main.js:99-107` |
| 缩放徽标 | 显示 | `style.css:1110` |
| 行控件 | 覆盖层行手柄/行按钮一律隐藏，改用 `#row-strip` 芯片：每行「行 N」+ ▲▼（首/末行禁用）+「⭐ 存预设」+「✕ 删行」（仅 1 行时禁用）；chip 白底圆角 10、按钮最小宽 44 高 40 | `style.css:1133-1186`、`interact.js:334-365` |
| 元素工具条 | 按钮高 44、内边距控件高 44、步进按钮高 22、对齐下拉最小高 40；两行布局 | `style.css:1262-1274` |
| 批量对齐按钮 / 格式按钮 / 图标分段 | 最小高 44 | `style.css:1363-1367` |

## 2.14 深色模式范围

`@media (prefers-color-scheme: dark)`（`style.css:1369-1396`）只改编辑器表面变量及若干元素底色（卡片、tab、chip、行芯片、模态、添加行按钮、缩放徽标、行控件、图标选择格、滚动条）；**SVG 牌面与预览保持项目原色**（注释明确：「仅编辑器表面跟随系统主题；SVG 牌面及预览保持项目原色」）。亮色/深色两套变量见 2.1 表。

---

# 3. 布局与信息架构

## 3.1 左栏：元素选择区（`index.html:21-30`；`panel.js:36-410`）

面板头「元素选择区」+ 收起按钮（`index.html:22-25`），其下是「站台吊板 / 线路图 ↗」链接（`index.html:26`，指向 platform 编辑器，本轮不要求）。

`#palette` 由 `SignPanel.buildPalette()` 填充（`panel.js:205-214`），结构 = 标签页（`元素` / `预设`）+ 内容区：

### 3.1.1 「元素」标签页 —— 桌面（>768px）
按组渲染 3 个分类（`DESKTOP_CATEGORIES`，`panel.js:64-69`）：

| 分类 | 条目（全部是「元素类型」或「预设图标 id」） |
|---|---|
| 方向指示 | **12 个方向图标**：向上/向下/向左/向右/左上/右上/右下/左下/前方向左/前方向右/左行向后/右行向后。注意它们创建的是 `icon` 元素（`icon` 属性 = `arrow_*`），**不是 `arrow` 元素**（`panel.js:40-53`） |
| 标识与文字 | 数字线路、文本线路、大文本、出入口、出口、双语文本、小号双语文本（`panel.js:67`） |
| 图标与间隔 | 图标（创建 `icon` 元素，默认 `icon:'elevator'`）、空白占位（`space`）（`panel.js:68`） |

> ⚠️ **`arrow` 元素类型在左栏没有任何入口**：本 fork 的方向指示全部改用 12 个箭头图标元素；`arrow` 类型仍存在于数据模型/渲染器中，只能通过导入项目 JSON、剪贴板或预设进入牌面（依据：`panel.js:55-69` 与 `state.js:20-23`）。

卡片交互（`panel.js:290-325`）：
- **点击**：立即创建元素并加到「当前行」——若当前选中某元素则插入其所在行（追加到行末），否则追加到最后一行（`panel.js:315-323`、`panel.js:327-334`）；随后 toast「已添加「X」」（success）；移动端还会自动选中新元素。
- **拖拽（仅桌面）**：HTML5 DnD，`dataTransfer` 同时写自定义 MIME `application/x-sign-new` 与 `text/plain`，payload 为 `{kind:'new',type,props?}`（`panel.js:115-141`）；`effectAllowed='copy'`；拖拽中卡片加 `.dragging`（透明度 .4）。移动端卡片 `draggable=false` 且 `dragstart` 被取消，只保留点击（`panel.js:126-140`）。
- 卡片以 36px 行高渲染一个「示例元素」缩略 SVG（`panel.js:86-98`、`panel.js:294-300`）：如数字线路示例 3 号线黄 `#FCD600`、大文本「4」、入口「C」、出口「1」等；空白占位用 36px 虚线方框（`panel.js:100-113`）。卡片 title 为「X：点击添加到当前行，或拖入编辑区」。

### 3.1.2 「元素」标签页 —— 移动端（≤768px）
6 个分类 chip（方向指示 / 线路标识 / 位置标识 / 文本 / 服务图标 / 辅助，`panel.js:55-62`），逐字对应 `CATEGORIES`；chip 横向滚动；下方是该分类的横向卡片条（卡片宽 88px，`style.css:1014`）。切换分类只替换卡片条、保留 chip 滚动位置（`panel.js:264-279`）。

### 3.1.3 「预设」标签页
- 桌面：纵向排列预设卡片；移动端：横向卡片条（`panel.js:181-198`）。
- 预设卡片 = 缩略图（按 48px 行高把整行元素渲染成 SVG，见 `panel.js:338-360`）+ 名称 + 悬停删除圆钮。
- 点击卡片：进入右栏「预设预览」模式（`App.previewPreset(id)`，`panel.js:395-397`）。
- 拖拽：MIME `application/x-sign-preset`，payload `{kind:'preset',id}`；**只能放入空行**，否则目标行显示红/灰无效高亮并禁止放置（`interact.js:808-817`）。
- 预设来源于「⭐ 存为预设」（桌面行按钮 / 移动端行芯片），保存时弹输入框要名称（`interact.js:1050-1062`）；空行不能保存（错误 toast）。
- 无预设时显示空态提示（`panel.js:366-369`）。

## 3.2 中栏：编辑区（`index.html:34-59`）

### 3.2.1 选择与剪贴板工具条 `#edit-actions`（`index.html:35-41`；`style.css:1188-1201`；`clipboard.js:96-118`）
4 个按钮 + 1 段状态文本：

| 控件 | 标题/tooltip | 禁用条件 | 行为 |
|---|---|---|---|
| 全选 | 全选元素（Ctrl+A） | 整牌没有任何元素 | `App.selectAll()`：按视觉顺序选中所有元素 |
| 复制 | 复制选中元素（Ctrl+C） | 无选中元素 | 写系统剪贴板 + 页面内副本，toast「已复制 N 个元素」 |
| 剪切 | 剪切选中元素（Ctrl+X） | 无选中元素 | 复制后删除，toast「已剪切 N 个元素」 |
| 粘贴 | 粘贴元素（Ctrl+V） | **始终可用**（空剪贴板时点按报错 toast） | 粘贴到「当前选中元素之后 / 剪切落点 / 最后一行末」 |
| `#selection-status` | — | — | 无选择时「Ctrl 单击加选 · Shift 单击范围选择」；有选择时「已选择 N 个元素」 |

工具条样式：`display:flex; flex:none; flex-wrap:wrap; align-items:center; gap:6`、padding 8px 12px、下边界 1px、白底（`style.css:1189-1190`）。

### 3.2.2 画布
`#canvas-scroll`（可滚动灰底）→ `#canvas-area`（居中列）→ `#canvas-wrap`（白牌面 + 阴影，内含 SVG / 覆盖层 / 空态提示）→ `#row-strip`（移动端）→ `#add-row-btn`（虚线「＋ 添加行」，标题「在底部添加新行」，点击在末尾追加空行并走撤销；`interact.js:59-61`）。

### 3.2.3 行控件（桌面）
- 每一行有一条 `.row-band` 覆盖层；行数 >1 时左侧出现拖动排序手柄「≡」（`interact.js:101-110`）。
- 每行右上角「⭐ 存为预设」「✕ 删除行」（`interact.js:112-131`）；删除非空行弹确认框「确定要删除此行吗？行内 N 个元素将一并删除。」，空行直接删除并 toast（`interact.js:1034-1048`）。
- 拖动手柄排序：指针进入某行上半 → 插入到该行之前；下半 → 之后；跨出牌面上方 = 第 0 位、下方 = 末尾；显示横向虚线指示器（`interact.js:718-756`）。

### 3.2.4 行控件（移动端）
`#row-strip` 每行一枚芯片（详见 2.13）。

### 3.2.5 缩放徽标
仅移动端显示，点击复位 100%（`interact.js:74`）。

### 3.2.6 浮动元素工具条 `#element-toolbar`
选中 1 个元素时出现（隐藏条件见 6.4），内容与布局见 2.11。按钮功能：
- 「← 向左移动元素」「→ 向右移动元素」：按通道做 ±1 位相邻交换（`interact.js:254-258`，实际调用 `State.moveElementsBy`）；不可移动时按钮禁用。
- 「左内边距 / 右内边距」：图标+当前值显示、点击变输入框、可左右拖动调节（每 8px = 0.05）、上下步进 ±0.05；到达 0/8 边界时对应按钮禁用。
- 「删除元素」：删除当前选中的全部元素并 toast「选中元素已删除」。
- 3 个对齐下拉：元素对齐（贴左/居中/贴右）、文字对齐（仅双语类）、内容对齐（仅数字线路/文本线路/出口）。

## 3.3 右栏：属性 / 设置 / 导出（`index.html:63-74`；`panel.js:447-515`）

右栏有 **4 种互斥模式**，由 `panelMode()` 决定（`panel.js:453-458`），面板头标题同步（`panel.js:488-494`）：

| 模式 | 触发条件 | 标题 | 返回按钮 |
|---|---|---|---|
| `batch` 批量属性 | 选中 ≥2 个元素 | 「批量属性」 | 显示 |
| `element` 元素属性 | 恰好选中 1 个元素 | 「元素属性」 | 显示 |
| `preset` 预设预览 | 无选择且 `App.presetPreviewId` 非空 | 「预设预览」 | 显示 |
| `settings` 标识牌设置 | 其它（默认） | 「标识牌设置」 | 隐藏 |

面板采用「结构签名」机制：签名变化才重建 DOM，否则只同步值（跳过正在聚焦的控件），保证输入不丢焦点（`panel.js:460-515`）。

### 3.3.1 标识牌设置面板（`panel.js:642-808`）
按顺序：
1. **行高（px）**：数字输入（min 32 / max 2048 / step 4）；改动走 `updateSignSettings`（若开启宽高比锁，联动改宽度）。
2. **行数（由添加/删除行控制）**：只读数字输入（disabled），显示 `rows.length`。
3. **宽度模式**：分段按钮「固定 / 动态」（`fixed` / `dynamic`）。
4. **标识牌宽度（1–15 格）**：下拉「自定义像素宽度 + 1…15」；提示「每格 256 px」；仅固定模式显示（动态模式隐藏）。选 1–15 时执行 `setSignWidthUnits`（宽度 = 格数 × 256，关闭比例锁）。
5. **固定宽度（px）**：数字输入（32–32768，step 16），仅固定模式显示。
6. **动态宽度提示**：动态模式显示「当前动态宽度：N px（由最宽行决定）」。
7. **锁定宽高比**：复选框（`aspectLocked`）。
8. 「牌面颜色与灰框」标题：
   - 标识牌背景色（颜色选择器，不可透明，透明值回退 `#FFFFFF`）。
   - 单独设置行背景色（行下拉「第 N 行」+ 颜色选择器 + 状态提示 + 「恢复标识牌背景色」按钮）。状态文案：「此行已单独设色；元素背景可覆盖它。」/「此行沿用标识牌背景色。」；仅 1 行且该行未单独设色时整个区块隐藏。
   - 灰框宽度（px）：0–512，提示「外缘与行间分割线使用相同宽度；0 为不显示」。
   - 灰框颜色：颜色选择器，提示「作用于每行灰框，宽度为 0 时隐藏」。
9. 分割线 + 「编辑器」标题：
   - 「输入线路号自动匹配线路色」复选框（编辑器偏好，localStorage）。
   - 说明文字：「开启后输入线路号会自动填充线路色；展开色板后可切换城市。」
   - 危险按钮「🗑 清空标识牌」→ 确认框「确定要清空所有行和元素吗？」→ `clearSign`（保留设置，重置为 1 行空白）+ 取消选择 + toast。
10. 分割线 + 「导出」标题：
    - 主按钮「⬇ 导出 SVG」；主按钮「⬇ 导出 PNG」；点击时按钮变「⏳ 正在准备字体…」并禁用，完成/失败后恢复（`panel.js:770-787`）。
11. 分割线 + 「项目」标题：
    - 「📤 导出项目（JSON）」下载 `{version,sign}`。
    - 「📥 导入项目（JSON）」选文件→反序列化校验→替换当前状态并取消选择，成功 toast「项目导入成功」，失败显示中文错误（`panel.js:791-805`、`storage.js:171-202`）。

### 3.3.2 元素属性面板（`panel.js:921-989`，字段见各 `build*Fields`）

面板顶部显示元素类型名（`TYPE_NAMES`，`panel.js:23-34`）：箭头/双语文本/小号双语文本/大文本/数字线路/文本线路/出入口/出口/空白占位/图标。默认固定分组「内容」「排版」「元素颜色」（没有内容的组自动隐藏，`panel.js:976-978`），末尾「🗑 删除元素」危险按钮（删除后取消选择 + toast「元素已删除」）。

| 元素类型 | 内容字段 | 排版字段 | 颜色字段 |
|---|---|---|---|
| `arrow` 箭头 | —（无独立内容字段） | 方向（8 向分段按钮 ← ↖ ↑ ↗ → ↘ ↓ ↙）；粗细比例（滑条 0.1–0.5 step 0.05，右侧显示百分比） | 箭头颜色 |
| `bilingual-text` 双语文本 | 中文文本、英文文本 | 文字样式与对齐（加粗切换钮 + 左/中/右对齐三钮） | 文字颜色；元素背景色（可透明） |
| `small-bilingual-text` 小号双语文本 | 同上 | 同上 | 同上 |
| `big-number` 大文本 | 内容（占位提示「文本，如 16 / 1/2/3/4 / 16号线」） | — | 文字颜色；元素背景色 |
| `number-line` 数字线路 | 线路列表（每条：线路号输入 1–2 位数字 + 移除 + 颜色；「＋ 添加线路」） | 内容对齐（左/右） | 文字颜色；元素背景色 |
| `text-line` 文本线路 | 线路名下沉复选框；中文线路名；英文名称 | 内容对齐（左/右） | 线路色条颜色；文字颜色；元素背景色 |
| `entrance` 出入口 | 编号（占位「编号，如 1 / C / 1,5-22」） | — | 文字颜色；元素背景色 |
| `exit` 出口 | 编号 | 内容对齐（左/右） | 文字颜色；**无元素背景色**（固定出口黄，提示「出口背景固定为黄色」） |
| `space` 空白占位 | 宽度（× 行高的倍数，0.1–8 step 0.1） | —（无内边距编辑器） | 元素背景色 |
| `icon` 图标 | 图标网格（按当前图标分类显示：服务设施 20 个 / 方向箭头 12 个） | 旋转（仅服务设施显示：0° / ↺90° / ↻90° / 180°） | 图标颜色；元素背景色 |

分组归属由「字段控件类型」自动分发（`panel.js:952-956`）：含颜色选择器的字段 → 「元素颜色」；含 `.seg-group / .format-btn / .alignment-buttons / range` 的字段 → 「排版」；其余 → 「内容」。

所有非 `space` 元素在「排版」末尾还有 `.padding-editor`：复选框「自动调整左右内边距」+ 四个数字框（左/右 0–8、上/下 0–1，step 0.05）；左/右两个输入框旁带拖拽图标（`panel.js:1285-1329`）。手动改左/右会置 `paddingAuto=false`（钉住）。

**元素对齐**（`elementAlign`）对所有元素都适用，但只在本面板出现于**固定宽度模式**（`panel.js:958-968`），三钮：贴左 / 居中 / 贴右；动态模式整个控件不出现。

### 3.3.3 批量属性面板（`panel.js:866-919`）
- 顶部「已选择 N 个元素」。
- 分组「批量排版」：
  - 元素对齐（左/中/右）：动态宽度模式或没有可对齐目标时整行隐藏。
  - 文字对齐（左/中/右）：仅对 `bilingual-text` / `small-bilingual-text` 生效；无目标时隐藏。
  - 内容对齐（左/右）：仅对 `number-line` / `text-line` / `exit` 生效；无目标时隐藏。
  - 左右内边距（行高比例）：两个数字框 + 拖拽图标；显示「不同」占位当值不一致（`panel.js:884-900`）。
- 分组「元素颜色」：元素背景色（可透明、可混合）；若选中集合里含出口，提示「出口保持固定黄色。」（`panel.js:902-914`）。
- 「删除选中元素」危险按钮。

### 3.3.4 预设预览面板（`panel.js:1333-1377`）
- 标题 = 预设名；缩略图（56px 行高整行渲染，白底 1px 边框圆角 6）；元素逐条说明（「1. 数字线路 — 3 号线」等，由 `describeElement` 生成，`panel.js:1379-1397`）。
- 提示「可将此预设拖入空行，或直接添加为新行。」
- 主按钮「＋ 添加为新行」：在末尾新增一行并把预设元素填入，退出预览 + toast。
- 危险按钮「🗑 删除此预设」（确认后删除并退出预览）。

## 3.4 全局层
`#toasts`（右上堆叠）与 `#modal-root`（模态挂载点），见 2.12；`#loading-overlay` 启动期遮罩，见 2.12。

---

# 4. 数据模型

## 4.1 SignState 顶层字段

定义与默认值：`createSign()`（`state.js:141-152`）；结构注释 `state.js:5-12`；反序列化校验 `deserializeSign()`（`state.js:655-720`）。

| 字段 | 默认值 | 类型 / 取值范围 | 语义 | 出处 |
|---|---|---|---|---|
| `widthMode` | `'fixed'` | `'fixed' \| 'dynamic'` | 固定宽度 / 动态宽度（由内容撑开） | `state.js:143`、`state.js:672` |
| `width` | `2048` | 整数 32–32768 | 固定模式下的牌面宽度（px）。2048 = 8 格 × 256 | `state.js:144`、`state.js:673-674`、`panel.js:686-689` |
| `rowHeight` | `192`（`Core.DEFAULT_ROW_HEIGHT`） | 整数 32–2048 | 每行高度（px），全牌统一；所有纵向比例与 padding 的基准。192 = 0.75 格 | `core.js:22`、`state.js:145`、`state.js:675-676` |
| `aspectLocked` | `false` | boolean | 锁定宽高比：改行高 / 改宽度时按比例联动另一个 | `state.js:146`、`state.js:714` |
| `backgroundColor` | `'#FFFFFF'` | `#RRGGBB` | 整牌背景色（行背景/元素背景的最终兜底） | `state.js:147`、`state.js:715` |
| `frameWidth` | `0` | 整数 0–512 | 灰框宽度（px，从牌面边界向内）；0 = 不显示 | `state.js:148`、`state.js:677-679`、`panel.js:726-734` |
| `frameColor` | `'#4A4A4A'`（`SIGN_FRAME_COLOR`） | `#RRGGBB` | 灰框颜色。反序列化缺失/非法时回退旧值 `#808080`（`LEGACY_SIGN_FRAME_COLOR`） | `core.js:19-20`、`state.js:149`、`state.js:717` |
| `rows` | `[createRow()]` | `Row[]`，非空 | 行数组，顺序即自上而下 | `state.js:150`、`state.js:669-670` |

派生值（不存储）：
- 牌面高度 = `rowHeight × rows.length`（`render.js:583-594`）。
- 动态模式实际宽度 = `max(rowHeight, 各行元素宽度之和)` 四舍五入（`render.js:531-539`）。
- 布局对象 `layout = {width, height, rows:[{id,y,height,elements:[{id,x,width}],contentWidth}]}`（`render.js:582-594`）。

## 4.2 Row

```js
{ id: uuid, backgroundColor: null | '#RRGGBB', elements: Element[] }
```
（`state.js:122-124`）

| 规则 | 说明 | 出处 |
|---|---|---|
| `backgroundColor = null` | 继承整牌背景色（渲染时 `row.backgroundColor \|\| sign.backgroundColor`） | `state.js:8`、`render.js:870-874` |
| 行背景更新 | `updateRowSettings(sign,rowId,{backgroundColor})`：null/非法颜色恢复继承；颜色随行一起移动 | `state.js:189-200` |
| 行数最少 1 | `deleteRow` 在只剩一行时直接返回原状态 | `state.js:164-175` |
| 行顺序 | `moveRow(sign,fromIndex,toIndex)` 按下标移动（拖拽行手柄/移动端 ▲▼ 使用） | `state.js:177-187` |
| 新行 | 空元素数组，背景 null；插入位置缺省追加末尾 | `state.js:156-162` |
| 行高 | 行本身没有独立高度字段，统一用 `sign.rowHeight` | `state.js:8`、`render.js:588-592` |

## 4.3 Element

```js
{ id: uuid, type: 十种之一, props: { ...默认属性, paddingAuto?: boolean } }
```
（`state.js:126-139`）

- `ELEMENT_TYPES`（顺序固定，`state.js:20-23`）：`arrow`, `bilingual-text`, `small-bilingual-text`, `big-number`, `number-line`, `text-line`, `entrance`, `exit`, `space`, `icon`。
- `createElement(type, props?)`（`state.js:126-139`）：深拷贝 `DEFAULT_PROPS[type]` → 设 `paddingAuto = (type !== 'space')` → 若传 props：先深合并 padding，再浅合并其余字段 → 生成新 uuid。
- 元素 id 用于 DOM `data-element-id` 与选中态；反序列化时重复 id 重新生成（`state.js:681-689`）。
- `CONTENT_ALIGN_TYPES = ['number-line','text-line','exit']`（支持内容左右对齐的元素，`state.js:26`）。
- `ICON_ROTATIONS = [-90, 0, 90, 180]`（`state.js:29`）。
- `ARROW_DIRECTIONS = ['up','down','left','right','left-up','right-up','right-down','left-down']`（`state.js:579-581`）。

## 4.4 十种元素的 DEFAULT_PROPS（全字段）

> 全部 `padding` 默认 `{top:0.2, right:0.2, bottom:0.2, left:0.2}`（`space` 为全 0），均为**相对行高的比例**。统一 `elementAlign` 默认 `'left'`、`backgroundColor` 默认 `null`（透明）。默认值表：`state.js:34-118`。

### 4.4.1 `arrow` 箭头（`state.js:35-42`）
| 字段 | 默认 | 类型/范围/枚举 | 语义 |
|---|---|---|---|
| `direction` | `'left'` | 8 向枚举（见 4.3） | 箭头方向；斜向为 45° 旋转后的轴对齐箭头 |
| `color` | `'#000000'` | `#RRGGBB` | 填充色 |
| `thicknessRatio` | `0.25` | 0.05–0.95（默认 0.25；UI 滑条 0.1–0.5） | 粗细 = 比例 × 内容尺寸 s |
| `backgroundColor` | `null` | `#RRGGBB` / null | 元素盒背景 |
| `elementAlign` | `'left'` | left/center/right | 固定宽度下的通道 |
| `padding` | 0.2×4 | 0–1（上/下）/ 0–8（左/右） | 内边距（比例） |

### 4.4.2 `bilingual-text` 双语文本（`state.js:43-52`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `textZh` | `'站名'` | string | 中文文本（第一行） |
| `textEn` | `'Station'` | string | 英文文本（第二行，小数号） |
| `color` | `'#000000'` | hex | 文字颜色 |
| `bold` | `false` | bool | 加粗（中文与英文一起切 700 字重） |
| `align` | `'center'` | left/center/right | **文字**对齐（不是元素对齐） |
| `backgroundColor` | `null` | hex/null | 元素背景 |
| `elementAlign` | `'left'` | left/center/right | 元素通道 |
| `padding` | 0.2×4 | 同上 | 内边距 |

### 4.4.3 `small-bilingual-text` 小号双语文本（`state.js:53-62`）
字段与 `bilingual-text` 完全相同，差别只在渲染字号：内容盒字号 × 0.75（见 5.7.3）。

### 4.4.4 `big-number` 大文本（`state.js:63-69`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `text` | `'1'` | string | 内容（数字或任意文本，如 `16`、`1/2/3/4`、`16号线`） |
| `color` | `'#000000'` | hex | 文字颜色 |
| `backgroundColor` / `elementAlign` / `padding` | 同通用 | — | — |

### 4.4.5 `number-line` 数字线路（`state.js:70-77`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `lines` | `[{number:'1', color:'#E4002B'}]` | `{number:string, color:hex}[]`，可空数组 | 线路列表；每条渲染「色条 + 线路号」；右对齐时只渲染第 1 条 |
| `textColor` | `'#000000'` | hex | 线路号 + 「号线/Line」标签颜色 |
| `align` | `'left'` | left/right | 内容对齐：right 时整组镜像（编号在左、色块在右）且只渲染第 1 条线路 |
| `backgroundColor` / `elementAlign` / `padding` | 同通用 | — | — |

> 默认色 `#E4002B` 是重庆地铁 1 号线红（注释 `state.js:71`）。

### 4.4.6 `text-line` 文本线路（`state.js:78-88`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `text` | `'环'` | string | 色条右侧大字（思源黑体=MiSans） |
| `textEn` | `'Loop Line'` | string | 英文线路名；与「线」组成双语标签（`nameSink=true` 时）或作为平铺标签第二行 |
| `nameSink` | `true` | bool | 「线路名下沉」：true = 大字显示「环」并与「线/英文」组合；false = 完整线路名与「线/英文」同字号平铺，无「线」后缀 |
| `blockColor` | `'#F2A900'` | hex | 贯穿色条颜色（默认重庆环线黄） |
| `textColor` | `'#000000'` | hex | 文字颜色 |
| `align` | `'left'` | left/right | 内容对齐（right = 色块移到右缘的镜像排版） |
| `backgroundColor` / `elementAlign` / `padding` | 同通用 | — | — |

### 4.4.7 `entrance` 出入口（`state.js:89-95`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `code` | `'1'` | string | 编号（1 / C / 1,5-22 等） |
| `color` | `'#000000'` | hex | 编号与「出入口/Entrance」标签颜色 |
| `backgroundColor` / `elementAlign` / `padding` | 同通用 | — | — |

### 4.4.8 `exit` 出口（`state.js:96-103`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `code` | `'1'` | string | 出口编号 |
| `color` | `'#000000'` | hex | 文字颜色 |
| `align` | `'left'` | left/right | 内容对齐：right 时编号移到「出口」右侧 |
| `backgroundColor` | `'#F7D917'`（`EXIT_COLOR`） | 固定出口黄 | **不允许透明/改色**：反序列化强制重置（`state.js:614`），面板不提供背景色选择 |
| `elementAlign` / `padding` | 同通用 | — | — |

### 4.4.9 `space` 空白占位（`state.js:104-109`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `widthRatio` | `0.5` | 0–8（面板 0.1–8，step 0.1） | 宽度 = 行高 × 比例（与内边距无关） |
| `backgroundColor` | `null` | hex/null | 可选背景（面板可设） |
| `elementAlign` | `'left'` | — | 可作为通道间隔 |
| `padding` | `{0,0,0,0}` | — | 默认全 0；即使改了也不影响宽度公式（仅影响元素背景盒宽？不：背景盒宽由 widthRatio 决定） |

> `space` 不参与自动内边距、无内边距编辑器、无可见内容（`state.js:609`、`state.js:645-647`、`panel.js:973-975`、`render.js:776`）。

### 4.4.10 `icon` 图标（`state.js:110-117`）
| 字段 | 默认 | 类型/范围 | 语义 |
|---|---|---|---|
| `icon` | `'elevator'` | 图标 id 字符串（32 个，见 5.9） | 引用 `js/icons.js` 图标库 |
| `color` | `'#000000'` | hex | 替换图标内 `fill="black"` 的颜色 |
| `rotation` | `0` | `-90 / 0 / 90 / 180` | 绕图标中心旋转；**仅 `cat === 'service'` 的图标生效**（方向图标无视该值） |
| `backgroundColor` / `elementAlign` / `padding` | 同通用 | — | — |

### 4.4.11 与已实现 Java 数据层的字段对照（供实现核对；此节内容不属于网页源码）

`docs/EDITOR_MODULE_CONTRACTS.md` 中的 `CRTSignElement` 把 `props` 平铺成 Java 字段，对应关系：

| 网页 `props` 字段 | Java `CRTSignElement` 字段 | 备注 |
|---|---|---|
| `direction`（arrow） | `arrowDirection` | 仅命名不同，枚举值一致（8 向） |
| `padding.top/right/bottom/left` | `paddingTop/paddingRight/paddingBottom/paddingLeft`（具体命名以该类为准） | 比例值，语义同 4.5 |
| `paddingAuto` | `paddingAuto` | 同上 |
| `color / backgroundColor / elementAlign / textZh / textEn / text / bold / align / thicknessRatio / lines / textColor / nameSink / blockColor / code / widthRatio / icon / rotation` | 同名 | 语义、默认值、取值范围与 4.4 一致（其中 `align` 复用：双语=文字对齐、number-line/text-line/exit=内容对齐） |
| — | `binding` / `bindingIds` | Java 端扩展（MTR 数据绑定），网页格式没有 |

互操作注意（源码行为）：
- 网页 `deserializeSign` → `sanitizeElement` 会用 `Object.assign(defaults, el.props)` 合并属性，**`props` 内的未知键会被保留**（渲染器不会使用它们），因此把 MC 扩展数据放进 `props` 里可与网页项目 JSON 往返；
- 但 `sanitizeElement` 的返回形状固定为 `{id, type, props}`，**放在元素顶层的未知字段会被丢弃**；若要跨端保留扩展数据，应放进 `props`。

## 4.5 padding / paddingAuto 规则

### 4.5.1 换算与上限
- `contentBox(padding, rowHeight)`（`render.js:46-52`）：
  `t = padding.top × h; r = padding.right × h; b = padding.bottom × h; l = padding.left × h; size = h − t − b`，返回 `{t,r,b,l,top:t,size}`。**元素内容尺寸 s = size**。
- 上限（`PADDING_MAX`，`state.js:498-499`）：`top/bottom ∈ [0,1]`，`left/right ∈ [0,8]`。反序列化统一 clamp（`sanitizePadding`，`state.js:501-509`）。
- UI 范围：元素面板左/右 0–8、上/下 0–1、step 0.05（`panel.js:1295-1300`）；批量面板仅左/右 0–8（`panel.js:886-890`）；浮动工具条输入 0–8（`interact.js:157-160`）；拖拽/步进调值同样 clamp 0–8（`state.js:294-302`）。
- 左/右只影响元素宽度（宽度=内容宽+l+r），上/下只压缩内容高度。

### 4.5.2 `paddingAuto`（相邻自动内边距）
- 新建元素默认 `paddingAuto=true`，`space` 恒 false（`state.js:127-129`）。
- 反序列化时**只有显式 `true` 才保留**，旧存档/未标记元素一律视为手动钉住（避免打开老牌面时悄悄改内边距）（`state.js:604-609`）。
- 手动编辑左/右内边距 → `paddingAuto=false`（`state.js:307-320`、`panel.js:1305-1308`）。
- 每次 `App.update` 之后都会运行 `applyPaddingAuto(sign)`（幂等），结果随这次变更一起落盘/进撤销（`ui.js:68-73`、`state.js:538-577`）。
- 算法（逐行、逐元素）：
  1. 跳过 `space` 与 `paddingAuto !== true` 的元素。
  2. 取通道 `lane = elementLane(el, widthMode==='fixed')`（动态模式恒为 `'left'`）。
  3. 只检查数组中**紧邻**的前/后一个元素（`i±1`）；若不存在、是 `space`、或通道不同，则不算邻居。
  4. 贴右通道的数组顺序与视觉左右相反：`hasLeft = (lane==='right') ? 右侧数组邻居 : 左侧数组邻居`，`hasRight` 反之。
  5. 自动侧有邻居 → `0.1`（`PADDING_AUTO_ADJACENT`）；无邻居 → `0.2`（`PADDING_AUTO_DEFAULT`）（`state.js:529-530`）。
  6. 写回 `padding.left/right`。
- 效果：两个相邻元素之间视觉间距为 `0.1h + 0.1h = 0.2h`；行首/行尾自动侧为 `0.2h`。

## 4.6 elementAlign / align / contentAlign 的区别

| 概念 | 属性 | 适用元素 | 取值 | 生效条件 | 作用 |
|---|---|---|---|---|---|
| 元素对齐 | `props.elementAlign` | 所有元素 | left/center/right | **仅固定宽度模式**；动态模式一概忽略（全部贴左流式） | 元素在行内进入哪条排版通道 |
| 文字对齐 | `props.align` | `bilingual-text`、`small-bilingual-text` | left/center/right | 始终 | 双语两行文字在元素文本框内的水平对齐 |
| 内容对齐 | `props.align`（同一字段，批量面板叫 contentAlign） | `number-line`、`text-line`、`exit` | left/right | 始终 | 元素内部整体镜像排版（编号/文字/色块左右次序反转） |

- 反序列化时：`elementAlign` 非法一律 `'left'`（`state.js:615`）；内容对齐类型 `align` 只接受 `'right'`，否则 `'left'`（`state.js:617`）；双语类型 `align` 非法回 `'center'`（`state.js:631`）。
- 批量面板的 `contentAlign` 只是 UI key，写入的仍是 `align`（`ui.js:195-201`）。

## 4.7 aspectLocked（锁定宽高比）

`updateSignSettings(sign, patch)`（`state.js:449-464`）：
1. `next = {...sign, ...patch}`。
2. 若 `next.aspectLocked === true` 且本次改了 `rowHeight`（且值不同）：`next.width = round(sign.width × next.rowHeight / sign.rowHeight)`。
3. 否则若 `next.aspectLocked === true` 且本次改了 `width`：`next.rowHeight = round(sign.rowHeight × next.width / sign.width)`。

注意：
- 判断用的是**补丁生效后**的开关值；只是勾选/取消勾选本身不会立刻缩放。
- 同时改宽和行高时只走第 2 条分支（宽度随行高）。
- 手动选择 1–15 格会强制 `aspectLocked=false`（`state.js:466-472`）。
- UI 里行高与宽度输入都直接调 `updateSignSettings`（`panel.js:646-698`）。

## 4.8 widthMode / 格 / 行高 / 灰框

### 4.8.1 fixed 与 dynamic
| 模式 | 宽度来源 | 行内布局 | 相关 UI |
|---|---|---|---|
| `fixed` | `sign.width`（px）；也可按 1–15 格选择 = `units × 256` | 三通道（左/中/右）+ `elementAlign` | 「固定宽度（px）」输入、格数下拉 |
| `dynamic` | 自适应：`round(max(rowHeight, 最宽行内容宽))` | 全部左起单通道，`elementAlign` 无效 | 隐藏固定宽度输入、显示「当前动态宽度：N px」 |

- 动态模式宽度计算 `computeSignWidth`（`render.js:531-539`）；布局 `layoutSign`（`render.js:582-594`）。
- 切换到动态模式不会清空 `sign.width`，切回固定模式沿用原值；`signWidthUnits` 在动态模式返回 `null`（`state.js:474-479`、`panel.js:675-684`）。
- 固定模式下元素可以排到牌面宽度之外（贴左/居中/贴右互不约束、可重叠）；SVG 根元素默认裁切视口外内容，视觉上被牌面边界裁掉。

### 4.8.2 `SIGN_GRID_SIZE = 256` 的「格」
- 定义：「服务器贴图的一格；宽度不随行高变化」（`core.js:21`）。即 256 px = 游戏里 1 格贴图宽。
- 默认宽度 2048 = 8 格；默认行高 192 = 0.75 格（`core.js:22` 注释「单行标识牌默认0.75格」）。
- 格数快捷档 1–15（`state.js:466-479`、`panel.js:666-684`）；`width = units × 256`，`signWidthUnits = width / 256`（须为 1–15 整数且 fixed 模式）。
- 行高与宽度**不联动**（除非 aspectLocked）；格只用于宽度。

### 4.8.3 rowHeight
- 全局唯一（所有行同高），`sign.rowHeight`；范围 32–2048，UI step 4（`panel.js:646-653`）。
- 所有纵向尺寸的比例基准：`contentBox`、stripe 高度、字号（`s×比例`）、`space.widthRatio×h`、自动内边距、`row.y = i×h`、牌面总高。
- 修改行高会立即改变所有元素字号与宽度（因为字号 = 内容尺寸 × 比例）。

### 4.8.4 frame（灰框）
- `frameWidth`：px，从牌面边界向内的框线宽；0 = 不画（`state.js:148`、`render.js:889`）。
- 几何 `rowFrameRects(signWidth, rowY, rowHeight, fw, first, last)`（`render.js:597-606`）：
  - 左右竖条：`{x:0,y:rowY,w:fw,h:rowHeight}` 与 `{x:W−fw,…}`（每行都有）；
  - 上横条：`{x:0,y:rowY,w:W,h: first?fw:fw/2}`；
  - 下横条：`{x:0,y:rowY+h−(last?fw:fw/2),w:W,h:last?fw:fw/2}`。
  - 即：最上行上缘、最下行下缘为完整 `fw`；相邻行之间的分割线两行各画一半（合起来 = `fw`）。
- 绘制顺序：每行元素全部画完后，再画该行的灰框（`render.js:888-893`），因此灰框**遮挡越界元素内容**；`pointer-events:none` 不拦截点击（`render.js:842-848`）。
- 颜色：`sign.frameColor || LEGACY_SIGN_FRAME_COLOR`（`render.js:892`）。
- 新牌默认深灰 `#4A4A4A`；旧项目缺色时回退 `#808080`（`core.js:19-20`、`state.js:717`）。

## 4.9 序列化 / 反序列化与校验

### 4.9.1 项目 JSON
- `SCHEMA_VERSION = 1`（`state.js:488`）。
- 导出：`JSON.stringify({version:1, sign: sign})`（`state.js:490-492`）；文件 `sign-project-YYYYMMDD-HHMMSS.json`（`storage.js:147-168`）。
- 自动保存：`localStorage['public-sign-autosave']` 存同一结构（`storage.js:10`、`storage.js:48-57`）。
- 导入/恢复：`deserializeSign(json)`（`state.js:659-720`）：
  - 接受字符串或对象；接受 `{version,sign}` 包装或**裸 sign 对象**（`sign = isPlainObject(data.sign) ? data.sign : data`）。
  - 非对象 → 抛「项目文件不是有效的 JSON 对象」；`rows` 非数组 →「缺少 rows 数组」；空数组 →「rows 不能为空」；某行不合法 →「存在无效的行」。
  - 数值收敛：`width` 四舍五入，非法或越界（32–32768）→ 2048；`rowHeight` 非法或越界（32–2048）→ 192；`frameWidth` 非法或 <0 → 0，>512 → 512。
  - 颜色统一 `normalizeHex`：`backgroundColor` 回退 `#FFFFFF`；`frameColor` 回退 `#808080`；行背景非法回退 `null`。
  - 元素经 `sanitizeElement`（`state.js:598-653`）清洗：未知 `type` 直接丢弃（不报错）；重复行/元素 id 重新生成；缺失 props 用默认值补齐。
  - `aspectLocked` 强转 boolean。

### 4.9.2 `sanitizeElement` 逐类规则（`state.js:598-653`）
| 规则 | 内容 |
|---|---|
| 通用 | 非对象或未知类型 → 返回 null（丢弃）；props = 深拷贝默认值 + 传入 props 浅合并；`padding` 经 `sanitizePadding`；`paddingAuto`：仅 `el.props.paddingAuto === true` 保留，`space` 恒 false |
| 颜色 | `props.color` 非法 → `#000000`；`backgroundColor` 非 null 时 normalize（非法 → null）；`exit` 强制 `EXIT_COLOR` |
| `elementAlign` | 非 `center`/`right` → `'left'` |
| `align`（内容对齐类） | 非 `'right'` → `'left'` |
| 双语类 | `textZh/textEn` 强制字符串（null→默认）；`bold = !!bold`；`align` 非 left/right → `'center'` |
| `big-number` | `text` 强制字符串 |
| `number-line` | `lines` 必须数组否则用默认；逐项过滤非对象；`number` 字符串；`color` 非法 → `#424A52`；空数组合法 |
| `text-line` | `text/textEn` 字符串；`nameSink = (nameSink !== false)`（缺省视为 true）；`blockColor/textColor` 归一化 |
| `arrow` | `direction` 非法 → `'left'`；`thicknessRatio` clamp 0.05–0.95 |
| `entrance`/`exit` | `code` 强制字符串 |
| `space` | `widthRatio` clamp 0–8 |
| `icon` | `icon` 非字符串 → `'elevator'`；`rotation` 只接受 -90/0/90/180，否则 0 |

### 4.9.3 id
`Core.uuid()`：优先 `crypto.randomUUID()`，否则模板 `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx` 随机生成（`core.js:225-234`）。

### 4.9.4 元素顺序与「视觉序」
- `Row.elements` 数组是排版与拖拽的权威顺序。
- 贴左/居中通道：数组序 = 视觉从左到右（居中为整组内从左到右）。
- **贴右通道：数组序与视觉序相反**——数组靠前者贴最右缘（`render.js:566-578`、`state.js:261-274`、`interact.js:412-462`）。
- 「全选」「Shift 范围选择」使用**视觉序**：按行顺序，把每行元素按 `x` 升序排列后串接（`ui.js:172-177`）。

---

# 5. 渲染规范（坐标系、盒模型、十种元素配方）

> 纯几何函数（metrics）与绘制函数分离：每个元素类型一个 `*Metrics(el, rowHeight, measure) → {width, 所有绘制坐标}` + 一个 `draw*(g, el, metrics)`；二者共用同一公式（`render.js:2-14`）。`measure` 接口见 5.8。

## 5.1 坐标系与基本约定

- 单位：牌面设计像素（SVG 用户单位）；坐标原点在整牌左上角，x 向右、y 向下（SVG 标准）。
- 牌面 `viewBox = "0 0 layout.width layout.height"`（`render.js:859`）。
- 行 `i` 的 y = `i × rowHeight`（`render.js:591`）。
- 元素先在自己的**局部坐标系**（盒原点 = 元素左缘/行顶）绘制，再由外层整体平移 `translate(floor(x), row.y)`（`render.js:879`）——水平取整消除亚像素透明缝，垂直行 y 为整数。
- 元素盒背景矩形 `(0,0,ceil(width), rowHeight)`（`render.js:822-829`）；命中框同尺寸透明矩形（`render.js:831-836`）。
- 选中态由 SVG `<g class="element selected">` + CSS 控制（`render.js:880-882`、`style.css:371-377`）。

## 5.2 通用盒模型

```
h = sign.rowHeight
p = contentBox(el.props.padding, h)   // p.t,p.r,p.b,p.l 为像素；p.size = h - p.t - p.b = 元素内容尺寸 s
元素宽度 width（各类型公式见 5.7）
元素盒 = (0,0,width,h)
内容盒 = (p.l, p.t, s, s)             // 元素内容起点 x = padding.left*h，y = padding.top*h
```

- 所有字号都是 `s × 某比例`（`s = p.size`），不是行高直接乘比例。
- 文本基线（baseline）统一从「内容盒顶部 + `INK_TOP_LEADING × s` + 实测 ascent」推导（`INK_TOP_LEADING = 0.036`，`render.js:32`、`render.js:149-150`、`render.js:221`）。
- 双语标签内部：中文墨区顶 = 内容顶 + `INK_TOP_LEADING × cs`（`cs` 为宿主内容尺寸）；英文墨区顶 = 中文墨区底 + `ZH_EN_GAP × cs`，`ZH_EN_GAP = 0.123`（`render.js:136-166`）。

## 5.3 行内元素水平布局（fixed 三通道 / dynamic 单通道）

`layoutRowElements(row, h, measure, signWidth, alignEnabled)`（`render.js:547-580`）：

```
slots[i] = {id, width: computeElementWidth(el[i])}
centerTotal = Σ width（仅 fixed 且 elementAlign=='center'）
leftX = 0; rightX = signWidth; centerX = (signWidth - centerTotal) / 2
for each element in array order:
    lane = fixed ? elementAlign(el) : 'left'
    if lane == 'right': rightX -= width; slot.x = rightX
    elif lane == 'center': slot.x = centerX; centerX += width
    else: slot.x = leftX; leftX += width
```

- 固定宽度：三通道互不避让，可能重叠（设计允许，`render.js:549` 注释）。
- 动态宽度：全部当作 `'left'` 从 0 起依次排列。
- `layoutSign` 计算 `row.contentWidth = Σ width`（`render.js:590`），固定模式下仅作信息，不裁剪。

## 5.4 整牌布局

```
computeSignWidth(sign)  // render.js:531-539
  if fixed: return sign.width
  w = sign.rowHeight
  for row: w = max(w, Σ element width)
  return round(w)

layoutSign(sign)        // render.js:582-594
  h = sign.rowHeight
  W = computeSignWidth(sign)
  H = h × rows.length
  alignEnabled = (widthMode === 'fixed')
  rows[ri] = {id, y: ri×h, height: h, elements: layoutRowElements(...), contentWidth}
```

## 5.5 灰框几何

见 4.8.4（`render.js:597-606`）。补充：灰框在行内元素绘制之后追加到同一 `<g class="sign-row">`（`render.js:888-893`），因此**上一层行画完后才会覆盖下一行越界内容**（逐行绘制顺序：sign-bg → row1(bg→elements→frame) → row2(...) → …）。

## 5.6 整牌渲染输出结构（`renderSignInto`，`render.js:855-897`）

```
<svg viewBox="0 0 W H" xmlns=...>
  <rect class="sign-bg" x=0 y=0 width=W height=H fill=sign.backgroundColor/>
  for each row ri:
    <g class="sign-row" [data-row-id]>
      <rect class="row-bg" x=0 y=rowY width=W height=rowHeight fill=row.backgroundColor || sign.backgroundColor/>
      for each element ei:
        <g class="element [selected]" data-element-id data-row-id transform="translate(floor(x),rowY)">
           [<rect fill=element.backgroundColor width=ceil(w) height=h/>]
           …元素内容…
           [<rect class="hitbox" width=ceil(w) height=h fill=transparent pointer-events=all/>]
        </g>
      [灰框 rects class="sign-frame" fill=frameColor pointer-events=none]
```

- `clean=true`（导出/缩略图）时：不写 `data-element-id`、不写 `data-row-id`、不加 hitbox、不加 selected 类（`render.js:814-837`、`render.js:869-885`）。
- 元素有效宽度 `w = max(metrics.width, 1)`；背景/命中框宽 `ceil(w)`（`render.js:820-825`）。
- 单元素独立渲染 `renderElementStandalone(el, rowHeight, measure, maxWidth)` 返回 `<svg viewBox="0 0 w h">`（用于左栏/预设缩略图，`render.js:899-911`）。

## 5.7 十种元素的渲染配方

> 记号：`h = rowHeight`；`p = contentBox(padding,h)`；`s = p.size`；`m = measure`；`FONT_ZH = MiSans 栈`、`FONT_NUM = Frutiger 栈`（`core.js:206-210`）。
> 常量（`render.js:23-41`）：`ZH_FONT_RATIO = 506/800 = 0.6325`；`EN_FONT_RATIO = 285/800 = 0.35625`；`NUM_FONT_RATIO = 1.3`；`STRIPE_RATIO = 10/28 ≈ 0.35714`；`LABEL_BOX_RATIO = 0.75`；`INK_TOP_LEADING = 0.036`；`ZH_EN_GAP = 0.123`；`EN_ALIGN_OFFSET = 1/30`。
> 文本测量 API（`core.js:342-382`）：`m(text, family, weight, sizePx)` 返回**整串**前进宽；`m.ascent(...)` 墨区上伸（墨顶到基线）；`m.descent(...)` 墨区下伸；`m.ink(...) → {abl, abr, adv}`（abl 通常为负=左字肩，abr 为墨区右侧坐标，adv 为前进宽）。

### 5.7.1 `arrow` 箭头（`arrowMetrics` `render.js:170-176`；几何 `render.js:56-127`；绘制 `render.js:675-683`）

- 尺寸：`th = clamp(thicknessRatio, 0.05, 0.95) × s`；箭盒为 `s × s`；元素宽度 `width = s + p.l + p.r`；绘制偏移 `(p.l, p.t)`。
- 轴向（left/right/up/down）三块多边形（`axisArrowGeometry(size=s, thickness=th, direction)`）：
  - `c = s/2`；`k = th / (2√2)`（箭柄半厚）。
  - `left`：`head1=[[0,c],[th,c],[th+c,0],[c,0]]`，`head2=[[0,c],[th,c],[th+c,s],[c,s]]`，`shaft=[[th/2,c−k],[s,c−k],[s,c+k],[th/2,c+k]]`。
  - `right`：水平镜像；
  - `up`：`head1=[[c,0],[c,th],[0,th+c],[0,c]]`，`head2=[[c,0],[c,th],[s,th+c],[s,c]]`，`shaft=[[c−k,th/2],[c+k,th/2],[c+k,s],[c−k,s]]`；
  - `down`：垂直镜像。
  - 形状含义：箭头头由两条 45° 斜边组成（视觉笔画宽 = `th/√2`），箭柄与头等厚衔接（`render.js:57-60` 注释）。
- 斜向（`left-up / right-up / right-down / left-down`，`DIAGONAL_ARROW` `render.js:93-100` + `arrowGeometry` `render.js:102-127`）：
  1. 设 `L = s×√2 − th`；
  2. 用轴向几何 `axisArrowGeometry(L, th, base)`，`base` 为 `'left'`（left-up、left-down）或 `'right'`（right-up、right-down）；
  3. 在虚拟盒内绕中心 `(L/2,L/2)` 旋转 `phi`：`left-up φ=+45°`、`right-up φ=−45°`、`right-down φ=+45°`、`left-down φ=−45°`；
  4. 旋转后把箭尖（base=left 时虚拟盒 `(0,L/2)`，base=right 时 `(L,L/2)`）平移到目标角 `corner`：`left-up→(0,0)`、`right-up→(s,0)`、`right-down→(s,s)`、`left-down→(0,s)`。
  5. 公式：`rel(p)=((p.x−L/2)cosφ−(p.y−L/2)sinφ, (p.x−L/2)sinφ+(p.y−L/2)cosφ)`；`o = corner×s − rel(tip)`；最终点 = `rel(p)+o`。
- 绘制：三个多边形依次以 `el.props.color` 填充（无描边）。
- 默认值示例（h=192, padding 0.2）：`s=115.2`、`th=28.8`、宽 192。

### 5.7.2 `bilingual-text` 双语文本（`standaloneBilingualMetrics` `render.js:188-213`，`bilingualMetrics` `render.js:136-166`，绘制 `render.js:685-695`）

- 内容尺寸 `s = p.size`；字号：中文 `zhSize = s × 0.6325`、英文 `enSize = s × 0.35625`；字重 `bold?700:400`。
- 基线：
  - `zhBase = p.t + INK_TOP_LEADING×s + ascent(textZh, ZH, weight, zhSize)`
  - `enBase = zhBase + descent(textZh, ZH, weight, zhSize) + ZH_EN_GAP×s + ascent(textEn, EN, weight, enSize)`
- 墨区：中文 `zhW = m(textZh,…)`、`enW = m(textEn,…)`；文本区宽 `label.width = max(zhW, enW)`。
- **大号（本元素）额外垂直居中**（`render.js:193-208`）：取中文墨区顶、英文墨区顶的最小值 `topMin`，中文墨区底、英文墨区底（含 descent）的最大值 `botMax`，`shift = (s − botMax − topMin)/2`，两基线各加 `shift`。即整组两行文字在内容盒内上下居中。
- 宽度：`width = max(zhW,enW) + p.l + p.r`。
- 绘制：`text` 元素 `fill=color`、`font-family=ZH/EN`、字号/字重如上；`x` 依据文字对齐（`bilingualAlignX` `render.js:635-649`）：
  - left：`zhX = 文本区左缘`，`enX = 左缘 + indent`；
  - right：`zhX = 左缘 + label.width − zhW`，`enX = 左缘 + label.width − enW − indent`；
  - center：各自 `左缘 + (label.width − 自身宽)/2`；
  - `indent = s × EN_ALIGN_OFFSET`（= `s/30`），仅 left/right 生效。
  - 注意：对齐基准是**纯文本区宽度 `textW`（不含 padding）**（`render.js:686-692` 注释）。
- 空文本：`m('') = 0`；若中文/英文为空，`tops/bottoms` 只统计非空项（`render.js:195-203`）；宽度仍按 max 计算。

### 5.7.3 `small-bilingual-text` 小号双语文本（`render.js:184-186`）
- 复用同一 `standaloneBilingualMetrics`，但 `boxRatio = 0.75`：
  - 字号 = `(s × 0.75) × 0.6325` / `(s × 0.75) × 0.35625` = `s×0.4744` / `s×0.2672`；
  - 垂直锚定用宿主 `cs = s`（`INK_TOP_LEADING×s`、`ZH_EN_GAP×s` 仍按 s）；
  - **不做垂直居中**（只有 boxRatio===1 才居中，`render.js:193` 注释：小号沿用组合标签的顶对齐）；
  - 仍保留自己的 padding、对齐、颜色（`render.js:182-183` 注释）。
- 宽度 = `max(zhW,enW) + p.l + p.r`。

### 5.7.4 `big-number` 大文本（`bigNumberMetrics` `render.js:215-232`；`inkTextLayout` `render.js:285-321`；`digitLayout` `render.js:234-274`；绘制 `render.js:697-706`）
- 字号 `size = s × 1.3`（Frutiger，缺字回退 MiSans 栈）。
- 文本按「墨区排版」`inkTextLayout(text, slotSize=s, fontSize=size, font=FONT_NUM, edge='lsb')`：
  - 单字符且 edge≠'margin'：`x = m(text)/2`（该字符前进盒中心），`textW = m(text)`。
  - 多字符：用 `digitLayout` 逐字按墨区排：
    - `ref = m.ink('3', …)`；`margin = (s − ref.adv)/2 − ref.abl`；`gap = (−ref.abl) + (ref.adv − ref.abr)`；
    - `inkLeft = margin`；对每个字符：`x = inkLeft + ink.adv/2 + ink.abl`（text-anchor=middle 的绘制点，使墨区左缘落在 inkLeft）；`inkLeft += (ink.abr+ink.abl) + gap`；
    - `advance = inkLeft − gap + margin`；`textW = advance`（edge='margin'）；edge='lsb' 时整体左移 `shift = −firstInk.abl − margin`，`textW = 末位 x + 末位 adv/2`。
  - 返回 `{chars:[{ch,x}], textW, firstInkLeft, lastInkRight}`。
- 基线：`base = p.t + INK_TOP_LEADING×s + ascent(text, FONT_NUM, 400, size)`（数字墨区顶与双语文本中文墨区顶统一）。
- **光学水平居中**：`optical = (firstInkLeft − (textW − lastInkRight))/2`；每个字形 x 再减 `optical`，使左右墨区间距相等（`render.js:223-229` 注释）。
- 绘制：逐字 `<text text-anchor="middle" x = p.l + d.x y = base fill=color font-family=FONT_NUM font-size=size>`。
- 宽度：`width = textW + p.l + p.r`。
- 默认值示例（h=192, padding .2）：`s=115.2`、字号 149.76。

### 5.7.5 `number-line` 数字线路（`numberLineMetrics` `render.js:323-384`；绘制 `render.js:708-722`）
- 内容尺寸 `s`；色条宽 `stripeW = s × (10/28)`；色条高 `stripeH = h − p.t`（从内容顶一直画到行底）。
- 「号线 / Line」标签：`label = bilingualMetrics('号线','Line', s×0.75, m, false, s)`：
  - 标签内容盒 `0.75s` → 字号 `zhSize = 0.75s×0.6325`、`enSize = 0.75s×0.35625`；
  - 垂直锚定按宿主 `cs = s`：`label.zhBase = INK_TOP_LEADING×s + ascent(...)`，`enBase` 同理；`labelTop = p.t + label.zhBase − label.zhAscent`（「号线」墨区顶）。
- 逐条线路：`num = String(line.number||'')`；`t = inkTextLayout(num, s, s×1.3, m, FONT_NUM, 'margin')`；
  - 数字基线 `base = labelTop + ascent(num, FONT_NUM, 400, s×1.3)`（数字墨区顶与「号线」墨区顶齐平）；
  - `advance = t.textW`；`margin = t.margin`（首条线路的基准墨边距 M₃）。
- 对齐与 x 计算（`right = props.align === 'right'`；**right 时只取 lines[0]**）：
  - **left（默认）**：
    - `labelX = p.l + Σ advance + Σ stripeW`，`labelAlign='left'`；
    - 第 i 条：`stripeX = p.l + i×stripeW + advAcc`（advAcc = 前 i 条 advance 之和），`digitsX = stripeX + stripeW`；
    - 顺序：色条 → 数字 → …（每条再来一组）→ 「号线」标签；
    - `width = labelX + label.width + p.r`。
  - **right**：
    - 数字从内容左缘起排：`digitsX = p.l + advAcc`；
    - `labelX = p.l + Σ advance`（标签紧跟数字组）；
    - `stripesX0 = labelX + label.width + firstMargin`（`firstMargin = lines[0].margin`，使「标签→色块」间距与左对齐「色块→数字」一致）；
    - `stripeX = stripesX0 + i×stripeW`；`width = stripesX0 + ΣstripeW + p.r`。
- 绘制：每条线路先画 `<rect x=stripeX y=p.t w=stripeW h=stripeH fill=line.color>`，再逐字画数字（text-anchor=middle，fill=`textColor`）；最后画「号线/Line」双语标签（`fill=textColor`，`drawBilingualBox(..., indentScale=0)`）。

### 5.7.6 `text-line` 文本线路（`textLineMetrics` `render.js:386-440`；绘制 `render.js:724-746`）
- 公共：`stripeW = s×10/28`；`stripeH = h − p.t`；`gap = s/8`；色条颜色 `blockColor`；文字颜色 `textColor`。
- **下沉模式（`nameSink !== false`，默认）**：
  - 大字字号 `fontSize = s`（与数字线路的数字同视觉高度）；逐字墨区排版 `inkTextLayout(text, s, s, m, FONT_ZH, 'lsb')`（中文字体），得 `textW、textChars、textX`；
  - 标签 `label = bilingualMetrics('线', textEn, s×0.75, m, false, s)`（中文「线」+ 英文名）；
  - 基线 `base = p.t + label.zhBase − label.zhAscent + ascent(text, FONT_ZH, 400, s)`（大字墨区顶与「线」墨区顶齐平）；
  - left：`stripeX=p.l`；`textX=p.l+stripeW+gap`；`labelX=p.l+stripeW+gap+textW+gap`；`width=p.l+stripeW+gap+textW+gap+label.width+p.r`；
  - right：`textX=p.l`；`labelX=p.l+textW+gap`；`stripeX=labelX+label.width+gap`；`width=stripeX+stripeW+p.r`。
  - 绘制：色条 → 大字逐字 `<text text-anchor=middle font-family=ZH font-size=s fill=textColor>` → 「线/英文」双语标签。
- **平铺模式（`nameSink === false`）**：
  - 标签 `flat = bilingualMetrics(text, textEn, s×0.75, m, false, s)`（完整线路名 + 英文名，同一 0.75s 标签排版；**不显示「线」后缀**）；
  - left：`stripeX=p.l`；`labelX=p.l+stripeW+gap`；`width=p.l+stripeW+gap+flat.width+p.r`；
  - right：`labelX=p.l`；`stripeX=labelX+flat.width+gap`；`width=stripeX+stripeW+p.r`；
  - 绘制：色条 + 双语标签（无大字）。
- 默认值示例：h=192、padding .2 → s=115.2、stripeW≈41.14、gap=14.4、大字字号 115.2。

### 5.7.7 `entrance` 出入口（`codeLabelMetrics` `render.js:442-481`；`entranceMetrics` `render.js:483-485`；绘制 `render.js:748-770`）
- `size = s×1.3`（编号字号，Frutiger）；`gap = s/8`（**注意：源码计算了 gap 但未叠加到编号↔标签间距**，注释说明末位基准墨边距 M₃ 已含于编号宽，`render.js:453-455`）。
- 标签 `label = bilingualMetrics('出入口','Entrance', s×0.75, m, false, s)`；
- `codeBase = p.t + label.zhBase − label.zhAscent + ascent(code, FONT_NUM, 400, size)`（编号墨区顶与标签墨区顶齐平）；
- 编号排版 `codeT = inkTextLayout(code, s, size, m, FONT_NUM, 'lsb')`；`trailingMargin = (s − ink3.adv)/2 − ink3.abl`（`ink3 = m.ink('3')`，即基准墨边距 M₃）；`codeW = codeT.lastInkRight + trailingMargin`；
- **entrance 永远左对齐**（`alignable=false`，内容对齐不适用）：
  - `codeX = p.l`；`labelX = p.l + codeW`；`labelAlign='left'`；`width = p.l + codeW + label.width + p.r`；
  - 近似公式：`labelX = p.l + 编号宽`（M₃ 内含于编号宽）。
- 绘制：编号逐字（text-anchor=middle、fill=`color`）→ 「出入口/Entrance」双语标签。

### 5.7.8 `exit` 出口（`exitMetrics` `render.js:487-489`；绘制 `render.js:772-774`）
- 与 entrance 同一 `codeLabelMetrics`，但：
  - 标签为「出口 / Exit」；`alignable=true`，支持 `align='right'`；
  - **出口标签文字颜色**：入口/出口用 `el.props.color`（不随背景自动对比色）；
  - 元素背景固定由通用渲染层画 `el.props.backgroundColor = EXIT_COLOR (#F7D917)`，覆盖整个元素盒（含 padding）。
- right 模式 x 公式（镜像）：
  - `rightCodeX = p.l + label.width + trailingMargin`（标签侧留 M₃）；`codeX = rightCodeX`；`labelX = p.l`；`labelAlign='right'`；
  - `width = p.l + label.width + codeW + p.r`；
  - 绘制顺序仍是先编号后标签（编号被放在标签右侧）。
- 单字符与多字符：编号都按墨区排版绘制；空编号 `codeChars` 为空时回退为整串 `text` 以 `codeCenterX` 居中（`render.js:750-764`）。

### 5.7.9 `space` 空白占位（`spaceMetrics` `render.js:491-493`；`drawSpace` `render.js:776`）
- 宽度 `width = widthRatio × h`（用**行高**，不是内容尺寸）；无内容、无内边距换算（padding 对该类型完全无影响）。
- 若设了 `backgroundColor`，通用渲染层会画 `ceil(width) × h` 的背景矩形。
- 宽度可为 0（`widthRatio=0`）；渲染层用 `max(width,1)` 兜底（`render.js:821`）。

### 5.7.10 `icon` 图标（`iconMetrics` `render.js:495-500`；绘制 `render.js:778-796`）
- 正方形内容：`width = s + p.l + p.r`；图标绘制区 `(p.l, p.t, s, s)`。
- 绘制：内嵌 `<svg x=p.l y=p.t width=s height=s viewBox=icon.vb preserveAspectRatio="xMidYMid meet">`，内部 HTML = `icon.body` 把 `fill="black"` 全部替换为 `el.props.color`。
  - 非方形 viewBox（401×400、60×60 等）按 `xMidYMid meet` 等比留白居中。
- 旋转：`rotation = (icon.cat === 'service') ? Number(props.rotation) : 0`；只接受 `-90/90/180`（0 时不加 transform），否则 0；transform 加在内嵌 `<svg>` 上：`rotate(deg, p.l+s/2, p.t+s/2)`（顺时针为正，SVG 坐标系）。
- 注意颜色替换的边界（源码行为）：只有 `fill="black"`（小写、双引号）会被替换；`no_entry` 内是 `fill="#BC0000"`（不换色）、`china_railway` 有 `stroke="#000000"` 的空心外框（描边保持黑色，内部黑色路径会被换色）。图标 body 形状/描边见 `icons.js` 对应条目。

## 5.8 文本测量模型（复刻关键）

- 测量器由浏览器 Canvas 2D 提供（`core.js:342-382`）：
  - `m(text, family, weight, sizePx)`：`ctx.font = weight + ' ' + size + 'px ' + family; ceil(ctx.measureText(text).width)`——**整串前进宽**（含字距/kerning）。
  - `m.ascent(...)`：`actualBoundingBoxAscent` 向上取整；不支持时回退 `0.75×size`。
  - `m.descent(...)`：`actualBoundingBoxDescent`；不支持时回退 `0.2×size`。
  - `m.ink(...)`：`{abl: actualBoundingBoxLeft, abr: actualBoundingBoxRight, adv: measureText().width}`；不支持时回退 `abl=-0.05size, abr=0.85size`。
  - 语义：测量时对齐点在原点、文本从 x=0 开始；`abl` 为墨区左缘（通常为负 = 左字肩向右），`abr` 为墨区右缘坐标，`abl+abr` = 墨区宽，`adv` = 前进宽。
- 字体栈（`core.js:206-210`）：`FONT_ZH = "'MiSans', 'Microsoft YaHei', sans-serif"`；`FONT_EN = FONT_ZH`；`FONT_NUM = "'Frutiger', 'MiSans', Arial, sans-serif"`；`FONT_NUM_CONDENSED` 仅保留未用；`FONT_METRICS` 旧度量常数（zhAsc 1.16 等）当前**不参与**布局（注释 `core.js:212-213`）。
- 字体预加载清单（`core.js:216-221`）：MiSans 400/600/700 + Frutiger 400（导出时另按需内嵌，见 7.7）。
- 布局对「墨区」的依赖点（复刻时不能简单用等宽字距替代）：
  1. 数字线路/大文本/编号的**逐字墨区排版**（`digitLayout`、`inkTextLayout`）——决定数字与色条/标签的间距、「1」等窄数字的视觉居中；
  2. 大文本的**光学居中**（5.7.4）；
  3. 双语文本的**垂直墨区锚定**与文字块居中（5.7.2）；
  4. 元素宽度 = 文本前进宽（整串测量）——影响动态宽度、居中/右对齐、拖放落点。
- 由于整串宽度用 `measureText`（含 kerning），而逐字排版用单字测量之和近似，两者对多位数会略有差异；源码直接采用各自结果（宽度用整串 `m(text)`；逐字位置用单字 ink）。此为源码既有精度模型，复刻时照抄。

## 5.9 图标库清单（`js/icons.js`，32 个）

- 结构：`ICONS = { id: { name, cat, vb, body } }`；`SignIcons.get(id)` 未知 id 回退 `elevator`；`CATS = [{id:'arrows',name:'方向图标'},{id:'service',name:'服务设施'}]`（`icons.js:42-46`）。
- 渲染颜色替换：见 5.7.10。
- **服务设施类（20 个，`cat:'service'`，可旋转）**：

| id | 中文名 | viewBox |
|---|---|---|
| `accessible` | 无障碍 | 0 0 400 400 |
| `accessible_elevator_platform` | 无障碍电梯平台 | 0 0 400 400 |
| `accessible_passage` | 无障碍通道 | 0 0 400 400 |
| `accessible_ramp` | 无障碍坡道 | 0 0 400 400 |
| `accessible_restroom` | 无障碍洗手间 | 0 0 400 400 |
| `check_in` | 检票 | 0 0 400 400 |
| `china_railway` | 中国铁路 | 0 0 400 400 |
| `elevator` | 电梯 | 0 0 400 400 |
| `exit` | 出口 | 0 0 400 400 |
| `exit_left` | 出口（向左） | 0 0 400 400 |
| `exit_right` | 出口（向右） | 0 0 400 400 |
| `metro` | 地铁 | 0 0 401 400 |
| `monorail` | 单轨 | 0 0 401 400 |
| `no_entry` | 禁止通行 | 0 0 60 60 |
| `nursing` | 母婴室 | 0 0 400 400 |
| `restroom` | 洗手间 | 0 0 400 400 |
| `stairs` | 楼梯 | 0 0 400 400 |
| `tickets` | 票务 | 0 0 400 400 |
| `train` | 列车 | 0 0 401 401 |
| `waiting` | 候车室 | 0 0 400 400 |

- **方向图标类（12 个，`cat:'arrows'`，不响应 rotation）**：`arrow_up` 向上、`arrow_down` 向下、`arrow_left` 向左、`arrow_right` 向右、`arrow_left_up` 左上、`arrow_right_up` 右上、`arrow_right_down` 右下、`arrow_left_down` 左下、`arrow_ahead_left` 前方向左、`arrow_ahead_right` 前方向右、`arrow_back_left` 左行向后、`arrow_back_right` 右行向后；viewBox 均为 `0 0 56 56`。
- 左栏「方向指示」12 张卡片直接创建对应 `icon` 元素（`panel.js:40-53`）；「服务图标」分类仅提供一张默认 `elevator` 卡片，进入右栏图标网格后可切换其它服务图标（`panel.js:60`、`panel.js:1227-1272`）。

## 5.10 背景与叠放顺序总表

| 层 | 内容 | 颜色来源 | 出处 |
|---|---|---|---|
| 1 最底 | 牌面背景 `<rect class="sign-bg">` | `sign.backgroundColor`（默认 `#FFFFFF`） | `render.js:861-864` |
| 2 | 行背景 `<rect class="row-bg">`（每行整宽 × 行高） | `row.backgroundColor || sign.backgroundColor` | `render.js:870-874` |
| 3 | 元素盒背景（仅 `props.backgroundColor != null`） | `props.backgroundColor` | `render.js:826-829` |
| 4 | 元素内容（图形/文字/图标） | 各自 props 颜色 | 5.7 |
| 5 | 元素 hitbox（透明，仅编辑器） | — | `render.js:831-836` |
| 6 | 灰框（每行，宽度 >0 时） | `frameColor` | `render.js:888-893` |
| 编辑器叠加 | overlay 行带/手柄/按钮/指示器（HTML，不参与导出） | 见 2.10 | `interact.js:80-140` |

---

# 6. 交互规格

## 6.1 输入模型总览

| 输入 | 实现 | 适用范围 | 出处 |
|---|---|---|---|
| 元素点击 / 拖拽 | Pointer Events（`pointerdown/move/up/cancel`），按 `pointerId` 锁定会话 | 鼠标 + 触屏 + 笔 | `interact.js:5-13`、`interact.js:50-58` |
| 左栏卡片拖放 | HTML5 DnD（自定义 MIME） | 仅桌面（移动端禁用） | `panel.js:115-141`、`interact.js:772-865` |
| 触屏画布手势 | 自实现 pointer 手势层（pinch/pan/double-tap） | 仅 ≤768px 且 `pointerType==='touch'` | `interact.js:867-1030` |
| 键盘 | `document.keydown` + 浏览器原生 copy/cut/paste 事件 | 全局（输入框/模态除外） | `interact.js:519-542`、`clipboard.js:105-118` |
| 侧栏/工具栏按钮 | 普通 click | 全局 | 各文件 |

**点击穿透与抑制**：pointerdown 时记录「是否落在编辑控件上」与位移；pointerup 时若位移 >5px（欧氏距离）或按在控件上，则抑制紧随的 click（`interact.js:471-515`）。「控件」白名单 `selectionControls`（`interact.js:29`）：`button, input, select, textarea, label, a, summary, [contenteditable], [role=button], [role=tab], .palette-card, .preset-card, .row-handle, .field, .panel-section, .padding-editor, .panel-header, #element-toolbar, .modal-mask, .toast`——点击这些不会取消选中元素。

## 6.2 从左侧添加元素（点击）

- 点击卡片 → 创建元素（图标卡带 `icon` 预设属性）→ `App.update(s => addElement(s, targetRowId, created))`。
- **目标行**：若当前有主选中元素且能找到 → 该元素所在行，追加到行末；否则 → 最后一行末尾（`panel.js:315-334`）。
- 移动端添加后自动选中新元素（`App.select(created.id)`，`panel.js:321`）；桌面不选中。
- 每次添加 toast「已添加「类型名」」（success）。
- 空白占位卡片没有可见预览，用 36px 虚线框表示（`panel.js:100-113`、`panel.js:297-299`）。

## 6.3 从左侧拖放（HTML5 DnD）

- MIME：新元素 `application/x-sign-new`；预设 `application/x-sign-preset`；同时写 `text/plain` 兜底；payload 为 JSON（`panel.js:116-124`、`interact.js:31-32`）。
- 拖到画布 `#canvas-wrap` 上：
  - `dragover`：识别 MIME；`dropEffect='copy'`；把屏幕坐标换算为牌面坐标；找行；**新元素**按左通道计算落点并显示竖直插入指示线；**预设**只有目标行为空行才显示可放置（`drop-ok`），否则该行显示 `drop-invalid` + 「🚫 预设只能放入空行」（`interact.js:787-818`）。
  - `drop`：新元素 → `addElement(s, rowId, createElement(type,props), gap)`；预设 → 用 `Presets.materialize(id)` 的元素数组**整体替换该行元素**（保留行 id/背景），toast「已应用预设」（`interact.js:830-865`）。
  - `dragleave`：离开画布时清除高亮与指示器（`interact.js:822-828`）。
- 空行判定以 `row.elements.length === 0` 为准。

## 6.4 选中

**选择状态结构**（`ui.js:53-55`）：`App.selection = { elementIds: string[], elementId: 主选中|null, anchorId: 范围锚点|null }`；`selectedIds()` 在 `elementIds` 为空时回退 `[elementId]`，并过滤已不存在的 id（`ui.js:157-161`）。

| 操作 | 行为 | 出处 |
|---|---|---|
| 单击元素 | 单选该元素（`App.select(id)`） | `interact.js:500-503` |
| Ctrl/Cmd + 单击 | 加选/减选：已在选择中则移除（主选中改为剩余最后一个），否则加入；`elementId` 为当前点击/剩余项 | `ui.js:150-154` |
| Shift + 单击 | 范围选择：以 `anchorId` 为锚，在**视觉序**（按行、行内按 x 升序）中取区间；配合 Ctrl 时与现有选择并集 | `ui.js:141-149`、`ui.js:172-177` |
| 全选 | `Ctrl/Cmd+A` 或工具条「全选」→ 按视觉序选中全部 | `interact.js:526-528`、`ui.js:179` |
| 点击空白处 | 若已有选中且目标不在控件白名单、无模态 → 取消选中（回到标识牌设置） | `interact.js:495-509` |
| Escape | 取消未完成拖拽 + 取消选中 | `interact.js:529-531` |
| 点选后自动行为 | 打开对应右栏模式；若在预设预览则退出预览（`selectMany` 中 `presetPreviewId=null`） | `ui.js:163-170` |

- 选择变化会 `breakHistoryGroup()`（打断撤销合并），并立即 `renderAll()`（`ui.js:163-170`）；**选择本身不进撤销栈**。
- 多选时右栏进入「批量属性」；主选中元素用于浮动工具条、内边距控件、对齐控件的值来源。
- 选中 ≥2 个元素时，拖动任意一个已选元素会整组拖动；若拖的是未选中元素，先单选它再拖（`interact.js:573`）。
- Shift 范围选择依赖 `anchorId`：第一次单击设置锚点，Shift 点击期间锚点保持不变（`ui.js:143-147`）。

## 6.5 元素拖拽移动（行内排序 / 跨行）

会话状态：`elemDrag = {elementId, ids, rowId, pointerId, active, ghost, startX, startY, dx, dy, target}`（`interact.js:546`）。

1. **按下**：在 `g.element` 上 `pointerdown`；ctrl/meta/shift 时不启动拖拽（留给选择）；鼠标下 `preventDefault`；记录会话并挂 document 级 move/up/cancel 监听（`interact.js:548-570`）。
2. **激活阈值**：位移 `|dx|+|dy| ≥ 5px` 才激活（`interact.js:626-630`）；未激活就抬起的会话交给 click 选中。
3. **激活时**：确保该元素被选中；按 `layout` 计算所有被拖元素的联合外接盒；克隆它们的 SVG 节点（去掉 `class` 与 hitbox）塞进 `.drag-ghost`（fixed 定位、透明度 .65）；原点标记 `.element.dragging`（透明度 .35）；`body.drag-in-progress`（`interact.js:572-622`）。
4. **移动**：幽灵跟随鼠标（保持按下点相对偏移）；把指针换算成牌面坐标，找目标行；用 `dropTargetAt` 计算落点 `{gap, indicatorX}`；显示竖直指示线并给目标行加 `drop-ok`；记录 `elemDrag.target = {rowId, gap}`（`interact.js:624-658`）。
5. **落点算法 `dropTargetAt(rowIndex, x, align, draggedWidth)`**（`interact.js:412-462`）：
   - 被拖元素所在通道 `lane`（fixed 模式下按元素 `elementAlign`，dynamic 一律 left）。
   - 取该行**同通道**元素的数组下标 `laneIdx`。
   - 通道为空：落点 = 该行数组末尾；指示线 x：left→0，center→`max(0,(W−draggedWidth)/2)`，right→`W`。
   - **贴右通道**（数组序与视觉相反）：从中最右端开始，指针落在某元素中点右侧则继续；间隙 `gap = j===0 ? laneIdx[0] : laneIdx[j-1]+1`；指示线在右缘（`x+width`）或最左元素的左缘。
   - **贴左/居中通道**：指针落在某元素中点右侧则继续；间隙 `gap = j<len ? laneIdx[j] : laneIdx[last]+1`；指示线在该元素左缘，末尾时在最右元素右缘。
6. **放下**：若存在 target 且非 `pointercancel` → `App.update(s => State.moveElements(s, ids, target.rowId, target.gap))`；清理幽灵/指示器/高亮；抑制紧随的 click（`interact.js:669-697`）。
7. **取消**：`pointercancel`、Escape、触屏第二指落下（会话未激活时）→ 清理（`interact.js:699-712`）。

`State.moveElements(sign, ids, toRowId, gapIndex)` 语义（`state.js:368-383`）：
- `gapIndex` 是**当前布局（含被拖元素）中的数组间隙序号**：目标行内、位于第 gapIndex 个元素之前。
- 取目标行，`gap = clamp(gapIndex, 0, 目标行当前元素数)`；`removedBefore = 目标行前 gap 个元素中属于被拖集合的数量`；先从各行移除所有被拖元素，再插入到目标行下标 `gap − removedBefore`。
- 被拖元素按照「全牌行顺序 + 行内数组顺序」收集成 `moved`，组内相对顺序不变。
- 同行内拖动时，落点语义让视觉落点与数组下标一致（贴右通道的数组序反转由 `dropTargetAt` 负责换算）。

## 6.6 元素左右微调（浮动工具条 ←/→）

- `State.moveElementsBy(sign, ids, direction)`（`state.js:342-366`）：
  - `direction ∈ {-1, +1}`（-1 向左/视觉前移）。
  - 对每一行、每一条通道（`left`/`center`/`right`）分别处理：取出该通道元素数组下标；**贴右通道先把下标数组反转**使其按视觉序；从视觉第 2 个开始按方向扫描，若相邻的「前一个被选中、后一个未被选中」则交换。
  - 选中的元素组整体按一个方向移动一位，组内顺序与相互间距保持。
- 按钮可用性：`moveElementsBy(state, selectedIds, ±1) === state` 时禁用（无变化）（`interact.js:299-300`）。
- 动态宽度模式下三通道已合并为 left，逻辑等价于普通左右换位。
- `State.moveElementBy` / `canMoveElementBy`（单元素版，`state.js:276-292`）在编辑器 UI 中**未被调用**（仅导出供测试/外部使用）。

## 6.7 内边距拖拽（左/右）

- 两处入口：浮动工具条的左/右内边距触发钮、右栏元素/批量面板的左/右内边距触发钮；共用 `UI.bindPaddingScrub(trigger, side, getElementId)`（`ui.js:349-407`）。
- 行为：
  1. `pointerdown`（仅主键、鼠标/笔、非 disabled）时要求当前元素存在且被选中；开始会话 `{id, ids, pointerId, startX, lastX, moved}`；`setPointerCapture`；`App.breakHistoryGroup()`。
  2. 移动：累计位移每满 **8px = 一档**，每档 `±0.05` 行高比例；`steps = trunc((clientX − lastX)/8)`；调用 `App.adjustPadding(side, steps×0.05, key)`，`key = ids.join(',')+':padding-scrub:'+side`，并带 `{continuous:true}`——整个拖动过程合并为**一步撤销**（`ui.js:244-249`、`ui.js:384-389`）。
  3. 拖动超过 5px 才显示「正在拖动」视觉（`scrubbing` 类 + body 强制 `ew-resize` 光标且禁选）；拖动结束后抑制可能产生的 click（避免误开输入框）。
  4. 值 clamp 0–8（`state.js:294-302`）；左/右任一侧被手动修改都会把 `paddingAuto` 置 false（钉住）。
- 浮动工具条的 ▲▼ 步进按钮：每次 `App.adjustPadding(side, ±0.05)`（无 key → 每次点击单独一步撤销）；到达 0/8 边界时对应按钮禁用（`interact.js:200-218`、`interact.js:301-305`）。
- 面板四向输入框（上/下 0–1，左/右 0–8，step 0.05）：`change` 事件提交数值（`panel.js:1301-1311`）。

## 6.8 行操作

| 操作 | 触发 | 行为 | 出处 |
|---|---|---|---|
| 添加行 | 「＋ 添加行」按钮（`#add-row-btn`） | `App.update(State.addRow)`：在末尾插入空行；可撤销 | `interact.js:59-61`、`state.js:156-162` |
| 行排序（桌面） | 行手柄「≡」拖动 | 按指针所在行的上/下半决定插入边界（0..n）；放下后 `moveRow(from, to)`，`to = boundary>from ? boundary−1 : boundary`；显示横向虚线指示器 | `interact.js:714-770` |
| 行排序（移动端） | 行芯片 ▲/▼ | `moveRowBy(i, ±1)`；首/末行对应按钮禁用 | `interact.js:367-371`、`interact.js:343-352` |
| 删除行（桌面） | 行右上「✕ 删除行」 | 空行直接删（toast「已删除空行」）；非空行确认「确定要删除此行吗？行内 N 个元素将一并删除。」（确定按钮「删除」）；只剩 1 行时按钮禁用/隐藏 | `interact.js:1034-1048` |
| 删除行（移动端） | 行芯片「✕ 删行」 | 同上；仅 1 行时禁用 | `interact.js:355-362` |
| 存为预设（桌面/移动端） | 行按钮「⭐ 存为预设」/「⭐ 存预设」 | 空行报错「空行不能保存为预设」；否则弹输入框要名称（默认空，占位「如：站台方向牌」），确认后 `SignPresets.save(name, row.elements)` + toast | `interact.js:1050-1062` |
| 清空标识牌 | 设置面板危险按钮 | 确认后保留设置、重置为一行空白，取消选择 | `panel.js:754-764`、`state.js:481-484` |
| 行背景色 | 设置面板「单独设置行背景色」 | 选择行 + 颜色；「恢复标识牌背景色」置 null | `panel.js:820-864` |

- 行拖拽手柄只在行数 >1 时渲染（`interact.js:101`）；触屏无悬停设备常显（`style.css:1117-1124`），移动端则完全由 `#row-strip` 替代（`interact.js:87-88`、`interact.js:334-340`）。
- 删除行不会连带删除选择状态之外的东西；若当前选中元素被删除，后续 `normalizeSelection` 会过滤（`ui.js:25-30`）。

## 6.9 撤销 / 重做

- 历史栈：`undoStack`、`redoStack`，上限 **20** 步（`HISTORY_LIMIT`，`ui.js:16-19`）；快照 = `{state, selection:{elementId, elementIds, anchorId}}`（`ui.js:21-23`）。
- 提交规则 `App.update(pureFn, key, options)`（`ui.js:66-90`）：
  1. 计算新状态并运行 `applyPaddingAuto`；
  2. 新状态与旧状态 `JSON.stringify` 完全相同 → 直接返回（无效移动、相同设置不消耗撤销步）；
  3. `App.state` 存在 且 不满足「key 与上次相同且（连续模式 或 距上次 <800ms）」时，把**旧状态快照**压入 undo；
  4. 任何新提交都会清空 redo；记录 `historyKey=key`、`historyTime=now`；
  5. 支持 `options.selectNew`：提交后自动选中「新旧状态差集」中的新元素（粘贴用）。
- 合并策略：
  - 文本输入：`patchProps` 对 `textZh/textEn/text/code` 生成 key=`元素id:属性名`，连续输入 800ms 内合并为一步；输入框 blur 时 `App.breakHistoryGroup()` 结束合并（`panel.js:810-817`、`panel.js:572-577`）。
  - 内边距拖动：`continuous:true` + 固定 key，整个指针会话合并（结束/开始时 break）。
  - 其余操作（滑条、对齐、删除、行操作、拖拽）都是一次提交一步；例如粗细滑条每次 `input` 都会压一步。
- 撤销/重做 `restoreHistory(from,to)`（`ui.js:39-48`）：先把当前快照压入对方栈（截断到 20）；弹出目标快照；**清空预设预览**；`commitState`（重渲染 + 自动保存）。撤销会恢复完整选择。
- 快捷键：`Ctrl/Cmd+Z` 撤销、`Ctrl/Cmd+Shift+Z` 重做（`interact.js:532-537`）；没有 `Ctrl+Y`。
- `breakHistoryGroup` 调用点：选择变化、内边距拖动开始/结束、文本输入 blur、进入预设预览、撤销/重做（`ui.js:39-48,163-170,251-258`、`panel.js:575`、`ui.js:367,402`）。

## 6.10 复制 / 剪切 / 粘贴

- **格式**：元素剪贴板 JSON，见 7.2（`state.js:385-417`）。
- **两个通道**：
  1. 浏览器原生 `copy`/`cut`/`paste` 事件（`clipboard.js:42-65`）——写/读 `text/plain`，粘贴时只有 JSON 且 `kind==='jr-sign-elements'` 才接管，其它文本交给浏览器默认行为。
  2. 工具条按钮（`clipboard.js:67-94`）——优先 `navigator.clipboard.writeText/readText`；系统剪贴板不可用时保留**页面内副本**，toast 追加「（可在当前页面粘贴）」。
- 屏蔽条件：焦点在 `input/textarea/[contenteditable]` 内、或存在模态遮罩时，不处理（原生文本复制/粘贴保留）（`clipboard.js:7-11`）。
- 大小上限：粘贴字符串 > 8MB 直接报错（`clipboard.js:29-30`）。
- **剪切**：复制后删除选中元素，并记录 `cutTarget = {rowId, gap}`：第一次扫描到的行 + 该行中第一个被选元素的数组下标（`clipboard.js:18-27`）。
- **粘贴落点**（`clipboard.js:29-40`）：
  1. 若当前有主选中元素 → 粘贴到它所在行、**它后面**（`gap = 该元素下标 + 1`）；
  2. 否则若 `cutTarget` 行仍存在 → 粘回剪切位置；
  3. 否则 → 最后一行末尾。
- `pasteElements`（`state.js:419-434`）：按剪贴板行偏移在目标行附近插入（行不够则自动追加空行）；第一个 offset=0 的组插到 `gapIndex`，其余组追加到目标行行末；元素全部重新生成 id、props 深拷贝；提交时 `selectNew:true` 自动选中新元素；toast「已粘贴 N 个元素」。
- 复制/剪切后 toast「已复制 N 个元素」/「已剪切 N 个元素」；页面内副本在跨页面刷新后失效。

## 6.11 删除 / 全选 / Escape

- 删除元素：`Delete` 或 `Backspace`（有主选中元素时，`interact.js:538-542`）；浮动工具条删除钮；批量面板/元素面板危险按钮。删除后 toast（面板按钮路径）。
- 全选：`Ctrl/Cmd+A`、工具条「全选」。
- Escape：取消进行中的元素拖拽（清理幽灵/指示器）并取消选择；模态对话框中 Escape 由对话框自行处理（输入框 Escape 取消）。
- 无选择时按 Delete 无操作（不拦截）。

## 6.12 画布缩放

- 缩放状态：`App.canvasZoom`（会话态，**不持久化**，默认 1；`ui.js:57`）。
- 显示宽度（`ui.js:118-135`）：
  ```
  avail = min(#canvas-area.clientWidth, 1600)
  displayW = min(avail, layout.width) × canvasZoom
  svg.style.width = displayW px；#canvas-wrap 与 #sign-overlay 同步
  ```
  - **不放大**：牌面原始宽小于可用宽时按 1:1 显示（`min(avail, layout.width)`）。
  - 高度由 SVG 自动等比（`height:auto`）。
- 缩放范围：`0.5 – 4`（`ZOOM_MIN/ZOOM_MAX`，`interact.js:873`）。
- 复位途径：移动端点击缩放徽标、空白处**双击**（<300ms、位移<30px）、跨越 768px 断点时自动复位（`interact.js:74`、`interact.js:973-982`、`main.js:132`）。
- **桌面鼠标滚轮不缩放**：源码中没有任何 wheel 事件监听（全仓搜索无 `wheel`），`#canvas-scroll` 保持原生滚动。**（任务书提到的「滚轮缩放」在源码中未实现；这是源码现状，不要臆造。）**
- 触屏 pinch 缩放：见 6.13。
- 徽标显示：`Math.round(canvasZoom×100) + '%'`（`interact.js:1027-1030`）。

## 6.13 触屏手势（仅移动端 ≤768px 且 touch）

参数（`interact.js:873-874`）：`ZOOM_MIN=0.5`、`ZOOM_MAX=4`、`TAP_MS=300`、`TAP_SLOP=30`、`PAN_SLOP=10`；手势状态 `{pointers, count, pan, panMoved, pinch, lastTap}`（`interact.js:876-883`）。

- 单指落在空白处（非元素/行手柄/行按钮/浮动工具条/添加行）且无进行中的拖拽 → 预备平移（记录初始 scrollLeft/Top，`pointercapture` 到滚动容器）。
- 双指 → pinch：记录两指距离与当前 zoom；移动时 `applyCanvasZoom(startZoom × d/dist, 两指中点)`，以两指中点为锚点同步修正滚动位置使内容点不跑（`interact.js:943-951`、`interact.js:1006-1019`）。
- 点击判定：`pan` 存在且未移动（<10px）且无拖拽会话 → 一次「tap」；两次 tap 满足 <300ms、位移 <30px → 复位缩放；否则记为 `lastTap`（`interact.js:960-989`）。
- 冲突规则（`interact.js:867-871`）：元素/行拖拽会锁定自己的 pointerId；未激活（未过阈值）的拖拽会话在第二指落下时取消并让位给 pinch；≥3 指维持现有手势。
- pinch 中一指抬起 → 手势结束，余指需要重新按下才能平移（`interact.js:984-988`）。
- 移动端 `#canvas-scroll` CSS `touch-action:none`，原生滚动完全由手势层接管（`style.css:1061-1069`）。

## 6.14 键盘快捷键全表

| 快捷键 | 作用 | 生效条件 / 备注 | 出处 |
|---|---|---|---|
| `Ctrl/Cmd + A` | 全选元素 | 焦点不在输入控件、无模态、非 IME 组合中 | `interact.js:526-528` |
| `Ctrl/Cmd + Z` | 撤销 | 同上 | `interact.js:532-536` |
| `Ctrl/Cmd + Shift + Z` | 重做 | 同上 | `interact.js:534-535` |
| `Delete` / `Backspace` | 删除选中元素 | 有主选中元素；无模态；不在输入控件 | `interact.js:538-542` |
| `Escape` | 取消元素拖拽 + 取消选择 | 有选中元素时 | `interact.js:529-531` |
| `Ctrl/Cmd + C` | 复制选中元素（写系统剪贴板 JSON） | 由浏览器 copy 事件处理；焦点不在输入控件、无模态 | `clipboard.js:42-54`、`clipboard.js:105-118` |
| `Ctrl/Cmd + X` | 剪切选中元素 | 同上 | `clipboard.js:52` |
| `Ctrl/Cmd + V` | 粘贴元素 | 由浏览器 paste 事件处理；只接管本编辑器 JSON | `clipboard.js:56-65` |
| `Enter` / `Space` | 预设卡片/图标卡片的键盘激活 | 焦点在卡片上时（button 原生行为/显式处理） | `panel.js:398-402` |
| `←/→/Home/End` | 左栏标签页切换（元素/预设） | 焦点在 tab 上 | `panel.js:163-170` |
| `Enter` / `Escape` | 输入对话框确认/取消 | 焦点在模态输入框 | `ui.js:476-479` |

- 输入控件（input/textarea/contenteditable）内**不拦截**任何全局快捷键，保留原生文本编辑（`interact.js:521-523`、`clipboard.js:7-11`）。
- 模态打开时全局快捷键全部失效（`interact.js:524-525`）。
- **没有**方向键微调元素、没有 `Ctrl+Y`、没有 `Ctrl+S`、没有滚轮缩放（源码未实现）。MC 端若把 `Ctrl+Y` 也做成重做，属增强，不影响与网页的互操作（网页只有 `Ctrl/Cmd+Shift+Z`）。

## 6.15 自动保存、多实例、偏好

- 自动保存：每次成功提交后 500ms 防抖写 `localStorage['public-sign-autosave']`（`storage.js:10,47-57`）；`beforeunload`/`pagehide`/页面隐藏时立即落盘（`main.js:146-161`）。恢复失败则用默认牌（`storage.js:59-69`）。
- 多实例守卫：`localStorage['public-sign-instance']` 每 2s 心跳；若另一实例时间戳在 5s 内则弹确认框「检测到已有运行中的编辑器实例。同时编辑多个标签页可能导致数据丢失。是否继续？」；拒绝则阻止启动（`storage.js:11-14,95-143`、`main.js:163-170`）。
- 编辑器偏好 `localStorage['public-sign-prefs']`：`{autoLineColor:boolean, paletteCity:string}`，默认 `{true,'shanghai'}`；城市必须是已知色板（内置或自定义）（`storage.js:12,22-43`）。
- 断点切换（768px 跨界）：清理两侧面板收起态、复位缩放、重建左栏、重渲染（`main.js:117-144`）。

## 6.16 Toast / 确认框 / 输入框 API 语义

- `SignUI.toast(message, type?)`：`type ∈ {'', 'success', 'error'}`；自动消失 2400ms（error 4200ms）（`ui.js:411-418`）。
- `SignUI.confirmDialog(message, title?, confirmText?)`：返回 `Promise<boolean>`；确定按钮为红色危险按钮（`ui.js:426-448`）。
- `SignUI.promptDialog(message, title?, defaultValue?, placeholder?)`：返回 `Promise<string|null>`（取消/Esc/空白 → null）（`ui.js:454-484`）。
- 主要调用点：删除行、删除预设、清空标识牌、删除元素（部分直接执行不弹框：元素删除、空行删除）、保存预设命名。

## 6.17 右栏模式切换触发汇总

| 触发 | 结果 | 出处 |
|---|---|---|
| 选中 1 个元素 | 元素属性面板 | `panel.js:453-458` |
| 选中 ≥2 个元素 | 批量属性面板 | 同上 |
| 选中 0 个 + 预设预览 id 存在 | 预设预览面板 | 同上 |
| 其余 | 标识牌设置面板 | 同上 |
| 点击右栏返回箭头 | 退出预览 / 取消选择 | `main.js:75-78` |
| 点击预设卡片 | 进入预览并清空选择 | `ui.js:252-257` |
| 移动端选中元素或进入预览 | 属性带自动展开（不自动折叠） | `panel.js:495-502` |

---

# 7. 导入导出与持久化

## 7.1 项目 JSON

- 导出/自动保存格式（`state.js:488-492`）：

```json
{
  "version": 1,
  "sign": {
    "widthMode": "fixed",
    "width": 2048,
    "rowHeight": 192,
    "aspectLocked": false,
    "backgroundColor": "#FFFFFF",
    "frameWidth": 0,
    "frameColor": "#4A4A4A",
    "rows": [
      {
        "id": "0d6f…uuid…",
        "backgroundColor": null,
        "elements": [
          {
            "id": "7a1c…uuid…",
            "type": "number-line",
            "props": {
              "lines": [{ "number": "3", "color": "#FCD600" }],
              "textColor": "#000000",
              "align": "left",
              "backgroundColor": null,
              "elementAlign": "left",
              "padding": { "top": 0.1, "right": 0.2, "bottom": 0.2, "left": 0.1 },
              "paddingAuto": true
            }
          }
        ]
      }
    ]
  }
}
```

- 字段含义与默认值见 4.1/4.2/4.4；`id` 为 UUID 字符串；`props` 一定包含完整默认字段（新建元素 = 默认值浅合并后）。
- 导出文件名：`sign-project-<YYYYMMDD-HHMMSS>.json`（`storage.js:147-168`）。
- 导入：文件选择 `.json,application/json`（`storage.js:187-202`），反序列化校验规则见 4.9.1；错误以中文 toast 显示「导入失败：…」。
- 兼容性：导入既接受带 `version/sign` 的包装，也接受裸 `sign` 对象；缺字段以默认值补齐，未知元素类型被丢弃而不是报错。
- MC 端：`docs/EDITOR_MODULE_CONTRACTS.md` 的 `CRTSignLayout.toJson() / fromJson(String)` 已按同一 JSON 结构实现（公共端），可直接与网页项目 JSON 互导；扩展字段的保留规则见 4.4.11。

## 7.2 元素剪贴板 JSON（`kind:'jr-sign-elements'`）

格式（`state.js:385-417`）：

```json
{
  "kind": "jr-sign-elements",
  "version": 1,
  "rows": [
    { "offset": 0, "elements": [ { "id": "…", "type": "bilingual-text", "props": { … } } ] },
    { "offset": 1, "elements": [ … ] }
  ]
}
```

- 生成：`serializeElements(sign, ids)`——按行收集被选元素，`offset = 行下标 − 第一行下标`；元素深拷贝（含 id，但粘贴时会重新生成 id）；不包含整牌设置与选择。
- 校验（`deserializeElements`）：
  - 顶层必须是对象、`kind === 'jr-sign-elements'`、`version === 1`，否则抛「剪贴板中没有有效的标识牌元素」；
  - `rows` 非空数组、长度 ≤1000；
  - 每个 row：`offset` 为整数 0–999 且**严格递增**（第一行必须 0）、`elements` 非空数组，否则「剪贴板行结构无效」；
  - 每个元素经 `sanitizeElement`，未知类型抛「剪贴板包含未知元素」；
  - 元素总数 >10000 →「剪贴板元素过多」。
- 粘贴：见 6.10。

## 7.3 预设（`localStorage['public-sign-presets']`）

- 结构（`presets.js:10-95`）：

```json
[
  {
    "id": "uuid",
    "name": "站台方向牌",
    "createdAt": 1730000000000,
    "elements": [ { "id": "uuid", "type": "…", "props": { … } } ]
  }
]
```

- 读盘校验：`Array.isArray`，过滤掉 `name` 非字符串或 `elements` 非数组的项；解析失败返回空数组（`presets.js:17-29`）。
- 保存：深拷贝整行元素（保留元素 id，但使用时重新生成）；写盘失败不触发 UI 刷新（`presets.js:31-56`）。
- 使用：`materialize(id)` 过滤未知类型、重新生成 id、深拷贝 props（`presets.js:68-81`）。
- 应用方式：拖入空行（整体替换该行元素）、预设预览面板「＋ 添加为新行」、左栏拖放（见 6.3）。
- 变更事件：保存/删除/改名成功写盘后通知监听者重建左栏（`presets.js:38`、`panel.js:408-410`）。

## 7.4 自定义色板（`localStorage['public-sign-custom-palettes']`）

- 存储结构（`palettes.js:22-27`）：`{"kind":"sign-palettes","version":1,"palettes":[{"id":"custom-…","name":"…","entries":[{"name":"1号线","color":"#2563EB"}]}]}`。
- 限制：色板名 1–40 字；每套 1–64 色；最多 20 套；每个颜色名称 1–40 字且 HEX 合法；id 形如 `custom-[\w-]{1,80}`。
- 与编辑器集成：
  - 颜色选择器的城市下拉出现「我的色板」分组（`color-picker.js:60-65`）；
  - `Core.lineColorFor(number, city)` 会查询自定义色板：自定义色板没有 `lines`，只有 `extras`（entry 的 name/color），数字线路自动配色按名称匹配（`core.js:275-295`）；
  - 多标签页同步：`storage` 事件 + `revision` 计数器（`palettes.js:29-40`）；
  - 色板管理对话框支持新建/复制当前配色/改色名+HEX/上下移/删除/导入导出 JSON（`palette-manager.js:7-78`）。

## 7.5 自动保存与实例键

| 键 | 内容 | 时机 | 出处 |
|---|---|---|---|
| `public-sign-autosave` | `{version:1,sign}` | 500ms 防抖 + 页面退出立即写 | `storage.js:10,47-57`、`main.js:146-161` |
| `public-sign-instance` | `{id, ts}` | 启动时写、每 2s 心跳、退出/隐藏时删 | `storage.js:11,82-143` |
| `public-sign-prefs` | `{autoLineColor, paletteCity}` | 偏好变更时立即写 | `storage.js:12,39-43` |
| `public-sign-presets` | 预设数组 | 预设增删时写 | `presets.js:14,31-38` |
| `public-sign-custom-palettes` | 自定义色板对象 | 色板保存/删除/导入时写 | `palettes.js:5,30-37` |

## 7.6 SVG 导出（`exporters.js:172-200`）

- 能力：把当前牌面渲染成**自包含 SVG 文件**：
  - 用 `clean:true` 渲染（无编辑器属性/hitbox/选中框）；
  - `width`/`height` 属性 = 布局宽高（像素），保留 `viewBox`；
  - 在 SVG 头部插入 `<style>`，把**实际用到的字体**以 base64 `@font-face` 内嵌（`truetype` 格式）：
    - 字体文件映射：MiSans-Regular/Semibold/Bold、Frutiger-Regular/Bold、FrutigerCondensed-Regular/Bold（`exporters.js:16-26`）；
    - 按元素类型扫描所需字体面（`collectUsedFonts`，`exporters.js:104-141`）：双语类按中/英与是否 bold；大文本用 num400（含非 ASCII 时加中文字体）；数字线路恒用 zh+en+num；文本线路用 zh（有英文再加 en）；出入口/出口有编号时用 num 且恒用 zh+en；
  - 文件内容以 `<?xml version="1.0" encoding="UTF-8"?>` 开头；文件名 `sign-<timestamp>.svg`；成功 toast「SVG 已导出（内嵌字体）」。
- 导出期间按钮显示「⏳ 正在准备字体…」并禁用（`panel.js:770-787`）。

## 7.7 PNG 导出（`exporters.js:202-251`）

- 能力：先构建与 SVG 导出一致的自包含 SVG 字符串，再 `SVG → Image → Canvas → PNG` 光栅化：
  - 输出像素尺寸 = 布局实际尺寸（`layout.width × layout.height`），scale 固定 1；
  - 内部超采样：`sampling = max(1, min(4, floor(32768/w), floor(32768/h), floor(sqrt(16777216/(w×h)))))`，以更高分辨率绘制再缩放，保证小牌清晰；
  - 限制：scale 只能 1/2/4（当前只用 1）；输出宽或高 >32768、或面积 >67108864 时报错「PNG 尺寸超出支持范围，请降低导出倍率或标识牌尺寸」；
  - 使用 `imageSmoothingQuality='high'`；PNG 底色由牌面背景色决定（默认白，通常不透明）；
  - 文件名 `sign-<timestamp>.png`；成功 toast「PNG 已导出（W×H px）」。
- 导出不含编辑器覆盖层（行手柄/选中框/工具条），因为导出基于 `SignRender` 纯净渲染。

## 7.8 其它导出能力（本轮只需知道存在）

- 「导出项目（JSON）」/「导入项目（JSON）」见 7.1。
- 自定义色板「导出全部/导入 JSON」见 7.4。
- 预设仅存本机 localStorage，无独立导入导出（源码未提供预设文件导入导出）。

---

# 8. 已知限制与「源码未明示」清单

> 下列条目均为源码核查结论；复刻时若需要「增强」请单独立项，不要写入对本规格的忠实实现。

| # | 事项 | 结论 / 依据 |
|---|---|---|
| 1 | 画布滚轮缩放 | **未实现**：全仓无 `wheel` 事件监听；桌面只能滚动，缩放仅移动端 pinch/徽标/双击（`interact.js:867-1030`） |
| 2 | 键盘微调/方向键 | **未实现**：方向键没有绑定；左右移动仅浮动工具条按钮（`interact.js:519-542`、`interact.js:222-261`） |
| 3 | 元素左栏入口 | `arrow` 元素类型**没有 UI 入口**（方向指示全部是 `icon` 元素）；只能通过 JSON/剪贴板/预设进入（`panel.js:55-69`） |
| 4 | 行数/元素数上限 | 源码**未明示**行数与元素数上限（`addRow`/`addElement` 无限制）；仅剪贴板有 rows≤1000、elements≤10000 的导入限制（`state.js:397-417`） |
| 5 | 预设数量上限 | 源码**未明示**上限（localStorage 容量限制除外，`presets.js`） |
| 6 | 逐行高度 | **不支持**：行高全牌统一（`state.js:145`、`render.js:588-592`） |
| 7 | 元素对齐 | 仅固定宽度模式生效；动态模式一律左起（`render.js:547-580`、`panel.js:958-968`） |
| 8 | 出口配色 | 背景固定 `#F7D917`，不可改/不可透明（`state.js:614`、`panel.js:1199-1211`） |
| 9 | 数字线路右对齐 | 只渲染第 1 条线路，其余数据保留但隐藏（`render.js:329-331`、`panel.js:1141-1150`） |
| 10 | 固定文本标签 | 「号线/Line」「线」「出入口/Entrance」「出口/Exit」为固定文本，不可编辑；可编辑的只有 `text/textEn/textZh/code/lines`（`render.js:327,399,448,484,488`） |
| 11 | 图标颜色替换范围 | 只替换 `fill="black"`；`no_entry` 的 `#BC0000`、`china_railway` 的 `stroke="#000000"` 外框不受颜色属性影响（`render.js:794`、`icons.js`） |
| 12 | 图标旋转 | 仅 `cat==='service'` 的 20 个图标响应 `rotation`；12 个方向箭头忽略该值（`render.js:783-785`） |
| 13 | 颜色格式 | 只支持 `#RRGGBB`/`#RGB`（统一规范化为大写 `#RRGGBB`）；**没有透明度通道**，透明仅以背景色 `null` 表达（`core.js:244-258`、`state.js:511-513`） |
| 14 | 牌面越界 | 固定模式下三通道互不避让、元素可越出牌面，SVG 视口默认裁切（内容不可见） | 
| 15 | 显示宽度上限 | `fitSignDisplay` 里 `Math.min(area.clientWidth, 1600)` 限制可用宽（`ui.js:125`） |
| 16 | 无效样式类 | `document.body` 拖拽时加的 `drag-in-progress` 类在 `style.css` 中**没有对应规则**（源码未定义视觉） |
| 17 | 未使用的核心代码 | `FONT_METRICS`、`FONT_NUM_CONDENSED`、`badgeTextMetrics`、`contrastTextColor` 在 index 编辑器渲染中未使用（platform 编辑器/测试保留）；`moveElement/moveElementBy/canMoveElementBy` 未被 UI 调用（`state.js:280-292`、`core.js:212-213,322-334,270-273`） |
| 18 | 依赖字体度量 | 布局宽度/字号/基线全部依赖 Canvas 实测字体；字体未加载完成时度量会偏差（有 30s 超时降级与错误 toast，`main.js:10-40,196-198`） |
| 19 | 文本测量混合模型 | 元素宽度用**整串** `measureText`（含 kerning），逐字墨区排位用**单字**测量；两者存在理论上的亚像素差，源码即如此（`render.js:136-321`） |
| 20 | 撤销范围 | 选择变化不产生撤销步，但撤销会恢复当时的选择；`canvasZoom` 不进撤销也不持久化 | 
| 21 | 自动保存 | 只有浏览器 localStorage；无服务端/账号同步；多标签守卫可被用户选择继续而绕过（`storage.js:95-118`） |
| 22 | 移动端拖放 | 左栏卡片 `draggable=false`，移动端无 HTML5 DnD，只能点击添加（`panel.js:126-141`） |
| 23 | 预设放置限制 | 预设只能放入**空行**（拖拽），或通过预览面板「添加为新行」；不能插入非空行（`interact.js:808-817`、`panel.js:1333-1365`） |
| 24 | 断点/设备 | 768px 布局断点；`hover:none` 的触屏常显规则独立于宽度（宽屏平板行控件常显） | 
| 25 | 编辑器深色模式 | 仅界面跟随系统；牌面颜色始终按项目数据渲染，不随主题变化（`style.css:1369-1370`） |
| 26 | 元素多选拖动 | 可整组跨行拖动；不限制目标行是否为空（与预设不同） | 
| 27 | 复制到系统剪贴板 | 依赖 `navigator.clipboard` 权限；失败时只有页面内副本（刷新丢失）（`clipboard.js:67-94`） |
| 28 | 第三方素材许可 | MiSans / Frutiger / 图标为第三方素材，遵循各自授权（README:42-46、NOTICE） |

---

# 9. MC Java 端复刻建议摘要

> 本节把 Web 规格翻译成 Minecraft Java 版（1.20.x 时代 GUI API，写法按目标版本调整）的实现建议。
> 原则：**牌面渲染必须自绘**（它不是普通表单 UI）；编辑器控件按项目约束自绘（见 9.2），可借用原版控件的命中/文本/滚动内部逻辑；若允许原版皮肤，则可直接用原版 widget。

## 9.1 总体架构建议

建议拆成 4 个与 Web 对应的纯逻辑/渲染层，便于逐项对照本规格验收：

```
SignData（= SignState/Row/Element，字段名与 JSON 完全一致）
   ↑ 编辑命令（不可变/深拷贝快照），对应 state.js 全部纯函数
SignLayout（= render.js 的 layoutSign/metrics）
   ← 依赖 SignTextMetrics（= core.js 的 Canvas 测量器移植）
SignRenderer（把 SignLayout 画到 GuiGraphics / Framebuffer）
SignEditorScreen（三栏布局 + 交互，对应 panel.js/interact.js/ui.js）
```

- **布局与渲染分离**：先算出 `{width, height, rows[{y, elements[{x, width, …绘制坐标}]}]}`，再统一绘制；选中框、命中检测都用同一份 layout（Web 源码就是这样做的，`render.js:814-838,855-897`）。
- 数据层保持与项目 JSON **同构**（字段、枚举、默认值、清洗规则、`version:1`、剪贴板 `kind:'jr-sign-elements'`），这样 Web 导出的项目 JSON 可直接在 MC 端打开，反之亦然。
- 若模组已有「JS 绘制管线」类基础设施，可作为预览/兜底；但**可编辑性、命中检测与撤销栈仍建议由 Java 数据层驱动**，不要依赖网页运行时。

## 9.2 适合用原版 widget 的部分

> **项目约束**：`docs/EDITOR_MODULE_CONTRACTS.md` 已决定 MC 编辑器控件**全部自绘**（浅色主题、复刻网页观感，不用原版 `ButtonWidget`）。因此下表更适合理解为「可以借用原版控件命中/文本/滚动逻辑的内部实现」，外观需自绘；若项目允许原版皮肤，则可直接使用。

| Web 控件 | 建议 MC 实现 | 注意点 |
|---|---|---|
| 普通按钮 `.btn`、工具条按钮、行按钮、危险按钮 | `Button.builder(Component, onPress).bounds(x,y,w,h).build()` | 危险按钮可用红色文字/自定义渲染；原版无「白底描边」默认皮肤，需要自定义按钮背景或贴图 |
| 复选框（锁定宽高比、自动内边距、线路名下沉、自动配色） | 目标版本有 `Checkbox` 就用；没有则用 `Button` + 开/关两张状态贴图或文字「已开启/已关闭」 | Web 的复选框是 15×15 + 主色 accent；MC 里做到「一眼可见状态」即可 |
| 文本输入（站名、英文名、编号、大文本、线路号） | `EditBox` + `Formatter`/`Filter`（线路号只留 0–9、限 2 位；行高/宽度限数字） | 需要实现「输入即生效」「失焦结束撤销合并」（Web 是 input 事件 + blur break，见 6.9） |
| 数字输入（行高、宽度、灰框、内边距、空白宽度） | `EditBox` + 数字校验 + 范围 clamp（行高 32–2048、宽 32–32768、灰框 0–512、左右 padding 0–8、上下 0–1） | Web 是 `change` 提交，不是每次按键；MC 建议失焦/回车提交，避免刷屏 |
| 四向枚举（宽度模式固定/动态、元素对齐、文字对齐、内容对齐、出口对齐） | `CycleButton`（点击循环）或自定义下拉弹层 | Web 是分段按钮/下拉；MC 原版没有下拉框，`CycleButton` 最接近且省事 |
| 滑条（箭头粗细比例 0.1–0.5 step 0.05） | `AbstractSliderButton` + 显示百分比 | 原版滑条默认 5% 步进；需要 `setValue`/`value` 换算并显示「25%」 |
| tooltip（按钮说明） | `setTooltip(Tooltip.create(...))` | 文案可照抄本规格 3.2.1/2.11 的 title |
| 确认框（删行、清空、删预设） | 原版 `ConfirmScreen`（是/否）或自定义 Screen | Web 的确定按钮是红色 |
| 输入框对话框（保存预设命名） | 自定义 Screen：一个 EditBox + 确定/取消 | Web 是 `promptDialog` |
| Toast 提示 | 原版 `Toast`（系统右上角）或自定义叠加文字（底部/顶部） | Web 是右上角自绘、2400/4200ms、可堆叠；原版 Toast 也会堆叠但样式不同 |
| 滚动区域 | 原版**没有**通用滚动容器。建议：右侧属性栏用 `AbstractSelectionList` 风格（行式）+ 分组；或自绘 ScrollPanel | 自绘需要：`mouseScrolled` 滚轮、`scissor`/`enableScissor` 裁剪、滚动条绘制、拖动滚动条 |
| 城市/线路色下拉 | `CycleButton` 或自定义弹出的色板列表 | 内置 4 城市 + 自定义色板（见附录 C、7.4） |

## 9.3 必须自绘的部分

1. **牌面画布**：整块标识牌（背景/行背景/所有元素/灰框）——用 `GuiGraphics` 或离屏渲染，见 9.4。
2. 选中虚线框（accent 2.5px 虚线段 7/4、半透明浅底）与 hover 描边 —— 用短矩形段拼虚线，按布局坐标绘制。
3. 行带/行拖拽手柄「≡」/行内按钮（⭐/✕）——悬浮出现、拖拽排序、目标行高亮（半透明 + 虚线框）。
4. 元素拖拽幽灵（透明度 0.65 + 阴影效果）与竖直/水平插入指示线（3px 虚线）。
5. 浮动元素工具条（含内边距 ▲▼ 步进、拖拽调值图标、对齐下拉）——建议自绘面板 + 原版按钮。
6. 颜色选择器：当前色块、HEX 输入、**原生取色器在 MC 不存在** → 要么只提供色板 + HEX 输入（推荐先做这个），要么自绘 HSV 选择器。
7. 图标选择网格（3 列正方格 + 选中主色框）：每格要缩放绘制对应 SVG 路径（需先把 `icons.js` 的路径转成 Java 可绘制的形状或烘焙成纹理）。
8. 预设卡片/元素卡片缩略图：把一行元素渲染成小尺寸预览（可复用离屏渲染）。
9. 缩放徽标、行芯片（移动端布局在 MC 里可省略，除非做触屏/窄窗口适配）。
10. 拖拽排序、内边距横向拖拽（8px=0.05 的档位感）等自定义指针交互。

## 9.4 牌面渲染实现建议

两个可行方案（推荐 B，必要时 A）。`docs/EDITOR_MODULE_CONTRACTS.md` 的 layout-render 模块（`CRTSignLayoutPainter.bake` + `CRTSignLayoutPreview`）采用的正是「烘焙纹理 + 预览叠加层」方案，与本建议 B 一致；实现细节以模块契约为准。

**方案 A：直接按几何绘制**
- 用 `PoseStack` 把「牌面设计像素」映射到 GUI 坐标：`scale = displayW / layout.width`；Y 同理（等比）。然后所有坐标用 float 直接画。
- 文字：MC 原版 `Font` 是位图字体、度量与 Canvas 不同，**不能直接满足 §5.8 的墨区模型**；必须用自定义文本渲染（见 9.5）。
- 优点：缩放任意、无纹理尺寸限制；缺点：文字渲染要自己实现（或依赖可用的 TTF 字体 provider 并接受近似）。

**方案 B：离屏烘焙成纹理再贴图（推荐）**
- 按牌面设计像素（`layout.width × layout.height`，1:1）把牌面渲染到 `NativeImage`/FBO，再缩放到屏幕区域绘制（正好等价 Web 的 PNG 导出能力，`exporters.js:205-236`）。
- 优点：文字/图形一次烘焙、还原度高、性能好（每帧只画一个 quad）；与 Web「导出 PNG」像素级一致。
- 注意：FBO/纹理尺寸上限（通常 16384）；牌面最大 32768 宽、行数多时高度可能超限 → 需要分块（tile）或对超大牌面退回方案 A/限制编辑尺寸。
- 编辑交互（拖动/选中/输入）在纹理之上用 GUI 坐标单独绘制覆盖层，命中检测仍用 layout 坐标换算。

**精度约定（两种方案都要遵守）**：
- 元素水平平移 `floor(x)`；元素背景/命中框宽 `ceil(width)`（`render.js:820-825,879`）——用于避免同色背景之间的亚像素缝。
- 测量宽度/上伸/下伸统一 `ceil`（`core.js:342-382`）；`ink` 的 abl/abr 用原始浮点。
- 绘制顺序：牌面背景 → 每行：行背景 → 元素（背景、内容、命中框）→ 该行灰框（`render.js:855-897`）。
- 灰框的共享边界半宽规则见 4.8.4，不能简单每行画 4 条整宽边（会双倍粗）。
- 选中框/手柄不属于牌面，不要烘焙进纹理。

## 9.5 文本与字体度量（最大难点）

**问题**：Web 的布局宽度、字号、基线、数字间距全部由 Canvas 实测字体墨区决定（§5.8）。MC 没有 Canvas，原版 `Font` 也没有 `actualBoundingBox*` 概念；直接拿原版字体的 `width()` 当 `measure` 会得到完全不同的排版。

**建议做法（按优先级）**：
1. **Java 端字体度量器**：用 `java.awt.Font`（`Font.createFont(TRUETYPE_FONT, ttf)` + `FontRenderContext(new AffineTransform(), true, true)`，即开启分数度量）：
   - 前进宽：`font.getStringBounds` / `GlyphVector.getGlyphPosition`（或 `FontMetrics.stringWidth` 仅在整数近似时）；
   - 墨区：`GlyphVector.getGlyphOutline(i).getBounds2D()` 或 `getGlyphMetrics` 的 `getBounds2D` → 换算 `abl = inkLeft − 原点`、`abr = inkRight − 原点`；
   - 上伸/下伸：墨区边界相对基线的上下值。
   - 与 Web 的 Canvas（Skia/FreeType）在关闭 hinting、开启分数度量时通常只差亚像素；只要**所有布局都用同一套度量器**，内部就自洽。
2. **烘焙度量表**：如果 PUA/字体内嵌不方便，可用一次性脚本在无头浏览器里跑 `SignCore.createCanvasMeasurer()` 对所需字符集导出 JSON（数字 0–9、常用标点、以及项目里出现的全部 CJK 字符）；运行时查表插值。CJK 是任意输入，不能全量预烘焙 → 需要回退规则（例如：CJK 前进宽=1em、墨区左右约 0.04em、上伸/下伸取字体全局值）。
3. **允许近似但必须自洽**：如果只要求「看起来像」而非像素级对齐，可统一用近似度量（CJK 1em、数字用固定 advance 表），但所有依赖墨区的公式（5.7.4/5.7.5/5.7.7 的数字间距、光学居中）要改用近似值后重新检查间距。

**字体资源**：网页使用 MiSans（中英 400/600/700）与 Frutiger（数字 400）。MC 端可把同一批 TTF 放进资源包，用 `type: "ttf"` 的字体 provider 注册；但字体 provider 的 advance/基线由 MC 定义，**与 §5.8 公式不保证一致**，因此若做像素级复刻，建议把文字作为「烘焙纹理」渲染（方案 B）或实现自定义 glyph 渲染，而不是直接用原版 `drawString`。
**基线公式**（照抄）：`baselineY = 内容顶 + 0.036×s + ascent(text)`；双语英文 `baselineY = 中文基线 + 中文descent + 0.123×s + 英文ascent`（`render.js:145-150`）。

## 9.6 数据与命令层建议

- 完整照搬 `state.js` 的函数语义（纯函数 + 不可变更新）；MC 端可用「深拷贝快照 + 提交」实现同样效果，注意：
  - 每次变更后执行 `applyPaddingAuto`（4.5.2），并按「新旧 JSON 相等则丢弃」的规则不产生撤销步；
  - 撤销栈 20 步、快照含选择；文本连续输入 800ms 合并（或 MC 里改为「同一输入框未失焦期间合并」）；内边距拖动/滑条拖动合并为一整步；
  - `updateSignSettings` 的宽高比联动规则（4.7）与 `setSignWidthUnits` 的「1–15 格 × 256 且关闭比例锁」；
  - 反序列化清洗规则（4.9）必须实现，否则手改的 JSON 可能让渲染崩溃（源码注释明确这是崩溃点，`state.js:598-653`）。
- 建议把「编辑命令」与 UI 解耦：`addRow/deleteRow/moveRow/addElement/deleteElement/moveElements/moveElementsBy/updateElementProps/updateElementsProps/deleteElements/pasteElements/updateSignSettings/...` 一一对应，便于写单元测试。
- 导出/导入项目 JSON 的文件位置建议：`config/nanbin/signs/` 或 `.minecraft/screenshots/` 目录；MC 没有系统文件选择框，可用自定义文件列表 Screen 或让用户在 EditBox 粘贴路径。剪贴板用 `GLFW.glfwGetClipboardString/glfwSetClipboardString` 交换同一 JSON。
- 预设/色板可复用同样 JSON 结构存到 `config/nanbin/…json`（Web 用 localStorage，MC 用文件）。

## 9.7 交互实现建议（无 DOM 拖拽）

- **统一指针模型**：Web 用 Pointer Events 同时服务鼠标/触屏；MC 端用 `mouseClicked/mouseDragged/mouseReleased`（+ 触屏/手柄事件按需）。所有坐标先换算成「牌面设计坐标」，再做与 Web 相同的判定（`toSignPoint` 换算，`interact.js:392-410`）。
- **点击 vs 拖拽阈值**：按下后移动 `|dx|+|dy| ≥ 5px`（换算到 GUI 像素，按显示缩放折算）才启动拖拽；否则抬起算点击（6.5 步 1–2）。
- **选中规则**：普通点击单选；Shift 点击范围（视觉序 = 行顺序 + 行内 x 升序）；Ctrl 点击加/减选；点击空白取消；Esc 取消。MC 的键盘焦点在 EditBox 内时不要拦截这些键（6.14）。
- **拖拽落点**：照抄 `dropTargetAt`（6.5 步 5）与 `moveElements` 的 gap 语义（含「目标行被移走元素数」修正）；贴右通道数组序与视觉序相反这一点最容易做错。
- **行拖拽**：用 `≡` 手柄，目标边界 = 指针所在行上半/下半（6.8）；也可简化为上下箭头按钮（移动端方案），但桌面建议复刻拖拽手感。
- **内边距拖拽**：累计水平位移每 8 屏幕像素 = 0.05（按显示缩放折算），拖动期间整段合并成一步撤销；点击触发钮进入文本输入（6.7）。
- **浮动工具条**：选中 1 个元素时出现在选中框下方（放不下则上方），内容 = 左/右内边距（值 + 拖拽 + ±0.05 步进）、←/→ 移动、删除、3 个对齐循环控件；多选时右栏批量面板显示对齐/背景/删除。
- **撤销/重做/复制/粘贴/全选/删除/取消选择**：按钮 + 快捷键都要提供；复制的 JSON 与 Web 兼容（7.2）。
- **文本编辑**：MC 的 EditBox 与 Web input 行为差异较大；建议「点字段 → 打开输入弹层或就地 EditBox → 回车/失焦提交」。注意 Web 文本输入是「输入即更新 + 800ms 合并撤销」。

## 9.8 尺寸、缩放与布局注意

- **单位**：牌面内部坐标 = 设计像素（与 Web 完全一致）；GUI 像素是另一个域。渲染时用 `scale = min(可视宽/牌面宽, 可视高/牌面高, 1) × zoom` 映射（Web 的 fitSignDisplay 不放大，`ui.js:118-135`）。
- **三栏宽度**：Web 固定 260px + 中栏自适应；MC GUI 坐标随 GUI Scale 与窗口分辨率变化（例如 1920×1080、GUI Scale 2 → 960×540 逻辑像素）。建议：左右栏按「固定 GUI 像素」或「窗口百分比」二选一（推荐 220–260 GUI px + 窄窗口自动收窄），中栏占剩余空间；Web 的面板收起/展开把手也可以保留为按钮。
- **画布滚动**：牌面大于可用区域时需要滚动查看（自绘 ScrollPanel，滚轮 + 拖动滚动条 + 可选中后自动滚动到可见）。
- **缩放范围**：Web 0.5–4（`interact.js:873`）；MC 里可用 `mouseScrolled` 实现滚轮缩放（这是 Web 没有的能力，属增强），并提供「复位到适应」按钮。
- **高 DPI**：离屏烘焙方案要按 GUI scale 取整避免糊；若直接绘制，注意 `PoseStack.scale` 后线宽（虚线、描边）要按屏幕像素补偿，避免被缩放得过粗/过细（Web 的选中框用了 `vector-effect:non-scaling-stroke` 等价于屏幕像素宽）。
- **灰框**：注意 4.8.4 的半宽规则与「画在所有元素之上」的叠放顺序（否则越界内容会盖住边框）。

## 9.9 复刻验收对照清单（建议逐条测试）

1. 新建默认牌：2048×192、1 行、白底、无灰框；行高改 192→288 时若锁宽高比，宽度变 3072。
2. 三栏布局与面板头/收起展开、空状态提示、添加行按钮位置。
3. 十种元素各自默认渲染结果（宽度/字号/基线/颜色），特别是：数字线路（色条 10/28 内容高、数字墨区间距、「号线/Line」标签位置）、文本线路 sink/flat 两模式、出口固定黄底、space 宽度 = ratio×行高。
4. 固定模式三通道（左/中/右可重叠）与动态模式（宽度=最宽行、全部左起）。
5. 内边距比例换算：行高 192 + padding 0.2 → 内容区 115.2；元素宽度 = 内容宽 + 左右 padding 像素。
6. paddingAuto：相邻同类元素（同通道）面向侧 0.1、外侧 0.2；手动改左/右后钉住不再自动。
7. 灰框宽度 8 时首行上缘/末行下缘 8px、行间分割线 4+4px，且覆盖越界内容。
8. 选中：单选/Ctrl 加减选/Shift 范围/全选/空白取消/Esc；多选进批量面板。
9. 拖拽：行内排序、跨行移动（含贴右通道的视觉序反转）、5px 阈值、幽灵与指示线、放下后撤销一步。
10. 内边距拖动：8px=0.05、整段一步撤销、0–8 clamp。
11. 行：添加、手柄拖动排序、删除确认/空行直删、只剩一行时不可删、存为预设/应用到空行/添加为新行。
12. 撤销/重做 20 步、文本输入 800ms 合并、恢复选择。
13. 复制/剪切/粘贴 JSON 与 Web 一致；粘贴落点（选中之后 / 剪切位置 / 末行）；跨行剪贴板偏移。
14. 项目 JSON 导入/导出往返一致（含清洗：非法颜色/越界数值/未知类型/重复 id）。
15. 导出 SVG/PNG 或烘焙纹理与布局一致（无编辑器装饰）。
16. 移动端/窄窗口（若做）：行条、属性带折叠、触控目标放大、缩放徽标。

---

# 附录 A：源码文件与行号索引

| 文件 | 行数 | 职责 | 本规格主要引用章节 |
|---|---|---|---|
| `index.html` | 98 | 页面骨架/DOM 挂载点/脚本加载顺序 | 1.2、2、3 |
| `style.css` | 1396 | 全部编辑器样式、三栏布局、响应式、深色模式 | 2 |
| `fonts.css` | 36 | MiSans / Frutiger 的 `@font-face` 声明 | 5.8、9.5 |
| `js/core.js` | 420 | 常量（EXIT_COLOR、SIGN_GRID_SIZE=256、DEFAULT_ROW_HEIGHT=192、颜色表、字体栈）、uuid/clamp/hex/luminance、城市色板派生、Canvas 文本测量器 | 1.3、4.1、4.8、5.8、附录 C |
| `js/palettes.js` | 41 | 自定义色板数据（localStorage、导入导出、跨标签同步） | 7.4 |
| `js/palette-manager.js` | 80 | 自定义色板管理对话框 | 2.8、7.4 |
| `js/icons.js` | 47 | 32 个图标数据（name/cat/viewBox/SVG body） | 5.9 |
| `js/state.js` | 758 | SignState/Row/Element 模型、默认属性、全部纯函数编辑操作、序列化/清洗 | 4、6、7 |
| `js/render.js` | 940 | 布局（layoutSign/layoutRowElements）、10 种元素 metrics + draw、灰框、整牌渲染 | 5、7.6 |
| `js/ui.js` | 498 | 全局 App 上下文（update/undo/renderAll/fitSignDisplay/selection）、DOM 工具、内边距拖拽、toast/对话框 | 1.3、6.4、6.9、6.12、6.16 |
| `js/clipboard.js` | 121 | 复制/剪切/粘贴（原生事件 + 按钮 + 页面内副本） | 6.10、7.2 |
| `js/presets.js` | 95 | 预设系统（localStorage） | 3.1.3、7.3 |
| `js/storage.js` | 219 | 自动保存、多实例守卫、偏好、项目 JSON 导入导出 | 6.15、7.1、7.5 |
| `js/exporters.js` | 263 | SVG/PNG 导出、字体内嵌、光栅化 | 7.6、7.7 |
| `js/color-picker.js` | 68 | 颜色选择器组件（HEX/原生色板/城市色板/透明） | 2.8 |
| `js/panel.js` | 1407 | 左栏元素/预设 + 右栏设置/元素/批量/预设预览面板 + 控件工厂 | 3.1、3.3 |
| `js/interact.js` | 1075 | 覆盖层、选择、元素拖拽、行拖拽、HTML5 DnD 接收、触屏手势、浮动工具条、行管理 | 2.10、6.1–6.8、6.12–6.14 |
| `js/main.js` | 212 | 启动装配（守卫/字体/恢复/断点监听/自动保存刷盘） | 1.3、6.15 |

# 附录 B：常量速查表

| 常量 | 值 | 定义处 | 用途 |
|---|---|---|---|
| `SVG_NS` | `http://www.w3.org/2000/svg` | `core.js:12` | SVG 命名空间 |
| `EXIT_COLOR` | `#F7D917` | `core.js:16` | 出口黄（默认背景/通用色板「出口黄」） |
| `SUBURBAN_COLOR` | `#026AA7` | `core.js:17` | 上海「机场线」色 |
| `GREY_COLOR` | `#424A52` | `core.js:18` | 通用灰；数字线路非法颜色回退 |
| `SIGN_FRAME_COLOR` | `#4A4A4A` | `core.js:19` | 新牌默认灰框色 |
| `LEGACY_SIGN_FRAME_COLOR` | `#808080` | `core.js:20` | 旧项目缺灰框色回退 |
| `SIGN_GRID_SIZE` | `256` | `core.js:21` | 一格贴图宽（px） |
| `DEFAULT_ROW_HEIGHT` | `192` | `core.js:22` | 默认行高（0.75 格） |
| `SCHEMA_VERSION` | `1` | `state.js:488` | 项目 JSON 版本 |
| `ELEMENT_TYPES` | 10 种（见 4.3） | `state.js:20-23` | 元素类型白名单 |
| `CONTENT_ALIGN_TYPES` | `number-line, text-line, exit` | `state.js:26` | 内容对齐支持类型 |
| `ICON_ROTATIONS` | `-90, 0, 90, 180` | `state.js:29` | 图标可旋转角 |
| `PADDING_MAX` | `{top:1,right:8,bottom:1,left:8}` | `state.js:499` | padding 上限 |
| `PADDING_AUTO_DEFAULT` | `0.2` | `state.js:529` | 无邻居自动侧 |
| `PADDING_AUTO_ADJACENT` | `0.1` | `state.js:530` | 有邻居自动侧 |
| `ARROW_DIRECTIONS` | 8 向 | `state.js:579-581` | 箭头方向枚举 |
| `ZH_FONT_RATIO` | `506/800 = 0.6325` | `render.js:23` | 双语中文字号/内容尺寸 |
| `EN_FONT_RATIO` | `285/800 = 0.35625` | `render.js:24` | 双语英文字号/内容尺寸 |
| `NUM_FONT_RATIO` | `1.3` | `render.js:25` | 大文本/编号字号/内容尺寸 |
| `STRIPE_RATIO` | `10/28 ≈ 0.357143` | `render.js:26` | 线路色条宽/内容尺寸 |
| `LABEL_BOX_RATIO` | `0.75` | `render.js:27` | 组合标签内容盒（「号线」/「线」/「出入口」/「出口」/小号双语） |
| `EN_ALIGN_OFFSET` | `1/30` | `render.js:28` | 左右对齐时英文额外缩进 × 内容尺寸 |
| `INK_TOP_LEADING` | `0.036` | `render.js:32` | 文本墨区顶到内容顶 |
| `ZH_EN_GAP` | `0.123` | `render.js:37` | 中文墨底到英文墨顶 |
| `CJK_FACE/CJK_ASC/CJK_DESC` | `0.92/0.84/0.08` | `render.js:34-36` | 字体解剖参考值（当前基线由实测推导） |
| `EN_ASC/EN_CAP/EN_DESC` | `0.74/0.72/0.21` | `render.js:38-40` | 英文解剖参考值 |
| `NUM_ASC` | `0.70` | `render.js:41` | 数字解剖参考值 |
| `HISTORY_LIMIT` | `20` | `ui.js:18` | 撤销/重做最大步数 |
| 撤销合并窗口 | `800ms` | `ui.js:76` | 同 key 连续更新合并 |
| 自动保存 debounce | `500ms` | `storage.js:48-57` | localStorage 写入 |
| 实例心跳/新鲜窗口 | `2000ms / 5000ms` | `storage.js:13-14` | 多实例守卫 |
| 元素拖拽阈值 | `5px`（|dx|+|dy|） | `interact.js:627` | 激活拖拽 |
| 内边距拖动 | `8px = 0.05` | `ui.js:384-388` | scrub 档位 |
| `ZOOM_MIN/ZOOM_MAX` | `0.5 / 4` | `interact.js:873` | 画布缩放范围 |
| `TAP_MS/TAP_SLOP/PAN_SLOP` | `300 / 30 / 10` | `interact.js:874` | 触屏双击/滑动判定 |
| 剪贴板 | kind `jr-sign-elements`、version 1、rows ≤1000、offset 0–999、元素 ≤10000、文本 ≤8MB | `state.js:397-417`、`clipboard.js:30` | 元素剪贴板 |
| 预设存储键 | `public-sign-presets` | `presets.js:14` | localStorage |
| 自定义色板键 | `public-sign-custom-palettes` | `palettes.js:5` | localStorage |
| 自动保存/实例/偏好键 | `public-sign-autosave` / `public-sign-instance` / `public-sign-prefs` | `storage.js:10-12` | localStorage |
| 移动端断点 | `(max-width: 768px)` | `ui.js:12-14`、`style.css:1036` | 布局切换 |
| 面板宽 | `260px` | `style.css:99` | 左右栏 |
| 画布显示上限 | `min(area.clientWidth, 1600)` | `ui.js:125` | fitSignDisplay |

# 附录 C：城市线路配色完整数据（`core.js:29-198`）

## C.1 城市与顺序
- `CITY_PALETTES`：`shanghai`（上海）、`chongqing`（重庆）、`chengdu`（成都）、`beijing`（北京地铁）；顺序 `PALETTE_CITY_ORDER = ['shanghai','chongqing','chengdu','beijing']`；`DEFAULT_CITY = 'shanghai'`（`core.js:191-198`）。
- 每城市 `{name, lines, extras[, universal]}`；`universal` 缺省用 `UNIVERSAL_SWATCHES`。

## C.2 上海（23 条数字线路 + 7 个附加色）
| 线路 | 背景色 | 前景色（数据中存在，编辑器不使用） |
|---|---|---|
| 1 | `#E3002B` | `#FFFFFF` |
| 2 | `#82BF25` | `#000000` |
| 3 | `#FCD600` | `#000000` |
| 4 | `#461D84` | `#FFFFFF` |
| 5 | `#944D9A` | `#FFFFFF` |
| 6 | `#D40068` | `#FFFFFF` |
| 7 | `#ED6F00` | `#000000` |
| 8 | `#0094D8` | `#FFFFFF` |
| 9 | `#87CAED` | `#000000` |
| 10 | `#C6AFD4` | `#000000` |
| 11 | `#871C2B` | `#FFFFFF` |
| 12 | `#007A60` | `#FFFFFF` |
| 13 | `#E999C0` | `#000000` |
| 14 | `#626020` | `#FFFFFF` |
| 15 | `#BCA886` | `#000000` |
| 16 | `#98D1C0` | `#000000` |
| 17 | `#BC796F` | `#FFFFFF` |
| 18 | `#C4984F` | `#000000` |
| 19 | `#F5AB78` | `#000000` |
| 20 | `#009F65` | `#FFFFFF` |
| 21 | `#F7AF00` | `#000000` |
| 22 | `#5F376F` | `#FFFFFF` |
| 23 | `#B0D478` | `#000000` |

附加色（extras）：机场线 `#026AA7`、嘉闵线 `#724A57`、南汇线 `#93ABBB`、示范区线 `#6FB16B`、南枫线 `#4C6DA9`、奉贤线 `#F6AE54`、浦江线 `#B5B5B6`。

## C.3 重庆（29 条数字线路 + 1 个附加色）
| 线路 | 背景色 | 备注 |
|---|---|---|
| 1 | `#E4002B` | 数字线路元素默认色 |
| 2 | `#007A33` | |
| 3 | `#003DA5` | |
| 4 | `#DC8633` | |
| 5 | `#00A3E0` | |
| 6 | `#F67599` | |
| 7 | `#008C95` | |
| 8 | `#7A9A01` | |
| 9 | `#862041` | |
| 10 | `#5F249F` | |
| 11 | `#D986BA` | |
| 12 | `#D2D755` | |
| 13 | `#B89D18` | |
| 14 | `#B94700` | |
| 15 | `#0057B7` | 名称「潘通2935C蓝」 |
| 16 | `#B04A5A` | |
| 17 | `#9F5CC0` | |
| 18 | `#2CD5C4` | |
| 19–20、26–28 | `#0057B7` | 与 15 同色，色板按颜色去重合并（显示为「潘通2935C蓝」） |
| 21 | `#309B42` | |
| 22 | `#2F6F7A` | |
| 23 | `#93C90F` | |
| 24 | `#D7A048` | |
| 25 | `#8A75D1` | |
| 29 | `#FF585D` | |

附加色：环线 `#F2A900`（文本线路元素默认 `blockColor`）。

## C.4 成都（30 条数字线路 + 19 个附加色）
| 线路 | 背景色 | 线路 | 背景色 |
|---|---|---|---|
| 1 | `#222A8C` | 13 | `#C5A900` |
| 2 | `#EB5A35` | 14 | `#6F263D` |
| 3 | `#D5006A` | 15 | `#D958A0` |
| 4 | `#00AA58` | 16 | `#0085CA` |
| 5 | `#A23E92` | 17 | `#80E0AA` |
| 6 | `#BE7331` | 18 | `#006268` |
| 7 | `#6DC6D6` | 19 | `#89ABE3` |
| 8 | `#A6C214` | 20 | `#B86125` |
| 9 | `#F1AD17` | 21 | `#FFC27B` |
| 10 | `#0050A3` | 22 | `#6AD1E3` |
| 11 | `#8C7732` | 23 | `#DFA0C9` |
| 12 | `#772583` | 26 | `#43B02A` |
| — | — | 27 | `#00A3E0` |
| — | — | 28 | `#9678D3` |
| — | — | 29 | `#7C78FF` |
| — | — | 30 | `#F67599` |
| — | — | 32 | `#003DA5` |
| — | — | 33 | `#8EDD65` |

（无 24/25/31 号线，源码注释如此。）附加色：S1线/新都金堂线 `#A4D65E`、S2线/金堂空港线 `#C98BDB`、S3线/资阳东线 `#75787B`、S4线/龙泉驿淮州线 `#97D700`、S5线/眉山仁寿线 `#5C88DA`、S6线/新津蒲江线 `#A05088`、S7线/新津南线 `#00CC78`、S8线/新津安仁线 `#007CCC`、S9线/温江都江堰线 `#539A58`、S10线/新都广汉线 `#C0FA08`、S11线/德阳罗江线 `#737B4C`、S12线/成华旌阳线 `#0050A0`、S13线/龙泉天府机场线 `#C55C64`、S14线/天府简阳线 `#B8B800`、S15线/东南环线 `#0080FF`、S16线/眉山青神线 `#FF8800`、S17线/资阳西线 `#059370`、S18线/新津北线 `#00BAF8`、蓉2号线 `#7F9F3A`。

## C.5 北京（13 条数字线路 + 6 个附加色）
| 线路 | 背景色 | 显示名 |
|---|---|---|
| 1 | `#A4343A` | 1号线/八通线 |
| 2 | `#004B87` | 2号线 |
| 4 | `#008C95` | 4号线/大兴线 |
| 5 | `#AA0061` | 5号线 |
| 6 | `#B58500` | 6号线 |
| 7 | `#FFC56E` | 7号线 |
| 8 | `#009B77` | 8号线 |
| 9 | `#97D700` | 9号线 |
| 10 | `#0092BC` | 10号线 |
| 13 | `#F4DA40` | 13号线 |
| 14 | `#CA9A8E` | 14号线 |
| 15 | `#653279` | 15号线 |
| 16 | `#6BA539` | 16号线 |

附加色：昌平线 `#D986BA`、房山线/燕房线 `#D86018`、机场线 `#A192B2`、西郊线 `#D22630`、亦庄线 `#D0006F`、S1线 `#A45A2A`。

## C.6 通用色与派生规则
- `UNIVERSAL_SWATCHES`（追加到每个城市色板末尾，不随城市变化）：出口黄 `#F7D917`、灰 `#424A52`、黑 `#000000`、白 `#FFFFFF`（`core.js:184-189`）。
- 选色板生成 `citySwatches(city)`（`core.js:302-315`）：先数字线路（name = `c.name || 线路号+'号线'`），再城市 extras（name），最后 universal；**按背景色去重，重复色并入首见色块**（重庆 15/19/20/26–28 即此规则）。
- 自动配色 `lineColorFor(numberStr, city)`（`core.js:285-295`）匹配规则（依次）：
  1. `c.line === 输入`；
  2. `c.line + '号线' === 输入`；
  3. `c.name` 按 `/` 拆分后与输入相等；
  4. 输入为纯数字且 `name === 输入 + '号线'`。
  自定义色板（§7.4）没有 `lines`，只有 `extras`（即第 3/4 条规则可命中）。
- 颜色全大写规范化（`normalizeHex`，`core.js:250-258`）；`isHexColor` 支持 `#RGB` 与 `#RRGGBB`（`core.js:244-248`）。

---

*（本规格由源码逐行抽取整理；如与源码冲突，以源码为准。）*

