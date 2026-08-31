package com.aaspas.customer.presentation.notifications

import com.aaspas.customer.domain.model.AppNotification

sealed class NotificationNavigationTarget {
    data class ShopDetails(val shopId: String) : NotificationNavigationTarget()
    data class OfferDetails(val offerId: String) : NotificationNavigationTarget()
    data object ShopProfile : NotificationNavigationTarget()
    data class ManageOffers(val shopId: String) : NotificationNavigationTarget()
    data class EditOffer(val shopId: String, val offerId: String) : NotificationNavigationTarget()
    data object Unavailable : NotificationNavigationTarget()
}

object NotificationNavigation {
    fun resolve(
        notification: AppNotification,
        userRole: String?,
        shopId: String?,
    ): NotificationNavigationTarget {
        val entityType = notification.entityType?.lowercase()
        val entityId = notification.entityId?.takeIf { it.isNotBlank() }
        val isShopOwner = userRole == "shop_owner"
        return when (entityType) {
            "shop" -> {
                if (isShopOwner) {
                    NotificationNavigationTarget.ShopProfile
                } else {
                    entityId?.let { NotificationNavigationTarget.ShopDetails(it) }
                        ?: NotificationNavigationTarget.Unavailable
                }
            }
            "offer" -> {
                val offerId = entityId ?: return NotificationNavigationTarget.Unavailable
                if (isShopOwner) {
                    val resolvedShopId = shopId?.takeIf { it.isNotBlank() }
                        ?: return NotificationNavigationTarget.EditOffer(shopId = "", offerId = offerId)
                    when (notification.type) {
                        "OFFER_REJECTED", "OFFER_RESUBMITTED" ->
                            NotificationNavigationTarget.EditOffer(resolvedShopId, offerId)
                        else -> NotificationNavigationTarget.ManageOffers(resolvedShopId)
                    }
                } else {
                    NotificationNavigationTarget.OfferDetails(offerId)
                }
            }
            else -> NotificationNavigationTarget.Unavailable
        }
    }
}
