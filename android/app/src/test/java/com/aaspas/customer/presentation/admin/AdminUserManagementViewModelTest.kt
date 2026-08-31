package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminUserListItem
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
class AdminUserManagementViewModelTest {

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
    fun `loads users`() = runTest {
        repository.usersResult = Result.Success(listOf(sampleUser()))
        val viewModel = AdminUserManagementViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminUserManagementStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.users.size)
    }

    @Test
    fun `search query updates state`() = runTest {
        repository.usersResult = Result.Success(emptyList())
        val viewModel = AdminUserManagementViewModel(repository)
        viewModel.onSearchQueryChange("owner@example.com")
        viewModel.submitSearch()
        advanceUntilIdle()

        assertEquals("owner@example.com", viewModel.uiState.value.searchQuery)
        assertEquals(AdminUserManagementStatus.Loaded, viewModel.uiState.value.status)
    }

    @Test
    fun `error when unauthorized`() = runTest {
        repository.usersResult = Result.Failure(AppError.Unauthorized)
        val viewModel = AdminUserManagementViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(AdminUserManagementStatus.Error, viewModel.uiState.value.status)
    }

    private fun sampleUser() = AdminUserListItem(
        userId = "user-1",
        fullName = "Customer One",
        email = "customer@example.com",
        role = "customer",
        isActive = true,
        shopId = null,
        shopName = null,
        createdAt = "2026-08-20T10:00:00Z",
    )
}
