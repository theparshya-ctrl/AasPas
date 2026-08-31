package com.aaspas.customer.presentation.shopowner.notifications

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationList
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferCounts
import com.aaspas.customer.domain.model.MerchantOfferStatus
import com.aaspas.customer.domain.model.MerchantShopLocation
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.MerchantShopStatus
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import com.aaspas.customer.presentation.shopowner.FakeShopOwnerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopOwnerNotificationsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var notificationRepository: FakeNotificationRepository
    private lateinit var shopOwnerRepository: FakeShopOwnerRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        notificationRepository = FakeNotificationRepository()
        shopOwnerRepository = FakeShopOwnerRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads notifications`() = runTest {
        notificationRepository.listResult = Result.Success(sampleList())
        val viewModel = ShopOwnerNotificationsViewModel(notificationRepository, shopOwnerRepository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(NotificationsLoadStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.notifications.size)
    }

    @Test
    fun `empty state when no notifications`() = runTest {
        notificationRepository.listResult = Result.Success(NotificationList(emptyList(), 0))
        val viewModel = ShopOwnerNotificationsViewModel(notificationRepository, shopOwnerRepository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(NotificationsLoadStatus.Empty, viewModel.uiState.value.status)
    }

    @Test
    fun `error state on failure`() = runTest {
        notificationRepository.listResult = Result.Failure(AppError.Server)
        val viewModel = ShopOwnerNotificationsViewModel(notificationRepository, shopOwnerRepository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(NotificationsLoadStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `click marks notification read and navigates`() = runTest {
        notificationRepository.listResult = Result.Success(sampleList())
        notificationRepository.markReadResult = Result.Success(sampleNotification(isRead = true))
        shopOwnerRepository.getOfferResult = Result.Success(sampleOffer())
        shopOwnerRepository.dashboardResult = Result.Success(shopOwnerRepository.sampleDashboard(true))

        val viewModel = ShopOwnerNotificationsViewModel(notificationRepository, shopOwnerRepository)
        viewModel.load()
        advanceUntilIdle()
        viewModel.onNotificationClick(sampleNotification(isRead = false))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.first().isRead)
        assertEquals(
            NotificationNavigationTarget.EditOffer("shop-1", "offer-1"),
            viewModel.uiState.value.navigationTarget,
        )
    }

    private fun sampleList() = NotificationList(
        notifications = listOf(sampleNotification(isRead = false)),
        unreadCount = 1,
    )

    private fun sampleNotification(isRead: Boolean) = AppNotification(
        id = "n1",
        type = "OFFER_REJECTED",
        audience = "business",
        title = "Offer rejected",
        message = "Reason: unclear terms",
        entityType = "offer",
        entityId = "offer-1",
        isRead = isRead,
        createdAt = "2026-08-26T10:32:00Z",
    )

    private fun sampleOffer() = MerchantOffer(
        id = "offer-1",
        shopId = "shop-1",
        title = "Summer Sale",
        description = "Valid offer",
        discountType = "percentage",
        discountValue = "10",
        status = MerchantOfferStatus.Rejected,
        startsAt = "2026-08-21T09:00:00Z",
        endsAt = "2026-08-21T21:00:00Z",
        photoUrl = null,
        applicableProducts = null,
        minPurchaseAmount = null,
        terms = null,
        rejectionReason = "unclear terms",
        isVerified = false,
        merchantConfirmedAt = null,
    )
}

class FakeNotificationRepository : NotificationRepository {
    var listResult: Result<NotificationList> = Result.Failure(AppError.Server)
    var markReadResult: Result<AppNotification> = Result.Failure(AppError.Server)

    override suspend fun listNotifications(unreadOnly: Boolean): Result<NotificationList> = listResult

    override suspend fun markRead(notificationId: String): Result<AppNotification> = markReadResult

    override suspend fun markAllRead(): Result<Int> = Result.Success(0)
}
