/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import java.util.Locale
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.locale.LocaleManager
import mozilla.components.support.locale.LocaleUseCases

/**
 * Controller responsible for handling user interactions on the locale settings screen. This includes selecting a new
 * locale, searching for a locale, and resetting to the system default locale.
 */
interface LocaleSettingsController {

    /**
     * Handles the selection of a new locale.
     *
     * @param locale The [Locale] selected by the user.
     */
    fun handleLocaleSelected(locale: Locale)

    /**
     * Handles user input in the locale search field.
     *
     * @param query The search string typed by the user.
     */
    fun handleSearchQueryTyped(query: String)

    /** Handles the selection of the system's default locale. */
    fun handleDefaultLocaleSelected()
}

/**
 * Default implementation of [LocaleSettingsController].
 *
 * This class manages the logic for changing the application's language. It handles user interactions from the locale
 * settings screen, such as selecting a new language or resetting to the system default. It coordinates with various
 * stores and use cases to update the application state, refresh related components like search engines, and apply the
 * new locale by recreating the activity.
 *
 * @param activity The current [Activity] context, required for recreating the UI and accessing resources.
 * @param localeSettingsStore The store that manages the state for the locale settings UI, such as the list of available
 *   locales and the current search query.
 * @param browserStore The main browser store, used here to dispatch actions like refreshing search engines when the
 *   locale changes.
 * @param localeUseCase A set of use cases for managing the application's locale, including setting a new locale or
 *   resetting to the system default.
 */
class DefaultLocaleSettingsController(
    private val activity: Activity,
    private val localeSettingsStore: LocaleSettingsStore,
    private val browserStore: BrowserStore,
    private val localeUseCase: LocaleUseCases,
) : LocaleSettingsController {

    override fun handleLocaleSelected(locale: Locale) {
        if (localeSettingsStore.state.selectedLocale == locale && !LocaleManager.isDefaultLocaleSelected(activity)) {
            return
        }
        localeSettingsStore.dispatch(LocaleSettingsAction.Select(locale))
        browserStore.dispatch(SearchAction.RefreshSearchEnginesAction)
        updateLocale(locale)
        updateBaseConfiguration(activity, locale)

        recreateActivity()
    }

    override fun handleDefaultLocaleSelected() {
        if (LocaleManager.isDefaultLocaleSelected(activity)) {
            return
        }
        localeSettingsStore.dispatch(LocaleSettingsAction.Select(localeSettingsStore.state.localeList[0]))
        browserStore.dispatch(SearchAction.RefreshSearchEnginesAction)
        resetToSystemDefault()
        updateBaseConfiguration(activity, localeSettingsStore.state.localeList[0])

        recreateActivity()
    }

    override fun handleSearchQueryTyped(query: String) {
        localeSettingsStore.dispatch(LocaleSettingsAction.Search(query))
    }

    /** Recreates the activity to apply locale changes and provides a smooth, instant transition. */
    @VisibleForTesting
    internal fun recreateActivity() {
        activity.recreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION") activity.overridePendingTransition(0, 0)
        }
    }

    /**
     * Sets the new application locale and updates the base configuration to apply it.
     *
     * @param locale The new [Locale] to set.
     */
    @VisibleForTesting
    internal fun updateLocale(locale: Locale) {
        LocaleManager.setNewLocale(activity, localeUseCase, locale)
    }

    /** Resets the application's locale to the system's default locale. */
    @VisibleForTesting
    internal fun resetToSystemDefault() {
        LocaleManager.resetToSystemDefault(activity, localeUseCase)
    }

    /** Update the locale for the configuration of the app context's resources */
    @Suppress("Deprecation")
    fun updateBaseConfiguration(context: Context, locale: Locale) {
        val resources = context.applicationContext.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
