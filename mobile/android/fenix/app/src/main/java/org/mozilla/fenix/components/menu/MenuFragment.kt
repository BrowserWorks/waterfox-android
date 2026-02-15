/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.lib.state.Middleware
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.settings.deletebrowsingdata.DefaultDeleteBrowsingDataController
import org.mozilla.fenix.settings.deletebrowsingdata.DefaultDeleteBrowsingDataController.DataStorage
import org.mozilla.fenix.settings.deletebrowsingdata.DefaultDeleteBrowsingDataController.DeleteDataUseCases
import org.mozilla.fenix.settings.deletebrowsingdata.DefaultDeleteBrowsingDataController.Stores
import android.app.Dialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.fragment.compose.content
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.R as materialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mozilla.components.compose.menu.Menu
import mozilla.components.compose.menu.store.MenuState
import mozilla.components.compose.menu.store.MenuStore
import mozilla.components.feature.automotive.isAndroidAutomotiveAvailable
import mozilla.components.lib.state.helpers.StoreProvider.Companion.composableStore
import mozilla.components.support.ktx.android.util.dpToPx
import mozilla.components.support.utils.ext.getWindowInsets
import mozilla.components.support.utils.ext.isLandscape
import mozilla.components.support.utils.ext.pixelSizeFor
import mozilla.components.support.utils.ext.top
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.R
import org.mozilla.fenix.bookmarks.BookmarkMenuItemProvider
import org.mozilla.fenix.browser.BackMenuItemProvider
import org.mozilla.fenix.browser.DesktopSiteMenuItemProvider
import org.mozilla.fenix.browser.ForwardMenuItemProvider
import org.mozilla.fenix.browser.RefreshMenuItemProvider
import org.mozilla.fenix.browser.ShareMenuItemProvider
import org.mozilla.fenix.browser.applinks.OpenInAppMenuItemProvider
import org.mozilla.fenix.browser.menu.MoreMenuItemsProvider
import org.mozilla.fenix.browser.menu.MoveToNormalTabsMenuItemProvider
import org.mozilla.fenix.browser.readermode.ReaderViewMenuItemProvider
import org.mozilla.fenix.collections.SaveToCollectionMenuItemProvider
import org.mozilla.fenix.components.FindInPageMenuItemProvider
import org.mozilla.fenix.components.menu.compose.MenuDialogBottomSheet
import org.mozilla.fenix.components.menu.compose.MenuHandleState
import org.mozilla.fenix.components.menu.middleware.MenuMiddleware
import org.mozilla.fenix.components.menu.middleware.MenuTelemetryMiddleware
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.isToolbarAtBottom
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.topsites.ShortcutMenuItemProvider
import org.mozilla.fenix.ipprotection.VpnMenuItemProvider
import org.mozilla.fenix.pdf.SaveAsPdfMenuItemProvider
import org.mozilla.fenix.print.PrintMenuItemProvider
import org.mozilla.fenix.shortcut.AddToHomeScreenMenuItemProvider
import org.mozilla.fenix.summarization.SummarizePageMenuItemProvider
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.translations.TranslationsEnabledSettings
import org.mozilla.fenix.translations.TranslationsMenuItemProvider
import org.mozilla.fenix.webcompat.DefaultWebCompatReporterMoreInfoSender
import org.mozilla.fenix.webcompat.ReportBrokenSiteMenuItemProvider
import org.mozilla.fenix.webcompat.middleware.DefaultWebCompatReporterRetrievalService

private const val EXPANDED_OFFSET = 56
private const val HIDING_FRICTION = 0.9f
private const val MENU_ANIMATION_DURATION = 300L
private const val MENU_ANIMATION_START_OFFSET_RATIO = 0.2f

