/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import android.content.res.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.top.sites.DefaultTopSitesStorage
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.Config
import org.mozilla.fenix.R
import org.mozilla.fenix.home.topsites.utils.fetchDefaultTopSites
import org.mozilla.fenix.utils.Settings

/**
 * A binding for observing [RegionState] and adding the default pinned sites that are included in the application on the
 * first run of the application.
 *
 * @param browserStore The [BrowserStore] to observe state changes.
 * @param topSitesStorage An instance of the [DefaultTopSitesStorage] used to add the default pinned sites.
 * @param settings [Settings] used for accessing the application preferences.
 * @param resources [Resources] used for accessing application resources.
 * @param crashReporter [CrashReporter] used for recording caught exceptions.
 * @param isReleased Whether or not the build is in a release channel.
 * @param mainDispatcher The dispatcher on which to observe state changes.
 * @param ioDispatcher The dispatcher used for I/O operations, specifically reading and parsing the default pinned
 *   shortcuts JSON.
 */
class DefaultPinnedSitesBinding(
    browserStore: BrowserStore,
    private val topSitesStorage: DefaultTopSitesStorage,
    private val settings: Settings,
    private val resources: Resources,
    private val crashReporter: CrashReporter,
    private val isReleased: Boolean = Config.channel.isReleased,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AbstractBinding<BrowserState>(browserStore, mainDispatcher = mainDispatcher) {

    override suspend fun onState(flow: Flow<BrowserState>) {
        if (settings.defaultTopSitesAdded) return

        flow
            .mapNotNull { it.search.region }
            .distinctUntilChanged()
            .collect { regionState ->
                if (isReleased && regionState == RegionState.Default) {
                    return@collect
                }

                val defaultPinnedSites = getPinnedSites(region = regionState.current)

                if (defaultPinnedSites.isNotEmpty()) {
                    topSitesStorage.addTopSites(
                        topSites = defaultPinnedSites.map { it.title to it.url },
                        isDefault = true,
                    )
                    settings.defaultTopSitesAdded = true
                }
            }
    }

    internal suspend fun getPinnedSites(region: String): List<DefaultTopSite> =
        withContext(ioDispatcher) {
            fetchDefaultTopSites(
                    resources = resources,
                    rawResId = R.raw.initial_shortcuts_empty,
                    crashReporter = crashReporter,
                    region = region,
                )
                .map { it.toDefaultTopSite() }
        }
}
