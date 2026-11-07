# CRT 导视牌编辑器 —— MTR 数据联动开发文档

> 目标读者：实现「CRT 导视牌编辑器」的工程师（Java，通过 `org.mtr.mapping.*` 映射层调用 MTR）。
> 目标仓库：`D:\NCM+mtr4.0\Nanbin`（Nanbin，MTR 4.0.5 附属，Fabric 源树，MC 1.20.1；Stonecutter 多版本）。
>
> **验证方式**：本文所有 MTR 侧签名均由
> `C:\Program Files\Java\jdk-21.0.10\bin\javap.exe -p -classpath libs\MTR-fabric-4.0.5+1.20.4.jar <类名>`
> 直接读取字节码得到；数据流结论来自对相关方法体的 `javap -c` 反汇编。
> 仓库侧行号对应当前工作树。**未列出的 API 一律视为不存在，不要臆造。**

---

## 0. 前置：数据在哪、什么时候可用

### 0.1 关键类归属（客户端独有 / 双端通用）

| 类 | 归属 | 说明 |
|---|---|---|
| `org.mtr.mod.client.MinecraftClientData` | **客户端独有** | 客户端数据快照，`extends org.mtr.core.data.ClientData` |
| `org.mtr.mod.client.DynamicTextureCache` / `IDrawing` / `RouteMapGenerator` | **客户端独有** | 动态纹理缓存、绘制工具 |
| `org.mtr.mod.screen.*`（`DashboardListSelectorScreen` / `PIDSConfigScreen` / `EditStationScreen` / `RailwaySignScreen` …） | **客户端独有** | 全部是界面类 |
| `org.mtr.mod.render.*`（`RenderRouteBase` / `MainRenderer` …） | **客户端独有** | 渲染工具 |
| `org.mtr.mod.InitClient` | **客户端独有** | `findStation` / `findClosePlatform` / `getStationColor` |
| `org.mtr.core.data.*`（`Station` / `Platform` / `SimplifiedRoute` / `Route` / `StationExit` / `Data` / `ClientData`） | **双端通用** | 数据模型，服务端 `Simulator` 也继承 `Data` |
| `org.mtr.mod.data.IGui` | 双端可加载（GUI 工具，仅在客户端用） | 颜色常量、字符串合并、`isCjk` 等纯函数 |
| `org.mtr.mod.generated.lang.TranslationProvider` | 双端可加载 | 本地化文本 holder |
| `org.mtr.mod.Init` | 双端（`main` 字段只在服务端/集成服务器非空） | 反射入口 |
| `org.mtr.core.simulation.Simulator` | 服务端 | `extends org.mtr.core.data.Data` |

> **红线**：`org.mtr.mod.client.*` / `org.mtr.mod.screen.*` / `org.mtr.mod.render.*` 只能在客户端类里 import。写进通用（common）代码会在专用服务器上 `NoClassDefFoundError`。仓库现有导视牌编辑器都放在 `com.Nanbin.client.*`，请沿用。

### 0.2 同步模型（决定了哪些字段客户端拿不到）

```
服务端 Simulator(extends Data)                     客户端 MinecraftClientData(extends ClientData→Data)
      │  get_data 请求                                  ▲
      └── DataResponse  ────────────────────────────────┘
          DataResponseSchema 只序列化：
          stations / platforms / sidings / "simplifiedRoutes" / depots / rails (+ xxxToKeep)
          ⚠ 没有 routes 列表
      addRoute(Route) 的实现 = SimplifiedRoute.addToList(...)   ← 服务端 Route 被“降级”成 SimplifiedRoute

      update_data（增量回写，Dashboard 等场景）
      └── UpdateDataResponse  ── 这个才带 routes 字段（含 routeNumber），但不是常规同步路径
```

**结论**：客户端常规数据里**只有** `SimplifiedRoute`，没有 `Route.routeNumber`。这是仓库里 `BlockCRTRailwaySign` 等用服务端反射 + NBT 的根本原因（仓库注释亦如此说明，见 `BlockCRTRailwaySign.java:141`、`BlockCRTStationName1.java:71-72`）。

字节码证据：
- `org.mtr.core.generated.operation.DataResponseSchema` 字段仅 `stations/platforms/sidings/simplifiedRoutes/depots/rails`（±`ToKeep`），无 `routes`。
- `org.mtr.core.operation.DataResponse#addRoute(org.mtr.core.data.Route)` 方法体：
  `simplifiedRoutes.add(SimplifiedRoute.addToList(...))`。
- `org.mtr.core.operation.UpdateDataResponse#write()` 才会把 `Route` 更新进 `data.routes`/`data.routeIdMap`。

### 0.3 生命周期 / 空值

- `MinecraftClientData.getInstance()` 的字节码是 `getstatic instance; areturn`；而该类 `<clinit>` 里就 `new` 了两份实例（`instance` 与 `dashboardInstance`）。**类加载后永不为 null**，但**数据可能为空**（进服前、首次同步前），必须按空列表处理。
- `getDashboardInstance()` 是 Dashboard 界面专用的另一份快照（`UpdateDataResponse#write()` 会同时写两份）。**导视牌编辑器一律用 `getInstance()`**，不要用 dashboard 实例。
- `MinecraftClientData#sync()`（公开方法）会调用 `ClientData#sync()` → `Data#sync()` 重建 `stationIdMap/platformIdMap/routeIdMap...`；`simplifiedRouteIdMap` 由 `sync()` 里的 `lambda$sync$3` 逐个 `put(getId(), route)`。
- 仓库 `com.Nanbin.client.Drawing.RouteMapGenerator` 的注释记录：`platform.routes` 每次 `sync` 会被清空重建，**实时渲染请优先用 `simplifiedRoutes`**（增量、稳定）。

---

## 1. 线路数据

### 1.1 `MinecraftClientData` 里到底有什么

MTR 侧签名（`javap -p`）：

```java
// org.mtr.mod.client.MinecraftClientData  —— 客户端独有
public final class MinecraftClientData extends org.mtr.core.data.ClientData {
    // 自己声明的字段（注意：没有 simplifiedRoutes！）
    public final Long2ObjectOpenHashMap<SimplifiedRoute> simplifiedRouteIdMap;
    public final ObjectArraySet<VehicleExtension> vehicles;
    public final Long2ObjectAVLTreeMap<PersistentVehicleData> vehicleIdToPersistentVehicleData;
    public final Long2ObjectAVLTreeMap<MinecraftClientData$LiftWrapper> liftWrapperList;
    public final Object2ObjectArrayMap<String, MinecraftClientData$RailWrapper> railWrapperList;
    public final Object2ObjectAVLTreeMap<String, LongArrayList> railIdToPreBlockedSignalColors;
    public final Object2ObjectAVLTreeMap<String, LongArrayList> railIdToCurrentlyBlockedSignalColors;
    public final ObjectArraySet<String> blockedRailIds;
    public final ObjectArrayList<DashboardListItem> railActions;
    public static String DASHBOARD_SEARCH; // 等 6 个搜索框字符串
    public static MinecraftClientData getInstance();
    public static MinecraftClientData getDashboardInstance();
    public static void reset();
    public void sync();
}
```

继承链（主要公开字段）：

```java
// org.mtr.core.data.ClientData
public final ObjectAVLTreeSet<SimplifiedRoute> simplifiedRoutes;
public final LongArrayList simplifiedRouteIds;

// org.mtr.core.data.Data（ClientData 的父类）
public final ObjectArraySet<Station>  stations;      public final Long2ObjectOpenHashMap<Station>  stationIdMap;
public final ObjectArraySet<Platform> platforms;     public final Long2ObjectOpenHashMap<Platform> platformIdMap;
public final ObjectArraySet<Route>    routes;        public final Long2ObjectOpenHashMap<Route>    routeIdMap;
public final ObjectArraySet<Siding>   sidings;       public final Long2ObjectOpenHashMap<Siding>   sidingIdMap;
public final ObjectArraySet<Depot>    depots;        public final Long2ObjectOpenHashMap<Depot>    depotIdMap;
public final ObjectArraySet<Lift>     lifts;         public final Long2ObjectOpenHashMap<Lift>     liftIdMap;
public final ObjectArraySet<Rail>     rails;         public final Object2ObjectOpenHashMap<String, Rail> railIdMap;
public final Long2ObjectOpenHashMap<Position> platformIdToPosition;
public static <...> tryGet(...); public static <...> put(...);
```

