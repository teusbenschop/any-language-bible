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

struct SetupView: View {

    private enum Step: Int {
        case welcome, source, save
    }

    private enum TestState: Equatable {
        case idle
        case testing
        case reachable
        case failed
    }

    @Environment(\.dismiss) private var dismiss

    // The saved value in the datastore (UserDefaults).
    @AppStorage(sourceUrlStorageKey) private var savedURL: String = ""

    @State private var step: Step = .welcome
    @State private var draftURL: String = ""
    @State private var testState: TestState = .idle
    @State private var testTask: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            Group {
                switch step {
                case .welcome: welcomePage
                case .source: urlPage
                case .save: savePage
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .navigationTitle(title(for: step))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
            }
            .safeAreaInset(edge: .bottom) { navigationBar }
            .animation(.default, value: step)
            .onAppear { draftURL = savedURL }
            .onChange(of: draftURL) {
                // Any edit invalidates an earlier test result.
                testTask?.cancel()
                testState = .idle
            }
            .onDisappear { testTask?.cancel() }
        }
        .interactiveDismissDisabled() // Leave only through the buttons.
    }


    private var welcomePage: some View {
        VStack(spacing: 16) {
            Image(systemName: "newspaper")
                .font(.system(size: 60))
                .foregroundStyle(.tint)
            Text("Welcome")
                .font(.title.bold())
            Text("This app will show the Bible of your choice.")
                .multilineTextAlignment(.center)
            Text("Install the Bible you prefer.")
                .multilineTextAlignment(.center)
            Text("Press Next to proceed.")
                .multilineTextAlignment(.center)
        }
        .padding()
    }


    private var urlPage: some View {
        Form {
            Section {
                HStack(spacing: 8) {
                    Image(systemName: "globe")
                        .foregroundStyle(isInvalidUrl ? .red : .secondary)
                        .accessibilityHidden(true)
                    TextField("Your source", text: $draftURL)
                        .textContentType(.URL)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
                .padding(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isInvalidUrl ? Color.red : Color.clear, lineWidth: 1.5)
                )
                .animation(.default, value: isInvalidUrl)
            } header: {
                Text("Source")
            } footer: {
                if isInvalidUrl {
                    Text("Please enter a valid web address.")
                        .foregroundStyle(.red)
                } else {
                    Text("Enter the web address of your preferred source.")
                }
            }

            Section {
                Button {
                    testConnection(text: validURL)
                } label: {
                    HStack {
                        Label("Test web address", systemImage: "antenna.radiowaves.left.and.right")
                        Spacer()
                        testStatusView
                    }
                }
                .disabled(validURL == nil || testState == .testing)
            }

            Section {
                switch testState {
                case .reachable:
                    Text("The address can be reached.\nTap Next to continue.")
                case .failed:
                    Text("The address could not be reached.\nCheck it and try again.")
                        .foregroundStyle(.red)
                default:
                    Text("The address must be reachable before you can continue.")
                }
            }
        }
    }


    private var savePage: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 60))
                .foregroundStyle(.green)
            Text("The news feed will be set to:")
            Text(validURL ?? "")
                .font(.callout.monospaced())
                .multilineTextAlignment(.center)
                .textSelection(.enabled)
            Button("Save and close") {
                if let validURL { savedURL = validURL }
                dismiss()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(validURL == nil)
        }
        .padding()
    }

    @ViewBuilder
    private var testStatusView: some View {
        switch testState {
        case .idle:      EmptyView()
        case .testing:   ProgressView()
        case .reachable: Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
        case .failed:    Image(systemName: "xmark.circle.fill").foregroundStyle(.red)
        }
    }


    private var navigationBar: some View {
        HStack {
            if step != .welcome {
                Button("Previous") { goBack() }
            }
            Spacer()
            if canGoNext {
                Button("Next") { goNext() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .background(.bar)
    }

    private var canGoNext: Bool {
        switch step {
        case .welcome: return true
        case .source:     return testState == .reachable
        case .save:    return false
        }
    }

    private func goNext() {
        guard canGoNext, let next = Step(rawValue: step.rawValue + 1) else { return }
        step = next
    }

    private func goBack() {
        guard let previous = Step(rawValue: step.rawValue - 1) else { return }
        step = previous
    }

    private func title(for step: Step) -> String {
        switch step {
        case .welcome: return "Welcome"
        case .source:     return "Source address"
        case .save:    return "Save"
        }
    }


    // The draft as a valid http(s) address, or nil.
    private var validURL: String? {
        Network.getValidUrlTextOrNil(Network.secureUrlText(from: draftURL.trimmingCharacters(in: .whitespacesAndNewlines)))
    }

    // Not empty, but not a valid http(s) address either.
    private var isInvalidUrl: Bool {
        let trimmed = draftURL.trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmed.isEmpty && validURL == nil
    }

    private func testConnection(text : String?) {
        guard text != nil else { return }
        guard let url = URL(string: text!) else { return }
        testTask?.cancel()
        testState = .testing
        testTask = Task {
            let ok = await Network.isReachable(url)
            guard !Task.isCancelled else { return }
            testState = ok ? .reachable : .failed
        }
    }


}
