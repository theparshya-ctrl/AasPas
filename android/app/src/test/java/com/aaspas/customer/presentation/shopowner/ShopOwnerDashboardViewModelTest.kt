package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantOfferCounts
import com.aaspas.customer.domain.model.MerchantShopLocation
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.MerchantShopStatus
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.model.NotificationList
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.presentation.shopowner.notifications.FakeNotificationRepository
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
class ShopOwnerDashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeShopOwnerRepository
    private lateinit var notificationRepository: FakeNotificationRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeShopOwnerRepository()
        notificationRepository = FakeNotificationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads dashboard`() = runTest {
        repository.dashboardResult = Result.Success(sampleDashboard())
        notificationRepository.listResult = Result.Success(NotificationList(emptyList(), 3))
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Loaded, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.dashboard)
        assertEquals(3, viewModel.uiState.value.unreadNotificationCount)
    }

    @Test
    fun `loads dashboard without notifications`() = runTest {
        repository.dashboardResult = Result.Success(sampleDashboard())
        notificationRepository.listResult = Result.Failure(AppError.Server)
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Loaded, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.dashboard)
        assertEquals(0, viewModel.uiState.value.unreadNotificationCount)
    }

    @Test
    fun `empty when no shop`() = runTest {
        repository.dashboardResult = Result.Failure(AppError.NotFound)
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Empty, viewModel.uiState.value.status)
    }

    @Test
    fun `unauthorized state`() = runTest {
        repository.dashboardResult = Result.Failure(AppError.Unauthorized)
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Unauthorized, viewModel.uiState.value.status)
    }

    @Test
    fun `error state`() = runTest {
        repository.dashboardResult = Result.Failure(AppError.Server)
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `retry reloads dashboard`() = runTest {
        repository.dashboardResult = Result.Failure(AppError.Server)
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()
        repository.dashboardResult = Result.Success(sampleDashboard())
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Loaded, viewModel.uiState.value.status)
    }

    private fun sampleDashboard() = ShopOwnerDashboard(
        shop = MerchantShopProfile(
            id = "shop-1",
            name = "Fresh Mart",
            description = null,
            category = "grocery",
            contactNumber = "+919876543210",
            businessHours = MerchantBusinessHours("09:00", "21:00"),
            photoUrl = null,
            status = MerchantShopStatus.Draft,
            rejectionReason = null,
            isVerified = false,
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
        isVerified = false,
        canEditProfile = true,
        canSubmitOffers = false,
        offerCounts = MerchantOfferCounts(draft = 2, pendingApproval = 0, rejected = 0, scheduled = 1, active = 3, expired = 0),
        statusMessage = "Complete your shop profile and submit for approval.",
    )
}