> 用 `MinecraftClientData.getInstance().simplifiedRoutes`（父类字段）、`.stationIdMap`、`.platformIdMap` 即可；不建议依赖 `routes/routeIdMap`（见 0.2）。

### 1.2 `SimplifiedRoute` 能取到什么

```java
// org.mtr.core.data.SimplifiedRoute extends org.mtr.core.generated.data.SimplifiedRouteSchema
public long   getId();                       // 线路 id（与 Route.id 相同）
public String getName();                     // 线路名，可能含 "|" / "||" 双语分隔
public int    getColor();                    // 实现为 (int)(color & 0xFFFFFF) —— 纯 RGB，无 alpha
public Route.CircularState getCircularState(); // NONE / CLOCKWISE / ANTICLOCKWISE
public ObjectArrayList<SimplifiedRoutePlatform> getPlatforms(); // 有序站台序列（见 §6）
public int    getPlatformIndex(long platformId); // 含该站台时返回下标，否则 -1
public static void addToList(ObjectArrayList<SimplifiedRoute>, Route); // 服务端把 Route 降级用
```

> **没有** `getRouteNumber()`。`getName()` 也要按 MTR 约定先 `split("\\|\\|")[0]`（见 §10.2）。

### 1.3 routeNumber：客户端拿不到 → 现有代码怎么解析并写 NBT

`Route#getRouteNumber()` 只存在于完整 `Route` 上：

```java
// org.mtr.core.data.Route extends RouteSchema extends NameColorDataBase
public String getRouteNumber();   public void setRouteNumber(String);
public String getRouteName();     // 实际为继承的 getName()
public ObjectArrayList<RoutePlatformData> getRoutePlatforms();
public String getDestination(int index);
public boolean getHidden();  public RouteType getRouteType();  public Route.CircularState getCircularState();
```

现有两条服务端反射路径（均可直接复用）：

**(A) 按“线路颜色 → 编号”解析**（铁路牌 `BlockCRTRailwaySign`，`BlockCRTRailwaySign.java:355-410`）

```java
// 服务端 BlockEntity 内（getWorld2().isClient() == false 时）
final Field mainField = Class.forName("org.mtr.mod.Init").getDeclaredField("main");
mainField.setAccessible(true);
final Object main = mainField.get(null);
final Field simulatorsField = main.getClass().getDeclaredField("simulators");
simulatorsField.setAccessible(true);
for (final Object simulator : (Iterable<?>) simulatorsField.get(main)) {
    final Data data = (Data) simulator;                       // Simulator extends Data
    for (final Station station : data.stations) {
        if (!station.inArea(org.mtr.mod.Init.blockPosToPosition(getPos2()))) continue;
        for (final Platform platform : station.savedRails)
            for (final Route route : platform.routes)         // 服务端 Route，有 routeNumber
                if (allColors.contains(route.getColor())) routeNumbers.put(route.getColor(), route.getRouteNumber());
        break;
    }
}
```

NBT 落盘格式（`BlockCRTRailwaySign.java:135-142, 272-349`）：

| 键 | 类型 | 含义 |
|---|---|---|
| `crt_route_colors` | `long[]` | 线路颜色（RGB，`(long) route.getColor()`，**有符号扩展**） |
| `crt_route_numbers` | `String` | 与上面同下标，`\u0001` 分隔的编号串 |
| `crt_cell_colors` | `String` | 每格独立线路色，`"<格子>:<色>,<色>;<格子>:<色>"` |
| `crt_sign_layout` | `String` | 自由布局 JSON（空=沿用 signIds） |

客户端读取接口（`BlockEntityCRTRailwaySign`）：

```java
public String   getRouteNumber(long color);      // 无 → ""
public String[] getRouteNumbers();               // 按 selectedIds 顺序，跳过无编号的颜色
public String[] getRouteNumbersForCell(long[] colors);
public long[]   getCellColors(int cell);         // 无 → null
public long[]   getSelectedIdsForCell(int cell); // 编辑器预填：本格有效线路色
```

**(B) 按“站台 id → 编号”解析**（站名牌 `BlockCRTStationName1.java:108-143`，`BlockCRTStationInfo1.java:370-421`）

```java
// StationName：platformId → 第一条有编号的 Route
final Platform platform = data.platformIdMap.get(platformId);
if (platform != null) for (final Route route : platform.routes) {
    final String number = route.getRouteNumber();
    if (number != null && !number.isEmpty()) { routeNumber = number; break; }
}
// StationInfo（NBT 键同 crt_route_colors/crt_route_numbers，但 key 是 platformId）：
for (final RoutePlatformData rp : route.getRoutePlatforms())
    if (rp.getPlatform() != null && allPlatformIds.contains(rp.getPlatform().getId()))
        routeNumbers.put(rp.getPlatform().getId(), route.getRouteNumber());
```

> ⚠ 同名键不同语义：`crt_route_colors` 在 `BlockCRTRailwaySign`/`BlockCRTRailwaySignWall1` 里是**线路颜色**；在 `BlockCRTStationInfo1/Double` 里是**站台 id**。新代码请勿跨方块复用同一读取函数。

**仓库里可直接复用的“客户端兜底”**（`com.Nanbin.client.Drawing.RouteMapGenerator`，`RouteMapGenerator.java:72-158`）：

```java
// 编号优先级：方块 NBT 同步值 → 客户端完整 Route（若有）→ 从线路名推导
ResolvedRouteData data = RouteMapGenerator.resolveRouteData(platformId, fallbackColor, syncedRouteNumber);
data.themeColor(); data.routeColor(); data.routeNumber();
RouteMapGenerator.deriveRouteNumberFromName("1号线|Line 1"); // MTR 规则：先截 ||，再取 | 前段
RouteMapGenerator.cleanRouteNumber("8号线");                  // 去掉「号线/线」后缀
```

### 1.4 渲染端可用的线路颜色/名称/编号获取路径（结论）

| 要拿的东西 | 客户端路径 | 服务端配合 |
|---|---|---|
| 线路颜色 | `SimplifiedRoute#getColor()`（RGB）→ `rgb \| 0xFF000000` 得 ARGB | 不需要 |
| 线路名 | `SimplifiedRoute#getName()` → `split("\\|\\|")[0]` | 不需要 |
| 线路编号 | **客户端常规数据没有**；优先读方块实体 NBT（`getRouteNumber(color)`），否则 `deriveRouteNumberFromName` 兜底 | **需要**：服务端反射 + NBT（现有方案）或新增 S2C 包 |
| 线路选择列表 | 按 MTR `RailwaySignScreen` 的写法：过滤本车站站台 → `DashboardListItem` | 不需要 |

MTR 官方列表构造（反汇编 `RailwaySignScreen#<init>` + `lambda$new$3`，可直接照抄）：

```java
final int color = simplifiedRoute.getColor();
if (!addedColors.contains(color) && simplifiedRoute.getPlatforms().stream()
        .anyMatch(p -> platformIds.contains(p.getPlatformId()))) {
    routes.add(new DashboardListItem(color, simplifiedRoute.getName().split("\\|\\|")[0], color));
    addedColors.add(color);
}
```

---

## 2. 车站数据

### 2.1 数据入口

- `MinecraftClientData.getInstance().stations`：`ObjectArraySet<Station>`（全量，未排序）。
- `MinecraftClientData.getInstance().stationIdMap`：`Long2ObjectOpenHashMap<Station>`（`Data#sync()` 重建）。
- 最近站查找：`InitClient.findStation(BlockPos)`（**只做区域精确匹配**，区域外返回 `null`）；更稳的兜底链见 §2.3。

### 2.2 `Station` 能取到什么

