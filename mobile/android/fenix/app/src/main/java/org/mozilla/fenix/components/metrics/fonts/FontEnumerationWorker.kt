/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics.fonts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/** Retained so previously scheduled font work completes without collecting or sending data. */
class FontEnumerationWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result = Result.success()

    companion object {
        private const val FONT_ENUMERATOR_WORK_NAME = "org.mozilla.fenix.metrics.font.work"

        /** Cancels legacy work instead of scheduling font enumeration. */
        fun sendActivatedSignalIfNeeded(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(FONT_ENUMERATOR_WORK_NAME)
        }

        /** Legacy submission count retained for API compatibility; no submissions are made. */
        const val DESIRED_SUBMISSIONS_NUMBER: Int = 4
    }
}
