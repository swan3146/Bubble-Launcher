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

import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "SegmentDownloader"

/**
 * 多线程分片下载器。
 *
 * 在 [Fetcher] 的单连接下载之前尝试一次：先用 Range 请求探测目标文件是否支持分段，
 * 支持则把文件切成多段并发下载到临时文件，最后整体移动到目标位置。
 * 任何一步不满足条件（不支持 Range、文件过小、校验失败等）都不会抛异常干扰主流程，
 * 而是返回 false 交回 [Fetcher] 用原有单线程逻辑完成下载。
 *
 * 计入统计的字节始终是"净落盘字节"：失败或取消时以负增量回滚已上报的量。
 */
object SegmentDownloader {
    //超时与重定向由 DOWNLOAD_OKHTTP_CLIENT（OkHttp）统一管理，
    //这里仅保留分片下载自身的缓冲与并发配置。
    private const val BUFFER_SIZE = 128 * 1024

    /** 小于该体积的文件切分收益有限，直接交给单线程下载。调低阈值让更多 Mod 走多线程 */
    private const val MIN_FILE_SIZE = 256L * 1024

    /** 单个分片的最小区间，避免切得过碎 */
    private const val MIN_SEGMENT_SIZE = 128L * 1024

    /** 单个文件的最大分片数量；与 MAX_SEGMENTED_FILES 相乘等于全局连接预算。拉满到 64 并发 */
    private const val MAX_SEGMENTS = 64

    /** 全局同时进行的连接上限，拉满到 256 */
    private const val MAX_GLOBAL_CONNECTIONS = 256

    /**
     * 同时进入分片模式的文件数上限，放宽到 32。
     */
    private const val MAX_SEGMENTED_FILES = 32

    /** 单个分片的失败重试次数 */
    private const val SEGMENT_RETRY = 3

    private val permits = Semaphore(MAX_GLOBAL_CONNECTIONS)

    private val filePermits = Semaphore(MAX_SEGMENTED_FILES)

    /**
     * 尝试以多线程分片方式下载
     *
     * @param urls 候选源，按顺序探测第一个支持分段传输的源
     * @param targetFile 目标文件；已存在且非空时不做处理（续传交由 [Fetcher] 处理）
     * @param sha1 期望的 SHA-1，非空时下载完成后校验
     * @param onBytes 落盘字节的带符号增量回调
     * @return 是否已由本方法完成下载
     */
    suspend fun tryDownload(
        urls: List<String>,
        targetFile: File,
        sha1: String? = null,
        onBytes: (Long) -> Unit = {}
    ): Boolean {
        if (urls.isEmpty()) return false
        //已有部分内容：交给 Fetcher 走续传逻辑，避免与其冲突
        if (targetFile.exists() && targetFile.length() > 0L) return false

        //探测请求本身也占用一个连接预算，且抢不到文件级名额时不做探测（D-19）
        if (!filePermits.tryAcquire()) return false
        try {
            return segmentedDownload(urls, targetFile, sha1, onBytes)
        } finally {
            filePermits.release()
        }
    }

