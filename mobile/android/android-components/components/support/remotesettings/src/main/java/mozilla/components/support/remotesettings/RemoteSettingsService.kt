/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.remotesettings

import android.content.Context
import android.os.Build
import mozilla.appservices.remotesettings.RemoteSettingsConfig
import mozilla.appservices.remotesettings.RemoteSettingsContext
import mozilla.appservices.remotesettings.RemoteSettingsServer
import java.util.Locale
import mozilla.appservices.remotesettings.RemoteSettingsService as AppServicesRemoteSettingsService
import mozilla.components.Build as AcBuild

/**
 * Wrapper around the app-services RemoteSettingsServer
 *
 * @param context [Context] that we get the storage directory from.  This is where cached records
 * get stored.
 * @param remoteSettingsServer [RemoteSettingsServer] to download from.
 */
class RemoteSettingsService(
    context: Context,
    server: RemoteSettingsServer,
    channel: String = "release",
    isLargeScreenSize: Boolean = false,
) {
    val remoteSettingsService: AppServicesRemoteSettingsService by lazy {
        val appContext = generateAppContext(context, channel, isLargeScreenSize)
        val databasePath = context.getDir("remote-settings", Context.MODE_PRIVATE).absolutePath
        AppServicesRemoteSettingsService(
            databasePath,
            RemoteSettingsConfig(server = server, appContext = appContext),
        )
    }
}

/**
 * Generate an app context to configure the `RemoteSettingsService` with.
 *
 * This is what's used for JEXL filtering.
 */
internal fun generateAppContext(
    context: Context,
    channel: String,
    isLargeScreenSize: Boolean,
    locale: Locale? = null,
): RemoteSettingsContext {
    val resolvedLocale = locale ?: Locale.getDefault()
    val formFactor = if (isLargeScreenSize) "tablet" else "phone"
    return RemoteSettingsContext(
        channel = channel,
        appVersion = AcBuild.VERSION,
        appId = context.packageName,
        locale = resolvedLocale.toLanguageTag(),
        os = "Android",
        osVersion = Build.VERSION.RELEASE,
        formFactor = formFactor,
        country = resolvedLocale.country,
    )
}

/**
 * Data class representing the RemoteSettingsConfig in appservices.
 */
data class RemoteSettingsServerConfig(
    var server: RemoteSettingsServer? = null,
    var bucketName: String? = null,
    var appContext: RemoteSettingsContext? = null,
)

/**
 * Convert [mozilla.components.support.remotesettings.RemoteSettingsServerConfig] into [RemoteSettingsConfig].
 */
fun mozilla.components.support.remotesettings.RemoteSettingsServerConfig.into(): RemoteSettingsConfig {
    return RemoteSettingsConfig(
        this.server,
        this.bucketName,
        this.appContext,
    )
}
