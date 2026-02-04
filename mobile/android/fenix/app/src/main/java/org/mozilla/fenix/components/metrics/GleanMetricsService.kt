/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.RunWhenReadyQueue
import org.mozilla.fenix.ext.components

/** Service responsible for sending the activation and installation pings. */
class GleanMetricsService(
    context: Context,
    @Suppress("UNUSED_PARAMETER") // Retain constructor defaults without queuing telemetry.
    runWhenReadyQueue: RunWhenReadyQueue = context.components.performance.visualCompletenessQueue,
) : MetricsService {
    override val type = MetricServiceType.Data

    private val logger = Logger("GleanMetricsService")
    private var initialized = false

    override fun start() {
        logger.debug("Data collection is disabled, not initializing Glean.")
        initialized = true
    }

    override fun stop() = Unit

    override fun track(event: Event) = Unit

    override fun shouldTrack(event: Event): Boolean {
        return false
    }
}
