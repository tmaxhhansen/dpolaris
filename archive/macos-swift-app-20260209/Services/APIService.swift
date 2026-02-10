//
//  APIService.swift
//  dPolaris
//
//  Network service for communicating with dPolaris AI backend
//

import Foundation

enum APIError: LocalizedError {
    case invalidURL
    case requestFailed(String)
    case decodingFailed(String)
    case serverError(String)
    case notConnected
    case connectionRefused

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .requestFailed(let message):
            return "Request failed: \(message)"
        case .decodingFailed(let message):
            return "Failed to decode response: \(message)"
        case .serverError(let message):
            return "Server error: \(message)"
        case .notConnected:
            return "Not connected to dPolaris AI. Make sure the server is running."
        case .connectionRefused:
            return "Could not connect to dPolaris AI server."
        }
    }

    var isConnectionError: Bool {
        switch self {
        case .notConnected, .connectionRefused:
            return true
        case .requestFailed(let message):
            return message.contains("connect") || message.contains("-1004")
        default:
            return false
        }
    }
}

@MainActor
class APIService: ObservableObject {
    static let shared = APIService()

    @Published var isConnected = false
    @Published var lastError: String?

    private var baseURL: String
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    private static let defaultHost = "127.0.0.1"
    private static let defaultPort = 8420
    private static let deepLearningJobPollIntervalSeconds: UInt64 = 2
    private static let deepLearningJobTimeoutSeconds: TimeInterval = 60 * 60
    private static let defaultDeepLearningEpochs = 100

    private struct DeepLearningTrainJobRequest: Codable {
        let symbol: String
        let model_type: String
        let epochs: Int
    }

    private struct TrainingJob: Codable {
        let id: String
        let status: String
        let type: String?
        let symbol: String?
        let modelType: String?
        let epochs: Int?
        let result: TrainingJobResult?
        let error: String?
        let logs: [String]?
        let createdAt: String?
        let updatedAt: String?
        let startedAt: String?
        let completedAt: String?

        enum CodingKeys: String, CodingKey {
            case id, status, type, symbol, epochs, result, error, logs
            case modelType = "model_type"
            case createdAt = "created_at"
            case updatedAt = "updated_at"
            case startedAt = "started_at"
            case completedAt = "completed_at"
        }
    }

    private struct TrainingJobResult: Codable {
        let symbol: String?
        let modelName: String?
        let modelType: String?
        let metrics: MLMetrics?
        let epochsTrained: Int?
        let device: String?

        enum CodingKeys: String, CodingKey {
            case symbol, metrics, device
            case modelName = "model_name"
            case modelType = "model_type"
            case epochsTrained = "epochs_trained"
        }
    }

    init(baseURL: String? = nil) {
        self.baseURL = baseURL ?? Self.loadBaseURLFromDefaults()
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
    }

    private static func loadBaseURLFromDefaults() -> String {
        let defaults = UserDefaults.standard
        let rawHost = defaults.string(forKey: "apiHost") ?? defaultHost
        let host = rawHost.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedHost = host.isEmpty ? defaultHost : host
        let configuredPort = defaults.object(forKey: "apiPort") != nil
            ? defaults.integer(forKey: "apiPort")
            : defaultPort
        let port = (1...65535).contains(configuredPort) ? configuredPort : defaultPort
        return "http://\(normalizedHost):\(port)"
    }

    func syncBaseURLFromDefaults() {
        baseURL = Self.loadBaseURLFromDefaults()
    }

    func updateBaseURL(host: String, port: Int, scheme: String = "http") {
        let trimmedHost = host.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedHost = trimmedHost.isEmpty ? Self.defaultHost : trimmedHost
        let normalizedPort = (1...65535).contains(port) ? port : Self.defaultPort
        baseURL = "\(scheme)://\(normalizedHost):\(normalizedPort)"
    }

    // MARK: - Connection

