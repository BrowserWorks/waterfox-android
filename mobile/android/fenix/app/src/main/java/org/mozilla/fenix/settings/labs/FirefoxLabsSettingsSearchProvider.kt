/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.labs

import android.content.Context
import org.mozilla.fenix.settings.settingssearch.SettingsSearchItem
import org.mozilla.fenix.settings.settingssearch.SettingsSearchProvider

/** Disabled [SettingsSearchProvider] for Firefox Labs. */
class FirefoxLabsSettingsSearchProvider : SettingsSearchProvider {

    override fun getSearchItems(context: Context): List<SettingsSearchItem> = emptyList()

    companion object {
        const val FIREFOX_LABS_KEY = "FIREFOX_LABS"
    }
}
