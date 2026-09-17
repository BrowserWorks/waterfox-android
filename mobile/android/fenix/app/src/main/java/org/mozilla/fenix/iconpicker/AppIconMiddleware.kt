/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.iconpicker

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store

/**
 * A middleware for handling side-effects in response to [AppIconAction]s.
 *
 * @param updateAppIcon An interface that updates the main activity alias with the newly selected one.
 * @param updateSearchWidgets An interface that updates the Firefox search widgets with the app icon.
 */
class AppIconMiddleware(
    private val updateAppIcon: AppIconUpdater,
    private val updateSearchWidgets: SearchWidgetsUpdater,
) : Middleware<AppIconState, AppIconAction> {

    override fun invoke(
        store: Store<AppIconState, AppIconAction>,
        next: (AppIconAction) -> Unit,
        action: AppIconAction,
    ) {
        val targetIcon =
            when (action) {
                is UserAction.Selected -> action.appIcon
                is UserAction.Confirmed -> action.newIcon
                is SystemAction.Applied -> action.newIcon
                else -> null
            }
        if (targetIcon != null && !targetIcon.isSelectable) {
            store.dispatch(SystemAction.DialogDismissed)
            return
        }

        next(action)

        when (action) {
            is UserAction.Confirmed -> {
                if (updateAppIcon(old = action.newIcon, new = action.oldIcon)) {
                    store.dispatch(SystemAction.Applied(action.newIcon))
                } else {
                    store.dispatch(
                        SystemAction.UpdateFailed(
                            oldIcon = action.oldIcon,
                            newIcon = action.newIcon,
                        )
                    )
                }
            }
            is SystemAction.Applied -> updateSearchWidgets()

            is UserAction.Dismissed,
            is UserAction.Selected,
            is SystemAction.DialogDismissed,
            is SystemAction.SnackbarDismissed,
            is SystemAction.SnackbarShown,
            is SystemAction.UpdateFailed -> {
                // no-op
            }
        }
    }
}

/** An interface for applying a new app icon. */
fun interface AppIconUpdater : (AppIcon, AppIcon) -> Boolean {
    override fun invoke(old: AppIcon, new: AppIcon): Boolean
}

/**
 * An interface for updating Firefox search widgets.
 *
 * The widgets display the app logo, that needs to be updated once the app icon changes.
 */
fun interface SearchWidgetsUpdater : () -> Unit {
    override fun invoke()
}
