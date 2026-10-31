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

import Foundation


// Cache: module id -> entire Bible as a flat array of verses.
// The lock makes access safe from several threads,
// and prevents the same module from being loaded twice.
// The box is a global constant,
// so the mutable state is isolated behind the lock.
// This satisfies Swift 6 strict concurrency, unlike a bare global `var`.
private final class BibleCache: @unchecked Sendable {
    private var storage: [Int: [BibleVerse]] = [:]
    private let lock = NSLock()

    // Returns the cached Bible for `id`, loading it on first use.
    // Returns an empty array if loading fails (failures are not cached).
    func bible(id: Int) -> [BibleVerse] {
        lock.lock()
        defer { lock.unlock() }
        if let cached = storage[id] {
            return cached
        }
        let entireBible = loadModuleJson(id: id)
        guard !entireBible.isEmpty else { return [] }
        storage[id] = entireBible
        return entireBible
    }
}

private let bibleCache = BibleCache()



// Returns the verses of the given book and chapter.
func getChapterVerses(id: Int, book: Int, chapter: Int) -> [BibleVerse] {
    bibleCache.bible(id: id)
        .filter { $0.book == book && $0.chapter == chapter }
        .sorted { $0.verse < $1.verse }
}



// Converts BibleVerse values into (number, text) tuples, ordered by verse number.
func convertToTuples(_ verses: [BibleVerse]) -> [(number: Int, text: String)] {
    verses
        .sorted { $0.verse < $1.verse }
        .map { (number: $0.verse, text: $0.text) }
}
