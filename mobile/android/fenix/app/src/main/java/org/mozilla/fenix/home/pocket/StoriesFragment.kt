/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.pocket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content

import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.ext.observeAsState
import org.mozilla.fenix.components.appstate.recommendations.ContentRecommendationsState
import org.mozilla.fenix.components.components

import org.mozilla.fenix.home.pocket.controller.NoOpPocketStoriesController
import org.mozilla.fenix.home.pocket.controller.PocketStoriesController
import org.mozilla.fenix.home.pocket.interactor.DefaultPocketStoriesInteractor
import org.mozilla.fenix.home.pocket.interactor.PocketStoriesInteractor
import org.mozilla.fenix.home.pocket.ui.StoriesScreen
import org.mozilla.fenix.theme.FirefoxTheme


/**
 * A [Fragment] displaying the stories screen.
 */
class StoriesFragment : Fragment() {

    private lateinit var interactor: PocketStoriesInteractor
    private lateinit var controller: PocketStoriesController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller = NoOpPocketStoriesController()

        interactor = DefaultPocketStoriesInteractor(
            controller = controller,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = content {
        FirefoxTheme {
            val appStore = components.appStore
            val storiesState by appStore.observeAsState(initialValue = ContentRecommendationsState()) { state ->
                state.recommendationState
            }

            StoriesScreen(
                state = storiesState,
                interactor = interactor,
                onNavigationIconClick = {
                    this@StoriesFragment.findNavController().popBackStack()
                },
            )
        }
    }
}
