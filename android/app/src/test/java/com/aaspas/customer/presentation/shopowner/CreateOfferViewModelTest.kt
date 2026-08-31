package com.aaspas.customer.presentation.shopowner



import android.net.Uri

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

class CreateOfferViewModelTest {



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

    fun `preview blocked when submit validation fails`() = runTest {

        val viewModel = createViewModel()

        viewModel.showPreview()

        advanceUntilIdle()



        assertEquals(CreateOfferStep.Form, viewModel.uiState.value.step)

        assertNotNull(viewModel.uiState.value.validationMessage)

    }



    @Test

    fun `preview opens with valid draft`() = runTest {

        val viewModel = createViewModel()

        fillValidDraft(viewModel)

        viewModel.showPreview()

        advanceUntilIdle()



        assertEquals(CreateOfferStep.Preview, viewModel.uiState.value.step)

    }



    @Test

    fun `save draft creates offer`() = runTest {

        val viewModel = createViewModel()

        viewModel.onTitleChange("Weekend Sale")

        viewModel.onDiscountValueChange("15")

        viewModel.saveDraft()

        advanceUntilIdle()



        assertEquals(CreateOfferStatus.Saved, viewModel.uiState.value.status)

        assertNotNull(repository.lastCreatedDraft)

    }



    @Test

    fun `photo pick stores pending uri without uploading`() = runTest {

        val viewModel = createViewModel(offerId = "offer-1")

        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://test/photo"))

        advanceUntilIdle()



        assertEquals("content://test/photo", viewModel.uiState.value.pendingPhotoUri)

        assertEquals(0, repository.uploadOfferPhotoCallCount)

    }



    @Test

    fun `save photo uploads after draft exists`() = runTest {

        val viewModel = createViewModel(

            offerId = "offer-1",

            compressImage = { _, _ -> byteArrayOf(1, 2, 3) },

        )

        advanceUntilIdle()

        viewModel.onPhotoPicked(Uri.parse("content://test/photo"))

        viewModel.savePendingPhoto()

        advanceUntilIdle()



        assertEquals(1, repository.uploadOfferPhotoCallCount)

        assertEquals("offer-1", repository.lastUploadedOfferId)

        assertTrue(viewModel.uiState.value.photoSaveSucceeded)

        assertNull(viewModel.uiState.value.pendingPhotoUri)

    }



    @Test

    fun `save photo creates draft first when offer id missing`() = runTest {

        val viewModel = createViewModel(

            compressImage = { _, _ -> byteArrayOf(1, 2, 3) },

        )

        fillValidDraft(viewModel)

        viewModel.onPhotoPicked(Uri.parse("content://test/photo"))

        viewModel.savePendingPhoto()

        advanceUntilIdle()



        assertNotNull(repository.lastCreatedDraft)

        assertEquals(1, repository.uploadOfferPhotoCallCount)

    }



    @Test

    fun `submit requires confirmation checkbox`() = runTest {

        val viewModel = createViewModel()

        fillValidDraft(viewModel)

        viewModel.showPreview()

        advanceUntilIdle()

        viewModel.submitForVerification()

        advanceUntilIdle()



        assertEquals(CreateOfferStatus.Idle, viewModel.uiState.value.status)

        assertNotNull(viewModel.uiState.value.validationMessage)

    }



    @Test

