/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.TypedValue
import android.view.Window
import androidx.annotation.AnyRes
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.content.getStatusBarColor
import mozilla.components.support.ktx.android.view.createWindowInsetsController
import mozilla.components.support.ktx.android.view.setNavigationBarColorCompat
import mozilla.components.support.ktx.android.view.setStatusBarColorCompat
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity
import org.mozilla.fenix.ext.settings

abstract class ThemeManager(
    protected val privacyStyleRes: Int,
) {

    abstract var currentTheme: BrowsingMode

    /**
     * Returns the style resource corresponding to the [currentTheme].
     */
    @get:StyleRes
    open val currentThemeResource get() = when (currentTheme) {
        BrowsingMode.Normal -> R.style.NormalTheme
        BrowsingMode.Private -> privacyStyleRes
    }

    /**
     * Handles status bar theme change since the window does not dynamically recreate
     *
     * @param activity The activity to apply the status bar theme to.
     * @param overrideThemeStatusBarColor Whether to override the theme's status bar color.
     */
    fun applyStatusBarTheme(activity: Activity, overrideThemeStatusBarColor: Boolean = false) =
        applyStatusBarTheme(activity.window, activity, overrideThemeStatusBarColor)

    private fun applyStatusBarTheme(
        window: Window,
        context: Context,
        overrideThemeStatusBarColor: Boolean,
    ) {
        val settings = context.settings()
        val isForcedDark =
            settings.shouldUseBlueTheme ||
            settings.shouldUseVioletTheme ||
            settings.shouldUsePinkTheme ||
            settings.shouldUseGreenTheme ||
            settings.shouldUseRedTheme ||
            settings.shouldUseOrangeTheme ||
            settings.shouldUseYellowTheme ||
            settings.shouldUseCyanTheme ||
            settings.shouldUsePurpleTheme ||
            settings.shouldUseBlackTheme

        when (currentTheme) {
            BrowsingMode.Normal -> {
                // Any custom color theme or black theme uses dark backgrounds -> light system bar icons.
                if (isForcedDark ||
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
                ) {
                    clearLightSystemBars(window)
                    setStatusBarColor(window, context, overrideThemeStatusBarColor)
                    updateNavigationBar(window, context)
                } else {
                    updateLightSystemBars(window, context, overrideThemeStatusBarColor)
                }
            }
            BrowsingMode.Private -> {
                clearLightSystemBars(window)
                setStatusBarColor(window, context, overrideThemeStatusBarColor)
                updateNavigationBar(window, context)
            }
        }
    }

    fun setActivityTheme(activity: Activity) {
        activity.setTheme(currentThemeResource)
    }

    companion object {
        /**
         * Resolves the attribute to a resource ID.
         *
         * @param attribute The attribute to resolve.
         * @param context Any context.
         * @return The resource ID of the resolved attribute.
         * @throws IllegalStateException if the attribute cannot be resolved.
         */
        @AnyRes
        fun resolveAttribute(attribute: Int, context: Context): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            val resolved = theme.resolveAttribute(attribute, typedValue, true)

            if (!resolved || typedValue.resourceId == 0) {
                // Provide fallback colors for common theme attributes to prevent crashes
                return when (attribute) {
                    org.mozilla.fenix.R.attr.textPrimary -> 
                        org.mozilla.fenix.R.color.fx_mobile_text_color_primary
                    org.mozilla.fenix.R.attr.textSecondary -> 
                        org.mozilla.fenix.R.color.fx_mobile_text_color_secondary
                    org.mozilla.fenix.R.attr.textDisabled -> 
                        org.mozilla.fenix.R.color.fx_mobile_text_color_disabled
                    org.mozilla.fenix.R.attr.textAccent -> 
                        org.mozilla.fenix.R.color.fx_mobile_text_color_accent
                    org.mozilla.fenix.R.attr.textOnColorPrimary -> 
                        org.mozilla.fenix.R.color.fx_mobile_text_color_oncolor_primary
                    org.mozilla.fenix.R.attr.borderPrimary -> 
                        org.mozilla.fenix.R.color.fx_mobile_border_color_primary
                    org.mozilla.fenix.R.attr.borderSecondary -> 
                        org.mozilla.fenix.R.color.fx_mobile_border_color_secondary
                    org.mozilla.fenix.R.attr.layer1 -> 
                        org.mozilla.fenix.R.color.fx_mobile_layer_color_1
                    org.mozilla.fenix.R.attr.layer2 -> 
                        org.mozilla.fenix.R.color.fx_mobile_layer_color_2
                    org.mozilla.fenix.R.attr.layer3 -> 
                        org.mozilla.fenix.R.color.fx_mobile_layer_color_3
                    org.mozilla.fenix.R.attr.accent -> 
                        org.mozilla.fenix.R.color.fx_mobile_text_color_accent
                    org.mozilla.fenix.R.attr.actionPrimary -> 
                        org.mozilla.fenix.R.color.fx_mobile_action_color_primary
                    org.mozilla.fenix.R.attr.bottomBarBackgroundTop -> 
                        org.mozilla.fenix.R.color.fx_mobile_layer_color_1
                    else -> {
                        // For unknown attributes, log the issue but don't crash
                        android.util.Log.w(
                            "ThemeManager",
                            "Failed to resolve attribute 0x${Integer.toHexString(attribute)} " +
                            "in theme and no fallback available. Using transparent color."
                        )
                        android.R.color.transparent
                    }
                }
            }

            return typedValue.resourceId
        }

        @Composable
        fun resolveAttributeColor(attribute: Int): androidx.compose.ui.graphics.Color {
            val resourceId = resolveAttribute(attribute, LocalContext.current)
            return colorResource(resourceId)
        }

        private fun updateLightSystemBars(window: Window, context: Context, overrideThemeStatusBarColor: Boolean) {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                setStatusBarColor(window, context, overrideThemeStatusBarColor)
                window.createWindowInsetsController().isAppearanceLightStatusBars = true
            } else {
                window.setStatusBarColorCompat(Color.BLACK)
            }

            if (SDK_INT >= Build.VERSION_CODES.O) {
                // API level can display handle light navigation bar color
                window.createWindowInsetsController().isAppearanceLightNavigationBars = true

                updateNavigationBar(window, context)
            }
        }

        private fun clearLightSystemBars(window: Window) {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                window.createWindowInsetsController().isAppearanceLightStatusBars = false
            }

            if (SDK_INT >= Build.VERSION_CODES.O) {
                // API level can display handle light navigation bar color
                window.createWindowInsetsController().isAppearanceLightNavigationBars = false
            }
        }

        private fun updateNavigationBar(window: Window, context: Context) {
            window.setNavigationBarColorCompat(context.getColorFromAttr(R.attr.layer1))
        }

        private fun setStatusBarColor(
            window: Window,
            context: Context,
            overrideThemeStatusBarColor: Boolean,
        ) {
            if (overrideThemeStatusBarColor) {
                window.setStatusBarColorCompat(context.getColorFromAttr(R.attr.layer3))
            } else {
                context.getStatusBarColor()?.let { window.setStatusBarColorCompat(it) }
            }
        }
    }
}

