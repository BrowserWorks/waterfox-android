/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.distributions

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.ext.PackageManagerWrapper

import org.mozilla.fenix.Config

import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.components.metrics.MetricServiceType
import org.mozilla.fenix.components.metrics.UTMParams
import java.io.File
import java.util.Locale

/**
 * This file will be present on vivo devices that have Firefox preinstalled.
 * Note: The file is added by the manufacturer.
 */
private const val VIVO_PREINSTALLED_FIREFOX_FILE_PATH = "/data/yzfswj/another/vivo_firefox.txt"
private const val VIVO_MANUFACTURER = "vivo"

private const val VIVO_INDIA_UTM_CAMPAIGN = "vivo-india-preinstall"

private const val DT_PROVIDER = "digital_turbine"
private const val DT_TELEFONICA_PACKAGE = "com.dti.telefonica"
private const val DT_VERIZON_PACKAGE = "com.logiagroup.logiadeck"
private const val DT_CRICKET_PACKAGE = "com.dti.cricket"
private const val DT_TRACFONE_PACKAGE = "com.dti.tracfone"

private const val AURA_PROVIDER = "aura"

private val logger = Logger(DistributionIdManager::class.simpleName)

/**
 * Class used to manage distribution Ids for distribution deals with third parties
 *
 * @param packageManager device package manager for checking installed packages
 * @param browserStoreProvider used to update and fetch the stored distribution Id
 * @param distributionProviderChecker used for checking content providers for a distribution provider
 * @param distributionSettings used to persist and retrieve the distribution ID
 * @param metricController a controller used to start Adjust.
 * @param appPreinstalledOnVivoDevice checks if the vivo preinstalled file exists.
 * @param isDtTelefonicaInstalled checks if the DT telefonica app is installed on the device
 * @param isDtUsaInstalled checks if one of the DT USA carrier apps is installed on the device
 */
