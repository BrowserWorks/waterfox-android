/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings

const val GCLID_PREFIX = "gclid="

/**
 * A service to determine if marketing onboarding is needed. This will need to be started before
 * onboarding to quickly check install referrer and see if a GCLID reference tag is present.
 *
 * This should be only used when user has not gone through the onboarding flow.
 */
class MarketingAttributionService(private val context: Context) {
    private val logger = Logger("MarketingAttributionService")
    private var referrerClient: InstallReferrerClient? = null

    /**
     * Starts the connection with the install referrer and handle the response.
     */
    @Suppress("CognitiveComplexMethod")
    fun start() {
        val client = InstallReferrerClient.newBuilder(context).build()
        referrerClient = client

        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // Connection established.
                            val installReferrerResponse = try {
                                client.installReferrer.installReferrer
                            } catch (e: RemoteException) {
                                // We can't do anything about this.
                                logger.error("Failed to retrieve install referrer response", e)
                                null
                            } catch (e: SecurityException) {
                                // https://issuetracker.google.com/issues/72926755
                                logger.error("Failed to retrieve install referrer response", e)
                                null
                            }

                            val distributionIdManager = context.components.distributionIdManager

                            if (!installReferrerResponse.isNullOrBlank()) {
                                response = installReferrerResponse
                                val utmParams =
                                    UTMParams.parseUTMParameters(installReferrerResponse)

                                distributionIdManager.updateDistributionIdFromUtmParams(utmParams)
                            }

                            CoroutineScope(Dispatchers.IO).launch {
                                context.settings().shouldShowMarketingOnboarding =
                                    shouldShowMarketingOnboarding(
                                        installReferrerResponse,
                                        distributionIdManager,
                                    )
                            }

                            return
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.PERMISSION_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        -> {
                            context.settings().shouldShowMarketingOnboarding = false
                            return
                        }
                    }

                    // End the connection, and null out the client.
                    stop()
                }

                override fun onInstallReferrerServiceDisconnected() {
                    referrerClient = null
                }
            },
        )
    }

    /**
     * Stops the connection with the install referrer.
     */
    fun stop() {
        referrerClient?.endConnection()
        referrerClient = null
    }

    /**
     * Companion object responsible for determine if a install referrer response should result in
     * showing the marketing onboarding flow.
     */
    companion object {
        private val marketingPrefixes = listOf(GCLID_PREFIX)
        var response: String? = null

        @VisibleForTesting
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

            return marketingPrefixes.any { installReferrerResponse.startsWith(it, ignoreCase = true) }
        }
    }
}
