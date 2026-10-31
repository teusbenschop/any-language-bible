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
import CryptoKit
import SwiftUI


enum Download {

    enum NetworkError: LocalizedError {
        case invalidServerAddress
        case invalidResponse
        case missingDateHeader
        case serverError
        case notUTF8

        var errorDescription: String? {
            switch self {
            case .invalidServerAddress:
                return "Could not refresh remote modules due to an invalid web address for the modules server"
            case .invalidResponse:
                return "Got invalid response from the modules server"
            case .missingDateHeader:
                return "Could not retrieve relevant information from the modules server"
            case .serverError:
                return "Download failed with a server error"
            case .notUTF8:
                return "Modules file is not valid UTF-8 text"
            }
        }
    }

    private static let serverAddress = "https://bibledit.org"


    // Download the index of available modules as gzipped JSON text.
    static func fetchModulesIndex() async throws -> Data {
        try await fetchData(file: "modules.json.gz")
    }


    // Download one module as gzipped JSON text.
    // The file is named "n.json.gz.dat", where "n" is the module identifier.
    static func fetchModule(id: Int) async throws -> Data {
        try await fetchData(file: "\(id).json.gz")
    }


    private static func fetchData(file: String) async throws -> Data {
        let baseUrl = try await securedBaseUrl()
        let (data, response) = try await URLSession.shared.data(from: baseUrl.appending(path: file))
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            throw NetworkError.serverError
        }
        return data
    }


    // The server address followed by the security prefix that changes every day.
    // The calculation on this client matches the calculation on the server.
    private static func securedBaseUrl() async throws -> URL {
        guard let url = URL(string: serverAddress) else {
            throw NetworkError.invalidServerAddress
        }
        return url.appending(path: "modules")
    }


}

extension Download {

    @MainActor // Run on main thread because it updates controls.
    static func downloadModuleList(isLoading: Binding<Bool>,
                                   statusMessage: Binding<String?>,
                                   modules: Binding<[Module]>) async {
        isLoading.wrappedValue = true
        defer { isLoading.wrappedValue = false }

        statusMessage.wrappedValue = "Downloading"

        do {
            let gzipped = try await fetchModulesIndex()
            let json = try gunzip(gzipped)

            // Save the text to disk and load it again as parsed JSON.
            saveModulesJson(data: json)
            modules.wrappedValue = loadModulesJson()

            statusMessage.wrappedValue = "Number of available modules: \(modules.wrappedValue.count)"
        } catch let error as NetworkError {
            statusMessage.wrappedValue = error.localizedDescription
        } catch {
            statusMessage.wrappedValue = "Request failed: \(error.localizedDescription)"
        }
    }

}

extension Download {

    @MainActor // Run on main thread because it updates controls.
    static func downloadRemoteModule(module: Module,
                                     isLoading: Binding<Bool>,
                                     statusMessage: Binding<String?>
    ) async {
        isLoading.wrappedValue = true
        defer { isLoading.wrappedValue = false }

        statusMessage.wrappedValue = "Downloading"

        do {
            // Download gzipped module, deflate it.
            let gzipped = try await Download.fetchModule(id: module.id)
            let json = try gunzip(gzipped)
            // Save the JSON to file.
            saveModuleJson(id: module.id, data: json)
            // Load the module JSON, parse it, create case folded JSON and save that.
            let verses = loadModuleJson(id: module.id)
            saveCaseFoldedModule(id: module.id, verses: verses)
            statusMessage.wrappedValue = "Module was downloaded"
            // Set this module's ID as activated.
            Settings.setVolumeId(module.id)
        } catch let error as Download.NetworkError {
            statusMessage.wrappedValue = error.localizedDescription
        } catch {
            statusMessage.wrappedValue = "Request failed: \(error.localizedDescription)"
        }
    }
}


extension Download {

    enum GunzipError: Error { case invalid }

    // A gzip file has:
    // * a 10-byte header,
    // * optional extra header fields,
    // * a raw DEFLATE stream,
    // * an 8-byte trailer (CRC32 + original size).
    // Apple’s .zlib decompression algorithm actually handles raw DEFLATE (no zlib header).
    // So the function below strips the gzip wrapper by hand
    // and hands the inner stream to the system decompressor.

    // The function takes gzip-compressed Data and returns the decompressed Data.
    static func gunzip(_ input: Data) throws -> Data {

        // Data slices keep the indices of their parent
        // (a slice of bytes 100..<200 has startIndex == 100).
        // Copying into a new Data resets indices to start at 0, so d[0], d[1] etc. are safe.
        let d = Data(input)

        // Validates the header.
        // count > 18: minimum is 10 header bytes + 8 trailer bytes,
        // so you need more than that to have any payload.
        // 0x1f 0x8b: the gzip magic number.
        // d[2] == 8: compression method 8 = DEFLATE, the only one gzip defines.
        guard d.count > 18, d[0] == 0x1f, d[1] == 0x8b, d[2] == 8 else { throw GunzipError.invalid }

        // Byte 3 is the flags byte, which says which optional header fields follow.
        // offset tracks where the data stream starts; the fixed header occupies bytes 0–9.
        let flags = d[3]
        var offset = 10

        // If the FEXTRA bit is set, an “extra” field follows:
        // a 2-byte little-endian length (XLEN), then that many bytes.
        // The guard checks the length bytes are present,
        // and the code then skips the 2 length bytes plus the extra data.
        if flags & 0x04 != 0 { // FEXTRA
            guard d.count > offset + 2 else { throw GunzipError.invalid }
            let xlen = Int(d[offset]) | (Int(d[offset + 1]) << 8)
            offset += 2 + xlen
        }

        // If FNAME is set, the original filename follows as a null-terminated string.
        // The loop scans to the terminating 0 byte (bounded by d.count),
        // then offset += 1 steps past the terminator.
        if flags & 0x08 != 0 { // FNAME
            while offset < d.count, d[offset] != 0 { offset += 1 }
            offset += 1
        }

        // Identical logic for the optional null-terminated comment.
        if flags & 0x10 != 0 { // FCOMMENT
            while offset < d.count, d[offset] != 0 { offset += 1 }
            offset += 1
        }

        // If FHCRC is set, a 2-byte CRC16 of the header follows; skip it.
        if flags & 0x02 != 0 { offset += 2 } // FHCRC

        // After skipping the header,
        // there must be at least one byte of DEFLATE data before the 8-byte trailer.
        // This also protects against an offset that ran past the end.
        guard offset < d.count - 8 else { throw GunzipError.invalid }

        // Extracts the raw DEFLATE stream:
        // everything between the header and the 8-byte trailer (CRC32 + ISIZE),
        // which is discarded.
        let deflate = d.subdata(in: offset ..< d.count - 8)

        // Bridges to NSData to use NSData.decompressed(using:),
        // with .zlib meaning raw DEFLATE in Apple’s API.
        // Returns the result as Data.
        return try (deflate as NSData).decompressed(using: .zlib) as Data
    }


}
