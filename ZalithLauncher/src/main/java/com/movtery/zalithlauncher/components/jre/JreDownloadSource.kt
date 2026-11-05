/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.components.jre

import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.utils.device.Architecture

/**
 * 可在线下载的 Java 运行环境（JRE）下载源。
 *
 * 内置 JRE（Internal-8/17/21/25）已随 APK 打包、覆盖绝大多数场景；当内置 JRE 因架构不匹配、
 * 解压损坏等原因不可用时，由 [JreDownloadSource] 提供网络兜底：按目标 Java 大版本 + 设备架构
 * 定位官方直链，并注入国内加速镜像前缀，最终交给下载引擎按候选顺序自动换源。
 *
 * 注意：官方公开的 PojavLauncher 系 JRE 目前只覆盖 Java 8 与 17（且 8 仅 arm64），
 * Java 21 / 25 无稳定外部源，仍以内置 JRE 为准。
 */
object JreDownloadSource {
    /**
     * 单个官方直链（GitHub Release asset）。value 为「架构 -> 文件名」的映射。
     * 文件名来自 [PojavLauncherTeam/android-openjdk-build-multiarch] 的公开 release，
     * 结构为 `jre<major>-<arch>-<date>-release.tar.xz`（单文件 tar.xz，直接可用 installRuntime 解压）。
     */
    private data class OfficialRelease(
        val majorVersion: Int,
        val tag: String,
        val assets: Map<Int, String>
    ) {
        fun url(arch: Int): String? = assets[arch]?.let { name ->
            "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/$tag/$name"
        }
    }

    private val RELEASES = listOf(
        OfficialRelease(
            majorVersion = 8,
            tag = "jre8-40df388",
            assets = mapOf(
                //该 release 仅提供 arm64 的 Android JRE
                Architecture.ARCH_ARM64 to "jre8-arm64-20220811-release.tar.xz"
            )
        ),
        OfficialRelease(
            majorVersion = 17,
            tag = "jre17-ec28559",
            assets = mapOf(
                Architecture.ARCH_ARM to "jre17-arm-20210914-release.tar.xz",
                Architecture.ARCH_ARM64 to "jre17-arm64-20210825-release.tar.xz",
                Architecture.ARCH_X86 to "jre17-x86-20220225-release.tar.xz",
                Architecture.ARCH_X86_64 to "jre17-x86_64-20210825-release.tar.xz"
            )
        )
    )

    /**
     * 国内 GitHub 加速镜像前缀。按顺序尝试，失败后回退到 GitHub 官方直链。
     * 镜像多为社区维护、可用性时有波动，因此直接拼进候选源列表交给下载引擎换源，而不是只依赖单一镜像。
     */
    private val MIRROR_PREFIXES = listOf(
        "https://gh-proxy.com/",
        "https://ghfast.top/",
        "https://ghproxy.net/"
    )

    /**
     * 目标 Java 大版本是否有可下载的外部源。
     * 返回 false 时上层应回退到内置 JRE 或直接提示用户，而不是尝试下载。
     */
    fun isDownloadable(majorVersion: Int): Boolean =
        RELEASES.any { it.majorVersion == majorVersion }

    /**
     * 返回目标 Java 大版本 + 设备架构对应的候选下载源列表（镜像在前、官方兜底）。
     * 若该版本/架构无任何可用源，返回空列表。
     */
    fun candidateUrls(majorVersion: Int, arch: Int): List<String> {
        val release = RELEASES.firstOrNull { it.majorVersion == majorVersion } ?: return emptyList()
        val official = release.url(arch) ?: return emptyList()

        return MIRROR_PREFIXES.map { it + official } + official
    }

    /** 当前设备架构对应的下载候选源（简写） */
    fun candidateUrls(majorVersion: Int): List<String> =
        candidateUrls(majorVersion, ZLApplication.DEVICE_ARCHITECTURE)
}
