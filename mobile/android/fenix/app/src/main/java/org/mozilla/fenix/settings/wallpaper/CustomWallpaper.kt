/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
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
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import org.mozilla.fenix.settings.wallpaper.CustomWallpaperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
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
            // If single image mode is enabled, also set landscape
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

        // Portrait Image Selector
        WallpaperSelector(
            imageUri = portraitImageUri,
            text = stringResource(id = R.string.wallpaper_select_portrait),
            modifier = Modifier.weight(1f),
        ) {
            portraitImageUriLauncher.launch(
                PickVisualMediaRequest(PickVisualMedia.ImageOnly)
            )
        }

        if (!useSingleImage) {
            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

            // Landscape Image Selector
            WallpaperSelector(
                imageUri = landscapeImageUri,
                text = stringResource(id = R.string.wallpaper_select_landscape),
                modifier = Modifier.weight(1f),
            ) {
                landscapeImageUriLauncher.launch(
                    PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                )
            }
        }

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

        // Single Image Switch
        SingleImageSwitch(
            checked = useSingleImage,
            onCheckedChange = { checked ->
                useSingleImage = checked
                if (checked && portraitImageUri != null) {
                    // When enabling single image mode, copy portrait to landscape
                    landscapeImageUri = portraitImageUri
                } else if (!checked) {
                    // When disabling single image mode, clear landscape if it matches portrait
                    if (portraitImageUri != null && landscapeImageUri == portraitImageUri) {
                        landscapeImageUri = null
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.dynamic400))

        // Save Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // Validate that at least one image is selected
                        val hasPortrait = portraitImageUri != null
                        val hasLandscape = landscapeImageUri != null && !useSingleImage
                        val hasSingleImage = useSingleImage && portraitImageUri != null
                        
                        if (hasPortrait || hasLandscape || hasSingleImage) {
                            val finalLandscapeUri = if (useSingleImage) portraitImageUri else landscapeImageUri
                            onSaveClick(portraitImageUri, finalLandscapeUri, useSingleImage)
                        }
                    },
                shape = RoundedCornerShape(FirefoxTheme.layout.corner.large),
                color = FirefoxTheme.colors.actionPrimary,
                elevation = FirefoxTheme.layout.elevation.medium,
            ) {
                Text(
                    text = stringResource(id = R.string.wallpaper_save_custom),
                    color = FirefoxTheme.colors.textActionPrimary,
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
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = FirefoxTheme.colors.borderSecondary,
                    shape = RoundedCornerShape(FirefoxTheme.layout.corner.large)
                ),
        ) {
            CustomWallpaperAndroidView(
                uri = imageUri,
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxSize(),
                contentDescription = text,
                defaultImageRes = R.drawable.ic_file_type_image
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { action() },
            shape = RoundedCornerShape(FirefoxTheme.layout.corner.large),
            color = FirefoxTheme.colors.layer2,
            elevation = FirefoxTheme.layout.elevation.medium,
        ) {
            Text(
                text = text,
                color = FirefoxTheme.colors.textPrimary,
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
            color = FirefoxTheme.colors.textPrimary,
            style = FirefoxTheme.typography.subtitle1,
            modifier = Modifier.weight(0.8f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = FirefoxTheme.colors.formOff,
                uncheckedTrackColor = FirefoxTheme.colors.formSurface,
                checkedThumbColor = FirefoxTheme.colors.formOn,
                checkedTrackColor = FirefoxTheme.colors.formSurface,
            ),
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
