//
//  PredictionsView.swift
//  dPolaris
//
//  View for ML predictions
//

import SwiftUI
import Charts

struct PredictionsView: View {
    @EnvironmentObject var appState: AppState
    @State private var symbolInput = ""
    @State private var isPredicting = false
    @State private var predictions: [Prediction] = []
    @State private var tradeSetups: [TradeSetup] = []
    @State private var models: [MLModel] = []

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text("ML Predictions")
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text("AI-powered price direction predictions")
                            .foregroundColor(.secondary)
                    }

                    Spacer()
                }
                .padding(.horizontal)

                // Prediction Input
                HStack {
                    TextField("Enter symbol (e.g., SPY)", text: $symbolInput)
                        .textFieldStyle(.roundedBorder)
                        .frame(maxWidth: 200)
                        .onSubmit { runPrediction() }

                    Button(action: runPrediction) {
                        if isPredicting {
                            ProgressView()
                                .scaleEffect(0.7)
                        } else {
                            Label("Predict", systemImage: "wand.and.stars")
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(symbolInput.isEmpty || isPredicting)

                    Spacer()

                    // Quick predict buttons
                    HStack {
                        ForEach(["SPY", "QQQ", "AAPL", "NVDA"], id: \.self) { symbol in
                            Button(symbol) {
                                symbolInput = symbol
                                runPrediction()
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
                .padding(.horizontal)

                // Available Models
                VStack(alignment: .leading, spacing: 12) {
                    Text("Available Models")
                        .font(.headline)

                    if models.isEmpty {
                        HStack {
                            Image(systemName: "exclamationmark.triangle")
                                .foregroundColor(.orange)
                            Text("No models trained. Train a model first in AI Manager.")
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(8)
                    } else {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(models) { model in
                                ModelSummaryCard(model: model)
                            }
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // Actionable Trade Setups
                VStack(alignment: .leading, spacing: 12) {
                    Text("Actionable Trade Setups")
                        .font(.headline)

                    if tradeSetups.isEmpty {
                        ContentUnavailableView(
                            "No Trade Setups Yet",
                            systemImage: "scope",
                            description: Text("Run a prediction to generate entry, stop, and target plans")
                        )
                        .frame(height: 180)
                    } else {
                        LazyVStack(spacing: 12) {
                            ForEach(tradeSetups.prefix(8)) { setup in
                                TradeSetupCard(setup: setup)
                            }
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // Predictions Grid
                VStack(alignment: .leading, spacing: 12) {
                    Text("Recent Predictions")
                        .font(.headline)

                    if predictions.isEmpty && appState.predictions.isEmpty {
                        ContentUnavailableView(
                            "No Predictions",
                            systemImage: "wand.and.stars",
                            description: Text("Run a prediction to see results here")
                        )
                        .frame(height: 200)
                    } else {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                            ForEach(predictions + appState.predictions) { prediction in
                                PredictionCard(prediction: prediction)
                            }
                        }
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
            await loadModels()
        }
    }

    private func runPrediction() {
        let symbol = symbolInput
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
        guard !symbol.isEmpty else { return }

        isPredicting = true

        Task {
            do {
                let prediction = try await APIService.shared.predict(symbol: symbol)
                predictions.insert(prediction, at: 0)

                do {
                    let setup = try await APIService.shared.getTradeSetup(symbol: symbol)
                    tradeSetups.removeAll { $0.symbol == setup.symbol }
                    tradeSetups.insert(setup, at: 0)
                } catch {
                    print("Trade setup error: \(error)")
                }

                symbolInput = ""
            } catch {
                print("Prediction error: \(error)")
            }
            isPredicting = false
        }
    }

    private func loadModels() async {
        do {
            models = try await APIService.shared.getModels()
        } catch {
            print("Models load error: \(error)")
        }
    }
}

// MARK: - Model Summary Card

struct ModelSummaryCard: View {
    let model: MLModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "cpu")
                    .foregroundColor(.blue)
                Text(model.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }

            if let metrics = model.metrics {
                HStack {
                    Text("Accuracy:")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(String(format: "%.1f", metrics.accuracyPercent))%")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(metrics.accuracyPercent > 55 ? .green : .orange)
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.textBackgroundColor))
        .cornerRadius(8)
    }
}

// MARK: - Prediction Card

struct PredictionCard: View {
    let prediction: Prediction

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text(prediction.symbol)
                    .font(.title2)
                    .fontWeight(.bold)

                Spacer()

                SignalBadge(signal: prediction.signal)
            }

            // Direction & Confidence
            HStack(spacing: 20) {
                VStack(alignment: .leading) {
                    Text("Direction")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    HStack {
                        Image(systemName: prediction.prediction == 1 ? "arrow.up.right" : "arrow.down.right")
                        Text(prediction.predictionLabel)
                            .fontWeight(.bold)
                    }
                    .foregroundColor(prediction.prediction == 1 ? .green : .red)
                }

                VStack(alignment: .leading) {
                    Text("Confidence")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(String(format: "%.1f", prediction.confidence * 100))%")
                        .font(.title3)
                        .fontWeight(.bold)
                }
            }

            // Probability Chart
            VStack(alignment: .leading, spacing: 4) {
                Text("Probability")
                    .font(.caption)
                    .foregroundColor(.secondary)

                GeometryReader { geometry in
                    HStack(spacing: 0) {
                        Rectangle()
                            .fill(Color.red.opacity(0.7))
                            .frame(width: geometry.size.width * CGFloat(prediction.probabilityDown))

                        Rectangle()
                            .fill(Color.green.opacity(0.7))
                            .frame(width: geometry.size.width * CGFloat(prediction.probabilityUp))
                    }
                    .cornerRadius(4)
                }
                .frame(height: 8)

                HStack {
                    Text("Down: \(String(format: "%.1f", prediction.probabilityDown * 100))%")
                        .font(.caption2)
                        .foregroundColor(.red)

                    Spacer()

                    Text("Up: \(String(format: "%.1f", prediction.probabilityUp * 100))%")
                        .font(.caption2)
                        .foregroundColor(.green)
                }
            }

            Divider()

            // Context
            VStack(alignment: .leading, spacing: 4) {
                Text("Context")
                    .font(.caption)
                    .foregroundColor(.secondary)

                HStack(spacing: 12) {
                    ContextItem(label: "RSI", value: String(format: "%.0f", prediction.context.rsi))
                    ContextItem(label: "Trend", value: prediction.context.trend)
                    ContextItem(label: "Vol", value: prediction.context.volatility)
                    ContextItem(label: "Mom", value: prediction.context.momentum)
                }
            }

            // Timestamp
            Text("Model: \(prediction.modelName)")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.windowBackgroundColor))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(prediction.prediction == 1 ? Color.green.opacity(0.3) : Color.red.opacity(0.3), lineWidth: 1)
        )
    }
}

// MARK: - Trade Setup Card

struct TradeSetupCard: View {
    let setup: TradeSetup

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(setup.symbol)
                        .font(.title3)
                        .fontWeight(.bold)
                    Text(setup.setupType.replacingOccurrences(of: "_", with: " ").capitalized)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                BiasBadge(bias: setup.bias)
            }

            HStack(spacing: 16) {
                MetricPill(label: "Confidence", value: "\(percent(setup.confidence))")
                MetricPill(label: "Prob Up", value: "\(percent(setup.probabilityUp))")
                MetricPill(label: "ATR%", value: "\(percent(setup.marketSnapshot.atrPercent))")
                MetricPill(label: "RSI", value: String(format: "%.1f", setup.marketSnapshot.rsi14))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Execution Plan")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("Entry trigger: \(price(setup.entry.trigger))")
                    .font(.caption)
                Text("Entry zone: \(price(setup.entry.zoneLow)) - \(price(setup.entry.zoneHigh))")
                    .font(.caption)
                Text("Stop: \(price(setup.risk.stopLoss))")
                    .font(.caption)
            }

            if !setup.targets.isEmpty {
                HStack(spacing: 16) {
                    ForEach(setup.targets) { target in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(target.label)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                            Text(price(target.price))
                                .font(.caption)
                                .fontWeight(.semibold)
                            if let r = target.rMultiple {
                                Text(String(format: "R %.2f", r))
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("Position Sizing")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(
                    "Max risk: \(currency(setup.risk.maxRiskDollars)) | Suggested shares: \(setup.risk.suggestedShares) | Position: \(percent(setup.risk.suggestedPositionPercent / 100))"
                )
                .font(.caption)
            }

            if !setup.reasons.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Why This Setup")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    ForEach(Array(setup.reasons.prefix(3).enumerated()), id: \.offset) { _, reason in
                        Text("• \(reason)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }

            if !setup.riskFlags.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Risk Flags")
                        .font(.caption)
                        .foregroundColor(.red)
                    ForEach(Array(setup.riskFlags.prefix(3).enumerated()), id: \.offset) { _, flag in
                        Text("• \(flag)")
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
            }

            Text(
                "Options: \(setup.optionsPlan.strategy) | \(setup.optionsPlan.dteRange) DTE | Delta \(setup.optionsPlan.deltaRange)"
            )
            .font(.caption2)
            .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.textBackgroundColor))
        .cornerRadius(10)
    }

    private func percent(_ value: Double) -> String {
        String(format: "%.1f%%", value * 100)
    }

    private func price(_ value: Double?) -> String {
        guard let value else { return "n/a" }
        return String(format: "$%.2f", value)
    }

    private func currency(_ value: Double?) -> String {
        guard let value else { return "n/a" }
        return String(format: "$%.0f", value)
    }
}

struct BiasBadge: View {
    let bias: String

    var body: some View {
        Text(bias)
            .font(.caption)
            .fontWeight(.bold)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(color.opacity(0.2))
            .foregroundColor(color)
            .cornerRadius(6)
    }

    private var color: Color {
        switch bias.uppercased() {
        case "LONG":
            return .green
        case "SHORT":
            return .red
        default:
            return .orange
        }
    }
}

struct MetricPill: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text(value)
                .font(.caption)
                .fontWeight(.semibold)
        }
    }
}

// MARK: - Signal Badge

struct SignalBadge: View {
    let signal: String

    var body: some View {
        Text(signal)
            .font(.caption)
            .fontWeight(.bold)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(backgroundColor)
            .foregroundColor(.white)
            .cornerRadius(6)
    }

    var backgroundColor: Color {
        switch signal {
        case "STRONG_BUY":
            return .green
        case "BUY":
            return .green.opacity(0.7)
        case "STRONG_SELL":
            return .red
        case "SELL":
            return .red.opacity(0.7)
        default:
            return .gray
        }
    }
}

// MARK: - Context Item

struct ContextItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text(value)
                .font(.caption)
                .fontWeight(.medium)
        }
    }
}

#Preview {
    PredictionsView()
        .environmentObject(AppState())
}
