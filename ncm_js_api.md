# Nanbin JS 指示牌脚本 API 文档

> 适用版本：**Minecraft 1.19.2 + MTR `FABRIC-1.19.2-3.2.2-hotfix-1` + Nanbin**（与 `common/build.gradle` 中的依赖一致）。
> 脚本运行在 **Nashorn 15.4** 引擎中（以 `--language=es6` 启动），支持**部分 ES6** 语法。
> 文中标注「实测」的表格与结论均在本仓库使用的 Nashorn 15.4 上逐条验证过。

## 目录

| 章节 | 内容 |
|---|---|
| [一](#一快速上手) | 快速上手：注册脚本 → 选择样式 |
| [二](#二语法与数据类型限制实测) | 语法与数据类型限制（实测） |
| [三](#三脚本生命周期) | 脚本生命周期 `create` / `render` / `dispose` |
| [四](#四绘制-apictx) | 绘制 API：`Text` / `Rect` / `Texture` / `Line` |
| [五](#五数据接口sign) | 数据接口 `sign` 全表 |
| [六](#六脚本文件位置与资源包覆盖) | 脚本文件位置与资源包覆盖 |
| [七](#七数据选择屏) | 数据选择屏（动态按钮） |
| [八](#八旧接口execute不推荐) | 旧接口 `execute`（不推荐） |
| [九](#九内置示例脚本) | 内置示例脚本 |
| [十](#十常见坑与排错) | 常见坑与排错 |
| [十一](#十一完整脚本模板) | 完整脚本模板 |

---

## 一、快速上手

### 1. 写脚本

在 `common/src/main/resources/assets/nanbin/js/` 下新建 `my_style.js`：

```js
function render(ctx, state, sign) {
    // 全屏白色背景
    Rect.create().pos(0, 0).size(sign.getWidth(), sign.getHeight()).color(0xFFFFFF).draw(ctx);
    // 居中显示站名（"中文|English" 会自动排成两行）
    Text.create()
        .pos(0, 0)
        .size(sign.getWidth(), sign.getHeight())
        .text(String(sign.getStationNames()))
        .color(0x000000)
        .bold()
        .centered()
        .draw(ctx);
}
```

### 2. 注册脚本

在 `common/src/main/resources/assets/nanbin/js_signs_config.json` 的 `scripts` 数组里登记：

```json
{
  "id": "my_style",
  "path": "nanbin:js/my_style.js",
  "icon": "nanbin:textures/block/sign/js_icon.png",
  "name": "js_sign.nanbin.my_style"
}
```

| 字段 | 必需 | 说明 |
|---|---|---|
| `id` | ✅ | 脚本唯一标识；同时决定样式标记 `crt_js_style_<id>` |
| `path` | ✅ | 脚本位置，推荐 `命名空间:js/文件名.js`（见[第六节](#六脚本文件位置与资源包覆盖)） |
| `icon` | ❌ | 样式选择器里的图标；留空用默认图标 `nanbin:textures/block/sign/js_icon.png` |
| `name` | ❌ | 语言键；留空回退为 `gui.nanbin.js_sign.<id>`。在 `lang/zh_cn.json` 等文件里翻译 |

```json
// common/src/main/resources/assets/nanbin/lang/zh_cn.json
"js_sign.nanbin.my_style": "我的样式"
```

> 注册多个脚本时逐个追加对象即可；多个数据包/资源包可以各自提供一份 `js_signs_config.json`，会**全部合并**读取。

### 3. 游戏内选择样式

1. 打开指示牌编辑屏幕（铁路指示牌 / CRT 站牌 / 双层站牌均支持）。
2. 点击屏幕底部中间的 **「JS 样式」** 按钮 → 在列表中选中脚本（已选中的样式会直接显示为按钮标题）。
3. 选中后**该行**由脚本接管渲染，普通图标编辑被锁定；选择「无样式（清除）」可恢复普通编辑。

> 双层（双面）站牌按**行**判定：某一行为 JS 样式不影响其它行编辑。

### 4. 选择脚本要显示的数据

锁定状态下点击「编辑」按钮会打开**数据选择屏**（见[第七节](#七数据选择屏)），
按钮按脚本实际用到的接口动态显示，选完点「保存」即可。

---

## 二、语法与数据类型限制（实测）

Nashorn 15.4 即使开启 `--language=es6`，ES6 也只实现了**一部分**。下表全部为实测结果。

### 2.1 语法特性

| 语法 | 支持 | 备注 |
|---|---|---|
| `var` / `function` | ✅ | 最稳妥的写法 |
| `const` / `let` | ✅ | 例外：`switch` 的 `case` 里不能直接 `let`（未加 `{}` 时） |
| 箭头函数 `(a) => a * 2` | ✅ | |
| 模板字符串 `` `a${1 + 2}b` `` | ✅ | |
| 默认参数 `function f(a = 5)` | ✅ | |
| 计算属性名 `{ [k]: 1 }` | ✅ | |
| 对象字面量简写方法 `{ m() {} }` | ✅ | |
| getter / setter `{ get x() {} }` | ✅ | |
| `for...of` | ✅ | 数组、`Set`、Java 数组都可以 |
| `Symbol` | ✅ | |
| `globalThis` | ✅ | |
| **类 `class` / `extends` / `super`** | ❌ | `ES6 class declarations and expressions are not yet implemented` |
| **解构** `var {a} = o` / `var [a] = arr` / 形参解构 | ❌ | `ES6 destructuring is not yet implemented` |
| **展开运算符** `f(...args)` / `[...arr]` / `{...obj}` | ❌ | `ES6 spread operator is not yet implemented` |
| **剩余参数** `function f(...args)` | ❌ | 用 `arguments` 代替 |
| **生成器** `function*` / `yield` | ❌ | |
| **`async` / `await`** | ❌ | 非法语法，脚本直接解析失败 |
| **`Promise`** | ❌ | 运行时 `Promise is not defined` |
| **可选 catch 绑定** `try {} catch {}` | ❌ | 必须写 `catch (e)` |
| **`new.target`** | ❌ | |
| **BigInt 字面量** `1n` | ❌ | |

### 2.2 标准库可用性

| API | 支持 | 备注 |
|---|---|---|
| `Array`：`join` `slice` `concat` `push` `pop` `shift` `unshift` `splice` `reverse` `sort(fn)` `indexOf` `lastIndexOf` `reduce` `reduceRight` `some` `every` `map` `filter` `forEach` | ✅ | 完整 |
| `Array.prototype.find` / `findIndex` / `fill` / `copyWithin` / `flat` / **`includes`** | ❌ | 用 `filter(...).length`、手写循环、`indexOf(x) >= 0` 代替 |
| `Array.from` / `Array.of` | ❌ | 需要转 JS 数组时用 `Java.from(javaArray)` |
| `Map` / `Set` / `WeakMap` / `WeakSet`、`Map.forEach` | ✅ | |
| `Object.keys` / `getOwnPropertyNames` / `freeze` / `defineProperty` / `create` / `getPrototypeOf` | ✅ | |
| `Object.assign` / `Object.values` / `Object.entries` / `Object.is` / `Object.fromEntries` | ❌ | 用 `for...in` 手工拷贝 |
| `String.prototype` 的 `indexOf` `slice` `substring` `substr` `split` `replace` `trim` `toUpperCase` `toLowerCase` `charAt` `charCodeAt` `concat` `match` `length` | ✅ | |
| `String.prototype.startsWith` / `endsWith` / `repeat` | ✅ | |
| **`String.prototype.includes`** | ❌ | 用 `s.indexOf(x) >= 0` |
| `String.prototype.padStart` / `padEnd` / `trimStart` / `trimEnd` | ❌ | 手工补零/裁剪 |
| `Math.round` `floor` `ceil` `abs` `min` `max` `pow` `sqrt` `random` `PI` `E` | ✅ | |
| `Math.trunc` / `sign` / `cbrt` / `hypot` / `log2` / `log10` | ❌ | `Math.trunc(x)` 可用 `x < 0 ? Math.ceil(x) : Math.floor(x)` |
| `Number.EPSILON` | ✅ | |
| `Number.isInteger` / `Number.isNaN` / `Number.parseInt` / `Number.parseFloat` | ❌ | 用全局 `isNaN` / `parseInt` / `parseFloat` |
| `JSON.stringify` / `JSON.parse` | ✅ | |
| `Date`、`RegExp`、`parseInt`、`parseFloat`、`isNaN`、`isFinite`、`encodeURIComponent` | ✅ | |
| `Java.type('java.lang.Math')` 等 Java 互操作、`Java.from(array)` | ✅ | 可直接调用 Java 类库 |

### 2.3 Java 返回值 → JS 类型（重要）

数据接口返回的是 **Java 值**，Nashorn 的转换规则并不总像 JS 那样：

| Java 返回类型 | JS 里的样子 | 注意事项 |
|---|---|---|
| `String` | 普通 JS 字符串 | `length` / `===` / `+` 都正常；但**没有 `includes`** |
| `boolean` | `boolean` | 正常 |
| `int` | `number` | 正常（如 `getRouteColor(i)`） |
| `float` / `double` | `number` | 正常（如 `getWidth()`） |
| **`long`** | **对象（装箱 `Long`）** | `typeof` 是 `"object"`；`c === 4283453520` 为 **false**，`c == 4283453520` 为 true；参与 `+` 拼接正常；要当数字用请 `Number(c)` |
| **Java 数组**（`String[]` / `long[]`） | 对象 | `.length` 和 `arr[i]` 可用；**`join`/`map`/`forEach`/`slice`/`indexOf` 全部不可用**——那是 Java 数组，没有 `Array.prototype`。用 `for` 循环或 `Java.from(arr)` |
| `null` | `null` | `x === null` 为 true |

```js
// ❌ 会抛 TypeError: numbers.join is not a function
var numbers = sign.getPlatformNumbers();     // Java String[]
var text = numbers.join("/");

// ✅ 手工拼接（推荐，不依赖 Java.*）
var text = "";
for (var i = 0; i < numbers.length; i++) { text += (i > 0 ? "/" : "") + numbers[i]; }

// ✅ 或先转成真正的 JS 数组
var text2 = Java.from(numbers).join("/");
```

```js
// ❌ 不要对 Java 数组元素直接做位运算，结果不可靠
Rect.create().color(sign.getSelectedColors()[0] | 0xFF000000).draw(ctx);

// ✅ 取出来当数字或直接传值
var color = sign.getSelectedColors()[0];
Rect.create().color(color).draw(ctx);          // 直接传，引擎会正确处理 32 位 ARGB
Rect.create().color(Number(color)).draw(ctx);  // 或显式转成 number
```

### 2.4 调试输出

```js
print("调试信息");
```

输出到玩家聊天框，带 `[JS] ` 前缀；**相同内容 2 秒内不重复发送**（防刷屏）。
渲染很频繁，不要每帧打印不同内容；日志也会同时出现在 `logs/latest.log`。

---

## 三、脚本生命周期

```js
function create(ctx, state, sign) {
    // 每块指示牌首次渲染前调用一次，用于初始化
    state.lastText = "";
}

function render(ctx, state, sign) {
    // 每帧调用，在这里绘制整个指示牌
}

function dispose(ctx, state, sign) {
    // 实例被回收时调用一次（指示牌拆除 / 区块卸载 / 样式切换后）
}
```

三个函数**都是可选的**，缺省则跳过。参数含义：

| 参数 | 说明 |
|---|---|
| `ctx` | 绘制上下文，见[第四节](#四绘制-apictx) |
| `state` | 跨帧状态对象，**每块指示牌独立**；首次渲染前是空对象 `{}`，可任意挂属性 |
| `sign` | 数据接口，见[第五节](#五数据接口sign) |

补充说明：

* 同一个脚本文件被多块指示牌使用时，**代码只加载一次**（共用函数与全局变量），但 `state` 每块独立。
  因此不要把「每块牌子的数据」放在全局变量里，要放进 `state`。
* 实例空闲 **10 秒**（未渲染）会被回收：先调用 `dispose`，再清掉 `state`。
* 生命周期函数都在客户端渲染线程上串行调用；脚本里的异常会被捕获并写入日志（`Failed to render JS sign script: ...`），不会导致游戏崩溃，但该帧不会有任何绘制。
* 建议把 `print` 之类的调试输出集中放在 `create`，或在 `render` 里配合 `state` 做节流。

---

## 四、绘制 API（ctx）

坐标系与世界坐标一致：`x ∈ [0, getWidth()]`，`y ∈ [0, getHeight()]`，原点在**左上角**。

| 类型 | 尺寸 | `getWidth()` | `getHeight()` |
|---|---|---|---|
| 铁路指示牌 / 普通 CRT 指示牌 | 长 3–11 格，格子 0.5 | 格数 × 0.5（如 7 格 = 3.5） | 0.5 |
| CRT 站牌（单面 / 双层） | 2 行 × 7 格，格子 0.3 | 7 × 0.3 = 2.1 | 0.3 |

所有绘制对象都是**链式 builder**，最后 `.draw(ctx)` 提交。同一帧内的绘制按调用顺序叠加（后画的盖在上面）。

### Text 文字

```js
Text.create()
    .pos(x, y)              // 左上角坐标
    .size(w, h)             // 文字框宽高（省略则自动铺满到指示牌右下角）
    .text("内容")            // 支持 "中文|English" 用 | 分行的双语排版
    .color(0x000000)        // 颜色，默认 0xFF000000（黑）
    .scale(0.9)             // 字号缩放，默认 1.0
    .bold()                 // 加粗（也可 .bold(true) / .bold(false)）
    .centered()             // 框内水平+垂直居中（也可 .centered(true/false)）
    .draw(ctx);
```

| 方法 | 参数 | 默认 | 说明 |
|---|---|---|---|
| `pos(x, y)` | 数字 | `0, 0` | 文字框左上角（世界单位） |
| `size(w, h)` | 数字 | 铺满剩余区域 | 文字框尺寸；文字会**自动缩放到框内** |
| `text(s)` | 字符串 | `""` | 用 `\|` 分行；中/英行分别取不同字号 |
| `color(c)` | 颜色 | `0xFF000000` | 无 alpha 时自动补为不透明 |
| `scale(s)` | 数字 | `1.0` | 在框内基础上再缩放，`< 1` 变小 |
| `bold(v)` / `bold()` | 布尔 | `false` | 加粗 |
| `centered(v)` / `centered()` | 布尔 | `true` | 居中；`centered(false)` 为左对齐 |

用 `|` 把中文和英文分开即可得到两行（这是 MTR 的惯例，`getStationNames()` 等接口返回的字符串里也常带 `|`）：

```js
.text("本站|This Station")
```

测量文本自然宽度（世界单位）：

```js
var w = Text.measure("文字", sign.getHeight());
```

> `measure` 只按「文字铺满给定高度」计算，**不包含 `.scale()` 的影响**；需要精确宽度时用 `Text.measure(text, h) * scale`。

### Rect 矩形（背景 / 色块 / 色条）

```js
Rect.create()
    .pos(x, y)
    .size(w, h)
    .color(0xFFFFFF)
    .draw(ctx);
```

| 方法 | 默认 | 说明 |
|---|---|---|
| `pos(x, y)` | `0, 0` | 左上角 |
| `size(w, h)` | 铺满剩余区域 | `w`/`h` 必须 > 0，否则不绘制 |
| `color(c)` | `0xFFFFFFFF` | 支持半透明（如 `0x80000000`） |

### Texture 贴图

```js
Texture.create()
    .texture("nanbin:textures/block/sign/crt_railway.png")  // 资源位置，必须带命名空间
    .pos(x, y)
    .size(w, h)
    .color(0xFFFFFFFF)   // 可做染色
    .draw(ctx);
```

### Line 线段

```js
Line.create()
    .from(x1, y1)
    .to(x2, y2)
    .width(0.05)      // 线宽（世界单位），默认 0.05
    .color(0x000000)
    .draw(ctx);
```

### 颜色写法

颜色统一按 **32 位 ARGB** 处理：

| 写法 | 结果 |
|---|---|
| `0xFFFFFF` | 自动补 alpha → 不透明白色 |
| `0x000000` | 自动补 alpha → 不透明黑色 |
| `0xFF000000` / `0x80123456` | 已带 alpha，原样使用 |
| `sign.getSelectedColors()[i]` | 直接传给 `.color()` 即可，引擎按 32 位解释 |
| `sign.getRouteColor(i)` | 同上（Java `int`） |

> **不要写成 `arr[i] | 0xFF000000`**。`getSelectedColors()` 的元素是 Java `Long` 对象，
> 位运算结果不可靠（实测会得到错误颜色）；直接传值最稳妥。

脚本自己画文字时需自行指定与背景对比的颜色（深底用 `0xFFFFFF`，浅底用 `0x000000`）。

---

## 五、数据接口（sign）

### 5.1 尺寸与标识

| 接口 | 返回 | 说明 |
|---|---|---|
| `getWidth()` | float | 渲染宽度（世界单位）= 格数 × 单格尺寸 |
| `getHeight()` | float | 渲染高度（世界单位）= 单格尺寸 |
| `getCellCount()` | int | 格子数量 |
| `getCellSize()` | float | 单格世界尺寸（铁路指示牌 0.5，站牌 0.3） |
| `getScreenKey()` | String | 当前屏幕唯一标识（脚本 + 坐标 + 行位置 + 朝向），多屏同脚本时可区分 |
| `getScreenType()` | String | 屏幕类型：`platform` / `route` / `exit` / `station` / `custom` |
| `getSignIds()` | String[] | 本行所有格子的 signId |
| `getCellSignId(index)` | String | 指定格子的 signId |
| `getCellIndex()` | int | 当前格序号 |
| `getBackgroundColor()` | int | 原版渲染使用的背景色 |

`getScreenType()` 依据本行**首个非样式格子**的类型判断，是一个脚本适配多种屏幕的关键分流依据：

```js
var type = String(sign.getScreenType());   // Java String → 先转成 JS 字符串再比较
if (type === "route") { /* 线路屏 */ }
else if (type === "exit") { /* 出口屏 */ }
else if (type === "platform") { /* 站台屏 */ }
else if (type === "station") { /* 车站屏 */ }
else { /* custom：自定义文本屏或其它 */ }
```

### 5.2 线路接口（route）

| 接口 | 返回 | 说明 |
|---|---|---|
| `getSelectedColors()` | long[] | **选中数据对应的线路颜色（ARGB）**，按颜色值升序；站点/站台会自动展开为其下属线路，并包含换乘站 |
| `getRouteColor(index)` | int | `getSelectedColors()[index]`，越界返回 0 |
| `getRouteName(color)` | String | 线路中文名（`"1号线\|Line 1"` 取第一段） |
| `getRouteName(color, true)` | String | 线路英文名 |
| `getRouteNumber(key)` | String | 线路编号；无编号映射时回退从线路名提取数字（环线可能为空串） |
| `getRouteDestination(color)` | String | 线路终点站名（最后一站） |
| `isLoopRoute(color)` | boolean | 是否环线 |
| `getRouteNumbers()` | String[] | 当前选中数据按顺序解析出的线路编号（跳过无编号的） |

**数据选择屏可选多条线路**：每次点选只是把该项的 id 追加进 `selectedIds`，
所以多选后**不是**返回「某一条」，而是 `getSelectedColors()` 里多出几个颜色值：

```js
var colors = sign.getSelectedColors();
var cellW = sign.getWidth() / Math.max(colors.length, 1);
for (var i = 0; i < colors.length; i++) {
    var color = colors[i];
    Rect.create().pos(i * cellW, 0).size(cellW, sign.getHeight() * 0.2).color(color).draw(ctx);
    Text.create().pos(i * cellW, sign.getHeight() * 0.2).size(cellW, sign.getHeight() * 0.3)
        .text(String(sign.getRouteNumber(color))).color(0x000000).scale(0.5).centered().draw(ctx);
}
```

要点：

* `getSelectedColors()` 的**顺序是颜色值升序**，不是点选顺序。需要固定顺序时请按自己的规则排序。
* 传 `color` / 平台 ID 给 `getRouteNumber()` 都能取到编号（内部先精确匹配，再按 RGB / ARGB 兜底）：
  铁路指示牌的编号表以**线路颜色**为键，站牌（StationInfo）的编号表以**站台 ID** 为键，两种屏幕用同一个接口即可。

### 5.3 站台接口（platform）

| 接口 | 返回 | 说明 |
|---|---|---|
| `getPlatformCount()` | int | 选中站台数量（只统计站台 ID） |
| `getPlatformNumbers()` | String[] | 站台编号（如 `["1","2"]`），按 ID 排序 |

### 5.4 车站接口（station）

| 接口 | 返回 | 说明 |
|---|---|---|
| `getSelectedStationIds()` | long[] | 选中的站点 / 站台 ID |
| `getStationNames()` | String | 站名合并串，多站用 `/` 分隔（站名本身可能带 `\|` 的双语换行） |
| `getStationName(platformId)` | String | 指定站台 / 站点 ID 的名称 |

### 5.5 出入口接口（exit）

| 接口 | 返回 | 说明 |
|---|---|---|
| `getExitCount()` | int | 出口数量 |
| `getExitNumbers()` | String[] | 出口编号（如 `["1A","2B"]`） |
| `getExitDestinations(index)` | String[] | 第 index 个出口的目的地列表 |
| `getExitInfo(index)` | String | 完整信息，格式 `编号：目的地1/目的地2`；无目的地时只返回编号 |

### 5.6 自定义文本接口（text）

| 接口 | 返回 | 说明 |
|---|---|---|
| `getCustomText()` | String | 玩家在编辑屏输入的自定义文本，未设置返回空串 |

编辑方式：普通屏幕可直接用 `nanbin_custom_text` 指示牌；JS 样式下点击「编辑」→「自定义文本」。

### 5.7 时间接口

| 接口 | 返回 | 说明 |
|---|---|---|
| `getWorldTime()` | long | 系统毫秒时间戳（Java `long`，要参与运算先 `Number(...)`） |
| `getFormattedTime(format)` | String | 按 Java `SimpleDateFormat` 格式化，如 `"HH:mm"`、`"yyyy-MM-dd"` |

### 5.8 其它

| 接口 | 返回 | 说明 |
|---|---|---|
| `createResult(text)` | JSSignResult | 仅配合旧 `execute` 接口使用，见[第八节](#八旧接口execute不推荐) |
| `createResult(text, textColor, textSize, textBold, backgroundColor)` | JSSignResult | 同上（带样式参数） |

### 5.9 使用注意

* 所有接口都是**读操作**，脚本无法修改指示牌数据。
* 数据来源是客户端已同步的 MTR 数据（`ClientData`），与游戏内看到的线路/车站信息一致。
* 没有选中任何数据时，数组类接口返回**空数组**（`length === 0`），字符串类接口返回 `""`；
  脚本要处理这种「未选择」状态，否则指示牌上会是一片空白。

---

## 六、脚本文件位置与资源包覆盖

`path` 字段支持两种写法：

| 写法 | 例子 | 说明 |
|---|---|---|
| **资源位置**（推荐） | `nanbin:js/my_style.js` | 等价于读取 `assets/nanbin/js/my_style.js`，通过资源管理器查找，**可被资源包覆盖** |
| 类路径 | `assets/nanbin/js/my_style.js` | 旧写法，直接从 mod jar 读取，资源包无法覆盖 |

资源位置的解析顺序是「资源包 > mod 内置资源」，因此整合包作者可以把同名脚本放进自己的资源包来覆盖 mod 里的版本：

```
资源包根目录/
└─ assets/nanbin/
   ├─ js/你的脚本.js
   └─ js_signs_config.json        ← 也可以只放这一份来追加/覆盖脚本
```

也兼容冗余写法：`nanbin:/js/x.js`、`nanbin:assets/nanbin/js/x.js`。

> 脚本改动后需要让资源重载生效：退出重进世界，或按 `F3 + T` 重载资源，再重新打开指示牌。

---

## 七、数据选择屏

JS 样式锁定后，点击「编辑」按钮不再进入图标编辑，而是打开**数据选择屏**。
按钮按脚本**实际调用过的接口**动态显示（脚本还没渲染过时显示全部按钮）：

| 脚本用到的接口 | 出现的按钮 | 选择器列出 |
|---|---|---|
| 站台接口 | 选择站台 | 本站及换乘站的全部站台 |
| 车站接口 | 选择车站 | 本站 + 换乘站 |
| 线路接口 | 选择线路 | 途经本片站点的线路（按颜色去重） |
| 出口接口 | 选择出口 | 该站的全部出口 |
| 自定义文本 | 自定义文本 | 打开文本输入框 |

* 选择器是**多选**的，可以同时选中多条线路 / 多个站台；取消选中即移除。
* 列表为空时按钮不显示（例如该站没有出口）。
* 数据写入该**行**的 `selectedIds`，点「保存」后写回指示牌。
* 因为按钮过滤依赖「脚本调用过哪些接口」的记录，**刚换上新脚本时请先让它渲染一帧**（把指示牌放在视野里），再打开编辑屏。

---

## 八、旧接口：`execute`（不推荐）

早期版本使用 `execute(ctx, state, sign)` 返回结果，由渲染器逐格绘制。
仍兼容（没有 `render` 时会自动走这条路径），但新脚本请使用 `create` / `render` / `dispose`。

```js
function execute(ctx, state, sign) {
    return sign.createResult("内容", 0x000000, 1.0, false, 0xFFFFFF);
}
```

返回值可以是：

| 返回 | 效果 |
|---|---|
| `sign.createResult(...)` | 整块指示牌使用同一份结果 |
| `{ cells: [ ... ] }` 或数组 | 每格独立结果（格子数不足时最后一格循环复用） |

---

## 九、内置示例脚本

| 脚本 | 名称 | 演示内容 |
|---|---|---|
| `crt_station_entrance.js` | 重庆轨道交通入站 | 站名显示、"未选择数据"提示（`getStationNames` + `print`） |
| `example_clock.js` | 示例时钟 | `Rect` 背景与进度条、`Text` 字号/加粗/居中、时间接口、`state` 跨帧状态 |
| `example_station_info.js` | 示例站台信息 | 线路色条 + 线路编号（多线路等分）、站名双语换行、站台号 |
| `example_multi_screen.js` | 示例多屏 | `getScreenType()` 分流：线路屏 / 出口屏 / 站台屏 / 车站屏 / 自定义文本屏 |

> 这些脚本都用「与真实 API 相同签名的 Java mock」按每种屏幕类型跑过（含「有数据 / 无数据」两种情况），
> 可以直接复制它们作为自己的起点。

---

## 十、常见坑与排错

| 现象 | 原因与处理 |
|---|---|
| 指示牌一片空白，日志有 `Failed to render JS sign script` | 脚本抛异常。用 `print` 定位；常见是调用了不支持的语法/方法（见第二节） |
| 报 `... is not a function`，如 `numbers.join` | 对 **Java 数组**用了 `Array.prototype` 的方法。改 `for` 循环或 `Java.from(arr)` |
| 报 `"abc".includes is not a function` | Nashorn 没有 `String.prototype.includes`，改 `indexOf(x) >= 0` |
| 报 `Promise is not defined`、`class ... not yet implemented` | 语法/内建不支持，改用 ES5 写法 |
| 颜色发白 / 不对 | 对 Java `long` 或数组元素做了位运算。直接传值或 `Number(x)` |
| `c === 4283453520` 为 false | `getSelectedColors()` 的元素是装箱 `Long`，用 `Number(c) === ...` 或 `c == ...` |
| 文字不显示 | `.size()` 为 0 或负数、文本框在指示牌外、`text` 为空串都会跳过绘制 |
| 修改了 .js 但游戏里没变化 | 脚本内容有缓存：重载资源（`F3 + T`）或重进世界 |
| 样式选择器里找不到新脚本 | `js_signs_config.json` 没登记、`id` 重复、`path` 写错或找不到文件 |
| 选择器图标是紫黑方块 | `icon` 路径不存在（要带命名空间且指向 `textures/...png`）；留空则用默认图标 |
| 锁定了却还能编辑图标 | 「无样式」时该行是普通模式；确认该行第 0 格是 `crt_js_style_<id>` |

排查步骤：

1. 看游戏内聊天框的 `[JS] ...` 输出；
2. 看 `fabric/run/logs/latest.log`（搜索脚本 id 或 `JS sign`）；
3. 用 `print(JSON.stringify({...}))` 把拿到的数据打出来确认接口返回值。

---

## 十一、完整脚本模板

一个可以同时适配线路屏 / 出口屏 / 站台屏 / 车站屏 / 自定义文本屏的模板：

```js
// 每块牌子独立的 state
function create(ctx, state, sign) {
    state.frames = 0;
}

function render(ctx, state, sign) {
    state.frames++;
    var type = String(sign.getScreenType());
    // 背景
    Rect.create().pos(0, 0).size(sign.getWidth(), sign.getHeight()).color(0xFFFFFF).draw(ctx);

    if (type === "route") {
        drawRoutes(ctx, sign);
    } else if (type === "exit") {
        drawExits(ctx, sign);
    } else if (type === "platform") {
        drawPlatforms(ctx, sign);
    } else if (type === "station") {
        drawStation(ctx, sign);
    } else {
        drawText(ctx, sign, String(sign.getCustomText()));
    }
}

function dispose(ctx, state, sign) {
    // 需要释放外部资源时写在这里（本模板无）
}

function drawRoutes(ctx, sign) {
    var colors = sign.getSelectedColors();
    if (colors.length === 0) {
        drawText(ctx, sign, "未选择线路|No route selected");
        return;
    }
    var cellW = sign.getWidth() / colors.length;
    for (var i = 0; i < colors.length; i++) {
        var color = colors[i];
        Rect.create().pos(i * cellW, 0).size(cellW, sign.getHeight()).color(color).draw(ctx);
        Text.create().pos(i * cellW, 0).size(cellW, sign.getHeight())
            .text(String(sign.getRouteNumber(color))).color(0xFFFFFF).bold().scale(0.7)
            .centered().draw(ctx);
    }
}

function drawExits(ctx, sign) {
    var count = sign.getExitCount();
    if (count === 0) {
        drawText(ctx, sign, "未选择出口|No exits selected");
        return;
    }
    var rowH = sign.getHeight() / count;
    for (var i = 0; i < count; i++) {
        var label = String(sign.getExitInfo(i));
        Text.create().pos(sign.getWidth() * 0.05, i * rowH)
            .size(sign.getWidth() * 0.9, rowH).text(label).color(0x000000).scale(0.6).draw(ctx);
    }
}

function drawPlatforms(ctx, sign) {
    var numbers = sign.getPlatformNumbers();
    var text = "";
    for (var i = 0; i < numbers.length; i++) { text += (i > 0 ? "/" : "") + numbers[i]; }
    drawText(ctx, sign, text.length > 0 ? "站台|Platform " + text : "未选择站台|No platform selected");
}

function drawStation(ctx, sign) {
    drawText(ctx, sign, String(sign.getStationNames()));
}

function drawText(ctx, sign, text) {
    if (text.length === 0) { text = "—"; }
    Text.create().pos(0, 0).size(sign.getWidth(), sign.getHeight())
        .text(text).color(0x000000).bold().scale(0.8).centered().draw(ctx);
}
```
