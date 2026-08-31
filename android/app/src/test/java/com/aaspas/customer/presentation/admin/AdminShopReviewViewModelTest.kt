package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminShopOwner
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.domain.model.MerchantBusinessHours
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
class AdminShopReviewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAdminRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAdminRepository()
        repository.shopReviewResult = Result.Success(sampleShop())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads shop review`() = runTest {
        val viewModel = AdminShopReviewViewModel(repository, "shop-1")
        advanceUntilIdle()

        assertEquals(AdminShopReviewStatus.Loaded, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.review)
    }

    @Test
    fun `reject requires reason`() = runTest {
        val viewModel = AdminShopReviewViewModel(repository, "shop-1")
        advanceUntilIdle()
        viewModel.rejectShop()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.validationMessage)
        assertEquals(AdminShopReviewStatus.Loaded, viewModel.uiState.value.status)
    }

    @Test
    fun `approve transitions to approved`() = runTest {
        val viewModel = AdminShopReviewViewModel(repository, "shop-1")
        advanceUntilIdle()
        viewModel.approveShop()
        advanceUntilIdle()

        assertEquals(AdminShopReviewStatus.Approved, viewModel.uiState.value.status)
    }

    @Test
    fun `reject with reason succeeds`() = runTest {
        val viewModel = AdminShopReviewViewModel(repository, "shop-1")
        advanceUntilIdle()
        viewModel.onRejectReasonChange("Address unclear")
        viewModel.rejectShop()
        advanceUntilIdle()

        assertEquals(AdminShopReviewStatus.Rejected, viewModel.uiState.value.status)
        assertEquals("Address unclear", repository.lastShopRejectReason)
    }

    private fun sampleShop() = AdminShopReview(
        shopId = "shop-1",
        shopName = "Fresh Mart",
        description = "Neighborhood grocery",
        category = "grocery",
        photoUrl = null,
        contactNumber = "+919876543210",
        businessHours = MerchantBusinessHours("09:00", "21:00"),
        status = "pending_approval",
        submittedAt = "2026-08-20T10:00:00Z",
        approvedAt = null,
        rejectionReason = null,
        isVerified = false,
        addressLine1 = "123 Main Road",
        addressLine2 = null,
        area = "Mumbai, MH",
        city = "Mumbai",
        pincode = "400001",
        latitude = 19.07609,
        longitude = 72.877426,
        owner = AdminShopOwner("user-1", "Owner", "owner@example.com", "+919876543210"),
    )
}
