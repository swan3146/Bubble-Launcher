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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "Fetcher"

/** 响应的状态码、最终地址与响应头集合（键不区分大小写） */
class ResponseInfo(
    val code: Int,
    val url: URL,
    headers: Map<String, List<String>>
) {
    private val values: Map<String, String> = headers.entries
        .associate { it.key.lowercase() to it.value.first() }

    fun header(name: String): String? = values[name.lowercase()]

    fun headerAsLong(name: String): Long = header(name)?.trim()?.toLongOrNull() ?: -1L

    companion object {
        /**
         * 从 OkHttp 响应头构建 [ResponseInfo]（键不区分大小写）
         */
        fun of(code: Int, url: URL, headers: Headers): ResponseInfo {
            val map = LinkedHashMap<String, MutableList<String>>()
            for (i in 0 until headers.size) {
                val name = headers.name(i)
                val value = headers.value(i)
                map.getOrPut(name) { mutableListOf() }.add(value)
            }
            return ResponseInfo(code, url, map)
        }
    }
}

/** 响应体传输编码；不支持 broker 压缩（如 br），遇到即视为该次尝试失败 */
enum class ContentEncoding {
    IDENTITY, GZIP;

    fun wrap(input: InputStream): InputStream =
        if (this == GZIP) GZIPInputStream(input) else input

    companion object {
        fun fromHeaders(response: ResponseInfo): ContentEncoding {
            val encoding = response.header("content-encoding") ?: return IDENTITY
            return when {
                encoding.isEmpty() || encoding == "identity" -> IDENTITY
                encoding.equals("gzip", ignoreCase = true) -> GZIP
                //D-23：不支持的编码无法解码，明确说明原因（重试无意义，交由上层换源）
                else -> throw IOException(
                    "Unsupported content encoding: $encoding (server forced it despite accept-encoding: identity)"
                )
            }
        }
    }
}

/**
 * Range 续传的资格上下文：只有 200 OK + accept-ranges: bytes + identity 编码 + 已知长度，
 * 且携带强 ETag 或 Last-Modified 的响应才具备续传资格。
 */
class ResumeContext private constructor(
    private val url: URL,
    private val contentLength: Long,
    private val strongETag: String?,
    private val lastModified: String?
) {
    /** 未压缩口径的已收字节，与 Content-Range 的起点语义对齐 */
    var countUncompressed = 0L
        private set

    fun addBytes(count: Long) {
        countUncompressed += count
    }

    fun ifRange(): String = strongETag ?: lastModified!!

    /** 206 响应是否与既有分段匹配：校验长度、验证器与 Content-Range 的逐字段一致性 */
    fun canResume(code: Int, response: ResponseInfo): Boolean {
        if (code != 206) return false
        if (ContentEncoding.fromHeaders(response) != ContentEncoding.IDENTITY) return false

        val bodyLength = response.headerAsLong("content-length")
        if (contentLength != bodyLength + countUncompressed) return false

        if (strongETag != null) {
            if (strongETag != response.header("etag")) return false
        } else {
            if (url != response.url) return false
            if (lastModified != response.header("last-modified").orEmpty()) return false
        }

        val match = CONTENT_RANGE_PATTERN.find(response.header("content-range").orEmpty())
            ?: return false
        val (start, end, total) = match.destructured
        val startValue = start.toLongOrNull() ?: return false
        val endValue = end.toLongOrNull() ?: return false
        val totalValue = total.toLongOrNull() ?: return false

        if (startValue != countUncompressed || endValue < startValue || totalValue != contentLength) {
            return false
        }
        return endValue - startValue + 1 == bodyLength
    }

    fun hasPartialContent(): Boolean = countUncompressed in 1 until contentLength

    companion object {
        private val CONTENT_RANGE_PATTERN = Regex("""bytes ([0-9]+)-([0-9]+)/([0-9]+)""")

        fun of(response: ResponseInfo): ResumeContext? {
            if (response.code != 200) return null
            if (!response.header("accept-ranges").orEmpty().equals("bytes", ignoreCase = true)) return null
            if (ContentEncoding.fromHeaders(response) != ContentEncoding.IDENTITY) return null

            val contentLength = response.headerAsLong("content-length")
            if (contentLength < 0) return null

            val eTag = response.header("etag")
            val strongETag = eTag?.takeIf { it.isNotBlank() && !it.startsWith("W/", ignoreCase = true) }
            val lastModified = response.header("last-modified").orEmpty()
            if (strongETag == null && lastModified.isBlank()) return null

            return ResumeContext(response.url, contentLength, strongETag, lastModified)
        }
    }
}

