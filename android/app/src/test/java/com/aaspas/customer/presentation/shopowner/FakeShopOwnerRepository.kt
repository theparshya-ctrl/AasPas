package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantOfferStatus
import com.aaspas.customer.domain.model.MerchantOfferCounts
import com.aaspas.customer.domain.model.MerchantShopLocation
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.MerchantShopStatus
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.domain.repository.ShopOwnerRepository

open class FakeShopOwnerRepository : ShopOwnerRepository {
    var dashboardResult: Result<ShopOwnerDashboard> = Result.Failure(AppError.Server)
    var listOffersResult: Result<List<MerchantOffer>> = Result.Success(emptyList())
    var getOfferResult: Result<MerchantOffer> = Result.Failure(AppError.NotFound)
    var createOfferResult: Result<MerchantOffer>? = null
    var updateOfferResult: Result<MerchantOffer>? = null
    var submitOfferResult: Result<MerchantOffer>? = null

    var lastSubmittedOfferId: String? = null
    var lastMerchantConfirmed: Boolean? = null
    var lastCreatedDraft: MerchantOfferDraft? = null
    var createShopResult: Result<MerchantShopProfile>? = null
    var lastCreateShopRequest: CreateShopRequest? = null
    var createShopCallCount: Int = 0
    var uploadShopPhotoResult: Result<MerchantShopProfile>? = null
    var uploadShopPhotoCallCount: Int = 0
    var uploadOfferPhotoResult: Result<MerchantOffer>? = null
    var uploadOfferPhotoCallCount: Int = 0
    var lastUploadedOfferId: String? = null

    data class CreateShopRequest(
        val name: String,
        val category: String,
        val description: String?,
        val contactNumber: String,
        val photoUrl: String?,
        val businessHours: MerchantBusinessHours,
        val location: MerchantShopLocationDraft,
    )

    override suspend fun getDashboard(): Result<ShopOwnerDashboard> = dashboardResult

    override suspend fun createShop(
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours,
        location: MerchantShopLocationDraft,
    ): Result<MerchantShopProfile> {
        createShopCallCount++
        lastCreateShopRequest = CreateShopRequest(
            name = name,
            category = category,
            description = description,
            contactNumber = contactNumber,
            photoUrl = photoUrl,
            businessHours = businessHours,
            location = location,
        )
        return createShopResult ?: Result.Success(
            MerchantShopProfile(
                id = "shop-new",
                name = name,
                description = description,
                category = category,
                contactNumber = contactNumber,
                businessHours = businessHours,
                photoUrl = photoUrl,
                status = MerchantShopStatus.Draft,
                rejectionReason = null,
                isVerified = false,
                location = MerchantShopLocation(
                    addressLine1 = location.addressLine1,
                    addressLine2 = location.addressLine2.takeIf { it.isNotEmpty() },
                    city = location.city,
                    state = location.state.takeIf { it.isNotEmpty() },
                    postalCode = location.postalCode.takeIf { it.isNotEmpty() },
                    latitude = location.latitude.toDoubleOrNull(),
                    longitude = location.longitude.toDoubleOrNull(),
                ),
            ),
        )
    }

    override suspend fun updateShopProfile(
        shopId: String,
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours?,
        location: MerchantShopLocationDraft,
    ): Result<MerchantShopProfile> = Result.Failure(AppError.Server)

    override suspend fun updateShopPhoto(shopId: String, photoUrl: String?): Result<MerchantShopProfile> {
        return updateShopProfile(
            shopId = shopId,
            name = "",
            category = "",
            description = null,
            contactNumber = "",
            photoUrl = photoUrl,
            businessHours = null,
            location = MerchantShopLocationDraft(),
        )
    }

    override suspend fun uploadShopPhoto(imageBytes: ByteArray): Result<MerchantShopProfile> {
        uploadShopPhotoCallCount++
        uploadShopPhotoResult?.let { return it }
        return Result.Success(
            sampleDashboard().shop.copy(photoUrl = "/media/shops/shop-1/uploaded.jpg"),
        )
    }

    override suspend fun uploadOfferPhoto(offerId: String, imageBytes: ByteArray): Result<MerchantOffer> {
        uploadOfferPhotoCallCount++
        lastUploadedOfferId = offerId
        return uploadOfferPhotoResult ?: Result.Success(
            sampleOffer("shop-1", MerchantOfferStatus.Draft).copy(
                id = offerId,
                photoUrl = "/media/offers/shop-1/$offerId/uploaded.jpg",
            ),
        )
    }

    override suspend fun listOffers(shopId: String): Result<List<MerchantOffer>> = listOffersResult

    override suspend fun getOffer(offerId: String): Result<MerchantOffer> = getOfferResult

    override suspend fun createOffer(draft: MerchantOfferDraft): Result<MerchantOffer> {
        lastCreatedDraft = draft
        return createOfferResult ?: Result.Success(sampleOffer(draft.shopId, MerchantOfferStatus.Draft))
    }

    override suspend fun updateOffer(draft: MerchantOfferDraft): Result<MerchantOffer> {
        return updateOfferResult ?: Result.Success(
            sampleOffer(draft.shopId, MerchantOfferStatus.Draft).copy(
                id = draft.offerId ?: "offer-1",
                title = draft.title,
            ),
        )
    }

    override suspend fun submitOffer(offerId: String, merchantConfirmed: Boolean): Result<MerchantOffer> {
        lastSubmittedOfferId = offerId
        lastMerchantConfirmed = merchantConfirmed
        return submitOfferResult ?: Result.Success(
            sampleOffer("shop-1", MerchantOfferStatus.PendingApproval).copy(id = offerId),
        )
    }

    override suspend fun submitShop(shopId: String): Result<MerchantShopProfile> {
        return submitShopResult ?: Result.Failure(AppError.Server)
    }

    var submitShopResult: Result<MerchantShopProfile>? = null

    fun sampleDashboard(canSubmitOffers: Boolean = false) = ShopOwnerDashboard(
        shop = MerchantShopProfile(
            id = "shop-1",
            name = "Fresh Mart",
            description = null,
            category = "grocery",
            contactNumber = "+919876543210",
            businessHours = MerchantBusinessHours("09:00", "21:00"),
            photoUrl = null,
            status = if (canSubmitOffers) MerchantShopStatus.Active else MerchantShopStatus.Draft,
            rejectionReason = null,
            isVerified = canSubmitOffers,
            location = MerchantShopLocation(
                addressLine1 = "123 Main Road",
                addressLine2 = null,
                city = "Mumbai",
                state = "MH",
                postalCode = "400001",
                latitude = 19.07609,
                longitude = 72.877426,
            ),
        ),
        isVerified = canSubmitOffers,
        canEditProfile = !canSubmitOffers,
        canSubmitOffers = canSubmitOffers,
        offerCounts = MerchantOfferCounts(0, 0, 0, 0, 0, 0),
        statusMessage = null,
    )

    fun sampleOffer(shopId: String, status: MerchantOfferStatus) = MerchantOffer(
        id = "offer-1",
        shopId = shopId,
        title = "10% Off",
        description = "Valid on all items in store today.",
        discountType = "percentage",
        discountValue = "10",
        status = status,
        startsAt = "2026-08-21T09:00:00Z",
        endsAt = "2026-08-21T21:00:00Z",
        photoUrl = null,
        applicableProducts = null,
        minPurchaseAmount = null,
        terms = null,
        rejectionReason = null,
        isVerified = false,
        merchantConfirmedAt = null,
    )
}
