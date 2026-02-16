/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.annotation.StyleRes
import androidx.compose.ui.graphics.Color
import mozilla.components.ui.colors.PhotonColors
import org.mozilla.fenix.R
import org.mozilla.fenix.utils.Settings

data class ThemeColorPalette(
    val layer1: Color,
    val layer2: Color,
    val layer3: Color,
    val control: Color,
    val action: Color,
    val textAccent: Color,
    val bright: Color,
)

enum class ThemeColor(
    val prefValue: String,
    @param:StyleRes val styleRes: Int,
    val labelRes: Int,
    val light: ThemeColorPalette,
    val dark: ThemeColorPalette,
) {
    Default(
        prefValue = "default",
        styleRes = R.style.NormalTheme,
        labelRes = R.string.preference_theme_color_default,
        light = ThemeColorPalette(
            PhotonColors.LightGrey10,
            PhotonColors.LightGrey20,
            PhotonColors.LightGrey30,
            PhotonColors.Ink20,
            PhotonColors.Ink20,
            PhotonColors.Violet70,
            PhotonColors.Violet60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.DarkGrey60,
            PhotonColors.DarkGrey30,
            PhotonColors.DarkGrey80,
            PhotonColors.Violet40,
            PhotonColors.Violet50,
            PhotonColors.Violet20,
            PhotonColors.Violet50,
        ),
    ),
    Blue(
        prefValue = "blue",
        styleRes = R.style.NormalTheme_Color_Blue,
        labelRes = R.string.preference_theme_color_blue,
        light = ThemeColorPalette(
            PhotonColors.Blue30,
            PhotonColors.Blue20,
            PhotonColors.Blue10,
            PhotonColors.Blue50,
            PhotonColors.Blue50,
            PhotonColors.Blue70,
            PhotonColors.Blue60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Blue40,
            PhotonColors.Blue30,
            PhotonColors.Blue20,
            PhotonColors.Blue50,
            PhotonColors.Blue50,
            PhotonColors.Blue20,
            PhotonColors.Blue50,
        ),
    ),
    Green(
        prefValue = "green",
        styleRes = R.style.NormalTheme_Color_Green,
        labelRes = R.string.preference_theme_color_green,
        light = ThemeColorPalette(
            PhotonColors.Green60,
            PhotonColors.Green50,
            PhotonColors.Green40,
            PhotonColors.Green50,
            PhotonColors.Green50,
            PhotonColors.Green70,
            PhotonColors.Green60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Green70,
            PhotonColors.Green60,
            PhotonColors.Green50,
            PhotonColors.Green40,
            PhotonColors.Green50,
            PhotonColors.Green20,
            PhotonColors.Green50,
        ),
    ),
    Yellow(
        prefValue = "yellow",
        styleRes = R.style.NormalTheme_Color_Yellow,
        labelRes = R.string.preference_theme_color_yellow,
        light = ThemeColorPalette(
            PhotonColors.Yellow50,
            PhotonColors.Yellow40,
            PhotonColors.Yellow30,
            PhotonColors.Yellow60,
            PhotonColors.Yellow50,
            PhotonColors.Yellow70,
            PhotonColors.Yellow60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Yellow60,
            PhotonColors.Yellow50,
            PhotonColors.Yellow40,
            PhotonColors.Yellow40,
            PhotonColors.Yellow50,
            PhotonColors.Yellow20,
            PhotonColors.Yellow50,
        ),
    ),
    Orange(
        prefValue = "orange",
        styleRes = R.style.NormalTheme_Color_Orange,
        labelRes = R.string.preference_theme_color_orange,
        light = ThemeColorPalette(
            PhotonColors.Orange40,
            PhotonColors.Orange30,
            PhotonColors.Orange20,
            PhotonColors.Orange50,
            PhotonColors.Orange50,
            PhotonColors.Orange70,
            PhotonColors.Orange60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Orange50,
            PhotonColors.Orange40,
            PhotonColors.Orange30,
            PhotonColors.Orange40,
            PhotonColors.Orange50,
            PhotonColors.Orange20,
            PhotonColors.Orange50,
        ),
    ),
    Red(
        prefValue = "red",
        styleRes = R.style.NormalTheme_Color_Red,
        labelRes = R.string.preference_theme_color_red,
        light = ThemeColorPalette(
            PhotonColors.Red30,
            PhotonColors.Red20,
            PhotonColors.Red10,
            PhotonColors.Red50,
            PhotonColors.Red50,
            PhotonColors.Red70,
            PhotonColors.Red60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Red40,
            PhotonColors.Red30,
            PhotonColors.Red20,
            PhotonColors.Red50,
            PhotonColors.Red50,
            PhotonColors.Red20,
            PhotonColors.Red50,
        ),
    ),
    Pink(
        prefValue = "pink",
        styleRes = R.style.NormalTheme_Color_Pink,
        labelRes = R.string.preference_theme_color_pink,
        light = ThemeColorPalette(
            PhotonColors.Pink20,
            PhotonColors.Pink10,
            PhotonColors.Pink05,
            PhotonColors.Pink50,
            PhotonColors.Pink50,
            PhotonColors.Pink70,
            PhotonColors.Pink60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Pink30,
            PhotonColors.Pink20,
            PhotonColors.Pink10,
            PhotonColors.Pink40,
            PhotonColors.Pink50,
            PhotonColors.Pink20,
            PhotonColors.Pink50,
        ),
    ),
    Purple(
        prefValue = "purple",
        styleRes = R.style.NormalTheme_Color_Purple,
        labelRes = R.string.preference_theme_color_purple,
        light = ThemeColorPalette(
            PhotonColors.Purple30,
            PhotonColors.Purple20,
            PhotonColors.Purple10,
            PhotonColors.Purple50,
            PhotonColors.Purple50,
            PhotonColors.Purple70,
            PhotonColors.Purple60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Purple40,
            PhotonColors.Purple30,
            PhotonColors.Purple20,
            PhotonColors.Purple50,
            PhotonColors.Purple50,
            PhotonColors.Purple20,
            PhotonColors.Purple50,
        ),
    ),
    Violet(
        prefValue = "violet",
        styleRes = R.style.NormalTheme_Color_Violet,
        labelRes = R.string.preference_theme_color_violet,
        light = ThemeColorPalette(
            PhotonColors.Violet30,
            PhotonColors.Violet20,
            PhotonColors.Violet10,
            PhotonColors.Violet50,
            PhotonColors.Violet50,
            PhotonColors.Violet70,
            PhotonColors.Violet60,
        ),
        dark = ThemeColorPalette(
            PhotonColors.Violet40,
            PhotonColors.Violet30,
            PhotonColors.Violet20,
            PhotonColors.Violet50,
            PhotonColors.Violet50,
            PhotonColors.Violet20,
            PhotonColors.Violet50,
        ),
    ),
    ;

    fun palette(isDark: Boolean): ThemeColorPalette = if (isDark) dark else light

    companion object {
        fun fromPreferenceValue(value: String?): ThemeColor =
            entries.firstOrNull { it.prefValue == value } ?: Default
    }
}

fun Settings.resolveThemeColor(isDark: Boolean): ThemeColor {
    val value = if (customThemeColorLight != ThemeColor.Default.prefValue ||
        customThemeColorDark != ThemeColor.Default.prefValue
    ) {
        if (isDark) customThemeColorDark else customThemeColorLight
    } else {
        ThemeColor.Default.prefValue
    }
    return ThemeColor.fromPreferenceValue(value)
}
