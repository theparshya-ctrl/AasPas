package com.aaspas.customer.presentation.shopdetails

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.ShopDetails
import com.aaspas.customer.domain.repository.ShopDetailsRepository
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
class ShopDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeShopDetailsRepository
    private lateinit var viewModel: ShopDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeShopDetailsRepository()
        viewModel = ShopDetailsViewModel(repository, "shop-1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading then success`() = runTest {
        repository.result = Result.Success(sampleShop())
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DetailsLoadStatus.Loaded, state.status)
        assertNotNull(state.shop)
    }

    @Test
    fun `not found state`() = runTest {
        repository.result = Result.Failure(AppError.NotFound)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(DetailsLoadStatus.NotFound, viewModel.uiState.value.status)
    }

    @Test
    fun `error state`() = runTest {
        repository.result = Result.Failure(AppError.Server)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(DetailsLoadStatus.Error, viewModel.uiState.value.status)
    }

    private fun sampleShop() = ShopDetails(
        id = "shop-1",
        name = "Fashion Hub",
        description = null,
        photoUrl = null,
        category = "Clothing",
        addressLine1 = "Market Road",
        addressLine2 = null,
        area = "Pimpri",
        city = "Pimpri",
        pincode = "411018",
        latitude = 18.63,
        longitude = 73.80,
        phone = "+919876543210",
        businessHours = null,
        isVerified = true,
        activeOfferCount = 0,
        distanceKm = 1.5,
        todayOffers = emptyList(),
        comingSoon = emptyList(),
    )

    private class FakeShopDetailsRepository : ShopDetailsRepository {
        var result: Result<ShopDetails> = Result.Failure(AppError.Server)

        override suspend fun getShopDetails(
            shopId: String,
            latitude: Double?,
            longitude: Double?,
        ): Result<ShopDetails> = result
    }
}
