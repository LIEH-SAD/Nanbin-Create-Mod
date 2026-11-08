# 移植台账：Nanbin MTR 4.0 (3.1) → MC 1.20.1 + YMTR 3.6.3

> 本文是所有移植成员的唯一约定来源。开工前必读；发现约定不成立时先改本文，再改代码。

## 1. 方向与两端

| | 来源（只读） | 目标（写入） |
|---|---|---|
| 路径 | `D:\NCM+mtr4.0\Nanbin` | `D:\NCMOD1.20.1\ymtr\Nanbin` |
| 分支 / 提交 | `mtr4.0` @ `e34adcb`（3.1最终功能性提交） | `1.20.1+ymtr`（自 `1.20.1+mtr3` @ `27e355c`） |
| MTR | 4.0.5，`org.mtr.mapping.* / org.mtr.core.* / org.mtr.mod.*` | YMTR 3.6.3，`mtr.*` + 原生 `net.minecraft.*` |
| MC / 映射 | 1.20.1 + Yarn（Stonecutter active=1.20.1） | 1.20.1 + Yarn（Architectury） |
| 构建 | `.\gradlew.bat :fabric:1.20.1:build` | `.\gradlew.bat :common:compileJava` / `build` |
| 权威源码 | `fabric/src/main/java` + `fabric/src/main/resources`（forge 是构建期同步副本） | `common/src/main/java` + `common/src/main/resources` |

**结论：游戏版本与映射一致，移植成本=MTR API 改写 + loader 抽象改写。**

## 2. 硬约定

1. **不修改来源工程任何文件**（含 `git checkout`、格式化、删除）。来源工作区还有未提交改动，必须原样保留。
2. **不修改 `D:\NCM+mtr4.0\Nanbin\forge/**`**（构建期同步产物）。
3. 目标侧只写 `common/src/main/java`、`common/src/main/resources`、`fabric|forge/src/main/**`（平台专属）与 `docs/`、`tools/`。
4. 已移植文件的**包名与类名沿用来源**；来源与目标既有的重命名以 `tools/parity/known-moves.txt` 为准。
5. 注释、中文文案、JSON 键名、NBT 键名**原样保留**。所有文件 UTF-8 无 BOM。
6. 编译门：`.\gradlew.bat :common:compileJava --console=plain` 必须 BUILD SUCCESSFUL；只修自己作用域内的错误。
7. 不提交 git（Lead 统一提交）；不新增测试框架；不跑 `./gradlew build`（太长，除非 Lead 要求）。
8. 已知既有告警（unchecked / 过时 API）不算错误，不要顺手重构。

## 3. 兼容层（`com.Nanbin.mapping`）

MTR 4.0 的映射层在本工程由 4 个薄封装替代。**移植文件中遇到下列 import 一律改到 `com.Nanbin.mapping`：**

| 来源 import | 目标 import | 说明 |
|---|---|---|
| `org.mtr.mapping.mapper.GraphicsHolder` | `com.Nanbin.mapping.GraphicsHolder` | 矩阵+顶点消费者（世界）/ DrawContext（界面）统一上下文 |
| `org.mtr.mapping.mapper.GuiDrawing` | `com.Nanbin.mapping.GuiDrawing` | 矩形/贴图绘制，UV 用归一化 0..1 |
| `org.mtr.mapping.mapper.TextHelper` | `com.Nanbin.mapping.TextHelper` | `translatable/literal` → `MutableText` |
| `org.mtr.libraries.com.google.gson.*` | `com.google.gson.*` | 直接换包 |
| `org.mtr.libraries.it.unimi.dsi.fastutil.*` | `it.unimi.dsi.fastutil.*` | 直接换包 |

`GraphicsHolder` 已实现：`push/pop/translate/scale/rotateXDegrees/rotateYDegrees/rotateZDegrees/drawText(String|Text|MutableText,int,int,int,boolean,int)/drawCenteredText/getDefaultLight()/getTextWidth(String|Text|MutableText)/wrapLines`，构造为 `new GraphicsHolder(matrices, vertexConsumers)` 或 `new GraphicsHolder(drawContext)`。

