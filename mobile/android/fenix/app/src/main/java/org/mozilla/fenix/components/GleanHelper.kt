/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.concept.fetch.Client
import mozilla.components.support.base.log.logger.Logger

/**
 * Helper function to initialize Glean.
 *
 * [applicationContext] the application context required for glean initialization.
 * [logger] the logger to send logs about initializing Glean.
 * [isTelemetryUploadEnabled] indicate if telemetry should be enabled to be uploaded.
 * [client] an instance of [Client] used to upload metrics.
 */
fun initializeGlean(applicationContext: Context, logger: Logger, isTelemetryUploadEnabled: Boolean, client: Client) {
    logger.debug("Preventing Glean from initializing, since telemetry is disabled")
    return
}
