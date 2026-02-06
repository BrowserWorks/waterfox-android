/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.info

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R

@RunWith(AndroidJUnit4::class)
class SecretSettingsKeysProviderTest {

    @Test
    fun `WHEN parsing the secret settings preference XML THEN the switch preference keys are returned`() {
        val keys = getSecretSettingsPreferenceKeys(testContext.resources)

        assertTrue(keys.contains(testContext.getString(R.string.pref_key_hide_collections)))
        assertTrue(keys.contains(testContext.getString(R.string.pref_key_doh_settings_enabled)))
        assertFalse(keys.contains(testContext.getString(R.string.pref_key_show_debug_info)))
        assertFalse(keys.contains(testContext.getString(R.string.pref_key_custom_glean_server_url)))
    }
}
