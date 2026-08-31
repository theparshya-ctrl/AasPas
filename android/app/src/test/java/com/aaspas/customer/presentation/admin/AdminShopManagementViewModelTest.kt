package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminShopListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminShopManagementViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAdminRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAdminRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads shops for management`() = runTest {
        repository.shopsResult = Result.Success(listOf(sampleShop()))
        val viewModel = AdminShopManagementViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminShopManagementStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.shops.size)
    }

    @Test
    fun `filter change reloads shops`() = runTest {
        repository.shopsResult = Result.Success(emptyList())
        val viewModel = AdminShopManagementViewModel(repository)
        viewModel.onFilterSelected(AdminShopFilter.ACTIVE)
        advanceUntilIdle()

        assertEquals(AdminShopFilter.ACTIVE, viewModel.uiState.value.filter)
        assertEquals(AdminShopManagementStatus.Loaded, viewModel.uiState.value.status)
    }

    @Test
    fun `error when unauthorized`() = runTest {
        repository.shopsResult = Result.Failure(AppError.Unauthorized)
        val viewModel = AdminShopManagementViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminShopManagementStatus.Error, viewModel.uiState.value.status)
    }

    private fun sampleShop() = AdminShopListItem(
        shopId = "shop-1",
        shopName = "Fresh Mart",
        ownerName = "Owner",
        ownerEmail = "owner@example.com",
        category = "grocery",
        city = "Mumbai",
        status = "active",
        isVerified = true,
        createdAt = "2026-08-20T10:00:00Z",
        offerCount = 2,
        rejectionReason = null,
    )
}
