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

package com.movtery.zalithlauncher.ui.screens.content.wallpaper

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.ui.components.LiquidLoadingIndicator
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.screens.content.elements.BackgroundVideoPauseGate
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.VideoPlayer
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PCL Pro Android: 动态壁纸选择器
 *
 * 布局会跟随横竖屏切换：
 * 横屏（启动器默认朝向，窗体高度很紧张）采用左右分栏，左侧预览、右侧壁纸列表与操作按钮；
 * 竖屏采用上下结构，中间内容区域可滚动，底部按钮始终固定在窗口底部，保证确认按钮不会被挤出屏幕。
 *
 * @param backgroundViewModel 用于把选中的壁纸落盘为启动器背景
 * @param onDismiss 关闭回调（应用成功与取消都会回调，由调用方记录引导状态）
 * @param introMode 首次启动引导模式，会额外显示引导文案
 */
@Composable
fun WallpaperPickerDialog(
    backgroundViewModel: BackgroundViewModel?,
    onDismiss: () -> Unit,
    introMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //PCL Pro Android: 弹窗内有预览视频，打开期间先暂停主界面背景视频，
    //避免两路 ExoPlayer 同时解码造成卡顿与闪黑（动态/静态壁纸看起来像互相冲突）
    DisposableEffect(Unit) {
        BackgroundVideoPauseGate.paused = true
        onDispose {
            BackgroundVideoPauseGate.paused = false
        }
    }

    var selected by remember {
        mutableStateOf(
            LauncherWallpapers.current() ?: LauncherWallpapers.builtIn.first()
        )
    }
    var applying by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }

    //支持把手机里的视频直接当作动态壁纸，不只是内置的那几个
    val localPicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = false,
            allowVideos = true,
            allowMultiple = false
        )
    ) { result ->
        val uri = result?.firstOrNull() ?: return@rememberLauncherForActivityResult
        val viewModel = backgroundViewModel ?: return@rememberLauncherForActivityResult
        if (applying) return@rememberLauncherForActivityResult
        applying = true
        scope.launch {
            try {
                viewModel.import(context, uri)
                //换成自己的视频后，不再属于任何内置壁纸
                LauncherWallpapers.clearAppliedIfNotBuiltIn()
                AllSettings.videoWallpaperId.save("")
                applied = true
                delay(400)
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.bl_wallpaper_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                applying = false
            }
        }
    }

    //预览用的播放组件在退出弹窗时必须销毁，否则后台会持续解码
    var previewEnabled by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        onDispose { previewEnabled = false }
    }

    val onPickLocal: () -> Unit = {
        if (!applying) localPicker.launch(Unit)
    }

    val onClose: () -> Unit = {
        if (!applying) onDismiss()
    }

    val onApply: () -> Unit = {
        if (!applying && !applied) {
            val viewModel = backgroundViewModel
            if (viewModel != null) {
                applying = true
                scope.launch {
                    try {
                        LauncherWallpapers.apply(context, selected, viewModel)
                        applied = true
                        delay(600)
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.bl_wallpaper_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        applying = false
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!applying) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !applying
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                //横屏可用高度很小，留够边距避免被系统栏压住
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight

                if (isLandscape) {
                    LauncherLandscapeLayout(
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        selected = selected,
                        onSelect = {
                            selected = it
                            applied = false
                        },
                        applying = applying,
                        applied = applied,
                        previewEnabled = previewEnabled,
                        introMode = introMode,
                        onPickLocal = onPickLocal,
                        onClose = onClose,
                        onApply = onApply
                    )
                } else {
                    LauncherPortraitLayout(
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        selected = selected,
                        onSelect = {
                            selected = it
                            applied = false
                        },
                        applying = applying,
                        applied = applied,
                        previewEnabled = previewEnabled,
                        introMode = introMode,
                        onPickLocal = onPickLocal,
                        onClose = onClose,
                        onApply = onApply
                    )
                }
            }
        }
    }
}

/**
 * 横屏布局：左侧预览，右侧标题、壁纸列表与操作按钮
 */
@Composable
private fun LauncherLandscapeLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    selected: BuiltInWallpaper,
    onSelect: (BuiltInWallpaper) -> Unit,
    applying: Boolean,
    applied: Boolean,
    previewEnabled: Boolean,
    introMode: Boolean,
    onPickLocal: () -> Unit,
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        WallpaperPreview(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
            selected = selected,
            previewEnabled = previewEnabled,
            applying = applying
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 14.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        ) {
            LauncherTitleBlock(
                introMode = introMode,
                compact = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            WallpaperThumbnailRow(
                modifier = Modifier.fillMaxWidth(),
                selected = selected,
                onSelect = onSelect,
                thumbnailWidth = if (maxHeight < 420.dp) 58.dp else 72.dp
            )

            Spacer(modifier = Modifier.weight(1f))

            LauncherActionBar(
                applying = applying,
                applied = applied,
                introMode = introMode,
                onPickLocal = onPickLocal,
                onClose = onClose,
                onApply = onApply
            )
        }
    }
}

/**
 * 竖屏布局：上方预览 + 壁纸列表（可滚动），底部按钮固定
 */
