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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.fenix.R
import org.mozilla.fenix.wallpapers.Wallpaper
import java.io.File
import java.io.IOException

/**
 * Utility object containing constants and helper functions for custom wallpaper functionality.
 */
object CustomWallpaperUtils {
    
    /**
     * Default aspect ratio for wallpaper thumbnails.
     */
    const val WALLPAPER_THUMBNAIL_ASPECT_RATIO = 1.1f
    
    /**
     * Maximum file size for custom wallpaper images (10MB).
     */
    const val MAX_WALLPAPER_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    
    /**
     * Supported image MIME types for custom wallpapers.
     */
    val SUPPORTED_MIME_TYPES = listOf(
        "image/jpeg",
        "image/jpg", 
        "image/png",
        "image/webp"
    )
    
    /**
     * Get the custom wallpaper preview file based on available images.
     * Returns portrait file if exists, otherwise landscape file, or null if neither exists.
     */
    fun getCustomWallpaperPreviewFile(context: Context): File? {
        val portraitFile = File(
            context.filesDir, 
            Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait)
        )
        val landscapeFile = File(
            context.filesDir,
            Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape)
        )
        
        return when {
            portraitFile.exists() -> portraitFile
            landscapeFile.exists() -> landscapeFile
            else -> null
        }
    }
    
    /**
     * Load an image from URI into an ImageView using coroutines.
     * Handles errors gracefully and sets a default image on failure.
     */
    fun loadImageFromUriAsync(
        imageView: ImageView,
        uri: Uri,
        scope: CoroutineScope,
        defaultImageRes: Int = R.drawable.ic_file_type_image
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
    
    /**
     * Load a bitmap from a URI with proper error handling.
     * Returns null if loading fails.
     */
    suspend fun loadBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }
    
    /**
     * Validate if a URI points to a valid image file.
     * Checks MIME type and file size.
     */
    suspend fun validateImageUri(
        context: Context,
        uri: Uri
    ): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Check MIME type
            val mimeType = contentResolver.getType(uri)
            if (mimeType == null || mimeType !in SUPPORTED_MIME_TYPES) {
                return@withContext ValidationResult.InvalidFormat
            }
            
            // Check file size
            val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.use { pfd ->
                val fileSize = pfd.statSize
                if (fileSize > MAX_WALLPAPER_FILE_SIZE) {
                    return@withContext ValidationResult.FileTooLarge
                }
            }
            
            // Try to decode to ensure it's a valid image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext ValidationResult.InvalidFormat
            }
            
            ValidationResult.Valid
        } catch (e: Exception) {
            ValidationResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Result of image validation.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        object InvalidFormat : ValidationResult()
        object FileTooLarge : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
    
    /**
     * Delete all custom wallpaper files.
     */
    suspend fun deleteAllCustomWallpapers(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val portraitFile = File(
                context.filesDir,
                Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait)
            )
            val landscapeFile = File(
                context.filesDir,
                Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape)
            )
            
            var success = true
            if (portraitFile.exists()) {
                success = portraitFile.delete() && success
            }
            if (landscapeFile.exists()) {
                success = landscapeFile.delete() && success
            }
            success
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Composable that displays a custom wallpaper image with proper lifecycle management.
 */
@Composable
fun CustomWallpaperImage(
    file: File?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    defaultImageRes: Int = R.drawable.ic_file_type_image
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    DisposableEffect(file) {
        if (file != null && file.exists()) {
            val job = scope.launch(Dispatchers.IO) {
                try {
                    val loadedBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    withContext(Dispatchers.Main) {
                        bitmap = loadedBitmap
                    }
                } catch (e: Exception) {
                    // Handle error silently, will show default image
                }
            }
            
            onDispose {
                job.cancel()
                bitmap?.recycle()
                bitmap = null
            }
        } else {
            onDispose { }
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = defaultImageRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.None
        )
    }
}

/**
 * Composable that displays a custom wallpaper using AndroidView with proper lifecycle management.
 */
@Composable
fun CustomWallpaperAndroidView(
    uri: Uri?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    defaultImageRes: Int = R.drawable.ic_file_type_image
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
                    defaultImageRes = defaultImageRes
                )
            } else {
                imageView.setImageResource(defaultImageRes)
            }
        },
        modifier = modifier
    )
}