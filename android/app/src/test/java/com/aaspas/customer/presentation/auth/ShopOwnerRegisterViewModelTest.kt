package com.aaspas.customer.presentation.auth

import com.aaspas.customer.core.auth.UserRoles
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
class ShopOwnerRegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ShopOwnerRegisterViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository()
        viewModel = ShopOwnerRegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `valid registration sends shop_owner role`() = runTest {
        fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Success, viewModel.uiState.value.status)
        assertEquals(UserRoles.SHOP_OWNER, authRepository.lastRegisterRole)
        assertTrue(authRepository.isLoggedIn())
    }

    @Test
    fun `invalid email shows validation error`() = runTest {
        viewModel.onFullNameChange("Owner")
        viewModel.onEmailChange("bad-email")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.ValidationError, viewModel.uiState.value.status)
        assertEquals(null, authRepository.lastRegisterRole)
    }

    @Test
    fun `password too short shows validation error`() = runTest {
        viewModel.onFullNameChange("Owner")
        viewModel.onEmailChange("owner@example.com")
        viewModel.onPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.ValidationError, viewModel.uiState.value.status)
    }

    @Test
    fun `password mismatch shows validation error`() = runTest {
        viewModel.onFullNameChange("Owner")
        viewModel.onEmailChange("owner@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password999")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.ValidationError, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("match"))
    }

    @Test
    fun `duplicate email shows api error`() = runTest {
        authRepository.registerResult = Result.Failure(AppError.Unexpected("Email already registered"))
        fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Error, viewModel.uiState.value.status)
        assertEquals(UserRoles.SHOP_OWNER, authRepository.lastRegisterRole)
    }

    @Test
    fun `api validation error is shown`() = runTest {
        authRepository.registerResult = Result.Failure(AppError.Unexpected("Invalid registration details"))
        fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Error, viewModel.uiState.value.status)
    }

    @Test
    fun `duplicate submit is ignored while loading`() = runTest {
        fillValidForm()
        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Success, viewModel.uiState.value.status)
        assertEquals(UserRoles.SHOP_OWNER, authRepository.lastRegisterRole)
    }

    @Test
    fun `customer registration still defaults to customer role`() = runTest {
        val customerViewModel = RegisterViewModel(authRepository)
        customerViewModel.onEmailChange("customer@example.com")
        customerViewModel.onPasswordChange("password123")
        customerViewModel.onConfirmPasswordChange("password123")
        customerViewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterStatus.Success, customerViewModel.uiState.value.status)
        assertEquals(UserRoles.CUSTOMER, authRepository.lastRegisterRole)
    }

    private fun fillValidForm() {
        viewModel.onFullNameChange("Shop Owner")
        viewModel.onEmailChange("owner-new@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
    }
}
