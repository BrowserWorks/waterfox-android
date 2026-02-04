/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.messaging

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.ext.components
import java.util.concurrent.CancellationException

const val CLICKED_MESSAGE_ID = "clickedMessageId"
const val DISMISSED_MESSAGE_ID = "dismissedMessageId"

private const val NIMBUS_UPDATE_OPERATION_TIMEOUT_MILLIS = 6500L

private val LOGGER = Logger("MessageNotificationWorker")

/**
 * Retained so previously scheduled marketing work completes without fetching or displaying messages.
 */
class MessageNotificationWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = Result.success()

    companion object {
        private const val MESSAGE_WORK_NAME = "org.mozilla.fenix.message.work"

        /**
         * Cancels legacy work instead of scheduling marketing notifications.
         */
        fun setMessageNotificationWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(MESSAGE_WORK_NAME)
        }

        /**
         * Cancel the message notification [CoroutineWorker] to stop polling Nimbus.
         */
        @Suppress("TooGenericExceptionCaught")
        suspend fun cancelMessageNotificationWorker(context: Context) {
            LOGGER.info("Canceling the message notification worker...")

            try {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(MESSAGE_WORK_NAME)
                    .await()

                LOGGER.info("Message notification worker cancellation completed.")
            } catch (e: CancellationException) {
                LOGGER.debug(
                    "Stopped waiting for message notification worker cancellation because" +
                        " the coroutine was cancelled.",
                )

                // Rethrow the exception so it propagates instead of being swallowed by the
                // operation-failure branch below.
                throw e
            } catch (e: Exception) {
                LOGGER.error("Failed to cancel the message notification worker.", e)
            }
        }

        @VisibleForTesting
        @Suppress("UNUSED_PARAMETER")
        internal suspend fun tryFetchAndApplyNimbusExperiments(
            nimbusSdk: NimbusApi,
            operationTimeout: Long = NIMBUS_UPDATE_OPERATION_TIMEOUT_MILLIS,
            experimentsFetched: CompletableDeferred<Unit> = CompletableDeferred(),
            experimentsApplied: CompletableDeferred<Unit> = CompletableDeferred(),
        ): Boolean = false
    }
}

/**
 * When a [Message] [Notification] is dismissed by the user record telemetry data and update the
 * [Message.metadata].
 *
 * This Service handles notifications created by earlier versions of [MessageNotificationWorker].
 */
class NotificationDismissedService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            val messaging = applicationContext.components.nimbus.messaging

            lifecycleScope.launch {
                // Get the relevant message.
                val message = intent.getStringExtra(DISMISSED_MESSAGE_ID)?.let { messageId ->
                    messaging.getMessage(messageId)
                }

                if (message != null) {
                    withContext(Dispatchers.IO) {
                        // Update message as 'dismissed'.
                        messaging.onMessageDismissed(message)
                    }
                }
            }
        }

        return START_REDELIVER_INTENT
    }
}

/**
 * When a [Message] [Notification] is clicked by the user record telemetry data and update the
 * [Message.metadata].
 *
 * This Activity handles notifications created by earlier versions of [MessageNotificationWorker].
 */
class NotificationClickedReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val messaging = applicationContext.components.nimbus.messaging

        lifecycleScope.launch {
            // Get the relevant message.
            val message = intent.getStringExtra(CLICKED_MESSAGE_ID)?.let { messageId ->
                messaging.getMessage(messageId)
            }

            if (message != null) {
                withContext(Dispatchers.IO) {
                    // Update message as 'clicked'.
                    messaging.onMessageClicked(message)
                }

                // Create the intent.
                val intent = messaging.getIntentForMessage(message)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // Start the message intent.
                startActivity(intent)
            }
        }

        // End this activity.
        finish()
    }
}
