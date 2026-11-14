package org.bibledit.anylanguagebible.Screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.bibledit.anylanguagebible.Utilities.AnnotationStore
import org.bibledit.anylanguagebible.Utilities.HighlightColor

// The floating action bar that shows while verses are selected.
// The caller places it, for example aligned to the bottom center of the Read tab,
// and shows it only while selection.isActive.
@Composable
fun VerseActionBar(
    selection: VerseSelection,
    annotations: AnnotationStore,
    book: String,
    chapter: Int,
    modifier: Modifier = Modifier,
    onShowNoteEditor: () -> Unit = {} // Only used by the disabled note button below.
) {
    val range = selection.range ?: return
    val shareText = selection.text(book, chapter)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier.padding(bottom = 8.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (item in HighlightColor.entries) {
                val active = annotations.allHighlighted(item, book, chapter, range)
                // The touch target is 48dp. The visible circle is 24dp.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) {
                            annotations.toggleHighlight(item, book, chapter, range)
                        }
                        .semantics {
                            contentDescription = "Highlight ${item.displayName}"
                            selected = active
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(item.color, CircleShape)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Copy.
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Verses", shareText))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selection.clear()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }

            // Share.
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }

            // Bookmark.
            val marked = annotations.isBookmarked(book, chapter, range)
            IconButton(onClick = { annotations.toggleBookmark(book, chapter, range) }) {
                Icon(
                    imageVector = if (marked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (marked) "Remove bookmark" else "Bookmark"
                )
            }

            // The notes editor button was disabled.
            // It does not save/load anyway.
//            IconButton(onClick = onShowNoteEditor) {
//                Icon(Icons.Default.Edit, contentDescription = "Add note")
//            }

            // Clear the selection.
            IconButton(onClick = { selection.clear() }) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Apply to the composable that draws one verse in ReadScreen.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.verseInteraction(
    verse: Int,
    book: String,
    chapter: Int,
    selection: VerseSelection,
    annotations: AnnotationStore
): Modifier {
    val haptic = LocalHapticFeedback.current
    val highlight = annotations.highlightColor(book, chapter, verse)
    val selected = selection.contains(verse)
    val shape = RoundedCornerShape(4.dp)
    val accent = MaterialTheme.colorScheme.primary

    return this
        // The highlight goes behind the content.
        .background(highlight?.color?.copy(alpha = 0.35f) ?: Color.Transparent, shape)
        // The selection tint goes over the content.
        .drawWithContent {
            drawContent()
            if (selected) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.18f),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }
        .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // No ripple, like the Swift version.
            onClickLabel = if (selected) "Deselect verse" else "Select verse",
            onClick = { selection.tap(verse) },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selection.longPress(verse)
            }
        )
        .semantics { this.selected = selected }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    reference: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val trimmed = text.trim()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(reference) },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                        enabled = trimmed.isNotEmpty(),
                        onClick = {
                            onSave(trimmed)
                            onDismiss()
                        }
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}
