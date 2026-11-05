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

package com.movtery.zalithlauncher.game.launch

import androidx.collection.ArrayMap
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.isAuthServerAccount
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.offline.OfflineYggdrasilServer
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.path.getAssetsHome
import com.movtery.zalithlauncher.game.path.getLibrariesHome
import com.movtery.zalithlauncher.game.plugin.natives.NativePluginManager
import com.movtery.zalithlauncher.game.version.download.artifactToPath
import com.movtery.zalithlauncher.game.version.download.filterLibrary
import com.movtery.zalithlauncher.game.version.download.getLibraryReplacement
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionInfo
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.path.LibPath
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.screens.content.elements.QuickPlay
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.ServerAddress
import com.movtery.zalithlauncher.utils.string.insertJSONValueList
import com.movtery.zalithlauncher.utils.string.isEmptyOrBlank
import com.movtery.zalithlauncher.utils.string.isLowerTo
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import com.movtery.zalithlauncher.utils.string.splitPreservingQuotes
import com.movtery.zalithlauncher.utils.string.toUnicodeEscaped
import java.io.File

private const val TAG = "LaunchArgs"

class LaunchArgs(
    private val runtimeLibraryPath: String,
    private val account: Account,
    private val offlineServer: OfflineYggdrasilServer,
    private val gameDirPath: File,
    private val version: Version,
    private val clientJar: File,
    private val gameManifest: GameManifest,
    private val lwjglVersion: Int,
    private val runtime: Runtime,
    private val readAssetsFile: (path: String) -> String,
    private val getCacioJavaArgs: (isJava8: Boolean) -> List<String>
) {
    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())
        argsList.addAll(NativePluginManager.getJVMEnv())

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = gameManifest.mainClass.substring(0, gameManifest.mainClass.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add("mio.Wrapper")
        argsList.add(gameManifest.mainClass)
        argsList.addAll(getMinecraftClientArgs())

        version.getVersionInfo()?.let { info ->
            val quickPlay = version.quickPlaySingle
            if (quickPlay != null) {
                when (quickPlay) {
                    is QuickPlay.Save -> {
                        if (quickPlay.saveName.isEmptyOrBlank()) return@let

                        if (info.quickPlay.isQuickPlaySingleplayer) {
                            //将不受支持的字符转换为Unicode
                            val saveName = quickPlay.saveName.toUnicodeEscaped()
                            argsList.apply {
                                add("--quickPlaySingleplayer")
                                add(saveName)
                            }
                        } else {
                            val msg = "Quick Play for singleplayer is not supported and has been skipped."
                            LoggerBridge.append(msg)
                            Logger.warning(TAG, msg)
                        }
                    }
                    is QuickPlay.Server -> {
                        argsList.addQuickPlayServer(
                            address = quickPlay.serverAddress,
                            quickPlay = info.quickPlay
                        )
                    }
                }
            } else {
                version.getServerIp()?.let { address ->
                    argsList.addQuickPlayServer(
                        address = address,
                        quickPlay = info.quickPlay
                    )
                }
            }
        }

        //追加版本配置的游戏参数，置于参数列表末尾
        argsList.addAll(version.getGameArgs().splitPreservingQuotes())

        return argsList
    }

    private fun MutableList<String>.addQuickPlayServer(
        address: String,
        quickPlay: VersionInfo.QuickPlay
    ) {
        runCatching {
            ServerAddress.parse(address)
        }.onFailure {
            val msg = "Unable to resolve the server address: $address. The automatic server join feature is unavailable."
            LoggerBridge.append(msg)
            Logger.warning(TAG, msg, it)
        }.getOrNull()?.let { parsed ->
            val args = if (quickPlay.isQuickPlayMultiplayer) {
                val port = if (parsed.port < 0) {
                    ServerAddress.DEFAULT_PORT
                } else {
                    parsed.port
                }

                listOf(
                    "--quickPlayMultiplayer",
                    "${parsed.getASCIIHost()}:$port"
                )
            } else {
                val port = parsed.port.takeIf { it >= 0 } ?: ServerAddress.DEFAULT_PORT
                listOf("--server", parsed.getASCIIHost(), "--port", port.toString())
            }

            addAll(args)
        }
    }

    /**
     * 组装 LWJGL 组件 classpath
     * 版本 >= 3.4.1 -> 使用 3.4.1 组件；否则使用 3.3.3 组件。
     * LWJGL2 时代（版本 <= 299）额外加入 lwjgl-lwjglx.jar 桥接层。
     * lwjgl.jar 核心优先 -> merged-modules -> 其余模块。
     */
    private fun getLWJGL3ClassPath(): String {
        val versionDir = lwjglVersionDir(lwjglVersion)
        val dir = File(PathManager.DIR_COMPONENTS, "lwjgl/$versionDir")
        val isLwjgl2 = lwjglVersion in 1..299
        return dir.listFiles { file -> file.name.endsWith(".jar") }
            ?.sortedBy { file -> lwjglJarOrder(file.name, versionDir, isLwjgl2) }
            ?.filter { file -> isLwjgl2 || file.name != "lwjgl-lwjglx.jar" }
            ?.joinToString(":") { it.absolutePath }
            ?: ""
    }

    private fun lwjglJarOrder(name: String, versionDir: String, isLwjgl2: Boolean): Int = when (name) {
        "lwjgl.jar" -> 0
        "lwjgl-$versionDir-merged-modules.jar" -> 1
        "lwjgl-lwjglx.jar" -> 3 // 桥接层放最后，仅 LWJGL2 使用
        else -> 2
    }

    private fun getJavaArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        if (account.isLocalAccount()) {
            if (account.hasSkinFile) {
                //该离线账号拥有本地皮肤，启用离线yggdrasil服务器
                offlineServer.start()
                offlineServer.addCharacter(account)
                offlineServer.getPort()?.let { port ->
                    val msg = "Using offline Yggdrasil server on port $port"
                    LoggerBridge.append(msg)
                    Logger.info(TAG, msg)
                    argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=http://localhost:$port")
                    argsList.add("-Dauthlibinjector.side=client")
                } ?: run {
                    //无法获取端口号，说明服务器未成功启动
                    val msg = "Failed to start offline Yggdrasil server!"
                    LoggerBridge.append(msg)
                    Logger.warning(TAG, msg)
                    //本次启动将被忽略，为避免浪费性能，关停服务器
                    offlineServer.stop()
                }
            }
        } else if (account.isAuthServerAccount()) {
            if (account.otherBaseUrl!!.contains("auth.mc-user.com")) {
                argsList.add("-javaagent:${LibPath.NIDE_8_AUTH.absolutePath}=${account.otherBaseUrl!!.replace("https://auth.mc-user.com:233/", "")}")
                argsList.add("-Dnide8auth.client=true")
            } else {
                argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=${account.otherBaseUrl}")
                argsList.add("-Dauthlibinjector.side=client")
            }
        }

        argsList.addAll(getCacioJavaArgs(runtime.javaVersion == 8))

        //MioLibPatcher 需作为 JVM 选项注册在 cacio agent 之后、-cp 之前
        argsList.add("-javaagent:${LibPath.MIO_LIB_PATCHER.absolutePath}")

        val configFilePath = version.getVersionPath().child("log4j2.xml")
        if (!configFilePath.exists()) {
            val is7 = (version.getVersionInfo()?.minecraftVersion ?: "0.0").isLowerTo("1.12")
            runCatching {
                val content = if (is7) {
                    readAssetsFile("components/log4j-1.7.xml")
                } else {
                    readAssetsFile("components/log4j-1.12.xml")
                }
                configFilePath.writeText(content)
            }.onFailure {
                Logger.warning(TAG, "Failed to write fallback Log4j configuration autonomously!", it)
            }
        }
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")
        argsList.add("-Dminecraft.client.jar=${clientJar.absolutePath}")
        argsList.add("-Dminecraft.launcher.brand=${BuildKeys.LAUNCHER_NAME}")
        argsList.add("-Dminecraft.launcher.version=${BuildConfig.VERSION_NAME}")

        return argsList
    }

    private fun getMinecraftJVMArgs(): Array<String> {
//        // Parse Forge 1.17+ additional JVM Arguments
//        if (versionInfo.inheritsFrom == null || versionInfo.arguments == null || versionInfo.arguments.jvm == null) {
//            return emptyArray()
//        }

        val varArgMap: MutableMap<String, String> = android.util.ArrayMap()
        val launchClassPath = buildList {
            add(getLWJGL3ClassPath())
            add(LibPath.MIO_LAUNCH_WRAPPER.absolutePath)
            putLaunchClassPath(gameManifest)
        }.joinToString(":")
        var hasClasspath = false //是否已经在jvm参数中包含 ${classpath} 配置

        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = getLibrariesHome(version.getGameHome())
        varArgMap["version_name"] = gameManifest.id
        varArgMap["natives_directory"] = runtimeLibraryPath
        setLauncherInfo(varArgMap)

        fun Any.processJvmArg(): String? = (this as? String)?.let { argument ->
            if (argument.startsWith("-Djava.library.path=")) {
                //26.2+ Mojang 更改到了具体的路径，需要手动重定向
                return@let $$"-Djava.library.path=${natives_directory}"
            }
            when {
                argument.startsWith("-DignoreList=") -> {
                    "$argument,${version.getVersionName()}.jar"
                }
                argument.contains("-Dio.netty.native.workdir") ||
                argument.contains("-Djna.tmpdir") ||
                argument.contains("-Dorg.lwjgl.system.SharedLibraryExtractPath") -> {
                    //使用一个可读的目录
                    argument.replace($$"${natives_directory}", PathManager.DIR_CACHE.absolutePath)
                }
                argument == $$"${classpath}" -> {
                    hasClasspath = true
                    launchClassPath
                }
                else -> argument
            }
        }

        val jvmArgs = gameManifest.arguments?.jvm
            ?.mapNotNull { it.processJvmArg() }
            ?.toTypedArray()
            ?: emptyArray()

        val replacedArgs = insertJSONValueList(jvmArgs, varArgMap)
        val repairedArgs = repairOrphanJvmArgs(replacedArgs)
        return if (hasClasspath) {
            repairedArgs
        } else {
            //不包含 ${classpath} 配置，则需要手动添加
            repairedArgs + arrayOf("-cp", launchClassPath)
        }
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L572-L592)
     */
    private fun MutableList<String>.putLaunchClassPath(gameManifest: GameManifest) {
        val classpath: Array<String> = generateLibClasspath(gameManifest)

        for (jarFile in classpath) {
            val jarFileObj = File(jarFile)
            if (!jarFileObj.exists()) {
                Logger.debug(TAG, "Ignored non-exists file: $jarFile")
                continue
            }
            add(jarFile)
        }
        if (clientJar.exists()) {
            add(clientJar.absolutePath)
        }
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L871-L882)
     */
    private fun generateLibClasspath(gameManifest: GameManifest): Array<String> {
        val libSortFix = LibSortFix(version.getVersionInfo())
        val libs = LinkedHashMap<GameManifest.Library, String>()

        for (libItem in gameManifest.libraries) {
            if (!(GameManifest.Rule.checkRules(libItem.rules) && !libItem.isNative)) continue
            val path = libItem.progressLibrary() ?: continue
            with(libSortFix) {
                libs.insertLib(libItem, getLibrariesHome(version.getGameHome()) + "/" + path)
            }
        }

        return libs.values.toTypedArray<String>()
    }


    /**
     * @return 库相对路径
     */
    private fun GameManifest.Library.progressLibrary(): String? {
        if (filterLibrary()) return null

        var path = artifactToPath(this)

        val versionSegment = name.split(":").getOrNull(2) ?: return path
        val versionParts = versionSegment.split(".")

        getLibraryReplacement(name, versionParts)?.let { replacement ->
            Logger.debug(TAG, "Library ${this.name} has been changed to version ${replacement.newName.split(":").last()}")
            path = replacement.newPath
        }

        return path
    }

    private fun getMinecraftClientArgs(): Array<String> {
        val varArgMap: MutableMap<String, String> = ArrayMap()
        varArgMap["auth_session"] = account.accessToken
        varArgMap["auth_access_token"] = account.accessToken
        varArgMap["auth_player_name"] = account.username
        varArgMap["auth_uuid"] = account.profileId.replace("-", "")
        varArgMap["auth_xuid"] = account.xUid ?: ""
        val assetsHome = getAssetsHome(version.getGameHome())
        varArgMap["assets_root"] = assetsHome
        varArgMap["assets_index_name"] = gameManifest.assetIndex.id
        //D-07：virtual 资源（1.7.10 及更早版本的 assets）在磁盘上是
        //assets\virtual\<assetIndexId>\... 布局，game_assets 必须指向该目录；
        //否则游戏能启动但没有声音、界面文字缺失，且不产生任何错误日志。
        //按目录是否存在判断，兼容老版本装出来的扁平布局
        val virtualAssetsDir = File(assetsHome, "virtual/${gameManifest.assetIndex.id}")
        varArgMap["game_assets"] = if (virtualAssetsDir.isDirectory) {
            virtualAssetsDir.absolutePath
        } else {
            assetsHome
        }
        varArgMap["game_directory"] = gameDirPath.absolutePath
        varArgMap["user_properties"] = "{}"
        varArgMap["user_type"] = "msa"
        varArgMap["version_name"] = gameManifest.id

        setLauncherInfo(varArgMap)

        val minecraftArgs: MutableList<String> = ArrayList()
        gameManifest.arguments?.apply {
            // Support Minecraft 1.13+
            game.forEach { if (it is String) minecraftArgs.add(it) }
        }

        return insertJSONValueList(
            splitAndFilterEmpty(
                gameManifest.minecraftArguments ?:
                minecraftArgs.toTypedArray().joinToString(" ")
            ), varArgMap
        )
    }

    private fun setLauncherInfo(verArgMap: MutableMap<String, String>) {
        verArgMap["launcher_name"] = BuildKeys.LAUNCHER_NAME
        verArgMap["launcher_version"] = BuildConfig.VERSION_NAME
        verArgMap["version_type"] = version.getCustomInfo()
            .takeIf { it.isNotEmptyOrBlank() }
            ?: gameManifest.type
    }

    private fun splitAndFilterEmpty(arg: String): Array<String> {
        val list: MutableList<String> = ArrayList()
        arg.split(" ").forEach {
            if (it.isNotEmpty()) list.add(it)
        }
        return list.toTypedArray()
    }
}

