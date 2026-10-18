/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.fxsuggest

import java.util.UUID
import mozilla.appservices.suggest.Suggestion
import mozilla.appservices.suggest.SuggestionProvider
import mozilla.appservices.suggest.SuggestionQuery
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.ktx.kotlin.toBitmap

private const val MAX_NUM_OF_FIREFOX_SUGGESTIONS = 1

/**
 * An [AwesomeBar.SuggestionProvider] that returns Firefox Suggest search suggestions.
 *
 * @param loadUrlUseCase A use case that loads a suggestion's URL when clicked.
 * @param includeSponsoredSuggestions Whether to return suggestions for sponsored content.
 * @param includeNonSponsoredSuggestions Whether to return suggestions for web content.
 * @param suggestionsHeader An optional header title for grouping the returned suggestions.
 * @param sponsoredSuggestionDescription The description to display for sponsored suggestions.
 * @param contextId The contextual services user identifier, used for telemetry.
 * @param scorer An [AwesomeBar.SuggestionProvider.Scorer] used to rank the suggestions.
 */
class FxSuggestSuggestionProvider(
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
    private val includeSponsoredSuggestions: Boolean,
    private val includeNonSponsoredSuggestions: Boolean,
    private val suggestionsHeader: String? = null,
    private val sponsoredSuggestionDescription: String,
    private val contextId: String? = null,
    private val scorer: AwesomeBar.SuggestionProvider.Scorer = DefaultScorer(),
) : AwesomeBar.SuggestionProvider {
    /** [AwesomeBar.Suggestion.metadata] keys for this provider's suggestions. */
    object MetadataKeys {
        const val CLICK_INFO = "click_info"
        const val IMPRESSION_INFO = "impression_info"
    }

    override val id: String = UUID.randomUUID().toString()

    override fun groupTitle(): String? = suggestionsHeader

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> =
        if (text.isEmpty()) {
            emptyList()
        } else {
            val providers = buildList {
                if (includeSponsoredSuggestions && false) {
                    add(SuggestionProvider.AMP)
                }
                if (includeSponsoredSuggestions && false) {
                    add(SuggestionProvider.AMP)
                }
                if (includeNonSponsoredSuggestions && true) {
                    add(SuggestionProvider.WIKIPEDIA)
                }
            }
            GlobalFxSuggestDependencyProvider.requireStorage()
                .query(
                    SuggestionQuery(
                        keyword = text,
                        providers = providers,
                        limit = MAX_NUM_OF_FIREFOX_SUGGESTIONS,
                    )
                )
                .into()
        }

    override fun onInputCancelled() {
        GlobalFxSuggestDependencyProvider.requireStorage().cancelReads()
    }

    private suspend fun List<Suggestion>.into(): List<AwesomeBar.Suggestion> = mapNotNull { suggestion ->
        val details =
            when (suggestion) {
                is Suggestion.Amp ->
                    SuggestionDetails(
                        title = suggestion.title,
                        url = suggestion.url,
                        fullKeyword = suggestion.fullKeyword,
                        isSponsored = true,
                        icon = suggestion.icon,
                        clickInfo =
                            contextId?.let {
                                FxSuggestInteractionInfo.Amp(
                                    blockId = suggestion.blockId,
                                    advertiser = suggestion.advertiser.lowercase(),
                                    reportingUrl = suggestion.clickUrl,
                                    iabCategory = suggestion.iabCategory,
                                    contextId = it,
                                    suggestionId = suggestion.suggestionId,
                                )
                            },
                        impressionInfo =
                            contextId?.let {
                                FxSuggestInteractionInfo.Amp(
                                    blockId = suggestion.blockId,
                                    advertiser = suggestion.advertiser.lowercase(),
                                    reportingUrl = suggestion.impressionUrl,
                                    iabCategory = suggestion.iabCategory,
                                    contextId = it,
                                    suggestionId = suggestion.suggestionId,
                                )
                            },
                    )
                is Suggestion.Wikipedia -> {
                    val interactionInfo = contextId?.let {
                        FxSuggestInteractionInfo.Wikipedia(contextId = it)
                    }
                    SuggestionDetails(
                        title = suggestion.title,
                        url = suggestion.url,
                        fullKeyword = suggestion.fullKeyword,
                        isSponsored = false,
                        icon = suggestion.icon,
                        clickInfo = interactionInfo,
                        impressionInfo = interactionInfo,
                    )
                }
                else -> return@mapNotNull null
            }
        AwesomeBar.Suggestion(
            provider = this@FxSuggestSuggestionProvider,
            icon = details.icon?.toBitmap(),
            title = details.title,
            description =
                if (details.isSponsored) {
                    sponsoredSuggestionDescription
                } else {
                    null
                },
            onSuggestionClicked = {
                loadUrlUseCase(details.url)
            },
            metadata =
                buildMap {
                    details.clickInfo?.let { put(MetadataKeys.CLICK_INFO, it) }
                    details.impressionInfo?.let { put(MetadataKeys.IMPRESSION_INFO, it) }
                },
        )
    }
        .let {
            scorer.score(it)
        }

    /** A default implementation of [AwesomeBar.SuggestionProvider.Scorer] used by [FxSuggestSuggestionProvider]. */
    private class DefaultScorer : AwesomeBar.SuggestionProvider.Scorer {
        override fun score(suggestions: List<AwesomeBar.Suggestion>): List<AwesomeBar.Suggestion> {
            return suggestions.map { suggestion ->
                suggestion.copy(score = Int.MIN_VALUE)
            }
        }
    }
}

internal data class SuggestionDetails(
    val title: String,
    val url: String,
    val fullKeyword: String,
    val isSponsored: Boolean,
    val icon: ByteArray?,
    val clickInfo: FxSuggestInteractionInfo? = null,
    val impressionInfo: FxSuggestInteractionInfo? = null,
)

/**
 * Additional information about a Firefox Suggest [AwesomeBar.Suggestion] to record in telemetry when the user interacts
 * with the suggestion.
 */
sealed interface FxSuggestInteractionInfo {
    /**
     * Interaction information for a sponsored Firefox Suggest search suggestion from AMP.
     *
     * @param blockId A unique identifier for the suggestion.
     * @param advertiser The name of the advertiser providing the sponsored suggestion.
     * @param reportingUrl The url to report the click or impression to.
     * @param iabCategory The categorization of the suggestion.
     * @param contextId The contextual services user identifier.
     * @param suggestionId The suggestion ID from MARS/Merino.
     */
    data class Amp(
        val blockId: Long,
        val advertiser: String,
        val reportingUrl: String,
        val iabCategory: String,
        val contextId: String,
        val suggestionId: String,
    ) : FxSuggestInteractionInfo

    /**
     * Interaction information for a Firefox Suggest search suggestion from Wikipedia.
     *
     * @param contextId The contextual services user identifier.
     */
    data class Wikipedia(val contextId: String) : FxSuggestInteractionInfo
}
