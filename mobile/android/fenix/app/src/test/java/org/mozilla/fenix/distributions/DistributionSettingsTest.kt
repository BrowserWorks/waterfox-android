/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.distributions

import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DistributionSettingsTest {

    lateinit var subject: DistributionSettings

    lateinit var settings: Settings

    @Before
    fun setup() {
        settings = Settings(testContext)
        subject = DefaultDistributionSettings(
            settings = settings,
        )
    }

    @Test
    fun `WHEN getDistributionId is called THEN the saved distributionID is returned`() {
        settings.distributionId = "test"
        assertEquals("test", subject.getDistributionId())
    }

    @Test
    fun `WHEN the distribution ID is saved THEN it can be retrieved`() {
        subject.saveDistributionId("test")
        assertEquals("test", subject.getDistributionId())
    }

}
