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

import com.movtery.zalithlauncher.game.download.engine.Fetcher
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import java.io.FileInputStream

private const val TAG = "RuntimeDownloader"

/**
 * Java 运行环境的在线下载器。
 *
 * 当内置 JRE（Internal-8/17/21/25）因架构不匹配、解压失败等原因不可用时，作为网络兜底：
 * 按目标 Java 大版本 + 设备架构从 [JreDownloadSource] 取候选源（国内镜像优先、GitHub 兜底），
 * 下载单个 tar.xz 后复用 [RuntimesManager.installRuntime] 解压安装。
 */
object RuntimeDownloader {
    /** 下载后安装所用的运行环境名（区别于内置的 Internal-*，避免冲突） */
    private fun runtimeName(majorVersion: Int): String = "Downloaded-$majorVersion"

    /**
     * 下载并安装目标 Java 大版本的运行环境，返回可用的运行环境名。
     *
     * @param majorVersion 目标 Java 大版本（如 8、17）
     * @param onMessage 进度/状态回调（后台线程），用于向用户反馈“下载中/安装中”
     * @return 安装成功后的运行环境名
     */
    suspend fun download(
        majorVersion: Int,
        onMessage: (String) -> Unit = {}
    ): String {
        val urls = JreDownloadSource.candidateUrls(majorVersion)
        if (urls.isEmpty()) {
            throw IllegalStateException("No download source available for Java $majorVersion on this device.")
        }

        val name = runtimeName(majorVersion)

        // 已安装且可用时直接复用，避免重复下载
        val existing = RuntimesManager.loadRuntime(name)
        if (existing.versionString != null) {
            Logger.info(TAG, "Runtime $name already installed, reuse it")
            return name
        }

        val tempFile = File(PathManager.DIR_CACHE, "$name.tar.xz")
        try {
            onMessage("Downloading Java $majorVersion runtime...")
            Logger.info(TAG, "Downloading Java $majorVersion runtime from ${urls.size} candidate source(s)")

            Fetcher.downloadFile(
                urls = urls,
                targetFile = tempFile,
                sha1 = null,
                onBytes = { /* 分片增量，精确百分比交由解压阶段的 installRuntime 上报 */ }
            )

            onMessage("Installing Java $majorVersion runtime...")
            Logger.info(TAG, "Downloaded ${tempFile.length()} bytes, installing as $name")

            FileInputStream(tempFile).use { input ->
                RuntimesManager.installRuntime(
                    nativeLibDir = PathManager.DIR_NATIVE_LIB,
                    inputStream = input,
                    name = name,
                    updateProgress = { _, _ -> /* 解压进度 */ }
                )
            }

            Logger.info(TAG, "Runtime $name installed and ready")
            return name
        } finally {
            // 无论成败都清理临时包，失败时 installRuntime 内部也会回滚目录
            runCatching { tempFile.delete() }
        }
    }
}
