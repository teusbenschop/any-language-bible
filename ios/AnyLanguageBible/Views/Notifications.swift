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
import Combine
import UserNotifications

// This is store that manages the notifications.
// The store reads the pending requests from iOS,
// so there is no second copy of the data to keep in sync.

struct WeeklyNotification: Identifiable, Hashable, Comparable {

    static let idPrefix = "news-weekly-"

    let weekday: Int   // 1 = Sunday ... 7 = Saturday (Gregorian)
    let hour: Int
    let minute: Int

    // Each notification gets an identifier made of weekday, hour and minute,
    // so adding the same day and time twice replaces the first instead of duplicating it.
    var id: String { "\(Self.idPrefix)\(weekday)-\(hour)-\(minute)" }

    var dayName: String { Calendar.current.weekdaySymbols[weekday - 1] }

    var timeText: String {
        var c = DateComponents()
        c.hour = hour
        c.minute = minute
        let date = Calendar.current.date(from: c) ?? Date()
        return date.formatted(date: .omitted, time: .shortened)
    }

    static func < (a: Self, b: Self) -> Bool {
        (a.weekday, a.hour, a.minute) < (b.weekday, b.hour, b.minute)
    }

    init(weekday: Int, hour: Int, minute: Int) {
        self.weekday = weekday
        self.hour = hour
        self.minute = minute
    }

    // Rebuilds an item from a pending request. Returns nil for requests that are not ours.
    init?(request: UNNotificationRequest) {
        guard request.identifier.hasPrefix(Self.idPrefix),
              let trigger = request.trigger as? UNCalendarNotificationTrigger,
              let weekday = trigger.dateComponents.weekday,
              let hour = trigger.dateComponents.hour,
              let minute = trigger.dateComponents.minute else { return nil }
        self.init(weekday: weekday, hour: hour, minute: minute)
    }
}


@MainActor
final class WeeklyNotificationStore: ObservableObject {

    @Published private(set) var items: [WeeklyNotification] = []

    private let center = UNUserNotificationCenter.current()

    func refresh() async {
        let requests = await center.pendingNotificationRequests()
        items = requests.compactMap { WeeklyNotification(request: $0) }.sorted()
    }

    // Returns false when the user has not allowed notifications.
    func add(_ item: WeeklyNotification) async -> Bool {
        let granted = (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        guard granted else { return false }

        let content = UNMutableNotificationContent()
        content.title = "News"
        content.body = "Check the latest news"
        content.sound = .default

        var components = DateComponents()
        components.weekday = item.weekday
        components.hour = item.hour
        components.minute = item.minute

        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        let request = UNNotificationRequest(identifier: item.id, content: content, trigger: trigger)

        // Same identifier replaces an existing one, so no duplicates.
        try? await center.add(request)
        await refresh()
        return true
    }

    func remove(_ item: WeeklyNotification) async {
        center.removePendingNotificationRequests(withIdentifiers: [item.id])
        await refresh()
    }


}
