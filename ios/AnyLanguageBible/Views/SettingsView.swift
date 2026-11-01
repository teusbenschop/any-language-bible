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
import Foundation
import CryptoKit



struct SettingsView: View {

    @State private var isLoading = false
    @State private var statusMessage: String?
    @State private var modules: [Module] = loadModulesJson()
    @State private var searchText: String = ""
    @EnvironmentObject private var status: Status
    @FocusState private var filterFocused: Bool


    var body: some View {
        VStack(spacing: 0) {

            VStack(spacing: 12) {
                Button(modules.isEmpty
                       ? String(localized: "Download list of modules")
                       : String(localized: "Update list of modules")) {
                    Task { await Download.downloadModuleList(isLoading: $isLoading, statusMessage: $statusMessage, modules: $modules) }
                }
                .disabled(isLoading)

                if isLoading {
                    ProgressView()
                        .progressViewStyle(.linear)
                }
                if let statusMessage {
                    Text(statusMessage)
                        .foregroundStyle(.secondary)
                }

                if !modules.isEmpty {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(.secondary)
                        TextField(String(localized: "Filter modules"), text: $searchText)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .focused($filterFocused)
                            .submitLabel(.done)
                            .onSubmit { filterFocused = false }
                        if !searchText.isEmpty {
                            Button {
                                searchText = ""
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundStyle(.secondary)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(10)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color(.secondarySystemBackground))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.secondary.opacity(0.4), lineWidth: 1)
                    )
                }
            }
            .padding()

            if !modules.isEmpty {
                List(filteredModules) { module in
                    Button {
                        filterFocused = false
                        Task { await Download.downloadRemoteModule(module: module, isLoading: $isLoading, statusMessage: $statusMessage) }
                    } label: {
                        HStack {
                            Text(module.name)
                            Spacer()
                            Image(systemName: "book")
                                .foregroundStyle(.tint)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .disabled(isLoading)
                }
                .listStyle(.plain)
                .scrollDismissesKeyboard(.interactively)
                .overlay {
                    if filteredModules.isEmpty {
                        Text(String(localized: "No matches"))
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        Spacer()
    }



    private var filteredModules: [Module] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        if query.isEmpty { return modules }
        return modules.filter {
            $0.name.localizedCaseInsensitiveContains(query)
            ||
            $0.abbrev.localizedCaseInsensitiveContains(query)
        }
    }




}
