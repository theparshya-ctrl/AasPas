package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.model.AppNotification
import kotlinx.coroutines.delay

@Composable
fun InAppNotificationBannerHost(
    store: RealtimeNotificationStore,
    onNotificationClick: (AppNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf<AppNotification?>(null) }
    val queue = remember { ArrayDeque<AppNotification>() }

    LaunchedEffect(store) {
        store.bannerEvent.collect { notification ->
            if (visible == null) {
                visible = notification
            } else {
                queue.addLast(notification)
            }
        }
    }

    LaunchedEffect(visible?.id) {
        val current = visible ?: return@LaunchedEffect
        delay(BANNER_VISIBLE_MS)
        if (visible?.id == current.id) {
            visible = queue.removeFirstOrNull()
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        visible?.let { notification ->
            InAppNotificationBanner(
                notification = notification,
                onClick = {
                    onNotificationClick(notification)
                    visible = queue.removeFirstOrNull()
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private const val BANNER_VISIBLE_MS = 5_000L
