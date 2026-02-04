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
import org.mozilla.fenix.nimbus.FxNimbus

private const val GCLID_PREFIX = "gclid="

/**
 * A service that fetches the install referrer and stores it for use after the user accepts
 * the Terms of Service.
 *
 * **WARNING:** This service IS started before the user accepts the Terms of Service.
 * Do NOT use the stored [InstallReferrerHandlingService.response] in any code path that fires
 * telemetry or makes network calls until after ToS is accepted.
 *
 * @param context The application context.
 * @param scope Coroutine scope used to launch background work.
 */
class InstallReferrerHandlingService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private val logger = Logger("InstallReferrerHandlingService")

    @VisibleForTesting
    internal var clientFactory: (Context) -> InstallReferrerClientWrapper = ::DefaultInstallReferrerClient

    /**
     * Starts the connection with the install referrer and handle the response.
     */
    fun start() {
        val client = clientFactory(context)

        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val installReferrerResponse = try {
                                client.getInstallReferrer()
                            } catch (e: RemoteException) {
                                logger.error("Failed to retrieve install referrer response", e)
                                null
                            } catch (e: SecurityException) {
                                logger.error("Failed to retrieve install referrer response", e)
                                null
                            }

                            val distributionIdManager = context.components.distributionIdManager

                            if (!installReferrerResponse.isNullOrBlank()) {
                                response = installReferrerResponse
                                val utmParams = UTMParams.parseUTMParameters(installReferrerResponse)
                                distributionIdManager.updateDistributionIdFromUtmParams(utmParams)
                            }

                            scope.launch {
                                context.components.settings.shouldShowMarketingOnboarding =
                                    shouldShowMarketingOnboarding(
                                        installReferrerResponse,
                                        distributionIdManager,
                                    )
                            }

                            safeEndConnection(client)
                            return
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.PERMISSION_ERROR,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED,
                        -> {
                            context.components.settings.shouldShowMarketingOnboarding = false
                            safeEndConnection(client)
                            return
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    context.components.settings.shouldShowMarketingOnboarding = false
                    safeEndConnection(client)
                }
            },
        )
    }

    /**
     * Companion object responsible for determine if a install referrer response should result in
     * showing the marketing onboarding flow.
     */
    companion object {
        private val marketingPrefixes = listOf(GCLID_PREFIX)

        @Suppress("TooGenericExceptionCaught")
        private fun safeEndConnection(client: InstallReferrerClientWrapper) {
            try {
                client.endConnection()
            } catch (e: Exception) {
                // endConnection can throw if the binding is already dead.
            }
        }

        /**
         * The raw install referrer string. Only read this after the user has accepted ToS —
         * do not use it to trigger telemetry or network calls before consent is given.
         */
        var response: String? = null

        @VisibleForTesting
        internal fun isMetaAttribution(installReferrerResponse: String?): Boolean {
            if (installReferrerResponse.isNullOrBlank()) {
                return false
            }

            val utmParams = UTMParams.parseUTMParameters(installReferrerResponse)
            return MetaParams.extractMetaAttribution(utmParams.content) != null
        }


        @Suppress("ReturnCount")
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

            if (!FxNimbus.features.marketingOnboardingCard.value().enabled) {
                return false
            }

            if (isMetaAttribution(installReferrerResponse)) {
                return true
            }

            return marketingPrefixes.any { installReferrerResponse.startsWith(it, ignoreCase = true) }
        }
    }
}
