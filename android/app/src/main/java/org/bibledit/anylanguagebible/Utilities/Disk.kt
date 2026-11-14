package org.bibledit.anylanguagebible.Utilities

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import android.util.AtomicFile
import java.io.FileOutputStream


// An object is a Kotlin singleton.
object Disk {

    private lateinit var filesDir: File
    private val json = Json { ignoreUnknownKeys = true }

    // Call once from Application.onCreate().
    fun init(context: Context) {
        filesDir = context.applicationContext.filesDir
    }


    fun eraseAllData() {
        val tag = "EraseAllData"
        try {
            val files = filesDir.listFiles() ?: emptyArray()
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


    // Atomically write data to file.
    // A crash halfway does not leave a half-written JSON file behind.
    private fun writeAtomically(name: String, data: ByteArray, tag: String) {
        val atomicFile = AtomicFile(File(filesDir, name))
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(data)
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            if (stream != null) atomicFile.failWrite(stream)
            Log.e(tag, "Failed to save $name", e)
        }
    }


    fun saveModulesJson(data: ByteArray) {
        writeAtomically("modules.json", data, "SaveModulesJson")
    }

    fun loadModulesJson(): List<Module> {
        try {
            val file = File(filesDir, "modules.json")
            val text = file.readText() // UTF-8 by default.
            return json.decodeFromString<List<Module>>(text)
        } catch (e: Exception) {
            val tag = "LoadModulesJson"
            Log.e(tag, "Failed to load modules.json", e)
            return emptyList()
        }
    }

    fun saveModuleJson(id: Int, data: ByteArray) {
        writeAtomically("$id.json", data, "SaveModuleJson")
    }

    fun loadModuleJson(id: Int): List<BibleVerse> {
        val tag = "BibleLoader"
        return try {
            val file = File(filesDir, "$id.json")
            Log.d(tag, file.absolutePath)
            val text = file.readText()
            json.decodeFromString<BibleFile>(text).data
        } catch (e: Exception) {
            Log.e(tag, "Failed to load $id.json: $e")
            emptyList()
        }
    }


    fun saveCaseFoldedModule(id: Int, verses: List<BibleVerse>) {
        val bytes = caseFoldedJson(verses) ?: return
        writeAtomically("$id-folded.json", bytes, "saveCaseFoldedModule")
    }

    fun loadFoldedModuleJson(id: Int): List<BibleVerse> {
        return try {
            val file = File(filesDir, "$id-folded.json")
            val text = file.readText()
            json.decodeFromString(ListSerializer(BibleVerse.serializer()), text)
        } catch (e: Exception) {
            Log.e("loadFoldedModuleJson", "Failed to load $id-folded.json: $e")
            emptyList()
        }
    }



}













