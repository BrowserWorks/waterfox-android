/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.browser.awesomebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.theme.AcornTheme
import mozilla.components.compose.browser.awesomebar.internal.CurrentTabData
import mozilla.components.compose.browser.awesomebar.internal.CurrentTabDetails
import mozilla.components.compose.browser.awesomebar.internal.CurrentTabDetailsInteractions
import mozilla.components.compose.browser.awesomebar.internal.SuggestionFetcher
import mozilla.components.compose.browser.awesomebar.internal.Suggestions
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.awesomebar.AwesomeBar.GroupedSuggestion
import mozilla.components.concept.base.profiler.Profiler

private const val CURRENT_TAB_DETAILS_MIN_HEIGHT = 72

/**
 * An awesome bar displaying suggestions from the list of provided [AwesomeBar.SuggestionProvider]s.
 *
 * @param text The text entered by the user and for which the AwesomeBar should show suggestions for.
 * @param currentTabData The current tab details to be displayed in the AwesomeBar.
 * @param colors The color scheme the AwesomeBar will use for the UI.
 * @param providers The list of suggestion providers to query whenever the [text] changes.
 * @param hiddenSuggestions The list of suggestions that should not be shown to users.
 * @param orientation Whether the AwesomeBar is oriented to the top or the bottom of the screen.
 * @param onSuggestionClicked Gets invoked whenever the user clicks on a suggestion in the AwesomeBar.
 * @param onAutoComplete Gets invoked when the user clicks on the "autocomplete" icon of a suggestion.
 * @param onRemoveClicked Gets invoked when the user clicks on the "remove" icon of a suggestion.
 * @param onCurrentSiteDetailsInteraction Invoked when the user interacts with any of the shown buttons.
 * @param onVisibilityStateUpdated Gets invoked when the list of currently displayed suggestions changes.
 * @param onScroll Gets invoked at the beginning of the user performing a scroll gesture.
 */
@Composable
fun AwesomeBar(
    text: String,
    currentTabData: CurrentTabData? = null,
    colors: AwesomeBarColors = AwesomeBarDefaults.colors(),
    providers: List<AwesomeBar.SuggestionProvider>,
    hiddenSuggestions: Set<GroupedSuggestion> = emptySet(),
    orientation: AwesomeBarOrientation = AwesomeBarOrientation.TOP,
    onSuggestionClicked: (AwesomeBar.SuggestionItem) -> Unit,
    onAutoComplete: (AwesomeBar.Suggestion) -> Unit,
    onRemoveClicked: (GroupedSuggestion) -> Unit,
    onCurrentSiteDetailsInteraction: (CurrentTabDetailsInteractions) -> Unit = {},
    onVisibilityStateUpdated: (AwesomeBar.VisibilityState) -> Unit = {},
    onScroll: () -> Unit = {},
    profiler: Profiler? = null,
    copyLinkLabel: String? = null,
    onCopyUrl: ((String) -> Unit)? = null,
) {
    val groups =
        remember(providers) {
            providers
                .groupBy { it.groupTitle() }
                .map { (title, groupedProviders) ->
                    AwesomeBar.SuggestionProviderGroup(
                        providers = groupedProviders,
                        title = title,
                        displayTitle = groupedProviders.first().displayGroupTitle(),
                    )
                }
        }

    AwesomeBar(
        text = text,
        currentTabData = currentTabData,
        colors = colors,
        groups = groups,
        hiddenSuggestions = hiddenSuggestions,
        orientation = orientation,
        onSuggestionClicked = { _, suggestion -> onSuggestionClicked(suggestion) },
        onAutoComplete = { _, suggestion -> onAutoComplete(suggestion) },
        onRemoveClicked = { group, suggestion -> onRemoveClicked(GroupedSuggestion(suggestion, group.id)) },
        onCurrentSiteDetailsInteraction = onCurrentSiteDetailsInteraction,
        onVisibilityStateUpdated = onVisibilityStateUpdated,
        onScroll = onScroll,
        profiler = profiler,
        copyLinkLabel = copyLinkLabel,
        onCopyUrl = onCopyUrl,
    )
}

/**
 * An awesome bar displaying suggestions in groups from the list of provided [AwesomeBar.SuggestionProviderGroup]s.
 *
 * @param text The text entered by the user and for which the AwesomeBar should show suggestions for.
 * @param currentTabData The current tab details to be displayed in the AwesomeBar.
 * @param colors The color scheme the AwesomeBar will use for the UI.
 * @param groups The list of groups of suggestion providers to query whenever the [text] changes.
 * @param hiddenSuggestions The list of suggestions that should not be shown to users.
 * @param orientation Whether the AwesomeBar is oriented to the top or the bottom of the screen.
 * @param onSuggestionClicked Gets invoked whenever the user clicks on a suggestion in the AwesomeBar.
 * @param onAutoComplete Gets invoked when the user clicks on the "autocomplete" icon of a suggestion.
 * @param onRemoveClicked Gets invoked when the user clicks on the "remove" icon of a suggestion.
 * @param onCurrentSiteDetailsInteraction Invoked when the user interacts with any of the shown buttons.
 * @param onVisibilityStateUpdated Gets invoked when the list of currently displayed suggestions changes.
 * @param onScroll Gets invoked at the beginning of the user performing a scroll gesture.
 */
