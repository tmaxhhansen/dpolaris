//
//  WatchlistView.swift
//  dPolaris
//
//  Watchlist for tracking stocks of interest
//

import SwiftUI

struct WatchlistView: View {
    @EnvironmentObject var appState: AppState
    @State private var watchlist: [WatchlistItem] = []
    @State private var quotes: [String: Quote] = [:]
    @State private var showingAddSymbol = false
    @State private var newSymbol = ""
    @State private var isLoading = false
    @State private var selectedItem: WatchlistItem?

    var body: some View {
        HSplitView {
            // Left panel - Watchlist
            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("Watchlist")
                        .font(.title2)
                        .fontWeight(.bold)

                    Spacer()

                    Button(action: refreshQuotes) {
                        Image(systemName: "arrow.clockwise")
                    }
                    .buttonStyle(.bordered)
                    .disabled(isLoading)

                    Button(action: { showingAddSymbol = true }) {
                        Image(systemName: "plus")
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()

                Divider()

                // Watchlist
                if watchlist.isEmpty {
                    ContentUnavailableView(
                        "Empty Watchlist",
                        systemImage: "star",
                        description: Text("Add symbols to track them here")
                    )
                } else {
                    List(selection: $selectedItem) {
                        ForEach(watchlist) { item in
                            WatchlistRow(item: item, quote: quotes[item.symbol])
                                .tag(item)
                                .contextMenu {
                                    Button(role: .destructive) {
                                        removeFromWatchlist(item)
                                    } label: {
                                        Label("Remove", systemImage: "trash")
                                    }

                                    Button {
                                        analyzeSymbol(item.symbol)
                                    } label: {
                                        Label("Analyze", systemImage: "chart.bar.doc.horizontal")
                                    }
                                }
                        }
                    }
                    .listStyle(.sidebar)
                }
            }
            .frame(minWidth: 300, maxWidth: 400)

            // Right panel - Quote detail
            if let item = selectedItem, let quote = quotes[item.symbol] {
                QuoteDetailView(item: item, quote: quote)
            } else if let item = selectedItem {
                VStack {
                    ProgressView()
                    Text("Loading \(item.symbol)...")
                        .foregroundColor(.secondary)
                }
            } else {
                ContentUnavailableView(
                    "Select a Symbol",
                    systemImage: "chart.line.uptrend.xyaxis",
                    description: Text("Choose a symbol from the watchlist to view details")
                )
            }
        }
        .sheet(isPresented: $showingAddSymbol) {
            AddSymbolSheet(onAdd: addToWatchlist)
        }
        .task {
            await loadWatchlist()
        }
    }

    private func loadWatchlist() async {
        do {
            watchlist = try await APIService.shared.getWatchlist()
            refreshQuotes()
        } catch {
            print("Watchlist load error: \(error)")
        }
    }

    private func refreshQuotes() {
        isLoading = true

        Task {
            for item in watchlist {
                do {
                    let quote = try await APIService.shared.getQuote(symbol: item.symbol)
                    quotes[item.symbol] = quote
                } catch {
                    print("Quote error for \(item.symbol): \(error)")
                }
            }
            isLoading = false
        }
    }

    private func addToWatchlist(symbol: String, notes: String?) {
        Task {
            do {
                let item = try await APIService.shared.addToWatchlist(symbol: symbol, notes: notes)
                watchlist.insert(item, at: 0)

                // Fetch quote for new item
                if let quote = try? await APIService.shared.getQuote(symbol: symbol) {
                    quotes[symbol] = quote
                }
            } catch {
                print("Add to watchlist error: \(error)")
            }
        }
    }

    private func removeFromWatchlist(_ item: WatchlistItem) {
        Task {
            do {
                try await APIService.shared.removeFromWatchlist(symbol: item.symbol)
                watchlist.removeAll { $0.id == item.id }
                quotes.removeValue(forKey: item.symbol)
            } catch {
                print("Remove from watchlist error: \(error)")
            }
        }
    }

    private func analyzeSymbol(_ symbol: String) {
        Task {
            do {
                let analysis = try await APIService.shared.analyze(symbol: symbol)
                appState.recentAnalyses.insert(analysis, at: 0)
            } catch {
                print("Analysis error: \(error)")
            }
        }
    }
}

// MARK: - Watchlist Row

