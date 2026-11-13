// ReadScreen.kt
package org.bibledit.anylanguagebible.Screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bibledit.anylanguagebible.R
import org.bibledit.anylanguagebible.Utilities.BibleVerse
import org.bibledit.anylanguagebible.Utilities.Settings.getVolumeId
import org.bibledit.anylanguagebible.Utilities.bookId
import org.bibledit.anylanguagebible.Utilities.bookName
import org.bibledit.anylanguagebible.Utilities.getNearestBook
import org.bibledit.anylanguagebible.Utilities.getVerses

// Replaces the SwiftUI @EnvironmentObject. Pass one instance down from the parent.
@Stable
class VerseSelection {
    var verseTexts: Map<Int, String> by mutableStateOf(emptyMap())
}

private val FONT_SIZE = 18.sp
private const val MAX_STORED_POSITIONS = 10
private const val RECENT_KEYS_KEY = "readingScroll.recentKeys"

@Composable
fun ReadScreen(
    // The state lives in the parent. The screen reads the values and asks to change them.
    book: String,
    onBookChange: (String) -> Unit,
    chapter: Int,
    onChapterChange: (Int) -> Unit,
    // Set when arriving from a bookmark. Cleared once it has been scrolled to.
    focusedVerse: Int?,
    onFocusedVerseChange: (Int?) -> Unit,
    verseSelection: VerseSelection,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reading", Context.MODE_PRIVATE) }
    val listState = rememberLazyListState()

    var verses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    // False until the first load has finished, so the empty state doesn't flash at startup.
    var hasLoaded by remember { mutableStateOf(false) }
    // Briefly highlighted after jumping to a verse.
    var highlightedVerse by remember { mutableStateOf<Int?>(null) }
    // The scroll key for which the saved position has been restored.
    // Saving is only allowed once this matches the current chapter,
    // so transient positions during a chapter change are never stored.
    var restoredFor by remember { mutableStateOf<String?>(null) }

    // Key for storing scroll position per book/chapter.
    val scrollKey = "readingScroll.$book.$chapter"

    // The effects below outlive a single composition, so they read the latest values from these.
    val currentScrollKey by rememberUpdatedState(scrollKey)
    val currentVerses by rememberUpdatedState(verses)
    val currentFocusedVerse by rememberUpdatedState(focusedVerse)

    // Persist the verse at the top whenever the scroll position changes.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                val key = currentScrollKey
                if (restoredFor != key) return@collect
                val verse = currentVerses.getOrNull(index)?.verse ?: return@collect
                savePosition(prefs, verse, key)
            }
    }

    // Loads the chapter and restores the scroll position. Restarts when book or chapter changes.
    LaunchedEffect(book, chapter) {
        val key = scrollKey
        restoredFor = null

        val volumeId = getVolumeId()

        // The book number may be null in case of unknown book name.
        val bookNumber = bookId(book)
        if (bookNumber == null) {
            verses = emptyList()
            verseSelection.verseTexts = emptyMap()
            hasLoaded = true
            return@LaunchedEffect
        }

        // The focused book may not exist in the current module.
        // This may happen when switching to another module,
        // and this module does not have all books that were in the former module.
        // So here look for the book in the module nearest to the focused book.
        val nearestBook = withContext(Dispatchers.IO) { getNearestBook(context, volumeId, bookNumber) }
        if (nearestBook == null) {
            verses = emptyList()
            verseSelection.verseTexts = emptyMap()
            hasLoaded = true
            return@LaunchedEffect
        }
        if (nearestBook != bookNumber) {
            // Changing book and chapter restarts this effect, which then loads the new chapter.
            onBookChange(bookName(nearestBook) ?: "")
            onChapterChange(1)
            return@LaunchedEffect
        }

        val loaded = withContext(Dispatchers.IO) { getVerses(context, volumeId, bookNumber, chapter) }
        verses = loaded
        verseSelection.verseTexts = loaded.associate { it.verse to it.text }
        hasLoaded = true
        listState.scrollToItem(0) // Forget the position of the previous chapter.

        // Jump to the verse requested by a bookmark, if any.
        var bookmarkTarget: Int? = null
        val target = currentFocusedVerse
        if (target != null) {
            onFocusedVerseChange(null)
            bookmarkTarget = target
            // Give the layout a moment to place the new rows.
            delay(80)
            val index = loaded.indexOfFirst { it.verse == target }
            if (index >= 0) {
                // Negative offset puts the verse roughly in the middle of the screen.
                listState.animateScrollToItem(index, -listState.layoutInfo.viewportSize.height / 2)
                highlightedVerse = target
            }
        } else {
            // Restore the saved position, or start at the top of a new chapter.
            val saved = if (prefs.contains(key)) prefs.getInt(key, 0) else null
            val destination = if (saved != null && loaded.any { it.verse == saved }) {
                saved
            } else {
                loaded.firstOrNull()?.verse
            }
            delay(80)
            if (destination != null) {
                val index = loaded.indexOfFirst { it.verse == destination }
                if (index >= 0) listState.scrollToItem(index)
            }
        }

        // From here on, scroll changes are the user's and may be saved.
        delay(50)
        restoredFor = key

        if (bookmarkTarget != null) {
            delay(2000)
            highlightedVerse = null
        }
    }

    // Swipe left/right for next/previous chapter.
    val swipeThresholdPx = with(LocalDensity.current) { 60.dp.toPx() }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(chapter) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -swipeThresholdPx) {
                            onChapterChange(chapter + 1)
                        } else if (total > swipeThresholdPx && chapter > 1) {
                            onChapterChange(chapter - 1)
                        }
                    },
                    onHorizontalDrag = { _, dragAmount -> total += dragAmount }
                )
            },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (hasLoaded && verses.isEmpty()) {
            item { NoBibleYet() }
        } else {
            itemsIndexed(verses, key = { _, verse -> verse.verse }) { _, verse ->
                VerseRow(
                    verse = verse,
                    book = book,
                    chapter = chapter,
                    highlighted = highlightedVerse == verse.verse
                )
            }
        }
    }
}

