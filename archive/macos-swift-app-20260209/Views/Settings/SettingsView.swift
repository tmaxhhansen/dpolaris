//
//  SettingsView.swift
//  dPolaris
//
//  App settings and configuration
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @AppStorage("apiHost") private var apiHost = "127.0.0.1"
    @AppStorage("apiPort") private var apiPort = 8420
    @AppStorage("backendPath") private var backendPath = "~/my-git/dPolaris_ai"
    @AppStorage("refreshInterval") private var refreshInterval = 60
    @AppStorage("showMenuBarIcon") private var showMenuBarIcon = true
    @AppStorage("notificationsEnabled") private var notificationsEnabled = true
    @AppStorage("soundEnabled") private var soundEnabled = true
    @AppStorage("darkMode") private var darkMode = false
    @AppStorage("autoStartDaemon") private var autoStartDaemon = true
    @AppStorage("goalAmount") private var goalAmount = 3_000_000.0

    @State private var isTestingConnection = false
    @State private var connectionStatus: ConnectionStatus = .unknown

    enum ConnectionStatus {
        case unknown, connected, failed
    }

    var body: some View {
        Form {
            // API Connection
            Section("API Connection") {
                HStack {
                    Text("Host")
                    Spacer()
                    TextField("Host", text: $apiHost)
                        .textFieldStyle(.roundedBorder)
                        .frame(width: 200)
                }

                HStack {
                    Text("Port")
                    Spacer()
                    TextField("Port", value: $apiPort, format: .number)
                        .textFieldStyle(.roundedBorder)
                        .frame(width: 100)
                }

                HStack {
                    Button(action: testConnection) {
                        if isTestingConnection {
                            ProgressView()
                                .scaleEffect(0.7)
                        } else {
                            Label("Test Connection", systemImage: "antenna.radiowaves.left.and.right")
                        }
                    }
                    .disabled(isTestingConnection)

                    Spacer()

                    HStack(spacing: 6) {
                        Circle()
                            .fill(connectionStatusColor)
                            .frame(width: 10, height: 10)
                        Text(connectionStatusText)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }

            // Daemon Settings
            Section("AI Daemon") {
                Toggle("Auto-start daemon on app launch", isOn: $autoStartDaemon)

                HStack {
                    Text("Backend Path")
                    Spacer()
                    TextField("Path to dpolaris_ai", text: $backendPath)
                        .textFieldStyle(.roundedBorder)
                        .frame(width: 300)
                }

                HStack {
                    Text("Current Status")
                    Spacer()
                    HStack(spacing: 6) {
                        Circle()
                            .fill(appState.isDaemonRunning ? Color.green : Color.red)
                            .frame(width: 10, height: 10)
                        Text(appState.isDaemonRunning ? "Running" : "Stopped")
                    }
                }

                HStack {
                    Button(action: { appState.startDaemon() }) {
                        Label("Start Daemon", systemImage: "play.fill")
                    }
                    .disabled(appState.isDaemonRunning)

                    Button(action: { appState.stopDaemon() }) {
                        Label("Stop Daemon", systemImage: "stop.fill")
                    }
                    .disabled(!appState.isDaemonRunning)
                }
            }

            // Data Refresh
            Section("Data Refresh") {
                Picker("Refresh Interval", selection: $refreshInterval) {
                    Text("30 seconds").tag(30)
                    Text("1 minute").tag(60)
                    Text("2 minutes").tag(120)
                    Text("5 minutes").tag(300)
                    Text("Manual only").tag(0)
                }

                Button("Refresh Now") {
                    appState.refreshAll()
                }
            }

            // Goal Settings
            Section("Portfolio Goal") {
                HStack {
                    Text("Target Amount")
                    Spacer()
                    TextField("Goal", value: $goalAmount, format: .currency(code: "USD"))
                        .textFieldStyle(.roundedBorder)
                        .frame(width: 150)
                }

                if let progress = appState.portfolio?.totalValue, goalAmount > 0 {
                    let percent = (progress / goalAmount) * 100
                    HStack {
                        Text("Progress")
                        Spacer()
                        Text("\(String(format: "%.2f", percent))%")
                            .fontWeight(.medium)
                    }
                }
            }

            // Notifications
            Section("Notifications") {
                Toggle("Enable Notifications", isOn: $notificationsEnabled)
                Toggle("Sound Effects", isOn: $soundEnabled)
                    .disabled(!notificationsEnabled)
            }

            // Appearance
            Section("Appearance") {
                Toggle("Show Menu Bar Icon", isOn: $showMenuBarIcon)

                Picker("Appearance", selection: $darkMode) {
                    Text("System").tag(false)
                    Text("Dark").tag(true)
                }
            }

            // Data Management
            Section("Data Management") {
                Button("Export Data") {
                    exportData()
                }

                Button("Clear Local Cache") {
                    clearCache()
                }

                Button("Reset All Settings", role: .destructive) {
                    resetSettings()
                }
            }

            // About
            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0")
                        .foregroundColor(.secondary)
                }

                HStack {
                    Text("Build")
                    Spacer()
                    Text("1")
                        .foregroundColor(.secondary)
                }

                Link("Documentation", destination: URL(string: "https://github.com/dpolaris")!)

                Link("Report Issue", destination: URL(string: "https://github.com/dpolaris/issues")!)
            }
        }
        .formStyle(.grouped)
        .navigationTitle("Settings")
        .onAppear {
            applyAPIConfiguration()
        }
        .onChange(of: apiHost) { _, _ in
            applyAPIConfiguration()
        }
        .onChange(of: apiPort) { _, _ in
            applyAPIConfiguration()
        }
    }

    private var connectionStatusColor: Color {
        switch connectionStatus {
        case .unknown: return .gray
        case .connected: return .green
        case .failed: return .red
        }
    }

    private var connectionStatusText: String {
        switch connectionStatus {
        case .unknown: return "Not tested"
        case .connected: return "Connected"
        case .failed: return "Failed"
        }
    }

    private func testConnection() {
        isTestingConnection = true
        applyAPIConfiguration()

        Task {
            let connected = await APIService.shared.checkConnection()
            connectionStatus = connected ? .connected : .failed
            appState.isConnected = connected
            isTestingConnection = false
        }
    }

    private func applyAPIConfiguration() {
        Task { @MainActor in
            APIService.shared.updateBaseURL(host: apiHost, port: apiPort)
        }
    }

    private func exportData() {
        // TODO: Implement data export
        print("Exporting data...")
    }

    private func clearCache() {
        // TODO: Implement cache clearing
        print("Clearing cache...")
    }

    private func resetSettings() {
        apiHost = "127.0.0.1"
        apiPort = 8420
        backendPath = "~/my-git/dPolaris_ai"
        refreshInterval = 60
        showMenuBarIcon = true
        notificationsEnabled = true
        soundEnabled = true
        darkMode = false
        autoStartDaemon = true
        goalAmount = 3_000_000.0
    }
}

