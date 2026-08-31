package com.aaspas.customer.presentation.components

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aaspas.customer.R
import com.aaspas.customer.core.network.MediaUrlResolver
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSizes
import com.aaspas.customer.presentation.theme.AasPasSpacing
import java.io.File

@Composable
fun ShopPhotoPickerField(
    modifier: Modifier = Modifier,
    photoUrl: String?,
    localPreviewUri: Uri? = null,
    localPreviewBytes: ByteArray? = null,
    isUploading: Boolean,
    enabled: Boolean,
    errorMessage: String?,
    successMessage: String? = null,
    onPhotoPicked: (Uri) -> Unit,
    onSavePhoto: (() -> Unit)? = null,
    onRemovePhoto: () -> Unit,
    fieldTitleRes: Int = R.string.shop_owner_photo,
    previewTitleRes: Int = R.string.shop_owner_photo_preview_title,
    previewDescRes: Int = R.string.shop_owner_photo_preview_desc,
    saveButtonRes: Int = R.string.shop_owner_save_photo,
    chooseGalleryRes: Int = R.string.shop_owner_photo_choose_gallery,
    chooseAnotherRes: Int = R.string.shop_owner_photo_choose_another,
    retakeRes: Int = R.string.shop_owner_photo_retake,
    removeRes: Int = R.string.shop_owner_photo_remove,
    changeRes: Int = R.string.shop_owner_photo_change,
    takeCameraRes: Int = R.string.shop_owner_photo_take_camera,
    uploadingRes: Int = R.string.shop_owner_photo_uploading,
    uploadHintRes: Int = R.string.shop_owner_photo_upload_hint,
    cameraFilePrefix: String = "shop-photo-",
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val hasPendingPhoto = localPreviewUri != null || localPreviewBytes != null

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(onPhotoPicked)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraUri?.let(onPhotoPicked)
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            createCameraUri(context, cameraFilePrefix)?.let { uri ->
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    fun openGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun openCamera() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
        Text(
            text = stringResource(
                if (hasPendingPhoto) {
                    previewTitleRes
                } else {
                    fieldTitleRes
                },
            ),
            style = MaterialTheme.typography.titleSmall,
            color = AasPasColors.TextPrimary,
        )
        val previewModel = when {
            localPreviewBytes != null -> ImageRequest.Builder(context).data(localPreviewBytes).build()
            localPreviewUri != null -> localPreviewUri
            else -> MediaUrlResolver.resolve(photoUrl)
        }
        if (previewModel != null) {
            AsyncImage(
                model = previewModel,
                contentDescription = stringResource(previewDescRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AasPasSizes.offerCardWidth * 0.45f)
                    .clip(RoundedCornerShape(AasPasRadius.md)),
                contentScale = ContentScale.Crop,
            )
        } else {
            DetailsImagePlaceholder(heightFraction = 0.45f)
        }

        if (enabled) {
            if (hasPendingPhoto) {
                if (onSavePhoto != null) {
                    Button(
                        onClick = onSavePhoto,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(saveButtonRes))
                    }
                }
                OutlinedButton(
                    onClick = ::openGallery,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(chooseAnotherRes))
                }
                OutlinedButton(
                    onClick = ::openCamera,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(retakeRes))
                }
                OutlinedButton(
                    onClick = onRemovePhoto,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(removeRes))
                }
            } else {
                Button(
                    onClick = ::openGallery,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (previewModel != null) {
                                changeRes
                            } else {
                                chooseGalleryRes
                            },
                        ),
                    )
                }
                OutlinedButton(
                    onClick = ::openCamera,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(takeCameraRes))
                }
                if (previewModel != null) {
                    OutlinedButton(
                        onClick = onRemovePhoto,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(removeRes))
                    }
                }
            }
        }

        if (isUploading) {
            CircularProgressIndicator(color = AasPasColors.PurplePrimary)
            Text(
                text = stringResource(uploadingRes),
                style = MaterialTheme.typography.bodySmall,
                color = AasPasColors.TextSecondary,
            )
        }
        successMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = AasPasColors.PurpleDark)
        }
        errorMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = AasPasColors.RedCritical)
        }
        if (!hasPendingPhoto) {
            Text(
                text = stringResource(uploadHintRes),
                style = MaterialTheme.typography.bodySmall,
                color = AasPasColors.TextSecondary,
            )
        }
    }
}

private fun createCameraUri(context: Context, filePrefix: String): Uri? {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile(filePrefix, ".jpg", directory)
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
