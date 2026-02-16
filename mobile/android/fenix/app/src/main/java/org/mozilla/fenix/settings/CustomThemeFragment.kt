/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.ThemeColor

/** Lets users select independent light and dark theme colors. */
class CustomThemeFragment : Fragment(), SystemInsetsPaddedFragment {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        FirefoxTheme {
            CustomThemeScreen(onColorChanged = { requireActivity().recreate() })
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preference_custom_theme))
    }
}

@Composable
private fun CustomThemeScreen(onColorChanged: () -> Unit) {
    val settings = LocalContext.current.components.settings
    var lightColorValue by remember { mutableStateOf(settings.customThemeColorLight) }
    var darkColorValue by remember { mutableStateOf(settings.customThemeColorDark) }
    val options = remember { ThemeColor.entries }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surface),
        contentPadding =
            PaddingValues(
                horizontal = FirefoxTheme.layout.space.static200,
                vertical = FirefoxTheme.layout.space.static150,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.preference_custom_theme_light_colors),
                style = themeColorHeadingStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        items(options) { option ->
            ThemeColorSwatch(
                label = stringResource(option.labelRes),
                color = option.swatchColor(isDark = false),
                selected = option.prefValue == lightColorValue,
                onClick = {
                    lightColorValue = option.prefValue
                    settings.customThemeColorLight = option.prefValue
                    onColorChanged()
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.preference_custom_theme_dark_colors),
                style = themeColorHeadingStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = FirefoxTheme.layout.space.static100),
            )
        }
        items(options) { option ->
            ThemeColorSwatch(
                label = stringResource(option.labelRes),
                color = option.swatchColor(isDark = true),
                selected = option.prefValue == darkColorValue,
                onClick = {
                    darkColorValue = option.prefValue
                    settings.customThemeColorDark = option.prefValue
                    onColorChanged()
                },
            )
        }
    }
}

@Composable
private fun ThemeColorSwatch(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(role = Role.RadioButton) { onClick() },
    ) {
        Box(
            modifier =
                Modifier.size(48.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(iconsR.drawable.mozac_ic_checkmark_24),
                    contentDescription = null,
                    tint = if (color.luminance() < 0.5f) Color.White else Color.Black,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style =
                FirefoxTheme.typography.caption.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val themeColorHeadingStyle =
    FirefoxTheme.typography.headline7.copy(
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 0.2.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

private fun ThemeColor.swatchColor(isDark: Boolean): Color =
    if (isDark) {
        dark.layer1
    } else {
        light.layer1
    }
