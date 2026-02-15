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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

@Composable
fun CustomWallpaper(
    currentPortraitImageUri: Uri?,
    currentLandscapeImageUri: Uri?,
    initialUseSingleImage: Boolean = false,
    onSaveClick: (Uri?, Uri?, Boolean) -> Unit,
) {
    var portraitImageUri by rememberSaveable {
        mutableStateOf(currentPortraitImageUri)
    }
    var landscapeImageUri by rememberSaveable {
        mutableStateOf(currentLandscapeImageUri)
    }
    var useSingleImage by rememberSaveable {
        mutableStateOf(initialUseSingleImage)
    }

    val portraitImageUriLauncher =
        rememberLauncherForActivityResult(PickVisualMedia()) {
            portraitImageUri = it
            if (useSingleImage) {
                landscapeImageUri = it
            }
        }
    val landscapeImageUriLauncher =
        rememberLauncherForActivityResult(PickVisualMedia()) {
            landscapeImageUri = it
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FirefoxTheme.layout.space.dynamic400),
    ) {
        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

        WallpaperSelector(
            imageUri = portraitImageUri,
            text = stringResource(id = R.string.wallpaper_select_portrait),
            modifier = Modifier.fillMaxWidth(),
        ) {
            portraitImageUriLauncher.launch(
                PickVisualMediaRequest(PickVisualMedia.ImageOnly),
            )
        }

        if (!useSingleImage) {
            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

            WallpaperSelector(
                imageUri = landscapeImageUri,
                text = stringResource(id = R.string.wallpaper_select_landscape),
                modifier = Modifier.fillMaxWidth(),
            ) {
                landscapeImageUriLauncher.launch(
                    PickVisualMediaRequest(PickVisualMedia.ImageOnly),
                )
            }
        }

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

        SingleImageSwitch(
            checked = useSingleImage,
            onCheckedChange = { checked ->
                useSingleImage = checked
                if (checked && portraitImageUri != null) {
                    landscapeImageUri = portraitImageUri
                } else if (!checked) {
                    if (portraitImageUri != null && landscapeImageUri == portraitImageUri) {
                        landscapeImageUri = null
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val hasPortrait = portraitImageUri != null
                        val hasLandscape = landscapeImageUri != null && !useSingleImage
                        val hasSingleImage = useSingleImage && portraitImageUri != null

                        if (hasPortrait || hasLandscape || hasSingleImage) {
                            val finalLandscapeUri = if (useSingleImage) portraitImageUri else landscapeImageUri
                            onSaveClick(portraitImageUri, finalLandscapeUri, useSingleImage)
                        }
                    },
                shape = RoundedCornerShape(FirefoxTheme.layout.corner.large),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = FirefoxTheme.layout.elevation.medium,
            ) {
                Text(
                    text = stringResource(id = R.string.wallpaper_save_custom),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = FirefoxTheme.typography.button,
                    modifier = Modifier.padding(FirefoxTheme.layout.space.dynamic400),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))
    }
}

@Composable
private fun WallpaperSelector(
    imageUri: Uri?,
    text: String,
    modifier: Modifier = Modifier,
    action: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.dynamic400),
    ) {
        Box(
            modifier = Modifier
                .height(FirefoxTheme.layout.size.dynamic1400)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(FirefoxTheme.layout.corner.large),
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

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { action() },
            shape = RoundedCornerShape(FirefoxTheme.layout.corner.large),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = FirefoxTheme.layout.elevation.medium,
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = FirefoxTheme.typography.button,
                modifier = Modifier.padding(FirefoxTheme.layout.space.dynamic400),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SingleImageSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .padding(vertical = FirefoxTheme.layout.space.dynamic400),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.wallpaper_use_single_image),
            color = MaterialTheme.colorScheme.onSurface,
            style = FirefoxTheme.typography.subtitle1,
            modifier = Modifier.padding(end = FirefoxTheme.layout.space.dynamic400),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
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