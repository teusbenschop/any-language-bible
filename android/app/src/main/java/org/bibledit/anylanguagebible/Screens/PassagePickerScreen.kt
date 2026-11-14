package org.bibledit.anylanguagebible.Screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

// Tap the title in the reading view to open the passage picker.

data class BibleBook(
    val name: String,
    val chapters: Int
) {
    val id: String get() = name
}

object BibleCatalog {
    val books: List<BibleBook> = listOf(
        BibleBook("Genesis", 50),
        BibleBook("Exodus", 40),
        BibleBook("Leviticus", 27),
        BibleBook("Numbers", 36),
        BibleBook("Deuteronomy", 34),
        BibleBook("Joshua", 24),
        BibleBook("Judges", 21),
        BibleBook("Ruth", 4),
        BibleBook("1 Samuel", 31),
        BibleBook("2 Samuel", 24),
        BibleBook("1 Kings", 22),
        BibleBook("2 Kings", 25),
        BibleBook("1 Chronicles", 29),
        BibleBook("2 Chronicles", 36),
        BibleBook("Ezra", 10),
        BibleBook("Nehemiah", 13),
        BibleBook("Esther", 10),
        BibleBook("Job", 42),
        BibleBook("Psalms", 150),
        BibleBook("Proverbs", 31),
        BibleBook("Ecclesiastes", 12),
        BibleBook("Song of Solomon", 8),
        BibleBook("Isaiah", 66),
        BibleBook("Jeremiah", 52),
        BibleBook("Lamentations", 5),
        BibleBook("Ezekiel", 48),
        BibleBook("Daniel", 12),
        BibleBook("Hosea", 14),
        BibleBook("Joel", 3),
        BibleBook("Amos", 9),
        BibleBook("Obadiah", 1),
        BibleBook("Jonah", 4),
        BibleBook("Micah", 7),
        BibleBook("Nahum", 3),
        BibleBook("Habakkuk", 3),
        BibleBook("Zephaniah", 3),
        BibleBook("Haggai", 2),
        BibleBook("Zechariah", 14),
        BibleBook("Malachi", 4),
        BibleBook("Matthew", 28),
        BibleBook("Mark", 16),
        BibleBook("Luke", 24),
        BibleBook("John", 21),
        BibleBook("Acts", 28),
        BibleBook("Romans", 16),
        BibleBook("1 Corinthians", 16),
        BibleBook("2 Corinthians", 13),
        BibleBook("Galatians", 6),
        BibleBook("Ephesians", 6),
        BibleBook("Philippians", 4),
        BibleBook("Colossians", 4),
        BibleBook("1 Thessalonians", 5),
        BibleBook("2 Thessalonians", 3),
        BibleBook("1 Timothy", 6),
        BibleBook("2 Timothy", 4),
        BibleBook("Titus", 3),
        BibleBook("Philemon", 1),
        BibleBook("Hebrews", 13),
        BibleBook("James", 5),
        BibleBook("1 Peter", 5),
        BibleBook("2 Peter", 3),
        BibleBook("1 John", 5),
        BibleBook("2 John", 1),
        BibleBook("3 John", 1),
        BibleBook("Jude", 1),
        BibleBook("Revelation", 22)
    )
}

// Replaces the two Swift bindings with the current values plus one callback.
// The caller shows this screen (full screen, dialog, or navigation destination)
// and hides it again in onDismiss.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassagePickerScreen(
    book: String,
    chapter: Int,
    onSelect: (book: String, chapter: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Start inside the current book's chapter grid.
    // Only the name is saved, so the state survives rotation.
    var selectedName by rememberSaveable {
        mutableStateOf(BibleCatalog.books.firstOrNull { it.name == book }?.name)
    }
    val selected = BibleCatalog.books.firstOrNull { it.name == selectedName }

    // The system back gesture goes from the chapter grid to the book list.
    BackHandler(enabled = selected != null) { selectedName = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.name ?: "Books") },
                navigationIcon = {
                    if (selected != null) {
                        IconButton(onClick = { selectedName = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selected == null) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                    }
                }
            )
        }
    ) { padding ->
        if (selected == null) {
            BookList(
                currentBook = book,
                onOpen = { selectedName = it.name },
                modifier = Modifier.padding(padding)
            )
        } else {
            ChapterGrid(
                book = selected,
                currentBook = book,
                currentChapter = chapter,
                onSelect = { number ->
                    onSelect(selected.name, number)
                    onDismiss()
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun BookList(
    currentBook: String,
    onOpen: (BibleBook) -> Unit,
    modifier: Modifier = Modifier
) {
    val books = BibleCatalog.books
    val index = books.indexOfFirst { it.name == currentBook }

    // Starts with the current book in view, so that rows exist to measure.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(index, 0))

    // Then animates the current book to the center, like scrollTo(anchor: .center).
    LaunchedEffect(Unit) {
        if (index < 0) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        val info = listState.layoutInfo
        val itemSize = info.visibleItemsInfo.first().size
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        listState.animateScrollToItem(index, scrollOffset = -(viewport - itemSize) / 2)
    }

    LazyColumn(state = listState, modifier = modifier) {
        items(books, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.name, modifier = Modifier.weight(1f))
                if (item.name == currentBook) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Current book",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ChapterGrid(
    book: BibleBook,
    currentBook: String,
    currentChapter: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        gridItems((1..book.chapters).toList(), key = { it }) { number ->
            val selected = book.name == currentBook && number == currentChapter
            Button(
                onClick = { onSelect(number) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .semantics { contentDescription = "Chapter $number" },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            ) {
                // "tnum" gives equal-width digits, like monospacedDigit().
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}
