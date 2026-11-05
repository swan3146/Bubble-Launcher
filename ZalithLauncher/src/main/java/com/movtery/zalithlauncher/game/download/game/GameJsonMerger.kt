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

package com.movtery.zalithlauncher.game.download.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.game.models.LaunchFor
import com.movtery.zalithlauncher.game.download.game.models.toLaunchForInfo
import com.movtery.zalithlauncher.game.download.game.optifine.LAUNCH_WRAPPER_MAIN
import com.movtery.zalithlauncher.game.download.game.optifine.OPTIFINE_FORGE_TWEAKER
import com.movtery.zalithlauncher.game.download.game.optifine.OPTIFINE_TWEAKER
import com.movtery.zalithlauncher.game.download.game.optifine.TRANSFORMER_DISCOVERY_SERVICE_LIB
import com.movtery.zalithlauncher.game.version.download.writeTextAtomically
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.json.merge
import com.movtery.zalithlauncher.utils.json.safeGetMember
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.isBiggerTo
import java.io.File
import java.io.IOException

private const val TAG = "GameJsonMerger"

const val GAME_JSON_MERGER_ID = "GameJsonMerger"

/** ModLauncher 主类，Forge 1.13 ~ 1.16 使用 */
private const val MOD_LAUNCHER_MAIN = "cpw.mods.modlauncher.Launcher"

/**
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/bf6fa718c89e8615b947d1c639ed16a72ce125e0/Plain%20Craft%20Launcher%202/Pages/PageDownload/ModDownloadLib.vb#L2187-L2353)
 */
