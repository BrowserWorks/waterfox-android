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

internal class DefaultMetricsStorage(
    context: Context,
    private val settings: Settings,
    private val checkDefaultBrowser: () -> Boolean,
    private val shouldSendGenerally: () -> Boolean = { shouldSendGenerally(settings) },
    private val getInstalledTime: () -> Long = { getInstalledTime(context) },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val dateTimeProvider: DateTimeProvider = DefaultDateTimeProvider(),
) : MetricsStorage {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val installAge by lazy { InstallAge(getInstalledTime()) }

    /**
     * Checks local state to see whether the [event] should be sent.
     */
    @Suppress("CyclomaticComplexMethod")
    override suspend fun shouldTrack(event: Event): Boolean =
        withContext(dispatcher) {
            // Update the persistent state before checking if we should track.
            updatePersistentState(event)
            val currentTime = dateTimeProvider.currentTimeMillis()
            shouldSendGenerally() && when (event) {
                Event.GrowthData.ConversionEvent1 -> {
                    installAge.isDuringFirstMonth(currentTime) &&
                            !settings.setAsDefaultGrowthSent &&
                            checkDefaultBrowser()
                }

                Event.GrowthData.ConversionEvent2 -> {
                    isInsideGrowthTrackingPeriod(currentTime) &&
                        hasBeenMoreThanDaySince(settings.resumeGrowthLastSent)
                }

                Event.GrowthData.ConversionEvent3 -> {
                    isInsideGrowthTrackingPeriod(currentTime) &&
                        hasBeenMoreThanDaySince(settings.uriLoadGrowthLastSent)
                }

                Event.GrowthData.ConversionEvent4 -> {
                    installAge.isDuringFirstWeek(currentTime) &&
                            !settings.firstWeekSeriesGrowthSent &&
                            hasBeenActiveThreeDaysInFirstWeek()
                }

                Event.GrowthData.ConversionEvent5 -> {
                    installAge.isDuringFirstMonth(currentTime) && !settings.adClickGrowthSent
                }

                Event.GrowthData.ConversionEvent6 -> {
                    !settings.usageTimeGrowthSent
                }

                is Event.GrowthData.ConversionEvent7 -> {
                    installAge.isDuringFirstWeek(currentTime) &&
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
                settings.resumeGrowthLastSent = dateTimeProvider.currentTimeMillis()
            }

            Event.GrowthData.ConversionEvent3 -> {
                settings.uriLoadGrowthLastSent = dateTimeProvider.currentTimeMillis()
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
        val currentTime = dateTimeProvider.currentTimeMillis()
        // Currently there is only interest in measuring usage during the first day of install.
        if (!settings.usageTimeGrowthSent && installAge.isDuringFirstDay(currentTime)) {
            application.registerActivityLifecycleCallbacks(
                FirstDayUsageRecorder(
                    this,
                    duringFirstDay = { installAge.isDuringFirstDay(this) },
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
        if (installAge.isDuringFirstWeek(currentDate.timeInMillis) && daysOfUse.none { it == currentDateString }) {
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
        if (!installAge.isDuringFirst7Days(currentTime) || eventSent) {
            return false
        }

        return firstWeekDaysOfUse.toTimestamps().any { date ->
            installAge.isDuringLastThreeDays(date)
        }
    }.getOrDefault(false)

    @VisibleForTesting
    internal fun shouldTrackFirstWeekRecurrentlyActivity(
        currentTime: Long,
        firstWeekDaysOfUse: Set<String>,
        eventSent: Boolean,
    ): Boolean = Result.runCatching {
        if (!installAge.isDuringFirst7Days(currentTime) || eventSent) {
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
        if (!installAge.isDuringFirst7Days(currentTime) || eventSent) {
            return false
        }

        updateIsDefaultBrowserDuringFirstFourDays(
            isDefaultBrowserDuringFirstFourDay = isBrowserSetToDefaultDuringFirstFourDays,
            currentTime = currentTime,
        )

        val isAllWeekActive = firstWeekDaysOfUse.toTimestamps().count { date ->
            installAge.isDuringFirst7Days(date)
        } == NUMBER_OF_DAYS_IN_A_WEEK

        return isBrowserSetToDefaultDuringFirstFourDays && isAllWeekActive
    }.getOrDefault(false)

    @VisibleForTesting
    internal fun activeInFirstPartOfTheWeek(firstWeekDaysOfUse: Set<String>): Boolean =
        firstWeekDaysOfUse.toTimestamps()
            .count { date -> installAge.isDuringFirstFourDays(date) } >= MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY

    @VisibleForTesting
    internal fun activeInLastPartOfTheWeek(firstWeekDaysOfUse: Set<String>): Boolean =
        firstWeekDaysOfUse.toTimestamps()
            .count { date -> installAge.isDuringLastThreeDays(date) } >= MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY

    @VisibleForTesting
    internal fun updateIsDefaultBrowserDuringFirstFourDays(
        currentTime: Long,
        isDefaultBrowserDuringFirstFourDay: Boolean,
        isDefaultBrowser: Boolean = checkDefaultBrowser(),
    ) {
        val shouldUpdate = !isDefaultBrowserDuringFirstFourDay &&
                isDefaultBrowser &&
                installAge.isDuringFirstFourDays(currentTime)

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

    private fun hasBeenMoreThanDaySince(lastSentTimestamp: Long): Boolean {
        return (dateTimeProvider.currentTimeMillis() - lastSentTimestamp) >= DAY_MILLIS
    }

    private fun shouldUpdateSearchUsage(): Boolean {
        val currentTime = dateTimeProvider.currentTimeMillis()
        return installAge.isAfterThirdDay(currentTime) &&
            installAge.isDuringFirstWeek(currentTime)
    }

    private fun isInsideGrowthTrackingPeriod(currentTime: Long): Boolean {
        return installAge.isAfterFirstDay(currentTime) &&
            installAge.isDuringFirstMonth(currentTime)
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
        private const val MINIMUM_DAYS_IN_FIRST_WEEK_SERIES = 3

        // Minimum active days required for recurrent activity.
        private const val MINIMUM_ACTIVE_DAYS_FOR_RECURRENT_ACTIVITY = 2

        private const val NUMBER_OF_DAYS_IN_A_WEEK = 7

        /**
         * Determines whether marketing events should be tracked.
         */
        fun shouldSendGenerally(settings: Settings): Boolean {
            return false
        }

        fun getInstalledTime(context: Context): Long = context.packageManagerCompatHelper
            .getPackageInfoCompat(context.packageName, 0)
            .firstInstallTime
    }
}
