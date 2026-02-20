/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import mozilla.components.browser.state.search.RegionState
import mozilla.components.compose.base.button.FilledButton
import mozilla.components.lib.state.ext.observeAsState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.components
import org.mozilla.fenix.distributions.DefaultDistributionProviderChecker
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme

class SecretDebugSettingsFragment : Fragment() {

    override fun onResume() {
        super.onResume()

        showToolbar(getString(R.string.preferences_debug_info))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        FirefoxTheme {
            SecretDebugSettingsScreen()
        }
    }
}

@Composable
private fun SecretDebugSettingsScreen() {
    val context = LocalContext.current
    val regionState: RegionState by components.core.store.observeAsState(
        initialValue = RegionState.Default,
        map = { it.search.region ?: RegionState.Default },
    )

    val distributionId: String by components.core.store.observeAsState(
        initialValue = "",
        map = { it.distributionId ?: "" },
    )

    val settings = components.settings

    val playInstallReferrer: String by remember {
        mutableStateOf(
            """
                utmTerm: ${settings.utmTerm}
                utmMedium: ${settings.utmMedium}
                utmSource: ${settings.utmSource}
                utmContent: ${settings.utmContent}
                utmCampaign: ${settings.utmCampaign}
            """.trimIndent(),
        )
    }

    Surface {
        SecretDebugSettingsScreenContent(
            regionState = regionState,
            distributionId = distributionId,
            playInstallReferrer = playInstallReferrer,
            onQueryProvider = {
                DefaultDistributionProviderChecker(context).queryProvider()
            },
        )
    }
}

@Composable
private fun SecretDebugSettingsScreenContent(
    regionState: RegionState,
    distributionId: String,
    playInstallReferrer: String,
    onQueryProvider: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(8.dp),
    ) {
        Text(
            text = stringResource(R.string.debug_info_region_home),
            style = FirefoxTheme.typography.headline6,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = regionState.home,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = stringResource(R.string.debug_info_region_current),
            style = FirefoxTheme.typography.headline6,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = regionState.current,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = stringResource(R.string.debug_info_distribution_id),
            style = FirefoxTheme.typography.headline6,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = distributionId,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = stringResource(R.string.debug_info_play_referrer),
            style = FirefoxTheme.typography.headline6,
            modifier = Modifier.padding(4.dp),
        )

        Text(
            text = playInstallReferrer,
            modifier = Modifier.padding(4.dp),
        )

        FilledButton(
            text = stringResource(R.string.debug_info_run_query_provider_test),
            onClick = onQueryProvider,
        )
    }
}

@PreviewLightDark
@Composable
private fun SecretDebugSettingsScreenPreview() {
    FirefoxTheme {
        Surface {
            SecretDebugSettingsScreenContent(
                regionState = RegionState(home = "US", current = "US"),
                distributionId = "distributionId",
                playInstallReferrer = "test",
                onQueryProvider = {},
            )
        }
    }
}