/**
 * 从版本清单中探测要求的 LWJGL 主版本
 * 解析 `org.lwjgl:lwjgl:X.Y.Z` / `org.lwjgl.lwjgl:lwjgl:X.Y.Z` 坐标，
 * 返回去掉句点后的整数（如 3.3.3→333、3.4.1→341、2.9.9→299）
 * @return 无法确定时返回 0（默认按 LWJGL3 处理）
 */
fun detectLwjglVersion(manifest: GameManifest): Int {
    for (lib in manifest.libraries) {
        val name = lib.name ?: continue
        val versionPrefix = when {
            name.startsWith("org.lwjgl.lwjgl:lwjgl:") -> "org.lwjgl.lwjgl:lwjgl:"
            name.startsWith("org.lwjgl:lwjgl:") -> "org.lwjgl:lwjgl:"
            else -> continue
        }
        val intVersion = name.substring(versionPrefix.length)
            .takeWhile { it.isDigit() || it == '.' }
            .filter { it != '.' }
            .toIntOrNull()
        if (intVersion != null && intVersion in 200..999) return intVersion
    }
    return 0
}

/**
 * LWJGL 版本整数 -> 组件目录名。
 * 版本 >= 3.4.1 -> 3.4.1 组件；否则（含 LWJGL2 桥接场景）-> 3.3.3 组件
 */
