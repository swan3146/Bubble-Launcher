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
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.launch

import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.components.jre.JreDownloadSource
import com.movtery.zalithlauncher.components.jre.RuntimeDownloader
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.readString
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "RuntimeResolver"

/**
 * 运行环境不可用（缺失 / arch 未知 / 目录不完整 / 内置环境解压失败）
 */
class RuntimeUnavailableException(message: String) : Exception(message)

/**
 * 启动前的 Java 运行环境解析器。
 *
 * 选定运行环境不可用时不再直接抛 IOException 让启动器崩溃，而是：
 * 1. 优先在已安装运行环境里挑一个满足版本要求且真正可用的；
 * 2. 都没有则按需解压内置 JRE（Internal-8/17/21/25）；
 * 3. 仍然不可用才抛出 [RuntimeUnavailableException]，由上层以对话框提示用户。
 */
object RuntimeResolver {
    /**
     * 当前设备是否为 x86 / x86_64（模拟器与部分虚拟化环境）
     */
    private val isX86Device: Boolean
        get() = ZLApplication.DEVICE_ARCHITECTURE == Architecture.ARCH_X86 ||
                ZLApplication.DEVICE_ARCHITECTURE == Architecture.ARCH_X86_64

    /**
     * x86 / x86_64 上内置的 JRE 25（OpenJDK 25 linux-amd64）在模拟器、以及不支持相应向量指令的
     * 虚拟化 CPU 上会一启动就触发 SIGILL：JVM 致命错误落在
     * StubRoutines::method_entry_barrier，进程随即被 exit hook 以 SIGKILL 收掉，
     * 表现为点启动后瞬间闪退、版本目录留下 hs_err_pid*.log。
     * 这类设备一律避开 25，回退到 21（1.21.11 起的最低要求）。
     */
    fun isUnsafeOnDevice(javaVersion: Int): Boolean = isX86Device && javaVersion >= 25

    /**
     * 运行环境是否真正可用：版本信息可读、架构匹配当前设备、目录存在
     */
    fun isUsable(runtime: Runtime): Boolean {
        if (runtime.versionString.isNullOrBlank()) return false
        if (runtime.arch.isNullOrBlank()) return false
        if (!runtime.isCompatible()) return false
        if (isUnsafeOnDevice(runtime.javaVersion)) return false
        val home = runCatching { RuntimesManager.getRuntimeHome(runtime.name) }.getOrNull()
        return home != null && home.isDirectory
    }

    /**
     * 解析出一个可用的运行环境名
     *
     * @param preferred 用户在设置 / 版本独立设置里选定的运行环境名
     * @param targetJavaVersion 当前版本需要的 Java 大版本
     * @param onMessage 解压内置环境时的进度回调（后台线程）
     */
    suspend fun resolve(
        context: Context,
        preferred: String,
        targetJavaVersion: Int,
        onMessage: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        RuntimesManager.loadRuntime(preferred).let { runtime ->
            if (isUsable(runtime)) return@withContext preferred
            Logger.warning(
                TAG,
                "Runtime \"$preferred\" is unusable (version=${runtime.versionString}, arch=${runtime.arch}), trying fallback"
            )
        }

        val usable = RuntimesManager.getRuntimes(forceLoad = true).filter { isUsable(it) }
        pickBest(usable, targetJavaVersion)?.let {
            Logger.info(TAG, "Fallback to installed runtime: ${it.name} (java ${it.javaVersion})")
            return@withContext it.name
        }

        val jre = pickInternalJre(context, targetJavaVersion)
        if (jre != null) {
            onMessage("Preparing Java runtime: ${jre.jreName}")
            installInternalJre(context, jre, onMessage)

            val installed = RuntimesManager.forceReload(jre.jreName)
            if (isUsable(installed)) {
                Logger.info(TAG, "Installed built-in runtime ${jre.jreName}, using it")
                return@withContext installed.name
            }

            Logger.warning(
                TAG,
                "Built-in runtime ${jre.jreName} could not be prepared (version=${installed.versionString}, arch=${installed.arch}), trying online download"
            )
        } else {
            Logger.warning(
                TAG,
                "No usable Java runtime found and no built-in JRE matches Java $targetJavaVersion, trying online download"
            )
        }

        //内置 JRE 也不可用：尝试从网络自动下载合适的 Java 版本（国内镜像优先、GitHub 兜底）
        val downloaded = downloadFallback(targetJavaVersion, onMessage)
        if (downloaded != null) {
            Logger.info(TAG, "Downloaded runtime $downloaded, using it")
            return@withContext downloaded
        }

        throw RuntimeUnavailableException(
            "No usable Java runtime found: built-in JRE is unavailable and Java $targetJavaVersion has no downloadable source on this device architecture."
        )
    }

