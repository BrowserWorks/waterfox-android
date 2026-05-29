/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.translations

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.R as materialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.translate.Language
import mozilla.components.concept.engine.translate.TranslationError
import mozilla.components.feature.downloads.FileSizeFormatter
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.GleanMetrics.Translations
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.openToBrowser
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.runIfFragmentIsAttached
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.translations.preferences.downloadlanguages.DownloadLanguageFileDialog
import org.mozilla.fenix.translations.preferences.downloadlanguages.DownloadLanguageFileDialogType
import org.mozilla.fenix.translations.preferences.downloadlanguages.DownloadLanguagesFeature

/** The enum is to know what bottom sheet to open. */
enum class TranslationsDialogAccessPoint {
    Translations,
    TranslationsOptions,
}

/** A bottom sheet fragment displaying the Firefox Translation dialog. */
class TranslationsDialogFragment : BottomSheetDialogFragment() {

    private val args by navArgs<TranslationsDialogFragmentArgs>()

    private val appStore: AppStore by lazy { requireComponents.appStore }
    private val browserStore: BrowserStore by lazy { requireComponents.core.store }

    private val translationDialogBinding = ViewBoundFeatureWrapper<TranslationsDialogBinding>()
    private val downloadLanguagesFeature = ViewBoundFeatureWrapper<DownloadLanguagesFeature>()

    private lateinit var translationsDialogStore: TranslationsDialogStore

