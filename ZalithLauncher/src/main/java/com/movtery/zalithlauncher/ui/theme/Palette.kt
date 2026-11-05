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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.ui.components.influencedByBackgroundColor

/** 应用整体背景的颜色 */
@Composable
fun backgroundColor(): Color = MaterialTheme.colorScheme.surfaceContainer
@Composable
fun onBackgroundColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * 卡片背景颜色
 * [androidx.compose.material3.Card]
 * [com.movtery.zalithlauncher.ui.components.BackgroundCard]
 * [androidx.compose.ui.window.Dialog]
 * @param influencedByBackground 是否受背景内容影响，更改自身不透明度
 */
@Composable
fun cardColor(
    influencedByBackground: Boolean = true
): Color {
    val base = influencedByBackgroundColor(
        color = MaterialTheme.colorScheme.surfaceBright,
        enabled = influencedByBackground
    )
    if (AllSettings.launcherMaterialStyle.state != MaterialStyle.LiquidGlass) return base
    //液态玻璃：卡片底色再整体压薄一层，
    //这样即使某些窗口没有单独挂 backdrop 采样源，也不会露出一块不透光的实心卡片
    return base.copy(alpha = (base.alpha * 0.62f).coerceIn(0.22f, 0.72f))
}
@Composable
fun onCardColor(): Color = MaterialTheme.colorScheme.onSurface
/**
 * 卡片顶部Title的背景颜色，半透明的surface
 */
@Composable
fun cardTitleColor(
    alpha: Float = 0.5f
): Color {
    if (AllSettings.launcherMaterialStyle.state != MaterialStyle.LiquidGlass) {
        return MaterialTheme.colorScheme.surface.copy(alpha = alpha)
    }
    //PCL Android FE: 液态玻璃下标题条同样要压薄，否则卡片顶部会浮出一条实色横条
    return MaterialTheme.colorScheme.surface.copy(alpha = (alpha * 0.55f).coerceIn(0.10f, 0.42f))
}

/**
 * 卡片上的Item的背景颜色
 * @param influencedByBackground 是否受背景内容影响，更改自身不透明度
 */
@Composable
fun itemColor(
    influencedByBackground: Boolean = true,
    isDark: Boolean = isLauncherInDarkTheme()
): Color {
    val base = influencedByBackgroundColor(
        color = if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        },
        enabled = influencedByBackground
    )
    if (AllSettings.launcherMaterialStyle.state != MaterialStyle.LiquidGlass) return base
    //PCL Android FE: 液态玻璃下“卡片里的内容块”（设置项行、下载条目、链接列表项）
    //原先仍是整块实色，和电池玻璃外壳撞在一起很不协调，这里统一压薄
    return base.copy(alpha = (base.alpha * 0.55f).coerceIn(0.12f, 0.52f))
}
@Composable
fun onItemColor() = MaterialTheme.colorScheme.onSurface