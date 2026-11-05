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

package com.movtery.zalithlauncher.setting.enums

/**
 * 界面材质样式
 * PCL Pro Android: 全局玻璃材质风格（仅保留液态玻璃与纯色两档）
 */
enum class MaterialStyle {
    /**
     * 液态玻璃：边缘折射 + 定向高光 + 背景实时采样
     */
    LiquidGlass,

    /**
     * 纯色：不采样背景，仅使用卡片纯色
     */
    Solid;

    fun next(): MaterialStyle {
        return when (this) {
            LiquidGlass -> Solid
            Solid -> LiquidGlass
        }
    }
}