**签名适配模板**（MTR4 → MTR3）：

```java
// 方块实体渲染器
// MTR4: public void render(T entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay)
// MTR3:
public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
    final GraphicsHolder graphicsHolder = new GraphicsHolder(matrices, vertexConsumers);
    ...
}

// Screen
// MTR4: public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta)
// MTR3:
public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    final GraphicsHolder graphicsHolder = new GraphicsHolder(context);
    ...
}
```

**已完成的 P1 兼容类**：`GraphicsHolder`、`GuiDrawing`、`TextHelper`。
**按需补充**（用到再让 Lead 加，不要各写一份）：`MtrData`（路线编号/平台路线/车站出口/选择列表项）、`VoxelShapes`、`DoorBlockExtension`、`WorldHelper`、`BlockRenderLayerHelper`、`GameRuleRegistry`、`ModLoaderHelper`、`TicketMachineHelper`。

## 4. MTR 4.0 → 3.6.3 常用 API 对照（节选）

| MTR 4.0 | YMTR 3.6.3 |
|---|---|
| `org.mtr.mapping.holder.BlockPos/BlockState/Block/Item/ItemStack/Identifier/Direction/...` | 原版 `net.minecraft.*`（`BlockPos`、`BlockState`、`ItemStack`、`Identifier`…） |
| `org.mtr.mapping.holder.VoxelShape/Box` | `net.minecraft.util.shape.VoxelShape`、`net.minecraft.util.math.Box` |
| `org.mtr.mapping.holder.BlockSettings` | `AbstractBlock.Settings`（`AbstractBlock.Settings.create()`） |
| `org.mtr.core.data.Station/Platform/Route/SimplifiedRoute` | `mtr.data.Station/Platform/Route`；**无 SimplifiedRoute** → 用 `ClientData.DATA_CACHE.requestPlatformIdToRoutes(platformId)` |
| `org.mtr.mod.client.MinecraftClientData` | `mtr.client.ClientData`（静态容器 + `DATA_CACHE`） |
| `org.mtr.mod.data.IGui` | `mtr.data.IGui`（常量与 `textOrUntitled` 等一致） |
| `org.mtr.mod.client.IDrawing` | `mtr.client.IDrawing`（`drawTexture(MatrixStack, VertexConsumer, ...)`） |
| `org.mtr.mod.render.RenderRailwaySign` | `mtr.render.RenderRailwaySign`（`getSign(id)` → `mtr.client.CustomResources.CustomSign`） |
| `org.mtr.mod.render.QueuedRenderLayer` | `mtr.render.RenderTrains.QueuedRenderLayer` |
| `org.mtr.mod.render.StoredMatrixTransformations` | `mtr.render.StoredMatrixTransformations` |
| `org.mtr.mod.resource.SignResource` | `mtr.client.CustomResources.CustomSign` |
| `org.mtr.mod.screen.DashboardListSelectorScreen` | `mtr.screen.DashboardListSelectorScreen`（`List<NameColorDataBase>`） |
| `org.mtr.mod.screen.DashboardListItem` | `mtr.data.DataConverter` |
| `org.mtr.mod.Init` / `InitClient` | 本工程 `com.Nanbin.Init` / `InitClient` + `ClientData.DATA_CACHE` |
| `org.mtr.mapping.registry.Registry` / `PacketHandler` | Architectury `DeferredRegister` + `Identifier` 通道（`com.Nanbin.mapping.Registry.registerReceiverC2S/S2C`） |
| `org.mtr.mapping.mapper.BlockExtension/BlockWithEntity/BlockEntityExtension` | `mtr.mappings.BlockMapper` / `EntityBlockMapper` / `BlockEntityMapper` |
| `org.mtr.mapping.mapper.BlockEntityRenderer<T>` | `mtr.mappings.BlockEntityRendererMapper<T>` |
| `org.mtr.mapping.mapper.DirectionHelper.FACING` | `Properties.HORIZONTAL_FACING`（配 `mtr.block.IBlock.getStatePropertySafe`） |
| `org.mtr.mapping.registry.BlockRegistryObject` | Architectury `RegistrySupplier<Block>` |
| `org.mtr.mod.render.MainRenderer.scheduleRender` | **无等价物** → 在 BER 内直接绘制（见 `common/.../client/Render/RenderCRTRailwaySign.drawTexturedRect`） |

