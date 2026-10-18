/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.reloadcover

import org.mozilla.fenix.utils.Settings

/**
 * Decisions about which pieces of the tab-reload-cover feature to activate. Both flags read from [Settings], whose
 * defaults flow from the Nimbus feature — the secret-settings toggles override Nimbus so developers can flip the
 * feature on/off locally without a Nimbus rollout. Scroll-aware capture requires the base cover to be enabled; enabling
 * it without the cover has no user-visible effect and would produce misleading telemetry, so it is gated on both flags.
 * Nimbus exposure is recorded inside [isCoverEnabled] so the exposure signal is colocated with the branch value's
 * consumption (which happens under the hood in the `settings.tabReloadCoverEnabled` default lambda).
 */
object TabReloadCoverGating {
    /**
     * True when the base tab-reload-cover feature should be attached to the browser fragment. Records Nimbus exposure
     * as a side effect — this is the single exposure recording point for the whole feature.
     */
    fun isCoverEnabled(settings: Settings): Boolean =
        settings.tabReloadCoverEnabled

    /**
     * True when the scroll-aware capture side-effect should also be attached. Requires the base cover to be on. Callers
     * must have already consulted [isCoverEnabled]; that call is what records exposure, so this one doesn't repeat it.
     */
    fun isScrollAwareEnabled(settings: Settings): Boolean =
        settings.tabReloadCoverEnabled && settings.tabReloadCoverScrollAwareEnabled
}
