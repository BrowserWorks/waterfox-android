/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.utils.Settings
import java.io.File
import java.util.Date

/**
 * Contains use cases related to the wallpaper feature.
 *
 * @param settings Used for retrieving and updating wallpaper-related settings like the currently selected wallpaper.
 * @param filesDir Used for loading wallpaper images from disk.
 * @param appStore Will receive dispatches of metadata updates like the currently selected wallpaper.
 * @param client Handles downloading wallpapers and their metadata.
 * @param storageRootDirectory The top level app-local storage directory.
 * @param currentLocale The locale currently being used on the device.
 * @param context Used to copy custom wallpaper images from content URIs.
 */
@Suppress("LongParameterList")
class WallpapersUseCases(
    settings: Settings,
    filesDir: File,
    appStore: AppStore,
    client: Client,
    storageRootDirectory: File,
    currentLocale: String,
    context: Context,
) {
    private val downloader = WallpaperDownloader(
        storageRootDirectory = storageRootDirectory,
        client = client,
        assetManager = context.assets,
    )
    private val fileManager = WallpaperFileManager(storageRootDirectory)

    val fetchCurrentWallpaperUseCase: FetchCurrentWallpaperUseCase by lazy {
        DefaultFetchCurrentWallpaperUseCase(settings, appStore, fileManager)
    }

    // Use case for initializing wallpaper feature. Should usually be called early
    // in the app's lifetime to ensure that any potential long-running tasks can complete quickly.
    val initialize: InitializeWallpapersUseCase by lazy {
        val metadataFetcher = WallpaperMetadataFetcher(client, context.assets)
        val migrationHelper = LegacyWallpaperMigration(
            storageRootDirectory = storageRootDirectory,
            settings = settings,
            selectWallpaper::invoke,
        )
        DefaultInitializeWallpaperUseCase(
            appStore = appStore,
            downloader = downloader,
            fileManager = fileManager,
            metadataFetcher = metadataFetcher,
            migrationHelper = migrationHelper,
            settings = settings,
            currentLocale = currentLocale,
        )
    }

    // Use case for loading specific wallpaper bitmaps.
    val loadBitmap: LoadBitmapUseCase by lazy {
        DefaultLoadBitmapUseCase(
            getFilesDir = { filesDir },
        )
    }

    val loadThumbnail: LoadThumbnailUseCase by lazy {
        DefaultLoadThumbnailUseCase(storageRootDirectory)
    }

    // Use case for selecting a new wallpaper.
    val selectWallpaper: SelectWallpaperUseCase by lazy {
        DefaultSelectWallpaperUseCase(settings, appStore, fileManager, downloader)
    }

    val setCustomWallpaper: SetCustomWallpaperUseCase by lazy {
        DefaultSetCustomWallpaperUseCase(
            context = context,
            fileManager = fileManager,
            appStore = appStore,
            settings = settings,
        )
    }

    /**
     * Contract for use cases that retrieve the user's currently selected wallpaper.
     */
    interface FetchCurrentWallpaperUseCase {
        /**
         * Start operation to retrieve user's currently selected wallpaper.
         */
        suspend operator fun invoke()
    }

    internal class DefaultFetchCurrentWallpaperUseCase(
        private val settings: Settings,
        private val appStore: AppStore,
        private val fileManager: WallpaperFileManager,
    ) : FetchCurrentWallpaperUseCase {
        override suspend fun invoke() {
            val currentWallpaper = when (settings.currentWallpaperName) {
                Wallpaper.CUSTOM -> if (fileManager.customWallpaperExists()) {
                    Wallpaper.Custom
                } else {
                    null
                }
                else -> Wallpaper.getCurrentWallpaperFromSettings(settings)
            }
            currentWallpaper?.let {
                appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(it))
            }
        }
    }

    /**
     * Contract for usecases that initialize the wallpaper feature.
     */
    interface InitializeWallpapersUseCase {
        /**
         * Start operations that should be down during initialization, like remote metadata
         * retrieval and determining the currently selected wallpaper.
         */
        suspend operator fun invoke()
    }

    @Suppress("LongParameterList")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultInitializeWallpaperUseCase(
        private val appStore: AppStore,
        private val downloader: WallpaperDownloader,
        private val fileManager: WallpaperFileManager,
        private val metadataFetcher: WallpaperMetadataFetcher,
        private val migrationHelper: LegacyWallpaperMigration,
        private val settings: Settings,
        private val currentLocale: String,
    ) : InitializeWallpapersUseCase {
        override suspend fun invoke() {
            val currentWallpaperName = if (settings.shouldMigrateLegacyWallpaper) {
                val migratedWallpaperName =
                    migrationHelper.migrateLegacyWallpaper(settings.currentWallpaperName)
                settings.currentWallpaperName = migratedWallpaperName
                settings.shouldMigrateLegacyWallpaper = false
                migratedWallpaperName
            } else {
                settings.currentWallpaperName
            }

            if (settings.shouldMigrateLegacyWallpaperCardColors) {
                migrationHelper.migrateExpiredWallpaperCardColors()
            }

            val possibleWallpapers = metadataFetcher.downloadWallpaperList().filter {
                !it.isExpired() && it.isAvailableInLocale()
            }
            val customIncluded = if (fileManager.customWallpaperExists()) {
                listOf(Wallpaper.Custom)
            } else {
                emptyList()
            }
            val currentWallpaper = when (currentWallpaperName) {
                Wallpaper.CUSTOM -> if (customIncluded.isNotEmpty()) {
                    Wallpaper.Custom
                } else {
                    Wallpaper.Default
                }
                else -> possibleWallpapers.find { it.name == currentWallpaperName }
                    ?: fileManager.lookupExpiredWallpaper(settings)
                    ?: Wallpaper.getCurrentWallpaperFromSettings(settings)
                    ?: Wallpaper.Default
            }

            // Dispatching this early will make it accessible to the home screen ASAP. This may have
            // been dispatched by FetchCurrentWallpaperUseCase, but this could include additional metadata.
            appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(currentWallpaper))

            fileManager.clean(
                currentWallpaper,
                possibleWallpapers + customIncluded,
            )

            val wallpapersWithUpdatedThumbnailState = possibleWallpapers.map { wallpaper ->
                val result = downloader.downloadThumbnail(wallpaper)
                wallpaper.copy(thumbnailFileState = result)
            }

            val defaultIncluded = defaultWallpapers + wallpapersWithUpdatedThumbnailState + customIncluded
            appStore.dispatch(AppAction.WallpaperAction.UpdateAvailableWallpapers(defaultIncluded))
        }

        private val defaultWallpapers: List<Wallpaper> =
            if (settings.enableHomepageEdgeToEdgeBackgroundFeature) {
                listOf(Wallpaper.EdgeToEdge, Wallpaper.Default)
            } else {
                listOf(Wallpaper.Default)
            }
        private fun Wallpaper.isExpired(): Boolean = when (this) {
            Wallpaper.Default -> false
            else -> {
                val expired = this.collection.endDate?.let { Date().after(it) } ?: false
                expired && this.name != settings.currentWallpaperName
            }
        }

        private fun Wallpaper.isAvailableInLocale(): Boolean =
            this.collection.availableLocales?.contains(currentLocale) ?: true
    }

    /**
     * Contract for usecase for loading bitmaps related to a specific wallpaper.
     */
    interface LoadBitmapUseCase {
        /**
         * Load the bitmap for a [wallpaper], if available.
         *
         * @param wallpaper The wallpaper to load a bitmap for.
         * @param orientation The orientation of wallpaper.
         */
        suspend operator fun invoke(wallpaper: Wallpaper, orientation: Int): Bitmap?
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultLoadBitmapUseCase(
        private val getFilesDir: suspend () -> File,
    ) : LoadBitmapUseCase {
        override suspend fun invoke(wallpaper: Wallpaper, orientation: Int): Bitmap? =
            loadWallpaperFromDisk(wallpaper, orientation)

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal suspend fun loadWallpaperFromDisk(
            wallpaper: Wallpaper,
            orientation: Int,
        ): Bitmap? = try {
            withContext(Dispatchers.IO) {
                val filesDir = getFilesDir()
                val file = File(filesDir, wallpaper.getLocalPathFromContext(orientation))
                val fallbackFile = wallpaper.getFallbackFile(filesDir, orientation)
                val fileToLoad = listOfNotNull(file, fallbackFile).firstOrNull { it.exists() }

                fileToLoad?.let { file ->
                    WallpaperBitmapUtils.decodeSampledBitmapFromFile(file)
                }
            }
        } catch (e: CancellationException) {
            // CancellationException must not be swallowed: if the coroutine was canceled while loading,
            // rethrowing ensures the cancellation propagates and callers won't treat a null result as a
            // load failure.
            throw e
        } catch (_: Exception) {
            null
        }

        /**
         * Get the expected local path on disk for a wallpaper. This will differ depending
         * on orientation and app theme.
         */
        private fun Wallpaper.getLocalPathFromContext(orientation: Int): String =
            Wallpaper.getLocalPath(name, orientation.getImageType())

        private fun Wallpaper.getFallbackFile(filesDir: File, orientation: Int): File? {
            if (name != Wallpaper.CUSTOM) {
                return null
            }

            val fallbackType = if (orientation.getImageType() == Wallpaper.ImageType.Landscape) {
                Wallpaper.ImageType.Portrait
            } else {
                Wallpaper.ImageType.Landscape
            }
            return File(filesDir, Wallpaper.getLocalPath(name, fallbackType))
        }

        private fun Int.getImageType(): Wallpaper.ImageType = if (this == Configuration.ORIENTATION_LANDSCAPE) {
            Wallpaper.ImageType.Landscape
        } else {
            Wallpaper.ImageType.Portrait
        }
    }

    /**
     * Contract for usecase for loading thumbnail bitmaps related to a specific wallpaper.
     */
    interface LoadThumbnailUseCase {
        /**
         * Load the bitmap for a [wallpaper] thumbnail, if available.
         *
         * @param wallpaper The wallpaper to load a thumbnail for.
         */
        suspend operator fun invoke(wallpaper: Wallpaper): Bitmap?
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultLoadThumbnailUseCase(private val filesDir: File) : LoadThumbnailUseCase {
        override suspend fun invoke(wallpaper: Wallpaper): Bitmap? = withContext(Dispatchers.IO) {
            if (wallpaper.name == Wallpaper.CUSTOM) {
                return@withContext Result.runCatching {
                    val portraitFile = File(
                        filesDir,
                        Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Portrait),
                    )
                    val landscapeFile = File(
                        filesDir,
                        Wallpaper.getLocalPath(Wallpaper.CUSTOM, Wallpaper.ImageType.Landscape),
                    )

                    val fileToLoad = when {
                        portraitFile.exists() -> portraitFile
                        landscapeFile.exists() -> landscapeFile
                        else -> null
                    }

                    fileToLoad?.let { file ->
                        WallpaperBitmapUtils.decodeSampledBitmapFromFile(
                            file = file,
                            maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_PIXELS,
                            maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION,
                        )
                    }
                }.getOrNull()
            }

            Result.runCatching {
                val path = Wallpaper.getLocalPath(wallpaper.name, Wallpaper.ImageType.Thumbnail)
                withContext(Dispatchers.IO) {
                    val file = File(filesDir, path)
                    WallpaperBitmapUtils.decodeSampledBitmapFromFile(
                        file = file,
                        maxPixels = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_PIXELS,
                        maxDimension = WallpaperBitmapUtils.MAX_WALLPAPER_THUMBNAIL_DIMENSION,
                    )
                }
            }.getOrNull()
        }
    }

    /**
     * Contract for usecase of selecting a new wallpaper.
     */
    interface SelectWallpaperUseCase {
        /**
         * Select a new wallpaper.
         *
         * @param wallpaper The selected wallpaper.
         */
        suspend operator fun invoke(wallpaper: Wallpaper): Wallpaper.ImageFileState
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultSelectWallpaperUseCase(
        private val settings: Settings,
        private val appStore: AppStore,
        private val fileManager: WallpaperFileManager,
        private val downloader: WallpaperDownloader,
    ) : SelectWallpaperUseCase {
        /**
         * Select a new wallpaper. Storage and the app store will be updated appropriately.
         *
         * @param wallpaper The selected wallpaper.
         */
        override suspend fun invoke(wallpaper: Wallpaper): Wallpaper.ImageFileState {
            return if (wallpaper.collection == Wallpaper.DefaultCollection ||
                fileManager.wallpaperImagesExist(wallpaper)
            ) {
                selectWallpaper(wallpaper)
                dispatchDownloadState(wallpaper, Wallpaper.ImageFileState.Downloaded)
                Wallpaper.ImageFileState.Downloaded
            } else {
                dispatchDownloadState(wallpaper, Wallpaper.ImageFileState.Downloading)
                val result = downloader.downloadWallpaper(wallpaper)
                dispatchDownloadState(wallpaper, result)
                if (result == Wallpaper.ImageFileState.Downloaded) {
                    selectWallpaper(wallpaper)
                }
                result
            }
        }

        @VisibleForTesting
        internal fun selectWallpaper(wallpaper: Wallpaper) {
            settings.currentWallpaperName = wallpaper.name
            settings.currentWallpaperTextColor = wallpaper.textColor ?: 0L
            settings.currentWallpaperCardColorLight = wallpaper.cardColorLight ?: 0L
            settings.currentWallpaperCardColorDark = wallpaper.cardColorDark ?: 0L
            appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(wallpaper))
        }

        private fun dispatchDownloadState(wallpaper: Wallpaper, downloadState: Wallpaper.ImageFileState) {
            appStore.dispatch(AppAction.WallpaperAction.UpdateWallpaperDownloadState(wallpaper, downloadState))
        }
    }

    /**
     * Contract for usecase of setting a custom wallpaper.
     */
    interface SetCustomWallpaperUseCase {
        /**
         * Set a custom wallpaper from URIs.
         *
         * @param portraitUri The portrait orientation image URI.
         * @param landscapeUri The landscape orientation image URI.
         * @param useSingleImage Whether to use only the portrait image for both orientations.
         */
        suspend operator fun invoke(portraitUri: Uri?, landscapeUri: Uri?, useSingleImage: Boolean): Boolean
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal class DefaultSetCustomWallpaperUseCase(
        private val context: Context,
        private val fileManager: WallpaperFileManager,
        private val appStore: AppStore,
        private val settings: Settings,
    ) : SetCustomWallpaperUseCase {
        override suspend fun invoke(portraitUri: Uri?, landscapeUri: Uri?, useSingleImage: Boolean): Boolean {
            val portraitSourceUri = portraitUri ?: landscapeUri
            val landscapeSourceUri = if (useSingleImage) {
                portraitSourceUri
            } else {
                landscapeUri ?: portraitUri
            }

            if (portraitSourceUri == null || landscapeSourceUri == null) {
                return false
            }

            val success = fileManager.copyCustomWallpaperImage(
                context,
                Wallpaper.ImageType.Portrait,
                portraitSourceUri,
            ) && fileManager.copyCustomWallpaperImage(
                context,
                Wallpaper.ImageType.Landscape,
                landscapeSourceUri,
            )

            if (success) {
                settings.currentWallpaperName = Wallpaper.CUSTOM
                settings.currentWallpaperTextColor = 0L
                settings.currentWallpaperCardColorLight = 0L
                settings.currentWallpaperCardColorDark = 0L
                settings.customWallpaperUseSingleImage = useSingleImage
                appStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(Wallpaper.Custom))

                val currentWallpapers = appStore.state.wallpaperState.availableWallpapers
                if (currentWallpapers.none { it.name == Wallpaper.CUSTOM }) {
                    appStore.dispatch(
                        AppAction.WallpaperAction.UpdateAvailableWallpapers(
                            currentWallpapers + Wallpaper.Custom,
                        ),
                    )
                }
            }

            return success
        }
    }
}