// MARK: - Settings Sections for Tab View

struct GeneralSettingsView: View {
    @AppStorage("refreshInterval") private var refreshInterval = 60
    @AppStorage("showMenuBarIcon") private var showMenuBarIcon = true
    @AppStorage("autoStartDaemon") private var autoStartDaemon = true

    var body: some View {
        Form {
            Section("Startup") {
                Toggle("Auto-start AI daemon on launch", isOn: $autoStartDaemon)
                Toggle("Show menu bar icon", isOn: $showMenuBarIcon)
            }

            Section("Data Refresh") {
                Picker("Auto-refresh interval", selection: $refreshInterval) {
                    Text("30 seconds").tag(30)
                    Text("1 minute").tag(60)
                    Text("2 minutes").tag(120)
                    Text("5 minutes").tag(300)
                    Text("Manual only").tag(0)
                }
            }
        }
        .formStyle(.grouped)
        .frame(width: 450, height: 250)
    }
}

struct APISettingsView: View {
    @AppStorage("apiHost") private var apiHost = "127.0.0.1"
    @AppStorage("apiPort") private var apiPort = 8420

    @State private var isTestingConnection = false
    @State private var connectionOK = false

    var body: some View {
        Form {
            Section("API Server") {
                TextField("Host", text: $apiHost)
                TextField("Port", value: $apiPort, format: .number)
            }

            Section {
                HStack {
                    Button("Test Connection") {
                        testConnection()
                    }
                    .disabled(isTestingConnection)

                    if isTestingConnection {
                        ProgressView()
                            .scaleEffect(0.7)
                    }

                    Spacer()

                    if connectionOK {
                        Label("Connected", systemImage: "checkmark.circle.fill")
                            .foregroundColor(.green)
                    }
                }
            }

            Section {
                Text("The API server should be running at http://\(apiHost):\(apiPort)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .formStyle(.grouped)
        .frame(width: 450, height: 250)
    }

    private func testConnection() {
        isTestingConnection = true
        connectionOK = false
        Task { @MainActor in
            APIService.shared.updateBaseURL(host: apiHost, port: apiPort)
        }

        Task {
            connectionOK = await APIService.shared.checkConnection()
            isTestingConnection = false
        }
    }
}

struct NotificationSettingsView: View {
    @AppStorage("notificationsEnabled") private var notificationsEnabled = true
    @AppStorage("soundEnabled") private var soundEnabled = true
    @AppStorage("alertOnPriceMove") private var alertOnPriceMove = true
    @AppStorage("alertOnSignal") private var alertOnSignal = true

    var body: some View {
        Form {
            Section("Notifications") {
                Toggle("Enable notifications", isOn: $notificationsEnabled)
                Toggle("Sound effects", isOn: $soundEnabled)
                    .disabled(!notificationsEnabled)
            }

            Section("Alert Types") {
                Toggle("Price movement alerts", isOn: $alertOnPriceMove)
                    .disabled(!notificationsEnabled)
                Toggle("Trading signal alerts", isOn: $alertOnSignal)
                    .disabled(!notificationsEnabled)
            }
        }
        .formStyle(.grouped)
        .frame(width: 450, height: 250)
    }
}

#Preview {
    SettingsView()
        .environmentObject(AppState())
}
