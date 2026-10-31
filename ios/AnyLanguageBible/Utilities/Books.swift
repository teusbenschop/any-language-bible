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

struct Book: Identifiable {
    let id: Int
    let english: String
}

// A file "books.json" was exported from Bibledit, and converted to the array below.

let books: [Book] = [
    Book(id: 1,   english: "Genesis"),
    Book(id: 2,   english: "Exodus"),
    Book(id: 3,   english: "Leviticus"),
    Book(id: 4,   english: "Numbers"),
    Book(id: 5,   english: "Deuteronomy"),
    Book(id: 6,   english: "Joshua"),
    Book(id: 7,   english: "Judges"),
    Book(id: 8,   english: "Ruth"),
    Book(id: 9,   english: "1 Samuel"),
    Book(id: 10,  english: "2 Samuel"),
    Book(id: 11,  english: "1 Kings"),
    Book(id: 12,  english: "2 Kings"),
    Book(id: 13,  english: "1 Chronicles"),
    Book(id: 14,  english: "2 Chronicles"),
    Book(id: 15,  english: "Ezra"),
    Book(id: 16,  english: "Nehemiah"),
    Book(id: 17,  english: "Esther"),
    Book(id: 18,  english: "Job"),
    Book(id: 19,  english: "Psalms"),
    Book(id: 20,  english: "Proverbs"),
    Book(id: 21,  english: "Ecclesiastes"),
    Book(id: 22,  english: "Song of Solomon"),
    Book(id: 23,  english: "Isaiah"),
    Book(id: 24,  english: "Jeremiah"),
    Book(id: 25,  english: "Lamentations"),
    Book(id: 26,  english: "Ezekiel"),
    Book(id: 27,  english: "Daniel"),
    Book(id: 28,  english: "Hosea"),
    Book(id: 29,  english: "Joel"),
    Book(id: 30,  english: "Amos"),
    Book(id: 31,  english: "Obadiah"),
    Book(id: 32,  english: "Jonah"),
    Book(id: 33,  english: "Micah"),
    Book(id: 34,  english: "Nahum"),
    Book(id: 35,  english: "Habakkuk"),
    Book(id: 36,  english: "Zephaniah"),
    Book(id: 37,  english: "Haggai"),
    Book(id: 38,  english: "Zechariah"),
    Book(id: 39,  english: "Malachi"),
    Book(id: 40,  english: "Matthew"),
    Book(id: 41,  english: "Mark"),
    Book(id: 42,  english: "Luke"),
    Book(id: 43,  english: "John"),
    Book(id: 44,  english: "Acts"),
    Book(id: 45,  english: "Romans"),
    Book(id: 46,  english: "1 Corinthians"),
    Book(id: 47,  english: "2 Corinthians"),
    Book(id: 48,  english: "Galatians"),
    Book(id: 49,  english: "Ephesians"),
    Book(id: 50,  english: "Philippians"),
    Book(id: 51,  english: "Colossians"),
    Book(id: 52,  english: "1 Thessalonians"),
    Book(id: 53,  english: "2 Thessalonians"),
    Book(id: 54,  english: "1 Timothy"),
    Book(id: 55,  english: "2 Timothy"),
    Book(id: 56,  english: "Titus"),
    Book(id: 57,  english: "Philemon"),
    Book(id: 58,  english: "Hebrews"),
    Book(id: 59,  english: "James"),
    Book(id: 60,  english: "1 Peter"),
    Book(id: 61,  english: "2 Peter"),
    Book(id: 62,  english: "1 John"),
    Book(id: 63,  english: "2 John"),
    Book(id: 64,  english: "3 John"),
    Book(id: 65,  english: "Jude"),
    Book(id: 66,  english: "Revelation"),
    Book(id: 67,  english: "Front Matter"),
    Book(id: 68,  english: "Back Matter"),
    Book(id: 69,  english: "Other Material"),
    Book(id: 70,  english: "Tobit"),
    Book(id: 71,  english: "Judith"),
    Book(id: 72,  english: "Esther (Greek)"),
    Book(id: 73,  english: "Wisdom of Solomon"),
    Book(id: 74,  english: "Sirach"),
    Book(id: 75,  english: "Baruch"),
    Book(id: 76,  english: "Letter of Jeremiah"),
    Book(id: 77,  english: "Song of the Three Children"),
    Book(id: 78,  english: "Susanna"),
    Book(id: 79,  english: "Bel and the Dragon"),
    Book(id: 80,  english: "1 Maccabees"),
    Book(id: 81,  english: "2 Maccabees"),
    Book(id: 82,  english: "1 Esdras (Greek)"),
    Book(id: 83,  english: "Prayer of Manasses"),
    Book(id: 84,  english: "Psalm 151"),
    Book(id: 85,  english: "3 Maccabees"),
    Book(id: 86,  english: "2 Esdras (Latin)"),
    Book(id: 87,  english: "4 Maccabees"),
    Book(id: 88,  english: "Daniel (Greek)"),
    Book(id: 89,  english: "Odes"),
    Book(id: 90,  english: "Psalms of Solomon"),
    Book(id: 91,  english: "Ezra Apocalypse"),
    Book(id: 92,  english: "5 Ezra"),
    Book(id: 93,  english: "6 Ezra"),
    Book(id: 94,  english: "Psalms 152-155"),
    Book(id: 95,  english: "2 Baruch (Apocalypse)"),
    Book(id: 96,  english: "Letter of Baruch"),
    Book(id: 97,  english: "Jubilees"),
    Book(id: 98,  english: "Enoch"),
    Book(id: 99,  english: "1 Meqabyan/Mekabis"),
    Book(id: 100, english: "2 Meqabyan/Mekabis"),
    Book(id: 101, english: "3 Meqabyan/Mekabis"),
    Book(id: 102, english: "Reproof"),
    Book(id: 103, english: "4 Baruch"),
    Book(id: 104, english: "Letter to the Laodiceans"),
    Book(id: 105, english: "Introduction Matter"),
    Book(id: 106, english: "Concordance"),
    Book(id: 107, english: "Glossary / Wordlist"),
    Book(id: 108, english: "Topical Index"),
    Book(id: 109, english: "Names Index"),
]

// Returns the optional identifier for a given book name.
func bookid(forName name: String) -> Int? {
    books.first { $0.english == name }?.id
}

// Returns the optional English book name for a given book indentifier.
func bookName(forId id: Int) -> String? {
    books.first { $0.id == id }?.english
}