    fun `submit sends merchant confirmed true`() = runTest {

        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = true))

        val viewModel = createViewModel()

        advanceUntilIdle()

        fillValidDraft(viewModel)

        viewModel.showPreview()

        viewModel.onMerchantConfirmedChange(true)

        viewModel.submitForVerification()

        advanceUntilIdle()



        assertEquals(CreateOfferStatus.Submitted, viewModel.uiState.value.status)

        assertEquals(true, repository.lastMerchantConfirmed)

        assertNotNull(repository.lastSubmittedOfferId)

    }



    @Test

    fun `loads existing offer for edit`() = runTest {

        repository.getOfferResult = Result.Success(

            repository.sampleOffer("shop-1", MerchantOfferStatus.Rejected).copy(

                title = "Rejected Offer",

                rejectionReason = "Incomplete terms",

            ),

        )

        val viewModel = createViewModel(offerId = "offer-1")

        advanceUntilIdle()



        assertEquals("Rejected Offer", viewModel.uiState.value.draft.title)

        assertEquals(CreateOfferStatus.Idle, viewModel.uiState.value.status)

        assertEquals(false, viewModel.uiState.value.isReadOnly)

        assertEquals(true, viewModel.uiState.value.requiresReVerification)

    }



    @Test

    fun `pending offer opens read only preview`() = runTest {

        repository.getOfferResult = Result.Success(

            repository.sampleOffer("shop-1", MerchantOfferStatus.PendingApproval).copy(title = "Pending Offer"),

        )

        val viewModel = createViewModel(offerId = "offer-1")

        advanceUntilIdle()



        assertEquals(true, viewModel.uiState.value.isReadOnly)

        assertEquals(CreateOfferStep.Preview, viewModel.uiState.value.step)

        assertEquals(MerchantOfferStatus.PendingApproval, viewModel.uiState.value.loadedOfferStatus)

    }



    @Test

    fun `read only offer cannot save draft`() = runTest {

        repository.getOfferResult = Result.Success(

            repository.sampleOffer("shop-1", MerchantOfferStatus.Active),

        )

        val viewModel = createViewModel(offerId = "offer-1")

        advanceUntilIdle()

        viewModel.onTitleChange("Changed")

        viewModel.saveDraft()

        advanceUntilIdle()



        assertEquals(0, repository.createShopCallCount)

    }



    @Test

    fun `submit failure shows error`() = runTest {

        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = true))

        repository.submitOfferResult = Result.Failure(AppError.Server)

        val viewModel = createViewModel()

        advanceUntilIdle()

        fillValidDraft(viewModel)

        viewModel.showPreview()

        viewModel.onMerchantConfirmedChange(true)

        viewModel.submitForVerification()

        advanceUntilIdle()



        assertEquals(CreateOfferStatus.Idle, viewModel.uiState.value.status)

        assertEquals(AppError.Server, viewModel.uiState.value.error)

        assertNotNull(viewModel.uiState.value.validationMessage)

    }



    @Test

    fun `submit blocked when shop not approved`() = runTest {

        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = false))

        val viewModel = createViewModel()

        advanceUntilIdle()

        fillValidDraft(viewModel)

        viewModel.showPreview()

        advanceUntilIdle()

        viewModel.onMerchantConfirmedChange(true)

        viewModel.submitForVerification()

        advanceUntilIdle()



        assertNotNull(viewModel.uiState.value.validationMessage)

        assertTrue(viewModel.uiState.value.validationMessage!!.contains("approved"))

    }



    @Test

    fun `submit allowed when shop approved`() = runTest {

        repository.dashboardResult = Result.Success(repository.sampleDashboard(canSubmitOffers = true))

        val viewModel = createViewModel()

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canSubmitOffers)

    }



    private fun createViewModel(

        offerId: String? = null,

        compressImage: (android.app.Application, Uri) -> ByteArray? = { _, _ -> null },

    ): CreateOfferViewModel {

        return CreateOfferViewModel(

            application = application,

            repository = repository,

            shopId = "shop-1",

            shopName = "Fresh Mart",

            shopPhotoUrl = null,

            offerId = offerId,

            compressImage = compressImage,

            ioDispatcher = dispatcher,

        )

    }



    private fun fillValidDraft(viewModel: CreateOfferViewModel) {

        viewModel.onTitleChange("Weekend Sale")

        viewModel.onDescriptionChange("Valid on all groceries this weekend only.")

        viewModel.onDiscountTypeChange("percentage")

        viewModel.onDiscountValueChange("10")

        viewModel.onStartDateChange("2026-08-21")

        viewModel.onStartTimeChange("09:00")

        viewModel.onEndDateChange("2026-08-21")

        viewModel.onEndTimeChange("21:00")

    }

}


