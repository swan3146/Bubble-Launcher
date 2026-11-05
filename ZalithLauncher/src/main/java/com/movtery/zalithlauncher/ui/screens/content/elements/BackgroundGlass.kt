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

import android.graphics.Matrix
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.core.graphics.withSave
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

/**
 * 背景毛玻璃效果：在元素内容下方对齐绘制启动器背景的预模糊结果
 * @param enabled 是否应用模糊效果
 */
@Composable
fun Modifier.backgroundGlass(
    blur: Int,
    color: Color,
    enabled: Boolean = true,
    cornerRadius: Dp = 0.dp,
    shape: Shape? = null,
): Modifier {
    if (!enabled) return this
    //纯色材质不绘制任何玻璃层，保持原本的纯色卡片
    val style = AllSettings.launcherMaterialStyle.state
    if (style == MaterialStyle.Solid) return this

    //PCL Pro Android: 液态玻璃与示例项目（AndroidLiquidGlass-kmp / Backdrop）保持一致，
    //改由 backdrop 实时采样：真实 RenderEffect 模糊 + 圆角 SDF 透镜折射 + 定向高光
    val liquidBackdrop = LocalLauncherBackdrop.current
    if (style == MaterialStyle.LiquidGlass && liquidBackdrop != null) {
        return this.liquidGlass(
            backdrop = liquidBackdrop,
            tint = color,
            cornerRadius = cornerRadius,
            shape = shape,
            blur = blur
        )
    }

    //仅在前景模糊模式下才采样背景；背景模糊模式下只绘制材质光泽
    val backgroundMode = AllSettings.backgroundBlurType.state == BackgroundBlur.Background
    val background = if (backgroundMode) null else {
        LocalBackgroundViewModel.current?.takeIf { it.isValid }
    }
    //只要背景图可见且模糊开启就应当采样（原先的 <100 判断把完全不透明背景图也排除了）
    val canSample = background != null
            && blur > 0
            && AllSettings.launcherBackgroundOpacity.state > 0
    val isDark = isLauncherInDarkTheme()
    val cornerPx = with(LocalDensity.current) { cornerRadius.toPx() }
    return this then GlassElement(blur, color, background, style, canSample, isDark, cornerPx)
}

/**
 * 启动器背景捕获：由 Background 挂载，发布背景内容与屏幕区域供毛玻璃效果采样
 * @param recordContent 是否将内容录制到共享层（前景模糊模式）
 * @param blurRadiusPx 前景模式 GPU 预模糊半径（像素，仅 Android 12+ 生效）
 * @param whiteOverlayAlpha 白色提亮层透明度（0 表示不绘制）
 */
internal fun Modifier.backgroundCapture(
    store: BackgroundViewModel,
    recordContent: Boolean,
    blurRadiusPx: Float,
    whiteOverlayAlpha: Float,
): Modifier = this then BackgroundCaptureElement(store, recordContent, blurRadiusPx, whiteOverlayAlpha)

internal fun whiteOverlayAlpha(blur: Int): Float {
    val t = (blur / 80f).coerceIn(0f, 1f)
    return lerp(start = 0f, stop = 0.25f, fraction = sqrt(t))
}

private data class GlassElement(
    val blur: Int,
    val color: Color,
    val store: BackgroundViewModel?,
    val style: MaterialStyle,
    val canSample: Boolean,
    val isDark: Boolean,
    val cornerRadiusPx: Float,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(blur, color, store, style, canSample, isDark, cornerRadiusPx)

    override fun update(node: GlassNode) {
        node.blur = blur
        node.color = color
        node.store = store
        node.style = style
        node.canSample = canSample
        node.isDark = isDark
        node.cornerRadiusPx = cornerRadiusPx
    }
}

/**
 * 玻璃绘制节点：绘制期间读取的状态变化只触发重绘，不触发重组
 * PCL Pro Android: 即使没有设置背景图，也绘制材质光泽，保证材质设置始终可见
 */
