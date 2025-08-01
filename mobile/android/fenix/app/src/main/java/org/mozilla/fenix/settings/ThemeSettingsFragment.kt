/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.GleanMetrics.AppTheme
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.utils.view.addToRadioGroup

/**
 * Lets the user choose themes and appearance options.
 */
class ThemeSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var radioLightTheme: RadioButtonPreference
    private lateinit var radioDarkTheme: RadioButtonPreference
    private lateinit var radioAutoBatteryTheme: RadioButtonPreference
    private lateinit var radioFollowDeviceTheme: RadioButtonPreference
    private lateinit var radioVioletTheme: RadioButtonPreference
    private lateinit var radioBlueTheme: RadioButtonPreference
    private lateinit var radioPinkTheme: RadioButtonPreference
    private lateinit var radioGreenTheme: RadioButtonPreference
    private lateinit var radioRedTheme: RadioButtonPreference
    private lateinit var radioOrangeTheme: RadioButtonPreference
    private lateinit var radioYellowTheme: RadioButtonPreference
    private lateinit var radioCyanTheme: RadioButtonPreference
    private lateinit var radioPurpleTheme: RadioButtonPreference
    private lateinit var radioBlackTheme: RadioButtonPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.theme_preferences, rootKey)

        setupPreferences()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_theme))
    }

    private fun setupPreferences() {
        bindFollowDeviceTheme()
        bindDarkTheme()
        bindLightTheme()
        bindAutoBatteryTheme()
        bindVioletTheme()
        bindBlueTheme()
        bindPinkTheme()
        bindGreenTheme()
        bindRedTheme()
        bindOrangeTheme()
        bindYellowTheme()
        bindCyanTheme()
        bindPurpleTheme()
        bindBlackTheme()
        setupRadioGroups()
    }

    private fun setupRadioGroups() {
        addToRadioGroup(
            radioLightTheme,
            radioDarkTheme,
            radioVioletTheme,
            radioBlueTheme,
            radioPinkTheme,
            radioGreenTheme,
            radioRedTheme,
            radioOrangeTheme,
            radioYellowTheme,
            radioCyanTheme,
            radioPurpleTheme,
            radioBlackTheme,
            if (SDK_INT >= Build.VERSION_CODES.P) {
                radioFollowDeviceTheme
            } else {
                radioAutoBatteryTheme
            },
        )
        
        // Set current theme selection
        val settings = requireContext().settings()

        // Prioritize custom themes to avoid conflicts
        if (settings.shouldUseVioletTheme) {
            radioVioletTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseBlueTheme) {
            radioBlueTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUsePinkTheme) {
            radioPinkTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseGreenTheme) {
            radioGreenTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseRedTheme) {
            radioRedTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseOrangeTheme) {
            radioOrangeTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseYellowTheme) {
            radioYellowTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseCyanTheme) {
            radioCyanTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUsePurpleTheme) {
            radioPurpleTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseBlackTheme) {
            radioBlackTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldFollowDeviceTheme) {
            radioFollowDeviceTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseAutoBatteryTheme) {
            radioAutoBatteryTheme.setCheckedWithoutClickListener(true)
        } else if (settings.shouldUseDarkTheme) {
            radioDarkTheme.setCheckedWithoutClickListener(true)
        } else {
            // Default to light theme
            radioLightTheme.setCheckedWithoutClickListener(true)
        }
    }

    private fun bindLightTheme() {
        radioLightTheme = requirePreference(R.string.pref_key_light_theme)
        radioLightTheme.onClickListener {
            clearAllThemePreferences()
            requireContext().settings().shouldUseLightTheme = true
            setNewTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun bindAutoBatteryTheme() {
        radioAutoBatteryTheme = requirePreference(R.string.pref_key_auto_battery_theme)
        radioAutoBatteryTheme.onClickListener {
            clearAllThemePreferences()
            requireContext().settings().shouldUseAutoBatteryTheme = true
            setNewTheme(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
        }
    }

    private fun bindDarkTheme() {
        radioDarkTheme = requirePreference(R.string.pref_key_dark_theme)
        radioDarkTheme.onClickListener {
            AppTheme.darkThemeSelected.record(
                AppTheme.DarkThemeSelectedExtra(
                    "SETTINGS",
                ),
            )
            clearAllThemePreferences()
            requireContext().settings().shouldUseDarkTheme = true
            setNewTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun bindFollowDeviceTheme() {
        radioFollowDeviceTheme = requirePreference(R.string.pref_key_follow_device_theme)
        if (SDK_INT >= Build.VERSION_CODES.P) {
            radioFollowDeviceTheme.onClickListener {
                clearAllThemePreferences()
                requireContext().settings().shouldFollowDeviceTheme = true
                setNewTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun bindVioletTheme() {
        radioVioletTheme = requirePreference(R.string.pref_key_violet_theme)
        radioVioletTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseVioletTheme = true }
        }
    }

    private fun bindBlueTheme() {
        radioBlueTheme = requirePreference(R.string.pref_key_blue_theme)
        radioBlueTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseBlueTheme = true }
        }
    }

    private fun bindPinkTheme() {
        radioPinkTheme = requirePreference(R.string.pref_key_pink_theme)
        radioPinkTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUsePinkTheme = true }
        }
    }

    private fun bindGreenTheme() {
        radioGreenTheme = requirePreference(R.string.pref_key_green_theme)
        radioGreenTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseGreenTheme = true }
        }
    }

    private fun bindRedTheme() {
        radioRedTheme = requirePreference(R.string.pref_key_red_theme)
        radioRedTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseRedTheme = true }
        }
    }

    private fun bindOrangeTheme() {
        radioOrangeTheme = requirePreference(R.string.pref_key_orange_theme)
        radioOrangeTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseOrangeTheme = true }
        }
    }

    private fun bindYellowTheme() {
        radioYellowTheme = requirePreference(R.string.pref_key_yellow_theme)
        radioYellowTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseYellowTheme = true }
        }
    }

    private fun bindCyanTheme() {
        radioCyanTheme = requirePreference(R.string.pref_key_cyan_theme)
        radioCyanTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUseCyanTheme = true }
        }
    }

    private fun bindPurpleTheme() {
        radioPurpleTheme = requirePreference(R.string.pref_key_purple_theme)
        radioPurpleTheme.onClickListener {
            setCustomTheme { settings -> settings.shouldUsePurpleTheme = true }
        }
    }

    private fun bindBlackTheme() {
        radioBlackTheme = requirePreference(R.string.pref_key_black_theme)
        radioBlackTheme.onClickListener {
            clearAllThemePreferences()
            requireContext().settings().shouldUseBlackTheme = true

            // Force dark base for OLED black theme
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

            (activity as? HomeActivity)?.let {
                it.browsingModeManager.mode = BrowsingMode.Normal
                it.themeManager.applyStatusBarTheme(it)
                it.recreate()
            }
        }
    }

    private fun clearAllThemePreferences() {
        val settings = requireContext().settings()
        settings.shouldUseLightTheme = false
        settings.shouldUseDarkTheme = false
        settings.shouldFollowDeviceTheme = false
        settings.shouldUseAutoBatteryTheme = false
        settings.shouldUseVioletTheme = false
        settings.shouldUseBlueTheme = false
        settings.shouldUsePinkTheme = false
        settings.shouldUseGreenTheme = false
        settings.shouldUseRedTheme = false
        settings.shouldUseOrangeTheme = false
        settings.shouldUseYellowTheme = false
        settings.shouldUseCyanTheme = false
        settings.shouldUsePurpleTheme = false
        settings.shouldUseBlackTheme = false
    }

    private fun setCustomTheme(applyTheme: (Settings) -> Unit) {
        clearAllThemePreferences()
        applyTheme(requireContext().settings())

        // Set system to dark mode as base for our custom themes
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Trigger theme manager to apply the theme and status bar theming
        (activity as? HomeActivity)?.let {
            it.browsingModeManager.mode = BrowsingMode.Normal
            it.themeManager.applyStatusBarTheme(it)
            it.recreate()
        }
    }

    private fun setNewTheme(mode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        
        clearAllThemePreferences()

        val settings = requireContext().settings()
        when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> settings.shouldUseLightTheme = true
            AppCompatDelegate.MODE_NIGHT_YES -> settings.shouldUseDarkTheme = true
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> settings.shouldFollowDeviceTheme = true
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> settings.shouldUseAutoBatteryTheme = true
        }
        
        AppCompatDelegate.setDefaultNightMode(mode)
        activity?.recreate()
        with(requireComponents.core) {
            engine.settings.preferredColorScheme = getPreferredColorScheme()
        }
        requireComponents.useCases.sessionUseCases.reload.invoke()
    }
}