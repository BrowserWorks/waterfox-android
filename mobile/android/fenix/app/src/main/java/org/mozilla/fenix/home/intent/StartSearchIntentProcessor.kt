/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.SearchWidget
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.metrics.MetricsUtils
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.utils.Settings

/**
 * When the search widget is tapped and the user has been onboarded, Fenix should open directly to search. Tapping the
 * private browsing mode launcher icon should also open to search.
 */
class StartSearchIntentProcessor(
    private val fenixBrowserUseCases: FenixBrowserUseCases,
    private val browsingModeManager: BrowsingModeManager,
    private val userHasBeenOnboarded: () -> Boolean,
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent, settings: Settings): Boolean {
        if (!userHasBeenOnboarded()) {
            return false
        }

        val event = intent.extras?.getString(HomeActivity.OPEN_TO_SEARCH)
        return if (event != null) {
            val source =
                when (event) {
                    SEARCH_WIDGET -> {
                        SearchWidget.newTabButton.record(NoExtras())
                        MetricsUtils.Source.WIDGET
                    }
                    STATIC_SHORTCUT_NEW_TAB,
                    STATIC_SHORTCUT_NEW_PRIVATE_TAB,
                    PRIVATE_BROWSING_PINNED_SHORTCUT -> {
                        MetricsUtils.Source.SHORTCUT
                    }
                    else -> null
                }

            out.removeExtra(HomeActivity.OPEN_TO_SEARCH)

            source?.let {
                if (event == SEARCH_WIDGET) {
                    browsingModeManager.updateMode(intent)
                    if (settings.enableHomepageAsNewTab) {
                        fenixBrowserUseCases.addNewHomepageTab(private = browsingModeManager.mode.isPrivate)
                    }
                }

                navController.nav(
                    id = null,
                    directions =
                        NavGraphDirections.actionGlobalHome(
                            focusOnAddressBar = true,
                            searchAccessPoint = it,
                        ),
                )
            }

            true
        } else {
            false
        }
    }

    companion object {
        const val SEARCH_WIDGET = "search_widget"
        const val STATIC_SHORTCUT_NEW_TAB = "static_shortcut_new_tab"
        const val STATIC_SHORTCUT_NEW_PRIVATE_TAB = "static_shortcut_new_private_tab"
        const val PRIVATE_BROWSING_PINNED_SHORTCUT = "private_browsing_pinned_shortcut"
    }
}