/** 单个候选源重试耗尽后的失败包装 */
class DownloadException(
    val url: String,
    cause: Throwable
) : IOException("Unable to download $url, ${cause.message}", cause)

/** 下载内容的校验和与声明不符 */
class ChecksumMismatchException(
    val algorithm: String,
    val expected: String,
    val actual: String
) : IOException("Checksum mismatch ($algorithm): expected=$expected, actual=$actual")

/**
 * [Modified from HMCL FetchTask](https://github.com/HMCL-dev/HMCL/blob/59bcc7fe/HMCLCore/src/main/java/org/jackhuang/hmcl/task/FetchTask.java)
 *
 * 候选源顺序下载的核心实现：单源内至多重试 [DEFAULT_RETRY] 次（间隔 200ms），
 * 手动跟随重定向（上限 20 跳），Range 断点续传（强校验），连接与读取超时 10s；
 * 4xx 视为该源无货（不重试、立即换下一候选），写盘损坏从头重试且不消耗重试次数。
 * 失败重试中被丢弃的字节会从统计中回滚，[Fetcher.downloadFile] 的 onBytes 回调因此可能收到负增量。
 */
object Fetcher {
    const val DEFAULT_RETRY = 5

    //连接/读超时与重定向由 DOWNLOAD_OKHTTP_CLIENT（OkHttp）统一管理。
    private const val RETRY_BACKOFF_MILLIS = 200L
    private const val BUFFER_SIZE = 128 * 1024

    /** 续传失效导致"不计次数的从头重来"的绝对上限（D-18） */
    private const val MAX_RESTART_FREE = 32

    /**
     * 按候选源顺序下载文件；全部候选耗尽后抛 [AllSourcesFailedException]，
     * message 内含每个源，因果链保留最后一次失败。
     *
     * @param onBytes 落盘字节的带符号增量回调（未压缩口径）：失败重试丢弃的字节以负增量回滚，
     * 全部增量的代数和等于最终落盘大小
     */
    suspend fun downloadFile(
        urls: List<String>,
        targetFile: File,
        sha1: String? = null,
        retry: Int = DEFAULT_RETRY,
        onBytes: (Long) -> Unit = {}
    ) {
        require(urls.isNotEmpty()) { "At least one URL is required" }
        require(retry > 0) { "Retry count must be greater than 0" }

        val failures = mutableListOf<DownloadException>()
        for (candidate in urls) {
            val url = try {
                URL(candidate)
            } catch (e: Exception) {
                failures += DownloadException(candidate, IOException("Invalid URL", e))
                continue
            }
            try {
                downloadCandidate(url, targetFile, sha1, retry, onBytes)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: DownloadException) {
                Logger.warning(TAG, "Candidate source failed: $candidate", e)
                failures += e
            }
        }

        val last = failures.removeAt(failures.lastIndex)
        failures.forEach { last.addSuppressed(it) }
        val detail = urls.joinToString(separator = "\n") { "- $it" }
        throw AllSourcesFailedException("All ${urls.size} candidate source(s) failed:\n$detail", last)
    }

    /** 单个候选源的跨尝试状态：落盘上下文与续传资格在重试间延续 */
    private class CandidateState(
        private val onBytes: (Long) -> Unit
    ) {
        var sink: FileSink? = null
        var resume: ResumeContext? = null

        /** 当前 sink 存续期间已上报统计的字节数 */
        private var countedBytes = 0L

        /** 落盘字节增量上报，正负均可（负数为回滚） */
        fun reportBytes(delta: Long) {
            countedBytes += delta
            onBytes(delta)
        }

        /** 丢弃落盘上下文：关闭并删除临时文件，并回滚该 sink 已计入统计的字节 */
        fun discardSink() {
            val current = sink
            if (current != null) {
                runCatching { current.close() }
                sink = null
            }
            if (countedBytes != 0L) {
                onBytes(-countedBytes)
                countedBytes = 0L
            }
        }

        /**
         * 失败收尾：关闭通道但**保留**分段文件，下一次进程启动后可继续续传（D-05）；
         * 已上报的字节按净值回滚，保证统计口径不虚高
         */
        fun persistSink() {
            val current = sink
            if (current != null) {
                runCatching { current.close(deletePart = false) }
                sink = null
            }
            if (countedBytes != 0L) {
                onBytes(-countedBytes)
                countedBytes = 0L
            }
        }

        /** 下载成功提交：已计入的字节随目标文件落地，此后不再回滚 */
        fun commitSink() {
            sink = null
            resume = null
            countedBytes = 0L
        }
    }

