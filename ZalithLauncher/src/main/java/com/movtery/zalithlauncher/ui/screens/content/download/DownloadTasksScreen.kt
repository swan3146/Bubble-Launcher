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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.movtery.zalithlauncher.ui.screens.content.download

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.coroutine.TitledTask
import com.movtery.zalithlauncher.game.download.game.GameInstallCenter
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.file.formatFileSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * PCL Pro Android: 下载任务详情页
 *
 * 从后台下载小窗点击进来后，可以看到：
 * 1. 当前下载的项目信息（版本名 / 游戏版本 / 加载器）
 * 2. 整体下载进度与实时下载速度
 * 3. 每个子任务（游戏本体、资源、库文件、Addon 等）的进度、阶段与速度
 */
@Composable
fun DownloadTasksScreen(modifier: Modifier = Modifier) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val installState by GameInstallCenter.state.collectAsStateWithLifecycle()
    val installer = (installState as? GameInstallCenter.State.Running)?.installer
    val installerTasks by remember(installer) {
        installer?.tasksFlow ?: MutableStateFlow(emptyList<TitledTask>())
    }.collectAsStateWithLifecycle()

    val runningTasks = remember(installerTasks, tasks) {
        buildList {
            addAll(installerTasks.map { it.task })
            addAll(tasks)
        }.distinct()
    }

    //项目整体进度：所有安装子任务进度的平均值
    //-1f 表示"不确定"：当没有任何子任务上报过有效进度时，整体进度也应为不确定，
    //而不是退回 0f（0 会显示成空条，与"下载中"的状态矛盾）
    val totalProgress by remember(installerTasks) {
        if (installerTasks.isEmpty()) flowOf(0f)
        else combine(installerTasks.map { it.task.progress }) { values ->
            val valid = values.filter { it >= 0f }
            if (valid.isEmpty()) -1f
            else valid.average().toFloat()
        }
    }.collectAsStateWithLifecycle(0f)

    //项目整体速度：所有安装子任务速度之和
    val totalSpeed by remember(installerTasks) {
        if (installerTasks.isEmpty()) flowOf(0L)
        else combine(installerTasks.map { it.task.rateBytesPerSec }) { values ->
            values.filterNotNull().sum()
        }
    }.collectAsStateWithLifecycle(0L)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (runningTasks.isEmpty()) {
            item {
                EmptyHint()
            }
        }

        if (installer != null || installerTasks.isNotEmpty()) {
            item {
                ProjectCard(
                    installer = installer,
                    progress = totalProgress,
                    speed = totalSpeed,
                    onCancelAll = {
                        GameInstallCenter.cancel()
                        tasks.forEach { TaskSystem.cancelTask(it.id) }
                    }
                )
            }
        }

        items(installerTasks, key = { "installer_${it.task.id}" }) { titledTask ->
            SubTaskCard(
                titledTask = titledTask,
                onCancel = { TaskSystem.cancelTask(titledTask.task.id) }
            )
        }

        items(tasks, key = { "task_${it.id}" }) { task ->
            SubTaskCard(
                task = task,
                onCancel = { TaskSystem.cancelTask(task.id) }
            )
        }
    }
}

/** 无任务时的提示 */
@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.bl_download_tasks_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/** 项目信息 + 整体进度卡片 */
@Composable
private fun ProjectCard(
    installer: GameInstaller?,
    progress: Float,
    speed: Long,
    onCancelAll: () -> Unit
) {
    val safeProgress = progress.takeIf { it >= 0f }?.coerceIn(0f, 1f)
    val percent = safeProgress?.let { (it * 100).toInt() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardColor(true),
        contentColor = onCardColor()
    ) {
        Column(modifier = Modifier.padding(all = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installer?.projectName
                            ?: stringResource(R.string.bl_download_tasks_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    installer?.let { ins ->
                        val detail = buildString {
                            append(ins.baseGameVersion)
                            ins.modLoaderSummary?.let { append(" · ").append(it) }
                        }
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(onClick = onCancelAll) {
                    Text(text = stringResource(R.string.bl_download_window_cancel))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ProgressBar(progress = safeProgress)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.bl_download_task_progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = percent?.let { "$it%" } ?: "--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.bl_download_task_speed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (speed > 0L) "${formatFileSize(speed)}/s" else "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** 单个子任务卡片：标题、阶段消息、进度、速度 */
@Composable
private fun SubTaskCard(
    onCancel: () -> Unit,
    titledTask: TitledTask? = null,
    task: Task? = null
) {
    val target = task ?: titledTask?.task ?: return

    val taskProgress by target.progress.collectAsStateWithLifecycle()
    val taskMessage by target.message.collectAsStateWithLifecycle()
    val rateBytesPerSec by target.rateBytesPerSec.collectAsStateWithLifecycle()

    val safeProgress = taskProgress.takeIf { it >= 0f }?.coerceIn(0f, 1f)
    val speedText = rateBytesPerSec?.takeIf { it > 0L }?.let { "${formatFileSize(it)}/s" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = cardColor(false),
        contentColor = onCardColor()
    ) {
        Column(modifier = Modifier.padding(all = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    titledTask?.let {
                        AndroidStringText(
                            text = it.title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    taskMessage?.let { message ->
                        AndroidStringText(
                            text = message,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                speedText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    modifier = Modifier.size(28.dp),
                    onClick = onCancel
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.generic_cancel)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProgressBar(progress = safeProgress)

            safeProgress?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = "${(it * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** 轻量进度条：不依赖 Material3 的进度条 API 版本差异 */
@Composable
private fun ProgressBar(progress: Float?) {
    val shape = RoundedCornerShape(50)
    val indeterminate = progress == null

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        if (indeterminate) {
            //不确定进度：填充块宽度 34%，在轨道内从左到右巡回滑动，模拟"正在工作"
            val transition = rememberInfiniteTransition(label = "taskProgress")
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "taskProgressPhase"
            )
            val slideOffset = maxWidth * (phase * 1.34f - 0.34f)
            Box(
                modifier = Modifier
                    .offset(x = slideOffset)
                    .width(maxWidth * 0.34f)
                    .height(6.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
