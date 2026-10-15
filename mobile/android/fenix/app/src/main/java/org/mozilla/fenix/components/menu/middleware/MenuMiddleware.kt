/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.middleware

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.menu.data.ExpandableMenuItem
import mozilla.components.compose.menu.store.MenuAction
import mozilla.components.compose.menu.store.MenuAction.Init
import mozilla.components.compose.menu.store.MenuAction.Update
import mozilla.components.compose.menu.store.MenuState
import mozilla.components.compose.menu.store.MenuStore
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.top.sites.PinnedSiteStorage
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.ui.widgets.withCenterAlignedButtons
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Vpn
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.UseCases
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.components.appstate.AppAction.BookmarkAction
import org.mozilla.fenix.components.appstate.AppAction.FindInPageAction
import org.mozilla.fenix.components.appstate.AppAction.ReaderViewAction
import org.mozilla.fenix.components.appstate.AppAction.ShortcutAction
import org.mozilla.fenix.components.menu.BrowserMenuBuilder
import org.mozilla.fenix.components.menu.MenuFragmentDirections
import org.mozilla.fenix.components.menu.store.IPProtectionMenuStatus
import org.mozilla.fenix.components.menu.store.MenuAction.AddBookmark
import org.mozilla.fenix.components.menu.store.MenuAction.AddShortcut
import org.mozilla.fenix.components.menu.store.MenuAction.CustomizeReaderView
import org.mozilla.fenix.components.menu.store.MenuAction.FindInPage
import org.mozilla.fenix.components.menu.store.MenuAction.IPProtectionToggle
import org.mozilla.fenix.components.menu.store.MenuAction.MoveToNonPrivateTab
import org.mozilla.fenix.components.menu.store.MenuAction.Navigate
import org.mozilla.fenix.components.menu.store.MenuAction.OnMoreMenuClicked
import org.mozilla.fenix.components.menu.store.MenuAction.OnSummarizationMenuExposed
import org.mozilla.fenix.components.menu.store.MenuAction.OpenInApp
import org.mozilla.fenix.components.menu.store.MenuAction.PrintRequested
import org.mozilla.fenix.components.menu.store.MenuAction.RemoveShortcut
import org.mozilla.fenix.components.menu.store.MenuAction.RequestDesktopSite
import org.mozilla.fenix.components.menu.store.MenuAction.RequestMobileSite
import org.mozilla.fenix.components.menu.store.MenuAction.SaveAsPdfRequested
import org.mozilla.fenix.components.menu.toMenuState
import org.mozilla.fenix.components.metrics.MetricsUtils
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.openToBrowser
import org.mozilla.fenix.home.topsites.AddShortcutEntryPoint
import org.mozilla.fenix.home.topsites.AddShortcutSource
import org.mozilla.fenix.summarization.eligibility.SummarizationEligibilityChecker
import org.mozilla.fenix.summarization.isSummarizePageMenuItem
import org.mozilla.fenix.summarization.onboarding.FenixSummarizationFeatureConfiguration
import org.mozilla.fenix.summarization.onboarding.SummarizationFeatureDiscoveryConfiguration
import org.mozilla.fenix.summarization.onboarding.SummarizeDiscoveryEvent
import org.mozilla.fenix.tabstray.ext.isNormalTab
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.webcompat.WEB_COMPAT_REPORTER_URL
import org.mozilla.fenix.webcompat.WebCompatReporterMoreInfoSender

/**
 * [MenuStore] middleware handling all user interactions.
 *
 * @param appStore [AppStore] for syncing with other application features
 * @param browserStore [BrowserStore] used to read information about the current browsing status.
 * @param ipProtectionStore [IPProtectionStore] used to read the current status and to toggle IP protection.
 * @param useCases [UseCases] helping this integrate with other features of the application.
 * @param browserMenuBuilder [BrowserMenuBuilder] providing the menu to show, kept up to date.
 * @param navController [NavController] for navigating to other screens.
 * @param summarizationSettings [FenixSummarizationFeatureConfiguration] for managing the summarization feature.
 * @param summarizationEligibilityChecker [SummarizationEligibilityChecker] for checking the eligibility of the
 *   summarization feature.
 * @param settings [Settings] for checking the user's preferences, like whether they allow telemetry.
 * @param webCompatReporterMoreInfoSender [WebCompatReporterMoreInfoSender] for sending the details of a broken site to
 *   webcompat.com.
 * @param pinnedSiteStorage [PinnedSiteStorage] for checking the shortcuts the user already has.
 * @param materialAlertDialogBuilder [MaterialAlertDialogBuilder] for telling the user when they cannot have another
 *   shortcut.
 * @param scope [CoroutineScope] tied to the lifetime of the menu, used for all work that is only useful while the menu
 *   is shown.
 * @param applicationScope [CoroutineScope] tied to the lifetime of the application, used for the work that cannot be
 *   interrupted and so must not be tied to the menu.
 */
