package com.aaspas.customer.presentation.auth

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
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
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoginStatus.Success, viewModel.uiState.value.status)
        assertTrue(authRepository.isLoggedIn())
    }

    @Test
    fun `login failure shows error`() = runTest {
        authRepository.loginResult = Result.Failure(AppError.Unauthorized)
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoginStatus.Error, viewModel.uiState.value.status)
        assertEquals(AppError.Unauthorized, viewModel.uiState.value.error)
    }

    @Test
    fun `validation failure for short password`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("short")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoginStatus.Error, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("8"))
    }
}