fun mergeGameJson(
    info: GameDownloadInfo,
    outputFolder: File,
    clientFolder: File,
    optiFineFolder: File? = null,
    forgeFolder: File? = null,
    neoForgeFolder: File? = null,
    fabricFolder: File? = null,
    legacyFabricFolder: File? = null,
    quiltFolder: File? = null,
    cleanroomFolder: File? = null
) {
    Logger.info(TAG,
        "Start merge version json, output: $outputFolder, Minecraft: $clientFolder\n" +
                (if (optiFineFolder != null) "，${ModLoader.OPTIFINE.displayName}: $optiFineFolder" else "") +
                (if (forgeFolder != null) "，${ModLoader.FORGE.displayName}: $forgeFolder" else "") +
                (if (neoForgeFolder != null) "，${ModLoader.NEOFORGE.displayName}: $neoForgeFolder" else "") +
                (if (fabricFolder != null) "，${ModLoader.FABRIC.displayName}: $fabricFolder" else "") +
                (if (legacyFabricFolder != null) "，${ModLoader.LEGACY_FABRIC.displayName}: $legacyFabricFolder" else "") +
                (if (quiltFolder != null) "，${ModLoader.QUILT.displayName}: $quiltFolder" else "") +
                (if (cleanroomFolder != null) "，${ModLoader.CLEANROOM.displayName}: $cleanroomFolder" else "")
    )

    outputFolder.mkdirs()

    //D-12：这些路径是全流程的前提，缺失时必须给出可读的错误，
    //而不是让 NullPointerException 冒到上层（堆栈对用户毫无意义，
    //且异常发生在合并阶段，前序解压/下载的成果全部作废）
    val outputJsonPath = outputFolder.gameFileOrNull("json")
        ?: throw IOException("Invalid output version folder: ${outputFolder.absolutePath}")
    val outputJar = outputFolder.gameFileOrNull("jar")
        ?: throw IOException("Invalid output version folder: ${outputFolder.absolutePath}")

    val minecraftJsonPath = clientFolder.gameFileOrNull("json")
        ?: throw IOException("Invalid Minecraft version folder: ${clientFolder.absolutePath}")
    val minecraftJar = clientFolder.gameFileOrNull("jar")
        ?: throw IOException("Invalid Minecraft version folder: ${clientFolder.absolutePath}")

    val optiFineJsonPath = optiFineFolder.gameFileOrNull("json")
    val forgeJsonPath = forgeFolder.gameFileOrNull("json")
    val neoForgeJsonPath = neoForgeFolder.gameFileOrNull("json")
    val fabricJsonPath = fabricFolder.gameFileOrNull("json")
    val legacyFabricJsonPath = legacyFabricFolder.gameFileOrNull("json")
    val quiltJsonPath = quiltFolder.gameFileOrNull("json")
    val cleanroomJsonPath = cleanroomFolder.gameFileOrNull("json")

    //读取和验证 Json
    //D-12：原版清单缺失/命名不规范时给出明确错误，避免 NPE
    val minecraftJson = minecraftJsonPath.getJsonOrNull("Minecraft")
        ?: throw IOException("Minecraft version JSON is missing or invalid: $minecraftJsonPath")
    //Addon
    val optiFineJson = optiFineJsonPath.getJsonOrNull(ModLoader.OPTIFINE.displayName)
    val forgeJson = forgeJsonPath.getJsonOrNull(ModLoader.FORGE.displayName)
    val neoForgeJson = neoForgeJsonPath.getJsonOrNull(ModLoader.NEOFORGE.displayName)
    val fabricJson = fabricJsonPath.getJsonOrNull(ModLoader.FABRIC.displayName)
    val legacyFabricJson = legacyFabricJsonPath.getJsonOrNull(ModLoader.LEGACY_FABRIC.displayName)
    //D-10：Quilt 的清单文件名来自 ModLoader.QUILT，旧实现用 FABRIC 去查，
    //查不到返回 null 后被 listOfNotNull 静默丢弃 —— 产物变成"原版 + 什么都没有"，
    //启动时找不到主类。这里同时补一条告警，避免再次静默
    val quiltJson = quiltJsonPath.getJsonOrNull(ModLoader.QUILT.displayName).also { json ->
        if (json == null && quiltFolder != null) {
            Logger.warning(TAG, "Quilt version JSON is missing: ${quiltJsonPath ?: quiltFolder.absolutePath}")
        }
    }
    val cleanroomJson = cleanroomJsonPath.getJsonOrNull(ModLoader.CLEANROOM.displayName)

    //处理 minecraftArguments
    val allArgs = listOfNotNull(
        minecraftJson.safeGetMinecraftArguments(),
        optiFineJson?.safeGetMinecraftArguments(),
        forgeJson?.safeGetMinecraftArguments(),
        neoForgeJson?.safeGetMinecraftArguments(),
        cleanroomJson?.safeGetMinecraftArguments()
        //Fabric、Quilt没有这样的参数
    ).joinToString(" ")

    val splitArgs = splitMinecraftArguments(allArgs)
    val realArgs = deduplicateMinecraftArguments(splitArgs).joinToString(" ")

    // ----------------------------------------------------------
    //                      开始合并 版本 Json
    // ----------------------------------------------------------

    val outputJson = minecraftJson.deepCopy()

    val vanillaLibraries = outputJson.remove("libraries") as? JsonArray
    listOfNotNull(
        optiFineJson,
        forgeJson, neoForgeJson,
        fabricJson, quiltJson,
        legacyFabricJson,
        cleanroomJson
    ).forEach { json ->
        json.remove("releaseTime")
        json.remove("time")
        outputJson.merge(json)
    }
    vanillaLibraries?.let { libs ->
        (outputJson.get("libraries") as? JsonArray)?.addAll(libs)
            ?: outputJson.add("libraries", libs)
    }

    if (realArgs.isNotBlank()) {
        outputJson.addProperty("minecraftArguments", realArgs)
    }
    outputJson.remove("_comment_")
    outputJson.remove("inheritsFrom")
    outputJson.remove("jar")
    outputJson.addProperty("id", outputFolder.name)

    //针对 libraries 进行去重
    deduplicateLibraries(outputJson)

    //D-11：同名数组按"追加"合并，arguments.game / arguments.jvm 没有去重兜底，
    //多重 loader 叠加（Forge + OptiFine、NeoForge + 兼容层）时
    //-Djava.library.path、--width/--height、-cp 类参数会重复出现，
    //谁生效取决于 JVM 与解析顺序，可能出现与用户设置不符的分辨率/内存
    deduplicateArguments(outputJson)

    //烘焙 OptiFine 与 Forge 的共存方式
    bakeOptiFineCompatibility(info, outputJson)

    //存入 LaunchFor 信息
    addLaunchForInfo(
        info = info,
        jsonObject = outputJson
    )

    //保存已合并的新版本 Json         输出更美观的Json
    //D-08：原子写入，避免进程被杀留下截断的合并清单
    writeTextAtomically(File(outputJsonPath), GSON.toJson(outputJson))
    if (minecraftJar != outputJar) {
        val outputJarFile = File(outputJar)
        val originalJarFile = File(minecraftJar)

        //D-08：旧实现在源 jar 不存在时什么都不做也不报错，
        //最终得到一个"安装完成但 classpath 缺主 jar"的版本
        if (!originalJarFile.isFile) {
            throw IOException("Minecraft client jar is missing, cannot merge version: ${originalJarFile.absolutePath}")
        }

        try {
            if (outputJarFile.exists() && !outputJarFile.delete()) {
                throw IOException("Failed to replace occupied file: ${outputJarFile.absolutePath}")
            }
            originalJarFile.copyTo(outputJarFile, overwrite = true)
        } catch (e: IOException) {
            Logger.error(TAG, "Failed to copy client jar to ${outputJarFile.absolutePath}", e)
            throw e
        }
        Logger.info(TAG, "Copied ${originalJarFile.absolutePath} to ${outputJarFile.absolutePath}")
    }
}

