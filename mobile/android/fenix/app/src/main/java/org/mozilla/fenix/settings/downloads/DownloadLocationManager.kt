/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.settings.downloads

import android.content.ContentResolver
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.net.toUri
import java.io.FileNotFoundException
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.utils.Settings

/** A utility class for managing the default download location. */
class DownloadLocationManager(
    private val settings: Settings,
    private val contentResolver: ContentResolver,
) {
    private val logger = Logger("DownloadLocationManager")

    /** The validated path for the default download location. */
    val defaultLocation: String
        get() = getDownloadsDefaultLocation()

    /**
     * Retrieves the user-configured download location from settings. If the app retains the necessary read and write
     * permissions for that location and the location is still valid, it returns the configured path. Otherwise, it
     * falls back to the public "Downloads" directory.
     *
     * @return The validated path for the default download location as a [String].
     */
    private fun getDownloadsDefaultLocation(): String {
        val defaultFallbackPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

        val configuredLocation = settings.downloadsDefaultLocation

        if (configuredLocation.isEmpty()) {
            return defaultFallbackPath
        }

        val locationUri = configuredLocation.toUri()

        val hasPermissions =
            contentResolver.persistedUriPermissions.any {
                it.uri == locationUri && it.isReadPermission && it.isWritePermission
            }

        if (!hasPermissions) {
            return defaultFallbackPath
        }

        return try {
            val documentUri =
                DocumentsContract.buildDocumentUriUsingTree(
                    locationUri,
                    DocumentsContract.getTreeDocumentId(locationUri),
                )
            val isLocationAccessible =
                contentResolver.query(documentUri, null, null, null, null)?.use {
                    true
                } ?: false

            if (isLocationAccessible) {
                configuredLocation
            } else {
                logger.warn("Resetting download location: query returned null.")
                resetLocation(defaultFallbackPath, null)
            }
        } catch (e: IllegalArgumentException) {
            resetLocation(defaultFallbackPath, e)
        } catch (e: SecurityException) {
            resetLocation(defaultFallbackPath, e)
        } catch (e: FileNotFoundException) {
            resetLocation(defaultFallbackPath, e)
        }
    }

    private fun resetLocation(fallback: String, e: Exception?): String {
        if (e != null) {
            logger.warn("Resetting download location to default due to ${e.javaClass.simpleName}.", e)
        }
        settings.downloadsDefaultLocation = fallback
        return fallback
    }
}
