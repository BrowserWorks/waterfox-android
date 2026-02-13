/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapter
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapterDelegate
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentNotYetSupportedAddonsBinding
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import mozilla.components.feature.addons.R as addonsR

private const val LEARN_MORE_URL =
    "https://www.waterfox.com/support/add-compatibility-waterfox"

/**
 * Fragment for displaying and managing add-ons that are not yet supported by the browser.
 */
class NotYetSupportedAddonFragment :
    Fragment(R.layout.fragment_not_yet_supported_addons), UnsupportedAddonsAdapterDelegate, SystemInsetsPaddedFragment {

    private val args by navArgs<NotYetSupportedAddonFragmentArgs>()
    private var unsupportedAddonsAdapter: UnsupportedAddonsAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        unsupportedAddonsAdapter = UnsupportedAddonsAdapter(
            addonManager = requireContext().components.addonManager,
            unsupportedAddonsAdapterDelegate = this@NotYetSupportedAddonFragment,
            addons = args.addons.toList(),
        )

        val binding = FragmentNotYetSupportedAddonsBinding.bind(view)

        binding.unsupportedAddOnsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = unsupportedAddonsAdapter
        }

        binding.learnMoreLabel.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).setData(LEARN_MORE_URL.toUri())
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(addonsR.string.mozac_feature_addons_unavailable_section))
    }

    override fun onUninstallError(addonId: String, throwable: Throwable) {
        this@NotYetSupportedAddonFragment.view?.let { view ->
            showSnackBar(view, getString(addonsR.string.mozac_feature_addons_failed_to_remove, ""))
        }

        if (unsupportedAddonsAdapter?.itemCount == 0) {
            findNavController().popBackStack()
        }
    }

    override fun onUninstallSuccess() {
        this@NotYetSupportedAddonFragment.view?.let { view ->
            showSnackBar(view, getString(addonsR.string.mozac_feature_addons_successfully_removed, ""))
        }
    }
}
