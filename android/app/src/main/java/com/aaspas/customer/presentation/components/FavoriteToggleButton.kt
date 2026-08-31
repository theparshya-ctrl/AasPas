package com.aaspas.customer.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.presentation.theme.AasPasColors

@Composable
fun FavoriteToggleButton(
    isSaved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = contentDescription ?: stringResource(R.string.save_favorite),
            tint = if (isSaved) AasPasColors.PurplePrimary else AasPasColors.TextSecondary,
        )
    }
}
