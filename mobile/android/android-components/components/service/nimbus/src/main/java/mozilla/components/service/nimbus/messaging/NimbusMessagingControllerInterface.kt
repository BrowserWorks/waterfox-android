/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.nimbus.messaging

import android.content.Intent

/**
 * API for interacting with the messaging component.
 *
 * The primary methods for interacting with the component are:
 * - [getNextMessage] to get the next [Message] for a given surface.
 * - [onMessageDisplayed] to be called when the message is displayed.
 * - [onMessageClicked] and [getIntentForMessage] to be called when the user taps on
 *  the message, and to get the action for the message.
 * - [onMessageDismissed] to be called when the user dismisses the message.
 * - [onMicrosurveyCompleted] to be called when a microsurvey has been completed by the user.
 */
interface NimbusMessagingControllerInterface {
    /**
     * Get all messages currently on the system. This includes any that have expired,
     * dismissed or clicked upon.
     */
    suspend fun getMessages(): List<Message>

    /**
     * Selects the next available message for the given surface from the given
     * list of messages.
     */
    fun getNextMessage(surfaceId: MessageSurfaceId, messages: List<Message>): Message?

    /**
     * A convenience method for `getNextMessage(surfaceId, getMessages())`.
     */
    suspend fun getNextMessage(surfaceId: MessageSurfaceId): Message?

    /**
     * Records metadata for a newly processed displayed message.
     */
    suspend fun onMessageDisplayed(displayedMessage: Message, bootIdentifier: String? = null): Message

    /**
     * Called when a message has been dismissed by the user.
     *
     * Records that the message has been dismissed.
     */
    suspend fun onMessageDismissed(message: Message)

    /**
     * Called when a microsurvey has been completed by the user.
     *
     * This function should be called when a microsurvey associated with a message
     * has been completed by the user.
     *
     * @param message The message associated with the microsurvey.
     */
    suspend fun onMicrosurveyCompleted(message: Message)

    /**
     * Called when a microsurvey has been dismissed by the user.
     *
     * @param id The id of the message associated with the microsurvey.
     */
    suspend fun onMicrosurveyDismissed(message: Message)

    /**
     * Called once the user has clicked on a message.
     *
     * This records that the message has been clicked on.
     */
    suspend fun onMessageClicked(message: Message)

    /**
     * Create and return the relevant [Intent] for the given [Message].
     *
     * @param message the [Message] to create the [Intent] for.
     * @return an [Intent] using the processed [Message].
     */
    fun getIntentForMessage(message: Message): Intent

    /**
     * Will attempt to get the [Message] for the given [id].
     *
     * @param id the [Message.id] of the [Message] to try to match.
     * @return the [Message] with a matching [id], or null if no [Message] has a matching [id].
     */
    suspend fun getMessage(id: String): Message?
}
