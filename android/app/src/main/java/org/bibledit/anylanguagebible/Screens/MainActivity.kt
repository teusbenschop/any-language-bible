package org.bibledit.anylanguagebible.Screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.bibledit.anylanguagebible.ui.theme.AnyLanguageBibleTheme
import org.bibledit.anylanguagebible.Utilities.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)
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

@Composable
fun MainScreen() {
    // Remembers the selected tab, also across rotation and process death.
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Read) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                AppTab.Read -> ReadScreen()
                AppTab.Search -> SearchScreen()
                AppTab.Bookmarks -> BookmarksScreen()
                AppTab.Settings -> SettingsScreen(
                    onSelect = { selectedTab = AppTab.Read })
            }
        }
    }
}

// Placeholder screens. Replace these with your real content.
@Composable
fun ReadScreen() { Text("Read") }

@Composable
fun SearchScreen() { Text("Search") }

@Composable
fun BookmarksScreen() { Text("Bookmarks") }


