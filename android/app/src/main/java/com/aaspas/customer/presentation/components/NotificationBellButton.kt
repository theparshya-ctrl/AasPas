package com.aaspas.customer.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aaspas.customer.R

@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.notifications_title)
    val unreadDescription = stringResource(R.string.notifications_unread_desc, unreadCount)
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                }
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = if (unreadCount > 0) unreadDescription else title,
                modifier = Modifier.semantics {
                    contentDescription = if (unreadCount > 0) unreadDescription else title
                },
            )
        }
    }
}
