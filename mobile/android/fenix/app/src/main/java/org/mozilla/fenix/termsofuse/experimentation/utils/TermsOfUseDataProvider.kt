/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse.experimentation.utils

import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.Engine.HttpsOnlyMode
import org.mozilla.fenix.utils.Settings

/**
 * Provides Terms of Use related data.
 */
interface TermsOfUseDataProvider {
    /**
     * Whether Strict Tracking Protection is enabled.
     */
    val useStrictTrackingProtection: Boolean

    /**
     * Whether Global Privacy Control is enabled.
     */
    val shouldEnableGlobalPrivacyControl: Boolean

    /**
     * Whether increased DoH protection is enabled.
     */
    fun isIncreasedDohProtectionEnabled(): Boolean

    /**
     * Whether HTTPS-only mode is enabled.
     */
    fun enabledHttpsOnlyMode(): Boolean

    /**
     * Whether sponsored Shortcuts are enabled.
     */
    val showSponsoredShortcuts: Boolean

    /**
     * Whether the Shortcuts feature is enabled.
     */
    val showShortcutsFeature: Boolean

    /**
     * Whether sponsored Stories are enabled.
     */
    val showSponsoredStories: Boolean

    /**
     * Whether the Stories feature is enabled.
     */
    val showStoriesFeature: Boolean
}

/**
 * Default implementation of [TermsOfUseDataProvider].
 */
class DefaultTermsOfUseDataProvider(private val settings: Settings) : TermsOfUseDataProvider {
    override val useStrictTrackingProtection: Boolean
        get() = settings.useStrictTrackingProtection

    override val shouldEnableGlobalPrivacyControl: Boolean
        get() = settings.shouldEnableGlobalPrivacyControl

    override fun isIncreasedDohProtectionEnabled(): Boolean {
        val dohSettingsMode = settings.getDohSettingsMode()
        return dohSettingsMode == Engine.DohSettingsMode.INCREASED ||
            dohSettingsMode == Engine.DohSettingsMode.MAX ||
            dohSettingsMode == Engine.DohSettingsMode.ULTRA
    }

    override fun enabledHttpsOnlyMode() = settings.getHttpsOnlyMode() != HttpsOnlyMode.DISABLED

    override val showSponsoredShortcuts: Boolean
        get() = settings.showContileFeature

    override val showShortcutsFeature: Boolean
        get() = settings.showTopSitesFeature

    override val showSponsoredStories: Boolean
        get() = settings.showPocketSponsoredStories

    override val showStoriesFeature: Boolean
        get() = settings.showPocketRecommendationsFeature
}
