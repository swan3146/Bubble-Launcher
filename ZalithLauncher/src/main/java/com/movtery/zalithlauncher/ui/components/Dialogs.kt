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

package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.renderers.KopperZinkRenderer
import com.movtery.zalithlauncher.game.renderer.renderers.MobileGluesRenderer
import com.movtery.zalithlauncher.game.renderer.renderers.NGGL4ESRenderer
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.game.version.installed.utils.isBiggerOrEqualVer
import com.movtery.zalithlauncher.game.version.installed.utils.isBiggerVer
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 返回对话框内容可用的最大高度，以窗口容器高度为上界
 *
 * 输入法动画期间窗口高度可能被系统短暂压缩，这里记录最大值，避免对话框高度随之抖动
 */
@Composable
fun rememberDialogMaxHeight(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    var maxHeight by remember(windowInfo) { mutableStateOf(windowInfo.containerSize.height) }
    maxHeight = maxOf(maxHeight, windowInfo.containerSize.height)
    return with(density) { maxHeight.toDp() }
}

/**
 * 展示警告对话框
 *
 * @param title 对话框标题
 * @param text 对话框内容
 * @param dismissByDialog 是否允许Dialog自己请求关闭
 * @param onConfirm 点击确认按钮的回调
 * @param onDismiss 点击取消或对话框外部的回调
 */
@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    dismissByDialog: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = cardColor(),
        tonalElevation = 0.dp,
        onDismissRequest = {
            if (dismissByDialog) onDismiss()
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fadeEdge(state = scrollState)
                    .verticalScrollWithBar(state = scrollState)
            ) {
                Text(text = text)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                MarqueeText(text = confirmText)
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                MarqueeText(text = dismissText)
            }
        }
    )
}

/**
 * 展示警告对话框
 *
 * @param title 对话框标题
 * @param text 对话框内容
 * @param dismissByDialog 是否允许Dialog自己请求关闭
 * @param onDismiss 点击确认或对话框外部的回调
 */
@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissByDialog: Boolean = true,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = cardColor(),
        tonalElevation = 0.dp,
        onDismissRequest = {
            if (dismissByDialog) onDismiss()
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fadeEdge(state = scrollState)
                    .verticalScrollWithBar(state = scrollState)
            ) {
                Text(text = text)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                MarqueeText(text = confirmText)
            }
        }
    )
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: @Composable () -> Unit = {},
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    AlertDialog(
        containerColor = cardColor(),
        tonalElevation = 0.dp,
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fadeEdge(state = scrollState)
                    .verticalScrollWithBar(state = scrollState)
            ) {
                text()
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                MarqueeText(text = confirmText)
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onCancel) {
                MarqueeText(text = dismissText)
            }
        }
    )
}

