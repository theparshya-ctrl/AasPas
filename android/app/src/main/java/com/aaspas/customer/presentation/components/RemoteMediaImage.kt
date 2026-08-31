package com.aaspas.customer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.aaspas.customer.core.network.MediaUrlResolver
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes

@Composable
fun RemoteMediaImage(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.45f,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val resolved = MediaUrlResolver.resolve(photoUrl)
    val imageModifier = modifier
        .fillMaxWidth()
        .height(AasPasSizes.offerCardWidth * heightFraction)
    if (resolved == null) {
        DetailsImagePlaceholder(modifier = imageModifier, heightFraction = heightFraction)
        return
    }
    SubcomposeAsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = imageModifier,
        contentScale = contentScale,
        loading = {
            DetailsImagePlaceholder(modifier = Modifier.matchParentSize(), heightFraction = heightFraction)
        },
        error = {
            DetailsImagePlaceholder(modifier = Modifier.matchParentSize(), heightFraction = heightFraction)
        },
    )
}

@Composable
fun CompactRemoteMediaImage(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val resolved = MediaUrlResolver.resolve(photoUrl)
    if (resolved == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(AasPasRadius.sm))
                .background(AasPasColors.PurpleLight),
        )
        return
    }
    SubcomposeAsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = modifier.clip(RoundedCornerShape(AasPasRadius.sm)),
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AasPasColors.PurpleLight),
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AasPasColors.PurpleLight),
            )
        },
    )
}
