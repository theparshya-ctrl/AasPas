package com.aaspas.customer.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class RoutesTest {

    @Test
    fun `map route includes category id and encoded name`() {
        val route = Routes.map(categoryId = "cat-fashion", categoryName = "Clothing / Fashion")
        assertTrue(route.startsWith("map?categoryId=cat-fashion&categoryName="))
        val encodedName = route.substringAfter("categoryName=")
        assertEquals("Clothing / Fashion", URLDecoder.decode(encodedName, Charsets.UTF_8.name()))
    }

    @Test
    fun `map route without category omits empty params`() {
        val route = Routes.map()
        assertEquals("map?categoryId=&categoryName=", route)
    }

    @Test
    fun `shop owner notifications route constant`() {
        assertEquals("shop-owner/notifications", Routes.SHOP_OWNER_NOTIFICATIONS)
    }

    @Test
    fun `shop owner register route constant`() {
        assertEquals("shop-owner/register", Routes.SHOP_OWNER_REGISTER)
    }

    @Test
    fun `admin shop review route includes shop id`() {
        assertEquals("admin/shops/shop-123", Routes.adminShopReview("shop-123"))
    }
}
