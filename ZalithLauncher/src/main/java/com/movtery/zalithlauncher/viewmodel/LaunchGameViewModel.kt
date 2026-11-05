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

package com.movtery.zalithlauncher.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.game.launch.GameLaunchFlow
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.screens.content.elements.LaunchGameOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.QuickPlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LaunchGameViewModel : ViewModel() {
    private val _launchFlow = MutableStateFlow<GameLaunchFlow?>(null)
    /**
     * 游戏启动流程
     */
    val launchFlow = _launchFlow.asStateFlow()

    private val _launchGameOperation = MutableStateFlow<LaunchGameOperation>(LaunchGameOperation.None)
    /**
     * 启动游戏操作状态
     */
    val launchGameOperation = _launchGameOperation.asStateFlow()

    fun start(
        activity: Activity,
        version: Version,
        exitActivity: () -> Unit,
        submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
        quickPlay: QuickPlay?,
        skipAccountRefresh: Boolean
    ) {
        //PCL Pro Android: 先落 flow，再在 update 之外启动，避免 update 的 lambda
        //因并发竞争被重复执行导致同一次点击触发多轮启动
        val flow = GameLaunchFlow(viewModelScope)
        _launchFlow.value = flow
        flow.launch(
            context = activity,
            version = version,
            exitActivity = exitActivity,
            submitError = submitError,
            onReloginRequired = { account ->
                activity.runOnUiThread {
                    updateOperation(
                        LaunchGameOperation.AccountRelogin(account, version, quickPlay)
                    )
                }
                cancel()
            },
            onRefreshFailed = { account, th ->
                activity.runOnUiThread {
                    updateOperation(
                        LaunchGameOperation.AccountRefreshFailed(account, th, version, quickPlay)
                    )
                }
                cancel()
            },
            onComplete = {
                //只有当前 flow 还挂在 state 上时才清空，防止误清新的一轮
                _launchFlow.update { current -> if (current === flow) null else current }
            },
            skipAccountRefresh = skipAccountRefresh
        )
    }

    fun cancel() {
        _launchFlow.value?.cancel()
        _launchFlow.update { null }
    }

    /**
     * 尝试启动游戏
     */
    fun tryLaunch(
        version: Version? = null
    ) {
        if (isLaunchIdle()) {
            updateOperation(
                LaunchGameOperation.TryLaunch(
                    version ?: VersionsManager.currentVersion.value
                )
            )
        }
    }

    /**
     * 是否有空闲可以接受新的启动请求
     *
     * 原实现用 `_launchFlow.value == null` 判断，一旦某轮启动因异常/无账号退出时
     * 没有清空 flow，就会永久锁死所有后续启动（点启动毫无反应）。
     * 现在改为以"任务是否真的在跑"为准。
     */
    private fun isLaunchIdle(): Boolean {
        if (launchGameOperation.value != LaunchGameOperation.None) return false
        val flow = _launchFlow.value
        if (flow != null && flow.isLaunching()) return false
        //流程对象残留但已不再运行，顺手清掉，避免死锁
        if (flow != null) _launchFlow.update { current -> if (current === flow) null else current }
        return true
    }

    /**
     * 快速启动（通过存档管理快速游玩存档）
     * @param saveName 存档文件名称
     */
    fun quickPlaySave(
        version: Version,
        saveName: String
    ) {
        if (isLaunchIdle()) {
            updateOperation(
                LaunchGameOperation.TryLaunch(
                    version = version,
                    quickPlay = QuickPlay.Save(saveName),
                )
            )
        }
    }

    /**
     * 尝试启动游戏快速游玩服务器
     * @param address 服务器地址
     */
    fun tryPlayServer(address: String) {
        val version = VersionsManager.currentVersion.value ?: return
        quickPlayServer(version, address)
    }

    /**
     * 通过服务器列表快速游玩服务器
     * @param address 服务器地址
     */
    fun quickPlayServer(
        version: Version,
        address: String
    ) {
        if (isLaunchIdle()) {
            updateOperation(
                LaunchGameOperation.TryLaunch(
                    version = version,
                    quickPlay = QuickPlay.Server(address),
                )
            )
        }
    }

    fun updateOperation(operation: LaunchGameOperation) {
        this._launchGameOperation.update { operation }
    }
}