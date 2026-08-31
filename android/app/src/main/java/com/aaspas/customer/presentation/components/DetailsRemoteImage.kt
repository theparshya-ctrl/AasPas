package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes

@Composable
fun DetailsRemoteImage(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.32f,
) {
    RemoteMediaImage(
        photoUrl = photoUrl,
        contentDescription = contentDescription ?: stringResource(R.string.shop_image_desc, ""),
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = AasPasRadius.lg, bottomEnd = AasPasRadius.lg)),
        heightFraction = heightFraction,
    )
}
