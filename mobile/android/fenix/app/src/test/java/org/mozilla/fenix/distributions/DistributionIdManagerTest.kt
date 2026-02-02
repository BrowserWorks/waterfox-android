/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.distributions

import kotlinx.coroutines.runBlocking
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ext.packageManagerWrapper

import org.junit.After
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.fake.FakeMetricController
import org.mozilla.fenix.components.metrics.MetricServiceType
import org.mozilla.fenix.components.metrics.UTMParams

import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import kotlin.collections.listOf

@RunWith(RobolectricTestRunner::class)
class DistributionIdManagerTest {


    private var providerValue: String? = null
    private var legacyProviderValue: String? = null
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

    @After
    fun tearDown() {
        providerValue = null
        legacyProviderValue = null
        storedId = null
        savedId = ""
        ShadowBuild.reset()
    }

    @Test
    fun `WHEN a device is made by vivo AND the vivo distribution file is found THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                appPreinstalledOnVivoDevice = { true },
            )

            // Mock Build.MANUFACTURER to simulate a Vivo device
            ShadowBuild.setManufacturer("vivo")

            val distributionId = subject.getDistributionId()

            assertEquals("vivo-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.VIVO_001, distribution)
        }

    @Test
    fun `WHEN a device is not made by vivo AND the vivo distribution file is found THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                appPreinstalledOnVivoDevice = { true },
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN a device is made by vivo AND the vivo distribution file is not found THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                appPreinstalledOnVivoDevice = { false },
            )

            // Mock Build.MANUFACTURER to simulate a Vivo device
            ShadowBuild.setManufacturer("vivo")

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the device is not vivo AND the channel is not mozilla online THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is digital_tubrine AND the DT app is installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtTelefonicaInstalled = { true },
            )

            providerValue = "digital_turbine"
            val distributionId = subject.getDistributionId()

            assertEquals("dt-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DT_001, distribution)
        }

    @Test
    fun `WHEN the provider is not digital_tubrine AND the DT app is installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtTelefonicaInstalled = { true },
            )

            providerValue = "some_provider"
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is not digital_tubrine AND the DT app is not installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtTelefonicaInstalled = { false },
            )

            providerValue = "some_provider"
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is null AND the DT app is installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtTelefonicaInstalled = { true },
            )

            providerValue = null
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is null AND the DT app is not installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtTelefonicaInstalled = { false },
            )

            providerValue = null
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the distribution is not default or mozilla online THEN the distribution is from a deal`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.DEFAULT)
            assertEquals(false, subject.isPartnershipDistribution())

            subject.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            assertEquals(true, subject.isPartnershipDistribution())

            subject.setDistribution(DistributionIdManager.Distribution.DT_001)
            assertEquals(true, subject.isPartnershipDistribution())

            subject.setDistribution(DistributionIdManager.Distribution.DT_002)
            assertEquals(true, subject.isPartnershipDistribution())

            subject.setDistribution(DistributionIdManager.Distribution.DT_003)
            assertEquals(true, subject.isPartnershipDistribution())

            subject.setDistribution(DistributionIdManager.Distribution.AURA_001)
            assertEquals(true, subject.isPartnershipDistribution())

            subject.setDistribution(DistributionIdManager.Distribution.XIAOMI_001)
            assertEquals(true, subject.isPartnershipDistribution())
        }

    @Test
    fun `WHEN the distribution should skip the marketing screen THEN the marketing screen is skipped`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.DEFAULT)
            assertEquals(false, subject.shouldSkipMarketingConsentScreen())

            subject.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            assertEquals(true, subject.shouldSkipMarketingConsentScreen())

            subject.setDistribution(DistributionIdManager.Distribution.DT_001)
            assertEquals(true, subject.shouldSkipMarketingConsentScreen())

            subject.setDistribution(DistributionIdManager.Distribution.DT_002)
            assertEquals(true, subject.shouldSkipMarketingConsentScreen())

            subject.setDistribution(DistributionIdManager.Distribution.DT_003)
            assertEquals(true, subject.shouldSkipMarketingConsentScreen())

            subject.setDistribution(DistributionIdManager.Distribution.AURA_001)
            assertEquals(true, subject.shouldSkipMarketingConsentScreen())

            subject.setDistribution(DistributionIdManager.Distribution.XIAOMI_001)
            assertEquals(true, subject.shouldSkipMarketingConsentScreen())
        }

    @Test
    fun `WHEN checking the distribution startup strategy THEN the correct strategy is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.DEFAULT)
            assertEquals(
                DistributionAdjustStartupStrategy.NONE,
                subject.getDistributionAdjustStartupStrategy(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            assertEquals(
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA,
                subject.getDistributionAdjustStartupStrategy(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.DT_001)
            assertEquals(
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA,
                subject.getDistributionAdjustStartupStrategy(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.DT_002)
            assertEquals(
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA,
                subject.getDistributionAdjustStartupStrategy(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.DT_003)
            assertEquals(
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA,
                subject.getDistributionAdjustStartupStrategy(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.AURA_001)
            assertEquals(
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_PLAY_STORE_KIDS,
                subject.getDistributionAdjustStartupStrategy(),
            )

            subject.setDistribution(DistributionIdManager.Distribution.XIAOMI_001)
            assertEquals(
                DistributionAdjustStartupStrategy.IMMEDIATE_WITH_COPPA,
                subject.getDistributionAdjustStartupStrategy(),
            )
        }

    @Test
    fun `WHEN the provider is aura THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            providerValue = "aura"
            val distributionId = subject.getDistributionId()

            assertEquals("aura-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.AURA_001, distribution)
        }

    @Test
    fun `WHEN the provider is DT AND a DT USA package is installed THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtUsaInstalled = { true },
            )

            providerValue = "digital_turbine"
            val distributionId = subject.getDistributionId()

            assertEquals("dt-002", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DT_002, distribution)
        }

    @Test
    fun `WHEN the provider is not DT AND a DT USA package is installed THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
                isDtUsaInstalled = { true },
            )

            providerValue = "some_provider"
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is DT and telefonica and USA packages are not installed THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            providerValue = "digital_turbine"
            val distributionId = subject.getDistributionId()

            assertEquals("dt-003", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DT_003, distribution)
        }

    @Test
    fun `WHEN the play install referrer response has a vivo india campaign THEN the distribution ID is updated`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            subject.updateDistributionIdFromUtmParams(
                UTMParams(
                    source = "source",
                    medium = "medium",
                    campaign = "adj_tracker%3D1234%26adj_campaign%3Dvivo-india-preinstall",
                    content = "content",
                    term = "term",
                ),
            )

            val distributionId = subject.getDistributionId()

            assertEquals("vivo-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.VIVO_001, distribution)
        }

    @Test
    fun `WHEN the play install referrer response has a xiaomi campaign THEN the distribution ID is updated`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            subject.updateDistributionIdFromUtmParams(
                UTMParams(
                    source = "source",
                    medium = "medium",
                    campaign = "xiaomi-001",
                    content = "content",
                    term = "term",
                ),
            )

            val distributionId = subject.getDistributionId()

            assertEquals("xiaomi-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.XIAOMI_001, distribution)
        }

    @Test
    fun `WHEN the play install referrer response does not have a distribution campaign THEN the distribution ID is not updated`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            subject.updateDistributionIdFromUtmParams(
                UTMParams(
                    source = "source",
                    medium = "medium",
                    campaign = "campaign",
                    content = "content",
                    term = "term",
                ),
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN there is a saved ID THEN the saved ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            testDistributionSettings.saveDistributionId("vivo-001")

            val distributionId = subject.getDistributionId()

            assertEquals("vivo-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.VIVO_001, distribution)
        }

    @Test
    fun `WHEN there is not a saved ID THEN a non blank ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = FakeMetricController(),
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `GIVEN the marketing screen should be skipped WHEN we try to start marketing metrics services THEN the services are started`() =
        runBlocking {
            val metricsController = FakeMetricController()

            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                metricController = metricsController,
            )
            subject.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            subject.startAdjustIfSkippingConsentScreen()

            assertEquals(
                listOf(MetricServiceType.Marketing),
                metricsController.startedServiceTypes,
            )
        }

}