fun lwjglVersionDir(lwjglVersion: Int): String = if (lwjglVersion >= 341) "3.4.1" else "3.3.3"

/**
 * 需要"吃掉下一个 token 作为值"的 JVM 模块化参数（空格分隔形式）。
 * Forge/NeoForge 的 version json 里 --add-opens / --add-exports 采用成对的
 * flag + value 结构，去重或合并历史版本若误删了重复的 flag，值会变成孤儿。
 */
private val JVM_VALUE_CONSUMING_FLAGS = setOf(
    "--add-opens", "--add-exports", "--add-modules", "--add-reads",
    "-p", "--module-path", "--upgrade-module-path"
)

/**
 * 修复 jvm 参数中的"孤儿值"（启动时防御性兜底）。
 *
 * 旧版本在合并/去重 Forge 的 version json 时，可能误删了重复的 --add-opens /
 * --add-exports flag，导致其值变成"孤儿"（前一个元素是值而非 flag），
 * 被 JVM 当成主类名，报错：
 *   "找不到或无法加载主类 java.base/java.lang.invoke=cpw.mods.securejarhandler"
 *
 * 这里按"最近出现的 --add-opens / --add-exports flag"补回缺失的 flag，
 * 恢复 flag + value 的成对结构。对结构正常的参数列表不产生任何改动。
 */
