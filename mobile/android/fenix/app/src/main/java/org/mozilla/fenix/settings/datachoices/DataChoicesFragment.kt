/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.datachoices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.navigation.fragment.navArgs
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme

/** Lets the user toggle telemetry on/off. */
class DataChoicesFragment : Fragment(), SystemInsetsPaddedFragment {
    private val args by navArgs<DataChoicesFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        return content {
            FirefoxTheme {
                val store =
                    DataChoicesStore(
                        initialState = DataChoicesState(itemToScrollTo = args.preferenceToScrollTo),
                        middleware =
                            listOf(
                                DataChoicesMiddleware(
                                    settings = context.components.settings,
                                    learnMoreClicked = ::learnMoreClicked,
                                    engine = context.components.core.engine,
                                    metrics = context.components.analytics.metrics,
                                    crashReporter = context.components.analytics.crashReporter,
                                )
                            ),
                    )
                store.dispatch(ViewCreated)

                DataChoicesScreen(store = store)
            }
        }
    }

    private fun learnMoreClicked(sumoTopic: SupportUtils.SumoTopic) {
        val context = requireContext()
        SupportUtils.launchSandboxCustomTab(
            context = context,
            url =
                SupportUtils.getSumoURLForTopic(
                    context = context,
                    topic = sumoTopic,
                ),
        )
    }
}
