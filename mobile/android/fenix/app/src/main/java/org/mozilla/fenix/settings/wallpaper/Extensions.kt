/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import org.mozilla.fenix.onboarding.WallpaperOnboardingDialogFragment.Companion.SEASONAL_WALLPAPERS_COUNT
import org.mozilla.fenix.onboarding.WallpaperOnboardingDialogFragment.Companion.THUMBNAILS_SELECTION_COUNT
import org.mozilla.fenix.wallpapers.Wallpaper
import kotlin.math.max

private const val CUSTOM_WALLPAPER_ENTRY_COUNT = 1

/**
 * The extension function to group wallpapers into the appropriate collections for display.
 **/
fun List<Wallpaper>.groupByDisplayableCollection(): Map<Wallpaper.Collection, List<Wallpaper>> {
    val (customWallpapers, regularWallpapers) = partition { it.name == Wallpaper.CUSTOM }

    val groupedWallpapers = regularWallpapers
        .groupBy { wallpaper ->
            if (wallpaper.collection == Wallpaper.DefaultCollection) {
                Wallpaper.ClassicFirefoxCollection
            } else {
                wallpaper.collection
            }
        }
        .mapValues { (_, wallpapers) ->
            wallpapers.filter { wallpaper ->
                wallpaper.thumbnailFileState == Wallpaper.ImageFileState.Downloaded
            }
        }

    val classicCollection = groupedWallpapers.keys.firstOrNull {
        it.name == Wallpaper.CLASSIC_FIREFOX_COLLECTION
    } ?: Wallpaper.ClassicFirefoxCollection

    val classicFirefoxWallpapers = (
        listOf(Wallpaper.Default) +
            groupedWallpapers[classicCollection].orEmpty().filterNot { it.name == Wallpaper.DEFAULT } +
            customWallpapers
        ).distinctBy { it.name }

    return groupedWallpapers + (classicCollection to classicFirefoxWallpapers)
}

/**
 * Returns a list of wallpapers to display in the wallpaper onboarding.
 *
 * The custom wallpaper entry is added separately, so this list reserves one thumbnail slot for it.
 * Seasonal wallpapers are prioritized and any remaining slots are filled by classic wallpapers.
 */
fun List<Wallpaper>.getWallpapersForOnboarding(): List<Wallpaper> {
    val wallpaperSelectionCount = THUMBNAILS_SELECTION_COUNT - CUSTOM_WALLPAPER_ENTRY_COUNT
    val selectableWallpapers = filterNot { it.name == Wallpaper.CUSTOM }
    val (localWallpapers, remoteWallpapers) = selectableWallpapers.partition {
        it.collection.name == Wallpaper.DEFAULT
    }

    val (allClassicWallpapers, allSeasonalWallpapers) = remoteWallpapers.partition {
        it.collection.name == Wallpaper.CLASSIC_FIREFOX_COLLECTION
    }

    val seasonalWallpapersCount = max(
        SEASONAL_WALLPAPERS_COUNT,
        wallpaperSelectionCount - localWallpapers.size - allClassicWallpapers.size,
    )
    val seasonalWallpapers = allSeasonalWallpapers.take(seasonalWallpapersCount)

    val classicWallpapers = allClassicWallpapers.take(
        wallpaperSelectionCount - localWallpapers.size - seasonalWallpapers.size,
    )

    return localWallpapers + seasonalWallpapers + classicWallpapers
}