/**
 * 去重 arguments.game / arguments.jvm 中重复的参数条目
 *
 * D-11：`_JsonObject.merge` 对同名数组是纯追加，多重 loader 叠加时
 * jvm/game 参数会重复累积
 *
 * 关键约束：对"必须吃掉下一个 token 作为值"的选项（如 --add-opens/--add-exports/--width），
 * 必须按 flag+value 成对去重，绝不能单独删掉重复的 flag——否则后面的值会变成孤儿，
 * 被 JVM 当成主类（Forge 1.20.1 启动崩溃：找不到或无法加载主类 java.base/java.lang.invoke=...）。
 */
private fun deduplicateArguments(jsonObject: JsonObject) {
    val arguments = jsonObject.get("arguments") as? JsonObject ?: return
    listOf("game", "jvm").forEach { key ->
        val array = arguments.get(key) as? JsonArray ?: return@forEach
        val seen = LinkedHashSet<String>()
        val result = JsonArray()
        var i = 0
        while (i < array.size()) {
            val element = array[i]
            val isValueConsumingFlag = element.isJsonPrimitive && element.asJsonPrimitive.isString &&
                element.asString in VALUE_CONSUMING_FLAGS
            // 只有当下一个 token 是普通字符串值时才按"成对"处理，避免误吞条件对象
            val next = if (i + 1 < array.size()) array[i + 1] else null
            if (isValueConsumingFlag && next != null && next.isJsonPrimitive && next.asJsonPrimitive.isString) {
                val identity = element.asString + " " + next.asString
                if (seen.add(identity)) {
                    result.add(element)
                    result.add(next)
                }
                i += 2
            } else {
                //自包含参数（-Dfoo=bar、-Xmx、条件对象等）按自身序列化结果去重
                val identity = element.toString()
                if (seen.add(identity)) result.add(element)
                i += 1
            }
        }
        if (result.size() != array.size()) {
            arguments.add(key, result)
            Logger.debug(TAG, "Deduplicated arguments.$key: ${array.size()} -> ${result.size()}")
        }
    }
}

/**
 * 这些选项必须"吃掉下一个 token 作为值"（空格分隔形式，而非 = 内联形式）。
 * 去重时必须按 flag+value 成对判断：
 * - 同一个 flag 后跟不同值（如两个 --add-opens 指向不同 module）必须都保留；
 * - 完全相同的 flag+value 对（如多个 loader 都加了 --width 854）才合并。
 */
