/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.doh

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.list.SwitchListItem
import org.mozilla.fenix.theme.FirefoxTheme

@Composable
internal fun UltraDnsVpnSettings(coordinator: UltraDnsCoordinator) {
    val state by coordinator.state.collectAsState()

    Column {
        SwitchListItem(
            label = stringResource(R.string.waterfox_dns_pause_on_vpn),
            checked = state.pauseOnVpn,
            description = stringResource(R.string.waterfox_dns_pause_on_vpn_summary),
            maxLabelLines = Int.MAX_VALUE,
            maxDescriptionLines = Int.MAX_VALUE,
            showSwitchAfter = true,
            onClick = coordinator::setPauseOnVpn,
        )

        if (state.vpnActive) {
            Text(
                text = stringResource(
                    if (state.isPaused) {
                        R.string.waterfox_dns_paused
                    } else {
                        R.string.waterfox_dns_vpn_pause_disabled
                    },
                ),
                style = FirefoxTheme.typography.body2,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Text(
            text = stringResource(R.string.waterfox_dns_vpn_limits),
            style = FirefoxTheme.typography.body2,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
