//
//  Models.swift
//  dPolaris
//
//  Data models matching the Python backend API
//

import Foundation

// MARK: - Portfolio

struct Portfolio: Codable, Identifiable {
    var id: String { "portfolio" }

    let totalValue: Double
    let cash: Double
    let invested: Double
    let dailyPnl: Double
    let dailyPnlPercent: Double
    let totalPnl: Double
    let totalPnlPercent: Double
    let goalTarget: Double
    let goalProgress: Double

    enum CodingKeys: String, CodingKey {
        case totalValue = "total_value"
        case cash
        case invested
        case dailyPnl = "daily_pnl"
        case dailyPnlPercent = "daily_pnl_percent"
        case totalPnl = "total_pnl"
        case totalPnlPercent = "total_pnl_percent"
        case goalTarget = "goal_target"
        case goalProgress = "goal_progress"
    }
}

// MARK: - Position

struct Position: Codable, Identifiable {
    let id: Int
    let symbol: String
    let positionType: String
    let quantity: Double
    let entryPrice: Double
    let currentPrice: Double?
    let unrealizedPnl: Double?
    let unrealizedPnlPercent: Double?
    let optionDetails: OptionDetails?

    enum CodingKeys: String, CodingKey {
        case id, symbol, quantity
        case positionType = "position_type"
        case entryPrice = "entry_price"
        case currentPrice = "current_price"
        case unrealizedPnl = "unrealized_pnl"
        case unrealizedPnlPercent = "unrealized_pnl_percent"
        case optionDetails = "option_details"
    }

    var marketValue: Double {
        (currentPrice ?? entryPrice) * quantity
    }
}

struct OptionDetails: Codable {
    let strike: Double
    let expiration: String
    let optionType: String
    let delta: Double?
    let theta: Double?
    let iv: Double?

    enum CodingKeys: String, CodingKey {
        case strike, expiration, delta, theta, iv
        case optionType = "option_type"
    }
}

// MARK: - AI Status

struct AIStatus: Codable {
    let daemonRunning: Bool
    let lastActivity: String?
    let totalMemories: Int
    let totalTrades: Int
    let modelsAvailable: Int
    let uptime: String?
    let winRate: Double?

    enum CodingKeys: String, CodingKey {
        case daemonRunning = "daemon_running"
        case lastActivity = "last_activity"
        case totalMemories = "total_memories"
        case totalTrades = "total_trades"
        case modelsAvailable = "models_available"
        case uptime
        case winRate = "win_rate"
    }
}

struct DeepLearningStatus: Codable {
    let device: String?
    let pythonVersion: String?
    let deepLearningEnabled: Bool?
    let deepLearningReason: String?

    enum CodingKeys: String, CodingKey {
        case device
        case pythonVersion = "python_version"
        case deepLearningEnabled = "deep_learning_enabled"
        case deepLearningReason = "deep_learning_reason"
    }
}

// MARK: - AI Memory

struct AIMemory: Codable, Identifiable {
    let id: Int
    let category: String
    let content: String
    let importance: Double
    let createdAt: String
    let accessCount: Int

    enum CodingKeys: String, CodingKey {
        case id, category, content, importance
        case createdAt = "created_at"
        case accessCount = "access_count"
    }

    var categoryIcon: String {
        switch category {
        case "trading_style": return "person.fill"
        case "risk_tolerance": return "exclamationmark.shield"
        case "mistake_pattern": return "xmark.circle"
        case "success_pattern": return "checkmark.circle"
        case "market_insight": return "chart.line.uptrend.xyaxis"
        case "symbol_knowledge": return "dollarsign.circle"
        case "strategy_performance": return "target"
        default: return "brain"
        }
    }

    var categoryColor: String {
        switch category {
        case "mistake_pattern": return "red"
        case "success_pattern": return "green"
        case "risk_tolerance": return "orange"
        default: return "blue"
        }
    }
}

// MARK: - Analysis

struct Analysis: Codable, Identifiable, Hashable {
    var id: String { "\(symbol)_\(timestamp)" }

    let symbol: String
    let analysis: String
    let timestamp: String
    let convictionScore: Double?
    let direction: String?

    enum CodingKeys: String, CodingKey {
        case symbol, analysis, timestamp
        case convictionScore = "conviction_score"
        case direction
    }

    static func == (lhs: Analysis, rhs: Analysis) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

// MARK: - Prediction

struct Prediction: Codable, Identifiable {
    var id: String { "\(symbol)_\(timestamp)" }

