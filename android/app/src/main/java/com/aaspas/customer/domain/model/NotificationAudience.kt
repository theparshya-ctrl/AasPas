package com.aaspas.customer.domain.model

enum class NotificationAudience(val value: String) {
    BUSINESS("business"),
    CUSTOMER("customer"),
    ADMIN("admin"),
    ;

    companion object {
        fun from(value: String?): NotificationAudience? {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
        }
    }
}
