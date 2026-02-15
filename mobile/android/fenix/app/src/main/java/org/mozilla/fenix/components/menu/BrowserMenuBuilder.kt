/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mozilla.components.compose.menu.data.ExpandableMenuItem
import mozilla.components.compose.menu.data.MenuItem as ShownMenuItem
import mozilla.components.compose.menu.data.MenuItemsGroup
import mozilla.components.compose.menu.data.StandardMenuItem
import org.mozilla.fenix.components.menu.FenixMenuItem.AddToHomeScreen
import org.mozilla.fenix.components.menu.FenixMenuItem.Back
import org.mozilla.fenix.components.menu.FenixMenuItem.Bookmark
import org.mozilla.fenix.components.menu.FenixMenuItem.CustomizeReaderView
import org.mozilla.fenix.components.menu.FenixMenuItem.DesktopSite
import org.mozilla.fenix.components.menu.FenixMenuItem.FindInPage
import org.mozilla.fenix.components.menu.FenixMenuItem.Forward
import org.mozilla.fenix.components.menu.FenixMenuItem.IPProtection
import org.mozilla.fenix.components.menu.FenixMenuItem.More
import org.mozilla.fenix.components.menu.FenixMenuItem.MoveToNormalTabs
import org.mozilla.fenix.components.menu.FenixMenuItem.OpenInApp
import org.mozilla.fenix.components.menu.FenixMenuItem.Quit
import org.mozilla.fenix.components.menu.FenixMenuItem.Print
import org.mozilla.fenix.components.menu.FenixMenuItem.Refresh
import org.mozilla.fenix.components.menu.FenixMenuItem.ReportBrokenSite
import org.mozilla.fenix.components.menu.FenixMenuItem.SaveAsPdf
import org.mozilla.fenix.components.menu.FenixMenuItem.SaveToCollection
import org.mozilla.fenix.components.menu.FenixMenuItem.Share
import org.mozilla.fenix.components.menu.FenixMenuItem.Shortcut
import org.mozilla.fenix.components.menu.FenixMenuItem.SummarizePage
import org.mozilla.fenix.components.menu.FenixMenuItem.Translate
import org.mozilla.fenix.components.menu.MenuPresentationMode.Grid
import org.mozilla.fenix.components.menu.MenuPresentationMode.Row

/**
 * Assembles the browser menu from the items each feature provides.
 *
 * This only knows which items may be shown, in what order and grouped how. What any one of them looks like is left to
 * its [MenuItemProvider].
 *
 * @param providerResolver Pure function for getting the [MenuItemProvider] for a given [FenixMenuItem].
 * @param configuration The sections of the menu, in the order they should be shown in.
 */