## 5. 阶段与作用域

| 阶段 | 内容 | 负责人 |
|---|---|---|
| P0 | 基线、分支、清单脚本 | Lead（完成） |
| P1 | 兼容层 | Lead（进行中） |
| P2 | `signage/crt/**`、`CRTRailwaySignEditor/editor/{Theme,Font,Presets,PresetCatalog}`、`legacy/**`、`util/TextureGenerator`、`client/Drawing/{LineMapData,LineMapDataResolver}` | data-model |
| P3 | `CRTRailwaySignEditor/render/**`、`client/JavaScriptSupport/**` 新增、`client/Drawing/**` 新增 8 类 | render-eng |
| P4 | `Registry/RegBlock/**`、`RegItem/**`、`client/Render/**` 新增 4 类、注册表接线 | blocks + Lead |
| P5 | `client/Screen/**`、`editor/{Host,HostImpl,Widgets}`、`uiHelper/**` | ui-editor |
| P6 | `packet/**` 17 个、`ClientPacketHelper`、mixin | Lead/ui-editor |
| P7 | 资源/语言/配方/元数据/docs | resources |
| P8 | 集成验收 | Lead |

**禁止交叉写**：`Blocks.java`、`Items.java`、`BlockEntityTypes.java`、`ItemsGroup.java`、`build.gradle`、`nanbin.mixins.json`、`fabric.mod.json`、`mods.toml` 只由 Lead 修改。

## 6. 不要照搬的东西

- `org/`（MTR 4.0 反编译参考 class）、`_sign_ref/`（标定脚本截图）、`*.hprof`、`Temp/`、`libs/`。
- Stonecutter 指令 `//? if ...`（只在 5 个文件 6 处，遇到必须手动展开成 1.20.1 分支）。
- `com/Nanbin/mapping/` 里 11 个 MTR4 loader/版本 shim（`WorldHelper`、`VoxelShapes`、`GameRuleRegistry`、`BooleanGameRule`、`IntegerGameRule`、`ModLoaderHelper`、`DoorBlockExtension`、`DefaultedItemStackList`、`FilterBuilder`、`BlockRenderLayerHelper`、`TicketMachineHelper`）：**按需重写为 Architectury 版本**，不原样复制。
- `Blocks/BlockTags.java`（空壳）、`util/TextureGenerator.java`（AWT 工具，非运行时依赖，最后评估）。

## 7. 验收脚本

```powershell
.\tools\parity\classes.ps1     # 类清单：MISSING 必须降到 0（known-moves 之外的 11 个 mapping shim 除外）
.\tools\parity\resources.ps1   # 资源/语言键清单
```

## 8. 已知源侧缺陷（移植时按本文处理）

1. `assets/nanbin/crt_signs.json` 在源 HEAD 已被删除（提交 `9274aba`），但 `CRTPresetCatalog` 仍按 id 引用其中的 49 个文字牌预设 → **从历史提交 `4834a43` 取回**并放入目标资源。
2. 源 `fabric.mod.json` 的 mod id 是 `lieh-nanbin`，目标沿用 `nanbin`（通道/资源命名空间不受影响，**不要改**）。
3. 源 `Items.java`/`ItemsGroup.java` 比目标短很多（MTR4 注册器自动带销），**不要反向覆盖**目标实现。

## 9. 追加决策（P0–P5a 期间确定）