```java
// org.mtr.core.data.Station extends org.mtr.core.generated.data.StationSchema
//   → AreaBase<Station, Platform> → AreaBaseSchema → NameColorDataBase
public final ObjectAVLTreeSet<Station> connectedStations;  // 换乘站（不含自己）

public long   getId();            // NameColorDataBase，final
public String getName();          // final；可能 ""，可能含 "|" 双语
public int    getColor();         // final；字节码 = (int)(color & 0xFFFFFF)，纯 RGB
public String getColorHex();      // final；6 位 hex（Utilities.numberToPaddedHexString(color, 6)）
public TransportMode getTransportMode();
public void   setName(String);  public void   setColor(int);

public long getZone1/2/3();  public void setZone1/2/3(long);

public ObjectArrayList<StationExit> getExits();                       // 见 §4
public Object2ObjectAVLTreeMap<Station, Int2ObjectAVLTreeMap<ObjectArrayList<Route>>>
        getInterchangeStationToColorToRoutesMap(boolean includeConnections);
public Object2ObjectAVLTreeMap<String, Int2ObjectAVLTreeMap<ObjectArrayList<String>>>
        getInterchangeStationNameToColorToRouteNamesMap(boolean includeConnections);
public ObjectAVLTreeSet<Route> getOneInterchangeRouteFromEachColor(boolean includeConnections);

// 来自 AreaBase：
public final ObjectArraySet<Platform> savedRails;   // ★ 车站的站台集合
public boolean inArea(Position pos); public boolean inArea(Position pos, double padding);
public Position getCenter();
```

### 2.3 找车站的兜底链（现有实现，直接复用）

`com.Nanbin.packet.ClientPacketHelper#findStationForSelection(BlockPos)`（`ClientPacketHelper.java:163-184`）：

1. `org.mtr.mod.InitClient.findStation(pos)` —— 区域精确命中；
2. 失败 → `InitClient.findClosePlatform(pos, 256, p -> close = p.area)` —— 最近站台所属站；
3. 再失败 → 遍历 `MinecraftClientData.getInstance().stations`，取 `station.getCenter().manhattanDistance(position)` 最小者。

### 2.4 车站名（单站 / 多站换乘）的文本合成

`RenderCRTRailwaySign.resolveSignText`（`RenderCRTRailwaySign.java:274-281`，与 MTR `RenderRailwaySign.drawSign` 的 station 分支逐字节等价）：

```java
return IGui.mergeStations(selectedIds.longStream()
        .filter(MinecraftClientData.getInstance().stationIdMap::containsKey)   // 过滤非法/过期 id
        .sorted()
        .mapToObj(id -> IGui.insertTranslation(TranslationProvider.GUI_MTR_STATION_CJK,
                TranslationProvider.GUI_MTR_STATION, 1,
                new String[]{ MinecraftClientData.getInstance().stationIdMap.get(id).getName() }))
        .collect(Collectors.toList()));
```

关键语义（全部经 `javap -c` 确认）：

- `TranslationProvider.GUI_MTR_STATION` = `"%s Station"`；`GUI_MTR_STATION_CJK` = `"%s站"`（`assets/mtr/lang/en_us.json`）。
- `IGui.insertTranslation(holderCjk, holderLatin, int insertIndex, String... args)`：
  把 `args` 按 `"|"` 拆成 CJK 行 / 拉丁行；**某行参数全是 CJK 才用 CJK 模板**，否则用拉丁模板；行与行之间用 `"|"` 连接；`args.length < insertIndex` 时返回 `""`。
- `IGui.mergeStations(List<String>)`：每站再按 `"|"` 拆语言行，逐行用 `GUI_MTR_SEPARATOR_CJK`/`GUI_MTR_SEPARATOR`（en_us 均为 `/`）连接多站，行间用 `"|"` 拼接。
- 空名保护：`IGui.textOrUntitled(String)`（空串 → `GUI_MTR_UNTITLED.getString()`，en_us = `Untitled`）。
- 纯文本场景（不要换行）：`IGui.formatStationName(s)`，字节码就是 `s.replace('|', ' ')`。

### 2.5 换乘站列表怎么给 UI

```java
final ObjectArraySet<Station> stations = new ObjectArraySet<>(station.connectedStations);
stations.add(station);
ObjectArraySet<DashboardListItem> items = MinecraftClientData.convertDataSet(stations);
// 过滤（可选）：MinecraftClientData.getFilteredDataSet(TransportMode, ObjectArraySet<T>)
```

（`convertDataSet` / `getFilteredDataSet` 见 `MinecraftClientData` 公开静态方法；仓库 `RailwaySignScreenMixin.java:97-107`、`CRTSignScreen.java:265-267` 均如此用。）

### 2.6 车站颜色

- `Station#getColor()` 是 RGB（内部已 `& 0xFFFFFF`；**未设置时默认 0 即黑色**，`NameColorDataBaseSchema` 构造器未给 color 赋初值）。
- 更安全的取色：`InitClient.getStationColor(BlockPos)`——字节码：`pos == null || findStation(pos) == null ? 0x7F7F7F : station.getColor()`（0x7F7F7F = 8355711）。
- 阴影/朝向调色：`RenderRouteBase.getShadingColor(Direction, int)`（客户端渲染工具，仓库 `RenderCRTStationName1.java:94` 在用）。

---

## 3. 站台数据

### 3.1 数据入口

```java
Station station = ...;                       // §2.3
station.savedRails                            // ObjectArraySet<Platform>（AreaBase 字段）
MinecraftClientData.getInstance().platformIdMap.get(platformId)   // Platform
```

### 3.2 `Platform` 能取到什么

```java
// org.mtr.core.data.Platform extends SavedRailBase<Platform, Station> → SavedRailBaseSchema → NameColorDataBase
public final ObjectAVLTreeSet<Route> routes;  // ⚠ 服务端数据；客户端 sync 期间会被清空重建，勿作实时源
public final IntAVLTreeSet routeColors;       // 经过该站台的线路颜色集合

public long   getId();            // NameColorDataBase
public String getName();          // 站台名（如 "1"、"2A"）
public int    getColor();         // 站台颜色（RGB）
public String getStationName();   // 字节码：area == null ? "" : ((Station) area).getName()
public long   getDwellTime();  public void setDwellTime(long);

// 来自 SavedRailBase：
public U area;                    // 所属车站（Platform.area 可 instanceof Station）
public boolean isValid();  public boolean containsPos(Position);  public Position getMidPosition();
```

### 3.3 `PIDSConfigScreen.getPlatformsForList`（MTR 官方站台列表，直接复用）

```java
public static ObjectImmutableList<DashboardListItem> getPlatformsForList(ObjectArrayList<Platform> platforms);
```

反汇编行为（`PIDSConfigScreen`）：

1. 先 `Collections.sort(platforms)`（**原地排序**，传副本进去）；
2. 每项 `new DashboardListItem(platform.getId(), name, 0)`，`name` 由 `makeConcatWithConstants` 生成：
   `platform.getName() + " " + IGui.mergeStations(该站台所属各 SimplifiedRoute 的终点站名)`；
   - 终点站名 = `SimplifiedRoutePlatform#getStationName()`（取 `route.getPlatforms()` 最后一个元素，`Utilities.getElement(list, -1)` 支持负下标）；
   - 过滤条件：`simplifiedRoute.getPlatformIndex(platform.getId()) >= 0`。

用法（仓库 `ClientPacketHelper.java:149-155` / `CRTSignScreen.java:263`）：

```java
final ObjectImmutableList<DashboardListItem> platforms =
        PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(station.savedRails));
```

### 3.4 `DashboardListItem` 构造方式

```java
// org.mtr.mod.screen.DashboardListItem implements Comparable<DashboardListItem>
public final long id;                    // 选择结果写进 selectedIds 的值
public final NameColorDataBase data;     // 用构造器②时非 null
private final String name;
private final int color;
public DashboardListItem(long id, String name, int color);   // ★ 自定义列表用这个
public DashboardListItem(NameColorDataBase data);            // id=name/color 取 data 的
public String getName(boolean ignoreCache);   // 直接返回字段
public int    getColor(boolean ignoreCache);  // 直接返回字段
// compareTo = name+color+id 拼串后小写比较（影响列表排序，不影响选择）
```

### 3.5 注意

- `station.savedRails` 可能为空（未画站台）→ `getPlatformsForList` 返回空列表，编辑器应显示「无数据」而不是崩溃。
- `Platform#getName()` 可能为 `""`；`Platform#getStationName()` 依赖 `area`，可能为 `""`。
- 客户端 `platform.routes` 不可靠（见 0.3）；需要「站台经过哪些线路」时用 `simplifiedRoutes` + `getPlatformIndex(platformId)` 反查。

