package org.bibledit.anylanguagebible.Screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bibledit.anylanguagebible.Utilities.AnnotationStore
import org.bibledit.anylanguagebible.Utilities.Disk
import org.bibledit.anylanguagebible.ui.theme.AnyLanguageBibleTheme
import org.bibledit.anylanguagebible.Utilities.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)
        Disk.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            AnyLanguageBibleTheme {
                MainScreen()
            }
        }
    }
}

// The four tabs, each with a label and two icons:
// an outlined one when not selected, a filled one when selected.
enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Read("Read", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Search("Search", Icons.Outlined.Search, Icons.Filled.Search),
    Bookmarks("Bookmarks", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Remembers the selected tab across rotation and activity recreation.
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Read) }

    // State for the Read screen.
    var book by rememberSaveable { mutableStateOf("Genesis") }
    var chapter by rememberSaveable { mutableIntStateOf(1) }
    var focusedVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    val verseSelection = remember { VerseSelection() }

    // Whether the passage picker is showing.
    var showPassagePicker by rememberSaveable { mutableStateOf(false) }

    val annotationStore: AnnotationStore = viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (selectedTab == AppTab.Read) {
                        // Tappable title on the Read tab; plain title elsewhere.
                        Row(
                            modifier = Modifier.clickable(
                                onClickLabel = "Opens the book and chapter picker"
                            ) { showPassagePicker = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("$book $chapter")
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                    } else {
                        Text(selectedTab.label)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Swap in the screen for the selected tab.
            when (selectedTab) {
                AppTab.Read -> ReadScreen(
                    book = book,
                    onBookChange = { book = it },
                    chapter = chapter,
                    onChapterChange = { chapter = it },
                    focusedVerse = focusedVerse,
                    onFocusedVerseChange = { focusedVerse = it },
                    verseSelection = verseSelection
                )
                AppTab.Search -> SearchScreen()
                AppTab.Bookmarks -> BookmarksScreen()
                AppTab.Settings -> SettingsScreen(
                    onSelect = { selectedTab = AppTab.Read })
            }

            if (selectedTab == AppTab.Read && verseSelection.isActive) {
                VerseActionBar(
                    selection = verseSelection,
                    annotations = annotationStore,
                    book = book,
                    chapter = chapter,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }


        }
    }

    // The passage picker, shown as a sheet.
    if (showPassagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showPassagePicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PassagePickerScreen(
                book = book,
                chapter = chapter,
                onSelect = { selectedBook, selectedChapter ->
                    book = selectedBook
                    chapter = selectedChapter
                },
                onDismiss = { showPassagePicker = false }
            )
        }
    }
}