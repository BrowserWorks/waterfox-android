/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ext.packageManagerWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.distributions.DistributionBrowserStoreProvider
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.distributions.DistributionProviderChecker
import org.mozilla.fenix.distributions.DistributionSettings
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.MarketingOnboardingCard
import org.mozilla.fenix.utils.Settings

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class InstallReferrerHandlingServiceTest {

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
    }

    private val distributionIdManager = DistributionIdManager(
        packageManager = testContext.packageManagerWrapper,
        browserStoreProvider = testBrowserStoreProvider,
        distributionProviderChecker = testDistributionProviderChecker,
        distributionSettings = testDistributionSettings,
        appPreinstalledOnVivoDevice = { true },
    )

    @Before
    fun setUp() {
        every { testContext.components.settings } returns Settings(testContext)
        InstallReferrerHandlingService.response = null
        testContext.components.settings.shouldShowMarketingOnboarding = true
        FxNimbus.features.marketingOnboardingCard.withCachedValue(MarketingOnboardingCard(enabled = true))
    }

    @Test
    fun `GIVEN a null referrer on OK response WHEN start is called THEN response is not stored and shouldShowMarketingOnboarding is false`() =
        runTest {
            val service = fakeService(
                responseCode = InstallReferrerClient.InstallReferrerResponse.OK,
                referrerResponse = null,
                scope = this,
            )

            service.start()
            advanceUntilIdle()

            assertNull(InstallReferrerHandlingService.response)
            assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
        }

    @Test
    fun `GIVEN a non-null referrer on OK response WHEN start is called THEN response is stored`() {
        val referrer = "utm_source=addons.mozilla.org&utm_medium=referral&utm_content=rta%3Atest"
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.OK, referrerResponse = referrer)

        service.start()

        assertEquals(referrer, InstallReferrerHandlingService.response)
    }

    @Test
    fun `GIVEN FEATURE_NOT_SUPPORTED WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED)

        service.start()

        assertNull(InstallReferrerHandlingService.response)
        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN DEVELOPER_ERROR WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN SERVICE_UNAVAILABLE WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN PERMISSION_ERROR WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(responseCode = InstallReferrerClient.InstallReferrerResponse.PERMISSION_ERROR)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    @Test
    fun `GIVEN a service disconnect WHEN start is called THEN shouldShowMarketingOnboarding is false`() {
        val service = fakeService(simulateDisconnect = true)

        service.start()

        assertFalse(testContext.components.settings.shouldShowMarketingOnboarding)
    }

    private fun fakeService(
        responseCode: Int = InstallReferrerClient.InstallReferrerResponse.OK,
        referrerResponse: String? = null,
        simulateDisconnect: Boolean = false,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    ) = InstallReferrerHandlingService(testContext, scope = scope).apply {
        clientFactory = {
            FakeReferrerClient(
                responseCode = responseCode,
                referrerResponse = referrerResponse,
                simulateDisconnect = simulateDisconnect,
            )
        }
    }

    @Test
    fun `WHEN the marketing onboarding Nimbus flag is disabled THEN we should not show marketing onboarding`() =
        runBlocking {
            FxNimbus.features.marketingOnboardingCard.withCachedValue(MarketingOnboardingCard(enabled = false))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
        }

    @Test
    fun `WHEN the marketing onboarding Nimbus flag is enabled THEN we should show marketing onboarding`() =
        runBlocking {
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is empty or null THEN we should not show marketing onboarding`() =
        runBlocking {
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding(null, distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding(" ", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse has a GCLID prefix THEN we should show marketing onboarding`() =
        runBlocking {
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=CjwKCAjw&utm_source=google&utm_medium=cpc&utm_campaign=Search_Brand&utm_content=ad_variation_1&utm_term=firefox+browser", distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is not in the marketing prefixes THEN we should not show marketing onboarding`() =
        runBlocking {
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding(" gclid=12345", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=google&utm_medium=cpc&utm_campaign=brand&utm_content=gclid%3D12345&utm_term=", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=google-play&utm_medium=organic", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=(not%20set)&utm_medium=(not%20set)", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=eea-browser-choice&utm_medium=preload", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("utm_source=addons.mozilla.org&utm_medium=referral&utm_campaign=amo-fx-cta-869140&utm_content=rta%3AezU4YzMyYWM0LTBkNmMtNGQ2Zi1hZTJjLTk2YWFmOGZmY2I2Nn0&utm_term=", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclida=", distributionIdManager))
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("test", distributionIdManager))
        }

    @Test
    fun `GIVEN a partnership distribution that skips the consent screen WHEN referrer is present THEN we should not show marketing onboarding`() =
        runBlocking {
            distributionIdManager.setDistribution(DistributionIdManager.Distribution.VIVO_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_002)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.DT_003)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.XIAOMI_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))

            distributionIdManager.setDistribution(DistributionIdManager.Distribution.AURA_001)
            assertFalse(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", distributionIdManager))
        }

    @Test
    fun `GIVEN a partnership distribution that should show the consent screen THEN we should show marketing onboarding`() =
        runBlocking {
            val mockedDistributionIdManager = mockk<DistributionIdManager> {
                coEvery { isPartnershipDistribution() } returns true
                coEvery { shouldSkipMarketingConsentScreen() } returns false
            }
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(null, mockedDistributionIdManager))
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding("gclid=12345", mockedDistributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is a Meta attribution THEN we should show marketing onboarding`() =
        runBlocking {
            val metaReferrer = """utm_source=apps.facebook.com&utm_medium=paid&utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}}"""
            assertTrue(InstallReferrerHandlingService.shouldShowMarketingOnboarding(metaReferrer, distributionIdManager))
        }

    @Test
    fun `WHEN installReferrerResponse is null or blank or malformed THEN isMetaAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(null))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(""))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(" "))

        val malformedReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}"""
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(malformedReferrer))
    }

    @Test
    fun `WHEN installReferrerResponse contains Meta utm_content params THEN isMetaAttribution returns true`() {
        val metaReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA","nonce":"NONCE"}}"""
        assertTrue(InstallReferrerHandlingService.isMetaAttribution(metaReferrer))
    }

    @Test
    fun `WHEN installReferrerResponse missing Meta data or nonce THEN isMetaAttribution returns false`() {
        var metaReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"nonce":"NONCE"}}"""
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(metaReferrer))

        metaReferrer = """utm_content={"app":12345,"t":1234567890,"source":{"data":"DATA"}}"""
        assertFalse(InstallReferrerHandlingService.isMetaAttribution(metaReferrer))
    }

    @Test
    fun `WHEN installReferrerResponse does not contain Meta params THEN isMetaAttribution returns false`() {
        assertFalse(InstallReferrerHandlingService.isMetaAttribution("utm_source=google&utm_medium=cpc"))
        assertFalse(InstallReferrerHandlingService.isMetaAttribution("gclid=12345"))
    }


}

private class FakeReferrerClient(
    private val responseCode: Int = InstallReferrerClient.InstallReferrerResponse.OK,
    private val referrerResponse: String? = null,
    private val simulateDisconnect: Boolean = false,
) : InstallReferrerClientWrapper {

    override fun startConnection(listener: InstallReferrerStateListener) {
        if (simulateDisconnect) {
            listener.onInstallReferrerServiceDisconnected()
        } else {
            listener.onInstallReferrerSetupFinished(responseCode)
        }
    }

    override fun getInstallReferrer(): String? = referrerResponse

    override fun endConnection() = Unit
}
