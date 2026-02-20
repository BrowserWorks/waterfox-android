/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import org.mozilla.fenix.distributions.DistributionIdManager

const val GCLID_PREFIX = "gclid="
private const val ADJUST_REFTAG_PREFIX = "adjust_reftag="

/**
 * Dependency-free placeholder service.
 *
 * Marketing attribution collection has been removed, so lifecycle methods are no-ops.
 * The onboarding decision logic is preserved in [shouldShowMarketingOnboarding].
 */
class MarketingAttributionService(
    @Suppress("UNUSED_PARAMETER") context: Any? = null,
) {
    fun start() = Unit

    fun stop() = Unit

    companion object {
        private val marketingPrefixes = listOf(
            GCLID_PREFIX,
            ADJUST_REFTAG_PREFIX,
        )

        var response: String? = null

        internal suspend fun shouldShowMarketingOnboarding(
            installReferrerResponse: String?,
            distributionIdManager: DistributionIdManager,
        ): Boolean {
            if (distributionIdManager.isPartnershipDistribution()) {
                return !distributionIdManager.shouldSkipMarketingConsentScreen()
            }

            if (installReferrerResponse.isNullOrBlank()) {
                return false
            }

            return marketingPrefixes.any { prefix ->
                installReferrerResponse.startsWith(prefix, ignoreCase = true)
            }
        }
    }
}