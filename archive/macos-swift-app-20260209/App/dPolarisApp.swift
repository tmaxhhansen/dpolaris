//
//  dPolarisApp.swift
//  dPolaris
//
//  Main application entry point
//

import SwiftUI

@main
struct dPolarisApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .frame(minWidth: 1200, minHeight: 800)
        }
        .windowStyle(.hiddenTitleBar)
        .commands {
            CommandGroup(replacing: .newItem) { }

            CommandMenu("dPolaris") {
                Button("Scout for Opportunities") {
                    appState.performAction(.scout)
                }
                .keyboardShortcut("s", modifiers: [.command, .shift])

                Button("Refresh Data") {
                    appState.refreshAll()
                }
                .keyboardShortcut("r", modifiers: .command)

                Divider()

                Button("Start AI Daemon") {
                    appState.startDaemon()
                }

                Button("Stop AI Daemon") {
                    appState.stopDaemon()
                }
            }
        }

        // Menu Bar Extra
        MenuBarExtra {
            MenuBarView()
                .environmentObject(appState)
        } label: {
            HStack(spacing: 4) {
                Image(systemName: "star.fill")
                if let pnl = appState.portfolio?.dailyPnl {
                    Text(pnl >= 0 ? "+\(formatCurrency(pnl))" : formatCurrency(pnl))
                        .font(.caption)
                }
            }
        }
        .menuBarExtraStyle(.window)

        // Settings Window
        Settings {
            SettingsView()
                .environmentObject(appState)
        }
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }
}

// MARK: - App Delegate

class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        // Connect to backend on launch
        Task {
            _ = await APIService.shared.checkConnection()
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        // Stop the server if we started it
        ServerManager.shared.stopServer()
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return false // Keep running in menu bar
    }
}

// MARK: - App State

@MainActor
class AppState: ObservableObject {
    @Published var isConnected = false
    @Published var isDaemonRunning = false
    @Published var isServerRunning = false
    @Published var isStartingServer = false
    @Published var serverOutput: String = ""
    @Published var portfolio: Portfolio?
    @Published var aiStatus: AIStatus?
    @Published var recentAnalyses: [Analysis] = []
    @Published var predictions: [Prediction] = []
    @Published var memories: [AIMemory] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private var serverManager = ServerManager.shared

    enum Action {
        case scout
        case analyze(String)
        case predict(String)
    }

    init() {
        // Start polling for updates
        startPolling()

        // Observe ServerManager state
        observeServerManager()
    }

    private func observeServerManager() {
        // Sync server state periodically
        Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.isServerRunning = self?.serverManager.isServerRunning ?? false
                self?.isStartingServer = self?.serverManager.isStarting ?? false
                self?.serverOutput = self?.serverManager.serverOutput ?? ""
            }
        }
    }

    func refreshAll() {
        Task {
            await loadPortfolio()
            await loadAIStatus()
            await loadMemories()
        }
    }

    func loadPortfolio() async {
        guard isConnected else { return }
        do {
            portfolio = try await APIService.shared.getPortfolio()
        } catch {
            handleError(error)
        }
    }

    func loadAIStatus() async {
        guard isConnected else { return }
        do {
            aiStatus = try await APIService.shared.getAIStatus()
            isDaemonRunning = aiStatus?.daemonRunning ?? false
        } catch {
            handleError(error)
        }
    }

    func loadMemories() async {
        guard isConnected else { return }
        do {
            memories = try await APIService.shared.getMemories()
        } catch {
            handleError(error)
        }
    }

    func performAction(_ action: Action) {
        Task {
            guard isConnected else {
                errorMessage = "Not connected to server"
                return
            }
            isLoading = true
            defer { isLoading = false }

            do {
                switch action {
                case .scout:
                    _ = try await APIService.shared.scout()

                case .analyze(let symbol):
                    let analysis = try await APIService.shared.analyze(symbol: symbol)
                    recentAnalyses.insert(analysis, at: 0)

                case .predict(let symbol):
                    let prediction = try await APIService.shared.predict(symbol: symbol)
                    predictions.insert(prediction, at: 0)
                }
            } catch {
                handleError(error)
            }
        }
    }

    func startDaemon() {
        Task {
            // Always verify connection before deciding whether to start the server.
            if !isConnected {
                isConnected = await APIService.shared.checkConnection()
            }

            if !isConnected {
                await serverManager.startServer()

                // Wait for connection after server boot.
                var attempts = 0
                while attempts < 10 {
                    try? await Task.sleep(nanoseconds: 1_000_000_000) // 1 second
                    let connected = await APIService.shared.checkConnection()
                    if connected {
                        isConnected = true
                        break
                    }
                    attempts += 1
                }
            }

            guard isConnected else {
                errorMessage = "Could not connect to server"
                return
            }

            do {
                try await APIService.shared.startDaemon()
                isDaemonRunning = true
                await loadAIStatus()
            } catch {
                handleError(error)
            }
        }
    }

    func stopDaemon() {
        Task {
            if isConnected {
                do {
                    try await APIService.shared.stopDaemon()
                } catch {
                    // Ignore errors when stopping
                }
            }
            isDaemonRunning = false
        }
    }

    func startServer() {
        Task {
            await serverManager.startServer()
            isConnected = await APIService.shared.checkConnection()
            if isConnected {
                await loadAIStatus()
            }
        }
    }

    func stopServer() {
        serverManager.stopServer()
        isConnected = false
        isDaemonRunning = false
    }

    func toggleDaemon() {
        if isDaemonRunning {
            stopDaemon()
        } else {
            startDaemon()
        }
    }

    private func startPolling() {
        // Poll every 5 seconds for updates
        Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            guard let self = self, self.isConnected else { return }
            Task { @MainActor in
                await self.loadPortfolio()
            }
        }

        // Poll AI status every 30 seconds
        Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
            guard let self = self, self.isConnected else { return }
            Task { @MainActor in
                await self.loadAIStatus()
            }
        }
    }

    private func handleError(_ error: Error) {
        // Suppress verbose connection errors - the overlay handles this
        if let apiError = error as? APIError, apiError.isConnectionError {
            isConnected = false
            return
        }

        // Check for URLError connection issues
        if let urlError = error as? URLError {
            switch urlError.code {
            case .cannotConnectToHost,
                 .cannotFindHost,
                 .dnsLookupFailed,
                 .networkConnectionLost,
                 .notConnectedToInternet,
                 .timedOut,
                 .secureConnectionFailed:
                isConnected = false
                return
            default:
                break
            }
        }

        // Check error message for connection issues
        let errorString = error.localizedDescription
        if errorString.contains("Could not connect") ||
           errorString.contains("-1004") ||
           errorString.localizedCaseInsensitiveContains("connection reset by peer") ||
           errorString.localizedCaseInsensitiveContains("socket is not connected") {
            isConnected = false
            return
        }

        // Only show non-connection errors
        errorMessage = error.localizedDescription
    }
}
