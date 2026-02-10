//
//  ServerManager.swift
//  dPolaris
//
//  Manages the dPolaris AI Python backend server process
//

import Foundation
import AppKit

@MainActor
class ServerManager: ObservableObject {
    static let shared = ServerManager()

    @Published var isServerRunning = false
    @Published var serverOutput: String = ""
    @Published var serverError: String?
    @Published var isStarting = false

    private var serverProcess: Process?
    private var outputPipe: Pipe?
    private var errorPipe: Pipe?

    // Path to the dpolaris_ai project
    private var serverPath: String {
        let home = FileManager.default.homeDirectoryForCurrentUser.path
        let defaultsPath = UserDefaults.standard.string(forKey: "backendPath")?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !defaultsPath.isEmpty {
            return NSString(string: defaultsPath).expandingTildeInPath
        }

        if let envPath = ProcessInfo.processInfo.environment["DPOLARIS_AI_PATH"],
           !envPath.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return NSString(string: envPath).expandingTildeInPath
        }

        let candidates = [
            "\(home)/my-git/dPolaris_ai",
            "\(home)/my-git/dpolaris_ai",
            "\(home)/dpolaris_ai"
        ]

        for candidate in candidates where FileManager.default.fileExists(atPath: candidate) {
            return candidate
        }

