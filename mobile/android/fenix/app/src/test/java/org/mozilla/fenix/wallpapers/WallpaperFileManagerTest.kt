/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mozilla.fenix.utils.Settings
import java.io.File
import java.io.RandomAccessFile

class WallpaperFileManagerTest {
    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()
    private lateinit var wallpapersFolder: File

    private val dispatcher = StandardTestDispatcher()

    private lateinit var fileManager: WallpaperFileManager

    private lateinit var settings: Settings

    @Before
    fun setup() {
        wallpapersFolder = File(tempFolder.root, "wallpapers")
        fileManager = WallpaperFileManager(
            storageRootDirectory = tempFolder.root,
            coroutineDispatcher = dispatcher,
        )
        settings = mockk {
            every { currentWallpaperName } returns WALLPAPER_NAME
            every { currentWallpaperTextColor } returns 0L
            every { currentWallpaperCardColorLight } returns 0L
            every { currentWallpaperCardColorDark } returns 0L
        }
    }

    @Test
    fun `GIVEN wallpaper directory exists WHEN looked up THEN wallpaper created with correct name`() = runTest(dispatcher) {
        createAllFiles(WALLPAPER_NAME)

        val result = fileManager.lookupExpiredWallpaper(settings)

        val expected = generateWallpaper(name = WALLPAPER_NAME)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN portrait file missing in directories WHEN expired wallpaper looked up THEN null returned`() = runTest(dispatcher) {
        File(wallpapersFolder, "$WALLPAPER_NAME/landscape.png").apply {
            mkdirs()
            createNewFile()
        }
        File(wallpapersFolder, "$WALLPAPER_NAME/thumbnail.png").apply {
            mkdirs()
            createNewFile()
        }

        val result = fileManager.lookupExpiredWallpaper(settings)

        assertEquals(null, result)
    }

    @Test
    fun `GIVEN landscape file missing in directories WHEN expired wallpaper looked up THEN null returned`() = runTest(dispatcher) {
        File(wallpapersFolder, "$WALLPAPER_NAME/portrait.png").apply {
            mkdirs()
            createNewFile()
        }
        File(wallpapersFolder, "$WALLPAPER_NAME/thumbnail.png").apply {
            mkdirs()
            createNewFile()
        }

        val result = fileManager.lookupExpiredWallpaper(settings)

        assertEquals(null, result)
    }

    @Test
    fun `GIVEN thumbnail file missing in directories WHEN expired wallpaper looked up THEN null returned`() = runTest(dispatcher) {
        File(wallpapersFolder, "$WALLPAPER_NAME/portrait.png").apply {
            mkdirs()
            createNewFile()
        }
        File(wallpapersFolder, "$WALLPAPER_NAME/landscape.png").apply {
            mkdirs()
            createNewFile()
        }

        val result = fileManager.lookupExpiredWallpaper(settings)

        assertEquals(null, result)
    }

    @Test
    fun `WHEN cleaned THEN current wallpaper and available wallpapers kept`() = runTest(dispatcher) {
        val currentName = "current"
        val currentWallpaper = generateWallpaper(name = currentName)
        val availableName = "available"
        val available = generateWallpaper(name = availableName)
        val unavailableName = "unavailable"
        createAllFiles(currentName)
        createAllFiles(availableName)
        createAllFiles(unavailableName)

        fileManager.clean(currentWallpaper, listOf(available))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(getAllFiles(currentName).all { it.exists() })
        assertTrue(getAllFiles(availableName).all { it.exists() })
        assertTrue(getAllFiles(unavailableName).none { it.exists() })
    }

    @Test
    fun `WHEN both wallpaper assets exist THEN the file lookup will succeed`() = runTest(dispatcher) {
        val wallpaper = generateWallpaper("name")
        createAllFiles(wallpaper.name)

        val result = fileManager.wallpaperImagesExist(wallpaper)

        assertTrue(result)
    }

    @Test
    fun `WHEN at least one wallpaper asset does not exist THEN the file lookup will fail`() = runTest(dispatcher) {
        val wallpaper = generateWallpaper("name")
        val allFiles = getAllFiles(wallpaper.name)
        (0 until (allFiles.size - 1)).forEach {
            allFiles[it].mkdirs()
            allFiles[it].createNewFile()
        }

        val result = fileManager.wallpaperImagesExist(wallpaper)

        assertFalse(result)
    }

    @Test
    fun `WHEN copying a custom wallpaper image THEN the destination contains the content URI data`() = runTest(dispatcher) {
        val sourceFile = tempFolder.newFile("custom-source.png").apply { writeText("custom") }
        val sourceUri = mockk<Uri>()
        val context = mockContextFor(sourceUri, sourceFile)

        val result = fileManager.copyCustomWallpaperImage(
            context,
            Wallpaper.ImageType.Portrait,
            sourceUri,
        )

        assertTrue(result)
        assertEquals("custom", getCustomWallpaperFile(Wallpaper.ImageType.Portrait).readText())
    }

    @Test
    fun `GIVEN existing custom wallpaper file WHEN copying from the same URI THEN the file is preserved`() = runTest(dispatcher) {
        val existingFile = getCustomWallpaperFile(Wallpaper.ImageType.Portrait).apply {
            parentFile?.mkdirs()
            writeText("existing")
        }
        val existingUri = mockk<Uri>()
        val context = mockContextFor(existingUri, existingFile)

        val result = fileManager.copyCustomWallpaperImage(
            context,
            Wallpaper.ImageType.Portrait,
            existingUri,
        )

        assertTrue(result)
        assertEquals("existing", existingFile.readText())
    }

    @Test
    fun `GIVEN content resolver returns no stream WHEN copying custom wallpaper image THEN false is returned`() = runTest(dispatcher) {
        val sourceUri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver> {
            every { openInputStream(sourceUri) } returns null
        }
        val context = mockk<Context> {
            every { getContentResolver() } returns contentResolver
        }

        val result = fileManager.copyCustomWallpaperImage(
            context,
            Wallpaper.ImageType.Portrait,
            sourceUri,
        )

        assertFalse(result)
        assertFalse(getCustomWallpaperFile(Wallpaper.ImageType.Portrait).exists())
    }

    @Test
    fun `GIVEN an oversized stream WHEN copying custom wallpaper image THEN the existing file is preserved`() = runTest(dispatcher) {
        val sourceFile = tempFolder.newFile("oversized-source.png")
        RandomAccessFile(sourceFile, "rw").use {
            it.setLength(WallpaperFileManager.MAX_CUSTOM_WALLPAPER_FILE_SIZE_BYTES + 1)
        }
        val sourceUri = mockk<Uri>()
        val context = mockContextFor(sourceUri, sourceFile)
        val existingFile = getCustomWallpaperFile(Wallpaper.ImageType.Portrait).apply {
            parentFile?.mkdirs()
            writeText("existing")
        }

        val result = fileManager.copyCustomWallpaperImage(
            context,
            Wallpaper.ImageType.Portrait,
            sourceUri,
        )

        assertFalse(result)
        assertEquals("existing", existingFile.readText())
        assertFalse(File("${existingFile.path}.tmp").exists())
    }

    private fun createAllFiles(name: String) {
        for (file in getAllFiles(name)) {
            file.mkdirs()
            file.createNewFile()
        }
    }

    private fun getAllFiles(name: String): List<File> {
        val folder = File(wallpapersFolder, name)
        return listOf(
            folder,
            File(folder, "portrait.png"),
            File(folder, "landscape.png"),
            File(folder, "thumbnail.png"),
        )
    }

    private fun getCustomWallpaperFile(imageType: Wallpaper.ImageType): File = File(
        tempFolder.root,
        Wallpaper.getLocalPath(Wallpaper.CUSTOM, imageType),
    )

    private fun mockContextFor(uri: Uri, file: File): Context {
        val contentResolver = mockk<ContentResolver> {
            every { openInputStream(uri) } answers { file.inputStream() }
        }
        return mockk {
            every { getContentResolver() } returns contentResolver
        }
    }

    private fun generateWallpaper(name: String) = Wallpaper(
        name = name,
        textColor = 0L,
        cardColorLight = 0L,
        cardColorDark = 0L,
        thumbnailFileState = Wallpaper.ImageFileState.Downloaded,
        assetsFileState = Wallpaper.ImageFileState.Downloaded,
        collection = Wallpaper.DefaultCollection,
    )

    private companion object {
        const val WALLPAPER_NAME = "name"
    }
}