class DefaultThemeManager(
    currentTheme: BrowsingMode,
    private val activity: Activity,
) : ThemeManager(privacyStyleRes = activity.getStyleRes()) {
    
    /**
     * Returns the style resource considering both browsing mode and user theme preferences.
     */
    @get:StyleRes
    override val currentThemeResource get() = when {
        currentTheme == BrowsingMode.Private -> privacyStyleRes
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseBlackTheme -> R.style.NormalBlackTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseVioletTheme -> R.style.VioletTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseBlueTheme -> R.style.BlueTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUsePinkTheme -> R.style.PinkTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseGreenTheme -> R.style.GreenTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseRedTheme -> R.style.RedTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseOrangeTheme -> R.style.OrangeTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseYellowTheme -> R.style.YellowTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUseCyanTheme -> R.style.CyanTheme
        currentTheme == BrowsingMode.Normal && activity.settings().shouldUsePurpleTheme -> R.style.PurpleTheme
        else -> R.style.NormalTheme
    }
    override var currentTheme: BrowsingMode = currentTheme
        set(value) {
            if (currentTheme != value) {
                // ExternalAppBrowserActivity doesn't need to switch between private and non-private.
                if (activity is ExternalAppBrowserActivity) return
                // Don't recreate if activity is finishing
                if (activity.isFinishing) return

                field = value

                val intent = activity.intent ?: Intent().also { activity.intent = it }
                intent.putExtra(HomeActivity.PRIVATE_BROWSING_MODE, value == BrowsingMode.Private)

                activity.recreate()
            }
        }
}

private fun Activity.getStyleRes(): Int = if (settings().feltPrivateBrowsingEnabled) {
    R.style.FeltPrivateTheme
} else {
    R.style.PrivateTheme
}
