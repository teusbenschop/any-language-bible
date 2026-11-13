package org.bibledit.anylanguagebible.Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class SearchHit(
    val id: Int, // Index of the verse in the module.
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String // Original text for display.
)

private class SearchResult(val hits: List<SearchHit>, val truncated: Boolean)

private class Scored(val index: Int, val score: Int)

class SearchModel : ViewModel() {

    var query by mutableStateOf("")
    var hits by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var truncated by mutableStateOf(false)
        private set
    var indexMissing by mutableStateOf(false)
        private set

    private var folded: List<BibleVerse> = emptyList()   // Searched.
    private var original: List<BibleVerse> = emptyList() // Displayed.
    private var loadedVolume: Int? = null
    private var searchedQuery: String? = null
    private var searchedVolume: Int? = null
    private val maxHits = 300

    // Brings the results up to date with the query.
    // Does nothing if the cached results are still current.
    // Cancellation (a new keystroke) is handled by the coroutine machinery:
    // the delay, the loading and the search loop all throw when cancelled.
    suspend fun refresh(volumeId: Int) {

        val phrase = normalize(query)

        if (phrase == searchedQuery && volumeId == searchedVolume) return

        if (phrase.isEmpty()) {
            hits = emptyList()
            truncated = false
            searchedQuery = phrase
            searchedVolume = volumeId
            return
        }

        // Debounce: a new keystroke cancels this coroutine before the search starts.
        delay(200)

        if (loadedVolume != volumeId) {
            load(volumeId)
        }

        val words = words(phrase)
        val folded = this.folded
        val original = this.original
        val maxHits = this.maxHits

        // Search off the main thread, and stop early when cancelled.
        val result = withContext(Dispatchers.Default) {
            search(folded, original, phrase, words, maxHits)
        }

        hits = result.hits
        truncated = result.truncated
        searchedQuery = phrase
        searchedVolume = volumeId
    }

    private suspend fun load(volumeId: Int) {
        val (foldedVerses, originalVerses) = withContext(Dispatchers.IO) {
            Pair(loadFoldedModuleJson(volumeId), loadModuleJson(volumeId))
        }
        folded = foldedVerses
        original = originalVerses
        loadedVolume = volumeId
        indexMissing = foldedVerses.isEmpty()
        // Force a new search against the newly loaded data.
        searchedQuery = null
    }

    // region Search

    /** Case fold the query, and collapse white space. */
    private fun normalize(text: String): String =
        text.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ")

    /** The distinct words of an already normalized query. */
    private fun words(phrase: String): List<String> =
        phrase
            .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter { it.isNotEmpty() }
            .distinct()

    /** Throws CancellationException when the calling coroutine is cancelled. */
    private suspend fun search(
        folded: List<BibleVerse>,
        original: List<BibleVerse>,
        phrase: String,
        words: List<String>,
        maxHits: Int
    ): SearchResult {
        if (words.isEmpty()) return SearchResult(emptyList(), false)

        val scored = ArrayList<Scored>()

        for ((index, verse) in folded.withIndex()) {
            if (index % 2000 == 0) currentCoroutineContext().ensureActive()

            val text = verse.text
            var matched = 0
            for (word in words) {
                if (text.contains(word)) matched++
            }
            if (matched == 0) continue

            // A verse with the whole phrase always outranks verses with only words.
            val phraseBonus = if (text.contains(phrase)) 1000 else 0
            scored.add(Scored(index, phraseBonus + matched))
        }

        // Highest score first, then in canonical order.
        scored.sortWith(compareByDescending<Scored> { it.score }.thenBy { it.index })

        val useOriginal = original.size == folded.size
        val hits = scored.take(maxHits).map { item ->
            val verse = folded[item.index]
            SearchHit(
                id = item.index,
                book = verse.book,
                chapter = verse.chapter,
                verse = verse.verse,
                text = if (useOriginal) original[item.index].text else verse.text
            )
        }
        return SearchResult(hits, scored.size > maxHits)
    }

    // endregion
}

@Composable
fun SearchScreen(
    model: SearchModel = viewModel(),
    onSelect: (SearchHit) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Runs on appearing, and on every change of query.
    // A new keystroke cancels the previous run.
    LaunchedEffect(model.query) {
        model.refresh(Settings.getVolumeId())
    }

    // Dismiss the keyboard when the list is scrolled.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        OutlinedTextField(
            value = model.query,
            onValueChange = { model.query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Word or phrase") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (model.query.isNotEmpty()) {
                    IconButton(onClick = { model.query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        when {
            model.indexMissing -> {
                Placeholder(
                    title = "No search index",
                    description = "Download a module first."
                )
            }

            model.hits.isEmpty() && model.query.isNotEmpty() -> {
                Placeholder(
                    title = "No results for \u201C${model.query}\u201D",
                    description = "Check the spelling or try a new search."
                )
            }

            else -> {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(model.hits, key = { it.id }) { hit ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(hit) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = reference(hit),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = hit.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider()
                    }
                    if (model.truncated) {
                        item {
                            Text(
                                text = "Showing the best ${model.hits.size} results.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun reference(hit: SearchHit): String =
    "${bookName(hit.book) ?: "Unknown"} ${hit.chapter}:${hit.verse}"


// ---------------------------------------------------------------------------
// PLACEHOLDERS: delete each of these once the real version exists in your
// project (import it instead if it lives in another package).
// ---------------------------------------------------------------------------

data class BibleVerse(val book: Int, val chapter: Int, val verse: Int, val text: String)

fun loadFoldedModuleJson(id: Int): List<BibleVerse> = emptyList() // TODO: port from Swift.

fun loadModuleJson(id: Int): List<BibleVerse> = emptyList() // TODO: port from Swift.

fun bookName(id: Int): String? = null // TODO: use the ported Utilities/Books.kt.

object Settings {
    fun getVolumeId(): Int = 0 // TODO: port from Swift Settings.
}
