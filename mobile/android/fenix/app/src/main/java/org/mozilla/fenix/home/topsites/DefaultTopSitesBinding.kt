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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.feature.top.sites.DefaultTopSitesStorage
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.mozilla.fenix.Config
import org.mozilla.fenix.R
import org.mozilla.fenix.utils.Settings

/**
 * A binding for observing [RegionState] and adding default top sites that are included in the
 * application.
 *
 * @param browserStore The [BrowserStore] to observe state changes.
 * @param topSitesStorage An instance of the [DefaultTopSitesStorage] used add to default top sites.
 * @param settings [Settings] used for accessing the application preferences.
 * @param resources [Resources] used for accessing application resources.
 * @param crashReporter [CrashReporter] used for recording caught exceptions.
 * @param isReleased Whether or not the build is in a release channel.
 * @param mainDispatcher The dispatcher on which to observe state changes.
 * @param ioDispatcher The dispatcher used for I/O operations,
 *                     specifically reading and parsing the initial shortcuts JSON.
 */
class DefaultTopSitesBinding(
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

                val defaultTopSites = getTopSites(region = regionState.current)

                if (defaultTopSites.isNotEmpty()) {
                    topSitesStorage.addTopSites(topSites = defaultTopSites, isDefault = true)
                    settings.defaultTopSitesAdded = true
                }
            }
    }

    internal suspend fun getTopSites(region: String): List<Pair<String, String>> = withContext(ioDispatcher) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonString = resources.openRawResource(R.raw.initial_shortcuts_empty).bufferedReader()
                .use { it.readText() }

            json.decodeFromString<DefaultTopSitesList>(jsonString).data.filter { item ->
                val includedInRegions =
                    item.includeRegions.isEmpty() || region in item.includeRegions
                val notExcludedInRegions =
                    item.excludeRegions.isEmpty() || region !in item.excludeRegions

                val includedInExperiments =
                    if (item.includeExperiments.isNotEmpty() &&
                        item.includeExperiments.first() == "firefox-jp-guide-default-site"
                    ) {
                        settings.showFirefoxJpGuideDefaultSite
                    } else {
                        true
                    }

                includedInRegions && notExcludedInRegions && includedInExperiments
            }.map {
                Pair(
                    it.title?.takeIf(String::isNotBlank) ?: it.url.tryGetHostFromUrl(),
                    it.url,
                )
            }
        } catch (e: SerializationException) {
            crashReporter.recordCrashBreadcrumb(
                Breadcrumb(
                    message = "DefaultShortcutsProvider - Failed to parse initial_shortcuts.json",
                ),
            )
            crashReporter.submitCaughtException(e)
            listOf()
        } catch (e: IllegalArgumentException) {
            crashReporter.recordCrashBreadcrumb(
                Breadcrumb(
                    message = "DefaultShortcutsProvider - Failed to parse initial_shortcuts.json",
                ),
            )
            crashReporter.submitCaughtException(e)
            listOf()
        }
    }
}

@Serializable
private data class DefaultTopSiteItem(
    val url: String,
    val title: String? = null,
    val order: Int,
    val schema: Long,
    @SerialName("exclude_locales")
    val excludeLocales: List<String> = emptyList(),
    @SerialName("exclude_regions")
    val excludeRegions: List<String> = emptyList(),
    @SerialName("include_locales")
    val includeLocales: List<String> = emptyList(),
    @SerialName("include_regions")
    val includeRegions: List<String> = emptyList(),
    @SerialName("exclude_experiments")
    val excludeExperiments: List<String> = emptyList(),
    @SerialName("include_experiments")
    val includeExperiments: List<String> = emptyList(),
    val id: String,
    @SerialName("last_modified")
    val lastModified: Long,
    @SerialName("search_shortcut")
    val searchShortcut: Boolean = false,
)

@Serializable
private data class DefaultTopSitesList(
    val data: List<DefaultTopSiteItem>,
)