@Composable
private fun LauncherPortraitLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    selected: BuiltInWallpaper,
    onSelect: (BuiltInWallpaper) -> Unit,
    applying: Boolean,
    applied: Boolean,
    previewEnabled: Boolean,
    introMode: Boolean,
    onPickLocal: () -> Unit,
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            LauncherTitleBlock(
                introMode = introMode,
                compact = false
            )

            //竖屏下预览按比例给高，但不超过窗体高度的一半，保证列表和按钮都有位置
            val previewHeight = minOf(maxWidth * 1.05f, maxHeight * 0.5f)
            WallpaperPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .padding(horizontal = 16.dp),
                selected = selected,
                previewEnabled = previewEnabled,
                applying = applying
            )

            WallpaperThumbnailRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                selected = selected,
                onSelect = onSelect,
                thumbnailWidth = 88.dp
            )
        }

        LauncherActionBar(
            applying = applying,
            applied = applied,
            introMode = introMode,
            onPickLocal = onPickLocal,
            onClose = onClose,
            onApply = onApply
        )
    }
}

@Composable
private fun LauncherTitleBlock(
    introMode: Boolean,
    compact: Boolean
) {
    Column(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = if (compact) 8.dp else 18.dp,
            bottom = if (compact) 4.dp else 10.dp
        )
    ) {
        Text(
            text = stringResource(R.string.bl_wallpaper_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (introMode) R.string.bl_wallpaper_intro_message
                else R.string.bl_wallpaper_settings_summary
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (compact) 2 else 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 选中壁纸的实时预览
 */
@Composable
private fun WallpaperPreview(
    modifier: Modifier = Modifier,
    selected: BuiltInWallpaper,
    previewEnabled: Boolean,
    applying: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
    ) {
        if (previewEnabled) {
            //PCL Pro Android: 不再用 key(selected.id) 每次重建 ExoPlayer（release+新建+prepare
            //会反复抢占硬件解码器，快速切壁纸直接卡死）。改为复用同一个播放器，VideoPlayer 内部
            //LaunchedEffect(videoUri) 会自动 setMediaItem 切到新片源，切换更顺。
            VideoPlayer(
                videoUri = Uri.parse(selected.assetUri),
                modifier = Modifier.fillMaxSize(),
                loop = true,
                muted = true
            )
        }

        //底部渐变与名称浮层
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = stringResource(selected.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = stringResource(selected.summaryRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (LauncherWallpapers.isApplied(selected)) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(R.string.bl_wallpaper_current),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        if (applying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                LiquidLoadingIndicator(color = Color.White)
            }
        }
    }
}

/**
 * 内置壁纸缩略图列表
 */
@Composable
private fun WallpaperThumbnailRow(
    modifier: Modifier = Modifier,
    selected: BuiltInWallpaper,
    onSelect: (BuiltInWallpaper) -> Unit,
    thumbnailWidth: Dp
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(items = LauncherWallpapers.builtIn, key = { it.id }) { wallpaper ->
            WallpaperThumbnail(
                wallpaper = wallpaper,
                selected = wallpaper.id == selected.id,
                applied = LauncherWallpapers.isApplied(wallpaper),
                thumbnailWidth = thumbnailWidth
            ) {
                onSelect(wallpaper)
            }
        }
    }
}

/**
 * 底部操作按钮：选本地视频 / 取消 / 应用
 */
@Composable
private fun LauncherActionBar(
    applying: Boolean,
    applied: Boolean,
    introMode: Boolean,
    onPickLocal: () -> Unit,
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            enabled = !applying,
            onClick = onPickLocal
        ) {
            Text(
                text = stringResource(R.string.bl_wallpaper_pick_local),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!introMode) {
            TextButton(
                enabled = !applying,
                onClick = onClose
            ) {
                Text(text = stringResource(R.string.generic_cancel))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (introMode) {
            TextButton(
                enabled = !applying,
                onClick = onClose
            ) {
                Text(text = stringResource(R.string.bl_wallpaper_later))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Button(
            enabled = !applying && !applied,
            onClick = onApply
        ) {
            if (applying) {
                LiquidLoadingIndicator(
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = stringResource(
                    if (applied) R.string.bl_wallpaper_applied
                    else R.string.bl_wallpaper_apply
                )
            )
        }
    }
}

@Composable
private fun WallpaperThumbnail(
    wallpaper: BuiltInWallpaper,
    selected: Boolean,
    applied: Boolean,
    thumbnailWidth: Dp = 96.dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(wallpaper.id) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(wallpaper.id) {
        thumbnail = LauncherWallpapers.thumbnail(context, wallpaper)
    }

    val scheme = MaterialTheme.colorScheme
    val borderColor = when {
        selected -> scheme.primary
        else -> scheme.outlineVariant.copy(alpha = 0.6f)
    }

    Column(
        modifier = Modifier
            .width(thumbnailWidth)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 14f)
                .heightIn(max = thumbnailWidth * 14f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surfaceVariant)
                .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
        ) {
            thumbnail?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (applied) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(10.dp),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = scheme.onPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(wallpaper.titleRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * PCL Pro Android: 启动器设置中的动态壁纸入口行
 */
@Composable
fun WallpaperSettingsEntry(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val current = LauncherWallpapers.current()
    val summary = if (current != null) {
        stringResource(current.titleRes)
    } else {
        stringResource(R.string.bl_wallpaper_settings_summary)
    }

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.bl_wallpaper_title),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.alpha(0.7f),
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_arrow_right_rounded),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
