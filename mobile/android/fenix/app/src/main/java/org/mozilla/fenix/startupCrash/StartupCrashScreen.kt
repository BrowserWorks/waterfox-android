/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.startupCrash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.compose.base.button.OutlinedButton
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

@Composable
internal fun StartupCrashScreen(store: StartupCrashStore) {
    val state by store.observeAsComposableState { it }
    val scrollState = rememberScrollState()

    Surface {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = FirefoxTheme.layout.space.static200),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScreenImg()

            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static300))

            ScreenText()

            Spacer(modifier = Modifier.height(FirefoxTheme.layout.space.static400))

            when (state.uiState) {
                UiState.Idle -> {
                    ReportButtons(store)
                }

                UiState.Loading -> {
                    CircularLoadButton()
                }

                UiState.Finished -> {
                    ReopenButton(store)
                }
            }
        }
    }
}

@Composable
private fun ReportButtons(store: StartupCrashStore) {
    Column(verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static100)) {
        FilledButton(
            text = stringResource(R.string.startup_crash_positive),
            modifier = Modifier.fillMaxWidth(),
        ) {
            store.dispatch(ReportTapped)
        }

        OutlinedButton(
            text = stringResource(R.string.startup_crash_negative),
            modifier = Modifier.fillMaxWidth(),
        ) {
            store.dispatch(NoTapped)
        }
    }
}

@Composable
private fun ReopenButton(store: StartupCrashStore) {
    FilledButton(
        text =
            stringResource(
                R.string.startup_crash_restart,
                stringResource(R.string.firefox),
            ),
        modifier = Modifier.fillMaxWidth(),
        icon = painterResource(iconsR.drawable.mozac_ic_checkmark_24),
    ) {
        store.dispatch(ReopenTapped)
    }
}

@Composable
private fun CircularLoadButton() {
    FilledButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            strokeWidth = 2.dp,
            trackColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ScreenImg() {
    Image(
        modifier = Modifier.width(200.dp).height(175.dp),
        alignment = Alignment.TopCenter,
        painter = painterResource(id = R.drawable.ic_kit_plug_error),
        contentDescription = null,
    )
}

@Composable
private fun ScreenText() {
    Column(
        verticalArrangement = Arrangement.spacedBy(FirefoxTheme.layout.space.static200),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text =
                stringResource(
                    R.string.startup_crash_title,
                    stringResource(R.string.firefox),
                ),
            style = FirefoxTheme.typography.headline5,
        )

        if (BuildConfig.CRASH_REPORTING) {
            Text(
                text =
                    stringResource(
                        R.string.startup_crash_body,
                        stringResource(R.string.firefox),
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = FirefoxTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal class UiStateProvider : PreviewParameterProvider<UiState> {
    override val values: Sequence<UiState> =
        sequenceOf(
            UiState.Idle,
            UiState.Loading,
            UiState.Finished,
        )
}

@Composable
@FlexibleWindowLightDarkPreview
internal fun StartupCrashScreenPreview(@PreviewParameter(UiStateProvider::class) uiState: UiState) {
    val store = remember {
        StartupCrashStore(
            initialState = StartupCrashState(uiState),
            middleware = emptyList(),
        )
    }
    FirefoxTheme {
        StartupCrashScreen(store)
    }
}
