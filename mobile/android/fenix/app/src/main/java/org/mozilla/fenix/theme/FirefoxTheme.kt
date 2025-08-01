/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mozilla.components.compose.base.theme.AcornColors
import mozilla.components.compose.base.theme.AcornTheme
import mozilla.components.compose.base.theme.AcornTypography
import mozilla.components.compose.base.theme.blueColorPalette
import mozilla.components.compose.base.theme.cyanColorPalette
import mozilla.components.compose.base.theme.darkColorPalette
import mozilla.components.compose.base.theme.greenColorPalette
import mozilla.components.compose.base.theme.layout.AcornLayout
import mozilla.components.compose.base.theme.layout.AcornWindowSize
import mozilla.components.compose.base.theme.lightColorPalette
import mozilla.components.compose.base.theme.orangeColorPalette
import mozilla.components.compose.base.theme.pinkColorPalette
import mozilla.components.compose.base.theme.privateColorPalette
import mozilla.components.compose.base.theme.purpleColorPalette
import mozilla.components.compose.base.theme.redColorPalette
import mozilla.components.compose.base.theme.violetColorPalette
import mozilla.components.compose.base.theme.yellowColorPalette
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
    val blackColorPalette = darkColorPalette.copy(
        layer1 = PhotonColors.Black,
        layer2 = PhotonColors.DarkGrey90,
        layer3 = PhotonColors.DarkGrey80,
        layer4Start = PhotonColors.Black,
        layer4Center = PhotonColors.Black,
        layer4End = PhotonColors.Black,
        actionTertiary = PhotonColors.DarkGrey80,
        borderPrimary = PhotonColors.DarkGrey70,
    )
    val colors = when (theme) {
        Theme.Light -> lightColorPalette
        Theme.Dark -> darkColorPalette
        Theme.Private -> privateColorPalette
        Theme.Violet -> violetColorPalette
        Theme.Blue -> blueColorPalette
        Theme.Pink -> pinkColorPalette
        Theme.Green -> greenColorPalette
        Theme.Red -> redColorPalette
        Theme.Orange -> orangeColorPalette
        Theme.Yellow -> yellowColorPalette
        Theme.Cyan -> cyanColorPalette
        Theme.Purple -> purpleColorPalette
        Theme.Black -> blackColorPalette
    }

    AcornTheme(
        colors = colors,
        content = content,
    )
}

/**
 * Indicates the theme that is displayed.
 */
enum class Theme {
    Light,
    Dark,
    Black,
    Private,
    Violet,
    Blue,
    Pink,
    Green,
    Red,
    Orange,
    Yellow,
    Cyan,
    Purple,
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
        fun getTheme(allowPrivateTheme: Boolean = true): Theme {
            val context = LocalContext.current
            val settings = context.settings()
            
            if (allowPrivateTheme && !inComposePreview && settings.lastKnownMode.isPrivate) {
                return Private
            }
            if (!inComposePreview) {
                if (settings.shouldUseBlackTheme) return Black
                if (settings.shouldUseVioletTheme) return Violet
                if (settings.shouldUseBlueTheme) return Blue
                if (settings.shouldUsePinkTheme) return Pink
                if (settings.shouldUseGreenTheme) return Green
                if (settings.shouldUseRedTheme) return Red
                if (settings.shouldUseOrangeTheme) return Orange
                if (settings.shouldUseYellowTheme) return Yellow
                if (settings.shouldUseCyanTheme) return Cyan
                if (settings.shouldUsePurpleTheme) return Purple
            }

            return when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_NO -> Light
                AppCompatDelegate.MODE_NIGHT_YES -> Dark
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> if (isSystemInDarkTheme()) Dark else Light
                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> if (isSystemInDarkTheme()) Dark else Light
                else -> Light
            }
        }
    }
}

/**
 * Provides access to the Firefox design system tokens.
 */
object FirefoxTheme {
    val colors: AcornColors
        @Composable
        get() = AcornTheme.colors

    val typography: AcornTypography
        get() = AcornTheme.typography

    val layout: AcornLayout
        @Composable
        get() = AcornTheme.layout

    val windowSize: AcornWindowSize
        @Composable
        get() = AcornTheme.windowSize
}
