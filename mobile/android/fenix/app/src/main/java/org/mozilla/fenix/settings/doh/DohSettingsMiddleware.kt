/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.doh

import androidx.navigation.NavController
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store

internal class DohSettingsMiddleware(
    private val getNavController: () -> NavController,
    private val getSettingsProvider: () -> DohSettingsProvider,
    private val openUrlInBrowser: (String) -> Unit,
    private val exitDohSettings: () -> Unit,
) : Middleware<DohSettingsState, DohSettingsAction> {

    override fun invoke(
        store: Store<DohSettingsState, DohSettingsAction>,
        next: (DohSettingsAction) -> Unit,
        action: DohSettingsAction,
    ) {
        next(action)

        when (action) {
            Init -> {
                // we dispatch another action that the reducer can handle
                store.dispatch(
                    DohSettingsRootAction.DohSettingsLoaded(
                        allProtectionLevels = getSettingsProvider().getProtectionLevels(),
                        selectedProtectionLevel = getSettingsProvider().getSelectedProtectionLevel(),
                        exceptionsList = getSettingsProvider().getExceptions(),
                        providers = getSettingsProvider().getDefaultProviders(),
                        selectedProvider = getSettingsProvider().getSelectedProvider(),
                    ),
                )
            }

            is BackClicked -> {
                if (!getNavController().popBackStack()) {
                    exitDohSettings()
                }
            }

            is LearnMoreClicked -> {
                openUrlInBrowser(action.url)
            }

            is DohSettingsRootAction.ExceptionsClicked -> {
                getNavController().navigate(DohSettingsDestinations.EXCEPTIONS_LIST)
            }

            is DohSettingsRootAction.DohOptionSelected -> {
                getSettingsProvider().setProtectionLevel(action.protectionLevel, action.provider)
            }

            is DohSettingsRootAction.DohCustomProviderDialogAction.AddCustomClicked -> {
                handleAddCustomProvider(store, action)
            }

            is DohSettingsRootAction.DefaultInfoClicked -> {
                getNavController().navigate(DohSettingsDestinations.INFO_DEFAULT)
            }

            is DohSettingsRootAction.IncreasedInfoClicked -> {
                getNavController().navigate(DohSettingsDestinations.INFO_INCREASED)
            }

            is DohSettingsRootAction.MaxInfoClicked -> {
                getNavController().navigate(DohSettingsDestinations.INFO_MAX)
            }

            is DohSettingsRootAction.UltraInfoClicked -> {
                getNavController().navigate(DohSettingsDestinations.INFO_ULTRA)
            }

            is ExceptionsAction.AddExceptionsClicked -> {
                getNavController().navigate(DohSettingsDestinations.ADD_EXCEPTION)
            }

            is ExceptionsAction.RemoveClicked -> {
                handleRemoveException(store, action)
            }

            is ExceptionsAction.RemoveAllClicked -> {
                getSettingsProvider().setExceptions(emptyList())
                store.dispatch(
                    ExceptionsAction.ExceptionsUpdated(
                        emptyList(),
                    ),
                )
            }

            is ExceptionsAction.SaveClicked -> {
                handleSaveException(store, action)
            }

            else -> {}
        }
    }

    private fun handleAddCustomProvider(
        store: Store<DohSettingsState, DohSettingsAction>,
        action: DohSettingsRootAction.DohCustomProviderDialogAction.AddCustomClicked,
    ) {
        try {
            val normalizedUrl = DohUrlValidator.validate(action.url)
            getSettingsProvider().setCustomProvider(normalizedUrl)
            store.dispatch(
                DohSettingsRootAction.DohCustomProviderDialogAction.ValidUrlDetected(
                    action.customProvider,
                    normalizedUrl,
                ),
            )
        } catch (e: UrlValidationException.NonHttpsUrlException) {
            store.dispatch(
                DohSettingsRootAction.DohCustomProviderDialogAction.NonHttpsUrlDetected,
            )
        } catch (e: UrlValidationException.InvalidUrlException) {
            store.dispatch(
                DohSettingsRootAction.DohCustomProviderDialogAction.InvalidUrlDetected,
            )
        }
    }

    private fun handleRemoveException(
        store: Store<DohSettingsState, DohSettingsAction>,
        action: ExceptionsAction.RemoveClicked,
    ) {
        val updatedExceptions =
            getSettingsProvider().getExceptions().filter { it != action.url }
        getSettingsProvider().setExceptions(updatedExceptions)
        store.dispatch(
            ExceptionsAction.ExceptionsUpdated(
                updatedExceptions,
            ),
        )
    }

    private fun handleSaveException(
        store: Store<DohSettingsState, DohSettingsAction>,
        action: ExceptionsAction.SaveClicked,
    ) {
        val url = DohUrlValidator.dropScheme(action.url)
        val currExceptions = getSettingsProvider().getExceptions()

        // If the url is already in the list, just exit AddExceptionScreen
        if (currExceptions.contains(url)) {
            store.dispatch(
                BackClicked,
            )
            return
        }

        try {
            DohUrlValidator.validate("https://$url")
            val updatedExceptions = currExceptions + url
            getSettingsProvider().setExceptions(updatedExceptions)
            store.dispatch(
                ExceptionsAction.ExceptionsUpdated(updatedExceptions),
            )
            store.dispatch(
                BackClicked,
            )
        } catch (e: UrlValidationException.InvalidUrlException) {
            store.dispatch(ExceptionsAction.InvalidUrlDetected)
        }
    }
}
