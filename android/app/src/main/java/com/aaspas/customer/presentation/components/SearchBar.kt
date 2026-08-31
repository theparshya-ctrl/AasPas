package com.aaspas.customer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun SearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg)
            .clickable(onClick = onClick),
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(AasPasSizes.searchBarHeight),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.search_hint),
                    tint = AasPasColors.TextSecondary,
                )
            },
            shape = RoundedCornerShape(AasPasRadius.pill),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = AasPasColors.Surface,
                disabledBorderColor = AasPasColors.Border,
                disabledPlaceholderColor = AasPasColors.TextSecondary,
            ),
        )
    }
}