| # | 决策 | 理由 |
|---|---|---|
| D9 | **不移植 `mixin/CRTSignLoaderMixin`** | YMTR 3.6.3 有正规入口 `mtr.client.CustomResources.registerReloadListener(resourceManager -> ...)`，在 `InitClient` 挂载即可，无需改字节码 |
| D10 | 目标 `RenderCRTRailwaySign` 保留 MTR4 版 `drawSignCRT`/`renderRouteNumberV2Regions` 的**形参签名**（含 `@Nullable StoredMatrixTransformations`），但内部**忽略 smt**、改用 `GraphicsHolder` 直绘 | 3.6.3 的 `StoredMatrixTransformations` 是 `MatrixStack` 语义，与 MTR4 的 `smt.transform(GraphicsHolder, offset)` 不同；保留签名可让 legacy 绘制层零改动 |
| D11 | JS 配置**双轨**保留：新增 `assets/nanbin/crt_js_signs_config.json`（来源 2 个脚本）＋保留 `js_signs_config.json`（目标旧 4 个脚本） | 避免丢失目标既有的 `example_*.js` 示例；`CRTJsSignLoader` 叠加加载、不 clear 现有 loader |
| D12 | 服务端安全审计把 `net.minecraft.client.item.TooltipContext` 加白名单；`mapping/`、`mixin/` 视为客户端允许包 | `TooltipContext` 是原版 tooltip 签名的一部分，两端 loader 通用，不构成专服加载风险 |
| D13 | 语言文件按来源**整文件覆盖**（9 个 locale） | 已核验目标独有键为 0，覆盖最稳；键总数 1538 → 2282 |
| D14 | 贴图/模型以来源覆盖，**保留目标独有**（14 贴图 / 2 js / 1 字体 / 3 根资源） | 目标基线有来源没有的特效贴图与示例脚本 |
| D15 | 三方共用的 78 处上游贴图/模型断链**按来源原样保留**（目标 72，比来源少 6） | 属来源既有缺口，本轮引入 0 处；不在移植期擅自补图 |
| D16 | **不移植来源 `RailwaySignScreenMixin` 里的 `@Redirect(getSortedSignIds)` CRT 标志注入** | 反编译核对：MTR3 `RailwaySignScreen` 的 `allSignIds` 直接来自 `new ArrayList<>(CustomResources.CUSTOM_SIGNS.keySet())` + 排序，凡进 `CUSTOM_SIGNS` 的标志自动出现在原版选择界面，无需改字节码 |
| D17 | 副作用：CRT 标志也会出现在**原版 MTR 导视牌**编辑界面 | MTR3 只有一个 `CUSTOM_SIGNS`（既作渲染查询又作界面列表），无法像 MTR4 那样只写渲染缓存；CRT 界面侧隔离仍由 `CRTSignRegistry` 的 `whitelist`/`getSignIdsFor` 保证 |
| D18 | 来源 `nanbin$editScrollBar`（原版导视牌界面的编辑按钮横向滚动条）**暂不移植** | 纯 GUI 便利性；在无法实机验证的前提下新增 Mixin 注入的崩溃风险高于收益。已列入遗留项，如有需求可在 P8 后单独做 |
| D19 | 数据包采用**通道制**而非来源的「一消息一类」 | 目标既有架构（`PacketHandler` C2S + `PacketOpenScreen` S2C + `ClientPacketHelper` 发送端）已成型；16 个来源 packet 类登记为 known-skip，载荷逻辑逐条搬入 |
| D20 | 新增 `tools/parity/accept.ps1` 一键验收（9 道门） | P8 用；也可随时增量运行 |

## 10. 进度台账

