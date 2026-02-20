/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ext.packageManagerWrapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.fake.FakeMetricController
import org.mozilla.fenix.distributions.DistributionBrowserStoreProvider
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.distributions.DistributionProviderChecker
import org.mozilla.fenix.distributions.DistributionSettings
import org.mozilla.fenix.ext.settings

@RunWith(AndroidJUnit4::class)
internal class MarketingAttributionServiceTest {

    private var providerValue: String? = null
    private var storedId: String? = null
    private var savedId: String = ""

    private val testDistributionProviderChecker = object : DistributionProviderChecker {
        override suspend fun queryProvider(): String? = providerValue
    }

    private val testBrowserStoreProvider = object : DistributionBrowserStoreProvider {
        override fun getDistributionId(): String? = storedId

        override fun updateDistributionId(id: String) {
            storedId = id
        }
    }

    private val testDistributionSettings = object : DistributionSettings {
        override fun getDistributionId(): String = savedId

        override fun saveDistributionId(id: String) {
            savedId = id
        }

        override fun setMarketingTelemetryPreferences() = Unit
    }

    val distributionIdManager = DistributionIdManager(
        packageManager = testContext.packageManagerWrapper,
        testBrowserStoreProvider,
        distributionProviderChecker = testDistributionProviderChecker,
        distributionSettings = testDistributionSettings,
        metricController = FakeMetricController(),
        appPreinstalledOnVivoDevice = { true },
    )

    @Before
    fun setUp() {
        MarketingAttributionService.response = null
        testContext.settings().shouldShowMarketingOnboarding = true
    }

    @Test
    fun `WHEN installReferrerResponse is empty or null THEN we should not show marketing onboarding`() =
        runBlocking {
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(" ", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is in the marketing prefixes THEN we should show marketing onboarding`() =
        runBlocking {
            assertTrue(MarketingAttributionService.shouldShowMarketingOnboarding("gclid=", distributionIdManager))
            assertTrue(MarketingAttributionService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
            assertTrue(MarketingAttributionService.shouldShowMarketingOnboarding("adjust_reftag=", distributionIdManager))
            assertTrue(MarketingAttributionService.shouldShowMarketingOnboarding("adjust_reftag=test", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is not in the marketing prefixes THEN we should show marketing onboarding`() =
        runBlocking {
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(" gclid=12345", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("utm_source=google-play&utm_medium=organic", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("utm_source=(not%20set)&utm_medium=(not%20set)", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("utm_source=eea-browser-choice&utm_medium=preload", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("gclida=", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("adjust_reftag_test", distributionIdManager))
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding("test", distributionIdManager))
        }

    @Test
    fun `GIVEN a partnership distribution WHEN we should skip the marketing screen THEN we skip it`() =
        runBlocking {
            distributionIdManager.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_001)
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_002)
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_003)
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.XIAOMI_001)
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.AURA_001)
            assertFalse(MarketingAttributionService.shouldShowMarketingOnboarding(null, distributionIdManager))
        }
}
