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
import WebKit

func createLoadingHTML(message: String) -> String {
    """
    <!DOCTYPE html>
    <html lang="en">
    <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title></title>
    <style>
      html, body {
        height: 100%;
        margin: 0;
      }
      body {
        display: flex;
        justify-content: center;
        align-items: center;
        text-align: center;
        font-family: -apple-system, system-ui, sans-serif;
      }
      div {
        padding: 1rem;
      }
    </style>
    </head>
    <body>
    <div>Loading news feed</div>
    </body>
    </html>
    """
}


func siteHasProblem(_ url: URL) async -> String? {
     var request = URLRequest(url: url)
     request.httpMethod = "GET"
     request.timeoutInterval = 5
     do {
          let (_, response) = try await URLSession.shared.data(for: request)
          if let http = response as? HTTPURLResponse {
               if (200..<400).contains(http.statusCode) {
                    return nil
               }
               return "The news outlet responded with status code \(http.statusCode)."
          }
          return "Unexpected response from the news outlet."
     } catch {
          print("Error: \(error)")
          return error.localizedDescription
     }
}


func getNewsFeedUrl() -> URL? {

    let siteUrl = UserDefaults.standard.string(forKey: sourceUrlStorageKey) ?? ""

    let trimmed = siteUrl.trimmingCharacters(in: .whitespacesAndNewlines)
    guard let url = URL(string: trimmed),
          let scheme = url.scheme?.lowercased(),
          scheme == "https",
          url.host() != nil else {
        return nil
    }
    return url
}


func loadPage(page: WebPage) async {
    let url = getNewsFeedUrl()
    page.load(html: createLoadingHTML(message: ""))
    if url == nil {
        page.load(html: createLoadingHTML(message: "No web address known"))
    }
    else if let problem = await siteHasProblem(url!) {
        page.load(html: createLoadingHTML(message: problem))
    } else {
        page.load(url!)
    }
}


let sourceUrlStorageKey = "sourceUrl"
