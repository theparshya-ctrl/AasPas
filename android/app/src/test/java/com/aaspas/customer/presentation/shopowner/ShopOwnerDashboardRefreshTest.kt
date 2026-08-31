package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.model.NotificationList
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
class ShopOwnerDashboardRefreshTest {

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
    fun `showShopCreatedSuccess stores message on dashboard`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard())
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.showShopCreatedSuccess("Shop created successfully.")
        viewModel.load(preserveCreatedMessage = true)
        advanceUntilIdle()

        assertEquals("Shop created successfully.", viewModel.uiState.value.createdSuccessMessage)
        assertEquals(ShopOwnerLoadStatus.Loaded, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.dashboard)
    }

    @Test
    fun `refreshNotifications updates unread count`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard())
        notificationRepository.listResult = Result.Success(NotificationList(emptyList(), unreadCount = 2))
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()
        notificationRepository.listResult = Result.Success(NotificationList(emptyList(), unreadCount = 5))
        viewModel.refreshNotifications()
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.unreadNotificationCount)
    }

    @Test
    fun `load after shop creation does not stay empty`() = runTest {
        repository.dashboardResult = Result.Failure(AppError.NotFound)
        val viewModel = ShopOwnerDashboardViewModel(repository, notificationRepository, RealtimeNotificationStore())
        viewModel.load()
        advanceUntilIdle()
        assertEquals(ShopOwnerLoadStatus.Empty, viewModel.uiState.value.status)

        repository.dashboardResult = Result.Success(repository.sampleDashboard())
        viewModel.load(preserveCreatedMessage = true)
        advanceUntilIdle()
        assertEquals(ShopOwnerLoadStatus.Loaded, viewModel.uiState.value.status)
    }
}