class DistributionIdManager(
    private val packageManager: PackageManagerWrapper,
    private val browserStoreProvider: DistributionBrowserStoreProvider,
    private val distributionProviderChecker: DistributionProviderChecker,
    private val distributionSettings: DistributionSettings,
    private val metricController: MetricController,
    private val appPreinstalledOnVivoDevice: () -> Boolean = { wasAppPreinstalledOnVivoDevice() },
    private val isDtTelefonicaInstalled: () -> Boolean = { isDtTelefonicaInstalled(packageManager) },
    private val isDtUsaInstalled: () -> Boolean = { isDtUsaInstalled(packageManager) },
) {

    private var distribution: Distribution? = null

    /**
     * Gets the distribution Id that is used to specify which distribution deal this install
     * is associated with.
     *
     * @return the distribution ID if one exists.
     */
    suspend fun getDistributionId(): String {
        distribution?.let { return it.id }

        val provider = distributionProviderChecker.queryProvider()

        val isProviderDigitalTurbine = isProviderDigitalTurbine(provider)

        val savedId = distributionSettings.getDistributionId()

        val distribution = when {
            savedId.isNotBlank() -> Distribution.fromId(savedId)
            isProviderDigitalTurbine && isDtTelefonicaInstalled() -> Distribution.DT_001
            isProviderDigitalTurbine && isDtUsaInstalled() -> Distribution.DT_002
            isProviderDigitalTurbine -> Distribution.DT_003
            isProviderAura(provider) -> Distribution.AURA_001
            isDeviceVivo() && appPreinstalledOnVivoDevice() -> Distribution.VIVO_001
            else -> Distribution.DEFAULT
        }

        setDistribution(distribution)

        return distribution.id
    }

    /**
     * Gets the distribution type that is used to specify which distribution deal this install
     * is associated with.
     *
     * @return the distribution type.
     */
    suspend fun getDistribution(): Distribution {
        return Distribution.fromId(getDistributionId())
    }

    /**
     * Checks the campaign UTM parameters from the google play install referrer response
     * and updates the distribution ID if necessary
     *
     * @param utmParams the UTM parameters from the google play install referrer response
     */
    fun updateDistributionIdFromUtmParams(utmParams: UTMParams) {
        when {
            utmParams.campaign.contains(VIVO_INDIA_UTM_CAMPAIGN) -> {
                setDistribution(Distribution.VIVO_001)
            }

            utmParams.campaign.contains(Distribution.XIAOMI_001.id) -> {
                setDistribution(Distribution.XIAOMI_001)
            }
        }
    }

    /**
     * Check if we can skip the marketing consent screen during onboarding based on the distribution
     *
     * @return true if the marketing consent screen can be skipped during onboarding
     */
    suspend fun shouldSkipMarketingConsentScreen(): Boolean {
        val adjustStartupStrategy = getDistributionAdjustStartupStrategy()

        return when (adjustStartupStrategy) {
            DistributionAdjustStartupStrategy.NONE,
            DistributionAdjustStartupStrategy.SHOW_CONSENT_SCREEN,
                -> false

            DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA,
            DistributionAdjustStartupStrategy.IMMEDIATE_WITH_PLAY_STORE_KIDS,
                -> true
        }
    }

    /**
     * Get the Adjust startup strategy for the current distribution.
     *
     * @return the Adjust startup strategy.
     */
    suspend fun getDistributionAdjustStartupStrategy(): DistributionAdjustStartupStrategy {
        val id = Distribution.fromId(getDistributionId())

        return when (id) {
            Distribution.DEFAULT -> DistributionAdjustStartupStrategy.NONE

            Distribution.VIVO_001,
            Distribution.DT_001,
            Distribution.DT_002,
            Distribution.DT_003,
            Distribution.XIAOMI_001,
                -> DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA

            Distribution.AURA_001 -> if (Config.channel.isNightlyOrDebug) {
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_PLAY_STORE_KIDS
            } else {
                DistributionAdjustStartupStrategy.SHOW_CONSENT_SCREEN
            }
        }
    }

    /**
     * Check if the distribution is part of a distribution deal
     *
     * @return true if the distribution is part of a distribution deal
     */
    suspend fun isPartnershipDistribution(): Boolean {
        val id = Distribution.fromId(getDistributionId())

        return when (id) {
            Distribution.DEFAULT -> false
            Distribution.VIVO_001 -> true
            Distribution.DT_001 -> true
            Distribution.DT_002 -> true
            Distribution.DT_003 -> true
            Distribution.AURA_001 -> true
            Distribution.XIAOMI_001 -> true
        }
    }

    /**
     * Sets the proper marketing telemetry preferences and starts Adjust if the
     * current distribution is one that should skip the marketing data sharing
     * consent screen.
     */
    suspend fun startAdjustIfSkippingConsentScreen() {
        if (shouldSkipMarketingConsentScreen()) {
            distributionSettings.setMarketingTelemetryPreferences()
            metricController.start(MetricServiceType.Marketing)
        }
    }

    private fun isDeviceVivo(): Boolean {
        return Build.MANUFACTURER?.lowercase(Locale.getDefault())?.contains(VIVO_MANUFACTURER)
            ?: false
    }

    private fun isProviderDigitalTurbine(provider: String?): Boolean = provider == DT_PROVIDER

    private fun isProviderAura(provider: String?): Boolean = provider == AURA_PROVIDER

    /**
     * This enum represents distribution IDs that are used in glean metrics.
     */
    enum class Distribution(val id: String) {
        DEFAULT(id = "Mozilla"),
        VIVO_001(id = "vivo-001"),
        DT_001(id = "dt-001"),
        DT_002(id = "dt-002"),
        DT_003(id = "dt-003"),
        AURA_001(id = "aura-001"),
        XIAOMI_001(id = "xiaomi-001"),
        ;

        companion object {
            /**
             * Get the distribution from a distribution ID string.
             */
            fun fromId(id: String): Distribution {
                return entries.find { it.id == id } ?: DEFAULT
            }
        }
    }

    @VisibleForTesting
    internal fun setDistribution(distribution: Distribution) {
        this.distribution = distribution
        browserStoreProvider.updateDistributionId(distribution.id)
        distributionSettings.saveDistributionId(distribution.id)
    }
}

/**
 * This enum represents how / when adjust starts up for distributions.
 */
enum class DistributionAdjustStartupStrategy {
    NONE,

    // Show the adjust data collection consent screen during onboarding and start
    // adjust after the user has consented.
    SHOW_CONSENT_SCREEN,

    // Start adjust immediately but enabled COPPA mode in adjust. COPPA mode will prevent
    // adjust from collecting personal identifiers and sharing data with third parties.
    IMMEDIATE_WITH_COPPA,

    // Start adjust immediately but enable Play Store Kids mode in adjust. This mode will prevent
    // adjust from collecting personal identifiers.
    IMMEDIATE_WITH_PLAY_STORE_KIDS,
}

/**
 * Checks for a file in the device that indicates if the app was preinstalled on a vivo device
 */
private fun wasAppPreinstalledOnVivoDevice(): Boolean {
    return try {
        File(VIVO_PREINSTALLED_FIREFOX_FILE_PATH).exists()
    } catch (e: SecurityException) {
        logger.error("File access denied", e)
        false
    }
}

/**
 * Checks if the Digital Turbine Telefonica app exists on the device
 */
private fun isDtTelefonicaInstalled(packageManager: PackageManagerWrapper): Boolean {
    val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
    return packages.any { it.packageName == DT_TELEFONICA_PACKAGE }
}

/**
 * Checks if one of the Digital Turbine USA apps exist on the device
 */
private fun isDtUsaInstalled(packageManager: PackageManagerWrapper): Boolean {
    val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
    return packages.any {
        val packageName = it.packageName.lowercase()
        packageName == DT_VERIZON_PACKAGE ||
            packageName == DT_CRICKET_PACKAGE ||
            packageName == DT_TRACFONE_PACKAGE
    }
}
