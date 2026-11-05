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

package com.movtery.zalithlauncher.ui.screens.content.download

import com.movtery.zalithlauncher.ui.theme.cardColor
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.gson.JsonSyntaxException
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.download.game.GameInstallCenter
import androidx.compose.ui.Alignment
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import com.movtery.zalithlauncher.game.download.game.optifine.CantFetchingOptiFineUrlException
import com.movtery.zalithlauncher.game.download.game.optifine.OptiFineForge17IncompatibleException
import com.movtery.zalithlauncher.game.download.jvm_server.JvmCrashException
import com.movtery.zalithlauncher.game.download.jvm_server.isProcessStartRefused
import com.movtery.zalithlauncher.game.version.download.DownloadFailedException
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.NotificationCheck
import com.movtery.zalithlauncher.ui.components.RegionRestrictionNotice
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.game.DownloadGameWithAddonScreen
import com.movtery.zalithlauncher.ui.screens.content.download.game.SelectGameVersionHost
import com.movtery.zalithlauncher.ui.screens.content.download.game.SelectGameVersionScreen
import com.movtery.zalithlauncher.ui.screens.content.elements.TitleTaskFlowDialog
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.utils.RegionRestriction
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.isUsingMobileData
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeoutException

/** 游戏安装状态操作 */
private sealed interface GameInstallOperation {
    data object None : GameInstallOperation
    /** 开始安装 */
    data object Install : GameInstallOperation
    /** 警告通知权限，可以无视，并直接开始安装 */
    data class WarningForNotification(val info: GameDownloadInfo) : GameInstallOperation
    /** 警告正在使用流量 */
    data class WarningForMobileData(val info: GameDownloadInfo) : GameInstallOperation
    /** 游戏安装出现异常 */
    data class Error(val th: Throwable) : GameInstallOperation
    /** 游戏已成功安装 */
    data object Success : GameInstallOperation
}

private class GameDownloadViewModel : ViewModel() {
    /**
     * 用于刷新游戏下载页面版本名称的检查
     */
    var versionNameErrorCheck by mutableStateOf(false)
    var installOperation by mutableStateOf<GameInstallOperation>(GameInstallOperation.None)

    /**
     * 游戏安装器：由全局安装中心持有，离开页面也不会中断
     */
    val installer: GameInstaller?
        get() = GameInstallCenter.runningInstaller

    /**
     * 刷新游戏下载页面内的版本名称检查
     */
    private fun refreshVersionNameCheck() {
        versionNameErrorCheck = !versionNameErrorCheck
    }

    fun install(
        context: Context,
        info: GameDownloadInfo,
        onStart: () -> Unit = {},
        onStop: () -> Unit = {},
    ) {
        installOperation = GameInstallOperation.Install
        val started = GameInstallCenter.start(
            context = context,
            info = info,
            onStart = onStart,
            onStop = {
                onStop()
                refreshVersionNameCheck()
                installOperation = when (val state = GameInstallCenter.state.value) {
                    is GameInstallCenter.State.Success -> GameInstallOperation.Success
                    is GameInstallCenter.State.Failed -> GameInstallOperation.Error(state.error)
                    else -> GameInstallOperation.None
                }
            }
        )
        if (!started) {
            //已有任务在后台执行，不重复发起
            installOperation = GameInstallOperation.None
        }
    }

    fun cancel() {
        GameInstallCenter.cancel()
        installOperation = GameInstallOperation.None
        refreshVersionNameCheck()
    }

    /**
     * 同步全局安装中心状态：任务在后台继续执行，页面只做展示
     */
    fun syncCenterState(state: GameInstallCenter.State) {
        when (state) {
            is GameInstallCenter.State.Running -> {
                if (installOperation !is GameInstallOperation.Install) {
                    installOperation = GameInstallOperation.Install
                }
            }
            is GameInstallCenter.State.Success -> {
                if (installOperation !is GameInstallOperation.Success) {
                    installOperation = GameInstallOperation.Success
                    refreshVersionNameCheck()
                }
            }
            is GameInstallCenter.State.Failed -> {
                if (installOperation !is GameInstallOperation.Error) {
                    installOperation = GameInstallOperation.Error(state.error)
                    refreshVersionNameCheck()
                }
            }
            GameInstallCenter.State.Idle -> Unit
        }
    }
}

@Composable
private fun rememberGameDownloadViewModel(
    key: NestedNavKey.DownloadGame
): GameDownloadViewModel {
    return viewModel(
        key = key.toString()
    ) {
        GameDownloadViewModel()
    }
}

