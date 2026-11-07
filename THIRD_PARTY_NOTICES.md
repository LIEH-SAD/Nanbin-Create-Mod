# 第三方开源声明（THIRD-PARTY NOTICES）

本文件记录 Nanbin 模组（下称“本 mod”）参考/使用的第三方项目及其许可与版权信息。若本文件与对应许可原文存在出入，以许可原文为准。

---

## 1. CRT-Sign-Web-Editor（Apache License 2.0）

### 1.1 使用方式

本 mod 的 **CRT 铁路指示牌编辑界面**在**信息架构**（编辑面板的分区、字段组织与交互流程）和**视觉风格**（配色、线路圆标、导视牌版式）上参考了开源网页项目 **CRT-Sign-Web-Editor**，并由本 mod 在 **Java 端**基于 Minecraft / MTR 的渲染管线**重新实现**。

> 本 mod **未逐行复制**该项目的源代码，也未在其代码基础上直接修改；参考的是界面结构与视觉呈现，实现代码由本 mod 独立编写。

### 1.2 上游链条与版权

| 顺序 | 项目 | 作者 / 维护者 | 仓库地址 | 许可 |
| --- | --- | --- | --- | --- |
| 原始上游 | JR-Guidance-Sign-Web-Editor | endcreeper861 | https://github.com/endcreeper861/JR-Guidance-Sign-Web-Editor | Apache License 2.0 |
| 本 mod 参考的 fork | CRT-Sign-Web-Editor | 修改者 PrisamaX0124（基于原作） | https://github.com/prisamax0124/CRT-Sign-Web-Editor | Apache License 2.0 |

说明：CRT-Sign-Web-Editor 是 JR-Guidance-Sign-Web-Editor 的 fork，上游两代项目均以 **Apache License 2.0** 发布。本 mod 保留上述项目的**版权与来源声明**，不改变其原有许可。

### 1.3 许可全文

Apache License 2.0 许可全文见 [docs/licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)。

如后续需要引入或分发该项目源码的修改版本，应遵守 Apache-2.0 第 4 条：随附许可副本、在修改文件中标注变更、保留版权与 NOTICE 声明。

---

## 2. 字体与图标素材（未随本 mod 打包）

CRT-Sign-Web-Editor 所使用的下列字体与图标素材，**均未复制进本 mod，也未随本 mod 打包分发**（未进入本仓库的源码或构建产物）；它们仍归属各自权利人，并适用各自的授权条款：

- **MiSans**：由小米（Xiaomi）发布，适用《MiSans 字体知识产权许可协议》，可按该协议免费使用及商用。
- **Frutiger**：由 CRT-Sign-Web-Editor 上游仓库提供，版权归其权利人所有。
- **Source Han Sans（思源黑体）**：由 Adobe 发布，适用 SIL Open Font License 1.1。
- **图标素材**：服务设施与方向图标来自 signmaker-main（https://github.com/hcrk9/signmaker/）；中国铁路路徽为按提供的参考图整理的矢量轮廓，并加了圆角矩形外框。图标版权归各自权利人所有。

上述素材的授权**不因**本 mod 或上游项目采用 Apache-2.0 而改变；Apache-2.0 仅覆盖代码，不自动覆盖第三方字体与图标。

> 补充说明：本 mod 资源目录（`fabric/src/main/resources/assets/nanbin/font/`、`forge/src/main/resources/assets/nanbin/font/`）中现有的 Alibaba PuHuiTi、Source Han Sans SC、Source Sans 3 字体文件属于本 mod 的**既有资源**，与 CRT-Sign-Web-Editor 无来源关系；其授权说明见同目录下的 `LICENSE.txt`。

---

## 3. 其他

- 本 mod 自身源代码的许可见仓库根目录 [LICENSE](../LICENSE)（MIT License）。
- 本文件仅用于披露来源与许可信息，不构成法律意见。

*最后更新：2026-11-07*
