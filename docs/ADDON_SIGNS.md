# CRT 导视标志追加包接口（signs / addon packs）

本 mod 的导视标志不再依赖单一 `crt_signs.json`，而是按「内置基础图标 + 可替换的 JSON 定义」加载。
资源包 / 追加包可以新增或覆盖标志，编辑器会**动态**读取当前已加载的标志列表。

## 1. 标志来源与合并顺序

| 顺序 | 位置 | 说明 |
| --- | --- | --- |
| 1 | **代码内置**（`CRTBuiltInSignIcons`） | 27 个基础图标（箭头、cross/check、线路/设施/出口图标）。即使删掉所有 JSON 也仍然可用，编辑器左侧方向/设施图标靠它们工作。 |
| 2 | `assets/nanbin/crt_signs.json` | 本 mod 原来的主定义文件（可被资源包覆盖/追加，多包会合并）。现在只包含「图标+文字」的文字牌定义。 |
| 3 | `assets/nanbin/signs.json` | 旧文件名，重命名前的追加包继续兼容。 |
| 4 | `assets/<任意命名空间>/crt_signs/*.json` | **推荐的追加包格式**：每个追加包放自己的文件，互不覆盖。 |

同一 `id` 后加载的会覆盖先加载的（可以借此替换内置图标贴图），不同 `id` 会全部合并。
目录追加包里的标志如果不写 `whitelist`，默认按 `["crt"]` 处理，方便第三方直接使用；
旧单文件（`crt_signs.json` / `signs.json`）仍保持严格语义：必须显式声明 `"whitelist": ["crt"]` 才会出现在 CRT 类导视上。

## 2. 追加包 JSON 格式

```json
{
  "signs": [
    {
      "id": "yourpack_escalator_big",
      "textureResource": "yourpack:textures/block/sign/escalator_big",
      "whitelist": ["crt"],
      "small": true,
      "customText": "大扶梯",
      "backgroundColor": "#FFFFFF",
      "flipTexture": false
    }
  ]
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 全局唯一标志 id；建议带自己的前缀。编辑器里 icon 元素保存的就是这个 id。 |
| `textureResource` | 是 | 贴图资源 id（`namespace:textures/...`），贴图放在追加包自己的命名空间下即可。 |
| `whitelist` | 目录包可省 | CRT 类导视启用列表；目录包省略时默认 `["crt"]`。 |
| `small` | 否 | 是否按小图标排版（旧格子渲染用；布局渲染会自动适配）。 |
| `customText` | 否 | 显示名（会出现在编辑器追加包分类的卡片标题；也是文字牌的自定义文字）。 |
| `backgroundColor` / `flipTexture` / `flipCustomText` | 否 | 与 MTR SignResource 字段一致。 |
| `textColor` / `textSize` / `textBold` | 否 | 本 mod 的旧格子文字样式（布局渲染不依赖）。 |

推荐目录：

```
your_pack/
└── assets/
    └── yourpack/
        ├── lang/zh_cn.json
        ├── textures/block/sign/escalator_big.png
        └── crt_signs/
            └── signs.json      ← 上面格式，可拆成多个文件
```

## 3. 编辑器中的表现

- **CRT 预设分类**：只显示当前真实的 `crt_signs.json` 里存在、且属于「图标+文字」预设列表的标志；删掉 `crt_signs.json` 后该分类自动隐藏，不会出现空卡片。把文件放回来（或由追加包提供同 id 定义）就会重新出现。
- **追加包标志分类**：自动列出所有「来自 JSON、且不属于内置图标 / 预设」的 id，按 `customText` 或美化后的 id 显示，点选即生成一个 `ICON` 元素（`icon = 标志 id`），世界渲染与预览共用同一套贴图缓存。
- 新增标志会在下一次资源重载（F3+T / 打开资源包界面应用）后进入注册表；重新打开编辑器即可看到新分类。

## 4. 清空 `crt_signs.json` 时

- 27 个内置基础图标（方向箭头、设施、线路、出口等）继续可用；
- 49 个「图标+文字」预设分类隐藏（因为定义不存在）；
- 追加包仍然可以通过上面的目录格式提供任意标志；
- 旧存档 `signIds` 渲染仍然通过注册表查询缓存工作。

## 5. 相关代码

- 加载：`client/CRTRailwaySignEditor/legacy/CRTSignRegistry#reload`
- 内置图标：`client/CRTRailwaySignEditor/legacy/CRTBuiltInSignIcons`
- 编辑器动态分类：`client/Screen/CRTSignEditorScreen#buildElementsTab`
- 预设过滤：`CRTPresetCatalog`（仅当注册表里存在对应 id 时显示）
