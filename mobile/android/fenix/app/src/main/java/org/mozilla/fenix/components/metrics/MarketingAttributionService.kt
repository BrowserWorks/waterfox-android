package org.mozilla.fenix.components.metrics

import org.mozilla.fenix.distributions.DistributionIdManager

const val GCLID_PREFIX = "gclid="

class MarketingAttributionService(private val context: Any? = null) {
    fun start() = Unit

    fun stop() = Unit

    companion object {
        var response: String? = null

        internal fun shouldShowMarketingOnboarding(
            installReferrerResponse: String?,
            distributionIdManager: DistributionIdManager,
        ): Boolean {
            return false
        }
    }
}