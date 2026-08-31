package com.aaspas.customer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.BuildConfig
import com.aaspas.customer.R
import com.aaspas.customer.core.version.AppVersion
import com.aaspas.customer.core.version.AppVersionIdentity
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AboutAppSection(
    modifier: Modifier = Modifier,
    identity: AppVersionIdentity? = null,
) {
    val resolved = identity ?: AppVersion.from(
        environment = BuildConfig.APP_ENVIRONMENT,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        debugBuild = BuildConfig.DEBUG,
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("about_app"),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
    ) {
        Column(
            modifier = Modifier.padding(AasPasSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
        ) {
            Text(
                text = stringResource(R.string.about_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = AasPasColors.TextSecondary,
            )
            Text(
                text = resolved.productName,
                style = MaterialTheme.typography.titleMedium,
                color = AasPasColors.TextPrimary,
            )
            Text(
                text = stringResource(R.string.about_environment, resolved.environment),
                style = MaterialTheme.typography.bodyMedium,
                color = AasPasColors.TextPrimary,
            )
            Text(
                text = stringResource(R.string.about_version, resolved.versionLine),
                style = MaterialTheme.typography.bodyMedium,
                color = AasPasColors.TextPrimary,
            )
            Text(
                text = stringResource(R.string.about_build, resolved.buildLine),
                style = MaterialTheme.typography.bodyMedium,
                color = AasPasColors.TextPrimary,
            )
            if (resolved.isDev) {
                Text(
                    text = stringResource(
                        R.string.about_dev_indicator,
                        resolved.versionName,
                        resolved.versionCode,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = AasPasColors.PurplePrimary,
                    modifier = Modifier.testTag("dev_indicator"),
                )
            }
        }
    }
}
