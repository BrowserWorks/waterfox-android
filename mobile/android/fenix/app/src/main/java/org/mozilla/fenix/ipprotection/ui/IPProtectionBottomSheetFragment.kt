/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ipprotection.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.R as materialR
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import mozilla.components.feature.ipprotection.store.IPProtectionAction
import mozilla.components.lib.state.helpers.StoreProvider.Companion.fragmentStore
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ipprotection.helpers.IsoPromoDeadline
import org.mozilla.fenix.ipprotection.helpers.formatPromoDateOrCatch
import org.mozilla.fenix.ipprotection.store.IPProtectionPromptAction
import org.mozilla.fenix.ipprotection.store.IPProtectionPromptPreferencesMiddleware
import org.mozilla.fenix.ipprotection.store.IPProtectionPromptState
import org.mozilla.fenix.ipprotection.store.IPProtectionPromptStore
import org.mozilla.fenix.ipprotection.store.IPProtectionPromptTelemetryMiddleware
import org.mozilla.fenix.ipprotection.store.Surface
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme

/** [BottomSheetDialogFragment] wrapper for the compose [IPProtectionBottomSheet]. */
class IPProtectionBottomSheetFragment : BottomSheetDialogFragment() {

    private val args by navArgs<IPProtectionBottomSheetFragmentArgs>()

    private var isAlreadyShowing: Boolean = false

    private val ipProtectionPromptStore by
        fragmentStore(IPProtectionPromptState) {
            IPProtectionPromptStore(
                initialState = it,
                middleware =
                    listOf(
                        IPProtectionPromptPreferencesMiddleware(
                            repository = requireComponents.ipProtectionPromptRepository
                        ),
                        IPProtectionPromptTelemetryMiddleware(),
                    ),
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!requireComponents.settings.isIPProtectionAvailable) {
            showsDialog = false
            dismissAllowingStateLoss()
            return
        }

        // The user might already have an account, that we should check - they either need to authorize, or they are
        // already entitled to use the service, and we will save them an authorization flow.
        // FIXME(IPP): the user might navigate into try now faster than the account check completes, in which case,
        //  even if already entitled, they would have to re-authorize vpn. We probably want to chain the check and
        //  toggle actions or even check the account before showing the onboarding (so we don't have to maintain the UI
        //  in a loading state.
        requireComponents.ipProtection.store.dispatch(IPProtectionAction.CheckAccount)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View?>(materialR.id.design_bottom_sheet)
                bottomSheet?.setBackgroundResource(android.R.color.transparent)

                if (!isAlreadyShowing) {
                    ipProtectionPromptStore.dispatch(IPProtectionPromptAction.OnImpression(args.surface))
                    isAlreadyShowing = true
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (!requireComponents.settings.isIPProtectionAvailable) return null

        isAlreadyShowing = savedInstanceState?.getBoolean(IS_ALREADY_SHOW_KEY) ?: false
        ipProtectionPromptStore.dispatch(IPProtectionPromptAction.OnPromptCreated)
        val maxGib = 50
        val formattedPromoDate =
            "2026-08-31".let { promoDate ->
                IsoPromoDeadline(promoDate)
                    .formatPromoDateOrCatch { requireComponents.analytics.crashReporter.submitCaughtException(it) }
                    ?.takeIf { LocalDate.now() <= LocalDate.parse(promoDate) }
            }
        return content {
            FirefoxTheme {
                IPProtectionBottomSheet(
                    maxGib = maxGib,
                    formattedPromoDate = formattedPromoDate,
                    onDismiss = { dismiss() },
                    onDismissRequest = {
                        ipProtectionPromptStore.dispatch(
                            IPProtectionPromptAction.OnPromptManuallyDismissed(args.surface)
                        )
                        dismiss()
                    },
                    onGetStartedClicked = {
                        ipProtectionPromptStore.dispatch(IPProtectionPromptAction.OnGetStartedClicked(args.surface))
                        findNavController()
                            .navigate(
                                NavGraphDirections.actionGlobalIpProtectionFragment(
                                    startAuthFlow = true,
                                    entrypoint = FenixFxAEntryPoint.IPProtectionOnboarding,
                                )
                            )
                        dismiss()
                    },
                    onLearnMoreClicked = {
                        ipProtectionPromptStore.dispatch(
                            IPProtectionPromptAction.OnBrowseWithExtraProtectionClicked(args.surface)
                        )
                        SupportUtils.launchSandboxCustomTab(
                            requireActivity(),
                            SupportUtils.getSumoURLForTopic(
                                requireActivity(),
                                SupportUtils.SumoTopic.VPN,
                                useMobilePage = true,
                            ),
                        )
                    },
                    onNotNowClicked = {
                        ipProtectionPromptStore.dispatch(IPProtectionPromptAction.OnNotNowClicked(args.surface))
                    },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_ALREADY_SHOW_KEY, isAlreadyShowing)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!requireComponents.settings.isIPProtectionAvailable) return

        ipProtectionPromptStore.dispatch(IPProtectionPromptAction.OnPromptDismissed)
    }

    companion object {
        private const val IS_ALREADY_SHOW_KEY = "is_already_showing"
        private const val TAG = "IPProtectionBottomSheetFragment"

        /**
         * Shows the IP Protection onboarding prompt for [surface], unless it is already shown.
         *
         * @param fragment The [Fragment] whose fragment manager shows the prompt.
         * @param surface The [Surface] the prompt is shown on.
         */
        fun showPrompt(fragment: Fragment, surface: Surface) {
            if (!fragment.requireComponents.settings.isIPProtectionAvailable) return

            if (fragment.parentFragmentManager.findFragmentByTag(TAG) != null) {
                return
            }

            IPProtectionBottomSheetFragment()
                .apply { arguments = IPProtectionBottomSheetFragmentArgs(surface).toBundle() }
                .showNow(fragment.parentFragmentManager, TAG)
        }
    }
}
