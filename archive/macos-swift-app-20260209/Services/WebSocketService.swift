//
//  WebSocketService.swift
//  dPolaris
//
//  WebSocket connections for real-time updates
//

import Foundation
import Combine

@MainActor
class WebSocketService: ObservableObject {
    static let shared = WebSocketService()

    @Published var portfolio: Portfolio?
    @Published var latestQuotes: [String: Quote] = [:]
    @Published var isConnected = false

    private var portfolioTask: URLSessionWebSocketTask?
    private var pricesTask: URLSessionWebSocketTask?
    private var chatTask: URLSessionWebSocketTask?

    private let baseURL = "ws://127.0.0.1:8420"

    // MARK: - Portfolio Stream

    func connectPortfolio() {
        guard let url = URL(string: "\(baseURL)/ws/portfolio") else { return }

        portfolioTask = URLSession.shared.webSocketTask(with: url)
        portfolioTask?.resume()
        isConnected = true

        receivePortfolioUpdates()
    }

    private func receivePortfolioUpdates() {
        portfolioTask?.receive { [weak self] result in
            Task { @MainActor in
                switch result {
                case .success(let message):
                    if case .string(let text) = message,
                       let data = text.data(using: .utf8) {
                        do {
                            let update = try JSONDecoder().decode(PortfolioWebSocketMessage.self, from: data)
                            self?.portfolio = update.data
                        } catch {
                            print("Portfolio decode error: \(error)")
                        }
                    }
                    self?.receivePortfolioUpdates()

                case .failure(let error):
                    print("Portfolio WebSocket error: \(error)")
                    self?.isConnected = false
                    // Reconnect after delay
                    try? await Task.sleep(nanoseconds: 5_000_000_000)
                    self?.connectPortfolio()
                }
            }
        }
    }

    // MARK: - Price Stream

    func connectPrices() {
        guard let url = URL(string: "\(baseURL)/ws/prices") else { return }

        pricesTask = URLSession.shared.webSocketTask(with: url)
        pricesTask?.resume()

        receivePriceUpdates()
    }

    private func receivePriceUpdates() {
        pricesTask?.receive { [weak self] result in
            Task { @MainActor in
                switch result {
                case .success(let message):
                    if case .string(let text) = message,
                       let data = text.data(using: .utf8) {
                        do {
                            let update = try JSONDecoder().decode(PriceWebSocketMessage.self, from: data)
                            for (symbol, quote) in update.quotes {
                                self?.latestQuotes[symbol] = quote
                            }
                        } catch {
                            print("Price decode error: \(error)")
                        }
                    }
                    self?.receivePriceUpdates()

                case .failure(let error):
                    print("Price WebSocket error: \(error)")
                    try? await Task.sleep(nanoseconds: 5_000_000_000)
                    self?.connectPrices()
                }
            }
        }
    }

    // MARK: - Chat Stream

    func sendChatMessage(_ message: String) async throws -> AsyncThrowingStream<String, Error> {
        guard let url = URL(string: "\(baseURL)/ws/stream") else {
            throw APIError.invalidURL
        }

        chatTask = URLSession.shared.webSocketTask(with: url)
        chatTask?.resume()

        guard let chatTask else {
            throw APIError.notConnected
        }

        // Send message
        let payload = ["message": message]
        let data = try JSONEncoder().encode(payload)
        guard let payloadString = String(data: data, encoding: .utf8) else {
            throw APIError.requestFailed("Failed to encode chat payload")
        }
        try await chatTask.send(.string(payloadString))

        // Return stream
        return AsyncThrowingStream { continuation in
            Task {
                while true {
                    do {
                        let result = try await chatTask.receive()

                        if case .string(let text) = result,
                           let data = text.data(using: .utf8) {
                            let response = try JSONDecoder().decode(ChatWebSocketMessage.self, from: data)

                            if response.type == "chunk" {
                                continuation.yield(response.content ?? "")
                            } else if response.type == "done" {
                                continuation.finish()
                                break
                            }
                        }
                    } catch {
                        continuation.finish(throwing: error)
                        break
                    }
                }
            }
        }
    }

    // MARK: - Disconnect

    func disconnect() {
        portfolioTask?.cancel(with: .goingAway, reason: nil)
        pricesTask?.cancel(with: .goingAway, reason: nil)
        chatTask?.cancel(with: .goingAway, reason: nil)
        isConnected = false
    }
}

// MARK: - WebSocket Message Types

struct PortfolioWebSocketMessage: Codable {
    let type: String
    let data: Portfolio?
    let timestamp: String
}

struct PriceWebSocketMessage: Codable {
    let type: String
    let quotes: [String: Quote]
    let timestamp: String
}

struct ChatWebSocketMessage: Codable {
    let type: String
    let content: String?
    let timestamp: String?
}

struct AlertWebSocketMessage: Codable {
    let type: String
    let alerts: [AlertNotification]
    let timestamp: String
}

struct AlertNotification: Codable, Identifiable {
    let id: Int
    let symbol: String
    let alertType: String
    let message: String
    let triggeredAt: String

    enum CodingKeys: String, CodingKey {
        case id, symbol, message
        case alertType = "alert_type"
        case triggeredAt = "triggered_at"
    }
}
