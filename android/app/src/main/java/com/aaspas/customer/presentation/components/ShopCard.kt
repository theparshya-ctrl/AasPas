package com.aaspas.customer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aaspas.customer.R
import com.aaspas.customer.domain.model.Shop
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ShopCard(
    shop: Shop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveClick: (() -> Unit)? = null,
) {
    val imageDesc = stringResource(R.string.shop_image_desc, shop.name)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.xs)
            .clickable(onClick = onClick)
            .semantics { contentDescription = shop.name },
        shape = RoundedCornerShape(AasPasRadius.md),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AasPasSpacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AasPasSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactRemoteMediaImage(
                photoUrl = shop.photoUrl,
                contentDescription = imageDesc,
                modifier = Modifier.size(AasPasSizes.minTouchTarget),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AasPasColors.TextPrimary,
                )
                shop.category?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AasPasColors.TextSecondary,
                    )
                }
                shop.addressArea?.let { area ->
                    Text(
                        text = area,
                        style = MaterialTheme.typography.labelMedium,
                        color = AasPasColors.TextSecondary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.distance_km, shop.distanceKm),
                    style = MaterialTheme.typography.labelLarge,
                    color = AasPasColors.PurplePrimary,
                )
                if (onSaveClick != null) {
                    FavoriteToggleButton(isSaved = shop.isSaved, onClick = onSaveClick)
                }
            }
        }
    }
}
