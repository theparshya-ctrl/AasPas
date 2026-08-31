package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.ShopOwnerDashboard

interface ShopOwnerRepository {
    suspend fun getDashboard(): Result<ShopOwnerDashboard>
    suspend fun createShop(
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours,
        location: MerchantShopLocationDraft,
    ): Result<MerchantShopProfile>
    suspend fun updateShopProfile(
        shopId: String,
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours?,
        location: MerchantShopLocationDraft,
    ): Result<MerchantShopProfile>

    suspend fun updateShopPhoto(shopId: String, photoUrl: String?): Result<MerchantShopProfile>

    suspend fun uploadShopPhoto(imageBytes: ByteArray): Result<MerchantShopProfile>

    suspend fun uploadOfferPhoto(offerId: String, imageBytes: ByteArray): Result<MerchantOffer>

    suspend fun listOffers(shopId: String): Result<List<MerchantOffer>>
    suspend fun getOffer(offerId: String): Result<MerchantOffer>
    suspend fun createOffer(draft: MerchantOfferDraft): Result<MerchantOffer>
    suspend fun updateOffer(draft: MerchantOfferDraft): Result<MerchantOffer>
    suspend fun submitOffer(offerId: String, merchantConfirmed: Boolean): Result<MerchantOffer>
    suspend fun submitShop(shopId: String): Result<MerchantShopProfile>
}