@Suppress("LongParameterList")
class MenuMiddleware(
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val ipProtectionStore: IPProtectionStore,
    private val useCases: UseCases,
    private val browserMenuBuilder: BrowserMenuBuilder,
    private val navController: NavController,
    private val summarizationSettings: SummarizationFeatureDiscoveryConfiguration,
    private val summarizationEligibilityChecker: SummarizationEligibilityChecker,
    private val settings: Settings,
    private val webCompatReporterMoreInfoSender: WebCompatReporterMoreInfoSender,
    private val pinnedSiteStorage: PinnedSiteStorage,
    private val materialAlertDialogBuilder: MaterialAlertDialogBuilder,
    private val scope: CoroutineScope,
    private val applicationScope: CoroutineScope,
) : Middleware<MenuState, MenuAction> {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun invoke(
        store: Store<MenuState, MenuAction>,
        next: (MenuAction) -> Unit,
        action: MenuAction,
    ) {
        when (action) {
            is Init -> observeMenuStructureUpdates(store)

            is CustomizeReaderView -> {
                dismissMenu()
                appStore.dispatch(ReaderViewAction.ReaderViewControlsShown)
            }

            is IPProtectionToggle -> handleIPProtectionToggle()

            is Navigate.IPProtectionSettings -> {
                Vpn.settingsPageTapped.record(Vpn.SettingsPageTappedExtra(entrypoint = "Menu"))
                navigateToIPProtectionSettings()
            }

            is Navigate.DeleteBrowsingData -> navigate(NavGraphDirections.actionGlobalDeleteBrowsingDataFragment())

            is AddBookmark -> addBookmark()

            is Navigate.EditBookmark -> {
                navigateToEditBookmark(action.guidToEdit)
            }

            is FindInPage -> {
                dismissMenu()
                appStore.dispatch(FindInPageAction.FindInPageStarted)
            }

            is RequestDesktopSite -> requestSiteMode(enableDesktopMode = true)

            is RequestMobileSite -> requestSiteMode(enableDesktopMode = false)

            is Navigate.Translate -> {
                navController.nav(
                    R.id.menuFragment,
                    MenuFragmentDirections.actionMenuFragmentToTranslationsDialogFragment(
                        sessionId = browserStore.state.selectedTabId
                    ),
                    navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build(),
                )
            }

            is Navigate.Summarizer -> {
                navController.nav(
                    R.id.menuFragment,
                    MenuFragmentDirections.actionMenuFragmentToSummarizationFragment(
                        sessionId = browserStore.state.selectedTabId
                    ),
                    navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build(),
                )
            }

            is OnMoreMenuClicked -> handleMoreBeingClicked(store)

            is OnSummarizationMenuExposed -> handleSummarizationOptionBeingShown()

            is MoveToNonPrivateTab ->
                browserStore.state.selectedTab?.id?.let { tabId ->
                    dismissMenu()
                    useCases.tabsUseCases.migratePrivateTabUseCase(tabId)
                }

            is Navigate.WebCompatReporter -> reportBrokenSite()

            is AddShortcut -> addShortcut()

            is RemoveShortcut -> removeShortcut()

            is Navigate.AddToHomeScreen -> addToHomeScreen()

            is Navigate.SaveToCollection -> saveCurrentPageToCollection(action.hasCollection)

            is OpenInApp -> openCurrentPageInApp()

            is SaveAsPdfRequested -> {
                dismissMenu()
                useCases.sessionUseCases.saveToPdf(browserStore.state.selectedTabId)
            }

            is PrintRequested -> {
                dismissMenu()
                useCases.sessionUseCases.printContent(browserStore.state.selectedTabId)
            }

            is Navigate.Back -> handleBackNavigation(action)

            is Navigate.Forward -> handleForwardNavigation(action)

            is Navigate.Share -> handleShare()

            is Navigate.Reload -> handleReload(action)

            is Navigate.Stop -> handleStop()

            else -> {
                // no-op
            }
        }

        next(action)
    }

    /** The menu is deliberately left open while connecting, so that the user can see the status change. */
    private fun handleIPProtectionToggle() {
        when (ipProtectionStore.state.toMenuState().status) {
            IPProtectionMenuStatus.Disabled -> {
                Vpn.menuTurnedOn.record()
                ipProtectionStore.dispatch(IPProtectionAction.Toggle)
            }

            IPProtectionMenuStatus.Enabled -> {
                Vpn.menuTurnedOff.record()
                ipProtectionStore.dispatch(IPProtectionAction.Toggle)
            }

            IPProtectionMenuStatus.AuthRequired -> {
                Vpn.menuTryItTapped.record(NoExtras())
                navigateToIPProtectionSettings()
            }

            IPProtectionMenuStatus.Activating,
            IPProtectionMenuStatus.DataLimitReached,
            IPProtectionMenuStatus.ConnectionError -> ipProtectionStore.dispatch(IPProtectionAction.Toggle)
        }
    }

    private fun requestSiteMode(enableDesktopMode: Boolean) {
        val tabId = browserStore.state.selectedTab?.id ?: return

        dismissMenu()
        useCases.sessionUseCases.requestDesktopSite(enable = enableDesktopMode, tabId = tabId)
    }

    private fun addBookmark() = scope.launch {
        val selectedTab = browserStore.state.selectedTab ?: return@launch
        val url = selectedTab.getTabUrl() ?: return@launch
        val title = selectedTab.content.title

        // Saving a bookmark will run until completion even if the coroutine is canceled.
        // As such we must ensure the work below does not reference any property of this middleware which could result
        // in it being leaked - together with everything it holds - while waiting for the bookmark to be saved.
        val addBookmark = useCases.bookmarksUseCases.addBookmark
        val result = applicationScope.async { addBookmark(url = url, title = title) }.await()

        appStore.dispatch(
            BookmarkAction.BookmarkAdded(
                guidToEdit = result.guidToEdit,
                parentNode = result.parentNode,
                source = MetricsUtils.BookmarkAction.Source.MENU_DIALOG,
            )
        )

        dismissMenu()
    }

    /**
     * A broken site is reported from inside the app if the user allows telemetry, since only then can the details of
     * the issue be collected. If they don't, the report is filled in on webcompat.com, with the details of the issue
     * sent separately before opening the website, so that the engine still has the page to collect them from.
     */
    private fun reportBrokenSite() {
        val selectedTab = browserStore.state.selectedTab ?: return
        val tabUrl = selectedTab.content.url

        if (settings.isTelemetryEnabled) {
            navigate(MenuFragmentDirections.actionMenuFragmentToWebCompatReporterFragment(tabUrl = tabUrl))
            return
        }

        scope.launch {
            webCompatReporterMoreInfoSender.sendMoreWebCompatInfo(
                reason = null,
                problemDescription = null,
                enteredUrl = null,
                tabUrl = selectedTab.getTabUrl(),
                engineSession = selectedTab.engineState.engineSession,
            )

            dismissMenu()
            navController.openToBrowser()
            useCases.fenixBrowserUseCases.loadUrlOrSearch(
                searchTermOrURL = "$WEB_COMPAT_REPORTER_URL$tabUrl",
                newTab = true,
                private = appStore.state.mode.isPrivate,
            )
        }
    }

    /** Shortcuts are limited in number, so the user is told when the current page cannot become one of them. */
    private fun addShortcut() = scope.launch {
        val selectedTab = browserStore.state.selectedTab ?: return@launch
        val url = selectedTab.getTabUrl() ?: return@launch
        val title = selectedTab.content.title

        val shortcuts = pinnedSiteStorage.getPinnedSites()
        // The menu item may have been shown before the page was known to already be a shortcut.
        if (shortcuts.any { it.url == url }) return@launch

        if (shortcuts.count { it.isPinned() } >= settings.topSitesMaxLimit) {
            showMaxShortcutsReached()
            dismissMenu()
            return@launch
        }

        useCases.topSitesUseCase.addPinnedSites(title = title, url = url)

        appStore.dispatch(
            ShortcutAction.ShortcutAdded(
                source = AddShortcutSource.MANUAL,
                entryPoint = AddShortcutEntryPoint.PAGE_MENU,
            )
        )

        dismissMenu()
    }

    private fun removeShortcut() = scope.launch {
        val url = browserStore.state.selectedTab?.getTabUrl() ?: return@launch
        val shortcut = pinnedSiteStorage.getPinnedSites().firstOrNull { it.url == url } ?: return@launch

        // Removing a shortcut also deletes the history entries of that page, which will run until completion even if
        // the
        // coroutine is canceled. As such we must ensure the work below does not reference any property of this
        // middleware which could result in it being leaked - together with everything it holds - while waiting.
        val removeShortcut = useCases.topSitesUseCase.removeTopSites
        applicationScope.async { removeShortcut(topSite = shortcut) }.await()

        dismissMenu()
    }

    private fun showMaxShortcutsReached() {
        materialAlertDialogBuilder
            .apply {
                setTitle(R.string.shortcut_max_limit_title)
                setMessage(R.string.shortcut_max_limit_content)
                setPositiveButton(R.string.top_sites_max_limit_confirmation_button) { dialog, _ -> dialog.dismiss() }
                create().withCenterAlignedButtons()
            }
            .show()
    }

    private fun addToHomeScreen() {
        settings.installPwaOpened = true

        // A page offering a web app manifest is installed as a PWA right away. Any other is added as a simple shortcut,
        // for which the user is first asked to confirm the name it will have on the home screen.
        when (useCases.webAppUseCases.isInstallable()) {
            true ->
                scope.launch {
                    useCases.webAppUseCases.addToHomescreen()
                    dismissMenu()
                }
            else ->
                navController.nav(
                    R.id.menuFragment,
                    MenuFragmentDirections.actionMenuFragmentToCreateShortcutFragment(),
                    navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build(),
                )
        }
    }

    private fun navigateToEditBookmark(guidToEdit: String?) {
        if (guidToEdit == null) return

        navigate(
            NavGraphDirections.actionGlobalBookmarkEditFragment(
                guidToEdit = guidToEdit,
                requiresSnackbarPaddingForToolbar = true,
            )
        )
    }

    private fun navigateToIPProtectionSettings() {
        navigate(
            NavGraphDirections.actionGlobalIpProtectionFragment(entrypoint = FenixFxAEntryPoint.IPProtectionMainMenu)
        )
    }

    private fun handleMoreBeingClicked(store: Store<MenuState, MenuAction>) {
        val moreItem =
            store.state.menuGroups
                .flatMap { it.items }
                .filterIsInstance<ExpandableMenuItem>()
                .firstOrNull { it.onClickEvent == OnMoreMenuClicked }
        if (
            moreItem?.subMenuItems?.any { it.isSummarizePageMenuItem() } == true &&
                browserStore.state.selectedTab?.content?.private == false &&
                summarizationSettings.shouldHighlightOverflowMenuItem
        ) {
            summarizationSettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuOverflowInteraction)
        }
    }

    private fun handleSummarizationOptionBeingShown() {
        scope.launch {
            val currentTab = browserStore.state.selectedTab
            val isSummarizationEnabled =
                summarizationSettings.showMenuItem &&
                    currentTab?.isNormalTab() ?: false &&
                    currentTab.checkSummarizationEligibility()

            if (isSummarizationEnabled) {
                summarizationSettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuItemExposure)
            }
        }
    }

    /**
     * The current page is opened in the app handling it, which the user is not told about again afterwards.
     *
     * Whether there is such an app is resolved as late as possible, since the user may have navigated away from the
     * page the item was built for.
     */
    private fun openCurrentPageInApp() {
        val url = browserStore.state.selectedTab?.content?.url ?: return
        val redirect = useCases.appLinksUseCases.appLinkRedirect(url)
        if (!redirect.hasExternalApp()) return

        settings.openInAppOpened = true

        useCases.appLinksUseCases.openAppLink(redirect.appIntent)
        dismissMenu()
    }

    private fun saveCurrentPageToCollection(collectionsAlreadyExist: Boolean) {
        browserStore.state.selectedTab?.let { currentSession ->
            navController.nav(
                R.id.menuFragment,
                MenuFragmentDirections.actionGlobalCollectionCreationFragment(
                    tabIds = arrayOf(currentSession.id),
                    selectedTabIds = arrayOf(currentSession.id),
                    saveCollectionStep =
                        if (collectionsAlreadyExist) {
                            SaveCollectionStep.SelectCollection
                        } else {
                            SaveCollectionStep.NameCollection
                        },
                ),
                navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build(),
            )
        }
    }

    private fun handleBackNavigation(action: Navigate.Back) {
        val tabId = browserStore.state.selectedTab?.id ?: return
        if (action.viewHistory) {
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build()
            navigate(
                NavGraphDirections.actionGlobalTabHistoryDialogFragment(activeSessionId = null),
                navOptions,
            )
        } else {
            dismissMenu()
            useCases.sessionUseCases.goBack(tabId = tabId)
        }
    }

    private fun handleForwardNavigation(action: Navigate.Forward) {
        val tabId = browserStore.state.selectedTab?.id ?: return
        if (action.viewHistory) {
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build()
            navigate(
                NavGraphDirections.actionGlobalTabHistoryDialogFragment(activeSessionId = null),
                navOptions,
            )
        } else {
            dismissMenu()
            useCases.sessionUseCases.goForward(tabId = tabId)
        }
    }

    private fun handleShare() {
        val selectedTab = browserStore.state.selectedTab ?: return
        dismissMenu()
        val shareData =
            ShareData(
                title = selectedTab.content.title,
                url = selectedTab.getTabUrl(),
                private = selectedTab.content.private,
            )
        useCases.shareUseCases.shareUrl(
            id = selectedTab.id,
            url = selectedTab.getTabUrl(),
            title = selectedTab.content.title,
            source = ShareSource.BROWSER_MENU,
            isPrivate = selectedTab.content.private,
            navigateToShareFragment = {
                navigate(
                    NavGraphDirections.actionGlobalShareFragment(
                        data = arrayOf(shareData),
                        showPage = true,
                        sessionId = selectedTab.id,
                    ),
                    navOptions = NavOptions.Builder().setPopUpTo(R.id.browserFragment, false).build(),
                )
            },
        )
    }

    private fun handleReload(action: Navigate.Reload) {
        val tabId = browserStore.state.selectedTab?.id ?: return
        dismissMenu()
        useCases.sessionUseCases.reload(
            tabId = tabId,
            flags =
                if (action.bypassCache) {
                    LoadUrlFlags.select(LoadUrlFlags.BYPASS_CACHE)
                } else {
                    LoadUrlFlags.none()
                },
        )
    }

    private fun handleStop() {
        val tabId = browserStore.state.selectedTab?.id ?: return
        dismissMenu()
        useCases.sessionUseCases.stopLoading(tabId = tabId)
    }

    private fun observeMenuStructureUpdates(store: Store<MenuState, MenuAction>) = scope.launch {
        browserMenuBuilder.menuStructure.collect { store.dispatch(Update(it)) }
    }

    private fun navigate(directions: NavDirections, navOptions: NavOptions? = null) {
        navController.nav(R.id.menuFragment, directions, navOptions)
    }

    private fun dismissMenu() {
        navController.popBackStack(R.id.menuFragment, true)
    }

    /** Only the shortcuts the user can add themselves count towards the limit of how many they can have. */
    private fun TopSite.isPinned() = this is TopSite.Default || this is TopSite.Pinned

    private suspend fun SessionState?.checkSummarizationEligibility(): Boolean =
        this@checkSummarizationEligibility?.engineState?.engineSession?.let { session ->
            summarizationEligibilityChecker.checkLanguage(session).getOrDefault(false)
        } ?: false
}
