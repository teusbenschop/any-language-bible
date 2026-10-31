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

struct BookmarksView: View {

    // Shared store, injected higher up,
    // so this view sees the same bookmarks that the verse menu adds.
    @EnvironmentObject private var annotationStore: AnnotationStore

    // Called when the user taps a bookmark.
    let onSelect: (Bookmark) -> Void

    var body: some View {

        // Sort bookmarks newest first.
        let sorted = annotationStore.bookmarks.sorted { $0.created > $1.created }

        if sorted.isEmpty {
            ContentUnavailableView("No bookmarks yet",
                                   systemImage: "bookmark",
                                   description: Text("Bookmarks appear here."))
        } else {
            List {
                ForEach(sorted) { bookmark in
                    Button {
                        onSelect(bookmark)
                    } label: {
                        HStack {
                            Image(systemName: "bookmark.fill")
                                .foregroundStyle(.tint)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(reference(for: bookmark))
                                    .font(.headline)
                                Text(bookmark.created, style: .date)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                .onDelete { offsets in
                    for index in offsets {
                        let b = sorted[index]
                        // toggleBookmark removes a bookmark that already exists.
                        annotationStore.toggleBookmark(book: b.book, chapter: b.chapter,
                                                   verses: b.firstVerse...b.lastVerse)
                    }
                }
            }
        }
    }

    private func reference(for b: Bookmark) -> String {
        "\(b.book) \(b.chapter):\(b.firstVerse)"
        + (b.firstVerse == b.lastVerse ? "" : "–\(b.lastVerse)")
    }}
