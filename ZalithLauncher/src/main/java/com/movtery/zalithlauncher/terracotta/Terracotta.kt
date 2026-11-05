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

package com.movtery.zalithlauncher.terracotta

import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.coroutine.MutableTransitionStateFlow
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.burningtnt.terracotta.TerracottaAndroidAPI
import java.io.IOException
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "Terracotta"

/** 状态变化后的高频轮询间隔 */
private const val ACTIVE_POLL_INTERVAL_MILLIS = 2L
/** 状态稳定时的空闲轮询间隔，原实现固定 1ms，长期空转会持续唤醒 CPU/JNI（耗电发热） */
private const val IDLE_POLL_INTERVAL_MILLIS = 24L
/** 读取异常或状态为空时的退避间隔 */
private const val BACKOFF_POLL_INTERVAL_MILLIS = 200L

/**
 * [Reference FCL](https://github.com/FCL-Team/FoldCraftLauncher/blob/52f0542/FCL/src/main/java/com/tungsten/fcl/terracotta/Terracotta.java)
 */
object Terracotta {
    const val TERRACOTTA_USER_NOTICE_VERSION = 1

    enum class Mode {
        /** 房主模式 */
        Host,
        /** 房客模式 */
        Guest
    }

    var initialized = false
        private set

    private var eventViewModel: EventViewModel? = null

    private var notificationJob: Job? = null
    private var metadata: TerracottaAndroidAPI.Metadata? = null

    var mode: Mode? = null
        private set

    private val _state = MutableTransitionStateFlow<TerracottaState.Ready?>(null)
    val state = _state.stateFlow
    val stateChanges = _state.changes

    private val stateRef = AtomicReference<TerracottaState.Ready?>(null)

    fun initialize(
        lifecycleScope: CoroutineScope,
        eventViewModel: EventViewModel
    ) {
        if (initialized) return
        this.eventViewModel = eventViewModel

        metadata = TerracottaAndroidAPI.initialize(
            PathManager.DIR_TERRACOTTA.absolutePath,
            PathManager.FILE_TERRACOTTA_LOG.absolutePath
        ) {
            eventViewModel.sendEvent(EventViewModel.Event.Terracotta.RequestVPN)
            lifecycleScope.startNotificationJob()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            //PCL Pro Android: 轮询必须自愈。原实现在 native 抛错或 JSON 为 null 时直接跳出循环，
            //之后联机状态永久不再更新（表现为联机卡住、按钮没反应），这里改为捕获异常并继续轮询。
            var failureCount = 0
            while (true) {
                var delayMillis = IDLE_POLL_INTERVAL_MILLIS
                try {
                    val current = stateRef.get()
                    val index = current?.index ?: -1

                    val stateJson = TerracottaAndroidAPI.getState()
                    val obj = TerracottaState.TerracottaStateGson.fromJson<TerracottaState.Ready>(
                        stateJson,
                        object : TypeToken<TerracottaState.Ready>() {}.type
                    )

                    if (obj == null) {
                        //native 尚未产出有效状态，短暂退避后重试，不终止轮询
                        failureCount++
                        if (failureCount == 1 || failureCount % 200 == 0) {
                            Logger.warning(TAG, "Terracotta state is null, keep polling (failureCount = $failureCount)")
                        }
                        delayMillis = BACKOFF_POLL_INTERVAL_MILLIS
                    } else {
                        failureCount = 0
                        if (obj.index > index) {
                            compareAndSet(current, obj)
                            //状态刚变化时贴近实时，避免下一个界面状态滞后
                            delayMillis = ACTIVE_POLL_INTERVAL_MILLIS
                        }
                    }
                } catch (e: Throwable) {
                    //捕获所有异常（含 native 的 UnsatisfiedLinkError / RuntimeException），保证轮询不中断
                    failureCount++
                    if (failureCount == 1 || failureCount % 200 == 0) {
                        Logger.warning(TAG, "Failed to poll terracotta state, retrying (failureCount = $failureCount)", e)
                    }
                    delayMillis = BACKOFF_POLL_INTERVAL_MILLIS
                }

                delay(delayMillis)
            }
        }

        initialized = true
    }

    fun setWaiting(manual: Boolean) {
        if (!initialized) return

        stopNotificationJob()
        if (manual) this.eventViewModel?.sendEvent(EventViewModel.Event.Terracotta.StopVPN)
        TerracottaAndroidAPI.setWaiting()
    }

    fun setScanning(room: String?, player: String?, extraNodes: List<String>?) {
        checkInitialized()
        if (_state.value !is TerracottaState.Waiting)
            throw Exception("reset state to waiting first!")

        mode = Mode.Host
        TerracottaAndroidAPI.setScanning(room, player, extraNodes)
    }

    fun setGuesting(room: String, player: String?, extraNodes: List<String>?): Boolean {
        checkInitialized()
        if (_state.value !is TerracottaState.Waiting)
            throw Exception("reset state to waiting first!")

        mode = Mode.Guest
        return TerracottaAndroidAPI.setGuesting(room, player, extraNodes)
    }

    fun parseRoomCode(room: String?): TerracottaAndroidAPI.RoomType? {
        if (!initialized || room == null) return null
        return TerracottaAndroidAPI.parseRoomCode(room)
    }

    fun getMetadata(): TerracottaAndroidAPI.Metadata =
        metadata ?: TerracottaAndroidAPI.Metadata("unknown", 0, "unknown")

    fun collectLogs(): String? {
        if (!initialized) return null
        return try {
            TerracottaAndroidAPI.collectLogs().use { reader ->
                val writer = StringWriter()
                val buf = CharArray(4096)
                var n: Int
                while (reader.read(buf).also { n = it } != -1) {
                    writer.write(buf, 0, n)
                }
                writer.toString()
            }
        } catch (e: IOException) {
            e.message?.let { Logger.warning(TAG, it) }
            "Failed to collect logs: ${e.message}"
        }
    }

    @Deprecated("This API is exposed for debug purpose.")
    fun testNativePanic() {
        if (!initialized) return
        TerracottaAndroidAPI.panic()
    }

    private fun checkInitialized() {
        if (!initialized) throw Exception("initialize Terracotta first!")
    }

    private fun compareAndSet(previous: TerracottaState.Ready?, next: TerracottaState.Ready) {
        if (stateRef.compareAndSet(previous, next)) {
            _state.set(next)
        }
    }

    private fun CoroutineScope.startNotificationJob() {
        notificationJob?.cancel()
        notificationJob = launch(Dispatchers.Default) {
            state.collect { state ->
                if (state != null && state !is TerracottaState.Waiting) {
                    val stringRes = state.localStringRes()
                    eventViewModel?.sendEvent(EventViewModel.Event.Terracotta.VPNUpdateState(stringRes))
                }
            }
        }
    }

    private fun stopNotificationJob() {
        notificationJob?.cancel()
        notificationJob = null
    }
}
