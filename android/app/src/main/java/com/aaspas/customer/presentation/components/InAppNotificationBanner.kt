package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.presentation.theme.AasPasColors

@Composable
fun InAppNotificationBanner(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Text(
            text = "🔔 ${notification.title}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Text(
            text = notification.message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
        )
    }
}
