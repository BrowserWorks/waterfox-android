/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum number of shortcut columns to show. */
internal const val TOP_SITES_MIN_COLUMNS = 4

/** Maximum number of shortcut columns to show. */
internal const val TOP_SITES_MAX_COLUMNS = 8

/** Default rows of shortcuts the homepage shows before the grid is expanded. */
internal const val TOP_SITES_COLLAPSED_ROWS = 2

/** Number of shortcuts in [rows] rows of [columns] shortcuts. */
internal fun collapsedTopSitesCount(columns: Int, rows: Int = TOP_SITES_COLLAPSED_ROWS) = columns * rows

/** Fixed gap between adjacent shortcuts. */
internal val TOP_SITES_SPACING = 8.dp

/** Spacer a shortcut places above its favicon card. */
internal val TOP_SITES_ITEM_TOP_SPACING = 4.dp

/** Spacer a shortcut places between its favicon card and its title. */
internal val TOP_SITES_ITEM_BOTTOM_SPACING = 6.dp

/** Ratio of the favicon image to its containing card, preserved when the card is scaled down. */
private const val TOP_SITES_FAVICON_RATIO = TOP_SITES_FAVICON_SIZE.toFloat() / TOP_SITES_FAVICON_CARD_SIZE

/**
 * The sizes a single shortcut is laid out with.
 *
 * @property itemWidth Width of the shortcut cell, which also bounds its title.
 * @property faviconCardSize Diameter of the circular card behind the favicon.
 * @property faviconSize Size of the favicon image inside the card.
 */
data class TopSiteItemLayout(
    val itemWidth: Dp,
    val faviconCardSize: Dp,
    val faviconSize: Dp,
) {
    companion object {
        /** Builder function used to construct an instance of [TopSiteItemLayout]. */
        fun sizes(
            itemWidth: Dp = TOP_SITES_ITEM_SIZE.dp,
            faviconCardSize: Dp = TOP_SITES_FAVICON_CARD_SIZE.dp,
            faviconSize: Dp = TOP_SITES_FAVICON_SIZE.dp,
        ) = TopSiteItemLayout(itemWidth = itemWidth, faviconCardSize = faviconCardSize, faviconSize = faviconSize)
    }
}

/**
 * The resolved layout for a row of shortcuts, computed from the available width.
 *
 * @property columns Number of shortcuts per row, between [TOP_SITES_MIN_COLUMNS] and [TOP_SITES_MAX_COLUMNS].
 * @property itemWidth Width of a single shortcut cell.
 * @property faviconCardSize Size of the favicon card, reduced from [TOP_SITES_FAVICON_CARD_SIZE] only when the cell is
 *   too small to contain it.
 * @property faviconSize Size of the favicon image inside the card.
 */
internal data class TopSitesRowLayout(
    val columns: Int,
    val itemWidth: Dp,
    val faviconCardSize: Dp,
    val faviconSize: Dp,
) {
    /** The sizes to lay each shortcut in this row out with. */
    val itemLayout: TopSiteItemLayout
        get() = TopSiteItemLayout(itemWidth = itemWidth, faviconCardSize = faviconCardSize, faviconSize = faviconSize)
}

/**
 * Resolves the number of columns, cell width and favicon size for a row of shortcuts given the [availableWidth], which
 * excludes any margin already reserved by the caller.
 *
 * The column count scales with the width between [TOP_SITES_MIN_COLUMNS] and [TOP_SITES_MAX_COLUMNS]. Spacing stays at
 * [TOP_SITES_SPACING], so to fit those columns the cell shrinks from [TOP_SITES_ITEM_SIZE], and on very narrow screens
 * the favicon shrinks with it. Fitting always wins: the cell keeps shrinking rather than letting the row overflow. Any
 * surplus width is left as margin, so the grid does not fill the whole width.
 */
internal fun calculateTopSitesRowLayout(availableWidth: Dp): TopSitesRowLayout {
    val maxItemSize = TOP_SITES_ITEM_SIZE.dp
    val columns = (availableWidth / maxItemSize).toInt().coerceIn(TOP_SITES_MIN_COLUMNS, TOP_SITES_MAX_COLUMNS)
    val gaps = columns - 1

    // The widest cell that still leaves room for the spacing, capped at the default size. Deliberately has no lower
    // bound: any floor large enough to matter would push the row past [availableWidth] and clip it.
    val itemWidth = ((availableWidth - TOP_SITES_SPACING * gaps) / columns).coerceIn(0.dp, maxItemSize)
    val faviconCardSize = itemWidth.coerceAtMost(TOP_SITES_FAVICON_CARD_SIZE.dp)

    return TopSitesRowLayout(
        columns = columns,
        itemWidth = itemWidth,
        faviconCardSize = faviconCardSize,
        faviconSize = faviconCardSize * TOP_SITES_FAVICON_RATIO,
    )
}