    /**
     * 启动时预热：把内置的全部 JRE（8/17/21/25）在后台解压出来。
     * 已解压且可用的直接跳过，避免重复解压占满磁盘 IO。
     * 任意一个失败不影响其它，也不影响启动流程——真正启动游戏时 [resolve] 仍会兜底。
     * x86 系设备上 25 会 SIGILL，按 [pickInternalJre] 同样的规则跳过 25。
     */
    suspend fun warmUpAllBuiltIn(context: Context, onMessage: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        Jre.entries.forEach { jre ->
            runCatching {
                //已安装且可用则跳过
                val existing = RuntimesManager.forceReload(jre.jreName)
                if (isUsable(existing)) return@forEach

                //架构没有对应 bin 包（如部分老设备没有 jre-25）或 x86 上 25 不安全，跳过
                if (!isArchSupported(context.assets, jre)) return@forEach
                if (isUnsafeOnDevice(jre.majorVersion)) return@forEach

                onMessage("Preparing Java runtime: ${jre.jreName}")
                installInternalJre(context, jre, onMessage)
                Logger.info(TAG, "Warmed up built-in runtime ${jre.jreName}")
            }.onFailure {
                Logger.warning(TAG, "Failed to warm up built-in runtime ${jre.jreName}", it)
            }
        }
    }

    /**
     * 从网络自动下载目标 Java 大版本的运行环境。
     * 无可用下载源、或下载/安装失败时返回 null，由调用方决定如何提示。
     */
    private suspend fun downloadFallback(
        targetJavaVersion: Int,
        onMessage: (String) -> Unit
    ): String? {
        if (!JreDownloadSource.isDownloadable(targetJavaVersion)) return null
        return try {
            RuntimeDownloader.download(targetJavaVersion) { message ->
                onMessage(message)
            }
        } catch (e: Throwable) {
            Logger.warning(TAG, "Failed to download Java $targetJavaVersion runtime", e)
            null
        }
    }

    /**
     * 从已安装运行环境里挑选：优先版本完全匹配，其次更高版本，最后任意可用项
     */
    private fun pickBest(runtimes: List<Runtime>, targetJavaVersion: Int): Runtime? {
        runtimes.firstOrNull { it.javaVersion == targetJavaVersion }?.let { return it }
        //B-17：只允许向上兼容。低于目标大版本的运行环境会直接抛 UnsupportedClassVersionError，
        //因此这里返回 null，交给内置 JRE 兜底，而不是"随便挑个最高的"糊弄过去
        return runtimes.filter { it.javaVersion > targetJavaVersion }.minByOrNull { it.javaVersion }
    }

    /**
     * 挑选与目标 Java 版本匹配的内置 JRE，且当前设备架构有对应的 bin 包
     */
    private fun pickInternalJre(context: Context, targetJavaVersion: Int): Jre? {
        val supported = Jre.entries.filter {
            //x86 系设备上 JRE 25 会 SIGILL，直接排除，避免解压完再崩
            isArchSupported(context.assets, it) && !isUnsafeOnDevice(it.majorVersion)
        }
        if (supported.isEmpty()) return null
        return supported.firstOrNull { it.majorVersion == targetJavaVersion }
            ?: supported.filter { it.majorVersion > targetJavaVersion }.minByOrNull { it.majorVersion }
            ?: supported.maxByOrNull { it.majorVersion }
    }

    private fun isArchSupported(assetManager: AssetManager, jre: Jre): Boolean {
        return runCatching {
            val runtimePack = "bin-" + Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE) + ".tar.xz"
            assetManager.list(jre.jrePath)?.contains(runtimePack) == true
        }.getOrElse { false }
    }

    private suspend fun installInternalJre(
        context: Context,
        jre: Jre,
        onMessage: (String) -> Unit
    ) {
        val assetManager = context.assets
        val runtimeVersion = runCatching {
            assetManager.open(jre.jrePath + "/version").readString()
        }.getOrElse {
            throw RuntimeUnavailableException("Built-in runtime ${jre.jreName} is missing from the APK assets.")
        }

        val archPack = "bin-" + Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE) + ".tar.xz"
        RuntimesManager.installRuntimeBinPack(
            universalFileInputStream = assetManager.open(jre.jrePath + "/universal.tar.xz"),
            platformBinsInputStream = assetManager.open(jre.jrePath + "/" + archPack),
            name = jre.jreName,
            binPackVersion = runtimeVersion,
            updateProgress = { textRes, textArgs ->
                onMessage(context.getString(textRes, *textArgs))
            }
        )
        RuntimesManager.postPrepare(jre.jreName)
    }
}
