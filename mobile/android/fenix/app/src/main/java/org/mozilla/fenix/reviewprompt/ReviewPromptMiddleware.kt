/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.reviewprompt

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.service.nimbus.evalJexlSafe
import mozilla.components.service.nimbus.messaging.use
import org.mozilla.experiments.nimbus.NimbusEventStore
import org.mozilla.experiments.nimbus.NimbusMessagingHelperInterface
import org.mozilla.fenix.GleanMetrics.CustomReviewPrompt
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppAction.ReviewPromptAction.CheckIfEligibleForReviewPrompt
import org.mozilla.fenix.components.appstate.AppAction.ReviewPromptAction.DoNotShowReviewPrompt
import org.mozilla.fenix.components.appstate.AppAction.ReviewPromptAction.ReviewPromptShown
import org.mozilla.fenix.components.appstate.AppAction.ReviewPromptAction.ShowCustomReviewPrompt
import org.mozilla.fenix.components.appstate.AppAction.ReviewPromptAction.ShowPlayStorePrompt
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.messaging.CustomAttributeProvider

private const val REVIEW_PROMPT_SHOWN_NIMBUS_EVENT_ID = "review_prompt_shown"

/**
 * [Middleware] evaluating the triggers to show a review prompt.
 *
 * @param continuousOnboardingInProgress If true then the prompt should not be shown.
 * @param shouldShowCustomPrompt If true enables showing custom prompt UI, if false falls back to Play Store prompt.
 * @param disableCustomPrompt Update settings to disable the custom prompt UI.
 * @param createJexlHelper Returns a helper for evaluating JEXL expressions.
 * @param buildTriggerMainCriteria Builds a sequence of trigger's main criteria that all need to be true.
 * @param buildTriggerSubCriteria Builds a sequence of trigger's sub-criteria.
 * Only one of these needs to be true (in addition to the main criteria).
 * @param nimbusEventStore [NimbusEventStore] used to record events evaluated in JEXL expressions.
 * @param isReviewPromptFeatureEnabled Whether review-prompt eligibility should be evaluated.
 */
