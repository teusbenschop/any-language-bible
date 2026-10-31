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
import Combine

enum HighlightColor: String, CaseIterable, Codable, Identifiable {
    case yellow, green, blue, pink

    var id: String { rawValue }
    var name: String { rawValue.capitalized }
    var color: Color {
        switch self {
        case .yellow: return .yellow
        case .green:  return .green
        case .blue:   return .cyan
        case .pink:   return .pink
        }
    }
}

struct Bookmark: Codable, Identifiable, Hashable {
    var id = UUID()
    var book: String
    var chapter: Int
    var firstVerse: Int
    var lastVerse: Int
    var created = Date()
}

struct VerseNote: Codable, Identifiable, Hashable {
    var id = UUID()
    var book: String
    var chapter: Int
    var firstVerse: Int
    var lastVerse: Int
    var text: String
    var created = Date()
}

@MainActor
final class AnnotationStore: ObservableObject {

    @Published private(set) var highlights: [String: HighlightColor] = [:]
    @Published private(set) var bookmarks: [Bookmark] = []
    @Published private(set) var notes: [VerseNote] = []

    private let defaultsKey = "Annotations"

    private struct Snapshot: Codable {
        var highlights: [String: HighlightColor]
        var bookmarks: [Bookmark]
        var notes: [VerseNote]
    }

    init() {
        if let data = UserDefaults.standard.data(forKey: defaultsKey),
           let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) {
            highlights = snapshot.highlights
            bookmarks = snapshot.bookmarks
            notes = snapshot.notes
        }
    }

    private func save() {
        let snapshot = Snapshot(highlights: highlights, bookmarks: bookmarks, notes: notes)
        if let data = try? JSONEncoder().encode(snapshot) {
            UserDefaults.standard.set(data, forKey: defaultsKey)
        }
    }

    private static func key(_ book: String, _ chapter: Int, _ verse: Int) -> String {
        "\(book)|\(chapter)|\(verse)"
    }

    // MARK: Highlights

    func highlightColor(book: String, chapter: Int, verse: Int) -> HighlightColor? {
        highlights[Self.key(book, chapter, verse)]
    }

    func allHighlighted(_ color: HighlightColor, book: String, chapter: Int, verses: ClosedRange<Int>) -> Bool {
        verses.allSatisfy { highlights[Self.key(book, chapter, $0)] == color }
    }

    /// Applies the colour to the range. If the whole range already has it, removes it.
    func toggleHighlight(_ color: HighlightColor, book: String, chapter: Int, verses: ClosedRange<Int>) {
        let keys = verses.map { Self.key(book, chapter, $0) }
        if keys.allSatisfy({ highlights[$0] == color }) {
            keys.forEach { highlights[$0] = nil }
        } else {
            keys.forEach { highlights[$0] = color }
        }
        save()
    }

    // MARK: Bookmarks

    func isBookmarked(book: String, chapter: Int, verses: ClosedRange<Int>) -> Bool {
        bookmarks.contains {
            $0.book == book && $0.chapter == chapter
            && $0.firstVerse == verses.lowerBound && $0.lastVerse == verses.upperBound
        }
    }

    func toggleBookmark(book: String, chapter: Int, verses: ClosedRange<Int>) {
        if isBookmarked(book: book, chapter: chapter, verses: verses) {
            bookmarks.removeAll {
                $0.book == book && $0.chapter == chapter
                && $0.firstVerse == verses.lowerBound && $0.lastVerse == verses.upperBound
            }
        } else {
            bookmarks.append(Bookmark(book: book, chapter: chapter,
                                      firstVerse: verses.lowerBound, lastVerse: verses.upperBound))
        }
        save()
    }

    // MARK: Notes

    func addNote(_ text: String, book: String, chapter: Int, verses: ClosedRange<Int>) {
        notes.append(VerseNote(book: book, chapter: chapter,
                               firstVerse: verses.lowerBound, lastVerse: verses.upperBound,
                               text: text))
        save()
    }
}
