package com.aaspas.customer.presentation.shopowner.notifications

import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationEntityType

sealed class NotificationNavigationTarget {
    data object ShopProfile : NotificationNavigationTarget()
    data class ManageOffers(val shopId: String) : NotificationNavigationTarget()
    data class EditOffer(val shopId: String, val offerId: String) : NotificationNavigationTarget()
    data object Unavailable : NotificationNavigationTarget()
}

object NotificationNavigation {
    fun resolve(
        notification: AppNotification,
        shopId: String?,
    ): NotificationNavigationTarget {
        return when (NotificationEntityType.from(notification.entityType)) {
            NotificationEntityType.SHOP -> NotificationNavigationTarget.ShopProfile
            NotificationEntityType.OFFER -> {
                val offerId = notification.entityId?.takeIf { it.isNotBlank() } ?: return NotificationNavigationTarget.Unavailable
                val resolvedShopId = shopId?.takeIf { it.isNotBlank() }
                    ?: return NotificationNavigationTarget.EditOffer(shopId = "", offerId = offerId)
                when (notification.type) {
                    "OFFER_REJECTED", "OFFER_RESUBMITTED" -> NotificationNavigationTarget.EditOffer(resolvedShopId, offerId)
                    else -> NotificationNavigationTarget.ManageOffers(resolvedShopId)
                }
            }
            null -> NotificationNavigationTarget.Unavailable
        }
    }
}
