/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mozilla.fenix.wallpapers.Wallpaper

class ExtensionsTest {
    private val classicCollection = getSeasonalCollection("classic-firefox")

    @Test
    fun `GIVEN wallpapers that include the default WHEN grouped by collection THEN default will be added to classic firefox`() {
        val seasonalCollection = getSeasonalCollection("finally fall")
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val seasonalWallpapers = (0..5).map { generateSeasonalWallpaper("${seasonalCollection.name}$it", seasonalCollection.name) }
        val allWallpapers = listOf(Wallpaper.Default) + classicFirefoxWallpapers + seasonalWallpapers

        val result = allWallpapers.groupByDisplayableCollection()

        assertEquals(2, result.size)
        assertEquals(listOf(Wallpaper.Default) + classicFirefoxWallpapers, result[classicCollection])
        assertEquals(seasonalWallpapers, result[seasonalCollection])
    }

    @Test
    fun `GIVEN no wallpapers but the default WHEN grouped by collection THEN the default will still be present`() {
        val result = listOf(Wallpaper.Default).groupByDisplayableCollection()

        assertEquals(1, result.size)
        assertEquals(listOf(Wallpaper.Default), result[Wallpaper.ClassicFirefoxCollection])
    }

    @Test
    fun `GIVEN wallpapers with thumbnails that have not downloaded WHEN grouped by collection THEN wallpapers without thumbnails will not be included`() {
        val seasonalCollection = getSeasonalCollection("finally fall")
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val downloadedSeasonalWallpapers = (0..5).map { generateSeasonalWallpaper("${seasonalCollection.name}$it", seasonalCollection.name) }
        val nonDownloadedSeasonalWallpapers = (0..5).map {
            generateSeasonalWallpaper(
                "${seasonalCollection.name}$it",
                seasonalCollection.name,
                Wallpaper.ImageFileState.Error,
            )
        }
        val allWallpapers = listOf(Wallpaper.Default) + classicFirefoxWallpapers + downloadedSeasonalWallpapers + nonDownloadedSeasonalWallpapers

        val result = allWallpapers.groupByDisplayableCollection()

        assertEquals(2, result.size)
        assertEquals(listOf(Wallpaper.Default) + classicFirefoxWallpapers, result[classicCollection])
        assertEquals(downloadedSeasonalWallpapers, result[seasonalCollection])
    }

    @Test
    fun `GIVEN custom wallpaper WHEN grouped by collection THEN custom is in classic firefox`() {
        val seasonalCollection = getSeasonalCollection("finally fall")
        val seasonalWallpapers = (0..5).map { generateSeasonalWallpaper("${seasonalCollection.name}$it", seasonalCollection.name) }
        val allWallpapers = listOf(Wallpaper.Default, Wallpaper.Custom) + seasonalWallpapers

        val result = allWallpapers.groupByDisplayableCollection()

        assertEquals(listOf(Wallpaper.Default, Wallpaper.Custom), result[classicCollection])
        assertEquals(seasonalWallpapers, result[seasonalCollection])
    }

    @Test
    fun `GIVEN that classic firefox thumbnails fail to download WHEN grouped by collection THEN default is still available`() {
        val seasonalCollection = getSeasonalCollection("finally fall")
        val downloadedSeasonalWallpapers = (0..5).map {
            generateSeasonalWallpaper(
                "${seasonalCollection.name}$it",
                seasonalCollection.name,
            )
        }
        val allWallpapers = listOf(Wallpaper.Default) + downloadedSeasonalWallpapers

        val result = allWallpapers.groupByDisplayableCollection()

        assertEquals(2, result.size)
        assertEquals(listOf(Wallpaper.Default), result[classicCollection])
        assertEquals(downloadedSeasonalWallpapers, result[seasonalCollection])
    }

    @Test
    fun `GIVEN two collections of appropriate size WHEN fetched for onboarding THEN result reserves one custom slot`() {
        val seasonalCollectionName = "finally fall"
        val seasonalWallpapers = (0..5).map { generateSeasonalWallpaper("${seasonalCollectionName}$it", seasonalCollectionName) }
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default) + classicFirefoxWallpapers + seasonalWallpapers

