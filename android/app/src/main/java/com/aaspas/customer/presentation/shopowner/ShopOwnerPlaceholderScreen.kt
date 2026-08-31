package com.aaspas.customer.presentation.shopowner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOwnerPlaceholderScreen(
    titleRes: Int,
    messageRes: Int,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = AasPasColors.TextSecondary,
            )
        }
    }
}
