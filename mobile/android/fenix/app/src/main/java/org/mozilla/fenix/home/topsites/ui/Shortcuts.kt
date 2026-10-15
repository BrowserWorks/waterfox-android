/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.fenix.compose.MenuItem
import org.mozilla.fenix.home.fake.FakeHomepagePreview
import org.mozilla.fenix.home.topsites.TOP_SITES_SPACING
import org.mozilla.fenix.home.topsites.TopSiteColors
import org.mozilla.fenix.home.topsites.TopSiteItem
import org.mozilla.fenix.home.topsites.calculateTopSitesRowLayout
import org.mozilla.fenix.home.topsites.getMenuItems
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * The grid of shortcuts shared by the homepage and the shortcuts library.
 *
 * The number of columns, the cell size and the spacing all adapt to the available width, so both surfaces lay their
 * shortcuts out identically.
 *
 * The homepage uses naturally measured rows inside the page's vertical scroll. The library, which is a screen of its
 * own, uses a scrolling lazy grid.
 *
 * @param topSites List of [TopSite] to display.
 * @param topSiteColors The color set defined by [TopSiteColors] used to style a top site.
 * @param showAddShortcut Whether to display the "Add shortcut" tile after the top sites.
 * @param scrollable Whether the grid scrolls itself. False when hosted inside another scrolling container.
 * @param menuItems The [MenuItem]s to show in a top site's dropdown menu.
 * @param onTopSiteClick Invoked when the user clicks on a top site.
 * @param onTopSiteLongClick Invoked when the user long clicks on a top site.
 * @param onTopSiteImpression Invoked when the user sees a provided top site.
 * @param onTopSitesItemBound Invoked during the composition of a top site item.
 * @param onAddShortcutClicked Invoked when the user clicks on the "Add shortcut" tile.
 * @param modifier The [Modifier] to be applied to the grid.
 */
@Composable
@Suppress("LongParameterList")
internal fun Shortcuts(
    topSites: List<TopSite>,
    topSiteColors: TopSiteColors,
    showAddShortcut: Boolean,
    scrollable: Boolean,
    menuItems: @Composable (TopSite) -> List<MenuItem>,
    onTopSiteClick: (TopSite) -> Unit,
    onTopSiteLongClick: (TopSite) -> Unit,
    onTopSiteImpression: (TopSite.Provided, Int) -> Unit,
    onTopSitesItemBound: () -> Unit,
    onAddShortcutClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemCount = topSites.size + if (showAddShortcut) 1 else 0
    val rowSpacing = FirefoxTheme.layout.space.static150

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val layout = remember(maxWidth) { calculateTopSitesRowLayout(maxWidth) }
        val rowWidth = layout.itemWidth * layout.columns + TOP_SITES_SPACING * (layout.columns - 1)
        val rowCount = (itemCount + layout.columns - 1) / layout.columns

        val shortcut: @Composable (Int) -> Unit = { index ->
            if (index < topSites.size) {
                val topSite = topSites[index]
                TopSiteItem(
                    topSite = topSite,
                    menuItems = menuItems(topSite),
                    position = index,
                    topSiteColors = topSiteColors,
                    onTopSiteClick = onTopSiteClick,
                    onTopSiteLongClick = onTopSiteLongClick,
                    onTopSiteImpression = onTopSiteImpression,
                    onTopSitesItemBound = onTopSitesItemBound,
                    itemLayout = layout.itemLayout,
                )
            } else {
                AddShortcutItem(
                    topSiteColors = topSiteColors,
                    onClick = onAddShortcutClicked,
                    itemLayout = layout.itemLayout,
                )
            }
        }

        if (scrollable) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(layout.columns),
                modifier = Modifier.width(rowWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(rowSpacing),
                horizontalArrangement = Arrangement.spacedBy(TOP_SITES_SPACING),
            ) {
                items(itemCount) { index -> shortcut(index) }
            }
        } else {
            Column(modifier = Modifier.width(rowWidth), verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
                repeat(rowCount) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TOP_SITES_SPACING),
                    ) {
                        val start = row * layout.columns
                        for (index in start until minOf(start + layout.columns, itemCount)) {
                            shortcut(index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@FlexibleWindowLightDarkPreview
private fun ShortcutsPreview(@PreviewParameter(ShortcutsPreviewParameterProvider::class) showAddShortcut: Boolean) {
    val interactor = FakeHomepagePreview.topSitesInteractor

    FirefoxTheme {
        Surface {
            Shortcuts(
                topSites = FakeHomepagePreview.topSites(),
                topSiteColors = TopSiteColors.colors(),
                showAddShortcut = showAddShortcut,
                scrollable = true,
                menuItems = { topSite ->
                    getMenuItems(
                        topSite = topSite,
                        onOpenInPrivateTabClicked = interactor::onOpenInPrivateTabClicked,
                        onEditTopSiteClicked = interactor::onEditTopSiteClicked,
                        onRemoveTopSiteClicked = interactor::onRemoveTopSiteClicked,
                        onSettingsClicked = interactor::onSettingsClicked,
                        onSponsorPrivacyClicked = interactor::onSponsorPrivacyClicked,
                    )
                },
                onTopSiteClick = {},
                onTopSiteLongClick = {},
                onTopSiteImpression = { _, _ -> },
                onTopSitesItemBound = {},
                onAddShortcutClicked = {},
                modifier = Modifier.fillMaxSize().padding(FirefoxTheme.layout.space.static200),
            )
        }
    }
}

private class ShortcutsPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}
