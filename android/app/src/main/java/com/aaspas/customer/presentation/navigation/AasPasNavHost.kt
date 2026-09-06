package com.aaspas.customer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.aaspas.customer.BuildConfig
import com.aaspas.customer.core.update.AppUpdateDiagnostics
import com.aaspas.customer.core.update.AppUpdateOffer
import com.aaspas.customer.presentation.components.AppUpdateDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aaspas.customer.R
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.core.auth.PendingAuthAction
import com.aaspas.customer.core.auth.UserRoles
import com.aaspas.customer.core.navigation.DirectionsIntentBuilder
import com.aaspas.customer.presentation.auth.LoginRoute
import com.aaspas.customer.presentation.auth.RegisterRoute
import com.aaspas.customer.presentation.auth.ShopOwnerRegisterRoute
import com.aaspas.customer.presentation.category.CategoryOffersRoute
import com.aaspas.customer.presentation.components.AasPasBottomNavigation
import com.aaspas.customer.presentation.components.InAppNotificationBannerHost
import com.aaspas.customer.presentation.favorites.FavoritesRoute
import com.aaspas.customer.presentation.home.HomeRoute
import com.aaspas.customer.presentation.location.LocationSelectionRoute
import com.aaspas.customer.presentation.map.MapRoute
import com.aaspas.customer.presentation.offerdetails.OfferDetailsRoute
import com.aaspas.customer.presentation.profile.ProfileRoute
import com.aaspas.customer.presentation.search.SearchRoute
import com.aaspas.customer.presentation.admin.AdminConsoleRoute
import com.aaspas.customer.presentation.admin.AdminOfferReviewRoute
import com.aaspas.customer.presentation.admin.AdminOfferVerificationRoute
import com.aaspas.customer.presentation.admin.AdminShopManagementDetailRoute
import com.aaspas.customer.presentation.admin.AdminShopManagementRoute
import com.aaspas.customer.presentation.admin.AdminShopReviewRoute
import com.aaspas.customer.presentation.admin.AdminShopVerificationRoute
import com.aaspas.customer.presentation.admin.AdminUserManagementRoute
import com.aaspas.customer.presentation.shopowner.CreateOfferRoute
import com.aaspas.customer.presentation.shopowner.ManageOffersRoute
import com.aaspas.customer.presentation.shopowner.ShopOwnerDashboardRoute
import com.aaspas.customer.presentation.shopowner.ShopOwnerOnboardingRoute
import com.aaspas.customer.presentation.shopowner.ShopOwnerProfileRoute
import com.aaspas.customer.presentation.notifications.NotificationsRoute
import com.aaspas.customer.presentation.shopdetails.ShopDetailsRoute
import java.net.URLDecoder

