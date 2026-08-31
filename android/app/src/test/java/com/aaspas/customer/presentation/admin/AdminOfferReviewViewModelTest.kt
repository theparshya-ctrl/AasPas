package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminOfferMerchant
import com.aaspas.customer.domain.model.AdminOfferReview
import com.aaspas.customer.domain.model.AdminOfferShop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminOfferReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAdminRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAdminRepository()
        repository.reviewResult = Result.Success(sampleReview())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads review`() = runTest {
        val viewModel = AdminOfferReviewViewModel(repository, "offer-1")
        advanceUntilIdle()

        assertEquals(AdminOfferReviewStatus.Loaded, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.review)
    }

    @Test
    fun `reject requires reason`() = runTest {
        val viewModel = AdminOfferReviewViewModel(repository, "offer-1")
        advanceUntilIdle()
        viewModel.rejectOffer()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.validationMessage)
        assertEquals(AdminOfferReviewStatus.Loaded, viewModel.uiState.value.status)
    }

    @Test
    fun `approve transitions to approved`() = runTest {
        val viewModel = AdminOfferReviewViewModel(repository, "offer-1")
        advanceUntilIdle()
        viewModel.approveOffer()
        advanceUntilIdle()

        assertEquals(AdminOfferReviewStatus.Approved, viewModel.uiState.value.status)
    }

    @Test
    fun `reject with reason succeeds`() = runTest {
        val viewModel = AdminOfferReviewViewModel(repository, "offer-1")
        advanceUntilIdle()
        viewModel.onRejectReasonChange("Discount information incorrect")
        viewModel.rejectOffer()
        advanceUntilIdle()

        assertEquals(AdminOfferReviewStatus.Rejected, viewModel.uiState.value.status)
        assertEquals("Discount information incorrect", repository.lastRejectReason)
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
