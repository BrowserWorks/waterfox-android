/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.doh.root

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.Dropdown
import mozilla.components.compose.base.LinkText
import mozilla.components.compose.base.LinkTextState
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.button.RadioButton
import mozilla.components.compose.base.button.TextButton
import mozilla.components.compose.base.menu.MenuItem
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.base.textfield.TextField
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.IconListItem
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.settings.doh.CustomProviderErrorState
import org.mozilla.fenix.settings.doh.DohSettingsState
import org.mozilla.fenix.settings.doh.ProtectionLevel
import org.mozilla.fenix.settings.doh.Provider
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import mozilla.components.ui.icons.R as iconsR

/**
 * Composable function that displays the root screen of DoH settings.
 *
 * @param state The current [DohSettingsState].
 * @param onLearnMoreClicked Invoked when the user wants to visit an external doc about DoH.
 * @param onExceptionsClicked Invoked when the user wants to manage exceptions.
 * @param onDohOptionSelected Invoked when the user selects a protection level.
 * @param onCustomClicked Invoked when the user chooses to configure a custom provider (dialog).
 * @param onCustomCancelClicked Invoked when the user exits the custom provider dialog.
 * @param onCustomAddClicked Invoked when the user adds a custom provider in the dialog.
 * @param onDefaultInfoClicked Invoked when the user accesses info about Default DoH level.
 * @param onIncreasedInfoClicked Invoked when the user accesses info about Increased DoH level.
 * @param onMaxInfoClicked Invoked when the user accesses info about Max DoH level.
 * @param onUltraInfoClicked Invoked when the user accesses info about Ultra DoH level.
 */
@Composable
internal fun DohSettingsScreen(
    state: DohSettingsState,
    onLearnMoreClicked: (String) -> Unit = {},
    onExceptionsClicked: () -> Unit = {},
    onDohOptionSelected: (ProtectionLevel, Provider?) -> Unit = { _: ProtectionLevel, _: Provider? -> },
    onCustomClicked: () -> Unit = {},
    onCustomCancelClicked: () -> Unit = {},
    onCustomAddClicked: (Provider.Custom, String) -> Unit = { _: Provider, _: String -> },
    onDefaultInfoClicked: () -> Unit = {},
    onIncreasedInfoClicked: () -> Unit = {},
    onMaxInfoClicked: () -> Unit = {},
    onUltraInfoClicked: () -> Unit = {},
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            DohSummary(
                onLearnMoreClicked = onLearnMoreClicked,
            )

            DohSelection(
                state = state,
                onDohOptionSelected = onDohOptionSelected,
                onCustomClicked = onCustomClicked,
                onCustomCancelClicked = onCustomCancelClicked,
                onCustomAddClicked = onCustomAddClicked,
                onDefaultInfoClicked = onDefaultInfoClicked,
                onIncreasedInfoClicked = onIncreasedInfoClicked,
                onMaxInfoClicked = onMaxInfoClicked,
                onUltraInfoClicked = onUltraInfoClicked,
            )

            HorizontalDivider(
                modifier = Modifier.padding(8.dp),
            )

            ExceptionsRow(onExceptionsClicked = onExceptionsClicked)
        }
    }
}

@Composable
private fun DohSummary(
    onLearnMoreClicked: (String) -> Unit,
) {
    val summary = stringResource(
        R.string.preference_doh_summary,
        stringResource(id = R.string.preference_doh_learn_more),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                style = FirefoxTheme.typography.body1,
                text = stringResource(R.string.preference_doh_title),
            )

            LinkText(
                text = summary,
                linkTextStates = listOf(
                    LinkTextState(
                        text = stringResource(R.string.preference_doh_learn_more),
                        url = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.DNS_OVER_HTTPS),
                        onClick = {
                            onLearnMoreClicked(it)
                        },
                    ),
                ),
                linkTextDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Start,
            )
        }
    }
}

/**
 * Protection level composable - used for all levels of protection
 */
