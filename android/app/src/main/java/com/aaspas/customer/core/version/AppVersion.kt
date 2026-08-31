package com.aaspas.customer.core.version

/**
 * Maps Gradle version metadata into display strings for About / DEV identity.
 * Pure functions so unit tests do not need the Android runtime.
 */
data class AppVersionIdentity(
    val productName: String,
    val environment: String,
    val versionName: String,
    val versionCode: Int,
    val isDev: Boolean,
) {
    val brandedName: String
        get() = if (isDev) "$productName DEV" else productName

    val compactIndicator: String?
        get() = if (isDev) "DEV • v$versionName ($versionCode)" else null

    val versionLine: String
        get() = versionName

    val buildLine: String
        get() = versionCode.toString()
}

object AppVersion {
    const val PRODUCT_NAME = "AasPas"
    const val ENVIRONMENT_DEV = "DEV"
    const val ENVIRONMENT_PRODUCTION = "PRODUCTION"

    fun from(
        environment: String,
        versionName: String,
        versionCode: Int,
        productName: String = PRODUCT_NAME,
        debugBuild: Boolean = false,
    ): AppVersionIdentity {
        val isDev = debugBuild || environment.equals(ENVIRONMENT_DEV, ignoreCase = true)
        return AppVersionIdentity(
            productName = productName,
            environment = if (isDev) ENVIRONMENT_DEV else ENVIRONMENT_PRODUCTION,
            versionName = versionName,
            versionCode = versionCode,
            isDev = isDev,
        )
    }
}
