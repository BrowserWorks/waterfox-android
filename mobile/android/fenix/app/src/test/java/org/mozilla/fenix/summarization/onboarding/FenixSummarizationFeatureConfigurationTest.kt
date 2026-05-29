/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.summarization.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.summarization.SummarizationSettingsBinding
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class FenixSummarizationFeatureConfigurationTest {
    private lateinit var testSettings: Settings
    private lateinit var discoverySettings: FenixSummarizationFeatureConfiguration
    private lateinit var settingsBinding: FakeSummarizationSettingsBinding

    private class FakeSummarizationSettingsBinding() : SummarizationSettingsBinding {
        val _isFeatureEnabled = MutableStateFlow(false)
        val _isGestureEnabled = MutableStateFlow(false)

        override val isFeatureEnabled: StateFlow<Boolean> = _isFeatureEnabled
        override val isGestureEnabled: StateFlow<Boolean> = _isGestureEnabled
    }

    @Before
    fun setUp() {
        testSettings = Settings(
            appContext = testContext,
            packageName = "package-name",
            isBenchmarkBuild = false,
        )
        settingsBinding = FakeSummarizationSettingsBinding()
        discoverySettings = FenixSummarizationFeatureConfiguration(
            settings = testSettings,
            summarizationSettingsBinding = settingsBinding,
        )
    }

    @Test
    fun `menu item is not visible even if the feature flag is enabled and the feature itself is turned on`() {
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        assertFalse(
            "Menu item should not be visible when summarization is disabled",
            discoverySettings.showMenuItem,
        )
    }

    @Test
    fun `menu item is not visible if the feature flag is enabled and the feature itself is not enabled`() {
        // GIVEN the feature flag is enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = true

        // AND the feature is not enabled
        settingsBinding._isFeatureEnabled.value = false

        assertFalse(
            "Menu item should not be visible if the feature flag is enabled and the feature itself is not enabled",
            discoverySettings.showMenuItem,
        )
    }

    @Test
    fun `menu item is not visible if the feature flag is not enabled`() {
        // GIVEN the feature flag is not enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = false

        // AND the feature is somehow left enabled
        settingsBinding._isFeatureEnabled.value = true

        assertFalse(
            "Menu item should not be visible if the feature flag is not enabled",
            discoverySettings.showMenuItem,
        )
    }

    @Test
    fun `menu item is not highlighted after menu item is exposed once`() {
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // menu item is exposed once
        discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuItemExposure)

        assertFalse(
            "Menu item should not be highlighted when summarization is disabled",
            discoverySettings.shouldHighlightMenuItem,
        )
    }

    @Test
    fun `menu item is not highlighted after menu item is exposed the second time`() {
        // Given both the feature flag and feature are enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // WHEN menu item is exposed twice
        discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuItemExposure)
        discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuItemExposure)

        assertFalse(
            "Menu item should not be highlighted when summarization is disabled",
            discoverySettings.shouldHighlightMenuItem,
        )
    }

    @Test
    fun `menu item is not highlighted if the feature is not enabled`() {
        // GIVEN the feature flag is enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = true

        // AND the feature is not enabled
        settingsBinding._isFeatureEnabled.value = false

        assertFalse(
            "Menu item should not be highlighted if the feature is not enabled",
            discoverySettings.shouldHighlightMenuItem,
        )
    }

    @Test
    fun `menu item is not highlighted if the feature flag is not enabled`() {
        // GIVEN the feature flag is NOT enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = false

        // GIVEN the feature is somehow enabled
        settingsBinding._isFeatureEnabled.value = true

        assertFalse(
            "Menu item should not be highlighted if the feature flag is not enabled",
            discoverySettings.shouldHighlightMenuItem,
        )
    }

    @Test
    fun `menu overflow is not highlighted if the feature flag is not enabled`() {
        // GIVEN the feature flag is NOT enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = false

        // GIVEN the feature is somehow enabled
        settingsBinding._isFeatureEnabled.value = true

        assertFalse(
            "Menu overflow should not be highlighted if the feature flag is not enabled",
            discoverySettings.shouldHighlightOverflowMenuItem,
        )
    }

    @Test
    fun `menu overflow is not highlighted if the feature is not enabled`() {
        // GIVEN the feature flag is enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = true

        // GIVEN the feature is NOT enabled
        settingsBinding._isFeatureEnabled.value = false

        assertFalse(
            "Menu overflow should not be highlighted if the feature is not enabled",
            discoverySettings.shouldHighlightOverflowMenuItem,
        )
    }

    @Test
    fun `menu overflow is not highlighted if the overflow menu item is not interacted with at all`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        assertFalse(
            "Menu overflow should not be highlighted when summarization is disabled",
            discoverySettings.shouldHighlightOverflowMenuItem,
        )
    }

    @Test
    fun `menu overflow is not highlighted after the overflow menu item is interacted with once`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // WHEN the overflow menu item is interacted with
        discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuOverflowInteraction)

        assertFalse(
            "Menu overflow should not be highlighted after the overflow menu item has been interacted with",
            discoverySettings.shouldHighlightOverflowMenuItem,
        )
    }

    @Test
    fun `menu overflow interaction does not get recorded if it is already more than one time`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // WHEN the overflow menu item is interacted with multiple times
        repeat(10) {
            discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuOverflowInteraction)
        }

        // THEN the settings should not be updated
        assertEquals(
            "Menu overflow interaction should not get recorded when summarization is disabled",
            0,
            testSettings.shakeToSummarizeMoreMenuItemInteractionCount.value,
        )
    }

    @Test
    fun `menu item exposure does not get recorded if it is already more than two times`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // WHEN the overflow menu item is interacted with multiple times
        repeat(10) {
            discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuItemExposure)
        }

        // THEN the settings should not be updated
        assertEquals(
            "Menu item exposure should not get recorded when summarization is disabled",
            0,
            testSettings.shakeToSummarizeMenuItemExposureCount.value,
        )
    }

    @Test
    fun `toolbar overflow menu interaction does not get recorded if it is already more than once`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // WHEN the toolbar overflow is interacted with multiple times
        repeat(10) {
            discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.ToolbarOverflowInteraction)
        }

        // THEN the settings should not be updated
        assertEquals(
            "Menu item exposure should not get recorded when summarization is disabled",
            0,
            testSettings.shakeToSummarizeToolbarInteractionCount.value,
        )
    }

    @Test
    fun `cfr is not marked as shown if it was not previously seen before`() {
        // GIVEN the feature flag is enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // when the cfr is shown
        discoverySettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.CfrExposure)

        // THEN the settings should not be updated
        assertFalse(
            "CFR item should not get recorded when summarization is disabled",
            testSettings.shakeToSummarizeToolbarCfrShown,
        )
    }

    @Test
    fun `toolbar cfr is not shown if the feature flag is not enabled`() {
        // GIVEN the feature flag is NOT enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = false

        // GIVEN the feature is enabled
        settingsBinding._isFeatureEnabled.value = true

        assertFalse(
            "Toolbar CFR should not be shown if the feature flag is not enabled",
            discoverySettings.shouldToolbarShowCfr,
        )
    }

    @Test
    fun `toolbar cfr is not shown if the user settings for the feature itself is not enabled`() {
        // GIVEN the feature flag is enabled
        testSettings.shakeToSummarizeFeatureFlagEnabled = true

        // AND the feature is NOT enabled
        settingsBinding._isFeatureEnabled.value = false

        assertFalse(
            "Toolbar CFR should not be shown if the feature itself is not enabled",
            discoverySettings.shouldToolbarShowCfr,
        )
    }

    @Test
    fun `toolbar cfr is not shown if we have not shown it before`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // AND we have not shown the flag before
        testSettings.shakeToSummarizeToolbarCfrShown = false

        assertFalse(
            "Toolbar CFR should not be shown when summarization is disabled",
            discoverySettings.shouldToolbarShowCfr,
        )
    }

    @Test
    fun `toolbar cfr is not shown if we have shown it before even if the feature is fully available`() {
        // GIVEN the feature flag fully available
        testSettings.shakeToSummarizeFeatureFlagEnabled = true
        settingsBinding._isFeatureEnabled.value = true

        // AND we have shown the flag before
        testSettings.shakeToSummarizeToolbarCfrShown = true

        assertFalse(
            "Toolbar CFR should not be shown if it has already been shown before",
            discoverySettings.shouldToolbarShowCfr,
        )
    }
}
