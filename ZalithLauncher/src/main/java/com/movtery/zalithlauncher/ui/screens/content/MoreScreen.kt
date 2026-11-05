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

package com.movtery.zalithlauncher.ui.screens.content

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.path.URL_PROJECT
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * PCL Pro Android: 更多页面
 * 聚合免责声明、意见反馈与全局材质样式切换
 */
@Composable
fun MoreScreen(
    key: NormalNavKey.More,
    backScreenViewModel: ScreenBackStackViewModel,
    openLink: (url: String) -> Unit
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    BaseScreen(
        Triple(key, mainScreenKey, false)
    ) { isVisible ->
        AnimatedLazyColumn(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(all = 12.dp)
        ) { scope ->
            animatedItem(scope) { yOffset ->
                HeroCard(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                )
            }

            animatedItem(scope) { yOffset ->
                //PCL Pro Android: 删除"问题与反馈"卡片，仅保留免责声明入口并占满整行
                EntryCard(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                        .fillMaxWidth(),
                    icon = painterResource(R.drawable.ic_pcl_shield),
                    title = stringResource(R.string.bl_more_item_disclaimer),
                    summary = stringResource(R.string.bl_more_item_disclaimer_summary),
                    onClick = {
                        backScreenViewModel.mainScreen.navigateTo(NormalNavKey.Disclaimer)
                    }
                )
            }

            animatedItem(scope) { yOffset ->
                MaterialSection(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                )
            }

            animatedItem(scope) { yOffset ->
                AboutMiniCard(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    openLink = openLink
                )
            }
        }
    }
}

/**
 * 顶部主卡片：版本、当前材质与运行环境
 */
@Composable
private fun HeroCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val style = AllSettings.launcherMaterialStyle.state

    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            //柔和的渐变与光斑，让顶部卡片更有层次
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.30f),
                            scheme.tertiary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                val glowRadius = size.minDimension * 0.95f
                val glowCenter = Offset(size.width * 0.92f, -size.height * 0.15f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.tertiary.copy(alpha = 0.30f), Color.Transparent),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = glowCenter
                )
            }

            Column(modifier = Modifier.padding(all = 18.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_launcher),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bl_more_hero_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.bl_more_hero_summary),
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor().copy(alpha = 0.68f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(text = "v${BuildConfig.VERSION_NAME}")
                    InfoChip(text = stringResource(style.nameRes()))
                    InfoChip(text = "Android ${Build.VERSION.RELEASE}")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * 入口卡片：图标 + 标题 + 副标题
 */
@Composable
private fun EntryCard(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    BackgroundCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = icon,
                        contentDescription = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = onCardColor().copy(alpha = 0.62f)
            )
        }
    }
}

/**
 * 全局材质样式切换
 */
@Composable
private fun MaterialSection(modifier: Modifier = Modifier) {
    val current = AllSettings.launcherMaterialStyle.state

    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_pcl_palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.bl_more_section_material),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MaterialStyle.entries.forEach { style ->
                    StylePreview(
                        modifier = Modifier.weight(1f),
                        style = style,
                        selected = style == current,
                        onClick = { AllSettings.launcherMaterialStyle.save(style) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.bl_more_material_hint),
                style = MaterialTheme.typography.labelSmall,
                color = onCardColor().copy(alpha = 0.62f)
            )
        }
    }
}

/**
 * 材质预览小卡：以矢量绘制的方式预览三种质感
 */
@Composable
private fun StylePreview(
    style: MaterialStyle,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val border = if (selected) {
        BorderStroke(1.6.dp, scheme.primary)
    } else {
        BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f))
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = border
    ) {
        Column(modifier = Modifier.padding(all = 8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    when (style) {
                        MaterialStyle.LiquidGlass -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    listOf(
                                        scheme.primary.copy(alpha = 0.55f),
                                        scheme.tertiary.copy(alpha = 0.34f)
                                    )
                                )
                            )
                            //边缘折射亮环
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.45f),
                                cornerRadius = CornerRadius(10.dp.toPx()),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            //斜向高光
                            drawLine(
                                color = Color.White.copy(alpha = 0.45f),
                                start = Offset(size.width * 0.18f, 0f),
                                end = Offset(size.width * 0.02f, size.height),
                                strokeWidth = 4.dp.toPx()
                            )
                        }

                        MaterialStyle.Solid -> {
                            drawRect(color = scheme.surfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(style.nameRes()),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = scheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 底部关于信息
 */
@Composable
private fun AboutMiniCard(
    modifier: Modifier = Modifier,
    openLink: (url: String) -> Unit
) {
    BackgroundCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = { openLink(URL_PROJECT) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_info_outlined),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bl_more_item_about),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(R.drawable.ic_pcl_open_in_new),
                contentDescription = null,
                tint = onCardColor().copy(alpha = 0.55f)
            )
        }
    }
}

internal fun MaterialStyle.nameRes(): Int {
    return when (this) {
        MaterialStyle.LiquidGlass -> R.string.bl_material_liquid_glass
        MaterialStyle.Solid -> R.string.bl_material_solid
    }
}

internal fun MaterialStyle.descRes(): Int {
    return when (this) {
        MaterialStyle.LiquidGlass -> R.string.bl_material_liquid_glass_desc
        MaterialStyle.Solid -> R.string.bl_material_solid_desc
    }
}