class ReviewPromptMiddleware(
    private val continuousOnboardingInProgress: () -> Boolean = { false },
    private val shouldShowCustomPrompt: () -> Boolean,
    private val disableCustomPrompt: () -> Unit,
    private val createJexlHelper: () -> NimbusMessagingHelperInterface,
    private val buildTriggerMainCriteria: (NimbusMessagingHelperInterface) -> Sequence<Boolean> =
        TriggerBuilder::mainCriteria,
    private val buildTriggerSubCriteria: (NimbusMessagingHelperInterface) -> Sequence<Boolean> =
        TriggerBuilder::subCriteria,
    private val nimbusEventStore: NimbusEventStore,
    private val isReviewPromptFeatureEnabled: () -> Boolean = { false },
) : Middleware<AppState, AppAction> {

    private object TriggerBuilder {
        fun mainCriteria(jexlHelper: NimbusMessagingHelperInterface): Sequence<Boolean> {
            return sequence {
                yield(hasNotBeenPromptedLastFourMonths(jexlHelper))
                yield(hasBeenOpenedSeveralTimes(jexlHelper))
            }
        }

        fun subCriteria(jexlHelper: NimbusMessagingHelperInterface): Sequence<Boolean> {
            return sequence {
                yield(isDefaultBrowser(jexlHelper))
            }
        }
    }

    override fun invoke(
        store: Store<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        if (action !is AppAction.ReviewPromptAction) {
            next(action)
            return
        }

        when (action) {
            CheckIfEligibleForReviewPrompt -> handleReviewPromptCheck(store)
            ReviewPromptShown -> handleReviewPromptShown()
            DoNotShowReviewPrompt -> Unit
            ShowCustomReviewPrompt -> Unit
            ShowPlayStorePrompt -> Unit
        }

        next(action)
    }

    private fun handleReviewPromptCheck(store: Store<AppState, AppAction>) {
        // We shouldn't show the review prompt if continuous onboarding is in progress.
        if (continuousOnboardingInProgress()) {
            return
        }

        if (store.state.reviewPrompt != ReviewPromptState.Unknown) {
            // We only want to try to show it once to avoid unnecessary disk reads.
            return
        }

        if (!isReviewPromptFeatureEnabled()) {
            store.dispatch(DoNotShowReviewPrompt)
            return
        }

        val shouldShowPrompt: Boolean = createJexlHelper().use { jexlHelper ->
            val allMainCriteriaSatisfied = buildTriggerMainCriteria(jexlHelper).all { it }
            if (!allMainCriteriaSatisfied) {
                return@use false
            }

            val atLeastOneOfSubCriteriaSatisfied = buildTriggerSubCriteria(jexlHelper).any { it }
            return@use atLeastOneOfSubCriteriaSatisfied
        }

        if (shouldShowPrompt) {
            if (shouldShowCustomPrompt()) {
                store.dispatch(ShowCustomReviewPrompt)
            } else {
                store.dispatch(ShowPlayStorePrompt)
            }
        } else {
            store.dispatch(DoNotShowReviewPrompt)
        }
    }

    private fun handleReviewPromptShown() {
        nimbusEventStore.recordEventOrThrow(eventId = REVIEW_PROMPT_SHOWN_NIMBUS_EVENT_ID)
            .invokeOnCompletion { cause ->
                when (cause) {
                    null -> recordResult("success")
                    is CancellationException -> recordResult("cancelled")
                    else -> {
                        // Failed.
                        disableCustomPrompt()
                        recordResult("error")
                    }
                }
            }
    }

    /**
     * Send telemetry for a result of [NimbusEventStore.recordEventOrThrow].
     */
    private fun recordResult(result: String) {
        CustomReviewPrompt.nimbusEventRecorded.record(
            CustomReviewPrompt.NimbusEventRecordedExtra(result),
        )
        CustomReviewPrompt.recordNimbusEventAttempts[result].add(1)
    }
}

/**
 * Checks that the prompt hasn't been shown in the last 4 months
 * to avoid hitting the Play In-App Review API quota.
 *
 * Implementation note:
 * A month is tracked in Nimbus as a 28-day bucket, so converting it to 4 weeks doesn't change the semantics.
 * Because of how buckets are advanced, using a monthly bucket can be almost an entire month off, so using weeks
 * gives us better precision (means we might wait for 4 months and almost a week, instead of almost 5 months).
 * This is why I opted for 16 weeks instead of 4 months.
 *
 * See:
 * [Nimbus Bucket Advancement & Retention docs](https://experimenter.info/mobile-behavioral-targeting/#bucket-advancement--retention)
 */
fun hasNotBeenPromptedLastFourMonths(jexlHelper: NimbusMessagingHelperInterface): Boolean {
    return jexlHelper.evalJexlSafe("'$REVIEW_PROMPT_SHOWN_NIMBUS_EVENT_ID'|eventLastSeen('Weeks') > 16")
}

/**
 * The raw string "is_default_browser" must match the json value provided in
 * [CustomAttributeProvider.getCustomAttributes].
 */
@VisibleForTesting
internal fun isDefaultBrowser(jexlHelper: NimbusMessagingHelperInterface) =
    jexlHelper.evalJexlSafe("is_default_browser")

/**
 * Evaluates whether the user has opened the app at least 5 times.
 *
 * Note: Because Nimbus limits data to 4 calendar years, this will ignore app opens before then.
 */
@VisibleForTesting
internal fun hasBeenOpenedSeveralTimes(
    jexlHelper: NimbusMessagingHelperInterface,
): Boolean {
    return jexlHelper.evalJexlSafe("'app_opened'|eventSum('Years', 4, 0) >= 5")
}
