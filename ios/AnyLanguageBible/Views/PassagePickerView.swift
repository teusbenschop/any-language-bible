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

import SwiftUI


// Tap the title in the reading view to open the passage picker.

struct BibleBook: Identifiable, Hashable {
    let name: String
    let chapters: Int
    var id: String { name }
}

enum BibleCatalog {
    static let books: [BibleBook] = [
        ("Genesis", 50),
        ("Exodus", 40),
        ("Leviticus", 27),
        ("Numbers", 36),
        ("Deuteronomy", 34),
        ("Joshua", 24),
        ("Judges", 21),
        ("Ruth", 4),
        ("1 Samuel", 31),
        ("2 Samuel", 24),
        ("1 Kings", 22),
        ("2 Kings", 25),
        ("1 Chronicles", 29),
        ("2 Chronicles", 36),
        ("Ezra", 10),
        ("Nehemiah", 13),
        ("Esther", 10),
        ("Job", 42),
        ("Psalms", 150),
        ("Proverbs", 31),
        ("Ecclesiastes", 12),
        ("Song of Solomon", 8),
        ("Isaiah", 66),
        ("Jeremiah", 52),
        ("Lamentations", 5),
        ("Ezekiel", 48),
        ("Daniel", 12),
        ("Hosea", 14),
        ("Joel", 3),
        ("Amos", 9),
        ("Obadiah", 1),
        ("Jonah", 4),
        ("Micah", 7),
        ("Nahum", 3),
        ("Habakkuk", 3),
        ("Zephaniah", 3),
        ("Haggai", 2),
        ("Zechariah", 14),
        ("Malachi", 4),
        ("Matthew", 28),
        ("Mark", 16),
        ("Luke", 24),
        ("John", 21),
        ("Acts", 28),
        ("Romans", 16),
        ("1 Corinthians", 16),
        ("2 Corinthians", 13),
        ("Galatians", 6),
        ("Ephesians", 6),
        ("Philippians", 4),
        ("Colossians", 4),
        ("1 Thessalonians", 5),
        ("2 Thessalonians", 3),
        ("1 Timothy", 6),
        ("2 Timothy", 4),
        ("Titus", 3),
        ("Philemon", 1),
        ("Hebrews", 13),
        ("James", 5),
        ("1 Peter", 5),
        ("2 Peter", 3),
        ("1 John", 5),
        ("2 John", 1),
        ("3 John", 1),
        ("Jude", 1),
        ("Revelation", 22)
    ].map {
        BibleBook(name: $0.0, chapters: $0.1)
    }
}


struct PassagePickerView: View {
    @Binding var book: String
    @Binding var chapter: Int
    @Environment(\.dismiss) private var dismiss

    // Start inside the current book's chapter grid.
    @State private var path: [BibleBook]

    // Runs every time the picker gets displayed.
    init(book: Binding<String>, chapter: Binding<Int>) {
        // The underscores refer to the property wrapper, rather than to the value it wraps.
        _book = book
        _chapter = chapter
        let current = BibleCatalog.books.first { $0.name == book.wrappedValue }
        _path = State(initialValue: current.map { [$0] } ?? [])
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollViewReader { proxy in
                List {
                    rows(BibleCatalog.books)
                }
                .onAppear {
                    // Wait one runloop tick so the list has laid out its rows.
                    DispatchQueue.main.async {
                        withAnimation { proxy.scrollTo(book, anchor: .center) }
                    }
                }
            }
            .navigationTitle("Books")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .navigationDestination(for: BibleBook.self) { selected in
                ChapterGrid(book: selected, currentBook: book, currentChapter: chapter) { number in
                    book = selected.name
                    chapter = number
                    dismiss()
                }
            }
        }
    }

    @ViewBuilder
    private func rows(_ books: [BibleBook]) -> some View {
        ForEach(books) { item in
            NavigationLink(value: item) {
                HStack {
                    Text(item.name)
                    Spacer()
                    if item.name == book {
                        Image(systemName: "checkmark")
                            .foregroundStyle(Color.accentColor)
                    }
                }
            }
            .id(item.name)   // target for scrollTo
        }
    }
}



private struct ChapterGrid: View {
    let book: BibleBook
    let currentBook: String
    let currentChapter: Int
    let onSelect: (Int) -> Void

    private let columns = [GridItem(.adaptive(minimum: 56), spacing: 10)]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(1...book.chapters, id: \.self) { number in
                    let selected = book.name == currentBook && number == currentChapter
                    Button {
                        onSelect(number)
                    } label: {
                        Text("\(number)")
                            .font(.body.monospacedDigit())
                            .frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(selected ? .accentColor : Color(.secondarySystemFill))
                    .foregroundStyle(selected ? Color.white : Color.primary)
                    .accessibilityLabel("Chapter \(number)")
                }
            }
            .padding()
        }
        .navigationTitle(book.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
