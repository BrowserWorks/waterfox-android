/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.di

import kotlinx.coroutines.CoroutineScope
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.webcompat.WebCompatState
import org.mozilla.fenix.webcompat.DefaultGleanBrokenSiteReportSender
import org.mozilla.fenix.webcompat.middleware.DefaultWebCompatReporterRetrievalService
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterNavigationMiddleware
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterStorageMiddleware
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterSubmissionMiddleware
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterTelemetryMiddleware

/** Provides middleware for the WebCompat Reporter store. */
object WebCompatReporterMiddlewareProvider {

    /**
     * Provides middleware for the WebCompat Reporter.
     *
     * @param browserStore [BrowserStore] used to access [BrowserState].
     * @param appStore [AppStore] used to persist [WebCompatState].
     * @param scope The [CoroutineScope] used for launching coroutines.
     */
    fun provideMiddleware(
        browserStore: BrowserStore,
        appStore: AppStore,
        scope: CoroutineScope,
    ) =
        listOf(
            provideStorageMiddleware(appStore),
            provideSubmissionMiddleware(
                appStore = appStore,
                browserStore = browserStore,
                scope = scope,
            ),
            provideNavigationMiddleware(),
            provideTelemetryMiddleware(),
        )

    private fun provideStorageMiddleware(appStore: AppStore) = WebCompatReporterStorageMiddleware(appStore = appStore)

    private fun provideSubmissionMiddleware(
        appStore: AppStore,
        browserStore: BrowserStore,
        scope: CoroutineScope,
    ): WebCompatReporterSubmissionMiddleware {
        val webCompatReporterRetrievalService = DefaultWebCompatReporterRetrievalService(browserStore = browserStore)

        val gleanBrokenSiteReportSender = DefaultGleanBrokenSiteReportSender(browserStore = browserStore)

        return WebCompatReporterSubmissionMiddleware(
            appStore = appStore,
            webCompatReporterRetrievalService = webCompatReporterRetrievalService,
            gleanBrokenSiteReportSender = gleanBrokenSiteReportSender,
            scope = scope,
        )
    }

    private fun provideNavigationMiddleware() = WebCompatReporterNavigationMiddleware()

    private fun provideTelemetryMiddleware() = WebCompatReporterTelemetryMiddleware()
}
