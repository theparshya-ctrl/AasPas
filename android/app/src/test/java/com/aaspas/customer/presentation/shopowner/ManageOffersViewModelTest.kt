package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.MerchantOfferStatus
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
class ManageOffersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeShopOwnerRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeShopOwnerRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads offers for shop`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = true))
        repository.listOffersResult = Result.Success(
            listOf(
                repository.sampleOffer("shop-1", MerchantOfferStatus.Draft),
                repository.sampleOffer("shop-1", MerchantOfferStatus.PendingApproval).copy(id = "offer-2"),
            ),
        )
        val viewModel = ManageOffersViewModel(repository, "shop-1")
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ManageOffersStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(2, viewModel.uiState.value.offers.size)
        assertEquals("Fresh Mart", viewModel.uiState.value.shopName)
    }

    @Test
    fun `sorts draft before active offers`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = true))
        repository.listOffersResult = Result.Success(
            listOf(
                repository.sampleOffer("shop-1", MerchantOfferStatus.Active).copy(id = "offer-active"),
                repository.sampleOffer("shop-1", MerchantOfferStatus.Draft).copy(id = "offer-draft"),
            ),
        )
        val viewModel = ManageOffersViewModel(repository, "shop-1")
        viewModel.load()
        advanceUntilIdle()

        assertEquals(MerchantOfferStatus.Draft, viewModel.uiState.value.offers.first().status)
    }

    @Test
    fun `maps pending and rejected states`() = runTest {
        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = true))
        repository.listOffersResult = Result.Success(
            listOf(
                repository.sampleOffer("shop-1", MerchantOfferStatus.PendingApproval),
                repository.sampleOffer("shop-1", MerchantOfferStatus.Rejected).copy(
                    id = "offer-2",
                    rejectionReason = "Missing photo",
                ),
            ),
        )
        val viewModel = ManageOffersViewModel(repository, "shop-1")
        viewModel.load()
        advanceUntilIdle()

        val statuses = viewModel.uiState.value.offers.map { it.status }
        assertTrue(statuses.contains(MerchantOfferStatus.PendingApproval))
        assertTrue(statuses.contains(MerchantOfferStatus.Rejected))
    }

    @Test
    fun `error state on failure`() = runTest {
        repository.listOffersResult = Result.Failure(AppError.Server)
        val viewModel = ManageOffersViewModel(repository, "shop-1")
        viewModel.load()
        advanceUntilIdle()

        assertEquals(ManageOffersStatus.Error, viewModel.uiState.value.status)
    }
}
