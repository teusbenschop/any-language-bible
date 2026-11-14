package org.bibledit.anylanguagebible.Utilities

import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Locale
import kotlin.collections.map

// Returns a copy of the verses with the text case folded.
private fun caseFoldedVerses(verses: List<BibleVerse>): List<BibleVerse> =
    verses.map { verse ->
        verse.copy(text = verse.text.foldCase())
    }

// Approximates Unicode case folding in a locale-independent way.
private fun String.foldCase(): String =
    this.uppercase(Locale.ROOT).lowercase(Locale.ROOT)


// Compact output. kotlinx.serialization does not escape slashes
// and writes non-ASCII characters as-is, so no extra options are needed.
private val encoder = Json

// Recursively sorts object keys so the output is stable.
private fun JsonElement.sortedKeys(): JsonElement = when (this) {
    is JsonObject -> JsonObject(
        entries.sortedBy { it.key }.associate { it.key to it.value.sortedKeys() }
    )
    is JsonArray -> JsonArray(map { it.sortedKeys() })
    else -> this
}

// Case folds the verses and encodes them as a plain JSON array [...].
fun caseFoldedJson(verses: List<BibleVerse>): ByteArray? {
    return try {
        val element = encoder.encodeToJsonElement(
            ListSerializer(BibleVerse.serializer()),
            caseFoldedVerses(verses)
        )
        encoder
            .encodeToString(JsonElement.serializer(), element.sortedKeys())
            .toByteArray(Charsets.UTF_8)
    } catch (e: Exception) {
        Log.e("BibleLoader", "Failed to encode case folded verses: $e")
        null
    }
}

