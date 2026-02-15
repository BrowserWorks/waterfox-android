/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperBitmapUtilsTest {
    @Test
    fun `GIVEN wallpaper bitmap within bounds WHEN calculating sample size THEN original size is used`() {
        val sampleSize = WallpaperBitmapUtils.calculateInSampleSize(
            width = 4000,
            height = 4000,
            maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_PIXELS,
            maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_DIMENSION,
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun `GIVEN wallpaper bitmap above bounds WHEN calculating sample size THEN sampled bitmap fits bounds`() {
        val width = 8000
        val height = 8000

        val sampleSize = WallpaperBitmapUtils.calculateInSampleSize(
            width = width,
            height = height,
            maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_PIXELS,
            maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_DIMENSION,
        )

        assertEquals(2, sampleSize)
        assertTrue(width / sampleSize <= WallpaperBitmapUtils.MAX_WALLPAPER_DIMENSION)
        assertTrue(height / sampleSize <= WallpaperBitmapUtils.MAX_WALLPAPER_DIMENSION)
        assertTrue(
            (width / sampleSize).toLong() * (height / sampleSize) <=
                WallpaperBitmapUtils.MAX_WALLPAPER_PIXELS,
        )
    }

    @Test
    fun `GIVEN thumbnail bitmap above bounds WHEN calculating sample size THEN sampled thumbnail fits bounds`() {
        val width = 4000
        val height = 3000

        val sampleSize = WallpaperBitmapUtils.calculateInSampleSize(
            width = width,
            height = height,
            maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_PIXELS,
            maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION,
        )

        assertEquals(8, sampleSize)
        assertTrue(width / sampleSize <= WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION)
        assertTrue(height / sampleSize <= WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION)
        assertTrue(
            (width / sampleSize).toLong() * (height / sampleSize) <=
                WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_PIXELS,
        )
    }

    @Test
    fun `GIVEN invalid bitmap bounds WHEN calculating sample size THEN original size is used`() {
        val sampleSize = WallpaperBitmapUtils.calculateInSampleSize(
            width = 0,
            height = 0,
            maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_PIXELS,
            maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_DIMENSION,
        )

        assertEquals(1, sampleSize)
    }
}
