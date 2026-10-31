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

// How selection behaves
// * Select: Tapping or long-pressing a verse selects it and sets the anchor of the range.
// * Extend: With a selection active, tapping another verse selects everything from the anchor to that verse. Tapping a nearer verse shortens the range.
// * Collapse and clear: Tapping the anchor collapses a range back to that one verse. Tapping it again clears the selection.
// * Long-press: Long-pressing a verse always starts a new selection there.
// * Reset: Changing chapter, book or tab clears the selection.


@MainActor
final class VerseSelection: ObservableObject {

    @Published private(set) var anchor: Int?
    @Published private(set) var focus: Int?

    // ReadingView fills this when it loads a chapter: verse number -> text.
    @Published var verseTexts: [Int: String] = [:]

    var range: ClosedRange<Int>? {
        guard let a = anchor, let f = focus else { return nil }
        return min(a, f)...max(a, f)
    }

    var isActive: Bool { range != nil }

    func contains(_ verse: Int) -> Bool { range?.contains(verse) ?? false }

    func tap(_ verse: Int) {
        guard let a = anchor else {
            anchor = verse
            focus = verse
            return
        }
        if verse == a {
            if focus == a { clear() } else { focus = a } // Collapse, then clear.
        } else {
            focus = verse // Extend or shorten the range.
        }
    }

    func longPress(_ verse: Int) {
        anchor = verse
        focus = verse
    }

    func clear() {
        anchor = nil
        focus = nil
    }

    func reference(book: String, chapter: Int) -> String {
        guard let r = range else { return "\(book) \(chapter)" }
        if r.lowerBound == r.upperBound { return "\(book) \(chapter):\(r.lowerBound)" }
        return "\(book) \(chapter):\(r.lowerBound)–\(r.upperBound)"
    }

    // Text for copy and share: reference line, then one line per verse.
    func text(book: String, chapter: Int) -> String {
        guard let r = range else { return "" }
        let lines = r.compactMap { v in verseTexts[v].map { "\(v) \($0)" } }
        return ([reference(book: book, chapter: chapter)] + lines).joined(separator: "\n")
    }
}
