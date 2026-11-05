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

package com.movtery.zalithlauncher.coroutine

import com.movtery.zalithlauncher.coroutine.TaskFlowExecutor.TaskPhase
import com.movtery.zalithlauncher.keepalive.TaskKeepAlive
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.isInterruptedIOException
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "TaskFlowExecutor"

/**
 * 动态任务流执行器，按照阶段顺序执行任务流
 */
class TaskFlowExecutor(
    private val scope: CoroutineScope
) {
    /**
     * 任务流阶段，包含一个阶段的所有任务
     */
    data class TaskPhase(
        val tasks: List<TitledTask>,
        val onComplete: (suspend () -> Unit)? = null
    )

    /**
     * 当前所有的任务流阶段
     */
    private val phases: MutableList<TaskPhase> = mutableListOf()

    private val _tasksFlow: MutableStateFlow<List<TitledTask>> = MutableStateFlow(emptyList())
    val tasksFlow = _tasksFlow.asStateFlow()

    private var job: Job? = null
    /**
     * 最近一次任务流的 Job（含已取消但尚未真正结束的）
     * 取消是异步生效的，需要它来等待"写入已停止"
     */
    private var lastJob: Job? = null
    /** 当前正在执行的任务的Job */
    private var currentTaskJob: Job? = null
    /** 当前任务流阶段索引 */
    private var currentPhaseIndex: Int = -1

    /**
     * 获取下一个阶段
     */
    private fun getNextPhase(): TaskPhase? {
        currentPhaseIndex++
        return phases.getOrNull(currentPhaseIndex)
    }

    /**
     * 添加阶段到末尾
     */
    fun addPhase(phase: TaskPhase) {
        phases.add(phase)
    }

    /**
     * 添加阶段列表到末尾
     */
    fun addPhases(phases: List<TaskPhase>) {
        this.phases.addAll(phases)
    }

    /**
     * 同步执行多阶段任务流
     */
    suspend fun executePhases(
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onCancel: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        currentPhaseIndex = -1
        try {
        while (true) {
            try {
                ensureActive()
                val phase = getNextPhase() ?: break
                //更新当前任务列表
                _tasksFlow.update { phase.tasks }

                //执行阶段内的所有任务
                for (task in phase.tasks) {
                    ensureActive()
                    task.task.updateStage(TaskStage.RUNNING)

                    //为每个任务创建独立的Job，以便可以立即取消
                    //使用SupervisorJob确保任务内部的子协程异常不会影响其他任务
                    val parentJob = coroutineContext[Job]
                    val taskJob = SupervisorJob(parentJob)
                    currentTaskJob = taskJob
                    
                    try {
                        withContext(task.task.dispatcher + taskJob) {
                            //使用coroutineScope确保任务内部的所有子协程都在这个作用域中
                            //当taskJob被取消时，coroutineScope内的所有子协程都会被取消
                            coroutineScope {
                                try {
                                    //将当前coroutineScope传递给任务，确保任务内部的launch都在这个作用域中
                                    task.task.task(this@coroutineScope, task.task)
                                } catch (e: CancellationException) {
                                    task.task.onCancel()
                                    throw e
                                } catch (e: Throwable) {
                                    task.task.onError(e)
                                    throw e
                                } finally {
                                    task.task.onFinally()
                                }
                            }
                        }
                        task.task.updateStage(TaskStage.COMPLETED)
                    } finally {
                        //确保taskJob被取消和清理，无论任务成功还是失败
                        taskJob.cancel()
                        currentTaskJob = null
                    }
                }

                //执行阶段完成回调
                phase.onComplete?.invoke()
            } catch (th: Throwable) {
                if (th is CancellationException || th.isInterruptedIOException()) {
                    Logger.debug(TAG, "The current task flow has been cancelled. ${th.getMessageOrToString()}")
                    onCancel()
                } else {
                    Logger.warning(TAG, "An exception occurred while executing the task flow.", th)
                    onError(th)
                }
                return@withContext
            }
        }

        onComplete()
        } finally {
            //任务流结束后复位阶段索引并清空阶段列表，
            //避免下一次复用同一执行器时旧阶段被重复执行
            currentPhaseIndex = -1
            phases.clear()
        }
    }

    /**
     * 异步执行多阶段任务流
     */
    fun executePhasesAsync(
        onStart: suspend () -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        if (isRunning()) {
            Logger.warning(TAG, "Another task flow is still running, the new request was ignored.")
            return
        }
        val newJob = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            //持有保活，避免启动器切至后台后任务流被系统中断
            TaskKeepAlive.acquire()
            try {
                onStart()
                executePhases(onComplete, onError, onCancel)
            } finally {
                TaskKeepAlive.release()
            }
        }
        job = newJob
        lastJob = newJob
        //无论正常完成、异常结束还是被取消，都必须复位运行状态，
        //否则 isRunning() 会恒为 true，导致后续同类任务被永久拦截
        newJob.invokeOnCompletion {
            //仅当当前记录的仍是这次的任务时才清理，避免取消后立即重新发起时误清新任务
            if (job === newJob) {
                job = null
            }
        }
        newJob.start()
    }

    fun isRunning(): Boolean = job?.isActive == true

    /**
     * 挂起等待上一次任务流真正结束
     *
     * [cancel] 只是发出取消信号，正在写盘的子协程不会立刻停止；
     * 取消后需要清理任务产物（例如删除半成品版本目录）时，
     * 必须先等任务流完全收尾，否则删除与写入会竞态
     *
     * @param timeoutMillis 最长等待时间，超时后返回（避免因卡死的 IO 无限等待）
     */
    suspend fun awaitIdle(timeoutMillis: Long = 5_000L) {
        val pending = lastJob ?: return
        withTimeoutOrNull(timeoutMillis) { pending.join() }
        if (lastJob === pending) {
            lastJob = null
        }
    }

    fun cancel() {
        //先取消当前正在执行的任务及其所有子协程
        currentTaskJob?.cancel()
        currentTaskJob = null

        val runningJob = job
        job = null
        runningJob?.cancel()

        _tasksFlow.update { emptyList() }
        currentPhaseIndex = -1
    }
}

/**
 * 构建任务流阶段
 */
fun buildPhase(
    onComplete: (suspend () -> Unit)? = null,
    builderAction: MutableList<TitledTask>.() -> Unit
): TaskPhase {
    val tasks: List<TitledTask> = buildList(builderAction = builderAction)
    return TaskPhase(
        tasks = tasks,
        onComplete = onComplete
    )
}