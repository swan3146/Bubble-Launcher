package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.moandjiezana.toml.Toml
import java.io.File
import java.util.zip.ZipFile

private val SYSTEM_DEPS = setOf(
    "minecraft", "fabricloader", "java", "forge", "neoforge", "quilt_loader",
    "mixin", "mixinextras", "os", "fabric", "intermediary", "language.kotlin",
    "kotlinforforge", "modloader"
)

private data class JarMod(
    val file: File,
    val id: String,
    val name: String,
    val deps: List<String>,
    val breaks: List<String>,
)

private data class Risk(
    val mod: JarMod,
    val reasons: List<String>,
)

private fun depId(raw: String): String =
    raw.trim().substringBefore(':').substringBefore('*').trim().lowercase()

private fun scan(modsDir: File): List<Risk> {
    val files = modsDir.listFiles()?.filter {
        it.isFile && it.extension.equals("jar", true)
    } ?: return emptyList()

    val mods = mutableListOf<JarMod>()
    for (f in files) {
        runCatching {
            ZipFile(f).use { zip ->
                //1) Fabric
                val fabricEntry = zip.getEntry("fabric.mod.json")
                if (fabricEntry != null) {
                    val json = zip.getInputStream(fabricEntry).bufferedReader().use { JSONObject(it.readText()) }
                    val id = json.optString("id", "")
                    if (id.isBlank()) return@use
                    val name = json.optString("name", f.nameWithoutExtension)
                    val deps = json.optJSONObject("depends")?.keys()?.asSequence()?.map { depId(it) }?.toList() ?: emptyList()
                    val breaks = json.optJSONObject("breaks")?.keys()?.asSequence()?.map { depId(it) }?.toList() ?: emptyList()
                    mods.add(JarMod(f, id.lowercase(), name, deps, breaks))
                    return@use
                }
                //2) Forge / NeoForge: META-INF/mods.toml 或 neoforge.mods.toml
                for (tomlPath in listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
                    val tomlEntry = zip.getEntry(tomlPath) ?: continue
                    val text = zip.getInputStream(tomlEntry).bufferedReader().use { it.readText() }
                    val toml = Toml().read(text)
                    val modsList = toml.toMap()["mods"] as? List<Map<String, Any?>> ?: continue
                    if (modsList.isEmpty()) continue
                    val modId = (modsList[0]["modId"] as? String).orEmpty()
                    if (modId.isBlank()) continue
                    val name = (modsList[0]["displayName"] as? String) ?: f.nameWithoutExtension
                    //依赖：[[dependencies.<modId>]] 中 mandatory=true 的项
                    val deps = mutableListOf<String>()
                    runCatching {
                        val depTable = toml.getTable("dependencies")
                        depTable?.getTables(modId)?.forEach { t ->
                            val mid = t.getString("modId")?.lowercase() ?: return@forEach
                            val mandatory = t.getBoolean("mandatory") ?: false
                            if (mandatory) deps.add(mid)
                        }
                    }
                    mods.add(JarMod(f, modId.lowercase(), name, deps, emptyList()))
                    return@use
                }
            }
        }
    }

    val installedIds = mods.map { it.id }.toSet()
    val idCount = mods.groupingBy { it.id }.eachCount()

    val risks = mutableListOf<Risk>()
    for (m in mods) {
        val reasons = mutableListOf<String>()
        val lack = m.deps.filter { it !in installedIds && it !in SYSTEM_DEPS }.distinct()
        if (lack.isNotEmpty()) reasons.add("缺失前置：${lack.joinToString("、")}")
        for (b in m.breaks) {
            if (b in installedIds) reasons.add("与已装模组冲突(breaks)")
        }
        if ((idCount[m.id] ?: 0) > 1) reasons.add("重复安装了同一个模组(id=${m.id})")
        if (reasons.isNotEmpty()) risks.add(Risk(m, reasons))
    }
    return risks
}

@Composable
fun ModCrashScanDialog(onDismiss: () -> Unit) {
    var scanning by remember { mutableStateOf(true) }
    var risks by remember { mutableStateOf<List<Risk>>(emptyList()) }
    val checked = remember { mutableStateOf<Set<String>>(emptySet()) }
    var fixedCount by remember { mutableStateOf(-1) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        scanning = true
        val v = VersionsManager.currentVersion.value
        if (v == null) { scanning = false; return@LaunchedEffect }
        val modsDir = VersionFolders.MOD.getDir(v.getGameDir())
        if (!modsDir.exists()) modsDir.mkdirs()
        val r = withContext(Dispatchers.IO) { runCatching { scan(modsDir) }.getOrDefault(emptyList()) }
        risks = r
        checked.value = r.map { it.mod.file.absolutePath }.toSet()
        scanning = false
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("崩溃排错", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("检测当前版本模组（Fabric / Forge / NeoForge）的缺失前置 / 冲突 / 重复安装",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                when {
                    scanning -> Text("正在扫描模组…", style = MaterialTheme.typography.bodyMedium)
                    fixedCount >= 0 -> {
                        Text("已修复：自动禁用了 $fixedCount 个有崩溃风险的模组（已改名为 .disabled，可在模组管理里重新启用）",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF81C784))
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { fixedCount = -1; tick++ }) { Text("重新扫描") }
                            OutlinedButton(onClick = onDismiss) { Text("关闭") }
                        }
                    }
                    risks.isEmpty() -> {
                        Text("未发现模组层面的崩溃风险。", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("若仍黑屏/崩溃，可能是 Java 版本、内存或模组本身与 MC 版本不兼容。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onDismiss) { Text("关闭") }
                    }
                    else -> {
                        Text("发现 ${risks.size} 个有风险的模组（默认全部勾选修复）",
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(risks) { r ->
                                val path = r.mod.file.absolutePath
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = path in checked.value,
                                        onCheckedChange = { on ->
                                            checked.value = if (on) checked.value + path else checked.value - path
                                        }
                                    )
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(r.mod.name, style = MaterialTheme.typography.titleSmall)
                                        r.reasons.forEach {
                                            Text("⚠ $it",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFE57373))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                //一键修复：把勾选的模组重命名为 .disabled（可逆）
                                var n = 0
                                risks.filter { it.mod.file.absolutePath in checked.value }.forEach { r ->
                                    val src = r.mod.file
                                    val dst = File(src.absolutePath + ".disabled")
                                    if (src.renameTo(dst)) n++
                                }
                                fixedCount = n
                            }) { Text("一键修复（禁用勾选模组）") }
                            OutlinedButton(onClick = onDismiss) { Text("取消") }
                        }
                    }
                }
            }
        }
    }
}
