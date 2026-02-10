//
//  MenuBarView.swift
//  dPolaris
//
//  Compact menu bar widget for quick access
//

import SwiftUI

struct MenuBarView: View {
    @EnvironmentObject var appState: AppState
    @State private var quickSymbol = ""

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Image(systemName: "star.circle.fill")
                    .foregroundColor(.blue)
                Text("dPolaris")
                    .fontWeight(.bold)

                Spacer()

                // Daemon status
                HStack(spacing: 4) {
                    Circle()
                        .fill(appState.isDaemonRunning ? Color.green : Color.red)
                        .frame(width: 8, height: 8)
                    Text(appState.isDaemonRunning ? "ON" : "OFF")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(.windowBackgroundColor))

            Divider()

            // Portfolio Summary
            VStack(spacing: 8) {
                HStack {
                    Text("Portfolio")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Spacer()

                    if let portfolio = appState.portfolio {
                        Text(formatCurrency(portfolio.totalValue))
                            .font(.headline)
                            .fontWeight(.bold)
                    } else {
                        Text("--")
                            .foregroundColor(.secondary)
                    }
                }

                if let portfolio = appState.portfolio {
                    HStack {
                        Text("Today")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Spacer()

                        HStack(spacing: 4) {
                            Image(systemName: portfolio.dailyPnl >= 0 ? "arrow.up.right" : "arrow.down.right")
                                .font(.caption2)
                            Text("\(portfolio.dailyPnl >= 0 ? "+" : "")\(formatCurrency(portfolio.dailyPnl))")
                                .font(.caption)
                            Text("(\(String(format: "%.2f", portfolio.dailyPnlPercent))%)")
                                .font(.caption2)
                        }
                        .foregroundColor(portfolio.dailyPnl >= 0 ? .green : .red)
                    }
                }
            }
            .padding(12)

            Divider()

            // Quick Quote
            HStack {
                TextField("Symbol", text: $quickSymbol)
                    .textFieldStyle(.roundedBorder)
                    .frame(width: 80)
                    .onSubmit {
                        fetchQuickQuote()
                    }

                Button("Quote") {
                    fetchQuickQuote()
                }
                .buttonStyle(.bordered)
                .disabled(quickSymbol.isEmpty)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)

            Divider()

            // Quick Actions
            VStack(spacing: 4) {
                MenuBarActionButton(
                    title: "Scout Opportunities",
                    icon: "magnifyingglass",
                    action: { sendCommand("@scout") }
                )

                MenuBarActionButton(
                    title: "Market Regime",
                    icon: "chart.xyaxis.line",
                    action: { sendCommand("@regime") }
                )

                MenuBarActionButton(
                    title: "Risk Check",
                    icon: "exclamationmark.shield",
                    action: { sendCommand("@risk") }
                )
            }
            .padding(.vertical, 4)

            Divider()

            // AI Status
            if let status = appState.aiStatus {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(status.totalMemories) memories")
                            .font(.caption)
                        Text("\(status.modelsAvailable) models")
                            .font(.caption)
                    }
                    .foregroundColor(.secondary)

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        Text("\(status.totalTrades) trades")
                            .font(.caption)
                        if let winRate = status.winRate {
                            Text("\(String(format: "%.0f", winRate))% win")
                                .font(.caption)
                        }
                    }
                    .foregroundColor(.secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)

                Divider()
            }

            // Footer Actions
            HStack {
                Button(action: { appState.toggleDaemon() }) {
                    Label(
                        appState.isDaemonRunning ? "Stop AI" : "Start AI",
                        systemImage: appState.isDaemonRunning ? "stop.fill" : "play.fill"
                    )
                }
                .buttonStyle(.borderless)

                Spacer()

                Button(action: openMainWindow) {
                    Label("Open App", systemImage: "macwindow")
                }
                .buttonStyle(.borderless)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)

            Divider()

            // Quit
            Button(action: { NSApplication.shared.terminate(nil) }) {
                Label("Quit dPolaris", systemImage: "power")
            }
            .buttonStyle(.borderless)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .frame(width: 280)
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }

    private func fetchQuickQuote() {
        guard !quickSymbol.isEmpty else { return }

        Task {
            do {
                let quote = try await APIService.shared.getQuote(symbol: quickSymbol.uppercased())
                // Show quote in notification or update UI
                showQuoteNotification(quote)
                quickSymbol = ""
            } catch {
                print("Quick quote error: \(error)")
            }
        }
    }

    private func showQuoteNotification(_ quote: Quote) {
        let content = UNMutableNotificationContent()
        content.title = "\(quote.symbol): $\(String(format: "%.2f", quote.price))"
        content.body = "\(quote.change >= 0 ? "+" : "")\(String(format: "%.2f", quote.change)) (\(String(format: "%.2f", quote.changePercent))%)"

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    private func sendCommand(_ command: String) {
        Task {
            do {
                _ = try await APIService.shared.chat(message: command)
            } catch {
                print("Command error: \(error)")
            }
        }
    }

    private func openMainWindow() {
        NSApplication.shared.activate(ignoringOtherApps: true)
        if let window = NSApplication.shared.windows.first(where: { $0.title == "dPolaris" }) {
            window.makeKeyAndOrderFront(nil)
        }
    }
}

// MARK: - Menu Bar Action Button

struct MenuBarActionButton: View {
    let title: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .frame(width: 20)
                Text(title)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
        }
        .buttonStyle(.borderless)
    }
}

// MARK: - Compact Watchlist View for Menu Bar

struct MenuBarWatchlistView: View {
    let symbols: [String]
    let quotes: [String: Quote]

    var body: some View {
        VStack(spacing: 0) {
            ForEach(symbols, id: \.self) { symbol in
                if let quote = quotes[symbol] {
                    HStack {
                        Text(symbol)
                            .font(.caption)
                            .fontWeight(.medium)

                        Spacer()

                        Text("$\(String(format: "%.2f", quote.price))")
                            .font(.caption)

                        HStack(spacing: 2) {
                            Image(systemName: quote.change >= 0 ? "arrow.up" : "arrow.down")
                                .font(.caption2)
                            Text("\(String(format: "%.1f", quote.changePercent))%")
                                .font(.caption2)
                        }
                        .foregroundColor(quote.change >= 0 ? .green : .red)
                        .frame(width: 60, alignment: .trailing)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                }
            }
        }
    }
}

// MARK: - Import for Notifications

import UserNotifications

#Preview {
    MenuBarView()
        .environmentObject(AppState())
}
