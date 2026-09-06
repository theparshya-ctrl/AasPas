package com.aaspas.customer.core.update

import com.aaspas.customer.data.remote.dto.AppVersionDto

data class AppUpdateOffer(
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val mandatory: Boolean,
    val releaseNotes: List<String>,
)

object AppUpdateEvaluator {
    fun evaluate(currentVersionCode: Int, remote: AppVersionDto?): AppUpdateOffer? {
        if (remote == null) return null
        val latestCode = remote.latestVersionCode
        if (latestCode <= 0 || latestCode <= currentVersionCode) return null
        val latestName = remote.latestVersionName.trim()
        if (latestName.isEmpty()) return null
        val downloadUrl = remote.downloadUrl?.trim().orEmpty()
        if (!downloadUrl.startsWith("https://")) return null
        val notes = remote.releaseNotes.map { it.trim() }.filter { it.isNotEmpty() }
        return AppUpdateOffer(
            latestVersionName = latestName,
            latestVersionCode = latestCode,
            downloadUrl = downloadUrl,
            mandatory = remote.mandatory,
            releaseNotes = notes,
        )
    }
}
