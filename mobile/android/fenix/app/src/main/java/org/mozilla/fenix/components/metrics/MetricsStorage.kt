/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.support.utils.DateTimeProvider
import mozilla.components.support.utils.DefaultDateTimeProvider
import mozilla.components.support.utils.ext.packageManagerCompatHelper
import org.mozilla.fenix.Config
import org.mozilla.fenix.android.DefaultActivityLifecycleCallbacks
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Interface defining functions around persisted local state for certain metrics.
 */
interface MetricsStorage {
    /**
     * Determines whether an [event] should be sent based on locally-stored state.
     */
    suspend fun shouldTrack(event: Event): Boolean

    /**
     * Updates locally-stored state for an [event] that has just been sent.
     */
    suspend fun updateSentState(event: Event)

    /**
     * Updates locally-stored data related to an [event] that has just been sent.
     */
    suspend fun updatePersistentState(event: Event)

    /**
     * Will try to register this as a recorder of app usage based on whether usage recording is still
     * needed. It will measure usage by to monitoring lifecycle callbacks from [application]'s
     * activities and should update local state using [updateUsageState].
     */
    fun tryRegisterAsUsageRecorder(application: Application)

    /**
     * Update local state with a [usageLength] measurement.
     */
    fun updateUsageState(usageLength: Long)
}

