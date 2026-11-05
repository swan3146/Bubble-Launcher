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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass

/**
 * 液态玻璃风格侧边栏（Rail）条目。
 *
 * 与录屏里的底部 Tab 保持一致的手感：
 * - 选中项使用一块玻璃胶囊作为底衬，从中间弹出并带轻微过冲
 * - 图标在选中时放大、切页时回弹
 * - 按下时纵向压扁、横向微胀（Y 轴挤压），松手弹簧复原
 *
 * 之所以不复用 [TextRailItem]：那个是横向布局（图标在文字左侧），
 * 而侧边栏需要图标在上、文字在下的纵向排布。
 *
 * @param selected 是否处于选中状态
 * @param onClick 点击回调
 * @param icon 图标内容
 * @param label 文字标签
 */
@Composable
fun GlassVerticalRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 520f)
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = selectionSpring,
        label = "RailSelection"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    //PCL Pro Android: 按压统一缩到 0.92，松手低阻尼弹簧自然过冲到 ~1.08 再回 1（Q 弹）
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "RailPress"
    )
    //图标回弹：选中放大，未选中略缩
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 0.94f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 560f),
        label = "RailIconScale"
    )

    val shape = RoundedCornerShape(22.dp)
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "RailContentColor"
    )

    //PCL Android FE: 处于 MovingGlassRail 容器内时，胶囊由容器统一绘制并在选项间上下滑动
    val sharedRail = inSharedRail()

    Box(
        modifier = modifier
            .reportRailSlot(selected)
            .widthIn(min = 56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        //选中底衬：一块跟着进度淡入的玻璃胶囊（容器统管时跳过）
        if (!sharedRail && progress > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = progress }
                    .backgroundGlass(
                        blur = AllSettings.backgroundBlur.state,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = shape
                    )
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                ) {
                    icon()
                }
                Spacer(modifier = Modifier.height(4.dp))
                label()
            }
        }
    }
}
