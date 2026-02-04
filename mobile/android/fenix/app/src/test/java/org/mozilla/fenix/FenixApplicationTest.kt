/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.ai.controls.AIFeatureBlock
import mozilla.components.concept.ai.controls.AIFeatureRegistry
import mozilla.components.concept.engine.webextension.DisabledFlags
import mozilla.components.concept.engine.webextension.Metadata
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.feature.addons.migration.DefaultSupportedAddonsChecker
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.BrowsersCache
import mozilla.components.support.utils.ext.packageManagerWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.GleanMetrics.Preferences
import org.mozilla.fenix.GleanMetrics.TopSites
import org.mozilla.fenix.components.metrics.MozillaProductDetector
import org.mozilla.fenix.distributions.DefaultDistributionBrowserStoreProvider
import org.mozilla.fenix.distributions.DistributionIdManager
import org.mozilla.fenix.distributions.DistributionProviderChecker
import org.mozilla.fenix.distributions.DistributionSettings
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FenixApplicationTest {

    @get:Rule val gleanTestRule = FenixGleanTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var application: FenixApplication
    private lateinit var browsersCache: BrowsersCache
    private lateinit var mozillaProductDetector: MozillaProductDetector
    private lateinit var browserStore: BrowserStore

    private val testDistributionProviderChecker =
        object : DistributionProviderChecker {
            override suspend fun queryProvider(): String? = null
        }

    private val testDistributionSettings =
        object : DistributionSettings {
            override fun getDistributionId(): String = ""

            override fun saveDistributionId(id: String) = Unit
        }

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        browsersCache = mockk(relaxed = true)
        mozillaProductDetector = mockk(relaxed = true)
        browserStore = BrowserStore()

        every { testContext.components.core } returns mockk(relaxed = true)
        every { testContext.components.nimbus } returns mockk(relaxed = true)
        every { testContext.components.aiControlsFeatureBlock } returns AIFeatureBlock.inMemory()
        every { testContext.components.aiFeatureRegistry } returns AIFeatureRegistry.inMemory()
        every { testContext.components.distributionIdManager } returns
            DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                browserStoreProvider = DefaultDistributionBrowserStoreProvider(browserStore),
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
            )
    }

    @Test
    fun `GIVEN there are unsupported addons installed WHEN subscribing for new add-ons checks THEN register for checks`() {
        val checker = mockk<DefaultSupportedAddonsChecker>(relaxed = true)
        val unSupportedExtension: WebExtension = mockk()
        val metadata: Metadata = mockk()

        every { unSupportedExtension.getMetadata() } returns metadata
        every { metadata.disabledFlags } returns DisabledFlags.select(DisabledFlags.APP_SUPPORT)

        application.subscribeForNewAddonsIfNeeded(checker, listOf(unSupportedExtension))

        verify { checker.registerForChecks() }
    }

    @Test
    fun `GIVEN there are no unsupported addons installed WHEN subscribing for new add-ons checks THEN unregister for checks`() {
        val checker = mockk<DefaultSupportedAddonsChecker>(relaxed = true)
        val unSupportedExtension: WebExtension = mockk()
        val metadata: Metadata = mockk()

        every { unSupportedExtension.getMetadata() } returns metadata
        every { metadata.disabledFlags } returns DisabledFlags.select(DisabledFlags.USER)

        application.subscribeForNewAddonsIfNeeded(checker, listOf(unSupportedExtension))

        verify { checker.unregisterForChecks() }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `WHEN startup metrics are requested THEN no telemetry or identifier is created`() = runBlocking {
        val settings = spyk(Settings(testContext))
        settings.contileContextId = "existing-context"
        every { settings.isTelemetryEnabled } returns true

        application.setStartupMetrics(
            browserStore = browserStore,
            settings = settings,
            mozillaProductDetector = mozillaProductDetector,
        )

        assertEquals("existing-context", settings.contileContextId)
        assertNull(TopSites.contextId.testGetValue())
        assertEquals(false, Preferences.telemetryEnabled.testGetValue())
        assertEquals("", Preferences.dohProtectionLevel.testGetValue())
        assertEquals(0L, Metrics.deviceTotalRam.testGetValue())
        verify(exactly = 0) { mozillaProductDetector.getInstalledMozillaProducts(any()) }
    }

    @Test
    @Config(sdk = [28])
    fun `GIVEN the current etp mode is custom WHEN tracking the etp metric THEN no cookies telemetry is recorded on SDK 28`() =
        runBlocking {
            val settings: Settings =
                mockk(relaxed = true) {
                    every { shouldUseTrackingProtection } returns true
                    every { useCustomTrackingProtection } returns true
                    every { blockCookiesSelectionInCustomTrackingProtection } returns "Test"
                }

            application.setStartupMetrics(
                browserStore = browserStore,
                settings = settings,
                mozillaProductDetector = mozillaProductDetector,
            )

            assertEquals("", Preferences.etpCustomCookiesSelection.testGetValue())
        }

    @Test
    fun `GIVEN the current etp mode is custom WHEN tracking the etp metric THEN no cookies telemetry is recorded`() =
        runBlocking {
            val settings: Settings =
                mockk(relaxed = true) {
                    every { shouldUseTrackingProtection } returns true
                    every { useCustomTrackingProtection } returns true
                    every { blockCookiesSelectionInCustomTrackingProtection } returns "Test"
                }

            val packageManager: PackageManager = testContext.packageManager
            shadowOf(packageManager)
                .setInstallSourceInfo(testContext.packageName, "initiating.package", "installing.package")

            application.setStartupMetrics(
                browserStore = browserStore,
                settings = settings,
                mozillaProductDetector = mozillaProductDetector,
            )

            assertEquals("", Preferences.etpCustomCookiesSelection.testGetValue())
        }

    @Test
    fun `WHEN the search configuration is updated in remote settings THEN set a new search configuration being available`() {
        val browserStore = BrowserStore(BrowserState(search = SearchState(isNewSearchConfigurationAvailable = false)))

        application.setupRefreshingSearchEngines(listOf("search-config-v2"), browserStore)

        assertTrue(browserStore.state.search.isNewSearchConfigurationAvailable)
    }

    @Test
    fun `WHEN the search configuration overrides are updated in remote settings THEN set a new search configuration being available`() {
        val browserStore = BrowserStore(BrowserState(search = SearchState(isNewSearchConfigurationAvailable = false)))

        application.setupRefreshingSearchEngines(listOf("search-config-overrides-v2"), browserStore)

        assertTrue(browserStore.state.search.isNewSearchConfigurationAvailable)
    }

    @Test
    fun `WHEN the search engine icons are updated in remote settings THEN set a new search configuration being available`() {
        val browserStore = BrowserStore(BrowserState(search = SearchState(isNewSearchConfigurationAvailable = false)))

        application.setupRefreshingSearchEngines(listOf("search-config-icons"), browserStore)

        assertTrue(browserStore.state.search.isNewSearchConfigurationAvailable)
    }
}
