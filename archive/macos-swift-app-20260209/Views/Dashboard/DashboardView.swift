//
//  DashboardView.swift
//  dPolaris
//
//  Main dashboard showing portfolio overview and goal progress
//

import SwiftUI
import Charts

struct DashboardView: View {
    @EnvironmentObject var appState: AppState
    @State private var goalProgress: GoalProgress?
    @State private var portfolioHistory: [PortfolioDataPoint] = []
    @State private var positions: [Position] = []

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text("Dashboard")
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text("Welcome back to dPolaris")
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Button(action: { appState.refreshAll() }) {
                        Image(systemName: "arrow.clockwise")
                    }
                    .buttonStyle(.bordered)
                }
                .padding(.horizontal)

                // Goal Progress Card
                GoalProgressCard(progress: goalProgress)
                    .padding(.horizontal)

                // Quick Stats
                HStack(spacing: 16) {
                    StatCard(
                        title: "Portfolio Value",
                        value: formatCurrency(appState.portfolio?.totalValue ?? 0),
                        change: appState.portfolio?.dailyPnl ?? 0,
                        changePercent: appState.portfolio?.dailyPnlPercent ?? 0,
                        icon: "briefcase.fill",
                        color: .blue
                    )

                    StatCard(
                        title: "Total P/L",
                        value: formatCurrency(appState.portfolio?.totalPnl ?? 0),
                        change: appState.portfolio?.totalPnl ?? 0,
                        changePercent: appState.portfolio?.totalPnlPercent ?? 0,
                        icon: "chart.line.uptrend.xyaxis",
                        color: (appState.portfolio?.totalPnl ?? 0) >= 0 ? .green : .red
                    )

                    StatCard(
                        title: "Cash",
                        value: formatCurrency(appState.portfolio?.cash ?? 0),
                        subtitle: "\(Int((appState.portfolio?.cash ?? 0) / (appState.portfolio?.totalValue ?? 1) * 100))% available",
                        icon: "dollarsign.circle.fill",
                        color: .orange
                    )
                }
                .padding(.horizontal)

                // Portfolio Chart
                VStack(alignment: .leading) {
                    Text("Portfolio Value")
                        .font(.headline)

                    if !portfolioHistory.isEmpty {
                        Chart(portfolioHistory) { point in
                            LineMark(
                                x: .value("Date", point.date),
                                y: .value("Value", point.value)
                            )
                            .foregroundStyle(.blue)
                            .interpolationMethod(.catmullRom)

                            AreaMark(
                                x: .value("Date", point.date),
                                y: .value("Value", point.value)
                            )
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [.blue.opacity(0.3), .blue.opacity(0.05)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .interpolationMethod(.catmullRom)
                        }
                        .chartYScale(domain: .automatic(includesZero: false))
                        .frame(height: 200)
                    } else {
                        ContentUnavailableView(
                            "No Data",
                            systemImage: "chart.line.downtrend.xyaxis",
                            description: Text("Portfolio history will appear here")
                        )
                        .frame(height: 200)
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // Open Positions
                VStack(alignment: .leading) {
                    HStack {
                        Text("Open Positions")
                            .font(.headline)

                        Spacer()

                        Text("\(positions.count) positions")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    if positions.isEmpty {
                        ContentUnavailableView(
                            "No Positions",
                            systemImage: "tray",
                            description: Text("Your open positions will appear here")
                        )
                        .frame(height: 100)
                    } else {
                        ForEach(positions) { position in
                            PositionRow(position: position)
                        }
                    }
                }
                .padding()
                .background(Color(.windowBackgroundColor))
                .cornerRadius(12)
                .padding(.horizontal)

                // AI Status Quick View
                AIStatusCard(status: appState.aiStatus, isDaemonRunning: appState.isDaemonRunning)
                    .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .task {
            await loadData()
        }
    }

    private func loadData() async {
        do {
            goalProgress = try await APIService.shared.getGoalProgress()
            positions = try await APIService.shared.getPositions()
            // Load portfolio history
        } catch {
            print("Dashboard load error: \(error)")
        }
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }
}

// MARK: - Goal Progress Card

struct GoalProgressCard: View {
    let progress: GoalProgress?

    var body: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading) {
                    Text("Goal: $3,000,000")
                        .font(.title2)
                        .fontWeight(.bold)

                    Text("Path to Financial Freedom")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                if let progress = progress {
                    StatusBadge(
                        text: progress.onTrack == true ? "On Track" : "Behind",
                        color: progress.onTrack == true ? .green : .orange
                    )
                }
            }

            // Progress Bar
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.gray.opacity(0.2))

                    RoundedRectangle(cornerRadius: 12)
                        .fill(
                            LinearGradient(
                                colors: [.blue, .purple],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: geometry.size.width * CGFloat((progress?.progressPercent ?? 0) / 100))

                    Text("\(String(format: "%.1f", progress?.progressPercent ?? 0))%")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.leading, 12)
                }
            }
            .frame(height: 28)

            HStack {
                VStack(alignment: .leading) {
                    Text("Current")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(formatCurrency(progress?.currentValue ?? 0))
                        .font(.title3)
                        .fontWeight(.semibold)
                }

                Spacer()

                VStack {
                    Text("Profit to Date")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(formatCurrency(progress?.profitToDate ?? 0))
                        .font(.title3)
                        .fontWeight(.semibold)
                        .foregroundColor(.green)
                }

                Spacer()

                VStack(alignment: .trailing) {
                    Text("Remaining")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(formatCurrency(progress?.profitRemaining ?? 0))
                        .font(.title3)
                        .fontWeight(.semibold)
                }
            }
        }
        .padding()
        .background(Color(.windowBackgroundColor))
        .cornerRadius(12)
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }
}

