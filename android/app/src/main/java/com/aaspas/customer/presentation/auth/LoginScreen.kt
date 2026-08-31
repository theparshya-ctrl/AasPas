package com.aaspas.customer.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.aaspas.customer.presentation.components.PasswordTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.auth.PendingAuthAction
import com.aaspas.customer.presentation.components.PasswordTextField
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onRegisterClick: () -> Unit,
    onShopOwnerRegisterClick: () -> Unit,
    onLoginSuccess: (role: String?) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(app.authRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.status, uiState.loggedInRole) {
        if (uiState.status == LoginStatus.Success) {
            PendingAuthAction.runAndClear()
            onLoginSuccess(uiState.loggedInRole)
        }
    }

    LoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        onRegisterClick = onRegisterClick,
        onShopOwnerRegisterClick = onShopOwnerRegisterClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onRegisterClick: () -> Unit,
    onShopOwnerRegisterClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sign_in)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(AasPasSpacing.lg)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
        ) {
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AasPasColors.TextSecondary,
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.password_label),
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.errorMessage?.let { message ->
                Text(text = message, color = AasPasColors.RedCritical)
            }
            Button(
                onClick = onSubmit,
                enabled = uiState.status != LoginStatus.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.status == LoginStatus.Loading) {
                    CircularProgressIndicator(
                        color = AasPasColors.White,
                        modifier = Modifier.height(AasPasSpacing.lg),
                    )
                } else {
                    Text(stringResource(R.string.sign_in))
                }
            }
            TextButton(onClick = onRegisterClick) {
                Text(stringResource(R.string.create_account))
            }
            TextButton(onClick = onShopOwnerRegisterClick) {
                Text(stringResource(R.string.shop_owner_register_entry))
            }
            Spacer(modifier = Modifier.height(AasPasSpacing.sm))
        }
    }
}