private val VALUE_CONSUMING_FLAGS = setOf(
    // JVM 模块化参数（空格形式）
    "--add-opens", "--add-exports", "--add-modules", "--add-reads",
    "-p", "--module-path", "-cp", "--classpath", "--upgrade-module-path",
    // 游戏参数
    "--username", "--version", "--gameDir", "--assetsDir", "--assetIndex",
    "--uuid", "--accessToken", "--session", "--userType", "--versionType",
    "--userProperties", "--quickPlaySingleplayer", "--quickPlayMultiplayer",
    "--quickPlayRealms", "--server", "--port", "--width", "--height",
    "--proxyHost", "--proxyPort", "--proxyUser", "--proxyPass",
    "--tracer", "--tweakClass", "--launchTarget",
    // Forge/FML 透传参数
    "--fml.forgeVersion", "--fml.mcVersion", "--fml.forgeGroup", "--fml.mcpVersion"
)
private fun addLaunchForInfo(
    info: GameDownloadInfo,
    jsonObject: JsonObject
) {
    val infos = mutableListOf(
        //默认 Minecraft 信息
        LaunchFor.Info(
            version = info.gameVersion,
            name = "Minecraft"
        )
    )

    info.optifine?.toLaunchForInfo()?.let { infos.add(it) }
    info.forge?.toLaunchForInfo()?.let { infos.add(it) }
    info.neoforge?.toLaunchForInfo()?.let { infos.add(it) }
    info.fabric?.toLaunchForInfo()?.let { infos.add(it) }
    info.quilt?.toLaunchForInfo()?.let { infos.add(it) }

    val launchFor = LaunchFor(
        infos = infos.toTypedArray()
    )

    jsonObject.add("launchFor", GSON.toJsonTree(launchFor))
}

/**
 * 快速获取游戏文件的绝对路径
 * @param suffix 文件后缀
 * path/to/game -> path/to/game/game.suffix
 */
private fun File?.gameFileOrNull(suffix: String): String? {
    return this?.let { it.resolve("${it.name}.$suffix").absolutePath }
}

/**
 * 确保目标 Json 的 minecraftArguments 不为 null 且部位空字符串
 */
fun JsonObject.safeGetMinecraftArguments(): String? {
    return this.safeGetMember("minecraftArguments").takeIf { it.isNotBlank() }
}

/**
 * 兼容性更好的 Minecraft 参数分割
 */
private fun splitMinecraftArguments(args: String): List<String> {
    val rawArgs = args.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
    val splitArgs = mutableListOf<String>()
    var currentArg: StringBuilder? = null

    for (segment in rawArgs) {
        when {
            //新参数以 - 开头
            segment.startsWith("-") -> {
                currentArg?.let { splitArgs.add(it.toString()) }
                currentArg = StringBuilder(segment)
            }
            currentArg != null -> {
                currentArg.append(" ").append(segment)
            }
            //无前缀的独立参数（比如 legacy 格式）
            else -> splitArgs.add(segment)
        }
    }
    currentArg?.let { splitArgs.add(it.toString()) }
    return splitArgs
}

/**
 * 对于 Minecraft 参数的去重
 */
fun deduplicateMinecraftArguments(args: List<String>): List<String> {
    val seenKeys = mutableSetOf<String>()
    val result = mutableListOf<String>()

    //反向遍历，保留首次出现的参数（相当于正向的最后一次出现）
    args.asReversed().forEach { arg ->
        //带值的可重复参数（如 --tweakClass）以完整参数串为键，避免不同 Tweaker 被错误折叠
        val key = if (arg.startsWith("--tweakClass")) arg else arg.split(" ").firstOrNull() ?: arg
        if (seenKeys.add(key)) {
            result.add(arg)
        }
    }

    return result.asReversed()
}

/**
 * 将 OptiFine 与 Forge 的共存方式按 Forge 时代烘焙进合并结果
 * 参考 [FCL MaintainTask](https://github.com/FoldCraftLauncher/FoldCraftLauncher/blob/main/FCL/src/main/java/com/tungsten/fclcore/download/MaintainTask.java)
 *
 * - 纯净环境：OptiFine Json 自带的 LaunchWrapper + OptiFineTweaker 直接生效，无需处理
 * - LaunchWrapper 时代（Forge ≤ 1.12.2）：Tweaker 替换为 OptiFineForgeTweaker 并排在 FMLTweaker 之后，
 *   OptiFine 以 installer Jar 追加到 classpath 末尾
 * - ModLauncher 时代（Forge 1.13 ~ 1.16）：OptiFine 移出 classpath，通过 TransformerDiscoveryService 加载
 * - BootstrapLauncher 时代（Forge/NeoForge 1.17+）：OptiFine 原生兼容，以 installer Jar 追加到 classpath 末尾
 */
