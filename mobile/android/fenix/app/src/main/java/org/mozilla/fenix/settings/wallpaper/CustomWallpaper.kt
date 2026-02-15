/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.theme.AcornCorners
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.SwitchListItem
import org.mozilla.fenix.theme.FirefoxTheme

@Composable
internal fun CustomWallpaper(
    currentPortraitImageUri: Uri?,
    currentLandscapeImageUri: Uri?,
    initialUseSingleImage: Boolean = false,
    onSaveClick: (Uri?, Uri?, Boolean) -> Unit,
) {
    var portraitImageUri by remember {
        mutableStateOf(
            currentPortraitImageUri ?: if (initialUseSingleImage) currentLandscapeImageUri else null,
        )
    }
    var landscapeImageUri by remember {
        mutableStateOf(currentLandscapeImageUri)
    }
    var useSingleImage by rememberSaveable {
        mutableStateOf(initialUseSingleImage)
    }

    val portraitImageUriLauncher = rememberLauncherForActivityResult(PickVisualMedia()) {
        portraitImageUri = it
        if (useSingleImage) {
            landscapeImageUri = it
        }
    }
    val landscapeImageUriLauncher = rememberLauncherForActivityResult(PickVisualMedia()) {
        landscapeImageUri = it
    }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FirefoxTheme.layout.space.dynamic400),
        ) {
            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))
            SectionTitle(text = stringResource(id = R.string.wallpaper_custom_images_header))
            ImageSelectors(
                portraitImageUri = portraitImageUri,
                landscapeImageUri = landscapeImageUri,
                useSingleImage = useSingleImage,
                onPortraitImageClick = {
                    portraitImageUriLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                onLandscapeImageClick = {
                    landscapeImageUriLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
            )
            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic200))
            SingleImageSwitch(
                useSingleImage = useSingleImage,
                onUseSingleImageChange = { checked ->
                    useSingleImage = checked
                    val (portraitUri, landscapeUri) = resolveImageSelection(
                        useSingleImage = checked,
                        portraitImageUri = portraitImageUri,
                        landscapeImageUri = landscapeImageUri,
                    )
                    portraitImageUri = portraitUri
                    landscapeImageUri = landscapeUri
                },
            )
            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))
            SaveCustomWallpaperButton(
                useSingleImage = useSingleImage,
                portraitImageUri = portraitImageUri,
                landscapeImageUri = landscapeImageUri,
                onSaveClick = onSaveClick,
            )
            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))
        }
    }
}

@Composable
private fun ImageSelectors(
    portraitImageUri: Uri?,
    landscapeImageUri: Uri?,
    useSingleImage: Boolean,
    onPortraitImageClick: () -> Unit,
    onLandscapeImageClick: () -> Unit,
) {
    ImageSelectionRow(
        imageUri = portraitImageUri,
        title = stringResource(id = R.string.wallpaper_portrait_image),
        modifier = Modifier.fillMaxWidth(),
        action = onPortraitImageClick,
    )

    if (!useSingleImage) {
        ImageSelectionRow(
            imageUri = landscapeImageUri,
            title = stringResource(id = R.string.wallpaper_landscape_image),
            modifier = Modifier.fillMaxWidth(),
            action = onLandscapeImageClick,
        )
    }
}

@Composable
private fun SingleImageSwitch(
    useSingleImage: Boolean,
    onUseSingleImageChange: (Boolean) -> Unit,
) {
    SwitchListItem(
        label = stringResource(id = R.string.wallpaper_use_single_image),
        description = stringResource(id = R.string.wallpaper_use_single_image_summary),
        checked = useSingleImage,
        showSwitchAfter = true,
        onClick = onUseSingleImageChange,
    )
}

private fun resolveImageSelection(
    useSingleImage: Boolean,
    portraitImageUri: Uri?,
    landscapeImageUri: Uri?,
): Pair<Uri?, Uri?> = if (useSingleImage) {
    val singleImageUri = portraitImageUri ?: landscapeImageUri
    singleImageUri to singleImageUri
} else {
    val resolvedLandscapeUri = if (portraitImageUri != null && landscapeImageUri == portraitImageUri) {
        null
    } else {
        landscapeImageUri
    }
    portraitImageUri to resolvedLandscapeUri
}

@Composable
private fun SaveCustomWallpaperButton(
    useSingleImage: Boolean,
    portraitImageUri: Uri?,
    landscapeImageUri: Uri?,
    onSaveClick: (Uri?, Uri?, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        FilledButton(
            text = stringResource(id = R.string.wallpaper_save_custom),
        ) {
            val (finalPortraitUri, finalLandscapeUri) = resolveSavedImageUris(
                useSingleImage = useSingleImage,
                portraitImageUri = portraitImageUri,
                landscapeImageUri = landscapeImageUri,
            )
            onSaveClick(finalPortraitUri, finalLandscapeUri, useSingleImage)
        }
    }
}

private fun resolveSavedImageUris(
    useSingleImage: Boolean,
    portraitImageUri: Uri?,
    landscapeImageUri: Uri?,
): Pair<Uri?, Uri?> = if (useSingleImage) {
    val finalPortraitUri = portraitImageUri ?: landscapeImageUri
    finalPortraitUri to finalPortraitUri
} else {
    portraitImageUri to landscapeImageUri
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = FirefoxTheme.typography.subtitle2,
        modifier = modifier.padding(
            horizontal = FirefoxTheme.layout.space.dynamic200,
            vertical = FirefoxTheme.layout.space.dynamic100,
        ),
    )
}

@Composable
private fun ImageSelectionRow(
    imageUri: Uri?,
    title: String,
    modifier: Modifier = Modifier,
    action: () -> Unit,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .clickable { action() }
            .padding(
                horizontal = FirefoxTheme.layout.space.dynamic200,
                vertical = FirefoxTheme.layout.space.dynamic100,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(AcornCorners.medium))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(AcornCorners.medium),
                ),
        ) {
            CustomWallpaperAndroidView(
                uri = imageUri,
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxSize(),
                contentDescription = stringResource(id = R.string.wallpaper_image_content_description),
                defaultImageRes = R.drawable.ic_file_type_image,
            )
        }

        Spacer(modifier = Modifier.width(FirefoxTheme.layout.space.dynamic300))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = FirefoxTheme.typography.body1,
            )
            Text(
                text = stringResource(
                    id = if (imageUri == null) {
                        R.string.wallpaper_select_image
                    } else {
                        R.string.wallpaper_change_image
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = FirefoxTheme.typography.caption,
            )
        }
    }
}

@FlexibleWindowLightDarkPreview
@Composable
private fun CustomWallpaperPreview() {
    FirefoxTheme {
        CustomWallpaper(
            currentPortraitImageUri = null,
            currentLandscapeImageUri = null,
            initialUseSingleImage = false,
            onSaveClick = { _, _, _ -> },
        )
    }
}
