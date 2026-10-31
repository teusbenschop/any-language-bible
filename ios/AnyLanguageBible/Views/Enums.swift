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


enum BibleTab: String, CaseIterable, Identifiable {
    case read, search, bookmarks, settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .read:      String(localized: "Read")
        case .search:    String(localized: "Search")
        case .bookmarks: String(localized: "Bookmarks")
        case .settings:  String(localized: "Settings")
        }
    }

    var plainIcon: String {
        switch self {
        case .read:      "book"
        case .search:    "magnifyingglass"
        case .bookmarks: "bookmark"
        case .settings:  "gear"
        }
    }

    var filledIcon: String {
        switch self {
        case .read:      "book.fill"
        case .search:    "magnifyingglass.circle.fill"
        case .bookmarks: "bookmark.fill"
        case .settings:  "gear.circle.fill"
        }
    }
}