struct WatchlistRow: View {
    let item: WatchlistItem
    let quote: Quote?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.symbol)
                    .font(.headline)

                if let notes = item.notes, !notes.isEmpty {
                    Text(notes)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            if let quote = quote {
                VStack(alignment: .trailing, spacing: 4) {
                    Text("$\(String(format: "%.2f", quote.price))")
                        .font(.subheadline)
                        .fontWeight(.medium)

                    HStack(spacing: 2) {
                        Image(systemName: quote.change >= 0 ? "arrow.up.right" : "arrow.down.right")
                            .font(.caption2)
                        Text("\(quote.change >= 0 ? "+" : "")\(String(format: "%.2f", quote.changePercent))%")
                            .font(.caption)
                    }
                    .foregroundColor(quote.change >= 0 ? .green : .red)
                }
            } else {
                ProgressView()
                    .scaleEffect(0.6)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Quote Detail View

struct QuoteDetailView: View {
    let item: WatchlistItem
    let quote: Quote

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text(quote.symbol)
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        if let name = quote.name {
                            Text(name)
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()

                    VStack(alignment: .trailing) {
                        Text("$\(String(format: "%.2f", quote.price))")
                            .font(.title)
                            .fontWeight(.bold)

                        HStack {
                            Image(systemName: quote.change >= 0 ? "arrow.up.right" : "arrow.down.right")
                            Text("\(quote.change >= 0 ? "+" : "")$\(String(format: "%.2f", quote.change))")
                            Text("(\(quote.changePercent >= 0 ? "+" : "")\(String(format: "%.2f", quote.changePercent))%)")
                        }
                        .foregroundColor(quote.change >= 0 ? .green : .red)
                    }
                }

                Divider()

                // Quote Details
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                    QuoteDetailItem(label: "Open", value: "$\(String(format: "%.2f", quote.open))")
                    QuoteDetailItem(label: "High", value: "$\(String(format: "%.2f", quote.high))")
                    QuoteDetailItem(label: "Low", value: "$\(String(format: "%.2f", quote.low))")

                    if let prevClose = quote.previousClose {
                        QuoteDetailItem(label: "Prev Close", value: "$\(String(format: "%.2f", prevClose))")
                    }

                    QuoteDetailItem(label: "Volume", value: formatVolume(quote.volume))

                    if let avgVol = quote.avgVolume {
                        QuoteDetailItem(label: "Avg Volume", value: formatVolume(avgVol))
                    }

                    if let marketCap = quote.marketCap {
                        QuoteDetailItem(label: "Market Cap", value: formatMarketCap(marketCap))
                    }

                    if let pe = quote.pe {
                        QuoteDetailItem(label: "P/E", value: String(format: "%.2f", pe))
                    }

                    if let eps = quote.eps {
                        QuoteDetailItem(label: "EPS", value: "$\(String(format: "%.2f", eps))")
                    }
                }

                // 52-Week Range
                VStack(alignment: .leading, spacing: 8) {
                    Text("52-Week Range")
                        .font(.headline)

                    if let low52 = quote.fiftyTwoWeekLow, let high52 = quote.fiftyTwoWeekHigh {
                        VStack(spacing: 4) {
                            GeometryReader { geometry in
                                let range = high52 - low52
                                let position = range > 0 ? (quote.price - low52) / range : 0.5

                                ZStack(alignment: .leading) {
                                    Rectangle()
                                        .fill(Color.gray.opacity(0.3))
                                        .frame(height: 8)
                                        .cornerRadius(4)

                                    Circle()
                                        .fill(Color.blue)
                                        .frame(width: 16, height: 16)
                                        .offset(x: CGFloat(position) * (geometry.size.width - 16))
                                }
                            }
                            .frame(height: 16)

                            HStack {
                                Text("$\(String(format: "%.2f", low52))")
                                    .font(.caption)
                                    .foregroundColor(.secondary)

                                Spacer()

                                Text("$\(String(format: "%.2f", high52))")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)

                // Notes
                if let notes = item.notes, !notes.isEmpty {
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

                // Quick Actions
                VStack(alignment: .leading, spacing: 12) {
                    Text("Quick Actions")
                        .font(.headline)

                    HStack(spacing: 12) {
                        ActionButton(title: "Analyze", icon: "chart.bar.doc.horizontal", color: .blue) {
                            // Trigger analysis
                        }

                        ActionButton(title: "Predict", icon: "wand.and.stars", color: .purple) {
                            // Trigger prediction
                        }

                        ActionButton(title: "Options", icon: "square.stack.3d.up", color: .orange) {
                            // View options chain
                        }
                    }
                }
            }
            .padding()
        }
    }

    private func formatVolume(_ volume: Double) -> String {
        if volume >= 1_000_000_000 {
            return String(format: "%.2fB", volume / 1_000_000_000)
        } else if volume >= 1_000_000 {
            return String(format: "%.2fM", volume / 1_000_000)
        } else if volume >= 1_000 {
            return String(format: "%.1fK", volume / 1_000)
        }
        return String(format: "%.0f", volume)
    }

    private func formatMarketCap(_ cap: Double) -> String {
        if cap >= 1_000_000_000_000 {
            return String(format: "$%.2fT", cap / 1_000_000_000_000)
        } else if cap >= 1_000_000_000 {
            return String(format: "$%.2fB", cap / 1_000_000_000)
        } else if cap >= 1_000_000 {
            return String(format: "$%.2fM", cap / 1_000_000)
        }
        return String(format: "$%.0f", cap)
    }
}

// MARK: - Quote Detail Item

struct QuoteDetailItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.subheadline)
                .fontWeight(.medium)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Action Button

struct ActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                Text(title)
                    .font(.caption)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(color.opacity(0.1))
            .foregroundColor(color)
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Add Symbol Sheet

struct AddSymbolSheet: View {
    @Environment(\.dismiss) var dismiss
    let onAdd: (String, String?) -> Void

    @State private var symbol = ""
    @State private var notes = ""

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Add to Watchlist")
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
                Section("Symbol") {
                    TextField("Enter symbol (e.g., AAPL)", text: $symbol)
                        .textCase(.uppercase)
                }

                Section("Notes (Optional)") {
                    TextField("Why are you watching this?", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .formStyle(.grouped)

            Divider()

            // Footer
            HStack {
                // Quick add buttons
                HStack(spacing: 8) {
                    ForEach(["SPY", "QQQ", "AAPL", "NVDA"], id: \.self) { sym in
                        Button(sym) {
                            symbol = sym
                        }
                        .buttonStyle(.bordered)
                    }
                }

                Spacer()

                Button("Add") {
                    onAdd(symbol.uppercased(), notes.isEmpty ? nil : notes)
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
                .disabled(symbol.isEmpty)
            }
            .padding()
        }
        .frame(width: 450, height: 350)
    }
}

// MARK: - Watchlist Item Model Extension

extension WatchlistItem: Hashable {
    static func == (lhs: WatchlistItem, rhs: WatchlistItem) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

#Preview {
    WatchlistView()
        .environmentObject(AppState())
}
