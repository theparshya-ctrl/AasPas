package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopStatus
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.presentation.shopowner.notifications.FakeNotificationRepository
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopOwnerOnboardingViewModelTest {

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
    fun `empty dashboard status indicates onboarding is needed`() = runTest {
        repository.dashboardResult = Result.Failure(AppError.NotFound)
        val dashboardViewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        dashboardViewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Empty, dashboardViewModel.uiState.value.status)
    }

    @Test
    fun `required field validation blocks submission`() = runTest {
        val viewModel = ShopOwnerOnboardingViewModel(repository)
        viewModel.createShop()
        advanceUntilIdle()

        assertEquals(ShopOwnerOnboardingStatus.Ready, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.validationMessage)
        assertEquals(0, repository.createShopCallCount)
    }

    @Test
    fun `successful create shop calls repository and marks success`() = runTest {
        val viewModel = ShopOwnerOnboardingViewModel(repository)
        fillValidForm(viewModel)
        viewModel.createShop()
        advanceUntilIdle()

        assertEquals(ShopOwnerOnboardingStatus.Success, viewModel.uiState.value.status)
        assertEquals("shop-new", viewModel.uiState.value.createdShopId)
        assertEquals(1, repository.createShopCallCount)
        assertEquals("Fresh Mart", repository.lastCreateShopRequest?.name)
    }

    @Test
    fun `api validation error is shown`() = runTest {
        repository.createShopResult = Result.Failure(AppError.Unexpected("Invalid GPS coordinates"))
        val viewModel = ShopOwnerOnboardingViewModel(repository)
        fillValidForm(viewModel)
        viewModel.createShop()
        advanceUntilIdle()

        assertEquals(ShopOwnerOnboardingStatus.Error, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.error is AppError.Unexpected)
    }

    @Test
    fun `network error is shown`() = runTest {
        repository.createShopResult = Result.Failure(AppError.Server)
        val viewModel = ShopOwnerOnboardingViewModel(repository)
        fillValidForm(viewModel)
        viewModel.createShop()
        advanceUntilIdle()

        assertEquals(ShopOwnerOnboardingStatus.Error, viewModel.uiState.value.status)
        assertEquals(AppError.Server, viewModel.uiState.value.error)
    }

    @Test
    fun `duplicate submit is ignored while submitting`() = runTest {
        repository.createShopResult = Result.Success(
            FakeShopOwnerRepository().sampleDashboard().shop,
        )
        val viewModel = ShopOwnerOnboardingViewModel(repository)
        fillValidForm(viewModel)
        viewModel.createShop()
        viewModel.createShop()
        advanceUntilIdle()

        assertEquals(1, repository.createShopCallCount)
    }

    @Test
    fun `retry after error returns to ready state`() = runTest {
        repository.createShopResult = Result.Failure(AppError.Server)
        val viewModel = ShopOwnerOnboardingViewModel(repository)
        fillValidForm(viewModel)
        viewModel.createShop()
        advanceUntilIdle()
        viewModel.retryAfterError()

        assertEquals(ShopOwnerOnboardingStatus.Ready, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `existing owner with shop stays on loaded dashboard`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard())
        val dashboardViewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        dashboardViewModel.load()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Loaded, dashboardViewModel.uiState.value.status)
        assertEquals(0, repository.createShopCallCount)
    }

    @Test
    fun `submit for verification still works after shop exists`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard())
        val dashboardViewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        dashboardViewModel.load()
        advanceUntilIdle()
        repository.submitShopResult = Result.Success(
            repository.sampleDashboard().shop.copy(status = MerchantShopStatus.PendingApproval),
        )
        dashboardViewModel.submitShopForVerification()
        advanceUntilIdle()

        assertEquals(ShopOwnerLoadStatus.Loaded, dashboardViewModel.uiState.value.status)
    }

    private fun fillValidForm(viewModel: ShopOwnerOnboardingViewModel) {
        viewModel.onNameChange("Fresh Mart")
        viewModel.onCategoryChange("grocery")
        viewModel.onContactNumberChange("+919876543210")
        viewModel.onOpensAtChange("09:00")
        viewModel.onClosesAtChange("21:00")
        viewModel.onAddressLine1Change("123 Main Road")
        viewModel.onCityChange("Mumbai")
        viewModel.onLatitudeChange("19.07609")
        viewModel.onLongitudeChange("72.877426")
    }
}