private class GlassNode(
    blur: Int,
    color: Color,
    store: BackgroundViewModel?,
    style: MaterialStyle,
    canSample: Boolean,
    isDark: Boolean,
    cornerRadiusPx: Float,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    var blur: Int = blur
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    var color: Color = color
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    var store: BackgroundViewModel? = store
        set(value) {
            if (field === value) return
            field = value
            invalidateDraw()
        }

    var style: MaterialStyle = style
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    var canSample: Boolean = canSample
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    var isDark: Boolean = isDark
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    var cornerRadiusPx: Float = cornerRadiusPx
        set(value) {
            if (field == value) return
            field = value
            invalidateDraw()
        }

    //元素在屏幕坐标系中的位置，随布局逐帧更新，绘制时据此对齐采样
    private var screenOrigin = Offset.Zero
    private var hasOrigin = false

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionOnScreen()
        if (!position.isSpecified) return
        if (!hasOrigin || position != screenOrigin) {
            screenOrigin = position
            hasOrigin = true
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawGlass()
        drawContent()
    }

    private fun DrawScope.drawGlass() {
        // 1. 有背景图时对齐绘制预模糊背景，液态玻璃额外绘制边缘折射
        if (canSample) {
            val model = store
            val bounds = model?.backgroundBounds
            if (model != null && bounds != null && !bounds.isEmpty && hasOrigin) {
                val width = size.width
                val height = size.height
                if (width > 0f && height > 0f) {
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    nativeCanvas.withSave {
                        val current = Matrix()
                        getMatrix(current)
                        val inverted = Matrix()
                        if (current.invert(inverted)) {
                            val values = FloatArray(9).also(current::getValues)
                            val left = bounds.left - screenOrigin.x + values[Matrix.MTRANS_X]
                            val top = bounds.top - screenOrigin.y + values[Matrix.MTRANS_Y]

                            concat(inverted)
                            drawSampledBackground(model, left, top, bounds.width, bounds.height)

                            //液态玻璃：卡片边缘一圈放大折射，模拟玻璃厚度带来的透镜位移
                            if (style == MaterialStyle.LiquidGlass) {
                                drawLiquidEdges(model, left, top, width, height, bounds.width, bounds.height)
                            }
                        }
                    }
                }
            }
        }

        // 2. 卡片自身底色
        // 玻璃材质下必须压住底色的不透明度，否则采样背景与光泽会被自身底色整块盖死（此前"没效果"的根因）
        val tintAlpha = when (style) {
            MaterialStyle.LiquidGlass -> (color.alpha * 0.22f).coerceIn(0.04f, 0.16f)
            MaterialStyle.Solid -> color.alpha
        }
        drawRect(color = color.copy(alpha = tintAlpha), blendMode = BlendMode.SrcOver)
        drawRect(
            color = Color.White.copy(alpha = whiteOverlayAlpha(blur) * 0.6f),
            blendMode = BlendMode.Softlight
        )

        // 3. 材质光泽层
        drawMaterialSheen()
    }

    /**
     * 材质光泽：不依赖背景采样，液态玻璃呈现高光与亮边，毛玻璃呈现均匀雾面
     */
    private fun DrawScope.drawMaterialSheen() {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return

        //圆角优先跟随卡片自身 shape，避免玻璃描边与外层裁剪圆角对不齐
        val radius = if (cornerRadiusPx > 0f) {
            minOf(height / 2f, cornerRadiusPx)
        } else {
            minOf(height / 2f, 12.dp.toPx())
        }
        val corner = CornerRadius(radius)

        when (style) {
            MaterialStyle.LiquidGlass -> {
                //玻璃厚度：顶部明亮高光 + 底部轻微内阴影，浅色主题下也靠明暗差形成体积感
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (isDark) 0.30f else 0.55f),
                        0.20f to Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (isDark) 0.28f else 0.09f)
                    ),
                    size = size,
                    cornerRadius = corner
                )
                //斜向反光：一条斜切高光带，模拟液态玻璃的流动反射
                drawRoundRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.30f to Color.White.copy(alpha = if (isDark) 0.14f else 0.26f),
                        0.44f to Color.White.copy(alpha = if (isDark) 0.04f else 0.10f),
                        0.62f to Color.Transparent,
                        1f to Color.Transparent,
                        start = Offset(0f, height),
                        end = Offset(width, 0f)
                    ),
                    size = size,
                    cornerRadius = corner
                )

                //PCL Pro Android: 不再绘制色散亮边与内侧暗线。
                //这两层描边会和 backdrop 液态玻璃自带的 Highlight 高光边叠加，
                //表现为卡片内部多出一圈粗细不一的边框（此前的视觉 bug）。
                //玻璃厚度只依靠上方体积渐变与斜向反光表现，边缘一律交给 Highlight。
            }

            MaterialStyle.Solid -> Unit
        }
    }

    /**
     * 将预模糊背景按其屏幕位置对齐绘制到当前元素上
     */
    private fun DrawScope.drawSampledBackground(
        model: BackgroundViewModel,
        left: Float,
        top: Float,
        srcWidth: Float,
        srcHeight: Float,
    ) {
        if (Build.VERSION.SDK_INT >= 31) {
            model.glassLayer?.let { layer ->
                translate(left, top) {
                    drawLayer(layer)
                }
            }
        } else {
            model.blurredBackground?.let { bitmap ->
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(srcWidth.roundToInt(), srcHeight.roundToInt())
                )
            }
        }
    }

    /**
     * 边缘折射带：以卡片中心为基准放大采样层，只保留四周的窄带，
     * 让边缘看起来像被玻璃折射压缩过的背景
     */
    private fun DrawScope.drawLiquidEdges(
        model: BackgroundViewModel,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        srcWidth: Float,
        srcHeight: Float,
    ) {
        //PCL Pro Android: 边缘折射带原按短边的 18% 计算，像设置项行、图标卡这类小尺寸卡片
        //几乎整块会被放大重绘，看起来就是“卡片内部多出一圈错位的边框/重影”，这里做了三点收敛：
        //1) 过小的卡片直接不做边缘折射；2) 带宽改为固定像素区间，不再随卡片一起膨胀；
        //3) 裁剪跟随卡片圆角，避免圆角外侧被直角带切出一段多余边线。
        val shortSide = minOf(width, height)
        if (shortSide < 56.dp.toPx()) return

        val band = (shortSide * 0.08f).coerceIn(3.dp.toPx(), 12.dp.toPx())
        val bandScale = 1.045f
        val centerX = width / 2f
        val centerY = height / 2f

        val regions = arrayOf(
            floatArrayOf(0f, 0f, band, height),
            floatArrayOf(width - band, 0f, width, height),
            floatArrayOf(0f, 0f, width, band),
            floatArrayOf(0f, height - band, width, height)
        )

        //与光泽层一致的圆角，保证折射带始终被裁在卡片形状内
        val radiusPx = if (cornerRadiusPx > 0f) minOf(height / 2f, cornerRadiusPx) else minOf(height / 2f, 12.dp.toPx())
        val rounded = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, width, height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            )
        }

        val canvas = drawContext.canvas.nativeCanvas
        regions.forEach { region ->
            canvas.withSave {
                clipPath(rounded.asAndroidPath())
                clipRect(region[0], region[1], region[2], region[3])
                translate(centerX, centerY)
                scale(bandScale, bandScale)
                translate(-centerX, -centerY)
                drawSampledBackground(model, left, top, srcWidth, srcHeight)
            }
        }
    }
}

