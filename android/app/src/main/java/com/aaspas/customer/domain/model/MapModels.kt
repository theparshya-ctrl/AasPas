package com.aaspas.customer.domain.model

data class MapShopPin(
    val shop: Shop,
    val offers: List<Offer>,
    val featuredOffer: Offer?,
    val activeOfferCount: Int,
)