        return candidates[0]
    }

    // Find Python executable
    private var pythonPath: String {
        // Prefer Python 3.12/3.11 for better PyTorch stability on macOS.
        let possiblePaths = [
            "/opt/homebrew/bin/python3.12",
            "/usr/local/bin/python3.12",
            "/Library/Frameworks/Python.framework/Versions/3.12/bin/python3.12",
            "/opt/homebrew/bin/python3.11",
            "/usr/local/bin/python3.11",
            "/Library/Frameworks/Python.framework/Versions/3.11/bin/python3.11",
            "/usr/local/bin/python3",
            "/opt/homebrew/bin/python3",
            "/Library/Frameworks/Python.framework/Versions/3.12/bin/python3",
            "/Library/Frameworks/Python.framework/Versions/3.11/bin/python3",
            "/Library/Frameworks/Python.framework/Versions/3.13/bin/python3",
            "/usr/bin/python3",
            "/opt/local/bin/python3"
        ]

        for path in possiblePaths {
            if FileManager.default.fileExists(atPath: path) {
                return path
            }
        }

        // Fall back to hoping python3 is in PATH
        return "python3"
    }

    private init() {}

    func startServer() async {
        guard !isServerRunning && !isStarting else { return }

        isStarting = true
        serverError = nil
        serverOutput = ""

        // Check if server directory exists
        guard FileManager.default.fileExists(atPath: serverPath) else {
            serverError = "dPolaris AI not found at \(serverPath)"
            isStarting = false
            return
        }

        // Check if virtual environment exists, if not try system Python
        let venvPython = "\(serverPath)/.venv/bin/python"
        let pythonToUse: String

        if FileManager.default.fileExists(atPath: venvPython) {
            pythonToUse = venvPython
        } else {
            pythonToUse = pythonPath
        }
        serverOutput += "Starting backend with Python: \(pythonToUse)\n"

        let process = Process()
        process.executableURL = URL(fileURLWithPath: pythonToUse)
        process.arguments = ["-m", "cli.main", "server"]
        process.currentDirectoryURL = URL(fileURLWithPath: serverPath)

        // Set up environment
        var environment = ProcessInfo.processInfo.environment
        environment["PYTHONUNBUFFERED"] = "1"
        process.environment = environment

        // Set up pipes for output
        let outputPipe = Pipe()
        let errorPipe = Pipe()
        process.standardOutput = outputPipe
        process.standardError = errorPipe

        self.outputPipe = outputPipe
        self.errorPipe = errorPipe
        self.serverProcess = process

        // Handle output asynchronously
        outputPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
            let data = handle.availableData
            if let output = String(data: data, encoding: .utf8), !output.isEmpty {
                Task { @MainActor in
                    self?.serverOutput += output
                    // Check for server started message
                    if output.contains("Uvicorn running") || output.contains("Application startup complete") {
                        self?.isServerRunning = true
                        self?.isStarting = false
                    }
                }
            }
        }

        errorPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
            let data = handle.availableData
            if let output = String(data: data, encoding: .utf8), !output.isEmpty {
                Task { @MainActor in
                    // Uvicorn logs to stderr, so check for startup here too
                    self?.serverOutput += output
                    if output.contains("Uvicorn running") || output.contains("Application startup complete") {
                        self?.isServerRunning = true
                        self?.isStarting = false
                    }
                }
            }
        }

        // Handle process termination
        process.terminationHandler = { [weak self] process in
            Task { @MainActor in
                self?.isServerRunning = false
                self?.isStarting = false
                self?.outputPipe?.fileHandleForReading.readabilityHandler = nil
                self?.errorPipe?.fileHandleForReading.readabilityHandler = nil

                if process.terminationStatus != 0 && process.terminationStatus != 15 {
                    self?.serverError = "Server exited with code \(process.terminationStatus)"
                }
            }
        }

        do {
            try process.run()

            // Give it a moment to start, then check connection
            try? await Task.sleep(nanoseconds: 2_000_000_000) // 2 seconds

            // If still starting, check if process is running
            if isStarting && process.isRunning {
                // Try to connect
                let connected = await APIService.shared.checkConnection()
                if connected {
                    isServerRunning = true
                    isStarting = false
                }
            }

        } catch {
            serverError = "Failed to start server: \(error.localizedDescription)"
            isStarting = false
        }
    }

    func stopServer() {
        guard let process = serverProcess, process.isRunning else {
            isServerRunning = false
            return
        }

        // Send SIGTERM for graceful shutdown
        process.terminate()

        // Give it a moment to shut down gracefully
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            if self?.serverProcess?.isRunning == true {
                // Force kill if still running
                self?.serverProcess?.interrupt()
            }
        }

        isServerRunning = false
        serverProcess = nil
    }

    func checkDependencies() -> (installed: Bool, message: String) {
        let venvPath = "\(serverPath)/.venv"
        let requirementsPath = "\(serverPath)/requirements.txt"

        // Check if venv exists
        if !FileManager.default.fileExists(atPath: venvPath) {
            return (false, "Virtual environment not found. Run setup first.")
        }

        // Check if requirements.txt exists
        if !FileManager.default.fileExists(atPath: requirementsPath) {
            return (false, "requirements.txt not found.")
        }

        return (true, "Dependencies appear to be installed.")
    }

    func setupEnvironment() async -> Bool {
        isStarting = true
        serverError = nil
        serverOutput = "Setting up Python environment...\n"

        // Remove existing venv if present (in case of previous failed attempt)
        let venvPath = "\(serverPath)/.venv"
        if FileManager.default.fileExists(atPath: venvPath) {
            serverOutput += "Removing existing virtual environment...\n"
            try? FileManager.default.removeItem(atPath: venvPath)
        }

        serverOutput += "Using Python for setup: \(pythonPath)\n"

        // Create virtual environment without pip first (avoids ensurepip issues on Python 3.13)
        let venvProcess = Process()
        venvProcess.executableURL = URL(fileURLWithPath: pythonPath)
        venvProcess.arguments = ["-m", "venv", "--without-pip", ".venv"]
        venvProcess.currentDirectoryURL = URL(fileURLWithPath: serverPath)

        do {
            try venvProcess.run()
            venvProcess.waitUntilExit()

            if venvProcess.terminationStatus != 0 {
                serverError = "Failed to create virtual environment"
                isStarting = false
                return false
            }

            serverOutput += "Virtual environment created.\n"
            serverOutput += "Downloading pip bootstrap...\n"

            // Download and run get-pip.py without shell piping.
            guard let getPipURL = URL(string: "https://bootstrap.pypa.io/get-pip.py") else {
                serverError = "Invalid get-pip.py URL"
                isStarting = false
                return false
            }

            let (scriptData, response) = try await URLSession.shared.data(from: getPipURL)
            if let httpResponse = response as? HTTPURLResponse,
               !(200...299).contains(httpResponse.statusCode) {
                serverError = "Failed to download pip bootstrap (HTTP \(httpResponse.statusCode))"
                isStarting = false
                return false
            }

            let getPipScriptURL = URL(fileURLWithPath: "\(serverPath)/.venv/get-pip.py")
            try scriptData.write(to: getPipScriptURL, options: .atomic)
            defer { try? FileManager.default.removeItem(at: getPipScriptURL) }

            serverOutput += "Installing pip...\n"

            let getPipProcess = Process()
            getPipProcess.executableURL = URL(fileURLWithPath: "\(venvPath)/bin/python3")
            getPipProcess.arguments = [getPipScriptURL.path]
            getPipProcess.currentDirectoryURL = URL(fileURLWithPath: serverPath)

            let getPipPipe = Pipe()
            getPipProcess.standardOutput = getPipPipe
            getPipProcess.standardError = getPipPipe

            getPipPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
                let data = handle.availableData
                if let output = String(data: data, encoding: .utf8), !output.isEmpty {
                    Task { @MainActor in
                        self?.serverOutput += output
                    }
                }
            }

            try getPipProcess.run()
            getPipProcess.waitUntilExit()
            getPipPipe.fileHandleForReading.readabilityHandler = nil

            if getPipProcess.terminationStatus != 0 {
                serverError = "Failed to install pip"
                isStarting = false
                return false
            }

            serverOutput += "Pip installed.\n"
            serverOutput += "Installing dependencies...\n"

            // Install requirements
            let pipProcess = Process()
            pipProcess.executableURL = URL(fileURLWithPath: "\(serverPath)/.venv/bin/pip")
            pipProcess.arguments = ["install", "-r", "requirements.txt"]
            pipProcess.currentDirectoryURL = URL(fileURLWithPath: serverPath)

            let outputPipe = Pipe()
            pipProcess.standardOutput = outputPipe
            pipProcess.standardError = outputPipe

            outputPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
                let data = handle.availableData
                if let output = String(data: data, encoding: .utf8), !output.isEmpty {
                    Task { @MainActor in
                        self?.serverOutput += output
                    }
                }
            }

            try pipProcess.run()
            pipProcess.waitUntilExit()

            outputPipe.fileHandleForReading.readabilityHandler = nil

            if pipProcess.terminationStatus != 0 {
                serverError = "Failed to install dependencies"
                isStarting = false
                return false
            }

            serverOutput += "\nSetup complete!\n"
            isStarting = false
            return true

        } catch {
            serverError = "Setup failed: \(error.localizedDescription)"
            isStarting = false
            return false
        }
    }
}
