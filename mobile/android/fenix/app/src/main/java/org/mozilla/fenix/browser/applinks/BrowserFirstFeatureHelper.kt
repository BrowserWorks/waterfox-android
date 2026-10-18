/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.applinks

/** Feature helper gating browser-first handling. */
interface BrowserFirstFeatureHelper {
    /** Whether browser-first handling is enabled. */
    val enabled: Boolean
}

/** The default implementation of [BrowserFirstFeatureHelper]. */
data object DefaultBrowserFirstFeatureHelper : BrowserFirstFeatureHelper {
    override val enabled: Boolean = false
}
