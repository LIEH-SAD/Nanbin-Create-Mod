# Nanbin JS 指示牌脚本 API 文档

> 适用于 MTR 4.0.5 + Nanbin 模组的全局 JS 指示牌样式系统。
> 脚本运行在 **Nashorn 15.4** 引擎中（ES6 模式），支持 **部分 ES6** 语法（`const`/`let`、箭头函数、模板字符串、默认参数、`Map`/`Set` 等）。

## 〇、脚本语法支持范围（Nashorn ES6 模式）

为避免写脚本时踩坑，这里列出 Nashorn 15.4 在 `--language=es6` 模式下**支持/不支持**的常见语法与 API：

| 语法 / API | 支持 | 说明 |
|---|---|---|
| `var` / `function` | ✅ | ES5 传统写法，完全兼容 |
| `const` / `let` | ✅ | 块级作用域变量 |
| 箭头函数 `(a) => a*2` | ✅ | |
| 模板字符串 `` `a${1+2}b` `` | ✅ | |
| 默认参数 `(a = 5)` | ✅ | |
| `Map` / `Set` | ✅ | `new Map()`、`.get()`、`.set()` 等 |
| 数组/对象字面量、`Math`、`Date` | ✅ | 标准 ES5/ES6 常用功能 |
| `class` | ❌ | ES6 class 声明/表达式**未实现** |
| 解构 `{a,b} = obj` / `[a,b] = arr` | ❌ | |
| 展开运算符 `...args` | ❌ | |
| `async` / `await` | ❌ | |
| `String.prototype.includes` / `padStart` / `padEnd` | ❌ | ES2017 方法；可用 `indexOf` 等替代 |
| `Object.assign` | ❌ | 可用 `for...in` 手工拷贝 |
| `Number.EPSILON` | ❌ | |
| 其它 ES2017+ 方法（`startsWith`/`endsWith`/`repeat`/`trimStart` 等） | ❌ | 用 ES5 等价写法替代 |

> 提示：脚本先用 `var` + `function` 写最稳妥；需要字符串包含判断时用 `"abc".indexOf("b") >= 0` 代替 `includes`。

---

## 一、脚本注册与样式选择

1. 在 `assets/nanbin/js/` 下创建 `.js` 脚本文件
2. 在 `assets/nanbin/js_signs_config.json` 的 `scripts` 数组注册：

```json
{
  "id": "my_style",
  "path": "assets/nanbin/js/my_style.js",
  "icon": "nanbin:textures/block/sign/js_icon.png",
  "name": "js_sign.nanbin.my_style"
}
```

- `id`：脚本唯一标识，同时作为样式标记 `crt_js_style_<id>` 写入指示牌
- `icon`：选择器中显示的图标贴图
- `name`：语言键，在 `lang/zh_cn.json`、`lang/en_us.json` 中翻译为真实名称（如 `"js_sign.nanbin.my_style": "我的样式"`）

3. 游戏内：打开指示牌编辑屏幕 → 顶部"JS 样式"按钮 → 选择样式。
   选择后整个指示牌由脚本接管渲染，普通编辑被锁定。

---

## 二、脚本生命周期

每个脚本必须实现以下函数（均为可选，缺省跳过）：

```js
function create(ctx, state, sign) {
    // 首次渲染前调用一次，用于初始化
    state.lastText = "";
}

function render(ctx, state, sign) {
    // 每帧调用，在此绘制整个指示牌
}

function dispose(ctx, state, sign) {
    // 卸载/缓存清理时调用一次
}
```

- `ctx`：绘制上下文（见第三节）
- `state`：跨帧状态对象，随每个屏幕独立存储
- `sign`：指示牌数据接口（见第四节）

---

## 三、绘制 API（ctx）

坐标系与世界坐标一致：x ∈ [0, getWidth()]，y ∈ [0, getHeight()]。
`getWidth()`/`getHeight()` 返回指示牌**实际渲染宽高**（世界单位），
RailwaySign 高 0.5、宽 格数×0.5；StationInfo 双格高 0.3。

