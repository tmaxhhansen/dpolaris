//
//  JournalView.swift
//  dPolaris
//
//  Trade journal for logging and reviewing trades
//

import SwiftUI
import Charts

struct JournalView: View {
    @EnvironmentObject var appState: AppState
    @State private var trades: [Trade] = []
    @State private var stats: TradeStats?
    @State private var selectedTrade: Trade?
    @State private var showingAddTrade = false
    @State private var filterDateRange: DateRange = .all
    @State private var filterSymbol = ""

    enum DateRange: String, CaseIterable {
        case today = "Today"
        case week = "This Week"
        case month = "This Month"
        case year = "This Year"
        case all = "All Time"
    }

    var filteredTrades: [Trade] {
        trades.filter { trade in
            let symbolMatch = filterSymbol.isEmpty || trade.symbol.localizedCaseInsensitiveContains(filterSymbol)
            return symbolMatch
        }
    }

    var body: some View {
        HSplitView {
            // Left panel - Trade list
            VStack(spacing: 0) {
                // Header with filters
                VStack(spacing: 12) {
                    HStack {
                        Text("Trade Journal")
                            .font(.title2)
                            .fontWeight(.bold)

                        Spacer()

                        Button(action: { showingAddTrade = true }) {
                            Label("Log Trade", systemImage: "plus")
                        }
                        .buttonStyle(.borderedProminent)
                    }

                    HStack {
                        TextField("Filter by symbol", text: $filterSymbol)
                            .textFieldStyle(.roundedBorder)
                            .frame(maxWidth: 150)

                        Picker("Period", selection: $filterDateRange) {
                            ForEach(DateRange.allCases, id: \.self) { range in
                                Text(range.rawValue).tag(range)
                            }
                        }
                        .pickerStyle(.menu)

                        Spacer()
                    }
                }
                .padding()

                Divider()

                // Trade list
                if filteredTrades.isEmpty {
                    ContentUnavailableView(
                        "No Trades",
                        systemImage: "doc.text",
                        description: Text("Log your first trade to start tracking")
                    )
                } else {
                    List(selection: $selectedTrade) {
                        ForEach(filteredTrades) { trade in
                            TradeListItem(trade: trade)
                                .tag(trade)
                        }
                    }
                    .listStyle(.sidebar)
                }
            }
            .frame(minWidth: 300, maxWidth: 400)

            // Right panel - Trade detail or stats
            if let trade = selectedTrade {
                TradeDetailView(trade: trade)
            } else {
                // Show stats overview when no trade selected
                JournalStatsView(stats: stats, trades: filteredTrades)
            }
        }
        .sheet(isPresented: $showingAddTrade) {
            AddTradeSheet(onSave: { newTrade in
                trades.insert(newTrade, at: 0)
            })
        }
        .task {
            await loadData()
        }
    }

    private func loadData() async {
        do {
            trades = try await APIService.shared.getJournal(limit: 200)
            stats = try await APIService.shared.getTradeStats()
        } catch {
            print("Journal load error: \(error)")
        }
    }
}

// MARK: - Trade List Item

struct TradeListItem: View {
    let trade: Trade

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(trade.symbol)
                    .font(.headline)

                Spacer()

                TradeSideBadge(side: trade.side)
            }

            HStack {
                Text("\(Int(trade.quantity)) @ $\(String(format: "%.2f", trade.entryPrice))")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                if let pnl = trade.pnl {
                    Text("\(pnl >= 0 ? "+" : "")$\(String(format: "%.2f", pnl))")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(pnl >= 0 ? .green : .red)
                }
            }

            Text(formatDate(trade.entryTime))
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }

    private func formatDate(_ timestamp: String) -> String {
        let components = timestamp.prefix(10)
        return String(components)
    }
}

// MARK: - Trade Side Badge

struct TradeSideBadge: View {
    let side: String

    var body: some View {
        Text(side.uppercased())
            .font(.caption2)
            .fontWeight(.bold)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(side.lowercased() == "long" ? Color.green : Color.red)
            .foregroundColor(.white)
            .cornerRadius(4)
    }
}

// MARK: - Trade Detail View

