package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.presentation.common.messageResId
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = AasPasColors.TextPrimary)
        Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = AasPasColors.TextSecondary)
    }
}

@Composable
fun CategoryMapEmptyState(
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.map_category_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = AasPasColors.TextPrimary,
        )
        Text(
            text = stringResource(R.string.map_category_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AasPasColors.TextSecondary,
        )
        OutlinedButton(onClick = onChangeLocation) {
            Text(text = stringResource(R.string.location_change_action))
        }
    }
}

@Composable
fun ErrorState(
    error: AppError?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    fallbackMessageResId: Int = R.string.error_generic,
) {
    val message = error?.let { stringResource(it.messageResId(fallbackMessageResId)) }
        ?: stringResource(fallbackMessageResId)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = AasPasColors.RedCritical)
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
fun GlobalErrorBanner(
    error: AppError?,
    fallbackMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = error?.let { stringResource(it.messageResId(R.string.error_generic)) }
        ?: fallbackMessage
        ?: stringResource(R.string.error_generic)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.sm),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.RedSurface),
        shape = RoundedCornerShape(AasPasRadius.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AasPasSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AasPasColors.RedCritical,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Composable
fun MapCtaCard(onViewMap: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.sm),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.PurpleLight),
        shape = RoundedCornerShape(AasPasRadius.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AasPasSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.map_cta_title),
                style = MaterialTheme.typography.bodyLarge,
                color = AasPasColors.PurpleDark,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onViewMap) {
                Text(text = stringResource(R.string.map_cta_button))
            }
        }
    }
}

@Composable
private fun SkeletonBox(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Border.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(AasPasRadius.md),
    ) {}
}

@Composable
fun CategorySkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = AasPasSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        repeat(4) {
            SkeletonBox(
                modifier = Modifier
                    .height(AasPasSizes.categoryChipHeight)
                    .width(AasPasSizes.categoryChipHeight * 2),
            )
        }
    }
}

@Composable
fun OfferCardsSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AasPasSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        items(count = 3) {
            SkeletonBox(
                modifier = Modifier
                    .width(AasPasSizes.offerCardWidth)
                    .height(AasPasSizes.offerCardWidth * 0.78f),
            )
        }
    }
}

@Composable
fun ShopCardsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        repeat(3) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AasPasSizes.minTouchTarget + AasPasSpacing.sm),
            )
        }
    }
}

@Composable
fun HomeLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.lg),
    ) {
        SkeletonBox(
            modifier = Modifier
                .padding(horizontal = AasPasSpacing.lg)
                .fillMaxWidth(0.6f)
                .height(AasPasSpacing.xxl),
        )
        SkeletonBox(
            modifier = Modifier
                .padding(horizontal = AasPasSpacing.lg)
                .fillMaxWidth()
                .height(AasPasSizes.searchBarHeight),
        )
        CategorySkeleton()
        SectionHeader(title = stringResource(R.string.section_today_offers))
        OfferCardsSkeleton()
        SectionHeader(title = stringResource(R.string.section_coming_soon))
        OfferCardsSkeleton()
        SectionHeader(title = stringResource(R.string.section_nearby_shops))
        ShopCardsSkeleton()
    }
}
