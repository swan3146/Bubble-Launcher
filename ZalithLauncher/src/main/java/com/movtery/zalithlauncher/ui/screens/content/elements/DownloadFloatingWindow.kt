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

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TitledTask
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.file.formatFileSize

/**
 * PCL Pro Android: 后台下载小窗
 *
 * 只要还有下载/安装任务在跑，就在右下角常驻一个进度小窗，
 * 用户可以边玩游戏、边翻设置，进度依然可见；点击直接进入下载页面。
 *
 * @param tasks 当前正在执行的资源/任务系统任务
 * @param installerTasks 游戏本体安装任务（由 GameInstallCenter 驱动，独立于 TaskSystem）
 * @param onExpandTasks 点击小窗时的回调，通常用于跳转到下载页面
 * @param onCancelAll 取消全部任务
 */
@Composable
fun DownloadFloatingWindow(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    installerTasks: List<TitledTask> = emptyList(),
    onExpandTasks: () -> Unit = {},
    onCancelAll: () -> Unit = {}
) {
    //安装器任务与普通下载任务合并展示：之前只接普通任务，导致下载游戏版本时小窗完全不出现
    val allTasks = remember(tasks, installerTasks) {
        buildList {
            addAll(installerTasks.map { it.task })
            addAll(tasks)
        }.distinct()
    }
    if (allTasks.isEmpty()) return

    //折叠状态下只留一行进度，避免遮挡界面
    var compact by rememberSaveable { mutableStateOf(false) }

    val first = allTasks.first()
    val progress by first.progress.collectAsStateWithLifecycle()
    val message by first.message.collectAsStateWithLifecycle()
    val speed by first.rateBytesPerSec.collectAsStateWithLifecycle()

    val percent = remember(progress) {
        if (progress < 0f) null else (progress * 100).toInt().coerceIn(0, 100)
    }
    val speedText = remember(speed) {
        speed?.takeIf { it > 0L }?.let { "${formatFileSize(it)}/s" }
    }

    //液体流动的相位，进度条内部高光沿着填充块不断移动
    val transition = rememberInfiniteTransition(label = "downloadLiquid")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "downloadLiquidPhase"
    )

    val shape = RoundedCornerShape(22.dp)
    val backdrop = LocalLauncherBackdrop.current
    val liquid = backdrop != null && AllSettings.launcherMaterialStyle.state == MaterialStyle.LiquidGlass
    val blur = AllSettings.backgroundBlur.state

    Box(
        modifier = modifier
            .widthIn(min = 182.dp, max = 276.dp)
            .shadow(elevation = 10.dp, shape = shape)
            .clip(shape)
            .then(
                if (liquid) {
                    Modifier.liquidGlass(
                        backdrop = backdrop,
                        tint = cardColor(false),
                        shape = shape,
                        blur = blur
                    )
                } else {
                    Modifier.backgroundGlass(
                        blur = blur,
                        color = cardColor(false),
                        shape = shape
                    )
                }
            )
            .clickable(onClick = onExpandTasks)
    ) {
        Column(modifier = Modifier.padding(all = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_arrow_downward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.bl_download_window_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = onCardColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (allTasks.size > 1) {
                    Text(
                        text = "×${allTasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor().copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { compact = !compact }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(
                            if (compact) R.drawable.ic_keyboard_arrow_up
                            else R.drawable.ic_keyboard_arrow_down
                        ),
                        contentDescription = stringResource(R.string.bl_download_window_expand),
                        tint = onCardColor()
                    )
                }
            }

            if (!compact) {
                Spacer(modifier = Modifier.height(8.dp))

                message?.let { text ->
                    AndroidStringText(
                        modifier = Modifier.padding(bottom = 6.dp),
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                LiquidProgressBar(
                    progress = percent?.let { it / 100f },
                    phase = phase,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    percent?.let {
                        Text(
                            text = "$it%",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor()
                        )
                    }
                    speedText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor().copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IdleCancelButton(onCancelAll = onCancelAll)
                }
            }
        }
    }
}

/**
 * 液态玻璃进度条
 *
 * 轨道为半透明玻璃，填充块使用主色渐变 + 顶部高光 + 沿着填充块流动的反光带，
 * 与全局液态玻璃材质保持同一套观感（不额外叠加 backdrop 采样，避免小窗每帧重绘拖慢界面）。
 *
 * @param progress 0f~1f，传 null 表示进度未知（此时填充块做来回滑动的不确定动画）
 */
@Composable
private fun LiquidProgressBar(
    progress: Float?,
    phase: Float,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = onCardColor().copy(alpha = 0.14f)
    val trackShape = RoundedCornerShape(percent = 50)

    val fraction = remember(progress) {
        progress?.coerceIn(0f, 1f)
    }
    //不确定进度：填充块宽度 34%，从左到右巡回滑动
    val movingStart = remember(phase) {
        if (fraction != null) 0f else (phase * 1.34f - 0.34f).coerceIn(-0.34f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier
            .height(11.dp)
            .clip(trackShape)
            .background(trackColor)
    ) {
        val barWidth = maxWidth
        val widthFraction = fraction ?: 0.34f
        //不确定进度时填充块在轨道内巡回滑动；确定进度时从左侧起算
        val moveOffset = if (fraction == null) barWidth * movingStart else barWidth * 0f

        Box(
            modifier = Modifier
                .offset(x = moveOffset)
                .width(barWidth * widthFraction)
                .fillMaxHeight()
                .clip(trackShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            primary.copy(alpha = 0.82f),
                            primary,
                            primary.copy(alpha = 0.86f)
                        )
                    )
                )
                .drawWithContent {
                    drawContent()
                    val w = size.width
                    val h = size.height
                    //沿填充块流动的反光带
                    val bandWidth = w * 0.55f
                    val x = -bandWidth + (w + bandWidth) * phase
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            startX = x,
                            endX = x + bandWidth
                        )
                    )
                    //顶部一层薄高光，形成玻璃厚度感
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.34f),
                        size = Size(w, h * 0.4f),
                        cornerRadius = CornerRadius(h * 0.2f)
                    )
                }
        )
    }
}

@Composable
private fun IdleCancelButton(onCancelAll: () -> Unit) {
    IconButton(
        modifier = Modifier.size(22.dp),
        onClick = onCancelAll
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.bl_download_window_cancel),
            tint = onCardColor().copy(alpha = 0.7f)
        )
    }
}
