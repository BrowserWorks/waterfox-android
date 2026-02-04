/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import mozilla.components.lib.installation.blockstore.InstallBlockStore
import mozilla.components.lib.installation.blockstore.InstallState

/**
 * A Worker that records an app open and submits the installation ping on the first open after a fresh install.
 *
 * @param context The application context.
 * @param workerParameters Setup parameters for a [CoroutineWorker].
 * @param blockStore Records the app open, returning the [InstallState] on the first open after a fresh install.
 * @param ioDispatcher The dispatcher the app open is recorded on.
 */
class InstallationWorker
@JvmOverloads
constructor(
    context: Context,
    workerParameters: WorkerParameters,
    private val blockStore: InstallBlockStore = InstallBlockStore.create(context),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = Result.success()

    /*
    override suspend fun doWork(): Result =
        withContext(ioDispatcher) {
            blockStore
                .recordAppOpen()
                .fold(
                    onSuccess = { installState ->
                        installState?.let {
                            Installation.installCounter.set(it.installCount.toLong())
                            it.previousLastSeenDate?.let { date -> Installation.lastSeenDate.set(date) }
                            Pings.installation.submit()
                        }
                        Result.success()
                    },
                    onFailure = {
                        if (runAttemptCount < InstallationMetricsService.MAX_RETRIES) {
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    },
                )
        }
    */
}
