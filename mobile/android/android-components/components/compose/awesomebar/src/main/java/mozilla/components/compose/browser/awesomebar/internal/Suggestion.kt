/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.browser.awesomebar.internal

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import mozilla.components.compose.base.theme.AcornTheme
import mozilla.components.compose.browser.awesomebar.AwesomeBarColors
import mozilla.components.compose.browser.awesomebar.AwesomeBarOrientation
import mozilla.components.compose.browser.awesomebar.AwesomeBarTestTags
import mozilla.components.compose.browser.awesomebar.R
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.ui.icons.R as iconsR

// We only show one row of text, covering at max screen width.
// Limit bigger texts that could cause slowdowns or even crashes.
private const val SUGGESTION_TEXT_MAX_LENGTH = 100

@Composable
@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongParameterList")
internal fun Suggestion(
    suggestion: AwesomeBar.Suggestion,
    colors: AwesomeBarColors,
    orientation: AwesomeBarOrientation,
    onSuggestionClicked: () -> Unit,
    onAutoComplete: () -> Unit,
    onRemoveClicked: () -> Unit,
    copyLinkLabel: String? = null,
    onCopyUrl: ((String) -> Unit)? = null,
) {
    var urlToCopy by remember(suggestion.provider.id, suggestion.id) { mutableStateOf<String?>(null) }
    val url = suggestion.url?.takeIf { it.isNotBlank() }
    val rowModifier = if (url != null && copyLinkLabel != null && onCopyUrl != null) {
        Modifier.combinedClickable(
            onClick = onSuggestionClicked,
            onLongClickLabel = copyLinkLabel,
            onLongClick = { urlToCopy = url },
        )
    } else {
        Modifier.clickable(onClick = onSuggestionClicked)
    }
    Box(Modifier.fillMaxWidth()) {
        SuggestionRow(suggestion, colors, orientation, rowModifier, onAutoComplete, onRemoveClicked)
        val selectedUrl = urlToCopy
        if (selectedUrl != null && copyLinkLabel != null && onCopyUrl != null) {
            DropdownMenu(expanded = true, onDismissRequest = { urlToCopy = null }) {
                DropdownMenuItem(
                    text = { Text(copyLinkLabel) },
                    onClick = {
                        urlToCopy = null
                        onCopyUrl(selectedUrl)
                    },
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: AwesomeBar.Suggestion,
    colors: AwesomeBarColors,
    orientation: AwesomeBarOrientation,
    modifier: Modifier,
    onAutoComplete: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = AcornTheme.layout.space.static600)
                .testTag(AwesomeBarTestTags.SUGGESTION)
                .padding(start = AcornTheme.layout.space.dynamic200, end = AcornTheme.layout.space.dynamic50)
    ) {
        val icon = suggestion.icon
        if (icon != null) {
            SuggestionIcon(
                icon = icon,
                indicator = suggestion.indicatorIcon,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 10.dp),
            )
        }
        SuggestionTitleAndDescription(
            title = suggestion.title?.take(SUGGESTION_TEXT_MAX_LENGTH),
            description = suggestion.description?.take(SUGGESTION_TEXT_MAX_LENGTH),
            colors = colors,
            modifier =
                Modifier.weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(vertical = AcornTheme.layout.space.static100),
        )
        if (suggestion.editSuggestion != null) {
            AutocompleteButton(
                onAutoComplete = onAutoComplete,
                orientation = orientation,
                colors = colors,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        if (suggestion.isRemovalAllowed) {
            RemoveButton(
                onRemoveClicked = onRemoveClicked,
                colors = colors,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
private fun SuggestionTitleAndDescription(
    title: String?,
    description: String?,
    colors: AwesomeBarColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text =
                if (title.isNullOrEmpty()) {
                    description ?: ""
                } else {
                    title
                },
            color = colors.title,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(IntrinsicSize.Max).padding(start = 2.dp, end = 8.dp),
        )
        if (description?.isNotEmpty() == true) {
            Text(
                text = description,
                color = colors.description,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(IntrinsicSize.Max).padding(start = 2.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun SuggestionIcon(
    icon: Bitmap,
    indicator: Drawable?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.width(30.dp).height(38.dp)) {
        Image(
            icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(2.dp)).width(24.dp).height(24.dp),
            contentScale = ContentScale.Crop,
        )
        if (indicator != null) {
            Image(
                indicator.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.padding(top = 22.dp, start = 14.dp).width(16.dp).height(16.dp),
            )
        }
    }
}

@Composable
@Suppress("MagicNumber")
private fun AutocompleteButton(
    onAutoComplete: () -> Unit,
    colors: AwesomeBarColors,
    orientation: AwesomeBarOrientation,
    modifier: Modifier,
) {
    Image(
        painterResource(iconsR.drawable.mozac_ic_append_up_left_24),
        colorFilter = ColorFilter.tint(colors.autocompleteIcon),
        contentDescription = stringResource(R.string.mozac_browser_awesomebar_edit_suggestion),
        modifier =
            modifier
                .size(48.dp)
                .rotate(
                    if (orientation == AwesomeBarOrientation.BOTTOM) {
                        270f
                    } else {
                        0f
                    }
                )
                .clickable { onAutoComplete() }
                .padding(12.dp),
    )
}

@Composable
private fun RemoveButton(
    onRemoveClicked: () -> Unit,
    colors: AwesomeBarColors,
    modifier: Modifier,
) {
    Image(
        painterResource(iconsR.drawable.mozac_ic_cross_24),
        colorFilter = ColorFilter.tint(colors.autocompleteIcon),
        contentDescription = stringResource(R.string.mozac_browser_awesomebar_remove_suggestion),
        modifier = modifier.size(48.dp).clickable { onRemoveClicked() }.padding(12.dp),
    )
}