@Suppress("TooManyFunctions")
internal class DefaultMetricsStorage(
    context: Context,
    private val settings: Settings,
    private val checkDefaultBrowser: () -> Boolean,
    private val shouldSendGenerally: () -> Boolean = { shouldSendGenerally(context) },
    private val getInstalledTime: () -> Long = { getInstalledTime(context) },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val dateTimeProvider: DateTimeProvider = DefaultDateTimeProvider(),
) : MetricsStorage {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Checks local state to see whether the [event] should be sent.
     */
    @Suppress("CyclomaticComplexMethod")
    override suspend fun shouldTrack(event: Event): Boolean =
        withContext(dispatcher) {
            // The side-effect of storing days of use always needs to happen.
            updateDaysOfUse()
            val currentTime = System.currentTimeMillis()
            shouldSendGenerally() && when (event) {
                Event.GrowthData.SetAsDefault -> {
                    currentTime.duringFirstMonth() &&
                        !settings.setAsDefaultGrowthSent &&
                        checkDefaultBrowser()
                }
                Event.GrowthData.FirstWeekSeriesActivity -> {
                    currentTime.duringFirstMonth() && shouldTrackFirstWeekActivity()
                }
                Event.GrowthData.SerpAdClicked -> {
                    currentTime.duringFirstMonth() && !settings.adClickGrowthSent
                }
                Event.GrowthData.UsageThreshold -> {
                    !settings.usageTimeGrowthSent &&
                        settings.usageTimeGrowthData > USAGE_THRESHOLD_MILLIS
                }
                Event.GrowthData.FirstAppOpenForDay -> {
                    currentTime.afterFirstDay() &&
                        currentTime.duringFirstMonth() &&
                        settings.resumeGrowthLastSent.hasBeenMoreThanDaySince()
                }
                Event.GrowthData.FirstUriLoadForDay -> {
                    currentTime.afterFirstDay() &&
                        currentTime.duringFirstMonth() &&
                        settings.uriLoadGrowthLastSent.hasBeenMoreThanDaySince()
                }
                is Event.GrowthData.UserActivated -> {
                    hasUserReachedActivatedThreshold()
                }
                is Event.FirstWeekPostInstall.LastThreeDaysActivity -> {
                    shouldTrackFirstWeekLastDaysActivity(
                        currentTime = dateTimeProvider.currentTimeMillis(),
                        firstWeekDaysOfUse = settings.firstWeekDaysOfUseGrowthData,
                        eventSent = settings.firstWeekPostInstallLastThreeDaysActivitySent,
                    )
                }
                is Event.FirstWeekPostInstall.RecurrentActivity -> {
                    shouldTrackFirstWeekRecurrentlyActivity(
                        currentTime = dateTimeProvider.currentTimeMillis(),
                        firstWeekDaysOfUse = settings.firstWeekDaysOfUseGrowthData,
                        eventSent = settings.firstWeekPostInstallRecurrentActivitySent,
                    )
                }
                is Event.FirstWeekPostInstall.EverydayActivityAndSetToDefault -> {
                    shouldTrackFirstWeekFullActivityDefault(
                        currentTime = dateTimeProvider.currentTimeMillis(),
                        firstWeekDaysOfUse = settings.firstWeekDaysOfUseGrowthData,
                        eventSent = settings.firstWeekPostInstallEverydayActivityAndSetToDefaultSent,
                    )
                }
            }
        }

    override suspend fun updateSentState(event: Event) = withContext(dispatcher) {
        when (event) {
            Event.GrowthData.SetAsDefault -> {
                settings.setAsDefaultGrowthSent = true
            }
            Event.GrowthData.FirstWeekSeriesActivity -> {
                settings.firstWeekSeriesGrowthSent = true
            }
            Event.GrowthData.SerpAdClicked -> {
                settings.adClickGrowthSent = true
            }
            Event.GrowthData.UsageThreshold -> {
                settings.usageTimeGrowthSent = true
            }
            Event.GrowthData.FirstAppOpenForDay -> {
                settings.resumeGrowthLastSent = System.currentTimeMillis()
            }
            Event.GrowthData.FirstUriLoadForDay -> {
                settings.uriLoadGrowthLastSent = System.currentTimeMillis()
            }
            is Event.GrowthData.UserActivated -> {
                settings.growthUserActivatedSent = true
            }
            is Event.FirstWeekPostInstall.LastThreeDaysActivity -> {
                settings.firstWeekPostInstallLastThreeDaysActivitySent = true
            }
            is Event.FirstWeekPostInstall.RecurrentActivity -> {
                settings.firstWeekPostInstallRecurrentActivitySent = true
            }
            is Event.FirstWeekPostInstall.EverydayActivityAndSetToDefault -> {
                settings.firstWeekPostInstallEverydayActivityAndSetToDefaultSent = true
            }
        }
    }

    override suspend fun updatePersistentState(event: Event) {
        when (event) {
            is Event.GrowthData.UserActivated -> {
                if (event.fromSearch && shouldUpdateSearchUsage()) {
                    settings.growthEarlySearchUsed = true
                } else if (!event.fromSearch && shouldUpdateUsageCount()) {
                    settings.growthEarlyUseCount.increment()
                    settings.growthEarlyUseCountLastIncrement = System.currentTimeMillis()
                }
            }
            else -> Unit
        }
    }

    override fun tryRegisterAsUsageRecorder(application: Application) {
        // Currently there is only interest in measuring usage during the first day of install.
        if (!settings.usageTimeGrowthSent && System.currentTimeMillis().duringFirstDay()) {
            application.registerActivityLifecycleCallbacks(UsageRecorder(this))
        }
    }

    override fun updateUsageState(usageLength: Long) {
        settings.usageTimeGrowthData += usageLength
    }

    private fun updateDaysOfUse() {
        val daysOfUse = settings.firstWeekDaysOfUseGrowthData
        val currentDate = Calendar.getInstance(Locale.US)
        val currentDateString = dateFormatter.format(currentDate.time)
        if (currentDate.timeInMillis.duringFirstWeek() && daysOfUse.none { it == currentDateString }) {
            settings.firstWeekDaysOfUseGrowthData = daysOfUse + currentDateString
        }
    }

    private fun shouldTrackFirstWeekActivity(): Boolean = Result.runCatching {
        if (!System.currentTimeMillis().duringFirstWeek() || settings.firstWeekSeriesGrowthSent) {
            return false
        }

        val daysOfUse = settings.firstWeekDaysOfUseGrowthData.map {
            dateFormatter.parse(it)
        }.sorted()

        // This loop will check whether the existing list of days of use, combined with the
        // current date, contains any periods of 3 days of use in a row.
        for (idx in daysOfUse.indices) {
            if (idx + 1 > daysOfUse.lastIndex || idx + 2 > daysOfUse.lastIndex) {
                continue
            }

            val referenceDate = daysOfUse[idx]!!.time.toCalendar()
            val secondDateEntry = daysOfUse[idx + 1]!!.time.toCalendar()
            val thirdDateEntry = daysOfUse[idx + 2]!!.time.toCalendar()
            val oneDayAfterReference = referenceDate.createNextDay()
            val twoDaysAfterReference = oneDayAfterReference.createNextDay()

            if (oneDayAfterReference == secondDateEntry && thirdDateEntry == twoDaysAfterReference) {
                return true
            }
        }
        return false
    }.getOrDefault(false)

    @VisibleForTesting
    internal fun shouldTrackFirstWeekLastDaysActivity(
        currentTime: Long,
        firstWeekDaysOfUse: Set<String>,
        eventSent: Boolean,
    ): Boolean = Result.runCatching {
        if (!currentTime.duringFirst7Days() || eventSent) {
            return false
        }

        return firstWeekDaysOfUse.toTimestamps().any { date ->
            date.duringLastThreeDays()
        }
    }.getOrDefault(false)

    @VisibleForTesting
    internal fun shouldTrackFirstWeekRecurrentlyActivity(
        currentTime: Long,
        firstWeekDaysOfUse: Set<String>,
        eventSent: Boolean,
    ): Boolean = Result.runCatching {
        if (!currentTime.duringFirst7Days() || eventSent) {
            return false
        }

        return activeInFirstPartOfTheWeek(firstWeekDaysOfUse) && activeInLastPartOfTheWeek(firstWeekDaysOfUse)
    }.getOrDefault(false)

    @VisibleForTesting
    internal fun shouldTrackFirstWeekFullActivityDefault(
        currentTime: Long,
        firstWeekDaysOfUse: Set<String>,
        eventSent: Boolean,
        isBrowserSetToDefaultDuringFirstFourDays: Boolean =
            settings.firstWeekPostInstallIsBrowserSetToDefaultDuringFirstFourDays,
    ): Boolean = Result.runCatching {
        if (!currentTime.duringFirst7Days() || eventSent) {
            return false
        }

        updateIsDefaultBrowserDuringFirstFourDays(
            isDefaultBrowserDuringFirstFourDay = isBrowserSetToDefaultDuringFirstFourDays,
            currentTime = currentTime,
        )

        val isAllWeekActive = firstWeekDaysOfUse.toTimestamps().count { date ->
            date.duringFirst7Days()
        } == NUMBER_OF_DAYS_IN_A_WEEK

        return isBrowserSetToDefaultDuringFirstFourDays && isAllWeekActive
    }.getOrDefault(false)

    @VisibleForTesting
    internal fun activeInFirstPartOfTheWeek(firstWeekDaysOfUse: Set<String>): Boolean =
        firstWeekDaysOfUse.toTimestamps()
            .count { date -> date.duringFirstFourDays() } >= MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY

    @VisibleForTesting
    internal fun activeInLastPartOfTheWeek(firstWeekDaysOfUse: Set<String>): Boolean =
        firstWeekDaysOfUse.toTimestamps()
            .count { date -> date.duringLastThreeDays() } >= MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY

    @VisibleForTesting
    internal fun updateIsDefaultBrowserDuringFirstFourDays(
        currentTime: Long,
        isDefaultBrowserDuringFirstFourDay: Boolean,
        isDefaultBrowser: Boolean = checkDefaultBrowser(),
    ) {
        val shouldUpdate = !isDefaultBrowserDuringFirstFourDay &&
            isDefaultBrowser &&
            currentTime.duringFirstFourDays()

        if (shouldUpdate) {
            settings.firstWeekPostInstallIsBrowserSetToDefaultDuringFirstFourDays = true
        }
    }

    private fun Set<String>.toTimestamps(): List<Long> = mapNotNull {
        dateFormatter.parse(it)?.time
    }

    private fun Long.toCalendar(): Calendar = Calendar.getInstance(Locale.US).also { calendar ->
        calendar.timeInMillis = this
    }

    private fun Long.hasBeenMoreThanDaySince() = System.currentTimeMillis() - this > DAY_MILLIS

    private fun Long.afterFirstDay() = this > getInstalledTime() + DAY_MILLIS

    private fun Long.duringFirstDay() = this < getInstalledTime() + DAY_MILLIS

    private fun Long.afterThirdDay() = this > getInstalledTime() + THREE_DAY_MILLIS

    private fun Long.afterFourthDay() = this >= getInstalledTimeToMidnight() + FOUR_DAY_MILLIS

    private fun Long.duringLastThreeDays() = this.afterFourthDay() && this.duringFirst7Days()

    private fun Long.duringFirstFourDays() = this < getInstalledTimeToMidnight() + FOUR_DAY_MILLIS

    private fun Long.duringFirstWeek() = this < getInstalledTime() + FULL_WEEK_MILLIS

    private fun Long.duringFirst7Days() = this < getInstalledTimeToMidnight() + SEVEN_DAYS_MILLIS

    private fun Long.duringFirstMonth() = this < getInstalledTime() + SHORTEST_MONTH_MILLIS

    private fun Calendar.createNextDay() = (this.clone() as Calendar).also { calendar ->
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    private fun getInstalledTimeToMidnight() = getInstalledTime().toMidnight()

    private fun Long.toMidnight(): Long = this.toCalendar().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun hasUserReachedActivatedThreshold(): Boolean {
        return !settings.growthUserActivatedSent &&
            settings.growthEarlyUseCount.value >= DAYS_ACTIVATED_THREASHOLD &&
            settings.growthEarlySearchUsed
    }

    private fun shouldUpdateUsageCount(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime.afterFirstDay() &&
            currentTime.duringFirstWeek() &&
            settings.growthEarlyUseCountLastIncrement.hasBeenMoreThanDaySince()
    }

    private fun shouldUpdateSearchUsage(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime.afterThirdDay() &&
            currentTime.duringFirstWeek()
    }

    /**
     * This will store app usage time to disk, based on Resume and Pause lifecycle events. Currently,
     * there is only interest in usage during the first day after install.
     */
    internal class UsageRecorder(
        private val metricsStorage: MetricsStorage,
    ) : DefaultActivityLifecycleCallbacks {
        private val activityStartTimes: MutableMap<String, Long?> = mutableMapOf()

        override fun onActivityResumed(activity: Activity) {
            super.onActivityResumed(activity)
            activityStartTimes[activity.componentName.toString()] = System.currentTimeMillis()
        }

        override fun onActivityPaused(activity: Activity) {
            super.onActivityPaused(activity)
            val startTime = activityStartTimes[activity.componentName.toString()] ?: return
            val elapsedTimeMillis = System.currentTimeMillis() - startTime
            metricsStorage.updateUsageState(elapsedTimeMillis)
        }
    }

    companion object {
        private const val DAY_MILLIS: Long = 1000 * 60 * 60 * 24
        private const val THREE_DAY_MILLIS: Long = 3 * DAY_MILLIS
        private const val FOUR_DAY_MILLIS: Long = 4 * DAY_MILLIS
        private const val SHORTEST_MONTH_MILLIS: Long = DAY_MILLIS * 28

        // Note this is 8 so that recording of FirstWeekSeriesActivity happens throughout the length
        // of the 7th day after install
        private const val FULL_WEEK_MILLIS: Long = DAY_MILLIS * 8

        private const val SEVEN_DAYS_MILLIS: Long = DAY_MILLIS * 7

        // The usage threshold we are interested in is currently 340 seconds.
        private const val USAGE_THRESHOLD_MILLIS = 1000 * 340

        // The usage threshold for "activated" growth users.
        private const val DAYS_ACTIVATED_THREASHOLD = 3

        // Minimum active days required for recurrent activity.
        private const val MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY = 2

        private const val NUMBER_OF_DAYS_IN_A_WEEK = 7

        /**
         * Determines whether events should be tracked based on some general criteria:
         * - user has installed as a result of a campaign
         * - this is a release build
         */
        fun shouldSendGenerally(context: Context): Boolean {
            return false
        }

        fun getInstalledTime(context: Context): Long = context.packageManagerCompatHelper
            .getPackageInfoCompat(context.packageName, 0)
            .firstInstallTime
    }
}
