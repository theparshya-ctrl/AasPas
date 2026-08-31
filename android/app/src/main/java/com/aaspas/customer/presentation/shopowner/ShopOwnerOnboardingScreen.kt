package com.aaspas.customer.presentation.shopowner



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.Button

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text

import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.input.KeyboardType

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.lifecycle.viewmodel.compose.viewModel

import com.aaspas.customer.AasPasApplication

import com.aaspas.customer.R

import com.aaspas.customer.core.common.userMessage

import com.aaspas.customer.core.location.AndroidLocationProvider

import com.aaspas.customer.presentation.components.PickerTimeField

import com.aaspas.customer.presentation.components.ShopPhotoPickerField
import com.aaspas.customer.presentation.theme.AasPasColors

import com.aaspas.customer.presentation.theme.AasPasSpacing

import kotlinx.coroutines.launch



@Composable

fun ShopOwnerOnboardingRoute(

    onBack: () -> Unit,

    onShopCreated: () -> Unit,

) {

    val app = LocalContext.current.applicationContext as AasPasApplication

    val context = LocalContext.current

    val locationProvider = AndroidLocationProvider(context)

    val scope = rememberCoroutineScope()

    val viewModel: ShopOwnerOnboardingViewModel = viewModel(

        factory = ShopOwnerOnboardingViewModelFactory(app, app.shopOwnerRepository),

    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()



    LaunchedEffect(uiState.status, uiState.createdShopId) {

        if (uiState.status == ShopOwnerOnboardingStatus.Success && uiState.createdShopId != null) {

            onShopCreated()

        }

    }



    ShopOwnerOnboardingScreen(

        uiState = uiState,

        onBack = onBack,

        onNameChange = viewModel::onNameChange,

        onCategoryChange = viewModel::onCategoryChange,

        onDescriptionChange = viewModel::onDescriptionChange,

        onContactNumberChange = viewModel::onContactNumberChange,

        onPhotoPicked = viewModel::onPhotoPicked,
        onPhotoRemoved = viewModel::onPhotoRemoved,

        onOpensAtChange = viewModel::onOpensAtChange,

        onClosesAtChange = viewModel::onClosesAtChange,

        onAddressLine1Change = viewModel::onAddressLine1Change,

        onAddressLine2Change = viewModel::onAddressLine2Change,

        onCityChange = viewModel::onCityChange,

        onStateChange = viewModel::onStateChange,

        onPostalCodeChange = viewModel::onPostalCodeChange,

        onLatitudeChange = viewModel::onLatitudeChange,

        onLongitudeChange = viewModel::onLongitudeChange,

        onUseCurrentLocation = {

            scope.launch {

                if (!locationProvider.hasLocationPermission()) return@launch

                locationProvider.getLastLocation()?.let { coords ->

                    viewModel.applyGpsLocation(coords.latitude, coords.longitude)

                }

            }

        },

        onCreateShop = viewModel::createShop,

        onRetry = viewModel::retryAfterError,

    )

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun ShopOwnerOnboardingScreen(

    uiState: ShopOwnerOnboardingUiState,

    onBack: () -> Unit,

    onNameChange: (String) -> Unit,

    onCategoryChange: (String) -> Unit,

    onDescriptionChange: (String) -> Unit,

    onContactNumberChange: (String) -> Unit,

    onPhotoPicked: (android.net.Uri) -> Unit,
    onPhotoRemoved: () -> Unit,

    onOpensAtChange: (String) -> Unit,

    onClosesAtChange: (String) -> Unit,

    onAddressLine1Change: (String) -> Unit,

    onAddressLine2Change: (String) -> Unit,

    onCityChange: (String) -> Unit,

    onStateChange: (String) -> Unit,

    onPostalCodeChange: (String) -> Unit,

    onLatitudeChange: (String) -> Unit,

    onLongitudeChange: (String) -> Unit,

    onUseCurrentLocation: () -> Unit,

    onCreateShop: () -> Unit,

    onRetry: () -> Unit,

) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text(stringResource(R.string.shop_owner_onboarding_title)) },

                navigationIcon = {

                    IconButton(onClick = onBack) {

                        Icon(

                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,

                            contentDescription = stringResource(R.string.back),

                        )

                    }

                },

            )

        },

        containerColor = AasPasColors.Background,

    ) { padding ->

        when (uiState.status) {

            ShopOwnerOnboardingStatus.Submitting -> {

                Column(

                    modifier = Modifier.padding(padding).fillMaxSize(),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center,

                ) {

                    CircularProgressIndicator(color = AasPasColors.PurplePrimary)

                    Text(

                        text = stringResource(R.string.shop_owner_onboarding_creating),

                        modifier = Modifier.padding(top = AasPasSpacing.md),

                        color = AasPasColors.TextSecondary,

                    )

                }

            }

            ShopOwnerOnboardingStatus.Error -> {

                Column(

                    modifier = Modifier

                        .padding(padding)

                        .padding(AasPasSpacing.lg)

                        .fillMaxSize(),

                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),

                ) {

                    Text(

                        text = stringResource(R.string.shop_owner_onboarding_error_title),

                        style = MaterialTheme.typography.titleMedium,

                        color = AasPasColors.TextPrimary,

                    )

                    Text(

                        text = uiState.error?.userMessage()

                            ?: stringResource(R.string.error_generic),

                        color = AasPasColors.RedCritical,

                    )

                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {

                        Text(stringResource(R.string.retry))

                    }

                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {

                        Text(stringResource(R.string.back))

                    }

                }

            }

            ShopOwnerOnboardingStatus.Success -> {

                Column(

                    modifier = Modifier

                        .padding(padding)

                        .padding(AasPasSpacing.lg)

                        .fillMaxSize(),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center,

                ) {

                    Text(

                        text = stringResource(R.string.shop_owner_onboarding_success),

                        style = MaterialTheme.typography.titleMedium,

                        color = AasPasColors.PurpleDark,

                    )

                    Text(

                        text = stringResource(R.string.shop_owner_onboarding_success_redirect),

                        modifier = Modifier.padding(top = AasPasSpacing.sm),

                        style = MaterialTheme.typography.bodyMedium,

                        color = AasPasColors.TextSecondary,

                    )

                    CircularProgressIndicator(

                        modifier = Modifier.padding(top = AasPasSpacing.lg),

                        color = AasPasColors.PurplePrimary,

                    )

                }

            }

            ShopOwnerOnboardingStatus.Ready -> {

                Column(

                    modifier = Modifier

                        .padding(padding)

                        .padding(AasPasSpacing.lg)

                        .fillMaxSize()

                        .verticalScroll(rememberScrollState()),

                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),

                ) {

                    Text(

                        text = stringResource(R.string.shop_owner_onboarding_subtitle),

                        style = MaterialTheme.typography.bodyLarge,

                        color = AasPasColors.TextSecondary,

                    )

                    OnboardingField(

                        value = uiState.name,

                        onValueChange = onNameChange,

                        label = stringResource(R.string.shop_owner_shop_name),

                    )

                    OnboardingField(

                        value = uiState.category,

                        onValueChange = onCategoryChange,

                        label = stringResource(R.string.shop_owner_category),

                    )

                    OnboardingField(

                        value = uiState.description,

                        onValueChange = onDescriptionChange,

                        label = stringResource(R.string.shop_owner_description),

                        singleLine = false,

                    )

                    OnboardingField(

                        value = uiState.contactNumber,

                        onValueChange = onContactNumberChange,

                        label = stringResource(R.string.shop_owner_phone),

                        keyboardType = KeyboardType.Phone,

                    )

                    ShopPhotoPickerField(
                        photoUrl = uiState.photoUrl.takeIf { it.isNotBlank() },
                        localPreviewUri = uiState.pendingPhotoUri?.let(android.net.Uri::parse),
                        localPreviewBytes = uiState.pendingPhotoBytes,
                        isUploading = uiState.isUploadingPhoto,
                        enabled = true,
                        errorMessage = uiState.photoErrorMessage,
                        onPhotoPicked = onPhotoPicked,
                        onRemovePhoto = onPhotoRemoved,
                    )

                    PickerTimeField(

                        value = uiState.opensAt,

                        onValueChange = onOpensAtChange,

                        label = stringResource(R.string.shop_owner_opens_at),

                    )

                    PickerTimeField(

                        value = uiState.closesAt,

                        onValueChange = onClosesAtChange,

                        label = stringResource(R.string.shop_owner_closes_at),

                    )

                    Text(

                        text = stringResource(R.string.shop_owner_location_section),

                        style = MaterialTheme.typography.titleSmall,

                        color = AasPasColors.TextPrimary,

                    )

                    OnboardingField(

                        value = uiState.locationDraft.addressLine1,

                        onValueChange = onAddressLine1Change,

                        label = stringResource(R.string.shop_owner_address_line1),

                    )

                    OnboardingField(

                        value = uiState.locationDraft.addressLine2,

                        onValueChange = onAddressLine2Change,

                        label = stringResource(R.string.shop_owner_address_line2),

                    )

                    OnboardingField(

                        value = uiState.locationDraft.city,

                        onValueChange = onCityChange,

                        label = stringResource(R.string.shop_owner_city),

                    )

                    OnboardingField(

                        value = uiState.locationDraft.state,

                        onValueChange = onStateChange,

                        label = stringResource(R.string.shop_owner_state),

                    )

                    OnboardingField(

                        value = uiState.locationDraft.postalCode,

                        onValueChange = onPostalCodeChange,

                        label = stringResource(R.string.shop_owner_pincode),

                    )

                    OnboardingField(

                        value = uiState.locationDraft.latitude,

                        onValueChange = onLatitudeChange,

                        label = stringResource(R.string.shop_owner_latitude),

                        keyboardType = KeyboardType.Decimal,

                    )

                    OnboardingField(

                        value = uiState.locationDraft.longitude,

                        onValueChange = onLongitudeChange,

                        label = stringResource(R.string.shop_owner_longitude),

                        keyboardType = KeyboardType.Decimal,

                    )

                    OutlinedButton(onClick = onUseCurrentLocation, modifier = Modifier.fillMaxWidth()) {

                        Text(stringResource(R.string.shop_owner_use_gps))

                    }

                    uiState.validationMessage?.let { message ->

                        Text(text = message, color = AasPasColors.RedCritical)

                    }

                    Button(

                        onClick = onCreateShop,

                        enabled = uiState.status != ShopOwnerOnboardingStatus.Submitting,

                        modifier = Modifier.fillMaxWidth(),

                    ) {

                        Text(stringResource(R.string.shop_owner_onboarding_create))

                    }

                }

            }

        }

    }

}



@Composable

private fun OnboardingField(

    value: String,

    onValueChange: (String) -> Unit,

    label: String,

    singleLine: Boolean = true,

    keyboardType: KeyboardType = KeyboardType.Text,

) {

    OutlinedTextField(

        value = value,

        onValueChange = onValueChange,

        label = { Text(label) },

        singleLine = singleLine,

        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),

        modifier = Modifier.fillMaxWidth(),

    )

}


