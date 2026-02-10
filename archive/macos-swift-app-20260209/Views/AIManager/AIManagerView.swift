//
//  AIManagerView.swift
//  dPolaris
//
//  View to manage and monitor the AI - see learning progress, memories, models
//

import SwiftUI
import Charts
#if os(macOS)
import AppKit
#endif

struct AIManagerView: View {
    enum TrainingMode: String, CaseIterable, Identifiable {
        case stable = "Stable (XGBoost)"
        case deepLearning = "Deep Learning"

        var id: String { rawValue }
    }

    enum DeepModelType: String, CaseIterable, Identifiable {
        case lstm = "lstm"
        case transformer = "transformer"

        var id: String { rawValue }

        var label: String { rawValue.uppercased() }
    }

    @EnvironmentObject var appState: AppState
    @State private var selectedCategory: String? = nil
    @State private var memories: [AIMemory] = []
    @State private var models: [MLModel] = []
    @State private var tradeStats: TradeStats?
    @State private var isTrainingModel = false
    @State private var trainSymbol = "SPY"
    @State private var trainingMode: TrainingMode = .stable
    @State private var deepModelType: DeepModelType = .lstm
    @State private var trainingStatusMessage: String?
    @State private var trainingErrorMessage: String?
    @State private var trainingLogs: [String] = []
    @State private var deepLearningEnabled = true
    @State private var deepLearningReason: String?

    let categories = [
        ("All", nil as String?),
        ("Trading Style", "trading_style"),
        ("Risk Tolerance", "risk_tolerance"),
        ("Success Patterns", "success_pattern"),
        ("Mistake Patterns", "mistake_pattern"),
        ("Market Insights", "market_insight"),
        ("Symbol Knowledge", "symbol_knowledge"),
        ("Strategy Performance", "strategy_performance"),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text("AI Manager")
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text("Monitor and control your AI assistant")
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    // Server & Daemon Control
                    VStack(alignment: .trailing, spacing: 8) {
                        // Server Status
                        HStack {
                            Circle()
                                .fill(appState.isConnected ? Color.green : (appState.isStartingServer ? Color.yellow : Color.red))
                                .frame(width: 10, height: 10)
                            Text(appState.isConnected ? "Server Connected" : (appState.isStartingServer ? "Starting..." : "Server Stopped"))
                                .font(.caption)
                        }

                        // Daemon Status
                        HStack {
                            Circle()
                                .fill(appState.isDaemonRunning ? Color.green : Color.gray)
                                .frame(width: 10, height: 10)
                            Text(appState.isDaemonRunning ? "Daemon Running" : "Daemon Stopped")
                                .font(.caption)
                        }

                        HStack {
                            if !appState.isConnected {
                                Button(action: { appState.startServer() }) {
                                    Label("Start Server", systemImage: "power")
                                }
                                .disabled(appState.isStartingServer)
                            } else {
                                Button(action: { appState.startDaemon() }) {
                                    Label("Start", systemImage: "play.fill")
                                }
                                .disabled(appState.isDaemonRunning)

                                Button(action: { appState.stopDaemon() }) {
                                    Label("Stop", systemImage: "stop.fill")
                                }
                                .disabled(!appState.isDaemonRunning)

                                Button(action: { appState.stopServer() }) {
                                    Label("Stop Server", systemImage: "power")
                                }
                            }
                        }
                        .buttonStyle(.bordered)
                    }
                }
                .padding(.horizontal)

                // AI Stats Overview
                HStack(spacing: 16) {
                    AIStatCard(
                        title: "Total Memories",
                        value: "\(appState.aiStatus?.totalMemories ?? memories.count)",
                        icon: "brain.head.profile",
                        color: .purple
                    )

                    AIStatCard(
                        title: "Trades Analyzed",
                        value: "\(tradeStats?.totalTrades ?? 0)",
                        icon: "doc.text.magnifyingglass",
                        color: .blue
                    )

                    AIStatCard(
                        title: "Win Rate",
                        value: String(format: "%.1f%%", tradeStats?.winRate ?? 0.0),
                        icon: "chart.pie.fill",
                        color: .green
                    )

                    AIStatCard(
                        title: "ML Models",
                        value: "\(models.count)",
                        icon: "cpu",
                        color: .orange
                    )
                }
                .padding(.horizontal)

