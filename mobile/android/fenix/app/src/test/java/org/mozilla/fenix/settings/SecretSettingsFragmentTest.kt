/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import io.mockk.every
import io.mockk.mockk
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.ReleaseChannel
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecretSettingsFragmentTest {
    private val fragment = SecretSettingsFragment()

    @Before
    fun setup() {
        every { testContext.components.settings } returns mockk<Settings>(relaxed = true)

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        activity.supportFragmentManager.beginTransaction().add(fragment, "test").commitNow()
    }

    @Test
    fun `WHEN advanced settings are created THEN unwanted controls and categories are absent`() {
        listOf(
                R.string.pref_key_google_lens_integration,
                R.string.pref_key_enable_fxsuggest,
                R.string.pref_key_use_remote_search_configuration,
                R.string.pref_key_search_optimization,
                R.string.pref_key_enable_ads_client_for_stories,
                R.string.pref_key_private_mode_and_stories_entry_point,
                R.string.pref_key_show_debug_info,
            )
            .forEach { keyResourceId ->
                val key = fragment.getPreferenceKey(keyResourceId)
                assertNull(key, fragment.findPreference<Preference>(key))
            }

        val screen = fragment.preferenceScreen
        val titles = (0 until screen.preferenceCount).map { screen.getPreference(it).title?.toString() }.toSet()
        val expectedTitles =
            setOf(
                    R.string.secret_settings_category_toolbar_title,
                    R.string.secret_settings_category_homepage_title,
                    R.string.secret_settings_category_tabs_title,
                    R.string.secret_settings_category_network_title,
                    R.string.secret_settings_category_performance_title,
                )
                .map { fragment.getString(it) }
                .toSet()

        assertEquals(expectedTitles, titles)
    }

    @Test
    fun `GIVEN a release build WHEN advanced settings are created THEN empty categories are hidden`() {
        fragment.setupPreferences(ReleaseChannel.Release)

        val screen = fragment.preferenceScreen
        val visibleTitles =
            (0 until screen.preferenceCount)
                .map { screen.getPreference(it) }
                .filter { it.isVisible }
                .map { it.title?.toString() }
                .toSet()
        val expectedTitles =
            setOf(
                    R.string.secret_settings_category_homepage_title,
                    R.string.secret_settings_category_tabs_title,
                    R.string.secret_settings_category_network_title,
                )
                .map { fragment.getString(it) }
                .toSet()

        assertEquals(expectedTitles, visibleTitles)
    }
}
