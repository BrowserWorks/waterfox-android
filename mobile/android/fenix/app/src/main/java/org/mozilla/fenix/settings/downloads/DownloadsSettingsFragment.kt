/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.downloads

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.navigation.fragment.navArgs
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlin.ifNullOrEmpty
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.scrollToPreferenceWithHighlight

/** A [androidx.preference.PreferenceFragmentCompat] that displays settings related to downloads. */
class DownloadsSettingsFragment : PreferenceFragmentCompat(), SystemInsetsPaddedFragment {
    private val logger = Logger("DownloadsSettingsFragment")
    private val args by navArgs<DownloadsSettingsFragmentArgs>()
    private lateinit var downloadLocationFormatter: DownloadLocationFormatter

    @VisibleForTesting
    internal var launcher: ActivityResultLauncher<Uri?> =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleSelectedDownloadDirectory(uri)
        }

    /** Launches the SAF folder picker, showing an error toast if no activity can handle it. */
    @VisibleForTesting
    internal fun launchDirectoryPicker() {
        try {
            launcher.launch(null)
        } catch (e: ActivityNotFoundException) {
            logger.warn("No activity found to handle the folder picker intent.", e)
            Toast.makeText(
                    requireContext(),
                    R.string.preferences_downloads_no_folder_picker_available,
                    Toast.LENGTH_LONG,
                )
                .show()
        }
    }

    /**
     * Processes the URI returned from the SAF folder picker, takes persistable permission, and updates the relevant
     * setting.
     *
     * @param uri The URI of the directory selected by the user. Can be null if the user cancelled.
     */
    private fun handleSelectedDownloadDirectory(uri: Uri?) {
        val safeUri = uri ?: return

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            requireContext().contentResolver.takePersistableUriPermission(safeUri, flags)
        } catch (e: SecurityException) {
            logger.error(
                "Failed to take persistable URI permission for the selected downloads directory.",
                e,
            )
        }

        requireComponents.settings.downloadsDefaultLocation = safeUri.toString()
        updateDownloadsLocationSummary()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        downloadLocationFormatter = DefaultDownloadLocationFormatter(DefaultAndroidFileUtils(requireContext()))
        setPreferencesFromResource(R.xml.downloads_settings_preferences, rootKey)
        findPreference<Preference>(getString(R.string.pref_key_downloads_default_location))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                launchDirectoryPicker()
                true
            }
        }

        setUpDeleteBehaviorPreference()

        val fileStorageCategory =
            findPreference<PreferenceCategory>(getString(R.string.pref_key_downloads_storage_category))
        fileStorageCategory?.isVisible = true
    }

    private fun setUpDeleteBehaviorPreference() {
        findPreference<DownloadDeleteBehaviorComposePreference>("pref_key_compose_delete_behavior")?.apply {
            currentBehavior = requireComponents.settings.deleteDownloadBehavior

            onBehaviorSelected = { requireComponents.settings.deleteDownloadBehavior = it }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_downloads_2))
        updateDownloadsLocationSummary()
        args.preferenceToScrollTo?.let {
            scrollToPreferenceWithHighlight(it)
        }
    }

    private fun updateDownloadsLocationSummary() {
        val preference = findPreference<Preference>(getString(R.string.pref_key_downloads_default_location))

        val storedLocation = requireComponents.settings.downloadsDefaultLocation
        val defaultLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        val locationToFormat = storedLocation.ifNullOrEmpty { defaultLocation }

        preference?.summary =
            try {
                downloadLocationFormatter.getFriendlyPath(locationToFormat)
            } catch (e: MissingUriPermission) {
                logger.warn("Resetting download location to default due to lost permissions.", e)
                requireComponents.settings.downloadsDefaultLocation = defaultLocation
                downloadLocationFormatter.getFriendlyPath(defaultLocation)
            }
    }
}
