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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 共享指示器控制器。
 *
 * PCL Android FE: 之前每个 RailItem 各自画一块选中胶囊，切换页面时只是"旧胶囊淡出、新胶囊淡入"，
 * 视觉上胶囊永远待在原地不动。这里改为容器统一持有一块胶囊，按选中项的位置做位移动画，
 * 于是切换页面时胶囊会真正沿着导航栏上下/左右滑动。
 */
@Stable
class RailIndicatorController {
    private var slotCounter = 0
    private val bounds = mutableStateMapOf<Int, Rect>()
    private var selectedSlot by mutableStateOf(-1)

    internal fun nextSlot(): Int = slotCounter++

    internal fun report(slot: Int, rect: Rect, selected: Boolean) {
        if (rect.width > 0f && rect.height > 0f && bounds[slot] != rect) {
            bounds[slot] = rect
        }
        if (selected && selectedSlot != slot) {
            selectedSlot = slot
        }
    }

    internal fun selectedBounds(): Rect? = bounds[selectedSlot]

    internal fun hasSelection(): Boolean = selectedSlot >= 0 && bounds[selectedSlot] != null

    /**
     * 手指横坐标命中的导航项。
     *
     * 先精确判断落在哪个子项的横向范围内；若手指落在两个子项之间的间隙（或两端之外），
     * 则退化为"取横向中心最近的一项"，保证轻扫也能换页。
     */
    internal fun slotAt(x: Float): Int? {
        val entries = bounds.entries.sortedBy { it.key }
        if (entries.isEmpty()) return null
        entries.firstOrNull { x >= it.value.left && x <= it.value.right }?.let { return it.key }
        return entries.minByOrNull { abs(it.value.center.x - x) }?.key
    }
}

val LocalRailIndicatorController = compositionLocalOf<RailIndicatorController?> { null }

/**
 * 带"滑动玻璃胶囊"的导航栏容器。
 *
 * 直接把一排 RailItem 放进来即可，子项会自动上报自身位置，
 * 容器负责绘制并在选中项之间滑动那块玻璃胶囊。
 *
 * @param visible 是否显示胶囊（例如当前页面不属于任何导航项时传 false）
 */
@Composable
fun MovingGlassRail(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    color: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(percent = 50),
    /**
     * PCL Pro Android: 支持横向滑动换页。
     * 传入回调后，按住玻璃胶囊横向拖动即可切换导航项，手指落下时命中的那一项会作为参数回传。
     * 传 null 时退化为只能点击换页（保持原行为）。
     */
    onSwipeNavigate: ((Int) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val controller = remember { RailIndicatorController() }

    //拖拽状态：胶囊跟手位移、按下放大倍数
    //这里只写入、不在本组合作用域读取，位移由胶囊自己在 draw 阶段消费，
    //避免拖动时每一帧都让整个导航栏重组，滑动才能保持顺滑
    val dragging = remember { mutableStateOf(false) }
    val dragOffsetX = remember { mutableFloatStateOf(0f) }
    //松手瞬间记录的最后拖拽位移：供胶囊把它作为弹簧动画的"起始偏移"，
    //这样胶囊不会在松手时瞬间跳回旧位置、再从旧位置弹到新位置（此前切换不自然的主因之一）
    val releaseOffsetX = remember { mutableFloatStateOf(0f) }
    val pressScale = remember { Animatable(1f) }

    //PCL Pro Android: 手指开始滑动 -> 胶囊慢慢鼓起；松手 -> 弹性回落到正常厚度
    LaunchedEffect(dragging.value) {
        if (dragging.value) {
            pressScale.animateTo(1.12f, animationSpec = tween(durationMillis = 180))
        } else {
            //PCL Pro Android: 松手时低阻尼回弹，胶囊晃一下再停，强化液体惯性
            pressScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 480f))
        }
    }

    Box(
        modifier = modifier.then(
            if (onSwipeNavigate != null) {
                Modifier.pointerInput(onSwipeNavigate) {
                    var startX = 0f
                    var currentX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            currentX = offset.x
                            dragOffsetX.floatValue = 0f
                            releaseOffsetX.floatValue = 0f
                            dragging.value = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            currentX += dragAmount
                            dragOffsetX.floatValue = currentX - startX
                        },
                        onDragEnd = {
                            val targetSlot = controller.slotAt(currentX)
                            //先记录松手位置，再标记拖拽结束：胶囊据此从手指位置平滑弹回
                            releaseOffsetX.floatValue = dragOffsetX.floatValue
                            dragging.value = false
                            dragOffsetX.floatValue = 0f
                            if (targetSlot != null) {
                                onSwipeNavigate(targetSlot)
                            }
                        },
                        onDragCancel = {
                            releaseOffsetX.floatValue = dragOffsetX.floatValue
                            dragging.value = false
                            dragOffsetX.floatValue = 0f
                        }
                    )
                }
            } else Modifier
        )
    ) {
        CompositionLocalProvider(LocalRailIndicatorController provides controller) {
            content()
        }

        val target = controller.selectedBounds()
        //只有真正存在选中项（且允许显示）时才绘制胶囊
        if (visible && target != null) {
            SlideIndicator(
                controller = controller,
                target = target,
                color = color,
                shape = shape,
                dragOffsetX = dragOffsetX,
                releaseOffsetX = releaseOffsetX,
                dragging = dragging.value,
                pressScale = pressScale
            )
        }
    }
}

