package com.aaspas.customer.presentation.shopowner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.domain.model.MerchantOfferCounts
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.MerchantShopStatus
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.presentation.components.NotificationBellButton
import com.aaspas.customer.presentation.components.AboutAppSection
import com.aaspas.customer.presentation.components.DetailsRemoteImage
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ShopOwnerDashboardRoute(
    shouldRefreshDashboard: Boolean = false,
    showShopCreatedSuccess: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    onShopCreatedSuccessHandled: () -> Unit = {},
    onBack: () -> Unit,
    onCreateOffer: (shopId: String, shopName: String, shopPhotoUrl: String?) -> Unit,
    onManageOffers: (shopId: String) -> Unit,
    onEditProfile: () -> Unit,
    onStartOnboarding: () -> Unit,
    onCustomerHome: () -> Unit,
    onNotifications: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: ShopOwnerDashboardViewModel = viewModel(
        factory = ShopOwnerDashboardViewModelFactory(
            app.shopOwnerRepository,
            app.notificationRepository,
            app.realtimeNotificationStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAutoRedirectedToOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(showShopCreatedSuccess) {
        if (showShopCreatedSuccess) {
            viewModel.showShopCreatedSuccess(
                app.getString(R.string.shop_owner_onboarding_success),
            )
            onShopCreatedSuccessHandled()
        }
    }

    LaunchedEffect(shouldRefreshDashboard) {
        if (shouldRefreshDashboard) {
            viewModel.load(preserveCreatedMessage = true)
            onRefreshHandled()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.status, shouldRefreshDashboard) {
        if (shouldRefreshDashboard || uiState.status == ShopOwnerLoadStatus.Loading) return@LaunchedEffect
        if (uiState.status == ShopOwnerLoadStatus.Empty && !hasAutoRedirectedToOnboarding) {
            hasAutoRedirectedToOnboarding = true
            onStartOnboarding()
        }
    }

    ShopOwnerDashboardScreen(
        uiState = uiState,
        onRetry = viewModel::load,
        onBack = onBack,
        onCreateOffer = onCreateOffer,
        onManageOffers = onManageOffers,
        onEditProfile = onEditProfile,
        onCustomerHome = onCustomerHome,
        onNotifications = onNotifications,
        onSubmitShop = viewModel::submitShopForVerification,
        onStartOnboarding = onStartOnboarding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOwnerDashboardScreen(
    uiState: ShopOwnerDashboardUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onCreateOffer: (shopId: String, shopName: String, shopPhotoUrl: String?) -> Unit,
    onManageOffers: (shopId: String) -> Unit,
    onEditProfile: () -> Unit,
    onCustomerHome: () -> Unit,
    onNotifications: () -> Unit,
    onSubmitShop: () -> Unit,
    onStartOnboarding: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shop_owner_dashboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    NotificationBellButton(
                        unreadCount = uiState.unreadNotificationCount,
                        onClick = onNotifications,
                    )
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            ShopOwnerLoadStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = AasPasColors.PurplePrimary)
                }
            }
            ShopOwnerLoadStatus.Unauthorized -> {
                EmptyState(
                    title = stringResource(R.string.shop_owner_unauthorized_title),
                    hint = stringResource(R.string.shop_owner_unauthorized_hint),
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            ShopOwnerLoadStatus.Empty -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(AasPasSpacing.lg)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = AasPasColors.PurplePrimary)
                    Text(
                        text = stringResource(R.string.shop_owner_onboarding_redirect),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AasPasColors.TextSecondary,
                    )
                    OutlinedButton(onClick = onStartOnboarding, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.shop_owner_onboarding_create))
                    }
                }
            }
            ShopOwnerLoadStatus.Error -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            ShopOwnerLoadStatus.Loaded,
            ShopOwnerLoadStatus.Submitting,
            -> {
                val dashboard = uiState.dashboard ?: return@Scaffold
                ShopOwnerDashboardContent(
                    dashboard = dashboard,
                    isSubmitting = uiState.status == ShopOwnerLoadStatus.Submitting,
                    submitMessage = uiState.submitMessage,
                    createdSuccessMessage = uiState.createdSuccessMessage,
                    modifier = Modifier.padding(padding),
                    onCreateOffer = onCreateOffer,
                    onManageOffers = onManageOffers,
                    onEditProfile = onEditProfile,
                    onCustomerHome = onCustomerHome,
                    onSubmitShop = onSubmitShop,
                )
            }
        }
    }
}

