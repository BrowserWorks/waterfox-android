/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import mozilla.components.lib.crash.CrashReporter

@Suppress("UNUSED_PARAMETER")
class AdjustMetricsService(
    application: Application,
    storage: MetricsStorage,
    crashReporter: CrashReporter,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MetricsService {
    override val type = MetricServiceType.Marketing

    override fun start() = Unit

    override fun stop() = Unit

    override fun track(event: Event) = Unit

    override fun shouldTrack(event: Event): Boolean = false
}