/** A bottom sheet fragment hosting the customizable menu. */
class MenuFragment : BottomSheetDialogFragment() {
    private val snackbarHostState = SnackbarHostState()
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private val deleteBrowsingDataController by lazy {
        DefaultDeleteBrowsingDataController(
            deleteDataUseCases =
                DeleteDataUseCases(
                    removeAllTabs = requireComponents.useCases.tabsUseCases.removeAllTabs,
                    removeAllDownloads = requireComponents.useCases.downloadUseCases.removeAllDownloads,
                ),
            dataStorage =
                DataStorage(
                    history = requireComponents.core.historyStorage,
                    permissions = requireComponents.core.permissionStorage,
                ),
            stores =
                Stores(
                    appStore = requireComponents.appStore,
                    browserStore = requireComponents.core.store,
                ),
            engine = requireComponents.core.engine,
            settings = requireComponents.settings,
        )
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Events.toolbarMenuVisible.record(NoExtras())

        return object : BottomSheetDialog(requireContext(), theme) {
                override fun onKeyDown(
                    keyCode: Int,
                    event: KeyEvent,
                ): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU) {
                        dismiss()
                        return true
                    }
                    return super.onKeyDown(keyCode, event)
                }
            }
            .apply {
                setOnShowListener {
                    val bottomSheet = findViewById<View?>(materialR.id.design_bottom_sheet)

                    bottomSheet?.let {
                        ViewCompat.setOnApplyWindowInsetsListener(it) { view, insets ->
                            val systemBarInsets = insets.getInsets(systemBars())
                            view.setPadding(0, systemBarInsets.top, 0, systemBarInsets.bottom)
                            insets
                        }
                    }
                    bottomSheet?.setBackgroundResource(R.drawable.bottom_sheet_with_top_rounded_corners)

                    // https://bugzilla.mozilla.org/show_bug.cgi?id=1982004
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        bottomSheet?.let { sheet ->
                            sheet.translationY = sheet.height * MENU_ANIMATION_START_OFFSET_RATIO
                            sheet
                                .animate()
                                .translationY(0f)
                                .setInterpolator(OvershootInterpolator())
                                .setDuration(MENU_ANIMATION_DURATION)
                                .start()
                        }
                    }

                    bottomSheetBehavior = bottomSheet?.let {
                        BottomSheetBehavior.from(it).apply {
                            maxWidth = calculateMenuSheetWidth()
                            peekHeight = resources.displayMetrics.heightPixels
                            maxHeight = calculateMenuSheetHeight()
                            skipCollapsed = true
                            state = BottomSheetBehavior.STATE_EXPANDED
                            hideFriction = HIDING_FRICTION
                        }
                    }
                }
            }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bottomSheetBehavior?.apply {
            maxWidth = calculateMenuSheetWidth()
            maxHeight = calculateMenuSheetHeight()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val menuStore by
            composableStore(MenuState(emptyList())) {
                buildMenuStore(it)
            }

        FirefoxTheme {
            BackHandler { dismissAllowingStateLoss() }

            MenuDialogBottomSheet(
                modifier =
                    Modifier.padding(vertical = FirefoxTheme.layout.space.static200)
                        .width(FirefoxTheme.layout.space.static400),
                onRequestDismiss = ::dismiss,
                menuHandleState =
                    MenuHandleState(
                        contentDescription =
                            stringResource(R.string.browser_close_main_menu_handlebar_content_description)
                    ),
                snackbarHostState = snackbarHostState,
                cornerShape =
                    MaterialTheme.shapes.extraLarge.copy(
                        bottomStart = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp),
                    ),
            ) {
                Menu(menuStore)
            }
        }
    }

    private fun calculateMenuSheetWidth(): Int {
        val isLandscape = requireContext().isLandscape()
        val screenWidthPx = requireContext().resources.configuration.screenWidthDp.dpToPx(resources.displayMetrics)
        val totalHorizontalPadding = 2 * pixelSizeFor(R.dimen.browser_menu_padding)
        val minScreenWidth = pixelSizeFor(R.dimen.browser_menu_max_width) + totalHorizontalPadding

        // We only want to restrict the width of the menu if the device is in landscape mode AND the
        // device's screen width is smaller than the menu's max width and total horizontal padding combined.
        // Otherwise, the menu being at max width would still leave sufficient padding on each side in landscape mode.
        return if (isLandscape && screenWidthPx < minScreenWidth) {
            screenWidthPx - totalHorizontalPadding
        } else {
            pixelSizeFor(R.dimen.browser_menu_max_width)
        }
    }

    private fun calculateMenuSheetHeight(): Int {
        val bottomSheet = dialog?.findViewById<View?>(materialR.id.design_bottom_sheet)
        val topBarHeight = bottomSheet?.getWindowInsets()?.top() ?: 0

        val orientationMaxHeight =
            if (requireContext().isLandscape()) {
                resources.displayMetrics.heightPixels
            } else {
                resources.displayMetrics.heightPixels - EXPANDED_OFFSET.dpToPx(resources.displayMetrics)
            }

        return orientationMaxHeight - topBarHeight
    }

    /** Pure function to get the [MenuItemProvider] for any [FenixMenuItem]. */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun buildMenuItemsProvidersResolver(): (FenixMenuItem) -> MenuItemProvider = { item ->
        when (item) {
            FenixMenuItem.CustomizeReaderView ->
                ReaderViewMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.IPProtection ->
                VpnMenuItemProvider(
                    ipProtectionStore = requireComponents.ipProtection.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.Bookmark ->
                BookmarkMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    bookmarksStorage = requireComponents.core.bookmarksStorage,
                    applicationScope = requireComponents.applicationScope,
                )

            FenixMenuItem.FindInPage -> FindInPageMenuItemProvider()

            FenixMenuItem.Quit -> menuItemProvider(
                StandardMenuItem(
                    title = Text.String(requireContext().getString(
                        R.string.browser_menu_delete_browsing_data_on_quit,
                        getString(R.string.app_name),
                    )),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_cross_circle_fill_24),
                    onClickEvent = MenuAction.DeleteBrowsingDataAndQuit,
                )
            )

            FenixMenuItem.DesktopSite ->
                DesktopSiteMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            is FenixMenuItem.More ->
                MoreMenuItemsProvider(
                    browserStore = requireComponents.core.store,
                    summarizationSettings = requireComponents.core.summarizeFeatureSettings,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.Translate ->
                TranslationsMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    translationsSettings = TranslationsEnabledSettings.dataStore(requireContext()),
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.SummarizePage ->
                SummarizePageMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    summarizationSettings = requireComponents.core.summarizeFeatureSettings,
                    eligibilityChecker = requireComponents.core.summarizationEligibilityChecker,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.Back ->
                BackMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.Forward ->
                ForwardMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.Share -> ShareMenuItemProvider()

            FenixMenuItem.Refresh ->
                RefreshMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.MoveToNormalTabs ->
                MoveToNormalTabsMenuItemProvider(browserStore = requireComponents.core.store)

            FenixMenuItem.ReportBrokenSite ->
                ReportBrokenSiteMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )

            FenixMenuItem.Shortcut ->
                ShortcutMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    pinnedSiteStorage = requireComponents.core.pinnedSiteStorage,
                    areShortcutsEnabled = requireComponents.settings.showTopSitesFeature,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )
            FenixMenuItem.AddToHomeScreen ->
                AddToHomeScreenMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    webAppUseCases = requireComponents.useCases.webAppUseCases,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )
            FenixMenuItem.SaveToCollection ->
                SaveToCollectionMenuItemProvider(
                    settings = requireComponents.settings,
                    tabCollectionStorage = requireComponents.core.tabCollectionStorage,
                )
            FenixMenuItem.OpenInApp ->
                OpenInAppMenuItemProvider(
                    browserStore = requireComponents.core.store,
                    appStore = requireComponents.appStore,
                    appLinksUseCases = requireComponents.useCases.appLinksUseCases,
                    scope = viewLifecycleOwner.lifecycle.coroutineScope,
                )
            FenixMenuItem.SaveAsPdf -> SaveAsPdfMenuItemProvider()
            FenixMenuItem.Print ->
                PrintMenuItemProvider(isAndroidAutomotiveAvailable = requireContext().isAndroidAutomotiveAvailable())
        }
    }

    private fun buildMenuStore(initialState: MenuState) =
        MenuStore(
            initialState = initialState,
            middleware =
                listOf(
                    MenuMiddleware(
                        appStore = requireComponents.appStore,
                        browserStore = requireComponents.core.store,
                        ipProtectionStore = requireComponents.ipProtection.store,
                        useCases = requireComponents.useCases,
                        browserMenuBuilder =
                            BrowserMenuBuilder(
                                providerResolver = buildMenuItemsProvidersResolver(),
                                isToolbarAtBottom = requireContext().isToolbarAtBottom(),
                                isExpandedToolbarEnabled =
                                    requireContext().components.settings.shouldUseExpandedToolbar,
                            ),
                        navController = findNavController(),
                        summarizationSettings = requireComponents.core.summarizeFeatureSettings,
                        summarizationEligibilityChecker = requireComponents.core.summarizationEligibilityChecker,
                        settings = requireComponents.settings,
                        webCompatReporterMoreInfoSender = buildWebCompatReporterMoreInfoSender(),
                        pinnedSiteStorage = requireComponents.core.pinnedSiteStorage,
                        materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext()),
                        scope = viewLifecycleOwner.lifecycle.coroutineScope,
                        applicationScope = requireComponents.applicationScope,
                    ),
                    Middleware<MenuState, mozilla.components.compose.menu.store.MenuAction> { _, next, action ->
                        next(action)
                        if (action == MenuAction.DeleteBrowsingDataAndQuit) {
                            activity?.let { activity ->
                                if (requireComponents.settings.shouldDeleteAnyDataOnQuit()) {
                                    activity.lifecycleScope.launch {
                                        deleteBrowsingDataController.clearBrowsingDataOnQuit {
                                            activity.finishAndRemoveTask()
                                        }
                                    }
                                } else {
                                    activity.finishAndRemoveTask()
                                }
                            }
                            dismiss()
                        }
                    },
                    MenuTelemetryMiddleware(accessPoint = MenuAccessPoint.Browser),
                ),
        )

    private fun menuItemProvider(item: MenuItem?) = object : MenuItemProvider {
        override val itemFlow = MutableStateFlow(item)
    }

    private fun buildWebCompatReporterMoreInfoSender() =
        DefaultWebCompatReporterMoreInfoSender(
            webCompatReporterRetrievalService =
                DefaultWebCompatReporterRetrievalService(browserStore = requireComponents.core.store)
        )
}
