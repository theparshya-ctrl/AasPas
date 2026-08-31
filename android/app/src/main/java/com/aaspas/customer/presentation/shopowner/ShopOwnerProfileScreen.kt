package com.aaspas.customer.presentation.shopowner

import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.presentation.components.ShopPhotoPickerField
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.PickerTimeField
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing
import kotlinx.coroutines.launch

@Composable
fun ShopOwnerProfileRoute(
    onBack: () -> Unit,
    onProfileSaved: () -> Unit = onBack,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val context = LocalContext.current
    val locationProvider = AndroidLocationProvider(context)
    val scope = rememberCoroutineScope()
    val viewModel: ShopOwnerProfileViewModel = viewModel(
        factory = ShopOwnerProfileViewModelFactory(app, app.shopOwnerRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onProfileSaved()
            viewModel.onNavigateBackHandled()
        }
    }

    ShopOwnerProfileScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onContactNumberChange = viewModel::onContactNumberChange,
        onPhotoPicked = viewModel::onPhotoPicked,
        onPhotoRemoved = viewModel::onPhotoRemoved,
        onSavePhoto = viewModel::savePendingPhoto,
        onPhotoSuccessShown = viewModel::onPhotoSuccessShown,
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
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOwnerProfileScreen(
    uiState: ShopOwnerProfileUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onContactNumberChange: (String) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onPhotoRemoved: () -> Unit,
    onSavePhoto: () -> Unit,
    onPhotoSuccessShown: () -> Unit,
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
    onSave: () -> Unit,
) {
    val photoOnlyMode = uiState.canEditPhoto && !uiState.canEdit && uiState.status == ShopOwnerProfileLoadStatus.Loaded
    val photoSuccessMessage = if (uiState.photoSaveSucceeded) {
        stringResource(R.string.shop_owner_photo_updated_success)
    } else {
        null
    }

    LaunchedEffect(uiState.photoSaveSucceeded) {
        if (uiState.photoSaveSucceeded) {
            kotlinx.coroutines.delay(4_000)
            onPhotoSuccessShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (photoOnlyMode) {
                                R.string.shop_owner_manage_photo
                            } else {
                                R.string.shop_owner_edit_profile
                            },
                        ),
                    )
                },
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
            ShopOwnerProfileLoadStatus.Loading,
            ShopOwnerProfileLoadStatus.Saving,
            -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = AasPasColors.PurplePrimary)
                }
            }
            ShopOwnerProfileLoadStatus.Unauthorized,
            ShopOwnerProfileLoadStatus.Error,
            -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            ShopOwnerProfileLoadStatus.Loaded,
            ShopOwnerProfileLoadStatus.Saved,
            -> {
                val photoOnlyMode = uiState.canEditPhoto && !uiState.canEdit
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(AasPasSpacing.lg)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                ) {
                    uiState.statusMessage?.let { message ->
                        Text(text = message, color = AasPasColors.PurpleDark, style = MaterialTheme.typography.bodyMedium)
                    }
                    when {
                        photoOnlyMode -> {
                            Text(
                                text = stringResource(R.string.shop_owner_profile_photo_only),
                                color = AasPasColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ShopPhotoPickerField(
                                photoUrl = uiState.photoUrl.takeIf { it.isNotBlank() },
                                localPreviewUri = uiState.pendingPhotoUri?.let(Uri::parse),
                                localPreviewBytes = uiState.pendingPhotoBytes,
                                isUploading = uiState.isUploadingPhoto,
                                enabled = true,
                                errorMessage = uiState.photoErrorMessage,
                                successMessage = photoSuccessMessage,
                                onPhotoPicked = onPhotoPicked,
                                onSavePhoto = onSavePhoto,
                                onRemovePhoto = onPhotoRemoved,
                            )
                        }
                        !uiState.canEdit -> {
                            Text(
                                text = stringResource(R.string.shop_owner_profile_read_only),
                                color = AasPasColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (!photoOnlyMode) {
                    ProfileField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = stringResource(R.string.shop_owner_shop_name),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.category,
                        onValueChange = onCategoryChange,
                        label = stringResource(R.string.shop_owner_category),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.description,
                        onValueChange = onDescriptionChange,
                        label = stringResource(R.string.shop_owner_description),
                        enabled = uiState.canEdit,
                        singleLine = false,
                    )
                    ProfileField(
                        value = uiState.contactNumber,
                        onValueChange = onContactNumberChange,
                        label = stringResource(R.string.shop_owner_phone),
                        enabled = uiState.canEdit,
                        keyboardType = KeyboardType.Phone,
                    )
                        ShopPhotoPickerField(
                            photoUrl = uiState.photoUrl.takeIf { it.isNotBlank() },
                            localPreviewUri = uiState.pendingPhotoUri?.let(Uri::parse),
                            localPreviewBytes = uiState.pendingPhotoBytes,
                            isUploading = uiState.isUploadingPhoto,
                            enabled = uiState.canEdit || uiState.canEditPhoto,
                            errorMessage = uiState.photoErrorMessage,
                            successMessage = photoSuccessMessage,
                            onPhotoPicked = onPhotoPicked,
                            onSavePhoto = onSavePhoto,
                            onRemovePhoto = onPhotoRemoved,
                        )
                    PickerTimeField(
                        value = uiState.opensAt,
                        onValueChange = onOpensAtChange,
                        label = stringResource(R.string.shop_owner_opens_at),
                        enabled = uiState.canEdit,
                    )
                    PickerTimeField(
                        value = uiState.closesAt,
                        onValueChange = onClosesAtChange,
                        label = stringResource(R.string.shop_owner_closes_at),
                        enabled = uiState.canEdit,
                    )
                    Text(
                        text = stringResource(R.string.shop_owner_location_section),
                        style = MaterialTheme.typography.titleSmall,
                        color = AasPasColors.TextPrimary,
                    )
                    ProfileField(
                        value = uiState.locationDraft.addressLine1,
                        onValueChange = onAddressLine1Change,
                        label = stringResource(R.string.shop_owner_address_line1),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.locationDraft.addressLine2,
                        onValueChange = onAddressLine2Change,
                        label = stringResource(R.string.shop_owner_address_line2),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.locationDraft.city,
                        onValueChange = onCityChange,
                        label = stringResource(R.string.shop_owner_city),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.locationDraft.state,
                        onValueChange = onStateChange,
                        label = stringResource(R.string.shop_owner_state),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.locationDraft.postalCode,
                        onValueChange = onPostalCodeChange,
                        label = stringResource(R.string.shop_owner_pincode),
                        enabled = uiState.canEdit,
                    )
                    ProfileField(
                        value = uiState.locationDraft.latitude,
                        onValueChange = onLatitudeChange,
                        label = stringResource(R.string.shop_owner_latitude),
                        enabled = uiState.canEdit,
                        keyboardType = KeyboardType.Decimal,
                    )
                    ProfileField(
                        value = uiState.locationDraft.longitude,
                        onValueChange = onLongitudeChange,
                        label = stringResource(R.string.shop_owner_longitude),
                        enabled = uiState.canEdit,
                        keyboardType = KeyboardType.Decimal,
                    )
                    if (uiState.canEdit) {
                        OutlinedButton(onClick = onUseCurrentLocation, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.shop_owner_use_gps))
                        }
                        uiState.validationMessage?.let { message ->
                            Text(text = message, color = AasPasColors.RedCritical)
                        }
                        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.shop_owner_save_profile))
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
