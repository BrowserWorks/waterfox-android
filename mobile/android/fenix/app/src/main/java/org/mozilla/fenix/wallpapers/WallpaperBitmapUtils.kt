/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.VisibleForTesting
import java.io.File

object WallpaperBitmapUtils {
    const val MAX_WALLPAPER_PIXELS = 16 * 1024 * 1024
    const val MAX_WALLPAPER_DIMENSION = 4096
    const val MAX_WALLPAPER_THUMBNAIL_PIXELS = 512 * 512
    const val MAX_WALLPAPER_THUMBNAIL_DIMENSION = 512

    fun decodeSampledBitmapFromFile(
        file: File,
        maxPixels: Int = MAX_WALLPAPER_PIXELS,
        maxDimension: Int = MAX_WALLPAPER_DIMENSION,
    ): Bitmap? = decodeSafely {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@decodeSafely null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxPixels = maxPixels,
                maxDimension = maxDimension,
            )
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }

    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxPixels: Int = MAX_WALLPAPER_PIXELS,
        maxDimension: Int = MAX_WALLPAPER_DIMENSION,
    ): Bitmap? = decodeSafely {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, bounds)
        } ?: return@decodeSafely null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return@decodeSafely null
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxPixels = maxPixels,
                maxDimension = maxDimension,
            )
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }

    @VisibleForTesting
    internal fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxPixels: Int,
        maxDimension: Int,
    ): Int {
        if (width <= 0 || height <= 0) {
            return 1
        }

        val safeMaxPixels = maxPixels.coerceAtLeast(1)
        val safeMaxDimension = maxDimension.coerceAtLeast(1)
        var inSampleSize = 1
        var scaledWidth = width
        var scaledHeight = height
        var scaledPixels = scaledWidth.toLong() * scaledHeight

        while (
            scaledWidth > safeMaxDimension ||
            scaledHeight > safeMaxDimension ||
            scaledPixels > safeMaxPixels
        ) {
            inSampleSize *= 2
            scaledWidth = (width / inSampleSize).coerceAtLeast(1)
            scaledHeight = (height / inSampleSize).coerceAtLeast(1)
            scaledPixels = scaledWidth.toLong() * scaledHeight
        }

        return inSampleSize
    }

    private fun decodeSafely(block: () -> Bitmap?): Bitmap? = try {
        block()
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}
