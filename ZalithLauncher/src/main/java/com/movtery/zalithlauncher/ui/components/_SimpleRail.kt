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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.ui.screens.content.elements.DisabledAlpha
import com.movtery.zalithlauncher.ui.screens.content.elements.LocalLauncherBackdrop
import com.movtery.zalithlauncher.ui.screens.content.elements.liquidGlass

/**
 * 导航栏item组件，可包含图标与文字。当item被选中时，
 * 会触发一个从中心向外扩展的胶囊形背景动画。
 *
 * @param text 用于定义item中文字内容的可组合函数
 * @param onClick 当该item被点击时调用的回调函数
 * @param selected 指示该item当前是否处于选中状态
 *                 选中动画的播放由此状态控制
 * @param icon 用于定义显示图标的可组合函数，图标显示在文字之前。
 * @param selectedPadding item被选中时，其内部内容的内边距
 * @param unSelectedPadding item未被选中时，其内部内容的内边距
 * @param shape 定义item裁剪边界与点击区域的形状。
 * @param backgroundColor item被选中时，显示的动画背景颜色。
 * @param selectedContentColor item被选中时，图标与文字的颜色。
 * @param unselectedContentColor item未被选中时，图标与文字的颜色。
 */
@Composable
fun TextRailItem(
    modifier: Modifier = Modifier,
    text: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    icon: @Composable RowScope.() -> Unit = {},
    selected: Boolean,
    selectedPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    unSelectedPadding: PaddingValues = selectedPadding,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true
) {
    //PCL Pro Android: 胶囊背景与内边距改用低阻尼弹簧，选中时带一点过冲，接近液态玻璃的软弹感
    val selectionSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 520f)
    val selectionSpringDp = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.72f, stiffness = 520f)
    val animationProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = selectionSpring,
        label = "SelectionAnimation"
    )
    //按压回弹：按下收到 0.92，松手用低阻尼弹簧，自然过冲到 ~1.08 再回 1，形成 Q 弹手感
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "PressScale"
    )

    //PCL Android FE: 处于 MovingGlassRail 容器内时，胶囊由容器统一绘制并在选项间滑动
    val sharedRail = inSharedRail()

    Box(
        modifier = modifier
            .reportRailSlot(selected)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else DisabledAlpha)
    ) {
        //背景扩散动画
        //PCL Android FE: 液态玻璃材质下，选中胶囊也必须是玻璃——原来这里画的是不透明的实心圆角矩形，
        //正是"顶栏/底栏选中项还是一块实色"的根源。改为真实 backdrop 采样 + 折射，
        //并且用整体淡入替代宽度逐帧扩展，避免每帧重算弯折面
        val railBackdrop = LocalLauncherBackdrop.current
        if (sharedRail) {
            //容器负责绘制滑动胶囊，这里不再画任何背景
        } else if (AllSettings.launcherMaterialStyle.state == MaterialStyle.LiquidGlass && railBackdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(animationProgress)
                    .liquidGlass(
                        backdrop = railBackdrop,
                        tint = backgroundColor,
                        shape = RoundedCornerShape(percent = 50),
                        blur = AllSettings.backgroundBlur.state
                    )
            )
        } else {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(animationProgress)
            ) {
                val maxWidth = size.width
                val minWidth = 0f
                val currentWidth = minWidth + (maxWidth - minWidth) * animationProgress

                val left = (maxWidth - currentWidth) / 2

                //绘制胶囊形状背景
                drawRoundRect(
                    color = backgroundColor,
                    topLeft = Offset(left, 0f),
                    size = Size(currentWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                )
            }
        }

        val paddingLeft by animateDpAsState(
            targetValue = if (selected) selectedPadding.calculateLeftPadding(LayoutDirection.Ltr) else unSelectedPadding.calculateLeftPadding(LayoutDirection.Ltr),
            animationSpec = selectionSpringDp
        )
        val paddingRight by animateDpAsState(
            targetValue = if (selected) selectedPadding.calculateRightPadding(LayoutDirection.Ltr) else unSelectedPadding.calculateRightPadding(LayoutDirection.Ltr),
            animationSpec = selectionSpringDp
        )
        val paddingTop by animateDpAsState(
            targetValue = if (selected) selectedPadding.calculateTopPadding() else unSelectedPadding.calculateTopPadding(),
            animationSpec = selectionSpringDp
        )
        val paddingBottom by animateDpAsState(
            targetValue = if (selected) selectedPadding.calculateBottomPadding() else unSelectedPadding.calculateBottomPadding(),
            animationSpec = selectionSpringDp
        )

        Row(
            modifier = Modifier
                .graphicsLayer {
                    //PCL Pro Android: 未选中文字/图标下移 2px、透明度 0.7；选中时回到 0、不透明
                    translationY = (1f - animationProgress) * 2.dp.toPx()
                    alpha = 0.7f + 0.3f * animationProgress
                }
                .padding(
                    start = paddingLeft,
                    end = paddingRight,
                    top = paddingTop,
                    bottom = paddingBottom
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentColor by animateColorAsState(
                targetValue = if (selected) selectedContentColor else unselectedContentColor
            )
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                icon()
                text()
            }
        }
    }
}