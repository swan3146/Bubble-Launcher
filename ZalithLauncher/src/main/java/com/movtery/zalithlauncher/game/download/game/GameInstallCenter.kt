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

package com.movtery.zalithlauncher.game.download.game

import android.content.Context
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "GameInstallCenter"

/**
 * 全局游戏安装/下载中心。
 *
 * 安装任务不再绑定在下载页面的 ViewModel 生命周期上：任务在应用级作用域中执行，
 * 用户切换页面、返回主界面都不会打断下载，页面只负责订阅状态展示。
 */
object GameInstallCenter {
    sealed interface State {
        /** 空闲 */
        data object Idle : State

        /** 正在安装或下载 */
        data class Running(val installer: GameInstaller) : State

        /** 安装完成 */
        data class Success(val versionName: String) : State

        /** 安装失败 */
        data class Failed(val error: Throwable) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** 当前正在执行的安装器，未在安装时为 null */
    val runningInstaller: GameInstaller?
        get() = (_state.value as? State.Running)?.installer

    val isRunning: Boolean
        get() = _state.value is State.Running

    /**
     * 发起一次安装/下载任务
     *
     * @return 是否成功发起，已有任务在执行时返回 false
     */
    fun start(
        context: Context,
        info: GameDownloadInfo,
        onStart: () -> Unit = {},
        onStop: () -> Unit = {}
    ): Boolean {
        if (isRunning) {
            Logger.warning(TAG, "Install request rejected, another task is running")
            return false
        }

        val applicationContext = context.applicationContext
        val installer = GameInstaller(applicationContext, info, scope)
        _state.value = State.Running(installer)
        onStart()

        installer.installGame(
            onInstalled = { version ->
                Logger.info(TAG, "Game installed: $version")
                _state.value = State.Success(version)
                VersionsManager.refresh("[GameInstallCenter] GameInstaller.onInstalled", version)
                onStop()
            },
            onError = { th ->
                Logger.error(TAG, "Failed to install the game!", th)
                _state.value = State.Failed(th)
                onStop()
            },
            onGameAlreadyInstalled = {
                Logger.warning(TAG, "Game already installed, request ignored")
                _state.value = State.Idle
                onStop()
            }
        )
        return true
    }

    /** 取消当前任务 */
    fun cancel() {
        val installer = runningInstaller ?: return
        installer.cancelInstall()
        _state.value = State.Idle
    }

    /** 消费掉完成/失败结果，避免重复提示 */
    fun clearResult() {
        if (_state.value is State.Running) return
        _state.value = State.Idle
    }
}