@Composable
fun AasPasNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as AasPasApplication
    val scope = rememberCoroutineScope()

    var appUpdateOffer by remember { mutableStateOf<AppUpdateOffer?>(null) }
    var appUpdateDismissed by remember { mutableStateOf(false) }
    var appUpdateChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (appUpdateChecked) return@LaunchedEffect
        appUpdateChecked = true
        if (BuildConfig.APP_ENVIRONMENT != "BETA") {
            AppUpdateDiagnostics.logSkip("environment=${BuildConfig.APP_ENVIRONMENT}")
            return@LaunchedEffect
        }
        val offer = app.appUpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
        val showDialog = offer != null && !appUpdateDismissed
        AppUpdateDiagnostics.logDialogTrigger(
            show = showDialog,
            reason = when {
                offer == null -> "no_update"
                appUpdateDismissed -> "dismissed_this_session"
                else -> "update_available"
            },
        )
        if (showDialog) {
            appUpdateOffer = offer
        }
    }

    LaunchedEffect(app) {
        app.realtimeNotificationCoordinator.start(scope)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val showBottomBar = currentRoute.startsWith("home") ||
        currentRoute.startsWith("search") ||
        currentRoute.startsWith("favorites") ||
        currentRoute.startsWith("profile")

    fun navigateToLogin(pendingAction: (() -> Unit)? = null) {
        PendingAuthAction.onAuthenticated = pendingAction
        navController.navigate(Routes.LOGIN)
    }

    fun openDirections(latitude: Double?, longitude: Double?, label: String?) {
        val intent = DirectionsIntentBuilder.build(latitude, longitude, label)
            ?: DirectionsIntentBuilder.buildFallback(latitude, longitude, label)
        intent?.let {
            if (it.resolveActivity(context.packageManager) != null) {
                context.startActivity(it)
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AasPasBottomNavigation(
                    currentRoute = currentRoute.substringBefore("?"),
                    onNavigate = { item ->
                        navController.navigate(item.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding),
            ) {
            composable(Routes.HOME) {
                HomeRoute(
                    onOfferClick = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onShopClick = { shopId -> navController.navigate(Routes.shopDetails(shopId)) },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onMapClick = { navController.navigate(Routes.map()) },
                    onCategoryClick = { category ->
                        navController.navigate(Routes.categoryOffers(category.id, category.name))
                    },
                    onNavigateToLogin = { action -> navigateToLogin(action) },
                    onChangeLocationClick = { navController.navigate(Routes.LOCATION_SELECTION) },
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsRoute(
                    onBack = { navController.popBackStack() },
                    onNavigateToShopDetails = { shopId -> navController.navigate(Routes.shopDetails(shopId)) },
                    onNavigateToOfferDetails = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onNavigateToShopProfile = { navController.navigate(Routes.SHOP_OWNER_PROFILE) },
                    onNavigateToManageOffers = { shopId ->
                        navController.navigate(Routes.shopOwnerManageOffers(shopId))
                    },
                    onNavigateToEditOffer = { shopId, offerId ->
                        navController.navigate(
                            Routes.shopOwnerCreateOffer(
                                shopId = shopId,
                                shopName = "",
                                offerId = offerId,
                            ),
                        )
                    },
                )
            }
            composable(Routes.LOCATION_SELECTION) {
                LocationSelectionRoute(
                    onBack = { navController.popBackStack() },
                    onLocationApplied = { navController.popBackStack() },
                )
            }
            composable(Routes.SEARCH) {
                SearchRoute(
                    onOfferClick = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onNavigateToLogin = { action -> navigateToLogin(action) },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesRoute(
                    onOfferClick = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onShopClick = { shopId -> navController.navigate(Routes.shopDetails(shopId)) },
                    onNavigateToLogin = { navigateToLogin() },
                )
            }
            composable(Routes.PROFILE) {
                ProfileRoute(
                    onLoginClick = { navigateToLogin() },
                    onRegisterClick = { navController.navigate(Routes.REGISTER) },
                    onFavoritesClick = {
                        navController.navigate(Routes.FAVORITES) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onShopOwnerDashboardClick = {
                        navController.navigate(Routes.SHOP_OWNER)
                    },
                    onAdminConsoleClick = {
                        navController.navigate(Routes.ADMIN_CONSOLE)
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginRoute(
                    onBack = {
                        PendingAuthAction.clear()
                        navController.popBackStack()
                    },
                    onRegisterClick = {
                        navController.navigate(Routes.REGISTER) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onShopOwnerRegisterClick = {
                        navController.navigate(Routes.SHOP_OWNER_REGISTER) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onLoginSuccess = { role ->
                        when (role) {
                            UserRoles.SHOP_OWNER -> {
                                navController.navigate(Routes.SHOP_OWNER) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                            UserRoles.ADMIN -> {
                                navController.navigate(Routes.ADMIN_CONSOLE) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                            else -> navController.popBackStack()
                        }
                    },
                )
            }
            composable(Routes.REGISTER) {
                RegisterRoute(
                    onBack = { navController.popBackStack() },
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onShopOwnerRegisterClick = {
                        navController.navigate(Routes.SHOP_OWNER_REGISTER) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onRegisterSuccess = { navController.popBackStack() },
                )
            }
            composable(Routes.SHOP_OWNER_REGISTER) {
                ShopOwnerRegisterRoute(
                    onBack = { navController.popBackStack() },
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SHOP_OWNER_REGISTER) { inclusive = true }
                        }
                    },
                    onRegisterSuccess = {
                        navController.navigate(Routes.SHOP_OWNER) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.MAP,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("categoryName") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val categoryId = entry.arguments?.getString("categoryId").orEmpty().takeIf { it.isNotEmpty() }
                val categoryName = URLDecoder.decode(
                    entry.arguments?.getString("categoryName").orEmpty(),
                    Charsets.UTF_8.name(),
                ).takeIf { it.isNotEmpty() }
                MapRoute(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    onBack = { navController.popBackStack() },
                    onShopClick = { shopId -> navController.navigate(Routes.shopDetails(shopId)) },
                    onOfferClick = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onChangeLocation = { navController.navigate(Routes.LOCATION_SELECTION) },
                )
            }
            composable(
                route = Routes.CATEGORY_OFFERS,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType },
                    navArgument("categoryName") { type = NavType.StringType },
                ),
            ) { entry ->
                val categoryId = entry.arguments?.getString("categoryId").orEmpty()
                val categoryName = URLDecoder.decode(
                    entry.arguments?.getString("categoryName").orEmpty(),
                    Charsets.UTF_8.name(),
                )
                CategoryOffersRoute(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    onOfferClick = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onMapClick = { navController.navigate(Routes.map(categoryId, categoryName)) },
                )
            }
            composable(
                route = Routes.OFFER_DETAILS,
                arguments = listOf(navArgument("offerId") { type = NavType.StringType }),
            ) { entry ->
                val offerId = entry.arguments?.getString("offerId").orEmpty()
                OfferDetailsRoute(
                    offerId = offerId,
                    onBack = { navController.popBackStack() },
                    onViewShop = { shopId -> navController.navigate(Routes.shopDetails(shopId)) },
                    onDirections = { lat, lng -> openDirections(lat, lng, null) },
                    onViewCurrentOffers = { shopId -> navController.navigate(Routes.shopDetails(shopId)) },
                )
            }
            composable(Routes.SHOP_OWNER) { backStackEntry ->
                val shouldRefreshDashboard by backStackEntry.savedStateHandle
                    .getStateFlow("refresh_dashboard", false)
                    .collectAsStateWithLifecycle()
                val showShopCreatedSuccess by backStackEntry.savedStateHandle
                    .getStateFlow("shop_created_success", false)
                    .collectAsStateWithLifecycle()
                ShopOwnerDashboardRoute(
                    shouldRefreshDashboard = shouldRefreshDashboard,
                    showShopCreatedSuccess = showShopCreatedSuccess,
                    onRefreshHandled = { backStackEntry.savedStateHandle["refresh_dashboard"] = false },
                    onShopCreatedSuccessHandled = {
                        backStackEntry.savedStateHandle["shop_created_success"] = false
                    },
                    onBack = { navController.popBackStack() },
                    onCreateOffer = { shopId, shopName, shopPhotoUrl ->
                        navController.navigate(Routes.shopOwnerCreateOffer(shopId, shopName, shopPhotoUrl))
                    },
                    onManageOffers = { shopId ->
                        navController.navigate(Routes.shopOwnerManageOffers(shopId))
                    },
                    onEditProfile = { navController.navigate(Routes.SHOP_OWNER_PROFILE) },
                    onStartOnboarding = { navController.navigate(Routes.SHOP_OWNER_ONBOARDING) },
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onCustomerHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.SHOP_OWNER_ONBOARDING) {
                ShopOwnerOnboardingRoute(
                    onBack = { navController.popBackStack() },
                    onShopCreated = {
                        navController.getBackStackEntry(Routes.SHOP_OWNER).savedStateHandle.apply {
                            set("refresh_dashboard", true)
                            set("shop_created_success", true)
                        }
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.SHOP_OWNER_PROFILE) {
                ShopOwnerProfileRoute(
                    onBack = {
                        navController.getBackStackEntry(Routes.SHOP_OWNER).savedStateHandle["refresh_dashboard"] = true
                        navController.popBackStack()
                    },
                    onProfileSaved = {
                        navController.getBackStackEntry(Routes.SHOP_OWNER).savedStateHandle["refresh_dashboard"] = true
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Routes.SHOP_OWNER_MANAGE_OFFERS,
                arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            ) { entry ->
                val shopId = entry.arguments?.getString("shopId").orEmpty()
                ManageOffersRoute(
                    shopId = shopId,
                    onBack = { navController.popBackStack() },
                    onEditOffer = { offerId ->
                        navController.navigate(
                            Routes.shopOwnerCreateOffer(
                                shopId = shopId,
                                shopName = "",
                                offerId = offerId,
                            ),
                        )
                    },
                    onViewOffer = { offerId ->
                        navController.navigate(
                            Routes.shopOwnerCreateOffer(
                                shopId = shopId,
                                shopName = "",
                                offerId = offerId,
                            ),
                        )
                    },
                )
            }
            composable(
                route = Routes.SHOP_OWNER_CREATE_OFFER,
                arguments = listOf(
                    navArgument("shopId") { type = NavType.StringType },
                    navArgument("shopName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("shopPhotoUrl") { type = NavType.StringType; defaultValue = "" },
                    navArgument("offerId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val shopId = entry.arguments?.getString("shopId").orEmpty()
                val shopName = URLDecoder.decode(
                    entry.arguments?.getString("shopName").orEmpty(),
                    Charsets.UTF_8.name(),
                )
                val shopPhotoUrl = URLDecoder.decode(
                    entry.arguments?.getString("shopPhotoUrl").orEmpty(),
                    Charsets.UTF_8.name(),
                ).takeIf { it.isNotBlank() }
                val offerId = entry.arguments?.getString("offerId").orEmpty().takeIf { it.isNotBlank() }
                CreateOfferRoute(
                    shopId = shopId,
                    shopName = shopName,
                    shopPhotoUrl = shopPhotoUrl,
                    offerId = offerId,
                    onBack = { navController.popBackStack() },
                    onSubmitted = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.ADMIN_CONSOLE) {
                AdminConsoleRoute(
                    onBack = { navController.popBackStack() },
                    onShopManagement = { navController.navigate(Routes.ADMIN_SHOP_MANAGEMENT) },
                    onUserManagement = { navController.navigate(Routes.ADMIN_USER_MANAGEMENT) },
                    onShopVerification = { navController.navigate(Routes.ADMIN_SHOP_VERIFICATION) },
                    onOfferVerification = { navController.navigate(Routes.ADMIN_OFFER_VERIFICATION) },
                    onCustomerHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                )
            }
            composable(Routes.ADMIN_SHOP_MANAGEMENT) {
                AdminShopManagementRoute(
                    onBack = { navController.popBackStack() },
                    onOpenShop = { shopId ->
                        navController.navigate(Routes.adminShopManagementDetail(shopId))
                    },
                )
            }
            composable(
                route = Routes.ADMIN_SHOP_MANAGEMENT_DETAIL,
                arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            ) { entry ->
                val shopId = entry.arguments?.getString("shopId").orEmpty()
                AdminShopManagementDetailRoute(
                    shopId = shopId,
                    onBack = { navController.popBackStack() },
                    onOpenVerificationReview = { id ->
                        navController.navigate(Routes.adminShopReview(id))
                    },
                )
            }
            composable(Routes.ADMIN_USER_MANAGEMENT) {
                AdminUserManagementRoute(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ADMIN_SHOP_VERIFICATION) {
                AdminShopVerificationRoute(
                    onBack = { navController.popBackStack() },
                    onReviewShop = { shopId ->
                        navController.navigate(Routes.adminShopReview(shopId))
                    },
                )
            }
            composable(
                route = Routes.ADMIN_SHOP_REVIEW,
                arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            ) { entry ->
                val shopId = entry.arguments?.getString("shopId").orEmpty()
                AdminShopReviewRoute(
                    shopId = shopId,
                    onBack = { navController.popBackStack() },
                    onCompleted = { navController.popBackStack() },
                )
            }
            composable(Routes.ADMIN_OFFER_VERIFICATION) {
                AdminOfferVerificationRoute(
                    onBack = { navController.popBackStack() },
                    onReviewOffer = { offerId ->
                        navController.navigate(Routes.adminOfferReview(offerId))
                    },
                )
            }
            composable(
                route = Routes.ADMIN_OFFER_REVIEW,
                arguments = listOf(navArgument("offerId") { type = NavType.StringType }),
            ) { entry ->
                val offerId = entry.arguments?.getString("offerId").orEmpty()
                AdminOfferReviewRoute(
                    offerId = offerId,
                    onBack = { navController.popBackStack() },
                    onCompleted = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.SHOP_DETAILS,
                arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            ) { entry ->
                val shopId = entry.arguments?.getString("shopId").orEmpty()
                ShopDetailsRoute(
                    shopId = shopId,
                    onBack = { navController.popBackStack() },
                    onOfferClick = { offerId -> navController.navigate(Routes.offerDetails(offerId)) },
                    onDirections = { lat, lng -> openDirections(lat, lng, null) },
                )
            }
            }
            InAppNotificationBannerHost(
                store = app.realtimeNotificationStore,
                onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            appUpdateOffer?.let { offer ->
                AppUpdateDialog(
                    offer = offer,
                    onUpdate = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(offer.downloadUrl))
                        context.startActivity(intent)
                    },
                    onLater = {
                        appUpdateDismissed = true
                        appUpdateOffer = null
                    },
                )
            }
        }
    }
}
