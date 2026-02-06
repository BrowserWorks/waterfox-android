/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import androidx.compose.runtime.staticCompositionLocalOf


/**
 * Feature helper for managing the release of the Tabs Tray UI enhancements.
 */
interface TabManagementFeatureHelper {

    /**
     * Whether the Tab Manager opening animation is enabled.
     */
    val openingAnimationEnabled: Boolean

    /**
     * Whether the Tab Groups feature is enabled.
     */
    val tabGroupsEnabled: Boolean

    /**
     * Whether drag and drop is enabled for the Tab Groups feature.
     */
    val tabGroupsDragAndDropEnabled: Boolean

    /**
     * Determines whether the "Ungroup" item is displayed in a tab group's three dot menu.
     */
    val ungroupTabGroupEnabled: Boolean

    /**
     * Whether onboarding is enabled for the Tab Groups feature.
     */
    val tabGroupsOnboardingEnabled: Boolean

    /**
     * Control whether reorder happens live during a drag and drop action for Tab Groups.
     */
    val tabGroupsLiveReorderEnabled: Boolean
}

/**
 * The default implementation of [TabManagementFeatureHelper].
 */
data object DefaultTabManagementFeatureHelper : TabManagementFeatureHelper {

    override val openingAnimationEnabled: Boolean
        get() = true

    override val tabGroupsEnabled: Boolean
        get() = true

    override val tabGroupsDragAndDropEnabled: Boolean
        get() = true

    override val tabGroupsLiveReorderEnabled: Boolean
        get() = true

    override val ungroupTabGroupEnabled: Boolean
        get() = false

    override val tabGroupsOnboardingEnabled: Boolean
        get() = true
}

val LocalTabManagementFeatureHelper = staticCompositionLocalOf<TabManagementFeatureHelper> {
    DefaultTabManagementFeatureHelper
}
