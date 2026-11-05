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

package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.game.addons.mirror.mapBMCLMirrorUrls
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.OperatingSystem
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.compareSHA1
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.fetchStringFromUrls
import com.movtery.zalithlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private const val TAG = "Minecraft.DownloadUtils"

private const val UTILS_LOG_TAG = "Minecraft.DownloaderUtils"

fun <T> String.parseTo(classOfT: Class<T>): T {
    return runCatching {
        GSON.fromJson(this, classOfT)
    }.getOrElse { e ->
        Logger.error(TAG, "Failed to parse JSON", e)
        throw e
    }
}

suspend fun <T> downloadAndParseJson(
    targetFile: File,
    url: String,
    expectedSHA: String?,
    classOfT: Class<T>
): T {
    suspend fun downloadAndParse(): T {
        val json = withContext(Dispatchers.IO) {
            val string = withRetry(UTILS_LOG_TAG, maxRetries = 2) {
                fetchStringFromUrls(
                    url.mapBMCLMirrorUrls()
                )
            }
            if (string.isBlank()) {
                Logger.error(TAG, "Downloaded string is empty, aborting.")
                throw IllegalStateException("Downloaded string is empty.")
            }
            //D-08：版本清单 JSON 必须原子落盘。直接 writeText 一旦在写入过程中
            //被杀进程，就会留下截断的 <version>.json，后续 VersionInfoParser 解析必失败，
            //且错误提示与用户操作毫不相关
            writeTextAtomically(targetFile, string)
            string
        }
        return json.parseTo(classOfT)
    }

    if (targetFile.exists()) {
        if (expectedSHA.isNullOrBlank()) {
            //D-13：没有 SHA-1 时 compareSHA1 恒为 false，旧实现会把本地已有的
            //清单直接删掉重下；离线场景下"先删除 + 下载失败"会让原本可用的版本变得不可用。
            //这里改为"能解析就复用，解析不了才重下"
            return runCatching {
                targetFile.readText().parseTo(classOfT)
            }.getOrElse {
                Logger.warning(TAG, "Local JSON is unusable and cannot be verified (no sha1), re-downloading...")
                downloadAndParse()
            }
        }

        if (compareSHA1(targetFile, expectedSHA)) {
            return runCatching {
                targetFile.readText().parseTo(classOfT)
            }.getOrElse {
                Logger.warning(TAG, "Failed to parse existing JSON, re-downloading...")
                downloadAndParse()
            }
        }

        //删除失败要留痕：随后重下会因目标被占用而失败，没有日志就完全看不出原因
        if (!FileUtils.deleteQuietly(targetFile)) {
            Logger.warning(TAG, "Failed to delete invalid JSON file: ${targetFile.absolutePath}")
        }
    }

    return downloadAndParse()
}

/**
 * 原子写入文本文件：先写同目录临时文件，再替换目标
 *
 * 用于版本清单、资源索引等"一旦截断就无法恢复"的小文件
 */
fun writeTextAtomically(targetFile: File, content: String) {
    val parent = targetFile.parentFile
    parent?.mkdirs()
    val tempFile = File(parent, "${targetFile.name}.zl2tmp")
    try {
        tempFile.writeText(content)
        if (!tempFile.renameTo(targetFile)) {
            //Windows 上目标存在时 renameTo 可能失败，先删目标再重试；
            //删除失败说明文件被占用，此时必须报错而不是留下半截内容
            if (targetFile.exists() && !targetFile.delete()) {
                throw java.io.IOException("Failed to replace occupied file: ${targetFile.absolutePath}")
            }
            if (!tempFile.renameTo(targetFile)) {
                throw java.io.IOException("Failed to move temp file into place: ${targetFile.absolutePath}")
            }
        }
    } finally {
        if (tempFile.exists()) tempFile.delete()
    }
}

fun artifactToPath(library: GameManifest.Library): String? {
    library.downloads?.artifact?.path?.let { return it }

    val libInfos = library.name.split(":")
    if (libInfos.size < 3) {
        Logger.error(TAG, "Invalid library name format: ${library.name}")
        return null
    }

    val groupId = libInfos[0].replace('.', '/')
    val artifactId = libInfos[1]
    val version = libInfos[2]

    var classifier = if (libInfos.size > 3) "-${libInfos[3]}" else ""
    if (library.isNative) run {
        val natives = library.natives ?: return@run
        if (natives.isNotEmpty()) {
            //Android，在这里使用Linux
            val native = natives[OperatingSystem.Linux] ?: return@run
            classifier = "-$native"
        }
    }

    return "$groupId/$artifactId/$version/$artifactId-$version$classifier.jar"
}

fun processLibraries(libraries: () -> List<GameManifest.Library>) {
    libraries().forEach { library ->
        if (library.filterLibrary()) return@forEach

        val versionSegment = library.name.split(":").getOrNull(2) ?: return
        val versionParts = versionSegment.split(".")

        getLibraryReplacement(library.name, versionParts)?.let { replacement ->
            Logger.debug(TAG, "Library ${library.name} has been changed to version ${replacement.newName.split(":").last()}")
            updateLibrary(library, replacement)
        }
    }
}

private fun updateLibrary(
    library: GameManifest.Library,
    replacement: LibraryReplacement
) {
    createLibraryInfo(library)
    library.name = replacement.newName
    library.downloads.artifact.apply {
        path = replacement.newPath
        sha1 = replacement.newSha1
        url = replacement.newUrl
        //新版本的大小与旧版本必然不同，残留旧值会令下载器提前截断文件
        if (replacement.newSize > 0) size = replacement.newSize
    }
}

private fun createLibraryInfo(library: GameManifest.Library) {
    if (library.downloads?.artifact == null) {
        library.downloads = GameManifest.DownloadsX().apply {
            this.artifact = GameManifest.Artifact()
        }
    }
}
