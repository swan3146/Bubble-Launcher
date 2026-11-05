/*
 * PCL Pro Android - Q弹按下效果组件
 */

package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.movtery.zalithlauncher.utils.animation.EmphasizedDecelerateEasing
import com.movtery.zalithlauncher.utils.animation.LiquidBounceEasing

/**
 * Q弹按下效果 - 按下时缩小到 0.95，松开后弹性回弹到 1.0
 */
@Composable
fun Modifier.bouncyPress(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.95f
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(
                targetValue = pressedScale,
                animationSpec = tween(durationMillis = 80, easing = EmphasizedDecelerateEasing)
            )
        } else {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LiquidBounceEasing)
            )
        }
    }

    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * 卡片入场弹性动画 - 从下方滑入 + 淡入 + 轻微放大
 */
@Composable
fun Modifier.enterBounce(delayMillis: Int = 0): Modifier {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(40f) }
    val scale = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, delayMillis = delayMillis, easing = EmphasizedDecelerateEasing)
        )
        translationY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, delayMillis = delayMillis, easing = LiquidBounceEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, delayMillis = delayMillis, easing = LiquidBounceEasing)
        )
    }

    return this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
        scaleX = scale.value
        scaleY = scale.value
    }
}
