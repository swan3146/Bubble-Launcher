# 🫧 Bubble Launcher

**由「泡泡（paopao340）」維護的 Minecraft: Java Edition 啟動器 · ZalithLauncher 2 的非官方修改版**

![Downloads](https://img.shields.io/github/downloads/swan3146/Bubble-Launcher/total)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)

[简体中文](README.md) · [English](README_EN_US.md)

---

## 🫧 這是什麼

Bubble Launcher 是一個 Android 上的 Minecraft: Java Edition 啟動器，基於 **ZalithLauncher 2（ZL2）** 二次開發、修改並重新命名而來。啟動核心沿用 **PojavLauncher**，介面以 **Jetpack Compose + Material Design 3** 重新打造。

名字叫「Bubble」，是因為它從 ZL2 這池水裡冒出來，是一個獨立的小氣泡——**不是官方，也不冒充官方**。本專案由泡泡（paopao340）獨立維護，僅代表二次開發者的修改成果。

> **一句話說清楚關係：** 我們是 ZL2 的下游修改版，不是 ZL2 官方，也不代表 ZL2 原作者。

> **注意：** 本專案目前為早期個人專案，公開資訊有限。使用前請自行確認倉庫與發佈來源。

---

## 👤 維護者

- 名稱：泡泡（paopao340）
- GitHub：https://github.com/paopao340
- 聯絡方式：wsdby110417@qq.com

---

## 🔗 上游與來源

### ZalithLauncher 2（直接上游）

本專案基於 **ZalithLauncher 2（ZL2）** 二次開發、修改並重新命名。

- 上游倉庫：https://github.com/ZalithLauncher/ZalithLauncher2
- 上游授權條款：**GPL-3.0**，含依 GPLv3 第七條新增的附加條款
- 上游著作權歸 ZalithLauncher 原作者及貢獻者所有
- 本專案**不是** ZL2 官方專案，與上游無隸屬、合作、授權、贊助或背書關係

**ZL2 附加條款對本專案的約束（已執行）：**

1. 名稱中不含「ZalithLauncher」或「ZL」，也不使用與官方名稱相近、可能導致混淆的名稱；
2. 程式啟動畫面或主介面以明顯方式標註「非官方修改版」；
3. 不移除原程式的著作權聲明。

### PojavLauncher（啟動核心）

本專案使用 **PojavLauncher** 作為啟動核心（JNI 層）。

- 上游倉庫：https://github.com/PojavLauncherTeam/PojavLauncher
- **授權條款以該倉庫目前的 LICENSE 檔案為準。** 該專案歷史上曾從 GPL 變更為 LGPL，下游引用時不宜自行斷言單一授權條款；該倉庫已封存，請以封存時的 LICENSE 檔案為準。
- PojavLauncher 的著作權歸其原作者及貢獻者所有。

### MobileGlues（渲染層）

- 倉庫／發佈頁：https://github.com/MobileGL-Dev/MobileGlues-release/releases
- 說明：MobileGlues 是一個 GL 實作，執行在宿主 OpenGL ES 3.x 之上，專為在行動裝置上執行 Minecraft: Java Edition 而設計。
- 授權：GNU LGPL-2.1（以該倉庫當前 LICENSE 檔案為準，存在升級至 LGPL-3.0 的記錄）。

### 上游官網與翻譯平台說明

ZL2 的官方網站 [zalithlauncher.cn](https://zalithlauncher.cn) 與 Weblate 專案 [hosted.weblate.org/projects/zalithlauncher2](https://hosted.weblate.org/projects/zalithlauncher2) **均由上游營運**。本專案不參與營運，不對其內容負責。上游已提醒有第三方冒用「Zalith Launcher」名義架設仿冒網站，請注意辨識。

---

## ⚠️ 先看這裡

- 本專案**不是** ZL2 官方專案，與 ZL2 原作者、維護者、著作權人、商標權人**沒有**隸屬、合作、授權、贊助或背書關係。
- 本專案**不提供 Minecraft 遊戲本體、資源包或任何遊戲核心檔案的下載／鏡像**。2026 年 10 月，PCL、HMCL 等中國主流第三方啟動器因合規原因集體關閉了遊戲本體下載功能；本專案遵循同樣的合規邊界。
- 本專案依 GPLv3 附加條款執行：名稱合規、程式內標註「非官方修改版」、原著作權聲明保留。
- 使用本專案產生的風險由使用者自行承擔，詳見下方免責聲明。

---

## 🛠 自行建置

**環境需求**

- Android Studio Bumblebee 以上
- Android SDK：最低 API 26 / 目標 API 35
- JDK 11

**指令**

```bash
git clone git@github.com:swan3146/Bubble-Launcher.git
cd Bubble-Launcher
./gradlew assembleDebug
```

也可以直接用 Android Studio 開啟建置。應用程式名稱可在 `ZalithLauncher/gradle.properties` 中修改。

---

## 🌐 關於翻譯

本專案尚未建立獨立翻譯平台。上游 ZL2 使用 Weblate（[專案位址](https://hosted.weblate.org/projects/zalithlauncher2)），想參與上游翻譯可以前往該處；本專案的翻譯內容與上游不綁定，也不代表本專案立場。

---

## 📜 免責聲明

**生效日期：** 2026 年 11 月 3 日  
**發佈主體：** 泡泡（paopao340）  
**聯絡方式：** wsdby110417@qq.com

### 1. 名稱變更與專案說明

本專案原基於 ZL2 進行二次開發、修改與重新命名，現名稱為 Bubble Launcher。本專案由泡泡（paopao340）獨立維護，僅代表二次開發者的修改成果。

### 2. 非官方聲明

本專案不是 ZL2 官方專案，與 ZL2 原作者、維護者、著作權人或商標權人之間不存在隸屬、合作、授權、贊助或背書關係，除非另有明確書面說明。ZL2 的名稱、商標、著作權及其他權利歸其原權利人所有。

### 3. 智慧財產權與授權

1. 本專案對 ZL2 原有程式碼、資源及文件的使用，遵守原專案所適用的 GPL-3.0 授權條款及附加條款。
2. 本專案新增或修改部分的著作權歸泡泡（paopao340）或相應貢獻者所有，但不得覆蓋或否定原權利人的權利。
3. 原授權條款要求的著作權聲明、授權條款文本及修改說明已按要求保留。
4. 「Bubble Launcher」僅作為本專案名稱使用，不代表對任何第三方商標或名稱享有權利。

### 4. 使用風險自行承擔

本專案按「現狀」提供。使用者應自行評估並承擔使用本專案產生的全部風險，包括但不限於：裝置損壞、資料遺失、系統異常、帳號封鎖、第三方服務中斷、相容性問題、安全風險及法律合規風險。使用前請自行備份資料並進行充分測試。

### 5. 無擔保聲明

在適用法律允許的最大範圍內，泡泡（paopao340）不對本專案作任何明示或默示擔保，包括但不限於適銷性、特定用途適用性、不侵權、無錯誤、持續可用及安全性擔保。本專案可能存在缺陷、漏洞或中斷，開發者不保證修復或持續更新。

### 6. 責任限制

在適用法律允許的最大範圍內，泡泡（paopao340）及貢獻者不對因使用或無法使用本專案而產生的任何直接、間接、附帶、特殊、懲罰性、後果性損害或利潤損失承擔責任，即使已被告知可能發生此類損害。

### 7. 合規使用

使用者不得利用本專案從事違法違規活動，不得侵犯他人智慧財產權、隱私權或其他合法權益。使用者應遵守所在地法律法規、第三方平台規則及原專案授權條款要求。因使用者違規使用產生的責任由使用者自行承擔。

### 8. 第三方內容

本專案可能包含或連結第三方外掛、資源、服務或程式碼。其著作權與責任歸相應權利人所有，本專案不對第三方內容的可用性、安全性、合法性或準確性作保證。

### 9. 權利主張與整改

如原 ZL2 權利人、貢獻者或任何第三方認為本專案的改名、二次開發、散佈或相關內容侵犯其合法權益，請透過 **wsdby110417@qq.com** 聯絡。我們將在核實後及時刪除、整改或停止相關散佈。

### 10. 聲明更新

本免責聲明可能隨專案發展更新，更新後自發布之日起生效。使用者應定期查看最新版本。

---

## ⚖️ License

本專案程式碼遵循 **GPL-3.0**（含 ZL2 附加條款）。

### 附加條款（GPLv3 第七條）

1. 散佈修改版本時，必須以合理方式修改程式名稱或版本號，以示與原始版本不同。（[GPLv3, 7(c)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L372-L374)）
   - 修改版本**不得在名稱中包含「ZalithLauncher」或其縮寫「ZL」**，也不得使用與官方名稱相近、可能導致混淆的名稱。
   - 所有修改版本**必須在程式啟動畫面或主介面中以明顯方式標註「非官方修改版」**。
2. 不得移除程式所顯示的著作權聲明。（[GPLv3, 7(b)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L368-L370)）

**本專案的執行情況：** 名稱「Bubble Launcher」不含 ZL；啟動畫面／主介面標註「非官方修改版」；原著作權聲明保留。如有遺漏，請透過上方電子郵件回報。

---

## 📚 使用的開源專案

| Library | Copyright | License | Link |
|---|---|---|---|
| **ZalithLauncher 2（上游基礎）** | © ZalithLauncher 原作者及貢獻者 | GPL-3.0 + 附加條款 | [↗](https://github.com/ZalithLauncher/ZalithLauncher2) |
| **PojavLauncher（啟動核心）** | © PojavLauncher 原作者及貢獻者 | 以倉庫 LICENSE 為準 | [↗](https://github.com/PojavLauncherTeam/PojavLauncher) |
| **MobileGlues-release** | © 2025-2026 MobileGL-Dev | LGPL-2.1（以倉庫目前 LICENSE 為準，存在升級至 LGPL-3.0 的記錄） | [↗](https://github.com/MobileGL-Dev/MobileGlues-release) |
| androidx-appcompat | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/appcompat) |
| androidx-constraintlayout-compose | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/develop/ui/compose/layouts/constraintlayout) |
| androidx-webkit | The Android Open Source Project | Apache 2.0 | [↗](https://developer.android.com/jetpack/androidx/releases/webkit) |
| ANGLE | The ANGLE Project Authors | BSD 3-Clause | [↗](http://angleproject.org/) |
| Apache Commons Codec | 見官方連結 | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-codec) |
| Apache Commons Compress | 見官方連結 | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-compress) |
| Apache Commons IO | 見官方連結 | Apache 2.0 | [↗](https://commons.apache.org/proper/commons-io) |
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
| proxy-client-android | 見官方連結 | LGPL-3.0 | [↗](https://github.com/TouchController/TouchController) |
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

**Bubble Launcher · 由泡泡（paopao340）維護 · ZalithLauncher 2 的非官方修改版**

</div>