所有绘制对象采用链式 builder，最后 `.draw(ctx)` 提交：

### Text 文字

```js
Text.create()
    .pos(x, y)              // 左上角坐标
    .size(w, h)             // 文字框宽高（省略则铺满剩余区域）
    .text("内容")            // 支持 "中文|English" 双行换行
    .color(0xFFFFFF)        // 颜色，默认 0xFF000000
    .scale(1.0)             // 字号缩放，默认 1.0
    .bold()                 // 加粗（可选，无参）
    .bold(true)             // 或显式传布尔
    .centered()             // 水平+垂直居中（中英文混合按字形宽度对齐）
    .centered(true)         // 或显式传布尔
    .draw(ctx);
```

测量文本宽度（世界单位）：

```js
var width = Text.measure("文字", sign.getHeight()); // 按当前渲染规则计算自然宽度
```

### Rect 矩形（背景/色块）

```js
Rect.create()
    .pos(x, y)
    .size(w, h)
    .color(0xFFFFFF)
    .draw(ctx);
```

### Texture 贴图

```js
Texture.create()
    .texture("nanbin:textures/...")   // 资源路径
    .pos(x, y)
    .size(w, h)
    .color(0xFFFFFFFF)
    .draw(ctx);
```

### Line 线段

```js
Line.create()
    .from(x1, y1)
    .to(x2, y2)
    .width(0.05)            // 线宽（世界单位），默认 0.05
    .color(0xFFFFFF)
    .draw(ctx);
```

---

## 四、指示牌数据接口（sign）

### 4.1 尺寸与标识

| 接口 | 返回 | 说明 |
|------|------|------|
| `getWidth()` | float | 渲染宽度（世界单位）= 格数 × 单格尺寸 |
| `getHeight()` | float | 渲染高度（世界单位）= 单格尺寸 |
| `getCellCount()` | int | 格子数量（格数） |
| `getCellSize()` | float | 单格世界尺寸 |
| `getScreenKey()` | String | 当前屏幕唯一标识（含坐标/行/朝向），多屏同脚本时可区分 |
| `getScreenType()` | String | 屏幕类型：`platform` / `route` / `exit` / `station` / `custom` |

`getScreenType()` 依据本行首个非样式格子的槽位类型判断，是**一个脚本适配多种屏幕**的关键分流依据：

```js
var type = sign.getScreenType();
if (type === "route") { /* 线路屏 */ }
else if (type === "exit") { /* 出口屏 */ }
else if (type === "platform") { /* 站台屏 */ }
else if (type === "station") { /* 车站屏 */ }
else if (type === "custom") { /* 自定义文本屏 */ }
```

### 4.2 站台接口（platform）

| 接口 | 返回 | 说明 |
|------|------|------|
| `getPlatformCount()` | int | 选中站台数量 |
| `getPlatformNumbers()` | String[] | 站台编号列表（如 `["1","2"]`，按 ID 排序） |

### 4.3 车站接口（station）

| 接口 | 返回 | 说明 |
|------|------|------|
| `getSelectedStationIds()` | long[] | 选中的站点/站台 ID |
| `getStationNames()` | String | 站名合并串（多站用 `/` 分隔） |
| `getStationName(platformId)` | String | 指定站台/站点 ID 的名称 |

### 4.4 线路接口（route）

| 接口 | 返回 | 说明 |
|------|------|------|
| `getSelectedColors()` | long[] | 选中线路的真实颜色（ARGB），含换乘站，有序 |
| `getRouteColor(routeIndex)` | int | 第 index 条线路颜色（`getSelectedColors()[index]`） |
| `getRouteName(color)` | String | 线路中文名（如 "1号线"），未选中返回空串 |
| `getRouteName(color, true)` | String | 线路英文名（如 "Line 1"） |
| `getRouteNumber(key)` | String | 线路编号（优先服务端编号映射，回退从名称提取数字） |
| `getRouteDestination(color)` | String | 线路终点站名 |
| `isLoopRoute(color)` | boolean | 是否环线 |
| `getRouteNumbers()` | String[] | 本行所有格子的线路编号（渲染器传入） |

