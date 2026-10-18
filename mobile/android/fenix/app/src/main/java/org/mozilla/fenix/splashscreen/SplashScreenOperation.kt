/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.splashscreen

/** An async operation performed during the splash screen. */
interface SplashScreenOperation {
    /** The type of the splash screen operation. */
    val type: String

    /** Indicates whether data was fetched during the operation. */
    val dataFetched: Boolean

    /** Executes the splash screen operation. */
    suspend fun run()

    /** Releases any observers associated with the operation. */
    fun dispose()
}
