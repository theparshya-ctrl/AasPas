package com.aaspas.customer.domain.map

import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.model.HomeLocation
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.Shop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMarkerGrouperTest {

    @Test
    fun `groups offers by shop and picks featured active offer`() {
        val feed = sampleFeed()
        val pins = MapMarkerGrouper.groupFromHome(feed)

        assertEquals(1, pins.size)
        assertEquals("shop-1", pins.first().shop.id)
        assertEquals(2, pins.first().offers.size)
        assertEquals("offer-1", pins.first().featuredOffer?.id)
        assertEquals(1, pins.first().activeOfferCount)
    }

    @Test
    fun `filters by category name`() {
        val feed = sampleFeed()
        val pins = MapMarkerGrouper.groupFromHome(
            feed,
            categoryFilter = MapCategoryFilter(categoryName = "Electronics"),
        )

        assertTrue(pins.isEmpty())
    }

    @Test
    fun `filters by category id`() {
        val feed = sampleFeed()
        val pins = MapMarkerGrouper.groupFromHome(
            feed,
            categoryFilter = MapCategoryFilter(categoryId = "cat-1"),
        )

        assertEquals(1, pins.size)
        assertEquals("shop-1", pins.first().shop.id)
    }

    @Test
    fun `category id resolves name from feed categories`() {
        val feed = sampleFeed()
        val resolved = MapMarkerGrouper.resolveCategoryName(
            feed,
            MapCategoryFilter(categoryId = "cat-1"),
        )
        assertEquals("Clothing", resolved)
    }

    @Test
    fun `includes today and coming soon for matching category only`() {
        val feed = sampleFeed(
            today = listOf(
                offer("offer-1", category = "Clothing"),
                offer("offer-other", category = "Electronics"),
            ),
            comingSoon = listOf(offer("offer-soon", category = "Clothing", isComingSoon = true)),
        )
        val pins = MapMarkerGrouper.groupFromHome(
            feed,
            categoryFilter = MapCategoryFilter(categoryId = "cat-1"),
        )

        assertEquals(1, pins.size)
        assertEquals(2, pins.first().offers.size)
        assertTrue(pins.first().offers.any { it.id == "offer-1" })
        assertTrue(pins.first().offers.any { it.id == "offer-soon" })
        assertTrue(pins.first().offers.none { it.id == "offer-other" })
    }

    @Test
    fun `excludes shops without offers`() {
        val feed = sampleFeed(
            shops = listOf(
                shop("shop-1", "Fashion Hub"),
                shop("shop-2", "Other Shop"),
            ),
        )
        val pins = MapMarkerGrouper.groupFromHome(feed)
        assertEquals(1, pins.size)
        assertEquals("shop-1", pins.first().shop.id)
    }

    @Test
    fun `pick featured offer prefers active offer`() {
        val offers = listOf(
            offer("soon", isComingSoon = true),
            offer("active", isComingSoon = false),
        )
        val featured = MapMarkerGrouper.pickFeaturedOffer(offers)
        assertEquals("active", featured?.id)
    }

    @Test
    fun `pick featured offer falls back to coming soon`() {
        val featured = MapMarkerGrouper.pickFeaturedOffer(listOf(offer("soon", isComingSoon = true)))
        assertEquals("soon", featured?.id)
    }

    @Test
    fun `one pin per shop with coordinates`() {
        val feed = sampleFeed()
        val pins = MapMarkerGrouper.groupFromHome(feed)

        assertEquals(1, pins.size)
        assertEquals(18.63, pins.first().shop.latitude!!, 0.0001)
        assertEquals(73.80, pins.first().shop.longitude!!, 0.0001)
    }

    @Test
    fun `empty when no offers`() {
        val feed = sampleFeed(today = emptyList(), comingSoon = emptyList())
        assertTrue(MapMarkerGrouper.groupFromHome(feed).isEmpty())
    }

    private fun sampleFeed(
        today: List<Offer> = listOf(offer("offer-1"), offer("offer-2", isComingSoon = true)),
        comingSoon: List<Offer> = emptyList(),
        shops: List<Shop> = listOf(shop("shop-1", "Fashion Hub"), shop("shop-2", "Other Shop")),
    ) = HomeFeed(
        location = HomeLocation(18.63, 73.80, true),
        categories = listOf(Category("cat-1", "Clothing", "clothing", 1)),
        todayOffers = today,
        comingSoon = comingSoon,
        nearbyShops = shops,
    )

    private fun offer(
        id: String,
        isComingSoon: Boolean = false,
        category: String = "Clothing",
    ) = Offer(
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
        category = category,
        isComingSoon = isComingSoon,
    )

    private fun shop(id: String, name: String) = Shop(
        id = id,
        name = name,
        photoUrl = null,
        category = "Clothing",
        addressArea = "Pimpri",
        distanceKm = 1.0,
        latitude = 18.63,
        longitude = 73.80,
    )
}
