package com.aaspas.customer.presentation.components



import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.KeyboardArrowDown

import androidx.compose.material.icons.outlined.LocationOn

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.semantics.Role

import androidx.compose.ui.semantics.role

import androidx.compose.ui.semantics.semantics

import com.aaspas.customer.R

import com.aaspas.customer.domain.model.LocationSource

import com.aaspas.customer.presentation.home.LocationLabelState

import com.aaspas.customer.presentation.theme.AasPasColors

import com.aaspas.customer.presentation.theme.AasPasSizes

import com.aaspas.customer.presentation.theme.AasPasSpacing



@Composable

fun LocationHeader(

    locationLabel: LocationLabelState,

    locationSource: LocationSource,

    locationAvailable: Boolean,

    locationDenied: Boolean,

    localityName: String? = null,

    onClick: () -> Unit,

    modifier: Modifier = Modifier,

) {

    val subtitleText = when {

        locationLabel == LocationLabelState.Loading -> stringResource(R.string.home_location_loading)

        locationSource == LocationSource.MANUAL -> stringResource(R.string.location_selected_subtitle)

        locationSource == LocationSource.CURRENT_GPS && locationAvailable -> stringResource(R.string.location_current_subtitle)

        locationSource == LocationSource.NONE || locationDenied -> stringResource(R.string.location_choose_subtitle)

        else -> stringResource(R.string.location_change_action)

    }

    val titleText = when {

        locationLabel == LocationLabelState.Loading -> stringResource(R.string.home_location_loading)

        !localityName.isNullOrBlank() -> localityName

        locationLabel == LocationLabelState.NearYou -> stringResource(R.string.home_location_near_you)

        else -> stringResource(R.string.home_location_unavailable)

    }

    val iconTint = when {

        locationSource == LocationSource.MANUAL && locationAvailable -> AasPasColors.PurplePrimary

        locationAvailable -> AasPasColors.GreenPositive

        locationDenied -> AasPasColors.TextSecondary

        else -> AasPasColors.AmberAttention

    }

    val iconDescription = when {

        locationAvailable -> stringResource(R.string.location_icon_available_desc)

        locationDenied -> stringResource(R.string.location_icon_denied_desc)

        else -> stringResource(R.string.location_icon_loading_desc)

    }



    Row(

        modifier = modifier

            .fillMaxWidth()

            .clickable(onClick = onClick)

            .semantics { role = Role.Button }

            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.sm),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),

    ) {

        Icon(

            imageVector = Icons.Outlined.LocationOn,

            contentDescription = iconDescription,

            tint = iconTint,

            modifier = Modifier.size(AasPasSizes.minTouchTarget / 2),

        )

        Column(modifier = Modifier.weight(1f)) {

            Text(

                text = titleText,

                style = MaterialTheme.typography.headlineSmall,

                color = AasPasColors.TextPrimary,

            )

            Text(

                text = subtitleText,

                style = MaterialTheme.typography.labelMedium,

                color = if (locationSource == LocationSource.MANUAL) {

                    AasPasColors.PurplePrimary

                } else {

                    AasPasColors.TextSecondary

                },

            )

        }

        Icon(

            imageVector = Icons.Filled.KeyboardArrowDown,

            contentDescription = stringResource(R.string.location_change_action),

            tint = AasPasColors.TextSecondary,

            modifier = Modifier.size(AasPasSizes.minTouchTarget / 2),

        )

    }

}

