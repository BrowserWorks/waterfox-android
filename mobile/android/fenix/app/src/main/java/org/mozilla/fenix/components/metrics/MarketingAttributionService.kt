/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import androidx.annotation.VisibleForTesting
import mozilla.components.support.base.log.logger.Logger

const val GCLID_PREFIX = "gclid="
const val ADJUST_REFTAG_PREFIX = "adjust_reftag="

/**
 * A service to determine if marketing onboarding is needed. This will need to be started before
 * onboarding to quickly check install referrer and see if GLICD or Adjust reference tag is present.
 *
 * This should be only used when user has not gone through the onboarding flow.
 */
class MarketingAttributionService(private val context: Context) {
    private val logger = Logger("MarketingAttributionService")

    /**
     * Starts the connection with the install referrer and handle the response.
     */
    fun start() {
    }

    /**
     * Stops the connection with the install referrer.
     */
    fun stop() {
    }

    /**
     * Companion object responsible for determine if a install referrer response should result in
     * showing the marketing onboarding flow.
     */
    companion object {
        private val marketingPrefixes = listOf(GCLID_PREFIX, ADJUST_REFTAG_PREFIX)

        @VisibleForTesting
        internal fun shouldShowMarketingOnboarding(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) {
                return false
            }

            return marketingPrefixes.any { installReferrerResponse.startsWith(it, ignoreCase = true) }
        }
    }
}
