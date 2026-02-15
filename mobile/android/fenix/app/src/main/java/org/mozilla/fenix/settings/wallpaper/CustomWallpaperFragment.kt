/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper

/** Validates and saves custom portrait and landscape wallpapers selected by the user. */
class CustomWallpaperFragment : Fragment(), SystemInsetsPaddedFragment {

    private val wallpaperUseCases by lazy {
        requireComponents.useCases.wallpaperUseCases
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val portraitFile =
            File(
                requireContext().filesDir,
                Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait),
            )
        val landscapeFile =
            File(
                requireContext().filesDir,
                Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape),
            )

        val portraitImageUri = if (portraitFile.exists()) Uri.fromFile(portraitFile) else null
        val landscapeImageUri = if (landscapeFile.exists()) Uri.fromFile(landscapeFile) else null
        val useSingleImageMode = requireComponents.settings.customWallpaperUseSingleImage

        return content {
            FirefoxTheme {
                CustomWallpaper(
                    currentPortraitImageUri = portraitImageUri,
                    currentLandscapeImageUri = landscapeImageUri,
                    initialUseSingleImage = useSingleImageMode,
                    onSaveClick = { portraitUri, landscapeUri, useSingleImage ->
                        if (validateInput(portraitUri, landscapeUri)) {
                            saveCustomWallpaper(portraitUri, landscapeUri, useSingleImage)
                        }
                    },
                )
            }
        }
    }

    private fun validateInput(
        portraitUri: Uri?,
        landscapeUri: Uri?,
    ): Boolean {
        if (portraitUri == null && landscapeUri == null) {
            showError(getString(R.string.wallpaper_error_no_image_selected))
            return false
        }

        return true
    }

    // Saving spans provider I/O, preference writes, store updates, and navigation; keep errors at the UI boundary.
    @Suppress("TooGenericExceptionCaught")
    private fun saveCustomWallpaper(
        portraitUri: Uri?,
        landscapeUri: Uri?,
        useSingleImage: Boolean,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!validateSelectedWallpapers(listOfNotNull(portraitUri, landscapeUri).distinct())) {
                    return@launch
                }

                val success =
                    wallpaperUseCases.setCustomWallpaper(
                        portraitUri,
                        landscapeUri,
                        useSingleImage,
                    )
                if (success) {
                    findNavController().popBackStack()
                } else {
                    showError(getString(R.string.wallpaper_error_save_failed))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError(getString(R.string.wallpaper_error_generic, e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun validateSelectedWallpapers(uris: List<Uri>): Boolean {
        for (uri in uris) {
            val errorMessage =
                when (val result = CustomWallpaperUtils.validateImageUri(requireContext(), uri)) {
                    is CustomWallpaperUtils.ValidationResult.InvalidFormat ->
                        getString(R.string.wallpaper_error_invalid_format)
                    is CustomWallpaperUtils.ValidationResult.FileTooLarge ->
                        getString(R.string.wallpaper_error_file_too_large)
                    is CustomWallpaperUtils.ValidationResult.Error ->
                        getString(R.string.wallpaper_error_generic, result.message)
                    is CustomWallpaperUtils.ValidationResult.Valid -> null
                }
            if (errorMessage != null) {
                showError(errorMessage)
                return false
            }
        }
        return true
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
