package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.aaspas.customer.R
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ShopPhotoUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
        Text(
            text = stringResource(R.string.shop_owner_photo),
            style = MaterialTheme.typography.titleSmall,
            color = AasPasColors.TextPrimary,
        )
        val previewUrl = value.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (previewUrl != null) {
            AsyncImage(
                model = previewUrl,
                contentDescription = stringResource(R.string.shop_owner_photo_preview_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AasPasSizes.offerCardWidth * 0.45f)
                    .clip(RoundedCornerShape(AasPasRadius.md)),
                contentScale = ContentScale.Crop,
            )
        } else {
            DetailsImagePlaceholder(heightFraction = 0.45f)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.shop_owner_photo_url)) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.shop_owner_photo_url_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AasPasColors.TextSecondary,
        )
        if (enabled && value.isNotBlank()) {
            OutlinedButton(onClick = { onValueChange("") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.shop_owner_photo_remove))
            }
        }
    }
}
