# 🫧 Bubble Launcher

**一个由“泡泡（paopao340）”维护的 Minecraft: Java Edition 启动器 · ZalithLauncher 2 的非官方修改版**

![Downloads](https://img.shields.io/github/downloads/swan3146/Bubble-Launcher/total)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)

[English](README_EN_US.md) · [繁體中文](README_ZH_TW.md)

---

## 🫧 这是什么

Bubble Launcher 是一个 Android 上的 Minecraft: Java Edition 启动器，基于 **ZalithLauncher 2（ZL2）** 二次开发、修改并重命名而来。启动核心沿用 **PojavLauncher**，界面用 **Jetpack Compose + Material Design 3** 重新打磨。

名字叫“Bubble”，是因为它从 ZL2 这池水里冒出来，是一个独立的小气泡——**不是官方，也不冒充官方**。项目由泡泡（paopao340）独立维护，只代表二次开发者的修改成果。

> **一句话说清楚关系：** 我们是 ZL2 的下游修改版，不是 ZL2 官方，也不代表 ZL2 原作者。

> **注意：** 本项目目前为早期个人项目，公开信息有限。使用前请自行确认仓库与发布来源。

---

## 👤 维护者

- 名称：泡泡（paopao340）
- GitHub：https://github.com/paopao340
- 联系方式：wsdby110417@qq.com

---

## 🔗 上游与来源

### ZalithLauncher 2（直接上游）

本项目基于 **ZalithLauncher 2（ZL2）** 二次开发、修改并重命名。

- 上游仓库：https://github.com/ZalithLauncher/ZalithLauncher2
- 上游许可证：**GPL-3.0**，含依据 GPLv3 第七条添加的附加条款
- 上游版权归 ZalithLauncher 原作者及贡献者所有
- 本项目**不是** ZL2 官方项目，与上游无隶属、合作、授权、赞助或背书关系

**ZL2 附加条款对本项目的约束：**

1. 名称中不含 “ZalithLauncher” 或 “ZL”，也不使用与官方名称相近、可能导致混淆的名称；
2. 程序启动页面或主界面以明显方式标注“非官方修改版”；
3. 不移除原程序的版权声明。

### PojavLauncher（启动核心）

本项目使用 **PojavLauncher** 作为启动核心（JNI 层）。

- 上游仓库：https://github.com/PojavLauncherTeam/PojavLauncher
- **许可证以该仓库当前 LICENSE 文件为准。** 该项目历史上经历过从 GPL 到 LGPL 的许可证变更，下游引用时不宜自行断言单一许可证；该仓库已归档，请以归档时的 LICENSE 文件为准。
- PojavLauncher 的版权归其原作者及贡献者所有。

### 上游官网与翻译平台说明

ZL2 的官方网站 [zalithlauncher.cn](https://zalithlauncher.cn) 和 Weblate 项目 [hosted.weblate.org/projects/zalithlauncher2](https://hosted.weblate.org/projects/zalithlauncher2) **均由上游运营**。本项目不参与运营，不对其内容负责。上游已提醒有第三方冒用“Zalith Launcher”名义搭建仿冒网站，请注意甄别。

---

## ⚠️ 先看这里

- 本项目**不是** ZL2 官方项目，与 ZL2 原作者、维护者、版权方、商标权人**没有**隶属、合作、授权、赞助或背书关系。
- 本项目**不提供 Minecraft 游戏本体、资源包或任何游戏核心文件的下载/镜像**。2026 年 10 月，PCL、HMCL 等国内主流第三方启动器因合规原因集体关闭了游戏本体下载功能；本项目遵循同样的合规边界。
- 本项目按 GPLv3 附加条款执行：名称合规、程序内标注“非官方修改版”、原版权声明保留。
- 使用本项目产生的风险由用户自行承担，详见下方免责声明。

---

## 🛠 自己构建

**环境**

- Android Studio Bumblebee 及以上
- Android SDK：最低 API 26 / 目标 API 35
- JDK 11

**命令**

```bash
git clone git@github.com:swan3146/Bubble-Launcher.git
cd Bubble-Launcher
./gradlew assembleDebug
```

也可以用 Android Studio 直接打开构建。应用名称在 `ZalithLauncher/gradle.properties` 中修改。

---

## 🌐 关于翻译

本项目暂未建立独立翻译平台。上游 ZL2 使用 Weblate（[项目地址](https://hosted.weblate.org/projects/zalithlauncher2)），想参与上游翻译可以去那里；本项目的翻译内容与上游不绑定，也不代表本项目立场。

---

## 📜 免责声明

**生效日期：** 2026 年 11 月 3 日  
**发布主体：** 泡泡（paopao340）  
**联系方式：** wsdby110417@qq.com

### 1. 名称变更与项目说明

本项目原基于 ZL2 进行二次开发、修改与重命名，现名称为 Bubble Launcher。本项目由泡泡（paopao340）独立维护，仅代表二次开发者的修改成果。

### 2. 非官方声明

本项目不是 ZL2 官方项目，与 ZL2 原作者、维护者、版权方或商标权人之间不存在隶属、合作、授权、赞助或背书关系，除非另有明确书面说明。ZL2 的名称、商标、版权及其他权利归其原权利人所有。

### 3. 知识产权与许可证

1. 本项目对 ZL2 原有代码、资源及文档的使用，遵守原项目所适用的 GPL-3.0 许可证及附加条款。
2. 本项目新增或修改部分的版权归泡泡（paopao340）或相应贡献者所有，但不得覆盖或否定原权利人的权利。
3. 原许可证要求的版权声明、许可证文本及修改说明已按要求保留。
4. “Bubble Launcher”仅作为本项目名称使用，不代表对任何第三方商标或名称享有权利。

### 4. 使用风险自担

本项目按“现状”提供。用户应自行评估并承担使用本项目产生的全部风险，包括但不限于：设备损坏、数据丢失、系统异常、账号封禁、第三方服务中断、兼容性问题、安全风险及法律合规风险。使用前请自行备份数据并进行充分测试。

### 5. 无担保声明

在适用法律允许的最大范围内，泡泡（paopao340）不对本项目作任何明示或默示担保，包括但不限于适销性、特定用途适用性、不侵权、无错误、持续可用及安全性担保。本项目可能存在缺陷、漏洞或中断，开发者不保证修复或持续更新。

### 6. 责任限制

在适用法律允许的最大范围内，泡泡（paopao340）及贡献者不对因使用或无法使用本项目而产生的任何直接、间接、附带、特殊、惩罚性、后果性损害或利润损失承担责任，即使已被告知可能发生此类损害。

### 7. 合规使用

用户不得利用本项目从事违法违规活动，不得侵犯他人知识产权、隐私权或其他合法权益。用户应遵守所在地法律法规、第三方平台规则及原项目许可证要求。因用户违规使用产生的责任由用户自行承担。

### 8. 第三方内容

本项目可能包含或链接第三方插件、资源、服务或代码。其版权与责任归相应权利人所有，本项目不对第三方内容的可用性、安全性、合法性或准确性作保证。

### 9. 权利主张与整改

如原 ZL2 权利人、贡献者或任何第三方认为本项目的改名、二次开发、分发或相关内容侵犯其合法权益，请通过 **wsdby110417@qq.com** 联系。我们将在核实后及时删除、整改或停止相关分发。

### 10. 声明更新

本免责声明可能随项目发展更新，更新后自发布之日起生效。用户应定期查看最新版本。

---

## ⚖️ License

本项目代码遵循 **GPL-3.0 license**（含 ZL2 附加条款）。

### 附加条款（GPLv3 第七条）

1. 分发修改版本时，必须以合理方式修改程序名称或版本号，以示与原始版本不同。（[GPLv3, 7(c)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L372-L374)）
   - 修改版本**不得在名称中包含 “ZalithLauncher” 或其缩写 “ZL”**，也不得使用与官方名称相近、可能导致混淆的名称。
   - 所有修改版本**必须在程序启动页面或主界面中以明显方式标注“非官方修改版”**。
2. 不得移除程序所显示的版权声明。（[GPLv3, 7(b)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L368-L370)）

**本项目的执行情况：** 名称“Bubble Launcher”不含 ZL；启动页/主界面标注“非官方修改版”；原版权声明保留。如有遗漏，请通过上方邮箱反馈。

---

## 📚 使用的开源项目

| Library | Copyright | License | Link |
|---|---|---|---|
| **ZalithLauncher 2（上游基础）** | © ZalithLauncher 原作者及贡献者 | GPL-3.0 + 附加条款 | [↗](https://github.com/ZalithLauncher/ZalithLauncher2) |
| **PojavLauncher（启动核心）** | © PojavLauncher 原作者及贡献者 | 以仓库 LICENSE 为准 | [↗](https://github.com/PojavLauncherTeam/PojavLauncher) |
| **MobileGlues-release** | © 2025-2026 MobileGL-Dev | LGPL-2.1（以仓库当前 LICENSE 为准，存在升级至 LGPL-3.0 的记录） | [↗](https://github.com/MobileGL-Dev/MobileGlues-release) |
| androidx-appcompat | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/appcompat) |
| androidx-constraintlayout-compose | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/develop/ui/compose/layouts/constraintlayout) |
| androidx-webkit | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/webkit) |
| ANGLE | The ANGLE Project Authors | BSD 3-Clause | [↗](http://angleproject.org/) |
| Apache Commons Codec | 见官方链接 | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-codec) |
| Apache Commons Compress | 见官方链接 | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-compress) |
| Apache Commons IO | 见官方链接 | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-io) |
| ByteHook | © 2020-2024 ByteDance, Inc. | MIT | [↗](https://github.com/bytedance/bhook) |
| BuildKeys | © 2026 MovTery | Apache 2.0 | [↗](https://github.com/MovTery/BuildKeys) |
| Coil Compose / Gifs / SVG | © 2025 Coil Contributors | Apache 2.0 | [↗](https://github.com/coil-kt/coil) |
| Fishnet | © 2025 Kyant | Apache 2.0 | [↗](https://github.com/Kyant0/Fishnet) |
| gl4es_extra_extra | © 2016-2018 S. Chevalier; © 2013-2016 R. Hileman | MIT | [↗](https://github.com/PojavLauncherTeam/gl4es_extra_extra) |
| Gson | © 2008 Google Inc. | Apache 2.0 | [↗](https://github.com/google/gson) |
| kotlinx.coroutines | © 2000-2020 JetBrains s.r.o. | Apache 2.0 | [↗](https://github.com/Kotlin/kotlinx.coroutines) |
| ktor-client-* / ktor-http / ktor-serialization | © 2000-2023 JetBrains s.r.o. | Apache 2.0 | [↗](https://ktor.io) |
| LWJGL | © 2012-present LWJGL | BSD 3-Clause | [↗](https://github.com/LWJGL/lwjgl3) |
| material-color-utilities | © 2021 Google LLC | Apache 2.0 | [↗](https://github.com/material-foundation/material-color-utilities) |
| Maven Artifact | The Apache Software Foundation | Apache 2.0 | [↗](https://github.com/apache/maven/tree/maven-3.9.9/maven-artifact) |
| Media3 | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/media3) |
| Mesa | The Mesa Authors | MIT | [↗](https://mesa3d.org/) |
| MMKV | © 2018 THL A29 Limited, a Tencent company | BSD 3-Clause | [↗](https://github.com/Tencent/MMKV) |
| Navigation 3 | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/navigation3) |
| NG-GL4ES | © 2016-2018 S. Chevalier; © 2013-2016 R. Hileman; © 2025-2026 BZLZHH | MIT | [↗](https://github.com/BZLZHH/NG-GL4ES) |
| OkHttp | © 2019 Square, Inc. | Apache 2.0 | [↗](https://github.com/square/okhttp) |
| Okio | © 2013 Square, Inc. | Apache 2.0 | [↗](https://square.github.io/okio/) |
| OpenNBT | © 2013-2021 Steveice10 | MIT | [↗](https://github.com/GeyserMC/OpenNBT) |
| Process Phoenix | © 2015 Jake Wharton | Apache 2.0 | [↗](https://github.com/JakeWharton/ProcessPhoenix) |
| proxy-client-android | 见官方链接 | LGPL-3.0 | [↗](https://github.com/TouchController/TouchController) |
| Reorderable | © 2023 Calvin Liang | Apache 2.0 | [↗](https://github.com/Calvin-LL/Reorderable) |
| sdl2-compat | © 2026 Sam Lantinga | Zlib | [↗](https://github.com/libsdl-org/sdl2-compat) |
| SDL3 | © 1997-2026 Sam Lantinga | Zlib | [↗](https://github.com/libsdl-org/SDL) |
| skinview3d | © 2014-2018 K. Rasmussen; © 2017-2022 H. Wen, S. Boult and contributors | MIT | [↗](https://github.com/bs-community/skinview3d) |
| sora-editor | © 2020-2026 Rosemoe | LGPL-2.1 | [↗](https://github.com/Rosemoe/sora-editor) |
| StringFog | © 2016-2023 Megatron King | Apache 2.0 | [↗](https://github.com/MegatronKing/StringFog) |
| tm4e (TextMate for Eclipse) | Eclipse Foundation | EPL-2.0 | [↗](https://github.com/eclipse-tm4e/tm4e) |
| XZ for Java | The XZ for Java authors and contributors | 0BSD | [↗](https://tukaani.org/xz/java.html) |

---

<div align="center">

**Bubble Launcher · 由泡泡（paopao340）维护 · ZalithLauncher 2 的非官方修改版**

</div>