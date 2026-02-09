/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.intent.processing

import android.app.SearchManager
import android.content.Intent
import android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestScope
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.findNormalOrPrivateTabByUrl
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags.Companion.APP_LINK_LAUNCH_TYPE_COLD
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags.Companion.APP_LINK_LAUNCH_TYPE_UNKNOWN
import mozilla.components.feature.intent.processing.TabIntentProcessor.Companion.EXTRA_APP_LINK_LAUNCH_TYPE
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.search.ext.createSearchEngine
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import mozilla.components.support.utils.SafeIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class TabIntentProcessorTest {

    private val scope = TestScope()

    private lateinit var middleware: CaptureActionsMiddleware<BrowserState, BrowserAction>

    private lateinit var searchEngine: SearchEngine
    private lateinit var store: BrowserStore
    private lateinit var engine: Engine
    private lateinit var engineSession: EngineSession

    private lateinit var sessionUseCases: SessionUseCases
    private lateinit var tabsUseCases: TabsUseCases
    private lateinit var searchUseCases: SearchUseCases

    @Before
    fun setup() {
        searchEngine = createSearchEngine(
            name = "Test",
            url = "https://localhost/?q={searchTerms}",
            icon = mock(),
        )

        engine = mock()
        engineSession = mock()
        doReturn(engineSession).`when`(engine).createSession(anyBoolean(), anyString())

        middleware = CaptureActionsMiddleware()

        store = BrowserStore(
            BrowserState(
                search = SearchState(regionSearchEngines = listOf(searchEngine)),
            ),
            middleware = EngineMiddleware.create(
                engine = mock(),
                scope = scope,
            ) + listOf(middleware),
        )

        sessionUseCases = SessionUseCases(store)
        tabsUseCases = TabsUseCases(store)
        searchUseCases = SearchUseCases(store, tabsUseCases, sessionUseCases)
    }

    @Test
    fun `open or select tab on ACTION_VIEW intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.dataString).thenReturn("http://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        assertEquals(1, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionView>(store.state.tabs[0].source)

        val tab = store.state.findNormalOrPrivateTabByUrl("http://mozilla.org", private = false)
        assertNotNull(tab)

        val otherTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(otherTab, select = true))
        assertEquals(2, store.state.tabs.size)
        assertEquals(otherTab, store.state.selectedTab)
        assertIs<SessionState.Source.Internal.None>(store.state.tabs[1].source)

        // processing the same intent again doesn't add an additional tab
        handler.process(intent)
        // processing a similar intent which produces the same url doesn't add an additional tab
        whenever(intent.dataString).thenReturn("mozilla.org")
        handler.process(intent)

        assertEquals(2, store.state.tabs.size)
        assertEquals(tab, store.state.selectedTab)
        // sources of existing tabs weren't affected
        assertIs<SessionState.Source.External.ActionView>(store.state.tabs[0].source)
        assertIs<SessionState.Source.Internal.None>(store.state.tabs[1].source)

        // Intent with a url that's missing a scheme
        whenever(intent.dataString).thenReturn("example.com")
        handler.process(intent)
        assertEquals(3, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionView>(store.state.tabs[0].source)
        assertNotNull(store.state.findNormalOrPrivateTabByUrl("http://example.com", private = false))
    }

    @Test
    fun `open or select tab on ACTION_MAIN intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_MAIN)
        whenever(intent.dataString).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        assertEquals(1, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionView>(store.state.tabs[0].source)

        val tab = store.state.findNormalOrPrivateTabByUrl("https://mozilla.org", false)
        assertNotNull(tab)

        val otherTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(otherTab, select = true))
        assertEquals(2, store.state.tabs.size)
        assertEquals(otherTab, store.state.selectedTab)

        handler.process(intent)
        assertEquals(2, store.state.tabs.size)
        assertEquals(tab, store.state.selectedTab)

        // Intent with a url that's missing a scheme
        whenever(intent.dataString).thenReturn("example.com")
        handler.process(intent)
        assertEquals(3, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionView>(store.state.tabs[0].source)
        assertNotNull(store.state.findNormalOrPrivateTabByUrl("http://example.com", private = false))
    }

    @Test
    fun `open or select tab on ACTION_NDEF_DISCOVERED intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(ACTION_NDEF_DISCOVERED)
        whenever(intent.dataString).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        assertEquals(1, store.state.tabs.size)

        val tab = store.state.findNormalOrPrivateTabByUrl("https://mozilla.org", false)
        assertNotNull(tab)

        val otherTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(otherTab, select = true))
        assertEquals(2, store.state.tabs.size)
        assertEquals(otherTab, store.state.selectedTab)

        handler.process(intent)
        assertEquals(2, store.state.tabs.size)
        assertEquals(tab, store.state.selectedTab)

        // Intent with a url that's missing a scheme
        whenever(intent.dataString).thenReturn("example.com")
        handler.process(intent)
        assertEquals(3, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionView>(store.state.tabs[0].source)
        assertNotNull(store.state.findNormalOrPrivateTabByUrl("http://example.com", private = false))
    }

    @Test
    fun `open tab on ACTION_SEND intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        assertEquals(1, store.state.tabs.size)
        assertEquals("https://mozilla.org", store.state.tabs[0].content.url)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[0].source)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("see https://getpocket.com")
        handler.process(intent)
        assertEquals(2, store.state.tabs.size)
        assertEquals("https://getpocket.com", store.state.tabs[1].content.url)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[1].source)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("see https://firefox.com and https://mozilla.org")
        handler.process(intent)
        assertEquals(3, store.state.tabs.size)
        assertEquals("https://firefox.com", store.state.tabs[2].content.url)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[2].source)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("checkout the Tweet: https://tweets.mozilla.com")
        handler.process(intent)
        assertEquals(4, store.state.tabs.size)
        assertEquals("https://tweets.mozilla.com", store.state.tabs[3].content.url)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[3].source)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("checkout the Tweet: HTTPS://tweets.mozilla.org")
        handler.process(intent)
        assertEquals(5, store.state.tabs.size)
        assertEquals("https://tweets.mozilla.org", store.state.tabs[4].content.url)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[4].source)

        // Intent with a url that's missing a scheme
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("example.com")
        handler.process(intent)
        assertEquals(6, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[5].source)
        assertNotNull(store.state.findNormalOrPrivateTabByUrl("http://example.com", private = false))
    }

    @Test
    fun `open tab and trigger search on ACTION_SEND if text is not a URL`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val searchTerms = "mozilla android"
        val searchUrl = "https://localhost/?q=mozilla%20android"

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(searchTerms)

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)

        assertEquals(1, store.state.tabs.size)
        assertEquals(searchUrl, store.state.tabs[0].content.url)
        assertEquals(searchTerms, store.state.tabs[0].content.searchTerms)
        assertIs<SessionState.Source.External.ActionSend>(store.state.tabs[0].source)
    }

    @Test
    fun `nothing happens on ACTION_SEND if no text is provided`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(" ")

        val processed = handler.process(intent)
        assertFalse(processed)
    }

    @Test
    fun `nothing happens on ACTION_SEARCH if text is empty`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(" ")

        val processed = handler.process(intent)
        assertFalse(processed)
    }

    @Test
    fun `open tab on ACTION_SEARCH intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn("http://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)

        assertEquals(1, store.state.tabs.size)
        assertEquals("http://mozilla.org", store.state.tabs[0].content.url)
        assertEquals("", store.state.tabs[0].content.searchTerms)
        assertIs<SessionState.Source.External.ActionSearch>(store.state.tabs[0].source)

        // Intent with a url that's missing a scheme
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn("example.com")
        handler.process(intent)
        assertEquals(2, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionSearch>(store.state.tabs[1].source)
        assertNotNull(store.state.findNormalOrPrivateTabByUrl("http://example.com", private = false))
    }

    @Test
    fun `open tab and trigger search on ACTION_SEARCH intent if text is not a URL`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val searchTerms = "mozilla android"
        val searchUrl = "https://localhost/?q=mozilla%20android"

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(searchTerms)

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)

        assertEquals(1, store.state.tabs.size)
        assertEquals(searchUrl, store.state.tabs[0].content.url)
        assertEquals(searchTerms, store.state.tabs[0].content.searchTerms)
        assertIs<SessionState.Source.External.ActionSearch>(store.state.tabs[0].source)
    }

    @Test
    fun `nothing happens on ACTION_WEB_SEARCH if text is empty`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_WEB_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(" ")

        val processed = handler.process(intent)
        assertFalse(processed)
    }

    @Test
    fun `open tab on ACTION_WEB_SEARCH intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_WEB_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn("http://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        assertEquals(1, store.state.tabs.size)
        assertEquals("http://mozilla.org", store.state.tabs[0].content.url)
        assertEquals("", store.state.tabs[0].content.searchTerms)
        assertIs<SessionState.Source.External.ActionSearch>(store.state.tabs[0].source)

        // Intent with a url that's missing a scheme
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn("example.com")
        handler.process(intent)
        assertEquals(2, store.state.tabs.size)
        assertIs<SessionState.Source.External.ActionSearch>(store.state.tabs[1].source)
        assertNotNull(store.state.findNormalOrPrivateTabByUrl("http://example.com", private = false))
    }

    @Test
    fun `open tab and trigger search on ACTION_WEB_SEARCH intent if text is not a URL`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val searchTerms = "mozilla android"
        val searchUrl = "https://localhost/?q=mozilla%20android"

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(searchTerms)

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        assertEquals(1, store.state.tabs.size)
        assertEquals(searchUrl, store.state.tabs[0].content.url)
        assertEquals(searchTerms, store.state.tabs[0].content.searchTerms)
        assertIs<SessionState.Source.External.ActionSearch>(store.state.tabs[0].source)
    }

    @Test
    fun `returns external flags when no intent extra for app link launch type extra present`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)
        val intent: Intent = mock()
        val safeIntent = SafeIntent(intent)

        val result = handler.computeLoadUrlFlags(safeIntent)

        assertEquals(EngineSession.LoadUrlFlags.external().value, result.value)
    }

    @Test
    fun `uses app link launch type from intent when the extra is present`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent = Intent().apply {
            putExtra(EXTRA_APP_LINK_LAUNCH_TYPE, APP_LINK_LAUNCH_TYPE_COLD)
        }
        val safeIntent = SafeIntent(intent)

        val result = handler.computeLoadUrlFlags(safeIntent)
        val expected = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.external().value, APP_LINK_LAUNCH_TYPE_COLD)

        assertEquals(expected.value, result.value)
    }

    @Test
    fun `uses the default unknown app link launch type when an invalid extra value is present`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent = Intent().apply {
            putExtra(EXTRA_APP_LINK_LAUNCH_TYPE, "testString")
        }
        val safeIntent = SafeIntent(intent)

        val result = handler.computeLoadUrlFlags(safeIntent)
        val expected = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.external().value, APP_LINK_LAUNCH_TYPE_UNKNOWN)

        assertEquals(expected.value, result.value)
    }

    @Test
    fun `does not use the default when the extra is present but an invalid integer value`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent = Intent().apply {
            putExtra(EXTRA_APP_LINK_LAUNCH_TYPE, -1)
        }
        val safeIntent = SafeIntent(intent)

        val result = handler.computeLoadUrlFlags(safeIntent)
        val expected = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.external().value, APP_LINK_LAUNCH_TYPE_UNKNOWN)

        // Invalid integer value might mean a new app link launch type that's not handled on GeckoView.
        // In that case, this test should fail to alarm for a non-mapped launch type value.
        assertNotEquals(expected.value, result.value)
    }

    @Test
    fun `process intent sets app link intent launch type value`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.dataString).thenReturn("http://mozilla.org")
        whenever(intent.hasExtra(EXTRA_APP_LINK_LAUNCH_TYPE)).thenReturn(true)
        whenever(intent.getIntExtra(EXTRA_APP_LINK_LAUNCH_TYPE, APP_LINK_LAUNCH_TYPE_UNKNOWN)).thenReturn(APP_LINK_LAUNCH_TYPE_COLD)

        handler.process(intent)

        val expected = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.external().value, APP_LINK_LAUNCH_TYPE_COLD)

        assertEquals(expected.value, store.state.tabs[0].engineState.initialLoadFlags.value)
    }

    @Test
    fun `process intent sets does not app link intent launch type value when there's no extra`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.dataString).thenReturn("http://mozilla.org")
        whenever(intent.hasExtra(EXTRA_APP_LINK_LAUNCH_TYPE)).thenReturn(false)

        handler.process(intent)

        val expected = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.external().value)

        assertEquals(expected.value, store.state.tabs[0].engineState.initialLoadFlags.value)
    }

    @Test
    fun `process intent sets default app link intent launch type value when there's no valid value for type`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.dataString).thenReturn("http://mozilla.org")
        whenever(intent.hasExtra(EXTRA_APP_LINK_LAUNCH_TYPE)).thenReturn(true)

        handler.process(intent)

        val expected = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.external().value)

        assertEquals(expected.value, store.state.tabs[0].engineState.initialLoadFlags.value)
    }

    @Test
    fun `getHostForDnsWarmup returns app link host when DoH is disabled`() {
        val settings = DefaultSettings(dohSettingsMode = Engine.DohSettingsMode.DEFAULT)
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertEquals("mozilla.org", result)
    }

    @Test
    fun `getHostForDnsWarmup returns app link host when DoH is OFF`() {
        val settings = DefaultSettings(dohSettingsMode = Engine.DohSettingsMode.OFF)
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertEquals("mozilla.org", result)
    }

    @Test
    fun `getHostForDnsWarmup returns DoH provider host when DoH is INCREASED`() {
        val settings = DefaultSettings(
            dohSettingsMode = Engine.DohSettingsMode.INCREASED,
            dohProviderUrl = "https://cloudflare-dns.com/dns-query",
        )
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertEquals("cloudflare-dns.com", result)
    }

    @Test
    fun `getHostForDnsWarmup returns DoH provider host when DoH is MAX`() {
        val settings = DefaultSettings(
            dohSettingsMode = Engine.DohSettingsMode.MAX,
            dohProviderUrl = "https://dns.nextdns.io/abc123",
        )
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertEquals("dns.nextdns.io", result)
    }

    @Test
    fun `getHostForDnsWarmup returns null when DoH is ULTRA`() {
        val settings = DefaultSettings(
            dohSettingsMode = Engine.DohSettingsMode.ULTRA,
            dohProviderUrl = "https://mozilla.cloudflare-dns.com/dns-query",
        )
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertNull(result)
    }

    @Test
    fun `getHostForDnsWarmup returns null when DoH is enabled but provider URL is empty`() {
        val settings = DefaultSettings(
            dohSettingsMode = Engine.DohSettingsMode.INCREASED,
            dohProviderUrl = "",
        )
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertNull(result)
    }

    @Test
    fun `getHostForDnsWarmup returns app link host when engine is null`() {
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = null)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertEquals("mozilla.org", result)
    }

    @Test
    fun `getHostForDnsWarmup returns null for malformed app link URL`() {
        val settings = DefaultSettings(dohSettingsMode = Engine.DohSettingsMode.OFF)
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("not a valid url")

        assertNull(result)
    }

    @Test
    fun `getHostForDnsWarmup returns null for malformed DoH provider URL`() {
        val settings = DefaultSettings(
            dohSettingsMode = Engine.DohSettingsMode.INCREASED,
            dohProviderUrl = "not a valid url",
        )
        whenever(engine.settings).thenReturn(settings)
        val handler = TabIntentProcessor(TabsUseCases(store), searchUseCases.newTabSearch, engine = engine)

        val result = handler.getHostForDnsWarmup("https://mozilla.org/path")

        assertNull(result)
    }
}