@Composable
private fun SlideIndicator(
    controller: RailIndicatorController,
    target: Rect,
    color: Color,
    shape: Shape,
    dragOffsetX: androidx.compose.runtime.State<Float>,
    releaseOffsetX: androidx.compose.runtime.MutableFloatState,
    dragging: Boolean,
    pressScale: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>
) {
    val density = LocalDensity.current
    val animated = remember { Animatable(target, Rect.VectorConverter) }
    var initialized by remember { mutableStateOf(false) }
    //液体形变上限：横向最多拉长到 22dp，纵向同步压薄，看起来像一滴被拉扯的液体
    val maxStretch = with(density) { 22.dp.toPx() }

    LaunchedEffect(target, dragging, releaseOffsetX.value) {
        //跟手阶段完全交给手指，不做位移动画，避免与拖拽互相拉扯
        if (dragging) return@LaunchedEffect
        if (!initialized) {
            //首次出现直接落位，避免从左上角飞过来
            animated.snapTo(target)
            initialized = true
        } else if (releaseOffsetX.value != 0f) {
            //PCL Pro Android: 松手时胶囊从"手指松开的位置"（target 叠加最后拖拽偏移）开始弹，
            //而不是瞬间跳回旧位置再弹；随后低阻尼弹簧弹到 target，带一点过冲，
            //做出"液体甩出去再晃回来"的惯性效果。
            val startRect = target.copy(
                left = target.left + releaseOffsetX.value,
                right = target.right + releaseOffsetX.value
            )
            animated.snapTo(startRect)
            //消费掉这次松手偏移，避免它残留影响下一次纯点击切换
            releaseOffsetX.floatValue = 0f
            animated.animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
            )
        } else {
            //普通切换（点击/侧边栏切换）：从当前位置弹簧滑到新 target，带一点过冲
            animated.animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
            )
        }
    }

    val rect = animated.value
    if (rect.width <= 0f || rect.height <= 0f) return

    val widthDp = with(density) { rect.width.toDp() }
    val heightDp = with(density) { rect.height.toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(width = widthDp, height = heightDp)
            //放在内容下层，避免高光压住图标与文字
            .zIndex(-1f)
            .graphicsLayer {
                val dragX = dragOffsetX.value
                //液体形变驱动：拖动时用手指位移（跟手拉扯），切换时用目标相对当前中心的
                //"追逐距离"（朝目标甩出去）。两者互斥，不会像之前那样把静止的 chase
                //与 dragX 叠加导致拖动时形变恒被钳死在最大值、毫无跟手松紧。
                val chase = target.center.x - rect.center.x
                //拖动阶段形变完全由手指决定；松手后的切换动画则由 chase 决定
                val drive = if (dragging) dragX else chase
                val driveAbs = abs(drive)
                //形变量随位移增大而增大，但到最大拉伸后趋于饱和（像液体被拉长到极限）
                val stretch = (driveAbs / (maxStretch * 0.9f)).coerceIn(0f, 1f)
                val eased = 1f - (1f - stretch) * (1f - stretch) // easeOut，轻拽就明显、重拽饱和
                val stretchRatio = eased * 0.30f
                translationX = dragX
                scaleX = pressScale.value * (1f + stretchRatio)
                scaleY = pressScale.value * (1f - stretchRatio * 0.42f)
                //拉伸原点：朝哪个方向移动，就从对侧作为原点拉长，形成朝移动方向甩出去的水滴感。
                //拖动与切换共用同一套方向判断，且用移动方向（而非 chase 符号）决定，避免弹簧
                //过冲时 chase 变号导致 transformOrigin 在 0↔1 之间突变、拉伸方向抽搐。
                val fx = if (drive >= 0f) 0f else 1f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(fx, 0.5f)
            }
            //PCL Pro Android: 选中胶囊恢复液态玻璃材质（backdrop 模糊 + 着色）
            .backgroundGlass(
                blur = AllSettings.backgroundBlur.state,
                color = color,
                shape = shape
            )
    )
}

/**
 * 把当前 RailItem 的位置上报给外层的 [MovingGlassRail]。
 * 没有外层容器时（单独使用 RailItem）返回 null，此时 RailItem 保持自己的选中背景。
 */
@Composable
internal fun Modifier.reportRailSlot(selected: Boolean): Modifier {
    val controller = LocalRailIndicatorController.current ?: return this
    val slot = remember(controller) { controller.nextSlot() }
    var rect by remember { mutableStateOf(Rect.Zero) }

    SideEffect {
        controller.report(slot, rect, selected)
    }

    return this.onGloballyPositioned { coordinates ->
        val current = coordinates.boundsInParent()
        if (current != rect) {
            rect = current
            controller.report(slot, current, selected)
        }
    }
}

/** 是否处于共享指示器容器内（容器内时子项不再自绘选中胶囊） */
@Composable
internal fun inSharedRail(): Boolean = LocalRailIndicatorController.current != null
