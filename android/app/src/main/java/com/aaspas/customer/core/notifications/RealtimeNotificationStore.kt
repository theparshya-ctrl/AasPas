package com.aaspas.customer.core.notifications

import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class RealtimeNotificationStore {
    private val knownIds = ConcurrentHashMap.newKeySet<String>()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _bannerEvent = MutableSharedFlow<AppNotification>(extraBufferCapacity = 1)
    val bannerEvent: SharedFlow<AppNotification> = _bannerEvent.asSharedFlow()

    fun applyFromRest(list: NotificationList) {
        _unreadCount.value = list.unreadCount
        list.notifications.forEach { knownIds.add(it.id) }
    }

    fun applyFromWebSocket(notification: AppNotification, unreadCount: Int): Boolean {
        if (!knownIds.add(notification.id)) {
            _unreadCount.value = unreadCount
            return false
        }
        _unreadCount.value = unreadCount
        _bannerEvent.tryEmit(notification)
        return true
    }

    fun setUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }

    fun decrementUnreadCount() {
        _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
    }

    fun reset() {
        knownIds.clear()
        _unreadCount.value = 0
    }
}