@Composable
private fun DohProtectionLevel(
    modifier: Modifier = Modifier,
    selected: Boolean,
    label: String,
    summary: String,
    showInfoIcon: Boolean,
    provider: @Composable (() -> Unit)? = null,
    onInfoClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                start = 72.dp,
                top = 6.dp,
                end = 16.dp,
                bottom = 6.dp,
            ),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            ProviderSummary(label, summary)

            provider?.invoke()
        }

        if (showInfoIcon) {
            IconButton(
                onClick = onInfoClick,
                contentDescription = stringResource(R.string.preference_doh_info_description),
            ) {
                Icon(
                    painter = painterResource(iconsR.drawable.mozac_ic_information_24),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun DohSelection(
    state: DohSettingsState,
    onDohOptionSelected: (ProtectionLevel, Provider?) -> Unit = { _: ProtectionLevel, _: Provider? -> },
    onCustomClicked: () -> Unit,
    onCustomCancelClicked: () -> Unit,
    onCustomAddClicked: (Provider.Custom, String) -> Unit,
    onDefaultInfoClicked: () -> Unit,
    onIncreasedInfoClicked: () -> Unit,
    onMaxInfoClicked: () -> Unit,
    onUltraInfoClicked: () -> Unit,
) {
    state.allProtectionLevels.forEach { protectionLevel ->
        when (protectionLevel) {
            is ProtectionLevel.Default -> DohProtectionLevel(
                modifier = Modifier.fillMaxWidth(),
                selected = protectionLevel == state.selectedProtectionLevel,
                label = stringResource(R.string.preference_doh_default_protection),
                summary = stringResource(
                    R.string.preference_doh_default_protection_summary,
                    stringResource(id = R.string.app_name),
                ),
                showInfoIcon = true,
                onInfoClick = onDefaultInfoClicked,
                onClick = {
                    onDohOptionSelected(protectionLevel, null)
                },
            )

            is ProtectionLevel.Increased -> DohProtectionLevel(
                modifier = Modifier.fillMaxWidth(),
                selected = protectionLevel == state.selectedProtectionLevel,
                label = stringResource(R.string.preference_doh_increased_protection),
                summary = stringResource(R.string.preference_doh_increased_protection_summary),
                showInfoIcon = true,
                provider = if (protectionLevel == state.selectedProtectionLevel) {
                    {
                        state.selectedProvider?.let {
                            ProviderDropdown(
                                selectedProviderOption = it,
                                onProviderSelected = { provider ->
                                    onDohOptionSelected(
                                        protectionLevel,
                                        provider,
                                    )
                                },
                                providers = state.providers,
                                onCustomClicked = onCustomClicked,
                            )
                        }
                    }
                } else {
                    null
                },
                onInfoClick = onIncreasedInfoClicked,
                onClick = {
                    onDohOptionSelected(
                        protectionLevel,
                        state.selectedProvider ?: state.providers.first(),
                    )
                },
            )

            is ProtectionLevel.Max -> DohProtectionLevel(
                modifier = Modifier.fillMaxWidth(),
                selected = protectionLevel == state.selectedProtectionLevel,
                label = stringResource(R.string.preference_doh_max_protection),
                summary = stringResource(
                    R.string.preference_doh_max_protection_summary,
                    stringResource(id = R.string.app_name),
                ),
                showInfoIcon = true,
                provider = if (protectionLevel == state.selectedProtectionLevel) {
                    {
                        state.selectedProvider?.let {
                            ProviderDropdown(
                                selectedProviderOption = it,
                                onProviderSelected = { provider ->
                                    onDohOptionSelected(
                                        protectionLevel,
                                        provider,
                                    )
                                },
                                providers = state.providers,
                                onCustomClicked = onCustomClicked,
                            )
                        }
                    }
                } else {
                    null
                },
                onInfoClick = onMaxInfoClicked,
                onClick = {
                    onDohOptionSelected(
                        protectionLevel,
                        state.selectedProvider ?: state.providers.first(),
                    )
                },
            )

            is ProtectionLevel.Ultra -> DohProtectionLevel(
                modifier = Modifier.fillMaxWidth(),
                selected = protectionLevel == state.selectedProtectionLevel,
                label = stringResource(R.string.preference_doh_ultra_protection),
                summary = stringResource(
                    R.string.preference_doh_ultra_protection_summary,
                    stringResource(id = R.string.app_name),
                ),
                showInfoIcon = true,
                provider = null,
                onInfoClick = onUltraInfoClicked,
                onClick = {
                    onDohOptionSelected(
                        protectionLevel,
                        null,
                    )
                },
            )

            is ProtectionLevel.Off -> DohProtectionLevel(
                modifier = Modifier.fillMaxWidth(),
                selected = protectionLevel == state.selectedProtectionLevel,
                label = stringResource(R.string.preference_doh_off),
                summary = stringResource(R.string.preference_doh_off_summary),
                showInfoIcon = false,
                onClick = {
                    onDohOptionSelected(protectionLevel, null)
                },
            )
        }
    }

    if (state.selectedProvider is Provider.Custom && state.isCustomProviderDialogOn) {
        AlertDialogAddCustomProvider(
            customProviderUrl = state.selectedProvider.url,
            customProviderErrorState = state.customProviderErrorState,
            onCustomCancelClicked = { onCustomCancelClicked() },
            onCustomAddClicked = { url ->
                onCustomAddClicked(state.selectedProvider, url)
            },
        )
    }
}

@Composable
private fun ProviderSummary(
    label: String,
    summary: String,
) {
    Text(
        text = label,
        style = FirefoxTheme.typography.body1,
    )

    Text(
        text = summary,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = FirefoxTheme.typography.body2,
    )
}

@Composable
private fun ProviderDropdown(
    selectedProviderOption: Provider,
    onProviderSelected: (Provider) -> Unit = {},
    providers: List<Provider>,
    onCustomClicked: () -> Unit,
) {
    val customText = stringResource(R.string.preference_doh_provider_custom)
    val defaultText = stringResource(R.string.preference_doh_provider_default)

    val placeholder = if (selectedProviderOption is Provider.BuiltIn) {
        selectedProviderOption.name + if (selectedProviderOption.default) " $defaultText" else ""
    } else {
        customText
    }

    val dropdownItems = buildProviderMenuItems(
        providers = providers,
        selectedProvider = selectedProviderOption,
        customText = customText,
        defaultText = defaultText,
        onProviderSelected = onProviderSelected,
        onCustomClicked = onCustomClicked,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Dropdown(
            label = stringResource(R.string.preference_doh_choose_provider),
            placeholder = placeholder,
            dropdownItems = dropdownItems,
        )
    }

    if (selectedProviderOption is Provider.Custom) {
        TextWithUnderline(
            text = selectedProviderOption.url,
            showCustomProviderDialog = { onCustomClicked() },
        )
    }
}

/**
 * Returns a list of [MenuItem.CheckableItem] based on the providers.
 */
private fun buildProviderMenuItems(
    providers: List<Provider>,
    selectedProvider: Provider,
    customText: String,
    defaultText: String,
    onProviderSelected: (Provider) -> Unit,
    onCustomClicked: () -> Unit,
): List<MenuItem.CheckableItem> {
    return providers.map { provider ->
        // Determine the label to display
        val text = when (provider) {
            is Provider.BuiltIn -> {
                provider.name + if (provider.default) " $defaultText" else ""
            }

            is Provider.Custom -> customText
        }

        MenuItem.CheckableItem(
            text = Text.String(text),
            isChecked = (provider == selectedProvider),
            onClick = {
                onProviderSelected(provider)
                if (provider is Provider.Custom) {
                    onCustomClicked()
                }
            },
        )
    }
}

@Composable
private fun AlertDialogAddCustomProvider(
    customProviderUrl: String,
    customProviderErrorState: CustomProviderErrorState,
    onCustomCancelClicked: () -> Unit,
    onCustomAddClicked: (String) -> Unit,
) {
    var customProviderInput by remember { mutableStateOf(customProviderUrl) }
    val onCustomProviderInputChange: (String) -> Unit = { it -> customProviderInput = it }
    val nonHttpsString = stringResource(R.string.preference_doh_provider_custom_dialog_error_https)
    val invalidString = stringResource(R.string.preference_doh_provider_custom_dialog_error_invalid)

    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.preference_doh_provider_custom_dialog_title),
                style = FirefoxTheme.typography.headline5,
            )
        },
        text = {
            TextField(
                value = customProviderInput,
                onValueChange = {
                    onCustomProviderInputChange(it)
                },
                placeholder = "",
                errorText = when (customProviderErrorState) {
                    CustomProviderErrorState.NonHttps -> nonHttpsString
                    CustomProviderErrorState.Invalid -> invalidString
                    else -> ""
                },
                label = stringResource(R.string.preference_doh_provider_custom_dialog_textfield),
                isError = customProviderErrorState != CustomProviderErrorState.Valid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        onDismissRequest = onCustomCancelClicked,
        confirmButton = {
            TextButton(
                text = stringResource(R.string.preference_doh_provider_custom_dialog_add),
                onClick = { onCustomAddClicked(customProviderInput) },
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.preference_doh_provider_custom_dialog_cancel),
                onClick = onCustomCancelClicked,
            )
        },
    )
}

