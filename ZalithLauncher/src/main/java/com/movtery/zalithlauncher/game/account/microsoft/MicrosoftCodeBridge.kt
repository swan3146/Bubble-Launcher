package com.movtery.zalithlauncher.game.account.microsoft

import kotlinx.coroutines.CompletableDeferred

/**
 * 在“内嵌 WebView 授权码登录”中，把 WebView 拦截到的 authorization code
 * 传递给等待中的登录协程。单次有效。
 */
object MicrosoftCodeBridge {
    @Volatile
    private var deferred: CompletableDeferred<String>? = null

    /** 由 WebViewScreen 在拦截到 nativeclient?code=... 时调用 */
    fun deliver(code: String) {
        deferred?.takeIf { !it.isCompleted }?.complete(code)
    }

    /** 由登录协程等待用户在 WebView 里完成登录 */
    suspend fun awaitCode(): String {
        deferred = CompletableDeferred()
        return deferred!!.await()
    }
}
