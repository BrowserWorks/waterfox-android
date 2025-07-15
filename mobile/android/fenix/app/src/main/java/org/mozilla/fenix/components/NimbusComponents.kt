/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.nimbus.NimbusDisabled
import mozilla.components.service.nimbus.messaging.FxNimbusMessaging
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.Message.Metadata
import mozilla.components.service.nimbus.messaging.MessageData
import mozilla.components.service.nimbus.messaging.MessageMetadataStorage
import mozilla.components.service.nimbus.messaging.MessageSurfaceId
import mozilla.components.service.nimbus.messaging.NimbusMessagingController
import mozilla.components.service.nimbus.messaging.NimbusMessagingControllerInterface
import mozilla.components.service.nimbus.messaging.NimbusMessagingStorage
import mozilla.components.service.nimbus.messaging.OnDiskMessageMetadataStorage
import mozilla.components.service.nimbus.messaging.StyleData
import org.mozilla.experiments.nimbus.NimbusEventStore
import org.mozilla.experiments.nimbus.NimbusMessagingHelperInterface
import org.mozilla.experiments.nimbus.NullNimbus
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.experiments.createNimbus
import org.mozilla.fenix.messaging.CustomAttributeProvider
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.perf.lazyMonitored
import org.mozilla.experiments.nimbus.NullVariables

/**
 * Provides sensible defaults for all FxNimbus features when Nimbus is disabled.
 * This enables features with privacy-focused defaults instead of disabling them entirely.
 */
object FxNimbusDefaults {
    // Fingerprinting Protection - enabled by default for privacy
    val fingerprintingProtectionEnabled = true
    val fingerprintingProtectionNormal = true
    val fingerprintingProtectionPrivate = true
    val fdlibmMathEnabled = true
    
    // Third-party Cookie Blocking - enabled by default for privacy
    val thirdPartyCookieBlockingEnabled = true
    val thirdPartyCookieBlockingNormal = true
    val thirdPartyCookieBlockingPrivate = true
    
    // Networking features - enabled for better performance
    val fetchPriorityEnabled = true
    val parallelMarkingEnabled = true
    val dohAutoselectEnabled = true
    val bannedPortList = ""
    
    // PKI and Crypto - enabled for security
    val certificateTransparencyMode = 1 // Enable CT
    val postQuantumKeyExchangeEnabled = true
    
    // Search features - enabled for better UX
    val suggestShippedDomainsEnabled = true
    val searchExtraParamsEnabled = false // Disabled for privacy
    
    // User characteristics - disabled for privacy
    val userCharacteristicsCurrentVersion = 1
    
    // Sent from Firefox - enabled with Waterfox branding
    val sentFromFirefoxEnabled = true
    val sentFromFirefoxShowSnackbar = true
    val sentFromFirefoxTemplateMessage = "%s\n\nSent from %s: %s"
    val sentFromFirefoxDownloadLink = "https://www.waterfox.net/download/"
    
    // Splash screen - reasonable timeout
    val splashScreenMaximumDurationMs = 3000
    
    // App launcher icon - disabled by default
    val alternativeAppLauncherIconEnabled = false
    val alternativeAppLauncherIconResetToDefault = false
    
    // Homepage features - enabled for better UX
    val homepageHideFrecentTopSitesEnabled = false
    val homepageAsNewTabEnabled = false
    val homepageSearchBarEnabled = true
    
    // Translations - enabled for better UX
    val translationsMainFlowToolbarEnabled = true
    val translationsMainFlowBrowserMenuEnabled = true
    
    // Print - enabled
    val printBrowserPrintEnabled = true
    
    // Tab strip - disabled by default
    val tabStripEnabled = false
    
    // Menu redesign - enabled
    val menuRedesignReportSiteIssue = true
    
    // Glean telemetry - privacy-focused defaults
    val gleanEnableEventTimestamps = false
    val gleanDelayPingLifetimeIo = true
    val gleanPingLifetimeThreshold = 1000
    val gleanPingLifetimeMaxTime = 60000
    val gleanMetricsEnabled = emptyMap<String, Boolean>()
    
    // Growth data - disabled for privacy
    val growthDataEnabled = false
    
    // Encourage search CFR - disabled
    val encourageSearchCfrEnabled = false
}

/**
 * Component group for access to Nimbus and other Nimbus services.
 */
class NimbusComponents(private val context: Context) {

    /**
     * The main entry point for the Nimbus SDK. Note that almost all access to feature configuration
     * should be mediated through a FML generated class, e.g. [FxNimbus].
     */
    val sdk: NimbusApi by lazyMonitored {
        NimbusDisabled(context).also { api ->
            // Initialize FxNimbus with disabled API to prevent crashes
            FxNimbus.initialize { api }
            // Set context for NullVariables to prevent NimbusFeatureException
            NullVariables.instance.setContext(context)
        }
    }

    /**
     * Convenience method for getting the event store from the SDK.
     *
     * Before EXP-4354, this is the main write API for recording events to drive
     * messaging, experiments and onboarding.
     *
     * Following EXP-4354, clients will not need to write these events
     * themselves.
     *
     * Read access to the event store should be done through
     * the JEXL helper available from [createJexlHelper].
     */
    val events: NimbusEventStore by lazyMonitored {
        NullNimbus(context)
        //sdk.events
    }

