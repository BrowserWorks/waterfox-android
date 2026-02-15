/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper

@RunWith(AndroidJUnit4::class)
class WallpaperSettingsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // FirefoxTheme resolves the theme through FenixApplication, which reads the AppStore.
        every { testContext.components.appStore } returns AppStore()
    }

    @Test
    fun `GIVEN a loaded thumbnail WHEN its place in the grid is taken by another wallpaper THEN the old bitmap is not drawn for it`() {
        val loaded = wallpaper("loaded")
        val pending = wallpaper("pending")
        val pendingLoad = CompletableDeferred<Unit>()
        val wallpapers = mutableStateOf(listOf(loaded))

        composeTestRule.setContent {
            FirefoxTheme {
                WallpaperThumbnails(
                    wallpapers = wallpapers.value,
                    selectedWallpaper = loaded,
                    loadWallpaperResource = { wallpaper, _ ->
                        if (wallpaper.name == pending.name) {
                            pendingLoad.await()
                        }
                        createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
                    },
                    onSelectWallpaper = {},
                    onSetCustomWallpaper = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(1, thumbnailsFor(loaded))

        // The grid is not keyed, so the item that drew the first wallpaper is reused for the second.
        wallpapers.value = listOf(pending)
        composeTestRule.waitForIdle()
        assertEquals(0, thumbnailsFor(pending))

        pendingLoad.complete(Unit)
        composeTestRule.waitForIdle()
        assertEquals(1, thumbnailsFor(pending))
    }

    private fun thumbnailsFor(wallpaper: Wallpaper): Int =
        nodesWithTag("${WallpaperSettingsTestTag.WALLPAPER_THUMBNAIL_ITEM}.${wallpaper.name}") +
            nodesWithTag("${WallpaperSettingsTestTag.WALLPAPER_THUMBNAIL_SELECTED}.${wallpaper.name}")

    private fun nodesWithTag(tag: String): Int = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun wallpaper(name: String) =
        Wallpaper(
            name = name,
            collection = Wallpaper.ClassicFirefoxCollection,
            textColor = null,
            cardColorLight = null,
            cardColorDark = null,
            thumbnailFileState = Wallpaper.ImageFileState.Downloaded,
            assetsFileState = Wallpaper.ImageFileState.Downloaded,
        )

    companion object {
        private const val BITMAP_SIZE = 4
    }
}
