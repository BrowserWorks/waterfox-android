/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.experiments

import android.content.Context
import mozilla.appservices.remotesettings.RemoteSettingsService
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.nimbus.NimbusDisabled
import mozilla.components.service.nimbus.messaging.FxNimbusMessaging
import mozilla.components.service.nimbus.messaging.NimbusSystem
import org.mozilla.experiments.nimbus.NimbusInterface
import org.mozilla.experiments.nimbus.internal.GeckoPrefHandler
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings

/**
 * Create the Nimbus singleton object for the Fenix app.
 */
@Suppress("UNUSED_PARAMETER")
fun createNimbus(
    context: Context,
    urlString: String?,
    remoteSettingsService: RemoteSettingsService?,
    geckoPrefHandler: GeckoPrefHandler,
): NimbusApi {
    return NimbusDisabled(context)
}

/**
 * Call `fetchExperiments` if the time since the last fetch is over a threshold.
 *
 * The threshold is given by the [NimbusSystem] feature object, defined in the
 * `nimbus.fml.yaml` file.
 */
fun NimbusInterface.maybeFetchExperiments(
    context: Context,
    feature: NimbusSystem = FxNimbusMessaging.features.nimbusSystem.value(),
    currentTimeMillis: Long = System.currentTimeMillis(),
) {
    if (context.settings().nimbusUsePreview) {
        context.settings().nimbusLastFetchTime = 0L
        fetchExperiments()
    } else {
        val minimumPeriodMinutes = feature.refreshIntervalForeground
        val lastFetchTimeMillis = context.settings().nimbusLastFetchTime
        val minimumPeriodMillis = minimumPeriodMinutes * Settings.ONE_MINUTE_MS

        if (currentTimeMillis - lastFetchTimeMillis >= minimumPeriodMillis) {
            context.settings().nimbusLastFetchTime = currentTimeMillis
            fetchExperiments()
        }
    }
}
