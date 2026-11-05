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

import com.movtery.zalithlauncher.game.download.engine.BatchDownloader
import com.movtery.zalithlauncher.game.download.engine.DownloadRequest
import com.movtery.zalithlauncher.utils.file.check7z
import com.movtery.zalithlauncher.utils.file.checkZip
import com.movtery.zalithlauncher.utils.file.compareSHA1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * 一个待下载文件的完整描述
 * 候选源、目标位置与校验方式
 * 批量执行交给 [BatchDownloader]，单个文件也可以通过 [com.movtery.zalithlauncher.game.download.engine.DownloadEngine] 直接驱动
 */
class DownloadTask(
    val urls: List<String>,
    private val verifyIntegrity: Boolean,
    val targetFile: File,
    val sha1: String? = null,
    /** 已知大小，用于进度统计与预分配；未知传 -1 */
    val size: Long = -1L,
    /**
     * 是否本身是可以被下载的，如果不可下载，则只允许以目标文件已存在的形式满足，
     * 若强行下载会以 404 失败
     */
    val isDownloadable: Boolean = true
) {
    init {
        require(urls.isNotEmpty()) { "DownloadTask requires at least one url" }
    }

    var fileDownloadedTask: (suspend () -> Unit)? = null

    internal fun toRequest(): DownloadRequest = DownloadRequest(
        urls = urls,
        targetFile = targetFile,
        sha1 = sha1,
        expectedSize = size,
        tag = this
    )

    internal suspend fun runFileDownloadedTask() {
        withContext(Dispatchers.IO) {
            fileDownloadedTask?.invoke()
        }
    }

    /** 目标已存在且校验可用时返回 true */
    fun existingFileValid(): Boolean {
        val file = targetFile
        if (!file.exists() || !file.isFile) return false

        //空文件必然是异常中断/写盘失败的产物，任何校验都不可能通过（D-04）
        if (file.length() <= 0L) {
            FileUtils.deleteQuietly(file)
            return false
        }

        if (!verifyIntegrity) return true

        if (sha1.isNullOrBlank()) {
            //D-04：没有 SHA-1 时不能再"存在即有效"，否则一次异常中断留下的半截文件
            //会在后续每次安装/修复时被判为可复用，下载器永不重新拉取（版本永久损坏）
            //① 压缩包类（jar/zip/7z）做结构完整性校验；超大包在 checkZip/check7z 内部会降级，不会卡死
            if (!archiveValid(file)) {
                removeInvalidFile(file)
                return false
            }
            //② 声明了大小的普通文件比对体积；大小未知时只接受非空文件（上面已挡掉 0 字节）
            if (size > 0L && file.length() != size) {
                removeInvalidFile(file)
                return false
            }
            return true
        }

        if (compareSHA1(file, sha1)) return true
        removeInvalidFile(file)
        return false
    }

    /**
     * D-06：删除校验失败的文件，但不再"静默"。
     *
     * 删除失败通常意味着文件正被占用（Windows 上被运行中的 JVM mmap 的 jar、
     * 被解压流持有的 zip）。此时若照常把任务排进下载队列，会白白消耗完整流量，
     * 最后在 Files.move 替换阶段以 AccessDeniedException 收场；直接在这里报出
     * 可读原因，让上层立即失败并提示用户关闭正在运行的实例
     */
    private fun removeInvalidFile(file: File) {
        FileUtils.deleteQuietly(file)
        if (file.exists()) throw FileOccupiedException(file)
    }

    private fun archiveValid(file: File): Boolean = when (file.extension.lowercase()) {
        "zip", "jar" -> checkZip(file)
        "7z" -> check7z(file)
        else -> true
    }
}

/** 本地文件校验失败且无法删除（通常是被其它进程占用） */
class FileOccupiedException(val file: File) : IOException(
    "Local file is invalid but cannot be replaced, it may be in use by another process: ${file.absolutePath}"
)
