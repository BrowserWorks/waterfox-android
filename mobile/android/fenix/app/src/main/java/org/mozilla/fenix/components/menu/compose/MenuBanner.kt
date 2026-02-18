/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.text.Text as AcornText
import mozilla.components.compose.base.theme.PreviewThemeProvider
import mozilla.components.compose.base.theme.Theme
import mozilla.components.compose.menu.ui.MenuBanner
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A full-width banner shown in the menu prompting the user to set Firefox as their default browser.
 *
 * The entire banner (icon, illustration, and text) is clickable to launch the system default-browser picker. An “X”
 * icon at the end lets the user permanently dismiss the banner.
 *
 * @param onDismiss Invoked when the user taps the dismiss icon (“X”).
 * @param onClick Invoked when the user taps anywhere else on the banner.
 * @param modifier [Modifier] to be applied to the layout.
 */
@Composable
fun MenuBanner(
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appName = stringResource(R.string.app_name)
    val shouldFlipIllustration =
        LocalContext.current.resources.getBoolean(R.bool.browser_menu_default_banner_flip_illustration)

    MenuBanner(
        title = AcornText.String(stringResource(id = R.string.browser_menu_default_banner_title, appName)),
        subtitle = AcornText.Resource(R.string.browser_menu_default_banner_subtitle_2),
        illustration = painterResource(id = R.drawable.firefox_as_default_banner_illustration),
        onDismiss = onDismiss,
        onClick = onClick,
        modifier = modifier,
        dismissContentDescription = AcornText.Resource(R.string.browser_menu_default_banner_dismiss_promotion),
        flipIllustration = shouldFlipIllustration,
    )
}

@Preview
@Composable
private fun MenuBannerPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        MenuBanner(
            onDismiss = {},
            onClick = {},
            modifier =
                Modifier.background(color = MaterialTheme.colorScheme.surface)
                    .padding(all = FirefoxTheme.layout.space.static200),
        )
    }
}
