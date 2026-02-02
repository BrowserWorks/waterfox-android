/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.iconpicker

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.AppIconSelection
import org.mozilla.fenix.helpers.FenixGleanTestRule

@RunWith(AndroidJUnit4::class)
class AppIconTelemetryMiddlewareTest {

    @get:Rule val gleanRule = FenixGleanTestRule(testContext)

    @Test
    fun `GIVEN user action Confirmed WHEN telemetry middleware gets invoked THEN no telemetry is recorded`() {
        val currentIcon = AppIcon.AppDefault
        val newIcon = AppIcon.AppWaterfox2019
        val store = buildStore(currentIcon, newIcon)

        assertTrue(AppIconSelection.appIconSelectionConfirmed.testGetValue().orEmpty().isEmpty())

        store.dispatch(UserAction.Confirmed(newIcon = newIcon, oldIcon = currentIcon))

        assertTrue(AppIconSelection.appIconSelectionConfirmed.testGetValue().orEmpty().isEmpty())
    }

    @Test
    fun `GIVEN system action SnackbarShown WHEN telemetry middleware gets invoked THEN no telemetry is recorded`() {
        val currentIcon = AppIcon.AppDefault
        val newIcon = AppIcon.AppWaterfox2019
        val store = buildStore(currentIcon, newIcon)

        assertTrue(AppIconSelection.errorSnackbarShown.testGetValue().orEmpty().isEmpty())

        store.dispatch(SystemAction.SnackbarShown(newIcon = newIcon, oldIcon = currentIcon))

        assertTrue(AppIconSelection.errorSnackbarShown.testGetValue().orEmpty().isEmpty())
    }

    private fun buildStore(
        currentAppIcon: AppIcon = AppIcon.AppDefault,
        userSelectedAppIcon: AppIcon? = AppIcon.AppWaterfox2019,
    ) =
        AppIconStore(
            initialState =
                AppIconState(
                    currentAppIcon = currentAppIcon,
                    userSelectedAppIcon = userSelectedAppIcon,
                    groupedIconOptions = mapOf(),
                ),
            middleware = listOf(AppIconTelemetryMiddleware()),
        )
}
