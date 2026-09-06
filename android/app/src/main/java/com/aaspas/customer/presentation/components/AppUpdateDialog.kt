package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.core.update.AppUpdateOffer
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AppUpdateDialog(
    offer: AppUpdateOffer,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!offer.mandatory) {
                onLater()
            }
        },
        title = {
            Text(text = stringResource(R.string.app_update_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.app_update_version_available,
                        offer.latestVersionName,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (offer.releaseNotes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.app_update_whats_new),
                        modifier = Modifier.padding(top = AasPasSpacing.md),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    offer.releaseNotes.forEach { note ->
                        Text(
                            text = stringResource(R.string.app_update_bullet, note),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(text = stringResource(R.string.app_update_action_update))
            }
        },
        dismissButton = if (!offer.mandatory) {
            {
                TextButton(onClick = onLater) {
                    Text(text = stringResource(R.string.app_update_action_later))
                }
            }
        } else {
            null
        },
    )
}
