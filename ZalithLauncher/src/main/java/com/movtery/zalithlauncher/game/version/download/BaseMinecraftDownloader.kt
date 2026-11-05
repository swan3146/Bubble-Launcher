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
import com.movtery.zalithlauncher.game.path.getAssetsHome
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.path.getLibrariesHome
import com.movtery.zalithlauncher.game.path.getResourcesHome
import com.movtery.zalithlauncher.game.path.getVersionsHome
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.game.versioninfo.models.AssetIndexJson
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.VersionManifest.Version
import com.movtery.zalithlauncher.utils.classes.Quadruple
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

const val DOWNLOADER_TAG = "MinecraftDownloader"

private const val TAG = "BaseMinecraftDownloader"
const val MINECRAFT_RES: String = "https://resources.download.minecraft.net/"

/**
 * 设计为通用化 Minecraft 原版完整下载
 * @param gameHome 下载目标所在的游戏目录
 */
class BaseMinecraftDownloader(
    gameHome: String = getGameHome()
) {
    //Dir
    val assetsTarget = File(getAssetsHome(gameHome)).ensureDirectory()
    val resourcesTarget = File(getResourcesHome(gameHome)).ensureDirectory()
    val versionsTarget = File(getVersionsHome(gameHome)).ensureDirectory()
    val librariesTarget = File(getLibrariesHome(gameHome)).ensureDirectory()
    val assetIndexTarget = File(assetsTarget, "indexes").ensureDirectory()

    suspend fun findVersion(version: String): Version? {
        val versionManifest = MinecraftVersions.getVersionManifest()
        return versionManifest.versions.find { it.id == version }
    }

    fun getVersionJsonPath(version: String, mcFolder: File = versionsTarget) =
        File(mcFolder, "$version/$version.json".replace("/", File.separator)).ensureParentDirectory()

    fun getVersionJarPath(version: String, mcFolder: File = versionsTarget) =
        File(mcFolder, "$version/$version.jar".replace("/", File.separator)).ensureParentDirectory()

    /**
     * 创建版本 Json
     */
    suspend fun createVersionJson(version: Version): GameManifest {
        return createVersionJson(version, version.id)
    }

    /**
     * 创建版本 Json
     * @param targetVersion 目标版本名称
     */
    suspend fun createVersionJson(
        version: Version,
        targetVersion: String,
        mcFolder: File = versionsTarget
    ): GameManifest {
        return downloadAndParseJson(
            targetFile = getVersionJsonPath(targetVersion, mcFolder),
            url = version.url,
            expectedSHA = version.sha1,
            classOfT = GameManifest::class.java
        )
    }

    /**
     * 创建 assets 索引 Json
     */
    suspend fun createAssetIndex(
        assetIndexTarget: File,
        gameManifest: GameManifest
    ): AssetIndexJson? {
        val indexFile = File(assetIndexTarget, "${gameManifest.assets}.json")
        return gameManifest.assetIndex?.let { assetIndex ->
            downloadAndParseJson(
                targetFile = indexFile,
                url = assetIndex.url,
                expectedSHA = assetIndex.sha1,
                classOfT = AssetIndexJson::class.java
            )
        }
    }

    /** 计划客户端jar下载 */
    fun loadClientJarDownload(
        gameManifest: GameManifest,
        clientName: String,
        mcFolder: File = versionsTarget,
        scheduleDownload: (urls: List<String>, hash: String?, targetFile: File, size: Long) -> Unit,
        scheduleCopy: (targetFile: File) -> Unit
    ) {
        val clientFile = getVersionJarPath(clientName, mcFolder)
        gameManifest.downloads?.client?.let { client ->
            scheduleDownload(client.url.mapBMCLMirrorUrls(), client.sha1, clientFile, client.size)
        } ?: run {
            //如果未提供下载方式，则很可能是需要复制原版的Jar文件
            scheduleCopy(clientFile)
        }
    }

    /**
     * 计划assets资产下载
     *
     * D-07：`virtual`（以及 `map_to_resources`）的旧版资源不能直接铺在 assets 根下，
     * 原版客户端要求 `assets\virtual\<assetIndexId>\...` 布局；启动侧再用
     * `-Dgame.assets.dir` 指向该目录。缺少这一层会导致 1.7.10 及更早版本
     * 安装后"能启动但没声音、界面文字缺失"，且不产生任何错误日志
     *
     * @param assetIndexId 资源索引 ID（即版本 JSON 的 `assets` 字段），virtual 布局的必要层级
     */
    suspend fun loadAssetsDownload(
        assetIndex: AssetIndexJson?,
        resourcesTargetDir: File = resourcesTarget,
        assetsTargetDir: File = assetsTarget,
        assetIndexId: String? = null,
        scheduleDownload: (urls: List<String>, hash: String?, targetFile: File, size: Long) -> Unit
    ) {
        assetIndex?.objects?.forEach { (path, objectInfo) ->
            currentCoroutineContext().ensureActive()

            val hashedPath = "${objectInfo.hash.substring(0, 2)}/${objectInfo.hash}"
            val targetPath = if (assetIndex.isMapToResources) resourcesTargetDir else assetsTargetDir
            val targetFile: File = when {
                assetIndex.isMapToResources -> File(targetPath, path)
                assetIndex.isVirtual -> {
                    //缺少索引 ID 时退回旧行为，但不能静默：至少让日志里能看出为什么布局不对
                    if (assetIndexId.isNullOrBlank()) {
                        Logger.warning(TAG, "Virtual asset index without id, falling back to flat layout")
                        File(targetPath, path)
                    } else {
                        File(targetPath, "virtual/$assetIndexId".replace("/", File.separator) + File.separator + path)
                    }
                }
                else -> File(targetPath, "objects/${hashedPath}".replace("/", File.separator))
            }
            scheduleDownload("$MINECRAFT_RES$hashedPath".mapBMCLMirrorUrls(), objectInfo.hash, targetFile, objectInfo.size)
        }
    }

    /** 计划库文件下载 */
    suspend fun loadLibraryDownloads(
        gameManifest: GameManifest,
        targetDir: File = librariesTarget,
        scheduleDownload: (urls: List<String>, hash: String?, targetFile: File, size: Long, isDownloadable: Boolean) -> Unit
    ) {
        gameManifest.libraries?.let { libraries ->
            processLibraries { libraries }
            libraries.forEach { library ->
                currentCoroutineContext().ensureActive()

                //D-20：只跳过组件库（components）确实覆盖的 lwjgl 版本。
                //原实现按 `org.lwjgl` 前缀整体跳过，Forge/NeoForge 在部分 MC 版本下要求的
                //3.2.x / 3.3.1 等版本并不在组件库内，跳过就会造成 native 与 jar 版本错配
                //（表现为窗口创建失败、OpenGL 初始化崩溃，日志里看不出少了什么）
                if (isLwjglProvidedByComponents(library.name)) return@forEach

                val artifactPath: String = artifactToPath(library) ?: return@forEach
                val (sha1: String?, urls, size, isDownloadable) = library.downloads?.let { downloads ->
                    downloads.artifact?.let { artifact ->
                        Quadruple(artifact.sha1, artifact.url.mapBMCLMirrorUrls(), artifact.size, true)
                    } ?: return@forEach
                } ?: run {
                    var isDownloadable = true
                    val baseUrls = library.url
                        ?.takeIf {
                            // fix(#53): Forge 明明可以不写，但还是给留了个空的值 >:(
                            it.isNotEmptyOrBlank()
                        }
                        ?.let { raw ->
                            //D-22：http→https 只是"优先尝试"，不能把 http 候选源直接抹掉，
                            //只提供 http 的旧 maven 因 TLS 失败时会必然下载失败
                            if (raw.startsWith("http://", ignoreCase = true)) {
                                listOf(raw.replaceFirst("http://", "https://"), raw)
                            } else {
                                listOf(raw)
                            }
                        }
                        ?: run {
                            //对于没有提供下载链接的，可能是需要文件已经安装，而不是临时获取
                            //不过尝试使用官方源下载，若下载失败则表明这个版本 文件有缺失的情况
                            isDownloadable = false
                            listOf("https://libraries.minecraft.net/")
                        }
                    val urls = baseUrls
                        .map { base -> "$base$artifactPath" }
                        .flatMap { it.mapBMCLMirrorUrls() }
                        .distinct()
                    Quadruple(library.sha1, urls, library.size, isDownloadable)
                }

                scheduleDownload(urls, sha1, File(targetDir, artifactPath), size, isDownloadable)
            }
        }
    }

//    /** 计划日志格式化配置下载 */
//    fun loadLog4jXMLDownload(
//        gameManifest: GameManifest,
//        version: String,
//        scheduleDownload: (url: String, hash: String?, targetFile: File, size: Long) -> Unit
//    ) {
//        val versionLoggingTarget = getLog4jXMLPath(version)
//        gameManifest.logging?.client?.file?.let { loggingConfig ->
//            scheduleDownload(loggingConfig.url, loggingConfig.sha1, versionLoggingTarget, loggingConfig.size)
//        }
//    }
}

/**
 * D-20：组件库（components）实际覆盖的 lwjgl 版本。
 *
 * 只有这些版本的 lwjgl 由 ZL2 自带组件提供，可以直接跳过下载；
 * 其它版本（Forge/NeoForge 可能要求 3.2.x、3.3.1 等）必须照常下载，
 * 否则 classpath 上的 jar 与 native 版本错配
 */
private val COMPONENT_LWJGL_VERSIONS = setOf("3.3.3", "3.4.1")

/** 库坐标 `groupId:artifactId:version[:classifier]` 的版本段 */
private fun libraryVersion(name: String): String? =
    name.split(":").getOrNull(2)?.takeIf { it.isNotBlank() }

/**
 * 判断该库条目是否已由组件库覆盖，可跳过下载
 */
private fun isLwjglProvidedByComponents(libraryName: String): Boolean {
    if (!libraryName.startsWith("org.lwjgl")) return false
    val version = libraryVersion(libraryName) ?: return false
    return version in COMPONENT_LWJGL_VERSIONS
}
