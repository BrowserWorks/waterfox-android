/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.Banner
import mozilla.components.compose.base.BannerColors
import mozilla.components.compose.base.theme.PreviewThemeProvider
import mozilla.components.compose.base.theme.Theme
import org.mozilla.fenix.R
import org.mozilla.fenix.home.fake.FakeHomepagePreview
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * State-based Message Card.
 *
 * @param messageCardState State representing the card.
 * @param modifier Modifier to be applied to the card.
 * @param onClick Invoked when user clicks on the message card.
 * @param onCloseButtonClick Invoked when user clicks on close button to remove message.
 */
@Composable
fun MessageCard(
    messageCardState: MessageCardState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onCloseButtonClick: () -> Unit = {},
) {
    with(messageCardState) {
        Banner(
            messageText = messageText,
            modifier =
                modifier.then(
                    if (buttonText.isNullOrBlank()) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                ),
            titleText = titleText,
            positiveButtonText = buttonText,
            positiveOnClick = onClick,
            colors = bannerColors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            onCloseButtonClick = onCloseButtonClick,
        )
    }
}

@Preview
@Composable
private fun MessageCardPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        Surface {
            MessageCard(
                messageCardState = FakeHomepagePreview.messageCardState(),
                modifier = Modifier.padding(all = 16.dp),
                onClick = {},
                onCloseButtonClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun MessageCardWithoutTitlePreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        Surface {
            MessageCard(
                messageCardState =
                    MessageCardState(
                        messageText = stringResource(id = R.string.default_browser_experiment_card_text),
                        bannerColors = BannerColors.bannerColors(),
                    ),
                modifier = Modifier.padding(all = 16.dp),
                onClick = {},
                onCloseButtonClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun MessageCardWithButtonLabelPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        Surface {
            MessageCard(
                messageCardState =
                    MessageCardState(
                        messageText = stringResource(id = R.string.default_browser_experiment_card_text),
                        titleText = stringResource(id = R.string.default_browser_experiment_card_title),
                        buttonText = stringResource(id = R.string.preferences_set_as_default_browser),
                        bannerColors = BannerColors.bannerColors(),
                    ),
                onClick = {},
                onCloseButtonClick = {},
            )
        }
    }
}

/**
 * State object that describes a message card.
 *
 * @property messageText The message card's body text to be displayed.
 * @property titleText An optional title of message card. If the provided title text is blank or null, the title will
 *   not be shown.
 * @property buttonText An optional button text of the message card. If the provided button text is blank or null, the
 *   button won't be shown.
 * @property bannerColors The color set defined by [BannerColors] used to style the message card.
 */
data class MessageCardState(
    val messageText: String,
    val titleText: String? = null,
    val buttonText: String? = null,
    val bannerColors: BannerColors,
) {

    /*
    /** Companion object for building [MessageCardState]. */
    companion object {

        /**
         * Builds a new [MessageCardState] from a [Message] and the current [WallpaperState].
         *
         * @param message [Message] to be displayed.
         * @param wallpaperState [WallpaperState] specifying the colors to be used.
         */
        @Composable
        fun build(message: Message, wallpaperState: WallpaperState): MessageCardState {
            val bannerColors = BannerColors.bannerColors(backgroundColor = wallpaperState.cardBackgroundColor)

            return MessageCardState(
                messageText = message.text,
                titleText = message.title,
                buttonText = message.buttonLabel,
                bannerColors = bannerColors,
            )
        }
    }
    */
}
