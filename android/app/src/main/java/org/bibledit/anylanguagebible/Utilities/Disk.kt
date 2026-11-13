package org.bibledit.anylanguagebible.Utilities

import kotlinx.serialization.Serializable
import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Locale
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import android.util.AtomicFile
import kotlinx.serialization.builtins.ListSerializer



fun eraseAllData(context: Context) {
    val tag = "EraseAllData"
    try {
        val directory = context.filesDir
        val files = directory.listFiles() ?: emptyArray()
        for (file in files) {
            try {
                // Also removes folders with everything inside, like removeItem does.
                if (!file.deleteRecursively()) {
                    Log.e(tag, "Failed to erase ${file.name}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to erase ${file.name}", e)
            }
        }
    } catch (e: Exception) {
        Log.e(tag, "Failed to erase the data", e)
    }
}

// Calling the above from a Composable:
// import androidx.compose.ui.platform.LocalContext
// val context = LocalContext.current
// Button(onClick = { eraseAllData(context) }) { Text("Erase all data") }



fun saveModulesJson(context: Context, data: ByteArray) {
    val tag = "SaveModulesJson"
    try {
        val directory = context.filesDir
        val file = File(directory, "modules.json")
        // Atomic write: write to a temporary file first, then rename it over the real file.
        // A crash halfway can then never leave a half-written modules.json behind.
        val temporary = File(directory, "modules.json.tmp")
        temporary.writeBytes(data)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw java.io.IOException("Could not rename the temporary file")
        }
    } catch (e: Exception) {
        Log.e(tag, "Failed to save the module", e)
    }
}


@Serializable
data class Module(
    val abbrev: String,
    val id: Int,
    val name: String,
    val source: String,
    val type: Int,
    val uuid: String,
    val version: String
)



private val json = Json { ignoreUnknownKeys = true }

fun loadModulesJson(context: Context): List<Module> {
    try {
        val file = File(context.filesDir, "modules.json")
        val text = file.readText() // UTF-8 by default.
        return json.decodeFromString<List<Module>>(text)
    } catch (e: Exception) {
        val tag = "LoadModulesJson"
        Log.e(tag, "Failed to load modules.json", e)
        return emptyList()
    }
}


fun saveModuleJson(context: Context, id: Int, data: ByteArray) {
    try {
        val directory = context.filesDir
        val file = File(directory, "$id.json")
        // Atomic write: write to a temporary file first, then rename it over the real file.
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeBytes(data)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IOException("Could not rename the temporary file")
        }
    } catch (e: Exception) {
        val tag = "SaveModuleJson"
        Log.e(tag, "Failed to save the module", e)
    }
}


@Serializable
data class BibleVerse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Serializable
data class BibleFile(
    val data: List<BibleVerse>
)


//@OptIn(ExperimentalSerializationApi::class)
//private val json = Json {
//    allowTrailingComma = true   // Tolerate trailing commas (like allowsJSON5).
//    allowComments = true        // JSON5 also permits comments.
//    isLenient = true            // Unquoted keys/strings, like JSON5.
//    ignoreUnknownKeys = true
//}


fun loadModuleJson(context: Context, id: Int): List<BibleVerse> {
    val tag = "BibleLoader"
    return try {
        val file = File(context.filesDir, "$id.json")
        Log.d(tag, file.absolutePath)
        val text = file.readText()
        json.decodeFromString<BibleFile>(text).data
    } catch (e: Exception) {
        Log.e(tag, "Failed to load $id.json: $e")
        emptyList()
    }
}


// Returns a copy of the verses with the text case folded.
fun caseFoldedVerses(verses: List<BibleVerse>): List<BibleVerse> =
    verses.map { verse ->
        verse.copy(text = verse.text.foldCase())
    }

// Approximates Unicode case folding in a locale-independent way.
fun String.foldCase(): String =
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


// Strict decoder: the folded file is written by us as plain JSON.
private val foldedDecoder = Json { ignoreUnknownKeys = true } // Todo remove this and use the above one. Once everything works.


fun saveCaseFoldedModule(context: Context, id: Int, verses: List<BibleVerse>) {
    val json = caseFoldedJson(verses) ?: return
    val file = File(context.filesDir, "$id-folded.json")
    val atomicFile = AtomicFile(file)
    var stream: java.io.FileOutputStream? = null
    try {
        file.parentFile?.mkdirs() // create: true
        stream = atomicFile.startWrite()
        stream.write(json)
        atomicFile.finishWrite(stream) // Syncs and commits the writing. // Todo use this above too? Test once everything works.
    } catch (e: Exception) {
        if (stream != null) atomicFile.failWrite(stream) // Keeps the old file intact.
        Log.e("saveCaseFoldedModule", "Failed to save $id-folded.json: $e")
    }
}


fun loadFoldedModuleJson(context: Context, id: Int): List<BibleVerse> {
    return try {
        val file = File(context.filesDir, "$id-folded.json")
        val text = file.readText()
        foldedDecoder.decodeFromString(ListSerializer(BibleVerse.serializer()), text)
    } catch (e: Exception) {
        Log.e("loadFoldedModuleJson", "Failed to load $id-folded.json: $e")
        emptyList()
    }
}