                // Memory Distribution Chart
                VStack(alignment: .leading) {
                    Text("Memory Distribution")
                        .font(.headline)

                    let memoryCounts = Dictionary(grouping: memories, by: { $0.category })
                        .mapValues { $0.count }

                    if memoryCounts.isEmpty {
                        ContentUnavailableView(
                            "No Memory Data",
                            systemImage: "brain",
                            description: Text("Memories will appear here as the AI learns from your trades and notes.")
                        )
                        .frame(height: 180)
                    } else {
                        Chart {
                            ForEach(Array(memoryCounts.keys.sorted()), id: \.self) { category in
                                BarMark(
                                    x: .value("Count", memoryCounts[category] ?? 0),
                                    y: .value("Category", formatCategory(category))
                                )
                                .foregroundStyle(colorForCategory(category))
                            }
                        }
                        .frame(height: 200)
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // ML Models Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("ML Models")
                            .font(.headline)

                        Spacer()

                        HStack {
                            TextField("Symbol", text: $trainSymbol)
                                .textFieldStyle(.roundedBorder)
                                .frame(width: 80)
                                .textCase(.uppercase)

                            Picker("Mode", selection: $trainingMode) {
                                ForEach(TrainingMode.allCases) { mode in
                                    Text(mode.rawValue).tag(mode)
                                }
                            }
                            .pickerStyle(.menu)
                            .frame(width: 170)

                            if trainingMode == .deepLearning {
                                Picker("Model", selection: $deepModelType) {
                                    ForEach(DeepModelType.allCases) { type in
                                        Text(type.label).tag(type)
                                    }
                                }
                                .pickerStyle(.menu)
                                .frame(width: 130)
                            }

                            Button(action: trainModel) {
                                if isTrainingModel {
                                    ProgressView()
                                        .scaleEffect(0.7)
                                } else {
                                    Label(trainingMode == .stable ? "Train Stable" : "Train Deep", systemImage: "hammer.fill")
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(
                                isTrainingModel ||
                                !appState.isConnected ||
                                sanitizedTrainSymbol.isEmpty ||
                                (trainingMode == .deepLearning && !deepLearningEnabled)
                            )
                        }
                    }

                    if trainingMode == .deepLearning {
                        Label(
                            deepLearningReason ?? "Deep Learning mode can run longer and may be less stable on some macOS/PyTorch builds.",
                            systemImage: deepLearningEnabled ? "exclamationmark.triangle" : "xmark.octagon"
                        )
                            .font(.caption)
                            .foregroundColor(deepLearningEnabled ? .orange : .red)
                    }

                    if let trainingStatusMessage {
                        Label(trainingStatusMessage, systemImage: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundColor(.green)
                    }

                    if let trainingErrorMessage {
                        Label(trainingErrorMessage, systemImage: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundColor(.red)
                    }

                    if !trainingLogs.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text("Training Logs")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Button("Copy") {
                                    copyTrainingLogsToClipboard()
                                }
                                .font(.caption)
                                .buttonStyle(.borderless)
                            }

                            ScrollView {
                                Text(displayedTrainingLogsText)
                                    .textSelection(.enabled)
                                    .font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(.secondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .frame(height: 130)
                            .padding(8)
                            .background(Color(.textBackgroundColor).opacity(0.4))
                            .cornerRadius(8)
                        }
                    }

                    if models.isEmpty {
                        ContentUnavailableView(
                            "No Models Trained",
                            systemImage: "cpu",
                            description: Text("Train a model to start making predictions")
                        )
                        .frame(height: 100)
                    } else {
                        ForEach(models) { model in
                            ModelCard(model: model)
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // Memory Browser
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("AI Memory")
                            .font(.headline)

                        Spacer()

                        Picker("Category", selection: $selectedCategory) {
                            ForEach(categories, id: \.1) { name, value in
                                Text(name).tag(value)
                            }
                        }
                        .pickerStyle(.menu)
                        .frame(width: 180)
                    }

                    let filteredMemories = selectedCategory == nil
                        ? memories
                        : memories.filter { $0.category == selectedCategory }

                    if filteredMemories.isEmpty {
                        ContentUnavailableView(
                            "No Memories",
                            systemImage: "brain",
                            description: Text("The AI will learn as you trade")
                        )
                        .frame(height: 150)
                    } else {
                        LazyVStack(spacing: 8) {
                            ForEach(filteredMemories.prefix(20)) { memory in
                                MemoryCard(memory: memory)
                            }
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // Learning Insights
                VStack(alignment: .leading, spacing: 12) {
                    Text("Learning Insights")
                        .font(.headline)

                    if let stats = tradeStats, stats.totalTrades > 0 {
                        VStack(alignment: .leading, spacing: 16) {
                            InsightRow(
                                icon: "arrow.up.circle.fill",
                                iconColor: .green,
                                title: "Best Performance",
                                description: "Average winning trade: \(formatCurrency(stats.avgWin ?? 0))"
                            )

                            InsightRow(
                                icon: "arrow.down.circle.fill",
                                iconColor: .red,
                                title: "Area to Improve",
                                description: "Average losing trade: \(formatCurrency(stats.avgLoss ?? 0))"
                            )

                            if let pf = stats.profitFactor {
                                InsightRow(
                                    icon: "scalemass.fill",
                                    iconColor: pf > 1 ? .green : .orange,
                                    title: "Profit Factor",
                                    description: String(format: "%.2f - %@", pf, pf > 1 ? "Profitable" : "Needs work")
                                )
                            }
                        }
                    } else {
                        ContentUnavailableView(
                            "Not Enough Data",
                            systemImage: "chart.bar.xaxis",
                            description: Text("Log more trades to see insights")
                        )
                        .frame(height: 100)
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .task {
            await loadData()
        }
        .onChange(of: selectedCategory) { _, _ in
            Task {
                await loadMemories()
            }
        }
        .onChange(of: trainingMode) { _, mode in
            guard mode == .deepLearning else { return }
            Task {
                await loadDeepLearningStatus()
            }
        }
    }

    private func loadData() async {
        await loadMemories()
        await loadDeepLearningStatus()

        do {
            models = try await APIService.shared.getModels()
            tradeStats = try await APIService.shared.getTradeStats()
        } catch {
            trainingErrorMessage = "Failed to load AI manager data: \(error.localizedDescription)"
        }
    }

    private func loadMemories() async {
        do {
            memories = try await APIService.shared.getMemories(category: selectedCategory, limit: 100)
            appState.memories = memories
        } catch {
            trainingErrorMessage = "Failed to load memories: \(error.localizedDescription)"
        }
    }

    private func trainModel() {
        trainingErrorMessage = nil
        trainingStatusMessage = nil
        trainingLogs = []

        guard appState.isConnected else {
            trainingErrorMessage = "Server is not connected. Start the backend first."
            return
        }

        let symbol = sanitizedTrainSymbol
        guard !symbol.isEmpty else {
            trainingErrorMessage = "Enter a valid ticker symbol."
            return
        }

        if trainingMode == .deepLearning && !deepLearningEnabled {
            trainingErrorMessage = deepLearningReason ?? "Deep learning is currently unavailable on this backend runtime."
            return
        }

        isTrainingModel = true

        Task { @MainActor in
            do {
                let result: String
                switch trainingMode {
                case .stable:
                    result = try await APIService.shared.trainStableModel(symbol: symbol)
                case .deepLearning:
                    result = try await APIService.shared.trainDeepLearningModel(
                        symbol: symbol,
                        modelType: deepModelType.rawValue,
                        onStatus: { status in
                            trainingStatusMessage = status
                        },
                        onLog: { line in
                            trainingLogs.append(line)
                        }
                    )
                }
                trainingStatusMessage = result

                do {
                    models = try await APIService.shared.getModels()
                } catch {
                    trainingErrorMessage = "Model trained, but model list refresh failed: \(error.localizedDescription)"
                }
            } catch {
                if trainingMode == .deepLearning &&
                    error.localizedDescription.localizedCaseInsensitiveContains("timed out") {
                    trainingErrorMessage = "Deep learning request timed out. Training may still be running; wait a bit and refresh models."
                } else {
                    trainingErrorMessage = "Training failed for \(symbol): \(error.localizedDescription)"
                }
            }
            isTrainingModel = false
        }
    }

    private func loadDeepLearningStatus() async {
        do {
            let status = try await APIService.shared.getDeepLearningStatus()
            let enabled = status.deepLearningEnabled ?? true
            deepLearningEnabled = enabled

            if enabled {
                deepLearningReason = "Deep Learning mode can run longer and may be less stable on some macOS/PyTorch builds."
            } else {
                if let reason = status.deepLearningReason, !reason.isEmpty {
                    deepLearningReason = "Deep Learning unavailable: \(reason)"
                } else if let pythonVersion = status.pythonVersion, !pythonVersion.isEmpty {
                    deepLearningReason = "Deep Learning unavailable on Python \(pythonVersion). Use Python 3.11/3.12 for stability."
                } else {
                    deepLearningReason = "Deep Learning unavailable on this backend runtime."
                }
            }
        } catch {
            deepLearningEnabled = true
            deepLearningReason = "Deep Learning mode can run longer and may be less stable on some macOS/PyTorch builds."
        }
    }

    private var sanitizedTrainSymbol: String {
        trainSymbol
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
            .filter { $0.isLetter || $0.isNumber || $0 == "." || $0 == "-" }
    }

    private var displayedTrainingLogsText: String {
        trainingLogs.suffix(200).joined(separator: "\n")
    }

    private func copyTrainingLogsToClipboard() {
        let logs = displayedTrainingLogsText
        guard !logs.isEmpty else { return }
        #if os(macOS)
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(logs, forType: .string)
        #endif
    }

    private func formatCategory(_ category: String) -> String {
        category.replacingOccurrences(of: "_", with: " ").capitalized
    }

    private func colorForCategory(_ category: String) -> Color {
        switch category {
        case "success_pattern": return .green
        case "mistake_pattern": return .red
        case "risk_tolerance": return .orange
        case "trading_style": return .blue
        case "market_insight": return .purple
        default: return .gray
        }
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }
}

// MARK: - AI Stat Card

struct AIStatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)

            Text(value)
                .font(.title)
                .fontWeight(.bold)

            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.windowBackgroundColor))
        .cornerRadius(12)
    }
}

// MARK: - Memory Card

struct MemoryCard: View {
    let memory: AIMemory

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: memory.categoryIcon)
                .foregroundColor(Color(memory.categoryColor))
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(memory.category.replacingOccurrences(of: "_", with: " ").capitalized)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Spacer()

                    // Importance indicator
                    HStack(spacing: 2) {
                        ForEach(0..<5) { i in
                            Circle()
                                .fill(Double(i) < memory.importance * 5 ? Color.yellow : Color.gray.opacity(0.3))
                                .frame(width: 6, height: 6)
                        }
                    }
                }

                Text(memory.content)
                    .font(.body)
                    .lineLimit(3)
            }
        }
        .padding()
        .background(Color(.textBackgroundColor))
        .cornerRadius(8)
    }
}

// MARK: - Model Card

struct ModelCard: View {
    let model: MLModel

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(model.name)
                    .font(.headline)

                Text("Type: \(model.modelType ?? "unknown") | Target: \(model.target ?? "n/a")")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            if let metrics = model.metrics {
                VStack(alignment: .trailing, spacing: 4) {
                    Text("\(String(format: "%.1f", metrics.accuracyPercent))%")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(metrics.accuracyPercent > 55 ? .green : .orange)

                    Text("Accuracy")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.textBackgroundColor))
        .cornerRadius(8)
    }
}

// MARK: - Insight Row

struct InsightRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let description: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(iconColor)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)

                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
    }
}

#Preview {
    AIManagerView()
        .environmentObject(AppState())
}
