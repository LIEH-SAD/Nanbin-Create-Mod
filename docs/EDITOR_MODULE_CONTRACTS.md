# CRT 导视牌编辑器：模块契约（供并行开发）

目标：把网页项目 CRT-Sign-Web-Editor 的导视牌编辑器 → Minecraft 屏幕（浅色现代 UI）。
功能规格见 [WEB_EDITOR_SPEC.md](WEB_EDITOR_SPEC.md)；MTR 数据联动见 [MTR_DATA_BINDING.md](MTR_DATA_BINDING.md)（若尚未生成，以实现时的代码为准）。

## 已就绪的数据层（Lead 已提交，勿改）

包 `com.Nanbin.signage.crt`（公共端，服务端可用）：

- `CRTSignLayout`：整牌布局（widthMode/width/rowHeight/aspectLocked/backgroundColor/frameWidth/frameColor/rows），
  `toJson()` / `fromJson(String)`、`createBlank(cellCount)`、行/元素增删移动、`findElement`、`findElement` 等等。
- `CRTSignLayout.Row`：`id`、`backgroundColor`（可空）、`elements`。
- `CRTSignElement`：`id/type/color/backgroundColor/elementAlign/padding*/paddingAuto/textZh/textEn/text/bold/align/
  arrowDirection/thicknessRatio/lines/textColor/nameSink/blockColor/code/widthRatio/icon/rotation`，
  以及 MTR 绑定 `binding`（`CRTSignElement.DataBinding`）与 `bindingIds`。
  `applyDefaults(type)`、`copy()`、`toJson()`、`fromJson(JsonElement)`。
- `CRTSignElementType`：10 种元素，`id` 与网页一致。
- `CRTSignClipboardData`：元素剪贴板（JSON 与网页项目兼容）。
- `CRTSignHistory`：撤销/重做（JSON 快照）。
- `CRTSignDataContext`：MTR 数据访问接口（线路名/英文名/编号、车站名合并、出口字母、站台名、上一站/下一站）。

视觉常量：`com.Nanbin.client.Screen.CRTEditorTheme`（网页 style.css 的 :root 颜色/尺寸/字号，浅色主题）。
**不要改成暗色主题**。

方块实体已加 `getSignLayout()/setSignLayout(String)`（铁路牌 `BlockCRTRailwaySign.BlockEntityCRTRailwaySign`、
墙挂牌 `BlockCRTRailwaySignWall1.BlockEntity`），保存包：`PacketUpdateCRTSignLayout(BlockPos, String json)`。

## 模块 A：布局渲染（layout-render）

包 `com.Nanbin.client.Drawing`，新文件（可小幅修改 `JSDrawSink` / `JSSignTextureBaker` / `CustomFontTextureCache`，只加不改既有行为）：

1. `CRTSignTextMetrics`：用 java.awt.Font（`CustomFontTextureCache` 已加载的字体，必要时加 public 取字体方法）提供
   `advance(text, fontType, weightBold, sizePx)`、`ascent(...)`、`descent(...)`、`ink(...)`（对齐网页 core.js 的 Canvas 测量语义，
   不可得时用 0.75/0.2 倍字号回退）。
2. `CRTSignLayoutFrame`：一帧布局结果（设计像素坐标，原点左上）：
   - `public final float width, height;`、`List<RowFrame> rows;`
   - `RowFrame { CRTSignLayout.Row row; int rowIndex; float y, height, contentWidth; List<ElementFrame> elements; }`
   - `ElementFrame { CRTSignElement element; String rowId; int rowIndex, index; float x, y, width, height; }`
   - `ElementFrame findElement(String elementId)`、`ElementFrame hitTest(float px, float py)`、`int rowAt(float py)`。
3. `CRTSignLayoutPainter`：
   - `static CRTSignLayoutFrame layout(CRTSignLayout layout, CRTSignDataContext data)`
     （按 WEB_EDITOR_SPEC §5.1–5.7 的盒模型/三通道/各元素 metrics 公式，绑定数据替换字面文本）
   - `static void draw(JSDrawSink sink, CRTSignLayoutFrame frame, CRTSignDataContext data)`
     （sign-bg → 每行 row-bg → 元素盒背景 → 元素内容 → 灰框；全部画到 JSDrawSink；
      文字用 `FittedTextRenderer`/`CustomFontTextureCache` 生成 NativeImage 后 `sink.drawImage`，图标用 signs.json 贴图 `sink.drawTexture`，
      多边形（箭头/徽标）用新增的 `sink.fillPolygon(float[] xs, float[] ys, int color)`）
   - `static void drawElementStandalone(JSDrawSink sink, CRTSignElement element, float rowHeight, CRTSignDataContext data)`（左栏卡片缩略图）
   - `static JSSignTextureBaker.Baked bake(CRTSignLayout layout, CRTSignDataContext data, String cacheKey)`（世界渲染用；内部内容指纹用 layout JSON + frame 数据）
