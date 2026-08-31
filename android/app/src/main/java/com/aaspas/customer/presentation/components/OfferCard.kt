package com.aaspas.customer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aaspas.customer.R
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun OfferCard(
    offer: Offer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveClick: (() -> Unit)? = null,
) {
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
    val imageDesc = stringResource(R.string.offer_image_desc, offer.title)

    Card(
        modifier = modifier
            .width(AasPasSizes.offerCardWidth)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${offer.title}, $badgeText" },
        shape = RoundedCornerShape(AasPasRadius.lg),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AasPasSpacing.xs),
    ) {
        Box {
            Column {
                RemoteMediaImage(
                    photoUrl = offer.photoUrl,
                    contentDescription = imageDesc,
                    heightFraction = 0.45f,
                )
                Column(
                    modifier = Modifier.padding(AasPasSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
                ) {
                    OfferBadge(isComingSoon = offer.isComingSoon, text = badgeText)
                    if (offer.isVerified) {
                        VerifiedOfferBadge()
                    }
                    Text(
                        text = DateFormatters.formatOfferValue(offer.discountType, offer.discountValue),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (offer.isComingSoon) AasPasColors.AmberAttention else AasPasColors.PurplePrimary,
                    )
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AasPasColors.TextPrimary,
                    )
                    Text(
                        text = offer.shopName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AasPasColors.TextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        offer.distanceKm?.let { distance ->
                            Text(
                                text = stringResource(R.string.distance_km, distance),
                                style = MaterialTheme.typography.labelMedium,
                                color = AasPasColors.TextSecondary,
                            )
                        }
                        Text(
                            text = validityText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (offer.isComingSoon) AasPasColors.AmberAttention else AasPasColors.GreenPositive,
                        )
                    }
                }
            }
            if (onSaveClick != null) {
                FavoriteToggleButton(
                    isSaved = offer.isSaved,
                    onClick = onSaveClick,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun VerifiedOfferBadge() {
    Surface(
        color = AasPasColors.PurpleLight,
        shape = RoundedCornerShape(AasPasRadius.sm),
    ) {
        Text(
            text = stringResource(R.string.verified_by_aaspas),
            modifier = Modifier.padding(horizontal = AasPasSpacing.sm, vertical = AasPasSpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = AasPasColors.PurpleDark,
        )
    }
}

@Composable
private fun OfferBadge(isComingSoon: Boolean, text: String) {
    Surface(
        color = if (isComingSoon) AasPasColors.AmberSurface else AasPasColors.GreenSurface,
        shape = RoundedCornerShape(AasPasRadius.sm),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AasPasSpacing.sm, vertical = AasPasSpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = if (isComingSoon) AasPasColors.AmberAttention else AasPasColors.GreenPositive,
        )
    }
}
