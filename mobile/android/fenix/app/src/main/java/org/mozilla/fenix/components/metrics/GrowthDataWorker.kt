/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import mozilla.components.support.utils.ext.packageManagerCompatHelper

/** Retained so previously scheduled growth work completes without collecting or sending data. */
class GrowthDataWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = Result.success()

    companion object {
        private const val GROWTH_USER_ACTIVATED_WORK_NAME = "org.mozilla.fenix.growth.work"

        /** Cancels legacy work instead of scheduling growth events. */
        fun sendActivatedSignalIfNeeded(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(GROWTH_USER_ACTIVATED_WORK_NAME)
        }
    }
}

private const val DAY_MILLIS: Long = 1000 * 60 * 60 * 24
private const val FULL_WEEK_MILLIS: Long = DAY_MILLIS * 7

/** Returns whether [now] is at least a full week after the application was first installed. */
internal fun isAfterFirstWeekFromInstall(
    context: Context,
    now: Long = System.currentTimeMillis(),
): Boolean = FULL_WEEK_MILLIS <= now - getInstalledTime(context)

private fun getInstalledTime(context: Context): Long =
    context.packageManagerCompatHelper.getPackageInfoCompat(context.packageName, 0).firstInstallTime
