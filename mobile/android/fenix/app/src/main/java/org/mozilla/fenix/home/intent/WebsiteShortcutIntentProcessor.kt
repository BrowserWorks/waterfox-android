/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.core.net.toUri
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.customtabs.SHORTCUT_CATEGORY
import mozilla.components.feature.intent.ext.putSessionId
import mozilla.components.feature.intent.processing.IntentProcessor
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.utils.toSafeIntent
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.home.intent.FennecBookmarkShortcutsIntentProcessor.Companion.ACTION_FENNEC_HOMESCREEN_SHORTCUT

/** Opens pinned website shortcuts, optionally selecting a tab with a matching URL. */
class WebsiteShortcutIntentProcessor(
    private val store: BrowserStore,
    private val tabsUseCases: TabsUseCases,
    private val reuseExistingTab: () -> Boolean,
) : IntentProcessor {
    override fun process(intent: Intent): Boolean {
        if (!matches(intent)) return false
        val safeIntent = intent.toSafeIntent()
        val url = safeIntent.dataString?.takeUnless { it.isBlank() }?.toNormalizedUrl() ?: return false
        val private = safeIntent.getBooleanExtra(HomeActivity.PRIVATE_BROWSING_MODE, false)
        val existingTab =
            if (reuseExistingTab()) {
                store.state.tabs
                    .filter { it.content.private == private && matchesShortcut(url, it) }
                    .maxByOrNull { it.lastAccess }
            } else {
                null
            }
        val tabId =
            if (existingTab != null) {
                tabsUseCases.selectTab(existingTab.id)
                existingTab.id
            } else {
                tabsUseCases.addTab(
                    url = url,
                    private = private,
                    source = SessionState.Source.Internal.HomeScreen,
                    flags = EngineSession.LoadUrlFlags.external(),
                    originalInput = url,
                )
            }
        intent.action = Intent.ACTION_VIEW
        intent.putSessionId(tabId)
        return true
    }

    private fun matchesShortcut(url: String, tab: TabSessionState): Boolean {
        return matchesUrl(url, tab.content.url) ||
            (tab.source == SessionState.Source.Internal.HomeScreen &&
                tab.content.loadRequest?.triggeredByRedirect == true &&
                tab.content.history.currentIndex == 0 &&
                tab.originalInput?.let { matchesUrl(url, it) } == true)
    }

    private fun matchesUrl(shortcutUrl: String, tabUrl: String): Boolean {
        val shortcut = shortcutUrl.toUri()
        val tab = tabUrl.toNormalizedUrl().toUri()
        if (shortcut.scheme !in listOf("http", "https") || tab.scheme !in listOf("http", "https")) {
            return shortcutUrl == tabUrl
        }
        return shortcut.scheme.equals(tab.scheme, ignoreCase = true) &&
            shortcut.host.equals(tab.host, ignoreCase = true) &&
            shortcut.port == tab.port &&
            shortcut.encodedUserInfo == tab.encodedUserInfo &&
            shortcut.encodedPath.orEmpty().ifEmpty { "/" } == tab.encodedPath.orEmpty().ifEmpty { "/" } &&
            shortcut.encodedQuery == tab.encodedQuery &&
            shortcut.encodedFragment == tab.encodedFragment
    }

    companion object {
        /** Whether the intent came from a pinned website shortcut. */
        fun matches(intent: Intent): Boolean =
            intent.action == ACTION_FENNEC_HOMESCREEN_SHORTCUT ||
                (intent.action == Intent.ACTION_VIEW && intent.hasCategory(SHORTCUT_CATEGORY))
    }
}
