/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.nimbus.messaging

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri

/**
 * Bookkeeping for message actions in terms of the messaging store.
 *
 * @param messagingStorage a NimbusMessagingStorage instance.
 * @param deepLinkScheme the deepLinkScheme for the app.
 */
open class NimbusMessagingController(
    private val messagingStorage: NimbusMessagingStorage,
    private val deepLinkScheme: String,
) : NimbusMessagingControllerInterface {
    override suspend fun onMessageDisplayed(displayedMessage: Message, bootIdentifier: String?): Message {
        return messagingStorage.onMessageDisplayed(displayedMessage, bootIdentifier)
    }

    override suspend fun onMessageDismissed(message: Message) {
        val messageMetadata = message.metadata
        val updatedMetadata = messageMetadata.copy(dismissed = true)
        messagingStorage.updateMetadata(updatedMetadata)
    }

    override suspend fun onMicrosurveyCompleted(message: Message) {
        val messageMetadata = message.metadata
        val updatedMetadata = messageMetadata.copy(pressed = true)
        messagingStorage.updateMetadata(updatedMetadata)
    }

    /**
     * Called once the user has clicked on a message.
     *
     * This records that the message has been clicked on, but does not record a
     * glean event. That should be done via [processMessageActionToUri].
     */
    override suspend fun onMessageClicked(message: Message) {
        val messageMetadata = message.metadata
        val updatedMetadata = messageMetadata.copy(pressed = true)
        messagingStorage.updateMetadata(updatedMetadata)
    }

    override suspend fun onMicrosurveyDismissed(message: Message) {
        val messageMetadata = message.metadata
        val updatedMetadata = messageMetadata.copy(dismissed = true)
        messagingStorage.updateMetadata(updatedMetadata)
    }

    override fun getIntentForMessage(message: Message) = Intent(
        Intent.ACTION_VIEW,
        processMessageActionToUri(message),
    )

    override suspend fun getMessage(id: String): Message? {
        return messagingStorage.getMessage(id)
    }

    /**
     * The [message] action needs to be examined for string substitutions.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun processMessageActionToUri(message: Message): Uri {
        val action = messagingStorage.generateUuidAndFormatMessage(message).second

        return convertActionIntoDeepLinkSchemeUri(action)
    }

    private fun convertActionIntoDeepLinkSchemeUri(action: String): Uri =
        if (action.startsWith("://")) {
            "$deepLinkScheme$action".toUri()
        } else {
            action.toUri()
        }

    override suspend fun getMessages(): List<Message> =
        messagingStorage.getMessages()

    override suspend fun getNextMessage(surfaceId: MessageSurfaceId) =
        getNextMessage(surfaceId, getMessages())

    override fun getNextMessage(surfaceId: MessageSurfaceId, messages: List<Message>) =
        messagingStorage.getNextMessage(surfaceId, messages)
}
