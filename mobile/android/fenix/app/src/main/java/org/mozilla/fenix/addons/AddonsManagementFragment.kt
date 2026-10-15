/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.FontStyle.FONT_WEIGHT_MEDIUM
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R as materialR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.webextension.InstallationMethod
import mozilla.components.concept.engine.webextension.isUnsupported
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.R as addonsR
import mozilla.components.feature.addons.ui.AddonsManagerAdapter
import mozilla.components.support.webextensions.WebExtensionSupport.installedExtensions
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentAddOnsManagementBinding
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.openToBrowser
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.runIfFragmentIsAttached
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SupportUtils.AMO_HOMEPAGE_FOR_ANDROID
import org.mozilla.fenix.theme.ThemeManager

/** Fragment use for managing add-ons. */
@Suppress("TooManyFunctions", "LargeClass")
class AddonsManagementFragment :
    Fragment(R.layout.fragment_add_ons_management), SystemInsetsPaddedFragment, MenuProvider {

    private var binding: FragmentAddOnsManagementBinding? = null

    private var addons: List<Addon> = emptyList()

    private var adapter: AddonsManagerAdapter? = null

    private var addonsLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddOnsManagementBinding.bind(view)
        addonsLoaded = false
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        bindRecyclerView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu
            .add(
                Menu.NONE,
                R.id.waterfox_addons_check_for_updates,
                Menu.NONE,
                R.string.waterfox_addons_check_for_updates,
            )
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.waterfox_addons_check_for_updates)?.isEnabled =
            addonsLoaded && updatableExtensionIds().isNotEmpty()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId != R.id.waterfox_addons_check_for_updates) {
            return false
        }

        val root = binding?.root ?: return true
        if (!addonsLoaded) {
            return true
        }
        val extensionIds = updatableExtensionIds()
        if (extensionIds.isNotEmpty()) {
            extensionIds.forEach { requireComponents.addonUpdater.update(it) }
            showSnackBar(root, getString(R.string.waterfox_addons_update_checks_requested))
        }
        return true
    }

    private fun updatableExtensionIds(): List<String> =
        installedExtensions.values.filter { !it.isBuiltIn() && !it.isUnsupported() }.map { it.id }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_extensions))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // letting go of the resources to avoid memory leak.
        adapter = null
        binding = null
        addonsLoaded = false
    }

    @Suppress("CognitiveComplexMethod")
    private fun bindRecyclerView() {
        val managementView =
            AddonsManagementView(
                navController = findNavController(),
                onInstallButtonClicked = ::installAddon,
                onMoreAddonsButtonClicked = ::openAMO,
                onLearnMoreClicked = { link, addon ->
                    binding?.root?.openLearnMoreLink(link, addon)
                },
            )

        val recyclerView = binding?.addOnsList
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        val shouldRefresh = adapter != null
        val viewScope = viewLifecycleOwner.lifecycleScope

        viewScope.launch(IO) {
            try {
                // Even if we block network requests, filter just in case
                val loadedAddons = requireContext().components.addonManager.getAddons().filter { it.isInstalled() }
                viewScope.launch(Dispatchers.Main) {
                    runIfFragmentIsAttached {
                        addons = loadedAddons
                        addonsLoaded = true
                        requireActivity().invalidateOptionsMenu()
                        if (!shouldRefresh) {
                            adapter =
                                AddonsManagerAdapter(
                                    addonsManagerDelegate = managementView,
                                    addons = addons,
                                    style = createAddonStyle(requireContext()),
                                    store = requireComponents.core.store,
                                )
                        }
                        binding?.addOnsProgressBar?.isVisible = false
                        binding?.addOnsEmptyMessage?.isVisible = addons.isEmpty()

                        recyclerView?.adapter = adapter
                        recyclerView?.accessibilityDelegate =
                            object : View.AccessibilityDelegate() {
                                override fun onInitializeAccessibilityNodeInfo(
                                    host: View,
                                    info: AccessibilityNodeInfo,
                                ) {
                                    super.onInitializeAccessibilityNodeInfo(host, info)

                                    adapter?.let {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            info.collectionInfo =
                                                AccessibilityNodeInfo.CollectionInfo(
                                                    it.itemCount,
                                                    1,
                                                    false,
                                                )
                                        } else {
                                            @Suppress("DEPRECATION")
                                            info.collectionInfo =
                                                AccessibilityNodeInfo.CollectionInfo.obtain(
                                                    it.itemCount,
                                                    1,
                                                    false,
                                                )
                                        }
                                    }
                                }
                            }

                        if (shouldRefresh) {
                            adapter?.updateAddons(addons)
                        }
                    }
                }
            } catch (e: AddonManagerException) {
                viewScope.launch(Dispatchers.Main) {
                    runIfFragmentIsAttached {
                        binding?.let {
                            showSnackBar(
                                it.root,
                                getString(addonsR.string.mozac_feature_addons_failed_to_load_extensions),
                            )
                        }
                        binding?.addOnsProgressBar?.isVisible = false
                        binding?.addOnsEmptyMessage?.isVisible = true
                    }
                }
            }
        }
    }

    private fun createAddonStyle(context: Context): AddonsManagerAdapter.Style {
        val sectionsTypeFace =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Typeface.create(Typeface.DEFAULT, FONT_WEIGHT_MEDIUM, false)
            } else {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

        return AddonsManagerAdapter.Style(
            sectionsTextColor = ThemeManager.resolveAttribute(materialR.attr.colorOnSurface, context),
            addonNameTextColor = ThemeManager.resolveAttribute(materialR.attr.colorOnSurface, context),
            addonSummaryTextColor = ThemeManager.resolveAttribute(materialR.attr.colorOnSurfaceVariant, context),
            sectionsTypeFace = sectionsTypeFace,
            addonAllowPrivateBrowsingLabelDrawableRes = R.drawable.ic_add_on_private_browsing_label,
        )
    }

    @VisibleForTesting
    internal fun provideAddonManager(): AddonManager {
        return requireContext().components.addonManager
    }

    internal fun installAddon(addon: Addon) {
        binding?.addonProgressOverlay?.overlayCardView?.visibility = View.VISIBLE

        if (requireComponents.appStore.state.mode.isPrivate) {
            binding
                ?.addonProgressOverlay
                ?.overlayCardView
                ?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.fx_mobile_private_layer_color_3,
                    )
                )
        }

        val installOperation =
            provideAddonManager()
                .installAddon(
                    url = addon.downloadUrl,
                    installationMethod = InstallationMethod.MANAGER,
                    onSuccess = {
                        runIfFragmentIsAttached {
                            adapter?.updateAddon(it)
                            binding?.addonProgressOverlay?.overlayCardView?.visibility = View.GONE
                        }
                    },
                    onError = { _ ->
                        binding?.addonProgressOverlay?.overlayCardView?.visibility = View.GONE
                    },
                )
        binding?.addonProgressOverlay?.cancelButton?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val safeBinding = binding
                // Hide the installation progress overlay once cancellation is successful.
                if (installOperation.cancel().await()) {
                    safeBinding?.addonProgressOverlay?.overlayCardView?.visibility = View.GONE
                }
            }
        }
    }

    private fun openAMO() {
        findNavController().openToBrowser()
        requireComponents.useCases.fenixBrowserUseCases.loadUrlOrSearch(
            searchTermOrURL = AMO_HOMEPAGE_FOR_ANDROID,
            newTab = true,
        )
    }
}