    private suspend fun segmentedDownload(
        urls: List<String>,
        targetFile: File,
        sha1: String?,
        onBytes: (Long) -> Unit
    ): Boolean {
        val probe = permits.withPermit {
            runInterruptible(Dispatchers.IO) { probe(urls) }
        } ?: return false
        if (probe.length < MIN_FILE_SIZE) return false

        val segmentCount = minOf(
            MAX_SEGMENTS,
            (probe.length / MIN_SEGMENT_SIZE).toInt().coerceAtLeast(1)
        )
        if (segmentCount < 2) return false

        val parent = targetFile.absoluteFile.parentFile ?: return false
        if (!parent.exists() && !parent.mkdirs()) return false

        val tempFile = File(parent, "${targetFile.name}.segpart")
        if (tempFile.exists() && !tempFile.delete()) return false

        var countedBytes = 0L
        val report: (Long) -> Unit = { delta ->
            countedBytes += delta
            onBytes(delta)
        }

        val segments = buildSegments(probe.length, segmentCount)
        Logger.info(TAG, "Download in $segmentCount segments: ${probe.url} (${probe.length} bytes)")

        try {
            coroutineScope {
                FileChannel.open(
                    tempFile.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { channel ->
                    val jobs = segments.map { segment ->
                        launch {
                            permits.withPermit {
                                downloadSegment(
                                    url = probe.url,
                                    channel = channel,
                                    start = segment.first,
                                    endInclusive = segment.last,
                                    report = report
                                )
                            }
                        }
                    }
                    //等待所有分片写完，此期间保持通道打开
                    jobs.forEach { it.join() }
                }
            }
        } catch (e: CancellationException) {
            report(-countedBytes)
            runCatching { tempFile.delete() }
            throw e
        } catch (e: Throwable) {
            Logger.warning(TAG, "Segmented download failed, fallback to single connection", e)
            report(-countedBytes)
            runCatching { tempFile.delete() }
            return false
        }

        val expectedSha1 = sha1?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (expectedSha1 != null) {
            val actual = runInterruptible(Dispatchers.IO) { tempFile.sha1() }
            if (!actual.equals(expectedSha1, ignoreCase = true)) {
                Logger.warning(TAG, "Segmented download checksum mismatch, fallback to single connection")
                report(-countedBytes)
                runCatching { tempFile.delete() }
                return false
            }
        }

        try {
            runInterruptible(Dispatchers.IO) {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (e: CancellationException) {
            report(-countedBytes)
            runCatching { tempFile.delete() }
            throw e
        } catch (e: Throwable) {
            Logger.warning(TAG, "Failed to move segmented download result", e)
            report(-countedBytes)
            runCatching { tempFile.delete() }
            return false
        }

        Logger.info(TAG, "Segmented download finished: ${targetFile.name} (${targetFile.length()} bytes)")
        return true
    }

    private class ProbeResult(val url: String, val length: Long)

    /** 探测候选源是否支持分段传输及其总长度 */
    private fun probe(urls: List<String>): ProbeResult? {
        for (candidate in urls) {
            var response: Response? = null
            try {
                val request = Request.Builder()
                    .url(candidate)
                    .header("range", "bytes=0-0")
                    .header("accept-encoding", "identity")
                    .build()
                response = DOWNLOAD_OKHTTP_CLIENT.newCall(request).execute()
                val code = response.code
                if (code != 206) continue
                val contentRange = response.header("Content-Range")
                val total = contentRange
                    ?.substringAfterLast('/')
                    ?.trim()
                    ?.toLongOrNull()
                    ?: -1L
                if (total <= 0L) continue
                //跟随重定向后的最终地址，后续分片直接命中
                val resolved = response.request.url.toString()
                return ProbeResult(resolved, total)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            } catch (e: Exception) {
                Logger.warning(TAG, "Failed to probe range support: $candidate", e)
            } finally {
                response?.close()
            }
        }
        return null
    }

    private fun buildSegments(length: Long, segmentCount: Int): List<LongRange> {
        val segmentSize = length / segmentCount
        return (0 until segmentCount).map { index ->
            val start = index * segmentSize
            val end = if (index == segmentCount - 1) length - 1 else (start + segmentSize - 1)
            start..end
        }
    }

    private suspend fun downloadSegment(
        url: String,
        channel: FileChannel,
        start: Long,
        endInclusive: Long,
        report: (Long) -> Unit
    ) {
        var position = start
        var attempt = 0
        while (position <= endInclusive) {
            try {
                position = runInterruptible(Dispatchers.IO) {
                    transferSegment(url, channel, position, endInclusive, report)
                }
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (attempt++ >= SEGMENT_RETRY) throw e
                Logger.warning(TAG, "Segment [$start-$endInclusive] failed, retrying", e)
                delay((200L * attempt).milliseconds)
            }
        }
    }

    /**
     * 下载一段区间并写入通道，返回已写到的位置（写完时为 endInclusive + 1）
     */
    private fun transferSegment(
        url: String,
        channel: FileChannel,
        start: Long,
        endInclusive: Long,
        report: (Long) -> Unit
    ): Long {
        val request = Request.Builder()
            .url(url)
            .header("range", "bytes=$start-$endInclusive")
            .header("accept-encoding", "identity")
            .build()
        DOWNLOAD_OKHTTP_CLIENT.newCall(request).execute().use { connection ->
            val code = connection.code
            if (code == 200) {
                throw IOException("Server ignored range request")
            }
            if (code != 206) {
                throw IOException("Unexpected http code: $code")
            }

            var position = start
            val buffer = ByteArray(BUFFER_SIZE)
            val input = connection.body.byteStream()
            input.use { stream ->
                while (true) {
                    if (Thread.currentThread().isInterrupted) {
                        throw InterruptedIOException("Segment download interrupted")
                    }
                    val read = stream.read(buffer)
                    if (read < 0) break
                    var offset = 0
                    while (offset < read) {
                        val written = channel.write(
                            ByteBuffer.wrap(buffer, offset, read - offset),
                            position + offset
                        )
                        if (written <= 0) throw IOException("Failed to write segment data")
                        offset += written
                    }
                    position += read
                    report(read.toLong())
                }
            }
            if (position <= endInclusive) {
                throw IOException("Segment truncated: $position <= $endInclusive")
            }
            return position
        }
    }

    private fun File.sha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}
