package com.aaspas.customer.presentation.favorites

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.FavoriteOffer
import com.aaspas.customer.domain.model.FavoritesFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.testsupport.FakeAuthRepository
import com.aaspas.customer.domain.repository.FavoritesRepository
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
class FavoritesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var favoritesRepository: FakeFavoritesRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: FavoritesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        favoritesRepository = FakeFavoritesRepository()
        authRepository = FakeAuthRepository()
        viewModel = FavoritesViewModel(favoritesRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unauthenticated shows unauthorized`() = runTest {
        authRepository = FakeAuthRepository(loggedIn = false)
        viewModel = FavoritesViewModel(favoritesRepository, authRepository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(FavoritesLoadStatus.Unauthorized, viewModel.uiState.value.status)
    }

    @Test
    fun `loading then success`() = runTest {
        authRepository = FakeAuthRepository(loggedIn = true)
        viewModel = FavoritesViewModel(favoritesRepository, authRepository)
        favoritesRepository.feed = Result.Success(sampleFeed())
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FavoritesLoadStatus.Loaded, state.status)
        assertEquals(1, state.offers.size)
    }

    @Test
    fun `empty favorites`() = runTest {
        authRepository = FakeAuthRepository(loggedIn = true)
        viewModel = FavoritesViewModel(favoritesRepository, authRepository)
        favoritesRepository.feed = Result.Success(FavoritesFeed(emptyList(), emptyList()))
        viewModel.load()
        advanceUntilIdle()

        assertEquals(FavoritesLoadStatus.Empty, viewModel.uiState.value.status)
    }

    @Test
    fun `error state`() = runTest {
        authRepository = FakeAuthRepository(loggedIn = true)
        viewModel = FavoritesViewModel(favoritesRepository, authRepository)
        favoritesRepository.feed = Result.Failure(AppError.NoInternet)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(FavoritesLoadStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `unsave offer reloads list`() = runTest {
        authRepository = FakeAuthRepository(loggedIn = true)
        viewModel = FavoritesViewModel(favoritesRepository, authRepository)
        favoritesRepository.feed = Result.Success(sampleFeed())
        viewModel.load()
        advanceUntilIdle()
        viewModel.unsaveOffer("offer-1")
        advanceUntilIdle()

        assertTrue(favoritesRepository.unsaveOfferCalled)
        assertEquals(2, favoritesRepository.listCallCount)
    }

    private fun sampleFeed() = FavoritesFeed(
        offers = listOf(
            FavoriteOffer(
                favoriteId = "fav-1",
                offer = Offer(
                    id = "offer-1",
                    title = "Deal",
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
                    isSaved = true,
                ),
                isActive = true,
                savedAt = "2025-08-19T10:00:00Z",
            ),
        ),
        shops = emptyList(),
    )

    private class FakeFavoritesRepository : FavoritesRepository {
        var feed: Result<FavoritesFeed> = Result.Failure(AppError.Server)
        var listCallCount = 0
        var unsaveOfferCalled = false

        override suspend fun listFavorites(latitude: Double?, longitude: Double?): Result<FavoritesFeed> {
            listCallCount += 1
            return feed
        }

        override suspend fun saveOffer(offerId: String) = Result.Success(true)
        override suspend fun unsaveOffer(offerId: String): Result<Boolean> {
            unsaveOfferCalled = true
            return Result.Success(false)
        }
        override suspend fun saveShop(shopId: String) = Result.Success(true)
        override suspend fun unsaveShop(shopId: String) = Result.Success(false)
    }
}