@Composable
private fun TextWithUnderline(
    text: String,
    modifier: Modifier = Modifier,
    showCustomProviderDialog: () -> Unit = {},
    underlineColor: Color = MaterialTheme.colorScheme.outline,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showCustomProviderDialog()
                },
            style = FirefoxTheme.typography.body2,
            text = text,
        )

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
            color = underlineColor,
        )
    }
}

@Composable
private fun ExceptionsRow(onExceptionsClicked: () -> Unit) {
    IconListItem(
        label = stringResource(R.string.preference_doh_exceptions),
        onClick = onExceptionsClicked,
        beforeIconPainter = painterResource(iconsR.drawable.mozac_ic_globe_24),
        beforeIconDescription = stringResource(R.string.preference_doh_exceptions_description),
    )
}

@FlexibleWindowPreview
@Composable
private fun DohScreenDefaultProviderPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        val provider = Provider.BuiltIn(
            url = "https://mozilla.cloudflare-dns.com/dns-query",
            name = "Cloudflare",
            default = true,
        )
        DohSettingsScreen(
            state = DohSettingsState(
                allProtectionLevels = listOf(
                    ProtectionLevel.Default,
                    ProtectionLevel.Increased,
                    ProtectionLevel.Max,
                    ProtectionLevel.Ultra,
                    ProtectionLevel.Off,
                ),
                selectedProtectionLevel = ProtectionLevel.Increased,
                providers = listOf(
                    provider,
                ),
                selectedProvider = provider,
                exceptionsList = emptyList(),
                isUserExceptionValid = true,
            ),
        )
    }
}

