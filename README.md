# Bubble Launcher

[English](README_EN_US.md) | [繁體中文](README_ZH_TW.md)

> [!NOTE]
> **Bubble Launcher** 是基于开源项目 [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2) 二次开发的 **Android** 端 [Minecraft: Java Edition](https://www.minecraft.net/) 启动器。
> 它是一个**非官方修改版**，与 ZalithLauncher 官方团队无任何隶属或关联关系。

Bubble Launcher 沿用 ZL2 的启动核心（基于 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)），采用 **Jetpack Compose** 与 **Material Design 3** 构建 UI，在原版基础上做了启动参数修复、下载加速、渲染器兼容、合规署名等改进。

## ✨ 特性

- 原生 Android 启动器，支持主流 Java 版本与 Forge/Fabric 等加载器
- Material Design 3 / Jetpack Compose 现代化界面
- 多渲染器兼容（内置 MobileGlues 等）
- 正版 / 外置（皮肤站）登录支持

## 📦 构建方式（开发者）

### 环境要求

* Android Studio 最新稳定版
* Android SDK：最低 API 26，目标 API 35
* JDK 17（AGP 要求）

### 构建步骤

```bash
# 使用 Android Studio 打开本项目，或命令行：
./gradlew assembleRelease
```

> 注意：本仓库为纯源码发布，不包含预编译原生库（jniLibs）与内置运行时；完整可构建产物需另行补回对应原生二进制。

## 📜 License

本项目在保留上游版权声明的前提下，遵循 **[GPL-3.0 license](LICENSE)** 开源协议。

### 关于上游 GPLv3 第七条附加条款

依据 ZL2 上游附加条款：

1. 本修改版已将名称改为 **Bubble Launcher**，未使用 "ZalithLauncher" 或 "ZL" 字样，并在免责声明中明确标注为非官方修改版。
2. 本项目保留了上游全部版权声明与第三方许可文本，未作删除。

### 第三方开源组件

本软件使用以下开源库（完整列表见 App 内「设置 → 关于 → 开源许可」）：

| Library | Copyright | License | Link |
|---|---|---|---|
| Zalith Launcher 2 | Copyright (C) 2025 MovTery and contributors | GPL-3.0 | [链接↗](https://github.com/ZalithLauncher/ZalithLauncher2) |
| PojavLauncher core | Copyright (C) PojavLauncherTeam | GPL-3.0 | [链接↗](https://github.com/PojavLauncherTeam/PojavLauncher) |
| LWJGL | Copyright © Lightweight Java Game Library | BSD 3-Clause | [链接↗](https://github.com/LWJGL/lwjgl3) |
| SDL3 / sdl2-compat | Copyright (C) Sam Lantinga | Zlib | [链接↗](https://github.com/libsdl-org/SDL) |
| MobileGlues | Copyright © MobileGL-Dev | LGPL-2.1 | [链接↗](https://github.com/MobileGL-Dev/MobileGlues-release) |
| ANGLE / Mesa / MMKV / OkHttp / Ktor / Gson / Coil / skinview3d / sora-editor 等 | 见各组件 | Apache-2.0 / MIT / BSD / LGPL 等 | 见 App 内开源许可页 |

Minecraft 与 Mojang 名称为 Mojang Studios 商标，本项目与 Mojang、微软无任何关联。
