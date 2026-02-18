/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.button.IconButton
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.PreviewThemeProvider
import org.mozilla.fenix.theme.Theme
import mozilla.components.ui.icons.R as iconsR

/**
 * A full-width banner shown in the menu prompting the user to set Firefox as their default browser.
 *
 * The entire banner (icon, illustration, and text) is clickable to launch the
 * system default-browser picker. An “X” icon at the end lets the user permanently dismiss the
 * banner.
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shouldFlipIllustration = LocalContext.current.resources.getBoolean(
        R.bool.browser_menu_default_banner_flip_illustration,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Box {
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp, top = 12.dp, bottom = 10.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.browser_menu_default_banner_title, appName),
                        style = FirefoxTheme.typography.body1,
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = R.string.browser_menu_default_banner_subtitle_2),
                        style = FirefoxTheme.typography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.firefox_as_default_banner_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(end = 16.dp)
                        .graphicsLayer(
                            scaleX = if (shouldFlipIllustration) {
                                if (isRtl) 1f else -1f
                            } else {
                                1f
                            },
                        ),
                )
            }

            IconButton(
                onClick = onDismiss,
                contentDescription = stringResource(id = R.string.browser_menu_default_banner_dismiss_promotion),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Icon(
                    painter = painterResource(id = iconsR.drawable.mozac_ic_cross_20),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 12.dp)
                        .align(Alignment.TopEnd),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun MenuBannerPreview(
    @PreviewParameter(PreviewThemeProvider::class) theme: Theme,
) {
    FirefoxTheme(theme) {
        MenuBanner(
            onDismiss = {},
            onClick = {},
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(all = FirefoxTheme.layout.space.static200),
        )
    }
}
