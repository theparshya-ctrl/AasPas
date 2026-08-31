package com.aaspas.customer.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.core.auth.RoleAccess
import com.aaspas.customer.presentation.components.AboutAppSection
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ProfileRoute(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onShopOwnerDashboardClick: () -> Unit,
    onAdminConsoleClick: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(app.authRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        onRetry = viewModel::loadProfile,
        onLoginClick = onLoginClick,
        onRegisterClick = onRegisterClick,
        onFavoritesClick = onFavoritesClick,
        onShopOwnerDashboardClick = onShopOwnerDashboardClick,
        onAdminConsoleClick = onAdminConsoleClick,
        onLogout = viewModel::logout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onShopOwnerDashboardClick: () -> Unit,
    onAdminConsoleClick: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_profile)) }) },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            ProfileLoadStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = AasPasColors.PurplePrimary)
                }
            }
            ProfileLoadStatus.Unauthorized -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(AasPasSpacing.lg)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                ) {
                    EmptyState(
                        title = stringResource(R.string.profile_guest_title),
                        hint = stringResource(R.string.profile_guest_hint),
                    )
                    Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.sign_in))
                    }
                    OutlinedButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.create_account))
                    }
                    AboutAppSection()
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
            ProfileLoadStatus.Error -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(AasPasSpacing.lg)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                ) {
                    ErrorState(
                        error = uiState.error,
                        onRetry = onRetry,
                    )
                    AboutAppSection()
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
            ProfileLoadStatus.Loaded -> {
                val user = uiState.user ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(AasPasSpacing.lg)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                ) {
                    Text(
                        text = user.fullName ?: stringResource(R.string.profile_default_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AasPasColors.TextPrimary,
                    )
                    ProfileInfoRow(
                        label = stringResource(R.string.email_label),
                        value = user.email,
                    )
                    ProfileInfoRow(
                        label = stringResource(R.string.account_role_label),
                        value = user.role.replaceFirstChar { it.uppercase() },
                    )
                    ProfileInfoRow(
                        label = stringResource(R.string.member_since_label),
                        value = DateFormatters.formatShortDate(user.createdAt),
                    )
                    Spacer(modifier = Modifier.height(AasPasSpacing.sm))
                    if (RoleAccess.canAccessShopOwner(user.role)) {
                        Button(
                            onClick = onShopOwnerDashboardClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.shop_owner_open_dashboard))
                        }
                    }
                    if (RoleAccess.canAccessAdmin(user.role)) {
                        Button(
                            onClick = onAdminConsoleClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.admin_open_console))
                        }
                    }
                    OutlinedButton(onClick = onFavoritesClick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.view_favorites))
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.logout))
                    }
                    AboutAppSection()
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AasPasColors.TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = AasPasColors.TextPrimary)
    }
}