---

## 4. 出口数据

### 4.1 `StationExit` / `Station#getExits`

```java
// org.mtr.core.data.StationExit extends StationExitSchema implements Comparable<StationExit>
public String getName();                                   // 如 "A"、"A1"、"B2"
public ObjectArrayList<String> getDestinations();          // 目的地文字列表
public void setName(String);
public int compareTo(StationExit);                         // 按名称排序

// Station
public ObjectArrayList<StationExit> getExits();
```

> `StationExit` **没有 id 字段**，也没有颜色；出口的“编号/字母”就是 `getName()`。

### 4.2 `EditStationScreen` 两个静态工具（MTR 官方，直接复用）

```java
// 1) 取出口列表；includeParentExits=true 时额外生成“单字母父出口”
public static ObjectArrayList<StationExit> getStationExits(Station station, boolean includeParentExits);

// 2) 转成选择器数据：id = serializeExit(exit.getName())，name = 出口名 + 目的地摘要
public static ObjectArrayList<DashboardListItem> getExitsForDashboardList(
        ObjectArrayList<StationExit> exits);

// 3) 反序列化（public）：long → 出口名字符串（每 8 bit 一个字符，低位是第一个字符）
public static String deserializeExit(long value);
```

反汇编细节：

- `getStationExits(station, true)` 的 `lambda$getStationExits$13`：对每个原始出口，取 `name.substring(0,1)` 作为父字母，若尚未建过则 `new StationExit()`+`setName(字母)` 加入结果，并把子出口的 `destinations` 合并进父出口；**随后仍会把原始出口也加入结果**。所以结果里同时有 `A`（合成）与 `A1/A2`（真实）。`false` 时只返回原始出口。
- `serializeExit`（private，字节码可见）：`for (char c : name.toCharArray()) l = (l << 8) + c;` —— **每字符只保留 8 bit**，中文/全角字符会丢位。出口名请只使用 ASCII 字母数字。
- `getExitsForDashboardList` 的 `name` 格式（`lambda$getExitsForDashboardList$14`）：`exit.getName()`；
  有目的地时追加 `"|" + destinations.get(0)`；若目的地多于 1 个再加 `"|(+" + (size-1) + ")"`；`color = 0`。
- 另一个 private `formatExitName(String)` = `name.split("\\|")[0]`，只用于显示。

### 4.3 出口字母怎么渲染（MTR 做法）

`RenderRailwaySign.drawSign` 的 exit 分支（反汇编）：先 `EditStationScreen.getStationExits(station, true)`，再按 `selectedIds`（值为 `serializeExit(name)`）过滤，对剩下每个出口取

```java
DynamicTextureCache.instance.getExitSignLetter(
        exit.getName().substring(0, 1),    // 字母
        exit.getName().substring(1),       // 余下编号（可为 ""）
        color);
```

所以：**出口名的第一个字符 = 大字母，其余 = 小字编号**。自己绘制时照此拆分；选择器里保存/回读 `selectedIds` 时用 `serializeExit`/`deserializeExit` 成对操作。

### 4.4 用法示例（≤10 行）

```java
final Station station = ClientPacketHelper.findStationForSelection(signPos);
if (station == null) { exitsForList = ObjectImmutableList.of(); return; }
exitsForList = new ObjectImmutableList<>(
        EditStationScreen.getExitsForDashboardList(EditStationScreen.getStationExits(station, true)));
// 回读选中出口：
for (long id : selectedIds) {
    final String name = EditStationScreen.deserializeExit(id); // "A" / "A1"
    final String letter = name.isEmpty() ? "" : name.substring(0, 1);
}
```

---

## 5. 选择屏复用：`DashboardListSelectorScreen`

### 5.1 类与构造器（全部签名）

```java
// org.mtr.mod.screen.DashboardListSelectorScreen extends MTRScreenBase implements IGui
//   MTRScreenBase extends org.mtr.mapping.mapper.ScreenExtension
public DashboardListSelectorScreen(
        ObjectImmutableList<DashboardListItem> allData,
        LongCollection selectedIds,               // 会被【直接修改】（引用保存）
        boolean isSingleSelect,
        boolean canRepeat,
        ScreenExtension parentScreen);            // 返回时恢复的父屏幕

public DashboardListSelectorScreen(
        Runnable onClose,                         // 关闭（Done/Esc）后回调
        ObjectImmutableList<DashboardListItem> allData,
        LongCollection selectedIds,
        boolean isSingleSelect,
        boolean canRepeat,
        ScreenExtension parentScreen);
```

构造器①= 以 `onClose=null` 委托②（字节码 `aconst_null` 后 `invokespecial <init>(Runnable,...)`）。

### 5.2 参数语义（反汇编确认）

| 参数 | 语义 |
|---|---|
| `allData` | 左侧「可选」列表的全部条目（`ObjectImmutableList`，不可变） |
| `selectedIds` | **直接持有引用**；`onAdd` → `selectedIds.add(item.id)`；`onDelete` → `selectedIds.rem(item.id)`（若 `canRepeat && selectedIds instanceof LongList` 则 `removeLong(index)`） |
| `isSingleSelect` | true 时 `onAdd` 先 `selectedIds.clear()`，再 add → 单选 |
| `canRepeat` | false 时左侧列表**排除**已在 `selectedIds` 中的项（同一 id 不能加两次）；true 时允许重复（Dashboard 用） |
| `parentScreen` | 交给 `MTRScreenBase(ScreenExtension)` 保存为 `previousScreen`；`onClose2` 时恢复 |

> 选择器**不会发送任何网络包**，也不会主动通知调用者“数据变了”；变化只体现在 `selectedIds` 引用里。

### 5.3 回调时机（字节码逐条）

```java
// DashboardListSelectorScreen#onClose2()
super.onClose2();                       // = MTRScreenBase.onClose2()：
                                        //   ScreenExtension.onClose2();
                                        //   MinecraftClient.openScreen(previousScreen)
if (this.onClose != null) this.onClose.run();   // ★ 在父屏幕恢复之后才执行
```

- `buttonDone`（Done 按钮）的按下回调 = `onClose2()`（`lambda$new$5`）。
- `init2()` 末尾以及每次 `onAdd/onDelete` 后都会 `updateList()`，列表实时刷新，**不关闭屏幕**。
- `updateList()`：左列 = `allData` 中未被选中（`canRepeat=false` 时）的项；右列 = 按 `selectedIds` 迭代，从 `allData` 里 `findFirst(id==)` 映射出来的项。**若某个 id 不在 `allData` 中，它仍留在 `selectedIds` 里，但右列不显示**（陷阱，见 §10.6）。
- `isSingleSelect=false / canRepeat=false` 是导视牌编辑的常用组合（仓库全部如此）。

### 5.4 从自定义屏幕打开 + 返回后如何知道数据变了

**方式 A（仓库现状，推荐）**：把自己持有的同一个 `LongAVLTreeSet` 传进去，关闭后引用内容已变；在**自己的** `onClose2/tick` 里重新读取。

```java
// CRTSignScreen.java:579-585
private void openDataSelector(ObjectImmutableList<DashboardListItem> list) {
    if (list == null || list.isEmpty()) return;
    MinecraftClient.getInstance().openScreen(
            new Screen(new DashboardListSelectorScreen(list, selectedIds, false, false, this)));
}
// 自己的 onClose2（CRTSignScreen.java:314-321）：saveNow() 把 selectedIds 发回服务端
```

**方式 B**：用带 `onClose` 的构造器，在回调里发包/刷新（回调发生在父屏幕恢复之后，可安全发包）：

```java
new DashboardListSelectorScreen(
        () -> Registry.sendPacketToServer(new PacketUpdateCRTRailwaySignWallData1(pos, signIds, selectedIds)),
        list, selectedIds, false, false, parent);
```

**方式 C（MTR 原版界面）**：`RailwaySignScreen` 自己持有 `selectedIds`，选择器关闭后仍停留在原屏幕；数据包在屏幕关闭/保存时统一发。仓库 `RailwaySignScreenMixin#nanbin$openRouteNumberSelector`（`RailwaySignScreenMixin.java:125-159`）即：

