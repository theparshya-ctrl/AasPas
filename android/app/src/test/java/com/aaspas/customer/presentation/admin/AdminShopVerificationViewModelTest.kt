package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminShopVerificationViewModelTest {

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
    fun `loads pending shops`() = runTest {
        repository.pendingShops = Result.Success(listOf(sampleShop()))
        val viewModel = AdminShopVerificationViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminShopVerificationStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.shops.size)
    }

    @Test
    fun `error when unauthorized`() = runTest {
        repository.pendingShops = Result.Failure(AppError.Unauthorized)
        val viewModel = AdminShopVerificationViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminShopVerificationStatus.Error, viewModel.uiState.value.status)
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
