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

package com.movtery.zalithlauncher.utils.device

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlin.math.roundToInt

private const val TAG = "DeviceUtils"

fun checkVulkanSupport(packageManager: PackageManager): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
}

/**
 * 根据设备能力自适应计算下载并发度。
 *
 * 固定拉满到 128 对高端设备合适，但低端设备（内存小、CPU 核心少）会因
 * 同时打开过多文件句柄 / 网络连接而卡顿甚至被系统杀掉。这里按：
 *  - 内存大小（MB）：越大允许的连接越多；
 *  - CPU 核心数：连接数不超过核心数的若干倍，避免线程/IO 争抢；
 * 在 [min]~[max] 区间内夹取，保证低端设备也能流畅下载、高端设备不损失速度。
 *
 * 结果通过 MMKV 无关的纯计算得出，调用成本极低，可安全作为默认值求值一次。
 */
fun adaptiveDownloadThreads(
    context: Context = GlobalContext,
    min: Int = 16,
    max: Int = 128
): Int {
    return runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memClass = am?.memoryClass ?: 128 //单位 MB，无法获取时按中等设备 128MB 处理

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        //内存维度：约每 64MB 允许 16 并发，内存越大并发越高（低端 64MB→16，高端 512MB+→128）
        val memBased = ((memClass / 64f) * 16).roundToInt().coerceIn(min, max)

        //CPU 维度：连接数不超过核心数 * 8，避免过多线程争抢；同时不低于 8 保证小文件并发收益
        val cpuBased = (cores * 8).coerceIn(8, max)

        val result = minOf(memBased, cpuBased).coerceIn(min, max)
        Logger.info(TAG, "Adaptive download threads: mem=${memClass}MB cores=$cores -> $result")
        result
    }.getOrElse {
        Logger.warning(TAG, "Failed to compute adaptive threads, fallback to $min", it)
        min
    }
}