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

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.VersionManifest
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.device.adaptiveDownloadThreads
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private const val TAG = "MinecraftDownloader"

/** 单个安装流程的最大并发下载连接数：按设备内存与 CPU 核心数自适应，避免低端设备卡顿 */
val DEFAULT_DOWNLOAD_THREADS: Int by lazy { adaptiveDownloadThreads() }

/**
 * Minecraft 安装器：装配版本 JSON、client jar、assets 与 libraries 的下载任务，
 * 并交由批量下载引擎执行
 * 分块并发、自动换源、失败整轮重试
 */
class MinecraftDownloader(
    private val context: Context,
    private val version: String,
    private val customName: String = version,
    private val gameHome: String = getGameHome(),
    private val downloader: BaseMinecraftDownloader = BaseMinecraftDownloader(gameHome),
    private val mode: DownloadMode = DownloadMode.DOWNLOAD,
    private val onCompletion: suspend (Task) -> Unit = {},
    private val onError: (message: String) -> Unit = {},
    private val onThrowable: ((throwable: Throwable) -> Unit)? = null,
    private val maxDownloadThreads: Int = DEFAULT_DOWNLOAD_THREADS
) {
    private var allDownloadTasks = mutableListOf<DownloadTask>()

    /**
     * 不依赖具体下载任务、需要在整批下载结束后执行的收尾动作
     * （典型场景：父版本 jar 不在本批任务中时的复制）
     */
    private val afterBatchActions = mutableListOf<suspend () -> Unit>()

    private fun <R> getTaskMessage(
        download: R,
        verify: R
    ): R =
        when (mode) {
            DownloadMode.DOWNLOAD -> download
            DownloadMode.VERIFY_AND_REPAIR -> verify
        }

    /**
     * 自定义 client 目录 ->client<-/versions/..
     */
    fun getDownloadTask(
        clientName: String = this.customName,
        clientVersionsDir: File = downloader.versionsTarget
    ): Task {
        return Task.runTask(
            id = DOWNLOADER_TAG,
            dispatcher = Dispatchers.Default,
            task = { task ->
                task.updateProgress(-1f)
                task.updateMessage(
                    getTaskMessage(androidText(R.string.minecraft_download_stat_download_task), null)
                )
                //D-25：任务列表与收尾动作只对本次执行有效。
                //MinecraftDownloader 被设计为可复用组件，重复调用会把上一批任务
                //（含已完成的）再次排入下载，造成重复下载与统计口径翻倍
                allDownloadTasks = mutableListOf()
                afterBatchActions.clear()

                if (mode == DownloadMode.DOWNLOAD) {
                    progressNewDownloadTasks(clientName, clientVersionsDir)
                } else {
                    //D-24：校验/修复模式下必须使用调用方传入的目标目录与版本名，
                    //否则会去校验正式 versions/<customName> 里那份过期清单，修复行为完全错位
                    val jsonFile = downloader.getVersionJsonPath(clientName, clientVersionsDir)
                        .takeIf { it.canRead() }
                        ?: throw IOException("Version $clientName JSON file is unreadable.")
                    val jsonText = jsonFile.readText()
                    val gameManifest = jsonText.parseTo(GameManifest::class.java)
                    progressDownloadTasks(gameManifest, clientName, clientVersionsDir)
                }

                if (allDownloadTasks.isNotEmpty()) {
                    task.runBatchDownloads(
                        tasks = allDownloadTasks,
                        maxConnections = maxDownloadThreads,
                        retryRounds = 1,
                        onSnapshot = { snapshot ->
                            task.updateSpeed(snapshot.speedBytesPerSec)
                            task.updateMessage(androidText(
                                getTaskMessage(R.string.minecraft_download_downloading_game_files, R.string.minecraft_download_verifying_and_repairing_files),
                                snapshot.downloadedFiles, snapshot.totalFiles,
                                formatFileSize(snapshot.downloadedBytes), formatFileSize(snapshot.totalBytes)
                            ))
                        }
                    )
                }
                //整批下载成功后再执行收尾动作（如复制父版本 jar），失败必须暴露出来，
                //否则会出现"安装完成但缺 client jar"的静默损坏
                afterBatchActions.forEach { it() }
                afterBatchActions.clear()

                //清除任务信息
                task.updateProgress(1f)
                task.updateMessage(null)

                onCompletion(task)
            },
            onError = { e ->
                Logger.error(TAG, "Failed to download Minecraft!", e)
                if (onThrowable != null) {
                    onThrowable(e)
                } else {
                    val message = when(e) {
                        is CancellationException -> return@runTask
                        is FileNotFoundException -> context.getString(R.string.minecraft_download_failed_notfound)
                        else -> e.getMessageOrToString()
                    }
                    onError(message)
                }
            }
        )
    }

    /**
     * 仅将 Jar、Json 文件安装到自定义版本目录中
     */
    private suspend fun progressNewDownloadTasks(
        clientName: String,
        clientVersionsDir: File
    ) {
        val gameManifest = downloader.findVersion(this.version)?.let {
            downloader.createVersionJson(it, clientName, clientVersionsDir)
        } ?: throw IllegalArgumentException("Version not found: $version")

        commonScheduleDownloads(gameManifest, null, clientName, clientVersionsDir)
    }

    private suspend fun progressDownloadTasks(
        gameManifest: GameManifest,
        clientName: String,
        clientVersionsDir: File = downloader.versionsTarget
    ) {
        val inheritsFromId = gameManifest.inheritsFrom
        val inheritsFrom = inheritsFromId?.let { downloader.findVersion(it) }

        //优先尝试解析原版
        if (inheritsFrom != null) {
            val parentManifest = downloader.createVersionJson(inheritsFrom)
            //父版本的资源同样要落到本次安装的目标目录（校验/修复模式下不是 versionsTarget）
            progressDownloadTasks(parentManifest, inheritsFrom.id, clientVersionsDir)
        } else if (inheritsFromId != null) {
            //D-09：父版本不存在于官方清单（用户重命名、整合包自带、已被移除的老快照）时，
            //旧实现直接放弃父版本 —— 子版本的 client jar 既不复制也不下载，
            //最终"装完了但打不开"。这里退回读取本地 versions/<id>/<id>.json
            val localParentJson = downloader.getVersionJsonPath(inheritsFromId)
            if (localParentJson.canRead()) {
                Logger.warning(
                    TAG,
                    "Parent version not found in official manifest, falling back to local JSON: ${localParentJson.absolutePath}"
                )
                try {
                    val parentManifest = localParentJson.readText().parseTo(GameManifest::class.java)
                    progressDownloadTasks(parentManifest, inheritsFromId, clientVersionsDir)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.error(TAG, "Failed to parse local parent version JSON: ${localParentJson.absolutePath}", e)
                }
            } else {
                Logger.warning(
                    TAG,
                    "Parent version is missing both in manifest and locally: $inheritsFromId, " +
                            "client jar will not be resolved"
                )
            }
        }

        commonScheduleDownloads(
            gameManifest = gameManifest,
            inheritsFrom = inheritsFrom,
            clientName = clientName,
            clientVersionsDir = clientVersionsDir
        )
    }

    private suspend fun commonScheduleDownloads(
        gameManifest: GameManifest,
        inheritsFrom: VersionManifest.Version? = null,
        clientName: String,
        clientVersionsDir: File
    ) {
        val assetsIndex = downloader.createAssetIndex(downloader.assetIndexTarget, gameManifest)

        downloader.loadClientJarDownload(
            gameManifest = gameManifest,
            clientName = clientName,
            mcFolder = clientVersionsDir,
            scheduleDownload = { urls, hash, targetFile, size ->
                scheduleDownload(urls, hash, targetFile, size)
            },
            scheduleCopy = { targetFile ->
                val parentId = gameManifest.inheritsFrom
                if (parentId != null) {
                    val inheritsJar = downloader.getVersionJarPath(inheritsFrom?.id ?: parentId)

                    val copyAction: suspend () -> Unit = {
                        when {
                            targetFile.exists() -> Unit
                            inheritsJar.exists() -> {
                                targetFile.ensureParentDirectory()
                                inheritsJar.copyTo(targetFile, overwrite = true)
                                Logger.info(TAG, "Copied ${inheritsJar.absolutePath} to ${targetFile.absolutePath}")
                            }
                            //父 jar 缺失必须告警：这是"安装完成但启动报 classpath 错误"的根因
                            else -> Logger.warning(
                                TAG,
                                "Parent version jar is missing, client jar cannot be copied: ${inheritsJar.absolutePath}"
                            )
                        }
                    }

                    //D-16：挂后任务用"追加"而不是赋值，并改用 canonicalFile 比较。
                    //旧实现既会覆盖前一个子版本挂上的复制动作，也会因盘符大小写/8.3 短路径
                    ///符号链接等原因匹配失配而静默什么都不做
                    val dependent = allDownloadTasks.find { sameFile(it.targetFile, inheritsJar) }
                    if (dependent != null) {
                        val previous = dependent.fileDownloadedTask
                        dependent.fileDownloadedTask = {
                            previous?.invoke()
                            copyAction()
                        }
                    } else {
                        //父 jar 不在本批任务里（本地已存在，或父版本清单不可得）：整批结束后再复制
                        afterBatchActions.add(copyAction)
                    }
                }
            }
        )
        downloader.loadAssetsDownload(
            assetIndex = assetsIndex,
            assetIndexId = gameManifest.assets
        ) { urls, hash, targetFile, size ->
            scheduleDownload(urls, hash, targetFile, size)
        }
        downloader.loadLibraryDownloads(gameManifest) { urls, hash, targetFile, size, isDownloadable ->
            scheduleDownload(urls, hash, targetFile, size, isDownloadable)
        }
    }

    /**
     * 判断两个 File 是否指向同一份物理文件
     * 优先比较 canonicalFile（可消除盘符大小写、8.3 短路径、符号链接差异）
     */
    private fun sameFile(a: File, b: File): Boolean = try {
        a.canonicalFile == b.canonicalFile
    } catch (_: IOException) {
        a.absolutePath.equals(b.absolutePath, ignoreCase = true)
    }

    /**
     * 提交计划下载
     */
    private fun scheduleDownload(urls: List<String>, sha1: String?, targetFile: File, size: Long, isDownloadable: Boolean = true) {
        allDownloadTasks.add(
            DownloadTask(
                urls = urls,
                verifyIntegrity = true,
                targetFile = targetFile,
                sha1 = sha1,
                size = size,
                isDownloadable = isDownloadable
            )
        )
    }
}