    let symbol: String
    let prediction: Int
    let predictionLabel: String
    let confidence: Double
    let probabilityUp: Double
    let probabilityDown: Double
    let signal: String
    let context: PredictionContext
    let modelName: String
    let timestamp: String

    enum CodingKeys: String, CodingKey {
        case symbol, prediction, confidence, signal, context, timestamp
        case predictionLabel = "prediction_label"
        case probabilityUp = "probability_up"
        case probabilityDown = "probability_down"
        case modelName = "model_name"
    }
}

struct PredictionContext: Codable {
    let rsi: Double
    let trend: String
    let volatility: String
    let momentum: String
}

// MARK: - Trade Setup

struct TradeSetup: Codable, Identifiable {
    var id: String { "\(symbol)_\(generatedAt)" }

    let symbol: String
    let generatedAt: String
    let bias: String
    let setupType: String
    let timeHorizonDays: Int
    let confidence: Double
    let modelConfidence: Double
    let probabilityUp: Double
    let probabilityDown: Double
    let entry: TradeSetupEntry
    let risk: TradeSetupRisk
    let targets: [TradeSetupTarget]
    let reasons: [String]
    let riskFlags: [String]
    let insights: [TradeSetupInsight]
    let optionsPlan: TradeSetupOptionsPlan
    let model: TradeSetupModel
    let marketSnapshot: TradeSetupMarketSnapshot

    enum CodingKeys: String, CodingKey {
        case symbol, bias, confidence, entry, risk, targets, reasons, insights, model
        case generatedAt = "generated_at"
        case setupType = "setup_type"
        case timeHorizonDays = "time_horizon_days"
        case modelConfidence = "model_confidence"
        case probabilityUp = "probability_up"
        case probabilityDown = "probability_down"
        case riskFlags = "risk_flags"
        case optionsPlan = "options_plan"
        case marketSnapshot = "market_snapshot"
    }
}

struct TradeSetupEntry: Codable {
    let trigger: Double?
    let zoneLow: Double?
    let zoneHigh: Double?
    let condition: String

    enum CodingKeys: String, CodingKey {
        case trigger, condition
        case zoneLow = "zone_low"
        case zoneHigh = "zone_high"
    }
}

struct TradeSetupRisk: Codable {
    let stopLoss: Double?
    let invalidation: String
    let riskPerShare: Double?
    let maxRiskDollars: Double
    let maxPortfolioRiskPercent: Double
    let suggestedShares: Int
    let suggestedNotional: Double
    let suggestedPositionPercent: Double

    enum CodingKeys: String, CodingKey {
        case invalidation
        case stopLoss = "stop_loss"
        case riskPerShare = "risk_per_share"
        case maxRiskDollars = "max_risk_dollars"
        case maxPortfolioRiskPercent = "max_portfolio_risk_percent"
        case suggestedShares = "suggested_shares"
        case suggestedNotional = "suggested_notional"
        case suggestedPositionPercent = "suggested_position_percent"
    }
}

struct TradeSetupTarget: Codable, Identifiable {
    var id: String { label }

    let label: String
    let price: Double
    let rMultiple: Double?

    enum CodingKeys: String, CodingKey {
        case label, price
        case rMultiple = "r_multiple"
    }
}

struct TradeSetupInsight: Codable, Identifiable {
    var id: String { title }

    let title: String
    let detail: String
}

struct TradeSetupOptionsPlan: Codable {
    let stance: String
    let strategy: String
    let dteRange: String
    let deltaRange: String
    let maxPremiumPctOfPortfolio: Double

    enum CodingKeys: String, CodingKey {
        case stance, strategy
        case dteRange = "dte_range"
        case deltaRange = "delta_range"
        case maxPremiumPctOfPortfolio = "max_premium_pct_of_portfolio"
    }
}

struct TradeSetupModel: Codable {
    let source: String
    let name: String
    let type: String
    let accuracy: Double?
}

struct TradeSetupMarketSnapshot: Codable {
    let lastPrice: Double
    let rsi14: Double
    let atr14: Double
    let atrPercent: Double
    let hvol20: Double
    let trend: String
    let momentum: String
    let volatilityRegime: String
    let volumeRatio20: Double
    let adx: Double

    enum CodingKeys: String, CodingKey {
        case trend, momentum, adx
        case lastPrice = "last_price"
        case rsi14 = "rsi_14"
        case atr14 = "atr_14"
        case atrPercent = "atr_percent"
        case hvol20 = "hvol_20"
        case volatilityRegime = "volatility_regime"
        case volumeRatio20 = "volume_ratio_20"
    }
}

// MARK: - Trade

struct Trade: Codable, Identifiable, Hashable {
    let id: Int
    let symbol: String
    let side: String
    let quantity: Double
    let entryPrice: Double
    let exitPrice: Double?
    let entryTime: String
    let exitTime: String?
    let pnl: Double?
    let pnlPercent: Double?
    let strategy: String?
    let notes: String?
    let status: String

