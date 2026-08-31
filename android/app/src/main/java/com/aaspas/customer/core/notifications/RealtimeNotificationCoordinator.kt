package com.aaspas.customer.core.notifications

import com.aaspas.customer.core.auth.SessionManager
import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.mapper.NotificationMapper
import com.aaspas.customer.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class RealtimeNotificationCoordinator(
    private val sessionManager: SessionManager,
    private val notificationRepository: NotificationRepository,
    private val store: RealtimeNotificationStore,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    private var client: RealtimeNotificationClient? = null
    private var started = false

    fun start(scope: CoroutineScope) {
        if (started) {
            return
        }
        started = true
        scope.launch {
            sessionManager.sessionState.collect { state ->
                    when (state) {
                        SessionState.LoggedIn -> connect(scope)
                        SessionState.LoggedOut -> disconnect()
                    }
                }
        }
    }

    private fun connect(scope: CoroutineScope) {
        if (client == null) {
            client = RealtimeNotificationClient(
                sessionManager = sessionManager,
                onMessage = { payload -> handleMessage(payload) },
                onConnected = { scope.launch { syncFromRest() } },
                onDisconnected = {},
            )
        }
        client?.connect(scope)
        scope.launch { syncFromRest() }
    }

    private fun disconnect() {
        client?.disconnect()
        store.reset()
    }

    private fun handleMessage(payload: String) {
        runCatching {
            val message = json.decodeFromString<RealtimeNotificationMessage>(payload)
            if (message.type != "notification") {
                return
            }
            val dto = message.notification ?: return
            val unreadCount = message.unreadCount ?: store.unreadCount.value
            store.applyFromWebSocket(NotificationMapper.toDomain(dto), unreadCount)
        }
    }

    suspend fun syncFromRest() {
        if (!sessionManager.isLoggedIn()) {
            store.reset()
            return
        }
        when (val result = notificationRepository.listNotifications(unreadOnly = false)) {
            is Result.Success -> store.applyFromRest(result.data)
            is Result.Failure -> Unit
        }
    }
}