    func checkConnection() async -> Bool {
        do {
            let _: HealthCheck = try await get("/health")
            isConnected = true
            return true
        } catch {
            isConnected = false
            lastError = error.localizedDescription
            return false
        }
    }

    // MARK: - Portfolio

    func getPortfolio() async throws -> Portfolio {
        try await get("/api/portfolio")
    }

    func getPositions() async throws -> [Position] {
        try await get("/api/positions")
    }

    func getGoalProgress() async throws -> GoalProgress {
        try await get("/api/goal")
    }

    // MARK: - AI Status & Memory

    func getAIStatus() async throws -> AIStatus {
        try await get("/api/status")
    }

    func getDeepLearningStatus() async throws -> DeepLearningStatus {
        try await get("/api/deep-learning/status")
    }

    func getMemories(category: String? = nil, limit: Int = 50) async throws -> [AIMemory] {
        var components = URLComponents()
        components.path = "/api/memories"
        var queryItems = [URLQueryItem(name: "limit", value: String(limit))]
        if let category = category {
            queryItems.append(URLQueryItem(name: "category", value: category))
        }
        components.queryItems = queryItems
        let path = components.string ?? "/api/memories?limit=\(limit)"
        return try await get(path)
    }

    func addMemory(category: String, content: String, importance: Double = 0.5) async throws {
        struct MemoryRequest: Codable {
            let category: String
            let content: String
            let importance: Double
        }

        let request = MemoryRequest(category: category, content: content, importance: importance)
        let _: [String: String] = try await post("/api/memories", body: request)
    }

    // MARK: - Analysis & Predictions

    func analyze(symbol: String) async throws -> Analysis {
        struct AnalyzeRequest: Codable {
            let symbol: String
            let analysis_type: String
        }

        let request = AnalyzeRequest(symbol: symbol.uppercased(), analysis_type: "full")
        return try await post("/api/analyze", body: request)
    }

    func predict(symbol: String) async throws -> Prediction {
        let normalizedSymbol = symbol.uppercased()

        struct DeepLearningPredictionResponse: Codable {
            let symbol: String
            let prediction: String
            let confidence: Double
            let probabilityUp: Double
            let probabilityDown: Double
            let modelType: String?

            enum CodingKeys: String, CodingKey {
                case symbol, prediction, confidence
                case probabilityUp = "probability_up"
                case probabilityDown = "probability_down"
                case modelType = "model_type"
            }
        }

        do {
            return try await post("/api/predict/\(normalizedSymbol)", body: EmptyBody())
        } catch {
            let response: DeepLearningPredictionResponse = try await post("/api/deep-learning/predict/\(normalizedSymbol)", body: EmptyBody())

            let direction = response.prediction.uppercased()
            let predictionValue = direction == "UP" ? 1 : 0
            let signal: String
            if response.probabilityUp >= 0.7 {
                signal = "STRONG_BUY"
            } else if response.probabilityUp >= 0.55 {
                signal = "BUY"
            } else if response.probabilityUp <= 0.3 {
                signal = "STRONG_SELL"
            } else if response.probabilityUp <= 0.45 {
                signal = "SELL"
            } else {
                signal = "NEUTRAL"
            }

            return Prediction(
                symbol: response.symbol,
                prediction: predictionValue,
                predictionLabel: direction,
                confidence: response.confidence,
                probabilityUp: response.probabilityUp,
                probabilityDown: response.probabilityDown,
                signal: signal,
                context: PredictionContext(
                    rsi: 50,
                    trend: "UNKNOWN",
                    volatility: "UNKNOWN",
                    momentum: "UNKNOWN"
                ),
                modelName: response.modelType ?? "deep-learning",
                timestamp: ISO8601DateFormatter().string(from: Date())
            )
        }
    }

    func getTradeSetup(symbol: String) async throws -> TradeSetup {
        let normalizedSymbol = symbol.uppercased()
        return try await post("/api/signals/\(normalizedSymbol)", body: EmptyBody())
    }

    func scout() async throws -> ScoutReport {
        try await get("/api/scout")
    }

