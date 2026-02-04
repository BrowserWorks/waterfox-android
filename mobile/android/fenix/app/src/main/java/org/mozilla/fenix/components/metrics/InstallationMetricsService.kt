/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import mozilla.components.support.utils.RunWhenReadyQueue

/**
 * Metrics service that records an app open once per process and schedules the worker, after the app is visually
 * complete. The worker is retried with exponential backoff.
 *
 * @param runWhenReadyQueue Schedules the worker once the app is visually complete.
 * @param workManager Provides the [WorkManager] the worker is scheduled with.
 * @param isEnabled Whether installation persistence is enabled.
 */
class InstallationMetricsService(
    private val runWhenReadyQueue: RunWhenReadyQueue,
    private val workManager: () -> WorkManager,
    private val isEnabled: () -> Boolean = { false },
) : MetricsService {
    override val type = MetricServiceType.Data

    private var started = false
    private var stopped = false

    override fun start() {
        stop()
        /*
        if (started) {
            return
        }

        started = true
        runWhenReadyQueue.runIfReadyOrQueue {
            if (stopped) {
                return@runIfReadyOrQueue
            }

            if (!isEnabled()) {
                logger.debug("Installation persistence disabled, skipping")
                return@runIfReadyOrQueue
            }

            scheduleWorker()
        }
        */
    }

    override fun stop() {
        stopped = true
        workManager().cancelUniqueWork(WORK_NAME)
    }

    private fun scheduleWorker() {
        val workRequest =
            OneTimeWorkRequestBuilder<InstallationWorker>()
                .setInitialDelay(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS,
                )
                .build()

        workManager()
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
    }

    override fun track(event: Event) = Unit

    override fun shouldTrack(event: Event): Boolean = false

    companion object {
        internal const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_SECONDS = 5L
        private const val INITIAL_BACKOFF_DELAY_SECONDS = 5L
        internal const val WORK_NAME = "org.mozilla.fenix.metrics.installation.work"
    }
}
