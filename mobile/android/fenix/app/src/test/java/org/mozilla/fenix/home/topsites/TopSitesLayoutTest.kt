/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopSitesLayoutTest {

    private fun TopSitesRowLayout.rowWidth() = itemWidth.value * columns + TOP_SITES_SPACING.value * (columns - 1)

    @Test
    fun `GIVEN four columns WHEN using the default collapsed rows THEN eight shortcuts are shown`() {
        assertEquals(8, collapsedTopSitesCount(columns = 4))
    }

    @Test
    fun `GIVEN a custom row count WHEN calculating collapsed shortcuts THEN the custom row count is used`() {
        assertEquals(24, collapsedTopSitesCount(columns = 4, rows = 6))
    }

    @Test
    fun `GIVEN a very narrow screen WHEN the layout is calculated THEN four shortcuts still fit on a row`() {
        // A 240dp-wide device such as the Unihertz Jelly Star, minus the grid's horizontal margin.
        val layout = calculateTopSitesRowLayout(availableWidth = 208.dp)

        assertEquals(TOP_SITES_MIN_COLUMNS, layout.columns)
        assertEquals(46f, layout.itemWidth.value, 0.01f)
        assertTrue("The row should fit within the screen", layout.rowWidth() <= 208f)
    }

    @Test
    fun `GIVEN a shortcut too small for a full favicon WHEN the layout is calculated THEN the favicon shrinks too`() {
        val layout = calculateTopSitesRowLayout(availableWidth = 208.dp)

        assertEquals(layout.itemWidth.value, layout.faviconCardSize.value, 0.01f)
        assertTrue(layout.faviconCardSize.value < TOP_SITES_FAVICON_CARD_SIZE)
        // The image keeps its proportion of the card.
        val ratio = TOP_SITES_FAVICON_SIZE.toFloat() / TOP_SITES_FAVICON_CARD_SIZE
        assertEquals(layout.faviconCardSize.value * ratio, layout.faviconSize.value, 0.01f)
    }

    @Test
    fun `GIVEN a typical phone WHEN the layout is calculated THEN the favicon stays its normal size`() {
        val layout = calculateTopSitesRowLayout(availableWidth = 328.dp)

        assertEquals(TOP_SITES_MIN_COLUMNS, layout.columns)
        assertEquals(76f, layout.itemWidth.value, 0.01f)
        assertEquals(TOP_SITES_FAVICON_CARD_SIZE.toFloat(), layout.faviconCardSize.value, 0.01f)
        assertEquals(TOP_SITES_FAVICON_SIZE.toFloat(), layout.faviconSize.value, 0.01f)
    }

    @Test
    fun `GIVEN more width than eight shortcuts need WHEN the layout is calculated THEN the extra becomes margin`() {
        val layout = calculateTopSitesRowLayout(availableWidth = 768.dp)

        assertEquals(TOP_SITES_MAX_COLUMNS, layout.columns)
        // 768dp has room for shortcuts wider than 84dp, but they stay at 84dp.
        assertEquals(TOP_SITES_ITEM_SIZE.toFloat(), layout.itemWidth.value, 0.01f)
        assertTrue("The leftover width should be left as margin", layout.rowWidth() < 768f)
    }

    @Test
    fun `GIVEN a very wide screen WHEN the layout is calculated THEN a row holds at most eight shortcuts`() {
        val layout = calculateTopSitesRowLayout(availableWidth = 1248.dp)

        assertEquals(TOP_SITES_MAX_COLUMNS, layout.columns)
        assertEquals(TOP_SITES_ITEM_SIZE.toFloat(), layout.itemWidth.value, 0.01f)
        // The leftover width stays as margin instead of stretching the grid.
        assertTrue("The grid should not fill the whole width", layout.rowWidth() < 1248f)
    }

    @Test
    fun `GIVEN widths wider than eight shortcuts need WHEN the layout is calculated THEN they stay normal size`() {
        for (width in listOf(400, 480, 600, 768, 1024, 1248, 4000)) {
            val itemWidth = calculateTopSitesRowLayout(availableWidth = width.dp).itemWidth.value

            assertTrue(
                "a shortcut of $itemWidth is wider than normal at $width",
                itemWidth <= TOP_SITES_ITEM_SIZE + 0.01f,
            )
        }
    }

    @Test
    fun `GIVEN any screen width WHEN the layout is calculated THEN a row holds between four and eight shortcuts`() {
        for (width in listOf(120, 208, 328, 480, 768, 1248, 4000)) {
            val columns = calculateTopSitesRowLayout(availableWidth = width.dp).columns

            assertTrue(
                "a row of $columns shortcuts is out of range at $width",
                columns in TOP_SITES_MIN_COLUMNS..TOP_SITES_MAX_COLUMNS,
            )
        }
    }

    @Test
    fun `GIVEN any screen width WHEN the layout is calculated THEN the row always fits within that width`() {
        for (width in listOf(100, 120, 152, 184, 208, 240, 328, 480, 600, 768, 1024, 1248, 4000)) {
            val rowWidth = calculateTopSitesRowLayout(availableWidth = width.dp).rowWidth()

            assertTrue("a row of $rowWidth is wider than the screen at $width", rowWidth <= width + 0.01f)
        }
    }
}
