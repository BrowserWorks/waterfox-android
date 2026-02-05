/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse

import androidx.annotation.VisibleForTesting
import org.mozilla.fenix.termsofuse.store.TermsOfUsePromptRepository

/**
 * Helps determine when the Terms of Use prompt should show.
 *
 * @param repository the repository for data related to the Terms of Use prompt.
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
class TermsOfUseManager(
    @Suppress("UNUSED_PARAMETER") // Retain the constructor API while Terms of Use prompts are disabled.
    repository: TermsOfUsePromptRepository,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {

    /** Determines whether the Terms of Use bottom sheet should be shown on the homepage. */
    @Suppress("FunctionOnlyReturningConstant") // Preserve the prompt API without showing Terms of Use.
    fun shouldShowTermsOfUsePromptOnHomepage() = false

    /** Determines whether the Terms of Use bottom sheet should be shown in the browser fragment. */
    @Suppress("FunctionOnlyReturningConstant") // Preserve the prompt API without showing Terms of Use.
    fun shouldShowTermsOfUsePromptOnBrowserFragment() = false

    /**
     * Determines whether the Terms of Use bottom sheet should be shown.
     *
     * @param ignoreFirstCheckSinceAppStart Used to check whether the app start check is required.
     * @param currentTimeInMillis The current time in milliseconds.
     * @return `true` if the Terms of Use bottom sheet should be shown; otherwise, `false`.
     */
    @VisibleForTesting
    // Preserve the test-facing signature and default clock evaluation while prompts are disabled.
    @Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
    internal fun shouldShowTermsOfUsePrompt(
        ignoreFirstCheckSinceAppStart: Boolean = false,
        currentTimeInMillis: Long = currentTimeMillis(),
    ): Boolean = false

    /** Lifecycle compatibility hook; Terms of Use prompts are disabled in Waterfox. */
    fun onStart() = Unit
}
