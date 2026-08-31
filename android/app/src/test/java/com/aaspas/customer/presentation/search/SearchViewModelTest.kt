package com.aaspas.customer.presentation.search

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.FavoritesFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.SearchResults
import com.aaspas.customer.testsupport.FakeAuthRepository
import com.aaspas.customer.domain.repository.FavoritesRepository
import com.aaspas.customer.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeSearchRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeSearchRepository()
        viewModel = SearchViewModel(repository, FakeFavoritesRepository(), FakeAuthRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `debounced search success`() = runTest {
        repository.searchResult = Result.Success(sampleResults())
        viewModel.onQueryChange("shirt")
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchLoadStatus.Loaded, state.status)
        assertEquals(1, state.offers.size)
    }

    @Test
    fun `short query without category stays idle`() = runTest {
        viewModel.onQueryChange("s")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(SearchLoadStatus.Idle, viewModel.uiState.value.status)
        assertEquals(0, repository.searchCallCount)
    }

    @Test
    fun `category filter triggers search`() = runTest {
        repository.searchResult = Result.Success(sampleResults())
        viewModel.onCategorySelected("cat-1")
        advanceUntilIdle()

        assertEquals(SearchLoadStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, repository.searchCallCount)
    }

    @Test
    fun `empty results`() = runTest {
        repository.searchResult = Result.Success(SearchResults(emptyList(), 0, 1, 20, 0))
        viewModel.onQueryChange("nothing")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(SearchLoadStatus.Empty, viewModel.uiState.value.status)
    }

    @Test
    fun `error state`() = runTest {
        repository.searchResult = Result.Failure(AppError.NoInternet)
        viewModel.onQueryChange("shirt")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(SearchLoadStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `location available flag`() = runTest {
        viewModel.setLocation(18.63, 73.80)
        assertTrue(viewModel.uiState.value.locationAvailable)
    }

    @Test
    fun `location change reloads active search`() = runTest {
        repository.searchResult = Result.Success(sampleResults())
        viewModel.onQueryChange("shirt")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(1, repository.searchCallCount)

        viewModel.setLocation(18.63, 73.80)
        advanceUntilIdle()

        assertEquals(2, repository.searchCallCount)
        assertEquals(18.63, repository.lastLatitude)
        assertEquals(73.80, repository.lastLongitude)
    }

    private fun sampleResults() = SearchResults(
        offers = listOf(
            Offer(
                id = "offer-1",
                title = "Shirt Sale",
                description = null,
                photoUrl = null,
                discountType = "percentage",
                discountValue = "20",
                startsAt = "2025-08-19T10:00:00Z",
                endsAt = "2025-08-25T18:00:00Z",
                shopId = "shop-1",
                shopName = "Fashion Hub",
                distanceKm = 1.0,
                category = "Clothing",
                isComingSoon = false,
            ),
        ),
        total = 1,
        page = 1,
        pageSize = 20,
        totalPages = 1,
    )

    private class FakeSearchRepository : SearchRepository {
        var searchResult: Result<SearchResults> = Result.Failure(AppError.Server)
        var searchCallCount = 0
        var lastLatitude: Double? = null
        var lastLongitude: Double? = null

        override suspend fun search(
            query: String?,
            categoryId: String?,
            latitude: Double?,
            longitude: Double?,
            page: Int,
        ): Result<SearchResults> {
            searchCallCount += 1
            lastLatitude = latitude
            lastLongitude = longitude
            return searchResult
        }

        override suspend fun listCategories(): Result<List<Category>> {
            return Result.Success(emptyList())
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
}