| 阶段 | 状态 | 关键产出 |
|---|---|---|
| P0 基线 | ✅ | 分支、`.gitignore`、4 个 parity 脚本、本台账 |
| P1 兼容层 | ✅ | `GraphicsHolder`/`GuiDrawing`/`TextHelper`/`MtrData`，`@ExpectPlatform` 审计通过 |
| P2 数据模型 + legacy | 🔄 | `signage/crt/**`、`editor/{Theme,Font,Presets,PresetCatalog,Widgets,Host}`、`legacy/**`（`CRTSignRegistry` 进行中） |
| P3 渲染引擎 | ✅ | `render/**` 8 类、`JavaScriptSupport/**` 3 类、`Drawing/**` 6 新增 + 增量、`MtrData` |
| P4 方块/物品/渲染器 | 🔄 | 5 个 mapping helper、7 新方块、`ItemCRTPSD1`、注册表接线已完成；4 个新渲染器与既有渲染器增量进行中 |
| P5 界面 | 🔄 | 控件库/JSON 屏/Host 接口/2 个 uiHelper 完成；编辑器主屏与既有屏进行中 |
| P6 网络 | 🔄 | 6 个新通道 + 4 个 C2S 接收器已接入 `PacketHandler`；剩余 open-screen 包随屏幕落地 |
| P7 资源 | ✅ | 全部资源/语言/配方/文档/许可对齐，`crt_signs.json` 从 `4834a43` 恢复（49 预设） |
| P8 集成 | ⏳ | 双端 build、清单零差异、旧档兼容、服务端安全 |

## 11. 已知遗留项（P8 收尾）

| # | 项 | 状态 |
|---|---|---|
| 1 | 世界渲染「自由布局」分支（`entity.getSignLayout()` → `CRTSignLayoutPainter.bake`/`CRTJsLayoutBake.bake` → 底板+整牌贴图，失败回退 legacy 逐格） | ✅ 已补（`RenderCRTRailwaySign` 1258 行 + `RenderCRTRailwaySignWall1`，JS bake 失败只画底板、非 JS 失败回退 legacy） |
| 2 | JS 整牌预烘焙（`renderJSStyleLine` 14/15 参 + `buildJSContentKey` 指纹 + `JSDrawSink` 短路；命中 1 个 quad，未命中重烘，失败回退实时绘制） | ✅ 已补 |
| 3 | 方块右键 `onUse`：墙挂牌/铁路牌/中间段 → S2C `PACKET_OPEN_BLOCK_ENTITY_SCREEN`；站名三块 → `PACKET_OPEN_CRT_PLATFORM_SCREEN`；自定义色基类同墙挂牌（来源同款兜底） | ✅ 已补 |
| 4 | 原版导视牌界面编辑按钮横向滚动条（D18） | ⏸ 不移植（纯 GUI，Mixin 风险 > 收益） |
| 5 | 上游继承缺口（与来源一致）：`crt_psd_cab_door` 缺 blockstate、`crt_rigid_catenary_1`/`crt_ticket_machine_2` 缺 item 模型、78 处贴图/模型断链 | ⏸ 保留（已登记 `known-resource-gaps.txt`） |
| 6 | `client/Drawing/NanbinSignLoader.java`（已被 `CRTSignRegistry` 取代） | ✅ 已删除 |
| 7 | 编辑器静态走查（`docs/EDITOR_TEST_CHECKLIST.md` §1–§10）：唯一可修项「JS 模式点画布无提示」已修（复用原本未被引用的 `gui.nanbin.crt_editor.js.locked_edit` 键） | ✅ |
| 8 | 撤销不恢复完整选择集、绑定列表打开时快照、`CRTSignDoubleScreen` 保留按行 JS 样式锁定 | 记录（来源既有/基线保留） |

## 12. 版本与产物

- `mod_version = 3.1.261107`，`Init.VERSION_DATA = "0"`、`IS_TEST_VERSION = false`（与来源 3.1 语义对齐）。
- 双端产物：`build/release/Nanbin-3.1.261107+1.20.1+fabric+mtr3.jar`（23.4 MB）、`...forge+mtr3.jar`（21.5 MB）。
- Fabric 端 Nashorn 以 JarInJar 打包（`META-INF/jars/nashorn-core-15.4.jar`）；Forge 端不打包（环境自带）。
- 混入：`nanbin.mixins.json` 维持 2 个（`RailwaySignScreenMixin`/`RenderRailwaySignMixin`），`init` 采用 `init`/`method_25426`/`m_7856_` 三变体 + `require = 0`，无需 refmap（MTR 自有方法名不受映射影响）。
- 客户端初始化顺序（`InitClient`）：`PacketOpenScreen` → `BlockEntityRender` → `BlockColor` → `CRTSignRegistry`（资源重载监听）→ `JSSignLoader`（旧 `js_signs_config.json`，4 个脚本）→ `CRTJsSignLoader`（`crt_js_signs_config.json`，叠加）→ `RouteMapOverride` → `FiltersGroup/FiltersMenu/RenderLayerReg`。

