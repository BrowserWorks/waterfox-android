/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.experiments

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.components.ShortcutManagerCompatWrapper
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings

/**
 * Removes legacy uninstall survey shortcuts without offering surveys.
 *
 * @param context The application context.
 * @param shortcutManagerWrapper The wrapper used to publish or remove dynamic shortcuts.
 * @param settings Retained for source compatibility; cannot enable surveys.
 */
class UninstallSurveyManager(
    private val context: Context,
    private val shortcutManagerWrapper: ShortcutManagerCompatWrapper,
    @Suppress("UNUSED_PARAMETER") settings: Settings = context.components.settings,
) {

    private val logger = Logger("UninstallSurveyManager")

    /**
     * Removes the dynamic shortcut left by an earlier installation.
     */
    fun updateUninstallSurveyShortcut() {
        try {
            shortcutManagerWrapper.removeDynamicShortcuts(context, listOf(SHORTCUT_ID))
        } catch (e: SecurityException) {
            logger.error("System security policy blocked uninstall survey shortcut cleanup", e)
        } catch (e: IllegalStateException) {
            logger.error("Failed to remove uninstall survey shortcut", e)
        }
    }

    /**
     * Consumes a legacy survey intent without navigating or prompting for uninstallation.
     *
     * @param intent The incoming [Intent] containing the action.
     * @param navController The [NavController] used to trigger the navigation.
     */
    @Suppress("UNUSED_PARAMETER")
    fun showUninstallSurvey(intent: Intent?, navController: NavController) {
        if (intent?.action == ACTION_UNINSTALL_SURVEY) {
            intent.action = null
        }
    }

    companion object {
        const val ACTION_UNINSTALL_SURVEY = "org.mozilla.fenix.ACTION_UNINSTALL_SURVEY"
        private const val SHORTCUT_ID = "uninstall_survey_shortcut"
    }
}
