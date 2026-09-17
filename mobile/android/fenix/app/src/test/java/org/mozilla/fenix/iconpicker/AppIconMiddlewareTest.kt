/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.iconpicker

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconMiddlewareTest {

    @Test
    fun `WHEN the store receives UserAction Confirmed THEN the middleware calls the updateAppIcon interface with the correct data from the action`() {
        val currentIcon = AppIcon.AppDefault
        val newIcon = AppIcon.AppWaterfox2019
        var updatedCurrentIcon: AppIcon? = null
        var updatedNewIcon: AppIcon? = null
        val middleware =
            AppIconMiddleware(
                updateAppIcon = { newIcon, currentIcon ->
                    updatedNewIcon = newIcon
                    updatedCurrentIcon = currentIcon
                    true
                },
                updateSearchWidgets = {},
            )
        val store =
            AppIconStore(
                initialState =
                    AppIconState(
                        currentAppIcon = currentIcon,
                        userSelectedAppIcon = null,
                        groupedIconOptions = mapOf(),
                    ),
                middleware = listOf(middleware),
            )

        store.dispatch(UserAction.Confirmed(newIcon = newIcon, oldIcon = currentIcon))

        assertEquals(currentIcon, updatedCurrentIcon)
        assertEquals(newIcon, updatedNewIcon)
    }

    @Test
    fun `WHEN updateAppIcon call is successful THEN the middleware dispatches the Applied system action to the store`() {
        val currentIcon = AppIcon.AppDefault
        val newIcon = AppIcon.AppWaterfox2019
        val middleware =
            AppIconMiddleware(
                updateAppIcon = { _, _ -> true },
                updateSearchWidgets = {},
            )
        val result = mutableListOf<AppIconAction>()
        val store =
            AppIconStore(
                initialState = AppIconState(currentAppIcon = currentIcon),
                reducer = { state, action ->
                    result.add(action)
                    state
                },
                middleware = listOf(middleware),
            )

        assertTrue(result.isEmpty())

        val confirmAction = UserAction.Confirmed(newIcon = newIcon, oldIcon = currentIcon)
        store.dispatch(confirmAction)

        assertEquals(listOf(confirmAction, SystemAction.Applied(newIcon)), result)
    }

    @Test
    fun `WHEN updateAppIcon call returns with an a failure THEN the middleware dispatches the UpdateFailed system action to the store`() {
        val currentIcon = AppIcon.AppDefault
        val newIcon = AppIcon.AppWaterfox2019
        val middleware =
            AppIconMiddleware(
                updateAppIcon = { _, _ -> false },
                updateSearchWidgets = {},
            )
        val result = mutableListOf<AppIconAction>()
        val store =
            AppIconStore(
                initialState = AppIconState(currentAppIcon = currentIcon),
                reducer = { state, action ->
                    result.add(action)
                    state
                },
                middleware = listOf(middleware),
            )

        assertTrue(result.isEmpty())

        val confirmAction = UserAction.Confirmed(newIcon = newIcon, oldIcon = currentIcon)
        store.dispatch(confirmAction)

        assertEquals(listOf(confirmAction, SystemAction.UpdateFailed(oldIcon = currentIcon, newIcon = newIcon)), result)
    }

    @Test
    fun `WHEN the store receives SystemAction Applied THEN the middleware calls the updateWidgets interface`() {
        var updateSearchWidgetsCalled = false
        val middleware =
            AppIconMiddleware(
                updateAppIcon = { _, _ -> false },
                updateSearchWidgets = {
                    updateSearchWidgetsCalled = true
                },
            )
        val store =
            AppIconStore(
                initialState =
                    AppIconState(
                        currentAppIcon = AppIcon.AppWaterfox2019,
                        userSelectedAppIcon = null,
                        groupedIconOptions = mapOf(),
                    ),
                middleware = listOf(middleware),
            )

        store.dispatch(SystemAction.Applied(AppIcon.AppWaterfox2019))

        assertTrue(updateSearchWidgetsCalled)
    }
}
