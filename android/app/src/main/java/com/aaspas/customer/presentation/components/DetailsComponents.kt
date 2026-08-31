package com.aaspas.customer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun DetailsImagePlaceholder(
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.32f,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AasPasSizes.offerCardWidth * heightFraction)
            .clip(RoundedCornerShape(bottomStart = AasPasRadius.lg, bottomEnd = AasPasRadius.lg))
            .background(AasPasColors.PurpleLight),
    )
}

@Composable
fun StatusBadge(
    text: String,
    isComingSoon: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AasPasRadius.pill))
            .background(if (isComingSoon) AasPasColors.AmberSurface else AasPasColors.GreenSurface)
            .padding(horizontal = AasPasSpacing.md, vertical = AasPasSpacing.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isComingSoon) AasPasColors.AmberAttention else AasPasColors.GreenPositive,
        )
    }
}