```java
MinecraftClient.getInstance().openScreen(new Screen(new DashboardListSelectorScreen(
        new ObjectImmutableList<>(nanbin$stationsForList), selectedIds, false, false,
        (ScreenExtension) (Object) this)));
// ci.cancel(); 阻止 MTR 原来的逻辑
```

### 5.5 判断“文件是否变化”的现有手段

- 仓库 `CRTSignScreen` 用内容签名 `signature()`/`lastSavedSignature` 与 `markChanged()`（`CRTSignScreen.java:289, 400-428`），`refreshIfStale()` 在 `init2/tick2` 里调用；不是依赖选择器回调。
- `RailwaySignScreenMixin` 直接用共享的 `selectedIds/signIds`，保存时统一 `PacketUpdateRailwaySignConfig`。
- 结论：选择器**没有值变化事件**；要么共享集合，要么用 `onClose` Runnable，要么自己维护签名比较。

---

## 6. 下一站 / 上一站 / 当前站

### 6.1 客户端能拿到线路站点顺序吗？——能

```java
// SimplifiedRoute
public ObjectArrayList<SimplifiedRoutePlatform> getPlatforms();  // 有序，下标 0..n-1
public int getPlatformIndex(long platformId);                    // 命中返回下标，否则 -1
public Route.CircularState getCircularState();                   // NONE / CLOCKWISE / ANTICLOCKWISE

// SimplifiedRoutePlatform
public long   getPlatformId();      public long   getStationId();
public String getStationName();     public String getDestination();  // 该站台在本线路的终点方向文字
// extends InterchangeColorsForStationName：
public void forEach(InterchangeColorsForStationName.ColorConsumer);  // 换乘颜色/线路名遍历
```

> 分叉线路在 MTR 4 里会被拆成多条 `SimplifiedRoute`（同一颜色可能出现多条），`getPlatformIndex` 先命中者胜；需要精确分支时用仓库 `com.Nanbin.client.Drawing.LineMapDataResolver.resolve(platformId)`（`LineMapDataResolver.java:33`）——它已实现主干/支线站点序列与换乘色解析。

### 6.2 现有 `findNextStationName` 的实现（`RenderCRTStationInfo1.java:272-296`）

```java
final LongAVLTreeSet firstLineSelected = getFirstLineSelected(entity);   // selectedIds.get(0)
final long platformId = firstLineSelected.firstLong();                   // 注意：StationInfo 里存的是【站台 id】
for (final SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
    final int index = route.getPlatformIndex(platformId);
    if (index >= 0) {
        if (index + 1 < route.getPlatforms().size())
            return route.getPlatforms().get(index + 1).getStationName();
        if (route.getCircularState() != Route.CircularState.NONE && !route.getPlatforms().isEmpty())
            return route.getPlatforms().get(0).getStationName();          // 环线回绕
        return null;                                                      // 终点站
    }
}
```

上一站（同文件 `327-348`）：`index > 0 → get(index-1)`；`index == 0 且环线 → get(size-1)`。
线路颜色（`299-316`）：`route.getColor() | 0xFF000000`。

### 6.3 可复用结论

| 数据 | 路径 | 前提 |
|---|---|---|
| 下一站/上一站站名 | 上面的 `getPlatformIndex + getPlatforms()` 算法 | 方块实体里要存**站台 id**（而非线路颜色），且该站台在 `simplifiedRoutes` 中 |
| 当前站 | `InitClient.findStation(pos)` / `ClientPacketHelper.findStationForSelection(pos)`；或已存 platformId → `platform.getStationName()` | 客户端数据已同步 |
| 终点方向 | `route.getPlatforms().get(last).getDestination()`（仓库 `JSSignEngine#getRouteDestination` 也这样写） | 客户端数据已同步 |
| 是否环线 | `route.getCircularState() != Route.CircularState.NONE` | — |

> `findNextStationName` 用 `firstLong()` 取“最小站台 id”，多行/多站台选择时结果由最小 id 决定——这是现有实现的隐含假设，新编辑器若要“每行独立方向”，请为每行单独存 platformId，而不是共用一个扁平 `selectedIds`。

---

## 7. 颜色工具（MTR 4.0.5 实际有什么）

### 7.1 结论先说

- **MTR 4.0.5 没有 `org.mtr.mod.data.Colors` 类，也没有 `IGui.colorFromInt` / `Colors.getColor`。**
  已对 `MTR-fabric-4.0.5+1.20.4.jar` 解包后的 5558 个 `org/mtr/**/*.class` 做常量池扫描：无 `colorFromInt`、无 `org/mtr/**/Colors` 类；`ARGB_*` 常量只出现在 `org.mtr.mod.data.IGui`。
- 现成的颜色能力 = `IGui` 的 `public static final int` 常量 + `NameColorDataBase#getColor()`（已 `&0xFFFFFF`）+ 位运算 + MTR 生成纹理的 API（`DynamicTextureCache#getColorStrip/getRouteSquare`）。

### 7.2 `org.mtr.mod.data.IGui` 常量（`javap -p -constants` 实测值）

| 常量 | 十进制 | 十六进制 | 含义 |
|---|---|---|---|
| `RGB_WHITE` | 16777215 | `0x00FFFFFF` | 纯白（无 alpha） |
| `ARGB_WHITE` | -1 | `0xFFFFFFFF` | 不透明白 |
| `ARGB_BLACK` | -16777216 | `0xFF000000` | 不透明黑 |
| `ARGB_WHITE_TRANSLUCENT` | 2147483647 | `0x7FFFFFFF` | 50% 透明白 |
| `ARGB_BLACK_TRANSLUCENT` | 2130706432 | `0x7F000000` | 50% 透明黑 |
| `ARGB_LIGHT_GRAY` | -5592406 | `0xFFAAAAAA` | 浅灰 |
| `ARGB_GRAY` | -10066330 | `0xFF666666` | 灰 |
| `ARGB_BACKGROUND` | -15592942 | `0xFF121212` | 面板底色 |
| `MAX_LIGHT_INTERIOR` | 15728816 | `0x00F000B0` | MTR 内部“全亮”lightmap 值（取光值用 `GraphicsHolder.getDefaultLight()` 更稳） |
| 其他布局常量 | — | — | `SQUARE_SIZE=20`、`PANEL_WIDTH=144`、`TEXT_HEIGHT=8`、`TEXT_PADDING=6`、`TEXT_FIELD_PADDING=4`、`LINE_HEIGHT=10`、`SMALL_OFFSET_16=0.05f`、`SMALL_OFFSET=0.003125f` |

### 7.3 ARGB / RGB 处理规则

- 数据层颜色（`NameColorDataBase#getColor()`、`SimplifiedRoute#getColor()`）**一律是 RGB**：前者字节码 `(int)(color & 0xFFFFFF)`，后者同样 `& 0xFFFFFF`。
- 转 ARGB：`int argb = rgb | 0xFF000000;`
- 拆分量：`r = (argb >> 16) & 0xFF; g = (argb >> 8) & 0xFF; b = argb & 0xFF; a = (argb >>> 24) & 0xFF;`
- `NameColorDataBase#getColorHex()`：6 位 hex 字符串（`Utilities.numberToPaddedHexString(color, 6)`），可直接给 `#RRGGBB` 文本控件。
- 选择集里的颜色是 `(long) route.getColor()`（`i2l`，`getColor()` 返回正的 RGB，故不会负数扩展；但历史/混合来源可能是 ARGB 或站台 id，比较时先 `& 0xFFFFFF`，参考 `JSSignEngine#findRoute`/`addColorIfRouteExists`）。

### 7.4 MTR 现成的颜色纹理生成（避免自己画色块）

```java
// org.mtr.mod.client.DynamicTextureCache（客户端独有）
public static DynamicTextureCache instance;
public DynamicResource getColorStrip(long platformId);                             // 该站台所经线路的多色色条
public DynamicResource getRouteSquare(int color, String text, IGui.HorizontalAlignment align); // 线路号方块
public DynamicResource getStationName(String name, float maxWidth);
public DynamicResource getExitSignLetter(String letter, String number, int color);
public DynamicResource getRouteMap(long platformId, boolean, boolean, float, boolean);
public DynamicResource getSignText(String text, HorizontalAlignment, float, int, int);
public DynamicResource getTextPixels(...); public void refresh(); public void reload(); public void tick();

// DynamicResource：public final int width; public final int height; public final Identifier identifier;
```

