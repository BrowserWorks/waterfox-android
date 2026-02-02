/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.nimbus.messaging

import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.test.runTest
import mozilla.components.support.test.any
import mozilla.components.support.test.eq
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mozilla.experiments.nimbus.NullVariables
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class NimbusMessagingControllerTest {

    private val storage: NimbusMessagingStorage = mock(NimbusMessagingStorage::class.java)

    private val deepLinkScheme = "deepLinkScheme"
    private val controller = NimbusMessagingController(storage, deepLinkScheme)

    @Before
    fun setup() {
        NullVariables.instance.setContext(testContext)
    }

    @Test
    fun `GIVEN message not expired WHEN calling onMessageDisplayed THEN update storage`() =
        runTest {
            val message = createMessage("id-1", style = StyleData(maxDisplayCount = 2))
            val displayedMessage = createMessage("id-1", style = StyleData(maxDisplayCount = 2), displayCount = 1)
            `when`(storage.onMessageDisplayed(eq(message), any())).thenReturn(displayedMessage)

            val actualMessage = controller.onMessageDisplayed(message)

            assertEquals(displayedMessage, actualMessage)
            verify(storage).onMessageDisplayed(eq(message), any())
        }

    @Test
    fun `GIVEN message is expired WHEN calling onMessageDisplayed THEN update storage`() =
        runTest {
            val message =
                createMessage("id-1", style = StyleData(maxDisplayCount = 1), displayCount = 0)
            val displayedMessage = createMessage("id-1", style = StyleData(maxDisplayCount = 1), displayCount = 1)
            `when`(storage.onMessageDisplayed(any(), any())).thenReturn(displayedMessage)

            val actualMessage = controller.onMessageDisplayed(message)

            assertEquals(displayedMessage, actualMessage)
            verify(storage).onMessageDisplayed(eq(message), any())
        }

    @Test
    fun `WHEN calling onMessageDismissed THEN update metadata`() =
        runTest {
            val message = createMessage("id-1")

            controller.onMessageDismissed(message)

            verify(storage).updateMetadata(message.metadata.copy(dismissed = true))
        }

    @Test
    fun `WHEN calling onMicrosurveyDismissed THEN update metadata`() =
        runTest {
            val message = createMessage("id-1")

            controller.onMicrosurveyDismissed(message)

            verify(storage).updateMetadata(message.metadata.copy(dismissed = true))
        }

    @Test
    fun `GIVEN action is URL WHEN calling processMessageActionToUri THEN return a deeplink URI`() {
        val message = createMessage("id-1")

        `when`(storage.generateUuidAndFormatMessage(message))
            .thenReturn(Pair(null, "://mock-uri"))

        val expectedUri = "$deepLinkScheme://mock-uri".toUri()

        val actualUri = controller.processMessageActionToUri(message)

        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `GIVEN a URL with a {uuid} WHEN calling processMessageActionToUri THEN return the formatted URI`() {
        val url = "http://mozilla.org?uuid={uuid}"
        val message = createMessage("id-1", action = "://open", messageData = MessageData(actionParams = mapOf("url" to url)))
        val uuid = UUID.randomUUID().toString()
        `when`(storage.generateUuidAndFormatMessage(message)).thenReturn(Pair(uuid, "://mock-uri"))

        val expectedUri = "$deepLinkScheme://mock-uri".toUri()

        val actualUri = controller.processMessageActionToUri(message)

        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `GIVEN action is deeplink WHEN calling processMessageActionToUri THEN return a deeplink URI`() {
        val message = createMessage("id-1", action = "://a-deep-link")
        `when`(storage.generateUuidAndFormatMessage(message))
            .thenReturn(Pair(null, message.action))

        val expectedUri = "$deepLinkScheme${message.action}".toUri()
        val actualUri = controller.processMessageActionToUri(message)

        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `GIVEN action unknown format WHEN calling processMessageActionToUri THEN return the action URI`() {
        val message = createMessage("id-1", action = "unknown")
        `when`(storage.generateUuidAndFormatMessage(message))
            .thenReturn(Pair(null, message.action))

        val expectedUri = message.action.toUri()
        val actualUri = controller.processMessageActionToUri(message)

        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `GIVEN message WHEN calling onMessageClicked THEN update stored metadata for message`() =
        runTest {
            val message = createMessage("id-1")
            assertFalse(message.metadata.pressed)

            controller.onMessageClicked(message)

            val updatedMetadata = message.metadata.copy(pressed = true)
            verify(storage).updateMetadata(updatedMetadata)
        }



    @Test
    fun `WHEN getIntentForMessageAction is called THEN return a generated Intent with the processed Message action`() {
        val message = createMessage("id-1", action = "unknown")
        `when`(storage.generateUuidAndFormatMessage(message))
            .thenReturn(Pair(null, message.action))

        val actualIntent = controller.getIntentForMessage(message)

        assertEquals(Intent.ACTION_VIEW, actualIntent.action)
        val expectedUri = message.action.toUri()
        assertEquals(expectedUri, actualIntent.data)
    }

    @Test
    fun `GIVEN stored messages contains a matching message WHEN calling getMessage THEN return the matching message`() =
        runTest {
            val message1 = createMessage("1")
            `when`(storage.getMessage(message1.id)).thenReturn(message1)
            val actualMessage = controller.getMessage(message1.id)

            assertEquals(message1, actualMessage)
        }

    @Test
    fun `GIVEN stored messages doesn't contain a matching message WHEN calling getMessage THEN return null`() =
        runTest {
            `when`(storage.getMessage("unknown id")).thenReturn(null)
            val actualMessage = controller.getMessage("unknown id")

            assertNull(actualMessage)
        }

    private fun createMessage(
        id: String,
        messageData: MessageData = MessageData(),
        action: String = messageData.action,
        style: StyleData = StyleData(),
        displayCount: Int = 0,
    ): Message =
        Message(
            id,
            data = messageData,
            style = style,
            metadata = Message.Metadata(id, displayCount),
            triggerIfAll = emptyList(),
            excludeIfAny = emptyList(),
            action = action,
        )
}
