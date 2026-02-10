//
//  ChatView.swift
//  dPolaris
//
//  Interactive chat interface with dPolaris AI
//

import SwiftUI

struct ChatView: View {
    @EnvironmentObject var appState: AppState
    @State private var messages: [ChatMessage] = []
    @State private var inputText = ""
    @State private var isLoading = false
    @FocusState private var isInputFocused: Bool

    let quickActions = [
        ("Scout", "@scout"),
        ("Risk", "@risk"),
        ("Regime", "@regime"),
        ("Performance", "@performance"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading) {
                    Text("Chat with dPolaris")
                        .font(.title2)
                        .fontWeight(.bold)

                    Text("Your AI trading assistant")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button(action: clearChat) {
                    Label("Clear", systemImage: "trash")
                }
                .buttonStyle(.bordered)
            }
            .padding()
            .background(Color(.windowBackgroundColor))

            Divider()

            // Messages
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 16) {
                        ForEach(messages) { message in
                            MessageBubble(message: message)
                                .id(message.id)
                        }

                        if isLoading {
                            HStack {
                                ProgressView()
                                    .scaleEffect(0.8)
                                Text("dPolaris is thinking...")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Spacer()
                            }
                            .padding(.horizontal)
                        }
                    }
                    .padding()
                }
                .onChange(of: messages.count) { _, _ in
                    if let lastMessage = messages.last {
                        withAnimation {
                            proxy.scrollTo(lastMessage.id, anchor: .bottom)
                        }
                    }
                }
            }

            Divider()

            // Quick Actions
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(quickActions, id: \.0) { action in
                        Button(action.0) {
                            sendMessage(action.1)
                        }
                        .buttonStyle(.bordered)
                    }

                    Divider()
                        .frame(height: 20)

                    // Quick analyze buttons
                    ForEach(["AAPL", "NVDA", "SPY"], id: \.self) { symbol in
                        Button("Analyze \(symbol)") {
                            sendMessage("@analyze \(symbol)")
                        }
                        .buttonStyle(.bordered)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
            }
            .background(Color(.windowBackgroundColor))

            Divider()

            // Input
            HStack(spacing: 12) {
                TextField("Ask dPolaris anything...", text: $inputText, axis: .vertical)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(Color(.textBackgroundColor))
                    .cornerRadius(8)
                    .focused($isInputFocused)
                    .lineLimit(1...5)
                    .onSubmit {
                        if !inputText.isEmpty {
                            sendMessage(inputText)
                        }
                    }

                Button(action: { sendMessage(inputText) }) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title)
                        .foregroundColor(.accentColor)
                }
                .buttonStyle(.plain)
                .disabled(inputText.isEmpty || isLoading)
                .keyboardShortcut(.return, modifiers: .command)
            }
            .padding()
            .background(Color(.windowBackgroundColor))
        }
        .onAppear {
            // Add welcome message
            if messages.isEmpty {
                messages.append(ChatMessage(
                    role: .assistant,
                    content: """
                    Welcome to dPolaris AI! I'm your trading intelligence assistant.

                    **Quick Commands:**
                    - `@scout` - Scan for trading opportunities
                    - `@analyze SYMBOL` - Deep analysis of a stock
                    - `@predict SYMBOL` - ML prediction
                    - `@risk` - Portfolio risk assessment
                    - `@regime` - Market regime analysis
                    - `@performance` - Your trading performance

                    What would you like to explore today?
                    """,
                    timestamp: Date()
                ))
            }
        }
    }

    private func sendMessage(_ text: String) {
        guard !text.isEmpty else { return }

        // Add user message
        messages.append(ChatMessage(
            role: .user,
            content: text,
            timestamp: Date()
        ))

        inputText = ""
        isLoading = true

        Task {
            do {
                let response = try await APIService.shared.chat(message: text)

                messages.append(ChatMessage(
                    role: .assistant,
                    content: response,
                    timestamp: Date()
                ))
            } catch {
                messages.append(ChatMessage(
                    role: .assistant,
                    content: "Sorry, I encountered an error: \(error.localizedDescription)",
                    timestamp: Date()
                ))
            }

            isLoading = false
        }
    }

    private func clearChat() {
        messages.removeAll()
    }
}

// MARK: - Message Bubble

struct MessageBubble: View {
    let message: ChatMessage

