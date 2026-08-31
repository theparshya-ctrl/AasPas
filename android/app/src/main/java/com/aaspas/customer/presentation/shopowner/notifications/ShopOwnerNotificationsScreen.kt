package com.aaspas.customer.presentation.shopowner.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.common.NotificationDateFormatters
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ShopOwnerNotificationsRoute(
    onBack: () -> Unit,
    onNavigateToShopProfile: () -> Unit,
    onNavigateToManageOffers: (shopId: String) -> Unit,
    onNavigateToEditOffer: (shopId: String, offerId: String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: ShopOwnerNotificationsViewModel = viewModel(
        factory = ShopOwnerNotificationsViewModelFactory(
            app.notificationRepository,
            app.shopOwnerRepository,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(uiState.navigationTarget) {
        when (val target = uiState.navigationTarget) {
            NotificationNavigationTarget.ShopProfile -> {
                onNavigateToShopProfile()
                viewModel.onNavigationHandled()
            }
            is NotificationNavigationTarget.ManageOffers -> {
                onNavigateToManageOffers(target.shopId)
                viewModel.onNavigationHandled()
            }
            is NotificationNavigationTarget.EditOffer -> {
                onNavigateToEditOffer(target.shopId, target.offerId)
                viewModel.onNavigationHandled()
            }
            NotificationNavigationTarget.Unavailable, null -> Unit
        }
    }

    ShopOwnerNotificationsScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onNotificationClick = viewModel::onNotificationClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOwnerNotificationsScreen(
    uiState: NotificationsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shop_owner_notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            NotificationsLoadStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            NotificationsLoadStatus.Error -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            NotificationsLoadStatus.Empty -> {
                EmptyState(
                    title = stringResource(R.string.shop_owner_notifications_empty_title),
                    hint = stringResource(R.string.shop_owner_notifications_empty_hint),
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            NotificationsLoadStatus.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(AasPasSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                ) {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val containerColor = if (notification.isRead) {
        AasPasColors.Surface
    } else {
        AasPasColors.PurpleLight
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(AasPasSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (notification.isRead) AasPasColors.TextSecondary else AasPasColors.PurplePrimary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = AasPasColors.TextPrimary,
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AasPasColors.TextSecondary,
                )
                Text(
                    text = NotificationDateFormatters.formatRelativeDateTime(notification.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = AasPasColors.TextSecondary,
                )
            }
        }
    }
}
