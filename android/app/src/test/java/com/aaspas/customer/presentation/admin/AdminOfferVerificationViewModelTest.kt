package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminOfferMerchant
import com.aaspas.customer.domain.model.AdminOfferReview
import com.aaspas.customer.domain.model.AdminOfferShop
import com.aaspas.customer.domain.model.AdminShopDetail
import com.aaspas.customer.domain.model.AdminShopListItem
import com.aaspas.customer.domain.model.AdminShopOwner
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.domain.model.AdminUserListItem
import com.aaspas.customer.domain.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminOfferVerificationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAdminRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAdminRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads pending offers`() = runTest {
        repository.pendingOffers = Result.Success(listOf(sampleReview()))
        val viewModel = AdminOfferVerificationViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminVerificationStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.offers.size)
    }

    @Test
    fun `error when unauthorized`() = runTest {
        repository.pendingOffers = Result.Failure(AppError.Unauthorized)
        val viewModel = AdminOfferVerificationViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminVerificationStatus.Error, viewModel.uiState.value.status)
    }

    private fun sampleReview() = AdminOfferReview(
        id = "offer-1",
        shopId = "shop-1",
        title = "Weekend Sale",
        description = "Valid on all items",
        discountType = "percentage",
        discountValue = "10",
        status = "pending_approval",
        startsAt = "2026-08-21T09:00:00Z",
        endsAt = "2026-08-21T21:00:00Z",
        photoUrl = null,
        applicableProducts = null,
        minPurchaseAmount = null,
        terms = null,
        merchantConfirmedAt = "2026-08-20T10:00:00Z",
        submittedAt = "2026-08-20T10:00:00Z",
        isVerified = false,
        shop = AdminOfferShop("shop-1", "Fresh Mart", "grocery", "active", true, null),
        merchant = AdminOfferMerchant("user-1", "Owner", "owner@example.com"),
    )
}

class FakeAdminRepository : AdminRepository {
    var pendingOffers: Result<List<AdminOfferReview>> = Result.Success(emptyList())
    var pendingShops: Result<List<AdminShopReview>> = Result.Success(emptyList())
    var reviewResult: Result<AdminOfferReview> = Result.Failure(AppError.NotFound)
    var shopReviewResult: Result<AdminShopReview> = Result.Failure(AppError.NotFound)
    var approveResult: Result<Unit> = Result.Success(Unit)
    var approveShopResult: Result<Unit> = Result.Success(Unit)
    var rejectResult: Result<Unit> = Result.Success(Unit)
    var rejectShopResult: Result<Unit> = Result.Success(Unit)
    var shopsResult: Result<List<AdminShopListItem>> = Result.Success(emptyList())
    var usersResult: Result<List<AdminUserListItem>> = Result.Success(emptyList())
    var shopDetailResult: Result<AdminShopDetail> = Result.Failure(AppError.NotFound)
    var lastRejectReason: String? = null
    var lastShopRejectReason: String? = null

    override suspend fun getDashboard() = Result.Failure(AppError.Server)

    override suspend fun listPendingOffers() = pendingOffers

    override suspend fun getOfferReview(offerId: String) = reviewResult

    override suspend fun approveOffer(offerId: String) = approveResult

    override suspend fun rejectOffer(offerId: String, reason: String): Result<Unit> {
        lastRejectReason = reason
        return rejectResult
    }

    override suspend fun listPendingShops() = pendingShops

    override suspend fun getShopReview(shopId: String) = shopReviewResult

    override suspend fun approveShop(shopId: String) = approveShopResult

    override suspend fun rejectShop(shopId: String, reason: String): Result<Unit> {
        lastShopRejectReason = reason
        return rejectShopResult
    }

    override suspend fun listShops(status: String?, search: String?) = shopsResult

    override suspend fun getShopDetail(shopId: String) = shopDetailResult

    override suspend fun listUsers(role: String?, search: String?) = usersResult
}
