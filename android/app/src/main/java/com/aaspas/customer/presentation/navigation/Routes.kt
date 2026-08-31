package com.aaspas.customer.presentation.navigation

import androidx.annotation.StringRes
import com.aaspas.customer.R

enum class BottomNavItem(val route: String, @StringRes val labelRes: Int) {
    Home("home", R.string.nav_home),
    Search("search", R.string.nav_search),
    Favorites("favorites", R.string.nav_favorites),
    Profile("profile", R.string.nav_profile),
}

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val SHOP_OWNER_REGISTER = "shop-owner/register"
    const val LOCATION_SELECTION = "location-selection"
    const val MAP = "map?categoryId={categoryId}&categoryName={categoryName}"
    const val OFFER_DETAILS = "offer/{offerId}"
    const val SHOP_DETAILS = "shop/{shopId}"
    const val CATEGORY_OFFERS = "category/{categoryId}/{categoryName}"
    const val SHOP_OWNER = "shop-owner"
    const val SHOP_OWNER_ONBOARDING = "shop-owner/onboarding"
    const val SHOP_OWNER_PROFILE = "shop-owner/profile"
    const val SHOP_OWNER_NOTIFICATIONS = "shop-owner/notifications"
    const val NOTIFICATIONS = "notifications"
    const val SHOP_OWNER_MANAGE_OFFERS = "shop-owner/offers?shopId={shopId}"
    const val SHOP_OWNER_CREATE_OFFER =
        "shop-owner/create-offer?shopId={shopId}&shopName={shopName}&shopPhotoUrl={shopPhotoUrl}&offerId={offerId}"
    const val ADMIN_CONSOLE = "admin"
    const val ADMIN_OFFER_VERIFICATION = "admin/offers"
    const val ADMIN_OFFER_REVIEW = "admin/offers/{offerId}"
    const val ADMIN_SHOP_VERIFICATION = "admin/shops"
    const val ADMIN_SHOP_REVIEW = "admin/shops/{shopId}"
    const val ADMIN_SHOP_MANAGEMENT = "admin/shop-management"
    const val ADMIN_SHOP_MANAGEMENT_DETAIL = "admin/shop-management/{shopId}"
    const val ADMIN_USER_MANAGEMENT = "admin/users"

    fun offerDetails(offerId: String) = "offer/$offerId"
    fun shopDetails(shopId: String) = "shop/$shopId"
    fun shopOwnerManageOffers(shopId: String) = "shop-owner/offers?shopId=$shopId"
    fun shopOwnerCreateOffer(
        shopId: String,
        shopName: String,
        shopPhotoUrl: String? = null,
        offerId: String? = null,
    ): String {
        val encodedName = java.net.URLEncoder.encode(shopName, Charsets.UTF_8.name())
        val encodedPhoto = java.net.URLEncoder.encode(shopPhotoUrl.orEmpty(), Charsets.UTF_8.name())
        val encodedOfferId = java.net.URLEncoder.encode(offerId.orEmpty(), Charsets.UTF_8.name())
        return "shop-owner/create-offer?shopId=$shopId&shopName=$encodedName&shopPhotoUrl=$encodedPhoto&offerId=$encodedOfferId"
    }
    fun adminOfferReview(offerId: String) = "admin/offers/$offerId"
    fun adminShopReview(shopId: String) = "admin/shops/$shopId"
    fun adminShopManagementDetail(shopId: String) = "admin/shop-management/$shopId"
    fun map(categoryId: String? = null, categoryName: String? = null): String {
        val id = categoryId?.trim().orEmpty()
        val encoded = categoryName?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { java.net.URLEncoder.encode(it, Charsets.UTF_8.name()) }
            ?: ""
        return "map?categoryId=$id&categoryName=$encoded"
    }
    fun categoryOffers(categoryId: String, categoryName: String): String {
        val encodedName = java.net.URLEncoder.encode(categoryName, Charsets.UTF_8.name())
        return "category/$categoryId/$encodedName"
    }
}
