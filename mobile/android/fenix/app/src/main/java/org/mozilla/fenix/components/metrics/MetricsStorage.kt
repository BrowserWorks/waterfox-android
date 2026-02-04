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
            // Update the persistent state before checking if we should track.
            updatePersistentState(event)
            val currentTime = System.currentTimeMillis()
            shouldSendGenerally() && when (event) {
                Event.GrowthData.ConversionEvent1 -> {
                    currentTime.duringFirstMonth() &&
                            !settings.setAsDefaultGrowthSent &&
                            checkDefaultBrowser()
                }

                Event.GrowthData.ConversionEvent2 -> {
                    currentTime.afterFirstDay() &&
                            currentTime.duringFirstMonth() &&
                            settings.resumeGrowthLastSent.hasBeenMoreThanDaySince()
                }

                Event.GrowthData.ConversionEvent3 -> {
                    currentTime.afterFirstDay() &&
                            currentTime.duringFirstMonth() &&
                            settings.uriLoadGrowthLastSent.hasBeenMoreThanDaySince()
                }

                Event.GrowthData.ConversionEvent4 -> {
                    currentTime.duringFirstWeek() &&
                            !settings.firstWeekSeriesGrowthSent &&
                            hasBeenActiveThreeDaysInFirstWeek()
                }

                Event.GrowthData.ConversionEvent5 -> {
                    currentTime.duringFirstMonth() && !settings.adClickGrowthSent
                }

                Event.GrowthData.ConversionEvent6 -> {
                    !settings.usageTimeGrowthSent
                }

                is Event.GrowthData.ConversionEvent7 -> {
                    currentTime.duringFirstWeek() &&
                            !settings.growthUserActivatedSent &&
                            settings.growthEarlySearchUsed &&
                            hasBeenActiveThreeDaysInFirstWeek()
                }

                is Event.FirstWeekPostInstall.ConversionEvent8 -> {
                    shouldTrackFirstWeekRecurrentlyActivity(
                        currentTime = dateTimeProvider.currentTimeMillis(),
                        firstWeekDaysOfUse = settings.firstWeekDaysOfUseGrowthData,
                        eventSent = settings.firstWeekPostInstallRecurrentActivitySent,
                    )
                }

                is Event.FirstWeekPostInstall.ConversionEvent9 -> {
                    shouldTrackFirstWeekFullActivityDefault(
                        currentTime = dateTimeProvider.currentTimeMillis(),
                        firstWeekDaysOfUse = settings.firstWeekDaysOfUseGrowthData,
                        eventSent = settings.firstWeekPostInstallEverydayActivityAndSetToDefaultSent,
                    )
                }

                is Event.FirstWeekPostInstall.ConversionEvent10 -> {
                    shouldTrackFirstWeekLastDaysActivity(
                        currentTime = dateTimeProvider.currentTimeMillis(),
                        firstWeekDaysOfUse = settings.firstWeekDaysOfUseGrowthData,
                        eventSent = settings.firstWeekPostInstallLastThreeDaysActivitySent,
                    )
                }
            }
        }

    override suspend fun updateSentState(event: Event) = withContext(dispatcher) {
        when (event) {
            Event.GrowthData.ConversionEvent1 -> {
                settings.setAsDefaultGrowthSent = true
            }

            Event.GrowthData.ConversionEvent2 -> {
                settings.resumeGrowthLastSent = System.currentTimeMillis()
            }

            Event.GrowthData.ConversionEvent3 -> {
                settings.uriLoadGrowthLastSent = System.currentTimeMillis()
            }

            Event.GrowthData.ConversionEvent4 -> {
                settings.firstWeekSeriesGrowthSent = true
            }

            Event.GrowthData.ConversionEvent5 -> {
                settings.adClickGrowthSent = true
            }

            Event.GrowthData.ConversionEvent6 -> {
                settings.usageTimeGrowthSent = true
            }

            is Event.GrowthData.ConversionEvent7 -> {
                settings.growthUserActivatedSent = true
            }

            is Event.FirstWeekPostInstall.ConversionEvent8 -> {
                settings.firstWeekPostInstallRecurrentActivitySent = true
            }

            is Event.FirstWeekPostInstall.ConversionEvent9 -> {
                settings.firstWeekPostInstallEverydayActivityAndSetToDefaultSent = true
            }

            is Event.FirstWeekPostInstall.ConversionEvent10 -> {
                settings.firstWeekPostInstallLastThreeDaysActivitySent = true
            }
        }
    }

    private fun updatePersistentState(event: Event) {
        updateDaysOfUse()
        when (event) {
            is Event.GrowthData.ConversionEvent7 -> {
                if (event.fromSearch && shouldUpdateSearchUsage()) {
                    settings.growthEarlySearchUsed = true
                }
            }

            else -> Unit
        }
    }

    override fun tryRegisterAsUsageRecorder(application: Application) {
        // Currently there is only interest in measuring usage during the first day of install.
        if (!settings.usageTimeGrowthSent && System.currentTimeMillis().duringFirstDay()) {
            application.registerActivityLifecycleCallbacks(
                FirstDayUsageRecorder(
                    this,
                    duringFirstDay = { duringFirstDay() },
                ),
            )
        }
    }

    override fun updateUsageState(usageLength: Long) {
        settings.firstDayUsageTimeGrowthData += usageLength
    }

    private fun updateDaysOfUse() {
        val daysOfUse = settings.firstWeekDaysOfUseGrowthData
        val currentDate = Calendar.getInstance(Locale.US)
        val currentDateString = dateFormatter.format(currentDate.time)
        if (currentDate.timeInMillis.duringFirstWeek() && daysOfUse.none { it == currentDateString }) {
            settings.firstWeekDaysOfUseGrowthData = daysOfUse + currentDateString
        }
    }

    private fun hasBeenActiveThreeDaysInFirstWeek(): Boolean = Result.runCatching {
        val distinctDaysCount = settings.firstWeekDaysOfUseGrowthData
            .asSequence()
            .mapNotNull { dateFormatter.parse(it) }
            .map { it.time.toCalendar() }
            .map { cal ->
                Triple(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                )
            }
            .distinct()
            .count()

        distinctDaysCount >= MINIMUM_DAYS_IN_FIRST_WEEK_SERIES
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

        return activeInFirstPartOfTheWeek(firstWeekDaysOfUse) && activeInLastPartOfTheWeek(
            firstWeekDaysOfUse,
        )
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

    private fun getInstalledTimeToMidnight() = getInstalledTime().toMidnight()

    private fun Long.toMidnight(): Long = this.toCalendar().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun shouldUpdateSearchUsage(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime.afterThirdDay() &&
                currentTime.duringFirstWeek()
    }

    /**
     * Stores first day app usage time to disk, based on Resume and Pause lifecycle events.
     */
    internal class FirstDayUsageRecorder(
        private val metricsStorage: MetricsStorage,
        private val duringFirstDay: Long.() -> Boolean,
        private val dateTimeProvider: DateTimeProvider = DefaultDateTimeProvider(),
    ) : DefaultActivityLifecycleCallbacks {
        private val activityStartTimes: MutableMap<String, Long?> = mutableMapOf()
        private var resumedDuringFirstDay: Boolean = false

        override fun onActivityResumed(activity: Activity) {
            super.onActivityResumed(activity)
            val currentTime = dateTimeProvider.currentTimeMillis()
            activityStartTimes[activity.componentName.toString()] = currentTime
            resumedDuringFirstDay = currentTime.duringFirstDay()
        }

        override fun onActivityPaused(activity: Activity) {
            super.onActivityPaused(activity)
            val startTime = activityStartTimes[activity.componentName.toString()] ?: return
            val elapsedTimeMillis = dateTimeProvider.currentTimeMillis() - startTime
            if (resumedDuringFirstDay) {
                metricsStorage.updateUsageState(elapsedTimeMillis)
            }
        }
    }

    companion object {
        private const val DAY_MILLIS: Long = 1000 * 60 * 60 * 24
        private const val THREE_DAY_MILLIS: Long = 3 * DAY_MILLIS
        private const val FOUR_DAY_MILLIS: Long = 4 * DAY_MILLIS
        private const val SHORTEST_MONTH_MILLIS: Long = DAY_MILLIS * 28

        private const val MINIMUM_DAYS_IN_FIRST_WEEK_SERIES = 3

        // Note this is 8 so that recording of FirstWeekSeriesActivity happens throughout the length
        // of the 7th day after install
        private const val FULL_WEEK_MILLIS: Long = DAY_MILLIS * 8

        private const val SEVEN_DAYS_MILLIS: Long = DAY_MILLIS * 7

        // Minimum active days required for recurrent activity.
        private const val MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY = 2

        private const val NUMBER_OF_DAYS_IN_A_WEEK = 7

        /**
         * Determines whether marketing events should be tracked.
         */
        fun shouldSendGenerally(context: Context): Boolean {
            return false
        }

        fun getInstalledTime(context: Context): Long = context.packageManagerCompatHelper
            .getPackageInfoCompat(context.packageName, 0)
            .firstInstallTime
    }
}