@Composable
@Suppress("LongMethod", "CognitiveComplexMethod")
fun AwesomeBar(
    text: String,
    currentTabData: CurrentTabData? = null,
    colors: AwesomeBarColors = AwesomeBarDefaults.colors(),
    groups: List<AwesomeBar.SuggestionProviderGroup>,
    hiddenSuggestions: Set<GroupedSuggestion> = emptySet(),
    orientation: AwesomeBarOrientation = AwesomeBarOrientation.TOP,
    onSuggestionClicked: (AwesomeBar.SuggestionProviderGroup, AwesomeBar.SuggestionItem) -> Unit,
    onAutoComplete: (AwesomeBar.SuggestionProviderGroup, AwesomeBar.Suggestion) -> Unit,
    onRemoveClicked: (AwesomeBar.SuggestionProviderGroup, AwesomeBar.Suggestion) -> Unit,
    onCurrentSiteDetailsInteraction: (CurrentTabDetailsInteractions) -> Unit,
    onVisibilityStateUpdated: (AwesomeBar.VisibilityState) -> Unit = {},
    onScroll: () -> Unit = {},
    profiler: Profiler? = null,
    copyLinkLabel: String? = null,
    onCopyUrl: ((String) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var tabDetailsHeight by remember { mutableStateOf(CURRENT_TAB_DETAILS_MIN_HEIGHT.dp) }
    val hasTabDetailsAtTop =
        remember(currentTabData, orientation) {
            currentTabData != null && orientation == AwesomeBarOrientation.TOP
        }
    val hasTabDetailsAtBottom =
        remember(currentTabData, orientation) {
            currentTabData != null && orientation == AwesomeBarOrientation.BOTTOM
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .then(if (hasTabDetailsAtBottom) Modifier.fillMaxHeight() else Modifier)
                .semantics {
                    testTagsAsResourceId = true
                    testTag = "mozac.awesomebar"
                }
                .background(colors.background)
    ) {
        if (groups.isEmpty() && currentTabData == null) return
        val fetcher = remember(groups) { SuggestionFetcher(groups, profiler) }

        val suggestions by
            remember(fetcher.state.value, hiddenSuggestions) {
                derivedStateOf {
                    val currentSuggestions = fetcher.state.value

                    // Simple scenario: No pending soft deletion -> no need to filter suggestions.
                    if (hiddenSuggestions.isEmpty()) {
                        return@derivedStateOf currentSuggestions.toSortedMap(
                            compareByDescending<AwesomeBar.SuggestionProviderGroup> { it.priority }
                                // Also using the ID avoids eliding results from groups with the same priority.
                                .thenBy { it.id }
                        )
                    }

                    // Complex scenario: Suggestions set for deletion -> need to avoid showing them in the meantime.
                    currentSuggestions
                        .mapValues { (group, suggestions) ->
                            suggestions.filterNot { suggestion ->
                                GroupedSuggestion(suggestion, group.id) in hiddenSuggestions
                            }
                        }
                        // Remove any groups that become empty after filtering hidden suggestions.
                        .filterValues { it.isNotEmpty() }
                        .toSortedMap(
                            compareByDescending<AwesomeBar.SuggestionProviderGroup> { it.priority }
                                // Also using the ID avoids eliding results from groups with the same priority.
                                .thenBy { it.id }
                        )
                }
            }

        LaunchedEffect(text, fetcher) {
            fetcher.fetch(text)
        }

        Box(
            // Fill all the space left by the tab details to keep the suggestions aligned to the top
            // while the tab details stay at the very bottom.
            modifier = if (hasTabDetailsAtBottom) Modifier.weight(1f) else Modifier
        ) {
            Suggestions(
                suggestions,
                colors,
                orientation,
                onSuggestionClicked,
                onAutoComplete,
                onRemoveClicked,
                onVisibilityStateUpdated,
                onScroll,
                modifier = if (hasTabDetailsAtBottom) Modifier.fillMaxSize() else Modifier,
                // Start the suggestions outside the overlaid tab details while still allowing them
                // to be scrolled behind the tab details.
                contentPadding =
                    when {
                        hasTabDetailsAtTop -> PaddingValues(top = tabDetailsHeight)
                        hasTabDetailsAtBottom -> PaddingValues(bottom = tabDetailsHeight)
                        else -> PaddingValues()
                    },
                copyLinkLabel = copyLinkLabel,
                onCopyUrl = onCopyUrl,
            )

            currentTabData?.let {
                CurrentTabDetails(
                    it,
                    onInteraction = onCurrentSiteDetailsInteraction,
                    modifier =
                        Modifier.align(
                                when (hasTabDetailsAtBottom) {
                                    true -> Alignment.BottomCenter
                                    false -> Alignment.TopCenter
                                }
                            )
                            .onSizeChanged { tabDetailsHeight = with(density) { it.height.toDp() } }
                            .padding(
                                horizontal = AcornTheme.layout.space.static200,
                                vertical = AcornTheme.layout.space.static100,
                            ),
                )
            }
        }
    }
}
