package com.aaspas.customer.presentation.location

import android.content.Context
import com.aaspas.customer.core.location.LocationSearchOutcome
import com.aaspas.customer.core.location.LocationSearchResult
import com.aaspas.customer.core.location.LocationSearchService
import com.aaspas.customer.core.location.SelectedLocationStore
import com.aaspas.customer.domain.model.LocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationSelectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var store: SelectedLocationStore
    private lateinit var searchService: FakeLocationSearchService
    private lateinit var viewModel: LocationSelectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("aaspas_selected_location", Context.MODE_PRIVATE).edit().clear().apply()
        store = SelectedLocationStore(context)
        searchService = FakeLocationSearchService()
        viewModel = LocationSelectionViewModel(searchService, store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
    }

    @Test
    fun `search returns loaded results`() = runTest {
        searchService.outcome = LocationSearchOutcome.Success(
            listOf(
                LocationSearchResult("Pimpri", 18.6298, 73.7997),
            ),
        )

        viewModel.onSearchQueryChange("Pimpri")
        advanceTimeBy(400)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationSearchStatus.Loaded, state.searchStatus)
        assertEquals(1, state.searchResults.size)
        assertEquals("Pimpri", state.searchResults.first().displayName)
    }

    @Test
    fun `search no results shows empty state`() = runTest {
        searchService.outcome = LocationSearchOutcome.Empty

        viewModel.onSearchQueryChange("UnknownPlace")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(LocationSearchStatus.Empty, viewModel.uiState.value.searchStatus)
    }

    @Test
    fun `search geocoder failure shows error`() = runTest {
        searchService.outcome = LocationSearchOutcome.GeocoderUnavailable

        viewModel.onSearchQueryChange("Pimpri")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(LocationSearchStatus.Error, viewModel.uiState.value.searchStatus)
        assertEquals(
            LocationSelectionViewModel.ERROR_GEOCODER_UNAVAILABLE,
            viewModel.uiState.value.searchErrorMessage,
        )
    }

    @Test
    fun `select result stores manual location`() = runTest {
        val result = LocationSearchResult("Pimpri", 18.6298, 73.7997)

        viewModel.selectSearchResult(result)
        advanceUntilIdle()

        val stored = store.current()
        assertEquals(LocationSource.MANUAL, stored.source)
        assertEquals("Pimpri", stored.displayName)
        assertEquals(18.6298, stored.latitude!!, 0.0001)
    }

    @Test
    fun `current gps resolved updates store`() = runTest {
        viewModel.onCurrentLocationResolved("Chinchwad", 18.6278, 73.7911)
        advanceUntilIdle()

        val stored = store.current()
        assertEquals(LocationSource.CURRENT_GPS, stored.source)
        assertEquals("Chinchwad", stored.displayName)
    }

    @Test
    fun `permission denied keeps manual selection`() = runTest {
        store.setManual("Pimpri", 18.6298, 73.7997)

        viewModel.onCurrentLocationDenied()
        advanceUntilIdle()

        assertEquals(LocationSource.MANUAL, store.current().source)
        assertEquals(
            LocationSelectionViewModel.ERROR_PERMISSION_DENIED,
            viewModel.uiState.value.currentLocationError,
        )
    }

    @Test
    fun `isCurrentSelection detects selected manual result`() {
        val result = LocationSearchResult("Pimpri", 18.6298, 73.7997)
        store.setManual("Pimpri", 18.6298, 73.7997)

        assertTrue(viewModel.isCurrentSelection(result))
    }

    private class FakeLocationSearchService : LocationSearchService {
        var outcome: LocationSearchOutcome = LocationSearchOutcome.Empty

        override suspend fun search(query: String, maxResults: Int): LocationSearchOutcome = outcome
    }
}
