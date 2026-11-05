/*
 * PCL Pro Android - Enhanced Animation Easing Curves
 */

package com.movtery.zalithlauncher.utils.animation

import androidx.compose.animation.core.Easing
import kotlin.math.cos
import kotlin.math.exp

/**
 * Q弹果冻缓动 - 阻尼余弦模型，轻微过冲回弹
 */
val JellyBounce = Easing { t ->
    (1 - 0.6f * exp(-8 * t) * cos(6 * Math.PI * t)).toFloat()
}

/**
 * 弹跳缓动 - 标准 BounceInterpolator
 */
val BounceEasing: Easing = Easing { t ->
    fun bounce(x: Float): Float = x * x * 8.0f
    var input = t * 1.1226f
    when {
        input < 0.3535f -> bounce(input)
        input < 0.7408f -> bounce(input - 0.54719f) + 0.7f
        input < 0.9644f -> bounce(input - 0.8526f) + 0.9f
        else -> bounce(input - 1.0435f) + 0.95f
    }
}

/**
 * 液态玻璃缓动 - 柔和的水滴弹起感，比 JellyBounce 更轻柔
 * 适合卡片、按钮的入场动画
 */
val LiquidBounceEasing = Easing { t ->
    (1 - 0.35f * exp(-6 * t) * cos(4 * Math.PI * t)).toFloat()
}

/**
 * Material 3 强调减速缓动 - 快速进入，平滑停靠
 * 适合新页面进入、元素入场
 */
val EmphasizedDecelerateEasing = Easing { t ->
    val u = 1f - t
    1f - u * u * u
}

/**
 * Material 3 强调加速缓动 - 缓慢起步，快速离开
 * 适合页面退出、元素消失
 */
val EmphasizedAccelerateEasing = Easing { t ->
    t * t * t
}

/**
 * 丝滑 S 曲线缓动 - 平滑对称，适合通用过渡
 */
val SmoothEasing = Easing { t ->
    t * t * (3f - 2f * t)
}
