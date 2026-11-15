package org.bibledit.anylanguagebible.Screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bibledit.anylanguagebible.Utilities.AnnotationStore
import org.bibledit.anylanguagebible.Utilities.Bookmark
import java.util.Date
import kotlin.math.roundToInt

// Shows the bookmarks, newest first.
// onSelect is called when the user taps a bookmark.
@Composable
fun BookmarksScreen(
    annotations: AnnotationStore,
    onSelect: (Bookmark) -> Unit
) {
    // Sort bookmarks newest first.
    val sorted = annotations.bookmarks.sortedByDescending { it.created }

    if (sorted.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("No bookmarks yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Bookmarks appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)

    // The id of the row that is currently swiped open. Only one row can be open.
    var openId by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sorted, key = { it.id }) { bookmark ->
            SwipeToRevealRow(
                isOpen = openId == bookmark.id,
                onOpenChange = { open ->
                    if (open) openId = bookmark.id
                    else if (openId == bookmark.id) openId = null
                },
                onDelete = {
                    // toggleBookmark removes a bookmark that already exists.
                    annotations.toggleBookmark(
                        bookmark.book,
                        bookmark.chapter,
                        bookmark.firstVerse..bookmark.lastVerse
                    )
                },
                onClick = { onSelect(bookmark) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = reference(bookmark),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = dateFormat.format(Date(bookmark.created)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

// A row that behaves like a swipe action in an iOS list:
// * A short swipe to the left snaps the row open and shows a trash button.
// * A swipe of more than half the row width deletes the row immediately.
// * Tapping the row while it is open closes it instead of selecting it.
@Composable
private fun SwipeToRevealRow(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) } // 0 is closed, negative is swiped to the left.
    var rowWidth by remember { mutableFloatStateOf(0f) }
    val buttonWidth = 88.dp
    val buttonWidthPx = with(LocalDensity.current) { buttonWidth.toPx() }

    // Follow changes made from outside, such as another row being opened.
    LaunchedEffect(isOpen) {
        offset.animateTo(if (isOpen) -buttonWidthPx else 0f)
    }

    fun delete() {
        scope.launch {
            offset.animateTo(-rowWidth, tween(180))
            onDelete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .onSizeChanged { rowWidth = it.width.toFloat() }
    ) {
        // Behind the row: the red area with the trash button.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .fillMaxHeight()
                    .clickable { delete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete bookmark",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }

        // The row itself, which slides to the left. It is opaque, so it hides the button at rest.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(-rowWidth, 0f))
                        }
                    },
                    onDragStopped = { velocity ->
                        val x = offset.value
                        when {
                            // Past halfway: delete right away.
                            x < -rowWidth * 0.5f -> delete()
                            else -> {
                                // A fast flick decides by direction, otherwise by position.
                                val open = when {
                                    velocity < -800f -> true
                                    velocity > 800f -> false
                                    else -> x < -buttonWidthPx * 0.5f
                                }
                                onOpenChange(open)
                                scope.launch {
                                    offset.animateTo(if (open) -buttonWidthPx else 0f)
                                }
                            }
                        }
                    }
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    if (isOpen) onOpenChange(false) else onClick()
                }
        ) {
            content()
        }
    }
}

private fun reference(b: Bookmark): String =
    "${b.book} ${b.chapter}:${b.firstVerse}" +
            (if (b.firstVerse == b.lastVerse) "" else "\u2013${b.lastVerse}")
