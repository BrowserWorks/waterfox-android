/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import mozilla.components.browser.state.search.SearchEngine
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Metrics

/** A utility object for recording search and bookmark metrics. */
object MetricsUtils {

    /** Possible sources for a performed search. */
    enum class Source {
        ACTION,
        SHORTCUT,
        SUGGESTION,
        TOPSITE,
        WIDGET,
        DIGITAL_ASSISTANT,
        NONE,
    }

    /**
     * Records the appropriate metric for performed searches.
     *
     * @param engine the engine used for searching.
     * @param isDefault whether the engine is the default engine or not.
     * @param searchAccessPoint the source of the search. Can be one of the values of [Source].
     */
    fun recordSearchMetrics(
        engine: SearchEngine,
        isDefault: Boolean,
        searchAccessPoint: Source,
    ) {
        val telemetryId =
            if (engine.type == SearchEngine.Type.CUSTOM) {
                "custom"
            } else {
                val baseId = engine.id.lowercase()
                if (!engine.telemetrySuffix.isNullOrEmpty()) {
                    "$baseId-${engine.telemetrySuffix}"
                } else {
                    baseId
                }
            }
        val source = searchAccessPoint.name.lowercase()

        Metrics.searchCount["$telemetryId.$source"].add()

        val performedSearchExtra =
            if (isDefault) {
                "default.$source"
            } else {
                "shortcut.$source"
            }

        Events.performedSearch.record(Events.PerformedSearchExtra(performedSearchExtra))
    }

    /**
     * Records appropriate metrics for adding a bookmark.
     *
     * @param source Describes where the action was called from.
     * @param count Number of times to record the metric.
     */
    fun recordBookmarkAddMetric(
        source: BookmarkAction.Source,
        count: Int = 1,
    ) {
        Metrics.bookmarksAdd[source.label()].add(count)
    }

    /**
     * Records the appropriate metric for performed Bookmark action.
     *
     * @param action The [BookmarkAction] being counted.
     * @param source Describes where the action was called from.
     */
    fun recordBookmarkMetrics(
        action: BookmarkAction,
        source: BookmarkAction.Source,
    ) {
        when (action) {
            BookmarkAction.EDIT -> Metrics.bookmarksEdit[source.label()].add()
            BookmarkAction.DELETE -> Metrics.bookmarksDelete[source.label()].add()
            BookmarkAction.OPEN -> Metrics.bookmarksOpen[source.label()].add()
        }
    }

    /** Describes which bookmark action is being recorded. */
    enum class BookmarkAction {
        EDIT,
        DELETE,
        OPEN;

        /** Possible sources for a bookmark action. */
        enum class Source {
            ADD_BOOKMARK_TOAST,
            BOOKMARK_EDIT_PAGE,
            BOOKMARK_PANEL,
            BROWSER_NAVBAR,
            BROWSER_TOOLBAR,
            MENU_DIALOG,
            PAGE_ACTION_MENU,
            TABS_TRAY,
            TEST,
        }
    }

    private fun BookmarkAction.Source.label() = name.lowercase()

    /** Enum marking the access point of the Tab Groups feature, for telemetry. */
    enum class TabGroupAccessPoint {
        TABS_TRAY,
        BROWSER_MENU,
    }
}
