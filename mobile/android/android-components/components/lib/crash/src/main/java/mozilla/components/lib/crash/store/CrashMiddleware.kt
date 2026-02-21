/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash.store

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.R

private const val SEVEN_DAYS_IN_MILLIS = DateUtils.DAY_IN_MILLIS * 7

/**
 * Represents the available options for crash reporting preferences.
 *
 * @property labelId The string resource label ID associated with the option.
 */
enum class CrashReportOption(
    @param:StringRes val labelId: Int,
    val label: String,
) {
    Ask(R.string.crash_reporting_ask, "Ask"),
    Auto(R.string.crash_reporting_auto, "Auto"),
    Never(R.string.crash_reporting_never, "Never"),
    ;

    /**
     * Companion object for [CrashReportOption] to provide utility methods.
     */
    companion object {
        /**
         * Convert a string to a [CrashReportOption] to avoid minification issues.
         */
        fun fromLabel(label: String): CrashReportOption {
            return entries.find { it.label.equals(label, ignoreCase = true) } ?: Ask
        }
    }
}

/**
 * An interface to store and retrieve a timestamp to defer submitting unsent crashes until.
 */
interface CrashReportCache {
    /**
     * Retrieves the previously stored cutoff date for crash reports.
     *
     * @return The cutoff date as a timestamp in milliseconds, or `null` if none has been set.
     */
    suspend fun getCutoffDate(): TimeInMillis?

    /**
     * Stores a cutoff date for crash reports.
     *
     * @param timeInMillis The cutoff date as a timestamp in milliseconds or `null`.
     */
    suspend fun setCutoffDate(timeInMillis: TimeInMillis?)

    /**
     * Gets the stored deferred timestamp.
     */
    suspend fun getDeferredUntil(): TimeInMillis?

    /**
     * Stores a deferred timestamp.
     */
    suspend fun setDeferredUntil(timeInMillis: TimeInMillis?)

    /**
     * Records that the user does not want to see the remote settings crash pull
     * anymore
     */
    suspend fun setCrashPullNeverShowAgain(neverShowAgain: Boolean)

    /**
     * Set the timestamp for when crash pulls should be deferred until in the event of one
     * having been submitted by a user.
     */
    suspend fun setCrashPullDeferUntil(timeInMillis: TimeInMillis)

    /**
     * Crash pulls should only be requested at most once per week when they are successfully submitted
     * by a user. This will return the timestamp for when it is appropriate to request a crash pull again.
     */
    suspend fun getCrashPullDeferUntil(): TimeInMillis?

    /**
     * Gets the currently set crash report option ('Ask', 'Always' or 'Never')
     */
    suspend fun getReportOption(): CrashReportOption

    /**
     * Stores the currently set crash report option ('Ask', 'Always' or 'Never')
     */
    suspend fun setReportOption(option: CrashReportOption)
}

/**
 * Middleware for the crash reporter.
 *
 * @param cache stored values for getting/setting deferredUntil.
 * @param crashReporter instance of [CrashReporter] for checking for and sending unsent crashes.
 * @param currentTimeInMillis get the current time in milliseconds.
 * @param scope [CoroutineScope] to run suspended functions on.
 */
