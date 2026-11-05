/*
 * PCL Pro Android - 液态流光 Shimmer 加载效果
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 液态流光 Shimmer - 横向流动的光泽扫过效果
 * 比传统透明度闪烁更有质感
 */
@Composable
fun Modifier.liquidShimmer(
    baseColor: Color = Color.Gray.copy(alpha = 0.3f),
    highlightColor: Color = Color.White.copy(alpha = 0.6f),
    shape: Shape = RectangleShape
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    baseColor,
                    highlightColor,
                    baseColor,
                    baseColor
                ),
                start = Offset(translateX - 150f, 0f),
                end = Offset(translateX + 150f, 0f)
            )
        )
}

/**
 * 保留旧版 API - 透明度呼吸效果
 */
@Composable
fun Modifier.infiniteShimmer(
    initialValue: Float = 0.3f,
    targetValue: Float = 0.6f
): Modifier {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    return this.then(Modifier.alpha(animatedAlpha))
}

/**
 * 液态流光骨架屏 Box
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    shape: Shape = RectangleShape
) {
    Box(
        modifier = modifier
            .liquidShimmer(
                baseColor = color.copy(alpha = 0.3f),
                highlightColor = Color.White.copy(alpha = 0.5f),
                shape = shape
            )
    )
}