用法：`DynamicTextureCache.instance.getColorStrip(platformId).identifier`，在 `MainRenderer.scheduleRender(identifier, ...)` 里画。
`getColorStrip` 内部转 `RouteMapGenerator.generateColorStrip(long)`（客户端独有）。
**参数语义（字节码确认）**：`getColorStrip` / `getRouteMap` 的 `long` 参数是 **platformId**——内部走 `RouteMapGenerator.getRouteStream(long, ...)`，用 `SimplifiedRoute#getPlatformIndex(long) >= 0` 过滤后收集线路颜色，**不是线路颜色、也不是线路 id**。只给单条线路画色条请自绘矩形，或用 `getRouteSquare(route.getColor(), 线路号/简称, alignment)`（`generateRouteSquare` 内部会 `| 0xFF000000`，因此传 RGB 即可）。

### 7.5 其他可取的颜色

- `InitClient.getStationColor(BlockPos)`：无站时 `0x7F7F7F`。
- `RenderRouteBase.getShadingColor(org.mtr.mapping.holder.Direction, int)`：按朝向给颜色加阴影（客户端渲染工具）。
- `Platform#getColor()` / `Platform#routeColors`（`IntAVLTreeSet`，经过该站台的线路 RGB 集合）。

---

## 8. 服务端数据解析：现有反射路径汇总（可复用）

### 8.1 为什么必须服务端解析

见 §0.2：常规 `get_data` 响应（`DataResponse`）只给 `SimplifiedRoute`；`Route#getRouteNumber()` 不在其中。仓库所有需要“真实线路编号”的方块实体都在**服务端**解析一次、写入 NBT、随方块实体同步给客户端。

### 8.2 通用反射模板（4 步，仓库 5 处代码逐字相同）

```java
final Field mainField = Class.forName("org.mtr.mod.Init").getDeclaredField("main");
mainField.setAccessible(true);
final Object main = mainField.get(null);                    // null：客户端 / 服务器未启动
final Field simulatorsField = main.getClass().getDeclaredField("simulators");
simulatorsField.setAccessible(true);
final Iterable<?> simulators = (Iterable<?>) simulatorsField.get(main);
```

MTR 侧对应签名：

```java
// org.mtr.mod.Init（双端）
private static org.mtr.core.Main main;
public static org.mtr.core.data.Position blockPosToPosition(org.mtr.mapping.holder.BlockPos);
public static org.mtr.mapping.holder.BlockPos positionToBlockPos(org.mtr.core.data.Position);
public static org.mtr.mapping.holder.BlockPos newBlockPos(double, double, double);
public static final org.apache.logging.log4j.Logger LOGGER;

// org.mtr.core.Main
private final ObjectImmutableList<org.mtr.core.simulation.Simulator> simulators;

// org.mtr.core.simulation.Simulator extends org.mtr.core.data.Data
```

### 8.3 仓库现有可复用方法（按用途）

| 方法 | 位置 | 输入 → 输出 |
|---|---|---|
| `BlockEntityCRTRailwaySign#updateRouteNumbersFromServer()` | `BlockCRTRailwaySign.java:355-410` | 方块位置 + selectedIds 颜色 → `routeNumbers[color]=number`；NBT `crt_route_colors/crt_route_numbers` |
| `BlockCRTRailwaySignWall1.BlockEntity#updateRouteNumbersFromServer()` | `BlockCRTRailwaySignWall1.java:303-335` | 同上；NBT `wall_route_colors/wall_route_numbers` |
| `BlockCRTStationName1.BlockEntity#updateRouteNumberFromServer()` | `BlockCRTStationName1.java:108-143` | `platformId` → 单个 `routeNumber`（NBT `routeNumber`） |
| `BlockCRTStationInfo1.BlockEntity#updateRouteNumbersFromServer()` | `BlockCRTStationInfo1.java:370-421` | `platformId` 集合 → `routeNumbers[platformId]=number`（NBT 同 `crt_route_*`） |
| `BlockCRTStationInfo1Double.BlockEntity#updateRouteNumbersFromServer(boolean front)` | `BlockCRTStationInfo1Double.java:405-...` | 同上（双面各自一份） |
| `BlockCRTAPGDoor1/2#predictNextArrival()` | `BlockCRTAPGDoor1.java:134-199` | 另类反射：`data.platformIdMap.values()` 遍历 + 反射 `minPosition` 字段（字段在 4.0.5 的 `SavedRailBase` 上不存在，异常被吞，属**反面案例**，不要照抄位置反射） |

### 8.4 解析目标数据（服务端 `Data` 侧 API）

```java
Simulator sim = ...;                        // 强转 (Data) sim
sim.stations / sim.stationIdMap             // Station
sim.platforms / sim.platformIdMap           // Platform
sim.routes / sim.routeIdMap                 // 完整 Route（有 routeNumber）
station.inArea(Position)                    // 位置 → 所属站
station.savedRails                          // 站台
platform.routes                             // 经过该站台的完整 Route
route.getRouteNumber()                      // ★ 客户端拿不到的那个字段
route.getRoutePlatforms()                   // RoutePlatformData；getPlatform() 取站台
route.getColor() / getName() / getId()      // 颜色/名/id
```

### 8.5 触发时机（现有约定）

1. 只在服务端：`if (getWorld2() == null || getWorld2().isClient()) return;`
2. 触发点：`setData(...)`（玩家保存编辑）、`setPlatformId(...)`、`setSignLayout(...)`、`setCellColors(...)`、`setSelectedIdsLine(...)` 等；
3. 解析完 `markDirty2()` → 方块实体 NBT 随包同步给附近客户端；
4. 解析失败/`main == null` 时保留空 map 并 `LOGGER.error`，渲染端必须有 `""` 兜底。

### 8.6 若需要“MTR 侧改名/改编号后自动刷新”

现有 NBT 方案只在玩家编辑时解析一次，MTR 里改线路编号后要等下次编辑。建议（按代价从低到高）：

1. **保持现状**：接受陈旧，直到玩家再次保存；
2. 服务端定时（如每 5–10 秒，或 MTR 数据变更时）重跑一次解析并 `markDirty2()`（注意别每次 tick 跑）；
3. 新增 S2C 包：参考仓库 `com.Nanbin.packet.PacketUpdateCRTRailwaySignWallData1` + `Init.java:41-58` 的 `REGISTRY.registerPacket(...)` 与 `com.Nanbin.mapping.Registry#sendPacketToServer/sendPacketToClient` 写法，包体建议：
   `PacketSyncSignRouteNumbers(BlockPos pos, long[] keys, String[] numbers)`（key 可为颜色或站台 id，按方块类型约定）。

---

## 9. 结论表：编辑器元素 → 数据路径

### 9.1 元素绑定表

| 元素 | 主数据路径（客户端） | 可直取？ | 需要服务端配合？ | 备注 |
|---|---|---|---|---|
| 线路名 | `MinecraftClientData.getInstance().simplifiedRoutes` → `SimplifiedRoute#getName()`，显示前 `split("\\|\\|")[0]` | ✅ | ❌ | 空名用 `IGui.textOrUntitled` |
| 线路编号 | ①方块实体 NBT：`getRouteNumber(color)`；②`RouteMapGenerator.resolveRouteData(...)` 兜底（完整 Route / 名字推导） | ⚠ 仅 NBT 同步值 / 推导值 | ✅ **必须**（反射 Simulator → `Route#getRouteNumber()` → NBT） | 编号键：铁路牌=颜色；StationInfo=站台 id |
| 线路色条 | ①单线：自绘矩形或 `getRouteSquare(route.getColor(), 简称, 对齐)`；②站台多线：`DynamicTextureCache#getColorStrip(platformId)`（参数是 platformId） | ✅ | ❌ | `SimplifiedRoute#getColor()` 为 RGB，需 `rgb \| 0xFF000000` |
| 车站名（单站） | `stationIdMap` / `stations` → `Station#getName()`；文本 `IGui.insertTranslation(GUI_MTR_STATION_CJK, GUI_MTR_STATION, 1, name)` | ✅ | ❌ | 空名 → `GUI_MTR_UNTITLED` |
| 车站名（多站换乘） | `station.connectedStations` + `MinecraftClientData.convertDataSet(...)`；文本 `IGui.mergeStations(List<String>)` | ✅ | ❌ | 见 §2.4 完整片段 |
| 站台 | `station.savedRails` → `PIDSConfigScreen.getPlatformsForList(new ObjectArrayList<>(...)))`；`Platform#getId/getName/getStationName/getColor/routeColors` | ✅ | ❌ | 排序/筛选在客户端 |
| 出口编号 | `Station#getExits()` 或 `EditStationScreen.getStationExits(station, true)` → `getExitsForDashboardList(...)`；id=`serializeExit(name)`，反解 `deserializeExit(long)` | ✅ | ❌（选择）；若需“目的地文本最新”，依赖 MTR 自身车站数据同步 | 出口名首字符=字母，其余=编号；仅 ASCII |
| 出口目的地 | `StationExit#getDestinations()` | ✅ | ❌ | 可能为空列表 |
| 下一站 | `simplifiedRoutes` + `getPlatformIndex(platformId)` + `getPlatforms()`（+环线回绕） | ✅ | ⚠ 需要方块里**存站台 id**（服务端选择屏写入后随 NBT 同步） | 见 §6.2 |
| 上一站 | 同上（index-1 / 环线尾部） | ✅ | 同上 | 同上 |
| 当前站 | `InitClient.findStation(pos)` / `ClientPacketHelper.findStationForSelection(pos)` / platformId → `Platform#getStationName()` | ✅ | ❌ | 站外指示牌必须走兜底链 |

