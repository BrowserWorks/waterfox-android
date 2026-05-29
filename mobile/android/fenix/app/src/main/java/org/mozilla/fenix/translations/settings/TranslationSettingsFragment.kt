/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.translations.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.GleanMetrics.Translations
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.translations.TranslationSettingsScreenOption
import org.mozilla.fenix.translations.TranslationSwitchItem
import org.mozilla.fenix.translations.TranslationsAIControllableFeature
import org.mozilla.fenix.translations.TranslationsDialogAccessPoint
import org.mozilla.fenix.translations.TranslationsEnabledSettings

/**
 * A fragment displaying the Firefox Translation settings screen.
 */
class TranslationSettingsFragment : Fragment(), UserInteractionHandler, SystemInsetsPaddedFragment {
    private val browserStore: BrowserStore by lazy { requireComponents.core.store }

    private val translationsFeature by lazy {
        TranslationsAIControllableFeature(
            settings = TranslationsEnabledSettings.dataStore(requireContext()),
            browserStore = browserStore,
        )
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.translation_settings_toolbar_title))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        FirefoxTheme {
            val scope = rememberCoroutineScope()
            val translationsState by browserStore.observeAsComposableState { it.translationEngine }
            val switchItems: List<TranslationSwitchItem> = getTranslationSwitchItemList()
            val settingsState = remember(translationsState.isTranslationsEnabled, switchItems) {
                TranslationsSettingsState(
                    showAutomaticTranslations = FxNimbus.features.translations.value().globalLangSettingsEnabled,
                    showNeverTranslate = FxNimbus.features.translations.value().globalSiteSettingsEnabled,
                    showDownloads = FxNimbus.features.translations.value().downloadsEnabled,
                    translationsEnabled = translationsState.isTranslationsEnabled,
                    switchItems = switchItems,
                )
            }
            TranslationSettings(
                state = settingsState,
                pageSettingsError = browserStore.observeAsComposableState { state ->
                    state.selectedTab?.translationsState?.settingsError
                }.value,
                onAutomaticTranslationClicked = {
                    Translations.action.record(Translations.ActionExtra("global_lang_settings"))
                    findNavController().navigate(
                        TranslationSettingsFragmentDirections
                            .actionTranslationSettingsFragmentToAutomaticTranslationPreferenceFragment(),
                    )
                },
                onNeverTranslationClicked = {
                    Translations.action.record(Translations.ActionExtra("global_site_settings"))
                    findNavController().navigate(
                        TranslationSettingsFragmentDirections
                            .actionTranslationSettingsToNeverTranslateSitePreference(),
                    )
                },
                onDownloadLanguageClicked = {
                    Translations.action.record(Translations.ActionExtra("downloads"))
                    findNavController().navigate(
                        TranslationSettingsFragmentDirections
                            .actionTranslationSettingsFragmentToDownloadLanguagesPreferenceFragment(),
                    )
                },
                onFeatureControlToggled = { enabled ->
                    scope.launch {
                        translationsFeature.set(enabled)
                    }
                },
                onNavigateToUrl = { url ->
                    SupportUtils.launchSandboxCustomTab(
                        context = requireContext(),
                        url = url,
                    )
                },
            )
        }
    }

    /**
     * Set the switch item values.
     * The first one is based on [mozilla.components.browser.state.state.TranslationsBrowserState.offerTranslation].
     * The second one is [DownloadLanguageFileDialog] visibility.
     * This pop-up will appear if the switch item is unchecked, the phone is in saving mode, and
     * doesn't have a WiFi connection.
     */
    @Composable
    private fun getTranslationSwitchItemList(): MutableList<TranslationSwitchItem> {
        val offerToTranslate = browserStore.observeAsComposableState { state ->
            state.translationEngine.offerTranslation
        }.value ?: requireComponents.settings.offerTranslation
        val translationSwitchItems = mutableListOf<TranslationSwitchItem>()

        translationSwitchItems.add(
            TranslationSwitchItem(
                type = TranslationSettingsScreenOption.OfferToTranslate(
                    hasDivider = false,
                ),
                textLabel = stringResource(R.string.translation_settings_offer_to_translate),
                isChecked = offerToTranslate,
                isEnabled = true,
                onStateChange = { _, checked ->
                    browserStore.dispatch(
                        TranslationsAction.UpdateGlobalOfferTranslateSettingAction(
                            offerTranslation = checked,
                        ),
                    )
                    // Ensures persistence of value
                    requireComponents.settings.offerTranslation = checked
                },
            ),
        )

        var isDownloadInSavingModeChecked by remember {
            mutableStateOf(requireComponents.settings.ignoreTranslationsDataSaverWarning)
        }

        translationSwitchItems.add(
            TranslationSwitchItem(
                type = TranslationSettingsScreenOption.AlwaysDownloadInSavingMode(
                    hasDivider = true,
                ),
                textLabel = stringResource(R.string.translation_settings_always_download),
                isChecked = isDownloadInSavingModeChecked,
                isEnabled = true,
                onStateChange = { _, checked ->
                    isDownloadInSavingModeChecked = checked
                    requireComponents.settings.ignoreTranslationsDataSaverWarning = checked
                },
            ),
        )
        return translationSwitchItems
    }

    override fun onBackPressed(): Boolean {
        return if (findNavController().previousBackStackEntry?.destination?.id == R.id.browserFragment) {
            if (browserStore.state.translationEngine.isTranslationsEnabled) {
                findNavController().navigate(
                    TranslationSettingsFragmentDirections.actionTranslationSettingsFragmentToTranslationsDialogFragment(
                        translationsDialogAccessPoint = TranslationsDialogAccessPoint.TranslationsOptions,
                    ),
                )
            } else {
                findNavController().popBackStack(R.id.browserFragment, false)
            }
            true
        } else {
            false
        }
    }
}
