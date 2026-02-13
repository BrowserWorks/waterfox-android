/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites.store

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.Middleware
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.home.topsites.AddShortcutSource

@RunWith(AndroidJUnit4::class)
class ShortcutsStoreTest {

    @Test
    fun `WHEN store is created THEN InitAction is dispatched`() {
        var initActionObserved = false
        val testMiddleware: Middleware<ShortcutsState, ShortcutsAction> = { _, next, action ->
            if (action == ShortcutsAction.InitAction) {
                initActionObserved = true
            }

            next(action)
        }

        ShortcutsStore(
            initialState = ShortcutsState.INITIAL,
            middleware = listOf(testMiddleware),
        )

        assertTrue(initActionObserved)
    }

    @Test
    fun `WHEN UpdateTopSites action is dispatched THEN topSites are updated`() = runTest {
        val store = ShortcutsStore(initialState = ShortcutsState.INITIAL)

        assertTrue(store.state.topSites.isEmpty())

        val topSites = listOf(TopSite.Pinned(id = 1L, title = "Mozilla", url = "https://mozilla.org", createdAt = 0))
        store.dispatch(ShortcutsAction.UpdateTopSites(topSites))

        assertEquals(topSites, store.state.topSites)
    }

    @Test
    fun `WHEN UpdatePopularSites action is dispatched THEN popularSites are updated`() = runTest {
        val store = ShortcutsStore(initialState = ShortcutsState.INITIAL)

        assertTrue(store.state.popularSites.isEmpty())

        val popularSites = listOf(PopularSite(title = "Mozilla", url = "https://mozilla.org"))
        store.dispatch(ShortcutsAction.UpdatePopularSites(popularSites))

        assertEquals(popularSites, store.state.popularSites)
    }

    @Test
    fun `WHEN ShowAddShortcutBottomSheet action is dispatched THEN dialogState is updated`() = runTest {
        val store = ShortcutsStore(initialState = ShortcutsState.INITIAL)

        assertEquals(DialogState.Closed, store.state.dialogState)

        store.dispatch(ShortcutsAction.ShowAddShortcutBottomSheet)

        assertEquals(DialogState.AddShortcutBottomSheet, store.state.dialogState)
    }

    @Test
    fun `WHEN ShowAddShortcutDialog action is dispatched THEN dialogState is updated`() = runTest {
        val store =
            ShortcutsStore(initialState = ShortcutsState.INITIAL.copy(dialogState = DialogState.AddShortcutBottomSheet))

        store.dispatch(ShortcutsAction.ShowAddShortcutDialog)

        assertEquals(DialogState.AddShortcut, store.state.dialogState)
    }

    @Test
    fun `WHEN CloseDialog action is dispatched THEN dialogState is updated`() = runTest {
        val store = ShortcutsStore(initialState = ShortcutsState.INITIAL.copy(dialogState = DialogState.AddShortcut))

        store.dispatch(ShortcutsAction.CloseDialog)

        assertEquals(DialogState.Closed, store.state.dialogState)
    }

    @Test
    fun `WHEN SaveShortcut action is dispatched THEN state is unchanged`() = runTest {
        val initialState = ShortcutsState.INITIAL.copy(dialogState = DialogState.AddShortcut)
        val store = ShortcutsStore(initialState = initialState)

        store.dispatch(
            ShortcutsAction.SaveShortcut(
                title = "Mozilla",
                url = "https://mozilla.org",
                source = AddShortcutSource.MANUAL,
            )
        )

        assertEquals(initialState, store.state)
    }
}
