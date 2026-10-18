/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState

/** A middleware that will map incoming actions to relevant events for [metrics]. */
class MetricsMiddleware(
    private val metrics: MetricController,
) : Middleware<AppState, AppAction> {
    override fun invoke(
        store: Store<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        handleAction(action)
        next(action)
    }

    private fun handleAction(action: AppAction) =
        when (action) {
            is AppAction.AppLifecycleAction.ResumeAction -> {
                metrics.track(Event.GrowthData.ConversionEvent1)
                metrics.track(Event.GrowthData.ConversionEvent2)
                // Conversion event 3 handled in [TelemetryMiddleware]
                metrics.track(Event.GrowthData.ConversionEvent4)
                // Conversion event 5 handled in [MetricController]
                // Conversion event 6 handled in [OnboardingFragment]
                metrics.track(Event.GrowthData.ConversionEvent7(fromSearch = false))
                metrics.track(Event.FirstWeekPostInstall.ConversionEvent8)
                metrics.track(Event.FirstWeekPostInstall.ConversionEvent9)
                metrics.track(Event.FirstWeekPostInstall.ConversionEvent10)
            }

            is AppAction.BookmarkAction.BookmarkAdded -> {
                MetricsUtils.recordBookmarkAddMetric(action.source)
            }

            else -> Unit
        }
}
