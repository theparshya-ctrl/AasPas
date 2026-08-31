package com.aaspas.customer.presentation.offerdetails

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.OfferDetails
import com.aaspas.customer.domain.model.OfferDetailsShop
import com.aaspas.customer.domain.model.OfferVisibilityStatus
import com.aaspas.customer.domain.repository.OfferDetailsRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfferDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeOfferDetailsRepository
    private lateinit var viewModel: OfferDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeOfferDetailsRepository()
        viewModel = OfferDetailsViewModel(repository, "offer-1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading then success`() = runTest {
        repository.result = Result.Success(sampleOffer(OfferVisibilityStatus.ACTIVE))
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DetailsLoadStatus.Loaded, state.status)
        assertNotNull(state.offer)
        assertNull(state.error)
    }

    @Test
    fun `not found maps to unavailable state`() = runTest {
        repository.result = Result.Failure(AppError.NotFound)
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DetailsLoadStatus.NotFound, state.status)
        assertNull(state.offer)
    }

    @Test
    fun `network error`() = runTest {
        repository.result = Result.Failure(AppError.NoInternet)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(DetailsLoadStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `coming soon offer loaded`() = runTest {
        repository.result = Result.Success(sampleOffer(OfferVisibilityStatus.COMING_SOON))
        viewModel.load()
        advanceUntilIdle()

        assertEquals(OfferVisibilityStatus.COMING_SOON, viewModel.uiState.value.offer?.status)
    }

    private fun sampleOffer(status: OfferVisibilityStatus) = OfferDetails(
        id = "offer-1",
        title = "Summer Sale",
        description = "Details",
        photoUrl = null,
        discountType = "percentage",
        discountValue = "20",
        startsAt = "2025-08-19T10:00:00Z",
        endsAt = "2025-08-25T18:00:00Z",
        status = status,
        shop = OfferDetailsShop(
            id = "shop-1",
            name = "Fashion Hub",
            photoUrl = null,
            category = "Clothing",
            addressArea = "Pimpri",
            latitude = 18.63,
            longitude = 73.80,
            distanceKm = 1.0,
        ),
    )

    private class FakeOfferDetailsRepository : OfferDetailsRepository {
        var result: Result<OfferDetails> = Result.Failure(AppError.Server)

        override suspend fun getOfferDetails(
            offerId: String,
            latitude: Double?,
            longitude: Double?,
        ): Result<OfferDetails> = result
    }
}
