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

package com.movtery.zalithlauncher.game.download.engine

import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * [Modified from HMCL FileDownloadTask](https://github.com/HMCL-dev/HMCL/blob/59bcc7fe/HMCLCore/src/main/java/org/jackhuang/hmcl/task/FileDownloadTask.java)
 *
 * 下载落盘上下文：先写入目标同目录下的临时文件，成功收尾时校验 SHA-1
 * 并原子替换目标文件；失败丢弃时删除临时文件。
 * 写盘失败由 [broken] 标记，调用方据此在重试时放弃续传、从头开始。
 */
class FileSink(
    targetFile: File,
    private val expectedChecksum: String?
) : Closeable {
    private val target: Path = targetFile.toPath()
    private val temp: Path
    private val channel: FileChannel
    private val digest: MessageDigest? = expectedChecksum?.let { MessageDigest.getInstance("SHA-1") }

    /**
     * D-05：复用上次进程遗留的分段文件时，其中已有的字节数；
     * 为 0 表示本次从零开始写
     */
    var resumedBytes: Long = 0L
        private set

    /** 最近一次写盘是否失败；为真时既有内容不可信，重试不得续传 */
    var broken = false
        private set

    /** 本次进程内实际写入的字节数（不含复用的遗留分段） */
    var writtenBytes = 0L
        private set

    init {
        val parent = target.toAbsolutePath().parent
        Files.createDirectories(parent)

        //D-05：临时文件使用确定性名字（<目标名>.zl2part），不再是随机文件名。
        //这样进程被杀后遗留的分段可以被下一次下载发现并续传，也不会在 assets/libraries
        //目录里散落大量无扩展名、无从排查的垃圾文件
        temp = parent.resolve(target.fileName.toString() + PART_SUFFIX)

        val existing = runCatching {
            if (Files.isRegularFile(temp)) Files.size(temp) else 0L
        }.getOrDefault(0L)
        resumedBytes = existing.coerceAtLeast(0L)

        //不截断：保留已有内容供续传；写入位置移动到已有内容末尾
        channel = FileChannel.open(
            temp,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE
        )
        if (resumedBytes > 0L) {
            channel.position(resumedBytes)
            //续传时摘要必须覆盖已有字节，否则最终 SHA-1 必然不匹配
            digest?.let { d -> feedDigest(d, temp, resumedBytes) }
        }
    }

    /** 把已有分段内容喂给摘要，保证续传后的 SHA-1 仍然覆盖整个文件 */
    private fun feedDigest(d: MessageDigest, path: Path, length: Long) {
        runCatching {
            FileChannel.open(path, StandardOpenOption.READ).use { readChannel ->
                val buffer = ByteBuffer.allocate(64 * 1024)
                var remaining = length
                while (remaining > 0) {
                    buffer.clear()
                    buffer.limit(minOf(buffer.capacity().toLong(), remaining).toInt())
                    val read = readChannel.read(buffer)
                    if (read <= 0) break
                    d.update(buffer.array(), 0, read)
                    remaining -= read
                }
            }
        }.onFailure {
            //读不出来就退化为从头下载，不能带着不可信的摘要继续；
            //同时把 resumedBytes 归零，否则调用方仍会按旧长度发 Range 请求，
            //而通道已回到起点，写入位置与请求偏移会错位
            Logger.warning(TAG, "Unable to read existing part file for checksum, discarding it", it)
            runCatching { channel.truncate(0L); channel.position(0L) }
            resumedBytes = 0L
        }
    }

    fun write(buffer: ByteArray, offset: Int, length: Int) {
        try {
            digest?.update(buffer, offset, length)
            val wrapped = ByteBuffer.wrap(buffer, offset, length)
            while (wrapped.hasRemaining()) {
                channel.write(wrapped)
            }
            writtenBytes += length
        } catch (e: IOException) {
            broken = true
            throw e
        }
    }

    /** 下载成功后的收尾：校验 SHA-1 并把临时文件原子替换为目标文件；抛出视为可重试失败 */
    fun finish() {
        var moved = false
        try {
            channel.close()

            if (expectedChecksum != null) {
                val actual = digest!!.digest().toHexString()
                if (!expectedChecksum.equals(actual, ignoreCase = true)) {
                    throw ChecksumMismatchException("SHA-1", expectedChecksum, actual)
                }
            }

            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                //D-06：目标被其它进程（或正在运行的 JVM）占用时，给出可读的原因，
                //而不是把裸的 AccessDeniedException 埋在 "All candidate source(s) failed" 后面
                throw IOException(
                    "Unable to replace \"${target.fileName}\" (the file may be in use by another process)",
                    e
                )
            }
            moved = true
        } finally {
            if (!moved) deleteTemp()
        }
    }

    override fun close() {
        close(deletePart = true)
    }

    /**
     * 关闭落盘通道；[deletePart] 为 false 时保留分段文件，
     * 供下次进程启动后继续续传（D-05）
     */
    fun close(deletePart: Boolean) {
        runCatching { channel.close() }
        if (deletePart) deleteTemp()
    }

    private fun deleteTemp() {
        runCatching { Files.deleteIfExists(temp) }
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }

    companion object {
        private const val TAG = "FileSink"

        /** D-05：确定性分段文件后缀 */
        private const val PART_SUFFIX = ".zl2part"

        /** 目标文件对应的分段文件路径 */
        fun partFileOf(targetFile: File): Path =
            targetFile.toPath().toAbsolutePath().parent
                ?.resolve(targetFile.name + PART_SUFFIX)
                ?: targetFile.toPath().resolveSibling(targetFile.name + PART_SUFFIX)

        /** 已存在的分段文件长度，0 表示没有可复用的内容 */
        fun existingPartLength(targetFile: File): Long = runCatching {
            val part = partFileOf(targetFile)
            if (Files.isRegularFile(part)) Files.size(part) else 0L
        }.getOrDefault(0L)

        /** 丢弃分段文件（服务器不认续传时使用） */
        fun discardPart(targetFile: File) {
            runCatching { Files.deleteIfExists(partFileOf(targetFile)) }
        }
    }
}