@Composable
fun DownloadGameScreen(
    key: NestedNavKey.DownloadGame,
    mainScreenKey: TitledNavKey?,
    downloadScreenKey: TitledNavKey?,
    downloadGameScreenKey: TitledNavKey?,
    onCurrentKeyChange: (TitledNavKey?) -> Unit,
    eventViewModel: EventViewModel
) {
    val viewModel: GameDownloadViewModel = rememberGameDownloadViewModel(key)

    val context = LocalContext.current
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    val regionState by RegionRestriction.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        RegionRestriction.ensureChecked()
    }

    if (regionState.restricting) {
        RegionRestrictionNotice(modifier = Modifier.fillMaxSize())
        return
    }

    val installCenterState by GameInstallCenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(installCenterState) {
        viewModel.syncCenterState(installCenterState)
    }

    GameInstallOperation(
        gameInstallOperation = viewModel.installOperation,
        updateOperation = {
            viewModel.installOperation = it
            if (it is GameInstallOperation.None) {
                GameInstallCenter.clearResult()
            }
        },
        installer = GameInstallCenter.runningInstaller,
        onInstall = { info ->
            viewModel.install(
                context = context,
                info = info,
                onStart = {
                    eventViewModel.sendKeepScreen(true)
                },
                onStop = {
                    eventViewModel.sendKeepScreen(false)
                }
            )
        },
        onCancel = {
            viewModel.cancel()
            eventViewModel.sendKeepScreen(false)
        }
    )

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = {
                onBack(backStack)
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.DownloadGame.SelectGameVersion> {
                    SelectGameVersionScreen(
                        host = SelectGameVersionHost.Download(
                            mainScreenKey = mainScreenKey,
                            downloadScreenKey = downloadScreenKey,
                            downloadGameScreenKey = downloadGameScreenKey
                        ),
                        eventViewModel = eventViewModel,
                    ) { versionString ->
                        backStack.navigateTo(
                            NormalNavKey.DownloadGame.Addons(versionString)
                        )
                    }
                }
                entry<NormalNavKey.DownloadGame.Addons> { key ->
                    val context = LocalContext.current
                    DownloadGameWithAddonScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadGameScreenKey = downloadGameScreenKey,
                        key = key,
                        refreshErrorCheck = viewModel.versionNameErrorCheck
                    ) { info ->
                        if (viewModel.installOperation !is GameInstallOperation.None) {
                            //不是待安装状态，拒绝此次安装
                            return@DownloadGameWithAddonScreen
                        }
                        if (!NotificationManager.checkNotificationEnabled(context)) {
                            //警告通知权限
                            viewModel.installOperation = GameInstallOperation.WarningForNotification(info)
                        } else {
                            if (isUsingMobileData(context)) {
                                //警告正在使用流量
                                viewModel.installOperation = GameInstallOperation.WarningForMobileData(info)
                            } else {
                                viewModel.install(
                                    context = context,
                                    info = info,
                                    onStart = {
                                        eventViewModel.sendKeepScreen(true)
                                    },
                                    onStop = {
                                        eventViewModel.sendKeepScreen(false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun GameInstallOperation(
    gameInstallOperation: GameInstallOperation,
    updateOperation: (GameInstallOperation) -> Unit = {},
    installer: GameInstaller?,
    onInstall: (GameDownloadInfo) -> Unit,
    onCancel: () -> Unit
) {
    when (gameInstallOperation) {
        is GameInstallOperation.None -> {}
        is GameInstallOperation.WarningForNotification -> {
            NotificationCheck(
                text = stringResource(R.string.notification_data_jvm_service_message),
                onGranted = {
                    //权限被授予，开始安装
                    onInstall(gameInstallOperation.info)
                },
                onIgnore = {
                    //用户不想授权，但是支持继续进行安装
                    onInstall(gameInstallOperation.info)
                },
                onDismiss = {
                    updateOperation(GameInstallOperation.None)
                }
            )
        }
        is GameInstallOperation.WarningForMobileData -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.download_install_warning_mobile_data),
                confirmText = stringResource(R.string.generic_anyway),
                onDismiss = {
                    updateOperation(GameInstallOperation.None)
                },
                onConfirm = {
                    //用户坚持使用移动网络
                    onInstall(gameInstallOperation.info)
                }
            )
        }
        is GameInstallOperation.Install -> {
            //PCL Pro Android: 安装/下载转入后台后，进度统一由 MainScreen 顶层的
            //后台下载悬浮窗（DownloadFloatingWindow）展示。
            //这里不再绘制任何浮层，避免被版本/模组列表等上层内容遮挡，也避免重复提示。
            Unit
        }
        is GameInstallOperation.Error -> {
            val th = gameInstallOperation.th
            Logger.error("InstallGame", "Failed to download the game!", th)
            val message = when (th) {
                is HttpRequestTimeoutException, is SocketTimeoutException, is TimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is SerializationException, is JsonSyntaxException -> stringResource(R.string.error_parse_failed)
                is CantFetchingOptiFineUrlException -> stringResource(R.string.download_install_error_cant_fetch_optifine_download_url)
                is OptiFineForge17IncompatibleException -> stringResource(R.string.download_install_error_optifine_forge17_incompatible, th.buildof)
                is JvmCrashException -> stringResource(R.string.download_install_error_jvm_crash, th.code)
                is DownloadFailedException -> stringResource(R.string.download_install_error_download_failed)
                else -> when {
                    th.isProcessStartRefused() -> stringResource(R.string.download_install_error_process_start)
                    else -> th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                }
            }
            val dismiss = {
                updateOperation(GameInstallOperation.None)
            }
            AlertDialog(
                containerColor = cardColor(),
                tonalElevation = 0.dp,
                onDismissRequest = dismiss,
                title = {
                    Text(text = stringResource(R.string.download_install_error_title))
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .verticalScrollWithBar(state = scrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = stringResource(R.string.download_install_error_message))
                        Text(text = message)
                    }
                },
                confirmButton = {
                    Button(onClick = dismiss) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            )
        }
        is GameInstallOperation.Success -> {
            SimpleAlertDialog(
                title = stringResource(R.string.download_install_success_title),
                text = stringResource(R.string.download_install_success_message)
            ) {
                updateOperation(GameInstallOperation.None)
            }
        }
    }
}