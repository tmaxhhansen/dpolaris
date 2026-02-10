//
//  AnalysisView.swift
//  dPolaris
//
//  View for running and viewing stock analyses
//

import SwiftUI

struct AnalysisView: View {
    @EnvironmentObject var appState: AppState
    @State private var symbolInput = ""
    @State private var isAnalyzing = false
    @State private var analyses: [Analysis] = []
    @State private var selectedAnalysis: Analysis?

    var body: some View {
        HSplitView {
            // Left panel - Analysis list
            VStack(spacing: 0) {
                // Search/Analyze bar
                HStack {
                    TextField("Enter symbol (e.g., AAPL)", text: $symbolInput)
                        .textFieldStyle(.roundedBorder)
                        .onSubmit { runAnalysis() }

                    Button(action: runAnalysis) {
                        if isAnalyzing {
                            ProgressView()
                                .scaleEffect(0.7)
                        } else {
                            Image(systemName: "magnifyingglass")
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(symbolInput.isEmpty || isAnalyzing)
                }
                .padding()

                Divider()

                // Analysis list
                if analyses.isEmpty && appState.recentAnalyses.isEmpty {
                    ContentUnavailableView(
                        "No Analyses Yet",
                        systemImage: "chart.bar.doc.horizontal",
                        description: Text("Enter a symbol above to start analyzing")
                    )
                } else {
                    List(selection: $selectedAnalysis) {
                        ForEach(analyses + appState.recentAnalyses) { analysis in
                            AnalysisListItem(analysis: analysis)
                                .tag(analysis)
                        }
                    }
                    .listStyle(.sidebar)
                }
            }
            .frame(minWidth: 250, maxWidth: 350)

            // Right panel - Analysis detail
            if let analysis = selectedAnalysis {
                AnalysisDetailView(analysis: analysis)
            } else {
                ContentUnavailableView(
                    "Select an Analysis",
                    systemImage: "doc.text.magnifyingglass",
                    description: Text("Choose an analysis from the list or run a new one")
                )
            }
        }
    }

    private func runAnalysis() {
        guard !symbolInput.isEmpty else { return }

        isAnalyzing = true
        let symbol = symbolInput.uppercased()

        Task {
            do {
                let analysis = try await APIService.shared.analyze(symbol: symbol)
                analyses.insert(analysis, at: 0)
                selectedAnalysis = analysis
                symbolInput = ""
            } catch {
                print("Analysis error: \(error)")
            }
            isAnalyzing = false
        }
    }
}

// MARK: - Analysis List Item

struct AnalysisListItem: View {
    let analysis: Analysis

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(analysis.symbol)
                    .font(.headline)

                Spacer()

                if let direction = analysis.direction {
                    DirectionBadge(direction: direction)
                }
            }

            Text(formatDate(analysis.timestamp))
                .font(.caption)
                .foregroundColor(.secondary)

            if let score = analysis.convictionScore {
                HStack(spacing: 4) {
                    Text("Conviction:")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(Int(score))/10")
                        .font(.caption)
                        .fontWeight(.medium)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private func formatDate(_ timestamp: String) -> String {
        // Simple date formatting
        let components = timestamp.prefix(16).split(separator: "T")
        if components.count == 2 {
            return "\(components[0]) \(components[1])"
        }
        return timestamp
    }
}

// MARK: - Direction Badge

struct DirectionBadge: View {
    let direction: String

    var body: some View {
        Text(direction)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(backgroundColor)
            .foregroundColor(.white)
            .cornerRadius(4)
    }

    var backgroundColor: Color {
        switch direction.lowercased() {
        case "bullish", "up", "buy":
            return .green
        case "bearish", "down", "sell":
            return .red
        default:
            return .gray
        }
    }
}

// MARK: - Analysis Detail View

struct AnalysisDetailView: View {
    let analysis: Analysis

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text(analysis.symbol)
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text("Analysis from \(formatDate(analysis.timestamp))")
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    VStack(alignment: .trailing) {
                        if let direction = analysis.direction {
                            DirectionBadge(direction: direction)
                        }

                        if let score = analysis.convictionScore {
                            HStack {
                                Text("Conviction:")
                                Text("\(Int(score))/10")
                                    .fontWeight(.bold)
                            }
                            .font(.caption)
                        }
                    }
                }

                Divider()

                // Analysis content (Markdown)
                Text(LocalizedStringKey(analysis.analysis))
                    .textSelection(.enabled)
            }
            .padding()
        }
    }

    private func formatDate(_ timestamp: String) -> String {
        let components = timestamp.prefix(16).split(separator: "T")
        if components.count == 2 {
            return "\(components[0]) at \(components[1])"
        }
        return timestamp
    }
}

#Preview {
    AnalysisView()
        .environmentObject(AppState())
}
