package com.dpolaris.javaapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Manages the dpolaris_ai backend process lifecycle directly via ProcessBuilder.
 * Provides start, stop, reset & restart capabilities for macOS.
 */
public final class BackendProcessManager {
    private static final String DEFAULT_AI_PATH = System.getProperty("user.home") + "/my-git/dpolaris_ai";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8420;
    private static final Path PID_FILE = Path.of(System.getProperty("user.home"), "dpolaris_data", "run", "backend.pid");
    private static final int HEALTH_CHECK_TIMEOUT_MS = 30000;
    private static final int PORT_FREE_TIMEOUT_MS = 15000;

    public enum State { STOPPED, STARTING, RUNNING, STOPPING, ERROR }
    public record Status(State state, Long pid, String message, Instant startedAt) {}

    private final AtomicReference<Process> processRef = new AtomicReference<>(null);
    private final AtomicReference<Long> pidRef = new AtomicReference<>(null);
    private final AtomicReference<State> stateRef = new AtomicReference<>(State.STOPPED);
    private final AtomicReference<Instant> startedAtRef = new AtomicReference<>(null);
    private final AtomicReference<String> messageRef = new AtomicReference<>("");
    private final List<Consumer<String>> logListeners = new ArrayList<>();

    private String aiPath = DEFAULT_AI_PATH;
    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private String devicePreference = "auto"; // auto, cpu, mps, cuda

    public void configure(String aiPath, String host, int port) {
        if (aiPath != null && !aiPath.isBlank()) {
            this.aiPath = expandHome(aiPath.trim());
        }
        if (host != null && !host.isBlank()) {
            this.host = host.trim();
        }
        if (port > 0 && port < 65536) {
            this.port = port;
        }
    }

    public void setDevicePreference(String device) {
        if (device != null && !device.isBlank()) {
            this.devicePreference = device.trim().toLowerCase();
        }
    }

    public String getDevicePreference() {
        return devicePreference;
    }

    public void addLogListener(Consumer<String> listener) {
        synchronized (logListeners) {
            logListeners.add(listener);
        }
    }

    public void removeLogListener(Consumer<String> listener) {
        synchronized (logListeners) {
            logListeners.remove(listener);
        }
    }

    private void emitLog(String line) {
        synchronized (logListeners) {
            for (Consumer<String> listener : logListeners) {
                try {
                    listener.accept(line);
                } catch (Exception ignored) {}
            }
        }
    }

    public Status getStatus() {
        return new Status(stateRef.get(), pidRef.get(), messageRef.get(), startedAtRef.get());
    }