4. `CRTSignLayoutPreview`（编辑界面顶部预览，GUI 绘制）：
   ```java
   public static final class Transform { float x, y, width, height, scale; CRTSignLayoutFrame frame;
       float toScreenX(float layoutX); float toScreenY(float layoutY);
       float toLayoutX(float screenX); float toLayoutY(float screenY); }
   public static Transform draw(GraphicsHolder graphics, float x, float y, float width, float height,
       CRTSignLayout layout, CRTSignDataContext data, String cacheKey,
       @Nullable String selectedElementId, int hoverRowIndex, boolean drawRowChrome);
   ```
   预览 = 与世界渲染同一套 `bake` 结果贴图 + 选中框/行带等叠加层；返回 Transform 供屏幕做命中测试。
   同时提供 `static void drawBackground(GraphicsHolder g, float x, float y, float w, float h)` 画画布区底色。

要求：严格按 WEB_EDITOR_SPEC 的常量/公式；文字测量用 java.awt 字体实际墨区（不要等宽近似）；
箭头多边形按 §5.7.1 的点集；图标 id → 本 mod 贴图映射（`nanbin`/`crt_*` 标志与网页服务图标 id 的对照表自行建立，未知回退 `crt_railway`）。
编译验证：`.\gradlew.bat :fabric:1.20.1:compileJava --configure-on-demand --offline --console=plain`。

## 模块 B：编辑界面（editor-ui）

新文件（不碰 A 的文件、不碰已有 `CRTSignScreen`/`CRTSignDoubleScreen`）：
- `com.Nanbin.client.Screen.CRTSignEditorScreen`
- `com.Nanbin.client.Screen.CRTEditorWidgets`（自绘控件：按钮/图标按钮/输入框/滑条/颜色选择/下拉/复选/滚动区/tooltip，浅色主题）
- `<lang>` 仅追加 `gui.nanbin.crt_editor.*` 键

构造契约（Lead 的 wiring 会这样调用）：

```java
public CRTSignEditorScreen(@Nullable ScreenExtension parent, BlockPos signPos,
        CRTSignLayout layout, CRTSignDataContext data,
        CRTSignEditorHost host)
```

`CRTSignEditorHost`（Lead 提供，位于 `com.Nanbin.client.Screen`）：

```java
public interface CRTSignEditorHost {
    void save(CRTSignLayout layout);                    // 每次改动后调用（可节流），关闭时也会调用
    List<DashboardListItem> getRoutesForList();         // MTR 线路（DashboardListItem）
    List<DashboardListItem> getStationsForList();
    List<DashboardListItem> getExitsForList();
    List<DashboardListItem> getPlatformsForList();
}
```

界面要求（对齐 WEB_EDITOR_SPEC §2/§3/§6，浅色）：
- 三栏：左 260 元素选择区（分类卡片：方向图标 12、服务图标、双语文本、小号双语、大文本、数字线路、文本线路、出入口、出口、空白）；
  中：选择/剪贴板工具条 + 画布（`CRTSignLayoutPreview`）+ 行控件（添加行/行上移下移删除）+ 底部缩放信息；
  右 260：未选中→标识牌设置（宽格数 1–15、动态宽度、行高、锁定宽高比、背景色、灰框宽/色）；
  选中元素→元素属性（类型专属字段 + 颜色/背景/对齐/内边距/粗体/旋转…）；多选→批量属性。
- 交互：点击选中、Ctrl 多选、Shift 范围选择、拖拽元素换行/排序、←/→ 微调、Delete、Ctrl+A/C/X/V/Z/Y、Escape；
  行背景色；元素数据绑定按钮（线路/车站/出口/站台 → 打开 MTR `DashboardListSelectorScreen`）。
- MTR 联动：`data` 提供实时数据；属性面板"绑定"下拉可选 `CRTSignElement.DataBinding`，绑定后元素显示 MTR 实际内容。
- 所有改动走 `CRTSignHistory`（撤销重做）+ `CRTSignClipboardData`（复制粘贴），并调用 `host.save(layout)`。
- 控件全部自绘（`GuiDrawing` + 文字），不要用原版浅色 ButtonWidget；输入框可用自绘 + keyPressed/charTyped。
- 视觉：`CRTEditorTheme` 的颜色/圆角/间距；卡片 hover、选中蓝框、危险色删除按钮、空态提示。

要求：先交付可编译可运行的"能选中/能改属性/能增删元素/能保存"的最小闭环，再迭代拖拽与批量属性；每轮编译通过。
