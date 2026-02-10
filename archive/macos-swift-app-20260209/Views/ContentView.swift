//
//  ContentView.swift
//  dPolaris
//
//  Main content view with sidebar navigation
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedTab: NavigationItem = .dashboard
    @State private var isRetrying = false

    enum NavigationItem: String, CaseIterable, Identifiable {
        case dashboard = "Dashboard"
        case aiManager = "AI Manager"
        case analysis = "Analysis"
        case predictions = "Predictions"
        case journal = "Journal"
        case watchlist = "Watchlist"
        case chat = "Chat"
        case settings = "Settings"

        var id: String { rawValue }

        var icon: String {
            switch self {
            case .dashboard: return "square.grid.2x2"
            case .aiManager: return "brain"
            case .analysis: return "chart.bar.doc.horizontal"
            case .predictions: return "wand.and.stars"
            case .journal: return "book"
            case .watchlist: return "eye"
            case .chat: return "bubble.left.and.bubble.right"
            case .settings: return "gear"
            }
        }
    }

    var body: some View {
        ZStack {
            NavigationSplitView {
                // Sidebar
                List(selection: $selectedTab) {
                    Section("Overview") {
                        NavigationLink(value: NavigationItem.dashboard) {
                            Label("Dashboard", systemImage: "square.grid.2x2")
                        }

                        NavigationLink(value: NavigationItem.aiManager) {
                            Label("AI Manager", systemImage: "brain")
                        }
                    }

                    Section("Trading") {
                        NavigationLink(value: NavigationItem.analysis) {
                            Label("Analysis", systemImage: "chart.bar.doc.horizontal")
                        }

                        NavigationLink(value: NavigationItem.predictions) {
                            Label("Predictions", systemImage: "wand.and.stars")
                        }

                        NavigationLink(value: NavigationItem.journal) {
                            Label("Journal", systemImage: "book")
                        }

                        NavigationLink(value: NavigationItem.watchlist) {
                            Label("Watchlist", systemImage: "eye")
                        }
                    }

                    Section("Interact") {
                        NavigationLink(value: NavigationItem.chat) {
                            Label("Chat", systemImage: "bubble.left.and.bubble.right")
                        }
                    }

                    Section {
                        NavigationLink(value: NavigationItem.settings) {
                            Label("Settings", systemImage: "gear")
                        }
                    }
                }
                .listStyle(.sidebar)
                .navigationSplitViewColumnWidth(min: 200, ideal: 220, max: 250)

                // Connection status
                .safeAreaInset(edge: .bottom) {
                    HStack {
                        Circle()
                            .fill(appState.isConnected ? Color.green : Color.red)
                            .frame(width: 8, height: 8)

                        Text(appState.isConnected ? "Connected" : "Disconnected")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Spacer()

                        if appState.isDaemonRunning {
                            Image(systemName: "bolt.fill")
                                .foregroundColor(.green)
                                .font(.caption)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .background(.bar)
                }
            } detail: {
                // Main content
                switch selectedTab {
                case .dashboard:
                    DashboardView()
                case .aiManager:
                    AIManagerView()
                case .analysis:
                    AnalysisView()
                case .predictions:
                    PredictionsView()
                case .journal:
                    JournalView()
                case .watchlist:
                    WatchlistView()
                case .chat:
                    ChatView()
                case .settings:
                    SettingsView()
                }
            }

            // Connection required overlay
            if !appState.isConnected {
                ConnectionRequiredOverlay(isRetrying: $isRetrying) {
                    await retryConnection()
                }
            }
        }
        .task {
            // Check connection on appear
            let connected = await APIService.shared.checkConnection()
            appState.isConnected = connected

            if connected {
                appState.refreshAll()
            }
        }
    }

    private func retryConnection() async {
        isRetrying = true
        let connected = await APIService.shared.checkConnection()
        appState.isConnected = connected
        if connected {
            appState.refreshAll()
        }
        isRetrying = false
    }
}

// MARK: - Connection Required Overlay

struct ConnectionRequiredOverlay: View {
    @Binding var isRetrying: Bool
    @StateObject private var serverManager = ServerManager.shared
    @State private var showingSetup = false
    @State private var showingLogs = false
    let onRetry: () async -> Void

    var body: some View {
        ZStack {
            Color(.windowBackgroundColor)
                .opacity(0.95)

            VStack(spacing: 24) {
                Image(systemName: serverManager.isStarting ? "server.rack" : "wifi.exclamationmark")
                    .font(.system(size: 64))
                    .foregroundColor(serverManager.isStarting ? .blue : .orange)
                    .symbolEffect(.pulse, isActive: serverManager.isStarting)

                Text(serverManager.isStarting ? "Starting Server..." : "Backend Server Not Running")
                    .font(.title)
                    .fontWeight(.bold)

                if serverManager.isStarting {
                    VStack(spacing: 12) {
                        ProgressView()
                            .scaleEffect(1.2)

                        Text("Initializing dPolaris AI backend...")
                            .foregroundColor(.secondary)

                        if showingLogs && !serverManager.serverOutput.isEmpty {
                            ScrollView {
                                Text(serverManager.serverOutput)
                                    .font(.system(.caption, design: .monospaced))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .frame(height: 150)
                            .padding(8)
                            .background(Color(.textBackgroundColor))
                            .cornerRadius(8)
                        }

                        Button(showingLogs ? "Hide Logs" : "Show Logs") {
                            showingLogs.toggle()
                        }
                        .buttonStyle(.link)
                    }
                } else {
                    Text("dPolaris needs the AI backend server to work.")
                        .foregroundColor(.secondary)

                    if let error = serverManager.serverError {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding(8)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(6)
                    }

                    HStack(spacing: 16) {
                        Button(action: {
                            Task {
                                await serverManager.startServer()
                                // Wait a bit then retry connection
                                try? await Task.sleep(nanoseconds: 1_000_000_000)
                                await onRetry()
                            }
                        }) {
                            HStack {
                                Image(systemName: "play.fill")
                                Text("Start Server")
                            }
                            .frame(width: 140)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.green)

                        Button(action: {
                            Task {
                                await onRetry()
                            }
                        }) {
                            HStack {
                                if isRetrying {
                                    ProgressView()
                                        .scaleEffect(0.7)
                                }
                                Text("Retry")
                            }
                            .frame(width: 100)
                        }
                        .buttonStyle(.bordered)
                        .disabled(isRetrying)
                    }

                    // Setup section for first-time users
                    VStack(spacing: 12) {
                        Divider()
                            .padding(.vertical, 8)

                        Text("First time setup?")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Button(action: {
                            showingSetup = true
                        }) {
                            HStack {
                                Image(systemName: "wrench.and.screwdriver")
                                Text("Setup Environment")
                            }
                        }
                        .buttonStyle(.bordered)
                    }
                }

                Text("Server endpoint: http://127.0.0.1:8420")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(40)
            .frame(maxWidth: 500)
        }
        .ignoresSafeArea()
        .sheet(isPresented: $showingSetup) {
            SetupSheet(onComplete: {
                showingSetup = false
                Task {
                    await serverManager.startServer()
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                    await onRetry()
                }
            })
        }
        .onChange(of: serverManager.isServerRunning) { _, isRunning in
            if isRunning {
                Task {
                    await onRetry()
                }
            }
        }
    }
}

// MARK: - Setup Sheet

struct SetupSheet: View {
    @StateObject private var serverManager = ServerManager.shared
    @Environment(\.dismiss) var dismiss
    @State private var isSettingUp = false
    let onComplete: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Environment Setup")
                    .font(.title2)
                    .fontWeight(.bold)

                Spacer()

                Button("Cancel") {
                    dismiss()
                }
                .buttonStyle(.bordered)
                .disabled(isSettingUp)
            }
            .padding()

            Divider()

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Info
                    VStack(alignment: .leading, spacing: 8) {
                        Label("What this does:", systemImage: "info.circle")
                            .font(.headline)

                        Text("1. Creates a Python virtual environment")
                        Text("2. Installs required dependencies (FastAPI, Anthropic, etc.)")
                        Text("3. Prepares the dPolaris AI backend for use")
                    }
                    .padding()
                    .background(Color(.controlBackgroundColor))
                    .cornerRadius(8)

                    // Requirements
                    VStack(alignment: .leading, spacing: 8) {
                        Label("Requirements:", systemImage: "checklist")
                            .font(.headline)

                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Python 3.9 or later installed")
                        }

                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Internet connection for downloading packages")
                        }
                    }
                    .padding()
                    .background(Color(.controlBackgroundColor))
                    .cornerRadius(8)

                    // Output log
                    if !serverManager.serverOutput.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Label("Progress:", systemImage: "terminal")
                                .font(.headline)

                            ScrollView {
                                Text(serverManager.serverOutput)
                                    .font(.system(.caption, design: .monospaced))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .frame(height: 200)
                            .padding(8)
                            .background(Color(.textBackgroundColor))
                            .cornerRadius(6)
                        }
                    }

                    if let error = serverManager.serverError {
                        Text(error)
                            .foregroundColor(.red)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                    }
                }
                .padding()
            }

            Divider()

            // Footer
            HStack {
                Spacer()

                if isSettingUp {
                    ProgressView()
                        .padding(.trailing, 8)
                    Text("Setting up...")
                } else {
                    Button("Run Setup") {
                        isSettingUp = true
                        Task {
                            let success = await serverManager.setupEnvironment()
                            isSettingUp = false
                            if success {
                                onComplete()
                            }
                        }
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
            .padding()
        }
        .frame(width: 550, height: 500)
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
