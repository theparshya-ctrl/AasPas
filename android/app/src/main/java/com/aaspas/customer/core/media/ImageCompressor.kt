package com.aaspas.customer.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.aaspas.customer.BuildConfig
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {
    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 82

    fun compressImage(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            val bitmap = decodeBitmap(context, uri)
                ?: run {
                    logDebug("decode_failed uri=$uri")
                    return null
                }
            val scaled = scaleDown(bitmap, MAX_DIMENSION)
            if (scaled !== bitmap) {
                bitmap.recycle()
            }
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                scaled.recycle()
                output.toByteArray().takeIf { it.isNotEmpty() }
            }
        }.onFailure { error ->
            logDebug("compress_failed uri=$uri error=${error.message}")
        }.getOrNull()
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri)?.let { return it }
        }
        return decodeWithBitmapFactory(context, uri)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        }.getOrNull()
    }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= maxDimension || currentHeight / 2 >= maxDimension) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return max(1, sampleSize)
    }

    private fun scaleDown(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val largest = max(width, height)
        if (largest <= maxDimension) return source
        val scale = maxDimension.toFloat() / largest.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}