### 9.2 建议的存储/通信设计（给实现者）

1. **只存 id，不存解析后的文本**：`selectedIds` 已有 MTR 语义（车站 id / 站台 id / 线路色 / `serializeExit`）。新增自由布局时，建议把每个元素存成 `{type, id}`（type 明确是 station/platform/routeColor/exit），避免现在“一套 selectedIds 混装”导致渲染端靠 signId 猜语义。
2. **routeNumber 单独一份 map**：沿用 `long[] keys + String[] numbers`（或 NBT `LongArray + 单字符串 + \u0001 分隔`）——兼容现有读取代码，也避免为每个元素重复存编号。
3. **需要新增包时的推荐**：
   - 只在玩家编辑时用：继续用已有 `PacketUpdate...` + BE `setData` 模式（服务端解析、写 NBT、`markDirty2`），**不需要新包**；
   - 需要 MTR 数据变化主动刷新、或数据不属于某个方块实体（例如全局编辑器缓存）：新增 `PacketSyncSignRouteNumbers`，注册方式照抄 `Init.java:41-58`；
   - 包体只传 `BlockPos/long[] keys/String[] numbers`，不要传整个屏幕状态（MTR 的 `PacketUpdateRailwaySignConfig` 也只传 `BlockPos + LongAVLTreeSet + String[]`，见其字段）。
4. **服务端解析入口复用**：把 §8.2 反射模板抽成 `MtrServerDataResolver`（仅服务端类），提供：
   - `String findRouteNumberAt(BlockPos pos, long routeColor)`
   - `String findRouteNumberForPlatform(long platformId)`
   - `Map<Long,String> findRouteNumbersForPlatforms(BlockPos pos, LongSet platformIds)`
   这样 5 处重复反射代码可收敛（现有各 BlockEntity 实现完全重复）。
5. **不要**把 `MinecraftClientData.routes/routeIdMap` 当稳定数据源（0.2），也不要在客户端尝试用 `PacketRequestData` 取 routeNumber（`DataResponse` 不含该字段）。

---

## 10. 陷阱清单（实现前务必读）

### 10.1 线程 / 每帧性能

- MTR 数据集合（`simplifiedRoutes`、`stations`、`stationIdMap` 等）是**主线程（客户端 tick）** 维护的；界面/渲染里可以直接读，但**不要在 `MainRenderer.scheduleRender(...)` 的 render 回调里做全量遍历/反射/字符串拼接**——回调可能每帧多次执行。
- 现有仓库反例/正例：
  - `RenderCRTRailwaySign.java:162-171` 每帧遍历 `getSelectedIds()` 建 `routeNumberMap`（元素数量很小，可接受）；
  - `RenderCRTStationInfo1.java:272-296` 每帧遍历全部 `simplifiedRoutes` 算下一站——线路多时是 O(N)；
  - 正例：`RenderCRTStationInfo1.java:143-158` 把“站名/上/下一站/颜色”合成**一张纹理**，每帧只画纹理。
- 建议：每个方块实体缓存 `ResolvedSignData`（含文本、颜色、编号、station 列表），失效条件 = `signIds/selectedIds/cellColors` 的 `Arrays.hashCode`/`Set.hashCode` 变化 **或** `MinecraftClientData` 同步版本变化（可在 `tick2` 里比对 `simplifiedRoutes.size() + 各 route hash`，或简单每秒刷新一次，与仓库自定义字体缓存策略一致）。
- 反射（`Class.forName`/`setAccessible`）**只允许服务端、且只在数据变更时**调用；不要放进 tick/render。

### 10.2 文本换行 / 双语分隔符 `|` 与 `||`（MTR 真实规则）

| 分隔 | MTR 行为（字节码证据） | 本仓库约定 |
|---|---|---|
| `\|`（单竖线） | **换行/语言行分隔**：`DynamicTextureCache#getTextPixels` 对 `IGui.textOrUntitled(text)` 做 `split("\\|")`；`IDrawing#drawStringWithFont` 先把 `"\|"` 连续串折叠再 split | 渲染多行：`RenderCRTRailwaySign.java:302` `signText.split("\\|")` 逐行排版 |
| `\|\|`（双竖线） | MTR 内部**复合 key 分隔符**：`Station` 的换乘映射用 `"%s\|\|%s"` 拼 `线路名\|\|编号` 再 `split("\\\\\|\|")` 取回（`Station#lambda$getInterchangeStationNameToColorToRouteNamesMap$2`）；`RouteMapGenerator#generateRouteMap` 用 `"stationName\|\|stationId"` 作 key，绘制时 `key.split("\\\\\|\|")[0]` | 名字含 `\|\|` 时**只显示前半段**：`CustomFontTextureCache#extractVisibleText`（`text.indexOf("\|\|")` 截断，`CustomFontTextureCache.java:352-361`）、`simplifiedRoute.getName().split("\\\\\|\|")[0]`、`JSSignEngine#getRouteName` 同 |
| `\|` 用于出口名 | `EditStationScreen#formatExitName` = `name.split("\\|")[0]`（显示时只取第一段） | 出口选择器/渲染按首字符当字母 |

实践建议：
1. 从 MTR 取到的任何 `getName()`，**先截 `||` 再截 `|`**，再交给换行渲染；
2. 多语言文本交给 MTR 的 `IGui.insertTranslation` / `mergeStations`，不要自己拼 `/`；
3. `Config.getClient().getLanguageDisplay()`（`NORMAL` / `CJK_ONLY` / `NON_CJK_ONLY`）会过滤 `|` 行（`getTextPixels` 的 `lambda$getTextPixels$15`：`isCjk(line) == (display == CJK_ONLY)`）——**语言显示设置改变后必须重建纹理缓存**（仓库 `CustomFontTextureCache` 的 key 未包含该设置，属已知风险，建议新代码把 `getLanguageDisplay()` 加入缓存 key 或在 `Client` 配置变化时清缓存）。

### 10.3 动态纹理缓存

- MTR：`DynamicTextureCache.instance` 是**客户端单例**，资源异步生成（内部 `generatingResources` 队列 + `getResource`），**首次取同一文本可能先返回默认/透明纹理**，下一 tick 才可用；`tick()` 处理过期，`refresh()` 在 `PacketUpdateData.update()` 末尾被调；`reload()` 仅资源重载时用。不要自己建 `Identifier` 重复烘焙同一文本。
- 仓库：`CustomFontTextureCache` 自管 `Map<String, Identifier>`，key 含 `fontType + 文本 + maxWidth + fontSize + textColor + Config.getClient().getDynamicTextureResolution()`（`CustomFontTextureCache.java:49-50`）；**漏掉分辨率/语言显示会导致配置改变后不刷新**。
- 纹理数量会随“站名×颜色×宽度”组合爆炸：编辑器预览应限制缓存 key 维度（例如预览只用一种分辨率），并在屏幕关闭时不必清理（跨屏复用），但**布局数据变化时要让预览纹理 key 变化**（仓库 `CRTSignScreen#refreshIfStale` + `signature()` 即此思路）。
- `RenderCRTStationName1.java:81-85` 每秒 `clearFittedTextureCache()` 是因为 MTR `Render` 早于 `BlockEntity` 加载；**不要模仿成每 tick 清缓存**（会持续重新烘焙，卡顿 + 显存泄漏）。