// MARK: - Stat Card

struct StatCard: View {
    let title: String
    let value: String
    var change: Double? = nil
    var changePercent: Double? = nil
    var subtitle: String? = nil
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Text(value)
                .font(.title2)
                .fontWeight(.bold)

            if let change = change, let percent = changePercent {
                HStack(spacing: 4) {
                    Image(systemName: change >= 0 ? "arrow.up.right" : "arrow.down.right")
                    Text("\(change >= 0 ? "+" : "")\(formatCurrency(change))")
                    Text("(\(String(format: "%.2f", percent))%)")
                }
                .font(.caption)
                .foregroundColor(change >= 0 ? .green : .red)
            } else if let subtitle = subtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.windowBackgroundColor))
        .cornerRadius(12)
    }

    private func formatCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: value)) ?? "$0"
    }
}

// MARK: - Position Row

struct PositionRow: View {
    let position: Position

    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(position.symbol)
                    .font(.headline)
                Text("\(Int(position.quantity)) shares @ $\(String(format: "%.2f", position.entryPrice))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing) {
                if let pnl = position.unrealizedPnl {
                    Text("\(pnl >= 0 ? "+" : "")$\(String(format: "%.2f", pnl))")
                        .fontWeight(.semibold)
                        .foregroundColor(pnl >= 0 ? .green : .red)
                }

                if let percent = position.unrealizedPnlPercent {
                    Text("\(percent >= 0 ? "+" : "")\(String(format: "%.2f", percent))%")
                        .font(.caption)
                        .foregroundColor(percent >= 0 ? .green : .red)
                }
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - AI Status Card

struct AIStatusCard: View {
    let status: AIStatus?
    let isDaemonRunning: Bool

    var body: some View {
        HStack(spacing: 20) {
            VStack(alignment: .leading) {
                HStack {
                    Circle()
                        .fill(isDaemonRunning ? Color.green : Color.gray)
                        .frame(width: 10, height: 10)

                    Text("AI Daemon")
                        .font(.headline)
                }

                Text(isDaemonRunning ? "Running" : "Stopped")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Divider()
                .frame(height: 40)

            VStack(alignment: .leading) {
                Text("\(status?.totalMemories ?? 0)")
                    .font(.title3)
                    .fontWeight(.bold)
                Text("Memories")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            VStack(alignment: .leading) {
                Text("\(status?.totalTrades ?? 0)")
                    .font(.title3)
                    .fontWeight(.bold)
                Text("Trades Logged")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            VStack(alignment: .leading) {
                Text("\(status?.modelsAvailable ?? 0)")
                    .font(.title3)
                    .fontWeight(.bold)
                Text("ML Models")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding()
        .background(Color(.windowBackgroundColor))
        .cornerRadius(12)
    }
}

// MARK: - Status Badge

struct StatusBadge: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color)
            .foregroundColor(.white)
            .cornerRadius(4)
    }
}

// MARK: - Portfolio Data Point

struct PortfolioDataPoint: Identifiable {
    let id = UUID()
    let date: Date
    let value: Double
}

#Preview {
    DashboardView()
        .environmentObject(AppState())
}