    private enum class AttemptResult { DONE, RESTART_FREE }

    private suspend fun downloadCandidate(
        url: URL,
        targetFile: File,
        sha1: String?,
        retry: Int,
        onBytes: (Long) -> Unit
    ) {
        val state = CandidateState(onBytes)
        val exceptions = mutableListOf<Exception>()
        var retryLimit = retry
        var retryTime = 0
        var restartFreeCount = 0
        try {
            while (retryTime < retryLimit) {
                currentCoroutineContext().ensureActive()
                Logger.info(TAG, "Download started: $url")
                val attempt = retryTime
                retryTime++

                try {
                    val result = runInterruptible(Dispatchers.IO) {
                        attempt(url, targetFile, sha1, state)
                    }
                    if (result == AttemptResult.DONE) return
                    //续传失效或 416：从头重来且不消耗重试次数，
                    //但必须设绝对上限，否则异常服务端（反复 200/416）会让循环无界（D-18）
                    restartFreeCount++
                    if (restartFreeCount > MAX_RESTART_FREE) {
                        throw IOException("Too many restart attempts ($restartFreeCount) for $url")
                    }
                    retryLimit++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: HttpResultException) {
                    Logger.warning(TAG, "Download failed: $url", e)
                    if (e.code in 400..499) throw DownloadException(url.toString(), e)
                    exceptions += e
                    if (attempt < retryLimit - 1) delay(RETRY_BACKOFF_MILLIS.milliseconds)
                } catch (e: Exception) {
                    Logger.warning(TAG, "Download failed: $url", e)
                    exceptions += e
                    if (attempt < retryLimit - 1) delay(RETRY_BACKOFF_MILLIS.milliseconds)
                }
            }
        } finally {
            //D-05：单个候选源失败后不删分段文件，留给下一次进程续传
            state.persistSink()
        }

        val last = exceptions.lastOrNull() ?: IOException("No exceptions")
        exceptions.dropLast(1).forEach { last.addSuppressed(it) }
        throw DownloadException(url.toString(), last)
    }

    /** 一次完整尝试：手动跟随重定向、处理续传握手、流式落盘并提交。阻塞执行，取消依赖线程中断 */
    private fun attempt(
        url: URL,
        targetFile: File,
        sha1: String?,
        state: CandidateState
    ): AttemptResult {
        //D-05：进入尝试前先准备落盘上下文；首次创建时会顺带发现并复用
        //上次进程遗留的 .zl2part 分段，从而实现跨进程断点续传
        if (state.sink == null) {
            state.sink = FileSink(targetFile, sha1)
        }
        val leftover = if (state.resume == null) state.sink!!.resumedBytes else 0L

        //D-15：只声明 identity，避免服务端返回 gzip 后与"仅 identity 才能续传"的设计冲突
        val headers = linkedMapOf("accept-encoding" to "identity")
        val resumeRequested = state.resume?.hasPartialContent() == true
        if (resumeRequested) {
            val resume = state.resume!!
            headers["range"] = "bytes=${resume.countUncompressed}-"
            headers["if-range"] = resume.ifRange()
        } else if (leftover > 0L) {
            //遗留分段没有可用的验证器，只按偏移请求，响应回来后再校验起点
            headers["range"] = "bytes=$leftover-"
        }

        // 使用 OkHttp 连接池复用（DOWNLOAD_OKHTTP_CLIENT 已配置 256 连接池 + 容灾 DNS），
        // 自动跟随重定向（301/302/303/307/308），海量小文件下载时省去反复 TCP 握手。
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }

        val conn: Response = DOWNLOAD_OKHTTP_CLIENT.newCall(requestBuilder.build()).execute()
        val response: ResponseInfo = try {
            ResponseInfo.of(conn.code, url, conn.headers)
        } catch (e: Throwable) {
            conn.close()
            throw e
        }

        val code = response.code

