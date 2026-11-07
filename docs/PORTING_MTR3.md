# 移植台账：Nanbin MTR 4.0 (3.1) → MC 1.20.1 + YMTR 3.6.3

> 本文是所有移植成员的唯一约定来源。开工前必读；发现约定不成立时先改本文，再改代码。

## 1. 方向与两端

| | 来源（只读） | 目标（写入） |
|---|---|---|
| 路径 | `D:\NCM+mtr4.0\Nanbin` | `D:\NCMOD1.20.1\ymtr\Nanbin` |
| 分支 / 提交 | `mtr4.0` @ `ed04f13`（3.1最终功能性提交） | `feature/mtr4-parity`（自 `1.20.1+mtr3` @ `27e355c`） |
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
