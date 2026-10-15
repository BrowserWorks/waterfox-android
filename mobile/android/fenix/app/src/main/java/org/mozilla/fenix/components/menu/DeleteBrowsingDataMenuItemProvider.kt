/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.store.MenuAction

/** Provides an optional shortcut to the delete browsing data settings. */
internal class DeleteBrowsingDataMenuItemProvider(showInMenu: Boolean) : MenuItemProvider {
    override val itemFlow: StateFlow<MenuItem?> =
        MutableStateFlow(
            if (showInMenu) {
                StandardMenuItem(
                    title = Text.Resource(R.string.preferences_delete_browsing_data),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_delete_24),
                    onClickEvent = MenuAction.Navigate.DeleteBrowsingData,
                )
            } else {
                null
            }
        )

    override fun handles(event: MenuEvent) = event == MenuAction.Navigate.DeleteBrowsingData

    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        menu.navigate(NavGraphDirections.actionGlobalDeleteBrowsingDataFragment())
    }
}
