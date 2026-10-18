/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.termsofuse.experimentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mozilla.components.compose.base.LinkText
import mozilla.components.compose.base.LinkTextState
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/*
 * This file is to enable experimentation with the Terms of Use prompt content. Code here is likely
 * to be modified or removed entirely, hence why it's kept self-contained.
 */

enum class TermsOfUsePromptContentOption { VALUE_0, VALUE_1, VALUE_2 }

/**
 * Stores the configurable content of the Terms of Use prompt.
 *
 * @property title The prompt title.
 * @property learnMoreContent Composable content containing the "Learn more" copy and link.
 */
data class TermsOfUsePromptContent(
    val title: String,
    val learnMoreContent: @Composable () -> Unit,
)

@VisibleForTesting
internal fun String.toTermsOfUsePromptContentOption(): TermsOfUsePromptContentOption =
    TermsOfUsePromptContentOption.entries.firstOrNull { it.name == this } ?: TermsOfUsePromptContentOption.VALUE_0

/**
 * Gets the [TermsOfUsePromptContent] for the given [id], and calls [onLearnMoreClicked] when the "Learn more" link is
 * clicked.
 *
 * @param context The [Context] used to get the string resources.
 * @param id The persisted ID of the [TermsOfUsePromptContent].
 * @param onLearnMoreClicked The callback to be called when the "Learn more" link is clicked.
 */
internal fun getTermsOfUsePromptContent(
    context: Context,
    id: String,
    onLearnMoreClicked: () -> Unit,
): TermsOfUsePromptContent =
    when (id.toTermsOfUsePromptContentOption()) {
        TermsOfUsePromptContentOption.VALUE_0 -> getTreatmentC(context, onLearnMoreClicked)
        TermsOfUsePromptContentOption.VALUE_1 -> getTreatmentA(context, onLearnMoreClicked)
        TermsOfUsePromptContentOption.VALUE_2 -> getTreatmentB(context, onLearnMoreClicked)
    }

/**
 * Experimental configuration.
 *
 * Show the ToU prompt with the "Terms of Use" title and "You can learn more here." learn more text.
 */
internal fun getTreatmentA(
    context: Context,
    onLearnMoreClicked: () -> Unit,
): TermsOfUsePromptContent =
    TermsOfUsePromptContent(
        title = context.getString(R.string.terms_of_use_prompt_title_option_a),
        learnMoreContent = { LearnMoreContentAlternative(onLearnMoreClicked) },
    )

/**
 * Experimental configuration.
 *
 * Show the ToU prompt with "A note from Firefox" title and "You can learn more here." learn more text.
 */
internal fun getTreatmentB(
    context: Context,
    onLearnMoreClicked: () -> Unit,
): TermsOfUsePromptContent =
    TermsOfUsePromptContent(
        title =
            context.getString(
                R.string.terms_of_use_prompt_title_option_b,
                context.getString(R.string.firefox),
            ),
        learnMoreContent = { LearnMoreContentAlternative(onLearnMoreClicked) },
    )

@Composable
private fun LearnMoreContentAlternative(onLearnMoreClicked: () -> Unit) {
    LearnMoreContent(
        copyTextRes = R.string.terms_of_use_prompt_body_line_two_alternative,
        linkTextRes = R.string.terms_of_use_prompt_body_line_two_alternative_link,
        onLearnMoreClicked = onLearnMoreClicked,
    )
}

/**
 * Default configuration.
 *
 * Show the ToU prompt with the current defaults, "We’ve got an update" title and "Please take a moment to review and
 * accept. Learn more." learn more text.
 */
internal fun getTreatmentC(
    context: Context,
    onLearnMoreClicked: () -> Unit,
): TermsOfUsePromptContent =
    TermsOfUsePromptContent(
        title = context.getString(R.string.terms_of_use_prompt_title),
        learnMoreContent = {
            LearnMoreContent(
                copyTextRes = R.string.terms_of_use_prompt_message_2,
                linkTextRes = R.string.terms_of_use_prompt_link_learn_more,
                onLearnMoreClicked = onLearnMoreClicked,
            )
        },
    )

@Composable
private fun LearnMoreContent(
    @StringRes copyTextRes: Int,
    @StringRes linkTextRes: Int,
    onLearnMoreClicked: () -> Unit,
) {
    val linkText = stringResource(linkTextRes)

    val learnMoreLinkState =
        LinkTextState(
            text = linkText,
            url = "", // URL is unused; navigation is handled via onLearnMoreClicked.
            onClick = { onLearnMoreClicked() },
        )

    LinkText(
        text = stringResource(copyTextRes, linkText),
        linkTextStates = listOf(learnMoreLinkState),
        style = FirefoxTheme.typography.body2.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        linkTextDecoration = TextDecoration.Underline,
    )
}

@PreviewLightDark
@Composable
private fun LearnMoreContentPreviewTreatmentA() {
    FirefoxTheme {
        Surface {
            getTreatmentA(LocalContext.current) {}.learnMoreContent()
        }
    }
}

@PreviewLightDark
@Composable
private fun LearnMoreContentPreviewTreatmentB() {
    FirefoxTheme {
        Surface {
            getTreatmentB(LocalContext.current) {}.learnMoreContent()
        }
    }
}

@PreviewLightDark
@Composable
private fun LearnMoreContentPreviewTreatmentC() {
    FirefoxTheme {
        Surface {
            getTreatmentC(LocalContext.current) {}.learnMoreContent()
        }
    }
}