@Composable
private fun NoBibleYet() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.no_bible_yet),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        val step = Modifier.padding(vertical = 2.dp)
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text(stringResource(R.string.read_step_intro), step, color, textAlign = TextAlign.Center)
        Text(stringResource(R.string.read_step_1), step, color, textAlign = TextAlign.Center)
        Text(stringResource(R.string.read_step_2), step, color, textAlign = TextAlign.Center)
        Text(stringResource(R.string.read_step_3), step, color, textAlign = TextAlign.Center)
    }
}

@Composable
private fun VerseRow(
    verse: BibleVerse,
    book: String,
    chapter: Int,
    highlighted: Boolean,
    fontSize: TextUnit = FONT_SIZE
) {
    val highlight by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        } else {
            Color.Transparent
        },
        label = "verseHighlight"
    )
    val numberColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Superscript-style verse number followed by the text.
    val text = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = fontSize * 0.6f,
                baselineShift = BaselineShift.Superscript,
                color = numberColor
            )
        ) {
            append("${verse.verse} ")
        }
        append(verse.text)
    }

    Text(
        text = text,
        fontSize = fontSize,
        lineHeight = (fontSize.value + 6).sp,
        fontFamily = FontFamily.Serif,
        // Full-width row, so the whole line is tappable and highlights evenly.
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight, RoundedCornerShape(6.dp))
            // Todo  fix: .verseInteraction(verse = verse.verse, book = book, chapter = chapter)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

// Stores the verse at the top for the key.
// Keeps only the most recent couple of keys.
private fun savePosition(prefs: SharedPreferences, verse: Int, key: String) {
    // Ordered list of the keys that hold a saved position, oldest first.
    // SharedPreferences has no ordered list, so the keys are joined with newlines.
    val recent = prefs.getString(RECENT_KEYS_KEY, "")
        .orEmpty()
        .split('\n')
        .filter { it.isNotEmpty() }
        .toMutableList()

    val editor = prefs.edit()

    // Only touch the list when this key is not already the most recent one,
    // which avoids rewriting it on every scroll tick.
    if (recent.lastOrNull() != key) {
        recent.removeAll { it == key }
        recent.add(key)
        while (recent.size > MAX_STORED_POSITIONS) {
            editor.remove(recent.removeAt(0))
        }
        editor.putString(RECENT_KEYS_KEY, recent.joinToString("\n"))
    }

    editor.putInt(key, verse)
    editor.apply()
}