    /**
     * Create a new JEXL evaluator suitable for use by any feature.
     *
     * JEXL evaluator context is provided by the app and changes over time.
     *
     * For this reason, an evaluator should be not be stored or cached.
     *
     * Since it has a native peer, to avoid leaking memory, the helper's [destroy] method
     * should be called after finishing the set of evaluations.
     *
     * This can be done automatically using the interface's `use` method, e.g.
     *
     * ```
     * val isEligible = nimbus.createJexlHelper().use { helper ->
     *    expressions.all { exp -> helper.evalJexl(exp) }
     * }
     * ```
     *
     * The helper has access to all context needed to drive decisions
     * about messaging, onboarding and experimentation.
     *
     * It also has a built-in cache.
     */
    fun createJexlHelper(): NimbusMessagingHelperInterface =
        messagingStorage.createMessagingHelper()

    /**
     * The main entry point for UI surfaces to interact with (get, click, dismiss) messages
     * from the Nimbus Messaging component.
     */
    val messaging: NimbusMessagingControllerInterface by lazyMonitored {
        NullNimbusMessagingController(
            messagingStorage = messagingStorage,
            deepLinkScheme = BuildConfig.DEEP_LINK_SCHEME,
        )
    }

    /**
     * Low level access to the messaging component.
     *
     * The app should access this through a [mozilla.components.service.nimbus.messaging.NimbusMessagingController].
     */
    private val messagingStorage by lazyMonitored {
        NimbusMessagingStorage(
            context = context,
            metadataStorage = NullMessageMetadataStorage(), //OnDiskMessageMetadataStorage(context),
            nimbus = sdk,
            messagingFeature = FxNimbusMessaging.features.messaging,
            attributeProvider = CustomAttributeProvider,
        )
    }
}
// Noop impl of MessageMetadataStorage to replace OnDiskMessageMetadataStorage
class NullMessageMetadataStorage(): MessageMetadataStorage {
    override suspend fun getMetadata(): Map<String, Message.Metadata> {
        var metadataMap: MutableMap<String, Message.Metadata> = hashMapOf()
        return metadataMap
    }

    override suspend fun addMetadata(metadata: Message.Metadata): Message.Metadata {
        return metadata
    }

    override suspend fun updateMetadata(metadata: Message.Metadata) {
        // noop
    }
}

class NullNimbusMessagingController(
    messagingStorage: NimbusMessagingStorage,
    deepLinkScheme: String,
) : NimbusMessagingController(messagingStorage, deepLinkScheme) {

    private val nullMessage: Message = Message(
        id = "",
        data = MessageData(),
        action = "",
        style = StyleData(),
        triggerIfAll = listOf(),
        excludeIfAny = listOf(),
        metadata = Metadata(""),
    )

    override suspend fun onMessageDisplayed(displayedMessage: Message, bootIdentifier: String?): Message {
        return nullMessage
    }

    /**
     * Called when a message has been dismissed by the user.
     *
     * Records a messageDismissed event, and records that the message
     * has been dismissed.
     */
    override suspend fun onMessageDismissed(message: Message) {
        return
    }

    /**
     * Called when a microsurvey attached to a message has been completed by the user.
     *
     * @param message The message containing the microsurvey that was completed.
     * @param answer The user's response to the microsurvey question.
     */
    override suspend fun onMicrosurveyCompleted(message: Message, answer: String) {
        return
    }

    /**
     * Called once the user has clicked on a message.
     *
     * This records that the message has been clicked on, but does not record a
     * glean event. That should be done via [processMessageActionToUri].
     */
    override suspend fun onMessageClicked(message: Message) {
        return
    }

    /**
     * Create and return the relevant [Intent] for the given [Message].
     *
     * @param message the [Message] to create the [Intent] for.
     * @return an [Intent] using the processed [Message].
     */
    override fun getIntentForMessage(message: Message) = Intent()

    /**
     * Will attempt to get the [Message] for the given [id].
     *
     * @param id the [Message.id] of the [Message] to try to match.
     * @return the [Message] with a matching [id], or null if no [Message] has a matching [id].
     */
    override suspend fun getMessage(id: String): Message? {
        return nullMessage
    }

    /**
     * The [message] action needs to be examined for string substitutions
     * and any `uuid` needs to be recorded in the Glean event.
     *
     * We call this `process` as it has a side effect of logging a Glean event while it
     * creates a URI string for the message action.
     */
    override fun processMessageActionToUri(message: Message): Uri {
        return Uri.EMPTY
    }

    override fun sendDismissedMessageTelemetry(messageId: String) {
        return
    }

    override fun sendShownMessageTelemetry(messageId: String) {
        return
    }

    override fun sendExpiredMessageTelemetry(messageId: String) {
        return
    }

    override fun sendClickedMessageTelemetry(messageId: String, uuid: String?) {
        return
    }

    override fun sendMicrosurveyCompletedTelemetry(messageId: String, answer: String) {
        return
    }

    override fun convertActionIntoDeepLinkSchemeUri(action: String): Uri = Uri.EMPTY

    override suspend fun getMessages(): List<Message> = listOf()

    override suspend fun getNextMessage(surfaceId: MessageSurfaceId) = nullMessage

    override fun getNextMessage(surfaceId: MessageSurfaceId, messages: List<Message>) = nullMessage
}
