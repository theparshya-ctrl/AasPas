package com.aaspas.customer.domain.shopowner

import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantOfferStatus

object MerchantOfferValidator {
    fun validateDraft(draft: MerchantOfferDraft): String? {
        if (draft.title.trim().length < 2) {
            return "Offer title is required (min 2 characters)."
        }
        return validateDiscount(draft.discountType, draft.discountValue)
    }

    fun validateForSubmit(draft: MerchantOfferDraft): String? {
        validateDraft(draft)?.let { return it }
        if (draft.description.trim().length < 10) {
            return "Description must be at least 10 characters."
        }
        if (draft.startDate.isBlank() || draft.startTime.isBlank()) {
            return "Start date and time are required."
        }
        if (draft.endDate.isBlank() || draft.endTime.isBlank()) {
            return "End date and time are required."
        }
        val startsAt = MerchantOfferDateTime.combine(draft.startDate, draft.startTime)
        val endsAt = MerchantOfferDateTime.combine(draft.endDate, draft.endTime)
        if (startsAt == null || endsAt == null) {
            return "Enter valid date (YYYY-MM-DD) and time (HH:MM)."
        }
        if (startsAt >= endsAt) {
            return "End must be after start."
        }
        return null
    }

    fun validateDiscount(type: String, value: String): String? {
        val amount = value.trim().toDoubleOrNull()
            ?: return "Enter a valid discount value."
        return when (type) {
            "percentage" -> if (amount <= 0 || amount > 100) {
                "Percentage discount must be between 0 and 100."
            } else {
                null
            }
            "fixed" -> if (amount <= 0) {
                "Fixed discount must be greater than 0."
            } else {
                null
            }
            else -> "Choose percentage or fixed discount."
        }
    }

    fun isEditable(status: MerchantOfferStatus): Boolean {
        return status == MerchantOfferStatus.Draft || status == MerchantOfferStatus.Rejected
    }
}

object MerchantOfferDateTime {
    fun combine(date: String, time: String): String? {
        val dateParts = date.trim().split("-")
        val timeParts = time.trim().split(":")
        if (dateParts.size != 3 || timeParts.size != 2) return null
        return "${date.trim()}T${time.trim()}:00Z"
    }

    fun splitDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return iso.substringBefore("T")
    }

    fun splitTime(iso: String?): String {
        if (iso.isNullOrBlank() || !iso.contains("T")) return ""
        val timePart = iso.substringAfter("T").substringBefore("Z").substringBefore("+")
        val parts = timePart.split(":")
        if (parts.size < 2) return ""
        return "${parts[0]}:${parts[1]}"
    }
}
