package com.aaspas.customer.presentation.profile

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.UserAccount
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository(loggedIn = true)
        authRepository.currentUserResult = Result.Success(sampleUser())
        viewModel = ProfileViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads profile when logged in`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(ProfileLoadStatus.Loaded, state.status)
        assertEquals("alice@example.com", state.user?.email)
    }

    @Test
    fun `logout clears profile state`() = runTest {
        advanceUntilIdle()
        viewModel.logout()
        advanceUntilIdle()

        assertTrue(authRepository.logoutCalled)
        assertEquals(ProfileLoadStatus.Unauthorized, viewModel.uiState.value.status)
    }

    @Test
    fun `unauthorized profile load`() = runTest {
        authRepository.currentUserResult = Result.Failure(AppError.Unauthorized)
        viewModel.loadProfile()
        advanceUntilIdle()

        assertEquals(ProfileLoadStatus.Unauthorized, viewModel.uiState.value.status)
    }

    private fun sampleUser() = UserAccount(
        id = "user-1",
        email = "alice@example.com",
        fullName = "Alice",
        role = "customer",
        createdAt = "2025-08-19T10:00:00Z",
    )
}
