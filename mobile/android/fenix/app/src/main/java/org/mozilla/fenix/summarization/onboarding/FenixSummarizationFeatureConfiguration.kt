/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.summarization.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.mozilla.fenix.summarization.SummarizationSettingsBinding
import org.mozilla.fenix.utils.Settings

/**
 * Discovery settings for the summarize feature. It's a wrapper around [org.mozilla.fenix.utils.Settings] for easier
 * testing and narrowed API
 */
class FenixSummarizationFeatureConfiguration(
    private val settings: Settings,
    private val summarizationSettingsBinding: SummarizationSettingsBinding,
) : SummarizationFeatureDiscoveryConfiguration {

    override val isFeatureAvailable: Boolean
        get() = false

    override val canShowFeature: Boolean
        get() = false

    override val showMenuItem: Boolean
        get() = canShowFeature

    override val shouldHighlightMenuItem: Boolean
        get() = canShowFeature && settings.shakeToSummarizeMenuItemExposureCount.underMaxCount()

    override val shouldHighlightOverflowMenuItem: Boolean
        get() = canShowFeature && settings.shakeToSummarizeMoreMenuItemInteractionCount.underMaxCount()

    override val shouldToolbarShowCfr: Boolean
        get() = canShowFeature && !settings.shakeToSummarizeToolbarCfrShown

    /**
     * We determine if we should highlight the toolbar by checking the feature flags &
     * checking the number of interactions
     */
    val shouldHighlightToolbarMenuButton: Boolean
        get() = canShowFeature && settings.shakeToSummarizeToolbarInteractionCount.underMaxCount()

    private val _toolbarMenuButtonHighlight = MutableStateFlow(shouldHighlightToolbarMenuButton)
    override val toolbarMenuButtonHighlight: Flow<Boolean>
        get() = _toolbarMenuButtonHighlight.combine(
            summarizationSettingsBinding.isFeatureEnabled,
        ) { shouldHighlightToolbarMenuButton, isFeatureEnabled ->
            isFeatureEnabled && shouldHighlightToolbarMenuButton
        }

    override fun cacheDiscoveryEvent(event: SummarizeDiscoveryEvent) {
        when (event) {
            SummarizeDiscoveryEvent.CfrExposure -> recordCfrExposure()
            SummarizeDiscoveryEvent.MenuItemExposure -> recordMenuItemExposure()
            SummarizeDiscoveryEvent.MenuOverflowInteraction -> recordMenuOverflowItemInteraction()
            SummarizeDiscoveryEvent.ToolbarOverflowInteraction -> recordToolbarOverflowMenuInteraction()
        }
    }

    private fun recordMenuItemExposure() {
        if (canShowFeature && settings.shakeToSummarizeMenuItemExposureCount.underMaxCount()) {
            settings.shakeToSummarizeMenuItemExposureCount.increment()
        }
    }

    private fun recordCfrExposure() {
        if (canShowFeature && !settings.shakeToSummarizeToolbarCfrShown) {
            settings.shakeToSummarizeToolbarCfrShown = true
        }
    }

    private fun recordMenuOverflowItemInteraction() {
        if (canShowFeature && settings.shakeToSummarizeMoreMenuItemInteractionCount.underMaxCount()) {
            settings.shakeToSummarizeMoreMenuItemInteractionCount.increment()
        }
    }

    private fun recordToolbarOverflowMenuInteraction() {
        if (canShowFeature && settings.shakeToSummarizeToolbarInteractionCount.underMaxCount()) {
            settings.shakeToSummarizeToolbarInteractionCount.increment()

            _toolbarMenuButtonHighlight.update { shouldHighlightToolbarMenuButton }
        }
    }
}