    // MARK: - Chat

    func chat(message: String) async throws -> String {
        struct ChatRequest: Codable {
            let message: String
        }

        struct ChatResponse: Codable {
            let response: String
            let timestamp: String
        }

        let request = ChatRequest(message: message)
        let response: ChatResponse = try await post("/api/chat", body: request)
        return response.response
    }

    // MARK: - ML Models

    func getModels() async throws -> [MLModel] {
        do {
            let models: [MLModel] = try await get("/api/models")
            return models
        } catch {
            struct ModelsEnvelope: Decodable {
                let models: [MLModel]
            }

            let envelope: ModelsEnvelope = try await get("/api/models")
            return envelope.models
        }
    }

    func trainStableModel(symbol: String) async throws -> String {
        struct ClassicTrainResponse: Codable {
            let symbol: String
            let result: String
        }

        let normalizedSymbol = symbol.uppercased()
        let response: ClassicTrainResponse = try await post("/api/train/\(normalizedSymbol)", body: EmptyBody())
        let result = response.result.trimmingCharacters(in: .whitespacesAndNewlines)
        if result.lowercased().hasPrefix("training failed") {
            throw APIError.serverError(result)
        }
        return result
    }

    func trainDeepLearningModel(
        symbol: String,
        modelType: String = "lstm",
        onStatus: ((String) -> Void)? = nil,
        onLog: ((String) -> Void)? = nil
    ) async throws -> String {
        struct TrainResponse: Codable {
            let modelName: String
            let modelType: String
            let device: String
            let metrics: MLMetrics?
            let epochsTrained: Int

            enum CodingKeys: String, CodingKey {
                case modelName = "model_name"
                case modelType = "model_type"
                case device
                case metrics
                case epochsTrained = "epochs_trained"
            }
        }

        let normalizedSymbol = symbol.uppercased()

        do {
            let queuedJob = try await enqueueDeepLearningTrainingJob(
                symbol: normalizedSymbol,
                modelType: modelType,
                epochs: Self.defaultDeepLearningEpochs
            )
            let shortJobID = String(queuedJob.id.prefix(8))
            onStatus?("Deep-learning job queued (\(shortJobID), epochs=\(Self.defaultDeepLearningEpochs))")

            let start = Date()
            var lastStatus = queuedJob.status.lowercased()
            var deliveredLogCount = 0

            while Date().timeIntervalSince(start) < Self.deepLearningJobTimeoutSeconds {
                let job = try await getTrainingJob(jobID: queuedJob.id)
                let status = job.status.lowercased()

                if let logs = job.logs {
                    if deliveredLogCount > logs.count {
                        deliveredLogCount = 0
                    }

                    if deliveredLogCount < logs.count {
                        let newLogs = logs.dropFirst(deliveredLogCount)
                        for line in newLogs {
                            onLog?(line)
                        }
                        deliveredLogCount = logs.count
                    }
                }

                if status != lastStatus {
                    lastStatus = status
                    onStatus?("Deep-learning job \(shortJobID): \(status)")
                }

                switch status {
                case "completed":
                    guard let result = job.result else {
                        throw APIError.serverError("Training completed without a result payload")
                    }

                    let accuracy = result.metrics?.accuracy ?? 0
                    let trainedModelType = (result.modelType ?? modelType).uppercased()
                    let epochsTrained = result.epochsTrained ?? 0
                    let device = result.device ?? "unknown"
                    return "Trained \(trainedModelType) on \(device) - \(epochsTrained) epochs, \(String(format: "%.1f", accuracy * 100))% accuracy"

                case "failed":
                    if let jobError = job.error?.lowercased(),
                       jobError.contains("signal 11") || jobError.contains("signal 6") {
                        throw APIError.serverError(
                            "Deep-learning worker crashed (macOS/PyTorch instability). Try Stable (XGBoost) mode for reliability."
                        )
                    }
                    if let jobError = job.error?.lowercased(),
                       jobError.contains("disabled on python 3.13") {
                        throw APIError.serverError(
                            "Deep learning is disabled on Python 3.13 due PyTorch instability. Use Stable (XGBoost) or run backend on Python 3.11/3.12."
                        )
                    }
                    throw APIError.serverError(job.error ?? "Deep learning job failed")

                case "queued", "running":
                    try await Task.sleep(
                        nanoseconds: Self.deepLearningJobPollIntervalSeconds * 1_000_000_000
                    )

                default:
                    try await Task.sleep(
                        nanoseconds: Self.deepLearningJobPollIntervalSeconds * 1_000_000_000
                    )
                }
            }

            throw APIError.requestFailed("Deep-learning job timed out")

        } catch {
            // Backward-compatibility with older backends that do not expose job endpoints.
            if isMissingTrainingJobEndpoint(error) {
                var components = URLComponents()
                components.path = "/api/deep-learning/train/\(normalizedSymbol)"
                components.queryItems = [URLQueryItem(name: "model_type", value: modelType)]
                let path = components.string ?? "/api/deep-learning/train/\(normalizedSymbol)?model_type=\(modelType)"

                let response: TrainResponse = try await post(path, body: EmptyBody(), timeout: 1800)
                let accuracy = response.metrics?.accuracy ?? 0
                return "Trained \(response.modelType.uppercased()) on \(response.device) - \(response.epochsTrained) epochs, \(String(format: "%.1f", accuracy * 100))% accuracy"
            }

            throw error
        }
    }

