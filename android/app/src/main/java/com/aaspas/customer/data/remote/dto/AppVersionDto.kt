package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionDto(
    @SerialName("latest_version_name") val latestVersionName: String = "",
    @SerialName("latest_version_code") val latestVersionCode: Int = 0,
    @SerialName("download_url") val downloadUrl: String? = null,
    val mandatory: Boolean = false,
    @SerialName("release_notes") val releaseNotes: List<String> = emptyList(),
)
