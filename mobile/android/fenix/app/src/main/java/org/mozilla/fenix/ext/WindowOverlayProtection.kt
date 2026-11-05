/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.os.Build
import android.view.Window
import androidx.annotation.MainThread
import java.io.Closeable
import java.util.WeakHashMap

private val overlayProtectionRequests = WeakHashMap<Window, Int>()

/** Hides application overlays until every protection request for this window has been released. */
@MainThread
internal fun Window.acquireOverlayProtection(): Closeable {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return Closeable {}
    }

    val requests = overlayProtectionRequests[this] ?: 0
    if (requests == 0) {
        setHideOverlayWindows(true)
    }
    overlayProtectionRequests[this] = requests + 1

    var released = false
    return Closeable {
        if (!released) {
            released = true
            val remaining = overlayProtectionRequests.getValue(this) - 1
            if (remaining == 0) {
                overlayProtectionRequests.remove(this)
                setHideOverlayWindows(false)
            } else {
                overlayProtectionRequests[this] = remaining
            }
        }
    }
}
