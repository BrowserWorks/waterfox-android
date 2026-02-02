/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites.controller

import android.app.Activity
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.top.sites.DefaultTopSitesProvider
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.TopSitesUseCases
import mozilla.components.service.mars.MozAdsUseCases
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.ShortcutsLibrary
import org.mozilla.fenix.GleanMetrics.TopSites
import org.mozilla.fenix.R
import org.mozilla.fenix.components.Analytics
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppAction.ShortcutAction
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.home.topsites.AddShortcutEntryPoint
import org.mozilla.fenix.home.topsites.AddShortcutSource
import org.mozilla.fenix.home.topsites.ShortcutsFragmentDirections
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class DefaultTopSiteControllerTest {

    @get:Rule val gleanTestRule = FenixGleanTestRule(testContext)

    private val activity: Activity = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val appStore: AppStore = mockk(relaxed = true)
    private val tabsUseCases: TabsUseCases = mockk(relaxed = true)
    private val selectTabUseCase: TabsUseCases = mockk(relaxed = true)
    private val topSitesUseCases: TopSitesUseCases = mockk(relaxed = true)
    private val defaultTopSitesProvider: DefaultTopSitesProvider = mockk(relaxed = true)
    private val fenixBrowserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
    private val mozAdsUseCases: MozAdsUseCases = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)

    private val searchEngine =
        SearchEngine(
            id = "test",
            name = "Test Engine",
            icon = mockk(relaxed = true),
            type = SearchEngine.Type.BUNDLED,
            resultUrls = listOf("https://example.org/?q={searchTerms}"),
        )

    private val googleSearchEngine =
        SearchEngine(
            id = "googleTest",
            name = "Google Test Engine",
            icon = mockk(relaxed = true),
            type = SearchEngine.Type.BUNDLED,
            resultUrls = listOf("https://www.google.com/?q={searchTerms}"),
            suggestUrl = "https://www.google.com/",
        )

    private val duckDuckGoSearchEngine =
        SearchEngine(
            id = "ddgTest",
            name = "DuckDuckGo Test Engine",
            icon = mockk(relaxed = true),
            type = SearchEngine.Type.BUNDLED,
            resultUrls = listOf("https://duckduckgo.com/?q=%7BsearchTerms%7D&t=fpas"),
            suggestUrl = "https://ac.duckduckgo.com/ac/?q=%7BsearchTerms%7D&type=list",
        )

    private lateinit var store: BrowserStore

    @Before
    fun setup() = runTest {
        store = BrowserStore(BrowserState(search = SearchState(regionSearchEngines = listOf(searchEngine))))

        every { navController.currentDestination } returns
            mockk {
                every { id } returns R.id.homeFragment
            }
        every { activity.components.analytics } returns analytics
    }

    @Test
    fun handleSelectDefaultTopSite() = runTest {
        val topSite =
            TopSite.Default(
                id = 1L,
                title = "Mozilla",
                url = "mozilla.org",
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openDefault.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectNonDefaultTopSite() = runTest {
        val topSite =
            TopSite.Frecent(
                id = 1L,
                title = "Mozilla",
                url = "mozilla.org",
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `GIVEN homepage as a new tab is enabled WHEN Default TopSite is selected THEN open top site in existing tab`() =
        runTest {
            val topSite =
                TopSite.Default(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    createdAt = 0,
                )
            val controller = spyk(createController(this))

            every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)
            every { settings.enableHomepageAsNewTab } returns true

            controller.handleSelectTopSite(topSite, position = 0)

            verify {
                navController.navigate(R.id.browserFragment)
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = topSite.url,
                    newTab = false,
                    private = false,
                )
            }
        }

    @Test
    fun `GIVEN current destination is the shortcuts fragment WHEN a top site is selected THEN open top site in a new tab`() =
        runTest {
            every { navController.currentDestination } returns
                mockk {
                    every { id } returns R.id.shortcutsFragment
                }

            val topSite =
                TopSite.Default(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    createdAt = 0,
                )
            val controller = spyk(createController(this, source = TopSitesSource.SHORTCUTS_LIBRARY))

            every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

            controller.handleSelectTopSite(topSite, position = 0)

            verify {
                navController.navigate(ShortcutsFragmentDirections.actionShortcutsFragmentToBrowserFragment())
                tabsUseCases.addTab.invoke(
                    url = topSite.url,
                    selectTab = true,
                    startLoading = true,
                )
            }
        }

    @Test
    fun `GIVEN the shortcuts library source WHEN a top site is selected THEN record telemetry with the shortcuts library source`() =
        runTest {
            val topSite =
                TopSite.Default(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    createdAt = 0,
                )
            val controller = spyk(createController(this, source = TopSitesSource.SHORTCUTS_LIBRARY))

            every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

            controller.handleSelectTopSite(topSite, position = 0)

            assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

            assertTrue(TopSites.openDefault.testGetValue().orEmpty().isEmpty())
        }

    @Test
    fun `GIVEN existing tab for url WHEN Default TopSite selected THEN open new tab`() = runTest {
        val url = "mozilla.org"
        val existingTabForUrl = createTab(url = url)

        store =
            BrowserStore(
                BrowserState(
                    tabs = listOf(existingTabForUrl),
                    search = SearchState(regionSearchEngines = listOf(searchEngine)),
                )
            )

        val topSite =
            TopSite.Default(
                id = 1L,
                title = "Mozilla",
                url = url,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openDefault.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `GIVEN existing tab for url WHEN Provided TopSite selected THEN open new tab`() = runTest {
        val url = "mozilla.org"
        val existingTabForUrl = createTab(url = url)

        store =
            BrowserStore(
                BrowserState(
                    tabs = listOf(existingTabForUrl),
                    search = SearchState(regionSearchEngines = listOf(searchEngine)),
                )
            )

        val topSite =
            TopSite.Provided(
                id = 1L,
                title = "Mozilla",
                url = url,
                clickUrl = "",
                imageUrl = "",
                impressionUrl = "",
                createdAt = 0,
            )
        val position = 0
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openContileTopSite.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { controller.recordTopSitesClickTelemetry(topSite, position) }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `GIVEN existing tab for url WHEN Frecent TopSite selected THEN navigate to tab`() = runTest {
        val url = "mozilla.org"
        val existingTabForUrl = createTab(url = url)

        store =
            BrowserStore(
                BrowserState(
                    tabs = listOf(existingTabForUrl),
                    search = SearchState(regionSearchEngines = listOf(searchEngine)),
                )
            )

        val topSite =
            TopSite.Frecent(
                id = 1L,
                title = "Mozilla",
                url = url,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openFrecency.testGetValue().orEmpty().isEmpty())

        verify {
            selectTabUseCase.invoke(existingTabForUrl.id)
            navController.navigate(R.id.browserFragment)
        }
    }

    @Test
    fun `GIVEN existing tab for url WHEN Pinned TopSite selected THEN navigate to tab`() = runTest {
        val url = "mozilla.org"
        val existingTabForUrl = createTab(url = url)

        store =
            BrowserStore(
                BrowserState(
                    tabs = listOf(existingTabForUrl),
                    search = SearchState(regionSearchEngines = listOf(searchEngine)),
                )
            )

        val topSite =
            TopSite.Pinned(
                id = 1L,
                title = "Mozilla",
                url = url,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openPinned.testGetValue().orEmpty().isEmpty())

        verify {
            selectTabUseCase.invoke(existingTabForUrl.id)
            navController.navigate(R.id.browserFragment)
        }
    }

    @Test
    fun handleSelectGoogleDefaultTopSiteUS() = runTest {
        val topSite =
            TopSite.Default(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("US", "US")))

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openDefault.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                url = SupportUtils.GOOGLE_US_URL,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectGoogleDefaultTopSiteXX() = runTest {
        val topSite =
            TopSite.Default(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("DE", "FR")))

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openDefault.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_XX_URL,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectGoogleDefaultTopSite_EventPerformedSearchTopSite() = runTest {
        assertTrue(Events.performedSearch.testGetValue().orEmpty().isEmpty())

        val topSite =
            TopSite.Default(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(googleSearchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(Events.performedSearch.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openDefault.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun handleSelectDuckDuckGoTopSite_EventPerformedSearchTopSite() = runTest {
        assertTrue(Events.performedSearch.testGetValue().orEmpty().isEmpty())

        val topSite =
            TopSite.Pinned(
                id = 1L,
                title = "DuckDuckGo",
                url = "https://duckduckgo.com",
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns
            listOf(
                googleSearchEngine,
                duckDuckGoSearchEngine,
            )

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(Events.performedSearch.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun handleSelectGooglePinnedTopSiteUS() = runTest {
        val topSite =
            TopSite.Pinned(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("US", "US")))

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openPinned.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_US_URL,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectGooglePinnedTopSiteXX() = runTest {
        val topSite =
            TopSite.Pinned(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("DE", "FR")))

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openPinned.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_XX_URL,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectGoogleFrecentTopSiteUS() = runTest {
        val topSite =
            TopSite.Frecent(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("US", "US")))

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openFrecency.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_US_URL,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectGoogleFrecentTopSiteXX() = runTest {
        val topSite =
            TopSite.Frecent(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("DE", "FR")))

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openFrecency.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openGoogleSearchAttribution.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_XX_URL,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun handleSelectProvidedTopSite() = runTest {
        val topSite =
            TopSite.Provided(
                id = 1L,
                title = "Mozilla",
                url = "mozilla.org",
                clickUrl = "",
                imageUrl = "",
                impressionUrl = "",
                createdAt = 0,
            )
        val position = 0
        val controller = spyk(createController(this))

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position)

        assertTrue(TopSites.openInNewTab.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.openContileTopSite.testGetValue().orEmpty().isEmpty())

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true,
            )
        }
        verify { controller.recordTopSitesClickTelemetry(topSite, position) }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Ignore("Bug 2016888 - passes on individual test run, fails when running entire app test suite.")
    @Test
    fun `WHEN the provided top site is clicked THEN send a click callback request`() = runTest {
        val controller = spyk(createController(this))
        val topSite =
            TopSite.Provided(
                id = 3,
                title = "Mozilla",
                url = "https://mozilla.com",
                clickUrl = "https://mozilla.com/click",
                imageUrl = "https://test.com/image2.jpg",
                impressionUrl = "https://mozilla.com/impression",
                createdAt = 3,
            )
        val position = 0

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        assertTrue(TopSites.contileClick.testGetValue().orEmpty().isEmpty())

        var topSiteImpressionPinged = false
        val job =
            Pings.topsitesImpression.testBeforeNextSubmit {
                assertEquals(0L, TopSites.contileTileId.testGetValue())
                assertTrue(TopSites.contileAdvertiser.testGetValue().orEmpty().isEmpty())
                assertTrue(TopSites.contileReportingUrl.testGetValue().orEmpty().isEmpty())

                topSiteImpressionPinged = true
            }

        controller.handleSelectTopSite(topSite, position)

        val event = TopSites.contileClick.testGetValue()!!

        assertTrue(event.orEmpty().isEmpty())

        job.join()
        assertFalse(topSiteImpressionPinged)
    }

    @Ignore("Bug 2016888 - passes on individual test run, fails when running entire app test suite.")
    @Test
    fun `GIVEN Ads client is enabled WHEN the provided top site is clicked THEN send a click callback request`() =
        runTest {
            val controller = spyk(createController(this))
            val topSite =
                TopSite.Provided(
                    id = 3,
                    title = "Mozilla",
                    url = "https://mozilla.com",
                    clickUrl = "https://mozilla.com/click",
                    imageUrl = "https://test.com/image2.jpg",
                    impressionUrl = "https://mozilla.com/impression",
                    createdAt = 3,
                )
            val position = 0

            every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

            assertTrue(TopSites.contileClick.testGetValue().orEmpty().isEmpty())

            var topSiteImpressionPinged = false
            val job =
                Pings.topsitesImpression.testBeforeNextSubmit {
                    assertEquals(0L, TopSites.contileTileId.testGetValue())
                    assertTrue(TopSites.contileAdvertiser.testGetValue().orEmpty().isEmpty())
                    assertTrue(TopSites.contileReportingUrl.testGetValue().orEmpty().isEmpty())

                    topSiteImpressionPinged = true
                }

            controller.handleSelectTopSite(topSite, position)

            coVerify { mozAdsUseCases.recordClickInteraction(clickUrl = topSite.clickUrl) }

            val event = TopSites.contileClick.testGetValue()!!

            assertTrue(event.orEmpty().isEmpty())

            job.join()
            assertFalse(topSiteImpressionPinged)
        }

    @Ignore("Bug 2016888 - passes on individual test run, fails when running entire app test suite.")
    @Test
    fun `WHEN the provided top site is seen THEN send a impression callback request`() = runTest {
        val controller = spyk(createController(this))
        val topSite =
            TopSite.Provided(
                id = 3,
                title = "Mozilla",
                url = "https://mozilla.com",
                clickUrl = "https://mozilla.com/click",
                imageUrl = "https://test.com/image2.jpg",
                impressionUrl = "https://mozilla.com/impression",
                createdAt = 3,
            )
        val position = 0

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        assertTrue(TopSites.contileImpression.testGetValue().orEmpty().isEmpty())

        var topSiteImpressionSubmitted = false
        val job =
            Pings.topsitesImpression.testBeforeNextSubmit {
                assertEquals(0L, TopSites.contileTileId.testGetValue())
                assertTrue(TopSites.contileAdvertiser.testGetValue().orEmpty().isEmpty())
                assertTrue(TopSites.contileReportingUrl.testGetValue().orEmpty().isEmpty())

                topSiteImpressionSubmitted = true
            }

        controller.handleTopSiteImpression(topSite, position)

        val event = TopSites.contileImpression.testGetValue()!!

        assertTrue(event.orEmpty().isEmpty())

        job.join()
        assertFalse(topSiteImpressionSubmitted)
    }

    @Ignore("Bug 2016888 - passes on individual test run, fails when running entire app test suite.")
    @Test
    fun `GIVEN Ads client is enabled WHEN the provided top site is seen THEN send a impression callback request`() =
        runTest {
            val controller = spyk(createController(this))
            val topSite =
                TopSite.Provided(
                    id = 3,
                    title = "Mozilla",
                    url = "https://mozilla.com",
                    clickUrl = "https://mozilla.com/click",
                    imageUrl = "https://test.com/image2.jpg",
                    impressionUrl = "https://mozilla.com/impression",
                    createdAt = 3,
                )
            val position = 0

            every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

            assertTrue(TopSites.contileImpression.testGetValue().orEmpty().isEmpty())

            var topSiteImpressionSubmitted = false
            val job =
                Pings.topsitesImpression.testBeforeNextSubmit {
                    assertEquals(0L, TopSites.contileTileId.testGetValue())
                    assertTrue(TopSites.contileAdvertiser.testGetValue().orEmpty().isEmpty())
                    assertTrue(TopSites.contileReportingUrl.testGetValue().orEmpty().isEmpty())

                    topSiteImpressionSubmitted = true
                }

            controller.handleTopSiteImpression(topSite, position)

            coVerify { mozAdsUseCases.recordImpressionInteraction(impressionUrl = topSite.impressionUrl) }

            val event = TopSites.contileImpression.testGetValue()!!

            assertTrue(event.orEmpty().isEmpty())

            job.join()
            assertFalse(topSiteImpressionSubmitted)
        }

    @Test
    fun `GIVEN the shortcuts library source WHEN a provided top site is clicked THEN record contile_click with the shortcuts library source`() =
        runTest {
            val controller = spyk(createController(this, source = TopSitesSource.SHORTCUTS_LIBRARY))
            val topSite =
                TopSite.Provided(
                    id = 3,
                    title = "Mozilla",
                    url = "https://mozilla.com",
                    clickUrl = "https://mozilla.com/click",
                    imageUrl = "https://test.com/image2.jpg",
                    impressionUrl = "https://mozilla.com/impression",
                    createdAt = 3,
                )

            every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

            assertTrue(TopSites.contileClick.testGetValue().orEmpty().isEmpty())

            controller.handleSelectTopSite(topSite, position = 0)

            val event = TopSites.contileClick.testGetValue()!!
            assertTrue(event.orEmpty().isEmpty())
        }

    @Test
    fun `GIVEN the shortcuts library source WHEN a provided top site is seen THEN record contile_impression with the shortcuts library source`() =
        runTest {
            val controller = spyk(createController(this, source = TopSitesSource.SHORTCUTS_LIBRARY))
            val topSite =
                TopSite.Provided(
                    id = 3,
                    title = "Mozilla",
                    url = "https://mozilla.com",
                    clickUrl = "https://mozilla.com/click",
                    imageUrl = "https://test.com/image2.jpg",
                    impressionUrl = "https://mozilla.com/impression",
                    createdAt = 3,
                )

            assertTrue(TopSites.contileImpression.testGetValue().orEmpty().isEmpty())

            controller.handleTopSiteImpression(topSite, position = 0)

            val event = TopSites.contileImpression.testGetValue()!!
            assertTrue(event.orEmpty().isEmpty())
        }

    @Test
    fun `WHEN the default Google top site is removed THEN the correct metric is recorded`() = runTest {
        val controller = spyk(createController(this))
        val topSite =
            TopSite.Default(
                id = 1L,
                title = "Google",
                url = SupportUtils.GOOGLE_URL,
                createdAt = 0,
            )
        assertTrue(TopSites.remove.testGetValue().orEmpty().isEmpty())
        assertTrue(TopSites.googleTopSiteRemoved.testGetValue().orEmpty().isEmpty())

        controller.handleRemoveTopSiteClicked(topSite)

        assertTrue(TopSites.googleTopSiteRemoved.testGetValue().orEmpty().isEmpty())

        assertTrue(TopSites.remove.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun `GIVEN a default top site from the provider WHEN top site is removed THEN it is dispatched to be blocked`() =
        runTest {
            val topSite =
                TopSite.Default(id = null, title = "Wikipedia", url = "https://www.wikipedia.org/", createdAt = null)
            coEvery { defaultTopSitesProvider.getDefaultTopSites() } returns listOf(topSite)
            val controller = createController(this)

            controller.handleRemoveTopSiteClicked(topSite)
            testScheduler.advanceUntilIdle()

            verify { appStore.dispatch(AppAction.RemoveTopSite(topSite = topSite, shouldBlock = true)) }
            coVerify { topSitesUseCases.removeTopSites(topSite) }
        }

    @Test
    fun `WHEN the default Google top site is removed THEN it is not dispatched to be blocked`() = runTest {
        val topSite = TopSite.Default(id = 1L, title = "Google", url = SupportUtils.GOOGLE_URL, createdAt = 0)
        coEvery { defaultTopSitesProvider.getDefaultTopSites() } returns
            listOf(
                TopSite.Default(id = null, title = "Wikipedia", url = "https://www.wikipedia.org/", createdAt = null)
            )
        val controller = createController(this)

        controller.handleRemoveTopSiteClicked(topSite)
        testScheduler.advanceUntilIdle()

        verify { appStore.dispatch(AppAction.RemoveTopSite(topSite = topSite, shouldBlock = false)) }
        coVerify { topSitesUseCases.removeTopSites(topSite) }
    }

    @Test
    fun `GIVEN a pinned top site sharing the url of a default top site WHEN it is removed THEN it is dispatched to be blocked`() =
        runTest {
            val topSite =
                TopSite.Pinned(id = 1L, title = "Wikipedia", url = "https://www.wikipedia.org/", createdAt = 0)
            coEvery { defaultTopSitesProvider.getDefaultTopSites() } returns
                listOf(
                    TopSite.Default(
                        id = null,
                        title = "Wikipedia",
                        url = "https://www.wikipedia.org/",
                        createdAt = null,
                    )
                )
            val controller = createController(this)

            controller.handleRemoveTopSiteClicked(topSite)
            testScheduler.advanceUntilIdle()

            verify { appStore.dispatch(AppAction.RemoveTopSite(topSite = topSite, shouldBlock = true)) }
            coVerify { topSitesUseCases.removeTopSites(topSite) }
        }

    @Test
    fun `GIVEN a frecent top site sharing the url of a default top site WHEN it is removed THEN it is dispatched to be blocked`() =
        runTest {
            val topSite = TopSite.Frecent(id = 1L, title = "Wikipedia", url = "https://wikipedia.org", createdAt = 0)
            coEvery { defaultTopSitesProvider.getDefaultTopSites() } returns
                listOf(
                    TopSite.Default(
                        id = null,
                        title = "Wikipedia",
                        url = "https://www.wikipedia.org/",
                        createdAt = null,
                    )
                )
            val controller = createController(this)

            controller.handleRemoveTopSiteClicked(topSite)
            testScheduler.advanceUntilIdle()

            verify { appStore.dispatch(AppAction.RemoveTopSite(topSite = topSite, shouldBlock = true)) }
            coVerify { topSitesUseCases.removeTopSites(topSite) }
        }

    @Test
    fun `GIVEN a pinned top site not sharing the url of a default top site WHEN it is removed THEN it is not dispatched to be blocked`() =
        runTest {
            val topSite = TopSite.Pinned(id = 1L, title = "Mozilla", url = "https://www.mozilla.org/", createdAt = 0)
            coEvery { defaultTopSitesProvider.getDefaultTopSites() } returns
                listOf(
                    TopSite.Default(
                        id = null,
                        title = "Wikipedia",
                        url = "https://www.wikipedia.org/",
                        createdAt = null,
                    )
                )
            val controller = createController(this)

            controller.handleRemoveTopSiteClicked(topSite)
            testScheduler.advanceUntilIdle()

            verify { appStore.dispatch(AppAction.RemoveTopSite(topSite = topSite, shouldBlock = false)) }
            coVerify { topSitesUseCases.removeTopSites(topSite) }
        }

    @Test
    fun `WHEN the frecent top site is updated THEN add the frecent top site as a pinned top site`() = runTest {
        val topSite =
            TopSite.Frecent(
                id = 1L,
                title = "Mozilla",
                url = "mozilla.org",
                createdAt = 0,
            )

        val controller = spyk(createController(this))
        val title = "Firefox"
        val url = "firefox.com"

        controller.updateTopSite(topSite = topSite, title = title, url = url)

        coVerify {
            topSitesUseCases.addPinnedSites(title = title, url = url)
        }
        verify {
            appStore.dispatch(ShortcutAction.FrecencyTopSitePromoted)
        }
    }

    @Test
    fun `WHEN the pinned top site is updated THEN update the pinned top site in storage`() = runTest {
        val topSite =
            TopSite.Pinned(
                id = 1L,
                title = "Mozilla",
                url = "mozilla.org",
                createdAt = 0,
            )

        val controller = spyk(createController(this))
        val title = "Firefox"
        val url = "firefox.com"

        controller.updateTopSite(topSite = topSite, title = title, url = url)

        coVerify {
            topSitesUseCases.updateTopSites(topSite = topSite, title = title, url = url)
        }
    }

    @Test
    fun `WHEN a new shortcut is saved THEN add the shortcut as a pinned top site`() = runTest {
        val controller = createController(this)
        val title = "Firefox"
        val url = "firefox.com"

        controller.handleSaveShortcut(
            title = title,
            url = url,
            source = AddShortcutSource.MANUAL,
            entryPoint = AddShortcutEntryPoint.HOMEPAGE,
        )
        testScheduler.advanceUntilIdle()

        coVerify {
            topSitesUseCases.addPinnedSites(title = title, url = url)
        }
        verify {
            appStore.dispatch(
                ShortcutAction.ShortcutAdded(
                    source = AddShortcutSource.MANUAL,
                    entryPoint = AddShortcutEntryPoint.HOMEPAGE,
                )
            )
        }
    }

    @Test
    fun `WHEN handleTopSiteSettingsClicked is called THEN navigate to the HomeSettingsFragment AND report the interaction`() =
        runTest {
            createController(this).handleTopSiteSettingsClicked()

            assertTrue(TopSites.contileSettings.testGetValue().orEmpty().isEmpty())

            verify { navController.navigate(R.id.homeSettingsFragment) }
        }

    @Test
    fun `WHEN handleSponsorPrivacyClicked is called THEN navigate to the privacy webpage AND report the interaction`() =
        runTest {
            createController(this).handleSponsorPrivacyClicked()

            assertTrue(TopSites.contileSponsorsAndPrivacy.testGetValue().orEmpty().isEmpty())

            verify {
                navController.navigate(R.id.browserFragment)
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.SPONSOR_PRIVACY),
                    newTab = true,
                    private = false,
                )
            }
        }

    @Test
    fun `GIVEN current destination is the shortcuts fragmentWHEN handleSponsorPrivacyClicked is called THEN navigate to the privacy webpage AND report the interaction`() =
        runTest {
            every { navController.currentDestination } returns
                mockk {
                    every { id } returns R.id.shortcutsFragment
                }

            createController(this, source = TopSitesSource.SHORTCUTS_LIBRARY).handleSponsorPrivacyClicked()

            assertTrue(TopSites.contileSponsorsAndPrivacy.testGetValue().orEmpty().isEmpty())

            verify {
                navController.navigate(ShortcutsFragmentDirections.actionShortcutsFragmentToBrowserFragment())
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.SPONSOR_PRIVACY),
                    newTab = true,
                    private = false,
                )
            }
        }

    @Test
    fun `WHEN top site long clicked is called THEN report the top site long click telemetry`() = runTest {
        assertTrue(TopSites.longPress.testGetValue().orEmpty().isEmpty())

        val topSite =
            TopSite.Provided(
                id = 1L,
                title = "Mozilla",
                url = "mozilla.org",
                clickUrl = "",
                imageUrl = "",
                impressionUrl = "",
                createdAt = 0,
            )

        createController(this).handleTopSiteLongClicked(topSite)

        assertTrue(TopSites.longPress.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun `WHEN handleOpenInPrivateTabClicked is called with a TopSite#Provided site THEN navigate to the top site and record telemetry`() =
        runTest {
            val topSite =
                TopSite.Provided(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    clickUrl = "",
                    imageUrl = "",
                    impressionUrl = "",
                    createdAt = 0,
                )
            createController(this).handleOpenInPrivateTabClicked(topSite)

            assertTrue(TopSites.openContileInPrivateTab.testGetValue().orEmpty().isEmpty())

            verify {
                navController.navigate(R.id.browserFragment)
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = topSite.url,
                    newTab = true,
                    private = true,
                )
            }
        }

    @Test
    fun `GIVEN current destination is the shortcuts fragment WHEN handleOpenInPrivateTabClicked is called with a TopSite#Provided site THEN navigate to the top site and record telemetry`() =
        runTest {
            every { navController.currentDestination } returns
                mockk {
                    every { id } returns R.id.shortcutsFragment
                }

            val topSite =
                TopSite.Provided(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    clickUrl = "",
                    imageUrl = "",
                    impressionUrl = "",
                    createdAt = 0,
                )
            createController(this, source = TopSitesSource.SHORTCUTS_LIBRARY).handleOpenInPrivateTabClicked(topSite)

            assertTrue(TopSites.openContileInPrivateTab.testGetValue().orEmpty().isEmpty())

            verify {
                navController.navigate(ShortcutsFragmentDirections.actionShortcutsFragmentToBrowserFragment())
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = topSite.url,
                    newTab = true,
                    private = true,
                )
            }
        }

    @Test
    fun `GIVEN homepage as a new tab is enabled WHEN handleOpenInPrivateTabClicked is called with a TopSite#Provided site THEN navigate to the top site and record telemetry`() =
        runTest {
            every { settings.enableHomepageAsNewTab } returns true

            val topSite =
                TopSite.Provided(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    clickUrl = "",
                    imageUrl = "",
                    impressionUrl = "",
                    createdAt = 0,
                )
            createController(this).handleOpenInPrivateTabClicked(topSite)

            assertTrue(TopSites.openContileInPrivateTab.testGetValue().orEmpty().isEmpty())

            verify {
                navController.navigate(R.id.browserFragment)
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = topSite.url,
                    newTab = true,
                    private = true,
                )
            }
        }

    @Test
    fun `WHEN handleOpenInPrivateTabClicked is called with a Default, Pinned, or Frecent top site THEN openInPrivateTab event is recorded`() =
        runTest {
            val controller = createController(this)
            val topSite1 =
                TopSite.Default(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    createdAt = 0,
                )
            val topSite2 =
                TopSite.Pinned(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    createdAt = 0,
                )
            val topSite3 =
                TopSite.Frecent(
                    id = 1L,
                    title = "Mozilla",
                    url = "mozilla.org",
                    createdAt = 0,
                )
            assertTrue(TopSites.openInPrivateTab.testGetValue().orEmpty().isEmpty())

            controller.handleOpenInPrivateTabClicked(topSite1)
            controller.handleOpenInPrivateTabClicked(topSite2)
            controller.handleOpenInPrivateTabClicked(topSite3)

            assertTrue(TopSites.openInPrivateTab.testGetValue().orEmpty().isEmpty())
        }

    fun `WHEN screen is shown THEN impression is logged`() = runTest {
        assertTrue(ShortcutsLibrary.viewed.testGetValue().orEmpty().isEmpty())
        val controller = createController(this)
        controller.handleShortcutsLibraryViewed()
        assertTrue(ShortcutsLibrary.viewed.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun `WHEN the shortcuts section is expanded THEN show_more is recorded`() = runTest {
        assertTrue(TopSites.showMore.testGetValue().orEmpty().isEmpty())

        val controller = createController(this)
        controller.handleExpandToggleClicked(isExpanded = true)

        assertTrue(TopSites.showMore.testGetValue().orEmpty().isEmpty())
        assertTrue(TopSites.showLess.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun `WHEN the shortcuts section is collapsed THEN show_less is recorded`() = runTest {
        assertTrue(TopSites.showLess.testGetValue().orEmpty().isEmpty())

        val controller = createController(this)
        controller.handleExpandToggleClicked(isExpanded = false)

        assertTrue(TopSites.showLess.testGetValue().orEmpty().isEmpty())
        assertTrue(TopSites.showMore.testGetValue().orEmpty().isEmpty())
    }

    private fun createController(
        scope: CoroutineScope,
        source: TopSitesSource = TopSitesSource.HOMEPAGE,
    ): DefaultTopSiteController =
        DefaultTopSiteController(
            activityRef = WeakReference(activity),
            navControllerRef = WeakReference(navController),
            store = store,
            appStore = appStore,
            settings = settings,
            addTabUseCase = tabsUseCases.addTab,
            selectTabUseCase = selectTabUseCase.selectTab,
            fenixBrowserUseCases = fenixBrowserUseCases,
            topSitesUseCases = topSitesUseCases,
            defaultTopSitesProvider = defaultTopSitesProvider,
            mozAdsUseCases = mozAdsUseCases,
            viewLifecycleScope = scope,
            source = source,
        )
}