struct TradeDetailView: View {
    let trade: Trade

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text(trade.symbol)
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        HStack {
                            TradeSideBadge(side: trade.side)
                            if trade.status == "closed" {
                                Text("CLOSED")
                                    .font(.caption2)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.gray)
                                    .foregroundColor(.white)
                                    .cornerRadius(4)
                            }
                        }
                    }

                    Spacer()

                    if let pnl = trade.pnl {
                        VStack(alignment: .trailing) {
                            Text("\(pnl >= 0 ? "+" : "")$\(String(format: "%.2f", pnl))")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(pnl >= 0 ? .green : .red)

                            if let percent = trade.pnlPercent {
                                Text("\(percent >= 0 ? "+" : "")\(String(format: "%.2f", percent))%")
                                    .font(.subheadline)
                                    .foregroundColor(percent >= 0 ? .green : .red)
                            }
                        }
                    }
                }

                Divider()

                // Trade Details
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                    DetailItem(label: "Entry Price", value: "$\(String(format: "%.2f", trade.entryPrice))")
                    DetailItem(label: "Quantity", value: "\(Int(trade.quantity))")

                    if let exitPrice = trade.exitPrice {
                        DetailItem(label: "Exit Price", value: "$\(String(format: "%.2f", exitPrice))")
                    }

                    DetailItem(label: "Entry Time", value: formatDateTime(trade.entryTime))

                    if let exitTime = trade.exitTime {
                        DetailItem(label: "Exit Time", value: formatDateTime(exitTime))
                    }

                    if let strategy = trade.strategy {
                        DetailItem(label: "Strategy", value: strategy)
                    }
                }

                // Notes
                if let notes = trade.notes, !notes.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Notes")
                            .font(.headline)

                        Text(notes)
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(.textBackgroundColor))
                            .cornerRadius(8)
                    }
                }

                // AI Insights placeholder
                VStack(alignment: .leading, spacing: 8) {
                    Text("AI Analysis")
                        .font(.headline)

                    VStack(alignment: .leading, spacing: 8) {
                        if let pnl = trade.pnl {
                            HStack {
                                Image(systemName: pnl >= 0 ? "checkmark.circle.fill" : "xmark.circle.fill")
                                    .foregroundColor(pnl >= 0 ? .green : .red)
                                Text(pnl >= 0 ? "Winning trade" : "Losing trade")
                            }
                        }

                        Text("The AI will analyze this trade and provide insights after accumulating more trading data.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.textBackgroundColor))
                    .cornerRadius(8)
                }
            }
            .padding()
        }
    }

    private func formatDateTime(_ timestamp: String) -> String {
        let components = timestamp.prefix(16).split(separator: "T")
        if components.count == 2 {
            return "\(components[0]) \(components[1])"
        }
        return timestamp
    }
}

// MARK: - Detail Item

struct DetailItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.body)
                .fontWeight(.medium)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Journal Stats View

struct JournalStatsView: View {
    let stats: TradeStats?
    let trades: [Trade]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Stats Overview
                if let stats = stats {
                    HStack(spacing: 16) {
                        JournalStatCard(
                            title: "Total Trades",
                            value: "\(stats.totalTrades)",
                            color: .blue
                        )

                        JournalStatCard(
                            title: "Win Rate",
                            value: String(format: "%.1f%%", stats.winRate),
                            color: stats.winRate >= 50 ? .green : .orange
                        )

                        JournalStatCard(
                            title: "Total P/L",
                            value: formatCurrency(stats.totalPnl ?? 0),
                            color: (stats.totalPnl ?? 0) >= 0 ? .green : .red
                        )

                        if let pf = stats.profitFactor {
                            JournalStatCard(
                                title: "Profit Factor",
                                value: String(format: "%.2f", pf),
                                color: pf >= 1 ? .green : .orange
                            )
                        }
                    }
                }

                // P/L Chart
                VStack(alignment: .leading, spacing: 12) {
                    Text("P/L Distribution")
                        .font(.headline)

                    let pnlValues = trades.compactMap { $0.pnl }

                    if pnlValues.isEmpty {
                        ContentUnavailableView(
                            "No P/L Data",
                            systemImage: "chart.bar",
                            description: Text("Close some trades to see P/L distribution")
                        )
                        .frame(height: 200)
                    } else {
                        Chart {
                            ForEach(Array(pnlValues.enumerated()), id: \.offset) { index, pnl in
                                BarMark(
                                    x: .value("Trade", index),
                                    y: .value("P/L", pnl)
                                )
                                .foregroundStyle(pnl >= 0 ? Color.green : Color.red)
                            }
                        }
                        .frame(height: 200)
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)

                // Win/Loss Breakdown
                if let stats = stats, stats.totalTrades > 0 {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Performance Breakdown")
                            .font(.headline)

                        HStack(spacing: 20) {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Circle()
                                        .fill(Color.green)
                                        .frame(width: 12, height: 12)
                                    Text("Wins: \(stats.winningTrades ?? 0)")
                                }
                                Text("Avg Win: \(formatCurrency(stats.avgWin ?? 0))")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Circle()
                                        .fill(Color.red)
                                        .frame(width: 12, height: 12)
                                    Text("Losses: \(stats.losingTrades ?? 0)")
                                }
                                Text("Avg Loss: \(formatCurrency(stats.avgLoss ?? 0))")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()
                        }

