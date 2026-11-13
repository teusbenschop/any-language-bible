package org.bibledit.anylanguagebible.Utilities

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream


object Download {

    sealed class NetworkError(message: String) : Exception(message) {
        class InvalidServerAddress : NetworkError(
            "Could not refresh remote modules due to an invalid web address for the modules server"
        )

        class InvalidResponse : NetworkError(
            "Got invalid response from the modules server"
        )

        class MissingDateHeader : NetworkError(
            "Could not retrieve relevant information from the modules server"
        )

        class ServerError : NetworkError(
            "Download failed with a server error"
        )

        class NotUtf8 : NetworkError(
            "Modules file is not valid UTF-8 text"
        )
    }

    class GunzipException(message: String) : Exception(message)

    // Download the index of available modules as gzipped JSON text.
    suspend fun fetchModulesIndex(): ByteArray = fetchData("modules.json.gz")

    // Download one module as gzipped JSON text.
    // The file is named "n.json.gz", where "n" is the module identifier.
    suspend fun fetchModule(id: Int): ByteArray = fetchData("$id.json.gz")

    private suspend fun fetchData(file: String): ByteArray = withContext(Dispatchers.IO) {
        val url = try {
            URL("https://bibledit.org/modules/$file")
        } catch (e: java.net.MalformedURLException) {
            throw NetworkError.InvalidServerAddress()
        }
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            val code = connection.responseCode
            if (code !in 200..299) throw NetworkError.ServerError()
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }


    // Download the module list and report progress through the callbacks.
    // Call this from a coroutine on the main thread: the callbacks update the UI.
    // The network and file work is moved to a background thread inside.
    suspend fun downloadModuleList(
        context: Context,
        setLoading: (Boolean) -> Unit,
        setStatusMessage: (String?) -> Unit,
        setModules: (List<Module>) -> Unit
    ) {
        setLoading(true)
        try {
            setStatusMessage("Downloading")
            try {
                val gzipped = fetchModulesIndex()
                val modules = withContext(Dispatchers.IO) {
                    val json = gunzip(gzipped)
                    // Save the text to disk and load it again as parsed JSON.
                    saveModulesJson(context,json)
                    loadModulesJson(context)
                }
                setModules(modules)
                setStatusMessage("Number of available modules: ${modules.size}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: NetworkError) {
                setStatusMessage(e.message)
            } catch (e: Exception) {
                Log.e("downloadModuleList", "downloadModuleList failed", e)
                setStatusMessage("Request failed: ${e.message}")
            }
        } finally {
            setLoading(false)
        }
    }


    // Download one module. Returns true on success.
    suspend fun downloadRemoteModule(
        context: Context,
        module: Module,
        setLoading: (Boolean) -> Unit,
        setStatusMessage: (String?) -> Unit
    ): Boolean {
        setLoading(true)
        try {
            setStatusMessage("Downloading")
            try {
                // Download gzipped module, deflate it.
                val gzipped = fetchModule(module.id)
                withContext(Dispatchers.IO) {
                    val json = gunzip(gzipped)
                    // Save the JSON to file.
                    saveModuleJson(context, module.id, json)
                    // Load the module JSON, parse it, create case folded JSON and save that.
                    val verses = loadModuleJson(context, module.id)
                    saveCaseFoldedModule(context, module.id, verses)
                }
                setStatusMessage("Module was downloaded")
                // Set this module's ID as activated.
                Settings.setVolumeId(module.id)
                // Success.
                return true
            } catch (e: CancellationException) {
                throw e
            } catch (e: NetworkError) {
                setStatusMessage(e.message)
            } catch (e: Exception) {
                Log.e("downloadRemoteModule", "downloadRemoteModule failed", e)
                setStatusMessage("Request failed: ${e.message}")
            }
            return false // Failure.
        } finally {
            setLoading(false)
        }
    }


    // Takes gzip-compressed bytes and returns the decompressed bytes.
    // Java's GZIPInputStream handles the gzip header, the DEFLATE stream and the trailer.
    // No preprocessing needed.
    fun gunzip(input: ByteArray): ByteArray {
        try {
            return GZIPInputStream(ByteArrayInputStream(input)).use { it.readBytes() }
        } catch (e: IOException) {
            throw GunzipException("Invalid gzip data")
        }
    }

}
