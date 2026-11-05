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

package com.movtery.zalithlauncher.ui.screens.content.settings

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.setting.enums.DarkMode
import com.movtery.zalithlauncher.setting.enums.MaterialStyle
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.DisabledAlpha
import com.movtery.zalithlauncher.ui.screens.content.nameRes
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.EnumSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.wallpaper.WallpaperPickerDialog
import com.movtery.zalithlauncher.ui.screens.content.wallpaper.WallpaperSettingsEntry
import com.movtery.zalithlauncher.ui.theme.ColorThemeType
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * PCL Pro Android: 个性化
 *
 * 承接原「启动器设置」中与主题、背景、动画相关的能力，
 * 从主页顶栏 Dock 栏的「个性化」入口直接进入。
 */
@Composable
fun PersonalizationScreen(
    backStackViewModel: ScreenBackStackViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
) {
    BaseScreen(
        screenKey = NormalNavKey.Personalization,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) { scope ->
            //外观：配色主题、暗黑模式、节日特效
            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    var customColorOperation by remember {
                        mutableStateOf<CustomColorOperation>(CustomColorOperation.None)
                    }
                    CustomColorOperation(
                        customColorOperation = customColorOperation,
                        updateOperation = { customColorOperation = it }
                    )

                    EnumSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.launcherColorTheme,
                        title = stringResource(R.string.settings_launcher_color_theme_title),
                        summary = stringResource(R.string.settings_launcher_color_theme_summary),
                        entries = ColorThemeType.entries,
                        getRadioEnable = { enum ->
                            if (enum == ColorThemeType.DYNAMIC) Build.VERSION.SDK_INT >= Build.VERSION_CODES.S else true
                        },
                        getRadioText = { enum ->
                            when (enum) {
                                ColorThemeType.DYNAMIC -> stringResource(R.string.theme_color_dynamic)
                                ColorThemeType.EMBERMIRE -> stringResource(R.string.theme_color_embermire)
                                ColorThemeType.VELVET_ROSE -> stringResource(R.string.theme_color_velvet_rose)
                                ColorThemeType.MISTWAVE -> stringResource(R.string.theme_color_mistwave)
                                ColorThemeType.GLACIER -> stringResource(R.string.theme_color_glacier)
                                ColorThemeType.VERDANTFIELD -> stringResource(R.string.theme_color_verdant_field)
                                ColorThemeType.URBAN_ASH -> stringResource(R.string.theme_color_urban_ash)
                                ColorThemeType.VERDANT_DAWN -> stringResource(R.string.theme_color_verdant_dawn)
                                ColorThemeType.CUSTOM -> stringResource(R.string.generic_custom)
                            }
                        },
                        maxItemsInEachRow = 5,
                        onRadioClick = { enum ->
                            if (enum == ColorThemeType.CUSTOM) customColorOperation = CustomColorOperation.Dialog
                        }
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherDarkMode,
                        items = DarkMode.entries,
                        title = stringResource(R.string.settings_launcher_dark_mode_title),
                        getItemText = { stringResource(it.textRes) }
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.launcherFestivalEffects,
                        title = stringResource(R.string.settings_launcher_festivals_effects_title),
                        summary = stringResource(R.string.settings_launcher_festivals_effects_summary)
                    )
                }
            }

            //背景：壁纸、不透明度、视频音量、模糊与材质
            LocalBackgroundViewModel.current?.let { backgroundViewModel ->
                AnimatedItem(scope) { yOffset ->
                    var showWallpaperPicker by remember { mutableStateOf(false) }

                    SettingsCardColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                    ) {
                        //PCL Pro Android: 液态玻璃材质下背景不透明度锁死 0（由玻璃 backdrop 直接采样清晰背景，
                        //再加一层半透明底色反而脏）；切回纯色材质时默认 50。切换材质时自动落盘。
                        val materialStyle = AllSettings.launcherMaterialStyle.state
                        LaunchedEffect(materialStyle) {
                            if (materialStyle == MaterialStyle.LiquidGlass) {
                                if (AllSettings.launcherBackgroundOpacity.state != 0) {
                                    AllSettings.launcherBackgroundOpacity.save(0)
                                }
                            } else {
                                if (AllSettings.launcherBackgroundOpacity.state == 0) {
                                    AllSettings.launcherBackgroundOpacity.save(50)
                                }
                            }
                        }
                        val opacityLocked = materialStyle == MaterialStyle.LiquidGlass

                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top
                        ) {
                            WallpaperSettingsEntry(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showWallpaperPicker = true }
                            )
                        }

                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle
                        ) {
                            CustomBackground(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundViewModel = backgroundViewModel,
                                submitError = submitError
                            )
                        }

                        if (showWallpaperPicker) {
                            WallpaperPickerDialog(
                                backgroundViewModel = backgroundViewModel,
                                onDismiss = { showWallpaperPicker = false }
                            )
                        }

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.launcherBackgroundOpacity,
                            title = stringResource(R.string.settings_launcher_background_opacity_title),
                            summary = stringResource(R.string.settings_launcher_background_opacity_summary),
                            valueRange = AllSettings.launcherBackgroundOpacity.floatRange,
                            suffix = "%",
                            enabled = backgroundViewModel.isValid && !opacityLocked,
                            fineTuningControl = true
                        )

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.videoBackgroundVolume,
                            title = stringResource(R.string.settings_launcher_background_video_volume_title),
                            summary = stringResource(R.string.settings_launcher_background_video_volume_summary),
                            valueRange = AllSettings.videoBackgroundVolume.floatRange,
                            suffix = "%",
                            enabled = backgroundViewModel.isValid && backgroundViewModel.isVideo,
                            fineTuningControl = true
                        )

                        IntSliderSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.backgroundBlur,
                            title = stringResource(R.string.settings_title_blur),
                            summary = stringResource(R.string.settings_launcher_background_blur_summary),
                            valueRange = AllSettings.backgroundBlur.floatRange,
                            suffix = "Dp",
                            enabled = backgroundViewModel.isValid,
                            fineTuningControl = true,
                            appendContent = {
                                val unit = AllSettings.backgroundBlurType
                                val state = unit.state
                                IconButton(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = DisabledAlpha),
                                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = DisabledAlpha),
                                    ),
                                    onClick = {
                                        unit.save(state.switch())
                                    },
                                    enabled = backgroundViewModel.isValid,
                                ) {
                                    Crossfade(
                                        targetState = state
                                    ) { target ->
                                        val painter = when (target) {
                                            BackgroundBlur.Background -> painterResource(R.drawable.ic_blur_circular_outlined)
                                            BackgroundBlur.Foreground -> painterResource(R.drawable.ic_blur_circular_filled)
                                        }
                                        Icon(
                                            painter = painter,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )

                        EnumSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Bottom,
                            unit = AllSettings.launcherMaterialStyle,
                            title = stringResource(R.string.bl_material_title),
                            summary = stringResource(R.string.settings_launcher_material_summary),
                            entries = MaterialStyle.entries,
                            getRadioEnable = { true },
                            getRadioText = { enum -> stringResource(enum.nameRes()) }
                        )
                    }
                }
            }

            //动画：速度、幅度与切换动画
            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.launcherAnimateSpeed,
                        title = stringResource(R.string.settings_launcher_animate_speed_title),
                        summary = stringResource(R.string.settings_launcher_animate_speed_summary),
                        valueRange = AllSettings.launcherAnimateSpeed.floatRange,
                        suffix = "x"
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.launcherAnimateExtent,
                        title = stringResource(R.string.settings_launcher_animate_extent_title),
                        summary = stringResource(R.string.settings_launcher_animate_extent_summary),
                        valueRange = AllSettings.launcherAnimateExtent.floatRange,
                        suffix = "x"
                    )

                    EnumSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.launcherSwapAnimateType,
                        title = stringResource(R.string.settings_launcher_swap_animate_type_title),
                        summary = stringResource(R.string.settings_launcher_swap_animate_type_summary),
                        entries = TransitionAnimationType.entries,
                        getRadioEnable = { true },
                        getRadioText = { enum ->
                            stringResource(enum.textRes)
                        }
                    )
                }
            }

            //PCL Pro Android: 自适应功耗开关
            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.adaptivePerformance,
                        title = stringResource(R.string.bl_adaptive_performance_title),
                        summary = stringResource(R.string.bl_adaptive_performance_summary)
                    )
                }
            }
        }
    }
}
