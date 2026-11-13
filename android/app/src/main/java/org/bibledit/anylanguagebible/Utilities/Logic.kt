package org.bibledit.anylanguagebible.Utilities

import android.content.Context
import kotlin.math.abs
import org.bibledit.anylanguagebible.Utilities.BibleVerse
import org.bibledit.anylanguagebible.Utilities.loadModuleJson

// Cache: module id -> entire Bible as a flat list of verses.
// The lock makes access safe from several threads,
// and prevents the same module from being loaded twice.
private object BibleCache {
    private val storage = HashMap<Int, List<BibleVerse>>()
    private val lock = Any()

    // Returns the cached Bible for `id`, loading it on first use.
    // Returns an empty list if loading fails (failures are not cached).
    fun bible(context: Context, id: Int): List<BibleVerse> = synchronized(lock) {
        storage[id]?.let { return@synchronized it }
        val entireBible = loadModuleJson(context, id)
        if (entireBible.isEmpty())
            return@synchronized emptyList()
        storage[id] = entireBible
        entireBible
    }
}


// Returns the verses of the given module / book / chapter.
fun getVerses(context: Context, id: Int, book: Int, chapter: Int): List<BibleVerse> =
    BibleCache.bible(context, id)
        .filter { it.book == book && it.chapter == chapter }
        .sortedBy { it.verse }


// Returns the book in the given module nearest to the book passed. May be null.
fun getNearestBook(context: Context, id: Int, book: Int): Int? {
    var nearest: Int? = null
    var nearestDistance = Int.MAX_VALUE

    for (verse in BibleCache.bible(context, id)) {
        val candidate = verse.book
        val distance = abs(candidate - book)

        // Exact match: nothing can be nearer.
        if (distance == 0) return candidate

        // Closer wins; on a tie, prefer the lower book number.
        if (distance < nearestDistance ||
            (distance == nearestDistance && candidate < (nearest ?: Int.MAX_VALUE))
        ) {
            nearest = candidate
            nearestDistance = distance
        }
    }

    return nearest
}


// A verse number with its text.
data class NumberedText(val number: Int, val text: String)


// Converts BibleVerse values into (number, text) pairs, ordered by verse number.
fun convertToTuples(verses: List<BibleVerse>): List<NumberedText> =
    verses
        .sortedBy { it.verse }
        .map { NumberedText(number = it.verse, text = it.text) }

