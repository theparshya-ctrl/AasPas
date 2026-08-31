package com.aaspas.customer.presentation.home

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.model.HomeLocation
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.domain.model.FavoritesFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.repository.FavoritesRepository
import com.aaspas.customer.domain.repository.HomeRepository
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.model.NotificationList
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.testsupport.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeHomeRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeHomeRepository()
        viewModel = HomeViewModel(
            repository,
            FakeFavoritesRepository(),
            FakeAuthRepository(),
            FakeNotificationRepository(),
            RealtimeNotificationStore(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading then success with offers`() = runTest {
        repository.result = Result.Success(sampleFeed())

        viewModel.loadHome(18.63, 73.80)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoad)
        assertNull(state.globalError)
        assertEquals(SectionStatus.Loaded, state.todayOffers.status)
        assertEquals(1, state.todayOffers.items.size)
        assertEquals(SectionStatus.Empty, state.comingSoon.status)
        assertTrue(state.locationAvailable)
        assertEquals(LocationLabelState.NearYou, state.locationLabel)
    }

    @Test
    fun `empty sections mapped correctly`() = runTest {
        repository.result = Result.Success(
            HomeFeed(
                location = HomeLocation(null, null, false),
                categories = emptyList(),
                todayOffers = emptyList(),
                comingSoon = emptyList(),
                nearbyShops = emptyList(),
            ),
        )

        viewModel.loadHome(null, null, locationDenied = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SectionStatus.Empty, state.categories.status)
        assertEquals(SectionStatus.Empty, state.todayOffers.status)
        assertEquals(SectionStatus.Empty, state.comingSoon.status)
        assertEquals(SectionStatus.Empty, state.nearbyShops.status)
        assertTrue(state.locationDenied)
    }

    @Test
    fun `api failure sets global error and section errors`() = runTest {
        repository.result = Result.Failure(AppError.NoInternet)

        viewModel.loadHome(null, null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.globalErrorType)
        assertEquals(AppError.NoInternet, state.globalErrorType)
        assertEquals(LocationLabelState.Unavailable, state.locationLabel)
        assertEquals(SectionStatus.Error, state.todayOffers.status)
    }

    @Test
    fun `manual location uses display name and selected source`() = runTest {
        repository.result = Result.Success(sampleFeed())
        repository.lastLatitude = null
        repository.lastLongitude = null

        viewModel.loadHome(
            latitude = 18.6298,
            longitude = 73.7997,
            locationSource = LocationSource.MANUAL,
            displayName = "Pimpri",
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationSource.MANUAL, state.locationSource)
        assertEquals("Pimpri", state.localityName)
        assertEquals(LocationLabelState.Locality, state.locationLabel)
        assertTrue(state.locationAvailable)
        assertEquals(18.6298, repository.lastLatitude!!, 0.0001)
        assertEquals(73.7997, repository.lastLongitude!!, 0.0001)
    }

    @Test
    fun `switching to gps updates source and reload coordinates`() = runTest {
        repository.result = Result.Success(sampleFeed())

        viewModel.loadHome(
            latitude = 18.6298,
            longitude = 73.7997,
            locationSource = LocationSource.MANUAL,
            displayName = "Pimpri",
        )
        advanceUntilIdle()

        viewModel.loadHome(
            latitude = 18.6278,
            longitude = 73.7911,
            locationSource = LocationSource.CURRENT_GPS,
            displayName = "Chinchwad",
            isRefresh = true,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationSource.CURRENT_GPS, state.locationSource)
        assertEquals("Chinchwad", state.localityName)
        assertEquals(18.6278, repository.lastLatitude!!, 0.0001)
    }

    @Test
    fun `permission denied keeps manual coordinates on retry`() = runTest {
        repository.result = Result.Success(sampleFeed())

        viewModel.loadHome(
            latitude = 18.6298,
            longitude = 73.7997,
            locationSource = LocationSource.MANUAL,
            displayName = "Pimpri",
            locationDenied = false,
        )
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(18.6298, repository.lastLatitude!!, 0.0001)
        assertEquals(LocationSource.MANUAL, viewModel.uiState.value.locationSource)
    }

    @Test
    fun `coming soon offer flagged in domain feed`() = runTest {
        repository.result = Result.Success(
            sampleFeed(
                comingSoon = listOf(
                    Offer(
                        id = "soon-1",
                        title = "Festive Preview",
                        description = null,
                        photoUrl = null,
                        discountType = "percentage",
                        discountValue = "15",
                        startsAt = "2025-08-22T10:00:00Z",
                        endsAt = "2025-08-30T18:00:00Z",
                        shopId = "shop-1",
                        shopName = "Fashion Hub",
                        distanceKm = null,
                        category = "Clothing",
                        isComingSoon = true,
                    ),
                ),
            ),
        )

        viewModel.loadHome(18.63, 73.80)
        advanceUntilIdle()

        val comingSoon = viewModel.uiState.value.comingSoon.items.first()
        assertTrue(comingSoon.isComingSoon)
        assertEquals("Festive Preview", comingSoon.title)
    }

    private fun sampleFeed(
        comingSoon: List<Offer> = emptyList(),
    ) = HomeFeed(
        location = HomeLocation(18.63, 73.80, true),
        categories = listOf(Category("cat-1", "Clothing", "clothing", 1)),
        todayOffers = listOf(
            Offer(
                id = "offer-1",
                title = "Today Deal",
                description = null,
                photoUrl = null,
                discountType = "percentage",
                discountValue = "10",
                startsAt = "2025-08-19T10:00:00Z",
                endsAt = "2025-08-25T18:00:00Z",
                shopId = "shop-1",
                shopName = "Fashion Hub",
                distanceKm = 1.0,
                category = "Clothing",
                isComingSoon = false,
            ),
        ),
        comingSoon = comingSoon,
        nearbyShops = emptyList(),
    )

    private class FakeHomeRepository : HomeRepository {
        var result: Result<HomeFeed> = Result.Failure(AppError.Server)
        var lastLatitude: Double? = null
        var lastLongitude: Double? = null

        override suspend fun getHome(latitude: Double?, longitude: Double?): Result<HomeFeed> {
            lastLatitude = latitude
            lastLongitude = longitude
            return result
        }
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        override suspend fun listFavorites(latitude: Double?, longitude: Double?) =
            Result.Success(FavoritesFeed(emptyList(), emptyList()))

        override suspend fun saveOffer(offerId: String) = Result.Success(true)
        override suspend fun unsaveOffer(offerId: String) = Result.Success(false)
        override suspend fun saveShop(shopId: String) = Result.Success(true)
        override suspend fun unsaveShop(shopId: String) = Result.Success(false)
    }

    private class FakeNotificationRepository : NotificationRepository {
        override suspend fun listNotifications(unreadOnly: Boolean) =
            Result.Success(NotificationList(emptyList(), 0))

        override suspend fun markRead(notificationId: String) =
            Result.Failure(AppError.NotFound)

        override suspend fun markAllRead(): Result<Int> = Result.Success(0)
    }
}
