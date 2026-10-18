/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.info

import org.mozilla.fenix.utils.Settings

/**
 * Aggregates the [DebugInfoSection]s provided by each [DebugInfoRepository].
 *
 * @param repositories The list of [DebugInfoRepository]s that provides
 */
class DebugInfoProvider(private val repositories: List<DebugInfoRepository>) {

    /** Returns a list of [DebugInfoSection]s to display. */
    suspend fun getDebugInfo(): List<DebugInfoSection> = repositories.map { it.getSection() }

    companion object {

        /**
         * Creates a [DebugInfoProvider] with the default set of [DebugInfoRepository]s.
         *
         * @param settings [Settings] used to check the application shared preferences.
         * @param versionName The application version name.
         * @param deviceLocale The BCP-47 language tag of the current locale.
         * @param secretSettingsKeys The list of preference keys declared in secret settings.
         */
        fun create(
            settings: Settings,
            versionName: String,
            deviceLocale: String,
            secretSettingsKeys: List<String>,
        ): DebugInfoProvider =
            DebugInfoProvider(
                repositories =
                    listOf(
                        BuildDebugInfoRepository(versionName),
                        DeviceDebugInfoRepository(localeTag = deviceLocale),
                        ConfigurationDebugInfoRepository(),
                        HomepageDebugInfoRepository(settings),
                        ToolbarDebugInfoRepository(settings),
                        SecretSettingsDebugInfoRepository(
                            preferenceKeys = secretSettingsKeys,
                            settings = settings,
                        ),
                    )
            )
    }
}
