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

// Design principles for the reading view.
// * Make the text the focus.
// * Minimal chrome, generous margins, comfortable line spacing
// * A clean serif or highly legible sans-serif font.
// * Let the reader change font size easily.
// * Offer light, dark, and sepia themes.
// * Many people read at night or in church, so dark mode and a low-brightness option matter.
// * Show verse numbers small and unobtrusive (superscript), so they help reference without breaking the flow.
// * Offer a paragraph view as well as a verse-per-line view.
// * Keep a slim header showing the current book, chapter, and translation.
// * Hide other controls while scrolling and bring them back on tap.
// * Support swiping left and right for previous and next chapter.
// * Keep scroll position when the user returns.


import SwiftUI

struct ReadingView: View {
    // The bindings refer to variables living in the parent view.
    @Binding var book: String
    @Binding var chapter: Int
    // Set when arriving from a bookmark. Cleared once it has been scrolled to.
    @Binding var focusedVerse: Int?

    @EnvironmentObject var verseSelection: VerseSelection

    @State private var verses: [BibleVerse] = []
    @State private var fontSize: Double = 18
    // Briefly highlighted after jumping to a verse.
    @State private var highlightedVerse: Int? = nil
    // The verse currently at the top of the scroll view.
    @State private var topVerse: Int? = nil
    // The scroll key for which the saved position has been restored.
    // Saving is only allowed once this matches the current chapter,
    // so transient positions during a chapter change are never stored.
    @State private var restoredFor: String? = nil

    // Key for storing scroll position per book/chapter.
    private var scrollKey: String { "readingScroll.\(book).\(chapter)" }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(verses, id: \.verse) { verse in
                        // Superscript-style verse number followed by the text.
                        let number = Text("\(verse.verse) ")
                            .font(.system(size: fontSize * 0.6))
                            .baselineOffset(fontSize * 0.4)
                            .foregroundStyle(.secondary)
                        Text("\(number)\(verse.text)")
                            .font(.system(size: fontSize, design: .serif))
                        // Full-width row, so the whole line is tappable and highlights evenly.
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 4)
                            .padding(.horizontal, 4)
                            .background(
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(highlightedVerse == verse.verse
                                          ? Color.accentColor.opacity(0.25)
                                          : Color.clear)
                            )
                            .verseInteraction(verse: verse.verse, book: book, chapter: chapter)
                            .id(verse.verse)
                    }
                }
                .scrollTargetLayout()
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .lineSpacing(6)
            }
            // Tracks which verse is at the top of the screen.
            .scrollPosition(id: $topVerse, anchor: .top)
            // Persist the position whenever it changes.
            .onChange(of: topVerse) { _, newValue in
                guard restoredFor == scrollKey, let newValue else { return }
                savePosition(newValue, forKey: scrollKey)
            }
            // Give copy and share the text of the verses on screen.
            .task(id: "\(book) \(chapter)") {

                let key = scrollKey
                restoredFor = nil
                topVerse = nil

                let volumeId = Settings.getVolumeId()

                // The book number may be nil in case of unknown book name.
                guard let bookNumber = bookid(forName: book) else {
                    verses = []
                    verseSelection.verseTexts = [:]
                    return
                }

                let chapter = chapter

                verses = getChapterVerses(id: volumeId, book: bookNumber, chapter: chapter)

                verseSelection.verseTexts = Dictionary(
                    uniqueKeysWithValues: convertToTuples(verses).map { ($0.number, $0.text) }
                )

                // Jump to the verse requested by a bookmark, if any.
                var bookmarkTarget: Int? = nil
                if let target = focusedVerse {
                    focusedVerse = nil
                    bookmarkTarget = target
                    // Give the layout a moment to place the new rows.
                    try? await Task.sleep(for: .milliseconds(80))
                    guard !Task.isCancelled else { return }
                    withAnimation {
                        proxy.scrollTo(target, anchor: .center)
                        highlightedVerse = target
                    }
                } else {
                    // Restore the saved position, or start at the top of a new chapter.
                    let saved = UserDefaults.standard.object(forKey: key) as? Int
                    let destination: Int?
                    if let saved, verses.contains(where: { $0.verse == saved }) {
                        destination = saved
                    } else {
                        destination = verses.first?.verse
                    }
                    try? await Task.sleep(for: .milliseconds(80))
                    guard !Task.isCancelled else { return }
                    if let destination {
                        proxy.scrollTo(destination, anchor: .top)
                    }
                }

                // From here on, scroll changes are the user's and may be saved.
                try? await Task.sleep(for: .milliseconds(50))
                guard !Task.isCancelled else { return }
                restoredFor = key

                if bookmarkTarget != nil {
                    try? await Task.sleep(for: .seconds(2))
                    guard !Task.isCancelled else { return }
                    withAnimation { highlightedVerse = nil }
                }
            }
        }
        // Swipe left/right for next/previous chapter.
        .gesture(
            DragGesture(minimumDistance: 40).onEnded { value in
                if value.translation.width < -60 {
                    chapter += 1
                } else if value.translation.width > 60, chapter > 1 {
                    chapter -= 1
                }
            }
        )
    }


    // Stores the verse at the top for the key.
    // Keeps only the most recent couple of keys.
    private func savePosition(_ verse: Int, forKey key: String) {

        let defaults = UserDefaults.standard

        // Ordered list of the keys that hold a saved position, oldest first.
        let recentKeysKey = "readingScroll.recentKeys"
        var recent = defaults.stringArray(forKey: recentKeysKey) ?? []

        // Only touch the list when this key is not already the most recent one,
        // which avoids rewriting it on every scroll tick.
        if recent.last != key {
            recent.removeAll { $0 == key }
            recent.append(key)
            let maxStoredPositions = 10
            while recent.count > maxStoredPositions {
                defaults.removeObject(forKey: recent.removeFirst())
            }
            defaults.set(recent, forKey: recentKeysKey)
        }

        defaults.set(verse, forKey: key)
    }

}
