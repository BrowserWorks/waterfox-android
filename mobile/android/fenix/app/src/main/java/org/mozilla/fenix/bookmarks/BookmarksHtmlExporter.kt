/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bookmarks

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.io.Writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.bookmarkStorage

internal suspend fun exportBookmarksToHtml(context: Context, destination: Uri) {
    val titles = composeRootTitles(context)
    val documentTitle = context.getString(R.string.library_bookmarks)
    withContext(Dispatchers.IO) {
        val tree = requireNotNull(context.bookmarkStorage.getTree(BookmarkRoot.Root.id, recursive = true).getOrThrow())
        ensureActive()
        val output =
            context.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("Could not open bookmark export document")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
            writer.write("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n<TITLE>")
            writer.writeEscaped(documentTitle)
            writer.write("</TITLE>\n<H1>")
            writer.writeEscaped(documentTitle)
            writer.write("</H1>\n<DL><p>\n")
            writer.writeBookmarkNodes(tree.children.orEmpty(), 1) { node ->
                val rootTitle =
                    when (node.guid) {
                        BookmarkRoot.Mobile.id -> titles["mobile"]
                        BookmarkRoot.Menu.id -> titles["menu"]
                        BookmarkRoot.Toolbar.id -> titles["toolbar"]
                        BookmarkRoot.Unfiled.id -> titles["unfiled"]
                        else -> null
                    }
                rootTitle ?: node.title.orEmpty()
            }
            writer.write("</DL><p>\n")
        }
    }
}

private suspend fun Writer.writeBookmarkNodes(
    nodes: List<BookmarkNode>,
    depth: Int,
    title: (BookmarkNode) -> String,
) {
    for (node in nodes.sortedBy { it.position ?: UInt.MAX_VALUE }) {
        currentCoroutineContext().ensureActive()
        val indentation = "    ".repeat(depth)
        write(indentation)
        when (node.type) {
            BookmarkNodeType.FOLDER -> {
                write("<DT><H3")
                writeDates(node)
                when (node.guid) {
                    BookmarkRoot.Toolbar.id -> write(" PERSONAL_TOOLBAR_FOLDER=\"true\"")
                    BookmarkRoot.Unfiled.id -> write(" UNFILED_BOOKMARKS_FOLDER=\"true\"")
                }
                write(">")
                writeEscaped(title(node))
                write("</H3>\n$indentation<DL><p>\n")
                writeBookmarkNodes(node.children.orEmpty(), depth + 1, title)
                write("$indentation</DL><p>\n")
            }
            BookmarkNodeType.ITEM -> {
                val url = requireNotNull(node.url).also { require(it.isNotBlank()) }
                write("<DT><A HREF=\"")
                writeEscaped(url)
                write("\"")
                writeDates(node)
                write(">")
                writeEscaped(node.title.orEmpty())
                write("</A>\n")
            }
            BookmarkNodeType.SEPARATOR -> write("<HR>\n")
        }
    }
}

private fun Writer.writeDates(node: BookmarkNode) {
    if (node.dateAdded > 0) {
        write(" ADD_DATE=\"${node.dateAdded / 1000}\"")
    }
    if (node.lastModified > 0) {
        write(" LAST_MODIFIED=\"${node.lastModified / 1000}\"")
    }
}

private fun Writer.writeEscaped(value: String) {
    for (character in value) {
        write(
            when (character) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> character.toString()
            }
        )
    }
}