        val result = allWallpapers.getWallpapersForOnboarding()
        val expected = listOf(
            Wallpaper.EdgeToEdge,
            Wallpaper.Default,
            generateSeasonalWallpaper("finally fall0", "finally fall"),
            generateSeasonalWallpaper("finally fall1", "finally fall"),
            generateSeasonalWallpaper("finally fall2", "finally fall"),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN five seasonal collections WHEN fetched for onboarding THEN result contains 3 seasonal`() {
        val seasonalCollectionAName = "finally winter"
        val seasonalWallpapers = generateSeasonalWallpaper("${seasonalCollectionAName}0", seasonalCollectionAName)
        val seasonalCollectionBName = "finally spring"
        val seasonalWallpaperB = generateSeasonalWallpaper("${seasonalCollectionBName}0", seasonalCollectionBName)
        val seasonalCollectionCName = "finally summer"
        val seasonalWallpapersC = generateSeasonalWallpaper("${seasonalCollectionCName}0", seasonalCollectionCName)
        val seasonalCollectionDName = "finally autumn"
        val seasonalWallpaperD = generateSeasonalWallpaper("${seasonalCollectionDName}0", seasonalCollectionDName)
        val seasonalCollectionEName = "finally vacation"
        val seasonalWallpapersE = generateSeasonalWallpaper("${seasonalCollectionEName}0", seasonalCollectionEName)

        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default) + classicFirefoxWallpapers + seasonalWallpapers +
            seasonalWallpaperB + seasonalWallpapersC + seasonalWallpaperD + seasonalWallpapersE

        val result = allWallpapers.getWallpapersForOnboarding()

        val expected = listOf(
            Wallpaper.EdgeToEdge,
            Wallpaper.Default,
            generateSeasonalWallpaper("finally winter0", "finally winter"),
            generateSeasonalWallpaper("finally spring0", "finally spring"),
            generateSeasonalWallpaper("finally summer0", "finally summer"),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN seasonal collection of insufficient size WHEN grouped for onboarding THEN result contains all seasonal and the rest is classic`() {
        val seasonalCollectionName = "finally fall"
        val seasonalWallpaper = generateSeasonalWallpaper("${seasonalCollectionName}0", seasonalCollectionName)
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default) + classicFirefoxWallpapers + seasonalWallpaper

        val result = allWallpapers.getWallpapersForOnboarding()

        val expected = listOf(
            Wallpaper.EdgeToEdge,
            Wallpaper.Default,
            generateSeasonalWallpaper("finally fall0", "finally fall"),
            generateClassicFirefoxWallpaper("firefox0"),
            generateClassicFirefoxWallpaper("firefox1"),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN no seasonal collection WHEN grouped for onboarding THEN result contains all classic`() {
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default) + classicFirefoxWallpapers

        val result = allWallpapers.getWallpapersForOnboarding()

        val expected = listOf(
            Wallpaper.EdgeToEdge,
            Wallpaper.Default,
            generateClassicFirefoxWallpaper("firefox0"),
            generateClassicFirefoxWallpaper("firefox1"),
            generateClassicFirefoxWallpaper("firefox2"),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN insufficient items in classic collection WHEN grouped for onboarding THEN result contains all classic`() {
        val classicFirefoxWallpapers = (0..2).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default) + classicFirefoxWallpapers

        val result = allWallpapers.getWallpapersForOnboarding()

        assertEquals(Wallpaper.EdgeToEdge, result.first())
        assertEquals(3, result.count { it.collection.name == classicCollection.name })
    }

    @Test
    fun `GIVEN no items in classic collection and some seasonal WHEN grouped for onboarding THEN result contains all seasonal`() {
        val seasonalCollectionName = "finally fall"
        val seasonalWallpapers = (0..5).map { generateSeasonalWallpaper("${seasonalCollectionName}$it", seasonalCollectionName) }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default) + seasonalWallpapers

        val result = allWallpapers.getWallpapersForOnboarding()

        val expected = listOf(
            Wallpaper.EdgeToEdge,
            Wallpaper.Default,
            generateSeasonalWallpaper("finally fall0", "finally fall"),
            generateSeasonalWallpaper("finally fall1", "finally fall"),
            generateSeasonalWallpaper("finally fall2", "finally fall"),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN no items but default and edge-to-edge WHEN grouped for onboarding THEN order is preserved from input`() {
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default)

        val result = allWallpapers.getWallpapersForOnboarding()
        val expected = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default)

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN edge-to-edge is not in available wallpapers WHEN grouped for onboarding THEN edge-to-edge is not in result`() {
        val seasonalCollectionName = "finally fall"
        val seasonalWallpapers = (0..5).map { generateSeasonalWallpaper("${seasonalCollectionName}$it", seasonalCollectionName) }
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.Default) + classicFirefoxWallpapers + seasonalWallpapers

        val expected = listOf(
            Wallpaper.Default,
            generateSeasonalWallpaper("finally fall0", "finally fall"),
            generateSeasonalWallpaper("finally fall1", "finally fall"),
            generateSeasonalWallpaper("finally fall2", "finally fall"),
            generateClassicFirefoxWallpaper("firefox0"),
        )

        val result = allWallpapers.getWallpapersForOnboarding()
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a custom wallpaper WHEN grouped for onboarding THEN it is excluded from the reserved list`() {
        val seasonalCollectionName = "finally fall"
        val seasonalWallpapers = (0..5).map {
            generateSeasonalWallpaper("${seasonalCollectionName}$it", seasonalCollectionName)
        }
        val classicFirefoxWallpapers = (0..5).map { generateClassicFirefoxWallpaper("firefox$it") }
        val allWallpapers = listOf(Wallpaper.EdgeToEdge, Wallpaper.Default, Wallpaper.Custom) +
            classicFirefoxWallpapers + seasonalWallpapers

        val result = allWallpapers.getWallpapersForOnboarding()

        assertEquals(5, result.size)
        assertFalse(result.contains(Wallpaper.Custom))
    }

    private fun generateClassicFirefoxWallpaper(name: String) = Wallpaper(
        name = name,
        textColor = 0L,
        cardColorLight = 0L,
        cardColorDark = 0L,
        thumbnailFileState = Wallpaper.ImageFileState.Downloaded,
        assetsFileState = Wallpaper.ImageFileState.Downloaded,
        collection = classicCollection,
    )

    private fun getSeasonalCollection(name: String) = Wallpaper.Collection(
        name = name,
        heading = null,
        description = null,
        learnMoreUrl = null,
        availableLocales = null,
        startDate = null,
        endDate = null,
    )

    private fun generateSeasonalWallpaper(
        wallpaperName: String,
        collectionName: String,
        thumbnailState: Wallpaper.ImageFileState = Wallpaper.ImageFileState.Downloaded,
    ) = Wallpaper(
        name = wallpaperName,
        textColor = 0L,
        cardColorLight = 0L,
        cardColorDark = 0L,
        thumbnailFileState = thumbnailState,
        assetsFileState = Wallpaper.ImageFileState.Downloaded,
        collection = getSeasonalCollection(collectionName),
    )
}
