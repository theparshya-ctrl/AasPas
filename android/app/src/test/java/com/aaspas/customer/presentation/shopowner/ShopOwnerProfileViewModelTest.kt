package com.aaspas.customer.presentation.shopowner

import android.net.Uri
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShopOwnerProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeShopOwnerRepository
    private lateinit var application: android.app.Application

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeShopOwnerRepository()
        application = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `photo pick stores pending uri without uploading`() = runTest {
        repository.dashboardResult = Result.Success(
            repository.sampleDashboard(canSubmitOffers = true),
        )
        val viewModel = ShopOwnerProfileViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://media/external/images/1"))
        advanceUntilIdle()

        assertEquals("content://media/external/images/1", viewModel.uiState.value.pendingPhotoUri)
        assertNull(viewModel.uiState.value.pendingPhotoBytes)
        assertEquals(false, viewModel.uiState.value.isUploadingPhoto)
    }

    @Test
    fun `save pending photo uploads and clears pending state`() = runTest {
        repository.dashboardResult = Result.Success(
            repository.sampleDashboard(canSubmitOffers = true),
        )
        val viewModel = ShopOwnerProfileViewModel(
            application = application,
            repository = repository,
            compressImage = { _, _ -> byteArrayOf(1, 2, 3) },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://media/external/images/1"))
        viewModel.savePendingPhoto()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingPhotoUri)
        assertEquals("/media/shops/shop-1/uploaded.jpg", viewModel.uiState.value.photoUrl)
        assertTrue(viewModel.uiState.value.photoSaveSucceeded)
        assertEquals(ShopOwnerProfileLoadStatus.Loaded, viewModel.uiState.value.status)
    }

    @Test
    fun `remove pending photo clears preview without api call`() = runTest {
        repository.dashboardResult = Result.Success(
            repository.sampleDashboard(canSubmitOffers = true),
        )
        val viewModel = ShopOwnerProfileViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://media/external/images/1"))
        viewModel.onPhotoRemoved()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingPhotoUri)
    }

    @Test
    fun `active shop can edit photo only`() = runTest {
        repository.dashboardResult = Result.Success(
            repository.sampleDashboard(canSubmitOffers = true),
        )
        val viewModel = ShopOwnerProfileViewModel(application, repository)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.canEdit)
        assertEquals(true, viewModel.uiState.value.canEditPhoto)
    }

    @Test
    fun `upload failure keeps pending photo uri`() = runTest {
        repository.dashboardResult = Result.Success(
            repository.sampleDashboard(canSubmitOffers = true),
        )
        repository.uploadShopPhotoResult = Result.Failure(AppError.NoInternet)
        val viewModel = ShopOwnerProfileViewModel(
            application = application,
            repository = repository,
            compressImage = { _, _ -> byteArrayOf(9) },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://media/external/images/1"))
        viewModel.savePendingPhoto()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingPhotoUri)
        assertEquals(false, viewModel.uiState.value.photoSaveSucceeded)
    }

    @Test
    fun `compression failure shows error and keeps pending uri`() = runTest {
        repository.dashboardResult = Result.Success(
            repository.sampleDashboard(canSubmitOffers = true),
        )
        val viewModel = ShopOwnerProfileViewModel(
            application = application,
            repository = repository,
            compressImage = { _, _ -> null },
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://media/external/images/1"))
        viewModel.savePendingPhoto()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingPhotoUri)
        assertNotNull(viewModel.uiState.value.photoErrorMessage)
        assertEquals(0, repository.uploadShopPhotoCallCount)
    }
}