class CrashMiddleware(
    private val cache: CrashReportCache,
    private val crashReporter: CrashReporter,
    private val currentTimeInMillis: () -> TimeInMillis = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    /**
     * Handle any middleware logic before an action reaches the [crashReducer].
     *
     * @param middlewareContext accessors for the current [CrashState] and dispatcher from the store.
     * @param next The next middleware in the chain.
     * @param action The current [CrashAction] to process in the middleware.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod", "CognitiveComplexMethod")
    fun invoke(
        middlewareContext: Pair<() -> CrashState, (CrashAction) -> Unit>,
        next: (CrashAction) -> Unit,
        action: CrashAction,
    ) {
        val (getState, dispatch) = middlewareContext

        next(action)

        when (action) {
            is CrashAction.Initialize -> scope.launch {
                when (cache.getReportOption()) {
                    CrashReportOption.Ask -> {
                        dispatch(CrashAction.CheckDeferred(listOf()))
                    }
                    CrashReportOption.Auto -> {
                        dispatch(CrashAction.CheckDeferred(listOf()))
                    }
                    CrashReportOption.Never -> {
                        return@launch
                    }
                }
            }
            is CrashAction.CheckDeferred -> scope.launch {
                val until = if (action.crashIds.isEmpty()) {
                    cache.getDeferredUntil()
                } else {
                    maxOf(cache.getDeferredUntil() ?: 0, cache.getCrashPullDeferUntil() ?: 0)
                        .takeIf { it != 0L }
                }
                val nextAction = until?.let {
                    CrashAction.RestoreDeferred(
                        now = currentTimeInMillis(),
                        until = it,
                        crashIds = action.crashIds,
                    )
                } ?: CrashAction.CheckForCrashes(action.crashIds)

                dispatch(nextAction)
            }
            is CrashAction.RestoreDeferred -> {
                if (getState() is CrashState.Ready) {
                    scope.launch {
                        cache.setDeferredUntil(null)
                        dispatch(CrashAction.CheckForCrashes(action.crashIds))
                    }
                }
            }
            is CrashAction.CheckForCrashes -> scope.launch {
                dispatch(
                    CrashAction.FinishCheckingForCrashes(
                        hasUnsentCrashes = crashReporter.hasUnsentCrashReportsSince(cutoffDate()),
                        crashIds = action.crashIds,
                    ),
                )
            }
            is CrashAction.FinishCheckingForCrashes -> scope.launch {
                when (cache.getReportOption()) {
                    CrashReportOption.Auto -> {
                        if (action.crashIds.isNotEmpty() || action.hasUnsentCrashes) {
                            dispatch(CrashAction.ShowPrompt(action.crashIds))
                        }
                    }
                    CrashReportOption.Ask -> {
                        if (action.crashIds.isNotEmpty() || action.hasUnsentCrashes) {
                            dispatch(CrashAction.ShowPrompt(action.crashIds))
                        }
                    }
                    CrashReportOption.Never -> Unit
                }
            }
            CrashAction.CancelTapped -> dispatch(CrashAction.Defer(now = currentTimeInMillis()))
            CrashAction.CancelForEverTapped -> scope.launch {
                cache.setCrashPullNeverShowAgain(true)
            }
            is CrashAction.Defer -> scope.launch {
                val state = getState()
                if (state is CrashState.Deferred) {
                    cache.setDeferredUntil(state.until)
                }
            }
            is CrashAction.ReportTapped -> scope.launch {
                if (action.crashIDs.isNotEmpty()) {
                    sendCrashReports(action.crashIDs)
                    cache.setCrashPullDeferUntil(currentTimeInMillis() + SEVEN_DAYS_IN_MILLIS)
                } else {
                    if (action.automaticallySendChecked) {
                        cache.setReportOption(CrashReportOption.Ask)
                    }
                    sendUnsentCrashReports()
                }
            }
            is CrashAction.PromptShown,
            is CrashAction.ShowPrompt,
            -> {} // noop
        }
    }

    private suspend fun cutoffDate(): TimeInMillis {
        return cache.getCutoffDate() ?: currentTimeInMillis().also {
            cache.setCutoffDate(it)
        }
    }

    private suspend fun sendUnsentCrashReports() {
        crashReporter.unsentCrashReportsSince(cutoffDate()).forEach {
            crashReporter.submitReport(it)
        }
    }

    @VisibleForTesting
    internal suspend fun sendCrashReports(crashIDs: List<String>) {
        crashReporter.findCrashReports(crashIDs.toTypedArray()).forEach {
            crashReporter.submitReport(it)
        }
    }
}
