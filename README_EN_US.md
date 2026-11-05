# 🫧 Bubble Launcher

**A Minecraft: Java Edition launcher maintained by Paopao (paopao340) · An unofficial modified version of ZalithLauncher 2**

![Downloads](https://img.shields.io/github/downloads/swan3146/Bubble-Launcher/total)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)

[简体中文](README.md) · [繁體中文](README_ZH_TW.md)

---

## 🫧 What Is This

Bubble Launcher is an Android launcher for Minecraft: Java Edition. It is a secondary development, modification, and rename of **ZalithLauncher 2 (ZL2)**. The launch core is based on **PojavLauncher**, and the UI is rebuilt with **Jetpack Compose + Material Design 3**.

The name “Bubble” reflects that it emerged from the ZL2 pool as an independent little bubble — **not official, and not pretending to be official**. The project is independently maintained by Paopao (paopao340) and represents only the secondary developer’s modifications.

> **In one sentence:** We are a downstream modified version of ZL2, not ZL2 official, and we do not represent the original ZL2 authors.

> **Note:** This is currently an early-stage personal project with limited public information. Please verify the repository and release sources before use.

---

## 👤 Maintainer

- Name: Paopao (paopao340)
- GitHub: https://github.com/paopao340
- Contact: wsdby110417@qq.com

---

## 🔗 Upstream and Origins

### ZalithLauncher 2 (Direct Upstream)

This project is a secondary development, modification, and rename based on **ZalithLauncher 2 (ZL2)**.

- Upstream repository: https://github.com/ZalithLauncher/ZalithLauncher2
- Upstream license: **GPL-3.0**, with additional terms added under Section 7 of GPLv3
- Copyright of the upstream belongs to the ZalithLauncher authors and contributors
- This project is **not** an official ZL2 project and has no affiliation, cooperation, authorization, sponsorship, or endorsement relationship with the upstream

**ZL2 additional terms applicable to this project (already implemented):**

1. The name does not contain “ZalithLauncher” or “ZL”, and does not use a name similar to the official name that may cause confusion;
2. The app’s splash screen or main interface clearly marks it as an “unofficial modified version”;
3. The original copyright notices are retained.

### PojavLauncher (Launch Core)

This project uses **PojavLauncher** as the launch core (JNI layer).

- Upstream repository: https://github.com/PojavLauncherTeam/PojavLauncher
- **The license is subject to the LICENSE file in that repository.** The project has historically undergone a license change from GPL to LGPL; downstream references should not assert a single license without checking the current file. The repository has been archived, so refer to the LICENSE file at the time of archiving.
- Copyright of PojavLauncher belongs to its original authors and contributors.

### MobileGlues (Renderer)

- Repository / releases: https://github.com/MobileGL-Dev/MobileGlues-release/releases
- About: MobileGlues is a GL implementation that runs on top of the host OpenGL ES 3.x, designed for running Minecraft: Java Edition on mobile devices.
- License: GNU LGPL-2.1 (subject to the repository’s current LICENSE file; a record of upgrade to LGPL-3.0 exists).

### Upstream Website and Translation Platform

ZL2’s official website [zalithlauncher.cn](https://zalithlauncher.cn) and Weblate project [hosted.weblate.org/projects/zalithlauncher2](https://hosted.weblate.org/projects/zalithlauncher2) are **operated by the upstream project**. This project does not operate them and is not responsible for their content. The upstream has warned that a third party has set up an imitation website under the “Zalith Launcher” name; please verify sources carefully.

---

## ⚠️ Read This First

- This project is **not** an official ZL2 project and has **no** affiliation, cooperation, authorization, sponsorship, or endorsement relationship with the ZL2 authors, maintainers, copyright holders, or trademark holders.
- This project **does not provide downloads or mirrors for Minecraft game files, resource packs, or any game core files**. In October 2026, several mainstream third-party launchers in China (e.g., PCL, HMCL) collectively disabled in-game download features for compliance reasons. This project follows the same compliance boundary.
- This project complies with the GPLv3 additional terms: the name is compliant, the app marks itself as an “unofficial modified version”, and the original copyright notices are retained.
- Risks arising from the use of this project are borne by the user. See the disclaimer below.

---

## 🛠 Build It Yourself

**Requirements**

- Android Studio Bumblebee or later
- Android SDK: minimum API 26 / target API 35
- JDK 11

**Commands**

```bash
git clone git@github.com:swan3146/Bubble-Launcher.git
cd Bubble-Launcher
./gradlew assembleDebug
```

You can also open the project directly in Android Studio. The app name can be changed in `ZalithLauncher/gradle.properties`.

---

## 🌐 About Translations

This project has not yet established an independent translation platform. Upstream ZL2 uses Weblate ([project link](https://hosted.weblate.org/projects/zalithlauncher2)); if you want to contribute to upstream translations, you can go there. This project’s translations are not bound to the upstream and do not represent this project’s position.

---

## 📜 Disclaimer

**Effective date:** November 3, 2026  
**Publisher:** Paopao (paopao340)  
**Contact:** wsdby110417@qq.com

### 1. Name Change and Project Description

This project was originally developed, modified, and renamed based on ZL2, and is now named Bubble Launcher. The project is independently maintained by Paopao (paopao340) and represents only the secondary developer’s modifications.

### 2. Unofficial Statement

This project is not an official ZL2 project and has no affiliation, cooperation, authorization, sponsorship, or endorsement relationship with the ZL2 authors, maintainers, copyright holders, or trademark holders, unless otherwise expressly stated in writing. The name, trademark, copyright, and other rights of ZL2 belong to their respective rights holders.

### 3. Intellectual Property and License

1. The use of ZL2’s original code, resources, and documentation in this project complies with the applicable GPL-3.0 license and additional terms of the original project.
2. The copyright of new or modified parts of this project belongs to Paopao (paopao340) or the respective contributors, but does not override or negate the rights of the original rights holders.
3. Copyright notices, license texts, and modification descriptions required by the original license have been retained as required.
4. “Bubble Launcher” is used only as the name of this project and does not represent any right to any third-party trademark or name.

### 4. Use at Your Own Risk

This project is provided “as is”. Users should evaluate and bear all risks arising from the use of this project, including but not limited to: device damage, data loss, system anomalies, account bans, third-party service interruptions, compatibility issues, security risks, and legal compliance risks. Please back up your data and test thoroughly before use.

### 5. No Warranty

To the maximum extent permitted by applicable law, Paopao (paopao340) makes no express or implied warranties regarding this project, including but not limited to warranties of merchantability, fitness for a particular purpose, non-infringement, error-free operation, continuous availability, and security. The project may contain defects, vulnerabilities, or interruptions, and the developer does not guarantee fixes or continuous updates.

### 6. Limitation of Liability

To the maximum extent permitted by applicable law, Paopao (paopao340) and contributors shall not be liable for any direct, indirect, incidental, special, punitive, or consequential damages or loss of profits arising from the use or inability to use this project, even if advised of the possibility of such damages.

### 7. Compliant Use

Users must not use this project for illegal or non-compliant activities, or infringe upon the intellectual property, privacy, or other lawful rights of others. Users should comply with local laws and regulations, third-party platform rules, and the original project’s license requirements. Liability arising from user violations shall be borne by the user.

### 8. Third-Party Content

This project may contain or link to third-party plugins, resources, services, or code. Their copyright and liability belong to the respective rights holders. This project does not guarantee the availability, security, legality, or accuracy of third-party content.

### 9. Claims and Remediation

If the original ZL2 rights holders, contributors, or any third party believe that this project’s renaming, secondary development, distribution, or related content infringes their lawful rights, please contact **wsdby110417@qq.com**. We will promptly delete, remediate, or stop the relevant distribution after verification.

### 10. Updates to This Disclaimer

This disclaimer may be updated as the project develops. Updates take effect on the date of publication. Users should periodically check the latest version.

---

## ⚖️ License

This project’s code is licensed under **GPL-3.0** (including the ZL2 additional terms).

### Additional Terms (GPLv3 Section 7)

1. When distributing modified versions, you must modify the program’s name or version number in a reasonable manner to distinguish it from the original version. ([GPLv3, 7(c)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L372-L374))
   - Modified versions **must not include “ZalithLauncher” or its abbreviation “ZL” in their names**, nor use names similar to the official name that may cause confusion.
   - All modified versions **must clearly mark themselves as an “unofficial modified version” on the splash screen or main interface**.
2. You must not remove the copyright notices displayed by the program. ([GPLv3, 7(b)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L368-L370))

**Implementation in this project:** The name “Bubble Launcher” does not contain ZL; the splash screen/main interface marks it as an “unofficial modified version”; the original copyright notices are retained. If anything is missing, please report it via the email above.

---

## 📚 Open Source Projects Used

| Library | Copyright | License | Link |
|---|---|---|---|
| **ZalithLauncher 2 (upstream base)** | © ZalithLauncher authors and contributors | GPL-3.0 + additional terms | [↗](https://github.com/ZalithLauncher/ZalithLauncher2) |
| **PojavLauncher (launch core)** | © PojavLauncher authors and contributors | Subject to repository LICENSE | [↗](https://github.com/PojavLauncherTeam/PojavLauncher) |
| **MobileGlues-release** | © 2025-2026 MobileGL-Dev | LGPL-2.1 (subject to the repository’s current LICENSE; a record of upgrade to LGPL-3.0 exists) | [↗](https://github.com/MobileGL-Dev/MobileGlues-release) |
| androidx-appcompat | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/appcompat) |
| androidx-constraintlayout-compose | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/develop/ui/compose/layouts/constraintlayout) |
| androidx-webkit | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/webkit) |
| ANGLE | The ANGLE Project Authors | BSD 3-Clause | [↗](http://angleproject.org/) |
| Apache Commons Codec | See official link | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-codec) |
| Apache Commons Compress | See official link | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-compress) |
| Apache Commons IO | See official link | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-io) |
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
| proxy-client-android | See official link | LGPL-3.0 | [↗](https://github.com/TouchController/TouchController) |
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

**Bubble Launcher · Maintained by Paopao (paopao340) · Unofficial modified version of ZalithLauncher 2**

</div>