## 13. 验收门（`tools/parity/accept.ps1`，9 道）

| 门 | 命令 | 期望 |
|---|---|---|
| 1 类清单 | `classes.ps1` | MISSING = 0（25 个登记为 known-move/known-skip） |
| 2 资源+语言 | `resources.ps1` | 各分组 missing = 0 |
| 3 注册表×资源×配方 | `registry.ps1` | RESULT: OK（上游缺口登记在 `known-resource-gaps.txt`） |
| 4 静态审计 | `audit.ps1` | `org.mtr.` 残留 0、客户端类泄漏 0、`@ExpectPlatform` 齐 |
| 5 编译 | `:common:compileJava` | BUILD SUCCESSFUL |
| 6 平台编译 | `:fabric:compileJava :forge:compileJava` | BUILD SUCCESSFUL |
| 7 离线渲染 | `:common:drawTest` | 10 张 PNG |
| 8 完整构建 | `build` | BUILD SUCCESSFUL |
| 9 产物 | `build/release/*.jar` + jar 内容抽检 | 双端 jar、`fabric.mod.json`/`nanbin.mixins.json`/`crt_signs.json`/`crt_js_signs_config.json` 在包内 |

## 14. FiltersGroup（创造模式页签过滤条）对齐

目标工程原本已有 `client/FiltersGroup/{FiltersGroup,FiltersMenu}.java`（MTR3 写法：`ziyue.filters.FilterBuilder` + `ItemsGroup.*.getKey()`），但物品清单停留在 3.1 之前。本轮按来源 `fabric/src/main/java/com/Nanbin/client/FiltersGroup/` 对齐（**只改物品清单，不改 API 形态**）：

| 过滤器 | 变化 |
|---|---|
| `CRT_BUILDING_Items` | 补 `CRT_RAILWAY_SIGN_WALL_1/2`；修正 `CRT_RAILWAY_SIGN_3_ODD` 重复登记（来源顺序为 3_EVEN→3_ODD→…） |
| `CRT_TICKETS` | 补 `CRT_TICKET_DISPLAY_1` |
| `CRT_DOOR` | 补 PSD Gen-1 三件：`CRT_PSD_DOOR_1`、`CRT_PSD_GLASS_1`、`CRT_PSD_GLASS_END_1` |
| `COMMON_BUILDING_Items` | 移出 `BUS_TICKET_PROCESSOR`（来源把它归到「功能方块」） |
| `FUNCTION_Items` | 补 `BUS_TICKET_PROCESSOR`（与来源一致） |
| `CRT_FENCE` / `ROAD_Items` / `FENCE_Items` | 与来源逐项一致，未改 |
| `CRT_OVERHEAD_LINES` / `CRT_ESCALATOR` | 来源同样是注释状态，保持不注册 |

- 过滤器名称/提示的 10 个语言键（`FiltersGroup.nanbin.*`）在 9 个 locale 中随 P7 资源已就位，无需新增。
- `FiltersMenu` 的「关于」保留按钮（CRT / 城市建设两个页签）与来源一致。
- 注册顺序：`InitClient.initMenuAndRenderLayers()` → `FiltersGroup::init` → `FiltersMenu::init` → `RenderLayerReg::init`（在 `ItemsGroup` 注册之后，符合过滤器按页签键查找的要求）。