    public synchronized boolean start() {
        if (stateRef.get() == State.RUNNING || stateRef.get() == State.STARTING) {
            messageRef.set("Backend already running or starting");
            return false;
        }

        stateRef.set(State.STARTING);
        messageRef.set("Starting backend...");
        emitLog("[Backend] Starting...");

        try {
            // Check if port is already in use
            if (isPortInUse(port)) {
                emitLog("[Backend] Port " + port + " already in use, attempting to find existing process");
                Long existingPid = findPidOnPort(port);
                if (existingPid != null) {
                    pidRef.set(existingPid);
                    stateRef.set(State.RUNNING);
                    messageRef.set("Backend already running (PID: " + existingPid + ")");
                    emitLog("[Backend] Found existing process on port " + port + " (PID: " + existingPid + ")");
                    return true;
                }
            }

            // Build the command
            String pythonPath = aiPath + "/.venv/bin/python";
            File pythonFile = new File(pythonPath);
            if (!pythonFile.exists()) {
                pythonPath = aiPath + "/.venv/Scripts/python.exe"; // Windows fallback
            }
            if (!new File(pythonPath).exists()) {
                stateRef.set(State.ERROR);
                messageRef.set("Python venv not found at: " + aiPath + "/.venv");
                emitLog("[Backend] ERROR: Python venv not found");
                return false;
            }

            List<String> command = List.of(
                pythonPath,
                "-m", "cli.main",
                "server",
                "--host", host,
                "--port", String.valueOf(port)
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(aiPath));
            pb.redirectErrorStream(true);

            // Set device preference environment variable
            pb.environment().put("DPOLARIS_DEVICE", devicePreference);
            emitLog("[Backend] Device preference: " + devicePreference);
            emitLog("[Backend] Command: " + String.join(" ", command));

            Process process = pb.start();
            processRef.set(process);
            pidRef.set(process.pid());
            startedAtRef.set(Instant.now());

            // Write PID file
            writePidFile(process.pid());
            emitLog("[Backend] PID: " + process.pid());

            // Start log reader thread
            Thread logReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitLog(line);
                    }
                } catch (IOException e) {
                    emitLog("[Backend] Log reader stopped: " + e.getMessage());
                }
            }, "backend-log-reader");
            logReader.setDaemon(true);
            logReader.start();

            // Wait for health check
            boolean healthy = waitForHealth(HEALTH_CHECK_TIMEOUT_MS);
            if (healthy) {
                stateRef.set(State.RUNNING);
                messageRef.set("Backend running on " + host + ":" + port);
                emitLog("[Backend] Health check passed - backend is ready");
                return true;
            } else {
                stateRef.set(State.ERROR);
                messageRef.set("Backend started but health check failed");
                emitLog("[Backend] ERROR: Health check failed after " + (HEALTH_CHECK_TIMEOUT_MS / 1000) + "s");
                return false;
            }

        } catch (Exception e) {
            stateRef.set(State.ERROR);
            messageRef.set("Failed to start: " + e.getMessage());
            emitLog("[Backend] ERROR: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean stop() {
        if (stateRef.get() == State.STOPPED) {
            messageRef.set("Backend already stopped");
            return true;
        }

        stateRef.set(State.STOPPING);
        messageRef.set("Stopping backend...");
        emitLog("[Backend] Stopping...");

        try {
            Process process = processRef.get();
            if (process != null && process.isAlive()) {
                process.destroy();
                boolean exited = process.waitFor(10, TimeUnit.SECONDS);
                if (!exited) {
                    emitLog("[Backend] Force killing process...");
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }

            // Also try to kill by PID if we have one
            Long pid = pidRef.get();
            if (pid != null) {
                killByPid(pid);
            }

            // Try to kill process on port if still running
            Long portPid = findPidOnPort(port);
            if (portPid != null) {
                emitLog("[Backend] Killing process on port " + port + " (PID: " + portPid + ")");
                killByPid(portPid);
            }

            // Wait for port to be free
            waitForPortFree(PORT_FREE_TIMEOUT_MS);

            processRef.set(null);
            pidRef.set(null);
            startedAtRef.set(null);
            stateRef.set(State.STOPPED);
            messageRef.set("Backend stopped");
            deletePidFile();
            emitLog("[Backend] Stopped");
            return true;

        } catch (Exception e) {
            stateRef.set(State.ERROR);
            messageRef.set("Failed to stop: " + e.getMessage());
            emitLog("[Backend] ERROR stopping: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean resetAndRestart() {
        emitLog("[Backend] Reset & Restart initiated");

        // Stop if running
        if (stateRef.get() != State.STOPPED) {
            if (!stop()) {
                emitLog("[Backend] Failed to stop during reset");
            }
        }

        // Wait for port to be completely free
        emitLog("[Backend] Waiting for port " + port + " to be free...");
        if (!waitForPortFree(PORT_FREE_TIMEOUT_MS)) {
            // Force kill anything on that port
            Long portPid = findPidOnPort(port);
            if (portPid != null) {
                emitLog("[Backend] Force killing PID " + portPid + " on port " + port);
                killByPid(portPid);
                waitForPortFree(5000);
            }
        }

        // Small delay to ensure cleanup
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start fresh
        emitLog("[Backend] Starting fresh...");
        return start();
    }

    public boolean isRunning() {
        // Check process first
        Process process = processRef.get();
        if (process != null && process.isAlive()) {
            return true;
        }

        // Check port
        if (isPortInUse(port)) {
            return checkHealth();
        }

        return false;
    }

    public boolean checkHealth() {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(5000);
            // Try HTTP health check
            var out = socket.getOutputStream();
            out.write(("GET /health HTTP/1.1\r\nHost: " + host + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = reader.readLine();
            return line != null && line.contains("200");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForHealth(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (checkHealth()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean waitForPortFree(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!isPortInUse(port)) {
                return true;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isPortInUse(int port) {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Long findPidOnPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-ti", ":" + port);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    return Long.parseLong(line.trim().split("\\s+")[0]);
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return null;
    }

    private void killByPid(long pid) {
        try {
            // Try graceful kill first
            new ProcessBuilder("kill", String.valueOf(pid)).start().waitFor(5, TimeUnit.SECONDS);
            Thread.sleep(500);

            // Force kill if still running
            ProcessBuilder check = new ProcessBuilder("kill", "-0", String.valueOf(pid));
            if (check.start().waitFor(2, TimeUnit.SECONDS) && check.start().exitValue() == 0) {
                new ProcessBuilder("kill", "-9", String.valueOf(pid)).start().waitFor(3, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {}
    }

    private void writePidFile(long pid) {
        try {
            Files.createDirectories(PID_FILE.getParent());
            Files.writeString(PID_FILE, String.valueOf(pid));
        } catch (IOException e) {
            emitLog("[Backend] Warning: Could not write PID file: " + e.getMessage());
        }
    }

    private void deletePidFile() {
        try {
            Files.deleteIfExists(PID_FILE);
        } catch (IOException ignored) {}
    }

    private static String expandHome(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
