package com.aaspas.customer.presentation.category

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.CategoryOffersFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.repository.CategoryOffersRepository
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
class CategoryOffersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCategoryOffersRepository
    private lateinit var viewModel: CategoryOffersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCategoryOffersRepository()
        viewModel = CategoryOffersViewModel(repository, "cat-1", "Clothing")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading then success`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.load(18.63, 73.80)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CategoryLoadStatus.Loaded, state.status)
        assertEquals(1, state.todayOffers.size)
        assertTrue(state.locationAvailable)
    }

    @Test
    fun `empty category offers`() = runTest {
        repository.result = Result.Success(
            CategoryOffersFeed(
                categoryId = "cat-1",
                categoryName = "Clothing",
                categorySlug = "clothing",
                todayOffers = emptyList(),
                comingSoon = emptyList(),
                totalActive = 0,
                totalComingSoon = 0,
                page = 1,
                pageSize = 20,
                totalPages = 0,
            ),
        )
        viewModel.load(null, null)
        advanceUntilIdle()

        assertEquals(CategoryLoadStatus.Empty, viewModel.uiState.value.status)
    }

    @Test
    fun `error state`() = runTest {
        repository.result = Result.Failure(AppError.Server)
        viewModel.load(null, null)
        advanceUntilIdle()

        assertEquals(CategoryLoadStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `location unavailable still loads`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.load(null, null)
        advanceUntilIdle()

        assertEquals(CategoryLoadStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(false, viewModel.uiState.value.locationAvailable)
    }

    private fun sampleFeed() = CategoryOffersFeed(
        categoryId = "cat-1",
        categoryName = "Clothing",
        categorySlug = "clothing",
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
                distanceKm = null,
                category = "Clothing",
                isComingSoon = false,
            ),
        ),
        comingSoon = emptyList(),
        totalActive = 1,
        totalComingSoon = 0,
        page = 1,
        pageSize = 20,
        totalPages = 1,
    )

    private class FakeCategoryOffersRepository : CategoryOffersRepository {
        var result: Result<CategoryOffersFeed> = Result.Failure(AppError.Server)

        override suspend fun getCategoryOffers(
            categoryId: String,
            latitude: Double?,
            longitude: Double?,
            page: Int,
        ): Result<CategoryOffersFeed> = result
    }
}
