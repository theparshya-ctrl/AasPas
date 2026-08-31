package com.aaspas.customer.core.notifications

import com.aaspas.customer.BuildConfig
import com.aaspas.customer.core.auth.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class RealtimeNotificationClient(
    private val sessionManager: SessionManager,
    private val onMessage: (String) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private val shouldReconnect = AtomicBoolean(false)

    fun connect(scope: CoroutineScope) {
        shouldReconnect.set(true)
        reconnectAttempt = 0
        openSocket(scope)
    }

    fun disconnect() {
        shouldReconnect.set(false)
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(NORMAL_CLOSE_CODE, "logout")
        webSocket = null
    }

    private fun openSocket(scope: CoroutineScope) {
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) {
            return
        }
        webSocket?.cancel()
        val request = Request.Builder()
            .url(buildWebSocketUrl(token))
            .build()
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    onConnected()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    onMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onDisconnected()
                    scheduleReconnect(scope)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onDisconnected()
                    scheduleReconnect(scope)
                }
            },
        )
    }

    private fun scheduleReconnect(scope: CoroutineScope) {
        if (!shouldReconnect.get()) {
            return
        }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt += 1
            val delayMs = min(
                INITIAL_BACKOFF_MS * (1L shl (reconnectAttempt - 1).coerceAtMost(5)),
                MAX_BACKOFF_MS,
            )
            delay(delayMs)
            if (shouldReconnect.get() && !sessionManager.getToken().isNullOrBlank()) {
                openSocket(scope)
            }
        }
    }

    private fun buildWebSocketUrl(token: String): String {
        val httpBase = BuildConfig.API_BASE_URL.trimEnd('/')
        val wsBase = when {
            httpBase.startsWith("https://") -> "wss://${httpBase.removePrefix("https://")}"
            httpBase.startsWith("http://") -> "ws://${httpBase.removePrefix("http://")}"
            else -> httpBase
        }
        return "$wsBase/api/v1/ws/notifications?token=$token"
    }

    companion object {
        private const val NORMAL_CLOSE_CODE = 1000
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
    }
}