                        // Win rate visual
                        GeometryReader { geometry in
                            HStack(spacing: 0) {
                                Rectangle()
                                    .fill(Color.green)
                                    .frame(width: geometry.size.width * CGFloat(stats.winRate / 100))

                                Rectangle()
                                    .fill(Color.red)
                            }
                            .cornerRadius(4)
                        }
                        .frame(height: 20)
                    }
                    .padding()
                    .background(Color(.windowBackgroundColor))
                    .cornerRadius(12)
                }

                // Recent Activity
                VStack(alignment: .leading, spacing: 12) {
                    Text("Recent Activity")
                        .font(.headline)

                    if trades.isEmpty {
                        Text("No trades recorded yet")
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(trades.prefix(5)) { trade in
                            HStack {
                                Text(trade.symbol)
                                    .fontWeight(.medium)

                                TradeSideBadge(side: trade.side)

                                Spacer()

                                if let pnl = trade.pnl {
                                    Text("\(pnl >= 0 ? "+" : "")$\(String(format: "%.2f", pnl))")
                                        .foregroundColor(pnl >= 0 ? .green : .red)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
            }
            .padding()
        }
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }
}

// MARK: - Journal Stat Card

struct JournalStatCard: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(color)

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

// MARK: - Add Trade Sheet

struct AddTradeSheet: View {
    @Environment(\.dismiss) var dismiss
    let onSave: (Trade) -> Void

    @State private var symbol = ""
    @State private var side = "long"
    @State private var quantity = ""
    @State private var entryPrice = ""
    @State private var exitPrice = ""
    @State private var strategy = ""
    @State private var notes = ""
    @State private var isClosed = false
    @State private var isSubmitting = false

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Log Trade")
                    .font(.title2)
                    .fontWeight(.bold)

                Spacer()

                Button("Cancel") {
                    dismiss()
                }
                .buttonStyle(.bordered)
            }
            .padding()

            Divider()

            // Form
            Form {
                Section("Trade Details") {
                    TextField("Symbol", text: $symbol)
                        .textCase(.uppercase)

                    Picker("Side", selection: $side) {
                        Text("Long").tag("long")
                        Text("Short").tag("short")
                    }
                    .pickerStyle(.segmented)

                    TextField("Quantity", text: $quantity)

                    TextField("Entry Price", text: $entryPrice)
                }

                Section("Close Trade") {
                    Toggle("Trade Closed", isOn: $isClosed)

                    if isClosed {
                        TextField("Exit Price", text: $exitPrice)
                    }
                }

                Section("Additional Info") {
                    TextField("Strategy", text: $strategy)

                    TextField("Notes", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .formStyle(.grouped)

            Divider()

            // Footer
            HStack {
                Spacer()

                Button(action: saveTrade) {
                    if isSubmitting {
                        ProgressView()
                            .scaleEffect(0.7)
                    } else {
                        Text("Save Trade")
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(symbol.isEmpty || quantity.isEmpty || entryPrice.isEmpty || isSubmitting)
            }
            .padding()
        }
        .frame(width: 500, height: 550)
    }

    private func saveTrade() {
        guard let qty = Double(quantity),
              let entry = Double(entryPrice) else { return }

        isSubmitting = true

        Task {
            do {
                let trade = try await APIService.shared.logTrade(
                    symbol: symbol.uppercased(),
                    side: side,
                    quantity: qty,
                    entryPrice: entry,
                    exitPrice: isClosed ? Double(exitPrice) : nil,
                    strategy: strategy.isEmpty ? nil : strategy,
                    notes: notes.isEmpty ? nil : notes
                )
                onSave(trade)
                dismiss()
            } catch {
                print("Save trade error: \(error)")
            }
            isSubmitting = false
        }
    }
}

#Preview {
    JournalView()
        .environmentObject(AppState())
}