private fun bakeOptiFineCompatibility(
    info: GameDownloadInfo,
    outputJson: JsonObject
) {
    if (info.optifine == null) return

    val hasForge = info.forge != null || info.neoforge != null
    if (!hasForge) return

    val mainClass = outputJson.safeGetMember("mainClass")

    //Forge 存在时 OptiFine 不再以独立 Tweaker 方式加载，先移除其 Tweaker 参数
    removeTweakArgument(outputJson, OPTIFINE_TWEAKER)

    when (mainClass) {
        LAUNCH_WRAPPER_MAIN -> {
            //如果 Forge 或 LiteLoader 已安装，OptiFine 需要 Forge 版 Tweaker，并保证其在 FMLTweaker 之后加载
            appendMinecraftArgument(outputJson, "--tweakClass $OPTIFINE_FORGE_TWEAKER")
            adjustOptiFineLibraries(outputJson, replaceWithInstaller = true)
        }
        MOD_LAUNCHER_MAIN -> {
            val optiFineMavenVersion = findOptiFineLibraryVersion(outputJson)
            adjustOptiFineLibraries(outputJson, replaceWithInstaller = false)
            optiFineMavenVersion?.let { version ->
                //通过 JVM 参数让 ModLauncher 的发现服务找到 OptiFine installer
                addJvmArgument(
                    outputJson,
                    "-Dhmcl.transformer.candidates=" +
                            $$"${library_directory}/optifine/OptiFine/$$version/OptiFine-$$version-installer.jar"
                )
                addLibraryIfAbsent(outputJson, TRANSFORMER_DISCOVERY_SERVICE_LIB)
            }
        }
        else -> adjustOptiFineLibraries(outputJson, replaceWithInstaller = true)
    }
}

/**
 * 从 minecraftArguments 与 arguments.game 中移除指定的 Tweaker 参数
 */
private fun removeTweakArgument(outputJson: JsonObject, tweaker: String) {
    //旧格式：minecraftArguments 字符串
    outputJson.safeGetMinecraftArguments()?.let { args ->
        val filtered = splitMinecraftArguments(args)
            .filterNot { it == "--tweakClass $tweaker" }
            .joinToString(" ")
        if (filtered.isNotBlank()) {
            outputJson.addProperty("minecraftArguments", filtered)
        } else {
            outputJson.remove("minecraftArguments")
        }
    }

    //新格式：arguments.game 数组
    outputJson.getAsJsonObject("arguments")?.getAsJsonArray("game")?.let { gameArgs ->
        val cleaned = JsonArray()
        var index = 0
        while (index < gameArgs.size()) {
            val element = gameArgs[index]
            val isTweakerPair = element.isJsonPrimitive &&
                    element.asString == "--tweakClass" &&
                    index + 1 < gameArgs.size() &&
                    gameArgs[index + 1].isJsonPrimitive &&
                    gameArgs[index + 1].asString == tweaker
            if (isTweakerPair) {
                index += 2
            } else {
                cleaned.add(element)
                index++
            }
        }
        outputJson.getAsJsonObject("arguments").add("game", cleaned)
    }
}

/**
 * 向 minecraftArguments 末尾追加参数
 */
private fun appendMinecraftArgument(outputJson: JsonObject, argument: String) {
    val existing = outputJson.safeGetMinecraftArguments()
    outputJson.addProperty(
        "minecraftArguments",
        existing?.takeIf { it.isNotBlank() }?.let { "$it $argument" } ?: argument
    )
}

/**
 * 向 arguments.jvm 末尾追加参数
 */
private fun addJvmArgument(outputJson: JsonObject, argument: String) {
    val arguments = outputJson.getAsJsonObject("arguments")
        ?: JsonObject().also { outputJson.add("arguments", it) }
    val jvm = arguments.getAsJsonArray("jvm") ?: JsonArray().also { arguments.add("jvm", it) }
    jvm.add(JsonPrimitive(argument))
}

/**
 * 查找合并结果中 OptiFine 本体库的 Maven 版本号
 */
private fun findOptiFineLibraryVersion(outputJson: JsonObject): String? {
    return outputJson.getAsJsonArray("libraries")
        ?.firstOrNull { element ->
            (element as? JsonObject)?.get("name")?.asString?.startsWith("optifine:OptiFine:") == true
        }
        ?.asJsonObject
        ?.get("name")?.asString
        ?.split(":")?.getOrNull(2)
}

