/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import java.io.Closeable
import org.mozilla.fenix.ext.acquireOverlayProtection
import org.mozilla.fenix.ext.removeSecure
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.secure

/**
 * A [Fragment] implementation that can be used to secure screens displaying sensitive information by not allowing
 * taking screenshots of their content and optionally hiding application overlays while the screen is visible.
 *
 * Fragments displaying such screens should extend [SecureFragment] instead of [Fragment] class.
 */
open class SecureFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    private var overlayProtection: Closeable? = null

    constructor() : this(0) {
        Fragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.secure()
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        if (requireComponents.settings.protectSensitiveScreens) {
            overlayProtection = requireActivity().window.acquireOverlayProtection()
        }
    }

    override fun onStop() {
        overlayProtection?.close()
        overlayProtection = null
        super.onStop()
    }

    override fun onDestroy() {
        this.removeSecure()
        super.onDestroy()
    }
}
