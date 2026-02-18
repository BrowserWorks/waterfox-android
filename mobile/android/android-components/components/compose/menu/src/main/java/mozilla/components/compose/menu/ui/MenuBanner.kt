/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.menu.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.text.Text as AcornText
import mozilla.components.compose.base.text.value
import mozilla.components.compose.base.theme.AcornTheme
import mozilla.components.compose.menu.R
import mozilla.components.compose.menu.data.MenuItemSummary
import mozilla.components.ui.icons.R as iconsR

/**
 * A full-width banner shown in the menu.
 *
 * @param title The title of the banner.
 * @param summary The subtitle of the banner.
 * @param icon The illustration to show in the banner.
 * @param onClick Invoked when the user taps anywhere else on the banner.
 * @param onDismiss Invoked when the user taps the dismiss icon (“X”).
 * @param modifier [Modifier] to be applied to the layout.
 */
@Composable
fun MenuBanner(
    title: AcornText,
    summary: MenuItemSummary,
    icon: MenuItemIcon,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    flipIllustration: Boolean = true,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Box {
            Row {
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .padding(
                                start = AcornTheme.layout.space.static300,
                                top = AcornTheme.layout.space.static150,
                                bottom = dimensionResource(R.dimen.mozac_menu_banner_bottom_padding),
                            )
                ) {
                    Text(
                        text = title.value,
                        style = AcornTheme.typography.body1,
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    )

                    Spacer(modifier = Modifier.height(AcornTheme.layout.space.static50))

                    Text(
                        text = summary.text.value,
                        style = AcornTheme.typography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = summary.maxLines,
                    )
                }

                Image(
                    painter = icon.painter,
                    contentDescription = null,
                    modifier =
                        Modifier.align(Alignment.Bottom)
                            .padding(end = AcornTheme.layout.space.static200)
                            .graphicsLayer(scaleX = if (flipIllustration && !isRtl) -1f else 1f),
                )
            }

            IconButton(
                onClick = onDismiss,
                contentDescription = stringResource(R.string.mozac_menu_banner_dismiss_description),
                modifier =
                    Modifier.align(Alignment.TopEnd).size(AcornTheme.layout.size.static600).semantics(
                        mergeDescendants = true
                    ) {},
            ) {
                Icon(
                    painter = painterResource(id = iconsR.drawable.mozac_ic_cross_20),
                    contentDescription = null,
                    modifier =
                        Modifier.padding(
                                top = AcornTheme.layout.space.static100,
                                end = AcornTheme.layout.space.static150,
                            )
                            .align(Alignment.TopEnd),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MenuBannerPreview() {
    AcornTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MenuBanner(
                title = AcornText.String("Banner title"),
                summary = MenuItemSummary(text = AcornText.String("Banner subtitle")),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_globe_24),
                onDismiss = {},
                onClick = {},
                modifier = Modifier.padding(AcornTheme.layout.space.static200),
            )
        }
    }
}
