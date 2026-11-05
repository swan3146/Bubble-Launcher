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

package com.movtery.zalithlauncher.terracotta

import com.google.gson.JsonParseException
import com.movtery.zalithlauncher.path.GLOBAL_CLIENT
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.isChinaMainland
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.safeBodyAsJson
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URISyntaxException

private const val TAG = "TerracottaNodeList"

private const val NODE_LIST_URL = "https://terracotta.glavo.site/nodes"

@Serializable
private data class TerracottaNode(
    @SerialName("url")
    val url: String? = null,
    @SerialName("region")
    val region: String? = null
) {
    fun validate() {
        requireNotNull(url, { "TerracottaNode.url cannot be null" })
        try {
            URI(url)
        } catch (e: URISyntaxException) {
            throw JsonParseException("Invalid URL: $url", e)
        }
    }
}

@Volatile
private var nodeList: List<URI>? = null

private val fetchMutex = Mutex()

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/a70b8d7/HMCL/src/main/java/org/jackhuang/hmcl/terracotta/TerracottaNodeList.java)
 */
suspend fun fetchNodes(): List<URI> {
    nodeList?.let { return it }

    return fetchMutex.withLock {
        nodeList?.let { return it }

        if (AllSettings.enableTerracottaNodes.getValue()) {
            // 使用自定义的 EasyTier 服务器节点
            val url = AllSettings.terracottaNodes.getValue()
            if (url.isNotEmptyOrBlank()) {
                val tempList = listOf(URI(url))
                nodeList = tempList
                return tempList
            }
            // 为空则继续使用默认的节点逻辑
        }

        //PCL Pro Android: 拉取失败时返回空列表，此时不写入缓存，避免一次网络抖动导致本次运行周期内永远拿不到节点
        var result = withContext(Dispatchers.IO) {
            fetchNodesFromRemote()
        }
        if (result.isEmpty()) {
            //重试一次，二次仍失败也不缓存
            result = withContext(Dispatchers.IO) {
                fetchNodesFromRemote()
            }
        }
        //PCL Pro Android：按 TCP 连通延迟给节点排序，最快的排前面，native 优先连低延迟中继，建房/进房更快
        result = sortByLatency(result)
        if (result.isNotEmpty()) {
            nodeList = result
        }
        result
    }
}

/**
 * 并发测量每个节点 host:port 的 TCP 连接延迟，按延迟升序排列。
 * 超时/连不上的节点排到最后但保留（native 仍可尝试），测量本身不阻塞过久。
 */
private suspend fun sortByLatency(nodes: List<URI>): List<URI> = withContext(Dispatchers.IO) {
    if (nodes.size <= 1) return@withContext nodes

    data class Measured(val uri: URI, val latencyMs: Long)

    coroutineScope {
        val measured = nodes.map { uri ->
            async {
                val host = uri.host ?: return@async Measured(uri, Long.MAX_VALUE)
                val port = uri.takeIf { it.port > 0 }?.port ?: when (uri.scheme?.lowercase()) {
                    "wss", "https" -> 443
                    "ws", "http", "tcp" -> 80
                    else -> -1
                }
                if (port <= 0) return@async Measured(uri, Long.MAX_VALUE)

                val start = System.nanoTime()
                val ok = runCatching {
                    java.net.Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress(host, port), 800)
                    }
                }.isSuccess
                val elapsed = (System.nanoTime() - start) / 1_000_000
                Measured(uri, if (ok) elapsed else Long.MAX_VALUE)
            }
        }.map { it.await() }

        measured.sortedBy { it.latencyMs }.map { it.uri }.also {
            Logger.info(TAG, "Terracotta nodes latency-sorted: " +
                measured.joinToString { m ->
                    "${m.uri.host}=${if (m.latencyMs == Long.MAX_VALUE) "timeout" else "${m.latencyMs}ms"}"
                })
        }
    }
}

private suspend fun fetchNodesFromRemote(): List<URI> {
    return runCatching {
        val nodes = GLOBAL_CLIENT
            .get(NODE_LIST_URL)
            .safeBodyAsJson<List<TerracottaNode?>?>()

        if (nodes.isNullOrEmpty()) {
            Logger.info(TAG, "No available Terracotta nodes found")
            return emptyList()
        }

        //PCL Pro Android：自动选择节点。
        //国内用户优先使用 region=CN 的节点；若列表里一个 CN 节点都没有（远端节点更新/CN 节点全挂），
        //则退而使用全部可用节点，避免 region 过滤把列表筛成空、联机直接失败。
        val allValid = nodes.mapNotNull { node ->
            node?.let { runCatching { parseNode(it) }.getOrNull() }
        }

        val isMainLand = isChinaMainland()
        val cnNodes = allValid.filter { it.region.equals("CN", ignoreCase = true) }
        val result = when {
            !isMainLand -> allValid
            cnNodes.isNotEmpty() -> cnNodes
            else -> allValid
        }.mapNotNull { runCatching { URI.create(it.url!!) }.getOrNull() }

        Logger.info(TAG, "Terracotta nodes selected: ${result.size} (total=${allValid.size}, cn=${cnNodes.size}, mainland=$isMainLand)")
        result
    }.onFailure {
        Logger.warning(TAG, "Failed to fetch terracotta node list", it)
    }.getOrDefault(emptyList())
}

private fun parseNode(
    node: TerracottaNode
): TerracottaNode? {
    return try {
        node.validate()
        node
    } catch (e: Exception) {
        Logger.warning(TAG, "Invalid terracotta node: $node", e)
        null
    }
}
