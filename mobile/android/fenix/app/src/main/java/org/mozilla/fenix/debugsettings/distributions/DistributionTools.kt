/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.distributions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.compose.base.button.FilledButton
import org.mozilla.fenix.R
import org.mozilla.fenix.components.components
import org.mozilla.fenix.distributions.DefaultDistributionProviderChecker
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme

/**
 * Distribution UI for the debug drawer that displays various distribution related tools.
 */

@Composable
fun DistributionTools() {
    val context = LocalContext.current
    val stateFlow = components.core.store.stateFlow

    val distributionId: String by remember {
        stateFlow.map { it.distributionId ?: "" }
    }.collectAsState(initial = "")

    val coroutineScope = rememberCoroutineScope()

    DistributionToolsContent(
        distributionId = distributionId,
        onQueryProvider = {
            coroutineScope.launch {
                DefaultDistributionProviderChecker(context).queryProvider()
            }
        },
    )
}

@Composable
private fun DistributionToolsContent(
    distributionId: String,
    onQueryProvider: () -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .padding(all = FirefoxTheme.layout.space.static200)
                .verticalScroll(state = rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.debug_drawer_distribution_id),
                style = FirefoxTheme.typography.headline6,
                modifier = Modifier.padding(FirefoxTheme.layout.space.static50),
            )

            Text(
                text = distributionId,
                modifier = Modifier.padding(FirefoxTheme.layout.space.static50),
            )

            FilledButton(
                text = stringResource(R.string.debug_drawer_run_query_provider_test),
                onClick = onQueryProvider,
            )
        }
    }
}

@Preview
@Composable
private fun DistributionToolsPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        DistributionToolsContent(
            distributionId = "distributionId",
            onQueryProvider = {},
        )
    }
}