private fun repairOrphanJvmArgs(args: Array<String>): Array<String> {
    // 快速路径：不存在"连续两个非 - 开头的值"时原样返回，避免多余分配
    var hasOrphan = false
    for (i in 1 until args.size) {
        val prev = args[i - 1]
        val cur = args[i]
        if (!prev.startsWith("-") && !cur.startsWith("-") && prev !in JVM_VALUE_CONSUMING_FLAGS) {
            hasOrphan = true
            break
        }
    }
    if (!hasOrphan) return args

    val result = ArrayList<String>(args.size + 4)
    var expectingValue = false
    // 最近见到的 --add-opens / --add-exports flag（独立 -D/-X 参数不会重置它）
    var lastModuleFlag: String? = null

    for (arg in args) {
        when {
            arg in JVM_VALUE_CONSUMING_FLAGS -> {
                result.add(arg)
                if (arg == "--add-opens" || arg == "--add-exports") {
                    lastModuleFlag = arg
                }
                expectingValue = true
            }
            arg.startsWith("-") -> {
                // 独立的 JVM 选项（-Dxxx、-Xxxx、--xxx=yyy 内联形式等）
                result.add(arg)
                expectingValue = false
            }
            expectingValue -> {
                // 前一个是 value-consuming flag，这是它的值（正常配对）
                result.add(arg)
                expectingValue = false
            }
            else -> {
                // 非 flag 且不在期待值状态：这是"孤儿值"，补回最近丢失的模块化 flag
                lastModuleFlag?.let { result.add(it) }
                result.add(arg)
            }
        }
    }
    return result.toTypedArray()
}