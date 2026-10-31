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

// The search design

// It does a clever search, first serches on phrase, then on words.
// Verses that contain more search words go higher in the ranking.
// All search works on the case folded text.
// Lists the search results.
// Tapping a result switches to the .read tab and focuses the text.
// Going back from the .read tab to the search tab,
// it retains former results, these are cached.
// Update the search results while typing.


import SwiftUI
import Combine

struct SearchHit: Identifiable {
    let id: Int // Index of the verse in the module.
    let book: Int
    let chapter: Int
    let verse: Int
    let text: String // Original text for display.
}

@MainActor
final class SearchModel: ObservableObject {

    @Published var query = ""
    @Published private(set) var hits: [SearchHit] = []
    @Published private(set) var truncated = false
    @Published private(set) var indexMissing = false

    private var folded: [BibleVerse] = []     // Searched.
    private var original: [BibleVerse] = []   // Displayed.
    private var loadedVolume: Int?
    private var searchedQuery: String?
    private var searchedVolume: Int?
    private let maxHits = 300

    // Brings the results up to date with the query.
    // Does nothing if the cached results are still current.
    func refresh(volumeId: Int) async {

        let phrase = Self.normalize(query)

        if phrase == searchedQuery && volumeId == searchedVolume { return }

        if phrase.isEmpty {
            hits = []
            truncated = false
            searchedQuery = phrase
            searchedVolume = volumeId
            return
        }

        // Debounce: a new keystroke cancels this task before the search starts.
        try? await Task.sleep(for: .milliseconds(200))
        if Task.isCancelled { return }

        if loadedVolume != volumeId {
            await load(volumeId)
            if Task.isCancelled { return }
        }

        let words = Self.words(in: phrase)
        let folded = self.folded
        let original = self.original
        let maxHits = self.maxHits

        // Search off the main thread, and stop early when cancelled.
        let task = Task.detached(priority: .userInitiated) {
            Self.search(folded: folded, original: original,
                        phrase: phrase, words: words, maxHits: maxHits)
        }
        let result = await withTaskCancellationHandler {
            await task.value
        } onCancel: {
            task.cancel()
        }
        guard let result, !Task.isCancelled else { return }

        hits = result.hits
        truncated = result.truncated
        searchedQuery = phrase
        searchedVolume = volumeId
    }

    private func load(_ volumeId: Int) async {
        let task = Task.detached(priority: .userInitiated) {
            await (loadFoldedModuleJson(id: volumeId), loadModuleJson(id: volumeId))
        }
        let (foldedVerses, originalVerses) = await task.value
        folded = foldedVerses
        original = originalVerses
        loadedVolume = volumeId
        indexMissing = foldedVerses.isEmpty
        // Force a new search against the newly loaded data.
        searchedQuery = nil
    }

    // MARK: - Search

    /// Case fold the query, and collapse white space.
    private static func normalize(_ text: String) -> String {
        text.folding(options: .caseInsensitive, locale: nil)
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }

    /// The distinct words of an already normalized query.
    private static func words(in phrase: String) -> [String] {
        var seen = Set<String>()
        return phrase
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { !$0.isEmpty && seen.insert($0).inserted }
    }

    /// Returns nil when cancelled.
    nonisolated private static func search(folded: [BibleVerse],
                                           original: [BibleVerse],
                                           phrase: String,
                                           words: [String],
                                           maxHits: Int) -> (hits: [SearchHit], truncated: Bool)? {
        guard !words.isEmpty else { return ([], false) }

        var scored: [(index: Int, score: Int)] = []

        for (index, verse) in folded.enumerated() {
            if index % 2000 == 0, Task.isCancelled { return nil }

            let text = verse.text
            var matched = 0
            for word in words where text.contains(word) { matched += 1 }
            if matched == 0 { continue }

            // A verse with the whole phrase always outranks verses with only words.
            let phraseBonus = text.contains(phrase) ? 1000 : 0
            scored.append((index, phraseBonus + matched))
        }

        // Highest score first, then in canonical order.
        scored.sort { $0.score != $1.score ? $0.score > $1.score : $0.index < $1.index }

        let useOriginal = original.count == folded.count
        let hits = scored.prefix(maxHits).map { item -> SearchHit in
            let verse = folded[item.index]
            return SearchHit(id: item.index,
                             book: verse.book,
                             chapter: verse.chapter,
                             verse: verse.verse,
                             text: useOriginal ? original[item.index].text : verse.text)
        }
        return (hits, scored.count > maxHits)
    }
}


struct SearchView: View {

    @ObservedObject var model: SearchModel
    let onSelect: (SearchHit) -> Void

    var body: some View {
        Group {
            if model.indexMissing {
                ContentUnavailableView("No search index",
                                       systemImage: "magnifyingglass",
                                       description: Text("Download a module first."))
            } else if model.hits.isEmpty && !model.query.isEmpty {
                ContentUnavailableView.search(text: model.query)
            } else {
                List {
                    Section {
                        ForEach(model.hits) { hit in
                            Button {
                                onSelect(hit)
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(reference(for: hit))
                                        .font(.headline)
                                    Text(hit.text)
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(3)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }
                    } footer: {
                        if model.truncated {
                            Text("Showing the best \(model.hits.count) results.")
                        }
                    }
                }
                .scrollDismissesKeyboard(.interactively)
            }
        }
        .searchable(text: $model.query, prompt: "Word or phrase")
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        // Runs on appearing, and on every change of query.
        // A new keystroke cancels the previous run.
        .task(id: model.query) {
            await model.refresh(volumeId: Settings.getVolumeId())
        }
    }

    private func reference(for hit: SearchHit) -> String {
        "\(String(describing: bookName(forId: hit.book) ?? "Unknown" )) \(hit.chapter):\(hit.verse)"
    }
}
