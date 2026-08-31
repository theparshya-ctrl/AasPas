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
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository()
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `registration success uses customer role by default`() = runTest {
        viewModel.onFullNameChange("Alice")
        viewModel.onEmailChange("alice@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Success, viewModel.uiState.value.status)
        assertEquals(com.aaspas.customer.core.auth.UserRoles.CUSTOMER, authRepository.lastRegisterRole)
        assertTrue(authRepository.isLoggedIn())
    }

    @Test
    fun `password mismatch validation`() = runTest {
        viewModel.onEmailChange("alice@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("different123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.ValidationError, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("match"))
    }

    @Test
    fun `registration server error`() = runTest {
        authRepository.registerResult = Result.Failure(AppError.Unexpected("Email already registered"))
        viewModel.onEmailChange("alice@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Error, viewModel.uiState.value.status)
    }
}
