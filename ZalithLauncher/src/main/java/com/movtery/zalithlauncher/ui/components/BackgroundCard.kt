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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.cardTitleColor
import com.movtery.zalithlauncher.ui.theme.onCardColor

/**
 * PCL Pro Android 液态玻璃边框
 * 液态玻璃的高光边由 backdrop 的 highlight（定向高光着色器）绘制，
 * 这里不再叠加静态描边，避免出现双线
 */
val liquidGlassBorder: BorderStroke? = null

/**
 * 按当前界面材质样式返回卡片边框
 * PCL Pro Android: 液态玻璃 / 毛玻璃 / 纯色三种质感
 */
@Composable
fun materialBorder(): BorderStroke? {
    return when (AllSettings.launcherMaterialStyle.state) {
        MaterialStyle.LiquidGlass -> liquidGlassBorder
        MaterialStyle.Solid -> null
    }
}

/**
 * 按当前界面材质样式返回卡片容器底色
 * PCL Pro Android: 玻璃材质下容器必须让位，否则 Card 自身底色与玻璃层各铺一层，
 * 会把背景采样和光泽整块冲成纯白，玻璃感彻底消失
 */
@Composable
fun materialCardColor(
    influencedByBackground: Boolean = true
): Color {
    return when (AllSettings.launcherMaterialStyle.state) {
        MaterialStyle.LiquidGlass -> Color.Transparent
        MaterialStyle.Solid -> cardColor(influencedByBackground)
    }
}

/**
 * 玻璃层绘制用的底色：容器被透明化时回退到标准卡片色
 */
@Composable
private fun glassTintColor(colors: CardColors, influencedByBackground: Boolean): Color {
    val container = colors.containerColor
    return if (container.alpha <= 0.01f) cardColor(influencedByBackground) else container
}

/**
 * 取卡片形状的圆角（dp），供玻璃层对齐描边
 */
@Composable
private fun shapeCornerDp(shape: Shape): Float {
    if (shape !is CornerBasedShape) return 0f
    val density = LocalDensity.current
    val probe = Size(200f, 200f)
    return with(density) { shape.topStart.toPx(probe, this) } / density.density
}

/**
 * 按当前界面材质样式返回卡片投影
 * PCL Pro Android: 玻璃材质需要有悬浮阴影，否则与纯色卡片无法区分
 */
@Composable
fun materialElevation(): CardElevation {
    return when (AllSettings.launcherMaterialStyle.state) {
        //PCL Pro Android: 玻璃卡自身是透明的，深层投影会从卡片形状边缘渗出一圈灰边，
        //与 Highlight 高光叠在一起看起来就像多了一层边框，故压到几乎无阴影
        MaterialStyle.LiquidGlass -> CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp,
            focusedElevation = 1.dp,
            hoveredElevation = 2.dp,
            draggedElevation = 4.dp
        )
        MaterialStyle.Solid -> CardDefaults.cardElevation()
    }
}

/**
 * 背景卡片组件，
 * 使用方式与原本的[Card]无异，但[BackgroundCard]配置了更舒适的背景颜色
 * PCL Pro Android: 增强液态玻璃质感
 */
@Composable
fun BackgroundCard(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = materialCardColor(influencedByBackground),
        contentColor = onCardColor()
    ),
    elevation: CardElevation = materialElevation(),
    blur: Int = AllSettings.backgroundBlur.state,
    border: BorderStroke? = materialBorder(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
    ) {
        Column(
            modifier = Modifier.backgroundGlass(
                blur = blur,
                color = glassTintColor(colors, influencedByBackground),
                enabled = influencedByBackground,
                cornerRadius = shapeCornerDp(shape).dp,
                shape = shape
            ),
            content = content
        )
    }
}

/**
 * 背景卡片组件，
 * 使用方式与原本的[Card]无异，但[BackgroundCard]配置了更舒适的背景颜色
 */
@Composable
fun BackgroundCard(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = materialCardColor(influencedByBackground),
        contentColor = onCardColor(),
        disabledContainerColor = materialCardColor(influencedByBackground)
    ),
    elevation: CardElevation = materialElevation(),
    blur: Int = AllSettings.backgroundBlur.state,
    border: BorderStroke? = materialBorder(),
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable @UiComposable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier.backgroundGlass(
                blur = blur,
                color = glassTintColor(colors, influencedByBackground),
                enabled = influencedByBackground,
                cornerRadius = shapeCornerDp(shape).dp,
                shape = shape
            ),
            content = content
        )
    }
}

/**
 * 适合在背景卡片组件顶部使用的标题栏Layout
 * @param alpha 组件背景颜色不透明度
 */
@Composable
fun CardTitleLayout(
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    alpha: Float = 0.5f,
    color: Color = influencedByBackgroundColor(
        color = cardTitleColor(alpha),
        enabled = influencedByBackground
    ),
    contentColor: Color = onCardColor(),
    blur: Int = AllSettings.backgroundBlur.state,
    content: @Composable @UiComposable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            contentColor = contentColor,
        ) {
            Column(
                modifier = Modifier.backgroundGlass(blur, color, influencedByBackground),
                content = content
            )
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}