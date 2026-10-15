/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.compose.base.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.home.fake.FakeHomepagePreview
import org.mozilla.fenix.home.topsites.interactor.TopSiteInteractor
import org.mozilla.fenix.home.ui.TopSitesSection
import org.mozilla.fenix.theme.FirefoxTheme
import org.robolectric.annotation.Config

@Config(qualifiers = "h1600dp")
@RunWith(AndroidJUnit4::class)
class TopSitesSectionTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val interactor: TopSiteInteractor = mockk(relaxed = true)

    private fun setSectionContent(
        count: Int = 0,
        showHeader: Boolean = true,
        isAddShortcutEnabled: Boolean = true,
        showShortcutsLibraryButton: Boolean = true,
        onAddShortcutClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FirefoxTheme(theme = Theme.Light) {
                Surface {
                    Column(modifier = Modifier.requiredWidth(360.dp)) {
                        TopSitesSection(
                            state =
                                TopSiteState(
                                    topSites =
                                        FakeHomepagePreview.topSites(
                                            providedCount = 0,
                                            pinnedCount = 0,
                                            defaultCount = count,
                                        ),
                                    colors = TopSiteColors.colors(),
                                    isAddShortcutEnabled = isAddShortcutEnabled,
                                    isExpandToggleEnabled = true,
                                    showShortcutsLibraryButton = showShortcutsLibraryButton,
                                ),
                            showHeader = showHeader,
                            interactor = interactor,
                            onTopSitesItemBound = {},
                            onAddShortcutClicked = onAddShortcutClicked,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `GIVEN the shortcuts section WHEN rendered THEN actions are hidden behind a menu beside the heading`() {
        setSectionContent(count = 8)

        val heading = composeTestRule.onNodeWithText("Shortcuts").fetchSemanticsNode().boundsInRoot
        val menu = composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).fetchSemanticsNode().boundsInRoot
        assertEquals(heading.center.y, menu.center.y, 1f)
        assertTrue(menu.left >= heading.right)
        composeTestRule.onNodeWithContentDescription("Shortcuts options").assertHasClickAction()
        composeTestRule.onNodeWithText("Add shortcut").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manage shortcuts").assertDoesNotExist()
        composeTestRule.onAllNodesWithTag(TopSitesTestTag.TOP_SITE_ITEM_ROOT).assertCountEquals(8)
        composeTestRule.onAllNodesWithTag(TopSitesTestTag.ADD_SHORTCUT_ROOT).assertCountEquals(0)
    }

    @Test
    fun `GIVEN no shortcuts WHEN the menu is opened THEN add and manage actions are still available`() {
        setSectionContent()

        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).performClick()

        composeTestRule.onNodeWithText("Add shortcut").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage shortcuts").assertIsDisplayed()
    }

    @Test
    fun `WHEN add shortcut is selected THEN the callback is invoked and the menu is dismissed`() {
        var clicked = false
        setSectionContent(onAddShortcutClicked = { clicked = true })

        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).performClick()
        composeTestRule.onNodeWithText("Add shortcut").performClick()

        assertTrue(clicked)
        composeTestRule.onNodeWithText("Add shortcut").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manage shortcuts").assertDoesNotExist()
    }

    @Test
    fun `WHEN manage shortcuts is selected THEN the library is opened and the menu is dismissed`() {
        setSectionContent()

        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).performClick()
        composeTestRule.onNodeWithText("Manage shortcuts").performClick()

        verify(exactly = 1) { interactor.onShowAllTopSitesClicked() }
        composeTestRule.onNodeWithText("Add shortcut").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manage shortcuts").assertDoesNotExist()
    }

    @Test
    fun `GIVEN adding shortcuts is disabled WHEN the menu is opened THEN only manage shortcuts is shown`() {
        setSectionContent(isAddShortcutEnabled = false)

        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).performClick()

        composeTestRule.onNodeWithText("Add shortcut").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manage shortcuts").assertIsDisplayed()
    }

    @Test
    fun `GIVEN the library is disabled WHEN the menu is opened THEN only add shortcut is shown`() {
        setSectionContent(showShortcutsLibraryButton = false)

        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).performClick()

        composeTestRule.onNodeWithText("Add shortcut").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage shortcuts").assertDoesNotExist()
    }

    @Test
    fun `GIVEN both actions are disabled THEN the heading is shown without an empty menu`() {
        setSectionContent(isAddShortcutEnabled = false, showShortcutsLibraryButton = false)

        composeTestRule.onNodeWithText("Shortcuts").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).assertDoesNotExist()
    }

    @Test
    fun `GIVEN the header is hidden THEN shortcut actions remain available`() {
        setSectionContent(showHeader = false)

        composeTestRule.onNodeWithText("Shortcuts").assertDoesNotExist()
        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).performClick()

        composeTestRule.onNodeWithText("Add shortcut").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage shortcuts").assertIsDisplayed()
    }

    @Test
    fun `GIVEN more than two rows WHEN show all is clicked THEN the grid expands without footer actions`() {
        setSectionContent(count = 12)
        composeTestRule.onAllNodesWithTag(TopSitesTestTag.TOP_SITE_ITEM_ROOT).assertCountEquals(8)

        composeTestRule.onNodeWithTag(TopSitesTestTag.EXPAND_TOGGLE).performClick()

        composeTestRule.onAllNodesWithTag(TopSitesTestTag.TOP_SITE_ITEM_ROOT).assertCountEquals(12)
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add shortcut").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manage shortcuts").assertDoesNotExist()
        composeTestRule.onNodeWithTag(TopSitesTestTag.SHORTCUTS_MENU).assertIsDisplayed()
    }
}