        if (resumeRequested && code == 416 /* HTTP_RANGE_NOT_SATISFIABLE */) {
            //已有分段不再可用：丢弃续传上下文，从头下载
            state.resume = null
            state.discardSink()
            conn.close()
            return AttemptResult.RESTART_FREE
        }
        if (code / 100 == 4) {
            conn.close()
            throw HttpResultException(code, "HTTP $code: $url")
        }
        if (code / 100 != 2) {
            conn.close()
            throw HttpResultException(code, "HTTP $code: $url")
        }

        val contentLength = response.headerAsLong("content-length")
        val contentEncoding = ContentEncoding.fromHeaders(response)

        if (!resumeRequested && leftover > 0L) {
            //D-05：遗留分段的两条结局
            if (code == HTTP_PARTIAL) {
                val start = contentRangeStart(response)
                if (start != leftover) {
                    //起点对不上，遗留内容不可信：清掉，下一轮从零开始
                    state.discardSink()
                    state.sink = FileSink(targetFile, sha1)
                    conn.close()
                    return AttemptResult.RESTART_FREE
                }
                Logger.info(TAG, "Resuming from leftover part: $url @$leftover")
                state.resume = null
            } else {
                //服务器忽略了 Range：丢弃遗留分段，用当前响应体从头写
                Logger.info(TAG, "Server ignored range for leftover part, restarting: $url")
                state.discardSink()
                state.sink = FileSink(targetFile, sha1)
                state.resume = ResumeContext.of(response)
            }
        } else if (resumeRequested) {
            if (state.resume!!.canResume(code, response)) {
                Logger.info(TAG, "Resuming $url from ${state.resume!!.countUncompressed}")
            } else {
                //服务器没能按段续传：丢弃已有内容，从头开始
                state.resume = null
                state.discardSink()
                state.sink = FileSink(targetFile, sha1)
                conn.close()
                return AttemptResult.RESTART_FREE
            }
        } else {
            //既无续传上下文也无遗留分段：若上一轮已写入部分内容（服务端不支持 Range），
            //无法确定偏移，只能作废重来
            if (state.sink!!.writtenBytes > 0L) {
                state.discardSink()
                state.sink = FileSink(targetFile, sha1)
            }
            state.resume = ResumeContext.of(response)
        }

        try {
            //OkHttp 的 body 在 2xx 响应下非空（非 2xx 已在上面抛 HttpResultException）
            conn.body.byteStream().use { input ->
                transfer(state.sink!!, state.resume, input, contentLength, contentEncoding, state::reportBytes)
            }
        } catch (e: Throwable) {
            if (state.sink?.broken == true) {
                //写盘失败（磁盘错误等）：整个上下文作废，重试从零开始
                state.resume = null
                state.discardSink()
            }
            throw e
        } finally {
            conn.close()
        }

        try {
            state.sink!!.finish()
        } catch (e: Throwable) {
            state.resume = null
            state.discardSink()
            throw e
        }
        state.commitSink()
        return AttemptResult.DONE
    }

    /** 解析 Content-Range 的起始偏移（仅用于复用遗留分段时的起点校验） */
    private fun contentRangeStart(response: ResponseInfo): Long {
        val value = response.header("content-range").orEmpty()
        val match = Regex("""bytes ([0-9]+)-""").find(value) ?: return -1L
        return match.groupValues[1].toLongOrNull() ?: -1L
    }

    private fun transfer(
        sink: FileSink,
        resume: ResumeContext?,
        rawInput: InputStream,
        contentLength: Long,
        contentEncoding: ContentEncoding,
        onBytes: (Long) -> Unit
    ) {
        contentEncoding.wrap(rawInput).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var written = 0L
            while (true) {
                if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Cancelled")
                val len = input.read(buffer)
                if (len == -1) break
                sink.write(buffer, 0, len)
                resume?.addBytes(len.toLong())
                written += len
                onBytes(len.toLong())
            }
            if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Cancelled")

            //gzip 响应的 content-length 是压缩口径，与落盘字节数不可比，截断由 GZIPInputStream 的尾部校验兜底
            if (contentEncoding == ContentEncoding.IDENTITY && contentLength >= 0 && written != contentLength) {
                throw IOException("Unexpected file size: $written, expected: $contentLength")
            }
        }
    }

    /** HTTP 206 Partial Content */
    private const val HTTP_PARTIAL = 206
}
