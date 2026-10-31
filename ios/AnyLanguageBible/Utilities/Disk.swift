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


import Combine
import Foundation


func eraseAllData() {
    do {
        let fileManager = FileManager.default
        let directory = try fileManager.url(for: .applicationSupportDirectory,
                                            in: .userDomainMask,
                                            appropriateFor: nil,
                                            create: true)
        let files = try fileManager.contentsOfDirectory(at: directory,
                                                        includingPropertiesForKeys: nil)
        for file in files {
            do {
                try fileManager.removeItem(at: file)
            } catch {
                print("Failed to erase \(file.lastPathComponent): \(error)")
            }
        }
    } catch {
        print("Failed to erase the data: \(error)")
    }
}


func saveModulesJson(data: Data) {
    do {
        let directory = try FileManager.default.url(for: .applicationSupportDirectory,
                                                    in: .userDomainMask,
                                                    appropriateFor: nil,
                                                    create: true)
        var fileURL = directory.appendingPathComponent("modules.json")
        try data.write(to: fileURL, options: .atomic)
        // Exclude the file from backup for safety.
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try fileURL.setResourceValues(values)
    } catch {
        print("Failed to save the module: \(error)")
    }
}


struct Module: Codable, Identifiable {
    let abbrev: String
    let id: Int
    let name: String
    let source: String
    let type: Int
    let uuid: String
    let version: String
}
// Conforming to Identifiable is optional but is used here.
// This way the array can be used directly in a SwiftUI List or ForEach,
// because the struct already has an id property.


func loadModulesJson() -> [Module] {
    do {
        let directory = try FileManager.default.url(for: .applicationSupportDirectory,
                                                    in: .userDomainMask,
                                                    appropriateFor: nil,
                                                    create: true)
        let data = try Data(contentsOf: directory.appendingPathComponent("modules.json"))
        return try JSONDecoder().decode([Module].self, from: data)
    } catch {
        print("Failed to load modules.json: \(error)")
        return []
    }
}


func saveModuleJson(id: Int, data: Data) {
    do {
        let directory = try FileManager.default.url(for: .applicationSupportDirectory,
                                                    in: .userDomainMask,
                                                    appropriateFor: nil,
                                                    create: true)
        var fileURL = directory.appendingPathComponent("\(id).json")
        try data.write(to: fileURL, options: .atomic)
        // Exclude the file from backup for safety.
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try fileURL.setResourceValues(values)
    } catch {
        print("Failed to save the module: \(error)")
    }
}


struct BibleVerse: Codable {
    let book: Int
    let chapter: Int
    let verse: Int
    let text: String
}

struct BibleFile: Codable {
    let data: [BibleVerse]
}

func loadModuleJson(id: Int) -> [BibleVerse] {
    do {
        let directory = try FileManager.default.url(for: .applicationSupportDirectory,
                                                    in: .userDomainMask,
                                                    appropriateFor: nil,
                                                    create: false)
        let data = try Data(contentsOf: directory.appendingPathComponent("\(id).json"))
        let decoder = JSONDecoder()
        decoder.allowsJSON5 = true // // Tolerate trailing commas.
        return try decoder.decode(BibleFile.self, from: data).data
    } catch {
        print("Failed to load \(id).json: \(error)")
        return []
    }
}


// Returns a copy of the verses with the text case folded.
func caseFoldedVerses(_ verses: [BibleVerse]) -> [BibleVerse] {
    verses.map { verse in
        BibleVerse(book: verse.book,
                   chapter: verse.chapter,
                   verse: verse.verse,
                   text: verse.text.folding(options: .caseInsensitive, locale: nil))
    }
    // The folding(options:locale:) does Unicode case folding.
    // Passing locale: nil makes it locale-independent.
}


// Case folds the verses and encodes them as a plain JSON array [...].
func caseFoldedJson(_ verses: [BibleVerse]) -> Data? {
    do {
        let encoder = JSONEncoder()
        // Keep non-ASCII characters readable and the output stable.
        encoder.outputFormatting = [.withoutEscapingSlashes, .sortedKeys]
        return try encoder.encode(caseFoldedVerses(verses))
    } catch {
        print("Failed to encode case folded verses: \(error)")
        return nil
    }
}


func saveCaseFoldedModule(id: Int, verses: [BibleVerse]) {
    guard let json = caseFoldedJson(verses) else { return }
    do {
        let directory = try FileManager.default.url(for: .applicationSupportDirectory,
                                                    in: .userDomainMask,
                                                    appropriateFor: nil,
                                                    create: true)
        try json.write(to: directory.appendingPathComponent("\(id)-folded.json"),
                       options: .atomic)
    } catch {
        print("Failed to save \(id)-folded.json: \(error)")
    }
}


func loadFoldedModuleJson(id: Int) -> [BibleVerse] {
    do {
        let directory = try FileManager.default.url(for: .applicationSupportDirectory,
                                                    in: .userDomainMask,
                                                    appropriateFor: nil,
                                                    create: false)
        let data = try Data(contentsOf: directory.appendingPathComponent("\(id)-folded.json"))
        return try JSONDecoder().decode([BibleVerse].self, from: data)
    } catch {
        print("Failed to load \(id)-folded.json: \(error)")
        return []
    }
}