@Composable
private fun ShopOwnerDashboardContent(
    dashboard: ShopOwnerDashboard,
    isSubmitting: Boolean,
    submitMessage: String?,
    createdSuccessMessage: String?,
    modifier: Modifier = Modifier,
    onCreateOffer: (shopId: String, shopName: String, shopPhotoUrl: String?) -> Unit,
    onManageOffers: (shopId: String) -> Unit,
    onEditProfile: () -> Unit,
    onCustomerHome: () -> Unit,
    onSubmitShop: () -> Unit,
) {
    val shop = dashboard.shop
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        dashboard.statusMessage?.let { message ->
            StatusBanner(message = message)
        }
        createdSuccessMessage?.let { message ->
            StatusBanner(message = message)
        }
        submitMessage?.let { message ->
            StatusBanner(message = message)
        }
        ShopStatusSection(shop = shop, isVerified = dashboard.isVerified)
        ShopProfileSummarySection(shop = shop)
        OfferCountsSection(counts = dashboard.offerCounts)
        when (shop.status) {
            MerchantShopStatus.Draft,
            MerchantShopStatus.Rejected,
            -> {
                OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.shop_owner_edit_profile))
                }
                Button(
                    onClick = onSubmitShop,
                    enabled = !isSubmitting && dashboard.canEditProfile,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (shop.status == MerchantShopStatus.Rejected) {
                            stringResource(R.string.shop_owner_submit_again)
                        } else {
                            stringResource(R.string.shop_owner_submit_for_verification)
                        },
                    )
                }
            }
            else -> Unit
        }
        Button(onClick = { onCreateOffer(shop.id, shop.name, shop.photoUrl) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shop_owner_create_offer))
        }
        OutlinedButton(onClick = { onManageOffers(shop.id) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shop_owner_manage_offers))
        }
        if (shop.status !in setOf(MerchantShopStatus.Draft, MerchantShopStatus.Rejected)) {
            when (shop.status) {
                MerchantShopStatus.Active -> {
                    OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.shop_owner_manage_photo))
                    }
                }
                MerchantShopStatus.PendingApproval -> {
                    OutlinedButton(
                        onClick = onEditProfile,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.shop_owner_edit_profile))
                    }
                }
                else -> Unit
            }
        }
        OutlinedButton(onClick = onCustomerHome, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shop_owner_view_customer_home))
        }
        AboutAppSection()
    }
}

@Composable
private fun StatusBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AasPasColors.PurpleLight),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(AasPasSpacing.md),
            style = MaterialTheme.typography.bodyMedium,
            color = AasPasColors.PurpleDark,
        )
    }
}

@Composable
private fun ShopStatusSection(shop: MerchantShopProfile, isVerified: Boolean) {
    DashboardSection(title = stringResource(R.string.shop_owner_section_status)) {
        DashboardInfoRow(stringResource(R.string.shop_owner_shop_name), shop.name)
        DashboardInfoRow(
            stringResource(R.string.shop_owner_verification_status),
            if (isVerified) {
                stringResource(R.string.shop_owner_verified)
            } else {
                stringResource(R.string.shop_owner_not_verified)
            },
        )
        DashboardInfoRow(
            stringResource(R.string.shop_owner_approval_status),
            shopStatusLabel(shop.status),
        )
    }
}

@Composable
private fun ShopProfileSummarySection(shop: MerchantShopProfile) {
    val location = shop.location
    DashboardSection(title = stringResource(R.string.shop_owner_section_profile)) {
        DetailsRemoteImage(
            photoUrl = shop.photoUrl,
            contentDescription = stringResource(R.string.shop_image_desc, shop.name),
            heightFraction = 0.28f,
        )
        shop.category?.let { DashboardInfoRow(stringResource(R.string.shop_owner_category), it) }
        location?.let {
            DashboardInfoRow(
                stringResource(R.string.shop_owner_area),
                listOfNotNull(it.city, it.state).joinToString(", "),
            )
            DashboardInfoRow(
                stringResource(R.string.shop_owner_location),
                listOfNotNull(it.latitude, it.longitude).joinToString(", "),
            )
        }
        shop.contactNumber?.let {
            DashboardInfoRow(stringResource(R.string.shop_owner_phone), it)
        }
        shop.businessHours?.let {
            DashboardInfoRow(
                stringResource(R.string.shop_owner_business_hours),
                "${it.opensAt} – ${it.closesAt}",
            )
        }
    }
}

@Composable
private fun OfferCountsSection(counts: MerchantOfferCounts) {
    DashboardSection(title = stringResource(R.string.shop_owner_section_offers)) {
        DashboardInfoRow(stringResource(R.string.shop_owner_count_active), counts.active.toString())
        DashboardInfoRow(stringResource(R.string.shop_owner_count_coming_soon), counts.comingSoon.toString())
        DashboardInfoRow(stringResource(R.string.shop_owner_count_draft), counts.draft.toString())
        DashboardInfoRow(stringResource(R.string.shop_owner_count_pending), counts.pendingApproval.toString())
        DashboardInfoRow(stringResource(R.string.shop_owner_count_rejected), counts.rejected.toString())
        DashboardInfoRow(stringResource(R.string.shop_owner_count_expired), counts.expired.toString())
    }
}

@Composable
private fun DashboardSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AasPasColors.TextPrimary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
        ) {
            Column(
                modifier = Modifier.padding(AasPasSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DashboardInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AasPasColors.TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = AasPasColors.TextPrimary)
    }
}

@Composable
private fun shopStatusLabel(status: MerchantShopStatus): String {
    return when (status) {
        MerchantShopStatus.Draft -> stringResource(R.string.shop_owner_status_draft)
        MerchantShopStatus.PendingApproval -> stringResource(R.string.shop_owner_status_pending)
        MerchantShopStatus.Rejected -> stringResource(R.string.shop_owner_status_rejected)
        MerchantShopStatus.Active -> stringResource(R.string.shop_owner_status_active)
        MerchantShopStatus.Unknown -> stringResource(R.string.shop_owner_status_unknown)
    }
}
