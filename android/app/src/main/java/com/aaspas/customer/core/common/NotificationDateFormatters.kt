package com.aaspas.customer.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationDateFormatters {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun formatRelativeDateTime(isoTimestamp: String): String {
        return runCatching {
            val zoned = Instant.parse(isoTimestamp).atZone(ZoneId.systemDefault())
            val dateLabel = when (zoned.toLocalDate()) {
                LocalDate.now(ZoneId.systemDefault()) -> "Today"
                LocalDate.now(ZoneId.systemDefault()).minusDays(1) -> "Yesterday"
                else -> DateFormatters.formatShortDate(isoTimestamp)
            }
            "$dateLabel, ${zoned.format(timeFormatter)}"
        }.getOrDefault(isoTimestamp)
    }
}
