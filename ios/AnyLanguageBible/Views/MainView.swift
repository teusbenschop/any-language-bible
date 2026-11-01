/*
 Copyright (©) 2026-2026 Teus Benschop.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */


// Design principles for a user-friendly Bible reader
// * Getting out of the way of the text.
// * Making navigation fast: Tap the title to open the passage selector.
// * Bottom bar with 3-5 items: Sits within thumb reach.
// * Tap or long-press a verse to bookmark it, etc.

import SwiftUI
import WebKit

struct MainView: View {

    @EnvironmentObject private var status: Status

    @State private var currentTab: BibleTab = .read
    @State private var showPassagePicker = false
    @State private var showNoteEditor = false
    // Verse to scroll to and highlight after jumping there from a bookmark.
    @State private var focusedVerse: Int? = nil

    @StateObject private var verseSelection = VerseSelection()
    @StateObject private var annotationStore = AnnotationStore()
    @StateObject private var searchModel = SearchModel()

    // Reading position. Persist these so the reader resumes where he left off.
    @AppStorage("VolumeBook") private var book: String = "John"
    @AppStorage("VolumeChapter") private var chapter: Int = 1


    var body: some View {

        NavigationStack {

            base
                .navigationTitle(navigationTitleText)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { toolbarContent }
                .sheet(isPresented: $showPassagePicker) { passagePicker }
                .sheet(isPresented: $showNoteEditor) { noteEditor }
                .task { }
                .onAppear() { }

        }

    }


    // The content, the action bar, the injected objects and the selection handling.
    private var base: some View {
        content
        // Action bar floats just above the bottom tab bar while verses are selected.
            .safeAreaInset(edge: .bottom) { actionBar }
        // Inject after safeAreaInset so the action bar receives them too.
            .environmentObject(verseSelection)
            .environmentObject(annotationStore)
            .animation(.easeInOut(duration: 0.2), value: verseSelection.isActive)
        // The verse selection belongs to one chapter on the Read tab.
        // Drop it when either changes.
            .onChange(of: book) { verseSelection.clear() }
            .onChange(of: chapter) { verseSelection.clear() }
            .onChange(of: currentTab) { verseSelection.clear() }
    }


    @ViewBuilder
    private var content: some View {
        switch currentTab {
        case .read:
            ReadingView(book: $book, chapter: $chapter, focusedVerse: $focusedVerse)
        case .search:
            SearchView(model: searchModel) { hit in // Called on hit tap.
                book = bookName(forId: hit.book) ?? ""
                chapter = hit.chapter
                focusedVerse = hit.verse
                currentTab = .read
            }
        case .bookmarks:
            BookmarksView(onSelect: { bookmark in // Called on bookmark tap.
                book = bookmark.book
                chapter = bookmark.chapter
                focusedVerse = bookmark.firstVerse
                currentTab = .read
            })
        case .settings:
            SettingsView(onSelect: {
                currentTab = .read
            })
        }
    }

    @ViewBuilder
    private var actionBar: some View {
        if currentTab == .read && verseSelection.isActive {
            VerseActionBar(book: book, chapter: chapter, showNoteEditor: $showNoteEditor)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        }
    }

    private var navigationTitleText: String {
        currentTab == .read ? "\(book) \(chapter)" : "\(currentTab.title)"
    }



    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        // Tappable title on the Read tab; plain title elsewhere.
        ToolbarItem(placement: .principal) {
            titleView
        }
        ToolbarItemGroup(placement: .bottomBar) {
            ForEach(BibleTab.allCases) { item in
                TabBarButton(tab: item, isSelected: currentTab == item) {
                    currentTab = item
                }
            }
        }
    }

    @ViewBuilder
    private var titleView: some View {
        if currentTab == .read {
            Button {
                showPassagePicker = true
            } label: {
                HStack(spacing: 4) {
                    Text("\(book) \(chapter)")
                        .font(.headline)
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.semibold))
                }
                .foregroundStyle(.primary)
            }
            .accessibilityLabel("\(book) \(chapter)")
            .accessibilityHint("Opens the book and chapter picker")
        } else {
            Text(currentTab.title).font(.headline)
        }
    }


    private var passagePicker: some View {
        PassagePickerView(book: $book, chapter: $chapter)
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
    }

    private var noteEditor: some View {
        NoteEditorView(reference: verseSelection.reference(book: book, chapter: chapter)) { text in
            if let range = verseSelection.range {
                annotationStore.addNote(text, book: book, chapter: chapter, verses: range)
            }
            verseSelection.clear()
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }


}


// One item of the bottom bar.
private struct TabBarButton: View {
    let tab: BibleTab
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: isSelected ? tab.filledIcon : tab.plainIcon)
                Text(tab.title).font(.caption2)
            }
            .foregroundStyle(isSelected ? Color.accentColor : Color.secondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityLabel(tab.title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