### 10.4 颜色 alpha

- 数据色是 RGB（0xFFFFFF 掩码），直接当 ARGB 用会得到全透明：**必须 `| 0xFF000000`**（仓库所有 `route.getColor() | 0xFF000000` 均因此）。
- 反向：从 ARGB 取 RGB 用 `& 0xFFFFFF`；比较两色永远先归一化（`JSSignEngine#findRoute`）。
- `selectedIds` 里存颜色时用 `(long) color`（`i2l`）；MTR 自己的 `DashboardListItem` 也是 `long id = color`。**不要**把“颜色”与“站台 id/车站 id”混进同一个集合后交给渲染端猜（现有 MTR 设计靠 signId 决定语义，编辑器请显式分桶）。
- `ARGB_WHITE_TRANSLUCENT = 0x7FFFFFFF` 常被误当成“白色”；它其实是 **50% 透明的白**，画背景请用 `ARGB_BLACK_TRANSLUCENT`/`ARGB_BACKGROUND` 或 `renderBackground`。

### 10.5 客户端/服务端边界

- 在 common 代码里引用 `MinecraftClientData` / `DashboardListSelectorScreen` / `DynamicTextureCache` 会在专用服务器崩溃；本仓库把屏幕/渲染放 `com.Nanbin.client`（参考 `fabric.src.main.java` 包结构）。
- 方块实体里做数据解析时永远先判 `getWorld2() == null || getWorld2().isClient()`（现有 5 处实现一致）。
- `org.mtr.mod.Init.main` 在**纯客户端或服务器未启动时为 null**；反射 4 步必须逐层判空 + `try/catch` 并记录日志（现有实现均如此）。

### 10.6 选择器与集合的坑

- `DashboardListSelectorScreen` **不会**把 `selectedIds` 中“不在 allData 里”的 id 清掉：切站/换数据源后旧 id 仍在集合里。打开前建议 `selectedIds.retainAll(合法 id 集合)` 或每次重建 `LongAVLTreeSet`。
- 同一个 `LongAVLTreeSet` 被多个 sign 元素类型共用时，来回切换 signId 会残留另一种语义的 id（例如先选站台 id 再切线路色）——MTR 原版就是这样，但 CRT 编辑器应改为“每个元素独立集合”或做类型过滤。
- `PIDSConfigScreen.getPlatformsForList` 会 `Collections.sort` **传入的列表**；传 `station.savedRails` 本体可能破坏其顺序假设，务必 `new ObjectArrayList<>(station.savedRails)`。
- `getStationExits(station, true)` 返回列表里字母父出口与真实出口并存，UI 上会出现 `A`、`A1` 两项；若要“只选字母”，用 `name.length() == 1` 过滤或 `getStationExits(station, false)` 自行聚合。

### 10.7 其他

- `InitClient.findStation` 只做区域精确匹配；指示牌常挂在站外，必须走 `ClientPacketHelper.findStationForSelection` 兜底链。
- 数据是**异步同步**：刚进世界/刚打开界面时 `simplifiedRoutes`、`stations` 可能为空；界面应在 `tick2` 里用“签名变化”刷新（仓库 `CRTSignScreen#refreshIfStale`），不要只在构造器里取一次。
- 反射依赖 MTR 自己的字段名 `main` / `simulators`（非 Minecraft 映射名，不受混淆影响，但 MTR 4.0.x 内可能改名）。建议集中封装（§9.2 第 4 点）并在失败时降级为空编号。
- `TranslationProvider` 文本随语言变化；不要把 `getString()` 结果长期缓存到方块/磁盘（只缓存 id/颜色，渲染时再取文本）。

---

## 附录 A：仓库现有实现索引（可直接借鉴）

| 用途 | 文件:行 |
|---|---|
| 车站兜底查找 | `fabric/src/main/java/com/Nanbin/packet/ClientPacketHelper.java:163-184` |
| 站台选择器（单点） | `ClientPacketHelper.java:149-155` |
| 线路选择列表构造（MTR 同款） | `fabric/src/main/java/com/Nanbin/client/Screen/CRTSignScreen.java:269-280`、`client/Screen/JSSignDataScreen.java:88-97` |
| 站名文本合成（MTR 同款） | `fabric/src/main/java/com/Nanbin/client/Render/RenderCRTRailwaySign.java:265-282` |
| 出口列表 | `CRTSignScreen.java:262`、`CRTSignDoubleScreen.java:107` |
| 下一/上一站 | `fabric/src/main/java/com/Nanbin/client/Render/RenderCRTStationInfo1.java:272-348` |
| 线路颜色/编号解析（客户端兜底） | `fabric/src/main/java/com/Nanbin/client/Drawing/RouteMapGenerator.java:72-158` |
| 车站序列/支线解析 | `fabric/src/main/java/com/Nanbin/client/Drawing/LineMapDataResolver.java:33-218` |
| 服务端 routeNumber 反射（颜色键） | `fabric/src/main/java/com/Nanbin/Registry/RegBlock/BlockCRTRailwaySign.java:351-410` |
| 服务端 routeNumber 反射（站台键） | `Registry/RegBlock/BlockCRTStationName1.java:104-143`、`BlockCRTStationInfo1.java:366-421` |
| 选择器打开方式 | `client/Screen/CRTSignScreen.java:579-585`、`client/Screen/JSSignDataScreen.java:186-189`、`mixin/RailwaySignScreenMixin.java:125-159` |
| JS 数据 API（选中 id → 名称/颜色/编号/出口） | `fabric/src/main/java/com/Nanbin/client/JavaScriptSupport/JSSignEngine.java:804-1174` |
| 自定义文本纹理缓存 | `fabric/src/main/java/com/Nanbin/client/Drawing/CustomFontTextureCache.java:42-72, 94-102, 352-361` |

## 附录 B：复核命令

```powershell
$jar = "libs\MTR-fabric-4.0.5+1.20.4.jar"
$jp  = "C:\Program Files\Java\jdk-21.0.10\bin\javap.exe"

# 类签名
& $jp -p -classpath $jar org.mtr.mod.client.MinecraftClientData
& $jp -p -classpath $jar org.mtr.core.data.SimplifiedRoute
& $jp -p -classpath $jar org.mtr.core.data.Station
& $jp -p -classpath $jar org.mtr.core.data.Platform
& $jp -p -classpath $jar org.mtr.core.data.StationExit
& $jp -p -classpath $jar org.mtr.mod.data.IGui
& $jp -p -classpath $jar org.mtr.mod.screen.DashboardListSelectorScreen
& $jp -p -classpath $jar org.mtr.mod.screen.PIDSConfigScreen
& $jp -p -classpath $jar org.mtr.mod.screen.EditStationScreen

# 常量值（含十进制/十六进制）
& $jp -p -constants -classpath $jar org.mtr.mod.data.IGui

# 数据流（get_data 只下发 SimplifiedRoute 的证据）
& $jp -p -classpath $jar org.mtr.core.generated.operation.DataResponseSchema
& $jp -c -p -classpath $jar org.mtr.core.operation.DataResponse | Select-String -Pattern "addRoute" -Context 0,8
& $jp -c -p -classpath $jar org.mtr.mod.packet.PacketUpdateData

# 选择器回调时机
& $jp -c -p -classpath $jar org.mtr.mod.screen.DashboardListSelectorScreen | Select-String -Pattern "onClose2" -Context 0,12
& $jp -c -p -classpath $jar org.mtr.mod.screen.MTRScreenBase

# 颜色工具不存在的核查（解包后扫描）
#   jar xf libs\MTR-fabric-4.0.5+1.20.4.jar org/mtr   （到临时目录）
#   Get-ChildItem -Recurse -Filter *.class | Select-String -Pattern 'colorFromInt'
#   → 无结果；"*Colors.class" 亦无结果
```
