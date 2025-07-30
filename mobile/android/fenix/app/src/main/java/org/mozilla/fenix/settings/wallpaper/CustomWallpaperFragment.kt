/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper
import org.mozilla.fenix.settings.wallpaper.CustomWallpaperUtils
import java.io.File

class CustomWallpaperFragment : Fragment() {

    private val wallpaperUseCases by lazy {
        requireComponents.useCases.wallpaperUseCases
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Get existing custom wallpaper files if they exist
        val portraitFile = File(requireContext().filesDir, Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait))
        val landscapeFile = File(requireContext().filesDir, Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape))
        
        val portraitImageUri = if (portraitFile.exists()) Uri.fromFile(portraitFile) else null
        val landscapeImageUri = if (landscapeFile.exists()) Uri.fromFile(landscapeFile) else null
        
        // Get stored single image mode setting
        val useSingleImageMode = requireComponents.settings.customWallpaperUseSingleImage
        
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FirefoxTheme {
                    CustomWallpaper(
                        currentPortraitImageUri = portraitImageUri,
                        currentLandscapeImageUri = landscapeImageUri,
                        initialUseSingleImage = useSingleImageMode,
                        onSaveClick = { portraitUri, landscapeUri, useSingleImage ->
                            if (validateInput(portraitUri, landscapeUri, useSingleImage)) {
                                saveCustomWallpaper(portraitUri, landscapeUri, useSingleImage)
                            }
                        },
                    )
                }
            }
        }
    }

    private fun validateInput(portraitUri: Uri?, landscapeUri: Uri?, useSingleImage: Boolean): Boolean {
        // Ensure at least one image is selected
        if (portraitUri == null && (!useSingleImage && landscapeUri == null)) {
            showError(getString(R.string.wallpaper_error_no_image_selected))
            return false
        }
        
        // If using single image, portrait must be selected
        if (useSingleImage && portraitUri == null) {
            showError(getString(R.string.wallpaper_error_portrait_required))
            return false
        }
        
        return true
    }
    
    private fun saveCustomWallpaper(portraitUri: Uri?, landscapeUri: Uri?, useSingleImage: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Validate images before saving
                portraitUri?.let { uri ->
                    when (val result = CustomWallpaperUtils.validateImageUri(requireContext(), uri)) {
                        is CustomWallpaperUtils.ValidationResult.InvalidFormat -> {
                            showError(getString(R.string.wallpaper_error_invalid_format))
                            return@launch
                        }
                        is CustomWallpaperUtils.ValidationResult.FileTooLarge -> {
                            showError(getString(R.string.wallpaper_error_file_too_large))
                            return@launch
                        }
                        is CustomWallpaperUtils.ValidationResult.Error -> {
                            showError(getString(R.string.wallpaper_error_generic, result.message))
                            return@launch
                        }
                        is CustomWallpaperUtils.ValidationResult.Valid -> {
                            // Continue with save
                        }
                    }
                }
                
                if (!useSingleImage && landscapeUri != null) {
                    when (val result = CustomWallpaperUtils.validateImageUri(requireContext(), landscapeUri)) {
                        is CustomWallpaperUtils.ValidationResult.InvalidFormat -> {
                            showError(getString(R.string.wallpaper_error_invalid_format))
                            return@launch
                        }
                        is CustomWallpaperUtils.ValidationResult.FileTooLarge -> {
                            showError(getString(R.string.wallpaper_error_file_too_large))
                            return@launch
                        }
                        is CustomWallpaperUtils.ValidationResult.Error -> {
                            showError(getString(R.string.wallpaper_error_generic, result.message))
                            return@launch
                        }
                        is CustomWallpaperUtils.ValidationResult.Valid -> {
                            // Continue with save
                        }
                    }
                }
                
                val success = wallpaperUseCases.setCustomWallpaper(portraitUri, landscapeUri, useSingleImage)
                if (success) {
                    // Navigate back to wallpaper settings
                    findNavController().popBackStack()
                } else {
                    showError(getString(R.string.wallpaper_error_save_failed))
                }
            } catch (e: Exception) {
                showError(getString(R.string.wallpaper_error_generic, e.message ?: "Unknown error"))
            }
        }
    }
    
    private fun showError(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.customize_wallpapers))
    }
}