/**
 * 处理合并结果中的 OptiFine 库
 * @param replaceWithInstaller true 时以 installer Jar 替换本体库并追加到 classpath 末尾（Forge 之后加载）；
 *                             false 时直接移除本体库（由 TransformerDiscoveryService 加载）；
 *                             两种情况都会移除与 Forge 自带 launchwrapper 冲突的 optifine:launchwrapper-of
 */
private fun adjustOptiFineLibraries(outputJson: JsonObject, replaceWithInstaller: Boolean) {
    val libraries = outputJson.getAsJsonArray("libraries") ?: return

    var installerEntry: JsonObject? = null
    val cleaned = JsonArray()
    libraries.forEach { element ->
        val name = (element as? JsonObject)?.get("name")?.asString
        when {
            //与 Forge/LiteLoader 自带的 launchwrapper 冲突，直接移除
            name?.startsWith("optifine:launchwrapper-of:") == true -> return@forEach
            name?.startsWith("optifine:OptiFine:") == true -> {
                if (replaceWithInstaller && installerEntry == null) {
                    val version = name.split(":").getOrNull(2)
                    if (version != null) {
                        installerEntry = JsonObject().apply {
                            addProperty("name", "optifine:OptiFine:$version:installer")
                        }
                    }
                }
                return@forEach
            }
            else -> cleaned.add(element)
        }
    }
    //OptiFine 应当在 classpath 末尾，保证在 Forge 之后加载
    installerEntry?.let { cleaned.add(it) }

    libraries.removeAll { true }
    cleaned.forEach { libraries.add(it) }
}

/**
 * 添加 HMCL TransformerDiscoveryService 库
 */
private fun addLibraryIfAbsent(outputJson: JsonObject, libraryName: String) {
    val libraries = outputJson.getAsJsonArray("libraries")
        ?: JsonArray().also { outputJson.add("libraries", it) }
    val exists = libraries.any { (it as? JsonObject)?.get("name")?.asString == libraryName }
    if (!exists) {
        libraries.add(JsonObject().apply { addProperty("name", libraryName) })
    }
}

/**
 * 去重 libraries，基于 groupId, artifactId, classifier 三元组去重
 * 如果存在多个版本的同一库，则保留版本号更高的库
 */
private fun deduplicateLibraries(jsonObject: JsonObject, baseFolder: String = ".minecraft") {
    if (!jsonObject.has("libraries")) return

    val libraries = jsonObject.getAsJsonArray("libraries")
    val libMap = mutableMapOf<Triple<String, String, String>, JsonObject>() // groupId, artifactId, classifier

    for (element in libraries) {
        if (!element.isJsonObject) continue
        val lib = element.asJsonObject
        val name = lib.get("name")?.asString ?: continue

        //解析库名称的组件（包含显式分类器）
        val (groupId, artifactId, version, explicitClassifier) = parseLibraryComponents(name)

        val path = getLibraryPath(name, baseFolder = baseFolder)
        val filename = path.substringAfterLast(File.separator)
        val extractedClassifier = extractClassifier(artifactId, version, filename)

        val finalClassifier = explicitClassifier ?: extractedClassifier

        val key = Triple(groupId, artifactId, finalClassifier)
        val existing = libMap[key]

        //检查库的版本，仅保留高版本的库
        if (existing == null || isCurrentVersionHigher(version, existing)) {
            libMap[key] = lib
        }
    }

    libraries.removeAll { true }
    libMap.values.forEach { libraries.add(it) }
}

/**
 * 提取文件名中的分类器
 */
private fun extractClassifier(artifactId: String, version: String, filename: String): String {
    val expectedBase = "$artifactId-$version"
    val baseWithJar = filename.substringBeforeLast(".jar")
    return baseWithJar
        .takeIf { it.startsWith(expectedBase) }
        ?.removePrefix(expectedBase)
        ?.removePrefix("-")
        ?: ""
}

/**
 * 比较当前库版本是否高于已存在的库
 * 使用 [org.apache.maven.artifact.versioning.ComparableVersion] 比较版本
 */
private fun isCurrentVersionHigher(currentVersion: String, existingLib: JsonObject): Boolean {
    val existingName = existingLib.get("name").asString
    val (_, _, existingVersion) = parseLibraryComponents(existingName)
    return currentVersion.isBiggerTo(existingVersion)
}


