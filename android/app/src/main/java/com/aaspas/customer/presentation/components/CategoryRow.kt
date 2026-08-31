package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.presentation.home.SectionStatus
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun CategoryRow(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AasPasSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        items(categories, key = { it.id }) { category ->
            AssistChip(
                onClick = { onCategoryClick(category) },
                label = {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                shape = RoundedCornerShape(AasPasRadius.pill),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AasPasColors.PurpleLight,
                    labelColor = AasPasColors.PurpleDark,
                ),
            )
        }
    }
}

@Composable
fun CategorySection(
    categories: List<Category>,
    status: SectionStatus,
    error: AppError?,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.section_categories))
        when (status) {
            SectionStatus.Loading -> CategorySkeleton()
            SectionStatus.Empty -> EmptyState(
                title = stringResource(R.string.empty_categories),
                hint = stringResource(R.string.empty_hint),
            )
            SectionStatus.Error -> ErrorState(error = error)
            SectionStatus.Loaded -> CategoryRow(
                categories = categories,
                onCategoryClick = onCategoryClick,
            )
        }
    }
}
