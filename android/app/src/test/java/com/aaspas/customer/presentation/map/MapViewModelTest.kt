package com.aaspas.customer.presentation.map

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.location.GeoCoordinates
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.model.HomeLocation
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.Shop
import com.aaspas.customer.domain.repository.HomeRepository
import com.aaspas.customer.presentation.map.provider.MapProviderType
import com.aaspas.customer.presentation.map.provider.MapRenderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeHomeRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeHomeRepository()
        viewModel = MapViewModel(repository, googleMapsKeyConfigured = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads nearby pins with location`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        assertEquals(MapLoadStatus.Loaded, viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.pins.size)
    }

    @Test
    fun `manual location center loads nearby at coordinates`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.setUserLocation(GeoCoordinates(18.6298, 73.7997), "Pimpri")
        advanceUntilIdle()

        assertEquals(18.6298, repository.lastLatitude!!, 0.0001)
        assertEquals(73.7997, repository.lastLongitude!!, 0.0001)
        assertEquals("Pimpri", viewModel.uiState.value.localityName)
        assertEquals(18.6298, viewModel.uiState.value.userLocation!!.latitude, 0.0001)
    }

    @Test
    fun `defaults to map view mode`() = runTest {
        assertEquals(MapViewMode.Map, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `uses google provider when key configured`() = runTest {
        assertEquals(MapProviderType.Google, viewModel.uiState.value.activeProvider)
        assertTrue(viewModel.uiState.value.googleMapsKeyConfigured)
    }

    @Test
    fun `uses osm provider when google key unavailable`() = runTest {
        val offlineViewModel = MapViewModel(repository, googleMapsKeyConfigured = false)
        assertEquals(MapProviderType.OpenStreetMap, offlineViewModel.uiState.value.activeProvider)
        assertFalse(offlineViewModel.uiState.value.googleMapsKeyConfigured)
        assertEquals(MapViewMode.Map, offlineViewModel.uiState.value.viewMode)
    }

    @Test
    fun `google failure falls back to osm without switching to list`() = runTest {
        viewModel.onGoogleMapsFailed()
        assertEquals(MapProviderType.OpenStreetMap, viewModel.uiState.value.activeProvider)
        assertTrue(viewModel.uiState.value.googleMapsInitFailed)
        assertEquals(MapViewMode.Map, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `osm failure marks map unavailable not list mode`() = runTest {
        val offlineViewModel = MapViewModel(repository, googleMapsKeyConfigured = false)
        offlineViewModel.onOpenStreetMapFailed()
        assertEquals(MapRenderState.Unavailable, offlineViewModel.uiState.value.mapRenderState)
        assertEquals(MapViewMode.Map, offlineViewModel.uiState.value.viewMode)
    }

    @Test
    fun `list mode only after explicit user selection`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.setViewMode(MapViewMode.List)
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        assertEquals(MapViewMode.List, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `select shop marker sets selected pin`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        viewModel.selectShopMarker("shop-1")
        assertEquals("shop-1", viewModel.uiState.value.selectedPin?.shop?.id)
    }

    @Test
    fun `one pin per shop groups multiple offers`() = runTest {
        repository.result = Result.Success(
            sampleFeed(
                today = listOf(
                    offer("offer-1"),
                    offer("offer-2", isComingSoon = true),
                ),
            ),
        )
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.pins.size)
        assertEquals(2, viewModel.uiState.value.pins.first().offers.size)
    }

    @Test
    fun `category filter applied through view model context`() = runTest {
        val categoryViewModel = MapViewModel(
            repository,
            googleMapsKeyConfigured = false,
            categoryName = "Electronics",
        )
        repository.result = Result.Success(sampleFeed())
        categoryViewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        assertEquals("Electronics", categoryViewModel.uiState.value.categoryName)
        assertTrue(categoryViewModel.uiState.value.pins.isEmpty())
        assertEquals(0, categoryViewModel.uiState.value.categoryOfferCount)
    }

    @Test
    fun `category id filter shows only matching category markers`() = runTest {
        val categoryViewModel = MapViewModel(
            repository,
            googleMapsKeyConfigured = false,
            categoryId = "cat-1",
        )
        repository.result = Result.Success(sampleFeed())
        categoryViewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        assertEquals("cat-1", categoryViewModel.uiState.value.categoryId)
        assertEquals("Clothing", categoryViewModel.uiState.value.categoryName)
        assertEquals(1, categoryViewModel.uiState.value.pins.size)
        assertEquals(1, categoryViewModel.uiState.value.categoryOfferCount)
    }

    @Test
    fun `category filter empty state when no matching offers`() = runTest {
        val categoryViewModel = MapViewModel(
            repository,
            googleMapsKeyConfigured = false,
            categoryId = "cat-1",
            categoryName = "Clothing",
        )
        repository.result = Result.Success(
            sampleFeed(today = emptyList(), comingSoon = emptyList()),
        )
        categoryViewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()

        assertEquals(MapLoadStatus.Loaded, categoryViewModel.uiState.value.status)
        assertTrue(categoryViewModel.uiState.value.pins.isEmpty())
        assertEquals(0, categoryViewModel.uiState.value.categoryOfferCount)
    }

    @Test
    fun `zero pins with valid location keeps loaded state for map rendering`() = runTest {
        repository.result = Result.Success(
            HomeFeed(
                location = HomeLocation(18.63, 73.80, true),
                categories = emptyList(),
                todayOffers = emptyList(),
                comingSoon = emptyList(),
                nearbyShops = emptyList(),
            ),
        )
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Delhi")
        advanceUntilIdle()

        assertEquals(MapLoadStatus.Loaded, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.pins.isEmpty())
        assertEquals(18.63, viewModel.uiState.value.userLocation!!.latitude, 0.0001)
        assertEquals(MapViewMode.Map, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `map list toggle works with zero pins`() = runTest {
        repository.result = Result.Success(
            HomeFeed(
                location = HomeLocation(18.63, 73.80, true),
                categories = emptyList(),
                todayOffers = emptyList(),
                comingSoon = emptyList(),
                nearbyShops = emptyList(),
            ),
        )
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Delhi")
        advanceUntilIdle()
        viewModel.setViewMode(MapViewMode.List)
        advanceUntilIdle()

        assertEquals(MapViewMode.List, viewModel.uiState.value.viewMode)
        assertTrue(viewModel.uiState.value.pins.isEmpty())
        viewModel.setViewMode(MapViewMode.Map)
        assertEquals(MapViewMode.Map, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `location change reloads nearby offers`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80), "Pimpri")
        advanceUntilIdle()
        repository.result = Result.Success(
            HomeFeed(
                location = HomeLocation(28.61, 77.20, true),
                categories = emptyList(),
                todayOffers = emptyList(),
                comingSoon = emptyList(),
                nearbyShops = emptyList(),
            ),
        )
        viewModel.setUserLocation(GeoCoordinates(28.6139, 77.2090), "Delhi")
        advanceUntilIdle()

        assertEquals(28.6139, repository.lastLatitude!!, 0.0001)
        assertEquals(77.2090, repository.lastLongitude!!, 0.0001)
        assertEquals("Delhi", viewModel.uiState.value.localityName)
        assertEquals(MapLoadStatus.Loaded, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.pins.isEmpty())
    }

    @Test
    fun `empty results`() = runTest {
        repository.result = Result.Success(
            HomeFeed(
                location = HomeLocation(null, null, false),
                categories = emptyList(),
                todayOffers = emptyList(),
                comingSoon = emptyList(),
                nearbyShops = emptyList(),
            ),
        )
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80))
        advanceUntilIdle()

        assertEquals(MapLoadStatus.Loaded, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.pins.isEmpty())
        assertNull(viewModel.uiState.value.selectedPin)
    }

    @Test
    fun `no location prompts location selection`() = runTest {
        viewModel.setUserLocation(null)
        assertTrue(viewModel.uiState.value.needsLocationSelection)
    }

    @Test
    fun `permission denied still loads without location`() = runTest {
        repository.result = Result.Success(sampleFeed())
        viewModel.onPermissionDenied()
        advanceUntilIdle()

        assertEquals(MapLoadStatus.Loaded, viewModel.uiState.value.status)
    }

    @Test
    fun `error state`() = runTest {
        repository.result = Result.Failure(AppError.NoInternet)
        viewModel.setUserLocation(GeoCoordinates(18.63, 73.80))
        advanceUntilIdle()

        assertEquals(MapLoadStatus.Error, viewModel.uiState.value.status)
    }

    private fun sampleFeed(
        today: List<Offer> = listOf(offer("offer-1")),
        comingSoon: List<Offer> = emptyList(),
    ) = HomeFeed(
        location = HomeLocation(18.63, 73.80, true),
        categories = listOf(Category("cat-1", "Clothing", "clothing", 1)),
        todayOffers = today,
        comingSoon = comingSoon,
        nearbyShops = listOf(
            Shop(
                id = "shop-1",
                name = "Fashion Hub",
                photoUrl = null,
                category = "Clothing",
                addressArea = "Pimpri",
                distanceKm = 1.0,
                latitude = 18.63,
                longitude = 73.80,
            ),
        ),
    )

    private fun offer(id: String, isComingSoon: Boolean = false) = Offer(
        id = id,
        title = "Deal $id",
        description = null,
        photoUrl = null,
        discountType = "percentage",
        discountValue = "10",
        startsAt = "2025-08-19T10:00:00Z",
        endsAt = "2025-08-25T18:00:00Z",
        shopId = "shop-1",
        shopName = "Fashion Hub",
        distanceKm = 1.0,
        category = "Clothing",
        isComingSoon = isComingSoon,
    )

    private class FakeHomeRepository : HomeRepository {
        var result: Result<HomeFeed> = Result.Failure(AppError.Server)
        var lastLatitude: Double? = null
        var lastLongitude: Double? = null

        override suspend fun getHome(latitude: Double?, longitude: Double?): Result<HomeFeed> {
            lastLatitude = latitude
            lastLongitude = longitude
            return result
        }
    }
}
