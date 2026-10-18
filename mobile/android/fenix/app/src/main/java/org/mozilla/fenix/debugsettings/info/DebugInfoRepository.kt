/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.info

import android.os.Build
import mozilla.components.Build as ComponentsBuild
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.utils.Settings
import org.mozilla.geckoview.BuildConfig as GeckoViewBuildConfig

/** An interface for providing application debug information to display. */
fun interface DebugInfoRepository {

    /** Builds the [DebugInfoSection] this repository is responsible for. */
    suspend fun getSection(): DebugInfoSection
}

/**
 * [DebugInfoRepository] for the application build information.
 *
 * @param versionName The application version name.
 */
class BuildDebugInfoRepository(private val versionName: String) : DebugInfoRepository {
    override suspend fun getSection() =
        buildSection("Build") {
            textItem("Version", versionName)
            textItem("VCS Commit", ComponentsBuild.GIT_HASH.ifBlank { "-" })
            textItem("GeckoView", "${GeckoViewBuildConfig.MOZ_APP_VERSION}-${GeckoViewBuildConfig.MOZ_APP_BUILDID}")
            textItem("Application Services", ComponentsBuild.APPLICATION_SERVICES_VERSION)
            textItem("Glean SDK", ComponentsBuild.GLEAN_SDK_VERSION)
            textItem("Build Date", BuildConfig.BUILD_DATE)
        }
}

/**
 * [DebugInfoRepository] for the device information.
 *
 * @param localeTag The BCP-47 language tag of the current locale.
 */
class DeviceDebugInfoRepository(private val localeTag: String) : DebugInfoRepository {
    override suspend fun getSection() =
        buildSection("Device") {
            textItem("Android", "Android ${Build.VERSION.RELEASE}")
            textItem("Manufacturer", Build.MANUFACTURER)
            textItem("Model", Build.MODEL)
            textItem("Locale", localeTag)
        }
}

/** [DebugInfoRepository] for the build configuration. */
class ConfigurationDebugInfoRepository : DebugInfoRepository {
    override suspend fun getSection() =
        buildSection("Configuration") {
            statusItem("Telemetry", BuildConfig.TELEMETRY)
            statusItem("Crash reporting", BuildConfig.CRASH_REPORTING)
        }
}

/**
 * [DebugInfoRepository] for the homepage feature settings.
 *
 * @param settings [Settings] used to read the homepage states.
 */
class HomepageDebugInfoRepository(private val settings: Settings) : DebugInfoRepository {
    override suspend fun getSection() =
        buildSection("Homepage") {
            statusItem("Top sites", settings.showTopSitesFeature)
            statusItem("Jump back in", settings.showRecentTabsFeature)
            statusItem("Bookmarks", settings.showBookmarksHomeFeature)
            statusItem("Recently visited", settings.historyMetadataUIFeature)
            statusItem("Stories", settings.showPocketRecommendationsFeature)
            statusItem("Sponsored stories", settings.showPocketSponsoredStories)
            statusItem("Synced tabs", settings.showSyncedTabs)
            statusItem("Collections", settings.collections)
            statusItem("Privacy report", settings.showPrivacyReportFeature)
        }
}

/**
 * [DebugInfoRepository] for the toolbar settings.
 *
 * @param settings [Settings] used to read the toolbar states.
 */
class ToolbarDebugInfoRepository(private val settings: Settings) : DebugInfoRepository {
    override suspend fun getSection() =
        buildSection("Toolbar") {
            textItem("Position", settings.toolbarPosition.name)
            statusItem("Expanded toolbar", settings.shouldUseExpandedToolbar)
            statusItem("Tab strip", settings.isTabStripEnabled)
        }
}

/**
 * [DebugInfoRepository] that fetches the persisted secret settings.
 *
 * @param preferenceKeys The list of preference keys declared in secret settings.
 * @param settings [Settings] used for accessing user preferences.
 */
class SecretSettingsDebugInfoRepository(
    private val preferenceKeys: List<String>,
    private val settings: Settings,
) : DebugInfoRepository {
    override suspend fun getSection() =
        buildSection("Secret settings") {
            // Get the list of secret settings preference where the setting was explicitly toggled by the user or by
            // code.
            val preferences = settings.preferences.all
            val storedKeys = preferenceKeys.filter { preferences.containsKey(it) }.sorted()

            if (storedKeys.isEmpty()) {
                textItem("Persisted settings", "-")
            } else {
                storedKeys.forEach { key ->
                    when (val value = preferences[key]) {
                        is Boolean -> statusItem(key, value)
                        else -> textItem(key, value.toString())
                    }
                }
            }
        }
}
