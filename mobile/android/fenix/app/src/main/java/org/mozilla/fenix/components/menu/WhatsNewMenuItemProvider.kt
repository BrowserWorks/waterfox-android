/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.whatsnew.WhatsNew

/** Provides release notes while an app update is still highlighted. */
internal class WhatsNewMenuItemProvider(
    private val context: Context,
    private val fenixBrowserUseCases: FenixBrowserUseCases,
) : MenuItemProvider {
    override val itemFlow: StateFlow<MenuItem?> =
        MutableStateFlow(
            if (WhatsNew.shouldHighlightWhatsNew(context)) {
                StandardMenuItem(
                    title = Text.Resource(R.string.browser_menu_whats_new),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_whats_new_24),
                    showNewIndicator = true,
                    onClickEvent = MenuAction.OpenWhatsNew,
                )
            } else {
                null
            }
        )

    override fun handles(event: MenuEvent) = event == MenuAction.OpenWhatsNew

    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        WhatsNew.userViewedWhatsNew(context)
        menu.dismiss()
        fenixBrowserUseCases.loadUrlOrSearch(
            searchTermOrURL = SupportUtils.WHATS_NEW_URL,
            newTab = true,
        )
    }
}
