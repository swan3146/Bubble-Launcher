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

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

/**
 * PCL Pro Android 液态玻璃
 *
 * 与 AndroidLiquidGlass-kmp 示例项目同源的实现思路：
 * 背景层用 [layerBackdrop] 录制一次，玻璃元素用 [drawBackdrop] 实时采样并用
 * RenderEffect 做真实的模糊（blur）、透镜折射（lens）与定向高光（highlight），
 * 不再依赖预捕获位图 + 自绘渐变，效果与示例项目保持一致。
 *
 * 采样源由 [LocalLauncherBackdrop] 下发，由启动器背景层挂载。
 */
val LocalLauncherBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * 把当前组件的内容录制为液态玻璃的采样源
 */
fun Modifier.launcherBackdrop(backdrop: LayerBackdrop): Modifier = this.layerBackdrop(backdrop)

/**
 * 液态玻璃表面
 *
 * @param backdrop 采样源
 * @param tint 玻璃底色（通常为卡片色），实际绘制时会压低不透明度
 * @param cornerRadius 圆角（[shape] 为空时生效）
 * @param shape 元素的真实形状，透镜折射依赖圆角 SDF，务必传卡片自身的形状
 * @param blur 用户设置的模糊强度（0~40），内部折算为 0~16dp 的模糊半径
 * @param tintFactor 底色不透明度系数（默认已压到极薄）
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: LayerBackdrop,
    tint: Color,
    cornerRadius: Dp = 12.dp,
    shape: Shape? = null,
    blur: Int = 20,
    tintFactor: Float = 0.18f,
): Modifier {
    val density = LocalDensity.current
    //模糊半径与折射强度对齐示例组件（底栏 8dp 模糊、24dp 折射量）
    //PCL Android FE: 原来 40 档模糊会折算成 16dp（≈48px）半径，列表里几十块玻璃同时做
    //RenderEffect 模糊会把 GPU 直接拉满，是启动器卡顿的主要来源；这里压到 3~10dp，
    //同时保留透镜折射维持液态观感
    val blurRadiusPx = with(density) { (blur.coerceIn(0, 40) * 0.28f).coerceIn(3f, 10f).dp.toPx() }
    //透镜折射依赖圆角 SDF，非圆角形状会让库直接抛异常，这里做一次白名单兜底
    val resolvedShape: Shape = when (shape) {
        is CornerBasedShape, is AbsoluteRoundedCornerShape -> shape
        else -> RoundedCornerShape(cornerRadius)
    }
    //PCL Pro Android: 通透度拉满——底色仅保留极薄一层，玻璃观感全部交给采样模糊与折射
    val surfaceAlpha = (tint.alpha * tintFactor).coerceIn(0.02f, 0.08f)

    //折射量上下限是纯密度换算，不随尺寸变化；这里缓存为 px，避免每次 updateEffects
    //（尺寸/密度变化时）都重复做 4 次 dp→px 浮点换算。材质参数本身不变。
    val refractionMinHeight = with(density) { 4.dp.toPx() }
    val refractionMaxHeight = with(density) { 14.dp.toPx() }
    val refractionMinAmount = with(density) { 6.dp.toPx() }
    val refractionMaxAmount = with(density) { 18.dp.toPx() }

    //高光参数固定，缓存避免每块玻璃每次重组都新建 Highlight 对象
    val glassHighlight = remember {
        Highlight(
            width = 0.55.dp,
            alpha = 0.40f,
            style = HighlightStyle.Default
        )
    }

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { resolvedShape },
        effects = {
            //PCL Pro Android: 全站统一同一套液态玻璃材质参数——此前按控件尺寸分级
            //（小件不加模糊/折射、中件只加模糊）会让列表行、标签、开关看起来像普通半透明色块，
            //与卡片、侧栏观感割裂。现在无论控件大小都走 blur + vibrancy + lens，
            //仅把折射量按控件短边等比收敛，避免小件边缘被折射吃掉。
            val minSide = size.minDimension
            blur(blurRadiusPx)
            vibrancy()
            val refractionHeight = (minSide * 0.10f).coerceIn(
                refractionMinHeight,
                refractionMaxHeight
            )
            val refractionAmount = (minSide * 0.12f).coerceIn(
                refractionMinAmount,
                refractionMaxAmount
            )
            lens(refractionHeight, refractionAmount)
        },
        highlight = {
            //PCL Pro Android: 高光边收敛到单条细亮线，并对齐卡片圆角。
            //此前 alpha=1f 的满强度高光会与 Material Card 的投影叠成一条硬边框，
            //表现为卡片内部多出一圈边框（视觉 bug）
            glassHighlight
        },
        //卡片阴影由 Material3 Card 自身提供，避免两层阴影叠加
        shadow = null,
        innerShadow = null,
        onDrawSurface = {
            drawRect(color = tint.copy(alpha = surfaceAlpha))
        }
    )
}