private data class BackgroundCaptureElement(
    val store: BackgroundViewModel,
    val recordContent: Boolean,
    val blurRadiusPx: Float,
    val whiteOverlayAlpha: Float,
) : ModifierNodeElement<BackgroundCaptureNode>() {
    override fun create() = BackgroundCaptureNode(store, recordContent, blurRadiusPx, whiteOverlayAlpha)

    override fun update(node: BackgroundCaptureNode) {
        node.store = store
        node.recordContent = recordContent
        node.blurRadiusPx = blurRadiusPx
        node.whiteOverlayAlpha = whiteOverlayAlpha
        node.sync()
        node.invalidateDraw()
    }
}

/**
 * 背景捕获节点：持有共享模糊层并随节点生命周期发布/摘除，
 * 布局时上报背景屏幕区域，Android 12- 前景模式下驱动帧捕获循环
 */
private class BackgroundCaptureNode(
    var store: BackgroundViewModel,
    var recordContent: Boolean,
    var blurRadiusPx: Float,
    var whiteOverlayAlpha: Float,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var graphicsContext: GraphicsContext? = null
    private var sourceLayer: GraphicsLayer? = null
    private var blurredLayer: GraphicsLayer? = null
    private var blurredEffectRadius = 0f
    private var captureJob: Job? = null

    override fun onAttach() {
        sync()
    }

    override fun onDetach() {
        releaseLayers()
        graphicsContext = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        store.updateBackgroundBounds(coordinates)
    }

    override fun ContentDrawScope.draw() {
        val source = sourceLayer
        if (recordContent && source != null) {
            source.record { this@draw.drawContent() }
            drawLayer(source)
            if (Build.VERSION.SDK_INT >= 31) {
                ensureBlurredLayer()?.record { drawLayer(source) }
            }
        } else {
            drawContent()
        }
        if (whiteOverlayAlpha > 0f) {
            drawRect(
                color = Color.White.copy(alpha = whiteOverlayAlpha),
                blendMode = BlendMode.Softlight
            )
        }
    }

    /**
     * 依据当前参数调和共享层的发布与捕获循环的启停
     */
    fun sync() {
        syncLayers()
        syncCaptureLoop()
    }

    private fun syncLayers() {
        if (recordContent) {
            val source = sourceLayer ?: createLayer()?.also { sourceLayer = it }
            if (Build.VERSION.SDK_INT >= 31) {
                store.attachGlassLayer(ensureBlurredLayer())
                store.attachCaptureLayer(null)
            } else {
                store.attachCaptureLayer(source)
                store.attachGlassLayer(null)
            }
        } else {
            releaseLayers()
        }
    }

    private fun ensureBlurredLayer(): GraphicsLayer? {
        blurredLayer?.let { layer ->
            if (blurredEffectRadius != blurRadiusPx) {
                layer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
                blurredEffectRadius = blurRadiusPx
            }
            return layer
        }
        val layer = createLayer() ?: return null
        layer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
        blurredEffectRadius = blurRadiusPx
        blurredLayer = layer
        return layer
    }

    private fun createLayer(): GraphicsLayer? {
        //release 必须归还给创建它的同一 context，故在创建时捕获
        val context = currentValueOf(LocalGraphicsContext)
        graphicsContext = context
        return context.createGraphicsLayer()
    }

    private fun syncCaptureLoop() {
        if (recordContent && Build.VERSION.SDK_INT < 31) {
            if (captureJob?.isActive == true) return
            captureJob = coroutineScope.launch {
                snapshotFlow {
                    store.isValid && store.captureLayer != null &&
                        AllSettings.backgroundBlur.state > 0 &&
                        AllSettings.launcherBackgroundOpacity.state < 100
                }.collectLatest { active ->
                    if (!active) return@collectLatest
                    while (currentCoroutineContext().isActive) {
                        val started = System.nanoTime()
                        store.captureGlassFrame()
                        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
                        // 旧安卓(<12)无 GPU RenderEffect，背景靠每帧重绘到 Bitmap，
                        // 30fps 足够毛玻璃观感，可显著降低中低端设备的卡顿与发热
                        delay((33L - elapsedMs).coerceAtLeast(0L).milliseconds)
                    }
                }
            }
        } else {
            captureJob?.cancel()
            captureJob = null
        }
    }

    private fun releaseLayers() {
        store.attachGlassLayer(null)
        store.attachCaptureLayer(null)
        val context = graphicsContext
        sourceLayer?.let { context?.releaseGraphicsLayer(it) }
        sourceLayer = null
        blurredLayer?.let { context?.releaseGraphicsLayer(it) }
        blurredLayer = null
    }
}