典型用法（线路色条 + 编号）：

```js
var colors = sign.getSelectedColors();
var barW = w / Math.max(colors.length, 1);
for (var i = 0; i < colors.length; i++) {
    var color = colors[i];
    Rect.create().pos(i * barW, 0).size(barW, h * 0.2).color(color | 0xFF000000).draw(ctx);
    Text.create().pos(i * barW, h * 0.2).size(barW, h * 0.3)
        .text(sign.getRouteNumber(color)).color(0x000000).scale(0.5).centered().draw(ctx);
}
```

### 4.5 出入口接口（exit）

| 接口 | 返回 | 说明 |
|------|------|------|
| `getExitCount()` | int | 出口数量 |
| `getExitNumbers()` | String[] | 出口编号列表（如 `["1A","2B"]`） |
| `getExitDestinations(index)` | String[] | 第 index 个出口的目的地列表 |
| `getExitInfo(index)` | String | 完整信息："编号：目的地1/目的地2" |

### 4.6 自定义文本接口（text）

| 接口 | 返回 | 说明 |
|------|------|------|
| `getCustomText()` | String | 玩家在编辑屏输入的自定义文本（未设置返回空串） |

普通屏幕可使用 `nanbin_custom_text` 指示牌 + 编辑屏输入文本；
JS 样式下点编辑按钮 → "自定义文本"按钮同样可输入，脚本用 `getCustomText()` 读取。

### 4.7 时间接口

| 接口 | 返回 | 说明 |
|------|------|------|
| `getWorldTime()` | long | 系统毫秒时间戳 |
| `getFormattedTime(format)` | String | 按 Java SimpleDateFormat 格式化（如 `"HH:mm"`、`"yyyy-MM-dd"`） |

### 4.8 其它

| 接口 | 返回 | 说明 |
|------|------|------|
| `getBackgroundColor()` | int | 原版渲染的背景色 |
| `getSignIds()` | String[] | 本行所有格子 signId |
| `getCellSignId(index)` | String | 指定格子的 signId |
| `getCellIndex()` | int | 当前格序号 |
| `createResult(text)` | JSSignResult | 兼容旧 execute 接口的返回值 |

### 4.9 调试输出

```js
print("调试信息");   // 输出到玩家聊天框（带 [JS] 前缀），相同内容 2 秒内去重
```

---

## 五、数据选择屏（动态按钮）

编辑锁定状态下点击"编辑"按钮会打开**数据选择屏**（类似 StationInfo），
按钮根据脚本实际调用的接口**动态显示**：

- 脚本调用了站台接口 → 显示"选择站台"
- 调用了车站接口 → 显示"选择车站"
- 调用了线路接口 → 显示"选择线路"
- 调用了出口接口 → 显示"选择出口"
- 调用了自定义文本 → 显示"自定义文本"
- 脚本未调用任何接口（或未渲染过）→ 显示全部按钮

> 注意：数据使用记录在脚本渲染时收集，需脚本至少渲染过一次再打开编辑屏，
> 按钮过滤才准确。

---

## 六、旧接口（execute，不推荐）

早期版本用 `execute(ctx, state, sign)` 返回 `JSSignResult`（单个）或 `{cells:[...]}`（每格独立）。
该接口仍兼容（自动逐格绘制），新脚本请使用 `create/render/dispose` 生命周期。

```js
function execute(ctx, state, sign) {
    return sign.createResult("内容", 0x000000, 1.0, false, 0xFFFFFF);
}
```

---

## 七、内置示例脚本

| 脚本 | 演示内容 |
|------|----------|
| `crt_station_entrance.js` | 站名显示、未选择提示 |