    enum CodingKeys: String, CodingKey {
        case id, symbol, side, quantity, strategy, notes, status, pnl
        case entryPrice = "entry_price"
        case exitPrice = "exit_price"
        case entryTime = "entry_time"
        case exitTime = "exit_time"
        case pnlPercent = "pnl_percent"
    }

    static func == (lhs: Trade, rhs: Trade) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

// MARK: - Trade Stats

struct TradeStats: Codable {
    let totalTrades: Int
    let winningTrades: Int?
    let losingTrades: Int?
    let avgWin: Double?
    let avgLoss: Double?
    let totalPnl: Double?
    let profitFactor: Double?

    enum CodingKeys: String, CodingKey {
        case totalTrades = "total_trades"
        case winningTrades = "winning_trades"
        case losingTrades = "losing_trades"
        case avgWin = "avg_win"
        case avgLoss = "avg_loss"
        case totalPnl = "total_pnl"
        case profitFactor = "profit_factor"
    }

    var winRate: Double {
        guard let wins = winningTrades, totalTrades > 0 else { return 0 }
        return Double(wins) / Double(totalTrades) * 100
    }
}

// MARK: - Goal Progress

struct GoalProgress: Codable {
    let currentValue: Double
    let target: Double
    let startingCapital: Double
    let progressPercent: Double
    let profitToDate: Double
    let profitRemaining: Double
    let onTrack: Bool?

    enum CodingKeys: String, CodingKey {
        case target
        case currentValue = "current_value"
        case startingCapital = "starting_capital"
        case progressPercent = "progress_percent"
        case profitToDate = "profit_to_date"
        case profitRemaining = "profit_remaining"
        case onTrack = "on_track"
    }
}

// MARK: - Watchlist Item

struct WatchlistItem: Codable, Identifiable {
    let id: Int
    let symbol: String
    let notes: String?
    let targetEntry: Double?
    let targetExit: Double?
    let priority: Int
    let addedAt: String?

    enum CodingKeys: String, CodingKey {
        case id, symbol, notes, priority
        case targetEntry = "target_entry"
        case targetExit = "target_exit"
        case addedAt = "added_at"
    }
}

// MARK: - Quote

struct Quote: Codable, Identifiable {
    var id: String { symbol }

    let symbol: String
    let name: String?
    let price: Double
    let previousClose: Double?
    let open: Double
    let high: Double
    let low: Double
    let volume: Double
    let avgVolume: Double?
    let change: Double
    let changePercent: Double
    let marketCap: Double?
    let pe: Double?
    let eps: Double?
    let fiftyTwoWeekHigh: Double?
    let fiftyTwoWeekLow: Double?
    let timestamp: String?

    enum CodingKeys: String, CodingKey {
        case symbol, name, price, open, high, low, volume, change, timestamp, pe, eps
        case previousClose = "previous_close"
        case avgVolume = "avg_volume"
        case changePercent = "change_percent"
        case marketCap = "market_cap"
        case fiftyTwoWeekHigh = "fifty_two_week_high"
        case fiftyTwoWeekLow = "fifty_two_week_low"
    }
}

// MARK: - ML Model

struct MLModel: Codable, Identifiable {
    var id: String { name }

    let name: String
    let latestVersion: String
    let target: String?
    let modelType: String?
    let metrics: MLMetrics?

    enum CodingKeys: String, CodingKey {
        case name, target, metrics
        case latestVersion = "latest_version"
        case modelType = "model_type"
    }
}

struct MLMetrics: Codable {
    let accuracy: Double?
    let precision: Double?
    let recall: Double?
    let f1: Double?

    var accuracyPercent: Double {
        (accuracy ?? 0) * 100
    }
}

// MARK: - Chat Message

struct ChatMessage: Identifiable, Equatable {
    let id = UUID()
    let role: Role
    let content: String
    let timestamp: Date

    enum Role {
        case user
        case assistant
    }

    var isUser: Bool { role == .user }
}

// MARK: - Scout Report

struct ScoutReport: Codable {
    let report: String
    let timestamp: String
}

// MARK: - API Response Wrappers

struct APIResponse<T: Codable>: Codable {
    let data: T?
    let error: String?
    let status: String?
}

struct HealthCheck: Codable {
    let status: String
    let timestamp: String
}