@Composable
fun SimpleEditDialog(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    onCancel: () -> Unit = onDismissRequest,
    onConfirm: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            BackgroundCard(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    simpleEditDialogBody(
                        title = title,
                        value = value,
                        onValueChange = onValueChange,
                        label = label,
                        isError = isError,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        keyboardOptions = keyboardOptions,
                        extraBody = extraBody,
                        extraContent = extraContent
                    ).invoke(this)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onCancel
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_cancel))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = onConfirm
                        ) {
                            MarqueeText(text = stringResource(R.string.generic_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleEditDialog(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onConfirm: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ImePanContainer(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            BackgroundCard(
                modifier = Modifier
                    .padding(all = 6.dp)
                    .heightIn(max = (maxHeight - 12.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    simpleEditDialogBody(
                        title = title,
                        value = value,
                        onValueChange = onValueChange,
                        label = label,
                        isError = isError,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        keyboardOptions = keyboardOptions,
                        extraBody = extraBody,
                        extraContent = extraContent
                    ).invoke(this)

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onConfirm
                    ) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun simpleEditDialogBody(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onConfirm: () -> Unit = {}
): @Composable ColumnScope.() -> Unit = {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
    )

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fadeEdge(state = scrollState)
            .weight(1f, fill = false)
            .verticalScrollWithBar(state = scrollState)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        extraBody?.let {
            it.invoke()
            Spacer(modifier = Modifier.size(8.dp))
        }

        val focusManager = LocalFocusManager.current

        if (singleLine) {
            SingleLineTextCheck(
                text = value,
                onSingleLined = onValueChange
            )
        }

        OwnOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            label = label,
            isError = isError,
            supportingText = supportingText,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                    onConfirm()
                }
            ),
            shape = MaterialTheme.shapes.large
        )
        extraContent?.invoke()
    }
}

@Composable
fun SimpleCheckEditDialog(
    title: String,
    text: String,
    value: String,
    checked: Boolean,
    checkBoxText: String? = null,
    onValueChange: (newValue: String) -> Unit,
    onCheckedChange: (newValue: Boolean) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    SimpleEditDialog(
        title = title,
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        extraBody = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        extraContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    checkBoxText?.let{ Text(text = it, style = MaterialTheme.typography.labelMedium) }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm
    )
}

/**
 * 一个很简单的列表Dialog
 * @param items 需要列出的items
 * @param onItemSelected item被点击的回调
 * @param onDismissRequest dialog被关闭的回调
 * @param showConfirm 是否通过确认按钮来触发item的点击回调
 */
@Composable
fun <T> SimpleListDialog(
    title: String,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    onDismissRequest: (selected: Boolean) -> Unit,
    current: T? = null,
    itemLayout: @Composable (item: T, isCurrent: Boolean, onClick: () -> Unit) -> Unit,
    showConfirm: Boolean = false,
    confirmText: @Composable RowScope.() -> Unit = {
        MarqueeText(text = stringResource(R.string.generic_confirm))
    }
) {
    var selectedItem: T? by remember { mutableStateOf(current) }

    Dialog(
        onDismissRequest = {
            onDismissRequest(false)
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            BackgroundCard(
                modifier = Modifier
                    .padding(all = 3.dp)
                    .heightIn(max = (maxHeight - 6.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fadeEdge(state = state)
                            .weight(1f, fill = false),
                        state = state
                    ) {
                        items(items) { item ->
                            val isCurrent = selectedItem == item
                            itemLayout(item, isCurrent) {
                                selectedItem = item
                                if (!showConfirm && !isCurrent) {
                                    onItemSelected(item)
                                    onDismissRequest(true)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(4.dp))

                    if (showConfirm) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (selectedItem != null) {
                                    onItemSelected(selectedItem!!)
                                    onDismissRequest(true)
                                }
                            }
                        ) {
                            confirmText()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleTaskDialog(
    title: String,
    text: String? = null,
    task: suspend () -> Unit,
    context: CoroutineContext = Dispatchers.Default,
    onDismiss: () -> Unit,
    onError: (Throwable) -> Unit = {}
) {
    var inProgress by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    if (inProgress) {
        ProgressDialog(
            title = title,
            text = text
        )
    } else {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        inProgress = true
        scope.launch(context) {
            try {
                task()
            } catch (e: Throwable) {
                onError(e)
            } finally {
                inProgress = false
            }
        }
    }
}

@Composable
fun ProgressDialog(
    title: String = stringResource(R.string.generic_in_progress),
    text: String? = null
) {
    Dialog(onDismissRequest = {}) {
        BackgroundCard(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                text?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
/**
 * 启动前选择渲染器
 *
 * @param minecraftVersion 当前游戏版本号，用于给出推荐
 * @param current 当前生效的渲染器唯一标识
 * @param onSubmit 用户确认选择的渲染器
 * @param onDismiss 用户取消启动
 */
@Composable
fun RendererPickDialog(
    minecraftVersion: String,
    current: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    //PCL Android FE: MG (MobileGlues) 置顶显示
    val renderers = remember {
        val mgId = MobileGluesRenderer.getUniqueIdentifier()
        Renderers.getRenderers().sortedBy { if (it.getUniqueIdentifier() == mgId) 0 else 1 }
    }
    //PCL Android FE: MG (MobileGlues) 的渲染库是按 arm64 编译的，x86 / x86_64 设备上加载会直接
    //在启动阶段触发 native 崩溃（表现为点启动就闪退、日志停在初始化阶段），因此 x86 设备不参与推荐
    val isX86Device = remember { Architecture.isx86Device() }
    //1.17 及以上版本推荐使用 MG (MobileGlues)；x86 设备（含模拟器）改为推荐 Krypton Wrapper
    val recommendIdentifier = remember(minecraftVersion, isX86Device) {
        val isModern = runCatching { minecraftVersion.isBiggerOrEqualVer("1.17") }.getOrDefault(false)
        when {
            isX86Device -> NGGL4ESRenderer.getUniqueIdentifier()
            isModern -> MobileGluesRenderer.getUniqueIdentifier()
            else -> null
        }
    }
    //PCL Android FE: 版本不匹配的渲染器（如 1.17 以下选 MG）启动必崩，这里直接禁用掉
    val unsupported = remember(minecraftVersion, isX86Device) {
        val versionUnsupported = renderers.filterNot { renderer ->
            runCatching {
                val min = renderer.getMinMCVersion()
                val max = renderer.getMaxMCVersion()
                (min == null || minecraftVersion.isBiggerOrEqualVer(min)) &&
                        (max == null || !minecraftVersion.isBiggerVer(max))
            }.getOrDefault(true)
        }.map { it.getUniqueIdentifier() }
        buildSet {
            addAll(versionUnsupported)
            //x86 设备禁用 MG 与 Kopper Zink，避免启动即崩
            if (isX86Device) {
                add(MobileGluesRenderer.getUniqueIdentifier())
                add(KopperZinkRenderer.getUniqueIdentifier())
            }
        }
    }
    //PCL Android FE: 有推荐渲染器时默认选中推荐项，玩家直接确认即可
    var selected by rememberSaveable(current, recommendIdentifier) {
        mutableStateOf(
            when {
                recommendIdentifier != null && recommendIdentifier !in unsupported -> recommendIdentifier
                current in unsupported -> renderers.firstOrNull { it.getUniqueIdentifier() !in unsupported }
                    ?.getUniqueIdentifier() ?: current
                else -> current
            }
        )
    }

    AlertDialog(
        containerColor = cardColor(),
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.game_pick_renderer_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.game_pick_renderer_summary, minecraftVersion),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(renderers, key = { it.getUniqueIdentifier() }) { renderer ->
                        val identifier = renderer.getUniqueIdentifier()
                        val enabled = identifier !in unsupported
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(enabled = enabled) { selected = identifier }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == identifier,
                                enabled = enabled,
                                onClick = { selected = identifier }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = renderer.getRendererName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (enabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                                renderer.getRendererSummary()?.let { summary ->
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (enabled) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                }
                                if (!enabled) {
                                    //版本不匹配的渲染器选中后必崩，直接标出来
                                    Text(
                                        text = "不支持当前版本 ${minecraftVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (identifier == recommendIdentifier) {
                                    Text(
                                        text = stringResource(R.string.game_pick_renderer_recommend_mg),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selected) }) {
                MarqueeText(text = stringResource(R.string.generic_confirm))
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                MarqueeText(text = stringResource(R.string.generic_cancel))
            }
        }
    )
}
