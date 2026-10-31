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
import Network


enum Network {


    static public func isValidHost(_ host: String) -> Bool {
        let host = host.lowercased()

        // Special cases: localhost and IP addresses.
        if host == "localhost" { return true }
        if host.contains(":") { return IPv6Address(host) != nil }
        if IPv4Address(host) != nil { return true }

        // Regular host name: dot-separated labels, e.g. news.example.org
        guard host.count <= 253 else { return false }
        let labels = host.split(separator: ".", omittingEmptySubsequences: false)
        guard labels.count >= 2 else { return false } // A dot is required, so "https://news" is invalid.

        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-"))
        for label in labels {
            guard (1...63).contains(label.count),
                  !label.hasPrefix("-"),
                  !label.hasSuffix("-"),
                  label.unicodeScalars.allSatisfy({ allowed.contains($0) })
            else { return false }
        }

        // The last label (top-level domain) needs at least 2 characters, including a letter.
        // This rejects things like "999.1.1.1" or "example.c".
        let tld = labels[labels.count - 1]
        return tld.count >= 2 && tld.unicodeScalars.contains { CharacterSet.letters.contains($0) }
    }


    // Returns the trimmed text if it is a valid web address, otherwise nil.
    static public func getValidUrlTextOrNil(_ text: String) -> String? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              trimmed.rangeOfCharacter(from: .whitespacesAndNewlines) == nil, // No spaces inside.
              let url = URL(string: trimmed),
              let scheme = url.scheme?.lowercased(),
              scheme == "https",
              let host = url.host(percentEncoded: false),
              Network.isValidHost(host)
        else { return nil }

        if let port = url.port, !(1...65535).contains(port) { return nil }
        return trimmed
    }


    // Turns user input into a secure web address.
    // - "news.org"         -> "https://news.org"
    // - "http://news.org"  -> "https://news.org"
    // - "HTTPS://news.org" -> "https://news.org"
    // Text with another scheme (like "ftp://...") is returned unchanged,
    // so that validation can reject it afterwards.
    static func secureUrlText(from text: String) -> String {
        var result = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !result.isEmpty else { return result }

        if let range = result.range(of: "^https?://", options: [.regularExpression, .caseInsensitive]) {
            // Has http:// or https://: make sure it is https://.
            result.replaceSubrange(range, with: "https://")
        } else if !result.contains("://") {
            // No scheme at all: add one.
            result = "https://" + result
        }
        return result
    }

    public static func statusIsOK(_ url: URL, method: String) async -> Bool {
        var request = URLRequest(url: url, timeoutInterval: 10)
        request.httpMethod = method
        guard let (_, response) = try? await URLSession.shared.data(for: request),
              let http = response as? HTTPURLResponse else { return false }
        return (200..<400).contains(http.statusCode)
    }


    public static func isReachable(_ url: URL) async -> Bool {
        return await Network.statusIsOK(url, method: "GET")
    }



}
