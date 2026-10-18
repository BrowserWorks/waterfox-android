/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.store

import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.store.CrashReportOption
import org.mozilla.fenix.crashes.crashReportOption
import org.mozilla.fenix.utils.Settings

/** The repository for managing user privacy preferences set during onboarding. */
interface PrivacyPreferencesRepository {

    /**
     * Retrieves the state of a specific preference.
     *
     * @param type The type of preference to retrieve.
     * @return Returns `true` if the preference is enabled.
     */
    fun getPreference(type: PreferenceType): Boolean

    /**
     * Updates a specific preference.
     *
     * @param type The type of preference to modify.
     * @param enabled The new state of the preference.
     */
    fun setPreference(type: PreferenceType, enabled: Boolean)
}

/** Enum representing the types of privacy preferences available. */
enum class PreferenceType {
    CrashReporting,
    UsageData,
}

/**
 * The default implementation of [PrivacyPreferencesRepository].
 *
 * @param settings The [Settings] instance for accessing and modifying privacy-related settings.
 * @param crashReporter The [CrashReporter] instance for propagating the telemetry setting.
 */
class DefaultPrivacyPreferencesRepository(
    private val settings: Settings,
    private val crashReporter: CrashReporter,
) : PrivacyPreferencesRepository {

    override fun getPreference(type: PreferenceType): Boolean {
        return when (type) {
            PreferenceType.CrashReporting -> {
                when (settings.crashReportOption()) {
                    CrashReportOption.Auto -> true
                    CrashReportOption.Ask -> false
                    CrashReportOption.Never -> false
                }
            }
            PreferenceType.UsageData -> settings.isTelemetryEnabled
        }
    }

    override fun setPreference(
        type: PreferenceType,
        enabled: Boolean,
    ) {
        when (type) {
            PreferenceType.CrashReporting -> {
                if (enabled) {
                    settings.crashReportChoice = CrashReportOption.Auto.label
                } else {
                    settings.crashReportChoice = CrashReportOption.Ask.label
                }
            }
            PreferenceType.UsageData -> {
                settings.isTelemetryEnabled = enabled
                crashReporter.setTelemetryEnabled(enabled)
                settings.isExperimentationEnabled = enabled

                // `metrics.stop(MetricServiceType.Data)` isn't required
                // as no metrics should be started at this point during onboarding.
                // IMPORTANT: definitely do not introduce `metrics.start(MetricServiceType.Data)`!

                // `engine.notifyTelemetryPrefChanged` isn't required, because the engine should not be used yet.
            }
        }
    }
}
