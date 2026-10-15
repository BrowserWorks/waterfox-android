/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.menu.DropdownMenu
import mozilla.components.compose.base.menu.MenuItem
import mozilla.components.compose.base.text.Text
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.home.topsites.TopSitesTestTag
import org.mozilla.fenix.wallpapers.WallpaperTheme

@Composable
internal fun ShortcutsMenu(
    onAddShortcutClicked: (() -> Unit)?,
    onManageShortcutsClicked: (() -> Unit)?,
) {
    if (onAddShortcutClicked == null && onManageShortcutsClicked == null) return

    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            contentDescription = stringResource(R.string.waterfox_shortcuts_menu),
            modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag(TopSitesTestTag.SHORTCUTS_MENU),
        ) {
            Icon(
                painter = painterResource(iconsR.drawable.mozac_ic_ellipsis_vertical_24),
                contentDescription = null,
                tint = WallpaperTheme.onWallpaper,
            )
        }

        DropdownMenu(
            menuItems =
                buildList {
                    onAddShortcutClicked?.let { onClick ->
                        add(
                            MenuItem.TextItem(
                                text = Text.Resource(R.string.homepage_shortcuts_add_shortcut),
                                onClick = onClick,
                            )
                        )
                    }
                    onManageShortcutsClicked?.let { onClick ->
                        add(
                            MenuItem.TextItem(
                                text = Text.Resource(R.string.waterfox_manage_shortcuts),
                                onClick = onClick,
                            )
                        )
                    }
                },
            expanded = expanded,
            onDismissRequest = { expanded = false },
        )
    }
}
