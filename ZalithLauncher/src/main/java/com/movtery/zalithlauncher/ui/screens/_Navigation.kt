/*
 * PCL Pro Android - Enhanced Navigation Animations
 */

package com.movtery.zalithlauncher.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.scene.Scene
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.animation.EmphasizedAccelerateEasing
import com.movtery.zalithlauncher.utils.animation.EmphasizedDecelerateEasing
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.utils.animation.getAnimateSpeed
import kotlin.reflect.KClass

/**
 * 兼容嵌套NavDisplay的返回事件处理
 */
fun <E: TitledNavKey> onBack(currentBackStack: NavBackStack<E>) {
    when (val key = currentBackStack.lastOrNull()) {
        is NormalNavKey -> currentBackStack.removeLastOrNull()
        is BackStackNavKey<*> -> {
            if (key.backStack.size <= 1) {
                currentBackStack.removeLastOrNull()
            } else {
                key.backStack.removeLastOrNull()
            }
        }
    }
}

fun <E: TitledNavKey> NavBackStack<E>.navigateOnce(key: E) {
    if (key == lastOrNull()) return
    clearWith(key)
}

fun <E: TitledNavKey> NavBackStack<E>.navigateTo(screenKey: E, useClassEquality: Boolean = false) {
    val current = lastOrNull()
    if (useClassEquality) {
        if (current != null && screenKey::class == current::class) return
    } else {
        if (screenKey == current) return
    }
    add(screenKey)
}

fun <E: TitledNavKey> NavBackStack<E>.removeAndNavigateTo(remove: KClass<*>, screenKey: E, useClassEquality: Boolean = false) {
    removeIf { key -> key::class == remove }
    navigateTo(screenKey, useClassEquality)
}

fun <E: TitledNavKey> NavBackStack<E>.removeAndNavigateTo(removes: List<KClass<*>>, screenKey: E, useClassEquality: Boolean = false) {
    removeIf { key -> key::class in removes }
    navigateTo(screenKey, useClassEquality)
}

fun <E: TitledNavKey> NavBackStack<E>.clearWith(navKey: E) {
    val targetClass = navKey::class.java
    if (none { it::class.java == targetClass }) {
        add(navKey)
    }
    removeIf { it::class.java != targetClass }
}

fun <E: TitledNavKey> NavBackStack<E>.clearKeys(vararg navKeys: E) {
    val classes = navKeys.map { it::class.java }
    removeIf { it::class.java in classes }
}

fun <E: TitledNavKey> NavBackStack<E>.addIfEmpty(navKey: E) {
    if (isEmpty()) {
        add(navKey)
    }
}

@Composable
fun rememberSwapTween(): FiniteAnimationSpec<Float> {
    val speed = AllSettings.launcherAnimateSpeed.state
    return remember(speed) {
        tween(
            durationMillis = (getAnimateSpeed() / 5) * 2,
            easing = EmphasizedDecelerateEasing
        )
    }
}

/**
 * 页面切换动画 - 升级为滑动+缩放+淡入淡出组合
 * 进入：从右侧滑入 + 轻微放大 + 淡入
 * 退出：向左侧滑出 + 轻微缩小 + 淡出
 */
@Composable
fun <T : Any> rememberTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val type = AllSettings.launcherSwapAnimateType.state
    val speed = AllSettings.launcherAnimateSpeed.state
    return remember(type, speed) {
        val duration = (getAnimateSpeed() / 5) * 2

        when (type) {
            //PCL Pro Android: iOS Dock 换页
            //新页面从右侧带轻微缩放弹性推入并压住旧页面，旧页面同步向左做小视差位移，
            //位移与缩放全部使用低阻尼 spring，收尾带一点过冲，形成液态玻璃的软弹手感
            TransitionAnimationType.DOCK, TransitionAnimationType.JELLY_BOUNCE -> {
                //整体手感跟随“动画速度”设置：速度越快，弹簧刚度越高
                val stiffness = (520f * (getAnimateSpeed().coerceAtLeast(1) / 300f)).coerceIn(360f, 900f)
                val slideSpec: FiniteAnimationSpec<IntOffset> = spring(
                    dampingRatio = 0.86f,
                    stiffness = stiffness,
                    visibilityThreshold = IntOffset(1, 1)
                )
                val scaleSpec: FiniteAnimationSpec<Float> = spring(
                    dampingRatio = 0.74f,
                    stiffness = stiffness * 0.92f,
                    visibilityThreshold = 0.001f
                )
                val alphaInSpec: FiniteAnimationSpec<Float> = tween(
                    durationMillis = (duration * 0.6f).toInt().coerceAtLeast(90),
                    easing = EmphasizedDecelerateEasing
                )
                val alphaOutSpec: FiniteAnimationSpec<Float> = tween(
                    durationMillis = (duration * 0.5f).toInt().coerceAtLeast(80),
                    easing = EmphasizedAccelerateEasing
                )
                val transform: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
                    ContentTransform(
                        //PCL Android FE: 去掉切页时的缩放。
                        //scale 每帧都在改变图层尺寸，会让每块液态玻璃的 RenderEffect 每帧重新分配、
                        //重新采样背景层，是切页动画掉帧的主要来源；位移+淡入淡出的观感差异很小但开销低一个量级。
                        targetContentEnter = slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { it / 4 }
                        ) + fadeIn(
                            animationSpec = alphaInSpec
                        ),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = slideSpec,
                            targetOffsetX = { -it / 10 }
                        ) + fadeOut(
                            animationSpec = alphaOutSpec
                        ),
                        targetContentZIndex = 1f
                    )
                }
                transform
            }
            TransitionAnimationType.CLOSE -> {
                {
                    ContentTransform(
                        fadeIn(animationSpec = snap()),
                        fadeOut(animationSpec = snap()),
                    )
                }
            }
            TransitionAnimationType.BOUNCE -> {
                {
                    ContentTransform(
                        targetContentEnter = fadeIn(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedDecelerateEasing)
                        ) + scaleIn(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedDecelerateEasing),
                            initialScale = 0.95f
                        ),
                        initialContentExit = fadeOut(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedAccelerateEasing)
                        ) + scaleOut(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedAccelerateEasing),
                            targetScale = 1.05f
                        ),
                    )
                }
            }
            else -> {
                // SLICE_IN / 默认：标准滑动过渡
                {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedDecelerateEasing),
                            initialOffsetX = { it / 4 }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedDecelerateEasing)
                        ),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedAccelerateEasing),
                            targetOffsetX = { -it / 5 }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = duration, easing = EmphasizedAccelerateEasing)
                        ),
                    )
                }
            }
        }
    }
}