class BrowserMenuBuilder(
    private val providerResolver: (FenixMenuItem) -> MenuItemProvider,
    private val configuration: List<MenuSectionConfiguration>,
) {
    constructor(
        providerResolver: (FenixMenuItem) -> MenuItemProvider,
        isToolbarAtBottom: Boolean = false,
        isExpandedToolbarEnabled: Boolean = false,
    ) : this(
        providerResolver = providerResolver,
        configuration = buildDefaultConfiguration(isToolbarAtBottom, isExpandedToolbarEnabled),
    )

    private val orderedItems = configuration.flatMap { section -> section.items.flatMap { it.withSubItems() } }

    /** The menu to show, re-emitted whenever any of the items in it changes. */
    val menuStructure: Flow<List<MenuItemsGroup>> =
        orderedItems.itemsFromProviders().map { items -> configuration.toGroups(items) }.distinctUntilChanged()

    /**
     * What the feature owning each of these items currently offers for it, re-emitted whenever any one of them changes.
     * An item that should not be shown for now is offered as `null`, meaning the provider decided that its item should
     * not be shown rather than that it has not decided yet.
     */
    private fun List<FenixMenuItem>.itemsFromProviders(): Flow<Map<FenixMenuItem, ShownMenuItem?>> =
        combine(map { providerResolver(it).itemFlow }) { provided -> zip(provided).toMap() }

    /**
     * A [MenuItemsGroup] for each of these sections, laid out the way the section asks for and holding only the items
     * that are currently shown. Sections left with nothing to show are dropped.
     */
    private fun List<MenuSectionConfiguration>.toGroups(items: Map<FenixMenuItem, ShownMenuItem?>) = map { section ->
        section.toGroup(shownItems = section.items.mapNotNull { it.getMenuItemToShow(items) })
    }
        .filterNot { it.items.isEmpty() }

    /** This item and the ones it expands to, since each of them is configured by a provider of its own. */
    private fun FenixMenuItem.withSubItems(): List<FenixMenuItem> =
        when (this) {
            is FenixExpandableMenuItem -> listOf(this) + subMenuItems
            else -> listOf(this)
        }

    /**
     * Get the menu item configuration to show for this or `null` if it isn't available
     *
     * @param items A map of all the menu items wanted to be shown in the menu to the actual menu items configuration
     *   available.
     */
    private fun FenixMenuItem.getMenuItemToShow(items: Map<FenixMenuItem, ShownMenuItem?>): ShownMenuItem? {
        if (this !is FenixExpandableMenuItem) return items[this]

        val header = items[this] as? ExpandableMenuItem ?: return null
        val children = subMenuItems.mapNotNull { items[it] as? StandardMenuItem }
        if (children.isEmpty()) return null

        val provider = providerResolver(this) as? ExpandableMenuItemProvider
        return provider?.updateWithSubMenuItems(header, children)
    }

    private fun MenuSectionConfiguration.toGroup(shownItems: List<ShownMenuItem>) =
        when (presentationMode) {
            Row -> MenuItemsGroup.Row(id = id, items = shownItems, isSticky = isSticky)
            Grid -> MenuItemsGroup.Grid(id = id, items = shownItems, isSticky = isSticky)
        }

    companion object {
        @VisibleForTesting internal val BROWSER_MENU_NAVIGATION_ID = "browser_navigation"
        @VisibleForTesting internal val BROWSER_MENU_GROUP_1_ID = "browser_group_1"
        @VisibleForTesting internal val BROWSER_MENU_GROUP_2_ID = "browser_group_2"
        @VisibleForTesting internal val BROWSER_MENU_GROUP_3_ID = "browser_group_3"

        @VisibleForTesting
        internal fun buildDefaultConfiguration(
            isToolbarAtBottom: Boolean,
            isExpandedToolbarEnabled: Boolean,
        ): List<MenuSectionConfiguration> {
            val navSection =
                MenuSectionConfiguration(
                    id = BROWSER_MENU_NAVIGATION_ID,
                    presentationMode = Grid,
                    items = listOf(Back, Forward, Share, Refresh),
                    isSticky = true,
                )
            val rest =
                listOf(
                    MenuSectionConfiguration(
                        id = BROWSER_MENU_GROUP_1_ID,
                        presentationMode = Row,
                        items = listOf(CustomizeReaderView),
                    ),
                    MenuSectionConfiguration(
                        id = BROWSER_MENU_GROUP_2_ID,
                        presentationMode = Row,
                        items = listOf(IPProtection),
                    ),
                    MenuSectionConfiguration(
                        id = BROWSER_MENU_GROUP_3_ID,
                        presentationMode = Row,
                        items =
                            listOf(
                                Bookmark,
                                FindInPage,
                                DesktopSite,
                                More(
                                    listOf(
                                        Translate,
                                        SummarizePage,
                                        MoveToNormalTabs,
                                        ReportBrokenSite,
                                        Shortcut,
                                        AddToHomeScreen,
                                        SaveToCollection,
                                        OpenInApp,
                                        SaveAsPdf,
                                        Print,
                                    )
                                ),
                                Quit,
                            ),
                    ),
                )
            return if (isToolbarAtBottom || isExpandedToolbarEnabled) rest + navSection else listOf(navSection) + rest
        }

        /** The default menu structure. */
        @VisibleForTesting
        internal val DEFAULT = buildDefaultConfiguration(isToolbarAtBottom = false, isExpandedToolbarEnabled = false)
    }
}