    private var behavior: BottomSheetBehavior<View>? = null
    private var isTranslationInProgress: Boolean? = null
    private var isDataSaverEnabledAndWifiDisabled = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                runIfFragmentIsAttached {
                    val bottomSheet = findViewById<View?>(materialR.id.design_bottom_sheet)
                    bottomSheet?.let {
                        it.setBackgroundResource(android.R.color.transparent)
                        behavior = BottomSheetBehavior.from(it)
                        behavior?.peekHeight = resources.displayMetrics.heightPixels
                        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        behavior?.skipCollapsed = true
                    }
                }
            }
        }

    @Suppress("LongMethod", "CognitiveComplexMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        translationsDialogStore =
            TranslationsDialogStore(
                TranslationsDialogState(),
                listOf(
                    TranslationsDialogMiddleware(
                        browserStore = browserStore,
                        settings = requireComponents.settings,
                        sessionId = args.sessionId,
                    )
                ),
            )
        return content {
            FirefoxTheme {
                var translationsVisibility by remember {
                    mutableStateOf(args.translationsDialogAccessPoint == TranslationsDialogAccessPoint.Translations)
                }

                val translationsDialogState = translationsDialogStore.observeAsComposableState { it }.value

                val learnMoreUrl =
                    SupportUtils.getSumoURLForTopic(
                        requireContext(),
                        SupportUtils.SumoTopic.TRANSLATIONS,
                    )

                isTranslationInProgress = translationsDialogState.isTranslationInProgress

                if (translationsDialogState.dismissDialogState is DismissDialogState.Dismiss) {
                    dismissDialog()
                }

                var showDownloadLanguageFileDialog by remember {
                    mutableStateOf(false)
                }

                var revertAlwaysTranslateLanguageCheckBox by remember {
                    mutableStateOf(false)
                }
                TranslationDialogBottomSheet(
                    onRequestDismiss = { behavior?.state = BottomSheetBehavior.STATE_HIDDEN }
                ) {
                    TranslationsAnimation(showMainSheet = translationsVisibility) { showMainPage ->
                        if (showMainPage) {
                            Column {
                                TranslationsDialogContent(
                                    learnMoreUrl = learnMoreUrl,
                                    showPageSettings = true,
                                    translationsDialogState = translationsDialogState,
                                    onSettingClicked = {
                                        Translations.action.record(Translations.ActionExtra("page_settings"))
                                        translationsVisibility = false
                                    },
                                    onShowDownloadLanguageFileDialog = {
                                        showDownloadLanguageFileDialog = true
                                    },
                                )
                            }
                        } else {
                            Column {
                                TranslationsOptionsDialogContent(
                                    learnMoreUrl = learnMoreUrl,
                                    showGlobalSettings = true,
                                    isTranslated = translationsDialogState.isTranslated == true,
                                    initialFrom = translationsDialogState.initialFrom,
                                    onBackClicked = { translationsVisibility = true },
                                    onTranslate = {
                                        translate(
                                            translationsDialogState = translationsDialogState,
                                            onShowDownloadLanguageFileDialog = {
                                                showDownloadLanguageFileDialog = true
                                                revertAlwaysTranslateLanguageCheckBox = true
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (showDownloadLanguageFileDialog) {
                        translationsDialogState.translationDownloadSize?.size?.let { fileSize ->
                            DownloadLanguageFileDialog(
                                fileSize = fileSize,
                                fileSizeFormatter = requireComponents.core.fileSizeFormatter,
                                onConfirmDownload = {
                                    showDownloadLanguageFileDialog = false
                                },
                                onCancel = {
                                    showDownloadLanguageFileDialog = false
                                    if (revertAlwaysTranslateLanguageCheckBox) {
                                        translationsDialogStore.dispatch(
                                            TranslationsDialogAction.UpdatePageSettingsValue(
                                                TranslationPageSettingsOption.AlwaysTranslateLanguage(),
                                                false,
                                            )
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        translationDialogBinding.set(
            feature =
                TranslationsDialogBinding(
                    browserStore = browserStore,
                    sessionId = args.sessionId,
                    translationsDialogStore = translationsDialogStore,
                    getTranslatedPageTitle = { localizedFrom, localizedTo ->
                        requireContext()
                            .getString(
                                R.string.translations_bottom_sheet_title_translation_completed,
                                localizedFrom,
                                localizedTo,
                            )
                    },
                ),
            owner = this,
            view = view,
        )
        translationsDialogStore.dispatch(TranslationsDialogAction.InitTranslationsDialog)

        downloadLanguagesFeature.set(
            feature =
                DownloadLanguagesFeature(
                    context = requireContext(),
                    wifiConnectionMonitor = requireContext().components.wifiConnectionMonitor,
                    onDataSaverAndWifiChanged = {
                        isDataSaverEnabledAndWifiDisabled = it
                    },
                ),
            owner = this,
            view = view,
        )
    }

    @Composable
    private fun TranslationsDialogContent(
        learnMoreUrl: String,
        showPageSettings: Boolean,
        translationsDialogState: TranslationsDialogState,
        onSettingClicked: () -> Unit,
        onShowDownloadLanguageFileDialog: () -> Unit,
    ) {
        val localView = LocalView.current

        TranslationsDialog(
            translationsDialogState = translationsDialogState,
            learnMoreUrl = learnMoreUrl,
            showPageSettings = showPageSettings,
            showFirstTime = requireComponents.settings.showFirstTimeTranslation,
            onSettingClicked = onSettingClicked,
            onLearnMoreClicked = { openBrowserAndLoad(learnMoreUrl) },
            onPositiveButtonClicked = {
                translate(
                    translationsDialogState = translationsDialogState,
                    onShowDownloadLanguageFileDialog = onShowDownloadLanguageFileDialog,
                )
            },
            onNegativeButtonClicked = {
                if (translationsDialogState.isTranslated || translationsDialogState.isTranslationInProgress) {
                    @Suppress("DEPRECATION")
                    localView.announceForAccessibility(
                        requireContext()
                            .getString(R.string.translations_bottom_sheet_restore_accessibility_announcement)
                    )
                    isTranslationInProgress = false
                    translationsDialogStore.dispatch(TranslationsDialogAction.RestoreTranslation)
                }
                dismiss()
            },
            onFromSelected = { fromLanguage ->
                translationsDialogState.initialTo?.let {
                    translationsDialogStore.dispatch(
                        TranslationsDialogAction.FetchDownloadFileSizeAction(
                            toLanguage = it,
                            fromLanguage = fromLanguage,
                        )
                    )
                }

                translationsDialogStore.dispatch(TranslationsDialogAction.UpdateFromSelectedLanguage(fromLanguage))
            },
            onToSelected = { toLanguage ->
                translationsDialogState.initialFrom?.let {
                    translationsDialogStore.dispatch(
                        TranslationsDialogAction.FetchDownloadFileSizeAction(
                            toLanguage = toLanguage,
                            fromLanguage = it,
                        )
                    )
                }

                translationsDialogStore.dispatch(TranslationsDialogAction.UpdateToSelectedLanguage(toLanguage))
            },
        )
    }

    private fun translate(
        translationsDialogState: TranslationsDialogState? = null,
        onShowDownloadLanguageFileDialog: () -> Unit,
    ) {
        if (translationsDialogState?.error is TranslationError.CouldNotLoadLanguagesError) {
            translationsDialogStore.dispatch(TranslationsDialogAction.FetchSupportedLanguages)
        } else {
            if (
                isDataSaverEnabledAndWifiDisabled &&
                    !requireComponents.settings.ignoreTranslationsDataSaverWarning &&
                    translationsDialogState?.translationDownloadSize != null
            ) {
                onShowDownloadLanguageFileDialog()
            } else {
                translationsDialogStore.dispatch(TranslationsDialogAction.TranslateAction)
            }
        }
    }

    @Composable
    private fun DownloadLanguageFileDialog(
        fileSize: Long,
        fileSizeFormatter: FileSizeFormatter,
        onConfirmDownload: () -> Unit,
        onCancel: () -> Unit,
    ) {
        var checkBoxEnabled by remember { mutableStateOf(false) }
        DownloadLanguageFileDialog(
            downloadLanguageDialogType = DownloadLanguageFileDialogType.TranslationRequest,
            fileSizeFormatter = fileSizeFormatter,
            fileSize = fileSize,
            isCheckBoxEnabled = checkBoxEnabled,
            onSavingModeStateChange = { checkBoxEnabled = it },
            onConfirmDownload = {
                requireComponents.settings.ignoreTranslationsDataSaverWarning = checkBoxEnabled
                onConfirmDownload()
                translationsDialogStore.dispatch(TranslationsDialogAction.TranslateAction)
            },
            onCancel = { onCancel() },
        )
    }

    @Composable
    private fun TranslationsOptionsDialogContent(
        learnMoreUrl: String,
        showGlobalSettings: Boolean,
        isTranslated: Boolean,
        initialFrom: Language? = null,
        onBackClicked: () -> Unit,
        onTranslate: () -> Unit,
    ) {
        val pageSettingsState =
            browserStore
                .observeAsComposableState { state ->
                    state.findTabOrCustomTabOrSelectedTab(args.sessionId)?.translationsState?.pageSettings
                }
                .value

        val offerTranslation =
            browserStore
                .observeAsComposableState { state ->
                    state.translationEngine.offerTranslation
                }
                .value

        val pageSettingsError =
            browserStore
                .observeAsComposableState { state ->
                    state.findTabOrCustomTabOrSelectedTab(args.sessionId)?.translationsState?.settingsError
                }
                .value

        val localView = LocalView.current

        TranslationsOptionsDialog(
            context = requireContext(),
            translationPageSettings = pageSettingsState,
            translationPageSettingsError = pageSettingsError,
            offerTranslation = offerTranslation,
            showGlobalSettings = showGlobalSettings,
            initialFrom = initialFrom,
            onStateChange = { type, checked ->
                if (type is TranslationPageSettingsOption.AlwaysTranslateLanguage && checked && !isTranslated) {
                    onTranslate()
                }
                translationsDialogStore.dispatch(
                    TranslationsDialogAction.UpdatePageSettingsValue(
                        type as TranslationPageSettingsOption,
                        checked,
                    )
                )

                if (checked) {
                    @Suppress("DEPRECATION")
                    localView.announceForAccessibility(type.descriptionId?.let { getString(it) })
                }
            },
            onBackClicked = onBackClicked,
            onTranslationSettingsClicked = {
                Translations.action.record(Translations.ActionExtra("global_settings"))
                findNavController()
                    .navigate(
                        TranslationsDialogFragmentDirections
                            .actionTranslationsDialogFragmentToTranslationSettingsFragment()
                    )
            },
            aboutTranslationClicked = {
                openBrowserAndLoad(learnMoreUrl)
            },
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        if (isTranslationInProgress == true) {
            appStore.dispatch(
                AppAction.TranslationsAction.TranslationStarted(
                    sessionId = browserStore.state.findTabOrCustomTabOrSelectedTab(args.sessionId)?.id
                )
            )
        }
    }

    private fun openBrowserAndLoad(learnMoreUrl: String) {
        val sessionId = args.sessionId
        when (sessionId != null && browserStore.state.findCustomTab(sessionId) != null) {
            true -> {
                requireComponents.useCases.sessionUseCases.loadUrl(
                    url = learnMoreUrl,
                    sessionId = sessionId,
                )
                dismiss()
                return
            }

            else -> {
                findNavController().openToBrowser()
                requireComponents.useCases.fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = learnMoreUrl,
                    newTab = true,
                )
            }
        }
    }

    private fun dismissDialog() {
        if (requireComponents.settings.showFirstTimeTranslation) {
            requireComponents.settings.showFirstTimeTranslation = false
        }
        dismiss()
    }
}