    func trainModel(symbol: String, modelType: String = "lstm") async throws -> String {
        // Backward-compatible behavior: prefer stable path, then attempt deep learning.
        do {
            return try await trainStableModel(symbol: symbol)
        } catch {
            return try await trainDeepLearningModel(symbol: symbol, modelType: modelType)
        }
    }

    // MARK: - Journal

    func getJournal(limit: Int = 50) async throws -> [Trade] {
        try await get("/api/journal?limit=\(limit)")
    }

    func getTradeStats() async throws -> TradeStats {
        try await get("/api/journal/stats")
    }

    func logTrade(
        symbol: String,
        side: String,
        quantity: Double,
        entryPrice: Double,
        exitPrice: Double? = nil,
        strategy: String? = nil,
        notes: String? = nil
    ) async throws -> Trade {
        struct TradeRequest: Codable {
            let symbol: String
            let side: String
            let quantity: Double
            let entry_price: Double
            let exit_price: Double?
            let strategy: String?
            let notes: String?
        }

        let request = TradeRequest(
            symbol: symbol.uppercased(),
            side: side,
            quantity: quantity,
            entry_price: entryPrice,
            exit_price: exitPrice,
            strategy: strategy,
            notes: notes
        )
        return try await post("/api/journal", body: request)
    }

    // MARK: - Watchlist

    func getWatchlist() async throws -> [WatchlistItem] {
        try await get("/api/watchlist")
    }

    func addToWatchlist(symbol: String, notes: String? = nil, targetEntry: Double? = nil) async throws -> WatchlistItem {
        struct WatchlistRequest: Codable {
            let symbol: String
            let notes: String?
            let target_entry: Double?
            let priority: Int
        }

        let request = WatchlistRequest(
            symbol: symbol.uppercased(),
            notes: notes,
            target_entry: targetEntry,
            priority: 5
        )
        return try await post("/api/watchlist", body: request)
    }

    func removeFromWatchlist(symbol: String) async throws {
        try await delete("/api/watchlist/\(symbol.uppercased())")
    }

    // MARK: - Market Data

    func getQuote(symbol: String) async throws -> Quote {
        try await get("/api/market/quote/\(symbol.uppercased())")
    }

    struct MarketOverview: Codable {
        let spy: Quote?
        let qqq: Quote?
        let dia: Quote?
        let vix: Quote?
    }

    func getMarketOverview() async throws -> MarketOverview {
        try await get("/api/market/overview")
    }

    // MARK: - Daemon Control

