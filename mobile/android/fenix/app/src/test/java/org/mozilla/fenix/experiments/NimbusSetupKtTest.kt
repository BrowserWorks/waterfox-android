/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.experiments

import android.content.Context
import io.mockk.mockk
import mozilla.components.service.nimbus.NimbusDisabled
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.fenix.components.NimbusComponents

class NimbusSetupKtTest {
    @Test
    fun `WHEN Nimbus is created THEN experiments remain disabled`() {
        val context = mockk<Context>(relaxed = true)

        assertTrue(createNimbus(context) is NimbusDisabled)
    }

    @Test
    fun `WHEN crash reporter requests enrollments THEN no Nimbus database is opened`() {
        val components = NimbusComponents(mockk(relaxed = true), lazy { mockk() })

        assertTrue(components.getEnrollmentsForCrashReporter().isEmpty())
    }
}
