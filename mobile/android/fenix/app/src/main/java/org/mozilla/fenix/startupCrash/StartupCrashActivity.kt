/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.startupCrash

import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import mozilla.components.lib.crash.CrashReporter
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * The [StartupCrashActivity] is the app activity launched when the ExceptionHandler is invoked before the
 * visualCompletenessQueue is ready. It will handle the crash report and app restart.
 */
class StartupCrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContentView(R.layout.activity_startup_crash)

        findViewById<ComposeView>(R.id.startupCrashActivity).setContent {
            FirefoxTheme {
                StartupCrashScreen(
                    store =
                        StartupCrashStore(
                            initialState = StartupCrashState(UiState.Finished),
                            middleware =
                                listOf(
                                    StartupCrashMiddleware(
                                        settings = components.settings,
                                        crashReporter = installCrashReporter(),
                                        restartHandler = ::restartFenix,
                                    )
                                ),
                        )
                )
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun installCrashReporter(): CrashReporter =
        components.analytics.crashReporter.also {
            it.install(applicationContext)
        }

    private fun restartFenix() {
        val restartIntent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(restartIntent)
        // Kill the existing process to ensure we get a clean start of the application
        Process.killProcess(Process.myPid())
    }
}
