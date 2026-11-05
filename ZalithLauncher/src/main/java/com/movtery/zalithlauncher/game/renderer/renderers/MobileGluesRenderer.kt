/*
 * Zalith Launcher 2 - Modified Branch
 * MobileGlues Renderer built-in
 */

package com.movtery.zalithlauncher.game.renderer.renderers

import com.movtery.zalithlauncher.game.renderer.RendererInterface

/**
 * MobileGlues 渲染器（内置版）
 * 原插件包名: com.fcl.plugin.mobileglues
 * 配置来源: MobileGlues-plugin V2.0.0 build.gradle.kts manifestPlaceholders
 */
object MobileGluesRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3"

    override fun getUniqueIdentifier(): String = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

    override fun getRendererName(): String = "MobileGlues"

    override fun getRendererSummary(): String = "MobileGlues (OpenGL 4.0, 1.17+)"

    override fun getMinMCVersion(): String = "1.17"

    override fun getMaxMCVersion(): String? = null

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "LIBGL_ES" to "3",
            "POJAVEXEC_EGL" to "libmobileglues.so",
            "LIBGL_EGL" to "libmobileglues.so"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libmobileglues.so"

    override fun getRendererEGL(): String = "libmobileglues.so"
}
