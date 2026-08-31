package com.aaspas.customer.presentation.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.presentation.components.RemoteMediaImage
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.domain.model.MapShopPin
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun MapMarkerSheetContent(
    pin: MapShopPin,
    onViewShop: () -> Unit,
    onViewOffer: (String) -> Unit,
    onDirections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        RemoteMediaImage(
            photoUrl = pin.shop.photoUrl,
            contentDescription = stringResource(R.string.shop_image_desc, pin.shop.name),
            heightFraction = 0.35f,
        )
        Text(
            text = pin.shop.name,
            style = MaterialTheme.typography.titleLarge,
            color = AasPasColors.TextPrimary,
        )
        pin.shop.category?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = AasPasColors.TextSecondary)
        }
        Text(
            text = stringResource(R.string.distance_km, pin.shop.distanceKm),
            style = MaterialTheme.typography.labelMedium,
            color = AasPasColors.PurplePrimary,
        )
        pin.featuredOffer?.let { offer ->
            MapOfferSummary(offer = offer)
        }
        if (pin.offers.size > 1) {
            Text(
                text = stringResource(R.string.map_more_offers),
                style = MaterialTheme.typography.labelMedium,
                color = AasPasColors.TextSecondary,
            )
            pin.offers.forEach { offer ->
                MapOfferListItem(
                    offer = offer,
                    onClick = { onViewOffer(offer.id) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
        ) {
            OutlinedButton(onClick = onViewShop, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.view_shop))
            }
            pin.featuredOffer?.id?.let { offerId ->
                Button(onClick = { onViewOffer(offerId) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.view_offer))
                }
            }
        }
        OutlinedButton(onClick = onDirections, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.directions_cta))
        }
    }
}

@Composable
private fun MapOfferSummary(offer: Offer) {
    val badgeText = if (offer.isComingSoon) {
        stringResource(R.string.coming_soon_badge)
    } else {
        stringResource(R.string.active_badge)
    }
    val validityText = if (offer.isComingSoon) {
        stringResource(R.string.starts_on, DateFormatters.formatShortDate(offer.startsAt))
    } else {
        stringResource(R.string.valid_until, DateFormatters.formatShortDate(offer.endsAt))
    }
    Surface(
        color = if (offer.isComingSoon) AasPasColors.AmberSurface else AasPasColors.GreenSurface,
        shape = RoundedCornerShape(AasPasRadius.sm),
    ) {
        Text(
            text = badgeText,
            modifier = Modifier.padding(horizontal = AasPasSpacing.sm, vertical = AasPasSpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = if (offer.isComingSoon) AasPasColors.AmberAttention else AasPasColors.GreenPositive,
        )
    }
    Text(
        text = offer.title,
        style = MaterialTheme.typography.bodyLarge,
        color = AasPasColors.TextPrimary,
    )
    Text(
        text = validityText,
        style = MaterialTheme.typography.labelMedium,
        color = if (offer.isComingSoon) AasPasColors.AmberAttention else AasPasColors.GreenPositive,
    )
}

@Composable
private fun MapOfferListItem(
    offer: Offer,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AasPasSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = offer.title,
            style = MaterialTheme.typography.bodyMedium,
            color = AasPasColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (offer.isComingSoon) {
            Text(
                text = stringResource(R.string.coming_soon_badge),
                style = MaterialTheme.typography.labelSmall,
                color = AasPasColors.AmberAttention,
            )
        }
    }
}
