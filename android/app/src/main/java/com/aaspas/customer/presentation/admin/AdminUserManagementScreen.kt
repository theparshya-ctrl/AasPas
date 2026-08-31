package com.aaspas.customer.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.domain.model.AdminUserListItem
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminUserManagementRoute(
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminUserManagementViewModel = viewModel(
        factory = AdminUserManagementViewModelFactory(app.adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    AdminUserManagementScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSearchSubmit = viewModel::submitSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    uiState: AdminUserManagementUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_user_management)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(AasPasSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.admin_user_search_hint)) },
                singleLine = true,
                trailingIcon = {
                    TextButton(onClick = onSearchSubmit) {
                        Text(stringResource(R.string.search))
                    }
                },
            )
            when (uiState.status) {
                AdminUserManagementStatus.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
                }
                AdminUserManagementStatus.Error -> {
                    ErrorState(error = uiState.error, onRetry = onRetry)
                }
                AdminUserManagementStatus.Loaded -> {
                    if (uiState.users.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.admin_user_management_empty_title),
                            hint = stringResource(R.string.admin_user_management_empty_hint),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                        ) {
                            items(uiState.users, key = { it.userId }) { user ->
                                AdminUserManagementCard(user = user)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserManagementCard(user: AdminUserListItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
            Text(user.fullName ?: user.email, style = MaterialTheme.typography.titleMedium)
            Text(user.email, color = AasPasColors.TextSecondary)
            Text(
                stringResource(R.string.admin_user_management_role_status, user.role, if (user.isActive) "active" else "inactive"),
                color = AasPasColors.TextSecondary,
            )
            user.shopName?.let {
                Text(stringResource(R.string.admin_user_management_shop, it), color = AasPasColors.TextSecondary)
            }
            Text(
                stringResource(R.string.admin_shop_management_created, DateFormatters.formatShortDate(user.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = AasPasColors.TextSecondary,
            )
        }
    }
}
