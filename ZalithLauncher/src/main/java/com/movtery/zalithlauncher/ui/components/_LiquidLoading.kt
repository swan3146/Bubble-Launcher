/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or
 * the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * PCL Pro Android 跳动的液体泡泡加载动画
 *
 * 三枚带高光的小圆泡沿波浪相位依次上下跳动，落地时轻微压扁、跃起时拉长，
 * 像水面上冒泡的液态质感。纯 Canvas 绘制，不做 backdrop 模糊，开销极低。
 */
@Composable
fun LiquidLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") strokeWidth: Dp = 2.dp,
) {
    val transition = rememberInfiniteTransition(label = "BubbleLoading")
    //整体相位 0..1 循环
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                0f at 0
                1f at 1100
            }
        ),
        label = "BubblePhase"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val bubbleCount = 3
        val spacing = w / (bubbleCount + 1)
        val baseY = h * 0.62f
        val jumpHeight = h * 0.30f

        for (i in 0 until bubbleCount) {
            //每枚泡泡错开 1/3 相位，形成依次跳动
            var p = (phase - i * 0.28f) % 1f
            if (p < 0f) p += 1f
            //上抛阶段(0..0.5)上升，下落(0.5..1)
            val up = if (p < 0.5f) p * 2f else (1f - p) * 2f
            //ease 让起跳快、到顶慢
            val eased = FastOutSlowInEasing.transform(up)
            val cx = spacing * (i + 1)
            val cy = baseY - eased * jumpHeight
            //离地越高越扁长、落地越圆
            val stretch = 0.82f + eased * 0.35f
            val r = w * 0.09f
            val rx = r * (2f - stretch)
            val ry = r * stretch

            //液体泡泡：径向渐变，左上高光、右下主色
            val center = Offset(cx, cy)
            val brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    color.copy(alpha = 0.95f)
                ),
                center = Offset(cx - rx * 0.35f, cy - ry * 0.35f),
                radius = rx * 1.6f
            )
            drawOval(
                brush = brush,
                topLeft = Offset(center.x - rx, center.y - ry),
                size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2)
            )
        }
    }
}