> ⚠️ 版本号注意：本工程用**日期式** `mod_version`（如 `3.1.261107`），来源工程用 `version=3.1.0`。曾出现一次被误改成 `3.1.0`（会改变产物名）。改版本前先确认 `gradle.properties` 的 `mod_version` 与 `build/release/` 命名约定一致。

## 15. Enchantment Glint（附魔光效）对齐

来源用 `org.mtr.mod.item.ItemBlockEnchanted` 标注「站台配色类」方块物品，使物品栏中带附魔流光，便于与普通方块区分。目标沿用 MTR3 的同名类 **`mtr.item.ItemBlockEnchanted`**（`javap` 确认：`extends BlockItem`、覆写 `hasGlint(ItemStack)=true`、构造 `(Block, Item.Settings)`）。

| 物品 id | 来源 | 目标（本轮前 → 后） |
|---|---|---|
| `crt_old_wall_1` | `ItemBlockEnchanted` | `BlockItem`(EPIC) → **`ItemBlockEnchanted`**(EPIC) |
| `crt_old_wall_2` | `ItemBlockEnchanted` | `BlockItem`(EPIC) → **`ItemBlockEnchanted`**(EPIC) |
| `station_color_ceiling` | `ItemBlockEnchanted` | `BlockItem`(UNCOMMON) → **`ItemBlockEnchanted`**(UNCOMMON) |
| `station_color_ceiling_2` | `ItemBlockEnchanted` | `BlockItem`(UNCOMMON) → **`ItemBlockEnchanted`(UNCOMMON) |

- 仅这 4 个（已全量 grep 来源 `fabric/src/main/java` 与资源，无其它 `Enchant`/`hasGlint` 标注）。
- 保留目标原有的 `Rarity` 设置（EPIC/UNCOMMON），只替换物品实现类；创造页签归属不变。
- 不需要额外贴图/模型：光效由 `Item.hasGlint` 在运行时叠加。

## 16. 旧存档导入不再生成「空白占位」（含 mtr4 分支）

**问题**：旧存档（只有格子 `signIds`）在打开导视牌编辑器前会先转换成自由布局，`CRTSignLegacyImporter.convertCell` 对**空白格**会生成一个 `SPACE` 元素（注释为「保留一个格宽，避免其它元素挤在一起」）。结果编辑器预览区里默认就铺满空白占位（墙挂牌尤其明显：`cells = 方块数 × 2`，空格全变占位）。

**改法**（两个仓库同步改）：

| 文件 | 改动 |
|---|---|
| 目标 `common/src/main/java/com/Nanbin/client/CRTRailwaySignEditor/legacy/CRTSignLegacyImporter.java` | 空白格 `return null`（跳过），删除 `CRTSignElementType.SPACE` 占位创建；类 javadoc 同步 |
| 来源 `fabric/src/main/java/com/Nanbin/client/CRTRailwaySignEditor/legacy/CRTSignLegacyImporter.java`（mtr4 分支） | 同上（逐行一致） |

- 影响面：仅「旧数据 → 布局」的一次性转换；不会改动世界渲染的 legacy 回退路径（未保存布局时仍按旧 `signIds` 渲染），也不会改动用户手动放置的 `SPACE` 元素（调色板里的「空白」卡照常可用）。
- 副作用（预期）：导入后的布局里，元素之间不再保留原空格宽度，内容按实际元素从左顺延；如需「只去掉首尾空格、保留中间对齐」的折中方案，可再调整。

## 17. JS 预设不显示：初始化时序回归修复

**现象**：目标版本编辑器「标识牌设置 → JS 样式」下拉、以及原版导视牌界面的 JS 样式选择器里一个脚本都没有（只有「无（自由布局）」）。

**根因**：P8 把 `InitClient` 里的 `NanbinSignLoader::register` 迁到 `CRTSignRegistry` 时，顺手把 JS 加载写成了**立即调用** `JSSignLoader::reload` / `CRTJsSignLoader::reload`。但 Fabric 的 `ClientModInitializer.onInitializeClient` 早于 `MinecraftClient` 实例创建，此时 `MinecraftClient.getInstance()` 为 null，`getResourceManager()` 抛 NPE 被 catch 吞掉 → `JSSignConfig` 一直是空的。基线代码是「注册监听器、资源重载时再加载」，所以之前正常。

