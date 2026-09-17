/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.iconpicker

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mozilla.components.ui.colors.R as colorsR
import org.mozilla.fenix.R

/**
 * Enum that represents app launcher icons available for the user to set as an alternative launcher icon. It is based
 * off <activity-alias> entries declared in the AndroidManifest, with addition of title and subtitle for representing
 * the icon name in the UI.
 *
 * The aliasSuffix is the ending part of the <activity-alias>. They are used to construct full component names for
 * switching the launcher icon at runtime.
 *
 * Example:
 * - "AppSolidLight" → android:name="${applicationId}.AppSolidLight"
 *
 * @property aliasSuffix A suffix portion of the `android:name` attribute in the manifest.
 * @property iconForegroundId The foreground drawable resource used in icon previews.
 * @property iconBackground The background layer used in icon previews, which can be a solid color or drawable.
 * @property titleId A string resource describing the icon in the icon picker screen.
 * @property subtitleId An optional string resource used as a secondary label.
 */
enum class AppIcon(
    val aliasSuffix: String,
    @param:DrawableRes val iconForegroundId: Int = R.drawable.ic_waterfox_default,
    val iconBackground: IconBackground = IconBackground.Color(colorResId = R.color.waterfox_launcher_background),
    @param:StringRes val titleId: Int,
    @param:StringRes val subtitleId: Int? = null,
) {
    AppDefault(
        aliasSuffix = "App",
        titleId = R.string.alternative_app_icon_option_default,
    ),

    AppSolidLight(
        aliasSuffix = "AppSolidLight",
        titleId = R.string.alternative_app_icon_option_light,
    ),
    AppSolidDark(
        aliasSuffix = "AppSolidDark",
        titleId = R.string.alternative_app_icon_option_dark,
    ),
    AppSolidRed(
        aliasSuffix = "AppSolidRed",
        titleId = R.string.alternative_app_icon_option_red,
    ),
    AppSolidGreen(
        aliasSuffix = "AppSolidGreen",
        titleId = R.string.alternative_app_icon_option_green,
    ),
    AppSolidBlue(
        aliasSuffix = "AppSolidBlue",
        titleId = R.string.alternative_app_icon_option_blue,
    ),
    AppSolidPurple(
        aliasSuffix = "AppSolidPurple",
        titleId = R.string.alternative_app_icon_option_purple,
    ),
    AppSolidPurpleDark(
        aliasSuffix = "AppSolidPurpleDark",
        titleId = R.string.alternative_app_icon_option_purple_dark,
    ),
    AppGradientSunrise(
        aliasSuffix = "AppGradientSunrise",
        titleId = R.string.alternative_app_icon_option_gradient_sunrise,
    ),
    AppGradientGoldenHour(
        aliasSuffix = "AppGradientGoldenHour",
        titleId = R.string.alternative_app_icon_option_gradient_golden_hour,
    ),
    AppGradientSunset(
        aliasSuffix = "AppGradientSunset",
        titleId = R.string.alternative_app_icon_option_gradient_sunset,
    ),
    AppGradientBlueHour(
        aliasSuffix = "AppGradientBlueHour",
        titleId = R.string.alternative_app_icon_option_gradient_blue_hour,
    ),
    AppGradientTwilight(
        aliasSuffix = "AppGradientTwilight",
        titleId = R.string.alternative_app_icon_option_gradient_twilight,
    ),
    AppGradientMidnight(
        aliasSuffix = "AppGradientMidnight",
        titleId = R.string.alternative_app_icon_option_gradient_midnight,
    ),
    AppGradientNorthernLights(
        aliasSuffix = "AppGradientNorthernLights",
        titleId = R.string.alternative_app_icon_option_gradient_northern_lights,
    ),
    AppRetro2004(
        aliasSuffix = "AppRetro2004",
        titleId = R.string.alternative_app_icon_option_retro_2004,
    ),
    AppPixelated(
        aliasSuffix = "AppPixelated",
        titleId = R.string.alternative_app_icon_option_pixelated,
    ),
    AppCuddling(
        aliasSuffix = "AppCuddling",
        titleId = R.string.alternative_app_icon_option_cuddling,
    ),
    AppPride(
        aliasSuffix = "AppPride",
        titleId = R.string.alternative_app_icon_option_pride,
    ),
    AppFlaming(
        aliasSuffix = "AppFlaming",
        titleId = R.string.alternative_app_icon_option_flaming,
    ),
    AppMinimal(
        aliasSuffix = "AppMinimal",
        titleId = R.string.alternative_app_icon_option_minimal,
    ),
    AppMomo(
        aliasSuffix = "AppMomo",
        titleId = R.string.alternative_app_icon_option_momo,
        subtitleId = R.string.alternative_app_icon_option_momo_subtitle,
    ),
    AppCool(
        aliasSuffix = "AppCool",
        titleId = R.string.alternative_app_icon_option_cool,
    ),
    AppWaterfox2011To2014(
        aliasSuffix = "AppWaterfox2011To2014",
        iconForegroundId = R.drawable.ic_waterfox_2011_2014,
        iconBackground = IconBackground.Color(colorResId = colorsR.color.novaWhite),
        titleId = R.string.waterfox_app_icon_2011_2014,
    ),
    AppWaterfox2014To2015(
        aliasSuffix = "AppWaterfox2014To2015",
        iconForegroundId = R.drawable.ic_waterfox_2014_2015,
        iconBackground = IconBackground.Color(colorResId = colorsR.color.novaWhite),
        titleId = R.string.waterfox_app_icon_2014_2015,
    ),
    AppWaterfox2015To2019(
        aliasSuffix = "AppWaterfox2015To2019",
        iconForegroundId = R.drawable.ic_waterfox_2015_2019,
        iconBackground = IconBackground.Color(colorResId = colorsR.color.novaWhite),
        titleId = R.string.waterfox_app_icon_2015_2019,
    ),
    AppWaterfox2019(
        aliasSuffix = "AppWaterfox2019",
        iconForegroundId = R.drawable.ic_waterfox_2019,
        iconBackground = IconBackground.Color(colorResId = colorsR.color.novaWhite),
        titleId = R.string.waterfox_app_icon_2019,
    ),
    AppWaterfox2019To2023(
        aliasSuffix = "AppWaterfox2019To2023",
        iconForegroundId = R.drawable.ic_waterfox_2019_2023,
        iconBackground = IconBackground.Color(colorResId = colorsR.color.novaWhite),
        titleId = R.string.waterfox_app_icon_2019_2023,
    ),
    AppAlternative(
        aliasSuffix = "AlternativeApp",
        titleId = R.string.alternative_app_icon_option_default,
    );

    /** Legacy aliases remain readable for upgrades but cannot be selected again. */
    val isSelectable: Boolean
        get() = this in selectableIcons

    /** [AppIcon] helper object */
    companion object {
        /** The Waterfox-only catalogue, also enforced at the launcher switch boundary. */
        val selectableIcons: List<AppIcon> = listOf(
            AppWaterfox2011To2014,
            AppWaterfox2014To2015,
            AppWaterfox2015To2019,
            AppWaterfox2019,
            AppWaterfox2019To2023,
            AppDefault,
        )

        /**
         * Returns the [AppIcon] associated with the given string.
         *
         * @param aliasSuffix The suffix from android:name in the manifest (e.g. "AppSolidLight"). Full definition
         *   example from the manifest: android:name="${applicationId}.AppSolidLight"
         */
        fun fromString(aliasSuffix: String): AppIcon = entries.find { it.aliasSuffix == aliasSuffix } ?: AppDefault
    }
}

/**
 * Represents the background layer of an app icon mipmap assigned to a `<activity-alias>`.
 *
 * It allows passing both `@DrawableRes` and `@ColorRes`, as mipmap files support both `drawable` and `color` parameters
 * for `<background android:drawable>`
 */
sealed class IconBackground {
    /**
     * A solid color background.
     *
     * @property colorResId The color resource ID to use.
     */
    data class Color(@param:ColorRes val colorResId: Int) : IconBackground()

    /**
     * A drawable background.
     *
     * @property drawableResId The drawable resource ID to use.
     */
    data class Drawable(@param:DrawableRes val drawableResId: Int) : IconBackground()
}