    var body: some View {
        HStack {
            if message.isUser {
                Spacer(minLength: 100)
            }

            VStack(alignment: message.isUser ? .trailing : .leading, spacing: 4) {
                // Message content
                if message.isUser {
                    Text(message.content)
                        .padding(12)
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(16)
                        .cornerRadius(4, corners: message.isUser ? .bottomRight : .bottomLeft)
                } else {
                    // AI message with markdown support
                    Text(LocalizedStringKey(message.content))
                        .padding(12)
                        .background(Color(.windowBackgroundColor))
                        .cornerRadius(16)
                        .cornerRadius(4, corners: .bottomLeft)
                        .textSelection(.enabled)
                }

                // Timestamp
                Text(formatTime(message.timestamp))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            if !message.isUser {
                Spacer(minLength: 100)
            }
        }
    }

    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

// MARK: - Corner Radius Extension

extension View {
    func cornerRadius(_ radius: CGFloat, corners: RectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: RectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = NSBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

struct RectCorner: OptionSet {
    let rawValue: Int

    static let topLeft = RectCorner(rawValue: 1 << 0)
    static let topRight = RectCorner(rawValue: 1 << 1)
    static let bottomLeft = RectCorner(rawValue: 1 << 2)
    static let bottomRight = RectCorner(rawValue: 1 << 3)

    static let allCorners: RectCorner = [.topLeft, .topRight, .bottomLeft, .bottomRight]
}

extension NSBezierPath {
    convenience init(roundedRect rect: CGRect, byRoundingCorners corners: RectCorner, cornerRadii: CGSize) {
        self.init()

        let topLeft = corners.contains(.topLeft) ? cornerRadii.width : 0
        let topRight = corners.contains(.topRight) ? cornerRadii.width : 0
        let bottomLeft = corners.contains(.bottomLeft) ? cornerRadii.width : 0
        let bottomRight = corners.contains(.bottomRight) ? cornerRadii.width : 0

        move(to: CGPoint(x: rect.minX + topLeft, y: rect.minY))

        // Top edge
        line(to: CGPoint(x: rect.maxX - topRight, y: rect.minY))
        if topRight > 0 {
            curve(to: CGPoint(x: rect.maxX, y: rect.minY + topRight),
                  controlPoint1: CGPoint(x: rect.maxX, y: rect.minY),
                  controlPoint2: CGPoint(x: rect.maxX, y: rect.minY + topRight))
        }

        // Right edge
        line(to: CGPoint(x: rect.maxX, y: rect.maxY - bottomRight))
        if bottomRight > 0 {
            curve(to: CGPoint(x: rect.maxX - bottomRight, y: rect.maxY),
                  controlPoint1: CGPoint(x: rect.maxX, y: rect.maxY),
                  controlPoint2: CGPoint(x: rect.maxX - bottomRight, y: rect.maxY))
        }

        // Bottom edge
        line(to: CGPoint(x: rect.minX + bottomLeft, y: rect.maxY))
        if bottomLeft > 0 {
            curve(to: CGPoint(x: rect.minX, y: rect.maxY - bottomLeft),
                  controlPoint1: CGPoint(x: rect.minX, y: rect.maxY),
                  controlPoint2: CGPoint(x: rect.minX, y: rect.maxY - bottomLeft))
        }

        // Left edge
        line(to: CGPoint(x: rect.minX, y: rect.minY + topLeft))
        if topLeft > 0 {
            curve(to: CGPoint(x: rect.minX + topLeft, y: rect.minY),
                  controlPoint1: CGPoint(x: rect.minX, y: rect.minY),
                  controlPoint2: CGPoint(x: rect.minX + topLeft, y: rect.minY))
        }

        close()
    }

    var cgPath: CGPath {
        let path = CGMutablePath()
        var points = [CGPoint](repeating: .zero, count: 3)

        for i in 0..<elementCount {
            let type = element(at: i, associatedPoints: &points)
            switch type {
            case .moveTo:
                path.move(to: points[0])
            case .lineTo:
                path.addLine(to: points[0])
            case .curveTo:
                path.addCurve(to: points[2], control1: points[0], control2: points[1])
            case .closePath:
                path.closeSubpath()
            @unknown default:
                break
            }
        }

        return path
    }
}

#Preview {
    ChatView()
        .environmentObject(AppState())
}
