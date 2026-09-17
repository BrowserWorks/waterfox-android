/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bindings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flow
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.HomepageAsANewTabPreferencesRepository
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases

/**
 * A binding for adding a homepage ("about:home") tab whenever normal browsing has no tabs on cold boot and warm
 * relaunch of the application, so that a tab is always available on startup. The exception is while the tab manager is
 * displayed, so that closing all tabs from the tab manager leaves the tab manager empty. The homepage tab is then added
 * once the tab manager is dismissed. Private browsing stays empty until the user opens a tab.
 *
 * @param browserStore The [BrowserStore] to observe the tabs state from.
 * @param appStore The [AppStore] used to determine if the tabs tray is visible.
 * @param browsingModeManager [BrowsingModeManager] used to determine the current browsing mode.
 * @param fenixBrowserUseCases [FenixBrowserUseCases] used to add the homepage tab.
 * @param repository [HomepageAsANewTabPreferencesRepository] used to access the homepage as a new tab preferences.
 * @param mainDispatcher The [CoroutineDispatcher] used to observe the [browserStore].
 */
class HomepageTabBinding(
    private val browserStore: BrowserStore,
    private val appStore: AppStore,
    private val browsingModeManager: BrowsingModeManager,
    private val fenixBrowserUseCases: FenixBrowserUseCases,
    private val repository: HomepageAsANewTabPreferencesRepository,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : AbstractBinding<BrowserState>(browserStore, mainDispatcher) {

    private val isPrivate: Boolean
        get() = browsingModeManager.mode.isPrivate

    override suspend fun onState(flow: Flow<BrowserState>) {
        combine(
                flow.map {
                    repository.getHomepageAsANewTabEnabled() &&
                        it.restoreComplete &&
                        !isPrivate &&
                        it.normalTabs.isEmpty()
                },
                appStore.flow().map { it.isTabsTrayVisible },
            ) { hasNoTabs, isTabsTrayVisible ->
                hasNoTabs && !isTabsTrayVisible
            }
            .distinctUntilChanged()
            .collect { shouldAddNewHomepageTab ->
                if (
                    shouldAddNewHomepageTab &&
                        !isPrivate &&
                        !appStore.state.isTabsTrayVisible &&
                        repository.getHomepageAsANewTabEnabled() &&
                        browserStore.state.restoreComplete &&
                        browserStore.state.normalTabs.isEmpty()
                ) {
                    fenixBrowserUseCases.selectOrAddHomepageTab(private = false)
                }
            }
    }
}
