package com.aaspas.customer.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.domain.model.AdminDashboard
import com.aaspas.customer.presentation.components.NotificationBellButton
import com.aaspas.customer.presentation.components.AboutAppSection
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminConsoleRoute(
    onBack: () -> Unit,
    onShopVerification: () -> Unit,
    onOfferVerification: () -> Unit,
    onShopManagement: () -> Unit,
    onUserManagement: () -> Unit,
    onCustomerHome: () -> Unit,
    onNotifications: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminConsoleViewModel = viewModel(
        factory = AdminConsoleViewModelFactory(
            app.adminRepository,
            app.notificationRepository,
            app.realtimeNotificationStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    AdminConsoleScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onShopVerification = onShopVerification,
        onOfferVerification = onOfferVerification,
        onShopManagement = onShopManagement,
        onUserManagement = onUserManagement,
        onCustomerHome = onCustomerHome,
        onNotifications = onNotifications,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    uiState: AdminConsoleUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShopVerification: () -> Unit,
    onOfferVerification: () -> Unit,
    onShopManagement: () -> Unit,
    onUserManagement: () -> Unit,
    onCustomerHome: () -> Unit,
    onNotifications: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_console_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    NotificationBellButton(
                        unreadCount = uiState.unreadNotificationCount,
                        onClick = onNotifications,
                    )
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            AdminConsoleStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            AdminConsoleStatus.Unauthorized -> {
                EmptyState(
                    title = stringResource(R.string.admin_unauthorized_title),
                    hint = stringResource(R.string.admin_unauthorized_hint),
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            AdminConsoleStatus.Error -> {
                ErrorState(error = uiState.error, onRetry = onRetry, modifier = Modifier.padding(padding).padding(AasPasSpacing.lg))
            }
            AdminConsoleStatus.Loaded -> {
                val dashboard = uiState.dashboard ?: return@Scaffold
                AdminConsoleContent(
                    dashboard = dashboard,
                    modifier = Modifier.padding(padding),
                    onShopVerification = onShopVerification,
                    onOfferVerification = onOfferVerification,
                    onShopManagement = onShopManagement,
                    onUserManagement = onUserManagement,
                    onCustomerHome = onCustomerHome,
                )
            }
        }
    }
}

@Composable
private fun AdminConsoleContent(
    dashboard: AdminDashboard,
    modifier: Modifier = Modifier,
    onShopVerification: () -> Unit,
    onOfferVerification: () -> Unit,
    onShopManagement: () -> Unit,
    onUserManagement: () -> Unit,
    onCustomerHome: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        Text(stringResource(R.string.admin_console_subtitle), color = AasPasColors.TextSecondary)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                Text(stringResource(R.string.admin_pending_offers_count, dashboard.pendingOffers), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.admin_pending_shops_count, dashboard.pendingShops), color = AasPasColors.TextSecondary)
            }
        }
        Button(onClick = onShopManagement, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_shop_management))
        }
        Button(onClick = onUserManagement, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_user_management))
        }
        Button(onClick = onShopVerification, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_shop_verification))
        }
        Button(onClick = onOfferVerification, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_offer_verification))
        }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_reports_placeholder))
        }
        OutlinedButton(onClick = onCustomerHome, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.admin_browse_as_customer))
        }
        AboutAppSection()
    }
}
