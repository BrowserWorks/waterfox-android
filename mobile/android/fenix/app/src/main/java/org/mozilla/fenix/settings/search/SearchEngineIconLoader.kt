/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import androidx.core.net.toUri
import java.io.IOException
import java.io.Reader
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runInterruptible
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.icons.IconRequest.Resource
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.support.base.log.logger.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal class SearchEngineIconLoader(
    private val icons: BrowserIcons,
    private val client: Client,
) {
    suspend fun load(searchUrl: String): Icon {
        val uri = searchUrl.toUri()
        if (uri.scheme?.lowercase() !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            return icons.loadIcon(IconRequest(searchUrl, isPrivate = true)).await()
        }

        val origin =
            uri.buildUpon()
                .scheme(uri.scheme?.lowercase())
                .encodedAuthority(uri.encodedAuthority?.substringAfterLast('@'))
                .path("/")
                .clearQuery()
                .fragment(null)
                .build()
                .toString()
        val request = IconRequest(origin, isPrivate = true)
        val cached = icons.loadIcon(request).await()
        if (cached.source != Icon.Source.GENERATOR) {
            return cached
        }

        val resources = runInterruptible(IO) { discoverIcons(origin) }
        val discovered =
            if (resources.isNotEmpty()) {
                icons.loadIcon(request.copy(resources = resources)).await()
            } else {
                null
            }
        return when {
            discovered != null && discovered.source != Icon.Source.GENERATOR -> discovered
            else ->
                icons
                    .loadIcon(request.copy(resources = listOf(Resource(origin + "favicon.ico", Resource.Type.FAVICON))))
                    .await()
        }
    }

    private fun discoverIcons(origin: String): List<Resource> {
        val request =
            Request(
                url = origin,
                cookiePolicy = Request.CookiePolicy.OMIT,
                connectTimeout = 5L to TimeUnit.SECONDS,
                readTimeout = 5L to TimeUnit.SECONDS,
            )

        return try {
            client.fetch(request).use { response ->
                if (!response.isSuccess) {
                    return emptyList()
                }
                val html = response.body.useBufferedReader { reader -> readHtml(reader) }
                Jsoup.parse(html, response.url)
                    .select("link[href]")
                    .mapNotNull { link -> iconResource(link) }
                    .distinctBy { it.url }
                    .take(MAX_ICON_RESOURCES)
            }
        } catch (error: IOException) {
            Logger("SearchEngineIconLoader").warn("Could not discover search engine icons", error)
            emptyList()
        }
    }

    private fun readHtml(reader: Reader): String {
        val buffer = CharArray(MAX_HTML_LENGTH)
        var length = 0
        while (length < buffer.size) {
            val count = reader.read(buffer, length, buffer.size - length)
            if (count == -1) {
                break
            }
            length += count
        }
        return String(buffer, 0, length)
    }

    private fun iconResource(link: Element): Resource? {
        val relations = link.attr("rel").lowercase().split(Regex("\\s+"))
        val type =
            when {
                "icon" in relations -> Resource.Type.FAVICON
                "apple-touch-icon" in relations -> Resource.Type.APPLE_TOUCH_ICON
                else -> return null
            }
        val url = link.absUrl("href")
        val iconUri = url.toUri()
        return if (
            iconUri.scheme in listOf("http", "https") && !iconUri.host.isNullOrBlank() && iconUri.userInfo == null
        ) {
            Resource(url, type)
        } else {
            null
        }
    }

    private companion object {
        const val MAX_HTML_LENGTH = 256 * 1024
        const val MAX_ICON_RESOURCES = 8
    }
}
