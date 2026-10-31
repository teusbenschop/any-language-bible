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

struct VerseActionBar: View {
    @EnvironmentObject var selection: VerseSelection
    @EnvironmentObject var annotations: AnnotationStore

    let book: String
    let chapter: Int
    @Binding var showNoteEditor: Bool

    var body: some View {
        if let range = selection.range {
            let shareText = selection.text(book: book, chapter: chapter)

            HStack(spacing: 12) {
                ForEach(HighlightColor.allCases) { item in
                    let active = annotations.allHighlighted(item, book: book, chapter: chapter, verses: range)
                    Button {
                        annotations.toggleHighlight(item, book: book, chapter: chapter, verses: range)
                    } label: {
                        Circle()
                            .fill(item.color)
                            .frame(width: 24, height: 24)
                            .overlay(Circle().strokeBorder(.primary.opacity(0.25), lineWidth: 1))
                            .overlay {
                                if active {
                                    Image(systemName: "checkmark")
                                        .font(.caption2.bold())
                                        .foregroundStyle(.black.opacity(0.6))
                                }
                            }
                    }
                    .accessibilityLabel("Highlight \(item.name)")
                    .accessibilityAddTraits(active ? .isSelected : [])
                }

                Divider().frame(height: 22)

                Button {
                    UIPasteboard.general.string = shareText
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    selection.clear()
                } label: {
                    Image(systemName: "doc.on.doc")
                }
                .accessibilityLabel("Copy")

                ShareLink(item: shareText) {
                    Image(systemName: "square.and.arrow.up")
                }
                .accessibilityLabel("Share")

                let marked = annotations.isBookmarked(book: book, chapter: chapter, verses: range)
                Button {
                    annotations.toggleBookmark(book: book, chapter: chapter, verses: range)
                } label: {
                    Image(systemName: marked ? "bookmark.fill" : "bookmark")
                }
                .accessibilityLabel(marked ? "Remove bookmark" : "Bookmark")

                Button {
                    showNoteEditor = true
                } label: {
                    Image(systemName: "square.and.pencil")
                }
                .accessibilityLabel("Add note")

                Button {
                    selection.clear()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel("Clear selection")
            }
            .font(.title3)
            .foregroundStyle(.primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(.regularMaterial, in: Capsule())
            .shadow(color: .black.opacity(0.15), radius: 6, y: 2)
            .padding(.bottom, 8)
        }
    }
}

/// Apply to the view that draws one verse in ReadingView.
struct VerseInteraction: ViewModifier {
    @EnvironmentObject var selection: VerseSelection
    @EnvironmentObject var annotations: AnnotationStore

    let verse: Int
    let book: String
    let chapter: Int

    func body(content: Content) -> some View {
        let highlight = annotations.highlightColor(book: book, chapter: chapter, verse: verse)
        let selected = selection.contains(verse)

        content
            .background(
                RoundedRectangle(cornerRadius: 4)
                    .fill(highlight?.color.opacity(0.35) ?? Color.clear)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .fill(selected ? Color.accentColor.opacity(0.18) : Color.clear)
                    .allowsHitTesting(false)
            )
            .contentShape(Rectangle())
            .onTapGesture { selection.tap(verse) }
            .onLongPressGesture {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                selection.longPress(verse)
            }
            .accessibilityAddTraits(selected ? .isSelected : [])
            .accessibilityAction(named: selected ? "Deselect verse" : "Select verse") {
                selection.tap(verse)
            }
    }
}

extension View {
    func verseInteraction(verse: Int, book: String, chapter: Int) -> some View {
        modifier(VerseInteraction(verse: verse, book: book, chapter: chapter))
    }
}

struct NoteEditorView: View {
    let reference: String
    let onSave: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            TextEditor(text: $text)
                .focused($focused)
                .padding(.horizontal)
                .navigationTitle(reference)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") {
                            onSave(text.trimmingCharacters(in: .whitespacesAndNewlines))
                            dismiss()
                        }
                        .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
                .onAppear { focused = true }
        }
    }
}