**修复**：
1. `JSSignLoader` / `CRTJsSignLoader` 各新增 `register()`：`CustomResources.registerReloadListener(rm -> reload())`（首次资源加载 + 每次 `/reload` 都会触发）；
2. `InitClient` 改为调用两者的 `register()`（不再是 `reload()`）；
3. 两个 `reload()` 增加 `MinecraftClient.getInstance() == null` 的兜底日志与提前返回；
4. `InitClient` 额外挂一个 **首 tick 兜底**：若第一个 tick 时 `JSSignConfig.getAllScripts()` 仍为空，主动补一次 `JSSignLoader.reload() + CRTJsSignLoader.reload()`（日志 `JS sign scripts are still empty after resource reload - reloading now`）。

**验证方式（实机）**：启动后日志应出现 `Loaded JS sign script: ...`（4 条旧配置 + 2 条 CRT 配置，同名 id 后者覆盖）；编辑器右栏「JS 样式」应列出 `crt_station_entrance`、`example_clock`、`example_station_info`、`example_multi_screen` 四项。
- 来源（mtr4 分支）无此问题：它的 `CRTJsSignLoader.reload()` 由 `CRTSignLoaderMixin` 钩 MTR 资源重载，时机正确，**不需要改**。

## 18. RenderCRTAPGDoor1/2 渲染位置对齐

对照来源 `fabric/src/main/java/com/Nanbin/client/Render/RenderCRTAPGDoor1.java` / `RenderCRTAPGDoor2.java`，本轮把目标侧的**几何/位置**差异对齐：

| 项 | 目标（改前） | 目标（改后，与来源一致） |
|---|---|---|
| Door1 门顶指示灯模型 | 灯条 `addBox(-8,-1.5,-5.2, 4,1,1,0.05)` **+** 一个旋转小方块（遮光片，pivot (0,-9.05,-4.95)、rot 0.3927、`addBox(-0.5,0.05,-3.05,1,1,3,0.05)`） | 只保留 `addBox(-8,-1.5,-5.2, 3,1,1,0.05)`（去掉多余遮光片，灯条宽度 4→3） |
| Door1 指示灯贴图 | `crt_apg_door_light_<x>_old.png` | `crt_door_light_<x>.png`（来源 eb24848 改名后的共享灯贴图） |
| Door1 指示灯变换 | `translate(-0.5,0,0)` **+ `scale(0.5,1,1)`** | 只 `translate(-0.5,0,0)`（来源该 scale 已注释，不缩放） |
| Door2 站台面地灯模型 | 厚 1px（`16×1×16`） | 厚 0（`16×0×16`，贴地）—— 来源 3.1 最终提交把 `16×9` 改为 `16×0`，目标从 1px 对齐到 0 |

已逐项核对一致、未改：Door1/Door2 其余模型（门页 bottom/top、地灯、锁定牌、`ModelSingleCube`）、所有 pivot/rotation、门开合平移、指示灯状态机（含列车接近闪烁）、贴图命名（Door2 的 `crt_apg_door_light_*` / `crt_apg_door_2_*`）、门号铭牌（来源两处 `renderDoorNumberPlate` 的绘制代码**均被注释**，目标同样不绘制）。

保留差异（行为、非位置）：来源 Door2 的站台面地灯固定使用 `crt_ground_light_off(_flipped)` 且不随状态变化；目标 Door2 仍按 Door1 的状态机显示绿/黄。如需完全对齐来源可再调。

> 来源 HEAD 说明：任务开始时为 `ed04f13`，后被来源作者 amend 为 `e34adcb`；差异仅「删除 6 个误提交的 `.hprof.p*` + `NanbinConfigScreen` +2 行 + `fabric.mod.template.json` −1 行」，不影响已移植内容。
