/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.fenix.R
import org.mozilla.fenix.wallpapers.Wallpaper
import org.mozilla.fenix.wallpapers.WallpaperBitmapUtils
import org.mozilla.fenix.wallpapers.WallpaperFileManager.Companion.MAX_CUSTOM_WALLPAPER_FILE_SIZE_BYTES

/** Loads, validates, and removes user-selected wallpaper images. */
object CustomWallpaperUtils {
    const val WALLPAPER_THUMBNAIL_ASPECT_RATIO = 1.1f

    /** Returns the portrait preview file, falling back to landscape, or null if neither exists. */
    fun getCustomWallpaperPreviewFile(context: Context): File? {
        val portraitFile =
            File(
                context.filesDir,
                Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait),
            )
        val landscapeFile =
            File(
                context.filesDir,
                Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape),
            )

        return when {
            portraitFile.exists() -> portraitFile
            landscapeFile.exists() -> landscapeFile
            else -> null
        }
    }

    /** Loads a sampled URI image in [scope], displaying [defaultImageRes] if decoding fails. */
    fun loadImageFromUriAsync(
        imageView: ImageView,
        uri: Uri,
        scope: CoroutineScope,
        defaultImageRes: Int = R.drawable.ic_file_type_image,
    ) {
        scope.launch {
            val bitmap = loadBitmapFromUri(imageView.context, uri)
            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(defaultImageRes)
                }
            }
        }
    }

    /** Decodes a thumbnail off the main thread, returning null on unreadable or invalid input. */
    suspend fun loadBitmapFromUri(
        context: Context,
        uri: Uri,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                WallpaperBitmapUtils.decodeSampledBitmapFromUri(
                    context = context,
                    uri = uri,
                    maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_PIXELS,
                    maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION,
                )
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }

    /** Checks the reported file size and image bounds, returning provider failures as [ValidationResult.Error]. */
    // Content providers may throw arbitrary unchecked exceptions; validation must retain its error fallback.
    @Suppress("TooGenericExceptionCaught")
    suspend fun validateImageUri(
        context: Context,
        uri: Uri,
    ): ValidationResult =
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver

                val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                fileDescriptor?.use { pfd ->
                    val fileSize = pfd.statSize
                    if (fileSize > MAX_CUSTOM_WALLPAPER_FILE_SIZE_BYTES) {
                        return@withContext ValidationResult.FileTooLarge
                    }
                }

                val options =
                    BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }

                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    return@withContext ValidationResult.InvalidFormat
                }

                ValidationResult.Valid
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ValidationResult.Error(e.message ?: "Unknown error")
            }
        }

    /** Outcome of inspecting a selected wallpaper URI. */
    sealed class ValidationResult {
        /** The image has valid dimensions and does not exceed the reported size limit. */
        object Valid : ValidationResult()

        /** The image decoder could not read valid dimensions. */
        object InvalidFormat : ValidationResult()

        /** The provider reports a file larger than the custom wallpaper limit. */
        object FileTooLarge : ValidationResult()

        /** The provider could not be read, with [message] describing the failure. */
        data class Error(val message: String) : ValidationResult()
    }

    /** Deletes both custom orientations, returning false if deletion fails or access is denied. */
    suspend fun deleteAllCustomWallpapers(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val portraitFile =
                    File(
                        context.filesDir,
                        Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait),
                    )
                val landscapeFile =
                    File(
                        context.filesDir,
                        Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape),
                    )

                var success = true
                if (portraitFile.exists()) {
                    success = portraitFile.delete() && success
                }
                if (landscapeFile.exists()) {
                    success = landscapeFile.delete() && success
                }
                success
            } catch (e: SecurityException) {
                false
            }
        }
}

/** Displays a sampled local wallpaper or [defaultImageRes] when the file cannot be decoded. */
@Composable
fun CustomWallpaperImage(
    file: File?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    defaultImageRes: Int = R.drawable.ic_file_type_image,
) {
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(file) {
        if (file != null && file.exists()) {
            val job =
                scope.launch(Dispatchers.IO) {
                    val loadedBitmap =
                        WallpaperBitmapUtils.decodeSampledBitmapFromFile(
                            file = file,
                            maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_PIXELS,
                            maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION,
                        )
                    withContext(Dispatchers.Main) {
                        bitmap = loadedBitmap
                    }
                }

            onDispose {
                job.cancel()
                bitmap?.recycle()
                bitmap = null
            }
        } else {
            onDispose {}
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(id = defaultImageRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.None,
        )
    }
}

/** Displays a URI-backed wallpaper in an [ImageView], using [defaultImageRes] when unavailable. */
@Composable
fun CustomWallpaperAndroidView(
    uri: Uri?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    defaultImageRes: Int = R.drawable.ic_file_type_image,
) {
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            if (uri != null) {
                CustomWallpaperUtils.loadImageFromUriAsync(
                    imageView = imageView,
                    uri = uri,
                    scope = scope,
                    defaultImageRes = defaultImageRes,
                )
            } else {
                imageView.setImageResource(defaultImageRes)
            }
        },
        modifier = modifier,
    )
}