    func startDaemon() async throws {
        let _: [String: String] = try await post("/api/scheduler/start", body: EmptyBody())
    }

    func stopDaemon() async throws {
        let _: [String: String] = try await post("/api/scheduler/stop", body: EmptyBody())
    }

    // MARK: - Training Jobs

    private func enqueueDeepLearningTrainingJob(
        symbol: String,
        modelType: String,
        epochs: Int
    ) async throws -> TrainingJob {
        let request = DeepLearningTrainJobRequest(
            symbol: symbol.uppercased(),
            model_type: modelType.lowercased(),
            epochs: epochs
        )
        return try await post("/api/jobs/deep-learning/train", body: request)
    }

    private func getTrainingJob(jobID: String) async throws -> TrainingJob {
        let encodedJobID = jobID.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobID
        return try await get("/api/jobs/\(encodedJobID)")
    }

    private func isMissingTrainingJobEndpoint(_ error: Error) -> Bool {
        let message: String
        if let apiError = error as? APIError {
            switch apiError {
            case .serverError(let details),
                 .requestFailed(let details),
                 .decodingFailed(let details):
                message = details.lowercased()
            default:
                return false
            }
        } else {
            message = error.localizedDescription.lowercased()
        }

        let isNotFound = message.contains("http 404") || message.contains("not found")
        return isNotFound
    }

    // MARK: - HTTP Methods

    private func get<T: Decodable>(_ path: String) async throws -> T {
        guard let url = URL(string: baseURL + path) else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        return try await perform(request)
    }

    private func post<T: Decodable, B: Encodable>(
        _ path: String,
        body: B,
        timeout: TimeInterval? = nil
    ) async throws -> T {
        guard let url = URL(string: baseURL + path) else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try encoder.encode(body)
        if let timeout {
            request.timeoutInterval = timeout
        }

        return try await perform(request)
    }

    private func delete(_ path: String) async throws {
        guard let url = URL(string: baseURL + path) else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (_, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.requestFailed("Invalid response")
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw APIError.serverError("HTTP \(httpResponse.statusCode)")
        }
    }

    private func perform<T: Decodable>(_ request: URLRequest) async throws -> T {
        func extractServerMessage(from data: Data, statusCode: Int) -> String {
            if let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                if let detail = object["detail"] as? String, !detail.isEmpty {
                    return "HTTP \(statusCode): \(detail)"
                }
                if let error = object["error"] as? String, !error.isEmpty {
                    return "HTTP \(statusCode): \(error)"
                }
                if let message = object["message"] as? String, !message.isEmpty {
                    return "HTTP \(statusCode): \(message)"
                }
            }

            if let raw = String(data: data, encoding: .utf8)?
                .trimmingCharacters(in: .whitespacesAndNewlines),
               !raw.isEmpty {
                return "HTTP \(statusCode): \(raw)"
            }

            return "HTTP \(statusCode)"
        }

        do {
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.requestFailed("Invalid response")
            }

            guard (200...299).contains(httpResponse.statusCode) else {
                throw APIError.serverError(
                    extractServerMessage(from: data, statusCode: httpResponse.statusCode)
                )
            }

            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decodingFailed(error.localizedDescription)
            }
        } catch let urlError as URLError {
            // Handle connection errors gracefully
            if urlError.code == .cannotConnectToHost || urlError.code == .networkConnectionLost || urlError.code == .notConnectedToInternet {
                isConnected = false
                throw APIError.connectionRefused
            }
            throw APIError.requestFailed(urlError.localizedDescription)
        } catch let apiError as APIError {
            throw apiError
        } catch {
            // Check for connection refused errors
            let errorString = error.localizedDescription
            if errorString.contains("Could not connect") || errorString.contains("-1004") {
                isConnected = false
                throw APIError.connectionRefused
            }
            throw APIError.requestFailed(error.localizedDescription)
        }
    }
}

// Helper for empty request bodies
private struct EmptyBody: Codable {}
