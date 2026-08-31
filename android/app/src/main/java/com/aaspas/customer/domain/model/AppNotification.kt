package com.aaspas.customer.domain.model

data class AppNotification(
    val id: String,
    val type: String,
    val audience: String,
    val title: String,
    val message: String,
    val entityType: String?,
    val entityId: String?,
    val isRead: Boolean,
    val createdAt: String,
)

data class NotificationList(
    val notifications: List<AppNotification>,
    val unreadCount: Int,
)

enum class NotificationEntityType(val value: String) {
    SHOP("shop"),
    OFFER("offer"),
    ;

    companion object {
        fun from(value: String?): NotificationEntityType? {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
        }
    }
}
