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

package com.movtery.zalithlauncher.ui.screens.content.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * PCL Pro Android: 内置动态壁纸资源目录（assets 下）
 */
private const val WALLPAPER_ASSET_DIR = "wallpapers"

/**
 * 一个内置动态壁纸
 * @param id 稳定 id，用于记录当前使用的壁纸
 * @param assetName assets/wallpapers 下的文件名
 */
data class BuiltInWallpaper(
    val id: String,
    val assetName: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int
) {
    /** 直接把 assets 里的视频交给 ExoPlayer 播放，用于实时预览 */
    val assetUri: String get() = "asset:///$WALLPAPER_ASSET_DIR/$assetName"
}

/**
 * 内置动态壁纸仓库：列表、首帧缩略图、应用到启动器背景
 */
object LauncherWallpapers {
    /**
     * 全部内置壁纸，顺序即选择器中的展示顺序
     */
    val builtIn: List<BuiltInWallpaper> = listOf(
        BuiltInWallpaper(
            id = "anime_halo",
            assetName = "wp_anime_halo.mp4",
            titleRes = R.string.bl_wallpaper_anime_halo,
            summaryRes = R.string.bl_wallpaper_anime_halo_summary
        ),
        BuiltInWallpaper(
            id = "anime_91",
            assetName = "wp_anime_91.mp4",
            titleRes = R.string.bl_wallpaper_anime_91,
            summaryRes = R.string.bl_wallpaper_anime_91_summary
        ),
        BuiltInWallpaper(
            id = "kuroha_lineart",
            assetName = "wp_kuroha_lineart.mp4",
            titleRes = R.string.bl_wallpaper_kuroha,
            summaryRes = R.string.bl_wallpaper_kuroha_summary
        ),
        BuiltInWallpaper(
            id = "genshin",
            assetName = "wp_genshin.mp4",
            titleRes = R.string.bl_wallpaper_genshin,
            summaryRes = R.string.bl_wallpaper_genshin_summary
        ),
        BuiltInWallpaper(
            id = "anime_character",
            assetName = "wp_anime_character.mp4",
            titleRes = R.string.bl_wallpaper_character,
            summaryRes = R.string.bl_wallpaper_character_summary
        ),
        BuiltInWallpaper(
            id = "anime_girl",
            assetName = "wp_anime_girl.mp4",
            titleRes = R.string.bl_wallpaper_girl,
            summaryRes = R.string.bl_wallpaper_girl_summary
        ),
        BuiltInWallpaper(
            id = "anime_angel",
            assetName = "wp_anime_angel.mp4",
            titleRes = R.string.bl_wallpaper_angel,
            summaryRes = R.string.bl_wallpaper_angel_summary
        ),
        BuiltInWallpaper(
            id = "gojo",
            assetName = "wp_gojo.mp4",
            titleRes = R.string.bl_wallpaper_gojo,
            summaryRes = R.string.bl_wallpaper_gojo_summary
        ),
        BuiltInWallpaper(
            id = "short_loop",
            assetName = "wp_short_loop.mp4",
            titleRes = R.string.bl_wallpaper_short_loop,
            summaryRes = R.string.bl_wallpaper_short_loop_summary
        )
    )

    private val thumbnailCache = ConcurrentHashMap<String, ImageBitmap>()

    /**
     * 当前正在使用的内置壁纸（可能为 null，表示没在使用内置壁纸）
     */
    fun current(): BuiltInWallpaper? {
        val id = AllSettings.videoWallpaperId.state
        if (id.isEmpty()) return null
        return builtIn.firstOrNull { it.id == id }
    }

    fun isApplied(wallpaper: BuiltInWallpaper): Boolean {
        return AllSettings.videoWallpaperId.state == wallpaper.id
    }

    /**
     * 取内置壁纸的首帧作为缩略图，结果会被缓存
     */
    suspend fun thumbnail(context: Context, wallpaper: BuiltInWallpaper): ImageBitmap? {
        thumbnailCache[wallpaper.id]?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            extractFirstFrame(context, "$WALLPAPER_ASSET_DIR/${wallpaper.assetName}")
        } ?: return null
        return bitmap.asImageBitmap().also { thumbnailCache[wallpaper.id] = it }
    }

    /**
     * 把内置壁纸应用到启动器背景
     */
    suspend fun apply(
        context: Context,
        wallpaper: BuiltInWallpaper,
        backgroundViewModel: BackgroundViewModel
    ) {
        val file = withContext(Dispatchers.IO) {
            copyToCache(context, wallpaper)
        }
        backgroundViewModel.applyLocalFile(file)
        AllSettings.videoWallpaperId.save(wallpaper.id)
    }

    /**
     * 用户换成自己的图片或视频后，内置壁纸记录需要清理，避免显示错误的"使用中"状态
     */
    suspend fun clearAppliedIfNotBuiltIn() {
        val id = AllSettings.videoWallpaperId.state
        if (id.isNotEmpty() && builtIn.none { it.id == id }) {
            AllSettings.videoWallpaperId.save("")
        }
    }

    /**
     * 把 assets 中的壁纸复制到应用私有目录，避免每次都从 APK 里解压
     */
    private fun copyToCache(context: Context, wallpaper: BuiltInWallpaper): File {
        val dir = File(PathManager.DIR_FILES_PRIVATE, "wallpapers")
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, wallpaper.assetName)
        if (target.exists() && target.length() > 0L) {
            return target
        }
        val temp = File(dir, "${wallpaper.assetName}.tmp")
        context.assets.open("$WALLPAPER_ASSET_DIR/${wallpaper.assetName}").use { input ->
            temp.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        return target
    }

    private fun extractFirstFrame(context: Context, assetPath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            context.assets.openFd(assetPath).use { fd ->
                retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
