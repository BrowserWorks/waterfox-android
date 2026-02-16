/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import mozilla.components.compose.base.theme.AcornColors
import mozilla.components.compose.base.theme.AcornTheme
import mozilla.components.compose.base.theme.AcornTypography
import mozilla.components.compose.base.theme.acornDarkColorScheme
import mozilla.components.compose.base.theme.acornLightColorScheme
import mozilla.components.compose.base.theme.acornPrivateColorScheme
import mozilla.components.compose.base.theme.darkColorPalette
import mozilla.components.compose.base.theme.layout.AcornLayout
import mozilla.components.compose.base.theme.layout.AcornWindowSize
import mozilla.components.compose.base.theme.lightColorPalette
import mozilla.components.compose.base.theme.privateColorPalette
import mozilla.components.compose.base.utils.inComposePreview
import mozilla.components.ui.colors.PhotonColors
import org.mozilla.fenix.ext.settings

/**
 * The theme for Mozilla Firefox for Android (Fenix).
 *
 * @param theme The current [Theme] that is displayed.
 * @param content The children composables to be laid out.
 */
@Composable
fun FirefoxTheme(
    theme: Theme = Theme.getTheme(),
    content: @Composable () -> Unit,
) {
    val settings = LocalContext.current.settings()
    val themeColor = settings.resolveThemeColor(theme == Theme.Dark)
    val useCustomColors = theme == Theme.Light || theme == Theme.Dark
    val customPalette = if (useCustomColors && themeColor != ThemeColor.Default) {
        themeColor.palette(theme == Theme.Dark)
    } else {
        null
    }

    val baseColors: AcornColors = when (theme) {
        Theme.Light -> lightColorPalette
        Theme.Dark -> darkColorPalette
        Theme.Black -> darkColorPalette
        Theme.Private -> privateColorPalette
    }

    val colors = if (customPalette != null) {
        val isDark = theme == Theme.Dark
        val layerNonOpaqueBase = if (isDark) {
            customPalette.action
        } else {
            customPalette.textAccent
        }

        baseColors.copy(
            layer2 = customPalette.layer2,
            layer3 = customPalette.layer3,
            layerAccent = customPalette.control,
            layerAccentNonOpaque = layerNonOpaqueBase.copy(alpha = if (isDark) 0.32f else 0.12f),
            iconActive = customPalette.control,
            borderAccent = customPalette.control,
            surfaceDimVariant = customPalette.layer2,
        )
    } else {
        baseColors
    }

    val baseColorScheme: ColorScheme = when (theme) {
        Theme.Light -> acornLightColorScheme()
        Theme.Dark -> acornDarkColorScheme()
        Theme.Black -> blackColorScheme
        Theme.Private -> acornPrivateColorScheme()
    }

    val colorScheme = if (customPalette != null) {
        val isDark = theme == Theme.Dark
        baseColorScheme.copy(
            primary = if (isDark) customPalette.textAccent else customPalette.action,
            primaryContainer = customPalette.layer2,
            inversePrimary = customPalette.action,
            background = customPalette.layer1,
            surface = customPalette.layer1,
            surfaceDim = customPalette.layer1,
            surfaceBright = customPalette.layer2,
            surfaceContainerLowest = customPalette.layer1,
            surfaceContainerLow = customPalette.layer2,
            surfaceContainer = customPalette.layer2,
            surfaceContainerHigh = customPalette.layer3,
            surfaceContainerHighest = customPalette.layer3,
            surfaceVariant = customPalette.layer2,
            outline = customPalette.control,
            outlineVariant = customPalette.layer3,
        )
    } else {
        baseColorScheme
    }

    AcornTheme(
        colors = colors,
        colorScheme = colorScheme,
        content = content,
    )
}

private val blackColorScheme = acornDarkColorScheme().copy(
    background = PhotonColors.Black,
    surface = PhotonColors.Black,
    surfaceDim = PhotonColors.Black,
    surfaceBright = PhotonColors.DarkGrey80,
    surfaceContainerLowest = PhotonColors.DarkGrey80,
    surfaceContainerLow = PhotonColors.DarkGrey80,
    surfaceContainer = PhotonColors.DarkGrey90,
    surfaceContainerHigh = PhotonColors.DarkGrey80,
    surfaceContainerHighest = PhotonColors.DarkGrey70,
    surfaceVariant = PhotonColors.DarkGrey80,
)

/**
 * Indicates the theme that is displayed.
 */
enum class Theme {
    Light,
    Dark,
    Black,
    Private,
    ;

    companion object {
        /**
         * Returns the current [Theme] that is displayed.
         *
         * @param allowPrivateTheme Boolean used to control whether [Theme.Private] is an option
         * for [FirefoxTheme] colors.
         * @return the current [Theme] that is displayed.
         */
        @Composable
        @ReadOnlyComposable
        fun getTheme(allowPrivateTheme: Boolean = true) =
            if (allowPrivateTheme &&
                !inComposePreview &&
                LocalContext.current.settings().lastKnownMode.isPrivate
            ) {
                Private
            } else if (isSystemInDarkTheme()) {
                if (LocalContext.current.settings().shouldUseBlackTheme) {
                    Black
                } else {
                    Dark
                }
            } else {
                Light
            }
    }
}

/**
 * Provides access to the Firefox design system tokens.
 */
object FirefoxTheme {
    val colors: AcornColors
        @Composable
        @ReadOnlyComposable
        get() = AcornTheme.colors

    val typography: AcornTypography
        get() = AcornTheme.typography

    val layout: AcornLayout
        @Composable
        @ReadOnlyComposable
        get() = AcornTheme.layout

    val windowSize: AcornWindowSize
        @Composable
        @ReadOnlyComposable
        get() = AcornTheme.windowSize
}
