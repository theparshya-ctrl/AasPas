package com.aaspas.customer.core.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatters {
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun formatShortDate(isoTimestamp: String?): String {
        if (isoTimestamp.isNullOrBlank()) return ""
        return runCatching {
            Instant.parse(isoTimestamp)
                .atZone(ZoneId.systemDefault())
                .format(shortDate)
        }.getOrDefault(isoTimestamp)
    }

    fun formatOfferValue(discountType: String, discountValue: String): String {
        return when (discountType.lowercase(Locale.getDefault())) {
            "percentage", "percent" -> "$discountValue% OFF"
            "up_to_percentage", "up_to_percent" -> "Up to $discountValue% off"
            "flat", "fixed" -> "₹$discountValue OFF"
            "up_to_fixed" -> "Up to ₹$discountValue off"
            else -> discountValue
        }
    }
}
