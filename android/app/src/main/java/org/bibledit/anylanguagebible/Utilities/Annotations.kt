package org.bibledit.anylanguagebible.Utilities

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID
import androidx.core.content.edit


@Serializable
enum class HighlightColor(val rawValue: String, val color: Color) {
    @SerialName("yellow")
    YELLOW("yellow", Color.Yellow);

    val id: String get() = rawValue

    // Not called "name", because that is already taken by Enum.name.
    val displayName: String
        get() = rawValue.replaceFirstChar { it.titlecase(Locale.ROOT) }
}

@Serializable
data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val book: String,
    val chapter: Int,
    val firstVerse: Int,
    val lastVerse: Int,
    val created: Long = System.currentTimeMillis() // Milliseconds since the epoch.
)

@Serializable
data class VerseNote(
    val id: String = UUID.randomUUID().toString(),
    val book: String,
    val chapter: Int,
    val firstVerse: Int,
    val lastVerse: Int,
    val text: String,
    val created: Long = System.currentTimeMillis()
)


// Call the functions that change data from the main thread, like @MainActor in Swift.
class AnnotationStore(application: Application) : AndroidViewModel(application) {

    var highlights by mutableStateOf<Map<String, HighlightColor>>(emptyMap())
        private set
    var bookmarks by mutableStateOf<List<Bookmark>>(emptyList())
        private set
    var notes by mutableStateOf<List<VerseNote>>(emptyList())
        private set

    private val defaultsKey = "Annotations"
    private val prefs = application.getSharedPreferences("annotations", Context.MODE_PRIVATE)

    // encodeDefaults is needed so that the generated ids and dates get written.
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Serializable
    private class Snapshot(
        val highlights: Map<String, HighlightColor>,
        val bookmarks: List<Bookmark>,
        val notes: List<VerseNote>
    )

    init {
        val text = prefs.getString(defaultsKey, null)
        if (text != null) {
            try {
                val snapshot = json.decodeFromString<Snapshot>(text)
                highlights = snapshot.highlights
                bookmarks = snapshot.bookmarks
                notes = snapshot.notes
            } catch (e: Exception) {
                Log.e("AnnotationStore", "Failed to load the annotations: $e")
            }
        }
    }

    private fun save() {
        try {
            val snapshot = Snapshot(highlights, bookmarks, notes)
            prefs.edit { putString(defaultsKey, json.encodeToString(snapshot)) }
        } catch (e: Exception) {
            Log.e("AnnotationStore", "Failed to save the annotations: $e")
        }
    }

    private fun key(book: String, chapter: Int, verse: Int): String =
        "$book|$chapter|$verse"

    // region Highlights

    fun highlightColor(book: String, chapter: Int, verse: Int): HighlightColor? =
        highlights[key(book, chapter, verse)]

    fun allHighlighted(color: HighlightColor, book: String, chapter: Int, verses: IntRange): Boolean =
        verses.all { highlights[key(book, chapter, it)] == color }

    // Applies the color to the range. If the whole range already has it, removes it.
    fun toggleHighlight(color: HighlightColor, book: String, chapter: Int, verses: IntRange) {
        val keys = verses.map { key(book, chapter, it) }
        highlights = if (keys.all { highlights[it] == color }) {
            highlights - keys.toSet()
        } else {
            highlights + keys.associateWith { color }
        }
        save()
    }

    // endregion

    // region Bookmarks

    private fun matches(bookmark: Bookmark, book: String, chapter: Int, verses: IntRange): Boolean =
        bookmark.book == book && bookmark.chapter == chapter
                && bookmark.firstVerse == verses.first && bookmark.lastVerse == verses.last

    fun isBookmarked(book: String, chapter: Int, verses: IntRange): Boolean =
        bookmarks.any { matches(it, book, chapter, verses) }

    fun toggleBookmark(book: String, chapter: Int, verses: IntRange) {
        bookmarks = if (isBookmarked(book, chapter, verses)) {
            bookmarks.filterNot { matches(it, book, chapter, verses) }
        } else {
            bookmarks + Bookmark(
                book = book,
                chapter = chapter,
                firstVerse = verses.first,
                lastVerse = verses.last
            )
        }
        save()
    }

    // endregion

    // region Notes

    fun addNote(text: String, book: String, chapter: Int, verses: IntRange) {
        notes = notes + VerseNote(
            book = book,
            chapter = chapter,
            firstVerse = verses.first,
            lastVerse = verses.last,
            text = text
        )
        save()
    }

    // endregion
}
