/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import mozilla.components.feature.ipprotection.IPProtectionUnavailableDialog
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.feature.ipprotection.store.state.Authorized
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.components.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.tabstray.redux.state.Page
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A host dialog for [IPProtectionUnavailableDialog].
 *
 * The dialog fragment is persistent - it won't be dismissed if the user taps outside or uses backpress gesture, but it
 * allows the user to toggle off the proxy, and it closes itself when the proxy error state is gone.
 */
class IPProtectionUnavailableDialogFragment : AppCompatDialogFragment() {

    private val isPrivateMode by lazy {
        (requireActivity() as HomeActivity).browsingModeManager.mode.isPrivate
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!requireComponents.settings.isIPProtectionAvailable) {
            showsDialog = false
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        if (!components.settings.isIPProtectionAvailable) return@content

        val state = components.ipProtection.store.observeAsComposableState { it }.value
        // This is not an early return strategy: we expect to reach this condition after the user has turned the proxy
        // off and the state machine has processed the toggle action. The proxy disconnects, the error state clears, and
        // that's the time to dismiss the alert.
        if (state.proxyStatus != Authorized.ConnectionError) {
            dismiss()
        }

        FirefoxTheme {
            IPProtectionUnavailableDialog(
                onDismiss = {
                    // no-op, the dialog fragment is intentionally blocking the UI so the user has to make a choice.
                },
                onTurnOffProxyClicked = {
                    requireComponents.ipProtection.store.dispatch(IPProtectionAction.Toggle)
                },
                onOpenTabsTrayClicked = {
                    findNavController()
                        .navigate(
                            IPProtectionUnavailableDialogFragmentDirections.actionGlobalTabManagementFragment(
                                page = if (isPrivateMode) Page.PrivateTabs else Page.NormalTabs
                            )
                        )
                },
            )
        }
    }
}
