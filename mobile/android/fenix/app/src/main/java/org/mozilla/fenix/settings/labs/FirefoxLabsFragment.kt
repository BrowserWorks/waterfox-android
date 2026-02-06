/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.labs

import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import org.mozilla.fenix.R

/** Retained for restored navigation state; Labs must never initialize or display. */
class FirefoxLabsFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        val navController = findNavController()
        if (!navController.popBackStack(R.id.settingsFragment, false)) {
            navController.navigate(
                R.id.settingsFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.firefoxLabsFragment, true).build(),
            )
        }
    }
}
