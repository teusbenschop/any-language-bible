package org.bibledit.anylanguagebible.Screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// How selection behaves
// * Select: Tapping or long-pressing a verse selects it and sets the anchor of the range.
// * Extend: With a selection active, tapping another verse selects everything from the anchor to that verse. Tapping a nearer verse shortens the range.
// * Collapse and clear: Tapping the anchor collapses a range back to that one verse. Tapping it again clears the selection.
// * Long-press: Long-pressing a verse always starts a new selection there.
// * Reset: Changing chapter, book or tab clears the selection.

// Call the functions that change data from the main thread, like @MainActor in Swift.
class VerseSelection {

    var anchor by mutableStateOf<Int?>(null)
        private set
    var focus by mutableStateOf<Int?>(null)
        private set

    // ReadScreen fills this when it loads a chapter: verse number -> text.
    var verseTexts by mutableStateOf<Map<Int, String>>(emptyMap())

    val range: IntRange?
        get() {
            val a = anchor ?: return null
            val f = focus ?: return null
            return minOf(a, f)..maxOf(a, f)
        }

    val isActive: Boolean
        get() = range != null

    fun contains(verse: Int): Boolean = range?.contains(verse) ?: false

    fun tap(verse: Int) {
        val a = anchor
        if (a == null) {
            anchor = verse
            focus = verse
            return
        }
        if (verse == a) {
            if (focus == a) clear() else focus = a // Collapse, then clear.
        } else {
            focus = verse // Extend or shorten the range.
        }
    }

    fun longPress(verse: Int) {
        anchor = verse
        focus = verse
    }

    fun clear() {
        anchor = null
        focus = null
    }

    fun reference(book: String, chapter: Int): String {
        val r = range ?: return "$book $chapter"
        if (r.first == r.last) return "$book $chapter:${r.first}"
        return "$book $chapter:${r.first}\u2013${r.last}"
    }

    // Text for copy and share: reference line, then one line per verse.
    fun text(book: String, chapter: Int): String {
        val r = range ?: return ""
        val lines = r.mapNotNull { v -> verseTexts[v]?.let { "$v $it" } }
        return (listOf(reference(book, chapter)) + lines).joinToString("\n")
    }
}