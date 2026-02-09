/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse.experimentation.utils

import io.mockk.every
import io.mockk.mockk
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.Engine.HttpsOnlyMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.fenix.utils.Settings

class DefaultTermsOfUseDataProviderTest {
    @Test
    fun `useStrictTrackingProtection returns the same as the referenced Settings value`() {
        val settings = mockk<Settings>()

        every { settings.useStrictTrackingProtection } returns true
        val defaultTermsOfUseDataProvider1 = DefaultTermsOfUseDataProvider(settings)
        assertTrue(defaultTermsOfUseDataProvider1.useStrictTrackingProtection)

        every { settings.useStrictTrackingProtection } returns false
        val defaultTermsOfUseDataProvider2 = DefaultTermsOfUseDataProvider(settings)
        assertFalse(defaultTermsOfUseDataProvider2.useStrictTrackingProtection)
    }

    @Test
    fun `shouldEnableGlobalPrivacyControl returns the same as the referenced Settings value`() {
        val settings = mockk<Settings>()

        every { settings.shouldEnableGlobalPrivacyControl } returns true
        val defaultTermsOfUseDataProvider1 = DefaultTermsOfUseDataProvider(settings)
        assertTrue(defaultTermsOfUseDataProvider1.shouldEnableGlobalPrivacyControl)

        every { settings.shouldEnableGlobalPrivacyControl } returns false
        val defaultTermsOfUseDataProvider2 = DefaultTermsOfUseDataProvider(settings)
        assertFalse(defaultTermsOfUseDataProvider2.shouldEnableGlobalPrivacyControl)
    }

    @Test
    fun `isIncreasedDohProtectionEnabled returns true for increased max or ultra`() {
        Engine.DohSettingsMode.entries.forEach {
            val settings = mockk<Settings>()
            every { settings.getDohSettingsMode() } returns it
            val defaultTermsOfUseDataProvider = DefaultTermsOfUseDataProvider(settings)

            val result = defaultTermsOfUseDataProvider.isIncreasedDohProtectionEnabled()
            when (it) {
                Engine.DohSettingsMode.INCREASED,
                Engine.DohSettingsMode.MAX,
                Engine.DohSettingsMode.ULTRA,
                    -> assertTrue(result)

                Engine.DohSettingsMode.DEFAULT,
                Engine.DohSettingsMode.OFF,
                    -> assertFalse(result)
            }
        }
    }

    @Test
    fun `enabledHttpsOnlyMode returns true if the referenced Settings value is increased or max`() {
        HttpsOnlyMode.entries.forEach {
            val settings = mockk<Settings>()
            every { settings.getHttpsOnlyMode() } returns it
            val defaultTermsOfUseDataProvider = DefaultTermsOfUseDataProvider(settings)

            val result = defaultTermsOfUseDataProvider.enabledHttpsOnlyMode()
            when (it) {
                HttpsOnlyMode.ENABLED_PRIVATE_ONLY,
                HttpsOnlyMode.ENABLED,
                    -> assertTrue(result)

                HttpsOnlyMode.DISABLED -> assertFalse(result)
            }
        }
    }

    @Test
    fun `showSponsoredShortcuts returns the same as the referenced Settings value`() {
        val settings = mockk<Settings>()

        every { settings.showContileFeature } returns true
        val defaultTermsOfUseDataProvider1 = DefaultTermsOfUseDataProvider(settings)
        assertTrue(defaultTermsOfUseDataProvider1.showSponsoredShortcuts)

        every { settings.showContileFeature } returns false
        val defaultTermsOfUseDataProvider2 = DefaultTermsOfUseDataProvider(settings)
        assertFalse(defaultTermsOfUseDataProvider2.showSponsoredShortcuts)
    }

    @Test
    fun `showShortcutsFeature returns the same as the referenced Settings value`() {
        val settings = mockk<Settings>()

        every { settings.showTopSitesFeature } returns true
        val defaultTermsOfUseDataProvider1 = DefaultTermsOfUseDataProvider(settings)
        assertTrue(defaultTermsOfUseDataProvider1.showShortcutsFeature)

        every { settings.showTopSitesFeature } returns false
        val defaultTermsOfUseDataProvider2 = DefaultTermsOfUseDataProvider(settings)
        assertFalse(defaultTermsOfUseDataProvider2.showShortcutsFeature)
    }

    @Test
    fun `showSponsoredStories returns the same as the referenced Settings value`() {
        val settings = mockk<Settings>()

        every { settings.showPocketSponsoredStories } returns true
        val defaultTermsOfUseDataProvider1 = DefaultTermsOfUseDataProvider(settings)
        assertTrue(defaultTermsOfUseDataProvider1.showSponsoredStories)

        every { settings.showPocketSponsoredStories } returns false
        val defaultTermsOfUseDataProvider2 = DefaultTermsOfUseDataProvider(settings)
        assertFalse(defaultTermsOfUseDataProvider2.showSponsoredStories)
    }

    @Test
    fun `showRecommendationsFeature returns the same as the referenced Settings value`() {
        val settings = mockk<Settings>()

        every { settings.showPocketRecommendationsFeature } returns true
        val defaultTermsOfUseDataProvider1 = DefaultTermsOfUseDataProvider(settings)
        assertTrue(defaultTermsOfUseDataProvider1.showStoriesFeature)

        every { settings.showPocketRecommendationsFeature } returns false
        val defaultTermsOfUseDataProvider2 = DefaultTermsOfUseDataProvider(settings)
        assertFalse(defaultTermsOfUseDataProvider2.showStoriesFeature)
    }
}