@FlexibleWindowPreview
@Composable
private fun DohScreenCustomProviderPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        val provider = Provider.Custom(url = "")
        DohSettingsScreen(
            state = DohSettingsState(
                allProtectionLevels = listOf(
                    ProtectionLevel.Default,
                    ProtectionLevel.Increased,
                    ProtectionLevel.Max,
                    ProtectionLevel.Ultra,
                    ProtectionLevel.Off,
                ),
                selectedProtectionLevel = ProtectionLevel.Increased,
                providers = listOf(
                    provider,
                ),
                selectedProvider = provider,
                exceptionsList = emptyList(),
                isUserExceptionValid = true,
            ),
        )
    }
}

@Preview
@Composable
private fun AlertDialogAddCustomProviderPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        AlertDialogAddCustomProvider(
            customProviderUrl = "https://mozilla.cloudflare-dns.com/dns-query",
            customProviderErrorState = CustomProviderErrorState.Valid,
            onCustomCancelClicked = {},
            onCustomAddClicked = {},
        )
    }
}

@Preview
@Composable
private fun AlertDialogAddCustomProviderErrorPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        AlertDialogAddCustomProvider(
            customProviderUrl = "https://mozilla.cloudflare-dns.com/dns-query",
            customProviderErrorState = CustomProviderErrorState.Invalid,
            onCustomCancelClicked = {},
            onCustomAddClicked = {},
        )
    }
}
