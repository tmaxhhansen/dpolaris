package com.dpolaris.javaapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class BackendController {
    enum State {
        STOPPED,
        STARTING,
        RUNNING,
        ERROR
    }

    private static final Path PYTHON_EXE = Path.of("C:\\my-git\\dpolaris_ai\\.venv\\Scripts\\python.exe");
    private static final Path WORKDIR = Path.of("C:\\my-git\\dpolaris_ai");
    private static final String HEALTH_URL = "http://127.0.0.1:8420/health";

    private final Object lock = new Object();
    private final BiConsumer<String, String> logListener;
    private final Consumer<State> stateListener;

    private volatile Process process;
    private volatile State state = State.STOPPED;
    private volatile String lastError = "";

    BackendController(BiConsumer<String, String> logListener, Consumer<State> stateListener) {
        this.logListener = logListener;
        this.stateListener = stateListener;
    }

    void start() throws IOException {
        synchronized (lock) {
            if (isProcessAlive()) {
                emitLog("SYSTEM", "Backend already running; start() no-op.");
                setState(State.RUNNING);
                return;
            }

            if (!Files.isRegularFile(PYTHON_EXE)) {
                String message = "Missing python executable: " + PYTHON_EXE
                        + " | Fix: create backend venv at C:\\my-git\\dpolaris_ai\\.venv";
                setError(message);
                throw new IOException(message);
            }
            if (!Files.isDirectory(WORKDIR)) {
                String message = "Missing backend workdir: " + WORKDIR;
                setError(message);
                throw new IOException(message);
            }

            ProcessBuilder builder = new ProcessBuilder(List.of(
                    PYTHON_EXE.toString(),
                    "-m",
                    "cli.main",
                    "server",
                    "--host",
                    "127.0.0.1",
                    "--port",
                    "8420"
            ));
            builder.directory(WORKDIR.toFile());
            builder.environment().put("LLM_PROVIDER", "none");
            builder.environment().put("PYTHONUNBUFFERED", "1");

            emitLog("SYSTEM", "Starting backend process: " + String.join(" ", builder.command()));
            lastError = "";
            setState(State.STARTING);
            process = builder.start();

            startLogGobbler(process.getInputStream(), "STDOUT");
            startLogGobbler(process.getErrorStream(), "STDERR");
            startExitWatcher(process);
        }
    }

    void stop() {
        Process current;
        synchronized (lock) {
            current = process;
            process = null;
        }
        if (current == null) {
            if (state != State.ERROR) {
                setState(State.STOPPED);
            }
            return;
        }

        emitLog("SYSTEM", "Stopping backend process...");
        current.destroy();
        try {
            if (!current.waitFor(5, TimeUnit.SECONDS)) {
                emitLog("SYSTEM", "Graceful stop timed out. Forcing termination.");
                current.destroyForcibly();
                current.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (state != State.ERROR) {
            setState(State.STOPPED);
        }
    }

    void restart() throws IOException {
        stop();
        start();
    }

    boolean isProcessAlive() {
        Process current = process;
        return current != null && current.isAlive();
    }

    boolean isHealthy() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(HEALTH_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2_000);
            conn.setReadTimeout(2_000);
            int code = conn.getResponseCode();
            return code == 200;
        } catch (IOException ex) {
            return false;
        }
    }

    boolean waitUntilHealthy(Duration timeout, Duration interval) {
        long timeoutMs = Math.max(1_000L, timeout.toMillis());
        long intervalMs = Math.max(100L, interval.toMillis());
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isHealthy()) {
                setState(State.RUNNING);
                return true;
            }
            if (!isProcessAlive()) {
                setError("Backend process exited before becoming healthy.");
                return false;
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                setError("waitUntilHealthy interrupted.");
                return false;
            }
        }
        setError("Backend did not become healthy within timeout.");
        return false;
    }

    State getState() {
        return state;
    }

    String getLastError() {
        return lastError == null ? "" : lastError;
    }

    private void startLogGobbler(java.io.InputStream stream, String channel) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    emitLog(channel, line);
                }
            } catch (IOException ex) {
                emitLog("SYSTEM", "Log stream failed (" + channel + "): " + ex.getMessage());
            }
        }, "backend-log-" + channel.toLowerCase());
        thread.setDaemon(true);
        thread.start();
    }

    private void startExitWatcher(Process expected) {
        Thread watcher = new Thread(() -> {
            int code = -1;
            try {
                code = expected.waitFor();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }

            boolean wasTracked;
            synchronized (lock) {
                wasTracked = process == expected;
                if (wasTracked) {
                    process = null;
                }
            }

            if (!wasTracked) {
                return;
            }

            emitLog("SYSTEM", "Backend process exited with code " + code);
            if (state == State.STARTING || state == State.RUNNING) {
                setError("Backend exited unexpectedly with code " + code + ".");
            } else {
                setState(State.STOPPED);
            }
        }, "backend-exit-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void setError(String message) {
        lastError = message;
        setState(State.ERROR);
        emitLog("SYSTEM", message);
    }

    private void setState(State next) {
        state = next;
        if (stateListener != null) {
            stateListener.accept(next);
        }
    }

    private void emitLog(String channel, String message) {
        if (logListener != null) {
            logListener.accept(channel, message == null ? "" : message);
        }
    }
}
