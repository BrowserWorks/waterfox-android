/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ActivationPing(
    private val context: Context,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun checkAndSend() {
        val unused = context to backgroundDispatcher
        unused.hashCode()
    }
}